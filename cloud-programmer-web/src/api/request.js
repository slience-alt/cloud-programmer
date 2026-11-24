import axios from 'axios'
import { message } from 'ant-design-vue'
import Cookies from 'js-cookie'
import router from '../router'

const request = axios.create({
  baseURL: 'http://localhost:8000',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = Cookies.get('accessToken')

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  response => response.data,
  error => {
    console.error('请求失败：', error)

    if (error.response) {
      const status = error.response.status

      if (status === 401) {
        message.error('登录已过期，请重新登录')

        // 清除登录信息
        Cookies.remove('accessToken')
        Cookies.remove('uname')

        // 跳转登录页，避免重复跳转
        if (router.currentRoute.value.path !== '/login') {
          router.push('/login')
        }
      } else if (status === 403) {
        message.error('没有权限执行该操作')
      } else if (status >= 500) {
        message.error('服务器异常')
      } else {
        message.error(
          error.response.data?.message || '请求失败'
        )
      }
    } else {
      message.error('网络连接失败')
    }

    return Promise.reject(error)
  }
)

export default request