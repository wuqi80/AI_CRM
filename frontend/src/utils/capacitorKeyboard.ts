import {
  getNativePlatform,
  isNativePlatform,
  isNativePluginAvailable
} from '@/utils/nativeMobileRuntime'

export type NativeKeyboardInsetCallbacks = {
  onShow: (keyboardHeight: number) => void
  onHide: () => void
}

export function shouldUseCapacitorKeyboardHide(
  isNativePlatformValue: boolean,
  isKeyboardPluginAvailable: boolean
): boolean {
  return isNativePlatformValue && isKeyboardPluginAvailable
}

export function shouldConfigureNativeKeyboard(
  isNativePlatformValue: boolean,
  isKeyboardPluginAvailable: boolean,
  platform: string
): boolean {
  return isNativePlatformValue && isKeyboardPluginAvailable && platform === 'ios'
}

export function shouldUseNativeKeyboardEvents(
  isNativePlatformValue: boolean,
  isKeyboardPluginAvailable: boolean
): boolean {
  return isNativePlatformValue && isKeyboardPluginAvailable
}

export function configureNativeKeyboard(): void {
  if (!shouldConfigureNativeKeyboard(
    isNativePlatform(),
    isNativePluginAvailable('Keyboard'),
    getNativePlatform()
  )) {
    return
  }

  void import('@/utils/capacitorKeyboardNative')
    .then(({ configureNativeKeyboardPlugin }) => configureNativeKeyboardPlugin())
    .catch((error) => {
      console.warn('Failed to configure native keyboard:', error)
    })
}

export function hideCapacitorKeyboard(): void {
  if (!shouldUseCapacitorKeyboardHide(
    isNativePlatform(),
    isNativePluginAvailable('Keyboard')
  )) {
    return
  }

  void import('@/utils/capacitorKeyboardNative')
    .then(({ hideNativeKeyboardPlugin }) => hideNativeKeyboardPlugin())
    .catch((error) => {
      console.warn('Failed to hide native keyboard:', error)
    })
}

export function registerNativeKeyboardInsetListeners(
  callbacks: NativeKeyboardInsetCallbacks
): () => void {
  if (!shouldUseNativeKeyboardEvents(
    isNativePlatform(),
    isNativePluginAvailable('Keyboard')
  )) {
    return () => undefined
  }

  let disposed = false
  let cleanup: (() => void) | null = null

  void import('@/utils/capacitorKeyboardNative')
    .then(({ registerNativeKeyboardInsetListenerPlugins }) => {
      const removeListeners = registerNativeKeyboardInsetListenerPlugins(callbacks)
      if (disposed) {
        removeListeners()
        return
      }
      cleanup = removeListeners
    })
    .catch((error) => {
      console.warn('Failed to register native keyboard listeners:', error)
    })

  return () => {
    disposed = true
    cleanup?.()
    cleanup = null
  }
}
