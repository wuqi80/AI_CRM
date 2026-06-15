import { get } from '@/utils/request'

export interface EnumOption {
  value: string
  label: string
  description?: string | null
  color?: string
}

export const getCustomerStageEnum = () => get<EnumOption[]>('/enum/customerStage')

export const getCustomerLevelEnum = () => get<EnumOption[]>('/enum/customerLevel')

export const getRelationTypeEnum = () => get<EnumOption[]>('/enum/relationType')

export const getRelationSourceEnum = () => get<EnumOption[]>('/enum/relationSource')
