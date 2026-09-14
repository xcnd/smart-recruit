<template>
  <div class="job-detail-page">
    <!-- Loading State -->
    <div v-if="loading" class="not-found">
      <div class="not-found-card">
        <div class="not-found-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" stroke-dasharray="40" stroke-dashoffset="0">
              <animateTransform attributeName="transform" type="rotate" from="0 12 12" to="360 12 12" dur="1s" repeatCount="indefinite" />
            </circle>
          </svg>
        </div>
        <h2 class="not-found-title">加载中...</h2>
      </div>
    </div>

    <!-- Not Found State -->
    <div v-else-if="!job" class="not-found">
      <div class="not-found-card">
        <div class="not-found-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
        </div>
        <h2 class="not-found-title">职位不存在</h2>
        <p class="not-found-desc">该职位可能已下架或链接无效</p>
        <router-link to="/jobs" class="not-found-link">
          查看所有热招职位
        </router-link>
      </div>
    </div>

    <!-- Job Detail -->
    <template v-else>
      <!-- Hero Section -->
      <section class="detail-hero">
        <!-- Glow orbs -->
        <div class="detail-hero__orb detail-hero__orb--1" aria-hidden="true" />
        <div class="detail-hero__orb detail-hero__orb--2" aria-hidden="true" />
        <div class="detail-hero__orb detail-hero__orb--3" aria-hidden="true" />

        <!-- Grid pattern overlay -->
        <div class="detail-hero__grid" aria-hidden="true" />

        <!-- Content -->
        <div class="container">
          <!-- Back Link -->
          <router-link to="/jobs" class="back-link">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <line x1="19" y1="12" x2="5" y2="12" />
              <polyline points="12 19 5 12 12 5" />
            </svg>
            返回职位列表
          </router-link>

          <!-- Job Header -->
          <div class="job-header">
            <p class="job-dept">{{ job.dept }}</p>
            <h1 class="job-title">{{ job.title }}</h1>
            <div class="job-tags">
              <span
                v-for="(tag, idx) in job.tags"
                :key="idx"
                class="job-tag"
                :class="tag.cls"
              >
                {{ tag.text }}
              </span>
            </div>
          </div>

          <!-- Meta Grid -->
          <div class="meta-grid">
            <div class="meta-item">
              <span class="meta-label">工作地点</span>
              <span class="meta-value">{{ job.location }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">经验要求</span>
              <span class="meta-value">{{ job.exp }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">薪资范围</span>
              <span class="meta-value salary-value">{{ job.salary }}</span>
            </div>
          </div>
        </div>

        </section>

      <!-- Detail Content -->
      <section class="detail-content">
        <div class="container">
          <div class="content-card">
            <!-- 岗位职责 -->
            <div class="content-section">
              <h4 class="content-section-title">
                <span class="content-section-icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="9 11 12 14 22 4" />
                    <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11" />
                  </svg>
                </span>
                岗位职责
              </h4>
              <ul class="content-list">
                <li v-for="(item, idx) in job.responsibilities" :key="'resp-' + idx">
                  {{ item }}
                </li>
              </ul>
            </div>

            <!-- 任职要求 -->
            <div class="content-section">
              <h4 class="content-section-title">
                <span class="content-section-icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
                    <polyline points="22 4 12 14.01 9 11.01" />
                  </svg>
                </span>
                任职要求
              </h4>
              <ul class="content-list">
                <li v-for="(item, idx) in job.requirements" :key="'req-' + idx">
                  {{ item }}
                </li>
              </ul>
            </div>

            <!-- 加分项 -->
            <div v-if="job.bonus && job.bonus.length > 0" class="content-section">
              <h4 class="content-section-title">
                <span class="content-section-icon bonus-icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                  </svg>
                </span>
                加分项
              </h4>
              <ul class="content-list bonus-list">
                <li v-for="(item, idx) in job.bonus" :key="'bonus-' + idx">
                  {{ item }}
                </li>
              </ul>
            </div>
          </div>

          <!-- Apply Button -->
          <div class="apply-section">
            <button
              v-if="alreadyApplied"
              class="apply-btn applied-btn"
              disabled
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
                <polyline points="22 4 12 14.01 9 11.01" />
              </svg>
              已投递
            </button>
            <p class="apply-hint" v-if="alreadyApplied">
              可在「<router-link to="/my-applications" class="apply-hint-link">我的投递</router-link>」中查看进度
            </p>
            <button v-else class="apply-btn" @click="handleApply">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4-4v2" />
                <circle cx="8.5" cy="7" r="4" />
                <line x1="20" y1="8" x2="20" y2="14" />
                <line x1="23" y1="11" x2="17" y2="11" />
              </svg>
              立即投递简历
            </button>
            <p class="apply-hint" v-if="!alreadyApplied">
              投递后可在「<router-link to="/my-applications" class="apply-hint-link">我的投递</router-link>」中随时查看进度
            </p>
          </div>
        </div>
      </section>

      <!-- Apply Dialog -->
      <el-dialog
        v-model="showApplyDialog"
        title="投递简历"
        width="520px"
        :close-on-click-modal="false"
        :close-on-press-escape="!submitting"
        center
        class="apply-dialog"
      >
        <el-form
          ref="applyFormRef"
          :model="applyForm"
          :rules="applyFormRules"
          label-position="top"
          @submit.prevent
        >
          <el-form-item label="姓名" prop="candidateName">
            <el-input
              v-model="applyForm.candidateName"
              placeholder="请输入您的姓名"
              maxlength="32"
              :disabled="submitting"
            />
          </el-form-item>

          <el-form-item label="手机号" prop="candidatePhone">
            <el-input
              v-model="applyForm.candidatePhone"
              placeholder="请输入您的手机号"
              maxlength="16"
              :disabled="submitting"
            />
          </el-form-item>

          <el-form-item label="邮箱" prop="candidateEmail">
            <el-input
              v-model="applyForm.candidateEmail"
              placeholder="请输入您的邮箱"
              maxlength="64"
              :disabled="submitting"
            />
          </el-form-item>

          <el-form-item label="附件简历">
            <input
              ref="fileInputRef"
              type="file"
              accept=".pdf,.doc,.docx"
              class="upload-input-hidden"
              @change="handleFileChange"
            />
            <div
              class="upload-drop-zone"
              :class="{
                'upload-drop-zone--dragover': isDragover,
                'upload-drop-zone--uploading': uploading,
                'upload-drop-zone--success': uploadDone,
                'upload-drop-zone--error': !!uploadError,
                'upload-drop-zone--disabled': submitting,
              }"
              @dragover.prevent="onDragOver"
              @dragleave.prevent="onDragLeave"
              @drop.prevent="onDrop"
              @click="uploading || submitting ? null : fileInputRef?.click()"
            >
              <!-- Empty / initial state -->
              <template v-if="!uploading && !uploadDone && !uploadError">
                <div class="upload-drop-icon">
                  <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" />
                    <polyline points="17 8 12 3 7 8" />
                    <line x1="12" y1="3" x2="12" y2="15" />
                  </svg>
                </div>
                <p class="upload-drop-title">点击或将简历文件拖拽到此处</p>
                <p class="upload-drop-hint">支持 PDF、DOC、DOCX 格式，大小不超过 15MB</p>
              </template>

              <!-- Uploading state -->
              <template v-else-if="uploading">
                <div class="upload-file-card">
                  <div class="upload-file-card__icon">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
                      <polyline points="14 2 14 8 20 8" />
                      <line x1="16" y1="13" x2="8" y2="13" />
                      <line x1="16" y1="17" x2="8" y2="17" />
                      <polyline points="10 9 9 9 8 9" />
                    </svg>
                  </div>
                  <div class="upload-file-card__info">
                    <p class="upload-file-card__name">{{ selectedFile?.name || '' }}</p>
                    <p class="upload-file-card__size">{{ formatFileSize(selectedFile?.size || 0) }}</p>
                    <div class="upload-file-card__progress-bar">
                      <div class="upload-file-card__progress-fill" />
                    </div>
                    <p class="upload-file-card__status">上传中...</p>
                  </div>
                </div>
              </template>

              <!-- Success state -->
              <template v-else-if="uploadDone">
                <div class="upload-file-card upload-file-card--done">
                  <div class="upload-file-card__icon upload-file-card__icon--done">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
                      <polyline points="14 2 14 8 20 8" />
                    </svg>
                    <span class="upload-file-card__check">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="20 6 9 17 4 12" />
                      </svg>
                    </span>
                  </div>
                  <div class="upload-file-card__info">
                    <p class="upload-file-card__name">{{ selectedFile?.name || '' }}</p>
                    <p class="upload-file-card__size">{{ formatFileSize(selectedFile?.size || 0) }}</p>
                    <p class="upload-file-card__status upload-file-card__status--done">上传成功</p>
                  </div>
                  <button
                    type="button"
                    class="upload-file-card__remove"
                    @click.stop="removeFile"
                    title="移除文件"
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                      <line x1="18" y1="6" x2="6" y2="18" />
                      <line x1="6" y1="6" x2="18" y2="18" />
                    </svg>
                  </button>
                </div>
              </template>

              <!-- Error state -->
              <template v-else-if="uploadError">
                <div class="upload-drop-icon upload-drop-icon--error">
                  <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="12" y1="8" x2="12" y2="12" />
                    <line x1="12" y1="16" x2="12.01" y2="16" />
                  </svg>
                </div>
                <p class="upload-drop-title upload-drop-title--error">{{ uploadError }}</p>
                <p class="upload-drop-hint">点击此处重新选择文件</p>
              </template>
            </div>
          </el-form-item>
        </el-form>

        <template #footer>
          <div class="dialog-footer">
            <el-button @click="showApplyDialog = false" :disabled="submitting" size="large">
              取消
            </el-button>
            <el-button
              type="primary"
              @click="handleSubmitApplication"
              :loading="submitting"
              size="large"
            >
              {{ submitting ? '提交中...' : '确认投递' }}
            </el-button>
          </div>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getCareersPublicJobById, uploadResume, submitApplication, checkApplied } from '@/api/careers'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

interface JobTag {
  text: string
  cls: string
}

interface DetailedJob {
  id: number
  title: string
  dept: string
  location: string
  exp: string
  salary: string
  category: string
  date: string
  tags: JobTag[]
  responsibilities: string[]
  requirements: string[]
  bonus?: string[]
}

const allJobs: DetailedJob[] = [
  {
    id: 1,
    title: '资深后端开发工程师（Java）',
    dept: '技术研发部 · 核心平台组',
    location: '北京',
    exp: '5-10年',
    salary: '40-70K·16薪',
    category: 'tech',
    date: '2026-07-28',
    tags: [
      { text: '热招', cls: 'hot' },
      { text: 'Java', cls: 'tech-tag' },
      { text: 'Spring Boot', cls: 'tech-tag' },
    ],
    responsibilities: [
      '负责 SmartRecruit 核心招聘引擎的后端架构设计与开发',
      '参与大规模分布式系统的性能优化与稳定性治理',
      '主导技术方案评审，推动技术债务清理与系统重构',
      '指导初中级工程师，建立代码规范与最佳实践',
    ],
    requirements: [
      '5 年以上 Java 开发经验，扎实的计算机基础',
      '精通 Spring Boot / Spring Cloud 微服务架构',
      '熟悉 MySQL、Redis、Elasticsearch、Kafka 等中间件',
      '有大规模分布式系统设计与调优经验',
    ],
    bonus: [
      '有 SaaS / 企业服务领域经验优先',
      '有开源项目贡献或技术博客者优先',
      '熟悉 AI / LLM 应用开发者优先',
    ],
  },
  {
    id: 2,
    title: 'AI 算法工程师（NLP/CV）',
    dept: 'AI 研究院 · 算法团队',
    location: '北京 / 上海',
    exp: '3-8年',
    salary: '50-90K·16薪',
    category: 'data',
    date: '2026-07-26',
    tags: [
      { text: '热招', cls: 'hot' },
      { text: 'NLP', cls: 'tech-tag' },
      { text: 'LLM', cls: 'tech-tag' },
    ],
    responsibilities: [
      '研发简历解析、人岗匹配、智能推荐等核心 AI 算法',
      '跟踪前沿大语言模型技术，探索 LLM 在招聘场景的深度应用',
      '设计并实现大规模 NLP/CV 模型训练与推理系统',
      '参与 AI 相关专利撰写与顶级会议论文投稿',
    ],
    requirements: [
      '硕士及以上学历，计算机/AI/数学等相关专业',
      '精通 Python，熟悉 PyTorch / TensorFlow 框架',
      '在 NLP/CV/推荐系统任一方向有深入研究与落地经验',
      '有 LLM 微调、RAG、Agent 等项目经验优先',
    ],
    bonus: [
      '有 ACL/EMNLP/CVPR/NeurIPS 等顶会论文发表经历',
      '有大规模分布式训练经验',
      '熟悉 CUDA 编程与模型推理优化',
    ],
  },
  {
    id: 3,
    title: '高级前端开发工程师（React/Vue）',
    dept: '技术研发部 · 前端团队',
    location: '上海',
    exp: '3-7年',
    salary: '35-60K·16薪',
    category: 'tech',
    date: '2026-07-25',
    tags: [
      { text: 'React', cls: 'tech-tag' },
      { text: 'Vue', cls: 'tech-tag' },
      { text: 'TypeScript', cls: 'tech-tag' },
    ],
    responsibilities: [
      '负责 SmartRecruit SaaS 平台前端架构设计与核心模块开发',
      '建设前端工程化体系（构建/测试/部署/监控）',
      '推动前端性能优化与用户体验提升',
      '参与组件库与低代码平台建设',
    ],
    requirements: [
      '3 年以上前端开发经验，精通 React 或 Vue 框架',
      '扎实的 JavaScript/TypeScript/CSS 基础',
      '熟悉前端工程化（Webpack/Vite/Monorepo）',
      '有大型前端项目架构经验',
    ],
    bonus: [
      '有微前端架构落地经验',
      '有 Node.js BFF 层开发经验',
      '有可视化/低代码平台经验优先',
    ],
  },
  {
    id: 4,
    title: '高级产品经理（SaaS 方向）',
    dept: '产品部 · 招聘产品线',
    location: '北京',
    exp: '5-8年',
    salary: '35-55K·16薪',
    category: 'product',
    date: '2026-07-24',
    tags: [
      { text: 'B端', cls: 'tech-tag' },
      { text: 'SaaS', cls: 'tech-tag' },
    ],
    responsibilities: [
      '负责 SmartRecruit 招聘 SaaS 产品的规划与迭代',
      '深入理解企业招聘业务流程，抽象通用化产品方案',
      '通过数据分析与客户访谈驱动产品决策',
      '协调设计、研发、测试团队，推动产品高质量交付',
    ],
    requirements: [
      '5 年以上产品经理经验，有 B 端 SaaS 产品背景',
      '出色的逻辑思维与抽象能力，善于从复杂场景中提炼产品方案',
      '具备良好的数据分析能力，熟练使用 SQL',
      '有招聘/HR Tech 领域经验优先',
    ],
    bonus: [
      '有从 0 到 1 的产品孵化经验',
      '有 AI 产品的设计与落地经验',
      '有国际化产品经验优先',
    ],
  },
  {
    id: 5,
    title: '资深 UI/UX 设计师',
    dept: '设计部 · 体验设计团队',
    location: '北京 / 深圳',
    exp: '3-6年',
    salary: '30-50K·16薪',
    category: 'product',
    date: '2026-07-22',
    tags: [
      { text: 'B端设计', cls: 'tech-tag' },
      { text: 'UX', cls: 'tech-tag' },
    ],
    responsibilities: [
      '负责 SmartRecruit 产品的 UI/UX 设计，打造极致用户体验',
      '建立并维护设计系统与组件库，确保产品体验一致性',
      '通过用户研究、可用性测试驱动设计迭代',
      '与产品、研发紧密协作，推动设计方案高质量落地',
    ],
    requirements: [
      '3 年以上 UI/UX 设计经验，有成熟的设计作品集',
      '精通 Figma 等设计工具，熟悉设计系统搭建',
      '有 B 端 / SaaS 产品设计经验，理解企业用户场景',
      '良好的设计表达能力与数据驱动的设计思维',
    ],
    bonus: [
      '有 AI 产品的设计经验',
      '有动效设计与前端实现能力',
      '有用户研究 / 用研方法论经验',
    ],
  },
  {
    id: 6,
    title: '大客户销售经理（HR Tech）',
    dept: '销售部 · 大客户团队',
    location: '北京 / 上海 / 深圳',
    exp: '5-10年',
    salary: '25-45K·16薪',
    category: 'market',
    date: '2026-07-20',
    tags: [
      { text: '热招', cls: 'hot' },
      { text: '大客户', cls: 'tech-tag' },
    ],
    responsibilities: [
      '负责 SmartRecruit 产品在大中型企业的销售与推广',
      '深入挖掘客户招聘痛点，制定针对性解决方案',
      '维护并深化核心客户关系，推动续约与增购',
      '收集市场与竞品信息，为产品迭代提供市场洞察',
    ],
    requirements: [
      '5 年以上 B2B 软件销售经验，有 SaaS/HR Tech 销售背景优先',
      '出色的商务谈判能力与解决方案式销售能力',
      '有 500 强企业或大型国企的客户资源与成功案例',
      '良好的抗压能力与自我驱动力',
    ],
    bonus: [
      '有 HR SaaS 产品成功销售经验',
      '管理过百万级 ARR 客户群',
      '985/211 本科及以上学历优先',
    ],
  },
  {
    id: 7,
    title: '数据平台开发工程师',
    dept: '数据与 AI 平台部',
    location: '杭州',
    exp: '3-7年',
    salary: '35-60K·16薪',
    category: 'data',
    date: '2026-07-18',
    tags: [
      { text: '大数据', cls: 'tech-tag' },
      { text: 'Flink', cls: 'tech-tag' },
    ],
    responsibilities: [
      '设计并开发 SmartRecruit 一站式数据平台',
      '构建实时/离线数据计算链路，支撑业务决策与 AI 训练',
      '负责数据质量治理与数据资产管理',
      '推动数据产品化，赋能业务团队自助取数与分析',
    ],
    requirements: [
      '3 年以上大数据开发经验，精通 Java/Scala/Python',
      '熟悉 Hadoop/Spark/Flink/Hive 等大数据技术栈',
      '有数据仓库建模与 ETL 开发经验',
      '有实时流计算（Flink/Kafka Streams）项目经验',
    ],
    bonus: [
      '有数据湖（Iceberg/Hudi）方案落地经验',
      '有数据平台产品建设经验',
      '熟悉 ClickHouse/Doris 等 OLAP 引擎',
    ],
  },
  {
    id: 8,
    title: '安全合规工程师',
    dept: '基础架构部 · 安全团队',
    location: '北京',
    exp: '5-8年',
    salary: '40-65K·16薪',
    category: 'tech',
    date: '2026-07-16',
    tags: [
      { text: '安全', cls: 'tech-tag' },
      { text: '合规', cls: 'tech-tag' },
    ],
    responsibilities: [
      '负责公司信息安全体系建设和持续改进',
      '主导 SOC2/ISO27001/等保三级等合规认证',
      '带领安全渗透测试与安全左移建设',
      '建立数据安全与隐私保护体系',
    ],
    requirements: [
      '5 年以上信息安全工作经验',
      '熟悉主流安全攻防技术，有渗透测试实战经验',
      '了解 SOC2/ISO27001/等保/GDPR 等合规标准',
      '有安全自动化与 DevSecOps 实践经验',
    ],
    bonus: [
      '持有 CISSP/CISP/CISA 等安全认证',
      '有知名互联网公司安全团队经验',
      '有数据安全/隐私计算相关经验',
    ],
  },
  {
    id: 9,
    title: '校园招聘 HR',
    dept: '人力资源部 · 招聘团队',
    location: '北京',
    exp: '2-5年',
    salary: '18-30K·16薪',
    category: 'operation',
    date: '2026-07-15',
    tags: [
      { text: '校招', cls: 'new' },
      { text: '雇主品牌', cls: 'tech-tag' },
    ],
    responsibilities: [
      '统筹 SmartRecruit 年度校园招聘项目',
      '策划并执行高校宣讲会、Open Day、线上直播等活动',
      '维护与目标高校就业办的合作关系',
      '建设并提升 SmartRecruit 雇主品牌影响力',
    ],
    requirements: [
      '2 年以上校园招聘或雇主品牌相关经验',
      '出色的活动策划与项目管理能力',
      '优秀的沟通表达与演讲能力',
      '能适应校招季高频出差',
    ],
    bonus: [
      '有互联网大厂校招经验优先',
      '有新媒体运营与内容创作能力',
      '985/211 本科及以上学历优先',
    ],
  },
]

const job = ref<DetailedJob | null>(null)
const loading = ref(true)

onMounted(async () => {
  const id = Number(route.params.id)

  // Quick check: localStorage (instant feedback, no network)
  if (isJobAppliedLocally(id)) {
    alreadyApplied.value = true
  }

  // First try hardcoded legacy data (IDs 1-9 with detailed descriptions)
  const found = allJobs.find((j) => j.id === id)
  if (found) {
    job.value = found
    return
  }

  // Try public API endpoint for jobs from database
  try {
    const apiJob = await getCareersPublicJobById(id)
    if (apiJob) {
      job.value = {
        id: apiJob.id,
        title: apiJob.title,
        dept: apiJob.dept,
        location: apiJob.location,
        exp: apiJob.exp || '',
        salary: apiJob.salary || '',
        category: apiJob.category || '',
        date: formatApiDate(apiJob.createTime || ''),
        tags: (apiJob.tags || []).map((t: any) =>
          typeof t === 'string' ? { text: t, cls: 'tech-tag' } : t
        ),
        responsibilities: apiJob.responsibilities || [],
        requirements: apiJob.requirements || [],
        bonus: apiJob.bonus || [],
      }
    }
  } catch {
    /* not found — job stays null */
  }

  // Verify applied status via API (update localStorage accordingly)
  if (job.value && userStore.isLoggedIn) {
    try {
      const result = await checkApplied(job.value.id, userId.value, userEmail.value)
      if (result.applied) {
        alreadyApplied.value = true
        markJobAppliedLocally(job.value.id)
      } else {
        unmarkJobAppliedLocally(job.value.id)
      }
    } catch {
      // If API fails, keep localStorage state as fallback
    }
  }
  loading.value = false
})

function formatApiDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

// ---- Apply dialog state ----

const showApplyDialog = ref(false)
const alreadyApplied = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const uploadDone = ref(false)
const uploadError = ref('')
const isDragover = ref(false)
const selectedFile = ref<File | null>(null)
const resumeUrl = ref('')
const applyFormRef = ref<FormInstance>()
const fileInputRef = ref<HTMLInputElement>()

const applyForm = ref({
  candidateName: '',
  candidatePhone: '',
  candidateEmail: '',
})

const applyFormRules: FormRules = {
  candidateName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
  ],
  candidatePhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  candidateEmail: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
}

const userId = computed(() => {
  const info = userStore.userInfo as Record<string, unknown> | null
  return (info?.id as number) || 0
})

const userEmail = computed(() => {
  const info = userStore.userInfo as Record<string, unknown> | null
  return (info?.email as string) || ''
})

function prefillForm() {
  const info = userStore.userInfo as Record<string, unknown> | null
  if (info) {
    applyForm.value.candidateName = (info.name as string) || ''
    applyForm.value.candidatePhone = ((info.phone || info.mobile) as string) || ''
    applyForm.value.candidateEmail = (info.email as string) || ''
  }
}

// ---- drag & drop ----

function onDragOver() {
  if (submitting.value) return
  isDragover.value = true
}

function onDragLeave() {
  isDragover.value = false
}

function onDrop(event: DragEvent) {
  isDragover.value = false
  if (submitting.value) return
  const file = event.dataTransfer?.files?.[0]
  if (file) validateAndUpload(file)
}

// ---- file upload ----

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) validateAndUpload(file)
  // Reset input so the same file can be re-selected
  input.value = ''
}

function validateAndUpload(file: File) {
  uploadError.value = ''
  uploadDone.value = false
  resumeUrl.value = ''

  const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()
  if (!['.pdf', '.doc', '.docx'].includes(ext)) {
    uploadError.value = '仅支持 PDF、DOC、DOCX 格式的文件'
    selectedFile.value = null
    return
  }

  if (file.size > 15 * 1024 * 1024) {
    uploadError.value = '文件大小不能超过 15MB'
    selectedFile.value = null
    return
  }

  selectedFile.value = file
  uploadFile(file)
}

async function uploadFile(file: File) {
  uploading.value = true
  uploadError.value = ''
  uploadDone.value = false
  try {
    const result = await uploadResume(file)
    resumeUrl.value = result.url
    uploadDone.value = true
  } catch {
    uploadError.value = '简历上传失败，请稍后重试'
    selectedFile.value = null
    uploadDone.value = false
  } finally {
    uploading.value = false
  }
}

function removeFile() {
  selectedFile.value = null
  resumeUrl.value = ''
  uploadDone.value = false
  uploadError.value = ''
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

async function handleApply() {
  if (!userStore.isLoggedIn) {
    router.push('/phone-login?redirect=' + encodeURIComponent(route.fullPath))
    return
  }

  // Check if hardcoded job (IDs 1-9) — no backend apply support
  if (job.value && job.value.id <= 9) {
    ElMessage.warning('演示职位暂不支持在线投递')
    return
  }

  prefillForm()
  resetUpload()
  showApplyDialog.value = true
}

function resetUpload() {
  selectedFile.value = null
  resumeUrl.value = ''
  uploading.value = false
  uploadDone.value = false
  uploadError.value = ''
  isDragover.value = false
}

// ---- localStorage applied-job tracking ----

const APPLIED_JOBS_KEY = 'sr_applied_jobs'

function getAppliedJobs(): number[] {
  try {
    const raw = localStorage.getItem(APPLIED_JOBS_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function isJobAppliedLocally(jobId: number): boolean {
  return getAppliedJobs().includes(jobId)
}

function markJobAppliedLocally(jobId: number) {
  const jobs = getAppliedJobs()
  if (!jobs.includes(jobId)) {
    jobs.push(jobId)
    localStorage.setItem(APPLIED_JOBS_KEY, JSON.stringify(jobs))
  }
}

function unmarkJobAppliedLocally(jobId: number) {
  const jobs = getAppliedJobs().filter((id) => id !== jobId)
  localStorage.setItem(APPLIED_JOBS_KEY, JSON.stringify(jobs))
}

async function handleSubmitApplication() {
  if (!applyFormRef.value || !job.value) return

  try {
    await applyFormRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await submitApplication({
      jobId: job.value.id,
      candidateName: applyForm.value.candidateName,
      candidatePhone: applyForm.value.candidatePhone,
      candidateEmail: applyForm.value.candidateEmail,
      resumeUrl: resumeUrl.value || undefined,
      candidateId: userId.value || undefined,
    })
    ElMessage.success('投递成功！')
    alreadyApplied.value = true
    markJobAppliedLocally(job.value.id)
    showApplyDialog.value = false
    resetUpload()
  } catch (err: any) {
    ElMessage.error(err?.message || '投递失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.job-detail-page {
  min-height: 100vh;
  background: var(--color-bg);
}

/* ================================================================
   Not Found State
   ================================================================ */
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
  padding: 40px 24px;
}

.not-found-card {
  text-align: center;
  padding: 64px 48px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  max-width: 480px;
  width: 100%;
}

.not-found-icon {
  color: var(--color-text-muted);
  margin-bottom: 24px;
  display: flex;
  justify-content: center;
}

.not-found-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 8px;
}

.not-found-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 32px;
}

.not-found-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: var(--color-primary);
  border-radius: var(--radius-full);
  transition: all var(--transition);
}

.not-found-link:hover {
  background: var(--color-primary-dark);
  opacity: 0.92;
}

/* ================================================================
   Hero Section
   ================================================================ */
.detail-hero {
  position: relative;
  width: 100%;
  padding: 80px 0 120px;
  background: linear-gradient(160deg, #0f172a 0%, #1e293b 30%, #151d30 50%, #101727 100%);
  overflow: hidden;
  isolation: isolate;
}

/* -- Glow orbs -- */
.detail-hero__orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.35;
  pointer-events: none;
  will-change: transform;
}

.detail-hero__orb--1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.6) 0%, transparent 70%);
  top: -15%;
  right: -5%;
  animation: detail-orb-float-1 12s ease-in-out infinite alternate;
}

.detail-hero__orb--2 {
  width: 340px;
  height: 340px;
  background: radial-gradient(circle, rgba(244, 63, 94, 0.45) 0%, transparent 70%);
  bottom: -10%;
  left: -8%;
  animation: detail-orb-float-2 10s ease-in-out infinite alternate;
}

.detail-hero__orb--3 {
  width: 280px;
  height: 280px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.5) 0%, transparent 70%);
  top: 30%;
  left: 50%;
  animation: detail-orb-float-3 8s ease-in-out infinite alternate;
}

@keyframes detail-orb-float-1 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(-40px, 30px) scale(1.08); }
}

@keyframes detail-orb-float-2 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(50px, -20px) scale(1.1); }
}

@keyframes detail-orb-float-3 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(-30px, -40px) scale(1.06); }
}

/* -- Grid pattern overlay -- */
.detail-hero__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 80px 80px;
  mask-image: radial-gradient(ellipse 65% 55% at 50% 38%, rgba(0, 0, 0, 1) 0%, rgba(0, 0, 0, 0.3) 60%, transparent 100%);
  -webkit-mask-image: radial-gradient(ellipse 65% 55% at 50% 38%, rgba(0, 0, 0, 1) 0%, rgba(0, 0, 0, 0.3) 60%, transparent 100%);
  pointer-events: none;
}

/* -- Container above effects -- */
.detail-hero .container {
  position: relative;
  z-index: 2;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 32px;
  transition: color var(--transition);
}

.back-link:hover {
  color: rgba(255, 255, 255, 0.8);
}

.job-header {
  margin-bottom: 32px;
}

.job-dept {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  margin-bottom: 8px;
}

.job-title {
  font-size: clamp(24px, 5vw, 36px);
  font-weight: 800;
  letter-spacing: -0.5px;
  line-height: 1.25;
  color: #ffffff;
  margin-bottom: 16px;
}

.job-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.job-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 500;
  border-radius: var(--radius-full);
  line-height: 1.5;
}

.job-tag.hot {
  background: rgba(244, 63, 94, 0.18);
  color: #fca5a5;
}

.job-tag.new {
  background: rgba(16, 185, 129, 0.18);
  color: #6ee7b7;
}

.job-tag.tech-tag {
  background: rgba(99, 102, 241, 0.18);
  color: #a5b4fc;
}

/* ================================================================
   Meta Grid
   ================================================================ */
.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding: 28px 32px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-lg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.meta-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  font-weight: 400;
}

.meta-value {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.meta-value.salary-value {
  color: #fca5a5;
}

/* ================================================================
   Detail Content
   ================================================================ */
.detail-content {
  padding: 0 0 100px;
  margin-top: -80px;
  position: relative;
  z-index: 2;
}

.content-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  padding: 48px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.content-section {
  margin-bottom: 36px;
}

.content-section:last-child {
  margin-bottom: 0;
}

.content-section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 16px;
  letter-spacing: -0.2px;
}

.content-section-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: var(--color-primary-bg);
  color: var(--color-primary);
  flex-shrink: 0;
}

.content-section-icon.bonus-icon {
  background: rgba(245, 158, 11, 0.1);
  color: var(--color-warning);
}

.content-list {
  padding-left: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.content-list li {
  position: relative;
  padding-left: 22px;
  font-size: 15px;
  color: var(--color-text-secondary);
  line-height: 1.75;
}

.content-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
  opacity: 0.5;
}

.bonus-list li::before {
  background: var(--color-warning);
}

/* ================================================================
   Apply Section
   ================================================================ */
.apply-section {
  margin-top: 40px;
  text-align: center;
}

.apply-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  max-width: 480px;
  padding: 16px 32px;
  font-size: 17px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #1677ff, var(--color-primary));
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition);
  font-family: inherit;
}

.apply-btn:hover {
  opacity: 0.92;
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.4);
  transform: translateY(-1px);
}

.apply-btn:active {
  transform: translateY(0);
}

.apply-hint {
  margin-top: 14px;
  font-size: 13px;
  color: var(--color-text-muted);
}

.apply-hint-link {
  color: #1677ff;
  font-weight: 500;
}

.apply-hint-link:hover {
  text-decoration: underline;
}

/* ================================================================
   Responsive
   ================================================================ */
@media (max-width: 768px) {
  .detail-hero {
    padding: 64px 0 100px;
  }

  .detail-content {
    margin-top: -60px;
  }

  .detail-hero__orb--1 {
    width: 220px;
    height: 220px;
  }

  .detail-hero__orb--2 {
    width: 180px;
    height: 180px;
  }

  .detail-hero__orb--3 {
    width: 160px;
    height: 160px;
  }

  .meta-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .content-card {
    padding: 28px 24px;
    border-radius: var(--radius-lg);
  }

  .content-section-title {
    font-size: 16px;
  }

  .content-list li {
    font-size: 14px;
  }

  .apply-btn {
    max-width: 100%;
    padding: 14px 24px;
    font-size: 16px;
  }

  .not-found-card {
    padding: 40px 24px;
  }
}

/* ================================================================
   Apply Dialog & Upload
   ================================================================ */
.apply-dialog :deep(.el-dialog) {
  border-radius: var(--radius-xl);
  overflow: hidden;
}

.apply-dialog :deep(.el-dialog__header) {
  padding: 24px 28px 0;
  border-bottom: none;
}

.apply-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
}

.apply-dialog :deep(.el-dialog__body) {
  padding: 16px 28px 8px;
}

.apply-dialog :deep(.el-dialog__footer) {
  padding: 8px 28px 24px;
}

.apply-dialog :deep(.el-form-item__label) {
  font-weight: 600;
  font-size: 13px;
  color: var(--color-text);
}

.upload-input-hidden {
  display: none;
}

/* ---- Drop zone ---- */
.upload-drop-zone {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 140px;
  padding: 28px 24px;
  border: 2px dashed #d0d5dd;
  border-radius: 12px;
  background: #fafbfc;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background 0.2s ease,
    box-shadow 0.2s ease;
  user-select: none;
}

.upload-drop-zone:hover {
  border-color: #1677ff;
  background: rgba(22, 119, 255, 0.02);
  box-shadow: 0 0 0 4px rgba(22, 119, 255, 0.06);
}

.upload-drop-zone--dragover {
  border-color: #1677ff;
  background: rgba(22, 119, 255, 0.06);
  box-shadow: 0 0 0 6px rgba(22, 119, 255, 0.1);
}

.upload-drop-zone--disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

.upload-drop-icon {
  color: #98a2b3;
  margin-bottom: 12px;
  transition: color 0.2s ease;
}

.upload-drop-zone--dragover .upload-drop-icon {
  color: #1677ff;
}

.upload-drop-icon--error {
  color: #f56c6c;
}

.upload-drop-title {
  font-size: 14px;
  font-weight: 500;
  color: #344054;
  margin: 0 0 6px;
}

.upload-drop-title--error {
  color: #f56c6c;
}

.upload-drop-hint {
  font-size: 12px;
  color: #98a2b3;
  margin: 0;
  line-height: 1.5;
}

/* ---- File card (uploading / done) ---- */
.upload-file-card {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #e4e7ec;
  border-radius: 10px;
}

.upload-file-card--done {
  border-color: #d1f0e2;
  background: #f6fef9;
}

.upload-file-card__icon {
  position: relative;
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f5ff;
  border-radius: 10px;
  color: #1677ff;
}

.upload-file-card__icon--done {
  background: #ecfdf5;
  color: #12b76a;
}

.upload-file-card__check {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #12b76a;
  border-radius: 50%;
  color: #fff;
}

.upload-file-card__info {
  flex: 1;
  min-width: 0;
}

.upload-file-card__name {
  font-size: 13px;
  font-weight: 600;
  color: #101828;
  margin: 0 0 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.upload-file-card__size {
  font-size: 12px;
  color: #98a2b3;
  margin: 0 0 10px;
}

.upload-file-card__progress-bar {
  width: 100%;
  height: 4px;
  background: #f0f1f3;
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 6px;
}

.upload-file-card__progress-fill {
  width: 60%;
  height: 100%;
  background: linear-gradient(90deg, #1677ff, #4f46e5);
  border-radius: 2px;
  animation: upload-progress 1.5s ease-in-out infinite;
}

@keyframes upload-progress {
  0% { width: 20%; }
  50% { width: 80%; }
  100% { width: 95%; }
}

.upload-file-card__status {
  font-size: 12px;
  color: #1677ff;
  margin: 0;
}

.upload-file-card__status--done {
  color: #12b76a;
  font-weight: 500;
}

.upload-file-card__remove {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #98a2b3;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
}

.upload-file-card__remove:hover {
  background: #fef3f2;
  color: #f56c6c;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.applied-btn {
  background: var(--el-color-success, #67c23a) !important;
  cursor: default !important;
  opacity: 0.85 !important;
}

.applied-btn:hover {
  box-shadow: none !important;
  transform: none !important;
}

@media (max-width: 768px) {
  .upload-drop-zone {
    min-height: 120px;
    padding: 20px 16px;
  }
}
</style>
