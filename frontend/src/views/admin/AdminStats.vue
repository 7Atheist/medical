<template>
  <el-card>
    <div style="font-weight:600; font-size:1.2em; margin-bottom:16px;">数据统计</div>
    <el-row :gutter="16">
      <el-col :span="6"><el-statistic title="用户数" :value="stats.userCount" /></el-col>
      <el-col :span="6"><el-statistic title="评估任务数" :value="stats.taskCount" /></el-col>
      <el-col :span="6"><el-statistic title="已完成评估" :value="stats.completedTaskCount" /></el-col>
      <el-col :span="6"><el-statistic title="量表面数" :value="stats.scaleCount" /></el-col>
    </el-row>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminStats } from '@/api'

const stats = ref({})

onMounted(async () => {
  try { stats.value = await adminStats() } catch (e) { /* 忽略 */ }
})
</script>
