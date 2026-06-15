<template>
  <div class="min-h-full bg-slate-50">
    <section class="border-b border-slate-200 bg-white px-4 py-5 md:px-8">
      <div class="max-w-5xl">
        <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 class="text-xl font-bold text-slate-900">全局搜索</h1>
            <p class="mt-1 text-sm text-slate-500">{{ resultSummary }}</p>
          </div>
          <div class="relative w-full md:max-w-lg">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-xl text-slate-400">search</span>
            <input
              v-model="searchInput"
              type="text"
              class="w-full rounded-lg border border-slate-200 bg-slate-50 py-2.5 pl-10 pr-24 text-sm text-slate-900 outline-none transition focus:border-primary focus:bg-white focus:ring-2 focus:ring-primary/20"
              placeholder="搜索客户、联系人、任务、日程、知识库"
              @keydown.enter="submitSearch(1)"
            />
            <button
              class="absolute right-1.5 top-1/2 -translate-y-1/2 rounded-md bg-primary px-4 py-1.5 text-sm font-medium text-white transition hover:bg-primary/90"
              @click="submitSearch(1)"
            >
              搜索
            </button>
          </div>
        </div>
      </div>
    </section>

    <section class="px-4 py-5 md:px-8">
      <div class="max-w-5xl space-y-4">
        <div class="flex gap-2 overflow-x-auto pb-1">
          <button
            v-for="option in visibleTypeOptions"
            :key="option.value"
            class="shrink-0 rounded-lg border px-3 py-2 text-sm font-medium transition"
            :class="activeType === option.value
              ? 'border-primary bg-primary text-white'
              : 'border-slate-200 bg-white text-slate-600 hover:border-primary/40 hover:text-primary'"
            @click="changeType(option.value)"
          >
            {{ option.label }}
          </button>
        </div>

        <div v-if="loading" class="flex items-center justify-center py-20 text-slate-400">
          <span class="material-symbols-outlined animate-spin text-3xl">progress_activity</span>
        </div>

        <div v-else-if="!normalizedKeyword" class="rounded-lg border border-dashed border-slate-200 bg-white py-20 text-center text-slate-400">
          <span class="material-symbols-outlined text-5xl">manage_search</span>
          <p class="mt-3 text-sm">输入关键词开始搜索</p>
        </div>

        <div v-else-if="results.length === 0" class="rounded-lg border border-dashed border-slate-200 bg-white py-20 text-center text-slate-400">
          <span class="material-symbols-outlined text-5xl">search_off</span>
          <p class="mt-3 text-sm">未找到匹配结果</p>
        </div>

        <div v-else class="overflow-hidden rounded-lg border border-slate-200 bg-white">
          <button
            v-for="item in results"
            :key="`${item.type}-${item.recordId}`"
            class="flex w-full items-start gap-4 border-b border-slate-100 px-4 py-4 text-left transition last:border-b-0 hover:bg-slate-50 md:px-5"
            @click="openResult(item)"
          >
            <span class="material-symbols-outlined mt-0.5 shrink-0 rounded-lg bg-slate-100 p-2 text-slate-500">
              {{ iconByType[item.type] }}
            </span>
            <span class="min-w-0 flex-1">
              <span class="flex flex-wrap items-center gap-2">
                <span class="truncate text-sm font-semibold text-slate-900">{{ item.title || '未命名记录' }}</span>
                <span class="rounded bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-500">{{ labelByType[item.type] }}</span>
              </span>
              <span v-if="item.subtitle" class="mt-1 block truncate text-xs text-slate-500">{{ item.subtitle }}</span>
              <span v-if="item.content" class="mt-2 block line-clamp-2 text-sm leading-6 text-slate-600">{{ item.content }}</span>
            </span>
            <span class="hidden shrink-0 text-xs font-medium text-slate-400 md:block">{{ formatTime(item.eventTime || item.updateTime || item.createTime) }}</span>
          </button>
        </div>

        <div v-if="totalPages > 1" class="flex items-center justify-end gap-2">
          <button
            class="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600 transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-40"
            :disabled="page <= 1"
            @click="submitSearch(page - 1)"
          >
            上一页
          </button>
          <button
            v-for="item in visiblePages"
            :key="item"
            class="size-9 rounded-lg border text-sm font-medium transition"
            :class="page === item ? 'border-primary bg-primary text-white' : 'border-slate-200 bg-white text-slate-600 hover:border-primary hover:text-primary'"
            @click="submitSearch(item)"
          >
            {{ item }}
          </button>
          <button
            class="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600 transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-40"
            :disabled="page >= totalPages"
            @click="submitSearch(page + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { queryGlobalSearch } from '@/api/search'
import { useUserStore } from '@/stores/user'
import type { GlobalSearchResultVO, GlobalSearchType } from '@/types/search'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const searchInput = ref('')
const activeType = ref<GlobalSearchType>('all')
const page = ref(1)
const pageSize = 20
const totalRow = ref(0)
const loading = ref(false)
const results = ref<GlobalSearchResultVO[]>([])

const typeOptions: Array<{ value: GlobalSearchType; label: string; permission?: string }> = [
  { value: 'all', label: '全部' },
  { value: 'customer', label: '客户', permission: 'customer:view' },
  { value: 'contact', label: '联系人', permission: 'contact:view' },
  { value: 'task', label: '任务', permission: 'task:view' },
  { value: 'schedule', label: '日程', permission: 'schedule:view' },
  { value: 'knowledge', label: '知识库', permission: 'knowledge:view' }
]

const labelByType: Record<Exclude<GlobalSearchType, 'all'>, string> = {
  customer: '客户',
  contact: '联系人',
  task: '任务',
  schedule: '日程',
  knowledge: '知识库'
}

const iconByType: Record<Exclude<GlobalSearchType, 'all'>, string> = {
  customer: 'apartment',
  contact: 'person',
  task: 'task_alt',
  schedule: 'event',
  knowledge: 'article'
}

const visibleTypeOptions = computed(() =>
  typeOptions.filter(option => !option.permission || userStore.hasPermission(option.permission))
)

const normalizedKeyword = computed(() => searchInput.value.trim())
const totalPages = computed(() => Math.ceil(totalRow.value / pageSize))

const resultSummary = computed(() => {
  if (!normalizedKeyword.value) return '搜索 CRM 数据'
  if (loading.value) return '正在搜索...'
  return `找到 ${totalRow.value} 条结果`
})

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = page.value
  const pages: number[] = []
  let start = Math.max(1, current - 2)
  const end = Math.min(total, start + 4)
  start = Math.max(1, end - 4)
  for (let i = start; i <= end; i += 1) pages.push(i)
  return pages
})

watch(
  () => route.query,
  () => {
    searchInput.value = firstQueryValue(route.query.keyword)
    activeType.value = normalizeType(firstQueryValue(route.query.type))
    if (!visibleTypeOptions.value.some(option => option.value === activeType.value)) {
      activeType.value = 'all'
    }
    page.value = normalizePage(firstQueryValue(route.query.page))
    fetchResults()
  },
  { immediate: true }
)

function firstQueryValue(value: unknown): string {
  if (Array.isArray(value)) return value[0] ? String(value[0]) : ''
  return value == null ? '' : String(value)
}

function normalizePage(value: string): number {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : 1
}

function normalizeType(value: string): GlobalSearchType {
  const type = value as GlobalSearchType
  return typeOptions.some(option => option.value === type) ? type : 'all'
}

function submitSearch(targetPage: number) {
  const keyword = normalizedKeyword.value
  const query: Record<string, string> = {}
  if (keyword) query.keyword = keyword
  if (activeType.value !== 'all') query.type = activeType.value
  if (targetPage > 1) query.page = String(targetPage)
  router.push({ path: '/search', query })
}

function changeType(type: GlobalSearchType) {
  activeType.value = type
  submitSearch(1)
}

async function fetchResults() {
  const keyword = normalizedKeyword.value
  if (!keyword) {
    results.value = []
    totalRow.value = 0
    return
  }

  loading.value = true
  try {
    const response = await queryGlobalSearch({
      keyword,
      type: activeType.value,
      page: page.value,
      limit: pageSize
    })
    results.value = response.list || []
    totalRow.value = response.totalRow || 0
  } finally {
    loading.value = false
  }
}

function openResult(item: GlobalSearchResultVO) {
  if (item.type === 'customer') {
    router.push(`/customer/${item.recordId}`)
    return
  }
  if (item.type === 'contact' && item.customerId) {
    router.push({ path: `/customer/${item.customerId}`, query: { contactId: item.recordId } })
    return
  }
  if (item.type === 'task') {
    router.push({ path: '/task', query: buildRecordQuery('taskId', item) })
    return
  }
  if (item.type === 'schedule') {
    router.push({ path: '/calendar', query: { scheduleId: item.recordId } })
    return
  }
  if (item.type === 'knowledge') {
    router.push({ path: '/knowledge', query: buildRecordQuery('knowledgeId', item) })
  }
}

function buildRecordQuery(idName: string, item: GlobalSearchResultVO): Record<string, string> {
  const query: Record<string, string> = { [idName]: String(item.recordId) }
  if (item.title) query.keyword = item.title
  return query
}

function formatTime(value?: string): string {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}
</script>
