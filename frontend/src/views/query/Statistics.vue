<template>
  <div>
    <el-card>
      <template #header>维修资金统计报表（按小区汇总）</template>
      <el-table :data="list" border stripe>
        <el-table-column prop="communityName" label="小区" width="160" />
        <el-table-column prop="accountCount" label="账户数" width="90" />
        <el-table-column label="缴存合计(元)" width="140">
          <template #default="{ row }">{{ money(row.totalDeposit) }}</template>
        </el-table-column>
        <el-table-column label="使用合计(元)" width="140">
          <template #default="{ row }">{{ money(row.totalUsed) }}</template>
        </el-table-column>
        <el-table-column label="退款合计(元)" width="140">
          <template #default="{ row }">{{ money(row.totalRefund) }}</template>
        </el-table-column>
        <el-table-column label="当前余额(元)" width="150">
          <template #default="{ row }"><b style="color:#409eff">{{ money(row.totalBalance) }}</b></template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-card style="margin-top:16px">
      <template #header>缴存 / 使用 / 退款占比</template>
      <div class="bars">
        <div class="bar-row" v-for="row in list" :key="row.communityId">
          <span class="bar-label">{{ row.communityName }}</span>
          <el-progress :percentage="pct(row.totalDeposit)" :format="() => `缴存 ${money(row.totalDeposit)}`" :stroke-width="18" />
          <el-progress :percentage="pct(row.totalUsed)" :format="() => `使用 ${money(row.totalUsed)}`" status="success" :stroke-width="18" />
          <el-progress :percentage="pct(row.totalRefund)" :format="() => `退款 ${money(row.totalRefund)}`" status="warning" :stroke-width="18" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { queryStatistics } from '../../api'

const list = ref([])
const money = (v) => Number(v || 0).toFixed(2)
const max = ref(0)

function pct(v) {
  return max.value ? Math.round((Number(v) / max.value) * 100) : 0
}
onMounted(async () => {
  list.value = await queryStatistics()
  max.value = Math.max(...list.value.map((r) => Number(r.totalDeposit || 0)), 1)
})
</script>

<style scoped>
.bars { max-width: 900px; }
.bar-row { margin-bottom: 18px; }
.bar-label { display: inline-block; width: 120px; font-weight: 600; }
</style>
