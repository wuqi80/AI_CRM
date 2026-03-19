<template>
  <div class="min-h-screen flex items-center justify-center bg-background-light">
    <div class="w-full max-w-md px-4">
      <!-- Logo and Title -->
      <div class="text-center mb-10">
        <img src="/logo.png" alt="悟空AI CRM" class="size-16 rounded-2xl shadow-xl shadow-primary/20 mb-4 inline-block" />
        <h1 class="text-2xl font-bold text-slate-900">悟空AI CRM</h1>
        <p class="mt-1 text-sm text-slate-500">创建您的企业账号</p>
      </div>

      <!-- Register Card -->
      <div class="bg-white rounded-2xl border border-slate-200 shadow-xl p-8">
        <h2 class="text-lg font-bold text-slate-900 mb-6">注册</h2>

        <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-position="top"
          @submit.prevent="handleRegister"
          hide-required-asterisk
        >
          <el-form-item label="公司名称" prop="companyName">
            <el-input
              v-model="formData.companyName"
              placeholder="请输入公司名称"
              size="large"
            />
          </el-form-item>

          <el-form-item label="联系人姓名" prop="realname">
            <el-input
              v-model="formData.realname"
              placeholder="请输入联系人姓名（选填）"
              size="large"
            />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input
              v-model="formData.email"
              placeholder="请输入邮箱（用作登录账号）"
              size="large"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="formData.password"
              type="password"
              placeholder="请输入密码（6-20位）"
              size="large"
              show-password
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="formData.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              size="large"
              show-password
            />
          </el-form-item>

          <el-form-item label="验证码" prop="verificationCode">
            <div class="flex w-full gap-3">
              <el-input
                v-model="formData.verificationCode"
                placeholder="请输入验证码"
                size="large"
                @keyup.enter="handleRegister"
              />
              <button
                type="button"
                class="shrink-0 rounded-lg border border-slate-200 px-4 text-sm font-medium text-slate-700 transition-colors hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-400"
                :disabled="sendingCode || countdown > 0"
                @click="handleSendCode"
              >
                {{ sendCodeText }}
              </button>
            </div>
          </el-form-item>

          <el-form-item class="mt-2">
            <button
              type="button"
              class="w-full bg-primary text-white py-2.5 rounded-lg text-sm font-medium hover:bg-primary/90 transition-colors shadow-lg shadow-primary/20 flex items-center justify-center gap-2 disabled:opacity-50"
              :disabled="loading"
              @click="handleRegister"
            >
              <span v-if="loading" class="size-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
              {{ loading ? '注册中...' : '注册' }}
            </button>
          </el-form-item>
        </el-form>

        <div class="text-center text-sm text-slate-500 mt-6">
          <p>已有账号？<router-link to="/login" class="text-primary font-medium hover:underline">立即登录</router-link></p>
        </div>
      </div>
    </div>
  </div>

  <SliderCaptchaDialog
    v-model="showCaptchaDialog"
    @verified="handleCaptchaVerified"
  />
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { register, sendEmailCode } from '@/api/auth'
import SliderCaptchaDialog from '@/components/auth/SliderCaptchaDialog.vue'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const sendingCode = ref(false)
const showCaptchaDialog = ref(false)
const countdown = ref(0)
let countdownTimer: number | undefined

const formData = reactive({
  companyName: '',
  realname: '',
  email: '',
  password: '',
  confirmPassword: '',
  verificationCode: ''
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== formData.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  companyName: [
    { required: true, message: '请输入公司名称', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

const sendCodeText = computed(() => {
  if (sendingCode.value) return '发送中...'
  if (countdown.value > 0) return `${countdown.value}s 后重试`
  return '发送验证码'
})

async function handleRegister() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true

    await register({
      companyName: formData.companyName.trim(),
      realname: formData.realname.trim() || undefined,
      email: formData.email.trim(),
      password: formData.password,
      verificationCode: formData.verificationCode.trim()
    })

    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    console.error('Register error:', error)
  } finally {
    loading.value = false
  }
}

async function handleSendCode() {
  if (!formRef.value || sendingCode.value || countdown.value > 0) return

  try {
    await formRef.value.validateField('email')
    showCaptchaDialog.value = true
  } catch (_error) {
    return
  }
}

async function handleCaptchaVerified(captchaVerification: string) {
  sendingCode.value = true
  try {
    await sendEmailCode({
      email: formData.email.trim(),
      type: 1,
      captchaVerification
    })
    ElMessage.success('验证码已发送，请查收邮箱')
    startCountdown()
  } finally {
    sendingCode.value = false
  }
}

function startCountdown() {
  countdown.value = 60
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
  }
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0 && countdownTimer) {
      window.clearInterval(countdownTimer)
      countdownTimer = undefined
    }
  }, 1000)
}

onBeforeUnmount(() => {
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
  }
})
</script>
