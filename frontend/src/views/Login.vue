<template>
  <div class="app-page" style="display:flex; align-items:center; justify-content:center; min-height:100vh;">
    <div class="app-card" style="max-width:440px; width:100%;">
      <div style="text-align:center; margin-bottom:6px;">
        <img src="/assets/patient/illustrations/nurse-welcome.png" alt="健康评估助手" style="width:140px; height:auto; display:block; margin:0 auto;" />
      </div>
      <div style="text-align:center; margin-bottom:20px;">
        <div class="app-title" style="font-size:1.6em;">老年综合健康评估</div>
        <div class="app-subtitle" style="margin-top:6px;">自助式评估 · 无需注册账号密码</div>
      </div>

      <el-form label-position="top" @submit.prevent="doLogin">
        <el-form-item label="您的姓名">
          <el-input v-model="name" size="large" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="idCard" size="large" placeholder="请输入18位身份证号" maxlength="18" />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="app-btn-primary"
            style="width:100%;"
            :loading="loading"
            @click="doLogin"
          >进入评估</el-button>
        </el-form-item>
      </el-form>

      <div style="font-size:0.85em; color:var(--color-text-secondary); line-height:1.8;">
        登录后系统将自动从身份证号解析您的出生日期、年龄、性别，用于更贴合地开展评估。
        登录信息仅用于识别身份、生成个人健康画像并关联个人评估档案。
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const user = useUserStore()
const name = ref('')
const idCard = ref('')
const loading = ref(false)

async function doLogin() {
  if (!name.value.trim() || !idCard.value.trim()) {
    ElMessage.warning('请填写姓名和身份证号')
    return
  }
  loading.value = true
  try {
    await user.login(name.value.trim(), idCard.value.trim())
    ElMessage.success('欢迎回来')
    router.push('/')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>
