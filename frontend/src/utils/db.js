/**
 * IndexedDB 本地草稿兜底（方案5.1：网络异常时继续答题，联网后自动同步后端）。
 * 使用 localforage 封装 IndexedDB，存储：
 *  - draft:{taskId} : 最近一次的评估草稿快照（当前题目 + 待同步答案）
 */
import localforage from 'localforage'

const store = localforage.createInstance({ name: 'geriatric-assessment' })

/** 保存草稿快照 */
export async function saveDraft(taskId, snapshot) {
  try { await store.setItem('draft:' + taskId, snapshot) } catch (e) { /* 忽略 */ }
}

/** 读取草稿快照 */
export async function loadDraft(taskId) {
  try { return await store.getItem('draft:' + taskId) } catch (e) { return null }
}

/** 清除草稿 */
export async function clearDraft(taskId) {
  try { await store.removeItem('draft:' + taskId) } catch (e) { /* 忽略 */ }
}

/** 保存待同步的离线答案队列 */
export async function pushOfflineAnswer(taskId, answer) {
  try {
    const key = 'offline:' + taskId
    const list = (await store.getItem(key)) || []
    list.push(answer)
    await store.setItem(key, list)
  } catch (e) { /* 忽略 */ }
}

/** 取并清空待同步的离线答案 */
export async function takeOfflineAnswers(taskId) {
  try {
    const key = 'offline:' + taskId
    const list = (await store.getItem(key)) || []
    await store.removeItem(key)
    return list
  } catch (e) { return [] }
}
