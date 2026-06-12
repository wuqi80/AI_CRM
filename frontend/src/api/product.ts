import { get, post, upload } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  ProductAddBO,
  ProductCategoryAddBO,
  ProductCategoryMoveBO,
  ProductCategoryUpdateBO,
  ProductCategoryVO,
  ProductImportPreviewVO,
  ProductImportResultVO,
  ProductImportRow,
  ProductQueryBO,
  ProductSettingsUpdateBO,
  ProductSettingsVO,
  ProductStatusUpdateBO,
  ProductTransferBO,
  ProductUpdateBO,
  ProductVO
} from '@/types/product'

export function addProduct(data: ProductAddBO): Promise<string> {
  return post('/product/add', data)
}

export function updateProduct(data: ProductUpdateBO): Promise<void> {
  return post('/product/update', data)
}

export function deleteProduct(productId: string): Promise<void> {
  return post(`/product/delete/${productId}`)
}

export function updateProductStatus(data: ProductStatusUpdateBO): Promise<void> {
  return post('/product/status', data)
}

export function transferProducts(data: ProductTransferBO): Promise<void> {
  return post('/product/transfer', data)
}

export function queryProductList(query: ProductQueryBO): Promise<PageResult<ProductVO>> {
  return post('/product/queryPageList', query)
}

export function getProductDetail(productId: string): Promise<ProductVO> {
  return get(`/product/${productId}`)
}

export function getProductSettings(): Promise<ProductSettingsVO> {
  return get('/product/settings')
}

export function updateProductSettings(data: ProductSettingsUpdateBO): Promise<void> {
  return post('/product/settings', data)
}

export function exportProducts(query: ProductQueryBO): Promise<Blob> {
  return post('/product/export', query, { responseType: 'blob' })
}

export function downloadProductImportTemplate(): Promise<Blob> {
  return get('/product/import/template', { responseType: 'blob' })
}

export function importProductPreview(file: File): Promise<ProductImportPreviewVO> {
  const formData = new FormData()
  formData.append('file', file)
  return upload('/product/import/preview', formData)
}

export function confirmProductImport(rows: ProductImportRow[]): Promise<ProductImportResultVO> {
  return post('/product/import/confirm', rows)
}

export function getProductCategoryTree(): Promise<ProductCategoryVO[]> {
  return get('/product/category/tree')
}

export function addProductCategory(data: ProductCategoryAddBO): Promise<string> {
  return post('/product/category/add', data)
}

export function updateProductCategory(data: ProductCategoryUpdateBO): Promise<void> {
  return post('/product/category/update', data)
}

export function moveProductCategory(data: ProductCategoryMoveBO): Promise<void> {
  return post('/product/category/move', data)
}

export function deleteProductCategory(categoryId: string): Promise<void> {
  return post(`/product/category/delete/${categoryId}`)
}
