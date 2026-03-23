<template>
  <div v-if="fields.length > 0" class="dynamic-field-form">
    <div
      v-for="field in fields"
      :key="field.fieldId"
      :class="field.fieldType === 'textarea' ? 'md:col-span-2' : ''"
      class="space-y-1.5"
    >
      <label class="text-xs font-bold text-slate-500 uppercase ml-1">
        {{ field.fieldLabel }}
        <span v-if="field.isRequired" class="text-red-400">*</span>
      </label>

      <el-input
        v-if="field.fieldType === 'text'"
        v-model="localValues[field.fieldName]"
        :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
        size="large"
        class="w-full wk-crm-el-field-input"
        @input="emitChange"
      />
      <el-input
        v-else-if="field.fieldType === 'textarea'"
        v-model="localValues[field.fieldName]"
        type="textarea"
        :rows="3"
        resize="none"
        :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
        class="w-full wk-crm-el-field-input"
        @input="emitChange"
      />
      <el-input
        v-else-if="field.fieldType === 'number'"
        v-model="localValues[field.fieldName]"
        type="number"
        :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
        size="large"
        class="w-full wk-crm-el-field-input"
        @input="emitChange"
      />
      <el-date-picker
        v-else-if="field.fieldType === 'date'"
        v-model="localValues[field.fieldName]"
        type="date"
        value-format="YYYY-MM-DD"
        :placeholder="field.placeholder || '选择日期'"
        size="large"
        class="w-full wk-crm-el-field-date"
        @change="emitChange"
      />
      <el-date-picker
        v-else-if="field.fieldType === 'datetime'"
        v-model="localValues[field.fieldName]"
        type="datetime"
        value-format="YYYY-MM-DD HH:mm:ss"
        :placeholder="field.placeholder || '选择日期时间'"
        size="large"
        class="w-full wk-crm-el-field-date"
        @change="emitChange"
      />
      <el-select
        v-else-if="field.fieldType === 'select'"
        v-model="localValues[field.fieldName]"
        :placeholder="field.placeholder || '请选择'"
        size="large"
        class="w-full wk-crm-el-field-select"
        clearable
        @change="emitChange"
      >
        <el-option
          v-for="opt in field.options"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-select
        v-else-if="field.fieldType === 'multiselect'"
        v-model="localValues[field.fieldName]"
        :placeholder="field.placeholder || '请选择'"
        size="large"
        class="w-full wk-crm-el-field-select"
        multiple
        clearable
        @change="emitChange"
      >
        <el-option
          v-for="opt in field.options"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <div v-else-if="field.fieldType === 'checkbox'" class="pt-1">
        <el-switch v-model="localValues[field.fieldName]" @change="emitChange" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import type { CustomField, EntityType } from '@/types/customField'
import { getEnabledFieldsByEntity } from '@/api/customField'

// 与新建任务一致：父级需带 wk-crm-el-field-scope 才应用 wk-crm-el-field-skin.css

const props = withDefaults(defineProps<{
  entityType: EntityType
  modelValue?: Record<string, any>
}>(), {
  modelValue: () => ({})
})

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, any>]
  'fieldsLoaded': [fields: CustomField[]]
}>()

const fields = ref<CustomField[]>([])
const localValues = ref<Record<string, any>>({})

// Parse and validate default value based on field type
function parseDefaultValue(field: CustomField): any {
  const val: unknown = (field as any).defaultValue
  if (val === undefined || val === null || val === '') return null

  switch (field.fieldType) {
    case 'number': {
      const num = Number(val)
      return isNaN(num) ? null : num
    }
    case 'date':
      return /^\d{4}-\d{2}-\d{2}$/.test(String(val)) ? val : null
    case 'datetime':
      return /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}(:\d{2})?$/.test(String(val)) ? val : null
    case 'checkbox':
      return val === true || val === 1 || String(val) === 'true' || String(val) === '1'
    case 'select':
      if (field.options?.some(opt => opt.value === val)) return val
      return null
    default:
      return val
  }
}

// Load custom fields
async function loadFields() {
  try {
    fields.value = await getEnabledFieldsByEntity(props.entityType)
    // Initialize default values
    fields.value.forEach(field => {
      if (localValues.value[field.fieldName] === undefined) {
        if (field.fieldType === 'multiselect') {
          localValues.value[field.fieldName] = []
        } else if (field.fieldType === 'checkbox') {
          const hasDefault = (field as any).defaultValue !== undefined && (field as any).defaultValue !== null && (field as any).defaultValue !== ''
          localValues.value[field.fieldName] = hasDefault ? parseDefaultValue(field) : false
        } else if ((field as any).defaultValue !== undefined && (field as any).defaultValue !== null && (field as any).defaultValue !== '') {
          localValues.value[field.fieldName] = parseDefaultValue(field)
        } else {
          localValues.value[field.fieldName] = null
        }
      }
    })
    emit('fieldsLoaded', fields.value)
  } catch {
    // Error handled by interceptor
  }
}

function emitChange() {
  emit('update:modelValue', { ...localValues.value })
}

// Watch for external value changes
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    Object.assign(localValues.value, newVal)
  }
}, { deep: true, immediate: true })

onMounted(() => {
  loadFields()
})

// Expose validation method
function validate(): boolean {
  for (const field of fields.value) {
    if (field.isRequired) {
      const value = localValues.value[field.fieldName]
      if (value === null || value === undefined || value === '' ||
          (Array.isArray(value) && value.length === 0)) {
        return false
      }
    }
  }
  return true
}

function getRequiredFieldLabels(): string[] {
  const missing: string[] = []
  for (const field of fields.value) {
    if (field.isRequired) {
      const value = localValues.value[field.fieldName]
      if (value === null || value === undefined || value === '' ||
          (Array.isArray(value) && value.length === 0)) {
        missing.push(field.fieldLabel)
      }
    }
  }
  return missing
}

defineExpose({ validate, getRequiredFieldLabels, fields, localValues })
</script>
