<template>
  <div class="referral-leaderboard-page">
    <!-- Page Header -->
    <div class="sr-page-header">
      <h1>内推排行榜</h1>
      <p>查看内推达人排名</p>
    </div>

    <!-- Loading State -->
    <template v-if="loading">
      <div class="sr-section">
        <el-skeleton :rows="6" animated />
      </div>
    </template>

    <!-- Error State -->
    <div v-else-if="error" class="sr-section">
      <div class="lb-error-state">
        <div class="error-icon">
          <el-icon :size="40"><WarningFilled /></el-icon>
        </div>
        <p class="error-message">{{ error }}</p>
        <el-button type="primary" @click="fetchLeaderboard" :loading="loading">重新加载</el-button>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="leaderboard.length === 0" class="sr-section">
      <div class="sr-empty">
        <el-empty description="暂无排行数据" :image-size="120" />
      </div>
    </div>

    <!-- Content -->
    <template v-else>
      <!-- Top 3 Podium -->
      <div class="lb-podium">
        <!-- Rank 2 (left) -->
        <div
          class="lb-podium-card lb-podium-2"
          v-if="leaderboard.length >= 2"
        >
          <div class="lb-podium-badge lb-badge-silver">
            <span>2</span>
          </div>
          <el-avatar
            :size="56"
            style="background: linear-gradient(135deg, #94a3b8, #64748b); font-size: 22px;"
          >
            {{ getDisplayInitial(leaderboard[1]) }}
          </el-avatar>
          <div class="lb-podium-name">{{ getDisplayName(leaderboard[1]) }}</div>
          <div class="lb-podium-dept">{{ leaderboard[1].departmentName || '-' }}</div>
          <div class="lb-podium-stats">
            <span>{{ leaderboard[1].referralCount }}次推荐</span>
          </div>
          <div class="lb-podium-bonus">&yen;{{ formatAmount(leaderboard[1].totalBonus) }}</div>
        </div>

        <!-- Rank 1 (center, largest) -->
        <div
          class="lb-podium-card lb-podium-1"
          v-if="leaderboard.length >= 1"
        >
          <div class="lb-podium-crown">
            <el-icon :size="28"><TrophyBase /></el-icon>
          </div>
          <div class="lb-podium-badge lb-badge-gold">
            <span>1</span>
          </div>
          <el-avatar
            :size="64"
            style="background: linear-gradient(135deg, #f59e0b, #d97706); font-size: 26px;"
          >
            {{ getDisplayInitial(leaderboard[0]) }}
          </el-avatar>
          <div class="lb-podium-name">{{ getDisplayName(leaderboard[0]) }}</div>
          <div class="lb-podium-dept">{{ leaderboard[0].departmentName || '-' }}</div>
          <div class="lb-podium-stats">
            <span>{{ leaderboard[0].referralCount }}次推荐</span>
          </div>
          <div class="lb-podium-bonus lb-bonus-gold">&yen;{{ formatAmount(leaderboard[0].totalBonus) }}</div>
        </div>

        <!-- Rank 3 (right) -->
        <div
          class="lb-podium-card lb-podium-3"
          v-if="leaderboard.length >= 3"
        >
          <div class="lb-podium-badge lb-badge-bronze">
            <span>3</span>
          </div>
          <el-avatar
            :size="56"
            style="background: linear-gradient(135deg, #d97706, #b45309); font-size: 22px;"
          >
            {{ getDisplayInitial(leaderboard[2]) }}
          </el-avatar>
          <div class="lb-podium-name">{{ getDisplayName(leaderboard[2]) }}</div>
          <div class="lb-podium-dept">{{ leaderboard[2].departmentName || '-' }}</div>
          <div class="lb-podium-stats">
            <span>{{ leaderboard[2].referralCount }}次推荐</span>
          </div>
          <div class="lb-podium-bonus">&yen;{{ formatAmount(leaderboard[2].totalBonus) }}</div>
        </div>
      </div>

      <!-- Full Ranking Table (rank 4+) -->
      <div class="sr-section" v-if="leaderboard.length > 3">
        <el-table :data="rankTableData" stripe style="width: 100%;">
          <el-table-column label="排名" width="70" align="center">
            <template #default="{ row }">
              <span class="lb-rank">{{ row.rank }}</span>
            </template>
          </el-table-column>
          <el-table-column label="姓名" min-width="120">
            <template #default="{ row }">
              {{ getDisplayName(row) }}
            </template>
          </el-table-column>
          <el-table-column label="部门" min-width="140">
            <template #default="{ row }">
              {{ row.departmentName || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="推荐数" width="100" align="center">
            <template #default="{ row }">
              {{ row.referralCount }}
            </template>
          </el-table-column>
          <el-table-column label="总奖金" width="140">
            <template #default="{ row }">
              <span class="lb-bonus-cell">&yen;{{ formatMoney(row.totalBonus) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { TrophyBase, WarningFilled } from '@element-plus/icons-vue'
import { getLeaderboard } from '@/api/referral'
import type { LeaderboardVO } from '@/types/models'

// ---- Helpers ----

function getDisplayName(item: LeaderboardVO): string {
  return item.referrerName || `用户#${item.referrerId}`
}

function getDisplayInitial(item: LeaderboardVO): string {
  if (item.referrerName) {
    return item.referrerName.charAt(0)
  }
  return '?'
}

// ---- State ----

const loading = ref(false)
const error = ref('')
const leaderboard = ref<LeaderboardVO[]>([])

// ---- Computed ----

/** Rank 4+ rows for the table */
const rankTableData = computed<LeaderboardVO[]>(() => {
  return leaderboard.value.slice(3)
})

// ---- Methods ----

function formatAmount(amount: number | null | undefined): string {
  if (amount == null || amount === 0) return '0'
  if (amount >= 10000) {
    return `${(amount / 10000).toFixed(1)}万`
  }
  return amount.toLocaleString('zh-CN')
}

function formatMoney(amount: number | null | undefined): string {
  if (amount == null || amount === 0) return '0'
  return amount.toLocaleString('zh-CN')
}

async function fetchLeaderboard() {
  loading.value = true
  error.value = ''
  try {
    leaderboard.value = await getLeaderboard()
  } catch (e: any) {
    console.error('Failed to load leaderboard:', e)
    error.value = e?.message || '加载排行榜数据失败'
    leaderboard.value = []
  } finally {
    loading.value = false
  }
}

// ---- Lifecycle ----

onMounted(() => {
  fetchLeaderboard()
})
</script>

<style scoped>
.referral-leaderboard-page {
  animation: lb-fade-in 0.3s ease;
}

@keyframes lb-fade-in {
  from { opacity: 0; transform: translateY(6px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ---- Error State ---- */
.lb-error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 0;
  text-align: center;
}

.error-icon {
  color: var(--c-danger);
  margin-bottom: 12px;
}

.error-message {
  font-size: 14px;
  color: var(--c-text-secondary);
  margin-bottom: 16px;
}

/* ---- Empty State ---- */
.sr-empty {
  text-align: center;
}

/* ---- Podium Section ---- */
.lb-podium {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 20px;
  margin-bottom: 32px;
  padding: 24px 0;
}

.lb-podium-card {
  background: var(--c-card);
  border-radius: var(--c-radius-lg);
  box-shadow: var(--c-shadow-md);
  padding: 24px 20px;
  text-align: center;
  width: 200px;
  border: 2px solid var(--c-border);
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.lb-podium-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--c-shadow-lg);
}

.lb-podium-1 {
  width: 240px;
  padding: 36px 24px 28px;
  border-color: #f59e0b;
  background: linear-gradient(180deg, #fff7ed 0%, var(--c-card) 30%);
}

.lb-podium-1::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: var(--c-radius-lg);
  padding: 2px;
  background: linear-gradient(135deg, #f59e0b, #fbbf24, #f59e0b);
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}

.lb-podium-2 {
  border-color: #94a3b8;
}

.lb-podium-3 {
  border-color: #b45309;
}

.lb-podium-crown {
  margin-top: -48px;
  margin-bottom: 8px;
  color: #f59e0b;
}

.lb-podium-badge {
  position: absolute;
  top: -16px;
  right: -8px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  box-shadow: var(--c-shadow-md);
}

.lb-badge-gold {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.lb-badge-silver {
  background: linear-gradient(135deg, #94a3b8, #64748b);
}

.lb-badge-bronze {
  background: linear-gradient(135deg, #d97706, #92400e);
}

.lb-podium-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--c-text);
  margin: 12px 0 4px;
}

.lb-podium-dept {
  font-size: 12px;
  color: var(--c-text-secondary);
  margin-bottom: 8px;
}

.lb-podium-stats {
  display: flex;
  justify-content: center;
  gap: 12px;
  font-size: 12px;
  color: var(--c-text-muted);
}

.lb-podium-bonus {
  font-size: 16px;
  font-weight: 700;
  color: var(--c-primary);
  margin-top: 8px;
}

.lb-bonus-gold {
  color: #d97706;
  font-size: 20px;
}

/* ---- Table ---- */
.lb-rank {
  font-weight: 600;
  color: var(--c-text-secondary);
}

.lb-bonus-cell {
  font-weight: 600;
  color: var(--c-primary);
}
</style>
