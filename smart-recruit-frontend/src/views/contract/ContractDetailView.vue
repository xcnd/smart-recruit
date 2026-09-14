<template>
  <div class="contract-detail-page">
    <!-- 页面操作区（打印时隐藏） -->
    <div class="no-print">
      <div class="detail-header">
        <el-button :icon="Back" @click="router.back()">返回</el-button>
        <h1>合同详情 · {{ detail?.contractNo || '' }}</h1>
        <el-tag v-if="detail" :type="statusTagType(detail.status)" size="large">{{ detail.statusLabel }}</el-tag>
      </div>

      <div class="detail-actions">
        <el-button v-if="detail?.status === 0" type="warning" @click="submitApproval">提交审批</el-button>
        <el-button v-if="detail?.status === 1" type="success" @click="approve">审批通过</el-button>
        <el-button
          v-if="detail?.status === 2"
          type="primary"
          :loading="sending"
          @click="send"
        >
          {{ sending ? '发送中...' : '发送候选人签署' }}
        </el-button>
        <el-button v-if="detail && [2, 3, 4].includes(detail.status)" type="success" @click="openHrSign">HR 签署</el-button>
        <el-button v-if="detail && [4, 7].includes(detail.status)" type="danger" @click="voidIt">作废</el-button>
        <el-button v-if="detail && [4, 6, 7].includes(detail.status)" type="primary" :icon="Printer" @click="doPrint">打印合同</el-button>
        <el-button v-if="detail && [0, 5].includes(detail.status)" type="danger" @click="remove">删除</el-button>
      </div>
    </div>

    <!-- 合同正文（打印区域） -->
    <div class="print-area">
      <div class="contract-paper">
        <div v-if="detail?.status === 4" class="contract-status-badge blue no-print">已签署</div>
        <div v-else-if="detail?.status === 7" class="contract-status-badge green no-print">生效中</div>
        <div v-else-if="detail?.status === 8" class="contract-status-badge red no-print">已作废</div>
        <div class="contract-head">
          <h2>录用合同（Offer 合同）</h2>
          <div class="contract-no">合同编号：{{ detail?.contractNo }}</div>
        </div>
        <div class="contract-content" v-html="displayContentHtml || '暂无合同正文'"></div>
        <div class="contract-sign-area">
          <div class="sign-box">
            <div class="sign-label">甲方（用人单位）</div>
            <div class="sign-line">{{ settingsStore.companyName || 'SmartRecruit 科技有限公司' }}</div>
            <div class="sign-line">签署人：{{ settingsStore.companyLegalRep || '________' }}</div>
            <div class="sign-line" v-if="detail?.signTime">签署时间：{{ detail?.signTime }}</div>
          </div>
          <div class="sign-box">
            <div class="sign-label">乙方（候选人）</div>
            <img v-if="detail?.candidateSignature" :src="detail.candidateSignature" class="sig-img" alt="候选人电子签章" />
            <div class="sign-line" v-if="!detail?.candidateSignature" style="color: #94a3b8">（待签署）</div>
            <div class="sign-line" v-if="detail?.signTime">签署时间：{{ detail?.signTime }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 签署记录（打印时隐藏） -->
    <div class="no-print sign-records">
      <h3>签署记录</h3>
      <el-table :data="detail?.signRecords || []" stripe size="small">
        <el-table-column prop="signerTypeLabel" label="签署方" width="100" />
        <el-table-column prop="signerName" label="签署人" width="130" />
        <el-table-column prop="actionLabel" label="动作" width="90" />
        <el-table-column prop="signTime" label="签署时间" width="170" />
        <el-table-column prop="signIp" label="签署 IP" width="140" />
        <el-table-column prop="remark" label="备注" min-width="160" />
      </el-table>
      <el-empty v-if="!detail?.signRecords?.length" description="暂无签署记录" :image-size="60" />
      <div v-if="detail?.rejectReason" class="reject-reason">
        拒绝原因：{{ detail.rejectReason }}
      </div>
      <div v-if="detail?.status === 8 && detail.voidReason" class="void-reason">
        作废原因：{{ detail.voidReason }}
      </div>
    </div>

    <!-- HR 签署弹窗 -->
    <el-dialog v-model="hrSignVisible" title="HR 签署合同" width="420px">
      <el-form label-position="top">
        <el-form-item label="签署人姓名">
          <el-input v-model="hrSignName" placeholder="如：李娜" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="hrSignVisible = false">取消</el-button>
        <el-button type="primary" :loading="signing" @click="doHrSign">确认签署</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Printer } from '@element-plus/icons-vue'
import {
  getContractDetail,
  submitContractApproval,
  approveContract,
  sendContract,
  hrSignContract,
  voidContract,
  deleteContract,
} from '@/api/contract'
import { useSettingsStore } from '@/stores/settings'
import type { ContractDetailVO } from '@/types/models'

const route = useRoute()
const router = useRouter()
const settingsStore = useSettingsStore()

const detail = ref<ContractDetailVO | null>(null)
const hrSignVisible = ref(false)
const hrSignName = ref('')
const signing = ref(false)
const sending = ref(false)

/** HTML 转义，防止合同文本中的特殊字符破坏结构。 */
function escHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

/** yyyy-MM-dd。 */
function fmtDate(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

/** 合同正文：甲方信息用系统配置实时替换占位线，填入内容保留底部下划线。 */
const displayContentHtml = computed(() => {
  let html = escHtml(detail.value?.content || '')
  // 去掉合同头部的签订日期行（历史合同兼容）
  html = html.replace(/^签订日期：[^\n]*\n+/, '')
  // 合同期限：缺省自动带出「今天起 3 年」，日期加下划线
  const termMatch = html.match(/合同期限自 ([^\s]+) 起至 ([^\s]+) 止/)
  if (termMatch) {
    const today = new Date()
    const from = termMatch[1] === '—' ? fmtDate(today) : termMatch[1]
    const until = termMatch[2] === '—'
      ? fmtDate(new Date(today.getFullYear() + 3, today.getMonth(), today.getDate()))
      : termMatch[2]
    html = html.replace(/合同期限自 [^\s]+ 起至 [^\s]+ 止/,
      `合同期限自 <span class="fill-line">${from}</span> 起至 <span class="fill-line">${until}</span> 止`)
  }
  // 入职日期与关键薪资加下划线
  html = html.replace(/预计为 ([0-9]{4}-[0-9]{2}-[0-9]{2})/g,
    '预计为 <span class="fill-line">$1</span>')
  html = html.replace(/试用期税前月工资为人民币 ([0-9,]+) 元/g,
    '试用期税前月工资为人民币 <span class="fill-line">$1</span> 元')
  html = html.replace(/转正后税前月工资为人民币 ([0-9,]+) 元/g,
    '转正后税前月工资为人民币 <span class="fill-line">$1</span> 元')
  html = html.replace(/签约奖金（([0-9,]+) 元）/g,
    '签约奖金（<span class="fill-line">$1</span> 元）')
  html = html.replace(/预计为人民币 ([0-9,]+) 元/g,
    '预计为人民币 <span class="fill-line">$1</span> 元')
  // 职级加下划线（如 职级 P7）
  html = html.replace(/职级 ([A-Za-z0-9-]+)/g,
    '职级 <span class="fill-line">$1</span>')
  // 岗位名称加下划线
  html = html.replace(/担任 ([^，]+?) 岗位/g,
    '担任 <span class="fill-line">$1</span> 岗位')
  if (settingsStore.companyName) {
    html = html.replace(/甲方（用人单位）：[^\n]*/, `甲方（用人单位）：${escHtml(settingsStore.companyName)}`)
  }
  if (settingsStore.companyCreditCode) {
    html = html.replace(/统一社会信用代码：_+/g,
      `统一社会信用代码：<span class="fill-line">${escHtml(settingsStore.companyCreditCode)}</span>`)
  }
  if (settingsStore.companyAddress) {
    html = html.replace(/住所：_+/g,
      `住所：<span class="fill-line">${escHtml(settingsStore.companyAddress)}</span>`)
  }
  if (settingsStore.companyLegalRep) {
    html = html.replace(/法定代表人：_+/g,
      `法定代表人：<span class="fill-line">${escHtml(settingsStore.companyLegalRep)}</span>`)
  }
  // 乙方信息：用合同已保存的值替换占位线
  if (detail.value?.candidateIdCard) {
    html = html.replace(/身份证号码：_+/g,
      `身份证号码：<span class="fill-line">${escHtml(detail.value.candidateIdCard)}</span>`)
  }
  if (detail.value?.candidatePhone) {
    html = html.replace(/联系电话：_+/g,
      `联系电话：<span class="fill-line">${escHtml(detail.value.candidatePhone)}</span>`)
  }
  if (detail.value?.candidateAddress) {
    html = html.replace(/通讯地址：_+/g,
      `通讯地址：<span class="fill-line">${escHtml(detail.value.candidateAddress)}</span>`)
  }
  return html
})

async function loadDetail() {
  try {
    detail.value = await getContractDetail(String(route.params.id))
    if (route.query.print === '1' && detail.value && [4, 6].includes(detail.value.status)) {
      setTimeout(() => window.print(), 300)
    }
  } catch {
    ElMessage.error('合同详情加载失败')
  }
}

function doPrint() {
  window.print()
}

async function submitApproval() {
  await ElMessageBox.confirm('确认提交审批？', '提交审批', { type: 'warning' })
  await submitContractApproval(String(detail.value?.id))
  ElMessage.success('已提交审批')
  loadDetail()
}

async function approve() {
  await ElMessageBox.confirm('确认审批通过？', '审批通过', { type: 'warning' })
  await approveContract(String(detail.value?.id))
  ElMessage.success('审批通过')
  loadDetail()
}

async function send() {
  await ElMessageBox.confirm('确认发送给候选人签署？', '发送签署', { type: 'warning' })
  sending.value = true
  try {
    await sendContract(String(detail.value?.id))
    ElMessage.success('已发送签署邮件至候选人邮箱')
    loadDetail()
  } catch {
    ElMessage.error('发送失败，请稍后重试')
  } finally {
    sending.value = false
  }
}

function openHrSign() {
  hrSignName.value = ''
  hrSignVisible.value = true
}

async function doHrSign() {
  if (!hrSignName.value.trim()) {
    ElMessage.warning('请输入签署人姓名')
    return
  }
  signing.value = true
  try {
    await hrSignContract(String(detail.value?.id), hrSignName.value.trim())
    ElMessage.success('HR 签署成功')
    hrSignVisible.value = false
    loadDetail()
  } catch {
    ElMessage.error('签署失败')
  } finally {
    signing.value = false
  }
}

async function remove() {
  await ElMessageBox.confirm('确认删除该合同？', '删除', { type: 'warning' })
  await deleteContract(String(detail.value?.id))
  ElMessage.success('已删除')
  router.push('/contracts')
}

async function voidIt() {
  try {
    const { value } = await ElMessageBox.prompt('请填写作废原因', '作废合同', {
      confirmButtonText: '确认作废',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入作废原因（必填）',
      inputValidator: (v: string) => (v && v.trim() ? true : '作废原因不能为空'),
      type: 'warning',
    })
    await voidContract(String(detail.value?.id), value.trim())
    ElMessage.success('合同已作废')
    loadDetail()
  } catch {
    // 用户取消或校验未通过
  }
}

function statusTagType(status?: number): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  if (status === 4) return 'primary'
  if (status === 6 || status === 7) return 'success'
  if (status === 1 || status === 2) return 'warning'
  if (status === 5 || status === 8) return 'danger'
  return 'info'
}

onMounted(loadDetail)
</script>

<style scoped>
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.detail-header h1 {
  font-size: 18px;
  margin: 0;
}

.detail-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.contract-paper {
  position: relative;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 32px 40px;
  max-width: 860px;
}

.contract-status-badge {
  position: absolute;
  top: 20px;
  right: 20px;
  border-radius: 8px;
  padding: 4px 14px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 3px;
  transform: rotate(-8deg);
  opacity: 0.85;
  user-select: none;
}

.contract-status-badge.blue { color: #2563eb; border: 2px solid #2563eb; }
.contract-status-badge.green { color: #059669; border: 2px solid #059669; }
.contract-status-badge.red { color: #dc2626; border: 2px solid #dc2626; }

.contract-head {
  text-align: center;
  border-bottom: 2px solid #1e293b;
  padding-bottom: 12px;
  margin-bottom: 16px;
}

.contract-head h2 {
  margin: 0 0 6px;
  font-size: 22px;
  letter-spacing: 4px;
}

.contract-no {
  font-size: 13px;
  color: #64748b;
}

.contract-meta {
  margin-bottom: 20px;
}

.contract-content {
  font-size: 14px;
  line-height: 2;
  color: #1e293b;
  white-space: pre-wrap;
  margin-bottom: 28px;
  min-height: 260px;
}

.contract-content :deep(.fill-line) {
  border-bottom: 1px solid #334155;
  padding: 0 2px 1px;
}

.contract-sign-area {
  display: flex;
  justify-content: space-between;
  border-top: 1px dashed #cbd5e1;
  padding-top: 24px;
}

.sign-box {
  width: 45%;
}

.sign-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 12px;
}

.sign-line {
  font-size: 14px;
  line-height: 2;
  color: #1e293b;
}

.sig-img {
  max-width: 180px;
  max-height: 80px;
  margin-top: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  background: #fff;
}

.sign-records {
  margin-top: 24px;
  max-width: 860px;
}

.sign-records h3 {
  margin: 0 0 12px;
  font-size: 16px;
}

.reject-reason {
  margin-top: 12px;
  color: #dc2626;
  font-size: 13px;
}

.void-reason {
  margin-top: 12px;
  padding: 10px 14px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 6px;
  color: #b91c1c;
  font-size: 13px;
  line-height: 1.6;
}

/* 打印样式：仅打印合同纸张 */
@media print {
  .no-print {
    display: none !important;
  }
  .contract-paper {
    border: none;
    padding: 18mm 14mm;
    max-width: none;
  }
}
</style>

<!-- 全局打印样式：隐藏后台布局（侧边栏/顶栏/页脚），只打印合同内容 -->
<style>
/* 页边距归零，让 Chrome/Edge 不打印浏览器自带的页眉页脚（URL/标题/时间） */
@page {
  margin: 0;
}

@media print {
  .sr-sidebar,
  .sr-topbar,
  .sr-footer {
    display: none !important;
  }
  .sr-content {
    padding: 0 !important;
  }
}
</style>
