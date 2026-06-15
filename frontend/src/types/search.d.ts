export type GlobalSearchType = 'all' | 'customer' | 'contact' | 'task' | 'schedule' | 'knowledge'

export interface GlobalSearchQueryBO {
  keyword?: string
  type?: GlobalSearchType
  page?: number
  limit?: number
}

export interface GlobalSearchResultVO {
  type: Exclude<GlobalSearchType, 'all'>
  recordId: string
  title: string
  subtitle?: string
  content?: string
  customerId?: string
  contactId?: string
  eventTime?: string
  createTime?: string
  updateTime?: string
}
