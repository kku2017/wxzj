<template>
  <div>
    <el-form inline class="filter">
      <el-form-item label="小区">
        <el-select v-model="query.communityId" clearable style="width: 160px">
          <el-option v-for="c in communities" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width: 130px">
          <el-option v-for="(t, v) in statusMap" :key="v" :label="t" :value="v" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="auth.isAdmin || auth.isProperty" type="success" @click="openCreate">发起使用申请</el-button>
    </el-form>

    <el-table :data="list" border stripe>
      <el-table-column prop="applyNo" label="申请单号" width="200" />
      <el-table-column prop="title" label="维修项目" min-width="160" />
      <el-table-column label="小区" width="130">
        <template #default="{ row }">{{ communityName(row.communityId) }}</template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="总金额(元)" width="120" />
      <el-table-column prop="shareArea" label="分摊面积(㎡)" width="120" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusMap[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="applyTime" label="申请时间" width="170" />
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row)">明细/流程</el-button>
          <el-button v-if="row.status === 'DRAFT' && (auth.isAdmin || auth.isProperty)" link type="warning" @click="submit(row)">提交审批</el-button>
          <el-button v-if="row.status === 'APPROVED' && auth.isAdmin" link type="success" @click="pay(row)">拨付</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createDialog" title="发起使用申请" width="600px">
      <el-form label-width="90px">
        <el-form-item label="小区" required>
          <el-select v-model="form.communityId" style="width: 100%" @change="loadHouses">
            <el-option v-for="c in communities" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目名称" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="维修原因"><el-input v-model="form.reason" /></el-form-item>
        <el-form-item label="项目内容"><el-input v-model="form.itemDesc" type="textarea" /></el-form-item>
        <el-form-item label="总金额(元)" required><el-input-number v-model="form.totalAmount" :min="0.01" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="涉及房屋" required>
          <el-select v-model="form.houseIds" multiple filterable style="width: 100%" placeholder="选择房屋（按面积分摊）">
            <el-option v-for="h in houses" :key="h.id" :label="houseLabel(h)" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" @click="create">提交</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailDrawer" :title="`明细 / 审批流程`" size="560px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="申请单号">{{ detail.applyNo }}</el-descriptions-item>
          <el-descriptions-item label="项目">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusMap[detail.status] }}</el-descriptions-item>
        </el-descriptions>
        <h4>分摊明细</h4>
        <el-table :data="items" size="small" border>
          <el-table-column prop="houseNo" label="房号" width="100" />
          <el-table-column prop="shareArea" label="面积(㎡)" width="100" />
          <el-table-column prop="shareAmount" label="分摊金额" width="120" />
        </el-table>
        <h4>审批记录</h4>
        <el-steps direction="vertical" :active="activeStep" v-if="proc">
          <el-step v-for="n in proc.nodes" :key="n.nodeNo" :title="n.nodeName" :description="stepDesc(n)" />
        </el-steps>
        <div v-if="proc && proc.canApprove" style="margin-top:16px">
          <el-alert type="info" :title="`请审批当前节点：${proc.currentNode?.nodeName}`" :closable="false" show-icon />
          <el-input v-model="opinion" placeholder="审批意见" style="margin:12px 0" />
          <el-button type="success" @click="doApprove(detail.id, 'PASS')">通过</el-button>
          <el-button type="danger" @click="doApprove(detail.id, 'REJECT')">驳回</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCommunities, listHouses, listUseApplies, createUseApply, submitUseApply, useItems, useProcess, approveUse, payUse } from '../../api'
import { useAuthStore } from '../../store/auth'

const auth = useAuthStore()
const statusMap = { DRAFT: '草稿', PENDING: '审批中', APPROVED: '已批准', PAID: '已拨付', REJECTED: '已驳回' }
const statusType = (s) => ({ DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', PAID: 'primary', REJECTED: 'danger' }[s])
const list = ref([])
const communities = ref([])
const houses = ref([])
const items = ref([])
const proc = ref(null)
const detail = ref(null)
const query = reactive({ communityId: null, status: '' })
const form = reactive({})
const createDialog = ref(false)
const detailDrawer = ref(false)
const opinion = ref('')

const communityName = (id) => communities.value.find((c) => c.id === id)?.name || id
const houseLabel = (h) => `${h.houseNo}（${h.area}㎡）`
const activeStep = () => (proc.value?.currentNode ? proc.value.currentNode.nodeNo - 1 : 0)

async function load() {
  list.value = await listUseApplies(query)
}
async function loadHouses() {
  houses.value = form.communityId ? await listHouses({ communityId: form.communityId }) : []
}
function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  form.houseIds = []
  form.totalAmount = 0
  createDialog.value = true
}
async function create() {
  if (!form.communityId || !form.title || !form.totalAmount || !form.houseIds?.length) return ElMessage.warning('请填写必填项')
  await createUseApply(form)
  ElMessage.success('草稿已创建，请提交审批')
  createDialog.value = false
  load()
}
async function submit(row) {
  await ElMessageBox.confirm('提交后进入审批流程，确定提交？', '提示', { type: 'warning' })
  await submitUseApply(row.id)
  ElMessage.success('已提交审批')
  load()
}
async function pay(row) {
  await ElMessageBox.confirm(`拨付 ${row.totalAmount} 元并扣减各户分摊金额？`, '拨付确认', { type: 'warning' })
  await payUse(row.id)
  ElMessage.success('拨付完成')
  load()
}
async function showDetail(row) {
  detail.value = row
  items.value = await useItems(row.id)
  proc.value = await useProcess(row.id)
  detailDrawer.value = true
}
function stepDesc(n) {
  const ap = proc.value?.approvals?.find((a) => a.nodeNo === n.nodeNo)
  if (ap) return `${ap.action === 'PASS' ? '通过' : '驳回'}（${ap.approverName}）：${ap.opinion || '-'}`
  if (proc.value?.currentNode?.nodeNo === n.nodeNo) return '待审批'
  return ''
}
async function doApprove(id, action) {
  await approveUse(id, { action, opinion: opinion.value })
  ElMessage.success(action === 'PASS' ? '审批通过' : '已驳回')
  opinion.value = ''
  load()
  showDetail(detail.value)
}
onMounted(async () => {
  communities.value = await listCommunities({})
  load()
})
</script>
