<template>
  <div>
    <FontScaleBar :show-logout="true" @logout="doLogout" />
    <div class="app-page">
      <div class="app-card">
        <div class="app-title" style="font-size:1.5em;">历史评估记录</div>
        <el-divider />

        <el-table :data="history" style="width:100%" v-loading="loading">
          <el-table-column prop="createdAt" label="开始时间" width="170">
            <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="inputModeText" label="方式" width="110" />
          <el-table-column prop="statusText" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'COMPLETED' ? 'success' : 'warning'">{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="{ row }">
              <el-button v-if="row.status === 'COMPLETED'" type="primary" text @click="viewReport(row)">查看报告</el-button>
              <el-button v-else type="primary" text @click="continueTask(row)">继续评估</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loading && !history.length" description="暂无历史记录" />
        <el-button style="margin-top:16px;" @click="$router.push('/')">返回首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import FontScaleBar from '@/components/FontScaleBar.vue'
import { useUserStore } from '@/store/user'
import { listHistory } from '@/api'

const router = useRouter()
const user = useUserStore()
const history = ref([])
const loading = ref(true)

onMounted(async () => {
  try { history.value = await listHistory() } catch (e) { /* 忽略 */ } finally { loading.value = false }
})

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

function viewReport(row) {
  router.push({ path: '/report', query: { taskId: row.taskId } })
}
function continueTask(row) {
  router.push({ path: `/assessment/${row.taskId}`, query: { mode: row.inputMode } })
}
function doLogout() {
  user.logout()
  router.push('/login')
}
</script>
