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
      <el-button type="success" @click="openCreate">发起退款申请</el-button>
    </el-form>

    <el-table :data="list" border stripe>
      <el-table-column prop="refundNo" label="退款单号" width="200" />
      <el-table-column label="小区" width="130">
        <template #default="{ row }">{{ communityName(row.communityId) }}</template>
      </el-table-column>
      <el-table-column label="房号" width="100">
        <template #default="{ row }">{{ houseNo(row.houseId) }}</template>
      </el-table-column>
      <el-table-column label="原因" width="110">
        <template #default="{ row }">{{ reasonMap[row.reason] || row.reason }}</template>
      </el-table-column>
      <el-table-column prop="amount" label="金额(元)" width="120" />
      <el-table-column prop="balanceAtApply" label="申请时余额" width="120" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusMap[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="applyTime" label="申请时间" width="170" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row)">流程</el-button>
          <el-button v-if="row.status === 'APPROVED' && auth.isAdmin" link type="success" @click="confirm(row)">办理退款</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createDialog" title="发起退款申请" width="480px">
      <el-form label-width="90px">
        <el-form-item label="资金账户" required>
          <el-select v-model="form.accountId" filterable style="width: 100%" placeholder="选择本人账户">
            <el-option v-for="a in accounts" :key="a.id" :label="`${a.accountNo}（余额 ${a.balance}）`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="退款原因" required>
          <el-select v-model="form.reason" style="width: 100%">
            <el-option v-for="(t, v) in reasonMap" :key="v" :label="t" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item label="退款金额" required><el-input-number v-model="form.amount" :min="0.01" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" @click="create">提交</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailDrawer" title="审批流程" size="480px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="退款单号">{{ detail.refundNo }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ detail.amount }} 元</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusMap[detail.status] }}</el-descriptions-item>
        </el-descriptions>
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
import { listCommunities, listAccounts, listHouses, listRefunds, createRefund, refundProcess, approveRefund, confirmRefund } from '../../api'
import { useAuthStore } from '../../store/auth'

const auth = useAuthStore()
const statusMap = { PENDING: '审批中', APPROVED: '已批准', REFUNDED: '已退款', REJECTED: '已驳回' }
const statusType = (s) => ({ PENDING: 'warning', APPROVED: 'success', REFUNDED: 'primary', REJECTED: 'danger' }[s])
const reasonMap = { TRANSFER: '产权转移', DEMOLITION: '房屋灭失', OVERPAY: '多缴误缴' }
const list = ref([])
const communities = ref([])
const accounts = ref([])
const houses = ref([])
const proc = ref(null)
const detail = ref(null)
const query = reactive({ communityId: null, status: '' })
const form = reactive({})
const createDialog = ref(false)
const detailDrawer = ref(false)
const opinion = ref('')

const communityName = (id) => communities.value.find((c) => c.id === id)?.name || id
const houseNo = (id) => houses.value.find((h) => h.id === id)?.houseNo || id
const activeStep = () => (proc.value?.currentNode ? proc.value.currentNode.nodeNo - 1 : 0)

async function load() {
  list.value = await listRefunds(query)
}
function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  form.amount = 0
  createDialog.value = true
}
async function create() {
  if (!form.accountId || !form.reason || !form.amount) return ElMessage.warning('请填写必填项')
  await createRefund(form)
  ElMessage.success('已提交审批')
  createDialog.value = false
  load()
}
async function confirm(row) {
  await ElMessageBox.confirm(`确认办理退款 ${row.amount} 元？`, '提示', { type: 'warning' })
  await confirmRefund(row.id)
  ElMessage.success('退款完成')
  load()
}
async function showDetail(row) {
  detail.value = row
  proc.value = await refundProcess(row.id)
  detailDrawer.value = true
}
function stepDesc(n) {
  const ap = proc.value?.approvals?.find((a) => a.nodeNo === n.nodeNo)
  if (ap) return `${ap.action === 'PASS' ? '通过' : '驳回'}（${ap.approverName}）：${ap.opinion || '-'}`
  if (proc.value?.currentNode?.nodeNo === n.nodeNo) return '待审批'
  return ''
}
async function doApprove(id, action) {
  await approveRefund(id, { action, opinion: opinion.value })
  ElMessage.success(action === 'PASS' ? '审批通过' : '已驳回')
  opinion.value = ''
  load()
  showDetail(detail.value)
}
onMounted(async () => {
  communities.value = await listCommunities({})
  houses.value = await listHouses({})
  accounts.value = await listAccounts({})
  load()
})
</script>
