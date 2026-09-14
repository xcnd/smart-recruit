<template>
  <div class="approval-page">
    <div class="sr-page-header">
      <div class="page-header-row">
        <div>
          <h1>Offer审批</h1>
          <p>集中处理待审批的Offer，查看审批历史记录</p>
        </div>
        <el-button type="primary" @click="openConfigDialog">
          <el-icon><Setting /></el-icon>
          配置审批流程
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="sr-section">
      <el-row :gutter="16" class="stats-row">
        <el-col :span="6">
          <div class="stat-card stat-pending">
            <div class="stat-value">{{ stats.pendingCount }}</div>
            <div class="stat-label">待审批</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-approved">
            <div class="stat-value">{{ stats.todayApproved }}</div>
            <div class="stat-label">今日通过</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-rejected">
            <div class="stat-value">{{ stats.todayRejected }}</div>
            <div class="stat-label">今日驳回</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-monthly">
            <div class="stat-value">{{ stats.monthlyTotal }}</div>
            <div class="stat-label">本月审批</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- Tab: 待审批 / 已审核 -->
    <div class="sr-section">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <!-- ====== 待审批 Tab ====== -->
        <el-tab-pane label="待审批" name="pending">
          <div class="sr-toolbar">
            <div class="sr-filter-bar">
              <el-input
                v-model="pendingFilters.candidateName"
                placeholder="搜索候选人姓名"
                :prefix-icon="Search"
                clearable
                style="width: 220px"
                @input="handlePendingSearch"
              />
              <el-select
                v-model="pendingFilters.departmentName"
                placeholder="部门筛选"
                clearable
                style="width: 180px"
                @change="handlePendingSearch"
              >
                <el-option label="技术研发部" value="技术研发部" />
                <el-option label="产品部" value="产品部" />
                <el-option label="设计部" value="设计部" />
                <el-option label="市场部" value="市场部" />
                <el-option label="运营部" value="运营部" />
                <el-option label="人力资源部" value="人力资源部" />
                <el-option label="财务部" value="财务部" />
              </el-select>
              <el-input
                v-model="pendingFilters.offerNo"
                placeholder="Offer编号"
                clearable
                style="width: 180px"
                @input="handlePendingSearch"
              />
              <el-date-picker
                v-model="pendingFilters.expectedOnboardDate"
                placeholder="入职时间"
                type="date"
                value-format="YYYY-MM-DD"
                clearable
                style="width: 160px"
                @change="handlePendingSearch"
              />
              <el-button :icon="Refresh" @click="loadPending">刷新</el-button>
            </div>
          </div>

          <el-table v-loading="pendingLoading" :data="pendingList" stripe empty-text="暂无待审批的Offer">
            <el-table-column type="index" label="序号" width="55" />
            <el-table-column prop="offerNo" label="Offer编号" width="175" />
            <el-table-column prop="candidateId" label="候选人ID" width="90" />
            <el-table-column prop="candidateName" label="候选人" width="100" />
            <el-table-column prop="positionTitle" label="职位" min-width="140" />
            <el-table-column prop="departmentName" label="部门" width="120" />
            <el-table-column prop="level" label="职级" width="70" />
            <el-table-column prop="creatorName" label="申请人" width="90" />
            <el-table-column prop="expectedOnboardDate" label="入职时间" width="110" />
            <el-table-column label="审核进度" width="160">
              <template #default="{ row }">
                <el-popover placement="bottom" :width="220" trigger="hover" :show-after="200">
                  <template #reference>
                    <span class="flow-summary">
                      {{ getFlowSummary(row) }}
                    </span>
                  </template>
                  <div class="flow-popover">
                    <div
                      v-for="(stage, si) in getFlowStages(row.departmentName)"
                      :key="si"
                      class="flow-popover-stage"
                      :class="{
                        active: (row.currentLevel || 1) >= stage.level,
                        current: (row.currentLevel || 1) === stage.level,
                      }"
                    >
                      <span class="flow-popover-dot" />
                      <div class="flow-popover-info">
                        <span class="flow-popover-label">{{ stage.nodeName }}</span>
                        <span class="flow-popover-approvers">
                          <template v-if="(row.currentLevel || 1) > stage.level">
                            已审批
                          </template>
                          <template v-else>
                            {{ stage.approvers?.map(a => a.approverName).join('、') || '待分配' }}
                          </template>
                        </span>
                      </div>
                      <el-tag
                        v-if="(row.currentLevel || 1) === stage.level"
                        size="small" type="warning"
                      >当前</el-tag>
                      <el-tag
                        v-else-if="(row.currentLevel || 1) > stage.level"
                        size="small" type="success"
                      >已通过</el-tag>
                      <el-tag v-else size="small" type="info">待审批</el-tag>
                    </div>
                  </div>
                </el-popover>
              </template>
            </el-table-column>
            <el-table-column label="审核人" width="160">
              <template #default="{ row }">
                {{ getCurrentApprovers(row) }}
              </template>
            </el-table-column>
            <el-table-column label="月薪(元)" width="110">
              <template #default="{ row }">
                {{ formatSalary(row.baseSalary) }}
              </template>
            </el-table-column>
            <el-table-column label="总包(万)" width="100">
              <template #default="{ row }">
                {{ formatPackage(row.totalPackage) }}
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" width="170">
              <template #default="{ row }">
                {{ formatDateTime(row.submitTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="showDetail(row.offerId)">
                  查看详情
                </el-button>
                <el-button link type="success" size="small" @click="openApproveDialog(row, true)">
                  通过
                </el-button>
                <el-button link type="danger" size="small" @click="openApproveDialog(row, false)">
                  驳回
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
            <el-pagination
              v-model:current-page="pendingPage"
              v-model:page-size="pendingSize"
              :total="pendingTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadPending"
              @current-change="loadPending"
            />
          </div>
        </el-tab-pane>

        <!-- ====== 已审核 Tab ====== -->
        <el-tab-pane label="已审核" name="history">
          <div class="sr-toolbar">
            <div class="sr-filter-bar">
              <el-input
                v-model="historyFilters.candidateName"
                placeholder="搜索候选人姓名"
                :prefix-icon="Search"
                clearable
                style="width: 220px"
              />
              <el-select
                v-model="historyFilters.departmentName"
                placeholder="部门筛选"
                clearable
                style="width: 180px"
              >
                <el-option label="技术研发部" value="技术研发部" />
                <el-option label="产品部" value="产品部" />
                <el-option label="设计部" value="设计部" />
                <el-option label="市场部" value="市场部" />
                <el-option label="运营部" value="运营部" />
                <el-option label="人力资源部" value="人力资源部" />
                <el-option label="财务部" value="财务部" />
              </el-select>
              <el-input
                v-model="historyFilters.offerNo"
                placeholder="Offer编号"
                clearable
                style="width: 180px"
              />
              <el-date-picker
                v-model="historyFilters.expectedOnboardDate"
                placeholder="入职时间"
                type="date"
                value-format="YYYY-MM-DD"
                clearable
                style="width: 160px"
              />
              <el-button :icon="Refresh" @click="loadHistory">刷新</el-button>
            </div>
          </div>

          <el-table v-loading="historyLoading" :data="filteredHistoryList" stripe empty-text="暂无审批记录">
            <el-table-column type="index" label="序号" width="55" />
            <el-table-column prop="offerNo" label="Offer编号" width="175" />
            <el-table-column prop="candidateId" label="候选人ID" width="90" />
            <el-table-column prop="candidateName" label="候选人" width="100" />
            <el-table-column prop="positionTitle" label="职位" min-width="140" />
            <el-table-column prop="departmentName" label="部门" width="120" />
            <el-table-column prop="level" label="职级" width="70" />
            <el-table-column prop="creatorName" label="申请人" width="90" />
            <el-table-column prop="expectedOnboardDate" label="入职时间" width="110" />
            <el-table-column label="审核进度" width="160">
              <template #default="{ row }">
                <el-popover placement="bottom" :width="220" trigger="hover" :show-after="200">
                  <template #reference>
                    <span class="flow-summary">
                      {{ getHistoryFlowSummary(row) }}
                    </span>
                  </template>
                  <div class="flow-popover">
                    <div
                      v-for="(stage, si) in getFlowStages(row.departmentName)"
                      :key="si"
                      class="flow-popover-stage"
                      :class="{
                        active: (row.currentLevel || 1) >= stage.level,
                        current: (row.currentLevel || 1) === stage.level,
                      }"
                    >
                      <span class="flow-popover-dot" />
                      <div class="flow-popover-info">
                        <span class="flow-popover-label">{{ stage.nodeName }}</span>
                        <span class="flow-popover-approvers" v-if="stage.approvers?.length">
                          <template v-if="(row.currentLevel || 1) > stage.level">
                            已审批
                          </template>
                          <template v-else-if="(row.currentLevel || 1) === stage.level && row.latestApproval?.status === 2">
                            驳回人：{{ row.latestApproval?.approverName }}
                          </template>
                          <template v-else>
                            审批人：{{ stage.approvers.map(a => a.approverName).join('、') }}
                          </template>
                        </span>
                      </div>
                      <el-tag
                        v-if="(row.currentLevel || 1) > stage.level"
                        size="small" type="success"
                      >已通过</el-tag>
                      <el-tag
                        v-else-if="(row.currentLevel || 1) === stage.level && row.latestApproval?.status === 2"
                        size="small" type="danger"
                      >已驳回</el-tag>
                      <el-tag
                        v-else-if="(row.currentLevel || 1) === stage.level"
                        size="small" type="success"
                      >已通过</el-tag>
                      <el-tag v-else size="small" type="info">未开始</el-tag>
                    </div>
                  </div>
                </el-popover>
              </template>
            </el-table-column>
            <el-table-column label="审核人" width="160">
              <template #default="{ row }">
                {{ getHistoryApprovers(row) }}
              </template>
            </el-table-column>
            <el-table-column label="月薪(元)" width="110">
              <template #default="{ row }">
                {{ formatSalary(row.baseSalary) }}
              </template>
            </el-table-column>
            <el-table-column label="总包(万)" width="100">
              <template #default="{ row }">
                {{ formatPackage(row.totalPackage) }}
              </template>
            </el-table-column>
            <el-table-column label="审批结果" width="100">
              <template #default="{ row }">
                <el-tag
                  :type="row.latestApproval?.status === 1 ? 'success' : 'danger'"
                  size="small"
                >
                  {{ row.latestApproval?.statusLabel || '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="最后审批时间" width="170">
              <template #default="{ row }">
                {{ formatDateTime(row.latestApproval?.approveTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="showDetail(row.offerId)">
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
            <el-pagination
              v-model:current-page="historyPage"
              v-model:page-size="historySize"
              :total="historyTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadHistory"
              @current-change="loadHistory"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ====== 审批详情抽屉 ====== -->
    <el-drawer
      v-model="drawerVisible"
      title="Offer详情"
      size="520px"
      direction="rtl"
    >
      <template v-if="detailOffer">
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">Offer编号</span>
              <span class="detail-value">{{ detailOffer.offerNo }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">候选人</span>
              <span class="detail-value">{{ detailOffer.candidateName }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">职位</span>
              <span class="detail-value">{{ detailOffer.positionTitle }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">部门</span>
              <span class="detail-value">{{ detailOffer.departmentName }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">职级</span>
              <span class="detail-value">{{ detailOffer.level || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">状态</span>
              <span class="detail-value">
                <el-tag :type="statusTagType(detailOffer.status)" size="small">
                  {{ statusLabel(detailOffer.status) }}
                </el-tag>
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-label">期望入职日期</span>
              <span class="detail-value">{{ detailOffer.expectedOnboardDate || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Offer有效期</span>
              <span class="detail-value">{{ detailOffer.validUntil || '-' }}</span>
            </div>
          </div>
        </div>

        <el-divider />

        <div class="detail-section">
          <h4>候选人信息</h4>
          <div v-if="detailCandidate" class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">手机号</span>
              <span class="detail-value">{{ detailCandidate.phone || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">邮箱</span>
              <span class="detail-value">{{ detailCandidate.email || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">学历</span>
              <span class="detail-value">{{ educationLabel(detailCandidate.education) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">毕业院校</span>
              <span class="detail-value">{{ detailCandidate.school || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">工作年限</span>
              <span class="detail-value">{{ detailCandidate.yearsOfExperience ? detailCandidate.yearsOfExperience + ' 年' : '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">当前公司</span>
              <span class="detail-value">{{ detailCandidate.currentCompany || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">当前职位</span>
              <span class="detail-value">{{ detailCandidate.currentPosition || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">简历</span>
              <span class="detail-value">
                <a v-if="detailResumeId" class="resume-link" @click.prevent="openResume(detailResumeId)">查看简历</a>
                <span v-else>-</span>
              </span>
            </div>
          </div>
          <el-skeleton v-else :rows="4" animated />
        </div>

        <el-divider />

        <div class="detail-section">
          <h4>薪资明细</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">月薪</span>
              <span class="detail-value">{{ formatSalary(detailOffer.baseSalary) }} 元</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">奖金月数</span>
              <span class="detail-value">{{ detailOffer.bonusMonths || '-' }} 个月</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">期权重酬</span>
              <span class="detail-value">{{ detailOffer.stockOptions ?? '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">签约奖金</span>
              <span class="detail-value">{{ detailOffer.signOnBonus ?? '-' }} 元</span>
            </div>
            <div class="detail-item" style="grid-column: span 2;">
              <span class="detail-label">年薪总包</span>
              <span class="detail-value" style="font-weight: 600; color: var(--c-primary); font-size: 16px;">
                {{ formatSalary(detailOffer.totalPackage) }} 元
              </span>
            </div>
          </div>
        </div>

        <el-divider />

        <div class="detail-section">
          <h4>面试情况</h4>
          <div v-if="detailInterviews.length > 0">
            <div
              v-for="(iv, idx) in detailInterviews"
              :key="iv.id"
              class="interview-card"
            >
              <div class="interview-header">
                <span class="interview-round">第{{ iv.round || idx + 1 }}轮 · {{ iv.typeLabel }}</span>
                <el-tag
                  :type="iv.result === 0 ? 'success' : iv.result === 1 ? 'danger' : 'info'"
                  size="small"
                >
                  {{ iv.result === 0 ? '通过' : iv.result === 1 ? '未通过' : iv.result === 2 ? '待定' : '未完成' }}
                </el-tag>
              </div>
              <div class="interview-meta">
                <span>面试官：{{ iv.interviewerName || '-' }}</span>
                <span v-if="iv.score != null">评分：<strong>{{ iv.score }}</strong> 分</span>
                <span v-if="iv.overallScore != null && iv.overallScore !== iv.score">综合分：<strong>{{ iv.overallScore }}</strong> 分</span>
              </div>
              <div v-if="iv.feedback" class="interview-feedback">
                <span class="feedback-label">面试反馈：</span>{{ iv.feedback }}
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无面试记录" :image-size="40" />
        </div>

        <el-divider />

        <div class="detail-section">
          <h4>审批历史</h4>
          <el-timeline v-if="detailOffer.approvals?.length">
            <el-timeline-item
              v-for="a in detailOffer.approvals"
              :key="a.id"
              :type="a.status === 1 ? 'success' : a.status === 2 ? 'danger' : 'info'"
              :timestamp="formatDateTime(a.approveTime)"
              placement="top"
            >
              <div class="approval-history-item">
                <div class="approval-history-header">
                  <span class="approval-history-approver">
                    {{ a.approverRole || a.approverName || '系统' }}
                  </span>
                  <el-tag
                    :type="a.status === 1 ? 'success' : a.status === 2 ? 'danger' : 'info'"
                    size="small"
                  >
                    {{ a.status === 1 ? '通过' : a.status === 2 ? '驳回' : '待审批' }}
                  </el-tag>
                </div>
                <div class="approval-history-name" v-if="a.approverRole && a.approverName">
                  {{ a.approverName }}
                </div>
                <div class="approval-history-level" v-if="a.approvalLevel">
                  第{{ a.approvalLevel }}级审批
                </div>
                <div class="approval-history-comment">
                  审核意见：{{ a.comment || (a.status === 1 ? '通过' : a.status === 2 ? '驳回' : '待审批') }}
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无审批记录" :image-size="40" />
        </div>
      </template>

      <template #footer>
        <el-button @click="drawerVisible = false">关闭</el-button>
        <el-button
          v-if="detailOffer && detailOffer.status === 1"
          type="success"
          @click="drawerVisible = false; openApproveDialog({ offerId: detailOffer.id, candidateName: detailOffer.candidateName }, true)"
        >
          审批通过
        </el-button>
        <el-button
          v-if="detailOffer && detailOffer.status === 1"
          type="danger"
          @click="drawerVisible = false; openApproveDialog({ offerId: detailOffer.id, candidateName: detailOffer.candidateName }, false)"
        >
          驳回
        </el-button>
      </template>
    </el-drawer>

    <!-- ====== 审批操作对话框 ====== -->
    <el-dialog
      v-model="approveDialogVisible"
      :title="approveAction ? '审批通过' : '审批驳回'"
      width="480px"
      :close-on-click-modal="false"
    >
      <div class="approve-dialog-content">
        <el-alert
          :title="approveAction ? '确认审批通过该Offer？' : '确认驳回该Offer？'"
          :type="approveAction ? 'success' : 'error'"
          :closable="false"
          show-icon
          style="margin-bottom: 16px;"
        >
          <template #default>
            <span>候选人：<strong>{{ approvingOffer?.candidateName }}</strong></span>
          </template>
        </el-alert>

        <el-form label-position="top">
          <el-form-item :label="approveAction ? '审批意见（选填）' : '驳回原因（必填）'">
            <el-input
              v-model="approveComment"
              type="textarea"
              :rows="3"
              :placeholder="approveAction ? '请输入审批意见...' : '请输入驳回原因，以便HR了解审批意见...'"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button
          :type="approveAction ? 'success' : 'danger'"
          :disabled="!approveAction && !approveComment.trim()"
          :loading="approving"
          @click="doApprove"
        >
          {{ approveAction ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ====== 审批流程配置对话框 ====== -->
    <el-dialog
      v-model="configDialogVisible"
      title="审批流程配置"
      width="1100px"
      top="40px"
      :close-on-click-modal="false"
      @closed="resetConfigForm"
    >
      <div class="config-dialog-body">
        <!-- 左侧：已有流程列表 -->
        <div class="config-left">
          <div class="config-left-header">
            <span class="config-left-title">已配置流程</span>
            <el-button size="small" type="primary" @click="createNewConfig">新增流程</el-button>
          </div>
          <div class="config-list">
            <div
              v-for="cfg in flowConfigList"
              :key="cfg.id"
              class="config-list-item"
              :class="{ selected: editingConfig?.id === cfg.id }"
              @click="selectConfig(cfg)"
            >
              <div class="config-item-main">
                <span class="config-item-name">{{ cfg.flowName }}</span>
                <el-tag :type="cfg.isActive ? 'success' : 'info'" size="small">
                  {{ cfg.isActive ? '启用' : '停用' }}
                </el-tag>
              </div>
              <div class="config-item-dept">{{ cfg.departmentName }} · {{ cfg.maxLevels }}级审批</div>
            </div>
            <el-empty v-if="flowConfigList.length === 0" description="暂无流程配置" :image-size="48" />
          </div>
        </div>

        <!-- 右侧：编辑区 -->
        <div class="config-right">
          <template v-if="editingConfig">
            <el-form label-position="top" size="default" class="config-form">
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-form-item label="流程名称">
                    <el-input v-model="configForm.flowName" placeholder="如：技术研发部审批流程" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="适用部门">
                    <el-tree-select
                      v-model="configForm.departmentName"
                      :data="departmentTree"
                      :props="{ label: 'name', value: 'name', children: 'children' }"
                      placeholder="请选择适用部门"
                      style="width: 100%"
                      filterable
                      check-strictly
                      :render-after-expand="false"
                    />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item>
                <template #label>
                  <div class="form-label-row">
                    <span>审批节点</span>
                    <el-button size="small" type="primary" :icon="Plus" @click="addNode">添加节点</el-button>
                  </div>
                </template>
                <div class="nodes-editor">
                  <div
                    v-for="(node, ni) in configForm.nodes"
                    :key="ni"
                    class="node-card"
                  >
                    <div class="node-card-header">
                      <span class="node-level-badge">第{{ ni + 1 }}级</span>
                      <el-input
                        v-model="node.nodeName"
                        placeholder="节点名称，如：HR经理"
                        size="small"
                        style="width: 160px"
                      />
                      <el-button
                        v-if="configForm.nodes.length > 1"
                        size="small"
                        type="danger"
                        :icon="Delete"
                        circle
                        @click="removeNode(ni)"
                      />
                    </div>
                    <div class="node-approvers">
                      <div class="approvers-label">审批人</div>
                      <div
                        v-for="(approver, ai) in node.approvers"
                        :key="ai"
                        class="approver-row"
                      >
                        <el-select
                          v-model="approver.approverRole"
                          placeholder="选择角色"
                          size="small"
                          style="width: 130px"
                          @change="approver.approverName = ''"
                        >
                          <el-option
                            v-for="r in approverRoleOptions"
                            :key="r"
                            :label="r"
                            :value="r"
                          />
                        </el-select>
                        <el-select
                          v-model="approver.approverName"
                          placeholder="选择审批人"
                          size="small"
                          style="width: 140px"
                          filterable
                          :disabled="!approver.approverRole"
                        >
                          <el-option
                            v-for="u in getApproverUsers(approver.approverRole)"
                            :key="u.value"
                            :label="u.label"
                            :value="u.label"
                          />
                        </el-select>
                        <el-button
                          v-if="node.approvers.length > 1"
                          size="small"
                          :icon="Delete"
                          circle
                          @click="node.approvers.splice(ai, 1)"
                        />
                      </div>
                      <el-button size="small" :icon="Plus" @click="addApprover(node)">
                        添加审批人
                      </el-button>
                    </div>
                  </div>
                </div>
              </el-form-item>

              <el-form-item label="流程说明">
                <el-input
                  v-model="configForm.description"
                  type="textarea"
                  :rows="2"
                  placeholder="选填，如：适用于技术研发部所有Offer审批"
                />
              </el-form-item>
            </el-form>
          </template>
          <el-empty v-else description="请从左侧选择或新增一个流程配置" :image-size="48" />
        </div>
      </div>

      <template #footer v-if="editingConfig">
        <div class="dialog-footer">
          <el-button
            v-if="editingConfig?.id"
            type="danger"
            link
            :icon="Delete"
            @click="deleteCurrentConfig"
          >
            删除此流程
          </el-button>
          <div class="footer-actions">
            <el-button @click="configDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="savingConfig" @click="saveCurrentConfig">保存</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Refresh, Setting, Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPendingApprovals,
  getApprovalHistory,
  getApprovalStats,
  getOfferDetail,
  approve,
  getFlowConfigs,
  createFlowConfig,
  updateFlowConfig,
  deleteFlowConfig,
} from '@/api/offer'
import { getDepartments } from '@/api/system'
import { getCandidateById } from '@/api/candidate'
import { getInterviews, getReport } from '@/api/interview'
import { getResumes } from '@/api/resume'
import type {
  ApprovalListVO, ApprovalStatsVO, OfferVO,
  ApprovalFlowConfigVO, FlowNode, ApproverInfo, CreateFlowConfigRequest, UpdateFlowConfigRequest,
  DepartmentTreeVO, CandidateVO, InterviewVO, InterviewReportVO,
} from '@/types/models'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/format'

const userStore = useUserStore()

// ---- 统计 ----
const stats = reactive<ApprovalStatsVO>({
  pendingCount: 0,
  todayApproved: 0,
  todayRejected: 0,
  monthlyTotal: 0,
})

// ---- 审批流配置 ----
const flowConfigList = ref<ApprovalFlowConfigVO[]>([])
const departmentTree = ref<DepartmentTreeVO[]>([])

async function loadDepartmentOptions() {
  try {
    departmentTree.value = await getDepartments()
  } catch {
    departmentTree.value = []
  }
}

const defaultStages: FlowNode[] = [
  { level: 1, nodeName: '团队负责人', approvers: [] },
  { level: 2, nodeName: '部门负责人', approvers: [] },
  { level: 3, nodeName: 'HR经理', approvers: [] },
]

// ---- 审批角色选项（按企业层级排列） ----
const approverRoleOptions = [
  '团队负责人', '部门负责人', '部门总监',
  'HRBP', 'HR经理', '招聘主管', '薪酬专员',
  '副总裁', '总裁',
]

// ---- 审批人选项（模拟从用户系统获取，按角色过滤） ----
const approverUsersByRole: Record<string, { value: number; label: string }[]> = {
  '团队负责人': [{ value: 1101, label: '邓超' }, { value: 1102, label: '郭峰' }, { value: 1103, label: '杨帆' }],
  '部门负责人': [{ value: 1005, label: '陈明' }, { value: 1006, label: '刘洋' }],
  '部门总监':   [{ value: 1003, label: '李强' }, { value: 1004, label: '赵敏' }],
  'HRBP':       [{ value: 1014, label: '徐娜' }, { value: 1015, label: '胡伟' }],
  'HR经理':     [{ value: 1001, label: '张伟' }, { value: 1002, label: '王芳' }],
  '招聘主管':   [{ value: 1007, label: '周静' }, { value: 1008, label: '孙磊' }],
  '薪酬专员':   [{ value: 1009, label: '吴婷' }, { value: 1010, label: '郑浩' }],
  '副总裁':     [{ value: 1011, label: '钱峰' }, { value: 1012, label: '马丽' }],
  '总裁':       [{ value: 1013, label: '黄磊' }],
}

function getApproverUsers(role: string): { value: number; label: string }[] {
  return approverUsersByRole[role] || []
}

/** 根据部门名获取对应审批流节点，找不到则返回默认流程 */
function getFlowStages(departmentName: string): FlowNode[] {
  const cfg = flowConfigList.value.find(
    c => c.departmentName === departmentName && c.isActive === 1,
  )
  return cfg?.nodes?.length ? cfg.nodes : defaultStages
}

// ---- 待审批列表 ----
const activeTab = ref('pending')
const pendingLoading = ref(false)
const pendingList = ref<ApprovalListVO[]>([])
const pendingPage = ref(1)
const pendingSize = ref(20)
const pendingTotal = ref(0)

const pendingFilters = reactive({
  candidateName: '',
  departmentName: '',
  offerNo: '',
  expectedOnboardDate: '',
})

// ---- 已审核列表 ----
const historyLoading = ref(false)
const historyList = ref<ApprovalListVO[]>([])
const historyPage = ref(1)
const historySize = ref(20)
const historyTotal = ref(0)

const historyFilters = reactive({
  candidateName: '',
  departmentName: '',
  offerNo: '',
  expectedOnboardDate: '',
})

const filteredHistoryList = computed(() => {
  let list = historyList.value
  const f = historyFilters
  if (f.candidateName) {
    const kw = f.candidateName.toLowerCase()
    list = list.filter(r => r.candidateName?.toLowerCase().includes(kw))
  }
  if (f.departmentName) {
    list = list.filter(r => r.departmentName === f.departmentName)
  }
  if (f.offerNo) {
    const kw = f.offerNo.toLowerCase()
    list = list.filter(r => r.offerNo?.toLowerCase().includes(kw))
  }
  if (f.expectedOnboardDate) {
    list = list.filter(r => r.expectedOnboardDate === f.expectedOnboardDate)
  }
  return list
})

// ---- 详情抽屉 ----
const drawerVisible = ref(false)
const detailOffer = ref<OfferVO | null>(null)
const detailCandidate = ref<CandidateVO | null>(null)
const detailInterviews = ref<Array<InterviewVO & { feedback?: string; overallScore?: number }>>([])
const detailResumeId = ref<string>('')

// ---- 审批对话框 ----
const approveDialogVisible = ref(false)
const approveAction = ref(true) // true=通过, false=驳回
const approveComment = ref('')
const approving = ref(false)
const approvingOffer = ref<{ offerId: string | number; candidateName: string; departmentName: string; approvalLevel: number } | null>(null)

// ---- 流程配置对话框 ----
const configDialogVisible = ref(false)
const editingConfig = ref<ApprovalFlowConfigVO | null>(null)
const savingConfig = ref(false)
const configForm = reactive<CreateFlowConfigRequest & { id?: number | string }>({
  flowName: '',
  departmentName: '',
  nodes: [],
  description: '',
})

function makeEmptyNode(level: number): FlowNode {
  return {
    level,
    nodeName: '',
    approvers: [{ approverId: 0, approverName: '', approverRole: '' }],
  }
}

function makeEmptyConfig(): CreateFlowConfigRequest & { id?: number | string } {
  return {
    flowName: '',
    departmentName: '',
    nodes: [makeEmptyNode(1), makeEmptyNode(2)],
    description: '',
  }
}

function resetConfigForm() {
  editingConfig.value = null
  Object.assign(configForm, makeEmptyConfig())
}

async function openConfigDialog() {
  await Promise.all([loadFlowConfigs(), loadDepartmentOptions()])
  resetConfigForm()
  configDialogVisible.value = true
}

async function loadFlowConfigs() {
  try {
    flowConfigList.value = await getFlowConfigs()
  } catch {
    flowConfigList.value = []
  }
}

function selectConfig(cfg: ApprovalFlowConfigVO) {
  editingConfig.value = cfg
  configForm.id = cfg.id
  configForm.flowName = cfg.flowName
  configForm.departmentName = cfg.departmentName
  configForm.nodes = cfg.nodes.map(n => ({
    level: n.level,
    nodeName: n.nodeName,
    approvers: n.approvers.map(a => ({
      approverId: a.approverId,
      approverName: a.approverName,
      approverRole: a.approverRole,
    })),
  }))
  configForm.description = cfg.description || ''
}

function createNewConfig() {
  editingConfig.value = { id: '', flowName: '', departmentName: '', isActive: 1, maxLevels: 2, nodes: [], description: '', createTime: '', updateTime: '' }
  const empty = makeEmptyConfig()
  configForm.id = undefined
  configForm.flowName = empty.flowName
  configForm.departmentName = empty.departmentName
  configForm.nodes = empty.nodes
  configForm.description = empty.description
}

function addNode() {
  const nextLevel = configForm.nodes.length + 1
  configForm.nodes.push(makeEmptyNode(nextLevel))
}

function removeNode(index: number) {
  configForm.nodes.splice(index, 1)
  configForm.nodes.forEach((n, i) => { n.level = i + 1 })
}

function addApprover(node: FlowNode) {
  node.approvers.push({ approverId: 0, approverName: '', approverRole: '' })
}

async function saveCurrentConfig() {
  if (!configForm.flowName.trim()) { ElMessage.warning('请输入流程名称'); return }
  if (!configForm.departmentName.trim()) { ElMessage.warning('请输入适用部门'); return }
  for (const node of configForm.nodes) {
    if (!node.nodeName.trim()) { ElMessage.warning('请填写所有节点名称'); return }
    for (const a of node.approvers) {
      if (!a.approverName.trim()) { ElMessage.warning('请填写所有审批人姓名'); return }
    }
  }

  savingConfig.value = true
  try {
    const payload: UpdateFlowConfigRequest = {
      flowName: configForm.flowName,
      departmentName: configForm.departmentName,
      nodes: configForm.nodes,
      description: configForm.description,
    }

    if (configForm.id) {
      await updateFlowConfig(configForm.id, payload)
      ElMessage.success('流程配置已更新')
    } else {
      await createFlowConfig(payload as CreateFlowConfigRequest)
      ElMessage.success('流程配置已创建')
    }
    await loadFlowConfigs()
    // Keep editing the matched config after save
    const refreshed = flowConfigList.value.find(c => c.departmentName === configForm.departmentName)
    if (refreshed) selectConfig(refreshed)
  } catch {
    ElMessage.error('保存流程配置失败')
  } finally {
    savingConfig.value = false
  }
}

async function deleteCurrentConfig() {
  if (!configForm.id) return
  try {
    await ElMessageBox.confirm(
      `确定要删除「${configForm.flowName}」的审批流程配置吗？删除后不可恢复。`,
      '确认删除',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' },
    )
    await deleteFlowConfig(configForm.id)
    ElMessage.success('流程配置已删除')
    resetConfigForm()
    await loadFlowConfigs()
  } catch {
    // user cancelled or error
  }
}

// ---- 生命周期 ----
onMounted(() => {
  loadStats()
  loadPending()
  loadFlowConfigs()
})

// ---- 统计数据 ----
async function loadStats() {
  try {
    const res = await getApprovalStats()
    stats.pendingCount = res.pendingCount
    stats.todayApproved = res.todayApproved
    stats.todayRejected = res.todayRejected
    stats.monthlyTotal = res.monthlyTotal
  } catch {
    // 静默失败，保持默认值
  }
}

// ---- 待审批列表 ----
async function loadPending() {
  pendingLoading.value = true
  try {
    const params: Record<string, unknown> = {
      page: pendingPage.value,
      size: pendingSize.value,
    }
    if (pendingFilters.candidateName) params.candidateName = pendingFilters.candidateName
    if (pendingFilters.departmentName) params.departmentName = pendingFilters.departmentName
    if (pendingFilters.offerNo) params.offerNo = pendingFilters.offerNo
    if (pendingFilters.expectedOnboardDate) params.expectedOnboardDate = pendingFilters.expectedOnboardDate

    const res = await getPendingApprovals(params)
    pendingList.value = res.records
    pendingTotal.value = res.total
  } catch {
    pendingList.value = []
    pendingTotal.value = 0
  } finally {
    pendingLoading.value = false
  }
}

function handlePendingSearch() {
  pendingPage.value = 1
  loadPending()
}

// ---- 已审核列表 ----
async function loadHistory() {
  historyLoading.value = true
  try {
    const res = await getApprovalHistory({
      page: historyPage.value,
      size: historySize.value,
    })
    historyList.value = res.records
    historyTotal.value = res.total
  } catch {
    historyList.value = []
    historyTotal.value = 0
  } finally {
    historyLoading.value = false
  }
}

function onTabChange(tab: string) {
  if (tab === 'history' && historyList.value.length === 0) {
    loadHistory()
  }
}

// ---- 详情抽屉 ----
async function showDetail(offerId: string | number) {
  try {
    detailOffer.value = await getOfferDetail(String(offerId))
    drawerVisible.value = true
    // 异步加载候选人和面试数据
    if (detailOffer.value?.candidateId) {
      const cid = String(detailOffer.value.candidateId)
      Promise.all([
        getCandidateById(cid).then(c => { detailCandidate.value = c }).catch(() => { detailCandidate.value = null }),
        getResumes({ candidateId: cid, size: 1 }).then(r => {
          detailResumeId.value = r.records?.[0]?.id || ''
        }).catch(() => { detailResumeId.value = '' }),
        getInterviews({ candidateId: cid, size: 50 }).then(async r => {
          // 按轮次正序排列
          const sorted = [...r.records].sort((a, b) => (a.round || 0) - (b.round || 0))
          // 仅已完成面试获取反馈报告，未完成的跳过避免报错提示
          const enriched = await Promise.all(
            sorted.map(async (iv) => {
              if (iv.status === 2) {
                try {
                  const report = await getReport(String(iv.id))
                  return { ...iv, feedback: report.feedback, overallScore: report.overallScore }
                } catch {
                  return { ...iv }
                }
              }
              return { ...iv }
            }),
          )
          detailInterviews.value = enriched
        }).catch(() => { detailInterviews.value = [] }),
      ])
    }
  } catch {
    ElMessage.warning('获取Offer详情失败')
  }
}

// ---- 审批操作 ----
function openApproveDialog(
  row: { offerId: string | number; candidateName: string; departmentName?: string } | ApprovalListVO,
  isApprove: boolean,
) {
  approvingOffer.value = {
    offerId: row.offerId,
    candidateName: row.candidateName,
    departmentName: (row as ApprovalListVO).departmentName || '',
    approvalLevel: (row as ApprovalListVO).currentLevel || 1,
  }
  approveAction.value = isApprove
  approveComment.value = ''
  approveDialogVisible.value = true
}

async function doApprove() {
  if (!approvingOffer.value) return

  if (!approveAction.value && !approveComment.value.trim()) {
    ElMessage.warning('驳回时必须填写审批意见')
    return
  }

  approving.value = true
  try {
    const { departmentName, approvalLevel } = approvingOffer.value
    const stages = getFlowStages(departmentName)
    const currentStage = stages[approvalLevel - 1]
    const approverRole = currentStage?.nodeName || ''

    await approve(String(approvingOffer.value.offerId), {
      approverId: Number(userStore.userInfo?.id) || 0,
      approverName: userStore.userInfo?.realName || userStore.userName || '',
      approverRole,
      status: approveAction.value ? 1 : 2, // 1=通过, 2=驳回
      comment: approveComment.value || undefined,
    })
    ElMessage.success(approveAction.value ? '审批通过' : '已驳回')
    approveDialogVisible.value = false
    loadStats()
    loadPending()
  } catch {
    // 错误已由 HTTP 拦截器统一弹窗提示
  } finally {
    approving.value = false
  }
}

const router = useRouter()

// ---- 工具方法 ----
/** 在新窗口打开候选人简历详情页 */
function openResume(candidateId: string | number) {
  const route = router.resolve({ name: 'ResumeDetail', params: { id: String(candidateId) } })
  window.open(route.href, '_blank')
}

/** 待审批列表的审核进度摘要 */
function getFlowSummary(row: ApprovalListVO): string {
  const stages = getFlowStages(row.departmentName)
  const currentLevel = row.currentLevel || 1
  if (currentLevel > stages.length) {
    return `全部通过 · ${stages.length}级`
  }
  const currentStage = stages[currentLevel - 1]
  return currentStage ? currentStage.nodeName : `第${currentLevel}级 / 共${stages.length}级`
}

/** 已审核列表的审核进度摘要 */
function getHistoryFlowSummary(row: ApprovalListVO): string {
  const stages = getFlowStages(row.departmentName)
  if (row.latestApproval?.status === 2) {
    const rejectLevel = row.currentLevel || row.latestApproval?.approvalLevel || 1
    const stage = stages[rejectLevel - 1]
    const nodeName = stage?.nodeName || `第${rejectLevel}级`
    return `${nodeName} · 已驳回`
  }
  return `全部通过 · ${stages.length}级`
}

/** 已审核列表的审核人 */
function getHistoryApprovers(row: ApprovalListVO): string {
  const stages = getFlowStages(row.departmentName)
  const level = row.currentLevel || row.latestApproval?.approvalLevel || 1
  if (level > stages.length) return '—'
  const stage = stages[level - 1]
  if (!stage?.approvers?.length) return '待分配'
  if (row.latestApproval?.status === 2) {
    return `${row.latestApproval?.approverName || ''}（${row.latestApproval?.approverRole || ''}）`
  }
  return stage.approvers.map(a => a.approverName + (a.approverRole ? `（${a.approverRole}）` : '')).join('、')
}

/** 获取当前节点的审核人信息 */
function getCurrentApprovers(row: ApprovalListVO): string {
  const stages = getFlowStages(row.departmentName)
  const currentLevel = row.currentLevel || 1
  if (currentLevel > stages.length) return '—'
  const currentStage = stages[currentLevel - 1]
  if (!currentStage?.approvers?.length) return '待分配'
  return currentStage.approvers.map(a => a.approverName + (a.approverRole ? `（${a.approverRole}）` : '')).join('、')
}

function formatSalary(val: number | undefined | null): string {
  if (val == null) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function formatPackage(val: number | undefined | null): string {
  if (val == null) return '-'
  return (Number(val) / 10000).toFixed(1)
}

function statusLabel(status: number | undefined): string {
  const map: Record<number, string> = {
    0: '草稿', 1: '待审批', 2: '已审批', 3: '已发送',
    4: '已接受', 5: '已拒绝', 6: '洽谈中', 7: '已过期',
  }
  return map[status ?? -1] || '未知'
}

function statusTagType(status: number | undefined): 'info' | 'warning' | 'success' | 'danger' | '' {
  const map: Record<number, 'info' | 'warning' | 'success' | 'danger' | ''> = {
    0: 'info', 1: 'warning', 2: '', 3: '', 4: 'success', 5: 'danger', 6: 'warning', 7: 'info',
  }
  return map[status ?? -1] || 'info'
}

const eduMap: Record<number, string> = {
  0: '高中', 1: '大专', 2: '本科', 3: '硕士', 4: '博士',
}
function educationLabel(edu: number | undefined): string {
  return edu != null && eduMap[edu] ? eduMap[edu] : '-'
}
</script>

<style scoped>
.approval-page {
  padding: 0;
}

.stats-row {
  margin-bottom: 4px;
}

.stat-card {
  padding: 20px 16px;
  border-radius: 8px;
  text-align: center;
}

.stat-pending {
  background: linear-gradient(135deg, #fff7ed, #ffedd5);
  border: 1px solid #fed7aa;
}

.stat-approved {
  background: linear-gradient(135deg, #f0fdf4, #dcfce7);
  border: 1px solid #bbf7d0;
}

.stat-rejected {
  background: linear-gradient(135deg, #fef2f2, #fee2e2);
  border: 1px solid #fecaca;
}

.stat-monthly {
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
  border: 1px solid #bfdbfe;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-pending .stat-value { color: #ea580c; }
.stat-approved .stat-value { color: #16a34a; }
.stat-rejected .stat-value { color: #dc2626; }
.stat-monthly .stat-value { color: #2563eb; }

.stat-label {
  font-size: 13px;
  color: var(--c-text-secondary);
  margin-top: 4px;
}

.comment-text {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Detail drawer */
.detail-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
  margin: 0 0 10px 0;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.detail-label {
  font-size: 12px;
  color: var(--c-text-secondary);
}

.detail-value {
  font-size: 13px;
  color: var(--c-text);
}

/* Approval flow mini indicator */
.approval-flow {
  display: flex;
  align-items: center;
  gap: 0;
}

.approval-stage {
  display: flex;
  align-items: center;
  gap: 4px;
  position: relative;
}

.approval-stage + .approval-stage {
  margin-left: 4px;
  padding-left: 14px;
}

.approval-stage + .approval-stage::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 10px;
  height: 1.5px;
  background: #d9d9d9;
  border-radius: 1px;
}

.approval-stage.active + .approval-stage::before {
  background: var(--c-primary, #409eff);
}

.stage-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  border: 1.5px solid #d9d9d9;
  background: #fff;
  flex-shrink: 0;
}

.approval-stage.active .stage-dot {
  background: var(--c-primary, #409eff);
  border-color: var(--c-primary, #409eff);
}

.approval-stage.current .stage-dot {
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.2);
}

.stage-label {
  font-size: 11px;
  color: #bfbfbf;
  white-space: nowrap;
}

.approval-stage.active .stage-label {
  color: var(--c-text, #303133);
}

.approval-stage.current .stage-label {
  color: var(--c-primary, #409eff);
  font-weight: 600;
}

/* Flow popover styles */
.flow-summary {
  cursor: pointer;
  color: var(--c-primary, #409eff);
  font-size: 13px;
  border-bottom: 1px dashed var(--c-primary, #409eff);
}
.flow-summary:hover {
  color: #337ecc;
}

.flow-popover {
  display: flex;
  flex-direction: column;
}

.flow-popover-stage {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 4px;
  position: relative;
}

.flow-popover-stage + .flow-popover-stage {
  border-top: 1px solid #f0f0f0;
}

.flow-popover-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid #d9d9d9;
  background: #fff;
  flex-shrink: 0;
}

.flow-popover-stage.active .flow-popover-dot {
  background: var(--c-primary, #409eff);
  border-color: var(--c-primary, #409eff);
}

.flow-popover-stage.current .flow-popover-dot {
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.2);
}

.flow-popover-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.flow-popover-label {
  font-size: 13px;
  color: #bfbfbf;
}

.flow-popover-stage.active .flow-popover-label {
  color: var(--c-text, #303133);
}

.flow-popover-stage.current .flow-popover-label {
  color: var(--c-primary, #409eff);
  font-weight: 600;
}

.flow-popover-approvers {
  font-size: 11px;
  color: var(--c-text-secondary);
  line-height: 1.4;
}

.dialog-footer {
  width: 100%;
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.resume-link {
  color: var(--c-primary, #409eff);
  text-decoration: none;
}
.resume-link:hover {
  text-decoration: underline;
}

.interview-card {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 8px;
}
.interview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.interview-round {
  font-size: 13px;
  font-weight: 600;
}
.interview-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--c-text-secondary);
}
.interview-meta strong {
  color: var(--c-text);
}
.interview-feedback {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e8e8e8;
  font-size: 12px;
  color: var(--c-text-secondary);
  line-height: 1.6;
}
.feedback-label {
  font-weight: 500;
  color: var(--c-text);
}

.approval-history-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.approval-history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.approval-history-approver {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
}

.approval-history-name {
  font-size: 12px;
  color: var(--c-text-secondary);
}

.approval-history-level {
  font-size: 12px;
  color: var(--c-text-secondary);
}

.approval-history-comment {
  font-size: 13px;
  color: var(--c-text-secondary);
  line-height: 1.5;
  padding: 6px 10px;
  background: #f8f8f8;
  border-radius: 4px;
  margin-top: 2px;
}

/* Page header row */
.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

/* Flow config dialog */
.config-dialog-body {
  display: flex;
  gap: 16px;
  min-height: 400px;
}

.config-left {
  width: 260px;
  flex-shrink: 0;
  border-right: 1px solid #e8e8e8;
  padding-right: 16px;
}

.config-left-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.config-left-title {
  font-size: 14px;
  font-weight: 600;
}

.config-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.config-list-item {
  padding: 10px 12px;
  border-radius: 6px;
  border: 1px solid #e8e8e8;
  cursor: pointer;
  transition: all 0.15s;
}

.config-list-item:hover {
  border-color: var(--c-primary, #409eff);
}

.config-list-item.selected {
  border-color: var(--c-primary, #409eff);
  background: rgba(64, 158, 255, 0.06);
}

.config-item-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.config-item-name {
  font-size: 13px;
  font-weight: 500;
}

.config-item-dept {
  font-size: 12px;
  color: var(--c-text-secondary);
  margin-top: 4px;
}

.config-right {
  flex: 1;
  padding-left: 4px;
}

.config-form {
  padding: 0;
}

.form-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.nodes-editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.node-card {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 12px;
  background: #fafafa;
}

.node-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.node-level-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  height: 24px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: var(--c-primary, #409eff);
  border-radius: 12px;
  padding: 0 8px;
}

.node-approvers {
  padding-left: 4px;
}

.approvers-label {
  font-size: 12px;
  color: var(--c-text-secondary);
  margin-bottom: 6px;
}

.approver-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}
</style>
