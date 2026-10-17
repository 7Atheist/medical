/**
 * 全局文字放大工具（方案6.5：标准/放大/特大三档）。
 * 通过给 <html> 根元素设置 class 驱动 CSS 变量 --app-font-scale 缩放，
 * 字号选择状态保存在 localStorage，下次打开自动沿用。
 *
 * 说明：CSS 变量只会向下(父→子)继承，因此必须把缩放 class 加到
 * <html> 根元素（body 的父级），而不是 #app，否则 body 读取不到变量。
 */
const KEY = 'app-font-scale'
const LEVELS = ['standard', 'large', 'xlarge'] // 标准 / 放大 / 特大
const LABELS = { standard: '标准', large: '放大', xlarge: '特大' }

export function getScale() {
  const saved = localStorage.getItem(KEY)
  return LEVELS.includes(saved) ? saved : 'standard'
}

export function setScale(level) {
  if (!LEVELS.includes(level)) level = 'standard'
  localStorage.setItem(KEY, level)
  applyScale(level)
}

export function applyScale(level) {
  const root = document.documentElement
  if (!root) return
  root.classList.remove(...LEVELS.map((l) => 'app-scale-' + l))
  root.classList.add('app-scale-' + level)
}

export function initScale() {
  applyScale(getScale())
}

export { LEVELS, LABELS }
