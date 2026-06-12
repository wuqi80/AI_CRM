const PRODUCT_TYPE_LABELS: Record<string, string> = {
  goods: '商品',
  service: '服务',
  subscription: '订阅',
  other: '其他'
}

export function formatProductTypeLabel(value?: string | null): string {
  const raw = String(value ?? '').trim()
  if (!raw) return '-'
  return PRODUCT_TYPE_LABELS[raw.toLowerCase()] || raw
}

interface ProductCreatorDisplaySource {
  createUserName?: string | null
  createUserId?: string | number | null
  ownerId?: string | number | null
  ownerName?: string | null
}

export function formatProductCreatorName(product?: ProductCreatorDisplaySource | null): string {
  const createUserName = String(product?.createUserName ?? '').trim()
  if (createUserName) return createUserName

  const createUserId = product?.createUserId == null ? '' : String(product.createUserId)
  const ownerId = product?.ownerId == null ? '' : String(product.ownerId)
  const ownerName = String(product?.ownerName ?? '').trim()
  if (createUserId && ownerId && createUserId === ownerId && ownerName) {
    return ownerName
  }

  return '-'
}
