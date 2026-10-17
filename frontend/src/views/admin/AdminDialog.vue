<template>
  <el-card>
    <div style="font-weight:600; font-size:1.2em; margin-bottom:14px;">对话话术管理</div>
    <el-table :data="dialogs" v-loading="loading">
      <el-table-column prop="scene" label="场景" width="150" />
      <el-table-column prop="content" label="话术内容" min-width="360" />
      <el-table-column prop="sortOrder" label="顺序" width="70" />
      <el-table-column label="启用" width="80">
        <template #default="{ row }"><el-switch v-model="row.enabled" @change="save(row)" /></template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminDialogs, adminSaveDialog } from '@/api'

const dialogs = ref([])
const loading = ref(true)

onMounted(async () => {
  try { dialogs.value = await adminDialogs() } catch (e) { /* 忽略 */ } finally { loading.value = false }
})

async function save(row) {
  try { await adminSaveDialog(row) } catch (e) { /* 忽略 */ }
}
</script>
