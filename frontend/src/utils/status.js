// 工单状态字典
export const statusMap = {
    0: { label: '待分配', type: 'warning' },
    1: { label: '处理中', type: 'primary' },
    2: { label: '待确认', type: 'info' },
    3: { label: '已完成', type: 'success' },
    4: { label: '已驳回', type: 'danger' }
}

// 角色字典
export const roleMap = {
    0: { label: '学生', type: 'success' },
    1: { label: '维修工', type: 'warning' },
    2: { label: '管理员', type: 'danger' }
}

// 账号状态字典
export const userStatusMap = {
    0: { label: '已禁用', type: 'danger' },
    1: { label: '正常', type: 'success' }
}