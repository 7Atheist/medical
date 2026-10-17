<template>
  <el-card>
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:14px;">
      <div style="font-weight:600; font-size:1.2em;">用户管理</div>
      <el-input v-model="kw" placeholder="按姓名搜索" style="width:200px;" clearable @keyup.enter="load" />
    </div>
    <el-table :data="users" v-loading="loading">
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="idCardMask" label="身份证号(脱敏)" min-width="180" />
      <el-table-column prop="age" label="年龄" width="80" />
      <el-table-column label="性别" width="80">
        <template #default="{ row }">{{ row.gender === 1 ? '男' : '女' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="建档时间" min-width="160">
        <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminUsers } from '@/api'

const users = ref([])
const loading = ref(true)
const kw = ref('')

onMounted(load)

async function load() {
  loading.value = true
  try { users.value = await adminUsers(kw.value) } catch (e) { /* 忽略 */ } finally { loading.value = false }
}

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')
</script>
