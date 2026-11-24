<template>
  <div class="mcp-page">
    <a-card :bordered="false">
      <div class="page-header">
        <div>
          <h2>MCP 管理</h2>
          <p class="page-desc">管理 Agent 可调用的 MCP 服务及连接配置</p>
        </div>
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>
          新建 MCP 服务
        </a-button>
      </div>

      <a-form layout="inline" class="search-form" @finish="loadServers">
        <a-form-item label="服务名称">
          <a-input
            v-model:value="keyword"
            placeholder="请输入 MCP 服务名称"
            allow-clear
            @pressEnter="loadServers"
          />
        </a-form-item>
        <a-form-item label="传输类型">
          <a-select
            v-model:value="transportType"
            placeholder="全部类型"
            allow-clear
            style="width: 160px"
            :options="transportOptions"
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="enabled"
            placeholder="全部状态"
            allow-clear
            style="width: 120px"
            :options="statusOptions"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="search">查询</a-button>
          <a-button class="reset-btn" @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="servers"
        :loading="loading"
        :pagination="pagination"
        :row-key="record => record.id"
        :scroll="{ x: 1100 }"
        @change="handleTableChange"
        class="mcp-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div class="server-name">{{ record.name || '-' }}</div>
          </template>

          <template v-else-if="column.key === 'description'">
            <a-tooltip :title="record.description">
              <span class="description-text">{{ record.description || '-' }}</span>
            </a-tooltip>
          </template>

          <template v-else-if="column.key === 'transportType'">
            <a-tag :color="record.transportType === 'STDIO' ? 'purple' : 'blue'">
              {{ transportLabel(record.transportType) }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'endpoint'">
            <a-tooltip :title="endpointText(record)">
              <span class="endpoint-text">{{ endpointText(record) }}</span>
            </a-tooltip>
          </template>

          <template v-else-if="column.key === 'enabled'">
            <a-switch
              :checked="record.enabled === true"
              :loading="record.statusLoading"
              checked-children="启用"
              un-checked-children="停用"
              @change="value => handleStatusChange(record, value)"
            />
          </template>

          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="openDetail(record)">详情</a-button>
              <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm
                title="确定删除这个 MCP 服务吗？"
                description="请确认没有 Agent 关联此服务，或后端已处理关联关系。"
                ok-text="删除"
                cancel-text="取消"
                @confirm="handleDelete(record)"
              >
                <a-button type="link" danger size="small">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新建 / 编辑 MCP 服务 -->
    <a-modal
      v-model:open="formVisible"
      :title="editing ? '编辑 MCP 服务' : '新建 MCP 服务'"
      :confirm-loading="submitting"
      ok-text="保存"
      cancel-text="取消"
      width="760px"
      @ok="handleSubmit"
      @cancel="resetForm"
      destroy-on-close
    >
      <a-form
        ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
      >
        <a-form-item label="服务名称" name="name">
          <a-input
            v-model:value="form.name"
            placeholder="例如：天气查询服务"
            :maxlength="100"
          />
        </a-form-item>

        <a-form-item label="服务描述" name="description">
          <a-textarea
            v-model:value="form.description"
            placeholder="简要描述这个 MCP 服务提供的能力"
            :rows="2"
            :maxlength="500"
            show-count
          />
        </a-form-item>

        <a-form-item label="传输类型" name="transportType">
          <a-radio-group v-model:value="form.transportType" :disabled="editing" @change="handleTransportChange">
            <a-radio value="STREAMABLE_HTTP">Streamable HTTP</a-radio>
            <a-radio value="STDIO">STDIO</a-radio>
          </a-radio-group>
          <div class="field-tip">请选择与 MCP 服务端实际启动方式一致的传输类型。</div>
        </a-form-item>

        <template v-if="form.transportType === 'STREAMABLE_HTTP'">
          <a-form-item label="服务地址" name="serverUrl">
            <a-input
              v-model:value="form.serverUrl"
              placeholder="例如：http://localhost:9000/mcp"
            />
          </a-form-item>

          <a-form-item label="HTTP 请求头配置" name="headersJson">
            <a-textarea
              v-model:value="form.headersJson"
              placeholder='请输入 JSON，例如：{"Authorization":"Bearer xxx"}'
              :rows="4"
            />
            <div class="field-tip">请输入 JSON 对象。请勿将真实密钥提交到代码仓库。</div>
          </a-form-item>
        </template>

        <template v-else>
          <a-form-item label="启动命令" name="command">
            <a-input
              v-model:value="form.command"
              placeholder="例如：java、npx 或 python"
            />
          </a-form-item>

          <a-form-item label="启动参数（JSON 数组）" name="argsJson">
            <a-textarea
              v-model:value="form.argsJson"
              placeholder='例如：["-jar", "weather-mcp.jar"]'
              :rows="3"
            />
          </a-form-item>

          <a-form-item label="环境变量（JSON 对象）" name="envJson">
            <a-textarea
              v-model:value="form.envJson"
              placeholder='例如：{"API_KEY":"your-api-key"}'
              :rows="4"
            />
            <div class="field-tip">环境变量可能包含密钥，详情页面会进行脱敏展示。</div>
          </a-form-item>
        </template>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="连接超时（毫秒）" name="connectTimeoutMs">
              <a-input-number
                v-model:value="form.connectTimeoutMs"
                :min="100"
                :max="300000"
                style="width: 100%"
                placeholder="默认 10000"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="请求超时（毫秒）" name="requestTimeoutMs">
              <a-input-number
                v-model:value="form.requestTimeoutMs"
                :min="100"
                :max="600000"
                style="width: 100%"
                placeholder="默认 30000"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="form.remark"
            placeholder="可填写部署环境、负责人等补充信息"
            :rows="2"
            :maxlength="500"
            show-count
          />
        </a-form-item>

        <a-form-item v-if="!editing" label="启用状态" name="enabled">
          <a-switch v-model:checked="form.enabled" checked-children="启用" un-checked-children="停用" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- MCP 服务详情 -->
    <a-modal
      v-model:open="detailVisible"
      title="MCP 服务详情"
      :footer="null"
      width="760px"
    >
      <a-descriptions bordered :column="1">
        <a-descriptions-item label="ID">{{ currentServer.id ?? '-' }}</a-descriptions-item>
        <a-descriptions-item label="服务名称">{{ currentServer.name || '-' }}</a-descriptions-item>
        <a-descriptions-item label="服务描述">{{ currentServer.description || '-' }}</a-descriptions-item>
        <a-descriptions-item label="传输类型">
          <a-tag :color="currentServer.transportType === 'STDIO' ? 'purple' : 'blue'">
            {{ transportLabel(currentServer.transportType) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="服务地址" v-if="currentServer.transportType === 'STREAMABLE_HTTP'">
          {{ currentServer.serverUrl || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="启动命令" v-if="currentServer.transportType === 'STDIO'">
          {{ currentServer.command || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="启动参数" v-if="currentServer.transportType === 'STDIO'">
          <pre class="config-content">{{ prettyJson(currentServer.argsJson) }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="环境变量" v-if="currentServer.transportType === 'STDIO'">
          <pre class="config-content">{{ maskConfig(currentServer.envJson) }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="HTTP 请求头" v-if="currentServer.transportType === 'STREAMABLE_HTTP'">
          <pre class="config-content">{{ maskConfig(currentServer.headersJson) }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="连接超时">{{ currentServer.connectTimeoutMs ?? '-' }} ms</a-descriptions-item>
        <a-descriptions-item label="请求超时">{{ currentServer.requestTimeoutMs ?? '-' }} ms</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="currentServer.enabled ? 'success' : 'default'">
            {{ currentServer.enabled ? '启用' : '停用' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="备注">{{ currentServer.remark || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ currentServer.createTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ currentServer.updateTime || '-' }}</a-descriptions-item>
      </a-descriptions>
      <div class="detail-footer">
        <a-button
          size="small"
          :disabled="!currentServer.id"
          @click="copyServerId"
        >复制服务 ID</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import {
  getMcpServerList,
  insertMcpServer,
  updateMcpServer,
  deleteMcpServer,
  updateMcpServerStatus
} from '../api/mcp'

const loading = ref(false)
const submitting = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const editing = ref(false)
const formRef = ref()

const keyword = ref('')
const transportType = ref(undefined)
const enabled = ref(undefined)
const servers = ref([])
const currentServer = ref({})

const transportOptions = [
  { label: 'Streamable HTTP', value: 'STREAMABLE_HTTP' },
  { label: 'STDIO', value: 'STDIO' }
]
const statusOptions = [
  { label: '启用', value: true },
  { label: '停用', value: false }
]

const form = reactive(createEmptyForm())

function createEmptyForm() {
  return {
    id: null,
    name: '',
    description: '',
    transportType: 'STREAMABLE_HTTP',
    serverUrl: '',
    command: '',
    argsJson: '[]',
    envJson: '{}',
    headersJson: '{}',
    enabled: true,
    connectTimeoutMs: 10000,
    requestTimeoutMs: 30000,
    remark: ''
  }
}

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: total => `共 ${total} 条`
})

const columns = [
  { title: '服务信息', key: 'name', dataIndex: 'name', width: 190 },
  { title: '描述', key: 'description', dataIndex: 'description', ellipsis: true },
  { title: '传输类型', key: 'transportType', dataIndex: 'transportType', width: 150 },
  { title: '连接配置', key: 'endpoint', width: 240 },
  { title: '状态', key: 'enabled', width: 100, align: 'center' },
  { title: '操作', key: 'actions', width: 190, fixed: 'right' }
]

const rules = {
  name: [{ required: true, message: '请输入 MCP 服务名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入 MCP 服务描述', trigger: 'blur' }],
  transportType: [{ required: true, message: '请选择传输类型', trigger: 'change' }],
  serverUrl: [{
    validator: async (_rule, value) => {
      if (form.transportType === 'STREAMABLE_HTTP' && !value?.trim()) {
        throw new Error('请输入服务地址')
      }
    },
    trigger: 'blur'
  }],
  command: [{
    validator: async (_rule, value) => {
      if (form.transportType === 'STDIO' && !value?.trim()) {
        throw new Error('请输入启动命令')
      }
    },
    trigger: 'blur'
  }],
  argsJson: [{ validator: validateJsonArray, trigger: 'blur' }],
  envJson: [{ validator: validateJsonObject, trigger: 'blur' }],
  headersJson: [{ validator: validateJsonObject, trigger: 'blur' }]
}

function validateJsonObject(_rule, value) {
  if (!value?.trim()) return Promise.resolve()
  try {
    const parsed = JSON.parse(value)
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
      return Promise.reject(new Error('请输入合法的 JSON 对象'))
    }
    return Promise.resolve()
  } catch {
    return Promise.reject(new Error('JSON 格式不正确'))
  }
}

function validateJsonArray(_rule, value) {
  if (!value?.trim()) return Promise.resolve()
  try {
    if (!Array.isArray(JSON.parse(value))) {
      return Promise.reject(new Error('请输入 JSON 数组'))
    }
    return Promise.resolve()
  } catch {
    return Promise.reject(new Error('JSON 格式不正确'))
  }
}

function transportLabel(value) {
  if (value === 'STDIO') return 'STDIO'
  if (value === 'STREAMABLE_HTTP') return 'Streamable HTTP'
  return value || '-'
}

function endpointText(record) {
  if (record.transportType === 'STDIO') {
    return [record.command, parseArgsForDisplay(record.argsJson)].filter(Boolean).join(' ')
  }
  return record.serverUrl || '-'
}

function parseArgsForDisplay(value) {
  try {
    const args = typeof value === 'string' ? JSON.parse(value) : value
    return Array.isArray(args) ? args.join(' ') : ''
  } catch {
    return ''
  }
}

function prettyJson(value) {
  if (!value) return '-'
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    return JSON.stringify(parsed, null, 2)
  } catch {
    return String(value)
  }
}

function maskConfig(value) {
  if (!value) return '-'
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    const sensitivePattern = /token|secret|password|authorization|api.?key|credential/i
    const masked = Object.fromEntries(
      Object.entries(parsed).map(([key, val]) => [
        key,
        sensitivePattern.test(key) ? '******' : val
      ])
    )
    return JSON.stringify(masked, null, 2)
  } catch {
    return '配置内容无法解析'
  }
}

async function loadServers() {
  loading.value = true
  try {
    const res = await getMcpServerList({
      name: keyword.value.trim(),
      transportType: transportType.value,
      enabled: enabled.value,
      current: pagination.current,
      size: pagination.pageSize
    })
    const data = res?.data ?? res
    if (Array.isArray(data)) {
      servers.value = data
      pagination.total = data.length
    } else {
      servers.value = data?.records ?? data?.list ?? data?.rows ?? []
      pagination.total = data?.total ?? data?.totalCount ?? servers.value.length
    }
  } catch (error) {
    console.error(error)
    message.error('获取 MCP 服务列表失败')
  } finally {
    loading.value = false
  }
}

function search() {
  pagination.current = 1
  loadServers()
}

function resetSearch() {
  keyword.value = ''
  transportType.value = undefined
  enabled.value = undefined
  pagination.current = 1
  loadServers()
}

function handleTableChange(pager) {
  pagination.current = pager.current
  pagination.pageSize = pager.pageSize
  loadServers()
}

function clearForm() {
  Object.assign(form, createEmptyForm())
}

function resetForm() {
  formRef.value?.clearValidate()
  clearForm()
}

function openCreate() {
  editing.value = false
  clearForm()
  formVisible.value = true
}

function openEdit(record) {
  editing.value = true
  Object.assign(form, createEmptyForm(), {
    ...record,
    argsJson: normalizeJsonText(record.argsJson, '[]'),
    envJson: normalizeJsonText(record.envJson, '{}'),
    headersJson: normalizeJsonText(record.headersJson, '{}'),
    enabled: record.enabled === true
  })
  formVisible.value = true
}

function normalizeJsonText(value, fallback) {
  if (value == null || value === '') return fallback
  if (typeof value === 'string') return value
  return JSON.stringify(value, null, 2)
}

function handleTransportChange() {
  formRef.value?.clearValidate(['serverUrl', 'command', 'argsJson', 'envJson', 'headersJson'])
}

function openDetail(record) {
  currentServer.value = { ...record }
  detailVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload = {
      name: form.name.trim(),
      description: form.description.trim(),
      transportType: form.transportType,
      serverUrl: form.transportType === 'STREAMABLE_HTTP' ? form.serverUrl.trim() : null,
      command: form.transportType === 'STDIO' ? form.command.trim() : null,
      argsJson: form.transportType === 'STDIO' ? normalizeJsonText(form.argsJson, '[]') : null,
      envJson: form.transportType === 'STDIO' ? normalizeJsonText(form.envJson, '{}') : null,
      headersJson: form.transportType === 'STREAMABLE_HTTP' ? normalizeJsonText(form.headersJson, '{}') : null,
      enabled: form.enabled,
      connectTimeoutMs: form.connectTimeoutMs,
      requestTimeoutMs: form.requestTimeoutMs,
      remark: form.remark?.trim() || ''
    }

    if (editing.value) {
      await updateMcpServer({ id: form.id, ...payload })
      message.success('MCP 服务更新成功')
    } else {
      await insertMcpServer(payload)
      message.success('MCP 服务创建成功')
    }

    formVisible.value = false
    resetForm()
    await loadServers()
  } catch (error) {
    console.error(error)
    message.error(editing.value ? 'MCP 服务更新失败' : 'MCP 服务创建失败')
  } finally {
    submitting.value = false
  }
}

async function handleStatusChange(record, value) {
  const previousStatus = record.enabled
  record.statusLoading = true
  try {
    const result = await updateMcpServerStatus(record.id, value)
    if (result === false || result?.data === false) {
      record.enabled = previousStatus
      message.error('修改状态失败，请检查是否存在关联或运行时限制')
      return
    }
    record.enabled = value
    message.success(value ? 'MCP 服务已启用' : 'MCP 服务已停用')
    await loadServers()
  } catch (error) {
    record.enabled = previousStatus
    console.error(error)
    message.error('修改 MCP 服务状态失败')
  } finally {
    record.statusLoading = false
  }
}

async function handleDelete(record) {
  try {
    const result = await deleteMcpServer(record.id)
    if (result === false || result?.data === false) {
      message.error('删除失败，请检查 Agent 关联关系')
      return
    }
    message.success('MCP 服务删除成功')
    if (servers.value.length === 1 && pagination.current > 1) {
      pagination.current--
    }
    await loadServers()
  } catch (error) {
    console.error(error)
    message.error('MCP 服务删除失败，请检查 Agent 关联关系')
  }
}

async function copyServerId() {
  try {
    await navigator.clipboard.writeText(String(currentServer.value.id))
    message.success('服务 ID 已复制')
  } catch {
    message.error('复制失败，请检查浏览器剪贴板权限')
  }
}

onMounted(loadServers)
</script>

<style scoped>
.mcp-page {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
}

.page-desc {
  margin: 0;
  color: #888;
}

.search-form {
  margin-bottom: 24px;
  row-gap: 12px;
}

.reset-btn {
  margin-left: 8px;
}

.mcp-table {
  margin-top: 12px;
}

.server-name {
  font-weight: 500;
}

.server-id {
  margin-top: 4px;
  color: #999;
  font-size: 12px;
}

.description-text,
.endpoint-text {
  display: inline-block;
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.field-tip {
  margin-top: 6px;
  color: #888;
  font-size: 12px;
}

.config-content {
  max-height: 240px;
  overflow: auto;
  margin: 0;
  padding: 12px;
  border-radius: 6px;
  background: #f5f5f5;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-size: 12px;
  line-height: 1.7;
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
