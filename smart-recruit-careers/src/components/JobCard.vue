<template>
  <article
    class="job-card"
    @click="$emit('click')"
  >
    <!-- Avatar -->
    <div class="job-avatar" :class="avatarClass">
      <span class="avatar-letter">{{ firstLetter }}</span>
    </div>

    <!-- Main content -->
    <div class="job-main">
      <h3 class="job-title">{{ job.title }}</h3>
      <div class="job-meta">
        <span class="meta-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="13" height="13">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
            <circle cx="12" cy="10" r="3"/>
          </svg>
          {{ job.location }}
        </span>
        <span class="meta-divider">|</span>
        <span class="meta-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="13" height="13">
            <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/>
            <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
          </svg>
          {{ job.exp }}
        </span>
        <span class="meta-divider">|</span>
        <span class="meta-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="13" height="13">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          {{ job.dept }}
        </span>
      </div>
      <div class="job-tags" v-if="job.tags && job.tags.length">
        <span
          v-for="(tag, i) in job.tags"
          :key="i"
          class="job-tag"
          :class="tag.cls"
        >{{ tag.text }}</span>
      </div>
    </div>

    <!-- Right side -->
    <div class="job-side">
      <span class="job-date">{{ job.date }}</span>
      <span class="job-salary">{{ job.salary }}</span>
      <button class="job-btn" tabindex="-1">
        查看详情
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="14" height="14">
          <line x1="5" y1="12" x2="19" y2="12"/>
          <polyline points="12 5 19 12 12 19"/>
        </svg>
      </button>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface JobTag {
  text: string
  cls: string
}

interface Job {
  id: number
  title: string
  dept: string
  location: string
  exp: string
  salary: string
  category: string
  date: string
  tags: JobTag[]
}

const props = defineProps<{
  job: Job
}>()

defineEmits<{
  click: []
}>()

const firstLetter = computed(() => {
  return props.job.title ? props.job.title.charAt(0) : 'S'
})

const avatarClass = computed(() => {
  const map: Record<string, string> = {
    tech: 'avatar-tech',
    product: 'avatar-product',
    design: 'avatar-design',
    market: 'avatar-market',
    data: 'avatar-data'
  }
  return map[props.job.category] || 'avatar-tech'
})
</script>

<style scoped>
.job-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 24px;
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: transform var(--transition-slow), box-shadow var(--transition-slow), border-color var(--transition-slow);
}

.job-card:hover {
  transform: translateX(4px);
  box-shadow: var(--shadow-lg);
  border-color: rgba(99, 102, 241, 0.2);
}

/* Avatar */
.job-avatar {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-letter {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  text-transform: uppercase;
}

.avatar-tech {
  background: linear-gradient(135deg, #1677ff, #4f46e5);
}

.avatar-product {
  background: linear-gradient(135deg, #f43f5e, #fb7185);
}

.avatar-design {
  background: linear-gradient(135deg, #06b6d4, #22d3ee);
}

.avatar-market {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
}

.avatar-data {
  background: linear-gradient(135deg, #10b981, #34d399);
}

/* Main content */
.job-main {
  flex: 1;
  min-width: 0;
}

.job-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.job-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--color-text-muted);
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  white-space: nowrap;
}

.meta-item svg {
  flex-shrink: 0;
}

.meta-divider {
  color: var(--color-border);
}

.job-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.job-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
  line-height: 1.6;
}

.job-tag.hot {
  background: rgba(244, 63, 94, 0.08);
  color: #f43f5e;
}

.job-tag.new {
  background: rgba(16, 185, 129, 0.08);
  color: #10b981;
}

.job-tag.urgent {
  background: rgba(245, 158, 11, 0.08);
  color: #f59e0b;
}

.job-tag.tech-tag {
  background: rgba(99, 102, 241, 0.08);
  color: var(--color-primary);
}

/* Right side */
.job-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
}

.job-date {
  font-size: 13px;
  color: var(--color-text-muted);
}

.job-salary {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-accent);
  white-space: nowrap;
}

.job-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  transition: background var(--transition), box-shadow var(--transition);
}

.job-btn:hover {
  background: var(--color-primary-dark);
  box-shadow: var(--shadow-md);
}

.job-btn svg {
  transition: transform var(--transition);
}

.job-card:hover .job-btn svg {
  transform: translateX(2px);
}

@media (max-width: 640px) {
  .job-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .job-side {
    flex-direction: row;
    align-items: center;
    gap: 12px;
    width: 100%;
    justify-content: flex-start;
    order: -1;
  }

  .job-btn {
    margin-left: auto;
  }
}
</style>
