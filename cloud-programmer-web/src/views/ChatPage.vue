
<template>
  <div class="chat-layout">
    <!-- 左侧侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <div class="brand-icon">
          <RobotOutlined />
        </div>
        <span v-if="!sidebarCollapsed" class="brand-name">
          Cloud Programmer
        </span>
        <button class="icon-button collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">
          <MenuFoldOutlined v-if="!sidebarCollapsed" />
          <MenuUnfoldOutlined v-else />
        </button>
      </div>

      <div class="sidebar-main" v-if="!sidebarCollapsed">
        <!-- 新聊天 -->
        <button class="new-chat-btn" @click="createChat">
          <PlusOutlined />
          <span>新聊天</span>
          <span class="shortcut">Ctrl K</span>
        </button>
         <!-- 功能导航 -->
        <div class="nav-section">
          <div class="section-title">工作空间</div>
          <!-- Agent选择 -->
         <button class="nav-item agent-nav-item" @click="agentModalOpen = true">
            <RobotOutlined />
            <span>{{ currentAgent?.name || '选择 Agent' }}</span>
            <span class="nav-arrow">  › </span>
          </button>
          <button class="nav-item" @click="openFeature('plugins')">
            <AppstoreOutlined />
            <span>插件市场</span>
            <span class="nav-arrow">›</span>
          </button>
          <button class="nav-item" @click="openFeature('upload')">
            <CloudUploadOutlined />
            <span>数据上传</span>
            <span class="nav-arrow">›</span>
          </button>
        </div>

        <!-- 历史会话 -->
        <div class="history-section">
          <div class="section-title">最近聊天</div>

          <div v-if="sessions.length === 0" class="empty-history">
            暂无历史聊天
          </div>

          <div
            v-for="session in sessions"
            :key="session.id"
            class="history-item"
            :class="{ active: String(currentSessionId) === String(session.id) }"
            @click="switchChat(session.id)"
          >
            <MessageOutlined class="history-icon" />

            <span class="history-title">{{ session.title }}</span>

            <a-dropdown trigger="click">
              <button
                class="history-more icon-button"
                @click.stop
              >
                <MoreOutlined />
              </button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="renameChat(session.id)">
                    重命名
                  </a-menu-item>
                  <a-menu-item danger @click="deleteChat(session)">
                    删除会话
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
        </div>
      </div>

      <!-- 折叠模式下保留导航 -->
      <div v-else class="collapsed-nav">
        <button class="icon-button collapsed-action" title="新聊天" @click="createChat">
          <PlusOutlined />
        </button>
        <button class="icon-button collapsed-action" title="插件市场" @click="openFeature('plugins')">
          <AppstoreOutlined />
        </button>
        <button class="icon-button collapsed-action" title="数据上传" @click="openFeature('upload')">
          <CloudUploadOutlined />
        </button>
      </div>

      <!-- 底部用户栏 -->
      <div class="user-area">
        <a-dropdown trigger="click" placement="topLeft">
          <div class="user-profile" :class="{ 'user-profile-collapsed': sidebarCollapsed }">
            <a-avatar :size="36" class="user-avatar">
              {{ username.slice(0, 1).toUpperCase() }}
            </a-avatar>

            <div v-if="!sidebarCollapsed" class="user-info">
              <div class="user-name">{{ username }}</div>
              <div class="user-role">个人账户</div>
            </div>

            <MoreOutlined v-if="!sidebarCollapsed" class="user-more" />
          </div>

          <template #overlay>
            <a-menu>
             <a-menu-item key="profile" @click="openProfile">
                <UserOutlined /> 个人资料
              </a-menu-item>
              <a-menu-divider />
              <a-menu-item key="logout" danger @click="logout">
                <LogoutOutlined /> 退出登录
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </aside>

    <!-- 右侧聊天区域 -->
    <main class="chat-main">
      <!-- 顶部栏 -->
      <header class="chat-header">
        <div class="header-left">
          <span class="page-title">
            {{ currentSession?.title || 'AI 对话' }}
          </span>
          <span class="agent-badge">
            <span class="status-dot"></span>
            AI Agent
          </span>
        </div>

        <div class="header-actions">
          <a-tooltip title="新建聊天">
            <button class="icon-button header-icon" @click="createChat">
              <PlusOutlined />
            </button>
          </a-tooltip>
        </div>
      </header>

      <!-- 消息内容 -->
      <div ref="messageContainer" class="message-container">
        <!-- 欢迎页面 -->
        <div v-if="!currentSession || currentSession.messages.length === 0"
             class="welcome-view">
          <div class="welcome-icon">
            <RobotOutlined />
          </div>

          <h1>有什么可以帮你？</h1>
          <p class="welcome-description">
            描述你的任务，让 AI Agent 帮你完成复杂工作
          </p>

          <div class="suggestion-grid">
            <button
              v-for="item in suggestions"
              :key="item.title"
              class="suggestion-card"
              @click="sendSuggestion(item.prompt)"
            >
              <div class="suggestion-icon">
                <component :is="item.icon" />
              </div>
              <div class="suggestion-text">
                <div class="suggestion-title">{{ item.title }}</div>
                <div class="suggestion-desc">{{ item.description }}</div>
              </div>
              <RightOutlined class="suggestion-arrow" />
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
      <div v-else class="message-list">
         <div v-for="message in currentSession.messages" :key="message.id" class="message-row" :class="message.role">
            <!-- AI消息 -->
            <template v-if="message.role === 'assistant'">
              <div class="message-avatar">
                <img :src="avatars.assistant" class="avatar-img" />
              </div>

              <div class="message-content">
                <div class="message-author">
                  Cloud Programmer
                </div>

              <div class="message-bubble assistant">
                  <!-- markdown内容 -->
                 <div class="message-text markdown-body" v-html="message.htmlContent || ''"></div>
                  <!-- 正在生成 -->
                 <span v-if="
                    sending &&
                    message.id === generatingMessageId
                  " class="inline-typing">
                    <span></span>
                    <span></span>
                    <span></span>
                  </span>
                </div>
                <AgentRunDetails
                  v-if="message.taskId"
                  :task-id="message.taskId"
                />
              </div>

            </template>


            <!-- 用户消息 -->
            <template v-else>

              <div class="message-content user-content">

                <div class="message-author">
                  {{ username }}
                </div>
                <div class="message-bubble user">
                  <div class="message-text">
                    {{ message.content }}
                  </div>
                </div>
              </div>
              <div class="message-avatar">
                <img :src="avatars.user" class="avatar-img" />
              </div>
            </template>
          </div>

      </div>

      </div>

      <!-- 底部输入区 -->
      <footer class="composer-area">
        <div class="composer-box">
          <textarea
            v-model="inputText"
            class="message-input"
            placeholder="向 Cloud Programmer 发送消息..."
            rows="1"
            @keydown.enter.exact.prevent="sendMessage"
            @keydown.enter.shift.exact="inputText += '\n'"
          ></textarea>

          <div class="composer-toolbar">
            <div class="composer-left">
              <a-tooltip title="数据上传">
                <button class="composer-tool" @click="openFeature('upload')">
                  <PaperClipOutlined />
                  <span>添加文件</span>
                </button>
              </a-tooltip>

              <div class="model-label">
                <ThunderboltOutlined />
                <span>AI Agent</span>
                <DownOutlined class="model-arrow" />
              </div>
            </div>

          <div class="composer-right">
              <span class="enter-hint">
                {{ sending ? 'AI 正在生成...' : 'Enter 发送' }}
              </span>
              <!-- 正在生成时显示停止按钮 -->
              <button v-if="sending" class="send-button stop-button" :disabled="!currentTaskId" title="停止生成"
                @click="handleCancelTask">
                <StopOutlined />
              </button>

              <!-- 未生成时显示发送按钮 -->
              <button v-else class="send-button" :disabled="!inputText.trim()" title="发送消息" @click="sendMessage">
                <ArrowUpOutlined />
              </button>
            </div>
          </div>
        </div>

        <div class="composer-disclaimer">
          AI 生成的内容可能存在错误，请核实重要信息。
        </div>
      </footer>
    </main>

  <a-modal v-model:open="agentModalOpen" title="选择 Agent" :footer="null" width="650px">
      <div class="agent-list">
        <div v-for="agent in agents" :key="agent.id" class="agent-card" @click="selectAgent(agent)">
          <div class="agent-card-icon">
            <RobotOutlined />
          </div>
          <div class="agent-card-content">
            <div class="agent-card-title">
              {{ agent.name }}
            </div>
            <div class="agent-card-desc">
              {{ agent.description }}
            </div>
          </div>
        </div>
      </div>
     <!-- 底部添加按钮 -->
      <div class="agent-add-area">
        <a-button type="primary" block @click="openCreateAgent">
          <PlusOutlined />
          添加 Agent
        </a-button>
      </div>
    </a-modal>
    <!-- 功能弹窗占位 -->
    <a-modal
      v-model:open="featureModalOpen"
      :title="featureTitle"
      :footer="null"
      centered
      width="520px"
    >
      <div class="feature-placeholder">
        <div class="feature-placeholder-icon">
          <component :is="featureIcon" />
        </div>
        <h3>{{ featureTitle }}</h3>
        <p>{{ featureDescription }}</p>
        <a-button type="primary" @click="featureModalOpen = false">
          知道了
        </a-button>
      </div>
    </a-modal>
   <a-modal v-model:open="createAgentModalOpen" title="创建 Agent" width="680px" :confirm-loading="createAgentLoading"
      @ok="createAgent">
      <a-form layout="vertical">
        <a-form-item label="Agent 名称" required>
          <a-input v-model:value="newAgent.name" placeholder="例如：Java 开发助手" :maxlength="50" />
        </a-form-item>

        <a-form-item label="Agent 描述">
          <a-textarea v-model:value="newAgent.description" :rows="2" placeholder="描述这个 Agent 的用途" :maxlength="200" />
        </a-form-item>

        <a-form-item label="模型" required>
          <a-input v-model:value="newAgent.modelName" placeholder="例如：deepseek-chat" />
        </a-form-item>

        <a-form-item label="系统提示词">
          <a-textarea v-model:value="newAgent.systemPrompt" :rows="4" placeholder="定义 Agent 的角色、行为和工作方式" />
        </a-form-item>

        <a-divider orientation="left">能力配置</a-divider>

        <a-form-item label="关联 Skill">
          <a-select v-model:value="newAgent.skillIds" mode="multiple" allow-clear show-search
            :filter-option="filterOption" :options="skillOptions" :loading="skillLoading"
            placeholder="请选择 Agent 需要使用的 Skill" style="width: 100%" />
          <div class="form-tip">
            Skill 用于扩展 Agent 的专业知识、行为规范和执行流程。
          </div>
        </a-form-item>

        <a-form-item label="关联 MCP">
          <a-select v-model:value="newAgent.mcpIds" mode="multiple" allow-clear show-search
            :filter-option="filterOption" :options="mcpOptions" :loading="mcpLoading"
            placeholder="请选择 Agent 可以调用的 MCP 服务" style="width: 100%" />
          <div class="form-tip">
            MCP 用于给 Agent 提供外部工具和系统访问能力。
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
   <a-modal v-model:open="profileModalOpen" title="个人资料" :footer="null" width="480px" centered>
      <a-spin :spinning="profileLoading">
        <div class="profile-panel">
          <a-avatar :size="64" class="profile-avatar">
            {{ (profile.nickname || profile.username || 'U').slice(0, 1).toUpperCase() }}
          </a-avatar>

          <div class="profile-display-name">
            {{ profile.nickname || profile.username || '未设置昵称' }}
          </div>

          <div class="profile-account">
            @{{ profile.username || '-' }}
          </div>

          <a-divider />

          <div class="profile-info-list">

            <div class="profile-info-item">
              <span>昵称</span>
              <span>{{ profile.nickname || '未设置' }}</span>
            </div>

            <div class="profile-info-item">
              <span>邮箱</span>
              <span>{{ profile.email || '未绑定' }}</span>
            </div>

            <div class="profile-info-item">
              <span>账户角色</span>
              <a-tag :color="profile.role === 'ADMIN' ? 'blue' : 'default'">
                {{ profile.role || '未知角色' }}
              </a-tag>
            </div>

          <div class="profile-info-item">
              <span>账户余额</span>
              <a-tag :color="Number(profile.credit) > 100 ? 'success' : 'error'">
                {{ profile.credit ?? 0 }}
              </a-tag>
            </div>

            <div class="profile-info-item">
              <span>上一次登录时间</span>
              <span>{{ formatDate(profile.lastLoginTime) }}</span>
            </div>
          </div>
        </div>
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, h} from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import Cookies from 'js-cookie'
import { createAgentTask, getAgentList, insertAgent, createConversation, getConversationList, renameConversation, getConversationMessages,deleteConversation,cancelAgentTask,getCurrentUser} from '../api/agent'
import { getSkillList } from '../api/skill.js'
import { getMcpServerList } from '../api/mcp.js'
import md from '../api/markdown'
import AgentRunDetails from '../components/AgentRunDetails.vue'
import {
  RobotOutlined,
  PlusOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MessageOutlined,
  MoreOutlined,
  AppstoreOutlined,
  CloudUploadOutlined,
  UserOutlined,
  LogoutOutlined,
  RightOutlined,
  CopyOutlined,
  PaperClipOutlined,
  ThunderboltOutlined,
  DownOutlined,
  ArrowUpOutlined,
  CodeOutlined,
  FileSearchOutlined,
  DatabaseOutlined,
  StopOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()

const avatars = {
  assistant:
    'https://api.dicebear.com/9.x/bottts/svg?seed=cloud',
  user:
    'https://api.dicebear.com/9.x/personas/svg?seed=user'
}

// 用户信息：优先读取登录时写入的 username Cookie
const username = ref(Cookies.get('uname') || '管理员')

// 页面状态
const agentModalOpen = ref(false)
const sidebarCollapsed = ref(false)
const inputText = ref('')
const sending = ref(false)
const messageContainer = ref(null)
const generatingMessageId = ref(null)
const currentAgentId = ref(null)
const featureModalOpen = ref(false)
const featureType = ref('plugins')
const currentTaskId = ref(null)
const isGenerating = ref(false)
const canceling = ref(false)
let currentEventSource = null
let closeCurrentStream = null
const featureConfig = {
  plugins: {
    title: '插件市场',
    description: '在这里发现和管理 Agent 可使用的工具、Skill 与 MCP 服务。',
    icon: AppstoreOutlined
  },
  upload: {
    title: '数据上传',
    description: '上传文档和项目资料，为后续知识库检索、RAG 和 Agent 分析做准备。',
    icon: CloudUploadOutlined
  },
  profile: {
    title: '个人资料',
    description: '后续可以在这里管理昵称、个人信息和账户设置。',
    icon: UserOutlined
  }
}

const featureTitle = computed(() => featureConfig[featureType.value].title)
const featureDescription = computed(() => featureConfig[featureType.value].description)
const featureIcon = computed(() => featureConfig[featureType.value].icon)

// 欢迎页推荐任务
const suggestions = [
  {
    title: '编写代码',
    description: '帮助我实现一个 Java 接口',
    prompt: '帮我设计并实现一个 Spring Boot REST API。',
    icon: CodeOutlined
  },
  {
    title: '分析资料',
    description: '分析上传的文档和项目资料',
    prompt: '帮我规划一个基于 RAG 的文档检索方案。',
    icon: FileSearchOutlined
  },
  {
    title: '数据库助手',
    description: '编写 SQL 并优化查询',
    prompt: '帮我分析 SQL 查询性能，并提出索引优化建议。',
    icon: DatabaseOutlined
  },
  {
    title: 'Agent 开发',
    description: '设计 Skills、Tools 和 MCP',
    prompt: '帮我设计一个支持 Skill 和 MCP 的 AI Agent。',
    icon: RobotOutlined
  }
]

const createAgentModalOpen = ref(false)
const createAgentLoading = ref(false)

const newAgent = ref({
  name: '',
  description: '',
  modelName: '',
  systemPrompt: '',
  skillIds: [],
  mcpIds: []
})

// Skill
const skills = ref([])
const skillLoading = ref(false)

// MCP
const mcps = ref([])
const mcpLoading = ref(false)

const profileModalOpen = ref(false)
const profileLoading = ref(false)

const profile = ref({
  username: '',
  nickname: '',
  email: '',
  role: '',
  credit: 0,
  lastLoginTime: null
})

// 统一处理接口返回值，兼容 Axios 原始响应和已解包响应
function unwrapResponse(res) {
  return res?.data?.data ?? res?.data ?? res
}

const skillOptions = computed(() =>
  skills.value.map(item => ({
    label: item.name,
    value: String(item.id)
  }))
)

const mcpOptions = computed(() =>
  mcps.value.map(item => ({
    label: item.name,
    value: String(item.id)
  }))
)

function filterOption(input, option) {
  return String(option?.label || '')
    .toLowerCase()
    .includes(input.toLowerCase())
}

function formatDate(value) {
  if (!value) return '-'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'

  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

let nextMessageId = 1

const sessions = ref([])
const currentSessionId = ref(null)
const agents = ref([])
const currentSession = computed(() =>
  sessions.value.find(
    item => String(item.id) === String(currentSessionId.value)
  ) || null
)

async function createChat() {
  try {

    if (!currentAgentId.value) {
      message.warning('请先选择一个 Agent')
      agentModalOpen.value = true
      return null
    }

    const res = await createConversation(currentAgentId.value)
    // 兼容 Axios 响应与已解包响应
    const conversation = unwrapResponse(res)


    if (!conversation?.id) {
      throw new Error('创建会话成功，但没有返回会话 ID')
    }


    const session = {
      id: conversation.id,
      title: conversation.title || '新聊天',
      messages: [],
      messagesLoaded: true,
      loadingMessages: false,
      updatedAt: conversation.updatedAt
        ? new Date(conversation.updatedAt).getTime()
        : Date.now()
    }


    sessions.value.unshift(session)
    currentSessionId.value = session.id
    inputText.value = ''

    return session
  } catch (error) {
    console.error('创建会话失败：', error)
    message.error('创建会话失败，请稍后重试')
    return null
  }
}


async function switchChat(id) {
  // 统一按字符串比较会话 ID
  currentSessionId.value = String(id)

  const session = sessions.value.find(
    item => String(item.id) === String(id)
  )

  if (!session) return

  // 已经加载过的会话不重复请求
  if (session.messagesLoaded) {
    await scrollToBottom()
    return
  }

  // 防止同一会话重复发起查询
  if (session.loadingMessages) return

  session.loadingMessages = true

  try {
    const res = await getConversationMessages(session.id)

    // 兼容 Axios 原始响应、直接返回数组及常见包装结构
    const records = res?.data?.data ?? res?.data ?? res

    if (!Array.isArray(records)) {
      throw new Error('历史消息接口返回的数据不是数组')
    }

    session.messages = records.map(item => {
      const content = item.content ?? ''

      return {
        id: item.id ?? `history-${session.id}-${Math.random()}`,
        role: String(item.role || '').toLowerCase(),
        content,
        htmlContent: String(item.role || '').toLowerCase() === 'assistant'
          ? renderMarkdown(content)
          : ''
      }
    })

    session.messagesLoaded = true

    // 更新全局消息 ID，避免新消息和历史消息的 key 冲突
    nextMessageId = Math.max(
      nextMessageId,
      session.messages.length + 1
    )

    // 只有当前展示的仍然是这个会话时才滚动
    if (String(currentSessionId.value) === String(id)) {
      await scrollToBottom()
    }
  } catch (error) {
    console.error('加载历史消息失败：', error)
    message.error('加载历史消息失败，请重试')
  } finally {
    session.loadingMessages = false
  }
}


const currentAgent = computed(() => {
  return agents.value.find(
    item => String(item.id) === String(currentAgentId.value)
  ) || null
})

async function selectAgent(agent) {
  if (String(currentAgentId.value) === String(agent.id)) {
    agentModalOpen.value = false
    return
  }

  if (sending.value) {
    message.warning('当前 Agent 正在执行任务，请先停止任务')
    return
  }

  currentAgentId.value = agent.id
  agentModalOpen.value = false

  // 清空旧 Agent 的会话
  sessions.value = []
  currentSessionId.value = null
  inputText.value = ''

  // 加载新 Agent 的会话
  await loadConversationList()
}

async function openCreateAgent() {
  agentModalOpen.value = false

  resetNewAgent()
  createAgentModalOpen.value = true

  await Promise.all([
    loadSkills(),
    loadMcps()
  ])
}

function resetNewAgent() {
  newAgent.value = {
    name: '',
    description: '',
    modelName: '',
    systemPrompt: '',
    skillIds: [],
    mcpIds: []
  }
}

async function loadSkills() {
  skillLoading.value = true

  try {
    const res = await getSkillList()
    const list = unwrapResponse(res)

    skills.value = Array.isArray(list) ? list : []
  } catch (error) {
    console.error('加载 Skill 失败：', error)
    message.error('加载 Skill 列表失败')
  } finally {
    skillLoading.value = false
  }
}

async function loadMcps() {
  mcpLoading.value = true

  try {
    const res = await getMcpServerList()
    const list = unwrapResponse(res)

    mcps.value = Array.isArray(list) ? list : []
  } catch (error) {
    console.error('加载 MCP 失败：', error)
    message.error('加载 MCP 列表失败')
  } finally {
    mcpLoading.value = false
  }
}

async function createAgent() {
  if (!newAgent.value.name.trim()) {
    message.warning('请输入 Agent 名称')
    return
  }

  if (!newAgent.value.modelName.trim()) {
    message.warning('请输入模型名称')
    return
  }

  if (createAgentLoading.value) return

  createAgentLoading.value = true

  try {
    const params = {
      name: newAgent.value.name.trim(),
      description: newAgent.value.description,
      modelName: newAgent.value.modelName.trim(),
      systemPrompt: newAgent.value.systemPrompt,
      skillIds: newAgent.value.skillIds,
      mcpIds: newAgent.value.mcpIds
    }

    const res = await insertAgent(params)
    const customAgent = unwrapResponse(res)

    if (!customAgent?.id) {
      throw new Error('创建成功，但没有返回 Agent ID')
    }

    agents.value.unshift(customAgent)
    currentAgentId.value = customAgent.id

    sessions.value = []
    currentSessionId.value = null

    createAgentModalOpen.value = false
    agentModalOpen.value = false

    message.success('Agent 创建成功')

    resetNewAgent()

  } catch (error) {
    console.error('创建 Agent 失败：', error)
    message.error(error?.message || 'Agent 创建失败')
  } finally {
    createAgentLoading.value = false
  }
}


async function loadConversationList() {
  try {
    const res = await getConversationList(currentAgentId.value)
    const list = res?.data?.data ?? res?.data ?? res

    if (!Array.isArray(list)) {
      throw new Error('会话列表接口返回的数据不是数组')
    }

    sessions.value = list.map(item => ({
      id: item.id,
      title: item.title || '新聊天',
      messages: [],
      messagesLoaded: false,
      loadingMessages: false,
      updatedAt: item.updatedAt || item.updateTime
        ? new Date(item.updatedAt || item.updateTime).getTime()
        : Date.now()
    }))

    if (sessions.value.length > 0) {
      // 默认恢复列表中的第一个会话
      await switchChat(sessions.value[0].id)
    } else {
      currentSessionId.value = null
    }
  } catch (error) {
    console.error('查询会话列表失败：', error)
    message.error('加载历史会话失败')
  }
}




async function deleteChat(session) {
  Modal.confirm({
    title: '删除会话',
    content: `确定删除「${session.title}」吗？删除后无法恢复。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',

    onOk: async () => {
      try {
        // 1. 先删除后端数据
        await deleteConversation(session.id)

        // 2. 删除前端列表中的会话
        sessions.value = sessions.value.filter(
          item => String(item.id) !== String(session.id)
        )

        // 3. 如果删除的是当前会话，切换到其他会话
        if (String(currentSessionId.value) === String(session.id)) {
          currentSessionId.value = null

          if (sessions.value.length > 0) {
            await switchChat(sessions.value[0].id)
          }
        }

        message.success('删除成功')
      } catch (error) {
        console.error('删除会话失败：', error)
        message.error('删除失败，请重试')
        return Promise.reject(error)
      }
    }
  })
}



async function renameChat(sessionId) {
  const session = sessions.value.find(
    item => String(item.id) === String(sessionId)
  )

  if (!session) {
    message.error('会话不存在')
    return
  }

  const titleRef = ref(session.title)

  Modal.confirm({
    title: '重命名会话',
    content: h('input', {
      value: titleRef.value,
      placeholder: '请输入会话名称',
      maxlength: 100,
      style: 'width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 6px;',
      onInput: event => {
        titleRef.value = event.target.value
      }
    }),
    okText: '保存',
    cancelText: '取消',

    onOk: async () => {
      const title = titleRef.value.trim()

      if (!title) {
        message.warning('会话名称不能为空')
        throw new Error('会话名称不能为空')
      }

      if (title === session.title) {
        return
      }

      try {
        await renameConversation(session.id, title)

        session.title = title
        session.updatedAt = Date.now()

        message.success('重命名成功')
      } catch (error) {
        console.error('重命名会话失败：', error)
        message.error('重命名失败，请重试')
        throw error
      }
    }
  })
}


function openFeature(type) {
  featureType.value = type
  featureModalOpen.value = true
}

function sendSuggestion(prompt) {
  inputText.value = prompt
  sendMessage()
}


async function sendMessage() {
  if (!currentAgentId.value) {
    message.warning('请选择Agent')
    return
  }

  const content = inputText.value.trim()
  if (!content || sending.value) return

  // 如果当前没有会话，先创建并等待后端返回会话 ID
  let session = currentSession.value
  if (!session) {
    session = await createChat()
    if (!session) return
  }
  // 仅在首次发送消息时，将用户消息设置为会话标题
  if (!session.messages.length && session.title === '新聊天') {
    const title = generateConversationTitle(content)

    // 立即更新前端显示
    session.title = title
    session.updatedAt = Date.now()

    // 保存到数据库
    try {
      await renameConversation(session.id, title)
    } catch (error) {
      console.error('保存会话标题失败：', error)
      message.warning('会话标题保存失败')
    }
  }

  // 添加用户消息
  session.messages.push({
    id: nextMessageId++,
    role: 'user',
    content
  })

  inputText.value = ''
  sending.value = true

  // 创建 AI 占位消息
  const aiMessage = {
    id: nextMessageId++,
    role: 'assistant',
    content: '',
    htmlContent: '',
    taskId: null
  }

  session.messages.push(aiMessage)
  generatingMessageId.value = aiMessage.id

  const assistantMessage = session.messages[session.messages.length - 1]

  await scrollToBottom()

  try {
    const res = await createAgentTask({
      message: content,
      conversationId: String(session.id),
      agentId: currentAgentId.value
    })

    // 兼容 Axios 原始响应和已经解包的响应
    const data = res?.data?.data ?? res?.data ?? res
    const taskId = data?.taskId
    isGenerating.value = true
    if (!taskId) {
      throw new Error('创建任务成功，但响应中没有 taskId')
    }

    currentTaskId.value = String(taskId)
    isGenerating.value = true

    // 将 taskId 保存到当前 AI 消息
    aiMessage.taskId = String(taskId)

    connectAgentStream(taskId, assistantMessage)
  } catch (error) {
    console.error('Agent 调用失败：', error)

    aiMessage.content = '调用 Agent 失败：' + error.message
    sending.value = false
    isGenerating.value = false
    currentTaskId.value = null
  }
}


const handleCancelTask = async () => {
  if (!currentTaskId.value || canceling.value) {
    return
  }

  const taskId = currentTaskId.value

  try {
    canceling.value = true

    // 先请求后端取消任务
    const res = await cancelAgentTask(taskId)

    const result = res?.data?.data ?? res?.data ?? res

    if (result === true) {
      // 后端确认接受取消后，再关闭 SSE
      if (closeCurrentStream) {
        closeCurrentStream()
      } else if (currentEventSource) {
        currentEventSource.close()
        currentEventSource = null
      }

      sending.value = false
      isGenerating.value = false
      currentTaskId.value = null
      generatingMessageId.value = null

      message.success('已发送取消请求，任务正在停止')
    } else {
      // 取消失败或任务已经结束，不主动关闭 SSE
      message.warning('任务已经结束或无法取消')
    }
  } catch (error) {
    console.error('取消 Agent 任务失败：', error)
    message.error('取消任务失败，请重试')
  } finally {
    canceling.value = false
  }
}



async function scrollToBottom() {
  await nextTick()

  const container = messageContainer.value
  if (container) {
    container.scrollTop = container.scrollHeight
  }
}

async function copyMessage(content) {
  try {
    await navigator.clipboard.writeText(content)
    message.success('复制成功')
  } catch {
    message.error('复制失败，请手动复制')
  }
}


function logout() {
  Modal.confirm({
    title: '退出登录',
    content: '确定退出当前账号吗？退出后需要重新登录。',
    okText: '退出登录',
    okType: 'danger',
    cancelText: '取消',

    async onOk() {
      try {
        // 1. 关闭 SSE 连接，同时清理 Token 缓冲定时器
        if (closeCurrentStream) {
          closeCurrentStream()
        } else if (currentEventSource) {
          currentEventSource.close()
          currentEventSource = null
        }

        // 2. 清理 Cookie
        Cookies.remove('accessToken', { path: '/' })
        Cookies.remove('uname', { path: '/' })

        // 3. 清理页面状态
        sessions.value = []
        agents.value = []
        currentSessionId.value = null
        currentAgentId.value = null
        currentTaskId.value = null
        inputText.value = ''

        sending.value = false
        isGenerating.value = false
        generatingMessageId.value = null
        canceling.value = false

        // 4. 返回登录页面
        await router.replace('/login')

        message.success('退出登录成功')
      } catch (error) {
        console.error('退出登录失败：', error)
        message.error('退出登录失败，请重试')
        throw error
      }
    }
  })
}


function generateConversationTitle(userContent) {
  if (!userContent || !userContent.trim()) {
    return '新聊天'
  }

  const title = userContent.trim().replace(/\s+/g, ' ')
  const maxLength = 30

  return title.length > maxLength
    ? title.slice(0, maxLength) + '...'
    : title
}

async function updateConversationTitle(session, userContent) {
  const title = generateConversationTitle(userContent)

  // 先更新前端显示
  session.title = title
  session.updatedAt = Date.now()

  // 再保存到数据库
  try {
    await renameConversation(session.id, title)
  } catch (error) {
    console.error('保存会话标题失败：', error)
    message.warning('会话标题保存失败')
  }
}


function connectAgentStream(taskId, aiMessage) {
  const url = `http://localhost:8888/api/agent/tasks/${taskId}/stream`

  const eventSource = new EventSource(url, {
    withCredentials: true
  })

  currentEventSource = eventSource

  let tokenBuffer = ''
  let flushTimer = null
  let closed = false
  let lastScrollTime = 0

  // 刷新 Token 缓冲区
  function flushTokens() {
    if (!tokenBuffer) {
      return
    }

    aiMessage.content += tokenBuffer
    tokenBuffer = ''

    aiMessage.htmlContent = renderMarkdown(aiMessage.content)

    const now = Date.now()
    if (now - lastScrollTime >= 200) {
      lastScrollTime = now
      scrollToBottom()
    }
  }

  // 定时批量刷新，避免每个 Token 都重新渲染 Markdown
  function scheduleFlush() {
    if (flushTimer !== null) {
      return
    }

    flushTimer = setTimeout(() => {
      flushTimer = null
      flushTokens()
    }, 80)
  }

  // 统一关闭连接并清理资源
  function closeStream() {
    if (closed) {
      return
    }

    closed = true
    eventSource.close()

    if (flushTimer !== null) {
      clearTimeout(flushTimer)
      flushTimer = null
    }

    // 把剩余 Token 刷新出来
    flushTokens()

    if (currentEventSource === eventSource) {
      currentEventSource = null
      closeCurrentStream = null
    }
  }

  // 统一结束前端任务状态
  function finishTask() {
    closeStream()

    sending.value = false
    isGenerating.value = false
    currentTaskId.value = null
    generatingMessageId.value = null
  }

  closeCurrentStream = closeStream

  // 1. 接收模型 Token
  eventSource.addEventListener('token', event => {
    if (eventSource !== currentEventSource || closed) {
      return
    }

    tokenBuffer += event.data
    scheduleFlush()
  })

  // 2. 任务成功
  eventSource.addEventListener('done', event => {
    if (eventSource !== currentEventSource || closed) {
      return
    }

    finishTask()
  })

  // 3. 任务取消
  eventSource.addEventListener('cancelled', event => {
    if (eventSource !== currentEventSource || closed) {
      return
    }

    finishTask()
    message.info('任务已停止')
  })

  // 4. 业务异常
  function handleTaskError(event) {
    if (eventSource !== currentEventSource || closed) {
      return
    }

    finishTask()
    message.error(event.data || 'Agent 执行失败')
  }

  // 推荐后端使用 task_error
  eventSource.addEventListener('task_error', handleTaskError)

  // 兼容后端暂时还在使用的 error 事件
  eventSource.addEventListener('error', event => {
    if (event instanceof MessageEvent) {
      handleTaskError(event)
    }
  })

  // 5. 网络连接异常
  eventSource.onerror = event => {
    console.warn('SSE 连接状态变化：', event)

    // CONNECTING：浏览器正在自动重连，不要关闭连接
    if (eventSource.readyState === EventSource.CONNECTING) {
      return
    }

    // CLOSED：连接已彻底关闭
    if (eventSource.readyState === EventSource.CLOSED) {
      if (eventSource === currentEventSource && !closed) {
        closeStream()
        sending.value = false
        isGenerating.value = false
        currentTaskId.value = null

        message.warning('实时连接已关闭，可以重新加载任务详情')
      }
    }
  }

  return eventSource
}



function renderMarkdown(content){
  return md.render(formatMarkdown(content))
}

function formatMarkdown(content){
    return content
        .replace(/\\#/g, '#')
        .replace(/^(#+)([^ #])/gm,'$1 $2');
}

async function openProfile() {
  profileModalOpen.value = true
  profileLoading.value = true

  try {
    const res = await getCurrentUser()
    const data = unwrapResponse(res)

    profile.value = {
      username: data.username || '',
      nickname: data.nickname || '',
      email: data.email || '',
      role: data.role || 'USER',
      credit: data.credit ?? 0,
      lastLoginTime: data.lastLoginTime || null
    }

    if (profile.value.username) {
      username.value = profile.value.username
    }
  } catch (error) {
    console.error('获取个人资料失败：', error)
    message.error('获取个人资料失败，请重新登录或稍后重试')
  } finally {
    profileLoading.value = false
  }
}






onMounted(async () => {
  try {
    const res = await getAgentList()
    const list = res?.data?.data ?? res?.data ?? res

    agents.value = Array.isArray(list) ? list : []

    if (agents.value.length > 0) {
      currentAgentId.value = agents.value[0].id
      await loadConversationList()
    } else {
      currentAgentId.value = null
      sessions.value = []
      currentSessionId.value = null
    }
  } catch (error) {
    console.error('初始化聊天页面失败：', error)
    message.error('初始化聊天页面失败')
  }
})



</script>

<style scoped>
* {
  box-sizing: border-box;
}

.chat-layout {
  display: flex;
  width: 100%;
  height: 100vh;
  min-width: 0;
  overflow: hidden;
  background: #ffffff;
  color: #202938;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI",
    "PingFang SC", "Microsoft YaHei", sans-serif;
}

/* 左侧侧边栏 */
.sidebar {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  width: 264px;
  background: #f7f8fa;
  border-right: 1px solid #e9ecf1;
  transition: width 0.2s ease;
}

.sidebar.collapsed {
  width: 68px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 68px;
  padding: 0 16px;
}

.brand-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  color: white;
  font-size: 19px;
  border-radius: 11px;
  background: linear-gradient(135deg, #596fe6, #3545a7);
  box-shadow: 0 3px 8px #596fe629;
}

.brand-name {
  flex: 1;
  font-size: 14px;
  font-weight: 650;
  letter-spacing: -0.3px;
  white-space: nowrap;
}

.icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 7px;
  color: #697386;
  background: transparent;
  cursor: pointer;
}

.icon-button:hover {
  background: #e9edf4;
  color: #28354a;
}

.collapse-btn {
  margin-left: auto;
}

.sidebar-main {
  flex: 1;
  min-height: 0;
  padding: 10px 12px;
  overflow-y: auto;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 42px;
  padding: 0 12px;
  border: 1px solid #e0e5ee;
  border-radius: 9px;
  background: #ffffff;
  color: #263248;
  font-size: 13px;
  font-weight: 550;
  cursor: pointer;
  box-shadow: 0 1px 2px #17203305;
}

.new-chat-btn:hover {
  border-color: #b7c5f8;
  background: #fdfdff;
}

.shortcut {
  margin-left: auto;
  font-size: 10px;
  font-weight: 400;
  color: #9aa2b1;
}

.history-section,
.nav-section {
  margin-top: 28px;
}

.section-title {
  padding: 0 9px;
  margin-bottom: 10px;
  color: #929aaa;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.empty-history {
  padding: 10px 9px;
  color: #9ba3b2;
  font-size: 12px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
  height: 38px;
  padding: 0 9px;
  margin-bottom: 3px;
  border-radius: 8px;
  color: #5c6678;
  font-size: 12px;
  cursor: pointer;
}

.history-item:hover,
.history-item.active {
  background: #e9edf5;
  color: #26354c;
}

.history-icon {
  flex-shrink: 0;
  font-size: 14px;
}

.history-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-more {
  display: none;
  width: 26px;
  height: 26px;
}

.history-item:hover .history-more {
  display: inline-flex;
}

.nav-section {
  padding-top: 4px;
  border-top: 1px solid #e7eaf0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  height: 42px;
  padding: 0 10px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #515d70;
  font-size: 13px;
  text-align: left;
  cursor: pointer;
}

.nav-item:hover {
  background: #e9edf4;
  color: #3045a5;
}

.nav-arrow {
  margin-left: auto;
  font-size: 20px;
  color: #a1a8b5;
}

.user-area {
  padding: 12px;
  border-top: 1px solid #e6e9ef;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 10px;
  cursor: pointer;
}

.user-profile:hover {
  background: #e9edf4;
}

.user-avatar {
  flex-shrink: 0;
  color: #4355b5;
  background: #e2e8ff;
  font-weight: 650;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  overflow: hidden;
  color: #293448;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-role {
  margin-top: 3px;
  color: #929aaa;
  font-size: 11px;
}

.user-more {
  color: #8d96a6;
}

.collapsed-nav {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  flex: 1;
  padding-top: 20px;
}

.collapsed-action {
  width: 40px;
  height: 40px;
  font-size: 17px;
}

.user-profile-collapsed {
  justify-content: center;
  padding: 5px 0;
}

/* 右侧主区域 */
.chat-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  height: 100%;
  background: #ffffff;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  height: 62px;
  padding: 0 30px;
  border-bottom: 1px solid #f0f1f4;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.page-title {
  overflow: hidden;
  color: #263247;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 9px;
  border: 1px solid #e4e9f5;
  border-radius: 6px;
  color: #596a9d;
  background: #f8faff;
  font-size: 10px;
  white-space: nowrap;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #47b881;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  width: 36px;
  height: 36px;
  font-size: 16px;
}

/* 消息区域 */
.message-container {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #d9dde5 transparent;
}

.welcome-view {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 760px;
  padding: 60px 24px 30px;
  margin: 0 auto;
}

.welcome-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  border: 1px solid #e5e9ff;
  border-radius: 18px;
  color: #596be0;
  background: linear-gradient(145deg, #f0f2ff, #e7ebff);
  font-size: 27px;
  box-shadow: 0 5px 20px #5265d40c;
}

.welcome-view h1 {
  margin: 22px 0 10px;
  color: #1f293a;
  font-size: 27px;
  font-weight: 650;
  letter-spacing: -0.8px;
  text-align: center;
}

.welcome-description {
  margin: 0;
  color: #8a93a3;
  font-size: 13px;
  text-align: center;
}

.suggestion-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
  margin-top: 40px;
}

.suggestion-card {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  min-height: 82px;
  padding: 15px;
  border: 1px solid #e9ecf2;
  border-radius: 12px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
}

.suggestion-card:hover {
  border-color: #c7d0fa;
  box-shadow: 0 5px 16px #384caa0a;
  transform: translateY(-1px);
}

.suggestion-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 37px;
  height: 37px;
  border-radius: 10px;
  color: #596cce;
  background: #f0f2ff;
  font-size: 17px;
}

.suggestion-text {
  flex: 1;
  min-width: 0;
}

.suggestion-title {
  color: #354056;
  font-size: 12px;
  font-weight: 600;
}

.suggestion-desc {
  margin-top: 5px;
  color: #929aaa;
  font-size: 11px;
  line-height: 1.5;
}

.suggestion-arrow {
  flex-shrink: 0;
  color: #b1b8c6;
  font-size: 11px;
}

/* 消息列表 */
.message-list {
  width: 100%;
  max-width: 820px;
  padding: 30px 24px 40px;
  margin: 0 auto;
}

.message-row {
  display:flex;
  align-items:flex-start;
  gap:12px;
  margin-bottom:28px;
}


.message-row.user {
  justify-content:flex-end;
}

.message-avatar {
 width:36px;
 height:36px;
 border-radius:50%;
 overflow:hidden;
 flex-shrink:0;
}


.avatar-img {
 width:100%;
 height:100%;
 object-fit:cover;

}

.ai-avatar {
  border: 1px solid #e4e8ff;
  color: #5367d1;
  background: #f0f2ff;
  font-size: 17px;
}

.user-message-avatar {
  color: #4657b4;
  background: #e5eaff;
  font-size: 12px;
  font-weight: 650;
}

.message-content {
  display: flex;
  flex-direction: column;
  min-width: 0;
  max-width: min(85%, 680px);
}

.message-author {
  margin-bottom: 9px;
  color: #7d8799;
  font-size: 11px;
}

.message-row.user .message-author {
  text-align: right;
}

.message-bubble {
  padding: 13px 16px;
  border-radius: 12px;
  color: #344054;
  font-size: 13px;
  line-height: 1.85;
  overflow-wrap: anywhere;
}

.message-bubble.user {
  max-width:600px;
  padding:12px 16px;
  border-radius:18px;
  background:#e8f0ff;
  color:#202938;
  line-height:1.7;
}

.message-bubble.assistant {
  max-width:700px;
  padding:14px 18px;
  border-radius:14px;
  background:#ffffff;
  line-height:1.8;
}


.message-text {
  white-space: pre-wrap;
}

.message-tools {
  display: flex;
  gap: 10px;
  margin-top: 9px;
}

.tool-button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 7px;
  border: 0;
  border-radius: 5px;
  color: #8992a2;
  background: transparent;
  font-size: 11px;
  cursor: pointer;
}

.tool-button:hover {
  color: #4e60be;
  background: #f3f5fb;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 12px 0;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #8290c7;
  animation: typing 1s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes typing {
  0%, 60%, 100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  30% {
    opacity: 1;
    transform: translateY(-4px);
  }
}

/* 底部输入区 */
.composer-area {
  flex-shrink: 0;
  width: 100%;
  padding: 14px 24px 18px;
  background: linear-gradient(to bottom, #ffffff 80%, #fafbfe);
}

.composer-box {
  width: 100%;
  max-width: 820px;
  padding: 14px 14px 10px;
  margin: 0 auto;
  border: 1px solid #dfe4ed;
  border-radius: 15px;
  background: #ffffff;
  box-shadow: 0 5px 24px #1e293b09;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.composer-box:focus-within {
  border-color: #aab8ef;
  box-shadow: 0 5px 24px #536bd10d;
}

.message-input {
  display: block;
  width: 100%;
  min-height: 48px;
  max-height: 180px;
  padding: 3px 2px 10px;
  resize: vertical;
  border: none;
  outline: none;
  color: #293448;
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.7;
}

.message-input::placeholder {
  color: #a0a8b6;
}

.composer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 34px;
}

.composer-left,
.composer-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.composer-tool {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 9px;
  border: 1px solid #e7eaf0;
  border-radius: 7px;
  color: #737e90;
  background: #ffffff;
  font-size: 11px;
  cursor: pointer;
}

.composer-tool:hover {
  color: #5266c9;
  border-color: #ccd4f5;
}

.model-label {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  border-radius: 6px;
  color: #64718a;
  background: #f5f7fb;
  font-size: 11px;
}

.model-arrow {
  margin-left: 2px;
  font-size: 9px;
}

.enter-hint {
  color: #a0a8b6;
  font-size: 10px;
}

.send-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 9px;
  color: #ffffff;
  background: #5266ce;
  font-size: 17px;
  cursor: pointer;
  transition: background 0.2s;
}

.send-button:hover:not(:disabled) {
  background: #4053b5;
}

.send-button:disabled {
  color: #a9b0bf;
  background: #edf0f5;
  cursor: not-allowed;
}

.composer-disclaimer {
  margin-top: 10px;
  color: #a1a8b5;
  font-size: 10px;
  text-align: center;
}

/* 功能弹窗 */
.feature-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 28px 16px 20px;
  text-align: center;
}

.feature-placeholder-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  border-radius: 16px;
  color: #5669cc;
  background: #eff2ff;
  font-size: 25px;
}

.feature-placeholder h3 {
  margin: 18px 0 8px;
  color: #293448;
  font-size: 16px;
}

.feature-placeholder p {
  max-width: 360px;
  margin-bottom: 24px;
  color: #8490a2;
  font-size: 12px;
  line-height: 1.8;
}

/* 小屏幕适配 */
@media (max-width: 900px) {
  .sidebar {
    width: 224px;
  }

  .sidebar-header {
    padding: 0 12px;
  }

  .brand-name {
    font-size: 12px;
  }

  .chat-header {
    padding: 0 18px;
  }

  .welcome-view {
    padding-top: 42px;
  }

  .composer-area {
    padding-right: 16px;
    padding-left: 16px;
  }
}

@media (max-width: 640px) {
  .sidebar {
    width: 60px;
  }

  .sidebar-header {
    justify-content: center;
    padding: 0;
  }

  .brand-name,
  .collapse-btn,
  .sidebar-main {
    display: none;
  }

  .collapsed-nav {
    display: flex;
  }

  .sidebar:not(.collapsed) {
    width: 60px;
  }

  .sidebar:not(.collapsed) .sidebar-header {
    justify-content: center;
  }

  .sidebar:not(.collapsed) .brand-name,
  .sidebar:not(.collapsed) .collapse-btn,
  .sidebar:not(.collapsed) .sidebar-main {
    display: none;
  }

  .sidebar:not(.collapsed) .collapsed-nav {
    display: flex;
  }

  .user-area {
    padding: 8px 4px;
  }

  .user-info,
  .user-more {
    display: none;
  }

  .user-profile {
    justify-content: center;
    padding: 5px 0;
  }

  .chat-header {
    height: 54px;
    padding: 0 12px;
  }

  .welcome-view {
    padding: 42px 16px 20px;
  }

  .welcome-view h1 {
    font-size: 22px;
  }

  .suggestion-grid {
    grid-template-columns: 1fr;
    gap: 9px;
    margin-top: 28px;
  }

  .suggestion-card {
    min-height: 72px;
    padding: 12px;
  }

  .message-list {
    padding: 22px 14px;
  }

  .composer-area {
    padding: 10px 10px 12px;
  }

  .enter-hint {
    display: none;
  }

  .composer-left {
    gap: 6px;
  }

  .composer-tool {
    padding: 0 7px;
  }

  .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
  max-width: 100%;
}

.message-text {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.inline-typing {
  display: inline-flex;
  margin-top: 6px;
}

.typing-indicator {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.typing-indicator span {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #999;
  animation: typing 1.2s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    opacity: 0.35;
    transform: translateY(0);
  }

  30% {
    opacity: 1;
    transform: translateY(-4px);
  }
}
.markdown-body {
  line-height: 1.8;
  font-size: 14px;
  color: #344054;
}

.markdown-body p {
  margin: 8px 0;
}

.markdown-body h1,
.markdown-body h2,
.markdown-body h3 {
  margin: 16px 0 8px;
  font-weight: 600;
}

.markdown-body ul,
.markdown-body ol {
  padding-left: 24px;
}

.markdown-body code {
  padding: 2px 5px;
  border-radius: 4px;
  background: #f5f5f5;
  font-family: Consolas, monospace;
}

.markdown-body pre {
  padding: 12px;
  overflow-x: auto;
  border-radius: 8px;
  background: #f6f8fa;
}

.markdown-body pre code {
  padding: 0;
  background: transparent;
}
}

.agent-card-container {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}


.agent-card {
  display: flex;
  align-items: flex-start;
  height: 110px;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  cursor: pointer;
  transition: all .2s;
}


.agent-card:hover {
  border-color: #409eff;
  box-shadow:
    0 4px 12px rgba(0,0,0,.08);
}



.agent-card-icon {

  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 22px;
}



.agent-card-title {

  margin-top: 12px;
  font-size: 15px;
  font-weight: 600;

}



.agent-card-desc {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}



.agent-card-footer {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}

.agent-list::-webkit-scrollbar {
  width: 6px;
}


.agent-list::-webkit-scrollbar-thumb {
  background: #d9dde7;
  border-radius: 10px;
}


.agent-list::-webkit-scrollbar-track {
  background: transparent;
}


.agent-add-area{
  margin-top:16px;
  padding-top:12px;
  border-top:1px solid #f0f0f0;
}
.stop-button {
  background: #ef4444;
  border-color: #ef4444;
  color: #fff;
}

.stop-button:hover:not(:disabled) {
  background: #dc2626;
  border-color: #dc2626;
}

.stop-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
/* 个人资料 */
.profile-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
}

.profile-avatar {
  background: linear-gradient(135deg, #596fe6, #3545a7);
  font-size: 24px;
}

.profile-display-name {
  margin-top: 14px;
  font-size: 18px;
  font-weight: 600;
  color: #202938;
}

.profile-account {
  margin-top: 5px;
  font-size: 13px;
  color: #999;
}

.profile-info-list {
  width: 100%;
}

.profile-info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 44px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}

.profile-info-item > span:first-child {
  color: #888;
}

.profile-info-item > span:last-child {
  color: #333;
  text-align: right;
}

/* Agent 创建表单 */
.form-tip {
  margin-top: 5px;
  font-size: 12px;
  color: #999;
  line-height: 20px;
}
</style>
