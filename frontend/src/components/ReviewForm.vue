<template>
  <section class="bg-slate-800/50 border border-slate-700/60 rounded-xl p-6 shadow-xl backdrop-blur-sm">
    <h2 class="text-lg font-semibold mb-4 text-slate-200 flex items-center gap-2">
      <span>🔍</span> 提交新的 Pull Request 评审任务
    </h2>

    <div class="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
      <!-- GitHub 仓库地址 -->
      <div class="md:col-span-2">
        <label class="block text-xs font-medium text-slate-400 mb-2">GitHub 仓库链接</label>
        <input v-model="form.repoUrl" type="text" placeholder="https://github.com/owner/repo" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-2.5 text-sm text-slate-200
                 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500/50
                 transition disabled:opacity-50 disabled:cursor-not-allowed" :disabled="loading"
          @keyup.enter="submit" />
      </div>

      <!-- PR 编号 -->
      <div>
        <label class="block text-xs font-medium text-slate-400 mb-2">PR 编号</label>
        <input v-model.number="form.prNumber" type="number" min="1" placeholder="例如: 42" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-2.5 text-sm text-slate-200
                 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500/50
                 transition disabled:opacity-50 disabled:cursor-not-allowed" :disabled="loading"
          @keyup.enter="submit" />
      </div>

      <!-- 提交按钮 -->
      <div>
        <button @click="submit" :disabled="loading || !isFormValid" class="w-full bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-700 disabled:cursor-not-allowed
                 text-white font-medium text-sm px-4 py-2.5 rounded-lg transition shadow-lg shadow-indigo-600/20
                 flex items-center justify-center gap-2">
          <!-- 加载动画 -->
          <svg v-if="loading" class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none"
            viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          {{ loading ? 'AI 正在深度审查中...' : '开始智能评审' }}
        </button>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="errorMsg" class="mt-4 bg-red-500/10 border border-red-500/20 rounded-lg px-4 py-3 text-sm text-red-400">
      {{ errorMsg }}
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'

const emit = defineEmits(['submit'])

const form = reactive({
  repoUrl: '',
  prNumber: null
})

const loading = ref(false)
const errorMsg = ref('')

const isFormValid = computed(() => {
  return form.repoUrl.trim() && form.prNumber && form.prNumber > 0
})

const submit = () => {
  errorMsg.value = ''

  if (!form.repoUrl.trim()) {
    errorMsg.value = '请输入 GitHub 仓库地址'
    return
  }
  if (!form.prNumber || form.prNumber <= 0) {
    errorMsg.value = '请输入有效的 PR 编号'
    return
  }

  loading.value = true
  emit('submit', {
    repoUrl: form.repoUrl.trim(),
    prNumber: form.prNumber
  })
}

/** 父组件调用：停止加载 */
const stopLoading = () => {
  loading.value = false
}

/** 父组件调用：显示错误 */
const showError = (msg) => {
  errorMsg.value = msg
  loading.value = false
}

defineExpose({ stopLoading, showError, loading })
</script>
