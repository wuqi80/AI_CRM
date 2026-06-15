import { defineStore } from 'pinia'
import { ref, type Ref } from 'vue'
import {
  getCustomerLevelEnum,
  getCustomerStageEnum,
  getRelationSourceEnum,
  getRelationTypeEnum,
  type EnumOption
} from '@/api/enum'

export const useEnumStore = defineStore('enums', () => {
  const customerStage = ref<EnumOption[]>([])
  const customerLevel = ref<EnumOption[]>([])
  const relationType = ref<EnumOption[]>([])
  const relationSource = ref<EnumOption[]>([])

  const loaded: Record<string, boolean> = {}
  const inflight: Record<string, Promise<void> | null> = {}

  async function ensure(
    key: string,
    target: Ref<EnumOption[]>,
    fetcher: () => Promise<EnumOption[]>,
    force = false
  ): Promise<void> {
    if (loaded[key] && !force) return
    if (inflight[key]) return inflight[key] as Promise<void>
    const task = (async () => {
      try {
        const data = await fetcher()
        target.value = Array.isArray(data) ? data : []
        loaded[key] = true
      } catch {
        target.value = target.value || []
      } finally {
        inflight[key] = null
      }
    })()
    inflight[key] = task
    return task
  }

  const ensureCustomerStage = (force = false) =>
    ensure('customerStage', customerStage, getCustomerStageEnum, force)
  const ensureCustomerLevel = (force = false) =>
    ensure('customerLevel', customerLevel, getCustomerLevelEnum, force)
  const ensureRelationType = (force = false) =>
    ensure('relationType', relationType, getRelationTypeEnum, force)
  const ensureRelationSource = (force = false) =>
    ensure('relationSource', relationSource, getRelationSourceEnum, force)

  function labelOf(options: EnumOption[], value?: string | null): string {
    if (!value) return ''
    return options.find(option => option.value === value)?.label || value
  }

  async function refreshAll(): Promise<void> {
    await Promise.all([
      ensureCustomerStage(true),
      ensureCustomerLevel(true),
      ensureRelationType(true),
      ensureRelationSource(true)
    ])
  }

  return {
    customerStage,
    customerLevel,
    relationType,
    relationSource,
    ensureCustomerStage,
    ensureCustomerLevel,
    ensureRelationType,
    ensureRelationSource,
    stageLabel: (value?: string | null) => labelOf(customerStage.value, value),
    levelLabel: (value?: string | null) => labelOf(customerLevel.value, value),
    relationTypeLabel: (value?: string | null) => labelOf(relationType.value, value),
    sourceLabel: (value?: string | null) => labelOf(relationSource.value, value),
    refreshAll
  }
})
