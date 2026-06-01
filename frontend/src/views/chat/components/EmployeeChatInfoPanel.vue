<template>
  <div class="flex h-full flex-col bg-white">
    <div class="shrink-0 border-b border-slate-100 px-5 py-5">
      <div class="flex items-start gap-3">
        <img v-if="employee.imgUrl" :src="employee.imgUrl" class="size-14 shrink-0 rounded-2xl object-cover" alt="avatar" />
        <div v-else class="flex size-14 shrink-0 items-center justify-center rounded-2xl bg-slate-900 text-lg font-bold text-white">
          {{ employeeInitial }}
        </div>
        <div class="min-w-0 flex-1">
          <div class="flex min-w-0 items-center gap-2">
            <h3 class="truncate text-base font-bold text-slate-900">{{ employeeName }}</h3>
            <span class="shrink-0 rounded-full px-2 py-1 text-[11px] font-bold" :class="statusClass">{{ statusLabel }}</span>
          </div>
          <p class="mt-1 truncate text-sm text-slate-500">{{ employee.post || '员工' }}</p>
          <p class="mt-0.5 truncate text-xs text-slate-400">{{ employee.deptName || '-' }}</p>
        </div>
      </div>

      <div class="mt-5 grid grid-cols-2 gap-3 text-sm">
        <InfoItem label="手机" :value="employee.mobile" />
        <InfoItem label="邮箱" :value="employee.email" />
        <InfoItem label="上级" :value="employee.parentName || '无'" />
        <InfoItem label="部门" :value="employee.deptName" />
      </div>
    </div>

    <div class="min-h-0 flex-1 overflow-y-auto px-5 py-4">
      <PanelSection title="相关任务" icon="task_alt" :empty="tasks.length === 0">
        <div class="space-y-2">
          <div v-for="task in tasks" :key="task.taskId" class="rounded-xl border border-slate-100 bg-slate-50 px-3 py-2.5">
            <div class="flex items-start justify-between gap-2">
              <p class="min-w-0 flex-1 truncate text-sm font-semibold text-slate-800">{{ task.title }}</p>
              <span class="shrink-0 rounded-full bg-white px-2 py-0.5 text-[11px] font-bold text-slate-500">{{ task.statusName || task.status || '-' }}</span>
            </div>
            <p class="mt-1 truncate text-xs text-slate-400">{{ formatDate(task.dueDate) }}</p>
          </div>
        </div>
      </PanelSection>

      <PanelSection title="相关日程" icon="event" :empty="schedules.length === 0">
        <div class="space-y-2">
          <div v-for="schedule in schedules" :key="schedule.scheduleId" class="rounded-xl border border-slate-100 bg-slate-50 px-3 py-2.5">
            <p class="truncate text-sm font-semibold text-slate-800">{{ schedule.title }}</p>
            <p class="mt-1 truncate text-xs text-slate-400">{{ formatDate(schedule.startTime) }}</p>
          </div>
        </div>
      </PanelSection>

      <PanelSection title="相关附件" icon="description" :empty="attachments.length === 0">
        <div class="space-y-2">
          <div
            v-for="attachment in attachments"
            :key="attachment.knowledgeId"
            class="flex items-center gap-3 rounded-xl border border-slate-100 bg-slate-50 px-3 py-2.5"
          >
            <span class="material-symbols-outlined shrink-0 text-[20px] text-slate-400">description</span>
            <span class="min-w-0 flex-1 truncate text-sm font-semibold text-slate-700">{{ attachment.name }}</span>
          </div>
        </div>
      </PanelSection>

      <PanelSection title="最近记录" icon="history" :empty="records.length === 0">
        <div class="space-y-3">
          <div v-for="record in records" :key="`${record.type}-${record.title}-${record.recordTime}`" class="flex gap-3">
            <span class="mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-500">
              <span class="material-symbols-outlined text-[16px] leading-none">{{ recordIcon(record.type) }}</span>
            </span>
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-semibold text-slate-800">{{ record.title || '-' }}</p>
              <p class="mt-0.5 truncate text-xs text-slate-400">{{ formatDate(record.recordTime) }}</p>
            </div>
          </div>
        </div>
      </PanelSection>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h } from 'vue'
import type { AddressBookDetail } from '@/types/addressBook'

const props = defineProps<{
  employee: AddressBookDetail
}>()

const employeeName = computed(() => props.employee.realname || '未命名员工')
const employeeInitial = computed(() => employeeName.value.charAt(0) || '?')
const tasks = computed(() => props.employee.relatedTasks || [])
const schedules = computed(() => props.employee.relatedSchedules || [])
const attachments = computed(() => props.employee.relatedAttachments || [])
const records = computed(() => props.employee.recentRecords || [])

const normalizedStatus = computed(() => {
  const value = String(props.employee.employeeStatus || '').trim()
  return value === 'resigned' || value === 'disabled' ? value : 'active'
})

const statusLabel = computed(() => {
  if (props.employee.employeeStatusName) return props.employee.employeeStatusName
  if (normalizedStatus.value === 'resigned') return '离职'
  if (normalizedStatus.value === 'disabled') return '停用'
  return '在职'
})

const statusClass = computed(() => {
  if (normalizedStatus.value === 'resigned') return 'bg-slate-100 text-slate-500'
  if (normalizedStatus.value === 'disabled') return 'bg-amber-50 text-amber-600'
  return 'bg-emerald-50 text-emerald-600'
})

function formatDate(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function recordIcon(type?: string) {
  if (type === 'schedule') return 'event'
  if (type === 'attachment') return 'description'
  return 'task_alt'
}

const InfoItem = defineComponent({
  props: {
    label: { type: String, required: true },
    value: { type: String, default: '' }
  },
  setup(componentProps) {
    return () => h('div', { class: 'min-w-0 rounded-xl bg-slate-50 px-3 py-2.5' }, [
      h('p', { class: 'text-[11px] font-bold text-slate-400' }, componentProps.label),
      h('p', { class: 'mt-1 truncate text-sm font-semibold text-slate-700', title: componentProps.value || '-' }, componentProps.value || '-')
    ])
  }
})

const PanelSection = defineComponent({
  props: {
    title: { type: String, required: true },
    icon: { type: String, required: true },
    empty: { type: Boolean, default: false }
  },
  setup(componentProps, { slots }) {
    return () => h('section', { class: 'mb-5' }, [
      h('div', { class: 'mb-2 flex items-center gap-2' }, [
        h('span', { class: 'material-symbols-outlined text-[18px] text-slate-400 leading-none' }, componentProps.icon),
        h('h4', { class: 'text-sm font-bold text-slate-900' }, componentProps.title)
      ]),
      componentProps.empty
        ? h('p', { class: 'rounded-xl bg-slate-50 px-3 py-3 text-sm text-slate-400' }, '-')
        : slots.default?.()
    ])
  }
})
</script>
