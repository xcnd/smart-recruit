import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

export const vPermission: Directive<HTMLElement, string> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string>) {
    const userStore = useUserStore()
    const { value } = binding

    if (value && !userStore.hasPermission(value)) {
      el.style.display = 'none'
      el.remove()
    }
  },
}

export default vPermission
