import { ref, reactive } from 'vue'

export function usePagination(defaultSize = 10) {
  const page = ref(1)
  const size = ref(defaultSize)
  const total = ref(0)

  function handlePageChange(p: number) {
    page.value = p
  }

  function handleSizeChange(s: number) {
    size.value = s
    page.value = 1
  }

  function resetPage() {
    page.value = 1
    size.value = defaultSize
    total.value = 0
  }

  function setTotal(t: number) {
    total.value = t
  }

  const queryParams = reactive({
    get page() { return page.value },
    get size() { return size.value },
  })

  return {
    page,
    size,
    total,
    queryParams,
    handlePageChange,
    handleSizeChange,
    resetPage,
    setTotal,
  }
}
