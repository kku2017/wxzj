<template>
  <div>
    <el-form inline class="filter">
      <el-form-item label="小区名称"><el-input v-model="query.name" placeholder="模糊查询" clearable /></el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="auth.isAdmin" type="success" @click="open(null)">新增小区</el-button>
    </el-form>
    <el-table :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="小区名称" />
      <el-table-column prop="address" label="地址" />
      <el-table-column prop="developer" label="开发商" />
      <el-table-column prop="buildYear" label="建成年份" width="90" />
      <el-table-column prop="area" label="总面积(㎡)" width="110" />
      <el-table-column prop="houseCount" label="房屋数" width="80" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</template>
      </el-table-column>
      <el-table-column v-if="auth.isAdmin" label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑小区' : '新增小区'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="小区名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="开发商"><el-input v-model="form.developer" /></el-form-item>
        <el-form-item label="建成年份"><el-input-number v-model="form.buildYear" :min="1900" :max="2100" /></el-form-item>
        <el-form-item label="总面积(㎡)"><el-input-number v-model="form.area" :min="0" /></el-form-item>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCommunities, saveCommunity, deleteCommunity } from '../../api'
import { useAuthStore } from '../../store/auth'

const auth = useAuthStore()
const list = ref([])
const query = reactive({ name: '' })
const dialog = ref(false)
const form = reactive({})

async function load() {
  list.value = await listCommunities(query)
}
function open(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row || { buildYear: new Date().getFullYear(), area: 0 })
  dialog.value = true
}
async function save() {
  await saveCommunity(form)
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}
async function remove(row) {
  await ElMessageBox.confirm(`确定删除小区「${row.name}」？`, '提示', { type: 'warning' })
  await deleteCommunity(row.id)
  ElMessage.success('删除成功')
  load()
}
onMounted(load)
</script>
