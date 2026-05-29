<template>
  <div
    class="bg-gradient-to-r from-indigo-950/40 to-slate-800 border border-indigo-500/30 rounded-xl p-6 animate-slide-up">
    <!-- 头部信息 -->
    <div class="flex flex-wrap items-center gap-3 mb-4">
      <h3 class="text-md font-bold text-indigo-400 uppercase tracking-wider flex items-center gap-2">
        <span>📋</span> AI 变更概要总结
      </h3>
      <span v-if="model"
        class="bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 text-xs px-2 py-0.5 rounded-full font-mono">
        {{ model }}
      </span>
      <span v-if="fileCount != null" class="bg-slate-700/50 text-slate-400 text-xs px-2 py-0.5 rounded-full">
        {{ fileCount }} 个文件变更
      </span>
    </div>

    <!-- 总结内容 -->
    <p class="text-slate-300 leading-relaxed text-sm">{{ summary }}</p>
  </div>
</template>

<script setup>
defineProps({
  summary: { type: String, default: '' },
  model: { type: String, default: '' },
  fileCount: { type: Number, default: 0 }
})
</script>
