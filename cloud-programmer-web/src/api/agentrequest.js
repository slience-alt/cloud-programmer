import axios from 'axios'
import Cookies from 'js-cookie'
import { message } from 'ant-design-vue'
import router from '../router'

const agentRequest = axios.create({
    baseURL: 'http://localhost:8888',
    timeout: 10000
})

// 请求拦截器
agentRequest.interceptors.request.use(
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
agentRequest.interceptors.response.use(
    response => response.data,
    error => {
        console.error('Agent 请求失败：', error)

        if (error.response) {
            const status = error.response.status

            if (status === 401) {
                message.error('登录已过期，请重新登录')

                Cookies.remove('accessToken')
                Cookies.remove('uname')

                if (router.currentRoute.value.path !== '/login') {
                    router.push('/login')
                }
            } else if (status === 403) {
                message.error('没有权限执行该操作')
            } else if (status >= 500) {
                message.error('Agent 服务异常')
            } else {
                message.error(
                    error.response.data?.message || 'Agent 请求失败'
                )
            }
        } else {
            message.error('Agent 服务连接失败')
        }

        return Promise.reject(error)
    }
)

export default agentRequest