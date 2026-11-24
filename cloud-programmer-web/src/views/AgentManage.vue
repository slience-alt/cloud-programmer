
<template>
  <div class="page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h2>Agent 管理</h2>
        <p>管理智能 Agent 的基本信息、模型配置和运行状态</p>
      </div>
    </div>

    <!-- 搜索区域 -->
    <a-card class="search-card">
      <a-form layout="inline" :model="queryParams">
        <a-form-item label="Agent 名称">
          <a-input
            v-model:value="queryParams.name"
            placeholder="请输入 Agent 名称"
            allow-clear
            @pressEnter="handleSearch"
          />
        </a-form-item>

        <a-form-item label="模型">
          <a-input
            v-model:value="queryParams.modelName"
            placeholder="请输入模型名称"
            allow-clear
            @pressEnter="handleSearch"
          />
        </a-form-item>

        <a-form-item>
          <a-button type="primary" @click="handleSearch">
            搜索
          </a-button>
          <a-button style="margin-left: 8px" @click="handleReset">
            重置
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- Agent 列表 -->
    <a-card class="table-card">
      <template #title>
        <div class="toolbar">
          <span>Agent 列表</span>
          <div>
            <a-button type="primary" @click="handleAdd">
              新增 Agent
            </a-button>
            <a-button
              danger
              :disabled="selectedRows.length === 0"
              style="margin-left: 8px"
              @click="handleBatchDelete"
            >
              批量删除
            </a-button>
          </div>
        </div>
      </template>

      <a-table
        :columns="columns"
        :data-source="agents"
        :loading="loading"
        :row-selection="rowSelection"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- Agent 描述 -->
          <template v-if="column.key === 'description'">
            <a-tooltip :title="record.description">
              <span class="description-text">
                {{ record.description || '-' }}
              </span>
            </a-tooltip>
          </template>

          <!-- 模型 -->
          <template v-if="column.key === 'modelName'">
            <a-tag color="blue">
              {{ record.modelName || '未配置' }}
            </a-tag>
          </template>

          <!-- 运行状态：约定 1 为在线，0 为离线 -->
          <!-- 是否启用 -->
         <template v-if="column.key === 'status'">
            <a-switch :checked="Number(record.status) === 1" :loading="record.statusLoading" checked-children="启用"
              un-checked-children="停用" @change="value => handleStatusChange(record, value)" />
          </template>

          <!-- 操作 -->
          <template v-if="column.key === 'action'">
            <a-space>
             <a-button type="link" @click="handleViewSkills(record)">
                Skills
              </a-button>
              <a-button type="link" @click="handleViewMcps(record)">
                MCP
              </a-button>
              <a-button type="link" @click="handleEdit(record)">
                编辑
              </a-button>
              <a-button
                type="link"
                danger
                @click="handleDelete(record)"
              >
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

  <!-- 查看 Skills 弹窗 -->
    <a-modal v-model:open="skillsModalVisible" title="关联的 Skills" :footer="null" width="760px" destroy-on-close>
      <!-- Agent 基本信息 -->
      <div class="agent-info">
        <div class="agent-info-name">
          {{ currentAgent?.name || '-' }}
        </div>

        <div class="agent-info-description">
          {{ currentAgent?.description || '暂无描述' }}
        </div>
      </div>

      <a-divider />

      <!-- 没有关联 Skill -->
      <a-empty v-if="currentSkills.length === 0" description="该 Agent 暂无关联的 Skills" />

      <!-- Skill 列表 -->
      <a-collapse v-else class="skill-collapse" :bordered="false">
        <a-collapse-panel v-for="skill in currentSkills" :key="skill.id" :header="skill.name">
          <!-- Skill 基本信息 -->
          <div class="skill-detail">
        
            <div class="skill-field">
              <span class="field-label">Skill 名称</span>
              <span>{{ skill.name }}</span>
            </div>

            <div class="skill-field">
              <span class="field-label">Skill 描述</span>
              <span>{{ skill.description || '暂无描述' }}</span>
            </div>

            <!-- 提示词 -->
            <div class="instruction-section">
              <div class="instruction-header">
                <span class="field-label">Skill 提示词（Instruction）</span>

                <a-button type="link" size="small" @click.stop="copyInstruction(skill.instruction)">
                  复制提示词
                </a-button>
              </div>

              <pre class="skill-instruction">{{ skill.instruction || '暂无提示词' }}</pre>
            </div>
          </div>
        </a-collapse-panel>
      </a-collapse>

      <!-- 底部 -->
      <div class="modal-footer">
        <a-button @click="skillsModalVisible = false">
          关闭
        </a-button>
      </div>
    </a-modal>

  <!-- 查看 MCP 弹窗 -->
   <a-modal v-model:open="mcpsModalVisible" title="关联 MCP Server" :footer="null" width="800px">
     <div class="agent-info">
        <div class="agent-info-name">
          {{ currentAgent?.name || '-' }}
        </div>

        <div class="agent-info-description">
          {{ currentAgent?.description || '暂无描述' }}
        </div>
      </div>

      <a-divider />
      <a-spin :spinning="mcpsLoading">
        <a-empty v-if="!currentMcps.length" description="暂无关联 MCP" />

      <a-collapse v-else>
          <a-collapse-panel v-for="mcp in currentMcps" :key="mcp.id" :header="mcp.name || `MCP Server ${mcp.id}`">
            <!-- 普通字段：两列展示 -->
            <a-descriptions bordered size="small" :column="2" class="mcp-descriptions">
              <a-descriptions-item v-for="item in getSimpleMcpEntries(mcp)" :key="item.key"
                :label="formatMcpFieldName(item.key)">
                {{ formatMcpFieldValue(item.value, item.key) }}
              </a-descriptions-item>
            </a-descriptions>

            <!-- JSON / 复杂配置：单独整行展示 -->
            <a-descriptions v-if="getComplexMcpEntries(mcp).length" bordered size="small" :column="1"
              class="mcp-complex-descriptions">
              <a-descriptions-item v-for="item in getComplexMcpEntries(mcp)" :key="item.key"
                :label="formatMcpFieldName(item.key)">
                <div class="mcp-config-container">
                  <div class="mcp-config-toolbar">
                    <span>配置内容</span>
                    <a-button type="link" size="small" @click.stop="copyMcpConfig(item.value)">
                      复制配置
                    </a-button>
                  </div>

                  <pre class="mcp-config-json">{{ formatMcpJson(item.value) }}</pre>
                </div>
              </a-descriptions-item>
            </a-descriptions>
          </a-collapse-panel>
        </a-collapse>
      </a-spin>
    </a-modal>
    <!-- 新增 / 编辑 Agent -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId !== null ? '编辑 Agent' : '新增 Agent'"
      :confirm-loading="submitLoading"
      width="720px"
      @ok="handleSubmit"
      @cancel="handleCancel"
    >
      <a-form
        ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
      >
        <a-form-item label="Agent 名称" name="name">
          <a-input
            v-model:value="form.name"
            placeholder="例如：Java 代码助手"
          />
        </a-form-item>

        <a-form-item label="Agent 描述" name="description">
          <a-textarea
            v-model:value="form.description"
            :rows="2"
            placeholder="描述 Agent 的用途和能力"
          />
        </a-form-item>

        <a-form-item label="模型名称" name="modelName">
          <a-input
            v-model:value="form.modelName"
            placeholder="请输入模型名称，例如 gpt-4o"
          />
        </a-form-item>

        <a-form-item label="系统提示词" name="systemPrompt">
          <a-textarea
            v-model:value="form.systemPrompt"
            :rows="8"
            show-count
            placeholder="请输入 Agent 的系统提示词，定义它的角色、行为和回答规则"
          />
        </a-form-item>
        <a-form-item label="关联 Skills" name="skillIds">
          <a-select v-model:value="form.skillIds" mode="multiple" :options="skillOptions" placeholder="请选择关联的 Skills"
            :loading="skillsLoading" allow-clear show-search :filter-option="filterSkillOption" style="width: 100%"
            :max-tag-count="3" />

          <div class="form-tip">
            可以关联多个 Skills，保存后将更新该 Agent 的关联关系
          </div>
        </a-form-item>

      <a-form-item label="关联 MCP" name="mcpIds">
          <a-select v-model:value="form.mcpIds" mode="multiple" :options="mcpOptions" placeholder="请选择关联的 MCP Server"
            :loading="mcpsLoading" allow-clear show-search :filter-option="filterMcpOption" style="width: 100%"
            :max-tag-count="3" />

          <div class="form-tip">
            可以关联多个 MCP Server，保存后将更新该 Agent 的关联关系
          </div>
        </a-form-item>

        <a-form-item v-if="editingId === null" label="是否启用" name="status">
          <a-radio-group v-model:value="form.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">停用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'

import {
  getAgentList,
  insertAgent,
  updateAgentStatus,
  updateAgent,
  deleteAgent,
  batchDeleteAgent
} from '../api/agent'

import {
  getSkillList,
} from '../api/skill'

import {
  getMcpServerList
} from '../api/mcp'

// ==================== 数据 ====================
const skillsModalVisible = ref(false)
const skillsLoading = ref(false)
const skills = ref([])
const agents = ref([])
const loading = ref(false)
const selectedRows = ref([])
const modalVisible = ref(false)
const submitLoading = ref(false)
const editingId = ref(null)
const formRef = ref()
const allSkills = ref([])
const currentSkills = ref([])
const currentAgent = ref(null)
const mcpsModalVisible = ref(false)
const mcpsLoading = ref(false)
const allMcps = ref([])
const currentMcps = ref([])

const mcpFieldLabels = {
  id: 'ID',
  name: '名称',
  description: '描述',
  status: '状态',
  url: '服务地址',
  endpoint: '服务地址',
  transportType: '传输类型',
  transport: '传输方式',
  command: '启动命令',
  args: '启动参数',
  env: '环境变量',
  headers: '请求头',
  config: '配置参数',
  timeout: '超时时间',
  createdAt: '创建时间',
  updatedAt: '更新时间'
}

function formatMcpFieldName(key) {
  return mcpFieldLabels[key] || key
}

function isComplexValue(value) {
  return value !== null && typeof value === 'object'
}

function formatMcpFieldValue(value, key) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }

  // 对敏感配置脱敏，避免详情页直接暴露密钥
  if (/token|secret|password|api.?key|access.?key/i.test(key)) {
    return '******'
  }

  if (typeof value === 'boolean') {
    return value ? '是' : '否'
  }

  if (typeof value === 'object') {
    return JSON.stringify(value, null, 2)
  }

  return String(value)
}

// ==================== 查询参数 ====================

const queryParams = reactive({
  name: '',
  modelName: ''
})

// 下拉框选项
const skillOptions = computed(() =>
  allSkills.value.map(skill => ({
    label: skill.name,
    value: skill.id
  }))
)

// 支持按 Skill 名称搜索
const filterSkillOption = (input, option) =>
  option.label.toLowerCase().includes(input.toLowerCase())

const mcpOptions = computed(() =>
  allMcps.value.map(mcp => ({
    label: mcp.name,
    value: mcp.id
  }))
)

// 支持按 MCP 名称搜索
const filterMcpOption = (input, option) =>
  option.label.toLowerCase().includes(input.toLowerCase())
// ==================== 分页 ====================

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: total => `共 ${total} 条`
})

// ==================== 表单 ====================

const createEmptyForm = () => ({
  name: '',
  description: '',
  modelName: '',
  systemPrompt: '',
  status: 1,
  skillIds: [],
  mcpIds: []
})

const form = reactive(createEmptyForm())

const rules = {
  name: [
    {
      required: true,
      whitespace: true,
      message: '请输入 Agent 名称'
    }
  ],
  modelName: [
    {
      required: true,
      whitespace: true,
      message: '请输入模型名称'
    }
  ],
  systemPrompt: [
    {
      required: true,
      whitespace: true,
      message: '请输入系统提示词'
    }
  ]
}

const skillColumns = [
  {
    title: 'Skill 名称',
    dataIndex: 'name',
    key: 'name'
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description'
  },
]

// ==================== 表格列 ====================

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
    width: 80
  },
  {
    title: 'Agent 名称',
    dataIndex: 'name',
    key: 'name',
    width: 180
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
    ellipsis: true
  },
  {
    title: '模型',
    dataIndex: 'modelName',
    key: 'modelName',
    width: 150
  },
  {
    title: '是否启用',
    dataIndex: 'status',
    key: 'status',
    width: 110
  },
  {
    title: '操作',
    key: 'action',
    width: 280,
    fixed: 'right'
  }
]

const copyInstruction = async (instruction) => {
  if (!instruction) {
    message.warning('当前 Skill 暂无提示词')
    return
  }

  try {
    await navigator.clipboard.writeText(instruction)
    message.success('提示词已复制')
  } catch (error) {
    console.error('复制失败：', error)
    message.error('复制失败，请手动复制')
  }
}

// ==================== 多选 ====================

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRows.value.map(item => item.id),
  onChange: (selectedRowKeys, rows) => {
    selectedRows.value = rows
  }
}))

const handleViewSkills = (record) => {
  currentAgent.value = record
  const skillIds = new Set(
    (record.skillIds || []).map(id => Number(id))
  )

  currentSkills.value = allSkills.value.filter(skill =>
    skillIds.has(Number(skill.id))
  )

  skillsModalVisible.value = true
}

// ==================== 查询列表 ====================

async function loadAgents() {
  loading.value = true

  try {
    const result = await getAgentList({
      name: queryParams.name,
      modelName: queryParams.modelName,
    })

    // 兼容分页对象和数组两种返回结构。
    // 请根据你实际的后端分页 DTO 调整这里。
    const data = result

    if (Array.isArray(data)) {
      agents.value = data
      pagination.total = data.length
    } else {
      agents.value = data?.records ?? data?.list ?? []
      pagination.total = data?.total ?? agents.value.length
    }
  } catch (error) {
    console.error(error)
    message.error('查询 Agent 失败')
  } finally {
    loading.value = false
  }
}

// ==================== 搜索与重置 ====================

function handleSearch() {
  pagination.current = 1
  loadAgents()
}

function handleReset() {
  queryParams.name = ''
  queryParams.modelName = ''
  pagination.current = 1
  loadAgents()
}

function handleTableChange(pageInfo) {
  pagination.current = pageInfo.current
  pagination.pageSize = pageInfo.pageSize
  loadAgents()
}

// ==================== 新增 ====================

function handleAdd() {
  editingId.value = null
  Object.assign(form, createEmptyForm(), {
    skillIds: [],
    mcpIds: []
  })
  formRef.value?.clearValidate()
  modalVisible.value = true
}

// ==================== 编辑 ====================

function handleEdit(record) {
  editingId.value = record.id

  Object.assign(form, {
    name: record.name ?? '',
    description: record.description ?? '',
    modelName: record.modelName ?? '',
    systemPrompt: record.systemPrompt ?? '',
    status: Number(record.status ?? 1),
    // 编辑时回显已关联的 Skills
    skillIds: Array.isArray(record.skillIds)
      ? record.skillIds.map(Number)
      : [],
    mcpIds: Array.isArray(record.mcpIds)
      ? record.mcpIds.map(Number)
      : []
  })

  formRef.value?.clearValidate()
  modalVisible.value = true
}

function handleViewMcps(record) {
  currentAgent.value = record
  const mcpIds = new Set(
    (record.mcpIds || []).map(Number)
  )
  currentMcps.value = allMcps.value.filter(mcp =>
    mcpIds.has(Number(mcp.id))
  )
  mcpsModalVisible.value = true
}

async function handleStatusChange(record, value) {
  const previousStatus = record.status
  record.statusLoading = true

  try {
    const status = value ? 1 : 0

    await updateAgentStatus(record.id, status)

    record.status = status
    message.success(value ? 'Agent 已启用' : 'Agent 已停用')
  } catch (error) {
    record.status = previousStatus
    console.error(error)
    message.error('修改 Agent 启用状态失败')
  } finally {
    record.statusLoading = false
  }
}

// ==================== 提交 ====================

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true


  try {
    // 明确指定字段，不把表单对象中的其他属性提交给后端。
    const payload = {
      name: form.name.trim(),
      description: form.description,
      modelName: form.modelName.trim(),
      systemPrompt: form.systemPrompt,
      status: form.status,
      skillIds: [...form.skillIds],
      mcpIds: [...(form.mcpIds)]
    }


    if (editingId.value !== null) {
      await updateAgent(editingId.value, payload)
      message.success('Agent 修改成功')
    } else {
      // 新增时不传 id，由后端生成。
      await insertAgent(payload)
      message.success('Agent 创建成功')
    }

    modalVisible.value = false
    selectedRows.value = []
    await loadAgents()
  } catch (error) {
    console.error(error)
    message.error('保存 Agent 失败')
  } finally {
    submitLoading.value = false
  }
}

function handleCancel() {
  formRef.value?.clearValidate()
}

// ==================== 删除 ====================

function handleDelete(record) {
  Modal.confirm({
    title: '确认删除？',
    content: `确定要删除 Agent「${record.name}」吗？`,
    okType: 'danger',
    async onOk() {
      await deleteAgent(record.id)
      message.success('删除成功')
      selectedRows.value = []
      await loadAgents()
    }
  })
}

// ==================== 批量删除 ====================

function handleBatchDelete() {
  const ids = selectedRows.value.map(item => item.id)

  if (ids.length === 0) return

  Modal.confirm({
    title: '确认批量删除？',
    content: `确定要删除选中的 ${ids.length} 个 Agent 吗？`,
    okType: 'danger',
    async onOk() {
      await batchDeleteAgent(ids)
      message.success('批量删除成功')
      selectedRows.value = []
      await loadAgents()
    }
  })
}

const getAgentSkills = (record) => {
  const skillIds = new Set(
    (record.skillIds || []).map(String)
  )

  return allSkills.value.filter(skill =>
    skillIds.has(String(skill.id))
  )
}

// 判断是否属于需要单独占行的复杂配置字段。
// 兼容 envJson、headersJson 这类 JSON 字符串。
function isMcpComplexField(key, value) {
  return (
    (value !== null && typeof value === 'object') ||
    /json|args|env|headers|config|options|parameters/i.test(key)
  )
}

// 普通字段
function getSimpleMcpEntries(mcp) {
  return Object.entries(mcp)
    .filter(([key, value]) => !isMcpComplexField(key, value))
    .map(([key, value]) => ({ key, value }))
}

// 复杂字段
function getComplexMcpEntries(mcp) {
  return Object.entries(mcp)
    .filter(([key, value]) => isMcpComplexField(key, value))
    .map(([key, value]) => ({ key, value }))
}

// 格式化 JSON 字符串；无法解析时保留原始内容
function formatMcpJson(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }

  if (typeof value === 'string') {
    try {
      return JSON.stringify(JSON.parse(value), null, 2)
    } catch {
      return value
    }
  }

  return JSON.stringify(value, null, 2)
}

// 复制完整配置
async function copyMcpConfig(value) {
  try {
    await navigator.clipboard.writeText(formatMcpJson(value))
    message.success('配置已复制')
  } catch (error) {
    console.error('复制 MCP 配置失败：', error)
    message.error('复制失败，请检查浏览器剪贴板权限')
  }
}

const fetchData = async () => {
  skillsLoading.value = true
  mcpsLoading.value = true

  try {
    const [skillRes, mcpRes] = await Promise.all([
      getSkillList(),
      getMcpServerList()
    ])

    allSkills.value = Array.isArray(skillRes)
      ? skillRes
      : skillRes?.records ?? skillRes?.list ?? []

    allMcps.value = Array.isArray(mcpRes)
      ? mcpRes
      : mcpRes?.records ?? mcpRes?.list ?? []
  } catch (error) {
    console.error('加载 Skill 或 MCP 数据失败', error)
    message.error('加载关联选项失败')
  } finally {
    skillsLoading.value = false
    mcpsLoading.value = false
  }
}


// ==================== 初始化 ====================

onMounted(() => {
  loadAgents()
  fetchData()
})
</script>

<style scoped>
.page {
  min-height: 100%;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
}

.page-header p {
  margin-top: 6px;
  margin-bottom: 0;
  color: #999;
}

.search-card {
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.description-text {
  display: inline-block;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
.agent-info {
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.agent-info-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 6px;
}

.agent-info-description {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.skill-collapse {
  background: transparent;
}

.skill-detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.skill-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.field-label {
  color: #6b7280;
  font-size: 13px;
  font-weight: 500;
}

.instruction-section {
  margin-top: 4px;
}

.instruction-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.skill-instruction {
  margin: 0;
  padding: 14px;
  max-height: 320px;
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  word-break: break-word;
  line-height: 1.8;
  font-size: 13px;
  color: #374151;
  background: #f5f7fa;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-family: inherit;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
.form-tip {
  margin-top: 6px;
  color: #999;
  font-size: 12px;
  line-height: 1.5;
}
/* 详情表格固定布局，防止长内容挤压其他列 */
.mcp-descriptions,
.mcp-complex-descriptions {
  width: 100%;
  min-width: 0;
}

.mcp-descriptions {
  margin-bottom: 12px;
}

.mcp-descriptions :deep(.ant-descriptions-view),
.mcp-complex-descriptions :deep(.ant-descriptions-view) {
  width: 100%;
  overflow: hidden;
}

.mcp-descriptions :deep(table),
.mcp-complex-descriptions :deep(table) {
  width: 100%;
  table-layout: fixed;
}

.mcp-descriptions :deep(.ant-descriptions-item-label),
.mcp-descriptions :deep(.ant-descriptions-item-content),
.mcp-complex-descriptions :deep(.ant-descriptions-item-label),
.mcp-complex-descriptions :deep(.ant-descriptions-item-content) {
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-word;
}

/* JSON 配置容器 */
.mcp-config-container {
  width: 100%;
  max-width: 100%;
  max-height: 280px;
  overflow: auto;
  box-sizing: border-box;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  background: #fafafa;
}

.mcp-config-toolbar {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 10px;
  background: #f5f5f5;
  border-bottom: 1px solid #e8e8e8;
}

.mcp-config-json {
  display: block;
  width: 100%;
  max-width: 100%;
  margin: 0;
  padding: 10px;
  box-sizing: border-box;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.6;
}

</style>
