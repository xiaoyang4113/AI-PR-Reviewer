<template>
  <div
    class="bg-slate-800/80 border border-slate-700 rounded-xl overflow-hidden shadow-md hover:border-slate-600/50 transition-colors">
    <!-- 文件头部信息 -->
    <div class="bg-slate-900/80 px-4 py-3 border-b border-slate-700 flex flex-wrap items-center justify-between gap-2">
      <div class="flex items-center gap-2 font-mono text-xs text-slate-300 min-w-0">
        <span class="bg-slate-700 text-slate-300 px-2 py-0.5 rounded shrink-0">FILE</span>
        <span class="font-semibold text-slate-100 truncate">{{ comment.filePath }}</span>
        <span v-if="comment.lineNumber" class="text-slate-500 shrink-0">: 第 {{ comment.lineNumber }} 行</span>
      </div>

      <!-- 风险等级标签 -->
      <span :class="riskBadgeClass"
        class="px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider shrink-0">
        {{ riskLabel }}
      </span>
    </div>

    <!-- 评审意见 -->
    <div class="p-4 space-y-3">
      <p class="text-slate-300 text-sm leading-relaxed">
        <span class="text-indigo-400 font-bold">评审意见：</span>
        {{ comment.suggestion }}
      </p>

      <!-- 代码对比区域 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 pt-2 font-mono text-xs">
        <!-- 问题代码 -->
        <div v-if="comment.matchCode" class="bg-red-950/20 border border-red-900/50 rounded-lg p-3 overflow-x-auto">
          <div class="text-red-400 font-bold mb-1 border-b border-red-900/30 pb-1">⚠️ 触发风险代码</div>
          <pre class="text-red-300/90 whitespace-pre-wrap"><code>{{ comment.matchCode }}</code></pre>
        </div>

        <!-- 优化代码 -->
        <div v-if="comment.optimizedCode"
          class="bg-emerald-950/20 border border-emerald-900/50 rounded-lg p-3 overflow-x-auto">
          <div class="text-emerald-400 font-bold mb-1 border-b border-emerald-900/30 pb-1">💡 建议优化方案</div>
          <pre class="text-emerald-300/90 whitespace-pre-wrap"><code>{{ comment.optimizedCode }}</code></pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  comment: { type: Object, required: true }
})

/** 风险等级对应的 Tailwind 样式 */
const riskBadgeClass = computed(() => {
  switch (props.comment.riskLevel) {
    case 'CRITICAL':
      return 'bg-red-500/10 text-red-400 border border-red-500/20'
    case 'WARNING':
      return 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
    default:
      return 'bg-blue-500/10 text-blue-400 border border-blue-500/20'
  }
})

/** 风险等级中文 */
const riskLabel = computed(() => {
  switch (props.comment.riskLevel) {
    case 'CRITICAL': return '🔴 严重'
    case 'WARNING': return '🟡 警告'
    default: return '🔵 建议'
  }
})
</script>
