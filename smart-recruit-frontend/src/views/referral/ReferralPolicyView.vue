<template>
  <div class="referral-policy-page">
    <div class="sr-page-header">
      <h1>内推政策与奖励</h1>
      <p>了解公司内推奖金标准、流程及常见问题</p>
    </div>

    <!-- Loading State: 3 skeleton sections -->
    <template v-if="loading">
      <div class="sr-section">
        <el-skeleton animated :rows="4" />
      </div>
      <div class="sr-section">
        <el-skeleton animated :rows="3" />
      </div>
      <div class="sr-section">
        <el-skeleton animated :rows="5" />
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
          <h3>暂时无法加载政策数据</h3>
          <p>请稍后重试，或联系 HR 部门获取最新内推政策</p>
          <el-button type="primary" @click="fetchPolicy" :loading="loading">
            重新加载
          </el-button>
        </div>
      </div>

      <!-- Fallback rules when API fails -->
      <div class="sr-section">
        <div class="sr-section-title">注意事项</div>
        <div class="fallback-rules">
          <div class="fallback-rule-item" v-for="(rule, i) in fallbackRules" :key="i">
            <span class="fallback-rule-dot"></span>
            {{ rule }}
          </div>
        </div>
      </div>
    </template>

    <!-- Empty State -->
    <template v-else-if="!policyData">
      <div class="sr-section" style="text-align: center; padding: 80px 24px;">
        <el-empty description="暂无政策数据" />
      </div>
    </template>

    <!-- Data State -->
    <template v-else>
      <!-- Section 1: 奖金标准 -->
      <div class="sr-section" v-if="policyData.bonusTiers?.length">
        <div class="sr-section-title">
          <el-icon :size="20" style="margin-right: 8px;"><Money /></el-icon>
          <span>奖金标准</span>
        </div>
        <el-table :data="policyData.bonusTiers" stripe style="width: 100%;">
          <el-table-column prop="tier" label="阶段" min-width="140" />
          <el-table-column prop="description" label="说明" min-width="200" />
          <el-table-column label="发放比例" width="120" align="center">
            <template #default="{ row }">
              <span class="bonus-ratio">{{ row.payoutRatio }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="trigger" label="触发条件" min-width="180" />
        </el-table>
      </div>

      <!-- Section 2: 注意事项 -->
      <div class="sr-section" v-if="policyData.rules?.length">
        <div class="sr-section-title">
          <el-icon :size="20" style="margin-right: 8px;"><List /></el-icon>
          <span>注意事项</span>
        </div>
        <el-timeline>
          <el-timeline-item
            v-for="(rule, idx) in policyData.rules"
            :key="idx"
            :timestamp="`第${idx + 1}条`"
            placement="top"
            color="#4f46e5"
          >
            {{ rule }}
          </el-timeline-item>
        </el-timeline>
      </div>

      <!-- Section 3: 内推流程 -->
      <div class="sr-section" v-if="policyData.process?.length">
        <div class="sr-section-title">
          <el-icon :size="20" style="margin-right: 8px;"><Document /></el-icon>
          <span>内推流程</span>
        </div>
        <el-steps
          :active="policyData.process.length"
          finish-status="success"
          align-center
          process-status="finish"
        >
          <el-step
            v-for="step in policyData.process"
            :key="step.step"
            :title="step.title"
            :description="step.desc"
          />
        </el-steps>
      </div>

      <!-- Section 4: 常见问题 -->
      <div class="sr-section" v-if="policyData.faqs?.length">
        <div class="sr-section-title">
          <el-icon :size="20" style="margin-right: 8px;"><QuestionFilled /></el-icon>
          <span>常见问题</span>
        </div>
        <el-collapse accordion>
          <el-collapse-item
            v-for="(faq, idx) in policyData.faqs"
            :key="idx"
            :title="faq.question"
          >
            <p class="faq-answer">{{ faq.answer }}</p>
          </el-collapse-item>
        </el-collapse>
      </div>

      <!-- Section 5: 联系方式 -->
      <div class="sr-section" v-if="policyData.contact">
        <div class="sr-section-title">
          <el-icon :size="20" style="margin-right: 8px;"><Phone /></el-icon>
          <span>联系方式</span>
        </div>
        <div class="contact-card">
          <div class="contact-item" v-if="policyData.contact.email">
            <span class="contact-label">邮箱</span>
            <a :href="`mailto:${policyData.contact.email}`" class="contact-value">
              {{ policyData.contact.email }}
            </a>
          </div>
          <div class="contact-item" v-if="policyData.contact.phone">
            <span class="contact-label">电话</span>
            <a :href="`tel:${policyData.contact.phone}`" class="contact-value">
              {{ policyData.contact.phone }}
            </a>
          </div>
          <div class="contact-item" v-if="policyData.contact.wechat">
            <span class="contact-label">微信</span>
            <span class="contact-value">{{ policyData.contact.wechat }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { WarningFilled, Money, List, Document, QuestionFilled, Phone } from '@element-plus/icons-vue'
import { getPolicy } from '@/api/referral'

// ---- Types ----

interface BonusTier {
  tier: string
  description: string
  payoutRatio: string
  trigger: string
}

interface ProcessStep {
  step: string
  title: string
  desc: string
}

interface FaqItem {
  question: string
  answer: string
}

interface ContactInfo {
  email?: string
  phone?: string
  wechat?: string
}

interface PolicyData {
  bonusTiers: BonusTier[]
  rules: string[]
  process: ProcessStep[]
  faqs: FaqItem[]
  contact: ContactInfo | null
}

// ---- State ----

const loading = ref(true)
const error = ref(false)
const policyData = ref<PolicyData | null>(null)

const fallbackRules = [
  '候选人必须通过正式面试流程并被成功录用',
  '内推奖金在候选人通过试用期后发放',
  '同一候选人被多人推荐，以最早推荐记录为准',
  'HR、招聘相关岗位人员不参与内推奖励',
  '禁止推荐举报期内或已进入面试流程的候选人',
  '内推人需确保候选人信息的真实性和准确性',
]

// ---- Methods ----

async function fetchPolicy() {
  loading.value = true
  error.value = false
  try {
    // Backend returns ApiResponse<Map<String, Object>>
    // The request interceptor unwraps ApiResponse → returns the Map directly
    // Map keys: bonusTiers (array of {tier, description, payoutRatio, trigger}),
    //   rules (string[]), process (array of {step, title, desc}),
    //   faqs (array of {question, answer}), contact ({email, phone, wechat})
    const res = await getPolicy()
    const data = res as unknown as Record<string, any>

    policyData.value = {
      bonusTiers: (Array.isArray(data.bonusTiers) ? data.bonusTiers : []) as BonusTier[],
      rules: (Array.isArray(data.rules) ? data.rules : []) as string[],
      process: (Array.isArray(data.process) ? data.process : []) as ProcessStep[],
      faqs: (Array.isArray(data.faqs) ? data.faqs : []) as FaqItem[],
      contact: data.contact ? (data.contact as ContactInfo) : null,
    }
  } catch {
    error.value = true
    policyData.value = null
  } finally {
    loading.value = false
  }
}

// ---- Lifecycle ----

onMounted(() => {
  fetchPolicy()
})
</script>

<style scoped>
.referral-policy-page {
  padding: 0;
}

/* Bonus ratio highlight */
.bonus-ratio {
  font-weight: 700;
  color: var(--c-primary);
  font-size: 15px;
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

/* Fallback rules */
.fallback-rules {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.fallback-rule-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 14px;
  color: var(--c-text);
  line-height: 1.6;
}

.fallback-rule-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--c-primary);
  margin-top: 6px;
  flex-shrink: 0;
}

/* FAQ answer */
.faq-answer {
  font-size: 14px;
  color: var(--c-text-secondary);
  line-height: 1.8;
  padding: 4px 0;
}

/* Contact */
.contact-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.contact-label {
  width: 48px;
  color: var(--c-text-secondary);
  flex-shrink: 0;
}

.contact-value {
  color: var(--c-primary);
  font-weight: 500;
  text-decoration: none;
  transition: opacity 0.2s;
}

a.contact-value:hover {
  opacity: 0.8;
}
</style>
