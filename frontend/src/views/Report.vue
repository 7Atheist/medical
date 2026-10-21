<template>
  <div>
    <FontScaleBar :show-logout="true" @logout="doLogout" />
    <div class="app-page">
      <div class="app-card">
        <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
          <div style="display:flex; align-items:center; gap:12px;">
            <img src="/assets/patient/illustrations/assessment-done.png" alt="评估完成" style="width:56px; height:auto;" />
            <div>
              <div class="app-title" style="font-size:1.5em;">老年综合评估报告</div>
              <div class="app-subtitle" style="margin:6px 0 0;">
                姓名：{{ report?.userName }}　年龄：{{ report?.age }}　性别：{{ report?.genderText }}
                <span v-if="report?.createdAt">　生成时间：{{ fmt(report.createdAt) }}</span>
              </div>
            </div>
          </div>
          <el-button type="primary" size="large" @click="exportPdf">导出PDF</el-button>
        </div>

        <el-divider />

        <div v-if="report && report.scaleResults.length">
          <div v-for="r in report.scaleResults" :key="r.scaleName" class="scale-result">
            <div style="display:flex; justify-content:space-between; flex-wrap:wrap; gap:8px;">
              <div style="font-weight:600;">{{ r.scaleName }}
                <span style="color:var(--color-text-secondary); font-weight:400;">（{{ r.tableNo }}）</span>
              </div>
              <el-tag :type="riskType(r.risk)" size="large">{{ r.riskLevel }}</el-tag>
            </div>
            <div style="margin:8px 0; color:var(--color-text);">
              得分：<b>{{ r.score }}</b>
              <span v-if="r.fullScore"> / {{ r.fullScore }}</span>
            </div>
            <div v-if="r.riskText" style="color:var(--color-text-secondary); margin-bottom:6px;">{{ r.riskText }}</div>
            <div v-if="r.suggestions && r.suggestions.length" class="suggestion">
              <div style="font-weight:600; margin-bottom:4px;">健康建议：</div>
              <div v-for="(s, i) in r.suggestions" :key="i">· {{ s }}</div>
            </div>
          </div>
        </div>
        <div v-else style="color:var(--color-text-secondary);">暂无量表结果。</div>

        <div v-if="report && report.overallSuggestions && report.overallSuggestions.length" style="margin-top:20px;">
          <div style="font-weight:600; font-size:1.1em;">总体健康生活建议</div>
          <div style="margin-top:8px; line-height:1.9;">
            <div v-for="(s, i) in report.overallSuggestions" :key="i">· {{ s }}</div>
          </div>
        </div>

        <div class="app-disclaimer">{{ report?.disclaimer || '本评估为自助健康调查工具，结果仅作为个人健康情况参考，不构成医疗诊断。' }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import FontScaleBar from '@/components/FontScaleBar.vue'
import { useUserStore } from '@/store/user'
import { getReportByTask, reportPdfUrl } from '@/api'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const report = ref(null)
const taskId = route.query.taskId

onMounted(async () => {
  if (!taskId) { router.replace('/'); return }
  try {
    report.value = await getReportByTask(taskId)
  } catch (e) {
    ElMessage.error(e.message || '加载报告失败')
  }
})

const riskType = (risk) => {
  if (risk >= 2) return 'danger'
  if (risk === 1) return 'warning'
  return 'success'
}

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

function exportPdf() {
  // 仅当有报告ID时后端才提供PDF；此处用任务查询后由后端生成
  ElMessage.info('正在生成PDF…')
  window.open(reportPdfUrl(report.value?.reportId || 0), '_blank')
}

function doLogout() {
  user.logout()
  router.push('/login')
}
</script>

<style scoped>
.scale-result {
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 14px;
  background: #fff;
}
.suggestion {
  margin-top: 6px;
  padding: 10px 14px;
  background: var(--color-primary-light);
  border-radius: 8px;
  color: var(--color-primary-dark);
}
</style>
