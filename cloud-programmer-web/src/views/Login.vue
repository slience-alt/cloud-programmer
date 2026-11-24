<template>
  <div class="login-page">
    <!-- 左侧 -->
    <div class="login-left">
      <div class="brand">
        <div class="brand-logo">AI</div>
        <span>Cloud Programmer</span>
      </div>

      <div class="slogan">
        <h1>
          AI 驱动的<br />
          新一代开发平台
        </h1>

        <p>
          让 AI Agent 帮助你完成开发、调试与部署，
          <br />
          让编程更加简单、高效。
        </p>
      </div>
    </div>

    <!-- 右侧 -->
    <div class="login-right">
      <div class="login-container">

        <div class="login-header">
          <h2>欢迎登录</h2>
          <p>登录 Cloud Programmer</p>
        </div>

        <a-form :model="form" layout="vertical" @finish="handleLogin">

          <!-- 用户名 -->
          <a-form-item label="用户名" name="username" :rules="[
            {
              required: true,
              message: '请输入用户名'
            }
          ]">
            <a-input v-model:value="form.username" size="large" placeholder="请输入用户名" />
          </a-form-item>

          <!-- 密码 -->
          <a-form-item label="密码" name="password" :rules="[
            {
              required: true,
              message: '请输入密码'
            }
          ]">
            <a-input-password v-model:value="form.password" size="large" placeholder="请输入密码" />
          </a-form-item>
          <!-- 登录按钮 -->
          <a-button type="primary" html-type="submit" size="large" block :loading="loading">
            登录
          </a-button>
         <div class="register-entry">
            <span>还没有账号？</span>
            <a @click="goRegister">立即注册</a>
          </div>

        </a-form>
      </div>
    </div>
  </div>
</template>

<script setup>

import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { login } from '../api/user'
import { useRouter, useRoute } from 'vue-router'
import Cookies from 'js-cookie'

const loading = ref(false)

const router = useRouter()
const route = useRoute()

const form = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (loading.value) {
    return
  }

  loading.value = true

  try {
    const res = await login(form)

    if (!res || !res.accessToken) {
      message.error(res?.message || '登录失败')
      return
    }

    const { accessToken, expiresIn } = res

    // Cookie 过期时间：秒转换为天
    const cookieOptions = {
      expires: expiresIn ? expiresIn / 86400 : 1,
      path: '/',
      sameSite: 'Strict',
      secure: window.location.protocol === 'https:'
    }

    // 保存 Token
    Cookies.set('accessToken', accessToken, cookieOptions)

    // 保存用户名
    Cookies.set('uname', form.username, cookieOptions)

    message.success('登录成功')

    // 获取登录前访问的地址
    const redirect = route.query.redirect

    // 防止跳转到外部地址
    const target = typeof redirect === 'string'
      && redirect.startsWith('/')
      && !redirect.startsWith('//')
      ? redirect
      : '/chat'

    // 替换当前登录页面
    router.replace(target)

  } catch (error) {
    console.error('登录失败：', error)

    const errorMessage =
      error.response?.data?.message ||
      error.message ||
      '登录失败，请稍后重试'

    message.error(errorMessage)

  } finally {
    loading.value = false
  }
}

const goRegister = () => {
  router.push('/register')
}

</script>

<style scoped>
.login-page {
  width: 100%;
  height: 100vh;
  display: flex;
  background: #ffffff;
}

/* =========================
   左侧
========================= */

.login-left {
  width: 55%;
  height: 100%;
  padding: 48px 70px;

  position: relative;

  background: linear-gradient(135deg,
      #1677ff 0%,
      #4096ff 100%);

  color: #ffffff;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;

  font-size: 20px;
  font-weight: 600;
}

.brand-logo {
  width: 42px;
  height: 42px;

  display: flex;
  align-items: center;
  justify-content: center;

  border-radius: 10px;

  background: rgba(255, 255, 255, 0.2);

  font-size: 16px;
  font-weight: 700;
}

.slogan {
  position: absolute;

  left: 70px;
  top: 50%;

  transform: translateY(-50%);
}

.slogan h1 {
  margin: 0 0 24px;

  font-size: 46px;
  line-height: 1.4;
  font-weight: 600;
}

.slogan p {
  margin: 0;

  font-size: 17px;
  line-height: 2;

  color: rgba(255, 255, 255, 0.85);
}

/* =========================
   右侧
========================= */

.login-right {
  flex: 1;

  display: flex;
  align-items: center;
  justify-content: center;

  background: #ffffff;
}

.login-container {
  width: 400px;
}

/* 标题 */

.login-header {
  margin-bottom: 36px;
}

.login-header h2 {
  margin: 0 0 10px;

  color: #1f1f1f;

  font-size: 30px;
  font-weight: 600;
}

.login-header p {
  margin: 0;

  color: #8c8c8c;

  font-size: 14px;
}

/* 登录选项 */

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;

  margin-top: -8px;
  margin-bottom: 20px;
}

.register-entry {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
  color: #999;
}

.register-entry a {
  color: #1677ff;
  margin-left: 5px;
  cursor: pointer;
}

.register-entry a:hover {
  color: #4096ff;
}

</style>