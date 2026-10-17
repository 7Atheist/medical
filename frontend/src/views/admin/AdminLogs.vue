<template>
  <el-card>
    <div style="font-weight:600; font-size:1.2em; margin-bottom:14px;">操作日志</div>
    <el-table :data="logs" v-loading="loading" height="600">
      <el-table-column prop="createdAt" label="时间" width="180">
        <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column prop="action" label="操作" width="200" />
      <el-table-column prop="detail" label="详情" min-width="300" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="140" />
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminLogs } from '@/api'

const logs = ref([])
const loading = ref(true)

onMounted(async () => {
  try { logs.value = await adminLogs() } catch (e) { /* 忽略 */ } finally { loading.value = false }
})

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '')
</script>
