import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  createSession,
  getChatApplications,
  getSessionList,
  deleteSession,
  updateSessionPin,
  getMessageList,
  sendMessageStream,
  sendMessageSync
} from '@/api/chat'
import type { ChatSession, ChatAttachmentDTO, ChatAttachmentVO, ChatMessage, ChatAppOption } from '@/types/common'

interface LocalMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  isStreaming?: boolean
  attachments?: ChatAttachmentVO[]
}

interface StreamingTask {
  sessionId: string
  userMessageId: string
  assistantMessageId: string
  startedAt: number
}

export const useChatStore = defineStore('chat', () => {
  const RAG_ENABLED_STORAGE_KEY = 'wk_ai_crm:chat_rag_enabled:v1'

  // State
  const sessions = ref<ChatSession[]>([])
  const currentSessionId = ref<string | null>(null)
  const messagesBySessionId = ref<Record<string, LocalMessage[]>>({})
  const streamingTasks = ref<Record<string, StreamingTask>>({})
  const loading = ref(false)
  const sessionsLoading = ref(false)
  const ragEnabled = ref(loadRagEnabled())
  const applications = ref<ChatAppOption[]>([])
  const applicationsLoading = ref(false)
  const currentAppCode = ref('crm')
  let fetchSessionsPromise: Promise<void> | null = null

  // Getters
  const messages = computed(() => {
    if (!currentSessionId.value) return []
    return messagesBySessionId.value[currentSessionId.value] || []
  })

  const isStreaming = computed(() => Object.keys(streamingTasks.value).length > 0)
  const streamingSessionIds = computed(() => Object.keys(streamingTasks.value))
  const currentSessionIsStreaming = computed(() =>
    currentSessionId.value ? Boolean(streamingTasks.value[currentSessionId.value]) : false
  )

  const currentSession = computed(() =>
    sessions.value.find(s => s.sessionId === currentSessionId.value)
  )

  const currentApplication = computed(() =>
    applications.value.find(app => app.code === currentAppCode.value)
  )

  // Actions
  async function fetchSessions() {
    if (fetchSessionsPromise) {
      return fetchSessionsPromise
    }

    sessionsLoading.value = true
    fetchSessionsPromise = getSessionList()
      .then((nextSessions) => {
        sessions.value = nextSessions
      })
      .finally(() => {
        sessionsLoading.value = false
        fetchSessionsPromise = null
      })

    return fetchSessionsPromise
  }

  async function fetchApplications() {
    applicationsLoading.value = true
    try {
      applications.value = await getChatApplications()
      if (!applications.value.some(app => app.code === currentAppCode.value)) {
        setCurrentAppCode(applications.value[0]?.code || 'crm')
      }
    } finally {
      applicationsLoading.value = false
    }
  }

  async function startNewSession(title?: string, agentId?: string, customerId?: string, appCode?: string): Promise<string> {
    const resolvedAppCode = appCode || currentAppCode.value
    const sessionId = await createSession({ title, agentId, customerId, appCode: resolvedAppCode })
    await fetchSessions()
    currentSessionId.value = sessionId
    setCurrentAppCode(resolvedAppCode)
    setSessionMessages(sessionId, [])
    return sessionId
  }

  async function selectSession(sessionId: string) {
    currentSessionId.value = sessionId
    const session = sessions.value.find(s => s.sessionId === sessionId)
    if (session?.appCode) {
      setCurrentAppCode(session.appCode)
    }
    loading.value = true
    try {
      const dbMessages = await getMessageList(sessionId)
      const loadedMessages = dbMessages.map(toLocalMessage)
      setSessionMessages(sessionId, mergeLoadedMessagesWithStreaming(sessionId, loadedMessages))
    } finally {
      loading.value = false
    }
  }

  async function removeSession(sessionId: string) {
    await deleteSession(sessionId)
    sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
    delete messagesBySessionId.value[sessionId]
    delete streamingTasks.value[sessionId]
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
    }
  }

  async function setSessionPinned(sessionId: string, pinned: boolean) {
    await updateSessionPin(sessionId, pinned)
    await fetchSessions()
  }

  async function sendMessage(
    content: string,
    attachments?: ChatAttachmentDTO[],
    attachmentVOs?: ChatAttachmentVO[],
    useRag?: boolean,
    knowledgeIds?: string[]
  ): Promise<void> {
    if (!currentSessionId.value) {
      // Create new session if none exists
      await startNewSession('新对话')
    }

    const sessionId = currentSessionId.value!
    if (streamingTasks.value[sessionId]) return
    const userMessageId = createLocalMessageId('user')
    const assistantMessageId = createLocalMessageId('assistant')

    // Add user message
    const userMessage: LocalMessage = {
      id: userMessageId,
      role: 'user',
      content,
      timestamp: new Date(),
      attachments: attachmentVOs
    }
    appendSessionMessage(sessionId, userMessage)

    // Add assistant placeholder
    const assistantMessage: LocalMessage = {
      id: assistantMessageId,
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      isStreaming: true
    }
    appendSessionMessage(sessionId, assistantMessage)

    streamingTasks.value[sessionId] = {
      sessionId,
      userMessageId,
      assistantMessageId,
      startedAt: Date.now()
    }

    try {
      const effectiveRagEnabled = (useRag ?? ragEnabled.value) || Boolean(knowledgeIds?.length)
      await sendMessageStream(
        sessionId,
        content,
        (chunk) => {
          const assistantMessage = ensureStreamingAssistantMessage(sessionId, assistantMessageId)
          assistantMessage.content += chunk
        },
        async () => {
          markAssistantMessageDone(sessionId, assistantMessageId)
          delete streamingTasks.value[sessionId]
          // Refresh session list to get updated title
          await fetchSessions()
        },
        (error) => {
          console.error('Stream error:', error)
          const lastMessage = ensureStreamingAssistantMessage(sessionId, assistantMessageId)
          if (lastMessage && lastMessage.role === 'assistant') {
            if (!lastMessage.content) {
              lastMessage.content = '抱歉，发生错误，请重试。'
            }
            lastMessage.isStreaming = false
          }
        },
        attachments,
        effectiveRagEnabled,
        currentAppCode.value,
        knowledgeIds
      )
    } catch (error) {
      // Error already handled by onError callback, but ensure message is marked as complete
      console.error('sendMessage error:', error)
    } finally {
      delete streamingTasks.value[sessionId]
      markAssistantMessageDone(sessionId, assistantMessageId)
    }
  }

  async function sendMessageWithSync(content: string, useRag?: boolean, knowledgeIds?: string[]): Promise<string> {
    if (!currentSessionId.value) {
      await startNewSession('新对话')
    }

    const sessionId = currentSessionId.value!
    if (streamingTasks.value[sessionId]) {
      return ''
    }

    // Add user message
    const userMessage: LocalMessage = {
      id: createLocalMessageId('user'),
      role: 'user',
      content,
      timestamp: new Date()
    }
    appendSessionMessage(sessionId, userMessage)

    loading.value = true
    try {
      const response = await sendMessageSync(
        sessionId,
        content,
        undefined,
        (useRag ?? ragEnabled.value) || Boolean(knowledgeIds?.length),
        currentAppCode.value,
        knowledgeIds
      )

      const assistantMessage: LocalMessage = {
        id: createLocalMessageId('assistant'),
        role: 'assistant',
        content: response,
        timestamp: new Date()
      }
      appendSessionMessage(sessionId, assistantMessage)

      return response
    } finally {
      loading.value = false
    }
  }

  function clearMessages() {
    const sessionId = currentSessionId.value
    if (sessionId && !streamingTasks.value[sessionId]) {
      setSessionMessages(sessionId, [])
    }
    currentSessionId.value = null
  }

  function setRagEnabled(value: boolean) {
    ragEnabled.value = value
    try {
      localStorage.setItem(RAG_ENABLED_STORAGE_KEY, value ? '1' : '0')
    } catch {
      // ignore storage failures
    }
  }

  function setCurrentAppCode(appCode: string) {
    currentAppCode.value = appCode || 'crm'
    const app = applications.value.find(item => item.code === currentAppCode.value)
    if (app?.defaultRagEnabled) {
      setRagEnabled(true)
    }
  }

  function loadRagEnabled(): boolean {
    try {
      return localStorage.getItem(RAG_ENABLED_STORAGE_KEY) === '1'
    } catch {
      return false
    }
  }

  function getSessionMessages(sessionId: string): LocalMessage[] {
    if (!messagesBySessionId.value[sessionId]) {
      messagesBySessionId.value[sessionId] = []
    }
    return messagesBySessionId.value[sessionId]
  }

  function setSessionMessages(sessionId: string, nextMessages: LocalMessage[]) {
    messagesBySessionId.value[sessionId] = nextMessages
  }

  function appendSessionMessage(sessionId: string, message: LocalMessage) {
    getSessionMessages(sessionId).push(message)
  }

  function findSessionMessage(sessionId: string, messageId: string): LocalMessage | undefined {
    return getSessionMessages(sessionId).find(message => message.id === messageId)
  }

  function ensureStreamingAssistantMessage(sessionId: string, assistantMessageId: string): LocalMessage {
    const existingMessage = findSessionMessage(sessionId, assistantMessageId)
    if (existingMessage) return existingMessage

    const message: LocalMessage = {
      id: assistantMessageId,
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      isStreaming: true
    }
    appendSessionMessage(sessionId, message)
    return message
  }

  function markAssistantMessageDone(sessionId: string, assistantMessageId: string) {
    const assistantMessage = findSessionMessage(sessionId, assistantMessageId)
    if (assistantMessage) {
      assistantMessage.isStreaming = false
    }
  }

  function mergeLoadedMessagesWithStreaming(
    sessionId: string,
    loadedMessages: LocalMessage[]
  ): LocalMessage[] {
    const task = streamingTasks.value[sessionId]
    if (!task) return loadedMessages

    const mergedMessages = [...loadedMessages]
    const localMessages = messagesBySessionId.value[sessionId] || []

    for (const localMessage of localMessages) {
      if (mergedMessages.some(message => message.id === localMessage.id)) {
        continue
      }
      if (localMessage.id === task.assistantMessageId || localMessage.isStreaming) {
        mergedMessages.push(localMessage)
        continue
      }
      if (localMessage.id === task.userMessageId && !hasEquivalentLoadedMessage(mergedMessages, localMessage)) {
        mergedMessages.push(localMessage)
      }
    }

    return mergedMessages.sort((a, b) => a.timestamp.getTime() - b.timestamp.getTime())
  }

  function hasEquivalentLoadedMessage(messagesToCheck: LocalMessage[], target: LocalMessage): boolean {
    const equivalenceWindow = 5 * 60 * 1000
    return messagesToCheck.some(message =>
      message.role === target.role &&
      message.content === target.content &&
      Math.abs(message.timestamp.getTime() - target.timestamp.getTime()) <= equivalenceWindow
    )
  }

  function toLocalMessage(message: ChatMessage): LocalMessage {
    return {
      id: message.messageId,
      role: message.role as 'user' | 'assistant',
      content: message.content,
      timestamp: new Date(message.createTime),
      isStreaming: false,
      attachments: message.attachments
    }
  }

  function createLocalMessageId(prefix: 'user' | 'assistant'): string {
    return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  }

  function isSessionStreaming(sessionId: string): boolean {
    return Boolean(streamingTasks.value[sessionId])
  }

  return {
    // State
    sessions,
    currentSessionId,
    messages,
    messagesBySessionId,
    streamingTasks,
    streamingSessionIds,
    isStreaming,
    currentSessionIsStreaming,
    loading,
    sessionsLoading,
    ragEnabled,
    applications,
    applicationsLoading,
    currentAppCode,
    // Getters
    currentSession,
    currentApplication,
    // Actions
    fetchSessions,
    fetchApplications,
    startNewSession,
    selectSession,
    removeSession,
    setSessionPinned,
    sendMessage,
    sendMessageWithSync,
    clearMessages,
    setRagEnabled,
    setCurrentAppCode,
    isSessionStreaming
  }
})
