<template>
    <div class="page">
        <!-- 页面标题 -->
        <div class="page-header">
            <div>
                <h2>用户管理</h2>
                <p>
                    管理系统用户信息
                </p>
            </div>
        </div>
        <!-- 搜索 -->
        <a-card class="search-card">
            <a-form layout="inline" :model="queryParams">
                <a-form-item label="昵称">
                    <a-input v-model:value="queryParams.nickname" placeholder="请输入昵称" allow-clear @pressEnter="handleSearch" />
                </a-form-item>
                <a-form-item label="角色">
                    <a-select v-model:value="queryParams.role" placeholder="请选择角色" allow-clear style="width: 160px">
                        <a-select-option value="1001">
                            管理员
                        </a-select-option>
                        <a-select-option value="1002">
                            普通用户
                        </a-select-option>
                    </a-select>
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
        <!-- 操作 -->
        <a-card class="table-card">
            <template #title>
                <div class="toolbar">
                    <span>
                        用户列表
                    </span>
                    <div>
                        <a-button type="primary" @click="handleAdd">
                            添加用户
                        </a-button>
                        <a-button danger :disabled="selectedRows.length === 0" style="margin-left: 8px"
                            @click="handleBatchDelete">
                            批量删除
                        </a-button>
                    </div>
                </div>
            </template>
            <a-table :columns="columns" :data-source="users" :loading="loading" :row-selection="rowSelection"
                :pagination="pagination" row-key="id" @change="handleTableChange">
                <!-- 角色 -->
                <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'role'">
                        <a-tag v-if="record.role === 1001" color="blue">
                            管理员
                        </a-tag>
                        <a-tag v-else color="default">
                            普通用户
                        </a-tag>
                    </template>
                    <!-- 状态 -->
                    <template v-if="column.key === 'enabled'">
                        <a-switch :checked="record.isEnable" :loading="record.statusLoading" @change="value => handleStatusChange(record, value)" />
                    </template>
                </template>
            </a-table>
        </a-card>
        <!-- 新增/编辑 -->
        <a-modal v-model:open="modalVisible" :title="editingId ? '编辑用户' : '添加用户'" :confirm-loading="submitLoading"
            @ok="handleSubmit">
            <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
                <a-form-item label="昵称" name="nickname">
                    <a-input v-model:value="form.nickname" placeholder="请输入昵称" />
                </a-form-item>
                <a-form-item label="角色" name="role">
                    <a-select v-model:value="form.role">
                        <a-select-option value="1002">
                            普通用户
                        </a-select-option>
                        <a-select-option value="1001">
                            管理员
                        </a-select-option>
                    </a-select>
                </a-form-item>
                <a-form-item label="积分" name="points">
                    <a-input-number v-model:value="form.points" :min="0" style="width: 100%" />
                </a-form-item>
                <a-form-item label="是否启用">
                    <a-switch v-model:checked="form.enabled" />
                </a-form-item>
            </a-form>
        </a-modal>
    </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
    message,
    Modal
} from 'ant-design-vue'

import {
    getUserList,
    addUser,
    updateUser,
    deleteUser,
    batchDeleteUser,
    updateUserStatus
} from '../api/user'

// ==================== 数据 ====================

const users = ref([])

const loading = ref(false)

const selectedRows = ref([])

const modalVisible = ref(false)

const submitLoading = ref(false)

const editingId = ref(null)

const formRef = ref()

// ==================== 查询条件 ====================

const queryParams = reactive({
    nickname: '',
    role: '',
    page: 1,
    size: 10
})

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

const form = reactive({
    nickname: '',
    role: 'USER',
    points: 0,
    enabled: true
})

const rules = {
    nickname: [
        {
            required: true,
            message: '请输入昵称'
        }
    ],
    role: [
        {
            required: true,
            message: '请选择角色'
        }
    ]
}

// ==================== 表格 ====================

const columns = [
    {
        title: 'ID',
        dataIndex: 'id',
        key: 'id',
        width: 80
    },
    {
        title: '昵称',
        dataIndex: 'nickname',
        key: 'nickname'
    },
    {
        title: '角色',
        dataIndex: 'role',
        key: 'role'
    },
    {
        title: '积分',
        dataIndex: 'points',
        key: 'points'
    },
    {
        title: '是否启用',
        dataIndex: 'enabled',
        key: 'enabled'
    },
]

// ==================== 多选 ====================

const rowSelection = computed(() => ({
    selectedRowKeys: selectedRows.value.map(item => item.id),
    onChange: (selectedRowKeys, rows) => {
        selectedRows.value = rows
    }
}))

// ==================== 查询 ====================

async function loadUsers() {
    loading.value = true

    try {
        const result = await getUserList({
            nickname: queryParams.nickname,
            role: queryParams.role,
            page: pagination.current,
            size: pagination.pageSize
        })

        users.value = result.data || []


    } catch (error) {

        console.error(error)

    } finally {

        loading.value = false

    }
}

// ==================== 搜索 ====================

function handleSearch() {
    pagination.current = 1

    loadUsers()
}

// ==================== 重置 ====================

function handleReset() {
    queryParams.nickname = ''
    queryParams.role = ''

    pagination.current = 1

    loadUsers()
}

// ==================== 分页 ====================

function handleTableChange(pageInfo) {

    pagination.current = pageInfo.current

    pagination.pageSize = pageInfo.pageSize

    loadUsers()
}

// ==================== 添加 ====================

function handleAdd() {

    editingId.value = null

    form.nickname = ''
    form.role = 'USER'
    form.points = 0
    form.enabled = true

    modalVisible.value = true
}

// ==================== 编辑 ====================

function handleEdit(record) {

    editingId.value = record.id

    form.nickname = record.nickname

    form.role = record.role

    form.points = record.points

    form.enabled = record.enabled

    modalVisible.value = true
}

// ==================== 保存 ====================

async function handleSubmit() {

    try {

        await formRef.value.validate()

        submitLoading.value = true

        if (editingId.value) {

            await updateUser(
                editingId.value,
                {
                    ...form
                }
            )

            message.success('修改成功')

        } else {

            await addUser({
                ...form
            })

            message.success('添加成功')
        }

        modalVisible.value = false

        loadUsers()

    } catch (error) {

        console.error(error)

    } finally {

        submitLoading.value = false

    }
}

// ==================== 删除 ====================

function handleDelete(record) {

    Modal.confirm({

        title: '确认删除？',

        content: `确定要删除用户「${record.nickname}」吗？`,

        okType: 'danger',

        async onOk() {

            await deleteUser(record.id)

            message.success('删除成功')

            loadUsers()
        }

    })
}

// ==================== 批量删除 ====================

function handleBatchDelete() {

    const ids = selectedRows.value.map(
        item => item.id
    )

    Modal.confirm({

        title: '确认批量删除？',

        content: `确定要删除选中的 ${ids.length} 个用户吗？`,

        okType: 'danger',

        async onOk() {

            await batchDeleteUser(ids)

            message.success('删除成功')

            selectedRows.value = []

            loadUsers()
        }

    })
}

// ==================== 状态 ====================

async function handleStatusChange(
    record,
    value
) {
    record.statusLoading = true
    try {
        const res = await updateUserStatus( record.id, value === false ? 0 : 1 )
        if (res.code === 200) {
            message.success('启用/禁用用户成功')
            loadUsers()
        }
    } catch (error) {
        console.error(error)

    } finally {
        record.statusLoading = false
    }
}

// ==================== 初始化 ====================

onMounted(() => {
    loadUsers()
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

    color: #999;
}

.search-card {
    margin-bottom: 20px;
}

.table-card {
    background: #fff;
}

.toolbar {
    display: flex;

    justify-content: space-between;

    align-items: center;
}
</style>