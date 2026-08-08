<template>
  <div>
    <el-form inline class="filter">
      <el-form-item label="小区">
        <el-select v-model="query.communityId" clearable style="width: 160px">
          <el-option v-for="c in communities" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width: 120px">
          <el-option label="待缴" value="PENDING" />
          <el-option label="已缴" value="PAID" />
          <el-option label="作废" value="CANCELLED" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="dialog = true">缴存登记</el-button>
    </el-form>
    <el-table :data="list" border stripe>
      <el-table-column prop="orderNo" label="缴存单号" width="200" />
      <el-table-column prop="houseNo" label="房号" width="100" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">{{ typeText(row.type) }}</template>
      </el-table-column>
      <el-table-column prop="quantity" label="面积(㎡)" width="100" />
      <el-table-column prop="unitPrice" label="单价" width="90" />
      <el-table-column prop="amount" label="金额(元)" width="120" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PAID' ? 'success' : row.status === 'CANCELLED' ? 'info' : 'warning'">
            {{ { PENDING: '待缴', PAID: '已缴', CANCELLED: '作废' }[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="payTime" label="到账时间" width="170" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" link type="success" @click="confirm(row)">到账确认</el-button>
          <el-button v-if="row.status === 'PENDING'" link type="danger" @click="cancel(row)">作废</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" title="缴存登记" width="560px">
      <el-form label-width="90px">
        <el-form-item label="小区">
          <el-select v-model="form.communityId" style="width: 100%" @change="onCommunityChange">
            <el-option v-for="c in communities" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房屋">
          <el-select v-model="form.houseId" filterable style="width: 100%" placeholder="选择房屋" @change="onHouseChange">
            <el-option v-for="h in houses" :key="h.id" :label="`${houseLabel(h)}`" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="缴存类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="初始缴存" value="INITIAL" />
            <el-option label="续缴" value="RENEWAL" />
            <el-option label="补缴" value="SUPPLEMENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="面积(㎡)"><el-input :model-value="house?.area" disabled /></el-form-item>
        <el-form-item label="标准单价"><el-input :model-value="standardPriceText" disabled /></el-form-item>
        <el-form-item label="应缴金额(元)">
          <b style="color:#e6a23c;font-size:18px">{{ estimated }}</b>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">登记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCommunities, listBuildings, listHouses, listStandards, listDeposits, createDeposit, confirmDeposit, cancelDeposit } from '../../api'

const list = ref([])
const communities = ref([])
const houses = ref([])
const standards = ref([])
const query = reactive({ communityId: null, status: '' })
const dialog = ref(false)
const form = reactive({ communityId: null, houseId: null, type: 'INITIAL', standardId: null, remark: '' })

const house = computed(() => houses.value.find((h) => h.id === form.houseId))
const standardPrice = computed(() => {
  const s = standards.value.find((x) => x.id === form.standardId)
  return s ? Number(s.unitPrice) : 0
})
const standardPriceText = computed(() => {
  const s = standards.value.find((x) => x.id === form.standardId)
  return s ? `${s.unitPrice} 元/㎡（${s.name}）` : '未选标准（默认取启用标准）'
})
const estimated = computed(() => {
  if (!house.value) return '-'
  const price = standardPrice.value || standards.value.find((s) => s.communityId === form.communityId && s.status === 'ACTIVE')?.unitPrice || 0
  return (Number(house.value.area) * Number(price)).toFixed(2) + ' 元'
})
const typeText = (t) => ({ INITIAL: '初始缴存', RENEWAL: '续缴', SUPPLEMENT: '补缴' }[t] || t)
const houseLabel = (h) => `${h.houseNo}（${h.area}㎡）`

async function load() {
  list.value = await listDeposits(query)
}
async function loadHouses() {
  houses.value = form.communityId ? await listHouses({ communityId: form.communityId }) : []
  form.houseId = null
}
function onCommunityChange() {
  loadHouses()
  standards.value = form.communityId ? awaitStandards(form.communityId) : []
  form.standardId = null
}
function onHouseChange() {
  if (!form.standardId) {
    const s = standards.value.find((x) => x.communityId === form.communityId && x.status === 'ACTIVE')
    if (s) form.standardId = s.id
  }
}
async function awaitStandards(cid) {
  return await listStandards({ communityId: cid, status: 'ACTIVE' })
}
watch(() => form.type, (t) => {
  if (!form.standardId) {
    const s = standards.value.find((x) => x.communityId === form.communityId && x.type === t && x.status === 'ACTIVE')
    if (s) form.standardId = s.id
  }
})

async function save() {
  if (!form.houseId) return ElMessage.warning('请选择房屋')
  await createDeposit(form)
  ElMessage.success('登记成功，状态待缴')
  dialog.value = false
  load()
}
async function confirm(row) {
  await ElMessageBox.confirm(`确认 ${row.amount} 元已到账？`, '到账确认', { type: 'warning' })
  await confirmDeposit(row.id)
  ElMessage.success('到账确认成功')
  load()
}
async function cancel(row) {
  await ElMessageBox.confirm('确定作废该缴存单？', '提示', { type: 'warning' })
  await cancelDeposit(row.id)
  ElMessage.success('已作废')
  load()
}
onMounted(async () => {
  communities.value = await listCommunities({})
  load()
})
</script>
