<template>
  <el-card>
    <div style="font-weight:600; font-size:1.2em; margin-bottom:14px;">语义映射词典（口语 → 标准选项）</div>
    <el-table :data="dicts" v-loading="loading">
      <el-table-column prop="questionId" label="题目ID" width="90">
        <template #default="{ row }">{{ row.questionId || '全局' }}</template>
      </el-table-column>
      <el-table-column prop="optionValue" label="映射选项值" width="120" />
      <el-table-column prop="synonyms" label="口语同义词" min-width="280" />
      <el-table-column prop="matchType" label="匹配方式" width="110" />
    </el-table>
    <div style="font-size:0.85em; color:var(--color-text-secondary); margin-top:10px;">
      可在数据库中为每道题维护口语同义词，提升语音/文字归一化准确率。
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminDictionary } from '@/api'

const dicts = ref([])
const loading = ref(true)

onMounted(async () => {
  try { dicts.value = await adminDictionary() } catch (e) { /* 忽略 */ } finally { loading.value = false }
})
</script>
