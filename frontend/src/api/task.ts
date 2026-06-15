import { post, get } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { Task, TaskAddBO, TaskUpdateBO, TaskQueryBO } from '@/types/common'
import { normalizeTaskList, normalizeTaskPriorityPayload } from '@/utils/taskPriority'

/**
 * Create task
 */
export function addTask(data: TaskAddBO): Promise<string> {
  return post('/task/add', normalizeTaskPriorityPayload(data))
}

/**
 * Update task
 */
export function updateTask(data: TaskUpdateBO): Promise<void> {
  return post('/task/update', normalizeTaskPriorityPayload(data))
}

/**
 * Delete task
 */
export function deleteTask(id: string): Promise<void> {
  return post(`/task/delete/${id}`)
}

/**
 * Query tasks with pagination
 */
export async function queryTaskList(query: TaskQueryBO): Promise<PageResult<Task>> {
  const result = await post<PageResult<Task>>('/task/queryPageList', query)
  return {
    ...result,
    list: normalizeTaskList(result.list)
  }
}

/**
 * Update task status
 */
export function updateTaskStatus(taskId: string, status: string): Promise<void> {
  return post('/task/updateStatus', null, { params: { taskId, status } })
}

/**
 * Get my tasks
 */
export async function getMyTasks(filter: string = 'all'): Promise<Task[]> {
  const tasks = await get<Task[]>('/task/myTasks', { params: { filter } })
  return normalizeTaskList(tasks)
}

/**
 * AI parse task from natural language
 */
export interface TaskAiParseVO {
  title: string
  dueDate: string
  priority: string
  taskType: string
  customerName: string
  participantNames: string
  assignedToName: string
  description: string
}

export function aiParseTask(content: string): Promise<TaskAiParseVO> {
  return post('/task/ai-parse', { content })
}
