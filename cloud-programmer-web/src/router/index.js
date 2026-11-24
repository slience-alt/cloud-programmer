
import {
  createRouter,
  createWebHistory
} from 'vue-router'

import Cookies from 'js-cookie'
import AdminLayout from '../layout/AdminLayout.vue'

const routes = [
  // 登录页面
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: {
      requiresAuth: false
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: {
      requiresAuth: false
    }
  },

  // 后台管理
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/users',
    meta: {
      requiresAuth: true
    },
    children: [
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('../views/UserManage.vue'),
        meta: {
          title: '用户管理'
        }
      },
      {
        path: 'agents',
        name: 'AgentManage',
        component: () => import('../views/AgentManage.vue'),
        meta: {
          title: 'Agent管理'
        }
      },
      {
        path: 'skills',
        name: 'SkillManage',
        component: () => import('../views/SkillManage.vue'),
        meta: {
          title: 'Skill 管理'
        }
      },
      {
        path: 'mcps',
        name: 'McpManage',
        component: () => import('../views/McpManage.vue'),
        meta: {
          title: 'MCP 管理'
        }
      }
    ]
  },

  // 首页：登录后进入 Chat
  {
    path: '/',
    redirect: '/chat'
  },

  // Chat 工作台
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('../views/ChatPage.vue'),
    meta: {
      requiresAuth: true
    }
  },

  // 404 页面
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局身份认证守卫
router.beforeEach(to => {
  const token = Cookies.get('accessToken')
  const isLogin = !!token

  if (to.matched.some(r => r.meta.requiresAuth) && !isLogin) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  if (['/login', '/register'].includes(to.path) && isLogin) {
    return '/chat'
  }

  return true
})

export default router
