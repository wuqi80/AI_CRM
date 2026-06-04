import { post } from '@/utils/request'
import type { DeptVO, DeptAddBO, DeptUpdateBO } from '@/types/dept'

/**
 * 查询部门树
 */
export function queryDeptTree(): Promise<DeptVO[]> {
  return post<DeptVO[]>('/managerDept/queryDeptTree')
}

/**
 * 添加部门
 */
export function addDept(data: DeptAddBO): Promise<void> {
  return post('/managerDept/add', data)
}

/**
 * 修改部门
 */
export function updateDept(data: DeptUpdateBO): Promise<void> {
  return post('/managerDept/update', data)
}

/**
 * 删除部门
 */
export function deleteDept(deptId: string | number): Promise<void> {
  return post('/managerDept/delete', null, { params: { deptId } })
}
