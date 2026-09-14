<template>
  <div class="jd-page" v-loading="loading">
    <!-- Error State -->
    <div v-if="error" class="jd-state-wrap">
      <div class="jd-state-card">
        <div class="jd-state-icon is-error">
          <svg width="56" height="56" viewBox="0 0 56 56" fill="none"><circle cx="28" cy="28" r="24" stroke="#f56c6c" stroke-width="2" fill="#fef0f0"/><path d="M28 16v16M28 40v0" stroke="#f56c6c" stroke-width="2.5" stroke-linecap="round"/></svg>
        </div>
        <p class="jd-state-title">加载失败</p>
        <p class="jd-state-desc">{{ error }}</p>
        <el-button type="primary" size="large" @click="fetchDetail">重新加载</el-button>
      </div>
    </div>

    <!-- Not Found -->
    <div v-else-if="!loading && !detail" class="jd-state-wrap">
      <div class="jd-state-card">
        <el-empty description="职位信息不存在或已下架" :image-size="100" />
      </div>
    </div>

    <!-- Program Disabled -->
    <div v-else-if="detail?.program?.status === 0" class="jd-state-wrap">
      <div class="jd-state-card">
        <div class="jd-state-icon is-warn">
          <svg width="56" height="56" viewBox="0 0 56 56" fill="none"><circle cx="28" cy="28" r="24" stroke="#e6a23c" stroke-width="2" fill="#fdf6ec"/><path d="M28 16v16M28 40v0" stroke="#e6a23c" stroke-width="2.5" stroke-linecap="round"/></svg>
        </div>
        <p class="jd-state-title">该职位所属的内推计划已停用</p>
        <p class="jd-state-desc">此职位暂时无法接受内推推荐，请联系管理员了解更多信息</p>
      </div>
    </div>

    <!-- ====== MAIN CONTENT ====== -->
    <div v-else-if="detail" class="jd-main">
      <!-- HEADER -->
      <header class="jd-header">
        <div class="jd-header-inner">
          <a href="/" class="jd-header-brand" title="SmartRecruit 智能招聘">
            <svg class="jd-logo" width="28" height="28" viewBox="0 0 28 28" fill="none">
              <rect width="28" height="28" rx="6" fill="#1677ff"/>
              <path d="M8 20V8l6 8.5L20 8v12" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
            </svg>
            <span class="jd-brand-text">SmartRecruit</span>
          </a>
          <div class="jd-header-right">
            <a v-if="shareTokenVal" :href="`/p/${shareTokenVal}`" class="jd-header-btn">投递简历</a>
            <template v-if="userStore.isLoggedIn">
              <span v-if="maskedPhone" class="jd-user-phone">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" class="jd-user-icon">
                  <rect x="2" y="1" width="12" height="14" rx="2" stroke="currentColor" stroke-width="1.3"/>
                  <circle cx="8" cy="10.5" r="1.5" fill="currentColor"/>
                </svg>
                {{ maskedPhone }}
              </span>
              <a :href="`/my-applications?ref=${backRef}`" class="jd-header-btn jd-header-btn--apply">我的投递</a>
              <button class="jd-logout-btn" @click="handleLogout">退出</button>
            </template>
          </div>
        </div>
      </header>

      <!-- HERO AREA -->
      <div class="jd-hero-wrapper">
        <div class="jd-hero">
          <!-- Left: Title & Meta -->
          <div class="jd-hero-main">
            <div class="jd-hero-eyebrow">
              <span class="jd-eyebrow-tag" :class="program?.status === 1 ? 'is-open' : 'is-closed'">
                <span class="jd-eyebrow-dot"></span>
                {{ program?.status === 1 ? '正在招聘' : '已停用' }}
              </span>
              <span class="jd-eyebrow-sep">·</span>
              <span class="jd-eyebrow-program">{{ program?.title || '内推计划' }}</span>
            </div>

            <h1 class="jd-hero-title">{{ detail.programJob?.jobTitle || detail.jobPosition?.title || '职位详情' }}</h1>

            <div class="jd-hero-tags">
              <span v-if="detail.jobPosition?.departmentName" class="jd-tag jd-tag--info">
                <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><rect x="1" y="1.5" width="10" height="9" rx="1.5" stroke="currentColor" stroke-width="1.2"/><line x1="1" y1="4.5" x2="11" y2="4.5" stroke="currentColor" stroke-width="1.2"/><line x1="4.5" y1="4.5" x2="4.5" y2="10.5" stroke="currentColor" stroke-width="1.2"/></svg>
                {{ detail.jobPosition.departmentName }}
              </span>
              <span v-if="positionTypeLabel" class="jd-tag jd-tag--info">
                <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><rect x="2" y="1" width="8" height="10" rx="1.5" stroke="currentColor" stroke-width="1.2"/><line x1="4" y1="4" x2="8" y2="4" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/><line x1="4" y1="6.5" x2="8" y2="6.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/><line x1="4" y1="9" x2="6.5" y2="9" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
                {{ positionTypeLabel }}
              </span>
              <span v-if="detail.programJob?.headCount != null" class="jd-tag jd-tag--highlight">
                招{{ detail.programJob.headCount }}人
              </span>
              <span v-if="detail.programJob?.tag === 0" class="jd-tag jd-tag--urgent">急聘</span>
              <span v-else-if="detail.programJob?.tag === 1" class="jd-tag jd-tag--hot">高额奖金</span>
            </div>

            <!-- Referral Context Bar -->
            <div v-if="referralContext?.referralCode" class="jd-ref-ctx-bar">
              <span class="jd-ref-ctx-icon">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="2" y="1" width="12" height="14" rx="2" stroke="currentColor" stroke-width="1.3"/><path d="M5.5 5.5h5M5.5 8.5h3.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
              </span>
              <span class="jd-ref-ctx-label">内推码</span>
              <span class="jd-ref-ctx-code">{{ referralContext.referralCode }}</span>
              <span v-if="referralContext.referrerName" class="jd-ref-ctx-referrer">
                推荐人：{{ referralContext.referrerName }}
              </span>
            </div>

            <div class="jd-hero-meta-strip">
              <div class="jd-meta-item">
                <div class="jd-meta-icon">
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="1.5" y="3.5" width="13" height="9" rx="1.5" stroke="currentColor" stroke-width="1.3"/><circle cx="8" cy="8" r="2.2" stroke="currentColor" stroke-width="1.3"/><path d="M1 6h14" stroke="currentColor" stroke-width="1.3"/></svg>
                </div>
                <div class="jd-meta-body">
                  <span class="jd-meta-label">薪资范围</span>
                  <span class="jd-meta-value jd-meta-value--salary">{{ salaryText || '面议' }}</span>
                </div>
              </div>
              <div class="jd-meta-item">
                <div class="jd-meta-icon">
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M8 1.5C5.5 1.5 3.5 3.5 3.5 6c0 3.5 4.5 8.5 4.5 8.5s4.5-5 4.5-8.5c0-2.5-2-4.5-4.5-4.5zm0 6c-.8 0-1.5-.7-1.5-1.5S7.2 4.5 8 4.5s1.5.7 1.5 1.5S8.8 7.5 8 7.5z" fill="currentColor"/></svg>
                </div>
                <div class="jd-meta-body">
                  <span class="jd-meta-label">工作地点</span>
                  <span class="jd-meta-value">{{ detail.jobPosition?.location || '-' }}</span>
                </div>
              </div>
              <div class="jd-meta-item">
                <div class="jd-meta-icon">
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.3"/><path d="M8 4.5V8l2.5 2.5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>
                </div>
                <div class="jd-meta-body">
                  <span class="jd-meta-label">经验要求</span>
                  <span class="jd-meta-value">{{ experienceLabel || '-' }}</span>
                </div>
              </div>
              <div class="jd-meta-item">
                <div class="jd-meta-icon">
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M2 4.5L8 1l6 3.5v7L8 15l-6-3.5v-7z" stroke="currentColor" stroke-width="1.3"/><path d="M2 4.5L8 8l6-3.5M8 8v7" stroke="currentColor" stroke-width="1.3"/></svg>
                </div>
                <div class="jd-meta-body">
                  <span class="jd-meta-label">学历要求</span>
                  <span class="jd-meta-value">{{ educationLabel || '-' }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Right: Quick CTA -->
          <div class="jd-hero-action">
            <a href="#apply" class="jd-hero-cta">
              <span>立即投递</span>
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M3 8h10M9 4l4 4-4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </a>
          </div>
        </div>
      </div>

      <!-- CONTENT BODY: Two columns -->
      <div class="jd-body">
        <!-- LEFT: Content Sections -->
        <div class="jd-content">
          <!-- Responsibilities -->
          <section v-if="responsibilitiesList.length" class="jd-card">
            <div class="jd-card-hd">
              <span class="jd-card-icon" style="background:#e6f4ff;color:#1677ff">
                <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><rect x="1" y="2" width="16" height="13" rx="2" stroke="currentColor" stroke-width="1.4"/><line x1="5" y1="8" x2="13" y2="8" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/><line x1="5" y1="11" x2="10" y2="11" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
              </span>
              <h2 class="jd-card-title">岗位职责</h2>
            </div>
            <ul class="jd-bullet-list">
              <li v-for="(item, idx) in responsibilitiesList" :key="idx">
                <span class="jd-bullet-num">{{ String(idx + 1).padStart(2, '0') }}</span>
                <span>{{ item }}</span>
              </li>
            </ul>
          </section>

          <!-- Requirements -->
          <section v-if="requirementsList.length" class="jd-card">
            <div class="jd-card-hd">
              <span class="jd-card-icon" style="background:#f0f5ff;color:#2f54eb">
                <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><circle cx="9" cy="9" r="7" stroke="currentColor" stroke-width="1.4"/><path d="M9 5v4l2.5 2" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
              </span>
              <h2 class="jd-card-title">任职要求</h2>
            </div>
            <ul class="jd-bullet-list">
              <li v-for="(item, idx) in requirementsList" :key="idx">
                <span class="jd-bullet-num">{{ String(idx + 1).padStart(2, '0') }}</span>
                <span>{{ item }}</span>
              </li>
            </ul>
          </section>

          <!-- What We Offer -->
          <section v-if="offerItems.length" class="jd-card">
            <div class="jd-card-hd">
              <span class="jd-card-icon" style="background:#f6ffed;color:#52c41a">
                <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M3 10l4 4 8-8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </span>
              <h2 class="jd-card-title">我们提供</h2>
            </div>
            <div class="jd-offer-grid">
              <div v-for="(item, idx) in offerItems" :key="idx" class="jd-offer-item">
                <span class="jd-offer-dot"></span>
                <span>{{ item }}</span>
              </div>
            </div>
          </section>

          <!-- Skills -->
          <section v-if="skillsList.length" class="jd-card">
            <div class="jd-card-hd">
              <span class="jd-card-icon" style="background:#fff7e6;color:#fa8c16">
                <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M2 2l4 4-4 4M16 2l-4 4 4 4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </span>
              <h2 class="jd-card-title">技能要求</h2>
            </div>
            <div class="jd-skills-wrap">
              <span v-for="(skill, idx) in skillsList" :key="idx" class="jd-skill-chip">{{ skill }}</span>
            </div>
          </section>

          <!-- SELF-APPLY FORM -->
          <section id="apply" class="jd-card jd-form-card">
            <div class="jd-card-hd">
              <span class="jd-card-icon" style="background:#e6f4ff;color:#1677ff">
                <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><circle cx="9" cy="7" r="3" stroke="currentColor" stroke-width="1.4"/><path d="M3 15c0-3.3 2.7-6 6-6s6 2.7 6 6" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
              </span>
              <h2 class="jd-card-title">投递简历</h2>
              <span class="jd-card-sub">上传简历，一键投递</span>
            </div>

            <!-- Login gate: not logged in -->
            <div v-if="!userStore.isLoggedIn" class="jd-login-gate">
              <div class="jd-login-gate-icon">
                <svg width="56" height="56" viewBox="0 0 56 56" fill="none"><circle cx="28" cy="28" r="24" stroke="#1677ff" stroke-width="2" fill="#e6f4ff"/><rect x="19" y="23" width="18" height="14" rx="2" stroke="#1677ff" stroke-width="2"/><circle cx="28" cy="19" r="4" stroke="#1677ff" stroke-width="2"/></svg>
              </div>
              <p class="jd-login-gate-text">登录后投递简历</p>
              <p class="jd-login-gate-desc">请先登录，投递信息将自动关联您的内推码</p>
              <el-button type="primary" size="large" @click="goToLogin" round>
                去登录
              </el-button>
            </div>

            <!-- Logged in: already applied -->
            <div v-else-if="alreadyApplied" class="jd-success">
              <div class="jd-success-icon">
                <svg width="72" height="72" viewBox="0 0 72 72" fill="none"><circle cx="36" cy="36" r="34" stroke="#1677ff" stroke-width="2.5" fill="#e6f4ff"/><path d="M22 36l10 10 18-20" stroke="#1677ff" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </div>
              <h3 class="jd-success-title">已投递</h3>
              <p class="jd-success-desc">您已成功投递该职位<br/>HR 团队将尽快处理，感谢您的投递</p>
            </div>

            <!-- Logged in: success -->
            <div v-else-if="submitted" class="jd-success">
              <div class="jd-success-icon">
                <svg width="72" height="72" viewBox="0 0 72 72" fill="none"><circle cx="36" cy="36" r="34" stroke="#52c41a" stroke-width="2.5" fill="#f6ffed"/><path d="M22 36l10 10 18-20" stroke="#52c41a" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </div>
              <h3 class="jd-success-title">投递成功!</h3>
              <p class="jd-success-desc">您的简历已成功投递<br/>HR 团队将尽快处理，感谢您的投递</p>
            </div>

            <!-- Logged in: form -->
            <el-form
              v-else
              ref="formRef"
              :model="form"
              :rules="formRules"
              @submit.prevent
              class="jd-form"
              size="large"
              label-position="top"
            >
              <el-form-item class="jd-form-item--readonly">
                <template #label><span class="jd-form-label">目标职位</span></template>
                <div class="jd-readonly-field">
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="1.5" y="1.5" width="13" height="13" rx="2" stroke="#1677ff" stroke-width="1.2"/><path d="M5 8l2 2 4-4" stroke="#1677ff" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                  {{ detail.programJob?.jobTitle || detail.jobPosition?.title || '' }}
                </div>
              </el-form-item>

              <el-form-item v-if="detail.jobPosition?.departmentName" class="jd-form-item--readonly">
                <template #label><span class="jd-form-label">所在部门</span></template>
                <div class="jd-readonly-field">
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="2" y="1.5" width="12" height="13" rx="1.5" stroke="#1677ff" stroke-width="1.2"/><line x1="2" y1="5" x2="14" y2="5" stroke="#1677ff" stroke-width="1.2"/><line x1="5.5" y1="5" x2="5.5" y2="14.5" stroke="#1677ff" stroke-width="1.2"/></svg>
                  {{ detail.jobPosition.departmentName }}
                </div>
              </el-form-item>

              <div class="jd-form-row">
                <el-form-item prop="candidateName">
                  <template #label><span class="jd-form-label">姓名</span></template>
                  <el-input v-model="form.candidateName" placeholder="请输入姓名" />
                </el-form-item>
                <el-form-item prop="candidatePhone">
                  <template #label><span class="jd-form-label">手机号</span></template>
                  <el-input v-model="form.candidatePhone" placeholder="请输入手机号" />
                </el-form-item>
              </div>

              <div class="jd-form-row">
                <el-form-item prop="candidateEmail">
                  <template #label><span class="jd-form-label">邮箱</span></template>
                  <el-input v-model="form.candidateEmail" placeholder="请输入邮箱" />
                </el-form-item>
                <el-form-item>
                  <template #label><span class="jd-form-label">投递附言 <em class="jd-optional">选填</em></span></template>
                  <el-input v-model="form.referralNote" type="textarea" :rows="2" placeholder="补充说明投递意向" />
                </el-form-item>
              </div>

              <div class="jd-form-row">
                <el-form-item class="jd-form-col-full" prop="resumeUrl">
                  <template #label><span class="jd-form-label">简历 <em class="jd-required">必填</em></span></template>

                  <!-- Upload zone (hidden after upload) -->
                  <el-upload
                    v-if="!form.resumeUrl"
                    :auto-upload="false"
                    :limit="1"
                    :show-file-list="false"
                    :on-change="handleResumeChange"
                    accept=".pdf,.doc,.docx"
                    drag
                    class="jd-resume-upload"
                  >
                    <div class="jd-upload-placeholder" :class="{ 'is-uploading': uploadingResume }">
                      <el-icon v-if="uploadingResume" :size="32" class="jd-upload-spinner"><Loading /></el-icon>
                      <el-icon v-else :size="28" color="#1677ff"><UploadFilled /></el-icon>
                      <p v-if="uploadingResume">简历上传中，请稍候...</p>
                      <p v-else>点击或拖拽上传简历</p>
                      <span v-if="!uploadingResume">支持 PDF、DOC、DOCX 格式</span>
                    </div>
                  </el-upload>

                  <!-- Uploaded file card (shown after success) -->
                  <div v-else class="jd-file-card">
                    <div class="jd-file-info">
                      <el-icon :size="22" color="#1677ff"><Document /></el-icon>
                      <a class="jd-file-name" :href="form.resumeUrl" target="_blank" :title="uploadedFileName">
                        {{ uploadedFileName }}
                      </a>
                    </div>
                    <el-button text type="danger" :icon="Delete" @click="handleResumeRemove">删除</el-button>
                  </div>
                </el-form-item>
              </div>

              <!-- 重复投递提示 -->
              <div v-if="duplicateTip" class="jd-duplicate-alert">
                <div class="jd-duplicate-alert-icon">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="#faad14" stroke-width="2"/><path d="M12 7v6m0 3v-2" stroke="#faad14" stroke-width="2" stroke-linecap="round"/></svg>
                </div>
                <div class="jd-duplicate-alert-body">
                  <span class="jd-duplicate-alert-title">{{ duplicateTip }}</span>
                  <span class="jd-duplicate-alert-desc">您可以在「我的投递」中查看投递进度</span>
                </div>
              </div>

              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  class="jd-submit-btn"
                  :loading="submitting"
                  @click="handleSubmit"
                  round
                >
                  投递简历
                </el-button>
              </el-form-item>
            </el-form>
          </section>
        </div>

        <!-- RIGHT: Sidebar -->
        <aside class="jd-sidebar">
          <div class="jd-sidebar-inner">
            <!-- Quick Info Card -->
            <div class="jd-card jd-sidebar-card">
              <h3 class="jd-sidebar-card-title">职位概览</h3>
              <dl class="jd-info-list">
                <div class="jd-info-row">
                  <dt>薪资范围</dt>
                  <dd>{{ salaryText || '面议' }}</dd>
                </div>
                <div class="jd-info-row">
                  <dt>工作地点</dt>
                  <dd>{{ detail.jobPosition?.location || '-' }}</dd>
                </div>
                <div class="jd-info-row">
                  <dt>经验要求</dt>
                  <dd>{{ experienceLabel || '-' }}</dd>
                </div>
                <div class="jd-info-row">
                  <dt>学历要求</dt>
                  <dd>{{ educationLabel || '-' }}</dd>
                </div>
                <div class="jd-info-row">
                  <dt>职位类型</dt>
                  <dd>{{ positionTypeLabel || '-' }}</dd>
                </div>
                <div class="jd-info-row" v-if="detail.jobPosition?.departmentName">
                  <dt>所属部门</dt>
                  <dd>{{ detail.jobPosition.departmentName }}</dd>
                </div>
                <div class="jd-info-row" v-if="detail.programJob?.headCount != null">
                  <dt>招聘人数</dt>
                  <dd><span class="jd-hc-badge">招{{ detail.programJob.headCount }}人</span></dd>
                </div>
              </dl>
            </div>

            <!-- Program Info Card -->
            <div class="jd-card jd-sidebar-card">
              <h3 class="jd-sidebar-card-title">所属计划</h3>
              <dl class="jd-info-list">
                <div class="jd-info-row">
                  <dt>计划名称</dt>
                  <dd>{{ program?.title || '-' }}</dd>
                </div>
                <div class="jd-info-row" v-if="program?.startDate">
                  <dt>计划周期</dt>
                  <dd>{{ program.startDate }}{{ program.endDate ? ' 至 ' + program.endDate : ' 起长期有效' }}</dd>
                </div>
                <div class="jd-info-row" v-if="program?.createBy">
                  <dt>创建人</dt>
                  <dd>{{ program.createBy }}</dd>
                </div>
              </dl>
            </div>

            <!-- CTA -->
            <a href="#apply" class="jd-sidebar-cta">
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><circle cx="9" cy="7" r="3" stroke="currentColor" stroke-width="1.5"/><path d="M3 15c0-3.3 2.7-6 6-6s6 2.7 6 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              投递简历
            </a>
          </div>
        </aside>
      </div>

      <!-- FOOTER -->
      <footer class="jd-footer">
        <div class="jd-footer-inner">
          <div class="jd-footer-brand">
            <svg width="20" height="20" viewBox="0 0 32 32" fill="none">
              <rect width="32" height="32" rx="7" fill="#1677ff"/>
              <path d="M9 23V9l7 9.5L23 9v14" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
            </svg>
            <span>SmartRecruit 智能招聘</span>
          </div>
          <span class="jd-footer-text">Powered by SmartRecruit</span>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import request from '@/api/request'
import { ElMessage, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import { UploadFilled, Loading, Document, Delete } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ---- State ----
const loading = ref(true)
const error = ref<string | null>(null)
const detail = ref<Record<string, any> | null>(null)
const referralContext = ref<{ referralCode?: string; referrerName?: string } | null>(null)
const shareTokenVal = computed(() => (route.params.token as string) || undefined)
const backRef = computed(() => {
  const token = shareTokenVal.value
  if (token) return `/p/${token}/job/${route.params.programJobId}`
  return `/p/job/${route.params.programJobId}`
})

const maskedPhone = computed(() => {
  const mobile = userStore.userInfo?.mobile
  if (!mobile || mobile.length !== 11) return null
  return mobile.slice(0, 3) + '****' + mobile.slice(7)
})

const program = computed(() => detail.value?.program as Record<string, any> | null)
const programJob = computed(() => detail.value?.programJob as Record<string, any> | null)

// ---- Computed Labels ----
const positionTypeLabel = computed(() => {
  const map: Record<number, string> = { 1: '全职', 2: '兼职', 3: '实习', 4: '外包' }
  return map[detail.value?.jobPosition?.positionType as number] || ''
})

const experienceLabel = computed(() => {
  const map: Record<number, string> = { 1: '应届生', 2: '1-3年', 3: '3-5年', 4: '5-10年', 5: '10年以上' }
  return map[detail.value?.jobPosition?.experienceLevel as number] || ''
})

const educationLabel = computed(() => {
  const map: Record<number, string> = { 1: '大专', 2: '本科', 3: '硕士', 4: '博士', 5: '不限' }
  return map[detail.value?.jobPosition?.educationLevel as number] || ''
})

const salaryText = computed(() => {
  const jp = detail.value?.jobPosition as Record<string, any> | undefined
  const min = jp?.minSalary as number | undefined
  const max = jp?.maxSalary as number | undefined
  if (!min && !max) return ''
  if (min && max) return `¥${(min / 1000).toFixed(0)}K - ¥${(max / 1000).toFixed(0)}K`
  if (min) return `¥${(min / 1000).toFixed(0)}K 起`
  return `最高 ¥${(max! / 1000).toFixed(0)}K`
})

// ---- Content Parsing ----
const responsibilitiesList = computed(() => parseJsonArray(detail.value?.jobPosition?.responsibilities))
const requirementsList = computed(() => parseJsonArray(detail.value?.jobPosition?.requirements))
const skillsList = computed(() => parseJsonArray(detail.value?.jobPosition?.skills))

const offerItems = computed(() => {
  const raw = detail.value?.jobPosition?.description as string
  if (!raw) return []
  return extractMarkdownSection(raw, '我们提供')
})

// ---- Referral Form ----
const submitted = ref(false)
const alreadyApplied = ref(false)
const submitting = ref(false)
const duplicateTip = ref('')
const formRef = ref<FormInstance>()

interface FormData {
  candidateName: string
  candidatePhone: string
  candidateEmail: string
  referralNote: string
  resumeUrl: string
}

const form = reactive<FormData>({
  candidateName: '',
  candidatePhone: '',
  candidateEmail: '',
  referralNote: '',
  resumeUrl: '',
})

const formRules: FormRules = {
  candidateName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  candidatePhone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  candidateEmail: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' },
  ],
  resumeUrl: [{ required: true, message: '请上传简历', trigger: 'change' }],
}

// ---- Resume Upload ----
const uploadingResume = ref(false)
const uploadedFileName = ref('')

async function handleResumeChange(file: UploadFile) {
  uploadingResume.value = true
  try {
    const formData = new FormData()
    formData.append('file', file.raw as File)
    const res = await axios.post('/api/v1/referrals/public/upload-resume', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    if (res.data?.code === 0 && res.data?.data?.url) {
      form.resumeUrl = res.data.data.url
      uploadedFileName.value = file.name
      formRef.value?.clearValidate('resumeUrl')
    } else {
      ElMessage.error(res.data?.message || '简历上传失败')
    }
  } catch {
    ElMessage.error('简历上传失败，请稍后重试')
  } finally {
    uploadingResume.value = false
  }
}

function handleResumeRemove() {
  form.resumeUrl = ''
  uploadedFileName.value = ''
}

// ---- API ----
async function fetchDetail() {
  const programJobId = route.params.programJobId as string
  if (!programJobId) {
    error.value = '缺少职位参数'
    loading.value = false
    return
  }

  loading.value = true
  error.value = null
  detail.value = null

  try {
    const res = await axios.get(`/api/v1/referrals/public/positions/${programJobId}/detail`)
    if (res.data?.code === 0 && res.data?.data) {
      detail.value = res.data.data
    } else {
      error.value = res.data?.message || '获取职位详情失败'
    }
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : '网络请求失败'
  } finally {
    loading.value = false
  }
}

async function fetchReferralContext() {
  const token = shareTokenVal.value
  if (!token) return
  try {
    const res = await axios.get(`/api/v1/referrals/public/programs/${token}`)
    if (res.data?.code === 0 && res.data?.data) {
      referralContext.value = {
        referralCode: res.data.data.referralCode,
        referrerName: res.data.data.referrerName,
      }
    }
  } catch {
    // Context is optional; silently ignore
  }
}

async function checkAlreadyApplied() {
  if (!userStore.isLoggedIn || !detail.value) return
  const programJobId = route.params.programJobId as string
  if (!programJobId) return
  try {
    const data = await request.get('/referrals/public/check-applied', {
      params: { programJobId: Number(programJobId) },
    })
    alreadyApplied.value = !!data
  } catch {
    // Silently ignore; treat as not applied
  }
}

function goToLogin() {
  router.push({ name: 'PhoneLogin', query: { redirect: route.fullPath } })
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'ReferralJobLandingWithToken', params: { ...route.params }, query: route.query, replace: true })
}

async function handleSubmit() {
  if (!formRef.value || !detail.value) return

  duplicateTip.value = ''

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const pj = programJob.value
    const pg = program.value
    const jp = detail.value.jobPosition as Record<string, any> | null
    if (!pg || !pj) return
    await request.post('/referrals/public/submit', {
      programId: (pg as Record<string, any>).id,
      programJobId: Number((pj as Record<string, any>).id),
      candidateName: form.candidateName,
      candidatePhone: form.candidatePhone,
      candidateEmail: form.candidateEmail,
      jobId: jp?.id ? Number(jp.id) : 0,
      jobTitle: (pj as Record<string, any>).jobTitle || jp?.title || undefined,
      referralNote: form.referralNote || undefined,
      resumeUrl: form.resumeUrl || undefined,
      shareToken: shareTokenVal.value,
    })
    submitted.value = true
    window.scrollTo({ top: document.getElementById('apply')!.offsetTop - 80, behavior: 'smooth' })
  } catch (err: any) {
    const msg = err?.message || ''
    if (msg.includes('已投递') || msg.includes('已经投递')) {
      duplicateTip.value = msg
    }
    // 其他错误由请求拦截器统一展示
  } finally {
    submitting.value = false
  }
}

// ---- Helpers ----
function parseJsonArray(raw: string | undefined): string[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) return parsed.map(String)
  } catch { /* not JSON */ }
  return []
}

function extractMarkdownSection(md: string, sectionTitle: string): string[] {
  const lines = md.split('\n')
  let inSection = false
  const items: string[] = []
  for (const line of lines) {
    const hMatch = line.match(/^##\s+(.+)/)
    if (hMatch) {
      inSection = hMatch[1].trim() === sectionTitle
      continue
    }
    if (!inSection) continue
    const itemMatch = line.match(/^[\-\*\d+\.\、]\s+(.+)/)
    if (itemMatch) {
      items.push(itemMatch[1].trim())
    }
  }
  return items
}

onMounted(async () => {
  await fetchDetail()
  fetchReferralContext()
  checkAlreadyApplied()
})
</script>

<style scoped>
/* ============================================================
   CSS VARIABLES
   ============================================================ */
.jd-page {
  --jd-primary: #1677ff;
  --jd-primary-deep: #0958d9;
  --jd-primary-subtle: #e6f4ff;
  --jd-accent: #fa8c16;
  --jd-success: #52c41a;
  --jd-warning: #e6a23c;
  --jd-danger: #ff4d4f;
  --jd-bg: #f7f8fc;
  --jd-surface: #ffffff;
  --jd-text: #121826;
  --jd-text-secondary: #4a4f5c;
  --jd-text-muted: #8b8fa3;
  --jd-text-placeholder: #b0b4c0;
  --jd-border: #e8ebf1;
  --jd-border-light: #f1f3f7;
  --jd-radius-sm: 6px;
  --jd-radius: 10px;
  --jd-radius-lg: 14px;
  --jd-radius-xl: 20px;
  --jd-shadow-xs: 0 1px 2px rgba(0,0,0,.03);
  --jd-shadow-sm: 0 1px 3px rgba(0,0,0,.05), 0 1px 2px rgba(0,0,0,.04);
  --jd-shadow: 0 4px 16px rgba(0,0,0,.06), 0 2px 6px rgba(0,0,0,.03);
  --jd-shadow-lg: 0 8px 32px rgba(0,0,0,.08), 0 4px 12px rgba(0,0,0,.04);
  --jd-font: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;

  min-height: 100vh;
  background: var(--jd-bg);
  font-family: var(--jd-font);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: var(--jd-text);
  line-height: 1.6;
}

/* ---- Error / State ---- */
.jd-state-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 40px 20px;
  background: var(--jd-bg);
}

.jd-state-card {
  background: var(--jd-surface);
  border-radius: var(--jd-radius-xl);
  padding: 72px 56px;
  text-align: center;
  max-width: 440px;
  width: 100%;
  box-shadow: var(--jd-shadow-lg);
  border: 1px solid var(--jd-border);
}

.jd-state-icon {
  margin-bottom: 20px;
}

.jd-state-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--jd-text);
  margin: 0 0 10px;
  letter-spacing: -0.2px;
}

.jd-state-desc {
  font-size: 15px;
  color: var(--jd-text-muted);
  margin: 0 0 28px;
  line-height: 1.7;
}

/* ============================================================
   HEADER
   ============================================================ */
.jd-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255,255,255,.88);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--jd-border);
}

.jd-header-inner {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 24px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.jd-logo {
  flex-shrink: 0;
}

.jd-header-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}

.jd-brand-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--jd-text);
  letter-spacing: -0.2px;
}

.jd-header-btn {
  font-size: 13px;
  font-weight: 500;
  color: var(--jd-primary);
  text-decoration: none;
  padding: 5px 14px;
  border-radius: 6px;
  border: 1px solid var(--jd-primary);
  transition: all .15s;
}

.jd-header-btn:hover {
  background: var(--jd-primary);
  color: #fff;
}

.jd-logout-btn {
  font-size: 13px;
  font-weight: 500;
  color: var(--jd-text-secondary);
  background: none;
  border: 1px solid var(--jd-border);
  border-radius: 6px;
  padding: 5px 14px;
  cursor: pointer;
  transition: all .15s;
}

.jd-logout-btn:hover {
  color: #f56c6c;
  border-color: #f56c6c;
}

.jd-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.jd-user-phone {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--jd-text-secondary);
}

.jd-user-icon {
  color: var(--jd-primary);
  flex-shrink: 0;
}

/* ============================================================
   HERO
   ============================================================ */
.jd-hero-wrapper {
  max-width: 960px;
  margin: 0 auto;
  padding: 44px 24px 0;
}

.jd-hero {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 32px;
  align-items: start;
  background: var(--jd-surface);
  border-radius: var(--jd-radius-xl);
  padding: 44px 48px;
  box-shadow: var(--jd-shadow);
  border: 1px solid var(--jd-border);
  position: relative;
  overflow: hidden;
}

.jd-hero::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #1677ff, #2f54eb, #722ed1);
}

.jd-hero::after {
  content: '';
  position: absolute;
  bottom: -160px;
  right: -60px;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle at center, rgba(22,119,255,.03) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.jd-hero-main {
  position: relative;
  z-index: 1;
  min-width: 0;
}

/* Eyebrow */
.jd-hero-eyebrow {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  font-size: 13px;
}

.jd-eyebrow-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  padding: 4px 12px;
  border-radius: 100px;
  font-size: 12px;
}

.jd-eyebrow-tag.is-open {
  background: #f6ffed;
  color: #389e0d;
  border: 1px solid #b7eb8f;
}

.jd-eyebrow-tag.is-closed {
  background: #fff1f0;
  color: #cf1322;
  border: 1px solid #ffa39e;
}

.jd-eyebrow-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.jd-eyebrow-sep {
  color: var(--jd-border);
}

.jd-eyebrow-program {
  color: var(--jd-text-muted);
}

/* Title */
.jd-hero-title {
  font-size: 30px;
  font-weight: 800;
  color: var(--jd-text);
  margin: 0 0 16px;
  letter-spacing: -0.6px;
  line-height: 1.25;
}

/* Tags */
.jd-hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 24px;
}

.jd-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 500;
  padding: 5px 12px;
  border-radius: 100px;
  white-space: nowrap;
}

.jd-tag--info {
  background: #f5f6fa;
  color: var(--jd-text-secondary);
  border: 1px solid var(--jd-border);
}

.jd-tag--highlight {
  background: var(--jd-primary-subtle);
  color: var(--jd-primary);
  border: 1px solid #91caff;
}

.jd-tag--urgent {
  background: #fff1f0;
  color: #cf1322;
  border: 1px solid #ffa39e;
  font-weight: 600;
}

.jd-tag--hot {
  background: #fff7e6;
  color: #d46b08;
  border: 1px solid #ffd591;
  font-weight: 600;
}

/* Referral Context Bar */
.jd-ref-ctx-bar {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  margin: 12px 0 24px;
  background: linear-gradient(135deg, #e6f4ff, #f0f8ff);
  border: 1px solid rgba(22, 119, 255, 0.12);
  border-radius: 8px;
  font-size: 13px;
}

.jd-ref-ctx-icon {
  color: #1677ff;
  display: flex;
  align-items: center;
}

.jd-ref-ctx-label {
  color: var(--jd-text-muted);
  margin-right: 2px;
}

.jd-ref-ctx-code {
  font-family: 'SF Mono', 'JetBrains Mono', 'Consolas', monospace;
  font-weight: 700;
  font-size: 15px;
  color: #1677ff;
  letter-spacing: 0.08em;
  background: rgba(22, 119, 255, 0.08);
  padding: 2px 8px;
  border-radius: 4px;
}

.jd-ref-ctx-referrer {
  color: var(--jd-text-muted);
  margin-left: 4px;
  padding-left: 8px;
  border-left: 1px solid rgba(22, 119, 255, 0.15);
}

.jd-ref-ctx-reg {
  margin-left: auto;
  font-size: 12px;
  font-weight: 600;
  color: #1677ff;
  text-decoration: none;
  padding: 4px 12px;
  border: 1px solid rgba(22, 119, 255, 0.3);
  border-radius: 6px;
  transition: all 0.2s;
}

.jd-ref-ctx-reg:hover {
  background: #1677ff;
  color: #fff;
}

/* Meta Strip */
.jd-hero-meta-strip {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.jd-meta-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #f8fbff, #edf4ff);
  border-radius: var(--jd-radius);
  border: 1px solid rgba(22,119,255,.08);
  transition: border-color .15s;
}

.jd-meta-item:hover {
  border-color: rgba(22,119,255,.18);
}

.jd-meta-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--jd-surface);
  border-radius: var(--jd-radius-sm);
  color: var(--jd-primary);
  box-shadow: 0 1px 3px rgba(22,119,255,.08);
}

.jd-meta-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.jd-meta-label {
  font-size: 11px;
  color: var(--jd-text-muted);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.jd-meta-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--jd-text-secondary);
}

.jd-meta-value--salary {
  color: var(--jd-accent);
  font-weight: 700;
  font-size: 15px;
}

/* Hero Action */
.jd-hero-action {
  display: flex;
  align-items: flex-start;
  padding-top: 4px;
}

/* Hero CTA */
.jd-hero-cta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 11px 28px;
  background: linear-gradient(135deg, #1677ff, #2f54eb);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  border-radius: 100px;
  text-decoration: none;
  transition: transform .15s, box-shadow .15s;
  box-shadow: 0 4px 12px rgba(22,119,255,.35);
}

.jd-hero-cta:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(22,119,255,.45);
}

/* ============================================================
   BODY LAYOUT
   ============================================================ */
.jd-body {
  max-width: 960px;
  margin: 0 auto;
  padding: 36px 24px 0;
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 28px;
  align-items: start;
}

/* ============================================================
   CONTENT CARDS
   ============================================================ */
.jd-card {
  background: var(--jd-surface);
  border-radius: var(--jd-radius-lg);
  box-shadow: var(--jd-shadow-xs);
  border: 1px solid var(--jd-border);
  padding: 28px 32px;
  margin-bottom: 20px;
}

.jd-card-hd {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 22px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--jd-border-light);
}

.jd-card-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: var(--jd-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
}

.jd-card-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--jd-text);
  margin: 0;
  letter-spacing: -0.2px;
  flex: 1;
}

.jd-card-sub {
  font-size: 13px;
  color: var(--jd-text-muted);
  white-space: nowrap;
}

.jd-card-sub strong {
  color: var(--jd-primary);
  font-weight: 700;
}

/* ---- Bullet List ---- */
.jd-bullet-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.jd-bullet-list li {
  display: flex;
  gap: 14px;
  padding: 10px 0;
  font-size: 14px;
  color: var(--jd-text-secondary);
  line-height: 1.75;
}

.jd-bullet-list li + li {
  border-top: 1px solid var(--jd-border-light);
}

.jd-bullet-num {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--jd-primary);
  background: var(--jd-primary-subtle);
  border-radius: var(--jd-radius-sm);
  margin-top: 1px;
}

/* ---- Offer Grid ---- */
.jd-offer-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.jd-offer-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 14px;
  color: var(--jd-text-secondary);
  line-height: 1.6;
  padding: 12px 16px;
  background: #f6ffed;
  border-radius: var(--jd-radius);
  border: 1px solid #d9f7be;
}

.jd-offer-dot {
  flex-shrink: 0;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--jd-success);
  margin-top: 7px;
}

/* ---- Skills ---- */
.jd-skills-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.jd-skill-chip {
  font-size: 13px;
  font-weight: 500;
  padding: 7px 16px;
  background: linear-gradient(135deg, #fff7e6, #fff3e0);
  color: #d46b08;
  border-radius: 100px;
  border: 1px solid #ffd591;
}

/* ============================================================
   FORM
   ============================================================ */
.jd-form-card {
  scroll-margin-top: 80px;
}

.jd-form-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--jd-text);
}

.jd-optional {
  font-style: normal;
  font-weight: 400;
  font-size: 12px;
  color: var(--jd-text-muted);
}

.jd-required {
  font-style: normal;
  font-weight: 400;
  font-size: 12px;
  color: #ff4d4f;
}

/* ---- Login Gate ---- */
.jd-login-gate {
  text-align: center;
  padding: 52px 24px;
}

.jd-login-gate-icon {
  margin-bottom: 16px;
}

.jd-login-gate-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--jd-text);
  margin: 0 0 8px;
}

.jd-login-gate-desc {
  font-size: 14px;
  color: var(--jd-text-muted);
  margin: 0 0 24px;
}

.jd-form {
  max-width: 100%;
}

.jd-form-item--readonly {
  margin-bottom: 22px;
}

.jd-readonly-field {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--jd-text);
  padding: 12px 16px;
  background: var(--jd-primary-subtle);
  border-radius: var(--jd-radius);
  border: 1px solid #91caff;
}

.jd-form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 20px;
}

.jd-form-col-full {
  grid-column: 1 / -1;
}

.jd-upload-placeholder {
  padding: 12px 0;
  text-align: center;
  transition: opacity 0.2s;
}

.jd-upload-placeholder.is-uploading {
  padding: 16px 0;
}

.jd-upload-placeholder p {
  margin: 8px 0 4px;
  font-size: 14px;
  color: var(--jd-text-secondary);
}

.jd-upload-placeholder.is-uploading p {
  margin-top: 12px;
  color: #1677ff;
  font-weight: 500;
}

.jd-upload-placeholder span {
  font-size: 12px;
  color: var(--jd-text-muted);
}

.jd-upload-spinner {
  color: #1677ff;
  animation: jd-spin 1s linear infinite;
}

@keyframes jd-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Uploaded file card */
.jd-file-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f0f5ff;
  border: 1px solid #d6e4ff;
  border-radius: 8px;
}
.jd-file-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}
.jd-file-name {
  font-size: 14px;
  color: #1677ff;
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.jd-file-name:hover {
  text-decoration: underline;
}

.jd-submit-btn {
  width: 100%;
  height: 52px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.3px;
  margin-top: 4px;
}

/* ---- Success State ---- */
.jd-success {
  text-align: center;
  padding: 52px 24px;
}

.jd-success-icon {
  margin-bottom: 20px;
}

.jd-success-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--jd-text);
  margin: 0 0 10px;
  letter-spacing: -0.3px;
}

.jd-success-desc {
  font-size: 15px;
  color: var(--jd-text-muted);
  line-height: 1.7;
  margin: 0;
}

/* ============================================================
   SIDEBAR
   ============================================================ */
.jd-sidebar {
  position: sticky;
  top: 80px;
}

.jd-sidebar-inner {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.jd-sidebar-card {
  padding: 22px 24px;
  margin-bottom: 0;
}

.jd-sidebar-card-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--jd-text);
  margin: 0 0 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--jd-border-light);
}

.jd-info-list {
  margin: 0;
  padding: 0;
}

.jd-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 9px 0;
  font-size: 13px;
}

.jd-info-row + .jd-info-row {
  border-top: 1px solid var(--jd-border-light);
}

.jd-info-row dt {
  color: var(--jd-text-muted);
  font-weight: 400;
}

.jd-info-row dd {
  color: var(--jd-text-secondary);
  font-weight: 500;
  text-align: right;
  max-width: 58%;
  word-break: break-word;
}

.jd-hc-badge {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  color: var(--jd-primary);
  background: var(--jd-primary-subtle);
  padding: 2px 10px;
  border-radius: 100px;
}

.jd-sidebar-cta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px;
  background: linear-gradient(135deg, #1677ff, #2f54eb);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  border-radius: var(--jd-radius);
  text-decoration: none;
  transition: transform .15s, box-shadow .15s;
  box-shadow: 0 4px 14px rgba(22,119,255,.3);
}

.jd-sidebar-cta:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(22,119,255,.4);
}

/* ============================================================
   FOOTER
   ============================================================ */
.jd-footer {
  max-width: 960px;
  margin: 0 auto;
  padding: 40px 24px;
}

.jd-footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 0;
  border-top: 1px solid var(--jd-border);
  font-size: 13px;
}

.jd-footer-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--jd-text-secondary);
  font-weight: 600;
}

.jd-footer-text {
  color: var(--jd-text-muted);
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 1024px) {
  .jd-hero {
    grid-template-columns: 1fr;
    padding: 36px 32px;
  }

  .jd-hero-meta-strip {
    grid-template-columns: 1fr 1fr;
  }

  .jd-body {
    grid-template-columns: 1fr;
  }

  .jd-sidebar {
    position: static;
  }

  .jd-offer-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .jd-hero-wrapper {
    padding: 28px 16px 0;
  }

  .jd-footer {
    padding: 40px 16px;
  }

  .jd-hero {
    padding: 28px 20px;
  }

  .jd-hero-title {
    font-size: 23px;
  }

  .jd-hero-meta-strip {
    grid-template-columns: 1fr;
  }

  .jd-hero-cta {
    width: 100%;
    justify-content: center;
  }

  .jd-body {
    padding: 28px 16px 0;
  }

  .jd-card {
    padding: 22px 18px;
  }

  .jd-form-row {
    grid-template-columns: 1fr;
  }

  .jd-footer-inner {
    flex-direction: column;
    gap: 8px;
  }
}

/* ---- Duplicate Apply Inline Alert ---- */
.jd-duplicate-alert {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 18px;
  margin-bottom: 20px;
  border-radius: 10px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
}

.jd-duplicate-alert-icon {
  flex-shrink: 0;
  margin-top: 1px;
}

.jd-duplicate-alert-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.jd-duplicate-alert-title {
  font-size: 14px;
  font-weight: 600;
  color: #8c6d1f;
  line-height: 1.5;
}

.jd-duplicate-alert-desc {
  font-size: 12px;
  color: #bfa35b;
  line-height: 1.4;
}
</style>
