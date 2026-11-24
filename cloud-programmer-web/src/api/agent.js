import agentRequest from './agentrequest'
import request from './request'


export function createAgentTask(data){
    return agentRequest({
        url:'/api/agent/tasks/submitStream',
        method:'post',
        data:{
            message:data.message,
            conversationId:data.conversationId,
            agentId: data.agentId
        }
    })
}

export function getAgentList(data = {}) {
    return request({
        url: '/agent/list',
        method: 'get',
        params: {
            name: data.name,
            modelName: data.modelName
        }
    })
}

export function insertAgent(data) {
    return request({
        url:'/agent/insert',
        method: 'post',
        data: {
            name: data.name,
            description: data.description,
            systemPrompt: data.systemPrompt,
            modelName: data.modelName,
            skillIds: data.skillIds,
            mcpIds: data.mcpIds
        }
    })
}

export function updateAgentStatus(id, status) {
    return request({
        url: '/agent/updateStatus',
        method: 'post',
        params: {
            id: id,
            status, status
        }
    })
}

export function updateAgent(id, data) {
    return request({
        url: '/agent/update',
        method: 'post',
        data: {
            id: id,
            name: data.name,
            description: data.description,
            systemPrompt: data.systemPrompt,
            modelName: data.modelName,
            skillIds: data.skillIds,
            mcpIds: data.mcpIds
        }
    })
}

export function deleteAgent(id) {
    return request({
        url: '/agent/delete',
        method: 'post',
        params: {
            id: id
        }
    })
}

export function batchDeleteAgent(ids) {
    return request({
        url: '/agetn/batchDelete',
        method: 'post',
        data: ids
    })
}

// 创建会话
export function createConversation(agentId) {
  return agentRequest({
    url: '/agent/conversations',
    method: 'post',
    data: { agentId }
  })
}

// 查询会话列表
export function getConversationList(agentId) {
  return agentRequest({
    url: '/agent/conversations',
    method: 'get',
    params: { agentId }
  })
}

// 重命名会话
export function renameConversation(id, title) {
  return agentRequest({
    url: `/agent/conversations/${id}/title`,
    method: 'put',
    data: { title }
  })
}

// 删除会话
export function deleteConversation(id) {
  return agentRequest({
    url: `/agent/conversations/${id}`,
    method: 'delete'
  })
}

export function getConversationMessages(conversationId) {
  return agentRequest({
    url: `/agent/conversations/${conversationId}/messages`,
    method: 'get'
  })
}

/**
 * 查询 Agent 执行详情
 */
export function getAgentRunDetail(taskId) {
    return agentRequest({
        url: `/api/agent/runs/${encodeURIComponent(taskId)}`,
        method: 'get'
    })
}

export function cancelAgentTask(taskId) {
  return agentRequest({
    url: `/api/agent/tasks/${taskId}/cancel`,
    method: 'post'
  })
}

export function getCurrentUser() {
  return request.get('/api/auth/me')
}