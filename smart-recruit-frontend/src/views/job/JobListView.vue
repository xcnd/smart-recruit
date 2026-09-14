<template>
  <div class="job-list-page">
    <div class="sr-page-header">
      <h1>职位管理</h1>
      <p>管理和发布招聘职位</p>
    </div>

    <!-- Filter & Toolbar -->
    <div class="sr-section">
      <div class="sr-toolbar">
        <div class="sr-filter-bar">
          <el-input
            v-model="filters.title"
            placeholder="搜索职位名称"
            :prefix-icon="Search"
            clearable
            style="width: 240px"
            @change="handleSearch"
          />
          <el-tree-select
            v-model="filters.departmentId"
            :data="deptOptions"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="部门"
            clearable
            check-strictly
            style="width: 180px"
            @change="handleSearch"
          />
          <el-select
            v-model="filters.status"
            placeholder="状态"
            clearable
            @change="handleSearch"
          >

            <el-option label="已发布" :value="1" />
            <el-option label="草稿" :value="0" />
            <el-option label="已关闭" :value="3" />
          </el-select>
          <el-select
            v-model="filters.type"
            placeholder="类型"
            clearable
            @change="handleSearch"
          >

            <el-option label="全职" :value="0" />
            <el-option label="实习" :value="2" />
            <el-option label="合同制" :value="3" />
            <el-option label="兼职" :value="1" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="发布开始"
            end-placeholder="发布结束"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 260px"
            @change="handleDateChange"
          />
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建职位</el-button>
      </div>

      <!-- Table -->
      <el-table
        v-loading="loading"
        :data="jobs"
        stripe
        style="width: 100%"
        empty-text="暂无职位数据"
      >
        <el-table-column type="index" label="序号" width="60" :index="indexMethod" />
        <el-table-column prop="title" label="职位名称" min-width="180">
          <template #default="{ row }">
            <span class="job-title-link" @click="$router.push(`/jobs/${row.id}`)">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="部门" width="120">
          <template #default="{ row }">{{ getDepartmentLabel(row.departmentId) }}</template>
        </el-table-column>
        <el-table-column label="招聘人数" width="90">
          <template #default="{ row }">{{ row.headCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="location" label="地点" width="120" />
        <el-table-column label="经验" width="90">
          <template #default="{ row }">{{ getExperienceLabel(row.level) }}</template>
        </el-table-column>
        <el-table-column label="薪资" width="150">
          <template #default="{ row }">
            <span style="color: var(--c-danger)">{{ (row.salaryMin / 1000).toFixed(0) }}K-{{ (row.salaryMax / 1000).toFixed(0) }}K</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 0 || row.type === 'FULL_TIME' ? 'primary' : 'info'">
              {{ getJobTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="jobStatusType(row.status)">
              {{ jobStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发布时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="createdBy" label="发布人" width="120" />
        <el-table-column label="操作" width="220" fixed="right" class-name="ops-col">
          <template #default="{ row }">
            <el-button size="small" text type="info" @click="$router.push(`/jobs/${row.id}`)">查看</el-button>
            <el-button size="small" text type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              size="small"
              text
              type="success"
              :loading="statusActionId === row.id"
              @click="publishJob(row)"
              v-if="row.status !== 'PUBLISHED'"
            >
              发布
            </el-button>
            <el-button
              size="small"
              text
              type="danger"
              :loading="statusActionId === row.id"
              @click="closeJob(row)"
              v-if="row.status === 'PUBLISHED'"
            >
              关闭
            </el-button>
            <el-popconfirm
              title="确定删除该职位吗？"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button size="small" text type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination Footer -->
      <div class="sr-pagination-footer">
        <span class="sr-pagination-info">共 <strong>{{ total }}</strong> 个职位</span>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingJob ? '编辑职位' : '新建职位'"
      width="780px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" label-position="left">
        <el-form-item label="职位名称" prop="title">
          <el-input v-model="form.title" placeholder="请输入职位名称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="部门" prop="departmentId">
              <el-tree-select
                v-model="form.departmentId"
                :data="deptOptions"
                :props="{ label: 'name', value: 'id', children: 'children' }"
                placeholder="请选择部门"
                check-strictly
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职级" prop="level">
              <el-select v-model="form.level" placeholder="请选择职级" style="width: 100%">
                <el-option v-for="lv in levels" :key="lv" :label="lv" :value="lv" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="招聘人数" prop="headCount">
              <el-input-number v-model="form.headCount" :min="1" :max="99" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最低薪资(K)" prop="salaryMin">
              <el-input-number v-model="form.salaryMin" :min="0" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最高薪资(K)" prop="salaryMax">
              <el-input-number v-model="form.salaryMax" :min="0" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="工作地点" prop="location">
              <el-input v-model="form.location" placeholder="如：北京" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="经验要求" prop="experience">
              <el-select v-model="form.experience" placeholder="请选择" style="width: 100%">
                <el-option label="应届生" value="FRESH" />
                <el-option label="1-3年" value="JUNIOR" />
                <el-option label="3-5年" value="MIDDLE" />
                <el-option label="5-10年" value="SENIOR" />
                <el-option label="10年以上" value="EXPERT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工作类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
                <el-option label="全职" value="FULL_TIME" />
                <el-option label="实习" value="INTERNSHIP" />
                <el-option label="合同制" value="CONTRACT" />
                <el-option label="兼职" value="PART_TIME" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="学历要求">
              <el-select v-model="form.educationRequired" placeholder="不限" clearable style="width: 100%">
                <el-option label="不限" :value="undefined" />
                <el-option label="高中及以上" :value="0" />
                <el-option label="大专及以上" :value="1" />
                <el-option label="本科及以上" :value="2" />
                <el-option label="硕士及以上" :value="3" />
                <el-option label="博士" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最小年龄">
              <el-input-number v-model="form.ageMin" :min="16" :max="70" placeholder="不限" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大年龄">
              <el-input-number v-model="form.ageMax" :min="16" :max="70" placeholder="不限" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="职位描述" prop="description">
          <div style="width: 100%;">
            <div style="display: flex; justify-content: flex-end; margin-bottom: 6px; gap: 8px;">
              <el-button
                size="small"
                type="primary"
                plain
                :loading="jdGenerating"
                @click="handleAiGenerateJd"
              >
                <el-icon style="margin-right: 4px;"><MagicStick /></el-icon>
                AI 生成 JD
              </el-button>
              <el-popover ref="descPopoverRef" placement="bottom-end" :width="600" trigger="click">
                <template #reference>
                  <el-button size="small" text type="primary">查看描述示例</el-button>
                </template>
                <div style="max-height: 400px; overflow-y: auto; padding: 4px 0;">
                  <div
                    v-for="tpl in descTemplates"
                    :key="tpl.title"
                    style="padding: 10px 12px; border: 1px solid #ebeef5; border-radius: 6px; margin-bottom: 8px; cursor: pointer;"
                    class="desc-tpl-item"
                    @click="applyDescTemplate(tpl)"
                  >
                    <div style="font-weight: 600; margin-bottom: 4px; font-size: 13px;">{{ tpl.title }}</div>
                    <div style="font-size: 12px; color: #909399; line-height: 1.6; white-space: pre-wrap; max-height: 72px; overflow: hidden;">{{ tpl.content }}</div>
                  </div>
                </div>
              </el-popover>
            </div>
            <div v-if="jdGenerating" class="ai-jd-generating">
              <div class="ai-jd-gen-head">
                <div class="ai-jd-gen-icon">
                  <el-icon :size="24"><MagicStick /></el-icon>
                </div>
                <div>
                  <div class="ai-jd-gen-title">AI 正在生成职位描述</div>
                  <div class="ai-jd-gen-sub">正在为「{{ form.title || '该岗位' }}」生成职责、要求与加分项</div>
                </div>
              </div>
              <div class="ai-jd-gen-steps">
                <div
                  v-for="(s, i) in jdGenSteps"
                  :key="s"
                  class="ai-jd-gen-step"
                  :class="{ active: i === jdGenStep, done: i < jdGenStep }"
                >
                  <span class="ai-jd-gen-dot"></span>
                  <span class="ai-jd-gen-step-text">{{ s }}</span>
                </div>
              </div>
              <div class="ai-jd-gen-bar">
                <div class="ai-jd-gen-bar-fill" :style="{ width: jdGenPercent + '%' }"></div>
              </div>
              <div class="ai-jd-gen-bar-text">{{ Math.round(jdGenPercent) }}%</div>
            </div>
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="8"
              :disabled="jdGenerating"
              placeholder="请输入职位描述"
            />
          </div>
        </el-form-item>
        <el-form-item label="技能要求" prop="skills">
          <el-select
            v-model="form.skills"
            multiple
            filterable
            allow-create
            placeholder="请选择或输入技能"
            style="width: 100%"
          >
            <el-option label="Java" value="Java" />
            <el-option label="Python" value="Python" />
            <el-option label="React" value="React" />
            <el-option label="Vue" value="Vue" />
            <el-option label="TypeScript" value="TypeScript" />
            <el-option label="Spring Boot" value="Spring Boot" />
            <el-option label="Docker" value="Docker" />
            <el-option label="Kubernetes" value="Kubernetes" />
            <el-option label="SQL" value="SQL" />
            <el-option label="机器学习" value="机器学习" />
            <el-option label="深度学习" value="深度学习" />
            <el-option label="NLP" value="NLP" />
            <el-option label="大数据" value="大数据" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editingJob ? '保存修改' : '创建职位' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Plus, MagicStick } from '@element-plus/icons-vue'
import { getJobs, createJob, updateJob, deleteJob, updateJobStatus, getJobById, startAiGenerateJd, getAiGenerateJdTask } from '@/api/job'
import { getDepartments } from '@/api/system'
import { formatDate, formatDateTime, getStatusLabel, getStatusType, getExperienceLabel, getJobTypeLabel, getDepartmentLabel } from '@/utils/format'
import type { JobVO, JobCreateDTO, DepartmentTreeVO } from '@/types/models'

const loaded = ref(false)
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const editingJob = ref<JobVO | null>(null)
const formRef = ref<FormInstance>()
const jdGenerating = ref(false)
/** AI 生成 JD 的步骤进度展示。 */
const jdGenStep = ref(0)
const jdGenSteps = ['分析岗位信息', '生成岗位职责', '生成任职要求', '生成加分项']
let jdGenStepTimer: ReturnType<typeof setInterval> | null = null
/** 进度条数值（时间驱动，平滑逼近 95%，与任务完成速度解耦）。 */
const jdGenPercent = ref(24)
let jdGenPercentTimer: ReturnType<typeof setInterval> | null = null

function startJdStepTimer() {
  stopJdStepTimer()
  jdGenStepTimer = setInterval(() => {
    if (jdGenStep.value < jdGenSteps.length - 1) {
      jdGenStep.value += 1
    }
  }, 800)
}

function stopJdStepTimer() {
  if (jdGenStepTimer) {
    clearInterval(jdGenStepTimer)
    jdGenStepTimer = null
  }
}

function startJdPercentTimer() {
  stopJdPercentTimer()
  jdGenPercent.value = 24
  jdGenPercentTimer = setInterval(() => {
    jdGenPercent.value = Math.min(95, jdGenPercent.value + (95 - jdGenPercent.value) * 0.08 + 1.2)
  }, 140)
}

function stopJdPercentTimer() {
  if (jdGenPercentTimer) {
    clearInterval(jdGenPercentTimer)
    jdGenPercentTimer = null
  }
}

function resetJdGenerating() {
  stopJdStepTimer()
  stopJdPercentTimer()
  jdGenerating.value = false
  jdGenStep.value = 0
  jdGenPercent.value = 24
}
/** 正在执行发布/关闭操作的职位 ID（按钮 loading 态）。 */
const statusActionId = ref<string | number | null>(null)
let jdTaskId = ''
let jdPollTimer: ReturnType<typeof setInterval> | null = null

const page = ref(1)
const size = ref(10)
const total = ref(0)
const jobs = ref<JobVO[]>([])

const filters = reactive<Record<string, unknown>>({
  title: undefined,
  departmentId: undefined,
  status: undefined,
  type: undefined,
  startDate: undefined,
  endDate: undefined,
})

const dateRange = ref<[string, string] | null>(null)

function handleDateChange(val: [string, string] | null) {
  if (val) {
    filters.startDate = val[0]
    filters.endDate = val[1]
  } else {
    filters.startDate = undefined
    filters.endDate = undefined
  }
  handleSearch()
}

const levels = ['P5', 'P6', 'P7', 'P8', 'P9']

const descTemplates = [
  {
    title: 'AI 应用开发工程师',
    content: '【岗位职责】\n1. 负责AI产品（智能Agent、RAG知识库、对话机器人等）的后端服务设计与开发\n2. 集成大语言模型（LLM）API，设计并优化Prompt Engineering和模型调用链路\n3. 搭建RAG检索增强生成系统，包括向量数据库、文档切分、语义检索等模块\n4. 开发AI Agent的推理和工具调用（Function Calling）能力，实现复杂任务自动编排\n5. 构建模型服务的监控和评测体系，保障线上AI服务质量与稳定性\n6. 跟踪前沿AI技术（Agent框架、MCP协议、多模态等），推动技术落地到业务场景\n\n【任职要求】\n1. 计算机相关专业本科及以上学历，3年以上后端开发经验，1年以上AI应用开发经验\n2. 精通Python或Java，熟悉FastAPI/Spring Boot等服务框架\n3. 熟悉LangChain/LlamaIndex等LLM应用框架，有实际项目落地经验\n4. 了解向量数据库（Milvus/Pinecone/Weaviate）、Embedding模型的选型与使用\n5. 熟悉OpenAI/Claude/DeepSeek等主流大模型API的调用与调优\n6. 有Agent开发经验（Coze/Dify/AutoGPT等平台或自研Agent框架）优先\n7. 具备强烈的好奇心和快速学习能力，能跟进AI技术快速迭代',
  },
  {
    title: 'Java 后端开发工程师',
    content: '【岗位职责】\n1. 负责核心业务系统的后端设计与开发，确保系统高可用、高性能\n2. 参与系统架构设计和技术方案评审，推动技术方案落地\n3. 编写高质量代码，进行代码评审，保障代码质量和系统稳定性\n4. 参与微服务架构设计与演进，持续优化系统性能\n5. 与产品、前端、测试等团队紧密协作，按时交付高质量产品\n\n【任职要求】\n1. 计算机相关专业本科及以上学历，3年以上Java开发经验\n2. 精通Java语言，熟悉JVM原理及性能调优\n3. 熟练掌握Spring Boot、Spring Cloud、MyBatis等主流框架\n4. 熟悉MySQL、Redis、消息队列等中间件的使用和优化\n5. 具备良好的系统设计能力和问题分析能力\n6. 有大规模分布式系统开发经验者优先',
  },
  {
    title: 'Java 实习生（可转正）',
    content: '【岗位职责】\n1. 参与业务系统后端模块的设计与开发，完成指派的开发任务\n2. 在导师指导下学习并实践Java开发规范和代码评审流程\n3. 参与技术文档撰写和维护，包括接口文档、设计文档等\n4. 协助完成单元测试编写和系统集成测试\n5. 参与团队技术分享和Code Review，持续提升工程能力\n\n【任职要求】\n1. 2026届或2027届本科及以上学历，计算机、软件工程等相关专业\n2. 扎实的计算机基础：数据结构、算法、操作系统、计算机网络\n3. 熟练掌握Java编程语言，了解面向对象设计原则\n4. 了解Spring Boot、MySQL、Redis等主流技术栈的基本使用\n5. 有实际项目经验或开源贡献者优先,参加过ACM/蓝桥杯等竞赛优先\n6. 实习期不少于3个月，每周至少4天，表现优异可转正\n7. 具备良好的沟通能力和团队协作精神，学习意愿强',
  },
  {
    title: 'Java 高级开发工程师',
    content: '【岗位职责】\n1. 负责核心业务领域的系统设计与技术方案制定，主导重点项目的技术落地\n2. 深入理解业务需求，识别技术风险，输出高质量技术方案和架构设计文档\n3. 负责高并发、高可用系统的性能优化，包括JVM调优、SQL优化、缓存策略等\n4. 制定团队编码规范和技术标准，指导初中级工程师成长\n5. 推动技术债务治理和系统重构，持续提升代码质量和系统可维护性\n6. 跟进前沿技术发展，推动适合业务场景的技术创新和落地\n\n【任职要求】\n1. 计算机相关专业本科及以上学历，5年以上Java开发经验\n2. 精通Java及JVM底层原理，有丰富的线上问题排查和性能调优经验\n3. 对Spring Cloud Alibaba、gRPC、Service Mesh等微服务技术栈有深入理解\n4. 熟练掌握MySQL分库分表、Redis集群、RocketMQ/Kafka等高可用方案\n5. 有DDD领域驱动设计、CQRS、事件驱动架构等设计模式实践经验\n6. 具备优秀的系统抽象能力和复杂问题拆解能力\n7. 有10万+QPS系统设计经验或大规模数据迁移经验者优先',
  },
  {
    title: 'Java 架构师/技术专家',
    content: '【岗位职责】\n1. 负责业务线整体技术架构规划与设计，制定技术演进路线和实施方案\n2. 主导重大技术项目的架构评审和方案决策，把控技术方向和质量标准\n3. 设计高可用、弹性可扩展的分布式系统，保障核心业务99.99%可用性\n4. 建立和完善技术规范和工程最佳实践，推动技术文化建设\n5. 识别并解决业务发展中的技术瓶颈，主导跨团队技术攻坚\n6. 指导和培养高级工程师，建设有战斗力的技术团队\n\n【任职要求】\n1. 计算机相关专业本科及以上学历，8年以上Java开发经验，3年以上架构设计经验\n2. 精通分布式系统理论（CAP、BASE、Paxos/Raft），有大规模分布式系统实战经验\n3. 对微服务、容器化、Service Mesh、Serverless等技术有深入理解和实践经验\n4. 精通MySQL/PostgreSQL、Redis、Elasticsearch、Kafka等主流中间件的原理和调优\n5. 有异地多活、单元化架构、全链路压测等高可用架构实战经验\n6. 能够从业务视角思考技术问题，具备将复杂业务需求转化为技术方案的能力\n7. 有开源项目贡献或技术博客/公众号影响力者优先\n8. 具备跨团队协作和大型项目管控能力，优秀的沟通表达和影响力',
  },
  {
    title: '前端开发工程师',
    content: '【岗位职责】\n1. 负责公司核心产品的前端架构设计与功能开发\n2. 参与前端技术选型和基础设施建设，包括组件库、脚手架、构建工具等\n3. 持续优化产品性能和用户体验，提升页面加载速度和交互流畅度\n4. 编写可维护的前端代码，制定前端开发规范和最佳实践\n5. 关注前端技术发展趋势，推动团队技术进步\n\n【任职要求】\n1. 计算机相关专业本科及以上学历，3年以上前端开发经验\n2. 精通HTML5、CSS3、JavaScript/TypeScript，熟悉ES6+规范\n3. 熟练掌握Vue或React框架及其生态工具\n4. 熟悉前端工程化，了解Webpack/Vite等构建工具原理\n5. 具备良好的审美能力和用户体验意识\n6. 有大型SPA应用或移动端开发经验者优先',
  },
  {
    title: '产品经理',
    content: '【岗位职责】\n1. 负责产品需求调研、用户分析及竞品分析，输出产品需求文档(PRD)\n2. 制定产品路线图和版本迭代计划，推动产品持续优化\n3. 协调设计、研发、测试等团队，确保产品高质量按时上线\n4. 跟踪产品上线后的数据表现，通过数据分析驱动产品优化决策\n5. 深入理解业务场景，挖掘用户核心诉求，提出创新性的产品方案\n\n【任职要求】\n1. 本科及以上学历，3年以上互联网产品经理经验\n2. 具备优秀的逻辑思维能力和数据分析能力\n3. 熟练使用Axure、Figma等产品设计工具\n4. 出色的沟通协调能力和项目推动能力\n5. 有B端SaaS产品或企业服务产品经验者优先',
  },
  {
    title: '数据分析师',
    content: '【岗位职责】\n1. 负责业务数据指标体系的搭建与维护，输出日常数据监控报表\n2. 深入分析业务数据，挖掘增长机会点和潜在问题，输出分析报告\n3. 与业务团队紧密合作，通过数据驱动业务决策和策略优化\n4. 设计和实施A/B实验，评估产品迭代和运营策略的效果\n5. 建设数据可视化看板，提升团队数据化运营能力\n\n【任职要求】\n1. 统计学、数学、计算机等相关专业本科及以上学历\n2. 2年以上数据分析相关工作经验\n3. 熟练掌握SQL，能够独立完成复杂数据查询和数据处理\n4. 熟练使用Python/R进行数据分析和建模\n5. 具备良好的业务理解能力和数据敏感度\n6. 有用户增长分析或推荐系统经验者优先',
  },
  {
    title: 'UI/UX 设计师',
    content: '【岗位职责】\n1. 负责产品整体视觉风格定义及界面设计，输出高质量UI设计稿\n2. 参与产品需求讨论，从设计角度提出用户体验优化方案\n3. 制定和维护设计规范，建设组件化设计系统(Design System)\n4. 跟进设计还原度，与前端协作确保设计效果准确落地\n5. 持续关注设计趋势，推动产品设计语言迭代升级\n\n【任职要求】\n1. 设计相关专业本科及以上学历，3年以上UI/UX设计经验\n2. 精通Figma/Sketch等设计工具，熟悉原型设计流程\n3. 具备扎实的视觉设计功底和良好的交互设计能力\n4. 理解前端基础知识，能与开发团队高效协作\n5. 有B端产品设计经验者优先\n6. 请附带作品集或作品链接',
  },
  {
    title: 'DevOps/SRE 工程师',
    content: '【岗位职责】\n1. 负责公司云原生基础设施的规划、建设和日常运维\n2. 设计并实现CI/CD流水线，提升研发效率和交付质量\n3. 建设监控告警体系和日志系统，保障线上服务SLA\n4. 参与容器化平台建设与Kubernetes集群管理\n5. 推动基础设施即代码(IaC)实践，提升运维自动化水平\n6. 处理线上故障并输出故障复盘报告，持续优化系统稳定性\n\n【任职要求】\n1. 计算机相关专业本科及以上学历，3年以上运维开发经验\n2. 精通Linux系统管理，熟悉Shell/Python脚本编程\n3. 熟练掌握Docker、Kubernetes容器编排技术\n4. 熟悉至少一种云平台（AWS/Azure/阿里云）\n5. 熟悉Prometheus、Grafana、ELK等监控日志技术栈\n6. 具备良好的故障排查能力和安全意识',
  },
]

const descPopoverRef = ref<InstanceType<typeof import('element-plus').ElPopover>>()

function applyDescTemplate(tpl: { title: string; content: string }) {
  form.description = tpl.content
  descPopoverRef.value?.hide()
}

/** 经验枚举 → 中文文案（AI JD 生成入参）。 */
const EXP_LABEL: Record<string, string> = {
  FRESH: '应届生',
  JUNIOR: '1-3年',
  MIDDLE: '3-5年',
  SENIOR: '5-10年',
  EXPERT: '10年以上',
}

/** 根据部门 ID 递归查找部门名称。 */
function findDeptName(id: string | number | undefined, nodes: DepartmentTreeVO[] = deptOptions.value): string {
  if (id === undefined || id === null || id === '') return ''
  for (const node of nodes) {
    if (String(node.id) === String(id)) return node.name
    if (node.children?.length) {
      const found = findDeptName(id, node.children)
      if (found) return found
    }
  }
  return ''
}

/** AI 生成 JD：异步启动任务并轮询结果，完成后回填职位描述（避免 LLM 调用超时）。 */
async function handleAiGenerateJd() {
  if (!form.title?.trim()) {
    ElMessage.warning('请先填写职位名称')
    return
  }
  stopJdPolling()
  jdGenerating.value = true
  jdGenStep.value = 0
  startJdStepTimer()
  startJdPercentTimer()
  try {
    // 表单类型为 JobCreateDTO，但运行时字段为 experience（与模板一致），此处安全取值
    const experience = (form as unknown as { experience?: string }).experience || 'MIDDLE'
    const task = await startAiGenerateJd({
      title: form.title.trim(),
      department: findDeptName(form.departmentId),
      experience: EXP_LABEL[experience] || '',
      location: form.location || '',
      skills: form.skills || [],
    })
    jdTaskId = task.taskId
    startJdPolling()
  } catch {
    resetJdGenerating()
    ElMessage.error('AI 生成任务启动失败，请稍后重试')
  }
}

/** 轮询异步 JD 生成任务（1.2s 间隔）。 */
function startJdPolling() {
  stopJdPolling()
  pollJdTask()
  jdPollTimer = setInterval(pollJdTask, 1200)
}

function stopJdPolling() {
  stopJdStepTimer()
  if (jdPollTimer) {
    clearInterval(jdPollTimer)
    jdPollTimer = null
  }
}

async function pollJdTask() {
  if (!jdTaskId) return
  try {
    const task = await getAiGenerateJdTask(jdTaskId)
    if (task.status === 'COMPLETED') {
      if (task.description) {
        form.description = task.description
        ElMessage.success('AI 已生成职位描述，请核对后保存')
      } else {
        ElMessage.warning('AI 生成内容为空，请手动填写')
      }
      resetJdGenerating()
      stopJdPolling()
    } else if (task.status === 'FAILED') {
      resetJdGenerating()
      stopJdPolling()
      ElMessage.error(task.message || 'AI 生成失败，请稍后重试')
    }
    // PENDING / PROCESSING 继续轮询
  } catch {
    // 轮询失败继续等待下一次
  }
}

function indexMethod(index: number) {
  return (page.value - 1) * size.value + index + 1
}

const deptOptions = ref<DepartmentTreeVO[]>([])

async function loadDepartments() {
  try {
    deptOptions.value = await getDepartments()
  } catch {
    deptOptions.value = []
  }
}

const form = reactive<JobCreateDTO>({
  title: '',
  departmentId: '',
  level: 'P6',
  headCount: 1,
  salaryMin: 15,
  salaryMax: 30,
  location: '北京',
  experience: 'MIDDLE',
  type: 'FULL_TIME',
  description: '',
  requirements: '',
  skills: [],
  educationRequired: undefined,
  ageMin: undefined,
  ageMax: undefined,
})

const formRules: FormRules = {
  title: [{ required: true, message: '请输入职位名称', trigger: 'blur' }],
  departmentId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  level: [{ required: true, message: '请选择职级', trigger: 'change' }],
  headCount: [{ required: true, message: '请输入招聘人数', trigger: 'blur' }],
  salaryMin: [{ required: true, message: '请输入最低薪资', trigger: 'blur' }],
  salaryMax: [{ required: true, message: '请输入最高薪资', trigger: 'blur' }],
  type: [{ required: true, message: '请选择工作类型', trigger: 'change' }],
}

// 与后端 RecruitmentEnums.JobStatus 对齐：0=草稿 1=已发布 2=暂停招聘 3=已关闭
const STATUS_MAP: Record<number, string> = { 0: 'DRAFT', 1: 'PUBLISHED', 2: 'PAUSED', 3: 'CLOSED' }

function mapJobRow(raw: Record<string, unknown>): Record<string, unknown> {
  const status = raw.status as number
  return {
    ...raw,
    status: STATUS_MAP[status] ?? status,
  }
}

async function loadJobs() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: page.value, size: size.value }
    for (const [k, v] of Object.entries(filters)) {
      if (v !== undefined && v !== null && v !== '') {
        params[k] = v
      }
    }
    const res = await getJobs(params)
    jobs.value = (res.records || []).map(mapJobRow) as unknown as JobVO[]
    total.value = Number(res.total)
  } catch {
    jobs.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  if (page.value === 1) { loadJobs() } else { page.value = 1 /* watcher triggers loadJobs */ }
}

// React to v-model page/size changes (modern API, no deprecated event handlers)
watch([page, size], () => {
  if (loaded.value) loadJobs()
})

function openCreateDialog() {
  editingJob.value = null
  Object.assign(form, {
    title: '', departmentId: deptOptions.value[0]?.id || '', level: 'P6', headCount: 1, salaryMin: 15, salaryMax: 30,
    location: '北京', experience: 'MIDDLE', type: 'FULL_TIME', description: '', requirements: '', skills: [],
    educationRequired: undefined, ageMin: undefined, ageMax: undefined,
  })
  dialogVisible.value = true
}

const EXP_TO_STR: Record<number, string> = { 0: 'FRESH', 1: 'JUNIOR', 2: 'MIDDLE', 3: 'SENIOR', 4: 'EXPERT', 5: 'EXPERT' }
const STR_TO_EXP: Record<string, number> = { FRESH: 0, JUNIOR: 1, MIDDLE: 2, SENIOR: 3, EXPERT: 4 }
const TYPE_TO_STR: Record<number, string> = { 0: 'FULL_TIME', 1: 'PART_TIME', 2: 'INTERNSHIP', 3: 'CONTRACT' }
const STR_TO_TYPE: Record<string, number> = { FULL_TIME: 0, PART_TIME: 1, INTERNSHIP: 2, CONTRACT: 3 }

function openEditDialog(job: JobVO) {
  editingJob.value = job
  Object.assign(form, {
    title: job.title,
    departmentId: job.departmentId,
    level: 'P6',
    headCount: job.headCount,
    salaryMin: job.salaryMin / 1000,
    salaryMax: job.salaryMax / 1000,
    location: job.location,
    experience: EXP_TO_STR[job.level as unknown as number] || 'MIDDLE',
    type: TYPE_TO_STR[job.type as unknown as number] || 'FULL_TIME',
    description: '',
    requirements: job.requirements || '',
    skills: [...(job.skills || [])],
    educationRequired: (job as Record<string, unknown>).educationRequired as number | undefined,
    ageMin: (job as Record<string, unknown>).ageMin as number | undefined,
    ageMax: (job as Record<string, unknown>).ageMax as number | undefined,
  })
  dialogVisible.value = true
  getJobById(String(job.id)).then(detail => {
    if (editingJob.value?.id === job.id) {
      form.description = (detail as Record<string, unknown>).description as string || ''
    }
  }).catch(() => {})
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const data = {
        ...form,
        salaryMin: form.salaryMin * 1000,
        salaryMax: form.salaryMax * 1000,
        level: STR_TO_EXP[form.experience] ?? 2,
        type: STR_TO_TYPE[form.type] ?? 0,
      }
      if (editingJob.value) {
        await updateJob(editingJob.value.id, data)
        ElMessage.success('职位更新成功')
      } else {
        await createJob(data)
        ElMessage.success('职位创建成功')
      }
      dialogVisible.value = false
      loadJobs()
    } catch {
      ElMessage.error('操作失败')
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(id: number) {
  try {
    await deleteJob(id)
    ElMessage.success('删除成功')
    loadJobs()
  } catch {
    ElMessage.error('删除失败')
  }
}

const JOB_STATUS_TEXT: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  PAUSED: '暂停招聘',
  CLOSED: '已关闭',
}

function jobStatusLabel(status: number | string): string {
  return JOB_STATUS_TEXT[String(status)] || getStatusLabel(status)
}

function jobStatusType(status: number | string): 'success' | 'warning' | 'danger' | 'info' | '' {
  const s = String(status)
  if (s === 'PUBLISHED') return 'success'
  if (s === 'PAUSED') return 'warning'
  if (s === 'CLOSED') return 'danger'
  if (s === 'DRAFT') return 'info'
  return getStatusType(status)
}

/** 发布职位（草稿/暂停/关闭 → 已发布）。 */
async function publishJob(job: JobVO) {
  statusActionId.value = job.id
  try {
    await updateJobStatus(job.id, 1)
    ElMessage.success('职位已发布')
    loadJobs()
  } catch {
    ElMessage.error('发布失败')
  } finally {
    statusActionId.value = null
  }
}

/** 关闭职位（已发布 → 关闭）。 */
async function closeJob(job: JobVO) {
  statusActionId.value = job.id
  try {
    await updateJobStatus(job.id, 3)
    ElMessage.success('职位已关闭')
    loadJobs()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    statusActionId.value = null
  }
}

onMounted(async () => {
  await Promise.all([loadJobs(), loadDepartments()])
  loaded.value = true
})

// 关闭弹窗时停止轮询，避免后台任务继续请求
watch(dialogVisible, (visible) => {
  if (!visible) {
    stopJdPolling()
    resetJdGenerating()
  }
})

onBeforeUnmount(() => {
  stopJdPolling()
})
</script>

<style scoped>
.job-title-link {
  font-weight: 600;
  color: #1a1a1a;
  cursor: pointer;
  transition: color 0.2s;
}
.job-title-link:hover {
  color: #409eff;
}

/* Pagination Footer */
.sr-pagination-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding: 12px 0 0;
  border-top: 1px solid var(--c-border, #ebeef5);
}
.sr-pagination-info {
  font-size: 13px;
  color: #909399;
}
.sr-pagination-info strong {
  font-weight: 600;
  color: #303133;
  margin: 0 2px;
}

:deep(.ops-col .el-button) {
  padding: 5px 6px;
}

.desc-tpl-item:hover {
  border-color: var(--c-primary, #409eff) !important;
  background: #ecf5ff;
}

/* ===== AI 生成 JD 专业加载面板 ===== */
.ai-jd-generating {
  position: relative;
  overflow: hidden;
  border: 1px solid #e0e7ff;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 10px;
  background: linear-gradient(135deg, #eef2ff 0%, #f5f3ff 100%);
}

.ai-jd-generating::before {
  content: '';
  position: absolute;
  top: 0;
  left: -40%;
  width: 40%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #6366f1, transparent);
  animation: aiJdShine 1.6s linear infinite;
}

@keyframes aiJdShine {
  to { left: 140%; }
}

.ai-jd-gen-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.ai-jd-gen-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
  animation: aiJdPulse 1.2s ease-in-out infinite;
}

@keyframes aiJdPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.06); }
}

.ai-jd-gen-title {
  font-size: 14px;
  font-weight: 600;
  color: #4338ca;
}

.ai-jd-gen-sub {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

.ai-jd-gen-steps {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 10px 0 12px;
  flex-wrap: wrap;
}

.ai-jd-gen-step {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #9ca3af;
  transition: color 0.3s;
}

.ai-jd-gen-step.active {
  color: #6366f1;
  font-weight: 600;
}

.ai-jd-gen-step.done {
  color: #34d399;
}

.ai-jd-gen-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d1d5db;
  transition: background 0.3s;
}

.ai-jd-gen-step.active .ai-jd-gen-dot {
  background: #6366f1;
  animation: aiJdDotPulse 1.2s ease-in-out infinite;
}

.ai-jd-gen-step.done .ai-jd-gen-dot {
  background: #34d399;
}

@keyframes aiJdDotPulse {
  0%, 100% { box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15); }
  50% { box-shadow: 0 0 0 6px rgba(99, 102, 241, 0.08); }
}

.ai-jd-gen-bar {
  position: relative;
  height: 8px;
  border-radius: 999px;
  background: #e0e7ff;
  overflow: hidden;
}

.ai-jd-gen-bar-fill {
  position: relative;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #6366f1, #8b5cf6, #a855f7);
  background-size: 200% 100%;
  animation: aiJdBarFlow 1.2s linear infinite;
  transition: width 0.6s ease;
}

@keyframes aiJdBarFlow {
  to { background-position: -200% 0; }
}

.ai-jd-gen-bar-text {
  margin-top: 4px;
  font-size: 11px;
  color: #6366f1;
  font-weight: 600;
}
</style>
