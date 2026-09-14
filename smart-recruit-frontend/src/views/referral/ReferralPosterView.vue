<template>
  <div class="referral-poster-page">
    <div class="sr-page-header">
      <h1>生成内推海报</h1>
      <p>选择内推职位，生成专属分享海报</p>
    </div>

    <!-- Loading State -->
    <template v-if="loading">
      <div class="sr-section">
        <div class="poster-layout">
          <div class="poster-preview-col">
            <el-skeleton style="width: 320px; height: 480px;" animated>
              <template #template>
                <div style="border-radius: var(--c-radius-lg); overflow: hidden; height: 100%; background: var(--c-bg-secondary);" />
              </template>
            </el-skeleton>
          </div>
          <div class="poster-controls-col">
            <el-skeleton :rows="4" animated />
          </div>
        </div>
      </div>
    </template>

    <!-- Error State -->
    <template v-else-if="error">
      <div class="sr-section">
        <div class="error-state">
          <div class="error-icon-wrapper">
            <el-icon :size="48" color="var(--c-danger)">
              <WarningFilled />
            </el-icon>
          </div>
          <h3>加载内推计划失败</h3>
          <p>请检查网络后重试</p>
          <el-button type="primary" @click="fetchPrograms" :loading="loading">
            重新加载
          </el-button>
        </div>
      </div>
    </template>

    <!-- Empty State -->
    <template v-else-if="programs.length === 0">
      <div class="sr-section" style="text-align: center; padding: 80px 24px;">
        <el-empty description="请先创建内推计划">
          <template #default>
            <p class="empty-hint">当前暂无启用的内推计划，请前往创建后再生成海报</p>
            <el-button type="primary" style="margin-top: 16px;" @click="$router.push('/referral')">
              前往内推管理
            </el-button>
          </template>
        </el-empty>
      </div>
    </template>

    <!-- Data State -->
    <template v-else>
      <div class="sr-section">
        <div class="poster-layout">
          <!-- Left: CSS-rendered Poster Preview -->
          <div class="poster-preview-col">
            <div class="poster">
              <div class="poster-inner">
                <div class="poster-header">
                  <div class="poster-logo">
                    <svg width="24" height="24" viewBox="0 0 28 28" fill="none">
                      <rect width="28" height="28" rx="6" fill="#1677ff"/>
                      <path d="M8 20V8l6 8.5L20 8v12" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
                    </svg>
                    <span>SmartRecruit</span>
                  </div>
                  <div class="poster-tag">内推</div>
                </div>
                <div class="poster-body">
                  <div class="poster-job-title">
                    {{ selectedProgram?.title || programs[0]?.title || '--' }}
                  </div>
                  <div
                    class="poster-job-desc"
                    v-if="selectedProgram?.description || programs[0]?.description"
                  >
                    {{ selectedProgram?.description || programs[0]?.description }}
                  </div>
                  <div class="poster-bonus">
                    <span class="poster-bonus-label">内推奖金</span>
                    <span class="poster-bonus-value">
                      {{
                        selectedProgram?.bonusAmount != null
                          ? '¥' + formatBonus(selectedProgram.bonusAmount)
                          : programs[0]?.bonusAmount != null
                            ? '¥' + formatBonus(programs[0].bonusAmount)
                            : '--'
                      }}
                    </span>
                  </div>
                </div>
                <div class="poster-footer">
                  <div class="poster-qr-section">
                    <div class="poster-qr-placeholder">
                      <el-icon :size="36"><Picture /></el-icon>
                      <span>扫码查看</span>
                    </div>
                    <span class="poster-qr-label">扫码查看职位详情</span>
                  </div>
                  <div class="poster-footer-text">
                    推荐有奖 - 职等你来
                  </div>
                </div>
              </div>
            </div>

            <!-- Action buttons after generation -->
            <div class="poster-actions" v-if="generatedResult">
              <el-button type="primary" :icon="Download" style="flex: 1;" @click="handleDownload">
                下载海报
              </el-button>
              <el-button :icon="Link" style="flex: 1;" @click="handleCopyLink">
                复制链接
              </el-button>
            </div>
          </div>

          <!-- Right: Controls -->
          <div class="poster-controls-col">
            <div class="sr-section-title">海报设置</div>
            <el-form label-width="80px" @submit.prevent>
              <el-form-item label="选择计划">
                <el-select
                  v-model="selectedProgramId"
                  placeholder="请选择内推计划"
                  style="width: 100%;"
                  filterable
                >
                  <el-option
                    v-for="program in programs"
                    :key="program.id"
                    :label="program.title"
                    :value="program.id"
                  >
                    <span>{{ program.title }}</span>
                    <span style="float: right; font-size: 12px; color: var(--c-primary);">
                      {{ program.bonusAmount != null ? '¥' + formatBonus(program.bonusAmount) : '' }}
                    </span>
                  </el-option>
                </el-select>
              </el-form-item>

              <el-form-item>
                <el-button
                  type="primary"
                  :loading="generating"
                  :disabled="!selectedProgramId"
                  @click="handleGenerate"
                  style="width: 100%;"
                >
                  {{ generating ? '生成中...' : '生成海报' }}
                </el-button>
              </el-form-item>
            </el-form>

            <!-- Generation result meta -->
            <div class="generated-info" v-if="generatedResult">
              <el-divider />
              <div class="generated-meta">
                <div class="generated-meta-item">
                  <span class="meta-label">生成时间</span>
                  <span class="meta-value">{{ generatedResult.generatedAt }}</span>
                </div>
                <div class="generated-meta-item">
                  <span class="meta-label">职位名称</span>
                  <span class="meta-value">{{ generatedResult.jobTitle }}</span>
                </div>
                <div class="generated-meta-item">
                  <span class="meta-label">海报链接</span>
                  <a
                    :href="generatedResult.posterUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="poster-link"
                  >
                    在新标签页中打开
                    <el-icon style="vertical-align: middle; margin-left: 4px;"><Link /></el-icon>
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Download, Picture, Link, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getPrograms, generatePoster } from '@/api/referral'
import { formatMoney } from '@/utils/format'
import type { ReferralProgramVO } from '@/types/models'

// ---- Types ----

interface PosterResult {
  programId: string
  jobTitle: string
  posterUrl: string
  shareLink: string
  generatedAt: string
}

// ---- State ----

const loading = ref(true)
const error = ref(false)
const generating = ref(false)

const programs = ref<ReferralProgramVO[]>([])
const selectedProgramId = ref<string | null>(null)
const generatedResult = ref<PosterResult | null>(null)

// ---- Computed ----

const selectedProgram = computed(() =>
  programs.value.find((p) => p.id === selectedProgramId.value) ?? null
)

// ---- Helpers ----

function formatBonus(val: number | string | null | undefined): string {
  if (val == null) return '--'
  const num = typeof val === 'string' ? parseFloat(val) : val
  if (isNaN(num)) return '--'
  return formatMoney(num)
}

function getNowString(): string {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const h = String(now.getHours()).padStart(2, '0')
  const min = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}:${s}`
}

// ---- API Actions ----

async function fetchPrograms() {
  loading.value = true
  error.value = false
  try {
    // getPrograms() returns ReferralProgramVO[] directly (NOT PageResult)
    // The request interceptor unwraps ApiResponse → returns the array directly
    const res = await getPrograms()
    programs.value = Array.isArray(res) ? res : []
    if (programs.value.length > 0) {
      selectedProgramId.value = programs.value[0].id
    }
  } catch {
    error.value = true
    programs.value = []
  } finally {
    loading.value = false
  }
}

async function handleGenerate() {
  if (!selectedProgramId.value) {
    ElMessage.warning('请先选择内推计划')
    return
  }
  generating.value = true
  try {
    // generatePoster expects a number; ReferralProgramVO.id is string, so convert
    const res = await generatePoster(Number(selectedProgramId.value))
    // Cast the Record<string, string> response to our local format
    const raw = res as unknown as Record<string, string>
    generatedResult.value = {
      programId: selectedProgramId.value,
      jobTitle: selectedProgram.value?.title || raw.jobTitle || '',
      posterUrl: raw.posterUrl || '',
      shareLink: raw.shareLink || raw.url || '',
      generatedAt: getNowString(),
    }
    ElMessage.success('海报已生成')
  } catch {
    // Error already shown by interceptor
  } finally {
    generating.value = false
  }
}

function handleDownload() {
  if (!generatedResult.value?.posterUrl) {
    ElMessage.warning('请先生成海报')
    return
  }
  window.open(generatedResult.value.posterUrl, '_blank')
}

async function handleCopyLink() {
  if (!generatedResult.value?.shareLink) {
    ElMessage.warning('暂无分享链接，请先生成海报')
    return
  }
  try {
    await navigator.clipboard.writeText(generatedResult.value.shareLink)
    ElMessage.success('链接已复制到剪贴板')
  } catch {
    // Fallback for older browsers
    const textarea = document.createElement('textarea')
    textarea.value = generatedResult.value.shareLink
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    try {
      document.execCommand('copy')
      ElMessage.success('链接已复制到剪贴板')
    } catch {
      ElMessage.warning('复制失败，请手动复制分享链接')
    }
    document.body.removeChild(textarea)
  }
}

// ---- Lifecycle ----

onMounted(() => {
  fetchPrograms()
})
</script>

<style scoped>
/* Layout */
.poster-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.poster-preview-col {
  width: 320px;
  flex-shrink: 0;
}

.poster-controls-col {
  flex: 1;
  min-width: 0;
}

/* Poster CSS rendering */
.poster {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: var(--c-radius-lg);
  overflow: hidden;
  aspect-ratio: 2 / 3;
  box-shadow: var(--c-shadow-md);
}

.poster-inner {
  padding: 28px 24px;
  color: #fff;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.poster-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.poster-logo {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.poster-tag {
  background: rgba(255, 255, 255, 0.2);
  padding: 3px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  backdrop-filter: blur(4px);
}

.poster-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.poster-job-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 6px;
  line-height: 1.3;
}

.poster-job-desc {
  font-size: 13px;
  opacity: 0.75;
  margin-bottom: 20px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.poster-bonus {
  background: rgba(255, 255, 255, 0.15);
  border-radius: var(--c-radius-md);
  padding: 12px 16px;
  backdrop-filter: blur(4px);
}

.poster-bonus-label {
  display: block;
  font-size: 12px;
  opacity: 0.7;
  margin-bottom: 4px;
}

.poster-bonus-value {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 1px;
}

.poster-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.poster-qr-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.poster-qr-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--c-radius-sm);
  font-size: 9px;
  backdrop-filter: blur(2px);
}

.poster-qr-label {
  font-size: 10px;
  opacity: 0.6;
  white-space: nowrap;
}

.poster-footer-text {
  font-size: 14px;
  font-weight: 500;
  opacity: 0.85;
}

/* Poster action buttons */
.poster-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

/* Generation result info */
.generated-info {
  margin-top: 4px;
}

.generated-meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.generated-meta-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.meta-label {
  color: var(--c-text-secondary);
  width: 64px;
  flex-shrink: 0;
}

.meta-value {
  color: var(--c-text);
  font-weight: 500;
}

.poster-link {
  color: var(--c-primary);
  text-decoration: none;
  font-weight: 500;
}

.poster-link:hover {
  text-decoration: underline;
}

/* Error state */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
  text-align: center;
}

.error-icon-wrapper {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--c-bg-secondary, #f8fafc);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.error-state h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text);
  margin-bottom: 8px;
}

.error-state p {
  font-size: 13px;
  color: var(--c-text-secondary);
  margin-bottom: 20px;
}

/* Empty state */
.empty-hint {
  color: var(--c-text-secondary);
  font-size: 13px;
  margin-top: 8px;
}

/* Responsive */
@media (max-width: 768px) {
  .poster-layout {
    flex-direction: column;
    align-items: center;
  }

  .poster-preview-col {
    width: 100%;
    max-width: 320px;
  }

  .poster-controls-col {
    width: 100%;
  }
}
</style>
