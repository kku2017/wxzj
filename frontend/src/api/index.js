import request from './request'

// 认证
export const login = (data) => request.post('/auth/login', data)
export const me = () => request.get('/auth/me')

// 基础数据
export const listCommunities = (params) => request.get('/basic/community', { params })
export const saveCommunity = (data) => request.post('/basic/community', data)
export const deleteCommunity = (id) => request.delete(`/basic/community/${id}`)

export const listBuildings = (params) => request.get('/basic/building', { params })
export const saveBuilding = (data) => request.post('/basic/building', data)
export const deleteBuilding = (id) => request.delete(`/basic/building/${id}`)

export const listHouses = (params) => request.get('/basic/house', { params })
export const saveHouse = (data) => request.post('/basic/house', data)
export const deleteHouse = (id) => request.delete(`/basic/house/${id}`)
export const bindOwner = (id, ownerId) => request.post(`/basic/house/${id}/bind-owner`, null, { params: { ownerId } })

export const listOwners = (params) => request.get('/basic/owner', { params })
export const saveOwner = (data) => request.post('/basic/owner', data)

export const listStandards = (params) => request.get('/basic/standard', { params })
export const saveStandard = (data) => request.post('/basic/standard', data)

export const listAccounts = (params) => request.get('/basic/account', { params })

// 缴存
export const listDeposits = (params) => request.get('/deposit', { params })
export const createDeposit = (data) => request.post('/deposit', data)
export const confirmDeposit = (id) => request.post(`/deposit/${id}/confirm`)
export const cancelDeposit = (id) => request.post(`/deposit/${id}/cancel`)

// 使用
export const listUseApplies = (params) => request.get('/use', { params })
export const useItems = (id) => request.get(`/use/${id}/items`)
export const useProcess = (id) => request.get(`/use/${id}/process`)
export const createUseApply = (data) => request.post('/use', data)
export const submitUseApply = (id) => request.post(`/use/${id}/submit`)
export const approveUse = (id, data) => request.post(`/use/${id}/approve`, data)
export const payUse = (id) => request.post(`/use/${id}/pay`)

// 退款
export const listRefunds = (params) => request.get('/refund', { params })
export const refundProcess = (id) => request.get(`/refund/${id}/process`)
export const createRefund = (data) => request.post('/refund', data)
export const approveRefund = (id, data) => request.post(`/refund/${id}/approve`, data)
export const confirmRefund = (id) => request.post(`/refund/${id}/confirm`)

// 查询
export const queryFlows = (params) => request.get('/query/flow', { params })
export const queryStatistics = () => request.get('/query/statistics')

// 工作流配置
export const listFlowDefs = () => request.get('/workflow/def')
export const listFlowNodes = (id) => request.get(`/workflow/def/${id}/node`)
export const saveFlowNode = (data) => request.post('/workflow/node', data)
export const deleteFlowNode = (id) => request.delete(`/workflow/node/${id}`)
export const saveFlowDef = (data) => request.post('/workflow/def', data)
