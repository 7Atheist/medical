<template>
  <div>
    <FontScaleBar :show-logout="true" @logout="doLogout" />
    <div class="app-page">

      <!-- 完成面板 -->
      <div v-if="done" class="app-card" style="text-align:center;">
        <div style="font-size:2em; color:var(--color-success);">✓</div>
        <div class="app-title" style="font-size:1.4em;">评估已完成</div>
        <p style="color:var(--color-text-secondary);">{{ doneMessage }}</p>
        <el-button type="primary" class="app-btn-primary" @click="$router.push({ path:'/report', query:{ taskId } })">
          查看评估报告
        </el-button>
        <div style="margin-top:12px;">
          <el-button text @click="$router.push('/')">返回首页</el-button>
        </div>
      </div>

      <!-- 评估进行中 -->
      <template v-else-if="question">
        <div class="app-card">
          <!-- 进度 -->
          <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:8px;">
            <div style="font-weight:600; color:var(--color-primary-dark);">
              <el-tag v-if="question.screening" size="small" type="warning">初筛</el-tag>
            </div>
            <div style="color:var(--color-text-secondary);">
              第 {{ question.questionIndex }} / {{ question.questionTotal }} 题
            </div>
          </div>
          <el-progress :percentage="progressPct" :stroke-width="10" :show-text="false" style="margin:10px 0 18px;" />

          <!-- 对话气泡 -->
          <div class="qa-question">{{ question.questionText }}</div>
          <div v-if="tip" class="qa-followup">{{ tip }}</div>

          <!-- 语音读题（可随时重听） -->
          <div style="margin-bottom:14px;">
            <el-button size="large" @click="readQuestion" :disabled="speaking">
              {{ speaking ? '正在播报…' : '🔊 再读一遍题目' }}
            </el-button>
          </div>

          <!-- 作答区：手动点选 与 语音 合为一体，任选其一 -->
          <div>
            <!-- 选项选择题：直接点击选择 -->
            <div v-if="showOptions" style="margin-bottom:16px;">
              <div style="font-size:1.05em; margin-bottom:8px;">请选择最符合您情况的选项（也可点下方🎤用语音回答）：</div>
              <el-radio-group v-model="selectedValue" class="app-options">
                <el-radio v-for="o in question.options" :key="o.value" :value="o.value" border size="large">
                  {{ o.label }}
                </el-radio>
              </el-radio-group>
            </div>

            <!-- 开放题：文字输入 -->
            <div v-if="showTextInput" style="margin-bottom:16px;">
              <el-input
                v-model="typedText"
                type="textarea"
                :rows="3"
                size="large"
                placeholder="请输入您的回答（也可点下方🎤用语音回答）"
              />
            </div>

            <!-- 语音作答：选择题与开放题均可用 -->
            <div style="margin-bottom:16px;">
              <el-button size="large" type="primary" :loading="recording" @click="startRecord">
                {{ recording ? '正在聆听…' : '🎤 语音回答' }}
              </el-button>
              <div v-if="recognizedText" class="qa-followup" style="margin-top:10px;">
                我听到的是：{{ recognizedText }}
                <div style="margin-top:6px;">
                  <el-button size="small" type="primary" @click="submitVoice">对，就是这个</el-button>
                  <el-button size="small" @click="recognizedText=''">不对，重新说</el-button>
                </div>
              </div>
            </div>

            <div style="display:flex; gap:12px; flex-wrap:wrap;">
              <el-button type="primary" class="app-btn-primary" @click="submitAnswer" :disabled="!canSubmit">
                提交
              </el-button>
              <el-button size="large" @click="pause">先暂停，下次再继续</el-button>
            </div>
          </div>
        </div>
      </template>

      <div v-else-if="loading" style="text-align:center; padding:60px; color:var(--color-text-secondary);">正在加载…</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import FontScaleBar from '@/components/FontScaleBar.vue'
import { useUserStore } from '@/store/user'
import { getQuestion, submitAnswer as submitAnswerApi, submitAudio } from '@/api'
import { speak, stopSpeak, recognizeSpeech } from '@/utils/speech'
import { saveDraft, clearDraft, loadDraft, pushOfflineAnswer, takeOfflineAnswers } from '@/utils/db'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const taskId = route.params.taskId

const loading = ref(true)
const question = ref(null)
const tip = ref('')               // 追问/提示气泡
const selectedValue = ref(null)
const typedText = ref('')
const recognizedText = ref('')
const recording = ref(false)
const speaking = ref(false)
const isFollowup = ref(false)
const done = ref(false)
const doneMessage = ref('')

// 综合模式：每道题同时支持「点选选项 / 语音回答 / 开放题打字」，三者任选其一
const showOptions = computed(() =>
  question.value && question.value.questionType !== 'OPEN' && question.value.options && question.value.options.length > 0)
const showTextInput = computed(() =>
  question.value && question.value.questionType === 'OPEN')
const canSubmit = computed(() => {
  if (done.value || !question.value) return false
  return !!selectedValue.value || !!recognizedText.value || !!typedText.value
})
const progressPct = computed(() =>
  question.value ? Math.round(((question.value.questionIndex - 1) / question.value.questionTotal) * 100) : 0)

onMounted(async () => {
  await syncOffline()
  await loadQuestion()
})

onUnmounted(() => stopSpeak())

async function loadQuestion() {
  loading.value = true
  try {
    const q = await getQuestion(taskId)
    if (!q) {
      // 任务已完成 → 直接看报告
      done.value = true
      doneMessage.value = '该评估已完成，您可以查看报告。'
      return
    }
    question.value = q
    resetInputs()
    // 进入评估页/第一题即自动朗读题目（用户点击“再读一遍”可重听）
    readQuestion()
  } catch (e) {
    ElMessage.error(e.message || '加载题目失败')
  } finally {
    loading.value = false
  }
}

function resetInputs() {
  selectedValue.value = null
  typedText.value = ''
  recognizedText.value = ''
  tip.value = ''
  isFollowup.value = false
}

async function readQuestion() {
  speaking.value = true
  try {
    await speak(question.value.questionText)
  } catch (e) {
    // 浏览器不支持语音播报时静默跳过
  } finally {
    speaking.value = false
  }
}

async function startRecord() {
  recording.value = true
  recognizedText.value = ''
  try {
    const text = await recognizeSpeech()
    recognizedText.value = text
  } catch (e) {
    // 浏览器ASR失败 → 尝试上传音频（需浏览器支持MediaRecorder）
    const ok = await tryMediaRecord()
    if (!ok) {
      ElMessage.warning('语音识别失败，请直接点选选项或输入文字作答')
    }
  } finally {
    recording.value = false
  }
}

async function tryMediaRecord() {
  return false // 简化：优先浏览器ASR；可在此扩展 MediaRecorder 上传
}

async function submitVoice() {
  if (!recognizedText.value) return
  await doSubmit({ answerSource: 'VOICE', text: recognizedText.value })
}

async function submitAnswer() {
  let payload
  // 优先级：已点选选项 > 语音识别文本 > 手动输入文字
  if (showOptions.value && selectedValue.value) {
    payload = { answerSource: 'OPTION', optionValue: selectedValue.value }
  } else if (recognizedText.value) {
    payload = { answerSource: 'VOICE', text: recognizedText.value }
  } else {
    payload = { answerSource: 'TEXT', text: typedText.value }
  }
  await doSubmit(payload)
}

async function doSubmit(payload) {
  payload.questionId = question.value.questionId
  // 提交即停止当前朗读，避免“还在读上一题”
  stopSpeak()
  speaking.value = false
  // 离线兜底：先本地缓存草稿
  await saveDraft(taskId, { questionId: question.value.questionId, payload, at: Date.now() })
  try {
    const resp = await submitAnswerApi(taskId, payload)
    await clearDraft(taskId)
    handleResponse(resp)
  } catch (e) {
    // 网络异常：答案入离线队列，联网后同步
    await pushOfflineAnswer(taskId, payload)
    ElMessage.warning('网络异常，您的回答已保存在本机，联网后将自动提交')
  }
}

function handleResponse(resp) {
  if (resp.type === 'DONE') {
    done.value = true
    doneMessage.value = resp.message || '评估已完成'
    return
  }
  if (resp.type === 'PAUSE') {
    tip.value = resp.message
    isFollowup.value = false
    return
  }
  if (resp.type === 'FOLLOWUP') {
    tip.value = resp.message
    isFollowup.value = true
    recognizedText.value = ''
    return
  }
  // QUESTION：进入下一题
  if (resp.question) {
    question.value = resp.question
    resetInputs()
    // 欢迎语不常驻显示（仅提示追问等有效信息）
    if (resp.message && !resp.message.includes('欢迎来到')) tip.value = resp.message
    // 进入下一题自动朗读题目
    readQuestion()
  } else {
    // 理论上不应到达
    ElMessage.warning('评估状态异常，请返回首页重试')
  }
}

async function syncOffline() {
  const pending = await takeOfflineAnswers(taskId)
  for (const p of pending) {
    try {
      const resp = await submitAnswerApi(taskId, p)
      handleResponse(resp)
    } catch (e) { /* 忽略，仍离线 */ }
  }
}

async function pause() {
  await saveDraft(taskId, { questionId: question.value?.questionId, paused: true, at: Date.now() })
  ElMessage.success('已为您保存进度，3天内回来可以继续')
  router.push('/')
}

function doLogout() {
  user.logout()
  router.push('/login')
}
</script>
