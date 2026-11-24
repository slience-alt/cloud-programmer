
package com.cloud.programmer.runtime.task;

import com.cloud.programmer.enums.ResponseCodeEnum;
import com.cloud.programmer.exception.BizException;
import reactor.core.Disposable;

import java.util.concurrent.atomic.AtomicBoolean;

public class AgentTaskContext {

    private final String taskId;

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * 当前正在执行的模型订阅
     */
    private volatile Disposable modelSubscription;

    public AgentTaskContext(String taskId) {
        this.taskId = taskId;
    }

    public boolean cancel() {
        if (!cancelled.compareAndSet(false, true)) {
            return false;
        }

        // 尽可能立即取消模型流
        Disposable subscription = modelSubscription;
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }

        return true;
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void setModelSubscription(Disposable subscription) {
        this.modelSubscription = subscription;

        // 处理取消和订阅注册之间的竞态
        if (cancelled.get() && subscription != null) {
            subscription.dispose();
        }
    }

    public void clearModelSubscription(Disposable subscription) {
        if (this.modelSubscription == subscription) {
            this.modelSubscription = null;
        }
    }

    public void checkCancelled() {
        if (cancelled.get() || Thread.currentThread().isInterrupted()) {
            throw new BizException(ResponseCodeEnum.TASK_IS_CONCELED);
        }
    }
}
