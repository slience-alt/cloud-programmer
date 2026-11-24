import request from './request.js'



/**
 * 用户登录
 */
export function login(data) {
  return request.post('/api/auth/login', data)
}

// 用户注册
export function register(data) {
  return request.post('/api/auth/register', data)
}

/**
 * 查询用户列表
 */
export function getUserList(params) {
  return request({
    url: '/user/list',
    method: 'post',
    params
  })
}

/**
 * 查询用户详情
 */
export function getUserDetail(id) {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

/**
 * 新增用户
 */
export function addUser(data) {
  return request({
    url: '/users',
    method: 'post',
    data
  })
}

/**
 * 修改用户
 */
export function updateUser(id, data) {
  return request({
    url: `/users/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除用户
 */
export function deleteUser(id) {
  return request({
    url: `/users/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除用户
 */
export function batchDeleteUser(ids) {
  return request({
    url: '/users/batch',
    method: 'delete',
    data: {
      ids
    }
  })
}

/**
 * 修改用户启用状态
 */
export function updateUserStatus(id, enabled) {
  return request({
    url: `/user/bannedUser`,
    method: 'post',
    params: {
      id: id,
      status: enabled
    }
  })
}
