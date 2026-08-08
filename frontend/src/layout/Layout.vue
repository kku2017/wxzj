<template>
  <el-container style="height: 100%">
    <el-aside width="220px" class="aside">
      <div class="logo">🛡️ 维修资金系统</div>
      <el-menu :default-active="$route.path" router background-color="#001529" text-color="#a6adb4" active-text-color="#409eff">
        <template v-if="!auth.isOwner">
          <el-sub-menu index="basic">
            <template #title><el-icon><OfficeBuilding /></el-icon>基础数据</template>
            <el-menu-item index="/basic/community">小区管理</el-menu-item>
            <el-menu-item index="/basic/building">楼栋管理</el-menu-item>
            <el-menu-item index="/basic/house">房屋管理</el-menu-item>
            <el-menu-item index="/basic/owner">业主管理</el-menu-item>
            <el-menu-item index="/basic/standard">缴存标准</el-menu-item>
          </el-sub-menu>
        </template>
        <template v-else>
          <el-menu-item index="/basic/house"><el-icon><HomeFilled /></el-icon>我的房屋</el-menu-item>
        </template>
        <el-menu-item v-if="auth.isAdmin || auth.isProperty" index="/deposit">
          <el-icon><Money /></el-icon>资金缴存
        </el-menu-item>
        <el-menu-item v-if="auth.isAdmin || auth.isProperty" index="/use">
          <el-icon><Tools /></el-icon>资金使用
        </el-menu-item>
        <el-menu-item index="/refund">
          <el-icon><RefreshLeft /></el-icon>资金退款
        </el-menu-item>
        <el-sub-menu index="query">
          <template #title><el-icon><Search /></el-icon>综合查询</template>
          <el-menu-item index="/query/account">账户查询</el-menu-item>
          <el-menu-item index="/query/flow">流水查询</el-menu-item>
          <el-menu-item v-if="!auth.isOwner" index="/query/statistics">统计报表</el-menu-item>
        </el-sub-menu>
        <el-menu-item v-if="auth.isAdmin || auth.isProperty" index="/migrate">
          <el-icon><Upload /></el-icon>数据迁移
        </el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/workflow">
          <el-icon><SetUp /></el-icon>流程配置
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">{{ $route.meta.title || '' }}</span>
        <el-dropdown @command="onCommand">
          <span class="user">
            <el-icon><User /></el-icon>
            {{ auth.user?.realName }}（{{ roleText }}）
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const router = useRouter()

const roleText = computed(() => ({ ADMIN: '系统管理员', PROPERTY: '物业操作员', COMMITTEE: '业委会', OWNER: '业主' }[auth.role] || auth.role))

function onCommand(cmd) {
  if (cmd === 'logout') {
    auth.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.aside { background: #001529; }
.logo { color: #fff; font-weight: 600; font-size: 16px; padding: 16px 12px; text-align: center; }
.el-menu { border-right: none; }
.header {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border-bottom: 1px solid #e5e7eb;
}
.title { font-size: 16px; font-weight: 600; }
.user { display: flex; align-items: center; gap: 4px; cursor: pointer; color: #333; }
</style>
