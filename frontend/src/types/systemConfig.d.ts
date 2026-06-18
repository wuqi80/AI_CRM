export type AiProvider =
  | 'openai'
  | 'dashscope'
  | 'moonshot'
  | 'deepseek'
  | 'ark'
  | 'hunyuan'
  | 'minimax'
  | 'zhipu'
  | 'wukong_external'
  | 'custom'

export type AiMode = 'custom'

export interface AiModelCapabilities {
  supportsStream: boolean
  supportsToolCall: boolean
  supportsVision: boolean
  supportsAudioTranscription: boolean
}

export interface AiProviderPreset {
  label: string
  value: AiProvider
  description?: string
  baseUrl: string
  models: string[]
  modelHint?: string
  extraHeadersHint?: string
  supportsStream: boolean
  supportsToolCall: boolean
  supportsVision: boolean
  supportsAudioTranscription: boolean
  configured?: boolean
  active?: boolean
  apiKeyConfigured?: boolean
  mobileCompleted?: boolean
  savedApiUrl?: string | null
  savedModel?: string | null
  savedTemperature?: number | null
  savedMaxTokens?: number | null
  savedExtraHeadersConfigured?: boolean
  savedExtraHeadersJson?: string | null
}

export interface AiConfig {
  provider: AiProvider
  providerLabel?: string
  apiUrl: string
  apiKey: string
  model: string
  temperature: number
  maxTokens: number
  extraHeadersConfigured?: boolean
  extraHeadersJson?: string | null
  capabilities?: AiModelCapabilities
  modelHint?: string
  extraHeadersHint?: string
  availableProviders?: AiProviderPreset[]
  mode?: AiMode
  customConfigSaved?: boolean
  ready?: boolean
  wukongExternalMobileCompleted?: boolean
  updateTime?: string
}

export interface AiConfigUpdateBO {
  provider?: AiProvider
  apiUrl: string
  apiKey: string
  model: string
  temperature?: number
  maxTokens?: number
  extraHeadersJson?: string
}

export interface AiProviderActivateBO {
  provider: AiProvider
}

export interface AiConnectionTestResult {
  success: boolean
  responseTime: number
  message: string
  model?: string
  provider?: AiProvider
}

export interface ExternalAiCaptchaProxyParams {
  apiUrl: string
  payload?: Record<string, unknown>
}

export interface ExternalAiSmsCodeParams {
  apiUrl: string
  mobile: string
  captchaVerification: string
}

export interface ExternalAiRegisterAndSaveParams {
  apiUrl: string
  mobile?: string
  verificationCode?: string
  accountName?: string
  model?: string
  temperature?: number
  maxTokens?: number
  extraHeadersJson?: string
}

export interface ExternalAiCompleteMobileParams {
  apiUrl?: string
  mobile: string
  verificationCode: string
}

export interface ExternalAiRegisterAndSaveResult {
  provider: AiProvider
  apiUrl: string
  model: string
  keyPrefix?: string
  apiKeyConfigured: boolean
}

export interface MinioConsoleConfig {
  enabled: boolean
  consoleUrl: string
}

export interface EnterpriseConfig {
  name: string | null
  logo: string | null
  logoUrl: string | null
  description: string | null
  updateTime?: string
}

export interface EnterpriseConfigUpdateBO {
  name?: string
  logo?: string
  description?: string
}
