import { post } from '@/utils/request'
import type { RoleVO, RolePermissionVO, RolePermissionSaveBO } from '@/types/role'

/**
 * 查询角色列表（含用户数量）
 */
export function queryRoleList(search?: string): Promise<RoleVO[]> {
  return post<RoleVO[]>('/managerRole/queryRoleListWithUserCount', search ? { search } : {})
}

/**
 * 添加角色
 */
export function addRole(data: { roleName: string; description?: string }): Promise<void> {
  return post('/managerRole/add', data)
}

/**
 * 更新角色
 */
export function updateRole(data: { roleId: string; roleName: string; description?: string }): Promise<void> {
  return post('/managerRole/update', data)
}

/**
 * 删除角色
 */
export function deleteRole(roleId: string): Promise<void> {
  return post('/managerRole/delete', [roleId])
}

/**
 * 查询角色权限配置
 */
export function getRolePermissions(roleId: string): Promise<RolePermissionVO[]> {
  return post<RolePermissionVO[]>(`/managerRole/queryPermissions/${roleId}`)
}

/**
 * 保存角色权限配置
 */
export function saveRolePermissions(data: RolePermissionSaveBO): Promise<void> {
  return post('/managerRole/savePermissions', data)
}

/**
 * 角色关联用户
 */
export function addUsersToRole(userIds: string[], roleId: string): Promise<void> {
  return post('/managerRole/relatedUser', { userIds, roleIds: [roleId] })
}

/**
 * 取消角色关联用户
 */
export function removeUserFromRole(userId: string, roleId: string): Promise<void> {
  return post('/managerRole/unbindingUser', null, { params: { userId, roleId } })
}
