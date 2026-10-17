<template>
  <div class="font-bar">
    <div class="font-bar-title" style="color: var(--color-primary-dark); font-weight:600;">
      老年综合健康评估
    </div>
    <div class="font-btns">
      <span style="color: var(--color-text-secondary); margin-right:6px;">文字大小</span>
      <el-button
        v-for="lv in LEVELS"
        :key="lv"
        size="small"
        :class="{ active: level === lv }"
        @click="set(lv)"
      >{{ LABELS[lv] }}</el-button>
      <el-button size="small" text @click="$emit('logout')" v-if="showLogout">退出</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { LEVELS, LABELS, getScale, setScale } from '@/utils/fontScale'

defineProps({ showLogout: { type: Boolean, default: false } })
defineEmits(['logout'])

const level = ref(getScale())
const set = (lv) => { level.value = lv; setScale(lv) }
</script>
