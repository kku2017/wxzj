<template>
  <div>
    <el-card shadow="never">
      <template #header>历史数据迁移</template>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px"
        title="支持上传手工记账导出的 house_info.csv（UTF-8/GBK 均可）。系统按小区/楼栋/房屋自动补齐档案，按证件号码绑定业主（支持一房多主），摘要 开户/缴存/分红/支用/退款 分别落为账户开户、缴存单与资金流水，并自动校验金额勾稽。"
        description="模板见 migration/house_info_template.csv。摘要建议：开户、缴存、支用、退款、分红；其他文本将记为失败行。" />
      <el-upload drag :auto-upload="false" :limit="1" accept=".csv" :on-change="onChange" :on-remove="() => (file = null)">
        <el-icon style="font-size: 40px; color: #909399"><UploadFilled /></el-icon>
        <div class="el-upload__text">将 CSV 拖到此处，或 <em>点击选择文件</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .csv 文件，建议使用模板填写后另存为 CSV（UTF-8 带 BOM）。</div>
        </template>
      </el-upload>
      <div style="margin-top: 16px; display: flex; gap: 12px">
        <el-button type="primary" :disabled="!file" :loading="loadingPreview" @click="doPreview">预览</el-button>
        <el-button type="success" :disabled="!file" :loading="loadingRun" @click="doRun">执行迁移</el-button>
        <el-button :disabled="!file" @click="reset">清空</el-button>
        <el-button @click="downloadTemplate">下载模板</el-button>
      </div>
    </el-card>

    <el-card v-if="preview" shadow="never" style="margin-top: 16px">
      <template #header>预览结果（共 {{ preview.totalRows }} 行）</template>
      <div style="margin-bottom: 8px">
        <el-tag v-for="h in preview.headers" :key="h" style="margin-right: 6px">{{ h }}</el-tag>
      </div>
      <el-table :data="preview.sample" border stripe max-height="360" size="small">
        <el-table-column v-for="h in preview.headers" :key="h" :prop="h" :label="h" min-width="110" />
      </el-table>
      <div v-if="preview.sample.length < preview.totalRows" style="margin-top: 8px; color: #909399">
        仅展示前 {{ preview.sample.length }} 行，完整 {{ preview.totalRows }} 行将全部迁移。
      </div>
    </el-card>

    <el-card v-if="report" shadow="never" style="margin-top: 16px">
      <template #header>迁移报告</template>
      <el-descriptions :column="4" border>
        <el-descriptions-item label="总行数">{{ report.totalRows }}</el-descriptions-item>
        <el-descriptions-item label="成功行">
          {{ report.totalRows - report.errorRows - report.skipped }}
        </el-descriptions-item>
        <el-descriptions-item label="跳过(重复发票)">
          <el-tag type="warning">{{ report.skipped }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="失败行">
          <el-tag type="danger">{{ report.errorRows }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="新增小区">{{ report.communityCreated }}</el-descriptions-item>
        <el-descriptions-item label="新增楼栋">{{ report.buildingCreated }}</el-descriptions-item>
        <el-descriptions-item label="新增房屋">{{ report.houseCreated }}</el-descriptions-item>
        <el-descriptions-item label="新增业主">{{ report.ownerCreated }}</el-descriptions-item>
        <el-descriptions-item label="新增账户">{{ report.accountCreated }}</el-descriptions-item>
        <el-descriptions-item label="新增缴存单">{{ report.depositCreated }}</el-descriptions-item>
        <el-descriptions-item label="新增资金流水">{{ report.flowCreated }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="report.errors.length" style="margin-top: 16px">
        <el-alert type="error" :closable="false" title="以下行未迁移，请修正后重新导入："
          :description="report.errors.join('；')" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { migratePreview, migrateExecute } from '../../api'

const file = ref(null)
const preview = ref(null)
const report = ref(null)
const loadingPreview = ref(false)
const loadingRun = ref(false)

function onChange(uploadFile) {
  file.value = uploadFile.raw
  preview.value = null
  report.value = null
}

async function doPreview() {
  loadingPreview.value = true
  try {
    preview.value = await migratePreview(file.value)
    report.value = null
  } finally {
    loadingPreview.value = false
  }
}

async function doRun() {
  await ElMessageBox.confirm('确认执行迁移？将自动创建小区/楼栋/房屋/业主/账户并生成缴存单与流水，重复发票将跳过。', '提示', { type: 'warning' })
  loadingRun.value = true
  try {
    report.value = await migrateExecute(file.value)
    preview.value = null
    if (report.value.errorRows === 0) {
      ElMessage.success(`迁移完成，共生成 ${report.value.flowCreated} 条流水`)
    } else {
      ElMessage.warning(`迁移完成，${report.value.errorRows} 行失败`)
    }
  } finally {
    loadingRun.value = false
  }
}

function reset() {
  file.value = null
  preview.value = null
  report.value = null
}

function downloadTemplate() {
  const header = '小区名称,楼,单元,房屋,业主姓名,证件号码,证件类型,电梯标志,摘要,交易时间,交易金额,发票号,面积'
  const example = '锦绣花园,1,1,101,李强,110101198001011234,身份证,有,缴存,2024-01-10,10000,F1001,89.50'
  const blob = new Blob(['\ufeff' + header + '\r\n' + example + '\r\n'], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'house_info_template.csv'
  a.click()
  URL.revokeObjectURL(url)
}
</script>
