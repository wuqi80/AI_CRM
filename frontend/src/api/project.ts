import { get, post } from '@/utils/request'
import type {
  ProjectCreate,
  ProjectAttachmentPayload,
  ProjectLaneSave,
  ProjectPageResult,
  ProjectQuery,
  ProjectSchedulePayload,
  ProjectTaskAttachmentPayload,
  ProjectTaskMove,
  ProjectTaskSave,
  ProjectUpdate,
  ProjectVO
} from '@/types/project'

export function queryProjectPageList(query: ProjectQuery): Promise<ProjectPageResult> {
  return post('/project/queryPageList', query)
}

export function getProjectDetail(projectId: string, taskKeyword?: string): Promise<ProjectVO> {
  return get(`/project/detail/${projectId}`, { params: { taskKeyword } })
}

export function addProject(data: ProjectCreate): Promise<ProjectVO> {
  return post('/project/add', data)
}

export function updateProject(data: ProjectUpdate): Promise<ProjectVO> {
  return post('/project/update', data)
}

export function archiveProject(projectId: string): Promise<ProjectVO> {
  return post(`/project/archive/${projectId}`)
}

export function restoreProject(projectId: string): Promise<ProjectVO> {
  return post(`/project/restore/${projectId}`)
}

export function addProjectAttachment(projectId: string, data: ProjectAttachmentPayload): Promise<ProjectVO> {
  return post(`/project/${projectId}/attachment/add`, data)
}

export function deleteProjectAttachment(projectId: string, attachmentId: string): Promise<ProjectVO> {
  return post(`/project/${projectId}/attachment/delete/${attachmentId}`)
}

export function addProjectSchedule(projectId: string, data: ProjectSchedulePayload): Promise<ProjectVO> {
  return post(`/project/${projectId}/schedule/add`, data)
}

export function deleteProjectSchedule(projectId: string, scheduleId: string): Promise<ProjectVO> {
  return post(`/project/${projectId}/schedule/delete/${scheduleId}`)
}

export function deleteProject(projectId: string): Promise<void> {
  return post(`/project/delete/${projectId}`)
}

export function addProjectLane(projectId: string, data: ProjectLaneSave): Promise<ProjectVO> {
  return post(`/project/${projectId}/lane/add`, data)
}

export function addProjectTask(projectId: string, data: ProjectTaskSave): Promise<ProjectVO> {
  return post(`/project/${projectId}/task/add`, data)
}

export function updateProjectTask(projectId: string, data: ProjectTaskSave): Promise<ProjectVO> {
  return post(`/project/${projectId}/task/update`, data)
}

export function addProjectTaskAttachment(
  projectId: string,
  taskId: string,
  data: ProjectTaskAttachmentPayload
): Promise<ProjectVO> {
  return post(`/project/${projectId}/task/${taskId}/attachment/add`, data)
}

export function deleteProjectTaskAttachment(
  projectId: string,
  taskId: string,
  attachmentId: string
): Promise<ProjectVO> {
  return post(`/project/${projectId}/task/${taskId}/attachment/delete/${attachmentId}`)
}

export function moveProjectTask(projectId: string, data: ProjectTaskMove): Promise<ProjectVO> {
  return post(`/project/${projectId}/task/move`, data)
}

export function deleteProjectTask(projectId: string, taskId: string): Promise<ProjectVO> {
  return post(`/project/${projectId}/task/delete/${taskId}`)
}
