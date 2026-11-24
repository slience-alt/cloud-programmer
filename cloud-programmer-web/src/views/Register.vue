
<template>
  <div class="register-page">
    <!-- 左侧品牌区域 -->
    <div class="register-left">
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

    <!-- 右侧注册区域 -->
    <div class="register-right">
      <div class="register-container">

        <div class="register-header">
          <h2>创建账号</h2>
          <p>注册 Cloud Programmer，开启 AI 开发之旅</p>
        </div>

        <a-form
          :model="form"
          :rules="rules"
          layout="vertical"
          @finish="handleRegister"
        >
          <!-- 用户名 -->
          <a-form-item label="用户名" name="username">
            <a-input
              v-model:value="form.username"
              size="large"
              placeholder="请输入用户名"
              autocomplete="username"
            />
          </a-form-item>

          <!-- 昵称 -->
          <a-form-item label="昵称" name="nickname">
            <a-input
              v-model:value="form.nickname"
              size="large"
              placeholder="请输入昵称（选填）"
            />
          </a-form-item>

          <!-- 邮箱 -->
          <a-form-item label="邮箱" name="email">
            <a-input
              v-model:value="form.email"
              size="large"
              placeholder="请输入邮箱（选填）"
              autocomplete="email"
            />
          </a-form-item>

          <!-- 密码 -->
          <a-form-item label="密码" name="password">
            <a-input-password
              v-model:value="form.password"
              size="large"
              placeholder="请输入密码（至少8位）"
              autocomplete="new-password"
            />
          </a-form-item>

          <!-- 确认密码 -->
          <a-form-item label="确认密码" name="confirmPassword">
            <a-input-password
              v-model:value="form.confirmPassword"
              size="large"
              placeholder="请再次输入密码"
              autocomplete="new-password"
            />
          </a-form-item>

          <!-- 注册按钮 -->
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            block
            :loading="loading"
          >
            注册账号
          </a-button>

          <div class="login-entry">
            <span>已有账号？</span>
            <a @click="goLogin">立即登录</a>
          </div>

        </a-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { register } from '../api/user'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: ''
})

// 确认密码校验
const validateConfirmPassword = async (_rule, value) => {
  if (!value) {
    return Promise.reject(new Error('请再次输入密码'))
  }

  if (value !== form.password) {
    return Promise.reject(new Error('两次输入的密码不一致'))
  }

  return Promise.resolve()
}

// 表单校验规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 50, message: '用户名长度为4-50位', trigger: 'blur' }
  ],
  nickname: [
    { max: 50, message: '昵称不能超过50个字符', trigger: 'blur' }
  ],
  email: [
    {
      type: 'email',
      message: '邮箱格式不正确',
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度为8-64位', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 注册
const handleRegister = async () => {
  if (loading.value) {
    return
  }

  loading.value = true

    try {
        // 不向后端传递确认密码
        const params = {
            username: form.username,
            nickname: form.nickname,
            email: form.email,
            password: form.password
        }

        const res = await register(params)

        if (!res || !res.accessToken) {
            message.error(res?.message || '注册失败')
            return
        }

        // 保存 Token
        const accessToken = res.accessToken

        const cookieOptions = {
            path: '/',
            sameSite: 'Strict',
            secure: window.location.protocol === 'https:'
        }

        Cookies.set('accessToken', accessToken, cookieOptions)
        Cookies.set('uname', form.username, cookieOptions)

        message.success('注册成功')

        // 自动进入聊天页面
        router.replace('/chat')

    } catch (error) {
        console.error('注册失败：', error)

        const errorMessage =
            error.response?.data?.message ||
            error.message ||
            '注册失败，请稍后重试'

        message.error(errorMessage)

    } finally {
        loading.value = false
    }
}

// 返回登录
const goLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.register-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  background: #ffffff;
}

/* 左侧 */

.register-left {
  width: 55%;
  min-height: 100vh;
  padding: 48px 70px;
  position: relative;
  background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
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

/* 右侧 */

.register-right {
  flex: 1;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30px 20px;
  background: #ffffff;
}

.register-container {
  width: 400px;
  max-width: 100%;
}

.register-header {
  margin-bottom: 28px;
}

.register-header h2 {
  margin: 0 0 10px;
  color: #1f1f1f;
  font-size: 30px;
  font-weight: 600;
}

.register-header p {
  margin: 0;
  color: #8c8c8c;
  font-size: 14px;
}

.login-entry {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
  color: #999999;
}

.login-entry a {
  color: #1677ff;
  margin-left: 5px;
  cursor: pointer;
}

.login-entry a:hover {
  color: #4096ff;
}

/* 小屏幕适配 */

@media screen and (max-width: 768px) {
  .register-left {
    display: none;
  }

  .register-right {
    width: 100%;
  }
}
</style>
