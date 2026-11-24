<template>
  <a-layout class="admin-layout">

    <!-- 左侧 -->
    <a-layout-sider
      v-model:collapsed="collapsed"
      :trigger="null"
      collapsible
      class="sidebar"
    >

      <!-- Logo -->
      <div class="logo">
        <div class="logo-icon">
          AI
        </div>

        <span v-if="!collapsed">
          Cloud Programmer
        </span>
      </div>

      <!-- 菜单 -->
      <a-menu
        v-model:selectedKeys="selectedKeys"
        theme="dark"
        mode="inline"
        @click="handleMenuClick"
      >

        <a-menu-item key="/admin/users">
          <template #icon>
            <UserOutlined />
          </template>

          用户管理
        </a-menu-item>

        <a-menu-item key="/admin/agents">
          <template #icon>
            <AppstoreOutlined />
          </template>

          Agent管理
        </a-menu-item>

        <a-menu-item key="/admin/skills">
          <template #icon>
            <AppstoreOutlined />
          </template>

          Skill管理
       </a-menu-item>
        <a-menu-item key="/admin/mcps">
          <template #icon>
            <AppstoreOutlined />
          </template>

          Mcp管理
        </a-menu-item>

      </a-menu>

    </a-layout-sider>

    <!-- 右侧 -->
    <a-layout>

      <!-- 顶部 -->
      <a-layout-header class="header">

        <div class="header-left">

          <MenuUnfoldOutlined
            v-if="collapsed"
            class="trigger"
            @click="collapsed = false"
          />

          <MenuFoldOutlined
            v-else
            class="trigger"
            @click="collapsed = true"
          />

        </div>

        <div class="header-right">

          <a-dropdown>

            <span class="user-info">
              <UserOutlined />

              <span>{{ username }}</span>

            </span>

            <template #overlay>

              <a-menu>

                <a-menu-item @click="logout">
                  退出登录
                </a-menu-item>

              </a-menu>

            </template>

          </a-dropdown>

        </div>

      </a-layout-header>

      <!-- 内容 -->
      <a-layout-content class="content">

        <router-view />

      </a-layout-content>

    </a-layout>

  </a-layout>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Cookies from 'js-cookie'

import {
  UserOutlined,
  AppstoreOutlined,
  MenuUnfoldOutlined,
  MenuFoldOutlined
} from '@ant-design/icons-vue'

const username = ref(Cookies.get('uname') || '未登录')

const router = useRouter()
const route = useRoute()

const collapsed = ref(false)

const selectedKeys = ref([route.path])

watch(
  () => route.path,
  path => {
    selectedKeys.value = [path]
  }
)

function handleMenuClick({ key }) {
  router.push(key)
}

function logout() {
  Cookies.remove('uname')
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.sidebar {
  min-height: 100vh;
}

.logo {
  height: 64px;

  display: flex;
  align-items: center;

  padding: 0 20px;

  color: #fff;

  font-size: 17px;
  font-weight: 600;

  overflow: hidden;
}

.logo-icon {
  width: 36px;
  height: 36px;

  margin-right: 10px;

  border-radius: 8px;

  display: flex;
  align-items: center;
  justify-content: center;

  background: #1677ff;

  color: #fff;

  font-size: 14px;
}

.header {
  background: #fff;

  padding: 0 24px;

  display: flex;
  align-items: center;
  justify-content: space-between;

  border-bottom: 1px solid #f0f0f0;
}

.trigger {
  font-size: 18px;

  cursor: pointer;
}

.user-info {
  cursor: pointer;

  display: flex;
  align-items: center;

  gap: 8px;
}

.content {
  padding: 24px;

  background: #f5f5f5;

  min-height: calc(100vh - 64px);
}
</style>