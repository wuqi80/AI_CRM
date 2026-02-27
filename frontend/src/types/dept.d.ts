export interface DeptVO {
  deptId: string
  deptName: string
  parentId: string
  sortOrder: number
  userCount: number
  children?: DeptVO[]
}

export interface DeptAddBO {
  deptName: string
  parentId?: string | number
  sortOrder?: number
}

export interface DeptUpdateBO {
  deptId: string | number
  deptName: string
  parentId?: string | number
  sortOrder?: number
}
