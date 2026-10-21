<template>
  <div>
    <FontScaleBar :show-logout="true" @logout="doLogout" />
    <div class="app-page">
      <div class="app-card" style="max-width:640px; margin:0 auto;">

        <!-- 顶部标题区 -->
        <div style="text-align:center; margin-bottom:24px;">
          <img src="/assets/patient/illustrations/assessment-done.png" alt="" style="width:80px; opacity:0.9;" />
          <div class="app-title" style="font-size:1.5em; margin-top:12px;">我的评估记录</div>
          <div style="color:#909399; margin-top:8px; font-size:0.95em;">这里是您之前做过的健康评估</div>
        </div>

        <el-divider />

        <!-- 记录列表：卡片式 -->
        <div v-loading="loading" style="min-height:200px;">
          <div v-for="row in history" :key="row.taskId" class="history-item"
               style="display:flex; align-items:center; padding:16px; border:1px solid #ebeef5; border-radius:12px; margin-bottom:12px; background:#fff; transition:all .2s;">
            <!-- 状态图标 -->
            <div style="width:48px; height:48px; border-radius:50%; display:flex; align-items:center; justify-content:center; flex-shrink:0; margin-right:16px;"
                 :style="{ background: row.status === 'COMPLETED' ? '#f0f9eb' : '#fdf6ec' }">
              <span style="font-size:24px;">{{ row.status === 'COMPLETED' ? '✓' : '⏸' }}</span>
            </div>
            <!-- 信息 -->
            <div style="flex:1; min-width:0;">
              <div style="font-weight:600; font-size:1.05em; margin-bottom:4px;">
                {{ row.currentScaleName || (row.status === 'COMPLETED' ? '评估完成' : '进行中') }}
              </div>
              <div style="color:#909399; font-size:0.88em;">
                {{ fmt(row.createdAt) }} · {{ row.inputModeText }}
              </div>
            </div>
            <!-- 按钮区 -->
            <el-tag v-if="row.status === 'COMPLETED'" type="success" style="margin-right:8px;">已完成</el-tag>
            <el-tag v-else type="warning" style="margin-right:8px;">进行中</el-tag>
            <el-button v-if="row.status === 'COMPLETED'" type="primary" round size="small" @click="viewReport(row)">查看报告</el-button>
            <el-button v-else type="primary" round size="small" @click="continueTask(row)">继续</el-button>
            <el-button type="danger" round size="small" plain style="margin-left:8px;" @click="removeTask(row)">删除</el-button>
          </div>

          <!-- 空状态 -->
          <div v-if="!loading && !history.length" style="text-align:center; padding:40px 0;">
            <div style="font-size:48px; margin-bottom:12px;">📋</div>
            <div style="color:#909399;">还没有评估记录</div>
            <el-button type="primary" round style="margin-top:16px;" @click="$router.push('/')">开始第一次评估</el-button>
          </div>
        </div>

        <el-button style="margin-top:16px; width:100%;" @click="$router.push('/')">返回首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import FontScaleBar from '@/components/FontScaleBar.vue'
import { useUserStore } from '@/store/user'
import { listHistory, deleteTask } from '@/api'

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
async function removeTask(row) {
  try {
    await ElMessageBox.confirm('确定要删除这条记录吗？删除后无法恢复。', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteTask(row.taskId)
    ElMessage.success('已删除')
    history.value = history.value.filter(h => h.taskId !== row.taskId)
  } catch (e) { /* 用户取消 */ }
}
function doLogout() {
  user.logout()
  router.push('/login')
}
</script>
