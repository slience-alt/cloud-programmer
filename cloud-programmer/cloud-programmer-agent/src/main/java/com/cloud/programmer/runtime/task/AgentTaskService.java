package com.cloud.programmer.runtime.task;

import com.cloud.programmer.enums.ResponseCodeEnum;
import com.cloud.programmer.exception.BizException;
import com.cloud.programmer.runtime.entity.AgentRun;
import com.cloud.programmer.runtime.entity.AgentRunEvent;
import com.cloud.programmer.runtime.orchestrator.AgentOrchestrator;
import com.cloud.programmer.runtime.service.AgentRunEventService;
import com.cloud.programmer.runtime.service.AgentRunService;
import com.cloud.programmer.runtime.stream.AgentStreamEvent;
import com.cloud.programmer.security.context.SecurityUtils;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class AgentTaskService {

    private final AgentOrchestrator agentOrchestrator;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final AgentRunService agentRunService;
    private final TaskInitializationService taskInitializationService;
    private final AgentRunEventService agentRunEventService;

    // 第一版暂时将任务和 SSE 事件存储在内存中
    private final ConcurrentMap<String, AgentTask> tasks =
            new ConcurrentHashMap<>();


    private final Map<String, SseEmitter> emitters =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Object> eventLocks = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, StringBuilder> tokenBuffers =
            new ConcurrentHashMap<>();


    /**
     * 每个任务对应的定时刷新任务
     */
    private final ConcurrentMap<String, ScheduledFuture<?>> flushFutures =
            new ConcurrentHashMap<>();

    /**
     * Token 缓冲刷新间隔
     */
    private static final long TOKEN_FLUSH_INTERVAL_MS = 200;

    /**
     * Token 缓冲达到该长度时立即刷新
     */
    private static final int TOKEN_FLUSH_THRESHOLD = 1024;

    /**
     * 负责定时刷新 Token 缓冲区
     */
    private final ScheduledExecutorService tokenFlushScheduler =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "agent-token-flusher");
                    thread.setDaemon(true);
                    return thread;
                }
            });

    public AgentTaskService(
            AgentOrchestrator agentOrchestrator,
            @Qualifier("agentTaskExecutor")
            ThreadPoolTaskExecutor taskExecutor,
            AgentRunService agentRunService, TaskInitializationService taskInitializationService, AgentRunEventService agentRunEventService) {

        this.agentOrchestrator = agentOrchestrator;
        this.taskExecutor = taskExecutor;
        this.agentRunService = agentRunService;
        this.agentRunEventService = agentRunEventService;
        this.taskInitializationService = taskInitializationService;
    }

    /**
     * 提交非流式任务
     */
    public AgentTask submit(String conversationId, String input, Long agentId) {

        // 1. 参数校验
        validateSubmitParams(input, agentId);
        // 2. 规范化会话 ID
        conversationId = normalizeConversationId(conversationId);

        // 3. 创建任务对象
        String taskId = UUID.randomUUID().toString();

        AgentTask task = new AgentTask(taskId, input, conversationId, agentId, SecurityUtils.getCurrentUserId());

        // 4. 初始化任务数据库记录
        // 方法返回时，事务已经提交
        taskInitializationService.initializeTask(taskId, conversationId, agentId, input, SecurityUtils.getCurrentUserId());
        // 5. 注册内存任务
        tasks.put(taskId, task);
        // 6. 提交线程池执行
        try {
            taskExecutor.execute(() -> executeTask(task));
        } catch (TaskRejectedException e) {
            tasks.remove(taskId, task);
            markRunFailedSafely(taskId, "线程池拒绝执行任务");
            throw e;
        }

        return task;
    }

    /**
     * 提交流式任务
     */
    /**
     * 提交流式任务
     */
    public AgentTask submitStream(String conversationId, String input, Long agentId) {
        validateSubmitParams(input, agentId);

        // 1. 规范化会话 ID
        conversationId = normalizeConversationId(conversationId);

        // 2. 创建任务对象
        String taskId = UUID.randomUUID().toString();
        AgentTask task = new AgentTask(taskId, input, conversationId, agentId, SecurityUtils.getCurrentUserId());

        // 3. 事务初始化：绑定会话与 Agent，并创建执行记录
        taskInitializationService.initializeTask(taskId, conversationId, agentId, input, SecurityUtils.getCurrentUserId());

        // 4. 注册内存任务
        tasks.put(taskId, task);

        // 5. 提交线程池执行
        try {
            taskExecutor.execute(() -> executeStreamTask(task));
        } catch (TaskRejectedException e) {
            tasks.remove(taskId, task);
            markRunFailedSafely(taskId, "线程池拒绝执行任务");
            throw e;
        }

        return task;
    }

    /**
     * 取消 Agent 任务
     */
    public boolean cancelTask(String taskId, Long userId) {
        AgentTask task = getOwnedTask(taskId, userId);

        if (!task.requestCancel()) {
            return false;
        }

        if ("PENDING".equals(task.getStatus())) {
            cancelTaskSafely(task, "用户取消任务");
        }

        return true;
    }
    /**
     * 处理空会话 ID
     */
    private String normalizeConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return conversationId;
    }

    private void cancelTaskSafely(AgentTask task, String reason) {
        String taskId = task.getTaskId();

        if (!task.cancel()) {
            return;
        }

        try {
            agentRunService.markCancelled(taskId, reason);
        } catch (Exception e) {
            log.error("更新 Agent 取消状态失败，taskId={}", taskId, e);
        }

        try {
            publishEvent(taskId, new AgentStreamEvent("cancelled", "任务已取消"));
            cleanupTaskResources(taskId);
        } catch (Exception e) {
            log.error(
                    "发布 Agent 取消事件失败，taskId={}",
                    taskId,
                    e
            );
        }
    }

    /**
     * 执行非流式任务
     */
    private void executeTask(AgentTask task) {
        String taskId = task.getTaskId();
        try {
            // 如果任务已经取消，或者状态不允许启动，直接退出
            if (!task.start()) {
                if (task.getContext().isCancelled()) {
                    cancelTaskSafely(task, "用户取消任务");
                }
                return;
            }

            // 数据库状态更新为 RUNNING
            agentRunService.markRunning(taskId);

            task.getContext().checkCancelled();

            String result = agentOrchestrator.run(task.getInput(), task.getConversationId(), task.getAgentId(), task.getOwnerUserId(), taskId, task.getContext());

            task.getContext().checkCancelled();

            // 先完成内存状态转换，再更新数据库
            if (task.complete(result)) {
                agentRunService.markSuccess(taskId, result);
            }

        } catch (Exception e) {

            if (task.getContext().isCancelled()) {
                log.info("Agent 任务已取消，taskId={}", taskId);
                cancelTaskSafely(task, "用户取消任务");
                return;
            }


            log.error("Agent 非流式任务执行失败，taskId={}", taskId, e);

            if (task.fail("Agent 执行失败，请检查服务日志")) {
                markRunFailedSafely(taskId, "Agent 执行失败");
            }
        }
    }

    /**
     * 执行流式任务
     */
    private void executeStreamTask(AgentTask task) {
        String taskId = task.getTaskId();
        try {
            if (!task.start()) {
                if (task.getContext().isCancelled()) {
                    cancelTaskSafely(task, "用户取消任务");
                }
                return;
            }
            agentRunService.markRunning(taskId);
            task.getContext().checkCancelled();
            String result = agentOrchestrator.runStream(task.getInput(), event -> publishEvent(taskId, event), task.getConversationId(), task.getAgentId(), task.getOwnerUserId(), taskId, task.getContext());
            task.getContext().checkCancelled();
            if (task.complete(result)) {
                agentRunService.markSuccess(taskId, result);

                // done 事件发布前会自动刷新剩余 Token
                publishEvent(taskId, new AgentStreamEvent("done", result));

                // 终态事件持久化成功后清理资源
                cleanupTaskResources(taskId);
            }
        } catch (Exception e) {
            if (task.getContext().isCancelled()) {
                log.info("Agent 任务已取消，taskId={}", taskId);
                cancelTaskSafely(task, "用户取消任务");
                return;
            }


            log.error("Agent 流式任务执行失败，taskId={}", taskId, e);

            if (task.fail("Agent 执行失败")) {
                markRunFailedSafely(taskId, "Agent 执行失败");
                try {
                    publishEvent(taskId, new AgentStreamEvent("error", "Agent 执行失败，请稍后重试"));
                    cleanupTaskResources(taskId);
                } catch (Exception publishException) {
                    log.error(
                            "发布 Agent 失败事件时发生异常，taskId={}",
                            taskId,
                            publishException
                    );
                }
            }
        }
    }

    /**
     * 安全地更新失败状态，避免数据库异常覆盖原始异常
     */
    private void markRunFailedSafely(String taskId, String message) {
        try {
            agentRunService.markFailed(taskId, message);
        } catch (Exception e) {
            log.error("更新 Agent 执行失败记录时发生异常，taskId={}", taskId, e);
        }
    }

    /**
     * 根据任务 ID 查询内存中的任务
     */
    public AgentTask getTask(String taskId, Long userId) {
        return getOwnedTask(taskId, userId);
    }

    /**
     * 发布 SSE 事件，同时保存事件历史
     */
    /**
     * 发布 SSE 事件，同时保存事件历史。
     *
     * Token 事件先进入缓冲区，达到阈值或定时到期后再合并持久化。
     * 非 Token 事件到来时，先刷新 Token 缓冲区，保证事件顺序。
     */
    private void publishEvent(String taskId, AgentStreamEvent event) {
        if (!StringUtils.hasText(taskId) || event == null) {
            return;
        }

        Object lock = eventLocks.computeIfAbsent(
                taskId,
                key -> new Object()
        );

        synchronized (lock) {
            // Token 事件走缓冲逻辑
            if ("token".equals(event.getType())) {
                appendToken(taskId, event.getContent());
                return;
            }

            // 非 Token 事件先刷新之前积累的 Token
            flushTokenBufferLocked(taskId);

            // 再保存并推送当前事件
            persistAndSendEvent(taskId, event.getType(), event.getContent());
        }
    }


    /**
     * 持久化事件并推送 SSE。
     *
     * 必须先写数据库，获得 eventId 后再发送 SSE。
     */
    private void persistAndSendEvent(String taskId, String eventType, String content) {

        // 1. 先持久化事件
        Long eventId;

        if ("token".equals(eventType)) {
            eventId = agentRunEventService.recordTokenBatch(taskId, content);
        } else {
            eventId = agentRunEventService.record(taskId, eventType, null, null, content, "SUCCESS", null);
        }


        // 2. 持久化失败不能继续发送没有有效 ID 的事件
        if (eventId == null) {
            throw new IllegalStateException(
                    "事件持久化失败，taskId=" + taskId + ", eventType=" + eventType
            );
        }

        // 2. 获取当前 SSE 连接
        SseEmitter emitter = emitters.get(taskId);

        if (emitter == null) {
            return;
        }
        // 3. 发送 SSE
        try {
            emitter.send(
                    SseEmitter.event()
                            .id(String.valueOf(eventId))
                            .name(eventType)
                            .data(content)
            );
        } catch (IOException | IllegalStateException e) {
            // 客户端连接可能已经断开
            log.warn("SSE 客户端连接已断开，停止推送，taskId={}, eventId={}, eventType={}", taskId, eventId, eventType);
            // 只移除当前失效的 emitter
            emitters.remove(taskId, emitter);
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // 连接已经失效，无需重复处理
            }
        }
    }

    /**
     * 建立 SSE 连接，并重放历史事件
     */
    public SseEmitter connect(
            String taskId,
            Long userId,
            Long lastEventId) {

        // 1. 先通过数据库校验任务归属
        AgentRun run = agentRunService.getOwnedRun(taskId, userId);

        if (run == null) {
            throw new BizException(ResponseCodeEnum.DATA_NOT_FOUND);
        }
        SseEmitter emitter = new SseEmitter(0L);
        // 2. 获取当前任务的事件锁
        Object lock = eventLocks.computeIfAbsent(taskId, key -> new Object());
        synchronized (lock) {
            // 3. 注册新的 SSE 连接
            SseEmitter oldEmitter = emitters.put(taskId, emitter);
            // 如果之前有连接，关闭旧连接
            if (oldEmitter != null && oldEmitter != emitter) {
                oldEmitter.complete();
            }
            Runnable cleanup = () -> emitters.remove(taskId, emitter);

            emitter.onCompletion(cleanup);
            emitter.onTimeout(() -> {
                cleanup.run();
                emitter.complete();
            });
            emitter.onError(error -> cleanup.run());
            try {
                // 4. 查询数据库中尚未接收的事件
                List<AgentRunEvent> history = agentRunEventService.listAfterEventId(taskId, lastEventId == null ? 0L : lastEventId);
                // 5. 按顺序回放历史事件
                for (AgentRunEvent event : history) {
                    emitter.send(
                            SseEmitter.event()
                                    .id(String.valueOf(event.getId()))
                                    .name(event.getEventType())
                                    .data(event.getOutputContent())
                    );
                }
                // 6. 如果任务已经结束，回放完成后关闭连接
                if (isTerminalStatus(run.getStatus())) {
                    cleanup.run();
                    emitter.complete();
                }
            } catch (Exception e) {
                cleanup.run();
                emitter.completeWithError(e);
                log.error(
                        "SSE 历史事件回放失败，taskId={}",
                        taskId,
                        e
                );
            }
        }
        return emitter;
    }

    @PreDestroy
    public void destroy() {
        tokenFlushScheduler.shutdown();
        try {
            if (!tokenFlushScheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                tokenFlushScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            tokenFlushScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 提交参数校验
     */
    private void validateSubmitParams(String input, Long agentId) {
        if (agentId == null) {
            throw new IllegalArgumentException("agentId 不能为空");
        }

        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("用户输入不能为空");
        }
    }

    /**
     * 获取当前用户拥有的任务
     */
    private AgentTask getOwnedTask(String taskId, Long userId) {
        AgentTask task = tasks.get(taskId);

        if (task == null || !userId.equals(task.getOwnerUserId())) {
            // 不区分任务不存在和无权访问，避免泄露任务是否存在
            throw new BizException(ResponseCodeEnum.DATA_NOT_FOUND);
        }

        return task;
    }

    private boolean isTerminalStatus(String status) {
        return "SUCCESS".equals(status)
                || "FAILED".equals(status)
                || "CANCELLED".equals(status);
    }

    /**
     * 将 Token 追加到缓冲区。
     * 调用方必须持有当前任务的 eventLock。
     */
    private void appendToken(String taskId, String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        StringBuilder buffer = tokenBuffers.computeIfAbsent(taskId, key -> new StringBuilder());
        buffer.append(content);
        // 达到阈值，立即刷新
        if (buffer.length() >= TOKEN_FLUSH_THRESHOLD) {
            try {
                flushTokenBufferLocked(taskId);
            } catch (Exception e) {
                // 刷新失败，不清理缓冲区，安排重试
                log.error("Token 缓冲区达到阈值后刷新失败，taskId={}", taskId, e);
                scheduleTokenFlush(taskId, 1000);
            }
            return;
        }

        // 未达到阈值，安排定时刷新
        scheduleTokenFlush(taskId, TOKEN_FLUSH_INTERVAL_MS);
    }

    /**
     * 安排 Token 刷新任务。
     * 调用方必须持有当前任务的 eventLock。
     */
    private void scheduleTokenFlush(String taskId, long delayMs) {

        ScheduledFuture<?> oldFuture = flushFutures.get(taskId);

        // 已经存在有效的定时任务，不重复创建
        if (oldFuture != null
                && !oldFuture.isDone()
                && !oldFuture.isCancelled()) {
            return;
        }

        // 用于让定时任务准确识别自己对应的 Future
        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();

        try {
            ScheduledFuture<?> future = tokenFlushScheduler.schedule(
                    () -> flushTokenBuffer(taskId, futureRef.get()),
                    Math.max(1L, delayMs),
                    TimeUnit.MILLISECONDS
            );

            futureRef.set(future);
            flushFutures.put(taskId, future);

        } catch (RejectedExecutionException e) {
            // 通常发生在应用关闭、线程池停止接收任务时
            log.error("Token 定时刷新任务提交失败，taskId={}", taskId, e);
        }
    }

    /**
     * 定时刷新入口。
     */
    private void flushTokenBuffer(String taskId, ScheduledFuture<?> currentFuture) {

        Object lock = eventLocks.get(taskId);

        if (lock == null) {
            return;
        }

        synchronized (lock) {
            // 只删除自己对应的 Future
            // 如果已经被其他刷新操作取消或替换，则直接退出
            if (currentFuture == null || !flushFutures.remove(taskId, currentFuture)) {
                return;
            }
            try {
                flushTokenBufferLocked(taskId);
            } catch (Exception e) {
                log.error("定时刷新 Token 缓冲区失败，taskId={}", taskId, e);
                // 数据库写入失败，保留缓冲区，安排重试
                if (tokenBuffers.containsKey(taskId)) {
                    scheduleTokenFlush(taskId, 1000);
                }
            }
        }
    }

    /**
     * 刷新 Token 缓冲区。
     * 调用方必须持有当前任务的 eventLock。
     */
    private void flushTokenBufferLocked(String taskId) {

        StringBuilder buffer = tokenBuffers.get(taskId);

        if (buffer == null || buffer.length() == 0) {
            cancelTokenFlush(taskId);
            return;
        }

        String content = buffer.toString();

        // 先持久化，持久化成功后才清理缓冲区
        persistAndSendEvent(taskId, "token", content);

        // 持久化成功后，移除当前缓冲区
        tokenBuffers.remove(taskId, buffer);

        // 取消尚未执行的定时任务
        cancelTokenFlush(taskId);
    }

    /**
     * 取消定时刷新任务。
     */
    /**
     * 取消当前任务的 Token 定时刷新任务。
     * 调用方必须持有当前任务的 eventLock。
     */
    private void cancelTokenFlush(String taskId) {

        ScheduledFuture<?> future = flushFutures.remove(taskId);

        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * 清理任务相关的 Token 缓冲资源。
     *
     * 必须在任务终态事件成功持久化后调用。
     */
    private void cleanupTaskResources(String taskId) {
        Object lock = eventLocks.get(taskId);

        if (lock == null) {
            return;
        }

        synchronized (lock) {
            // 如果还有未持久化的 Token，不允许清理
            if (tokenBuffers.containsKey(taskId)) {
                log.warn(
                        "任务仍存在未持久化的 Token，暂不清理资源，taskId={}",
                        taskId
                );
                return;
            }

            cancelTokenFlush(taskId);

            // 仅清理 Token 相关资源，不删除 tasks
            eventLocks.remove(taskId, lock);
        }
    }
}