<template>
  <div>
    <el-form inline class="filter">
      <el-form-item label="类型">
        <el-select v-model="query.type" clearable style="width: 130px">
          <el-option label="缴存" value="DEPOSIT" />
          <el-option label="使用" value="USE" />
          <el-option label="退款" value="REFUND" />
        </el-select>
      </el-form-item>
      <el-form-item label="方向">
        <el-select v-model="query.direction" clearable style="width: 100px">
          <el-option label="收入" value="IN" />
          <el-option label="支出" value="OUT" />
        </el-select>
      </el-form-item>
      <el-form-item label="账户号"><el-input v-model="query.accountNo" clearable style="width: 150px" /></el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
    </el-form>
    <el-table :data="list" border stripe>
      <el-table-column prop="flowNo" label="流水号" width="210" />
      <el-table-column prop="accountNo" label="账户号" width="130" />
      <el-table-column label="房号" width="100">
        <template #default="{ row }">{{ houseNo(row.houseId) }}</template>
      </el-table-column>
      <el-table-column label="类型" width="90">
        <template #default="{ row }">{{ typeMap[row.type] }}</template>
      </el-table-column>
      <el-table-column label="方向" width="70">
        <template #default="{ row }">
          <el-tag :type="row.direction === 'IN' ? 'success' : 'danger'">{{ row.direction === 'IN' ? '收入' : '支出' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="amount" label="金额(元)" width="110" />
      <el-table-column prop="balance" label="变动后余额" width="110" />
      <el-table-column prop="relatedNo" label="关联单号" width="210" />
      <el-table-column prop="remark" label="备注" min-width="140" />
      <el-table-column prop="bizTime" label="时间" width="170" />
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { queryFlows, listAccounts, listHouses } from '../../api'

const typeMap = { DEPOSIT: '缴存', USE: '使用', REFUND: '退款' }
const list = ref([])
const houses = ref([])
const accounts = ref([])
const query = reactive({ type: '', direction: '', accountNo: '' })

const houseNo = (id) => houses.value.find((h) => h.id === id)?.houseNo || id

async function load() {
  const accountId = accounts.value.find((a) => a.accountNo === query.accountNo)?.id
  list.value = await queryFlows({ ...query, accountId })
}
onMounted(async () => {
  houses.value = await listHouses({})
  accounts.value = await listAccounts({})
  load()
})
</script>
