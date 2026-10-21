<template>
  <div>
    <FontScaleBar :show-logout="true" @logout="doLogout" />

    <!-- 完成面板 -->
    <div v-if="done" class="chat-container">
      <div class="app-card" style="text-align:center;">
        <img src="/assets/patient/illustrations/assessment-done.png" style="width:90px; margin-bottom:10px;" />
        <div class="app-title" style="font-size:1.4em;">评估完成啦</div>
        <p style="color:var(--color-text-secondary);">{{ doneMessage }}</p>
        <el-button type="primary" class="app-btn-primary" @click="$router.push({ path:'/report', query:{ taskId } })">
          查看我的健康报告
        </el-button>
        <div style="margin-top:12px;">
          <el-button text @click="$router.push('/')">返回首页</el-button>
        </div>
      </div>
    </div>

    <!-- 聊天界面 -->
    <div v-else-if="question" class="chat-container">
      <div class="chat-card">
        <!-- 聊天消息流 -->
        <div class="chat-messages" ref="scrollRef">
          <!-- 历史消息 -->
          <div v-for="(m, i) in messages" :key="i" class="chat-row" :class="m.role">
            <img v-if="m.role === 'nurse'" src="/assets/patient/illustrations/assistant-robot.png" class="chat-avatar" />
            <div class="chat-bubble" :class="m.type">{{ m.text }}</div>
          </div>

          <!-- 当前题目：护士气泡 -->
          <div class="chat-row nurse">
            <img src="/assets/patient/illustrations/assistant-robot.png" class="chat-avatar" />
            <div class="chat-bubble nurse-bubble">
              <div v-if="question.screening" style="font-size:0.85em; opacity:0.7; margin-bottom:4px;">先简单了解一下</div>
              {{ displayQuestionText }}
              <div style="margin-top:8px;">
                <el-button text size="small" @click="readQuestion" :disabled="speaking" style="padding:0; font-size:0.9em;">
                  {{ speaking ? '播报中…' : '🔊 再听一遍' }}
                </el-button>
              </div>
            </div>
          </div>

          <!-- 追问提示 -->
          <div v-if="tip" class="chat-row nurse">
            <img src="/assets/patient/illustrations/assistant-robot.png" class="chat-avatar" />
            <div class="chat-bubble nurse-bubble tip-bubble">{{ tip }}</div>
          </div>
        </div>

        <!-- 底部作答区 -->
        <div class="chat-input-area">
          <!-- 选项气泡：点了即提交 -->
          <div v-if="showOptions" class="chat-options">
            <button
              v-for="o in question.options"
              :key="o.value"
              class="chat-option"
              @click="pickOption(o)"
            >{{ o.label }}</button>
          </div>

          <!-- 开放题：文字输入 -->
          <div v-if="showTextInput" class="chat-text-row">
            <input
              v-model="typedText"
              class="chat-text-input"
              placeholder="说点什么…"
              @keyup.enter="submitTyped"
            />
            <button class="chat-send-btn" @click="submitTyped">发送</button>
          </div>

          <!-- 语音识别确认 -->
          <div v-if="recognizedText" class="chat-recog">
            <div style="margin-bottom:6px;">我听到的是：<b>{{ recognizedText }}</b></div>
            <el-button size="small" type="primary" @click="submitVoice">对，就是这个</el-button>
            <el-button size="small" @click="recognizedText=''">不对，重说</el-button>
          </div>

          <!-- 底部操作：语音 + 暂停 -->
          <div class="chat-actions">
            <button class="chat-voice-btn" :class="{ recording }" @click="startRecord">
              {{ recording ? '🎤 聆听中…' : '🎤 语音回答' }}
            </button>
            <button class="chat-pause-btn" @click="pause">稍后再来</button>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="loading" style="text-align:center; padding:60px; color:var(--color-text-secondary);">正在准备…</div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import FontScaleBar from '@/components/FontScaleBar.vue'
import { useUserStore } from '@/store/user'
import { getQuestion, submitAnswer as submitAnswerApi, wrapQuestion } from '@/api'
import { speak, stopSpeak, createRecognizer } from '@/utils/speech'
import { saveDraft, clearDraft, loadDraft, pushOfflineAnswer, takeOfflineAnswers } from '@/utils/db'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const taskId = route.params.taskId

const loading = ref(true)
const question = ref(null)
const tip = ref('')
const typedText = ref('')
const recognizedText = ref('')
const recording = ref(false)
const speaking = ref(false)
const done = ref(false)
const doneMessage = ref('')
// 显示给患者的话术（AI 包装后的生活化开场白；包装失败则用原题）
const displayQuestionText = ref('')

// 聊天消息流：{ role: 'nurse'|'patient', text, type: 'text'|'ack' }
const messages = ref([])
const scrollRef = ref(null)

const showOptions = computed(() =>
  question.value && question.value.questionType !== 'OPEN' && question.value.options && question.value.options.length > 0)
const showTextInput = computed(() =>
  question.value && question.value.questionType === 'OPEN')

onMounted(async () => {
  await syncOffline()
  await loadQuestion()
})

onUnmounted(() => stopSpeak())

async function scrollToBottom() {
  await nextTick()
  await new Promise(r => setTimeout(r, 50)) // 等气泡渲染完
  if (scrollRef.value) scrollRef.value.scrollTop = scrollRef.value.scrollHeight
}

// 消息列表变化时自动滚到底部（覆盖新消息 push、答题后更新等场景）
watch(messages, () => scrollToBottom(), { deep: true })
watch(displayQuestionText, () => scrollToBottom())

async function loadQuestion() {
  loading.value = true
  try {
    const q = await getQuestion(taskId)
    if (!q) {
      done.value = true
      doneMessage.value = '该评估已完成，您可以查看报告。'
      return
    }
    question.value = q
    tip.value = ''
    typedText.value = ''
    recognizedText.value = ''
    // 先让 AI 把医学题干包装成生活化话术，再显示（无 key 时接口返回原题，自动降级）
    displayQuestionText.value = q.questionText
    try {
      const res = await wrapQuestion(q.questionText, q.options || [], user.profile || {})
      if (res && res.wrapped) displayQuestionText.value = res.wrapped
    } catch (e) { /* 包装失败兜底用原题 */ }
    readQuestion()
    scrollToBottom()
  } catch (e) {
    ElMessage.error(e.message || '加载题目失败')
  } finally {
    loading.value = false
  }
}

async function readQuestion() {
  speaking.value = true
  try {
    await speak(displayQuestionText.value || question.value.questionText)
  } catch (e) { /* 浏览器不支持语音时静默 */ }
  finally { speaking.value = false }
}

// 点选项气泡 → 直接提交
async function pickOption(o) {
  // 先把患者回答追加到聊天流
  messages.value.push({ role: 'patient', text: o.label, type: 'text' })
  scrollToBottom()
  await doSubmit({ questionId: question.value.questionId, answerSource: 'OPTION', optionValue: o.value })
}

// 开放题文字提交
async function submitTyped() {
  if (!typedText.value.trim()) return
  const text = typedText.value.trim()
  messages.value.push({ role: 'patient', text, type: 'text' })
  typedText.value = ''
  scrollToBottom()
  await doSubmit({ questionId: question.value.questionId, answerSource: 'TEXT', text })
}

// 语音识别实例（手动控制：点开始/再点停止）
let activeRecognizer = null

function startRecord() {
  // 再次点击 = 停止录音
  if (recording.value) {
    stopRecord()
    return
  }
  // 开始录音
  recording.value = true
  recognizedText.value = ''
  activeRecognizer = createRecognizer()
  if (!activeRecognizer) {
    recording.value = false
    ElMessage.warning('当前浏览器不支持语音识别，请直接输入文字')
    return
  }
  let finalText = ''
  activeRecognizer.onresult = (ev) => {
    // 收集识别到的文本（连续识别可能有多段）
    for (let i = ev.resultIndex; i < ev.results.length; i++) {
      finalText += ev.results[i][0].transcript
    }
  }
  activeRecognizer.onerror = () => {
    recording.value = false
    activeRecognizer = null
    if (finalText) {
      recognizedText.value = finalText
    } else {
      ElMessage.warning('语音识别失败，请直接输入文字')
    }
  }
  activeRecognizer.onend = () => {
    recording.value = false
    if (finalText) {
      recognizedText.value = finalText
    }
    activeRecognizer = null
  }
  try {
    activeRecognizer.start()
  } catch (e) {
    recording.value = false
    activeRecognizer = null
    ElMessage.warning('无法启动录音，请检查浏览器麦克风权限')
  }
}

function stopRecord() {
  if (activeRecognizer) {
    try { activeRecognizer.stop() } catch (e) {}
  }
  recording.value = false
}

async function submitVoice() {
  if (!recognizedText.value) return
  messages.value.push({ role: 'patient', text: recognizedText.value, type: 'text' })
  const text = recognizedText.value
  recognizedText.value = ''
  scrollToBottom()
  await doSubmit({ questionId: question.value.questionId, answerSource: 'VOICE', text })
}

async function doSubmit(payload) {
  stopSpeak()
  speaking.value = false
  await saveDraft(taskId, { questionId: question.value.questionId, payload, at: Date.now() })
  try {
    const resp = await submitAnswerApi(taskId, payload)
    await clearDraft(taskId)
    await handleResponse(resp)
  } catch (e) {
    await pushOfflineAnswer(taskId, payload)
    ElMessage.warning('网络异常，您的回答已保存在本机')
  }
}

// 护士温暖回应语
const acks = ['好的，我记下了～', '了解啦～', '嗯嗯，明白了～', '好的～', '谢谢你告诉我～']

async function handleResponse(resp) {
  if (resp.type === 'DONE') {
    messages.value.push({ role: 'nurse', text: '太棒了，我们聊完啦！我这就帮您整理一份健康报告。', type: 'ack' })
    done.value = true
    doneMessage.value = resp.message || '评估已完成'
    return
  }
  if (resp.type === 'PAUSE') {
    tip.value = resp.message
    return
  }
  if (resp.type === 'FOLLOWUP') {
    tip.value = resp.message
    recognizedText.value = ''
    return
  }
  if (resp.question) {
    // 护士先回一句温暖过渡，再自然进入下一题
    messages.value.push({ role: 'nurse', text: acks[Math.floor(Math.random() * acks.length)], type: 'ack' })
    question.value = resp.question
    tip.value = ''
    recognizedText.value = ''
    if (resp.message && !resp.message.includes('欢迎来到')) tip.value = resp.message
    // 先包装下一题再显示
    displayQuestionText.value = resp.question.questionText
    try {
      const res = await wrapQuestion(resp.question.questionText, resp.question.options || [], user.profile || {})
      if (res && res.wrapped && question.value?.questionId === resp.question.questionId) {
        displayQuestionText.value = res.wrapped
      }
    } catch (e) { /* 兜底原题 */ }
    readQuestion()
    scrollToBottom()
  } else {
    ElMessage.warning('评估状态异常，请返回首页重试')
  }
}

async function syncOffline() {
  const pending = await takeOfflineAnswers(taskId)
  for (const p of pending) {
    try {
      const resp = await submitAnswerApi(taskId, p)
      await handleResponse(resp)
    } catch (e) { /* 忽略 */ }
  }
}

async function pause() {
  await saveDraft(taskId, { questionId: question.value?.questionId, paused: true, at: Date.now() })
  ElMessage.success('已为您保存，3天内回来可以继续')
  router.push('/')
}

function doLogout() {
  user.logout()
  router.push('/login')
}
</script>

<style scoped>
.chat-container {
  max-width: 640px;
  margin: 0 auto;
  padding: 12px 16px;
}
.chat-card {
  background: #fff;
  border-radius: 16px;
  border: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  max-height: 760px;
  overflow: hidden;
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 16px;
  background: #f7fafb;
}
.chat-row {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-end;
  gap: 8px;
}
.chat-row.patient {
  justify-content: flex-end;
}
.chat-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--color-primary-light);
}
.chat-bubble {
  max-width: 75%;
  padding: 12px 16px;
  border-radius: 16px;
  line-height: 1.6;
  font-size: 1.02em;
}
.nurse-bubble {
  background: #fff;
  border: 1px solid var(--color-border);
  border-bottom-left-radius: 4px;
  color: var(--color-text);
}
.patient .chat-bubble {
  background: var(--color-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.chat-bubble.ack {
  background: #eef7f0;
  color: var(--color-success);
  font-size: 0.92em;
}
.tip-bubble {
  background: #fff8e6;
  border-color: #ffe08a;
}
.chat-input-area {
  padding: 12px 16px 16px;
  background: #fff;
  border-top: 1px solid var(--color-border);
}
.chat-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.chat-option {
  padding: 10px 18px;
  border-radius: 20px;
  border: 1px solid var(--color-primary);
  background: #fff;
  color: var(--color-primary-dark);
  font-size: 0.98em;
  cursor: pointer;
  transition: all .15s;
}
.chat-option:hover {
  background: var(--color-primary);
  color: #fff;
}
.chat-text-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.chat-text-input {
  flex: 1;
  padding: 10px 14px;
  border-radius: 20px;
  border: 1px solid var(--color-border);
  font-size: 1em;
  outline: none;
}
.chat-text-input:focus {
  border-color: var(--color-primary);
}
.chat-send-btn {
  padding: 10px 18px;
  border-radius: 20px;
  border: none;
  background: var(--color-primary);
  color: #fff;
  cursor: pointer;
}
.chat-recog {
  margin-bottom: 12px;
  padding: 10px 12px;
  background: #f0f7ff;
  border-radius: 10px;
  font-size: 0.95em;
}
.chat-actions {
  display: flex;
  gap: 10px;
}
.chat-voice-btn {
  flex: 1;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid var(--color-border);
  background: #fff;
  font-size: 1em;
  cursor: pointer;
}
.chat-voice-btn.recording {
  background: #fff0f0;
  border-color: #f56c6c;
  color: #f56c6c;
}
.chat-pause-btn {
  padding: 12px 18px;
  border-radius: 12px;
  border: 1px solid var(--color-border);
  background: #fff;
  color: var(--color-text-secondary);
  cursor: pointer;
}

/* ---------- 移动端小屏适配 ---------- */
@media (max-width: 480px) {
  .chat-container {
    padding: 8px;
  }
  .chat-card {
    border-radius: 12px;
    height: calc(100vh - 100px);
  }
  .chat-messages {
    padding: 12px;
  }
  .bubble {
    max-width: 85%;
    padding: 10px 12px;
    font-size: 1em;
  }
  .option-btn {
    padding: 10px 12px;
    font-size: 0.95em;
  }
}
</style>
