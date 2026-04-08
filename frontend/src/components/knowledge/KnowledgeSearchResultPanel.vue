<template>
  <div class="mx-auto max-w-6xl">
    <div class="mb-8 flex flex-wrap items-start justify-between gap-4">
      <div class="flex items-start gap-4">
        <button
          type="button"
          class="mt-1 flex size-10 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-500 transition-all hover:border-primary/30 hover:text-primary"
          @click="$emit('back')"
        >
          <span class="material-symbols-outlined text-lg">arrow_back</span>
        </button>
        <div>
          <h3 class="text-3xl font-black tracking-tight text-slate-950">AI 检索结果</h3>
          <p class="mt-2 text-sm text-slate-500">“{{ keyword }}”</p>
        </div>
      </div>

      <div v-if="result && !loading" class="flex items-center gap-6 text-xs text-slate-400">
        <span>检索耗时: {{ formatElapsed(result.tookMs) }}</span>
        <span class="font-bold text-primary">匹配度: {{ result.matchPercent }}%</span>
      </div>
    </div>

    <div class="space-y-10">
      <section class="overflow-hidden rounded-[2rem] border border-[#cfe1ff] bg-[linear-gradient(180deg,#eff6ff_0%,#eef5ff_100%)] p-6 shadow-[0_18px_45px_rgba(59,130,246,0.08)] md:p-8">
        <div class="mb-6 flex items-center gap-3 text-primary">
          <div class="flex size-9 items-center justify-center rounded-2xl bg-white/80 shadow-sm">
            <span class="material-symbols-outlined text-lg">neurology</span>
          </div>
          <div>
            <p class="text-sm font-black tracking-wide">AI 智能解答</p>
            <p class="text-xs text-primary/70">基于知识库内容生成的结论与依据</p>
          </div>
        </div>

        <div v-if="loading" class="space-y-4">
          <div class="h-5 w-48 animate-pulse rounded-full bg-white/70" />
          <div class="space-y-3 rounded-[1.75rem] bg-white/70 p-6">
            <div class="h-4 animate-pulse rounded-full bg-slate-200/70" />
            <div class="h-4 w-11/12 animate-pulse rounded-full bg-slate-200/70" />
            <div class="h-4 w-10/12 animate-pulse rounded-full bg-slate-200/70" />
            <div class="h-4 w-8/12 animate-pulse rounded-full bg-slate-200/70" />
          </div>
        </div>

        <div
          v-else-if="result?.answer"
          class="wk-markdown text-[15px] leading-8 text-slate-800"
          v-html="renderedAnswer"
        />

        <div v-else class="rounded-[1.75rem] bg-white/70 p-6 text-sm text-slate-500">
          当前没有生成可展示的解答内容。
        </div>
      </section>

      <section>
        <div class="mb-4 flex items-center justify-between gap-4">
          <p class="text-xs font-black uppercase tracking-[0.28em] text-slate-400">关联参考文档</p>
          <p v-if="result && !loading" class="text-xs text-slate-400">{{ result.totalHits }} 份命中文档</p>
        </div>

        <div v-if="loading" class="space-y-4">
          <div
            v-for="index in 3"
            :key="index"
            class="rounded-[1.75rem] border border-slate-200 bg-white p-6 shadow-sm"
          >
            <div class="mb-4 h-5 w-56 animate-pulse rounded-full bg-slate-200" />
            <div class="space-y-3">
              <div class="h-4 animate-pulse rounded-full bg-slate-100" />
              <div class="h-4 w-11/12 animate-pulse rounded-full bg-slate-100" />
            </div>
          </div>
        </div>

        <div v-else-if="!result?.references?.length" class="rounded-[1.75rem] border border-dashed border-slate-200 bg-white px-6 py-12 text-center text-sm text-slate-400">
          暂未找到相关参考文档。
        </div>

        <div v-else class="space-y-4">
          <article
            v-for="item in result.references"
            :key="item.knowledgeId"
            class="group flex flex-col gap-4 rounded-[1.75rem] border border-slate-200 bg-white p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:border-primary/20 hover:shadow-md md:flex-row md:items-center md:justify-between md:p-6"
          >
            <div class="flex min-w-0 flex-1 items-start gap-4">
              <div class="flex size-12 shrink-0 items-center justify-center rounded-2xl bg-slate-50 text-slate-500">
                <span class="material-symbols-outlined text-xl">{{ getTypeIcon(item.type) }}</span>
              </div>

              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
                  <h4 class="truncate text-lg font-black tracking-tight text-slate-900">{{ item.name }}</h4>
                  <span class="text-xs font-bold text-primary">相关度: {{ item.matchPercent }}%</span>
                </div>

                <p class="mt-1 text-xs text-slate-400">
                  <span v-if="item.customerName">{{ item.customerName }}</span>
                  <span v-if="item.customerName && item.createTime"> · </span>
                  <span v-if="item.createTime">更新于 {{ formatDate(item.createTime) }}</span>
                </p>

                <p class="mt-3 line-clamp-3 text-sm leading-7 text-slate-500">
                  {{ item.excerpt || item.summary || '暂无摘要' }}
                </p>
              </div>
            </div>

            <button
              type="button"
              class="shrink-0 rounded-full px-4 py-2 text-sm font-bold text-primary transition-all hover:bg-primary/5"
              @click="$emit('open', item.knowledgeId)"
            >
              立即阅读
            </button>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { KnowledgeAiSearchVO } from '@/types/common'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps<{
  loading: boolean
  keyword: string
  result: KnowledgeAiSearchVO | null
}>()

defineEmits<{
  back: []
  open: [knowledgeId: string | number]
}>()

const renderedAnswer = computed(() => renderMarkdown(props.result?.answer || ''))

function formatElapsed(ms?: number): string {
  if (!ms || ms <= 0) return '0.1s'
  if (ms < 1000) return `${(ms / 1000).toFixed(1)}s`
  return `${(ms / 1000).toFixed(1)}s`
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

function getTypeIcon(type?: string): string {
  const icons: Record<string, string> = {
    meeting: 'groups',
    email: 'mail',
    recording: 'mic',
    document: 'description',
    proposal: 'slideshow',
    contract: 'gavel'
  }
  return icons[(type || '').toLowerCase()] || 'description'
}
</script>

<style scoped>
.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
