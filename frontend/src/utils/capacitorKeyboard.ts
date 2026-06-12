import { Capacitor } from '@capacitor/core'
import { Keyboard, KeyboardResize } from '@capacitor/keyboard'

export function shouldUseCapacitorKeyboardHide(
  isNativePlatform: boolean,
  isKeyboardPluginAvailable: boolean
): boolean {
  return isNativePlatform && isKeyboardPluginAvailable
}

export function shouldConfigureNativeKeyboard(
  isNativePlatform: boolean,
  isKeyboardPluginAvailable: boolean,
  platform: string
): boolean {
  return isNativePlatform && isKeyboardPluginAvailable && platform === 'ios'
}

export function configureNativeKeyboard(): void {
  if (!shouldConfigureNativeKeyboard(
    Capacitor.isNativePlatform(),
    Capacitor.isPluginAvailable('Keyboard'),
    Capacitor.getPlatform()
  )) {
    return
  }

  void Keyboard.setResizeMode({ mode: KeyboardResize.None }).catch((error) => {
    console.warn('设置原生键盘 resize 模式失败:', error)
  })
  void Keyboard.setAccessoryBarVisible({ isVisible: false }).catch((error) => {
    console.warn('设置原生键盘 accessory bar 失败:', error)
  })
}

export function hideCapacitorKeyboard(): void {
  if (!shouldUseCapacitorKeyboardHide(
    Capacitor.isNativePlatform(),
    Capacitor.isPluginAvailable('Keyboard')
  )) {
    return
  }

  void Keyboard.hide().catch((error) => {
    console.warn('关闭原生键盘失败:', error)
  })
}
