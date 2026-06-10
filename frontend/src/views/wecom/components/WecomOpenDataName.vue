<template>
  <span ref="rootRef" class="wecom-open-data-name">
    <ww-open-data
      v-if="shouldRenderOpenData"
      type="userName"
      :openid="userId"
      :corpid="corpId || undefined"
    >
      {{ displayFallback }}
    </ww-open-data>
    <span v-else>{{ displayFallback }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { getWecomJsSdkAgentConfig } from '@/api/wecom'

const props = withDefaults(defineProps<{
  userId?: string
  corpId?: string
  fallback?: string
}>(), {
  userId: '',
  corpId: '',
  fallback: ''
})

const rootRef = ref<HTMLElement | null>(null)
const ready = ref(false)
const displayFallback = computed(() => props.fallback || props.userId || '-')
const shouldRenderOpenData = computed(() => Boolean(props.userId && ready.value))

type WecomOpenDataWindow = typeof window & {
  wx?: {
    agentConfig?: (options: Record<string, unknown>) => void
  }
  WWOpenData?: {
    bind?: (element?: Element | null) => void
  }
  __wecomOpenDataReady?: Promise<boolean>
}

const WECHAT_JS_SDK = 'https://res.wx.qq.com/open/js/jweixin-1.2.0.js'
const WECOM_JS_SDK = 'https://open.work.weixin.qq.com/wwopen/js/jwxwork-1.0.0.js'

onMounted(async () => {
  ready.value = await ensureOpenDataReady()
  bindOpenData()
})

watch(() => [props.userId, props.corpId], () => {
  bindOpenData()
})

async function ensureOpenDataReady() {
  if (!props.userId || typeof window === 'undefined') {
    return false
  }
  const wecomWindow = window as WecomOpenDataWindow
  if (!wecomWindow.__wecomOpenDataReady) {
    wecomWindow.__wecomOpenDataReady = initializeOpenData(wecomWindow)
  }
  return wecomWindow.__wecomOpenDataReady
}

async function initializeOpenData(wecomWindow: WecomOpenDataWindow) {
  try {
    await loadScript(WECHAT_JS_SDK)
    await loadScript(WECOM_JS_SDK)
    if (!wecomWindow.wx?.agentConfig) {
      return false
    }
    const config = await getWecomJsSdkAgentConfig(currentSignUrl())
    return await new Promise<boolean>((resolve) => {
      wecomWindow.wx?.agentConfig?.({
        corpid: config.corpId,
        agentid: config.agentId,
        timestamp: config.timestamp,
        nonceStr: config.nonceStr,
        signature: config.signature,
        jsApiList: [],
        success: () => resolve(true),
        fail: () => resolve(false)
      })
    })
  } catch {
    return false
  }
}

function loadScript(src: string) {
  const existing = document.querySelector<HTMLScriptElement>(`script[src="${src}"]`)
  if (existing) {
    return Promise.resolve()
  }
  return new Promise<void>((resolve, reject) => {
    const script = document.createElement('script')
    script.src = src
    script.async = true
    script.referrerPolicy = 'origin'
    script.onload = () => resolve()
    script.onerror = () => reject(new Error(`Failed to load ${src}`))
    document.head.appendChild(script)
  })
}

async function bindOpenData() {
  if (!ready.value) {
    return
  }
  await nextTick()
  const wecomWindow = window as WecomOpenDataWindow
  const element = rootRef.value?.querySelector('ww-open-data')
  wecomWindow.WWOpenData?.bind?.(element)
}

function currentSignUrl() {
  const href = window.location.href
  const hashIndex = href.indexOf('#')
  return hashIndex >= 0 ? href.slice(0, hashIndex) : href
}
</script>

<style scoped>
.wecom-open-data-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
