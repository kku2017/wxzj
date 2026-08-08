<template>
  <div>
    <el-form inline class="filter">
      <el-form-item label="姓名"><el-input v-model="query.name" clearable /></el-form-item>
      <el-form-item label="电话"><el-input v-model="query.phone" clearable /></el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="auth.isAdmin" type="success" @click="open(null)">新增业主</el-button>
    </el-form>
    <el-table :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="gender" label="性别" width="70" />
      <el-table-column prop="idCard" label="身份证号" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column prop="address" label="地址" />
      <el-table-column v-if="auth.isAdmin" label="操作" width="80">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑业主' : '新增业主'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="身份证号" required><el-input v-model="form.idCard" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" style="width: 100%">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
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
import { listOwners, saveOwner } from '../../api'
import { useAuthStore } from '../../store/auth'

const auth = useAuthStore()
const list = ref([])
const query = reactive({ name: '', phone: '' })
const dialog = ref(false)
const form = reactive({})

async function load() {
  list.value = await listOwners(query)
}
function open(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? { ...row } : { gender: '男' })
  dialog.value = true
}
async function save() {
  if (!form.name || !form.idCard) return ElMessage.warning('请填写姓名与身份证号')
  try {
    await saveOwner(form)
  } catch (e) { return }
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}
onMounted(load)
</script>
