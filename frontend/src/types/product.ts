export type ProductStatus = 'active' | 'inactive'

export interface ProductVO {
  productId: string
  productName: string
  productCode?: string
  mainImage?: string
  mainImageUrl?: string
  categoryId?: string
  categoryName?: string
  categoryPath?: string
  productType?: string
  unit?: string
  standardPrice?: number | string
  costPrice?: number | string
  ownerId?: string
  ownerName?: string
  status: ProductStatus
  description?: string
  customFields?: Record<string, unknown>
  createUserId?: string
  createUserName?: string
  updateUserId?: string
  createTime?: string
  updateTime?: string
}

export interface ProductQueryBO {
  keyword?: string
  categoryId?: string
  includeChildCategory?: boolean
  productType?: string
  status?: ProductStatus | ''
  ownerId?: string
  page?: number
  limit?: number
}

export interface ProductAddBO {
  productName: string
  productCode?: string
  mainImage?: string
  categoryId?: string
  productType?: string
  unit?: string
  standardPrice?: number | string
  costPrice?: number | string
  ownerId?: string
  description?: string
  customFields?: Record<string, unknown>
}

export interface ProductUpdateBO extends ProductAddBO {
  productId: string
}

export interface ProductStatusUpdateBO {
  productId: string
  status: ProductStatus
}

export interface ProductTransferBO {
  productIds: string[]
  ownerId: string
}

export interface ProductCategoryVO {
  categoryId: string
  parentId?: string
  categoryName: string
  categoryPath?: string
  level: number
  sortOrder?: number
  status?: number
  createTime?: string
  updateTime?: string
  children?: ProductCategoryVO[]
}

export interface ProductCategoryAddBO {
  parentId?: string
  categoryName: string
  sortOrder?: number
}

export interface ProductCategoryUpdateBO extends ProductCategoryAddBO {
  categoryId: string
}

export interface ProductCategoryMoveBO {
  categoryId: string
  parentId?: string
  sortOrder?: number
}

export interface ProductSettingsVO {
  codeRequired: boolean
}

export interface ProductSettingsUpdateBO {
  codeRequired: boolean
}

export interface ProductImportRow {
  rowNum?: number
  productName?: string
  productCode?: string
  categoryPath?: string
  categoryId?: string
  productType?: string
  unit?: string
  standardPrice?: number | string
  costPrice?: number | string
  ownerName?: string
  ownerId?: string
  status?: ProductStatus | string
  description?: string
  duplicate?: boolean
  existingProductId?: string
  handleMode?: string
  errors?: string[]
}

export interface ProductImportPreviewVO {
  totalRows: number
  validRows: number
  errorRows: number
  duplicateRows: number
  rows: ProductImportRow[]
  errors: string[]
}

export interface ProductImportResultVO {
  imported: number
  updated: number
  skipped: number
  errors: string[]
}
