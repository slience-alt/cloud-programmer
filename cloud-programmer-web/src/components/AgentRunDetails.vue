<template>
  <div class="agent-run-details">
    <!-- 展开入口 -->
    <button
      class="run-detail-toggle"
      type="button"
      @click="toggleDetails"
    >
      <span class="toggle-left">
        <span class="run-detail-icon">◷</span>
        <span>执行详情</span>

        <span
          v-if="detail"
          class="run-status"
          :class="statusClass(detail.status)"
        >
          {{ statusText(detail.status) }}
        </span>
      </span>

      <span class="toggle-right">
        <span v-if="detail?.durationMs != null">
          {{ formatDuration(detail.durationMs) }}
        </span>
        <span class="chevron" :class="{ expanded: expanded }">›</span>
      </span>
    </button>

    <!-- 详情内容 -->
    <div v-if="expanded" class="run-detail-panel">
      <div v-if="loading" class="run-message">
        正在加载执行详情…
      </div>

      <div v-else-if="error" class="run-error">
        <span>{{ error }}</span>
        <button type="button" @click="loadDetail">重试</button>
      </div>

      <template v-else-if="detail">
        <!-- 概览 -->
        <div class="run-overview">
          <div class="overview-item">
            <span>执行状态</span>
            <strong :class="statusClass(detail.status)">
              {{ statusText(detail.status) }}
            </strong>
          </div>

          <div class="overview-item">
            <span>总耗时</span>
            <strong>{{ formatDuration(detail.durationMs) }}</strong>
          </div>

          <div class="overview-item">
            <span>执行步骤</span>
            <strong>{{ displayEvents.length }}</strong>
          </div>
        </div>

        <!-- 错误信息 -->
        <div
          v-if="detail.errorMessage"
          class="run-error-block"
        >
          <strong>执行失败原因</strong>
          <pre>{{ detail.errorMessage }}</pre>
        </div>

        <!-- 事件时间线 -->
       <div v-if="detail.events?.length" class="event-list">
          <div v-for="(event, index) in displayEvents" :key="event.id ?? `${event.eventType}-${index}`"
            class="event-item">
            <div class="event-timeline">
              <span
                class="event-dot"
                :class="statusClass(event.status)"
              ></span>
              <span
                v-if="index < detail.events.length - 1"
                class="event-line"
              ></span>
            </div>

            <div class="event-content">
              <div class="event-heading">
                <div class="event-title">
                  {{ eventTitle(event) }}
                </div>

                <span
                  class="run-status"
                  :class="statusClass(event.status)"
                >
                  {{ statusText(event.status) }}
                </span>
             </div>

              <div class="event-meta">
                <span v-if="event.durationMs != null">
                  耗时 {{ formatDuration(event.durationMs) }}
                </span>

                <span v-if="event.displayType === 'token_group'">
                  {{ formatTime(event.startTime) }}
                  <template v-if="event.endTime && event.endTime !== event.startTime">
                    - {{ formatTime(event.endTime) }}
                  </template>
                </span>

                <span v-else-if="event.createTime">
                  {{ formatTime(event.createTime) }}
                </span>

                <span v-if="event.displayType === 'token_group'">
                  {{ event.eventCount }} 条输出
                </span>
             </div>
              <details v-if="event.inputContent || event.outputContent" class="event-detail-content">
                <summary>
                  {{ event.displayType === 'token_group'
                    ? '查看模型输出内容'
                  : '查看调用内容'
                  }}
                </summary>

                <div v-if="event.inputContent" class="content-block">
                  <strong>输入参数</strong>
                  <pre>{{ event.inputContent }}</pre>
                </div>

                <div v-if="event.outputContent" class="content-block">
                  <strong>输出结果</strong>
                  <pre>{{ event.outputContent }}</pre>
                </div>
              </details>
            </div>
          </div>
        </div>

        <div v-else class="run-message">
          暂无执行事件记录
        </div>

        <!-- 输入与最终结果 -->
        <details class="result-details">
          <summary>查看任务输入与最终结果</summary>

          <div v-if="detail.inputText" class="content-block">
            <strong>任务输入</strong>
            <pre>{{ detail.inputText }}</pre>
          </div>

          <div v-if="detail.resultText" class="content-block">
            <strong>最终结果</strong>
            <pre>{{ detail.resultText }}</pre>
          </div>
        </details>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { getAgentRunDetail } from '../api/agent'

const props = defineProps({
  taskId: {
    type: String,
    default: ''
  }
})

const expanded = ref(false)
const loading = ref(false)
const error = ref('')
const detail = ref(null)

let requestVersion = 0

async function toggleDetails() {
  expanded.value = !expanded.value

  if (expanded.value && !detail.value && props.taskId) {
    await loadDetail()
  }
}

async function loadDetail() {
  if (!props.taskId) {
    error.value = '当前消息没有关联的任务 ID'
    return
  }

  const version = ++requestVersion
  loading.value = true
  error.value = ''

  try {
    const response = await getAgentRunDetail(props.taskId)

    // 防止切换到其他任务后，旧请求覆盖当前详情
    if (version !== requestVersion) return

    // 兼容 API 方法返回 Axios Response 或直接返回 data
    detail.value = response?.data ?? response
  } catch (e) {
    if (version !== requestVersion) return

    console.error('获取 Agent 执行详情失败：', e)
    error.value =
      e?.response?.status === 404
        ? '未找到执行记录，请稍后重试'
        : e?.response?.data?.message ||
          e?.message ||
          '获取执行详情失败'
  } finally {
    if (version === requestVersion) {
      loading.value = false
    }
  }
}

watch(
  () => props.taskId,
  () => {
    requestVersion++
    detail.value = null
    error.value = ''
    loading.value = false
  }
)

function statusText(status) {
  const statusMap = {
    PENDING: '等待执行',
    RUNNING: '执行中',
    SUCCESS: '成功',
    COMPLETED: '成功',
    FAILED: '失败',
    ERROR: '异常'
  }

  return statusMap[String(status || '').toUpperCase()] || status || '未知'
}

function statusClass(status) {
  const value = String(status || '').toUpperCase()

  if (['SUCCESS', 'COMPLETED'].includes(value)) return 'status-success'
  if (['FAILED', 'ERROR'].includes(value)) return 'status-failed'
  if (['RUNNING', 'PENDING'].includes(value)) return 'status-running'

  return 'status-default'
}

function eventTitle(event) {
  const type = String(event.eventType || '').toUpperCase()

  if (type === 'MODEL_CALL') return `模型调用${event.eventName ? `：${event.eventName}` : ''}`
  if (type === 'TOOL_CALL') return `工具调用：${event.eventName || '未命名工具'}`
  if (type === 'ERROR') return `执行异常${event.eventName ? `：${event.eventName}` : ''}`

  return event.eventName || event.eventType || '执行事件'
}

function formatDuration(value) {
  if (value == null || Number.isNaN(Number(value))) return '—'

  const ms = Number(value)
  if (ms < 1000) return `${ms} ms`

  return `${(ms / 1000).toFixed(2)} 秒`
}

function formatTime(value) {
  if (!value) return ''

  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

/**
 * 合并连续的 Token 事件
 *
 * 规则：
 * 1. 连续 Token 合并为一条模型输出记录
 * 2. 遇到非 Token 事件时结束当前分组
 * 3. 工具调用、工具返回、异常等事件仍然独立展示
 */
function groupRunEvents(events) {
  if (!Array.isArray(events) || events.length === 0) {
    return []
  }

  const result = []

  events.forEach(event => {
    const eventType = String(event.eventType || '').toLowerCase()
    const isToken = eventType === 'token'

    if (!isToken) {
      result.push({
        ...event,
        displayType: 'normal'
      })
      return
    }

    const last = result[result.length - 1]

    // 连续 Token 合并到上一条记录
    if (last && last.displayType === 'token_group') {
      last.outputContent += event.outputContent || ''
      last.eventCount += 1
      last.endTime = event.createTime || last.endTime
      last.lastEventId = event.id

      // 保留最新一条 Token 的状态
      last.status = event.status || last.status
    } else {
      // 创建新的 Token 分组
      result.push({
        id: event.id,
        lastEventId: event.id,
        displayType: 'token_group',
        eventType: 'token',
        eventName: '模型输出',
        inputContent: '',
        outputContent: event.outputContent || '',
        startTime: event.createTime,
        endTime: event.createTime,
        eventCount: 1,
        status: event.status
      })
    }
  })

  return result
}

// 时间线实际展示的数据
const displayEvents = computed(() => {
  return groupRunEvents(detail.value?.events || [])
})
</script>

<style scoped>
.agent-run-details {
  margin-top: 10px;
  max-width: 760px;
  border: 1px solid #e8eaf0;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  color: #303747;
  font-size: 13px;
}

.run-detail-toggle {
  display: flex;
  width: 100%;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 11px 14px;
  border: 0;
  background: #fafbfc;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.run-detail-toggle:hover {
  background: #f4f6fa;
}

.toggle-left,
.toggle-right {
  display: flex;
  align-items: center;
  gap: 9px;
}

.run-detail-icon {
  color: #64748b;
  font-size: 17px;
}

.toggle-right {
  color: #7b8495;
  white-space: nowrap;
}

.chevron {
  display: inline-block;
  font-size: 22px;
  transition: transform 0.2s;
}

.chevron.expanded {
  transform: rotate(90deg);
}

.run-status {
  display: inline-block;
  padding: 2px 7px;
  border-radius: 5px;
  font-size: 11px;
  white-space: nowrap;
}

.status-success {
  color: #16803c;
  background: #eaf8ef;
}

.status-failed {
  color: #c73535;
  background: #fff0f0;
}

.status-running {
  color: #976300;
  background: #fff6dd;
}

.status-default {
  color: #64748b;
  background: #eef1f5;
}

.run-detail-panel {
  padding: 14px;
  border-top: 1px solid #e8eaf0;
}

.run-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 18px;
}

.overview-item {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 10px;
  border-radius: 8px;
  background: #f7f8fa;
}

.overview-item > span {
  color: #7b8495;
  font-size: 12px;
}

.overview-item strong {
  font-weight: 600;
}

.event-list {
  display: flex;
  flex-direction: column;
}

.event-item {
  display: flex;
  gap: 12px;
  min-height: 66px;
}

.event-timeline {
  position: relative;
  display: flex;
  justify-content: center;
  width: 14px;
  flex-shrink: 0;
}

.event-dot {
  z-index: 1;
  width: 9px;
  height: 9px;
  margin-top: 5px;
  border: 2px solid currentColor;
  border-radius: 50%;
  background: #fff;
}

.event-line {
  position: absolute;
  top: 16px;
  bottom: 0;
  width: 1px;
  background: #e2e6ed;
}

.event-content {
  flex: 1;
  min-width: 0;
  padding-bottom: 16px;
}

.event-heading {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.event-title {
  font-weight: 600;
  overflow-wrap: anywhere;
}

.event-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 5px;
  color: #8790a0;
  font-size: 11px;
}

.event-detail-content,
.result-details {
  margin-top: 9px;
  color: #525c6d;
}

.event-detail-content summary,
.result-details summary {
  cursor: pointer;
  color: #536b94;
}

.content-block {
  margin-top: 10px;
  min-width: 0;
}

.content-block strong {
  display: block;
  margin-bottom: 5px;
  font-size: 12px;
}

.content-block pre,
.run-error-block pre {
  margin: 0;
  padding: 10px;
  max-height: 280px;
  overflow: auto;
  border-radius: 6px;
  background: #f6f7f9;
  color: #414a59;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.run-error-block {
  margin-bottom: 16px;
  padding: 10px;
  border: 1px solid #ffd6d6;
  border-radius: 8px;
  background: #fff7f7;
  color: #b42323;
}

.run-error-block pre {
  margin-top: 8px;
  background: #fff;
  color: #a12828;
}

.run-message {
  padding: 16px 4px;
  color: #8790a0;
  text-align: center;
}

.run-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #c73535;
}

.run-error button {
  padding: 4px 9px;
  border: 1px solid #e5bcbc;
  border-radius: 5px;
  background: #fff;
  color: #b42323;
  cursor: pointer;
}

@media (max-width: 600px) {
  .run-overview {
    grid-template-columns: 1fr;
  }
}
</style>