<template>
  <div class="admin-layout">
    <aside class="admin-side">
      <div style="font-weight:700; font-size:1.15em; color:var(--color-primary-dark); padding:6px 12px 18px;">
        评估系统 · 后台
      </div>
      <el-menu :default-active="active" router>
        <el-menu-item index="/admin/stats">数据统计</el-menu-item>
        <el-menu-item index="/admin/scales">量表管理</el-menu-item>
        <el-menu-item index="/admin/dialog">对话话术</el-menu-item>
        <el-menu-item index="/admin/dictionary">语义词典</el-menu-item>
        <el-menu-item index="/admin/params">系统参数</el-menu-item>
        <el-menu-item index="/admin/users">用户管理</el-menu-item>
        <el-menu-item index="/admin/logs">操作日志</el-menu-item>
      </el-menu>
      <el-button style="width:100%; margin-top:20px;" @click="$router.push('/')">返回前台</el-button>
    </aside>
    <main class="admin-main">
      <!-- 管理令牌校验 -->
      <div v-if="!authed" style="max-width:420px; margin:60px auto;">
        <el-card>
          <div style="font-weight:600; margin-bottom:12px;">请输入后台管理令牌</div>
          <el-input v-model="token" type="password" placeholder="管理令牌" size="large" @keyup.enter="check" />
          <el-button type="primary" style="width:100%; margin-top:14px;" @click="check">进入后台</el-button>
          <div style="font-size:0.85em; color:var(--color-text-secondary); margin-top:10px;">
            默认令牌 admin@2026，请上线后在【系统参数】中修改。
          </div>
        </el-card>
      </div>
      <router-view v-else />
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { adminLogin } from '@/api'

const route = useRoute()
const active = computed(() => route.path)
const authed = ref(!!localStorage.getItem('adminToken'))
const token = ref('')

function check() {
  if (!token.value) return
  localStorage.setItem('adminToken', token.value)
  authed.value = true
}
</script>
