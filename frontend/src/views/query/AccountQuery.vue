<template>
  <div>
    <el-form inline class="filter">
      <el-form-item label="小区">
        <el-select v-model="query.communityId" clearable style="width: 160px">
          <el-option v-for="c in communities" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="房号"><el-input v-model="query.houseNo" clearable style="width: 130px" /></el-form-item>
      <el-form-item label="账户号"><el-input v-model="query.accountNo" clearable style="width: 160px" /></el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
    </el-form>
    <el-table :data="list" border stripe>
      <el-table-column prop="accountNo" label="账户号" width="140" />
      <el-table-column label="小区" width="130">
        <template #default="{ row }">{{ communityName(row.communityId) }}</template>
      </el-table-column>
      <el-table-column label="楼栋/房号" width="120">
        <template #default="{ row }">{{ houseLabel(row.houseId) }}</template>
      </el-table-column>
      <el-table-column prop="balance" label="当前余额(元)" width="130" sortable>
        <template #default="{ row }"><b style="color:#409eff">{{ row.balance }}</b></template>
      </el-table-column>
      <el-table-column prop="totalDeposit" label="累计缴存" width="120" />
      <el-table-column prop="totalUsed" label="累计使用" width="120" />
      <el-table-column prop="totalRefund" label="累计退款" width="120" />
      <el-table-column prop="openTime" label="开户时间" width="170" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">{{ statusMap[row.status] || row.status }}</template>
      </el-table-column>
    </el-table>
    <el-descriptions title="合计" :column="4" border style="margin-top:16px">
      <el-descriptions-item label="账户数">{{ list.length }}</el-descriptions-item>
      <el-descriptions-item label="余额合计">{{ total('balance') }}</el-descriptions-item>
      <el-descriptions-item label="缴存合计">{{ total('totalDeposit') }}</el-descriptions-item>
      <el-descriptions-item label="使用合计">{{ total('totalUsed') }}</el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { listCommunities, listHouses, listAccounts } from '../../api'

const statusMap = { ACTIVE: '正常', FROZEN: '冻结', CLOSED: '注销' }
const list = ref([])
const communities = ref([])
const houses = ref([])
const query = reactive({ communityId: null, houseNo: '', accountNo: '' })

const communityName = (id) => communities.value.find((c) => c.id === id)?.name || id
const houseLabel = (id) => {
  const h = houses.value.find((x) => x.id === id)
  return h ? `${h.houseNo}` : id
}
const total = (key) => list.value.reduce((s, r) => s + Number(r[key] || 0), 0).toFixed(2)

async function load() {
  list.value = await listAccounts(query)
}
onMounted(async () => {
  communities.value = await listCommunities({})
  houses.value = await listHouses({})
  load()
})
</script>
