import { reactive } from 'vue'

export type ToastType = 'success' | 'error' | 'info'

type ToastItem = {
  id: number
  message: string
  type: ToastType
}

const state = reactive({
  items: [] as ToastItem[],
})

let toastId = 0

const show = (message: string, type: ToastType = 'info', durationMs = 2800) => {
  const id = ++toastId
  state.items.push({ id, message, type })

  window.setTimeout(() => {
    remove(id)
  }, durationMs)
}

const remove = (id: number) => {
  const index = state.items.findIndex((item) => item.id === id)
  if (index >= 0) {
    state.items.splice(index, 1)
  }
}

export const useToast = () => ({
  toasts: state.items,
  success: (message: string, durationMs?: number) => show(message, 'success', durationMs),
  error: (message: string, durationMs?: number) => show(message, 'error', durationMs),
  info: (message: string, durationMs?: number) => show(message, 'info', durationMs),
  remove,
})
