<template>
  <div class="min-h-screen bg-slate-900 text-slate-100 font-sans">
    <!-- ===================== 顶部导航栏 ===================== -->
    <header
      class="border-b border-slate-800 bg-slate-900/50 backdrop-blur sticky top-0 z-50 px-6 py-4 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="bg-indigo-600 p-2 rounded-lg text-white font-black tracking-wider text-sm">AI</div>
        <h1 class="text-xl font-bold bg-gradient-to-r from-white to-slate-400 bg-clip-text text-transparent">
          XEngineer Code Reviewer
          <span class="text-xs text-indigo-400 font-mono ml-1">v1.0</span>
        </h1>
      </div>
      <div class="text-xs text-slate-500 font-mono hidden sm:block">
        七牛云 XEngineer 暑期实训营 · 第二批次作品
      </div>
    </header>

    <!-- ===================== 主体内容区 ===================== -->
    <main class="max-w-7xl mx-auto p-6 space-y-8">
      <!-- 评审表单 -->
      <ReviewForm ref="formRef" @submit="handleSubmit" />

      <!-- ========== 评审结果区 ========== -->
      <template v-if="result">
        <!-- Mock 降级警告横幅 -->
        <div v-if="result.mockMode"
             class="bg-amber-500/10 border border-amber-500/30 rounded-xl p-4 flex items-start gap-3 animate-fade-in">
          <span class="text-2xl shrink-0">⚠️</span>
          <div>
            <p class="text-amber-400 font-bold text-sm">当前展示的是演示数据（Mock）</p>
            <p class="text-amber-400/70 text-xs mt-1">
              AI API 密钥未配置或 API 调用失败，系统已自动降级为 Mock 演示数据。
              请检查 <code class="bg-amber-500/10 px-1 rounded">application-dev.yml</code> 中的
              <code class="bg-amber-500/10 px-1 rounded">app.ai.api-key</code> 配置是否正确，
              或确认 DeepSeek API 服务是否可用。
            </p>
          </div>
        </div>

        <!-- AI 总结 -->
        <SummaryCard :summary="result.summary" :model="result.aiModel" :file-count="result.fileCount" />

        <!-- 评审意见列表 -->
        <section class="space-y-4">
          <h3 class="text-lg font-bold text-slate-200 flex items-center gap-2">
            <span>🛠️</span>
            发现潜在缺陷与重构建议
            <span class="bg-slate-700 text-slate-300 text-xs px-2.5 py-0.5 rounded-full font-normal">
              {{ result.comments?.length || 0 }}
            </span>
          </h3>

          <div class="space-y-4 animate-fade-in">
            <CommentCard v-for="(comment, index) in result.comments" :key="index" :comment="comment"
              :style="{ animationDelay: `${index * 80}ms` }" />
          </div>
        </section>

        <!-- 无评论时 -->
        <div v-if="!result.comments?.length"
          class="bg-emerald-500/10 border border-emerald-500/20 rounded-xl p-6 text-center">
          <span class="text-3xl">🎉</span>
          <p class="text-emerald-400 text-sm mt-2">AI 未发现明显问题，代码质量良好！</p>
        </div>
      </template>

      <!-- ========== 空状态 ========== -->
      <div v-else-if="!loading" class="text-center py-20 border border-dashed border-slate-800 rounded-xl">
        <div class="text-4xl mb-3">🚀</div>
        <p class="text-slate-500 text-sm">输入 GitHub PR 链接，开始 AI 智能代码评审</p>
        <p class="text-slate-600 text-xs mt-2">支持公开仓库的 Pull Request 分析</p>
      </div>

      <!-- ========== 加载骨架屏 ========== -->
      <div v-if="loading" class="space-y-6 animate-fade-in">
        <div class="bg-gradient-to-r from-indigo-950/20 to-slate-800/50 border border-indigo-500/10 rounded-xl p-6">
          <div class="h-4 bg-slate-700/50 rounded w-40 mb-4 animate-pulse"></div>
          <div class="h-4 bg-slate-700/50 rounded w-3/4 animate-pulse"></div>
        </div>
        <div class="space-y-4">
          <div class="h-4 bg-slate-700/50 rounded w-48 animate-pulse"></div>
          <div class="bg-slate-800/50 border border-slate-700/30 rounded-xl p-6 space-y-3">
            <div class="h-4 bg-slate-700/30 rounded w-full animate-pulse"></div>
            <div class="h-4 bg-slate-700/30 rounded w-5/6 animate-pulse"></div>
            <div class="h-20 bg-slate-700/20 rounded-lg animate-pulse"></div>
          </div>
        </div>
      </div>
    </main>

    <!-- ===================== 评审历史 ===================== -->
    <section v-if="history.length" class="mt-12">
      <h3 class="text-lg font-bold text-slate-200 mb-4 flex items-center gap-2">
        <span>📚</span> 评审历史
      </h3>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div v-for="item in history" :key="item.id"
             class="bg-slate-800/60 border border-slate-700/50 rounded-lg p-4 hover:border-slate-600/50 transition-colors cursor-pointer"
             @click="viewDetail(item.id)">
          <div class="flex items-center justify-between mb-2">
            <span class="text-slate-200 font-medium text-sm truncate">{{ item.prTitle || 'PR #' + item.prNumber }}</span>
            <span :class="statusClass(item.status)" class="text-xs px-2 py-0.5 rounded-full font-mono">
              {{ statusLabel(item.status) }}
            </span>
          </div>
          <div class="text-xs text-slate-500 mb-1">{{ item.repoUrl }} · PR #{{ item.prNumber }}</div>
          <p class="text-slate-400 text-xs line-clamp-2">{{ item.summary || '暂无总结' }}</p>
        </div>
      </div>
      <div v-if="hasMore" class="text-center mt-4">
        <button @click="loadMore"
                class="bg-slate-800 hover:bg-slate-700 text-slate-300 text-sm px-6 py-2 rounded-lg transition border border-slate-700">
          加载更多
        </button>
      </div>
    </section>

    <!-- ===================== 底部 ===================== -->
    <footer class="border-t border-slate-800 py-4 text-center text-xs text-slate-600">
      AI PR Review 助手 · DeepSeek V4 Flash · GitHub API
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import CommentCard from './components/CommentCard.vue'
import ReviewForm from './components/ReviewForm.vue'
import SummaryCard from './components/SummaryCard.vue'

/** 后端 API 地址 */
const API_BASE = '/api/review'

const formRef = ref(null)
const result = ref(null)
const loading = ref(false)
const history = ref([])
const historyPage = ref(1)
const hasMore = ref(false)
const PAGE_SIZE = 10

/** 页面加载时获取评审历史 */
onMounted(async () => {
  await fetchHistory()
})

/** 加载更多历史记录 */
const loadMore = async () => {
  historyPage.value++
  await fetchHistory()
}

/** 请求历史列表 */
const fetchHistory = async () => {
  try {
    const res = await fetch(`${API_BASE}/list?page=${historyPage.value}&size=${PAGE_SIZE}`)
    const json = await res.json()
    if (json.code === 200 && json.data) {
      const items = json.data
      if (historyPage.value === 1) {
        history.value = items
      } else {
        history.value.push(...items)
      }
      hasMore.value = items.length === PAGE_SIZE
    }
  } catch { /* 静默失败 */ }
}

/** 点击历史记录项，获取完整详情并展示 */
const viewDetail = async (taskId) => {
  try {
    const res = await fetch(`${API_BASE}/${taskId}`)
    const json = await res.json()
    if (json.code === 200 && json.data) {
      result.value = json.data
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  } catch { /* 静默失败 */ }
}

/** 状态标签中文 */
const statusLabel = (s) => ({
  COMPLETED: '✅ 完成', PROCESSING: '⏳ 分析中', FAILED: '❌ 失败', PENDING: '⏸ 待处理'
}[s] || s)

/** 状态标签样式 */
const statusClass = (s) => ({
  COMPLETED: 'bg-emerald-500/10 text-emerald-400', PROCESSING: 'bg-amber-500/10 text-amber-400',
  FAILED: 'bg-red-500/10 text-red-400', PENDING: 'bg-slate-500/10 text-slate-400'
}[s] || 'bg-slate-500/10 text-slate-400')

/**
 * 处理表单提交
 */
const handleSubmit = async (formData) => {
  loading.value = true
  result.value = null

  try {
    const response = await fetch(`${API_BASE}/create`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData)
    })

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}))
      throw new Error(errorData.message || `请求失败 (${response.status})`)
    }

    const json = await response.json()
    if (json.code === 200 && json.data) {
      result.value = json.data
      // 刷新历史列表
      history.value = [json.data, ...history.value]
    } else {
      throw new Error(json.message || '返回数据格式异常')
    }
  } catch (error) {
    formRef.value?.showError(error.message || '网络异常，请检查后端服务是否启动')
  } finally {
    loading.value = false
    formRef.value?.stopLoading()
  }
}
</script>
