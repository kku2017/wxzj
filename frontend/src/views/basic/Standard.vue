<template>
  <div>
    <el-form inline class="filter">
      <el-form-item label="小区">
        <el-select v-model="query.communityId" clearable style="width: 170px">
          <el-option v-for="c in communities" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.type" clearable style="width: 130px">
          <el-option label="初始缴存" value="INITIAL" />
          <el-option label="续缴" value="RENEWAL" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="auth.isAdmin" type="success" @click="open(null)">新增标准</el-button>
    </el-form>
    <el-table :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="小区" width="150">
        <template #default="{ row }">{{ communityName(row.communityId) }}</template>
      </el-table-column>
      <el-table-column prop="name" label="标准名称" />
      <el-table-column prop="unitPrice" label="单价(元/㎡)" width="110" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ row.type === 'INITIAL' ? '初始缴存' : '续缴' }}</template>
      </el-table-column>
      <el-table-column prop="effectiveDate" label="生效日期" width="120" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</template>
      </el-table-column>
      <el-table-column v-if="auth.isAdmin" label="操作" width="80">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑标准' : '新增标准'" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="所属小区" required>
          <el-select v-model="form.communityId" style="width: 100%">
            <el-option v-for="c in communities" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标准名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="单价(元/㎡)" required><el-input-number v-model="form.unitPrice" :min="0.01" :precision="2" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="初始缴存" value="INITIAL" />
            <el-option label="续缴" value="RENEWAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="生效日期"><el-date-picker v-model="form.effectiveDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listCommunities, listStandards, saveStandard } from '../../api'
import { useAuthStore } from '../../store/auth'

const auth = useAuthStore()
const list = ref([])
const communities = ref([])
const query = reactive({ communityId: null, type: '' })
const dialog = ref(false)
const form = reactive({})

const communityName = (id) => communities.value.find((c) => c.id === id)?.name || id
async function load() {
  list.value = await listStandards(query)
}
function open(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? { ...row } : { type: 'INITIAL', unitPrice: 0, status: 'ACTIVE' })
  dialog.value = true
}
async function save() {
  if (!form.communityId || !form.name || !form.unitPrice) return ElMessage.warning('请填写必填项')
  await saveStandard(form)
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}
onMounted(async () => {
  communities.value = await listCommunities({})
  load()
})
</script>
