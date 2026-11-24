
<template>
  <div class="skill-page">
    <a-card :bordered="false">
      <div class="page-header">
        <div>
          <h2>Skill 管理</h2>
          <p class="page-desc">管理 Agent 可使用的技能与执行指令</p>
        </div>
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>
          新建 Skill
        </a-button>
      </div>

      <a-form layout="inline" class="search-form" @finish="loadSkills">
        <a-form-item label="Skill 名称">
          <a-input
            v-model:value="keyword"
            placeholder="请输入 Skill 名称"
            allow-clear
            @pressEnter="loadSkills"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="loadSkills">查询</a-button>
          <a-button class="reset-btn" @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="skills"
        :loading="loading"
        :pagination="pagination"
        :row-key="record => record.id"
        @change="handleTableChange"
        class="skill-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'description'">
            <a-tooltip :title="record.description">
              <span class="description-text">
                {{ record.description || '-' }}
              </span>
            </a-tooltip>
          </template>

          <template v-else-if="column.key === 'instruction'">
            <a-tag color="blue">
              {{ record.instruction ? '已配置' : '未配置' }}
            </a-tag>
          </template>
         <template v-if="column.key === 'status'">
            <a-switch :checked="Number(record.status) === 1" :loading="record.statusLoading" checked-children="启用"
              un-checked-children="停用" @change="value => handleStatusChange(record, value)" />
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="openDetail(record)">
                详情
              </a-button>
              <a-button type="link" size="small" @click="openEdit(record)">
                编辑
              </a-button>
              <a-popconfirm
                title="确定删除这个 Skill 吗？"
                description="请确认没有 Agent 依赖此 Skill，或后端已处理关联关系。"
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

    <!-- 新建 / 编辑 -->
    <a-modal
      v-model:open="formVisible"
      :title="editing ? '编辑 Skill' : '新建 Skill'"
      :confirm-loading="submitting"
      ok-text="保存"
      cancel-text="取消"
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
        <a-form-item label="Skill 名称" name="name">
          <a-input
            v-model:value="form.name"
            placeholder="例如：Java 代码审查"
            :maxlength="100"
          />
        </a-form-item>

        <a-form-item label="Skill 描述" name="description">
          <a-textarea
            v-model:value="form.description"
            placeholder="简要描述这个 Skill 的用途"
            :rows="3"
            :maxlength="500"
            show-count
          />
        </a-form-item>

        <a-form-item label="Skill 指令" name="instruction">
          <a-textarea
            v-model:value="form.instruction"
            placeholder="请输入技能执行指令、步骤和注意事项"
            :rows="10"
            show-count
          />
          <div class="field-tip">
            指令会在 Agent 运行时被加载，用于指导模型完成任务。
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 指令详情 -->
    <a-modal
      v-model:open="detailVisible"
      title="Skill 详情"
      :footer="null"
      width="720px"
    >
      <a-descriptions bordered :column="1">
        <a-descriptions-item label="ID">
          {{ currentSkill.id }}
        </a-descriptions-item>
        <a-descriptions-item label="名称">
          {{ currentSkill.name }}
        </a-descriptions-item>
        <a-descriptions-item label="描述">
          {{ currentSkill.description || '-' }}
        </a-descriptions-item>
      </a-descriptions>

      <div class="instruction-header">
        <strong>Skill 指令</strong>
        <a-button
          size="small"
          :disabled="!currentSkill.instruction"
          @click="copyInstruction"
        >
          复制指令
        </a-button>
      </div>
      <pre class="instruction-content">{{ currentSkill.instruction || '暂无指令' }}</pre>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import {
    getSkillList,
    insertSkill,
    updateSkill,
    deleteSkill,
    updataStatus
} from '../api/skill'

const loading = ref(false)
const submitting = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const editing = ref(false)
const formRef = ref()

const keyword = ref('')
const skills = ref([])
const currentSkill = ref({})

const form = reactive({
  id: null,
  name: '',
  description: '',
  instruction: ''
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: total => `共 ${total} 条`
})

const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    { title: 'Skill 名称', dataIndex: 'name', key: 'name', width: 180 },
    {
        title: '描述',
        dataIndex: 'description',
        key: 'description',
        ellipsis: true
    },
    {
        title: '指令配置',
        dataIndex: 'instruction',
        key: 'instruction',
        width: 110
    },
    {
        title: '状态',
        key: 'status',
        width: 100,
        align: 'center'
    },
    { title: '操作', key: 'actions', width: 200, fixed: 'right' }
]

const rules = {
  name: [
    { required: true, message: '请输入 Skill 名称', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入 Skill 描述', trigger: 'blur' }
  ],
  instruction: [
    { required: true, message: '请输入 Skill 指令', trigger: 'blur' }
  ]
}

// 兼容返回数组、分页对象两种常见格式。
// 如果后端使用其他字段名，请按实际响应结构调整。
async function loadSkills() {
  loading.value = true
  try {
    const res = await getSkillList({
      name: keyword.value.trim(),
    })

    const data = res?.data ?? res
    if (Array.isArray(data)) {
      skills.value = data
      pagination.total = data.length
    } else {
      skills.value = data?.records ?? data?.list ?? data?.rows ?? []
      pagination.total =
        data?.total ?? data?.totalCount ?? skills.value.length
    }
  } catch (error) {
    message.error('获取 Skill 列表失败')
  } finally {
    loading.value = false
  }
}

async function handleStatusChange(record, value) {
  const previousStatus = record.status
  record.statusLoading = true

  try {
    const status = value ? 1 : 0

    let result = await updataStatus(record.id, status)
    if (result) {
        record.status = status
        message.success(value ? 'Skill 已启用' : 'Skill 已停用')
        loadSkills()
    } else {
        message.error('Skill停用失败，该Skill已被关联！')
        return
    }
   
  } catch (error) {
    record.status = previousStatus
    console.error(error)
    message.error('修改 Skill 启用状态失败')
  } finally {
    record.statusLoading = false
  }
}

function handleTableChange(pager) {
  pagination.current = pager.current
  pagination.pageSize = pager.pageSize
  loadSkills()
}

function resetSearch() {
  keyword.value = ''
  pagination.current = 1
  loadSkills()
}

function clearForm() {
  form.id = null
  form.name = ''
  form.description = ''
  form.instruction = ''
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
  Object.assign(form, {
    id: record.id,
    name: record.name ?? '',
    description: record.description ?? '',
    instruction: record.instruction ?? ''
  })
  formVisible.value = true
}

function openDetail(record) {
  currentSkill.value = { ...record }
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
      instruction: form.instruction
    }

    console.log(form.id)

    if (editing.value) {
      await updateSkill({ id: form.id, ...payload })
      message.success('Skill 更新成功')
    } else {
      await insertSkill(payload)
      message.success('Skill 创建成功')
    }

    formVisible.value = false
    resetForm()
    await loadSkills()
  } catch (error) {
    console.log(error)
    message.error(editing.value ? 'Skill 更新失败' : 'Skill 创建失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(record) {
    try {
        let result = await deleteSkill(record.id)
        if (result) {
            message.success('Skill 删除成功')
        } else {
            message.error('Skill 删除失败，请检查关联关系')
        }


        if (skills.value.length === 1 && pagination.current > 1) {
            pagination.current--
        }
        await loadSkills()
    } catch (error) {
        message.error('Skill 删除失败，请检查关联关系')
    }
}

async function copyInstruction() {
  try {
    await navigator.clipboard.writeText(currentSkill.value.instruction || '')
    message.success('指令已复制')
  } catch {
    message.error('复制失败，请检查浏览器剪贴板权限')
  }
}

onMounted(loadSkills)
</script>

<style scoped>
.skill-page {
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
}

.reset-btn {
  margin-left: 8px;
}

.skill-table {
  margin-top: 12px;
}

.description-text {
  display: inline-block;
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.field-tip {
  margin-top: 8px;
  color: #888;
  font-size: 12px;
}

.instruction-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 20px 0 12px;
}

.instruction-content {
  max-height: 420px;
  overflow: auto;
  padding: 16px;
  border-radius: 6px;
  background: #f5f5f5;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-size: 13px;
  line-height: 1.8;
}
</style>
