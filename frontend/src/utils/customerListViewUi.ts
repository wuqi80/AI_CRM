import type { CustomerListVO, CustomerStage } from '@/types/customer'

/** 看板列投放区视觉：与阶段视图模板一致，已成交/已流失使用绿/红强调 */
export type KanbanDropTone = 'neutral' | 'success' | 'danger'

export interface StageColumn {
  id: CustomerStage
  label: string
  laneClass: string
  titleClass: string
  countBadgeClass: string
  dropTone: KanbanDropTone
}

type StageStyle = Omit<StageColumn, 'id' | 'label'>

/** 内置阶段的看板配色；用户新增的阶段走 DEFAULT_STAGE_STYLE */
const STAGE_STYLE: Record<string, StageStyle> = {
  lead: {
    laneClass: 'bg-[#EBECF0]/50 dark:bg-slate-800/55',
    titleClass: 'text-slate-500 dark:text-slate-300',
    countBadgeClass: 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200',
    dropTone: 'neutral'
  },
  qualified: {
    laneClass: 'bg-[#E2E8F0]/40 dark:bg-blue-950/30',
    titleClass: 'text-[#137FEC] dark:text-blue-300',
    countBadgeClass: 'bg-blue-100 text-[#137FEC] dark:bg-blue-500/15 dark:text-blue-200',
    dropTone: 'neutral'
  },
  proposal: {
    laneClass: 'bg-[#E9EDFF]/40 dark:bg-indigo-950/30',
    titleClass: 'text-primary',
    countBadgeClass: 'bg-primary text-white dark:text-slate-950',
    dropTone: 'neutral'
  },
  negotiation: {
    laneClass: 'bg-indigo-50/50 dark:bg-indigo-950/25',
    titleClass: 'text-slate-600 dark:text-indigo-200',
    countBadgeClass: 'bg-slate-200 text-slate-700 dark:bg-indigo-500/15 dark:text-indigo-200',
    dropTone: 'neutral'
  },
  closed: {
    laneClass: 'bg-green-50/50 dark:bg-emerald-950/25',
    titleClass: 'text-green-600 dark:text-emerald-300',
    countBadgeClass: 'bg-green-100 text-green-700 dark:bg-emerald-500/15 dark:text-emerald-200',
    dropTone: 'success'
  },
  lost: {
    laneClass: 'bg-red-50/50 dark:bg-red-950/25',
    titleClass: 'text-red-600 dark:text-red-300',
    countBadgeClass: 'bg-red-100 text-red-700 dark:bg-red-500/15 dark:text-red-200',
    dropTone: 'danger'
  }
}

const DEFAULT_STAGE_STYLE: StageStyle = {
  laneClass: 'bg-[#EBECF0]/50 dark:bg-slate-800/55',
  titleClass: 'text-slate-500 dark:text-slate-300',
  countBadgeClass: 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200',
  dropTone: 'neutral'
}

/** 由选项（真相源）构建看板列，未知阶段使用默认样式 */
export function buildStageColumns(options: { value: string; label: string }[]): StageColumn[] {
  return options.map(option => ({
    id: option.value as CustomerStage,
    label: option.label,
    ...(STAGE_STYLE[option.value] ?? DEFAULT_STAGE_STYLE)
  }))
}

/** 客户列表阶段看板列（内置默认，store 未加载时作为兜底） */
export const KANBAN_STAGE_COLUMNS: StageColumn[] = buildStageColumns([
  { value: 'lead', label: '线索' },
  { value: 'qualified', label: '资格审查' },
  { value: 'proposal', label: '方案报价' },
  { value: 'negotiation', label: '谈判中' },
  { value: 'closed', label: '已成交' },
  { value: 'lost', label: '已流失' }
])

export function normalizeListStage(stage: string | undefined | null): CustomerStage {
  // 不再用白名单强制归一，保留用户新增阶段的原值（空值落到 lead）
  return (stage || 'lead') as CustomerStage
}

export function formatCardQuotation(q: number | undefined | null): string {
  if (q === null || q === undefined) return '-'
  const n = Number(q)
  if (!Number.isFinite(n)) return '-'
  return `¥${n.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`
}

export function formatLastContactDate(v: string | undefined | null): string {
  if (!v) return '未跟进'
  return new Date(v).toLocaleDateString('zh-CN')
}

export function lastFollowUpHighlightClass(v: string | undefined | null): string {
  if (!v) return 'text-slate-400'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return 'text-slate-600'
  const stale = d.getTime() < Date.now() - 30 * 24 * 60 * 60 * 1000
  return stale ? 'rounded bg-red-50 px-1.5 py-0.5 font-medium text-red-600' : 'text-slate-600'
}

export function customersInStage(customers: CustomerListVO[], stage: CustomerStage): CustomerListVO[] {
  return customers.filter(r => normalizeListStage(r.stage) === stage)
}
