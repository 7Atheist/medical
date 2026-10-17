<template>
  <el-card>
    <div style="font-weight:600; font-size:1.2em; margin-bottom:14px;">系统参数配置</div>
    <el-table :data="params" v-loading="loading">
      <el-table-column prop="paramKey" label="参数键" width="230" />
      <el-table-column label="参数值" min-width="320">
        <template #default="{ row }">
          <el-input v-model="row.paramValue" :type="row.paramKey.includes('key') || row.paramKey.includes('token') ? 'password' : 'text'" />
        </template>
      </el-table-column>
      <el-table-column prop="description" label="说明" min-width="220" />
      <el-table-column label="操作" width="90">
        <template #default="{ row }"><el-button size="small" type="primary" text @click="save(row)">保存</el-button></template>
      </el-table-column>
    </el-table>
    <div style="font-size:0.85em; color:var(--color-text-secondary); margin-top:10px;">
      关键参数：max_followup_count（追问次数）、confidence_threshold（置信度阈值）、
      draft_retention_days（草稿保留天数=3）、admin_token（后台令牌，上线请修改）。
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminParams, adminUpdateParam } from '@/api'

const params = ref([])
const loading = ref(true)

onMounted(async () => {
  try { params.value = await adminParams() } catch (e) { /* 忽略 */ } finally { loading.value = false }
})

async function save(row) {
  try {
    await adminUpdateParam(row.paramKey, row.paramValue)
    ElMessage.success('已保存')
  } catch (e) { ElMessage.error(e.message || '保存失败') }
}
</script>
