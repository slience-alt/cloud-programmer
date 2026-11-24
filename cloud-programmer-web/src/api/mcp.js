import agentRequest from './agentrequest'
import request from './request'

export function getMcpServerList(params) {
    return request({
        url: '/mcp/getMcpServerList',
        method: 'get',
        params
    })
}

export function updateMcpServerStatus(id, status) {
    return request({
        url: '/mcp/updateStatus',
        method: 'post',
        params: {
            id: id,
            status, status
        }
    })
}

export function insertMcpServer(data) {
    return request({
        url: '/mcp/insert',
        method: 'post',
        data: data
    })
}

export function deleteMcpServer(id) {
    return request({
        url: '/mcp/delete',
        method: 'post',
        params: {
            id: id
        }
    })
}

export function updateMcpServer(data) {
    return request({
        url: '/mcp/update',
        method: 'post',
        data: data
    })
}

