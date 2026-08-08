<template>
  <div>
    <el-form inline class="filter">
      <el-form-item label="小区">
        <el-select v-model="query.communityId" clearable placeholder="全部" style="width: 180px">
          <el-option v-for="c in communities" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="楼栋号"><el-input v-model="query.buildingNo" clearable /></el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="auth.isAdmin" type="success" @click="open(null)">新增楼栋</el-button>
    </el-form>
    <el-table :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="小区" width="160">
        <template #default="{ row }">{{ communityName(row.communityId) }}</template>
      </el-table-column>
      <el-table-column prop="buildingNo" label="楼栋号" width="100" />
      <el-table-column prop="name" label="楼栋名称" />
      <el-table-column prop="floors" label="楼层数" width="90" />
      <el-table-column prop="area" label="建筑面积(㎡)" width="120" />
      <el-table-column v-if="auth.isAdmin" label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑楼栋' : '新增楼栋'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="所属小区" required>
          <el-select v-model="form.communityId" style="width: 100%">
            <el-option v-for="c in communities" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋号" required><el-input v-model="form.buildingNo" /></el-form-item>
        <el-form-item label="楼栋名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="楼层数"><el-input-number v-model="form.floors" :min="1" /></el-form-item>
        <el-form-item label="建筑面积"><el-input-number v-model="form.area" :min="0" /></el-form-item>
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
import { listCommunities, listBuildings, saveBuilding, deleteBuilding } from '../../api'
import { useAuthStore } from '../../store/auth'

const auth = useAuthStore()
const list = ref([])
const communities = ref([])
const query = reactive({ communityId: null, buildingNo: '' })
const dialog = ref(false)
const form = reactive({})

async function load() {
  list.value = await listBuildings(query)
}
const communityName = (id) => communities.value.find((c) => c.id === id)?.name || id
async function loadCommunities() {
  communities.value = await listCommunities({})
}
function open(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row || { floors: 6, area: 0 })
  dialog.value = true
}
async function save() {
  if (!form.communityId || !form.buildingNo) return ElMessage.warning('请填写小区与楼栋号')
  await saveBuilding(form)
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}
async function remove(row) {
  await ElMessageBox.confirm(`确定删除楼栋「${row.name}」？`, '提示', { type: 'warning' })
  await deleteBuilding(row.id)
  ElMessage.success('删除成功')
  load()
}
onMounted(() => { load(); loadCommunities() })
</script>
