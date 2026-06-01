<template>
  <div class="flex h-full flex-col gap-5 px-4 py-6 md:px-6">
    <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
      <div class="min-w-0">
        <h2 class="text-[22px] font-bold text-slate-900">通讯录</h2>
        <p class="mt-1 text-[13px] text-slate-500">企业内部员工与员工对象工作记录</p>
      </div>

      <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div class="relative">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[18px] text-slate-400">search</span>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索姓名、部门、职位、手机或邮箱"
            class="h-10 w-full rounded-xl border border-slate-200 bg-white pl-10 pr-4 text-sm outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/20 sm:w-80"
            @input="debouncedLoadEmployees"
            @keydown.enter="loadEmployees"
          />
        </div>
        <select
          v-model="employeeStatus"
          class="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-600 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
          @change="loadEmployees"
        >
          <option value="">全部状态</option>
          <option value="active">在职</option>
          <option value="resigned">离职</option>
          <option value="disabled">停用</option>
        </select>
      </div>
    </div>

    <div class="min-h-0 flex-1 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div v-if="loading" class="flex h-full items-center justify-center py-20">
        <span class="material-symbols-outlined animate-spin text-3xl text-slate-300">progress_activity</span>
      </div>

      <template v-else-if="employees.length === 0">
        <div class="flex h-full min-h-[360px] flex-col items-center justify-center text-center text-slate-400">
          <span class="material-symbols-outlined mb-2 text-4xl opacity-50">contacts</span>
          <p class="text-sm">未找到员工</p>
        </div>
      </template>

      <template v-else>
        <el-table
          v-if="!isMobile"
          :data="employees"
          height="100%"
          row-key="userId"
          table-layout="fixed"
          class="wk-address-book-table"
          @row-click="openEmployeeChat"
        >
          <el-table-column label="员工" min-width="240">
            <template #default="{ row }">
              <div class="flex min-w-0 items-center gap-3">
                <img v-if="row.imgUrl" :src="row.imgUrl" class="size-10 shrink-0 rounded-full object-cover" alt="avatar" />
                <div v-else class="flex size-10 shrink-0 items-center justify-center rounded-full bg-slate-900 text-sm font-bold text-white">
                  {{ employeeInitial(row) }}
                </div>
                <div class="min-w-0">
                  <p class="truncate text-sm font-bold text-slate-900">{{ employeeName(row) }}</p>
                  <p class="truncate text-xs text-slate-400">{{ row.email || '-' }}</p>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="部门" min-width="150">
            <template #default="{ row }">
              <span class="block truncate text-sm text-slate-700">{{ row.deptName || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="职位" min-width="150">
            <template #default="{ row }">
              <span class="block truncate text-sm text-slate-700">{{ row.post || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="手机号" min-width="150">
            <template #default="{ row }">
              <span class="block truncate text-sm text-slate-700">{{ row.mobile || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <span class="rounded-full px-2 py-1 text-xs font-bold" :class="employeeStatusClass(row)">
                {{ employeeStatusLabel(row) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="最近相关任务" min-width="160">
            <template #default="{ row }">
              <span class="text-sm text-slate-500">{{ formatDateTime(row.recentTaskTime) }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div v-else class="h-full overflow-y-auto p-3">
          <button
            v-for="employee in employees"
            :key="employee.userId"
            type="button"
            class="mb-3 flex w-full items-start gap-3 rounded-2xl border border-slate-200 bg-white p-4 text-left shadow-sm transition-colors active:bg-slate-50"
            @click="openEmployeeChat(employee)"
          >
            <img v-if="employee.imgUrl" :src="employee.imgUrl" class="size-11 shrink-0 rounded-full object-cover" alt="avatar" />
            <div v-else class="flex size-11 shrink-0 items-center justify-center rounded-full bg-slate-900 text-sm font-bold text-white">
              {{ employeeInitial(employee) }}
            </div>
            <div class="min-w-0 flex-1">
              <div class="flex items-start justify-between gap-2">
                <div class="min-w-0">
                  <p class="truncate text-sm font-bold text-slate-900">{{ employeeName(employee) }}</p>
                  <p class="truncate text-xs text-slate-400">{{ employee.post || '员工' }} · {{ employee.deptName || '-' }}</p>
                </div>
                <span class="shrink-0 rounded-full px-2 py-1 text-[11px] font-bold" :class="employeeStatusClass(employee)">
                  {{ employeeStatusLabel(employee) }}
                </span>
              </div>
              <div class="mt-3 grid grid-cols-1 gap-1 text-sm text-slate-600">
                <p class="truncate">{{ employee.mobile || '-' }}</p>
                <p class="truncate">{{ employee.email || '-' }}</p>
                <p class="truncate text-xs text-slate-400">最近相关任务：{{ formatDateTime(employee.recentTaskTime) }}</p>
              </div>
            </div>
          </button>
        </div>
      </template>
    </div>

    <div v-if="total > 0" class="flex items-center justify-between text-sm text-slate-500">
      <span>共 {{ total }} 位员工</span>
      <el-pagination
        v-if="!isMobile"
        v-model:current-page="page"
        :page-size="limit"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="loadEmployees"
      />
      <div v-else class="flex items-center gap-2">
        <button class="size-8 rounded border border-slate-200 bg-white disabled:opacity-50" :disabled="page <= 1" @click="changePage(page - 1)">
          <span class="material-symbols-outlined text-[18px] leading-none">chevron_left</span>
        </button>
        <span>{{ page }} / {{ totalPages }}</span>
        <button class="size-8 rounded border border-slate-200 bg-white disabled:opacity-50" :disabled="page >= totalPages" @click="changePage(page + 1)">
          <span class="material-symbols-outlined text-[18px] leading-none">chevron_right</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { queryAddressBook } from '@/api/addressBook'
import { useResponsive } from '@/composables/useResponsive'
import { isRequestErrorHandled } from '@/utils/requestError'
import type { AddressBookEmployee } from '@/types/addressBook'

const router = useRouter()
const { isMobile } = useResponsive()

const employees = ref<AddressBookEmployee[]>([])
const loading = ref(false)
const keyword = ref('')
const employeeStatus = ref('')
const page = ref(1)
const limit = ref(20)
const total = ref(0)
let searchTimer: number | null = null

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / limit.value)))

onMounted(() => {
  void loadEmployees()
})

function debouncedLoadEmployees() {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    page.value = 1
    void loadEmployees()
  }, 260)
}

async function loadEmployees() {
  loading.value = true
  try {
    const result = await queryAddressBook({
      keyword: keyword.value.trim() || undefined,
      employeeStatus: employeeStatus.value || undefined,
      page: page.value,
      limit: limit.value
    })
    employees.value = result.list || []
    total.value = Number(result.totalRow || 0)
  } catch (error) {
    if (!isRequestErrorHandled(error)) {
      ElMessage.warning('通讯录加载失败')
    }
    employees.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function changePage(nextPage: number) {
  if (nextPage < 1 || nextPage > totalPages.value || nextPage === page.value) return
  page.value = nextPage
  void loadEmployees()
}

function employeeName(employee: AddressBookEmployee) {
  return employee.realname || '未命名员工'
}

function employeeInitial(employee: AddressBookEmployee) {
  return employeeName(employee).charAt(0) || '?'
}

function normalizeStatus(status?: string) {
  return status === 'resigned' || status === 'disabled' ? status : 'active'
}

function employeeStatusLabel(employee: AddressBookEmployee) {
  if (employee.employeeStatusName) return employee.employeeStatusName
  const status = normalizeStatus(employee.employeeStatus)
  if (status === 'resigned') return '离职'
  if (status === 'disabled') return '停用'
  return '在职'
}

function employeeStatusClass(employee: AddressBookEmployee) {
  const status = normalizeStatus(employee.employeeStatus)
  if (status === 'resigned') return 'bg-slate-100 text-slate-500'
  if (status === 'disabled') return 'bg-amber-50 text-amber-600'
  return 'bg-emerald-50 text-emerald-600'
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function openEmployeeChat(employee: AddressBookEmployee) {
  router.push({ path: '/chat', query: { employeeId: employee.userId } })
}
</script>

<style scoped>
:deep(.wk-address-book-table .el-table__row) {
  cursor: pointer;
}

:deep(.wk-address-book-table .el-table__body tr:hover > td.el-table__cell) {
  background: #f8fafc;
}
</style>
