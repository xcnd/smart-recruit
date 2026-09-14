/**
 * signature_pad（szimek/signature_pad）最小类型声明。
 *
 * <p>完整类型随 npm 包提供；此处仅声明本项目使用到的 API，
 * 便于在未安装依赖时也能通过类型检查。</p>
 */
declare module 'signature_pad' {
  export interface SignaturePadOptions {
    dotSize?: number | (() => number)
    minWidth?: number
    maxWidth?: number
    minDistance?: number
    backgroundColor?: string
    penColor?: string
    throttle?: number
    velocityFilterWeight?: number
    onBegin?: (event: MouseEvent | TouchEvent) => void
    onEnd?: (event: MouseEvent | TouchEvent) => void
  }

  export default class SignaturePad {
    constructor(canvas: HTMLCanvasElement, options?: SignaturePadOptions)
    isEmpty(): boolean
    clear(): void
    on(): void
    off(): void
    toDataURL(type?: string, encoderOptions?: number): string
    fromDataURL(dataUrl: string, options?: {
      ratio?: number
      width?: number
      height?: number
      callback?: () => void
    }): void
    fromData(data: unknown): void
  }
}
