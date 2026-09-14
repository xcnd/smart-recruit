<template>
  <div class="sign-page">
    <div class="sign-header no-print">
      <h1>合同签署</h1>
      <p>请仔细阅读合同内容，确认无误后进行签署</p>
    </div>

    <div v-if="loading" class="sign-loading no-print" v-loading="true" element-loading-text="合同加载中..."></div>

    <template v-else-if="contract">
      <!-- 已签署标识 -->
      <!-- 合同预览 -->
      <div class="contract-paper">
        <!-- 已签署/生效中标识（合同右上角） -->
        <div v-if="contract.status === 4" class="signed-badge blue no-print">已签署</div>
        <div v-else-if="contract.status === 7" class="signed-badge green no-print">生效中</div>
        <div v-else-if="contract.status === 8" class="signed-badge red no-print">已作废</div>
        <div class="contract-head">
          <h2>录用合同（Offer 合同）</h2>
          <div class="contract-no">合同编号：{{ contract.contractNo }}</div>
        </div>
        <div class="contract-content" v-html="displayContentHtml || '暂无合同正文'"></div>
        <div class="contract-sign-area">
          <div class="sign-box">
            <div class="sign-label">甲方（用人单位）</div>
            <div class="sign-line">{{ settingsStore.companyName || 'SmartRecruit 科技有限公司' }}</div>
            <div class="sign-line">签署人：{{ settingsStore.companyLegalRep || '________' }}</div>
          </div>
          <div class="sign-box">
            <div class="sign-label">乙方（候选人）</div>
            <img v-if="contract.candidateSignature" :src="contract.candidateSignature" class="sig-img" alt="候选人电子签章" />
            <div class="sign-line" v-else style="color: #94a3b8">（待签署）</div>
          </div>
        </div>
      </div>

      <!-- 签署操作 -->
      <div v-if="contract.status === 3" class="sign-actions no-print">
        <div class="candidate-form">
          <div class="candidate-form-title">乙方信息确认（请如实填写）</div>
          <el-form label-position="top">
            <div class="candidate-form-grid">
              <el-form-item label="身份证号码">
                <el-input v-model="candidateInfo.idCard" placeholder="请输入身份证号码" size="large" />
              </el-form-item>
              <el-form-item label="联系电话">
                <el-input v-model="candidateInfo.phone" placeholder="请输入手机号码" size="large" />
              </el-form-item>
            </div>
            <el-form-item label="通讯地址">
              <el-input v-model="candidateInfo.address" placeholder="请输入通讯地址（选填）" size="large" />
            </el-form-item>
          </el-form>
        </div>
        <div class="signature-pad">
          <div class="signature-pad-title">手写电子签章</div>
          <div class="sig-wrap" @pointerdown="onPadBegin">
            <SignaturePad ref="padRef" @begin="onPadBegin" @change="onPadChange" />
            <div v-if="!signed && !padTouched" class="sig-guide">请在此处手写签名</div>
          </div>
          <div class="signature-pad-actions">
            <el-button size="small" @click="clearSignature">清除重签</el-button>
            <span v-if="signed" class="sig-ok">✔ 已签名（{{ contract?.candidateName }}）</span>
            <span v-else class="sig-hint">请在框内手写您的签名（支持鼠标/手指）</span>
          </div>
        </div>
        <div class="sign-buttons">
          <el-button type="primary" size="large" :loading="signing" @click="doSign(true)">确认签署</el-button>
          <el-button size="large" :loading="signing" @click="doSign(false)">拒绝签署</el-button>
        </div>
      </div>

      <!-- 签署完成状态 -->
      <el-result
        class="no-print"
        v-if="finished"
        :icon="finishedAccept ? 'success' : 'warning'"
        :title="finishedAccept ? '签署成功' : '已拒绝签署'"
        :sub-title="finishedAccept ? `您已于 ${finishTime} 完成签署` : '您的拒绝意见已记录，HR 会与您联系'"
      />
    </template>

    <el-result v-else-if="loadError" class="no-print" icon="error" title="链接无效或已失效" sub-title="请与 HR 联系获取正确的签署链接" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getContractByToken, signContractByToken } from '@/api/contract'
import SignaturePad from '@/components/SignaturePad.vue'
import { useSettingsStore } from '@/stores/settings'
import type { ContractDetailVO } from '@/types/models'

const route = useRoute()
const token = String(route.params.token)
const settingsStore = useSettingsStore()

const loading = ref(true)
const loadError = ref(false)
const contract = ref<ContractDetailVO | null>(null)
const signing = ref(false)
const finished = ref(false)
const finishedAccept = ref(false)
const finishTime = ref('')

// ==================== 乙方信息（候选人签署时填写） ====================
const candidateInfo = reactive({
  idCard: '',
  phone: '',
  address: '',
})

/** HTML 转义，防止合同文本中的特殊字符破坏结构。 */
function escHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

/** yyyy-MM-dd。 */
function fmtDate(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

/** 合同正文：甲方信息/乙方表单填入的值显示在填空线上（保留下划线）。 */
const displayContentHtml = computed(() => {
  let html = escHtml(contract.value?.content || '')
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
  // 甲方信息：用系统配置实时替换占位线（兼容历史合同）
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
  // 乙方信息：优先用合同已保存的值（签署后刷新页面也不会丢），表单状态仅作签署前实时预览
  const idCard = contract.value?.candidateIdCard || candidateInfo.idCard.trim()
  const phone = contract.value?.candidatePhone || candidateInfo.phone.trim()
  const address = contract.value?.candidateAddress || candidateInfo.address.trim()
  if (idCard) html = html.replace(/身份证号码：_+/g,
    `身份证号码：<span class="fill-line">${escHtml(idCard)}</span>`)
  if (phone) html = html.replace(/联系电话：_+/g,
    `联系电话：<span class="fill-line">${escHtml(phone)}</span>`)
  if (address) html = html.replace(/通讯地址：_+/g,
    `通讯地址：<span class="fill-line">${escHtml(address)}</span>`)
  return html
})

// ==================== 手写电子签章 ====================
const padRef = ref<{ clear(): void; getSignature(): string | null } | null>(null)
const signatureData = ref('')
const signed = ref(false)
const padTouched = ref(false)

// 用户填写乙方信息后，签名引导文案也不再显示
/** 用户点击/落笔即隐藏引导文字。 */
function onPadBegin() {
  padTouched.value = true
}

/** 签名板内容变化回调。 */
function onPadChange(empty: boolean) {
  signed.value = !empty
  signatureData.value = empty ? '' : (padRef.value?.getSignature() || '')
}

/** 清除重签。 */
function clearSignature() {
  padRef.value?.clear()
  signed.value = false
  padTouched.value = false
  signatureData.value = ''
}

async function loadContract() {
  loading.value = true
  loadError.value = false
  try {
    contract.value = await getContractByToken(token)
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

async function doSign(accept: boolean) {
  if (accept) {
    // 提交时直接从画板读取签名，避免依赖事件时序
    const sig = padRef.value?.getSignature()
    if (!sig) {
      ElMessage.warning('请在签名板上手写您的电子签章')
      return
    }
    signatureData.value = sig

    // 乙方信息校验：身份证 15/18 位，手机号 11 位
    const idCard = candidateInfo.idCard.trim()
    const phone = candidateInfo.phone.trim()
    if (!/^\d{15}$|^\d{17}[\dXx]$/.test(idCard)) {
      ElMessage.warning('请输入正确的身份证号码')
      return
    }
    if (!/^1\d{10}$/.test(phone)) {
      ElMessage.warning('请输入正确的手机号码')
      return
    }
  }
  const name = contract.value?.candidateName || '候选人'
  let remark: string | undefined
  if (!accept) {
    try {
      const { value } = await ElMessageBox.prompt('请填写拒绝原因（可选）', '拒绝签署', {
        confirmButtonText: '确认拒绝',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：薪资与预期不符',
      })
      remark = value || undefined
    } catch {
      return
    }
  }
  signing.value = true
  try {
    await signContractByToken(token, {
      name,
      accept,
      remark,
      signature: accept ? signatureData.value : undefined,
      idCard: accept ? candidateInfo.idCard.trim() : undefined,
      phone: accept ? candidateInfo.phone.trim() : undefined,
      address: accept ? candidateInfo.address.trim() || undefined : undefined,
    })
    finished.value = true
    finishedAccept.value = accept
    finishTime.value = new Date().toLocaleString('zh-CN')
    await loadContract()
    if (accept) ElMessage.success('签署成功')
  } catch {
    ElMessage.error('签署失败，请稍后重试')
  } finally {
    signing.value = false
  }
}

onMounted(loadContract)
</script>

<style scoped>
.sign-page {
  position: relative;
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 20px 60px;
}

.signed-badge {
  position: absolute;
  top: 24px;
  right: 20px;
  border-radius: 8px;
  padding: 6px 16px;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 4px;
  transform: rotate(-8deg);
  opacity: 0.85;
  user-select: none;
}

.signed-badge.blue { color: #2563eb; border: 2px solid #2563eb; }
.signed-badge.green { color: #059669; border: 2px solid #059669; }
.signed-badge.red { color: #dc2626; border: 2px solid #dc2626; }

.sign-header {
  text-align: center;
  margin-bottom: 24px;
}

.sign-header h1 {
  margin: 0 0 6px;
  font-size: 24px;
}

.sign-header p {
  margin: 0;
  color: #94a3b8;
}

.sign-loading {
  min-height: 300px;
}

.contract-paper {
  position: relative;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 32px 40px;
}

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

.contract-meta p {
  margin: 4px 0;
  font-size: 14px;
}

.contract-content {
  font-size: 14px;
  line-height: 2;
  color: #1e293b;
  white-space: pre-wrap;
  margin: 20px 0 28px;
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
}

.sign-actions {
  margin-top: 24px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 20px 24px;
}

.sign-buttons {
  display: flex;
  gap: 12px;
}

.candidate-form {
  margin-bottom: 18px;
}

.candidate-form-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
}

.candidate-form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.signature-pad {
  margin-bottom: 18px;
}

.signature-pad-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}

.sig-wrap {
  position: relative;
}

.sig-guide {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 18px;
  pointer-events: none;
  user-select: none;
}

.signature-pad-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.sig-hint {
  font-size: 12px;
  color: #94a3b8;
}

.sig-ok {
  font-size: 12px;
  font-weight: 600;
  color: #059669;
}

.sig-img {
  max-width: 180px;
  max-height: 80px;
  margin-top: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  background: #fff;
}

/* 打印：只输出合同纸张，隐藏页面头部、表单与操作区 */
@media print {
  .no-print {
    display: none !important;
  }
  .sign-page {
    padding: 0;
    max-width: none;
  }
  .contract-paper {
    border: none;
    padding: 18mm 14mm;
  }
  .contract-content {
    min-height: 0;
  }
}
</style>

<!-- 全局打印样式：页边距归零，去掉浏览器自带的页眉页脚（URL/标题/时间） -->
<style>
@page {
  margin: 0;
}
</style>
