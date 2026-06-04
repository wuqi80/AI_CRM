<template>
  <button
    class="wk-floating-new-chat-button"
    :class="`wk-floating-new-chat-button--${props.placement}`"
    :style="buttonStyle"
    type="button"
    aria-label="New chat"
    title="AI Assistant"
    @click="handleClick"
    @pointerdown="handlePointerDown"
  >
    <WkIcon name="new-chat" :size="22" class="wk-floating-new-chat-button__icon" />
    <span class="wk-floating-new-chat-button__label">Chat</span>
  </button>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import WkIcon from '@/components/common/WkIcon.vue'

type Point = {
  x: number
  y: number
}

const props = withDefaults(
  defineProps<{
    placement?: 'viewport' | 'menu'
  }>(),
  {
    placement: 'viewport',
  }
)

const emit = defineEmits<{
  (e: 'new-chat'): void
}>()

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()

const STORAGE_KEY = 'wk_ai_crm:floating_ai_button_pos:v1'
const BUTTON_SIZE = 56
const EDGE_PADDING = 20

const isDragging = ref(false)
const draggedDuringPointer = ref(false)
const dragStartPointer = ref<Point | null>(null)
const dragStartPos = ref<Point | null>(null)
const position = ref<Point | null>(null)

function clamp(n: number, min: number, max: number) {
  return Math.min(max, Math.max(min, n))
}

function defaultPosition(placement: 'viewport' | 'menu') {
  const rightOffset = 24
  const bottomOffset = placement === 'menu' ? 64 : 24
  const x = window.innerWidth - BUTTON_SIZE - rightOffset
  const y = window.innerHeight - BUTTON_SIZE - bottomOffset

  const maxX = Math.max(EDGE_PADDING, window.innerWidth - BUTTON_SIZE - EDGE_PADDING)
  const maxY = Math.max(EDGE_PADDING, window.innerHeight - BUTTON_SIZE - EDGE_PADDING)

  return {
    x: clamp(x, EDGE_PADDING, maxX),
    y: clamp(y, EDGE_PADDING, maxY),
  }
}

function loadPosition(placement: 'viewport' | 'menu') {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return defaultPosition(placement)
    const parsed = JSON.parse(raw) as Partial<Point>
    if (typeof parsed?.x !== 'number' || typeof parsed?.y !== 'number') return defaultPosition(placement)

    const maxX = Math.max(EDGE_PADDING, window.innerWidth - BUTTON_SIZE - EDGE_PADDING)
    const maxY = Math.max(EDGE_PADDING, window.innerHeight - BUTTON_SIZE - EDGE_PADDING)
    return {
      x: clamp(parsed.x, EDGE_PADDING, maxX),
      y: clamp(parsed.y, EDGE_PADDING, maxY),
    }
  } catch {
    return defaultPosition(placement)
  }
}

function savePosition(pos: Point) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(pos))
  } catch {
    // ignore
  }
}

const buttonStyle = computed(() => {
  if (!position.value) return {}
  return {
    left: `${position.value.x}px`,
    top: `${position.value.y}px`,
  }
})

async function handleClick() {
  if (draggedDuringPointer.value) return

  emit('new-chat')

  const detailProjectId = resolveProjectDetailId()
  if (detailProjectId) {
    await router.replace({
      name: 'ProjectDetail',
      params: { id: detailProjectId },
      query: { ...route.query, taskId: undefined, view: 'ai' },
    })
    return
  }

  const detailCustomerId = resolveCustomerDetailId()
  if (detailCustomerId) {
    await router.push({ path: '/chat', query: { customerId: detailCustomerId } })
    chatStore.requestComposerFocus()
    return
  }

  chatStore.beginNewSessionDraft('New chat', undefined, undefined, 'crm')
  await router.push({ path: '/chat' })
  chatStore.requestComposerFocus()
}

function resolveProjectDetailId(): string {
  if (route.name !== 'ProjectDetail') return ''
  const raw = route.params.id
  return String(Array.isArray(raw) ? raw[0] || '' : raw || '')
}

function resolveCustomerDetailId(): string {
  if (route.name !== 'CustomerDetail') return ''
  const raw = route.params.id
  return String(Array.isArray(raw) ? raw[0] || '' : raw || '')
}

function handlePointerDown(e: PointerEvent) {
  if (e.pointerType === 'mouse' && e.button !== 0) return

  ;(e.currentTarget as HTMLElement | null)?.setPointerCapture?.(e.pointerId)

  isDragging.value = true
  draggedDuringPointer.value = false
  dragStartPointer.value = { x: e.clientX, y: e.clientY }
  dragStartPos.value = position.value ?? defaultPosition(props.placement)

  window.addEventListener('pointermove', handlePointerMove, { passive: false })
  window.addEventListener('pointerup', handlePointerUp, { passive: true })
  window.addEventListener('pointercancel', handlePointerUp, { passive: true })
}

function handlePointerMove(e: PointerEvent) {
  if (!isDragging.value || !dragStartPointer.value || !dragStartPos.value) return
  e.preventDefault()

  const dx = e.clientX - dragStartPointer.value.x
  const dy = e.clientY - dragStartPointer.value.y

  if (!draggedDuringPointer.value && (Math.abs(dx) > 3 || Math.abs(dy) > 3)) {
    draggedDuringPointer.value = true
  }

  const maxX = Math.max(EDGE_PADDING, window.innerWidth - BUTTON_SIZE - EDGE_PADDING)
  const maxY = Math.max(EDGE_PADDING, window.innerHeight - BUTTON_SIZE - EDGE_PADDING)
  position.value = {
    x: clamp(dragStartPos.value.x + dx, EDGE_PADDING, maxX),
    y: clamp(dragStartPos.value.y + dy, EDGE_PADDING, maxY),
  }
}

function handlePointerUp() {
  if (!isDragging.value) return
  isDragging.value = false

  if (position.value) savePosition(position.value)

  if (draggedDuringPointer.value) {
    window.setTimeout(() => {
      draggedDuringPointer.value = false
    }, 100)
  }

  cleanupPointerListeners()
}

function cleanupPointerListeners() {
  window.removeEventListener('pointermove', handlePointerMove)
  window.removeEventListener('pointerup', handlePointerUp)
  window.removeEventListener('pointercancel', handlePointerUp)
}

function handleResize() {
  if (!position.value) return
  const maxX = Math.max(EDGE_PADDING, window.innerWidth - BUTTON_SIZE - EDGE_PADDING)
  const maxY = Math.max(EDGE_PADDING, window.innerHeight - BUTTON_SIZE - EDGE_PADDING)
  position.value = {
    x: clamp(position.value.x, EDGE_PADDING, maxX),
    y: clamp(position.value.y, EDGE_PADDING, maxY),
  }
  savePosition(position.value)
}

onMounted(() => {
  position.value = loadPosition(props.placement)
  window.addEventListener('resize', handleResize, { passive: true })
})

onBeforeUnmount(() => {
  cleanupPointerListeners()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.wk-floating-new-chat-button {
  display: inline-flex;
  height: 54px;
  min-width: 124px;
  align-items: center;
  justify-content: center;
  gap: 12px;
  border: 1px solid rgb(255 255 255 / 0.12);
  border-radius: 9999px;
  background: #252525;
  color: #fff;
  box-shadow:
    0 18px 42px rgb(15 23 42 / 0.24),
    0 2px 8px rgb(15 23 42 / 0.18);
  cursor: pointer;
  padding: 0 22px;
  transition:
    background-color 160ms ease,
    box-shadow 160ms ease,
    transform 160ms ease;
}

.wk-floating-new-chat-button--viewport {
  position: fixed;
  right: max(24px, calc(18px + env(safe-area-inset-right)));
  bottom: max(24px, calc(18px + env(safe-area-inset-bottom)));
  z-index: 120;
}

.wk-floating-new-chat-button--menu {
  position: absolute;
  right: max(24px, calc(18px + env(safe-area-inset-right)));
  bottom: max(64px, calc(58px + env(safe-area-inset-bottom)));
  z-index: 20;
  box-shadow:
    0 10px 24px rgb(15 23 42 / 0.18),
    0 0 10px 2px rgb(15 23 42 / 0.18);
}

.wk-floating-new-chat-button:hover {
  background: #1f1f1f;
  box-shadow:
    0 20px 48px rgb(15 23 42 / 0.28),
    0 2px 10px rgb(15 23 42 / 0.2);
  transform: translateY(-1px);
}

.wk-floating-new-chat-button--menu:hover {
  box-shadow:
    0 12px 28px rgb(15 23 42 / 0.22),
    0 0 12px 3px rgb(15 23 42 / 0.2);
}

.wk-floating-new-chat-button:active {
  transform: translateY(1px) scale(0.98);
}

.wk-floating-new-chat-button__icon {
  flex-shrink: 0;
}

.wk-floating-new-chat-button__label {
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .wk-floating-new-chat-button--viewport,
  .wk-floating-new-chat-button--menu {
    right: max(18px, calc(14px + env(safe-area-inset-right)));
  }

  .wk-floating-new-chat-button--viewport {
    bottom: max(20px, calc(16px + env(safe-area-inset-bottom)));
  }

  .wk-floating-new-chat-button--menu {
    bottom: max(60px, calc(56px + env(safe-area-inset-bottom)));
  }

  .wk-floating-new-chat-button {
    height: 52px;
    min-width: 116px;
    gap: 10px;
    padding: 0 20px;
  }

  .wk-floating-new-chat-button__label {
    font-size: 17px;
  }
}
</style>
