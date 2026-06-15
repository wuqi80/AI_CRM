import { post } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { GlobalSearchQueryBO, GlobalSearchResultVO } from '@/types/search'

export function queryGlobalSearch(query: GlobalSearchQueryBO): Promise<PageResult<GlobalSearchResultVO>> {
  return post('/search/global', query)
}
