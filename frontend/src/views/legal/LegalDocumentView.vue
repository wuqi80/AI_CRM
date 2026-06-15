<template>
  <main class="legal-page">
    <section class="legal-shell">
      <div class="legal-brand">
        <img :src="logoImg" alt="AI CRM" class="legal-brand__logo" />
        <span class="legal-brand__name">AI CRM</span>
      </div>

      <article class="legal-document">
        <p class="legal-document__eyebrow">Legal</p>
        <h1>{{ documentTitle }}</h1>
        <p class="legal-document__intro">{{ documentIntro }}</p>
        <div class="legal-document__content" aria-live="polite">
          <p v-if="isLoading" class="legal-document__state">正在加载{{ documentTitle }}...</p>
          <pre v-else-if="documentContent" class="legal-document__text">{{ documentContent }}</pre>
          <div v-else class="legal-document__state legal-document__state--error">
            <p>{{ loadError || '文档加载失败' }}</p>
            <button type="button" @click="loadDocument">重新加载</button>
          </div>
        </div>
      </article>

      <RouterLink class="legal-page__back" :to="backRoute">返回登录</RouterLink>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import logoImg from '@/assets/images/logo.png'
import { getApiBaseUrl } from '@/utils/request'

type LegalDocumentType = 'agreement' | 'privacy'

const props = defineProps<{
  documentType: LegalDocumentType
}>()

const route = useRoute()

const documentTitle = computed(() => (props.documentType === 'privacy' ? '隐私声明' : '用户协议'))
const documentIntro = computed(() => `请仔细阅读以下${documentTitle.value}内容。`)
const documentApiUrl = computed(() => `${getApiBaseUrl()}/legal-document/${props.documentType}`)
const backRoute = computed(() => {
  const query: Record<string, string> = {}
  if (route.query.agreementDialog === '1') {
    query.agreementDialog = '1'
  }
  if (typeof route.query.redirect === 'string' && route.query.redirect) {
    query.redirect = route.query.redirect
  }
  return { name: 'Login', query }
})

const documentContent = ref('')
const isLoading = ref(false)
const loadError = ref('')

async function loadDocument(): Promise<void> {
  isLoading.value = true
  loadError.value = ''
  try {
    const response = await fetch(documentApiUrl.value, { cache: 'no-store' })
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    documentContent.value = await response.text()
  } catch (error) {
    documentContent.value = ''
    loadError.value = error instanceof Error ? `文档加载失败：${error.message}` : '文档加载失败'
  } finally {
    isLoading.value = false
  }
}

watch(
  () => props.documentType,
  () => {
    void loadDocument()
  },
  { immediate: true }
)
</script>

<style scoped>
.legal-page {
  min-height: 100vh;
  min-height: 100dvh;
  overflow-y: auto;
  background: #f8fafc;
  color: #0f172a;
}

.legal-shell {
  width: min(100%, 760px);
  min-height: 100%;
  box-sizing: border-box;
  margin: 0 auto;
  padding: 2rem 1.25rem 3rem;
}

.legal-brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 2rem;
}

.legal-brand__logo {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 0.75rem;
  background: #fff;
  object-fit: contain;
  padding: 0.3rem;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.legal-brand__name {
  font-size: 1.1rem;
  font-weight: 800;
}

.legal-document {
  background: #fff;
  padding: clamp(1.5rem, 5vw, 2.5rem);
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.06);
}

.legal-document__eyebrow {
  margin: 0 0 0.6rem;
  color: #137fec;
  font-size: 0.78rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.legal-document h1 {
  margin: 0;
  font-size: clamp(1.8rem, 8vw, 2.5rem);
  line-height: 1.2;
}

.legal-document__intro {
  margin: 1rem 0 0;
  color: #475569;
  font-size: 1rem;
  line-height: 1.8;
}

.legal-document__content {
  max-height: min(52vh, 34rem);
  margin-top: 1.75rem;
  overflow-y: auto;
  background: #fff;
  overscroll-behavior: contain;
  scrollbar-color: #cbd5e1 #fff;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
}

.legal-document__text {
  margin: 0;
  color: #0f172a;
  font: inherit;
  font-size: 0.96rem;
  line-height: 1.9;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  word-break: break-word;
}

.legal-document__state {
  display: flex;
  min-height: 12rem;
  align-items: center;
  justify-content: center;
  margin: 0;
  color: #64748b;
  font-size: 0.95rem;
  line-height: 1.7;
  text-align: center;
}

.legal-document__state--error {
  flex-direction: column;
  gap: 0.9rem;
}

.legal-document__state--error button {
  border: 0;
  border-radius: 0.5rem;
  background: #137fec;
  color: #fff;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 700;
  padding: 0.5rem 1rem;
}

.legal-page__back {
  display: inline-flex;
  margin-top: 1.5rem;
  color: #137fec;
  font-size: 0.95rem;
  font-weight: 700;
  text-decoration: none;
}
</style>
