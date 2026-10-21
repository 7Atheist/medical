<template>
  <div>
    <FontScaleBar :show-logout="true" @logout="doLogout" />
    <div class="app-page">
      <div class="app-card">
        <div style="display:flex; align-items:center; gap:14px; margin-bottom:6px;">
          <img src="/assets/patient/illustrations/assistant-robot.png" alt="助手" style="width:60px; height:60px; border-radius:50%; background:var(--color-primary-light); object-fit:cover;" />
          <div>
            <div class="app-title" style="font-size:1.5em;">
              {{ greeting }}，{{ profile?.name || '您好' }}
            </div>
            <div class="app-subtitle">
              您的健康评估助手，随时可暂停、可继续，进度自动保存。
            </div>
          </div>
        </div>

        <!-- 新评估 -->
        <div style="margin: 22px 0 10px;">
          <div style="font-weight:600; margin-bottom:12px;">开始新的评估</div>
          <div class="mode-card" @click="start">
            <div class="mode-title">开始评估</div>
            <div class="mode-desc">每道题均可直接点选选项，或点击🎤用语音回答（开放题也可手动输入），两种方式任选</div>
          </div>
        </div>

        <!-- 未完成草稿 -->
        <template v-if="drafts.length">
          <div style="font-weight:600; margin:24px 0 12px;">未完成的评估（可一键继续）</div>
          <div v-for="d in drafts" :key="d.taskId" class="draft-item" @click="continueDraft(d)">
            <div>
              <div style="font-weight:600;">综合评估（点选+语音）</div>
              <div style="font-size:0.9em; color:var(--color-text-secondary);">
                {{ d.currentScaleName || '评估中' }} · 最后作答：{{ fmt(d.lastAnswerAt) }}
              </div>
            </div>
            <el-button type="primary" text>继续评估</el-button>
          </div>
        </template>

        <div style="margin-top:24px;">
          <el-button @click="$router.push('/history')" style="width:100%;" size="large">查看历史评估记录</el-button>
        </div>

        <div class="app-disclaimer">本评估为自助健康调查工具，结果仅作为个人健康情况参考，不构成医疗诊断，不替代医生诊疗。如有不适请及时就医。</div>
      </div>

      <div style="text-align:center; margin-top:18px;">
        <el-button text size="small" @click="$router.push('/admin')">后台管理</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import FontScaleBar from '@/components/FontScaleBar.vue'
import { useUserStore } from '@/store/user'
import { createTask, listDrafts } from '@/api'

const router = useRouter()
const user = useUserStore()
const drafts = ref([])
const profile = computed(() => user.profile)

const greeting = computed(() => {
  const age = profile.value?.age
  if (age >= 80) return '敬爱的' + (profile.value?.genderText === '男' ? '爷爷' : '奶奶')
  if (age >= 60) return '尊敬的'
  return '您好'
})

onMounted(loadDrafts)

async function loadDrafts() {
  try { drafts.value = await listDrafts() } catch (e) { /* 忽略 */ }
}

async function start() {
  try {
    const taskId = await createTask('MANUAL')
    router.push({ path: `/assessment/${taskId}` })
  } catch (e) {
    ElMessage.error(e.message || '创建评估失败')
  }
}

function continueDraft(d) {
  ElMessageBox.confirm('将回到您上次的位置继续评估，之前的内容不会丢失。', '继续评估', { confirmButtonText: '继续', cancelButtonText: '取消' })
    .then(() => router.push({ path: `/assessment/${d.taskId}` }))
    .catch(() => {})
}

function fmt(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

function doLogout() {
  user.logout()
  router.push('/login')
}
</script>

<style scoped>
.mode-card {
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 22px 20px;
  cursor: pointer;
  background: #fff;
  transition: all .15s;
  text-align: center;
}
.mode-card:hover { border-color: var(--color-primary); box-shadow: var(--shadow); }
.mode-title { font-size: 1.25em; font-weight: 600; color: var(--color-primary-dark); margin-bottom: 8px; }
.mode-desc { color: var(--color-text-secondary); font-size: 0.92em; }
.draft-item {
  display: flex; justify-content: space-between; align-items: center;
  border: 1px solid var(--color-border); border-radius: 10px;
  padding: 14px 18px; margin-bottom: 10px; cursor: pointer; background: #fff;
}
.draft-item:hover { border-color: var(--color-primary); }
</style>
