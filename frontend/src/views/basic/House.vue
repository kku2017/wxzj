<template>
  <div>
    <el-form inline class="filter">
      <el-form-item label="小区">
        <el-select v-model="query.communityId" clearable style="width: 170px" @change="query.buildingId = null">
          <el-option v-for="c in communities" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="楼栋">
        <el-select v-model="query.buildingId" clearable style="width: 140px" :disabled="!query.communityId">
          <el-option v-for="b in buildings" :key="b.id" :label="b.buildingNo" :value="b.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="房号"><el-input v-model="query.houseNo" clearable /></el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="auth.isAdmin" type="success" @click="open(null)">新增房屋</el-button>
    </el-form>
    <el-table :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="小区" width="130">
        <template #default="{ row }">{{ communityName(row.communityId) }}</template>
      </el-table-column>
      <el-table-column label="楼栋" width="80">
        <template #default="{ row }">{{ buildingNo(row.buildingId) }}</template>
      </el-table-column>
      <el-table-column prop="houseNo" label="房号" width="100" />
      <el-table-column prop="floor" label="楼层" width="70" />
      <el-table-column prop="area" label="面积(㎡)" width="100" />
      <el-table-column label="业主" width="130">
        <template #default="{ row }">{{ ownerName(row.ownerId) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">{{ row.status === 'ACTIVE' ? '已售' : '空置' }}</template>
      </el-table-column>
      <el-table-column v-if="auth.isAdmin" label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-button link type="warning" @click="bind(row)">绑定业主</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑房屋' : '新增房屋'" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="所属小区" required>
          <el-select v-model="form.communityId" style="width: 100%" @change="form.buildingId = null">
            <el-option v-for="c in communities" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属楼栋" required>
          <el-select v-model="form.buildingId" style="width: 100%" :disabled="!form.communityId">
            <el-option v-for="b in buildings" :key="b.id" :label="b.buildingNo + (b.name ? ' ' + b.name : '')" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房号" required><el-input v-model="form.houseNo" placeholder="如 1-101" /></el-form-item>
        <el-form-item label="楼层"><el-input-number v-model="form.floor" :min="1" /></el-form-item>
        <el-form-item label="面积(㎡)" required><el-input-number v-model="form.area" :min="0.1" :precision="2" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="已售" value="ACTIVE" />
            <el-option label="空置" value="EMPTY" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bindDialog" title="绑定业主" width="420px">
      <el-select v-model="bindOwnerId" filterable placeholder="选择业主" style="width: 100%">
        <el-option v-for="o in owners" :key="o.id" :label="`${o.name}（${o.phone}）`" :value="o.id" />
      </el-select>
      <template #footer>
        <el-button @click="bindDialog = false">取消</el-button>
        <el-button type="primary" @click="doBind">绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCommunities, listBuildings, listHouses, saveHouse, deleteHouse, bindOwner, listOwners } from '../../api'
import { useAuthStore } from '../../store/auth'

const auth = useAuthStore()
const list = ref([])
const communities = ref([])
const buildings = ref([])
const owners = ref([])
const query = reactive({ communityId: null, buildingId: null, houseNo: '' })
const dialog = ref(false)
const form = reactive({})
const bindDialog = ref(false)
const bindOwnerId = ref(null)
const bindTarget = ref(null)

const communityName = (id) => communities.value.find((c) => c.id === id)?.name || id
const buildingNo = (id) => buildings.value.find((b) => b.id === id)?.buildingNo || id
const ownerName = (id) => owners.value.find((o) => o.id === id)?.name || '-'

async function load() {
  list.value = await listHouses(query)
}
async function loadBuildings() {
  buildings.value = query.communityId ? await listBuildings({ communityId: query.communityId }) : []
}
watch(() => query.communityId, loadBuildings)

function open(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? { ...row } : { communityId: query.communityId, buildingId: query.buildingId, floor: 1, area: 0, status: 'ACTIVE' })
  dialog.value = true
}
async function save() {
  if (!form.communityId || !form.buildingId || !form.houseNo || !form.area) return ElMessage.warning('请填写必填项')
  await saveHouse(form)
  ElMessage.success('保存成功（自动开户）')
  dialog.value = false
  load()
}
function bind(row) {
  bindTarget.value = row
  bindOwnerId.value = null
  bindDialog.value = true
}
async function doBind() {
  if (!bindOwnerId.value) return ElMessage.warning('请选择业主')
  await bindOwner(bindTarget.value.id, bindOwnerId.value)
  ElMessage.success('绑定成功')
  bindDialog.value = false
  load()
}
async function remove(row) {
  await ElMessageBox.confirm(`确定删除房屋「${row.houseNo}」？`, '提示', { type: 'warning' })
  await deleteHouse(row.id)
  ElMessage.success('删除成功')
  load()
}
onMounted(async () => {
  communities.value = await listCommunities({})
  owners.value = await listOwners({})
  load()
})
</script>
