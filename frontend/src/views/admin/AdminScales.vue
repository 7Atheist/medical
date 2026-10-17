<template>
  <el-card>
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:14px;">
      <div style="font-weight:600; font-size:1.2em;">量表管理（21套）</div>
    </div>
    <el-table :data="scales" v-loading="loading" height="600">
      <el-table-column prop="sortOrder" label="顺序" width="60" />
      <el-table-column prop="name" label="量表名称" min-width="200" />
      <el-table-column prop="tableNo" label="表号" width="90" />
      <el-table-column prop="category" label="类别" width="150" />
      <el-table-column label="初筛" width="70">
        <template #default="{ row }">
          <el-tag :type="row.screening ? 'warning' : 'info'" size="small">{{ row.screening ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="题目" width="90">
        <template #default="{ row }">
          <el-button size="small" text @click="openQuestions(row)">查看题目</el-button>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="save(row)" />
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="drawer" :title="'题目：' + (curScale?.name || '')" size="70%">
      <el-table :data="questions" v-loading="qLoading" height="560">
        <el-table-column prop="sortOrder" label="#" width="50" />
        <el-table-column prop="questionText" label="题干" min-width="260" show-overflow-tooltip />
        <el-table-column label="题型" width="90">
          <template #default="{ row }">{{ row.questionType === 'OPEN' ? '开放题' : '选择题' }}</template>
        </el-table-column>
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" />
          </template>
        </el-table-column>
      </el-table>
      <div style="font-size:0.85em; color:var(--color-text-secondary); margin-top:10px;">
        提示：题目、选项、计分与跳转规则按 WS/T 888—2026 标准原文在数据库中维护，
        正式上线前请用标准文本校正示例题。
      </div>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminScales, adminSaveScale, adminQuestions } from '@/api'

const scales = ref([])
const loading = ref(true)
const drawer = ref(false)
const curScale = ref(null)
const questions = ref([])
const qLoading = ref(false)

onMounted(async () => {
  try { scales.value = await adminScales() } catch (e) { /* 忽略 */ } finally { loading.value = false }
})

async function save(row) {
  try { await adminSaveScale(row) } catch (e) { /* 忽略 */ }
}

async function openQuestions(row) {
  curScale.value = row
  drawer.value = true
  qLoading.value = true
  try { questions.value = await adminQuestions(row.id) } catch (e) { /* 忽略 */ } finally { qLoading.value = false }
}
</script>
