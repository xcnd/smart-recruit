<template>
  <div class="notification-page">
    <!-- Page Header -->
    <div class="sr-page-header">
      <h1>消息通知</h1>
      <p>汇聚 Offer 审批、面试安排、入职流程等业务事件的通知</p>
    </div>

    <!-- Toolbar -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-radio-group v-model="readFilter" @change="handleFilterChange">
          <el-radio-button :value="undefined">全部</el-radio-button>
          <el-radio-button :value="0">未读</el-radio-button>
          <el-radio-button :value="1">已读</el-radio-button>
        </el-radio-group>
        <el-select
          v-model="typeFilter"
          placeholder="通知类型"
          clearable
          style="width: 140px;"
          @change="handleFilterChange"
        >
          <el-option
            v-for="(label, code) in TYPE_OPTIONS"
            :key="code"
            :label="label"
            :value="Number(code)"
          />
        </el-select>
      </div>
      <el-button
        type="primary"
        plain
        :disabled="!notificationStore.unreadCount"
        @click="handleMarkAllRead"
      >
        全部已读
      </el-button>
    </div>

    <!-- Notification List -->
    <div class="notification-list" v-loading="loading">
      <el-table
        :data="notifications"
        stripe
        row-key="id"
        @row-click="handleRowClick"
      >
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="notificationTypeTag(row.type)" size="small" effect="plain">
              {{ notificationTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="220">
          <template #default="{ row }">
            <div class="notif-title" :class="{ unread: row.read === 0 }">
              <span v-if="row.read === 0" class="notif-dot"></span>
              {{ row.title }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="320" show-overflow-tooltip />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">
            <span class="notif-time">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.read === 0 ? 'danger' : 'info'" size="small">
              {{ row.read === 0 ? '未读' : '已读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.read === 0"
              size="small"
              text
              type="primary"
              @click.stop="handleMarkRead(row)"
            >
              标记已读
            </el-button>
            <el-button
              v-if="row.actionUrl"
              size="small"
              text
              @click.stop="handleJump(row)"
            >
              查看
            </el-button>
            <span v-if="row.read === 1 && !row.actionUrl" style="color: var(--c-text-muted); font-size: 12px;">—</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无通知" :image-size="80" />
        </template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 消息详情 Dialog -->
    <el-dialog v-model="detailVisible" title="消息详情" width="520px" :close-on-click-modal="false">
      <div v-if="currentNotif" class="nv-detail">
        <div class="nv-detail-title">{{ currentNotif.title }}</div>
        <div class="nv-detail-meta">
          <el-tag :type="notificationTypeTag(currentNotif.type)" size="small" effect="plain">
            {{ notificationTypeLabel(currentNotif.type) }}
          </el-tag>
          <span>{{ formatDateTime(currentNotif.createdAt) }}</span>
        </div>
        <div class="nv-detail-content">{{ currentNotif.content || '暂无内容' }}</div>
        <div v-if="currentNotif.actionUrl" class="nv-detail-url">
          关联页面：{{ currentNotif.actionUrl }}
        </div>
      </div>
      <template #footer>
        <el-button v-if="currentNotif?.actionUrl" type="primary" @click="jumpDetail">前往处理</el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getNotifications } from '@/api/notification'
import { useNotificationStore } from '@/stores/notification'
import { notificationTypeLabel, notificationTypeTag } from '@/utils/notification'
import { formatDateTime } from '@/utils/format'
import type { NotificationVO } from '@/types/models'
import { navigateToActionUrl } from '@/utils/notification'

const TYPE_OPTIONS: Record<number, string> = {
  0: '系统',
  1: '面试',
  2: 'Offer',
  3: '入职',
  4: '内推',
  5: '人才',
  6: '招聘',
}

const router = useRouter()
const notificationStore = useNotificationStore()

const notifications = ref<NotificationVO[]>([])
const loading = ref(false)
const detailVisible = ref(false)
const currentNotif = ref<NotificationVO | null>(null)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const readFilter = ref<number | undefined>(undefined)
const typeFilter = ref<number | undefined>(undefined)

async function loadData() {
  loading.value = true
  try {
    const result = await getNotifications({
      page: page.value,
      size: size.value,
      read: readFilter.value,
      type: typeFilter.value,
    })
    notifications.value = result.records
    total.value = result.total
  } catch {
    // HTTP 拦截器统一提示
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  page.value = 1
  loadData()
}

function handleSizeChange() {
  page.value = 1
  loadData()
}

async function handleMarkRead(row: NotificationVO) {
  await notificationStore.markRead(row)
  await loadData()
}

async function handleMarkAllRead() {
  await notificationStore.markAllRead()
  ElMessage.success('已全部标记为已读')
  await loadData()
}

async function handleRowClick(row: NotificationVO) {
  if (row.read === 0) {
    await notificationStore.markRead(row)
    row.read = 1
  }
  currentNotif.value = row
  detailVisible.value = true
}

function handleJump(row: NotificationVO) {
  currentNotif.value = row
  detailVisible.value = true
}

/** 从消息详情跳转到关联业务页面。 */
function jumpDetail() {
  if (!currentNotif.value?.actionUrl) return
  detailVisible.value = false
  const result = navigateToActionUrl(router, currentNotif.value.actionUrl)
  if (result === 'none') {
    ElMessage.warning('该消息暂无可跳转的页面')
  } else if (result === 'same') {
    ElMessage.info('已在该页面')
  }
}

onMounted(() => {
  loadData()
  notificationStore.loadUnread()
})
</script>

<style scoped>
.notification-page {
  max-width: 1200px;
}

.sr-page-header {
  margin-bottom: 20px;
}

.sr-page-header h1 {
  font-size: 20px;
  font-weight: 700;
  color: var(--c-text);
  margin-bottom: 4px;
}

.sr-page-header p {
  font-size: 13px;
  color: var(--c-text-secondary);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 14px 16px;
  background: var(--el-bg-color);
  border: 1px solid var(--c-border-light);
  border-radius: var(--c-radius-lg);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notification-list {
  padding: 16px;
  background: var(--el-bg-color);
  border: 1px solid var(--c-border-light);
  border-radius: var(--c-radius-lg);
}

.notif-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--c-text-secondary);
}

.notif-title.unread {
  color: var(--c-text);
  font-weight: 600;
}

.notif-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--c-danger);
  flex-shrink: 0;
}

.notif-time {
  font-size: 12px;
  color: var(--c-text-muted);
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* -- 消息详情 -- */
.nv-detail-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}
.nv-detail-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 14px;
}
.nv-detail-content {
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
  white-space: pre-wrap;
  word-break: break-word;
}
.nv-detail-url {
  margin-top: 14px;
  font-size: 12px;
  color: #94a3b8;
  word-break: break-all;
}
</style>
