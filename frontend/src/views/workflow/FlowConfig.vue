<template>
  <div>
    <el-card>
      <template #header>审批流程配置（管理员）</template>
      <el-table :data="defs" border stripe>
        <el-table-column prop="code" label="流程编码" width="110" />
        <el-table-column prop="name" label="流程名称" />
        <el-table-column label="节点数" width="90">
          <template #default="{ row }">{{ nodeMap[row.id]?.length || 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card style="margin-top:16px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>流程节点</span>
          <el-select v-model="currentDefId" style="width:220px" @change="loadNodes">
            <el-option v-for="d in defs" :key="d.id" :label="`${d.name}（${d.code}）`" :value="d.id" />
          </el-select>
        </div>
      </template>
      <el-table :data="nodes" border stripe>
        <el-table-column prop="nodeNo" label="顺序" width="70" />
        <el-table-column prop="nodeName" label="节点名称" />
        <el-table-column label="审批角色" width="140">
          <template #default="{ row }">{{ roleMap[row.approverRole] || row.approverRole }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button type="success" style="margin-top:12px" @click="open(null)">新增节点</el-button>
    </el-card>

    <el-dialog v-model="dialog" :title="form.id ? '编辑节点' : '新增节点'" width="440px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="流程" required>
          <el-select v-model="form.flowDefId" style="width: 100%" :disabled="!!form.id">
            <el-option v-for="d in defs" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="顺序" required><el-input-number v-model="form.nodeNo" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="节点名称" required><el-input v-model="form.nodeName" /></el-form-item>
        <el-form-item label="审批角色" required>
          <el-select v-model="form.approverRole" style="width: 100%">
            <el-option v-for="(t, v) in roleMap" :key="v" :label="t" :value="v" />
          </el-select>
        </el-form-item>
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
import { listFlowDefs, listFlowNodes, saveFlowNode, deleteFlowNode } from '../../api'

const roleMap = { ADMIN: '系统管理员', PROPERTY: '物业操作员', COMMITTEE: '业委会', OWNER: '业主' }
const defs = ref([])
const nodes = ref([])
const nodeMap = ref({})
const currentDefId = ref(null)
const dialog = ref(false)
const form = reactive({})

async function load() {
  defs.value = await listFlowDefs()
  for (const d of defs.value) {
    nodeMap.value[d.id] = await listFlowNodes(d.id)
  }
  if (!currentDefId.value && defs.value.length) currentDefId.value = defs.value[0].id
  loadNodes()
}
async function loadNodes() {
  if (!currentDefId.value) return
  nodes.value = await listFlowNodes(currentDefId.value)
}
function open(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? { ...row } : { flowDefId: currentDefId.value, nodeNo: (nodes.value.length || 0) + 1, approverRole: 'ADMIN' })
  dialog.value = true
}
async function save() {
  if (!form.flowDefId || !form.nodeName || !form.approverRole) return ElMessage.warning('请填写必填项')
  await saveFlowNode(form)
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}
async function remove(row) {
  await ElMessageBox.confirm(`确定删除节点「${row.nodeName}」？`, '提示', { type: 'warning' })
  await deleteFlowNode(row.id)
  ElMessage.success('删除成功')
  load()
}
onMounted(load)
</script>
