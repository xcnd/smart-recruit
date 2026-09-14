<template>
  <div class="resume-detail-page">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <el-button :icon="ArrowLeft" text @click="$router.back()">返回</el-button>
      <el-button type="danger" plain :icon="Delete" @click="handleDelete">删除简历</el-button>
    </div>

    <div v-loading="loading">
      <!-- Header -->
      <div class="sr-section" v-if="resume">
        <div class="resume-header">
          <div class="resume-header-left">
            <el-avatar shape="square" :src="resume.avatarUrl" :style="{ background: 'var(--c-primary)', fontSize: '22px', flexShrink: 0, borderRadius: '4px', width: '72px', height: '96px' }">
              {{ resume.candidateName?.charAt(0) }}
            </el-avatar>
            <div class="resume-header-name-area">
              <h2 style="margin: 0; font-size: 20px;">{{ resume.candidateName }}</h2>
              <p style="margin: 2px 0 0; color: var(--c-text-secondary); font-size: 13px;">
                应聘: {{ displayJobTitle }}
              </p>
              <p style="margin: 2px 0 0;">
                <a v-if="resume.fileUrl" :href="downloadUrl" target="_blank" style="font-size: 12px; color: var(--c-primary); text-decoration: none;">
                  <el-icon style="vertical-align: middle; font-size: 13px;"><Download /></el-icon> 下载原始简历
                </a>
              </p>
            </div>
          </div>

          <!-- 基本信息 - 放在姓名右侧 -->
          <div v-if="basicInfoFields.length" class="resume-header-info">
            <div class="info-item" v-for="item in basicInfoFields" :key="item.label">
              <span class="info-label">{{ item.label }}：</span>
              <span class="info-value">{{ item.value }}</span>
            </div>
          </div>

          <div class="resume-score">
            <div class="resume-score-circle" :class="{ 'is-empty': !hasScore }" :style="{ '--score': resume.matchScore }">
              <svg viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="45" fill="none" stroke="#e2e8f0" stroke-width="8" />
                <circle
                  v-if="hasScore"
                  cx="50" cy="50" r="45" fill="none"
                  :stroke="scoreColor(resume.matchScore)" stroke-width="8"
                  stroke-linecap="round"
                  :stroke-dasharray="`${resume.matchScore * 2.827} 282.7`"
                  transform="rotate(-90 50 50)"
                />
              </svg>
              <span v-if="hasScore" class="resume-score-text">{{ resume.matchScore }}<small>分</small></span>
              <span v-else class="resume-score-empty">待评分</span>
            </div>
            <div style="margin-top: 8px; font-size: 13px; color: var(--c-text-secondary);">
              来源：<el-tag v-if="resume.source != null" size="small" :type="getSourceTagType(resume.source)">
                {{ getSourceLabel(resume.source) }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- Referrer Info (仅内推简历显示) -->
      <div class="sr-section" v-if="resume && resume.source === 1 && (resume.referrerName || resume.referrerDepartment || resume.referrerPosition)">
        <div class="sr-section-title">内推信息</div>
        <div class="referrer-info-grid">
          <div class="referrer-info-item" v-if="resume.referrerName">
            <span class="referrer-info-label">内推人：</span>
            <span class="referrer-info-value">{{ resume.referrerName }}</span>
          </div>
          <div class="referrer-info-item" v-if="resume.referrerDepartment">
            <span class="referrer-info-label">所在部门：</span>
            <span class="referrer-info-value">{{ resume.referrerDepartment }}</span>
          </div>
          <div class="referrer-info-item" v-if="resume.referrerPosition">
            <span class="referrer-info-label">职位：</span>
            <span class="referrer-info-value">{{ resume.referrerPosition }}</span>
          </div>
        </div>
      </div>

      <!-- Professional Skills -->
      <div class="sr-section" v-if="skillsRawText">
        <div class="sr-section-title">专业技能</div>
        <div class="skill-detail-list">
          <div v-for="(rec, idx) in skillRecords" :key="idx" class="skill-record">
            <div class="skill-record-line">
              <span class="skill-detail-bullet"></span>
              <span class="skill-detail-text">
                <strong v-if="rec.keyword" class="skill-keyword">{{ rec.keyword }}：</strong>{{ rec.first }}
              </span>
            </div>
            <div v-for="(cl, ci) in rec.cont" :key="'c' + ci" class="skill-record-line skill-record-cont">
              <span class="skill-cont-placeholder"></span>
              <span class="skill-detail-text">{{ cl }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Skill Tags -->
      <div class="sr-section" v-if="skillCategories.length">
        <div class="sr-section-title">专业标签</div>

        <div v-for="cat in skillCategories" :key="cat.name" style="margin-bottom: 12px;">
          <div class="skill-category-label">{{ cat.name }}</div>
          <div style="display: flex; flex-wrap: wrap; gap: 8px;">
            <el-tag
              v-for="skill in cat.skills"
              :key="skill"
              size="large"
              effect="plain"
              :type="cat.type as 'primary' | 'success' | 'warning' | 'danger' | 'info'"
            >
              {{ skill }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- AI Analysis -->
      <div class="sr-section" v-if="aiResult">
        <div class="sr-section-title">AI分析结果</div>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 24px;">
          <!-- Radar Chart -->
          <div>
            <div style="height: 280px;">
              <canvas ref="radarChartRef"></canvas>
            </div>
          </div>

          <!-- Suggestion and Strengths/Weaknesses -->
          <div>
            <el-tag
              :type="suggestionTagType(aiResult.suggestion)"
              size="large"
              effect="dark"
              style="margin-bottom: 16px;"
            >
              {{ suggestionLabel(aiResult.suggestion) }}
            </el-tag>

            <div style="margin-bottom: 16px;">
              <h4 style="font-size: 14px; color: var(--c-success); margin-bottom: 8px;">优势</h4>
              <ul style="list-style: disc; padding-left: 20px; color: var(--c-text-secondary);">
                <li v-for="(s, i) in aiResult.strengths" :key="'s' + i" style="margin-bottom: 4px;">{{ s }}</li>
              </ul>
            </div>

            <div>
              <h4 style="font-size: 14px; color: var(--c-danger); margin-bottom: 8px;">待提升</h4>
              <ul style="list-style: disc; padding-left: 20px; color: var(--c-text-secondary);">
                <li v-for="(w, i) in aiResult.weaknesses" :key="'w' + i" style="margin-bottom: 4px;">{{ w }}</li>
              </ul>
            </div>
          </div>
        </div>

        <div style="margin-top: 16px; padding: 12px; background: var(--c-bg); border-radius: var(--c-radius-md);">
          <p style="font-size: 13px; color: var(--c-text-secondary); line-height: 1.6;">{{ aiResult.summary }}</p>
        </div>
      </div>

      <!-- Work Experience -->
      <div class="sr-section" v-if="experiences.length">
        <div class="sr-section-title">工作经历</div>
        <el-timeline>
          <el-timeline-item
            v-for="(exp, idx) in experiences"
            :key="idx"
            :timestamp="exp.period"
            placement="top"
            :color="exp.color"
          >
            <el-card shadow="never">
              <h4 style="margin: 0 0 8px; font-size: 15px;">{{ exp.company }} · {{ exp.title }}</h4>
              <div v-if="exp.description" class="desc-records">
                <div v-for="(rec, ri) in splitDescRecords(exp.description)" :key="ri"
                     class="desc-line" :class="{
                       'desc-line-l2': getBulletMeta(rec).level === 2,
                       'desc-line-indent': getBulletMeta(rec).indented
                     }">
                  <span v-if="getBulletMeta(rec).level === 1" class="skill-detail-bullet desc-bullet"></span>
                  <span v-else-if="getBulletMeta(rec).level === 2" class="skill-detail-bullet desc-bullet-l2"></span>
                  <span v-else class="desc-bullet-placeholder"></span>
                  <span class="desc-text">
                    <template v-if="parseDescParts(getBulletMeta(rec).displayText).hasLabel">
                      <strong class="desc-label">{{ parseDescParts(getBulletMeta(rec).displayText).label }}</strong>{{ parseDescParts(getBulletMeta(rec).displayText).content }}
                    </template>
                    <template v-else>{{ getBulletMeta(rec).displayText }}</template>
                  </span>
                </div>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>

      <!-- Project Experience -->
      <div class="sr-section" v-if="projects.length">
        <div class="sr-section-title">项目经历</div>
        <el-timeline>
          <el-timeline-item
            v-for="(proj, idx) in projects"
            :key="idx"
            :timestamp="proj.period"
            placement="top"
            :color="PROJ_COLORS[idx % PROJ_COLORS.length]"
          >
            <el-card shadow="never">
              <h4 style="margin: 0 0 8px; font-size: 15px;">{{ proj.name }}<template v-if="proj.role"> · {{ proj.role }}</template></h4>
              <div v-if="proj.description" class="desc-records">
                <div v-for="(rec, ri) in splitDescRecords(proj.description)" :key="ri"
                     class="desc-line" :class="{
                       'desc-line-l2': getBulletMeta(rec).level === 2,
                       'desc-line-indent': getBulletMeta(rec).indented
                     }">
                  <span v-if="getBulletMeta(rec).level === 1" class="skill-detail-bullet desc-bullet"></span>
                  <span v-else-if="getBulletMeta(rec).level === 2" class="skill-detail-bullet desc-bullet-l2"></span>
                  <span v-else class="desc-bullet-placeholder"></span>
                  <span class="desc-text">
                    <template v-if="parseDescParts(getBulletMeta(rec).displayText).hasLabel">
                      <strong class="desc-label">{{ parseDescParts(getBulletMeta(rec).displayText).label }}</strong>{{ parseDescParts(getBulletMeta(rec).displayText).content }}
                    </template>
                    <template v-else>{{ getBulletMeta(rec).displayText }}</template>
                  </span>
                </div>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>

      <!-- Awards & Honors -->
      <div class="sr-section" v-if="awards.length">
        <div class="sr-section-title">奖项荣誉</div>
        <div v-for="(award, idx) in awards" :key="idx" class="award-item">
          <div class="award-info">
            <span class="award-name">{{ award.name }}</span>
            <span v-if="award.date" class="award-date">{{ award.date }}</span>
          </div>
          <p v-if="award.description && award.description !== award.name" class="award-desc">{{ award.description }}</p>
        </div>
      </div>

      <!-- Education -->
      <div class="sr-section" v-if="education.length">
        <div class="sr-section-title">教育背景</div>
        <div v-for="edu in education" :key="edu.school" class="edu-item">
          <div class="edu-info">
            <span style="font-weight: 600;">{{ edu.school }}</span>
            <span style="color: var(--c-text-secondary); margin-left: 12px;">{{ edu.degree }} · {{ edu.major }}</span>
          </div>
          <span style="font-size: 13px; color: var(--c-text-muted);">{{ edu.period }}</span>
        </div>
      </div>

      <!-- Self Evaluation / Summary -->
      <div class="sr-section" v-if="summary">
        <div class="sr-section-title">自我评价</div>
        <p class="summary-text">{{ summary }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Download, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Chart, registerables } from 'chart.js'
import { getResumeDetail, getAiResult, deleteResume } from '@/api/resume'
import { useRouter } from 'vue-router'
import { getSourceLabel, getSourceTagType } from '@/utils/format'
import type { ResumeVO, AiAnalysisResultVO } from '@/types/models'

Chart.register(...registerables)

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const resume = ref<ResumeVO | null>(null)
const aiResult = ref<AiAnalysisResultVO | null>(null)
const radarChartRef = ref<HTMLCanvasElement>()
let radarChart: Chart | null = null

const EXP_COLORS = ['#4f46e5', '#0ea5e9', '#059669', '#d97706']
const PROJ_COLORS = ['#8b5cf6', '#ec4899', '#f97316', '#06b6d4']

interface ExpItem { company: string; title: string; period: string; description: string; color: string }
interface ProjItem { name: string; role: string; period: string; description: string; color: string }
interface EduItem { school: string; degree: string; major: string; period: string }
interface SkillCategory { name: string; type: string; skills: string[] }

const displayJobTitle = computed(() => {
  const jt = resume.value?.jobTitle as string || ''
  if (jt) return jt
  const pc = (resume.value as Record<string, unknown>)?.parsedContent as Record<string, unknown> | undefined
  return (pc?.desiredPosition as string) || ''
})

function formatPeriod(start: string, end: string): string {
  const s = start || ''
  const e = end || '至今'
  return `${s} - ${e}`
}

/** 兼容 parser 输出的 start/end 和 LLM 输出的 startDate/endDate */
function getDateField(item: Record<string, string>, ...keys: string[]): string {
  for (const k of keys) {
    if (item[k]) return item[k]
  }
  return ''
}

/** 将描述文本按 \n\n 切分为记录列表（向后兼容旧数据按 \n 切分） */
function splitDescRecords(desc: string): string[] {
  if (desc.includes('\n\n')) {
    return desc.split('\n\n').map(r => r.replace(/^[\n\r]+|[\n\r]+$/g, '')).filter(r => r.length > 0)
  }
  return desc.split('\n').map(r => r.replace(/^[\n\r]+|[\n\r]+$/g, '')).filter(r => r.length > 0)
}

const experiences = computed<ExpItem[]>(() => {
  const pc = (resume.value as Record<string, unknown>)?.parsedContent as Record<string, unknown> | undefined
  if (pc && Array.isArray(pc.experience)) {
    return (pc.experience as Array<Record<string, string>>)
      .filter(exp => exp.company || exp.position)
      .map((exp, idx) => ({
        company: exp.company || '',
        title: exp.position || '',
        period: formatPeriod(
          getDateField(exp, 'start', 'startDate'),
          getDateField(exp, 'end', 'endDate'),
        ),
        description: exp.description || '',
        color: EXP_COLORS[idx % EXP_COLORS.length],
      }))
  }
  return []
})

const projects = computed<ProjItem[]>(() => {
  const pc = (resume.value as Record<string, unknown>)?.parsedContent as Record<string, unknown> | undefined
  if (pc && Array.isArray(pc.projects)) {
    return (pc.projects as Array<Record<string, string>>).map((proj, idx) => ({
      name: proj.company || proj.name || '',
      role: proj.position || proj.role || '',
      period: formatPeriod(
        getDateField(proj, 'start', 'startDate'),
        getDateField(proj, 'end', 'endDate'),
      ),
      description: proj.description || '',
      color: PROJ_COLORS[idx % PROJ_COLORS.length],
    }))
  }
  return []
})

const education = computed<EduItem[]>(() => {
  const pc = (resume.value as Record<string, unknown>)?.parsedContent as Record<string, unknown> | undefined
  if (pc && Array.isArray(pc.education)) {
    return (pc.education as Array<Record<string, string>>).map(edu => ({
      school: edu.school || '',
      degree: edu.degree || '',
      major: edu.major || '',
      period: formatPeriod(
        getDateField(edu, 'start', 'startDate'),
        getDateField(edu, 'end', 'endDate'),
      ),
    }))
  }
  return []
})

// ─── 技能分类 ───

const SKILL_CATEGORY_RULES: Array<{ name: string; type: string; keywords: string[] }> = [
  {
    name: '编程语言', type: 'primary',
    keywords: ['Java', 'Python', 'JavaScript', 'TypeScript', 'Go', 'Golang', 'Rust',
      'C++', 'C#', 'PHP', 'Ruby', 'Scala', 'Kotlin', 'Swift', 'Dart', 'R', 'MATLAB',
      'Perl', 'Shell', 'Bash', 'Lua', 'Groovy'],
  },
  {
    name: '后端框架', type: 'success',
    keywords: ['Spring', 'Spring Boot', 'Spring Cloud', 'Spring MVC', 'MyBatis', 'Hibernate',
      'JPA', 'Django', 'Flask', 'FastAPI', 'Express', 'Koa', 'NestJS', 'Gin', 'Laravel',
      'ThinkPHP', '.NET', 'ASP.NET', 'Entity Framework'],
  },
  {
    name: '前端框架', type: 'warning',
    keywords: ['React', 'Vue', 'Angular', 'Svelte', 'jQuery', 'Bootstrap',
      'Element', 'Ant Design', 'Tailwind CSS', 'Next.js', 'Nuxt.js', 'Vite', 'Webpack',
      'Redux', 'Vuex', 'Pinia', 'React Native', 'Flutter', 'Electron', 'Uni-app', 'Taro'],
  },
  {
    name: '数据库与缓存', type: 'danger',
    keywords: ['MySQL', 'PostgreSQL', 'Oracle', 'SQL Server', 'SQLite', 'MongoDB',
      'Redis', 'Elasticsearch', 'Cassandra', 'HBase', 'Neo4j', 'ClickHouse', 'TiDB',
      'Doris', 'Hive', 'Memcached', 'DynamoDB', 'Couchbase', 'InfluxDB'],
  },
  {
    name: '中间件与DevOps', type: 'info',
    keywords: ['Kafka', 'RabbitMQ', 'RocketMQ', 'ActiveMQ', 'Nginx', 'Tomcat',
      'Zookeeper', 'Nacos', 'Consul', 'Eureka', 'Apollo', 'Sentinel', 'Hystrix',
      'Docker', 'Kubernetes', 'K8s', 'Jenkins', 'GitLab CI', 'GitHub Actions',
      'Terraform', 'Ansible', 'Helm', 'Prometheus', 'Grafana', 'ELK',
      'Dubbo', 'gRPC', 'Netty', 'WebSocket', 'CI/CD', 'Git', 'Maven', 'Gradle',
      'AWS', 'Azure', 'GCP', '阿里云', '腾讯云', '华为云'],
  },
  {
    name: 'AI与数据科学', type: 'success',
    keywords: ['TensorFlow', 'PyTorch', 'Keras', 'Scikit-learn', 'Pandas', 'NumPy',
      'Spark', 'Flink', 'Hadoop', '机器学习', '深度学习', '自然语言处理', '计算机视觉',
      'NLP', 'CV', 'LLM', '大模型', 'Transformer', 'GPT', 'RAG', 'LangChain',
      '数据挖掘', '数据分析', '数据仓库', 'ETL'],
  },
  {
    name: '其他能力', type: 'info',
    keywords: ['团队协作', '沟通能力', '领导力', '项目管理', '敏捷开发', 'Scrum',
      'DDD', '微服务', '系统架构', '性能优化', '高并发', '分布式系统',
      '代码审查', '单元测试', '集成测试', '自动化测试', '技术文档'],
  },
]

function categorizeSkill(skill: string): string | null {
  const lower = skill.toLowerCase()
  for (const cat of SKILL_CATEGORY_RULES) {
    for (const kw of cat.keywords) {
      if (lower === kw.toLowerCase() || lower.includes(kw.toLowerCase())) {
        return cat.name
      }
    }
  }
  return null
}

/** 是否已打分（分数存在且 > 0），未打分时展示「待评分」的专业占位效果。 */
const hasScore = computed(() => {
  const score = resume.value?.matchScore
  return typeof score === 'number' && score > 0
})

const skillCategories = computed<SkillCategory[]>(() => {
  const skills = resume.value?.skills || []
  if (!skills.length) return []

  const map = new Map<string, { type: string; skills: Set<string> }>()
  // Ensure consistent category ordering
  for (const rule of SKILL_CATEGORY_RULES) {
    map.set(rule.name, { type: rule.type, skills: new Set() })
  }

  for (const skill of skills) {
    const cat = categorizeSkill(skill as string)
    if (cat && map.has(cat)) {
      map.get(cat)!.skills.add(skill as string)
    }
  }

  const result: SkillCategory[] = []
  for (const [name, data] of map) {
    if (data.skills.size > 0) {
      result.push({ name, type: data.type, skills: [...data.skills] })
    }
  }
  return result
})
const awards = computed(() => {
  const pc = (resume.value as Record<string, unknown>)?.parsedContent as Record<string, unknown> | undefined
  if (pc && Array.isArray(pc.awards)) {
    return (pc.awards as Array<Record<string, string>>)
      .filter(a => a.name)
      .map(a => ({
        name: a.name || '',
        date: a.date || a.awardDate || '',
        description: a.description || '',
      }))
  }
  return []
})

const skillsRawText = computed(() => {
  const pc = (resume.value as Record<string, unknown>)?.parsedContent as Record<string, unknown> | undefined
  return (pc?.skillsText as string) || ''
})

const summary = computed(() => {
  const pc = (resume.value as Record<string, unknown>)?.parsedContent as Record<string, unknown> | undefined
  return (pc?.summary as string) || ''
})


/**
 * 技能行首关键词（PDF 中为加粗标题，后跟具体内容）。
 * 匹配优先级：长的在前，避免 "核心框架" 被 "核心" 误匹配。
 */
const SKILL_KEYWORDS = [
  '编程语言', '脚本语言', '标记语言',
  '核心框架', '后端框架', '前端框架', '后端技术', '前端技术', 'UI框架',
  'ORM框架', 'RPC框架', '微服务框架',
  '数据库', '关系型数据库', '非关系型数据库', 'NoSQL', '缓存', '搜索引擎',
  '中间件', '消息队列', '消息中间件',
  '开发工具', '构建工具', '依赖管理', 'IDE', '编辑器',
  'DevOps', 'CI/CD', '容器化', '容器编排', '云计算', '云平台',
  '版本控制', '代码管理',
  '操作系统', 'Linux',
  '大数据', '数据处理', '流处理', '数据仓库',
  '网络协议', '通信协议',
  '测试框架', '自动化测试', '单元测试', '集成测试',
  '项目管理', '协作工具', '文档工具',
  '设计模式', '架构设计', '系统设计',
  'AI', '人工智能', '机器学习', '深度学习', '自然语言处理',
  '语言能力', '外语水平',
  '业务领域', '行业经验',
]

// 按长度倒序排列，确保长关键词优先匹配
const SORTED_SKILL_KEYWORDS = [...SKILL_KEYWORDS].sort((a, b) => b.length - a.length)
const SKILL_KW_PATTERN = new RegExp(
  '^(' + SORTED_SKILL_KEYWORDS.map(k => k.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|') + ')\\s+'
)

interface SkillRecord {
  keyword: string
  first: string
  cont: string[]
}

const skillRecords = computed<SkillRecord[]>(() => {
  // 后端用 \n\n 分隔记录，\n 表示同一条记录内的续行
  const rawText = skillsRawText.value
  if (!rawText) return []

  const records: SkillRecord[] = []

  if (rawText.includes('\n\n')) {
    // 新格式：\n\n 为记录边界
    const recordBlocks = rawText.split('\n\n')
    for (const block of recordBlocks) {
      const blockLines = block.split('\n').map(s => s.trim()).filter(s => s)
      if (!blockLines.length) continue

      const firstLine = stripBullet(blockLines[0])
      const contLines = blockLines.slice(1).map(stripBullet)

      const m = firstLine.match(SKILL_KW_PATTERN)
      if (m) {
        records.push({ keyword: m[1], first: firstLine.substring(m[0].length), cont: contLines })
      } else {
        records.push({ keyword: '', first: firstLine, cont: contLines })
      }
    }
  } else {
    // 回退：按 \n 拆分，每行可能以 bullet 开头
    const rawLines = rawText.split('\n').map(s => s.trim()).filter(s => s)
    if (!rawLines.length) return []

    for (const line of rawLines) {
      const stripped = stripBullet(line)
      // 以 bullet 开头的行视为独立记录
      const hasBullet = /^[●•◆►▸\-·○■□▲△▼▽★☆]/.test(line)
      const m = stripped.match(SKILL_KW_PATTERN)

      if (hasBullet || m) {
        // 新记录
        records.push({
          keyword: m ? m[1] : '',
          first: m ? stripped.substring(m[0].length) : stripped,
          cont: [],
        })
      } else {
        // 无 bullet 无关键词 → 续行
        if (records.length > 0) {
          records[records.length - 1].cont.push(stripped)
        } else {
          records.push({ keyword: '', first: stripped, cont: [] })
        }
      }
    }
  }

  return records
})

function stripBullet(text: string): string {
  return text.replace(/^[●•◆►▸\-·○■□▲△▼▽★☆]\s*/, '').trim()
}

/** 后端控制的列表层级标记：● = Level 1，○ = Level 2 */
const BULLET_L1 = '\u25CF' // ●
const BULLET_L2 = '\u25CB' // ○

interface BulletMeta { level: 0 | 1 | 2; displayText: string; indented: boolean }

function getBulletMeta(text: string): BulletMeta {
  // 跳过前导缩进字符（全角空格 \u3000、半角空格、制表符）
  let idx = 0
  while (idx < text.length && (text.charAt(idx) === '\u3000' || text.charAt(idx) === ' ' || text.charAt(idx) === '\t')) {
    idx++
  }
  const indentLen = idx
  const rest = text.substring(idx)

  if (rest.startsWith(BULLET_L1)) {
    const after = rest.substring(1)
    const display = after.startsWith(' ') ? after.substring(1) : after
    return { level: 1, displayText: display, indented: indentLen > 0 }
  }
  if (rest.startsWith(BULLET_L2)) {
    const after = rest.substring(1)
    const display = after.startsWith(' ') ? after.substring(1) : after
    return { level: 2, displayText: display, indented: indentLen > 0 }
  }
  return { level: 0, displayText: text, indented: false }
}

/** 项目/工作经历中的粗体标签（与后端 PROJECT_LABEL_PATTERN 保持一致） */
const DESC_LABEL_PATTERN = /^(项目(?:名称|描述|介绍|背景|角色|职责|亮点|周期|技术栈|架构)|所属公司|我的职责|职位|工作描述|使用技术|职责|技术栈|开发环境|责任描述)[：:]/

interface DescParts { hasLabel: boolean; label: string; content: string }

function parseDescParts(text: string): DescParts {
  const m = text.match(DESC_LABEL_PATTERN)
  if (m) {
    return { hasLabel: true, label: m[0], content: text.substring(m[0].length) }
  }
  return { hasLabel: false, label: '', content: text }
}

const basicInfoFields = computed(() => {
  const pc = (resume.value as Record<string, unknown>)?.parsedContent as Record<string, unknown> | undefined
  if (!pc) return []
  const items: { label: string; value: string }[] = []
  const add = (label: string, key: string) => {
    const v = (pc[key] as string) || ''
    if (v) items.push({ label, value: v })
  }
  add('邮箱', 'email')
  add('电话', 'phone')
  add('性别', 'gender')
  // 年龄：优先根据出生年月计算，其次使用解析出的年龄
  const birthDate = (pc.birthDate as string) || ''
  if (birthDate) {
    const year = parseInt(birthDate.substring(0, 4))
    if (year > 1900) {
      const age = new Date().getFullYear() - year
      items.push({ label: '年龄', value: age + '岁' })
    }
  } else {
    const parsedAge = (pc.age as string) || ''
    if (parsedAge) items.push({ label: '年龄', value: parsedAge + '岁' })
  }
  // 工作年限
  const workYears = (pc.workYears as string) || ''
  if (workYears) items.push({ label: '工作经验', value: workYears + '年' })
  add('期望职位', 'desiredPosition')
  add('所在地', 'location')
  add('户籍', 'household')
  add('政治面貌', 'politicalStatus')
  // 学历：从教育经历中提取最高学历
  const eduList = pc.education as Array<Record<string, string>> | undefined
  if (eduList && eduList.length > 0) {
    const highest = eduList[0]?.degree || ''
    if (highest) items.push({ label: '学历', value: highest })
  }
  return items
})

// ─── Download URL ───

const downloadUrl = computed(() => {
  const fileUrl = resume.value?.fileUrl as string || ''
  if (!fileUrl) return ''
  const match = fileUrl.match(/\/\/[^/]+\/\d+\/[^/]+\/(.+)/)
  if (match) {
    return `/api/v1/files/download/${match[1]}`
  }
  if (fileUrl.startsWith('/uploads/')) {
    return `/api/v1/files/download/${fileUrl.replace('/uploads/', '')}`
  }
  return fileUrl
})

function scoreColor(score: number): string {
  if (score >= 80) return '#059669'
  if (score >= 60) return '#d97706'
  return '#dc2626'
}

function suggestionTagType(s: string): 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    STRONG_HIRE: 'success', HIRE: 'success', CONSIDER: 'warning', REJECT: 'danger',
  }
  return map[s] || 'info'
}

function suggestionLabel(s: string): string {
  const map: Record<string, string> = {
    STRONG_HIRE: '强烈推荐', HIRE: '推荐录用', CONSIDER: '可考虑', REJECT: '不推荐',
  }
  return map[s] || s
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除 <strong>${resume.value?.candidateName}</strong> 的简历吗？此操作不可恢复。`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning', dangerouslyUseHTMLString: true }
    )
    await deleteResume(route.params.id as string)
    ElMessage.success('简历已删除')
    router.push('/resumes')
  } catch {
    // user cancelled or error handled by interceptor
  }
}

async function loadResume() {
  loading.value = true
  try {
    const res = await getResumeDetail(route.params.id as string)
    resume.value = res as unknown as ResumeVO
  } catch {
    resume.value = null
  }

  try {
    aiResult.value = await getAiResult(route.params.id as string)
    setTimeout(() => renderRadarChart(), 100)
  } catch {
    aiResult.value = null
  } finally {
    loading.value = false
  }
}

function renderRadarChart() {
  if (!radarChartRef.value || !aiResult.value?.dimensions?.length) return
  if (radarChart) radarChart.destroy()

  const maxScore = aiResult.value.dimensions[0]?.maxScore || 100
  radarChart = new Chart(radarChartRef.value, {
    type: 'radar',
    data: {
      labels: aiResult.value.dimensions.map(d => d.name),
      datasets: [{
        label: '候选人得分',
        data: aiResult.value.dimensions.map(d => d.score),
        backgroundColor: 'rgba(79, 70, 229, 0.15)',
        borderColor: '#4f46e5',
        borderWidth: 2,
        pointBackgroundColor: '#4f46e5',
        pointRadius: 4,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        r: {
          min: 0,
          max: maxScore,
          ticks: { stepSize: maxScore / 5, font: { size: 10 } },
          pointLabels: { font: { size: 12 } },
        },
      },
      plugins: { legend: { display: false } },
    },
  })
}

onMounted(() => loadResume())
onBeforeUnmount(() => { if (radarChart) radarChart.destroy() })
</script>

<style scoped>
.resume-header {
  display: flex;
  align-items: flex-start;
  gap: 32px;
}

.resume-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.resume-header-name-area {
  min-width: 120px;
}

.resume-header-info {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 32px;
  align-self: center;
}

.info-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
  font-size: 13px;
  line-height: 2;
}

.info-label {
  color: #6b7280;
  flex-shrink: 0;
  font-weight: 400;
}

.info-value {
  color: #1f2937;
  font-weight: 600;
}

.resume-score {
  flex-shrink: 0;
}

.resume-score-circle {
  width: 96px;
  height: 96px;
  position: relative;
}

.resume-score-circle svg {
  width: 100%;
  height: 100%;
}

.resume-score-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  color: var(--c-text);
}

.resume-score-text small {
  font-size: 12px;
  font-weight: 400;
  color: var(--c-text-secondary);
}

/* 未打分时的专业占位效果 */
.resume-score-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 500;
  color: var(--c-text-muted, #94a3b8);
  letter-spacing: 1px;
}

.resume-score-circle.is-empty {
  opacity: 0.9;
}

.skill-detail-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.skill-record {
  margin-bottom: 2px;
  padding: 5px 8px;
  border-radius: 6px;
  transition: background 0.15s ease;
}

.skill-record:hover {
  background: rgba(79, 70, 229, 0.04);
}

.skill-record-line {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.skill-record-cont {
  /* 续行无 bullet */
}

.skill-cont-placeholder {
  flex-shrink: 0;
  width: 7px;
}

.skill-detail-bullet {
  flex-shrink: 0;
  width: 7px;
  height: 7px;
  margin-top: 9px;
  border-radius: 1.5px;
  background: linear-gradient(135deg, #4f46e5 0%, #818cf8 100%);
}

.skill-keyword {
  font-weight: 700;
  color: #1e293b;
}

.skill-detail-text {
  font-size: 14px;
  color: #374151;
  line-height: 1.85;
}

/* 工作/项目经历描述记录样式 */
.desc-records {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.desc-line {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.desc-bullet {
  margin-top: 6px;
}

.desc-bullet-l2 {
  margin-top: 6px;
  background: linear-gradient(135deg, #cbd5e1 0%, #94a3b8 100%);
}

.desc-bullet-placeholder {
  flex-shrink: 0;
  width: 7px;
}

.desc-line-l2 {
  padding-left: 2em;
}

.desc-line-indent {
  padding-left: 2em;
}

.desc-label {
  font-weight: 700;
  color: #1e293b;
}

.desc-text {
  font-size: 13px;
  color: var(--c-text-secondary);
  line-height: 1.8;
  white-space: pre-line;
  flex: 1;
}

.skill-category-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text-secondary);
  margin-bottom: 6px;
  padding-left: 2px;
}

.edu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--c-border-light);
}

.edu-item:last-child { border-bottom: none; }

.award-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--c-border-light);
}

.award-item:last-child { border-bottom: none; }

.award-info {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
}

.award-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--c-text);
}

.award-date {
  font-size: 13px;
  color: var(--c-text-muted);
  flex-shrink: 0;
}

.award-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--c-text-secondary);
  line-height: 1.6;
}

.summary-text {
  font-size: 14px;
  color: #374151;
  line-height: 1.5;
  white-space: pre-line;
  margin: 0;
}

.referrer-info-grid {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
}

.referrer-info-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 14px;
}

.referrer-info-label {
  color: #6b7280;
  flex-shrink: 0;
}

.referrer-info-value {
  color: #1f2937;
  font-weight: 600;
}
</style>
