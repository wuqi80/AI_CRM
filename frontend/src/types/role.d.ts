export interface RoleVO {
  roleId: string
  roleName: string
  realm: string
  description: string
  dataType: number
  userCount: number
  createTime: string
}

export interface ActionPerm {
  menuId: string
  action: string
  actionName: string
  enabled: boolean
  dataScope: number | null
  hasScopeOption: boolean
}

export interface RolePermissionVO {
  module: string
  moduleName: string
  actions: ActionPerm[]
}

export interface PermItem {
  menuId: string
  dataScope: number | null
}

export interface RolePermissionSaveBO {
  roleId: string
  permissions: PermItem[]
}
