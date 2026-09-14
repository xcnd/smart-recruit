<template>
  <div class="search-bar">
    <div class="search-row">
      <div class="search-input-wrapper">
        <svg
          class="search-icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          width="18"
          height="18"
        >
          <circle cx="11" cy="11" r="8"/>
          <line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input
          type="text"
          class="search-input"
          :value="keyword"
          :placeholder="placeholder"
          @input="onKeywordInput"
          @keydown.enter="onEnter"
        />
      </div>
      <button class="search-btn" @click="onSearchClick">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          width="18"
          height="18"
        >
          <circle cx="11" cy="11" r="8"/>
          <line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <span>搜索</span>
      </button>
    </div>
    <div class="filter-chips">
      <button
        v-for="cat in categories"
        :key="cat"
        class="filter-chip"
        :class="{ active: filter === cat }"
        @click="onFilterClick(cat)"
      >
        {{ cat }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  filter?: string
  keyword?: string
  categories?: string[]
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  filter: '全部职位',
  keyword: '',
  categories: () => ['全部职位', '技术', '产品 & 设计', '市场 & 销售', '数据 & AI', '运营 & 职能'],
  placeholder: '搜索职位名称、关键词...'
})

const emit = defineEmits<{
  'update:filter': [value: string]
  'update:keyword': [value: string]
  search: []
}>()

function onKeywordInput(e: Event) {
  const target = e.target as HTMLInputElement
  emit('update:keyword', target.value)
}

function onFilterClick(cat: string) {
  emit('update:filter', cat)
}

function onEnter() {
  emit('search')
}

function onSearchClick() {
  emit('search')
}
</script>

<style scoped>
.search-bar {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
}

.search-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.search-input-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  background: var(--color-bg);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  transition: border-color var(--transition), box-shadow var(--transition);
}

.search-input-wrapper:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.search-icon {
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  font-size: 15px;
  color: var(--color-text);
  background: none;
  border: none;
  outline: none;
}

.search-input::placeholder {
  color: var(--color-text-muted);
}

.search-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 12px 24px;
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  transition: background var(--transition), box-shadow var(--transition);
  flex-shrink: 0;
}

.search-btn:hover {
  background: var(--color-primary-dark);
  box-shadow: var(--shadow-md);
}

.filter-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.filter-chip {
  padding: 6px 18px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--transition);
}

.filter-chip:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.filter-chip.active {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

@media (max-width: 480px) {
  .search-row {
    flex-direction: column;
  }

  .search-btn {
    justify-content: center;
  }
}
</style>
