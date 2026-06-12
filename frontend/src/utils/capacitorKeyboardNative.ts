import type { PluginListenerHandle } from '@capacitor/core'
import { Keyboard, KeyboardResize, type KeyboardInfo } from '@capacitor/keyboard'
import type { NativeKeyboardInsetCallbacks } from '@/utils/capacitorKeyboard'

export async function configureNativeKeyboardPlugin(): Promise<void> {
  await Keyboard.setResizeMode({ mode: KeyboardResize.None })
  await Keyboard.setAccessoryBarVisible({ isVisible: false })
}

export async function hideNativeKeyboardPlugin(): Promise<void> {
  await Keyboard.hide()
}

export function registerNativeKeyboardInsetListenerPlugins(
  callbacks: NativeKeyboardInsetCallbacks
): () => void {
  let disposed = false
  const handles: PluginListenerHandle[] = []

  const addHandle = (listenerPromise: Promise<PluginListenerHandle>) => {
    void listenerPromise
      .then((handle) => {
        if (disposed) {
          void handle.remove()
          return
        }
        handles.push(handle)
      })
      .catch((error) => {
        console.warn('Failed to register native keyboard listener:', error)
      })
  }

  const handleShow = (info: KeyboardInfo) => {
    callbacks.onShow(Math.max(0, Math.round(info.keyboardHeight || 0)))
  }
  const handleHide = () => {
    callbacks.onHide()
  }

  addHandle(Keyboard.addListener('keyboardWillShow', handleShow))
  addHandle(Keyboard.addListener('keyboardDidShow', handleShow))
  addHandle(Keyboard.addListener('keyboardWillHide', handleHide))
  addHandle(Keyboard.addListener('keyboardDidHide', handleHide))

  return () => {
    disposed = true
    handles.splice(0).forEach((handle) => {
      void handle.remove()
    })
  }
}
