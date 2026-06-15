import type { Task, TaskPriority } from '@/types/common'

const TASK_PRIORITY_VALUES = ['HIGH', 'MEDIUM', 'LOW'] as const
const DEFAULT_TASK_PRIORITY: TaskPriority = 'MEDIUM'

export function normalizeTaskPriority(priority: unknown): TaskPriority {
  const normalized = String(priority ?? '').trim().toUpperCase()

  if (TASK_PRIORITY_VALUES.includes(normalized as TaskPriority)) {
    return normalized as TaskPriority
  }

  return DEFAULT_TASK_PRIORITY
}

export function normalizeOptionalTaskPriority(priority: unknown): TaskPriority | undefined {
  if (priority == null || String(priority).trim() === '') {
    return undefined
  }

  return normalizeTaskPriority(priority)
}

export function normalizeTask<T extends Task>(task: T): T {
  return {
    ...task,
    priority: normalizeTaskPriority(task.priority)
  }
}

export function normalizeTaskList<T extends Task>(tasks: T[] | null | undefined): T[] {
  return (tasks || []).map(normalizeTask)
}

export function normalizeTaskPriorityPayload<T extends { priority?: unknown }>(data: T): T {
  return {
    ...data,
    priority: normalizeOptionalTaskPriority(data.priority)
  } as T
}
