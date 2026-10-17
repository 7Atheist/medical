/**
 * 语音能力封装（方案7.1/6.2）：
 *  - 语音播报：浏览器 SpeechSynthesis 中文合成（免费、无需服务）
 *  - 语音识别：优先浏览器 Web Speech API；不可用时由后端 ASR 服务（/assess/{taskId}/audio）
 * 封装为统一接口，方便降级。
 */

// ---------------- 语音播报（TTS） ----------------
export function ttsSupported() {
  return typeof window !== 'undefined' && 'speechSynthesis' in window
}

/**
 * 播报一段中文文本。
 * @returns {Promise} resolve 于播报结束；不支持则立即 reject
 */
export function speak(text) {
  return new Promise((resolve) => {
    if (!ttsSupported()) {
      resolve()
      return
    }
    const u = new SpeechSynthesisUtterance(text)
    u.lang = 'zh-CN'
    u.rate = 0.85 // 语速放慢，适配老人
    u.pitch = 1
    // 尽量选择中文语音
    const voices = window.speechSynthesis.getVoices()
    const zh = voices.find((v) => v.lang && v.lang.startsWith('zh'))
    if (zh) u.voice = zh
    let settled = false
    const finish = () => { if (!settled) { settled = true; clearTimeout(fallback) } }
    u.onend = () => { finish(); resolve() }
    u.onerror = () => { finish(); resolve() } // 出错也不阻断流程
    // 安全兜底：按文本长度估算时长，最多 30s 后强制结束，避免卡在“正在播报”
    const cap = Math.min(30000, Math.max(5000, (text ? text.length : 0) * 300))
    const fallback = setTimeout(() => { if (!settled) { settled = true; try { window.speechSynthesis.cancel() } catch (e) {} resolve() } }, cap)
    window.speechSynthesis.speak(u)
  })
}

export function stopSpeak() {
  if (ttsSupported()) window.speechSynthesis.cancel()
}

// ---------------- 语音识别（ASR） ----------------
export function asrSupported() {
  return typeof window !== 'undefined' &&
    (window.SpeechRecognition || window.webkitSpeechRecognition)
}

/**
 * 浏览器语音识别，返回识别文本。
 * @returns {Promise<string>} 识别到的文本
 */
export function recognizeSpeech(timeoutMs = 10000) {
  return new Promise((resolve, reject) => {
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition
    if (!SR) {
      reject(new Error('当前浏览器不支持语音识别'))
      return
    }
    const rec = new SR()
    rec.lang = 'zh-CN'
    rec.interimResults = false
    rec.maxAlternatives = 1
    let done = false
    const timer = setTimeout(() => {
      if (!done) { done = true; try { rec.stop() } catch (e) {} }
      reject(new Error('语音识别超时'))
    }, timeoutMs)
    rec.onresult = (ev) => {
      if (done) return
      done = true
      clearTimeout(timer)
      const text = ev.results[0][0].transcript
      resolve(text)
    }
    rec.onerror = (ev) => {
      if (done) return
      done = true
      clearTimeout(timer)
      reject(new Error('语音识别失败: ' + (ev.error || '')))
    }
    rec.onend = () => {
      clearTimeout(timer)
      if (!done) { done = true; reject(new Error('未识别到语音')) }
    }
    try { rec.start() } catch (e) { reject(e) }
  })
}
