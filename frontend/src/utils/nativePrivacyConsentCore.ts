export const NATIVE_PRIVACY_CONSENT_STORAGE_KEY = 'wk-ai-crm:native-privacy-consent:v1'
export const NATIVE_PRIVACY_CONSENT_ACCEPTED_VALUE = 'accepted'

export type NativePrivacyBootstrapState = 'ready' | 'needs-consent'

export type PrivacyConsentStorage = Pick<Storage, 'getItem' | 'setItem'>

export type NativePrivacyConsentInput = {
  nativeRuntime: boolean
  hasConsent: boolean
}

export function hasStoredNativePrivacyConsent(
  storage?: PrivacyConsentStorage | null
): boolean {
  if (!storage) return false

  try {
    return storage.getItem(NATIVE_PRIVACY_CONSENT_STORAGE_KEY) === NATIVE_PRIVACY_CONSENT_ACCEPTED_VALUE
  } catch {
    return false
  }
}

export function storeNativePrivacyConsent(
  storage?: PrivacyConsentStorage | null
): boolean {
  if (!storage) return false

  try {
    storage.setItem(NATIVE_PRIVACY_CONSENT_STORAGE_KEY, NATIVE_PRIVACY_CONSENT_ACCEPTED_VALUE)
    return true
  } catch {
    return false
  }
}

export function shouldRequireNativePrivacyConsent({
  nativeRuntime,
  hasConsent
}: NativePrivacyConsentInput): boolean {
  return nativeRuntime && !hasConsent
}

export function resolveNativePrivacyBootstrapState(
  input: NativePrivacyConsentInput
): NativePrivacyBootstrapState {
  return shouldRequireNativePrivacyConsent(input) ? 'needs-consent' : 'ready'
}
