import request from './request'

export function getSkillList(data = {}) {
    return request({
        url: '/skill/list',
        method: 'get',
        params: {
            name: data.name
        }
    })
}

export function insertSkill(data) {
    return request({
        url: '/skill/insert',
        method: 'post',
        data: {
            name: data.name,
            description: data.description,
            instruction: data.instruction
        }
    })
}

export function updateSkill(data) {
    return request({
        url: '/skill/update',
        method: 'post',
        data: {
            id: data.id,
            name: data.name,
            description: data.description,
            instruction: data.instruction
        }
    })
}

export function deleteSkill(id) {
    return request({
        url: '/skill/delete',
        method: 'post',
        params: {
            id: id
        }
    })
}

export function updataStatus(id, status) {
    return request({
        url: '/skill/updateStatus',
        method: 'post',
        params: {
            id: id,
            status: status
        }
    })
}