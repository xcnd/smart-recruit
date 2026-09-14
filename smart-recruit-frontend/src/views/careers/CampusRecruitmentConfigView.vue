<template>
  <div class="careers-settings-page">
    <div class="page-header">
      <div class="page-title">
        <el-icon :size="22"><School /></el-icon>
        <span>校园招聘</span>
      </div>
      <el-text type="info" size="small">管理招聘官网「校园招聘」页面的所有展示内容</el-text>
    </div>

    <div class="settings-layout">
      <div class="settings-sidebar">
        <el-menu :default-active="activeCategory" class="category-menu" @select="onCategorySelect">
          <el-menu-item v-for="cat in categories" :key="cat.key" :index="cat.key">
            <el-icon :size="16"><component :is="cat.icon" /></el-icon>
            <span>{{ cat.label }}</span>
          </el-menu-item>
        </el-menu>
      </div>

      <div class="settings-content">
        <!-- Hero 区域 -->
        <template v-if="activeCategory === 'hero'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header"><span class="card-title">Hero 区域</span></div>
            </template>
            <el-form label-width="100px" label-position="left">
              <el-form-item label="徽章文字">
                <el-input v-model="form.heroBadge" maxlength="32" style="max-width: 320px;" />
              </el-form-item>
              <el-form-item label="主标题">
                <el-input v-model="form.heroTitle" maxlength="64" style="max-width: 520px;" />
              </el-form-item>
              <el-form-item label="描述文字">
                <el-input v-model="form.heroDesc" type="textarea" :rows="3" style="max-width: 600px;" />
              </el-form-item>
              <el-form-item label="CTA 按钮">
                <el-input v-model="form.heroCta" maxlength="32" style="max-width: 260px;" />
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never" class="mt-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">Hero 统计数据</span>
                <el-button size="small" type="primary" text @click="addArrayItem('heroStats', statDefaults)">
                  <el-icon><Plus /></el-icon>添加
                </el-button>
              </div>
            </template>
            <div class="array-list">
              <div v-for="(item, idx) in form.heroStats" :key="idx" class="array-item-row">
                <div class="array-item-fields">
                  <el-input v-model="item.number" maxlength="16" style="width: 120px;" placeholder="数值" />
                  <el-input v-model="item.label" maxlength="32" style="width: 200px;" placeholder="标签" />
                </div>
                <div class="array-item-actions">
                  <el-button size="small" :disabled="idx === 0" @click="moveArrayItem('heroStats', idx, -1)">
                    <el-icon><Top /></el-icon>
                  </el-button>
                  <el-button size="small" :disabled="idx === form.heroStats.length - 1" @click="moveArrayItem('heroStats', idx, 1)">
                    <el-icon><Bottom /></el-icon>
                  </el-button>
                  <el-button size="small" type="danger" text :disabled="form.heroStats.length <= 1" @click="removeArrayItem('heroStats', idx)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </el-card>
        </template>

        <!-- 招聘流程 -->
        <template v-if="activeCategory === 'process'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header"><span class="card-title">板块标题</span></div>
            </template>
            <el-form label-width="100px" label-position="left">
              <el-form-item label="标题">
                <el-input v-model="form.processTitle" maxlength="64" style="max-width: 420px;" />
              </el-form-item>
              <el-form-item label="副标题">
                <el-input v-model="form.processSubtitle" maxlength="128" style="max-width: 520px;" />
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never" class="mt-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">流程步骤</span>
                <el-button size="small" type="primary" text @click="addArrayItem('processSteps', stepDefaults)">
                  <el-icon><Plus /></el-icon>添加
                </el-button>
              </div>
            </template>
            <div class="array-list">
              <div v-for="(item, idx) in form.processSteps" :key="idx" class="array-card">
                <div class="array-card-header">
                  <span class="array-card-index">步骤 #{{ idx + 1 }}</span>
                  <div class="array-item-actions">
                    <el-button size="small" :disabled="idx === 0" @click="moveArrayItem('processSteps', idx, -1)">
                      <el-icon><Top /></el-icon>
                    </el-button>
                    <el-button size="small" :disabled="idx === form.processSteps.length - 1" @click="moveArrayItem('processSteps', idx, 1)">
                      <el-icon><Bottom /></el-icon>
                    </el-button>
                    <el-button size="small" type="danger" text :disabled="form.processSteps.length <= 1" @click="removeArrayItem('processSteps', idx)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
                <el-form label-width="60px" label-position="left" class="array-card-form">
                  <el-form-item label="名称">
                    <el-input v-model="item.title" maxlength="32" />
                  </el-form-item>
                  <el-form-item label="描述">
                    <el-input v-model="item.description" maxlength="128" />
                  </el-form-item>
                </el-form>
              </div>
            </div>
          </el-card>
        </template>

        <!-- 宣讲会行程 -->
        <template v-if="activeCategory === 'talks'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header"><span class="card-title">板块标题</span></div>
            </template>
            <el-form label-width="100px" label-position="left">
              <el-form-item label="标题">
                <el-input v-model="form.talksTitle" maxlength="64" style="max-width: 420px;" />
              </el-form-item>
              <el-form-item label="副标题">
                <el-input v-model="form.talksSubtitle" maxlength="128" style="max-width: 520px;" />
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never" class="mt-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">宣讲会场次</span>
                <el-button size="small" type="primary" text @click="addArrayItem('talks', talkDefaults)">
                  <el-icon><Plus /></el-icon>添加
                </el-button>
              </div>
            </template>
            <div class="array-list">
              <div v-for="(item, idx) in form.talks" :key="idx" class="array-card">
                <div class="array-card-header">
                  <span class="array-card-index">场次 #{{ idx + 1 }}</span>
                  <div class="array-item-actions">
                    <el-button size="small" :disabled="idx === 0" @click="moveArrayItem('talks', idx, -1)">
                      <el-icon><Top /></el-icon>
                    </el-button>
                    <el-button size="small" :disabled="idx === form.talks.length - 1" @click="moveArrayItem('talks', idx, 1)">
                      <el-icon><Bottom /></el-icon>
                    </el-button>
                    <el-button size="small" type="danger" text :disabled="form.talks.length <= 1" @click="removeArrayItem('talks', idx)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
                <el-form label-width="80px" label-position="left" class="array-card-form">
                  <el-row :gutter="12">
                    <el-col :span="8">
                      <el-form-item label="月份">
                        <el-input-number v-model="item.month" :min="1" :max="12" style="width: 100%;" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="日期">
                        <el-input-number v-model="item.day" :min="1" :max="31" style="width: 100%;" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="状态">
                        <el-select v-model="item.status" style="width: 100%;">
                          <el-option label="即将开始" value="upcoming" />
                          <el-option label="已结束" value="ended" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-row :gutter="12">
                    <el-col :span="12">
                      <el-form-item label="学校">
                        <el-input v-model="item.school" maxlength="32" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="时段">
                        <el-input v-model="item.timeSlot" maxlength="32" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-form-item label="地点">
                    <el-input v-model="item.venue" maxlength="128" />
                  </el-form-item>
                </el-form>
              </div>
            </div>
          </el-card>
        </template>

        <!-- 校招职位 -->
        <template v-if="activeCategory === 'positions'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header"><span class="card-title">职位板块标题</span></div>
            </template>
            <el-form label-width="100px" label-position="left">
              <el-form-item label="标题">
                <el-input v-model="form.title" maxlength="64" style="max-width: 420px;" />
              </el-form-item>
              <el-form-item label="副标题">
                <el-input v-model="form.subtitle" maxlength="256" style="max-width: 600px;" />
              </el-form-item>
              <el-form-item label="空状态提示">
                <el-input v-model="form.emptyText" maxlength="128" style="max-width: 520px;" />
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never" class="mt-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">筛选分类</span>
                <el-button size="small" type="primary" text @click="addArrayItem('filters', '')">
                  <el-icon><Plus /></el-icon>添加
                </el-button>
              </div>
            </template>
            <div class="array-list">
              <div v-for="(item, idx) in form.filters" :key="idx" class="array-item-row">
                <div class="array-item-fields">
                  <el-input v-model="form.filters[idx]" maxlength="32" style="width: 260px;" placeholder="分类名称" />
                </div>
                <div class="array-item-actions">
                  <el-button size="small" :disabled="idx === 0" @click="moveArrayItem('filters', idx, -1)">
                    <el-icon><Top /></el-icon>
                  </el-button>
                  <el-button size="small" :disabled="idx === form.filters.length - 1" @click="moveArrayItem('filters', idx, 1)">
                    <el-icon><Bottom /></el-icon>
                  </el-button>
                  <el-button size="small" type="danger" text :disabled="form.filters.length <= 1" @click="removeArrayItem('filters', idx)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </el-card>

          <!-- 职位 CRUD Table -->
          <el-card shadow="never" class="mt-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">校招职位列表</span>
                <el-button size="small" type="primary" @click="openCreateDialog">
                  <el-icon><Plus /></el-icon>新建职位
                </el-button>
              </div>
            </template>

            <div class="toolbar">
              <div class="toolbar-left">
                <el-input
                  v-model="jobKeyword"
                  placeholder="搜索职位名称 / 部门"
                  clearable
                  style="width: 260px;"
                  @clear="loadJobs"
                  @keyup.enter="loadJobs"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <el-select v-model="jobCategory" placeholder="职位分类" clearable style="width: 160px;" @change="loadJobs">
                  <el-option label="全部" value="" />
                  <el-option label="技术研发" value="tech" />
                  <el-option label="产品设计" value="product" />
                  <el-option label="市场销售" value="market" />
                  <el-option label="数据AI" value="data" />
                  <el-option label="运营职能" value="operation" />
                  <el-option label="实习" value="intern" />
                </el-select>
                <el-button @click="loadJobs">查询</el-button>
              </div>
            </div>

            <el-table :data="pagedJobs" v-loading="tableLoading" stripe class="jobs-table">
              <el-table-column type="index" label="序号" width="60" />
              <el-table-column prop="title" label="职位名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="dept" label="部门" min-width="120" show-overflow-tooltip />
              <el-table-column prop="location" label="地点" width="100" />
              <el-table-column prop="exp" label="经验" width="90" />
              <el-table-column prop="salary" label="薪资" width="120" />
              <el-table-column label="发布时间" width="110" align="center">
                <template #default="{ row }">
                  {{ formatDate(row.createTime) }}
                </template>
              </el-table-column>
              <el-table-column label="标签" width="140">
                <template #default="{ row }">
                  <el-tag
                    v-for="(tag, tIdx) in (row.tags || [])"
                    :key="tIdx"
                    size="small"
                    :type="tag.cls === 'hot' ? 'danger' : tag.cls === 'new' ? 'success' : 'info'"
                    style="margin-right: 4px;"
                  >
                    {{ tag.text }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-switch
                    :model-value="row.status === 1"
                    @click="handleToggleStatus(row)"
                    size="small"
                  />
                </template>
              </el-table-column>
              <el-table-column label="排序" width="70" align="center">
                <template #default="{ row }">{{ (row.sortOrder ?? 0) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="150" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" link type="primary" @click="openEditDialog(row)">编辑</el-button>
                  <el-popconfirm title="确认删除该职位？" @confirm="handleDelete(row.id!)">
                    <template #reference>
                      <el-button size="small" link type="danger">删除</el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrapper" v-if="total > pageSize">
              <el-pagination
                v-model:current-page="page"
                :page-size="pageSize"
                :total="total"
                background
                layout="prev, pager, next"
                @current-change="loadJobs"
              />
            </div>
          </el-card>
        </template>

        <!-- Save Buttons — only for hero / process / talks / positions (config only) -->
        <div v-if="activeCategory !== 'jobs'" class="settings-actions">
          <el-button type="primary" :loading="submitting" :disabled="!hasChanges" @click="handleSave">
            <el-icon v-if="!submitting"><Check /></el-icon>
            <span>{{ submitting ? '保存中...' : '保存设置' }}</span>
          </el-button>
          <el-button :disabled="!hasChanges || submitting" @click="handleReset">重置</el-button>
        </div>
      </div>
    </div>

    <!-- Create / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑职位' : '新建职位'"
      width="700px"
      destroy-on-close
      @closed="resetJobForm"
    >
      <el-form ref="jobFormRef" :model="jobForm" label-width="100px" label-position="left">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属部门" required>
              <el-tree-select
                v-model="jobForm.dept"
                :data="deptTree"
                :props="{ label: 'name', value: 'name', children: 'children' }"
                placeholder="请选择部门"
                filterable
                check-strictly
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位名称" required>
              <el-select
                v-model="jobForm.title"
                placeholder="搜索并选择职位"
                filterable
                allow-create
                default-first-option
                style="width: 100%;"
                @change="onJobSelect"
              >
                <el-option
                  v-for="j in jobOptions"
                  :key="j.id"
                  :label="j.title"
                  :value="j.title"
                >
                  <span>{{ j.title }}</span>
                  <span style="float: right; color: #999; font-size: 12px;">{{ j.departmentName }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工作地点" required>
              <el-input v-model="jobForm.location" maxlength="64" placeholder="如：北京" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经验要求">
              <el-input v-model="jobForm.exp" maxlength="32" placeholder="如：应届生" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="薪资范围">
              <el-input v-model="jobForm.salary" maxlength="64" placeholder="如：15-25K·16薪" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位分类">
              <el-select v-model="jobForm.category" style="width: 100%;">
                <el-option label="技术研发" value="tech" />
                <el-option label="产品设计" value="product" />
                <el-option label="市场销售" value="market" />
                <el-option label="数据AI" value="data" />
                <el-option label="运营职能" value="operation" />
                <el-option label="实习" value="intern" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="jobForm.sortOrder" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发布状态">
              <el-switch
                :model-value="jobForm.status === 1"
                active-text="发布"
                inactive-text="草稿"
                @change="jobForm.status = $event ? 1 : 0"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="职位标签">
          <div class="tags-editor">
            <div v-for="(tag, tagIdx) in jobForm.tags" :key="tagIdx" class="tags-editor-row">
              <el-input v-model="tag.text" maxlength="8" style="width: 120px;" placeholder="标签文字" />
              <el-select v-model="tag.cls" style="width: 140px;" placeholder="样式">
                <el-option label="热招(hot)" value="hot" />
                <el-option label="技术标签" value="tech-tag" />
                <el-option label="新(new)" value="new" />
              </el-select>
              <el-button size="small" type="danger" text @click="jobForm.tags.splice(tagIdx, 1)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button size="small" type="primary" text @click="jobForm.tags.push({ text: '', cls: 'tech-tag' })">
              <el-icon><Plus /></el-icon>添加标签
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="岗位职责">
          <div class="tags-editor">
            <div v-for="(item, idx) in jobForm.responsibilities" :key="idx" class="tags-editor-row">
              <el-input v-model="jobForm.responsibilities[idx]" style="flex:1;" placeholder="输入一条职责" />
              <el-button size="small" type="danger" text @click="jobForm.responsibilities.splice(idx, 1)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button size="small" type="primary" text @click="jobForm.responsibilities.push('')">
              <el-icon><Plus /></el-icon>添加
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="任职要求">
          <div class="tags-editor">
            <div v-for="(item, idx) in jobForm.requirements" :key="idx" class="tags-editor-row">
              <el-input v-model="jobForm.requirements[idx]" style="flex:1;" placeholder="输入一条要求" />
              <el-button size="small" type="danger" text @click="jobForm.requirements.splice(idx, 1)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button size="small" type="primary" text @click="jobForm.requirements.push('')">
              <el-icon><Plus /></el-icon>添加
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="加分项">
          <div class="tags-editor">
            <div v-for="(item, idx) in jobForm.bonus" :key="idx" class="tags-editor-row">
              <el-input v-model="jobForm.bonus[idx]" style="flex:1;" placeholder="输入一条加分项" />
              <el-button size="small" type="danger" text @click="jobForm.bonus.splice(idx, 1)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button size="small" type="primary" text @click="jobForm.bonus.push('')">
              <el-icon><Plus /></el-icon>添加
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="handleJobSubmit">
          {{ editingId ? '保存修改' : '立即创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { School, Check, Plus, Top, Bottom, Delete, Picture, Timer, VideoCamera, List, Search } from '@element-plus/icons-vue'
import { getConfigs, updateConfigs, getDepartments } from '@/api/system'
import { getJobs } from '@/api/job'
import { getCareersJobs, createCareersJob, updateCareersJob, deleteCareersJob, updateCareersJobStatus } from '@/api/careers'
import { getExperienceLabel } from '@/utils/format'
import { parseJsonArray, normalizeStatItems, normalizeObjectArray } from '@/utils/json'
import type { SysConfigVO, CareersJobVO, JobVO, DepartmentTreeVO } from '@/types/models'

const REC_TYPE = 'CAMPUS'

interface CategoryItem { key: string; label: string; icon: any }
const categories: CategoryItem[] = [
  { key: 'hero', label: 'Hero 区域', icon: Picture },
  { key: 'process', label: '招聘流程', icon: Timer },
  { key: 'talks', label: '宣讲会行程', icon: VideoCamera },
  { key: 'positions', label: '校招职位', icon: List },
]
const activeCategory = ref('hero')

// ---- Config ----
interface HeroStat { number: string; label: string }
interface ProcessStep { title: string; description: string }
interface CampusTalk { month: number; day: number; school: string; venue: string; timeSlot: string; status: string }

interface CampusForm {
  heroBadge: string; heroTitle: string; heroDesc: string; heroCta: string; heroStats: HeroStat[]
  processTitle: string; processSubtitle: string; processSteps: ProcessStep[]
  talksTitle: string; talksSubtitle: string; talks: CampusTalk[]
  title: string; subtitle: string; emptyText: string; filters: string[]
}

const statDefaults: HeroStat = { number: '', label: '' }
/** Hero 统计数据默认示例：与官网校园招聘页兜底保持一致，避免配置缺失时页面空白。 */
const defaultHeroStats: HeroStat[] = [
  { number: '50+', label: '校招职位' },
  { number: '20+', label: '目标院校' },
  { number: '6 个月', label: '培养计划' },
]
const stepDefaults: ProcessStep = { title: '', description: '' }
const talkDefaults: CampusTalk = { month: 9, day: 1, school: '', venue: '', timeSlot: '', status: 'upcoming' }

const form = reactive<CampusForm>({
  heroBadge: '', heroTitle: '', heroDesc: '', heroCta: '', heroStats: defaultHeroStats.map((s) => ({ ...s })),
  processTitle: '', processSubtitle: '', processSteps: [{ ...stepDefaults }],
  talksTitle: '', talksSubtitle: '', talks: [{ ...talkDefaults }],
  title: '', subtitle: '', emptyText: '', filters: ['全部职位'],
})

const submitting = ref(false)
const jsonArrayFields = ['heroStats', 'processSteps', 'talks', 'filters']
const originalValues = reactive<Record<string, string>>({})

const categoryFields: Record<string, string[]> = {
  hero: ['heroBadge', 'heroTitle', 'heroDesc', 'heroCta', 'heroStats'],
  process: ['processTitle', 'processSubtitle', 'processSteps'],
  talks: ['talksTitle', 'talksSubtitle', 'talks'],
  positions: ['title', 'subtitle', 'emptyText', 'filters'],
}

const hasChanges = computed(() => {
  const fields = categoryFields[activeCategory.value]
  if (!fields) return false
  return fields.some(f => {
    const v1 = jsonArrayFields.includes(f) ? JSON.stringify((form as any)[f]) : String((form as any)[f] ?? '')
    const v2 = originalValues[f] ?? ''
    return v1 !== v2
  })
})

const fieldToKey: Record<string, string> = {
  heroBadge: 'careers_campus_hero_badge',
  heroTitle: 'careers_campus_hero_title',
  heroDesc: 'careers_campus_hero_desc',
  heroCta: 'careers_campus_hero_cta',
  heroStats: 'careers_campus_hero_stats',
  processTitle: 'careers_campus_process_title',
  processSubtitle: 'careers_campus_process_subtitle',
  processSteps: 'careers_campus_process_steps',
  talksTitle: 'careers_campus_talks_title',
  talksSubtitle: 'careers_campus_talks_subtitle',
  talks: 'careers_campus_talks',
  title: 'careers_campus_title',
  subtitle: 'careers_campus_subtitle',
  emptyText: 'careers_campus_empty_text',
  filters: 'careers_campus_filters',
}

const keyToField = Object.fromEntries(Object.entries(fieldToKey).map(([k, v]) => [v, k]))

function addArrayItem(field: string, defaults: any) {
  ;(form as any)[field].push(typeof defaults === 'string' ? defaults : { ...defaults })
}

function removeArrayItem(field: string, idx: number) {
  ;(form as any)[field].splice(idx, 1)
}

function moveArrayItem(field: string, idx: number, direction: number) {
  const arr = (form as any)[field]
  const target = idx + direction
  if (target < 0 || target >= arr.length) return;
  [arr[idx], arr[target]] = [arr[target], arr[idx]]
}

function snapshotAll() {
  for (const field of Object.keys(fieldToKey)) {
    if (jsonArrayFields.includes(field)) originalValues[field] = JSON.stringify((form as any)[field])
    else originalValues[field] = String((form as any)[field] ?? '')
  }
}

function onCategorySelect(key: string) {
  activeCategory.value = key
  if (key === 'positions') loadJobs()
}

async function handleSave() {
  submitting.value = true
  try {
    const fields = categoryFields[activeCategory.value]
    const items = fields.map(f => ({
      configKey: fieldToKey[f],
      configValue: jsonArrayFields.includes(f) ? JSON.stringify((form as any)[f]) : String((form as any)[f] ?? ''),
    }))
    await updateConfigs(items)
    for (const f of fields) {
      if (jsonArrayFields.includes(f)) originalValues[f] = JSON.stringify((form as any)[f])
      else originalValues[f] = String((form as any)[f] ?? '')
    }
    ElMessage.success(`${categories.find(c => c.key === activeCategory.value)?.label} 保存成功`)
  } catch { } finally { submitting.value = false }
}

function handleReset() {
  const fields = categoryFields[activeCategory.value]
  for (const f of fields) {
    if (jsonArrayFields.includes(f)) {
      try { (form as any)[f] = JSON.parse(originalValues[f]) } catch { }
    } else {
      (form as any)[f] = originalValues[f]
    }
  }
}

// ---- Jobs CRUD ----
const jobList = ref<CareersJobVO[]>([])
const tableLoading = ref(false)
const page = ref(1)
const pageSize = 12
const total = ref(0)
const jobKeyword = ref('')
const jobCategory = ref('')

const pagedJobs = computed(() => jobList.value)

const dialogVisible = ref(false)
const dialogSubmitting = ref(false)
const deptTree = ref<DepartmentTreeVO[]>([])
const jobOptions = ref<JobVO[]>([])
const editingId = ref<number | null>(null)
const jobForm = reactive<{
  title: string; dept: string; location: string; exp: string; salary: string
  category: string; sortOrder: number; status: number; tags: { text: string; cls: string }[]
  responsibilities: string[]; requirements: string[]; bonus: string[]
}>({
  title: '', dept: '', location: '', exp: '', salary: '',
  category: 'tech', sortOrder: 0, status: 1, tags: [],
  responsibilities: [], requirements: [], bonus: [],
})

function resetJobForm() {
  editingId.value = null
  jobForm.title = ''; jobForm.dept = ''; jobForm.location = ''
  jobForm.exp = ''; jobForm.salary = ''; jobForm.category = 'tech'
  jobForm.sortOrder = 0; jobForm.status = 1; jobForm.tags = []
  jobForm.responsibilities = []; jobForm.requirements = []; jobForm.bonus = []
}

async function loadJobs() {
  tableLoading.value = true
  try {
    const res = await getCareersJobs({
      recType: REC_TYPE,
      keyword: jobKeyword.value || undefined,
      category: jobCategory.value || undefined,
      page: page.value,
      size: pageSize,
    })
    jobList.value = res.records || []
    total.value = res.total || 0
  } catch { } finally { tableLoading.value = false }
}

async function loadDepts() {
  try {
    deptTree.value = await getDepartments()
  } catch { deptTree.value = [] }
}

async function fetchJobs() {
  try {
    const res = await getJobs({ page: 1, size: 200, status: 1 })
    jobOptions.value = res.records || []
  } catch { jobOptions.value = [] }
}

function onJobSelect(title: string) {
  if (!title) return
  const matched = jobOptions.value.find(j => j.title === title)
  if (matched) {
    if (!jobForm.dept) jobForm.dept = matched.departmentName || ''
    if (!jobForm.location) jobForm.location = matched.location || ''
    if (!jobForm.exp) jobForm.exp = getExperienceLabel(matched.experienceLevel ?? 0)
    if (!jobForm.salary && matched.salaryMin && matched.salaryMax) {
      const minK = Math.round(matched.salaryMin / 1000)
      const maxK = Math.round(matched.salaryMax / 1000)
      jobForm.salary = `${minK}-${maxK}K`
    }
  }
}

function openCreateDialog() {
  resetJobForm()
  loadDepts()
  fetchJobs()
  dialogVisible.value = true
}

function openEditDialog(row: CareersJobVO) {
  editingId.value = row.id!
  jobForm.title = row.title; jobForm.dept = row.dept
  jobForm.location = row.location; jobForm.exp = row.exp
  jobForm.salary = row.salary; jobForm.category = row.category
  jobForm.sortOrder = (row.sortOrder ?? 0)
  jobForm.status = row.status
  jobForm.tags = (row.tags || []).map((t: any) => ({ text: t.text, cls: t.cls }))
  jobForm.responsibilities = [...(row.responsibilities || [])]
  jobForm.requirements = [...(row.requirements || [])]
  jobForm.bonus = [...(row.bonus || [])]
  loadDepts()
  fetchJobs()
  dialogVisible.value = true
}

async function handleJobSubmit() {
  if (!jobForm.title.trim() || !jobForm.dept.trim() || !jobForm.location.trim()) {
    ElMessage.warning('职位名称、部门、地点为必填项')
    return
  }
  dialogSubmitting.value = true
  try {
    const payload = {
      recType: REC_TYPE,
      title: jobForm.title.trim(),
      dept: jobForm.dept.trim(),
      location: jobForm.location.trim(),
      exp: jobForm.exp,
      salary: jobForm.salary,
      category: jobForm.category,
      sortOrder: jobForm.sortOrder,
      status: jobForm.status,
      tags: jobForm.tags.filter(t => t.text.trim()),
      responsibilities: jobForm.responsibilities.filter(s => s.trim()),
      requirements: jobForm.requirements.filter(s => s.trim()),
      bonus: jobForm.bonus.filter(s => s.trim()),
    }
    if (editingId.value) {
      await updateCareersJob(editingId.value, payload)
      ElMessage.success('职位已更新')
    } else {
      await createCareersJob(payload)
      ElMessage.success('职位已创建')
    }
    dialogVisible.value = false
    await loadJobs()
  } catch { } finally { dialogSubmitting.value = false }
}

async function handleDelete(id: number) {
  try {
    await deleteCareersJob(id)
    ElMessage.success('职位已删除')
    await loadJobs()
  } catch { }
}

async function handleToggleStatus(row: CareersJobVO) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await updateCareersJobStatus(row.id!, newStatus)
    row.status = newStatus
    ElMessage.success(newStatus === 1 ? '已发布' : '已下架')
  } catch { }
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

onMounted(async () => {
  try {
    const configs: SysConfigVO[] = await getConfigs()
    for (const config of configs) {
      const field = keyToField[config.configKey]
      if (!field) continue
      if (jsonArrayFields.includes(field)) {
        const fallback = (form as any)[field] as any[]
        const parsed = parseJsonArray<any>(config.configValue, fallback)
        if (field === 'heroStats') {
          // 统计数据：规范化字段并兜底脏数据，避免出现大量空行
          (form as any)[field] = normalizeStatItems(parsed, fallback)
        } else {
          // 其余对象数组配置：补齐字段、空对象整体兜底
          (form as any)[field] = normalizeObjectArray(parsed, fallback)
        }
      } else {
        (form as any)[field] = config.configValue
      }
    }
  } catch { }
  snapshotAll()
})
</script>

<style scoped>
.careers-settings-page { max-width: 1500px; }
.page-header { margin-bottom: 24px; }
.page-title { display: flex; align-items: center; gap: 8px; font-size: 18px; font-weight: 600; color: var(--c-text); margin-bottom: 4px; }
.settings-layout { display: flex; gap: 24px; align-items: flex-start; }
.settings-sidebar { flex-shrink: 0; width: 180px; }
.category-menu { border-right: none; border-radius: var(--c-radius-lg); overflow: hidden; }
.category-menu :deep(.el-menu-item) { height: 44px; line-height: 44px; font-size: 14px; }
.category-menu :deep(.el-menu-item .el-icon) { margin-right: 8px; }
.settings-content { flex: 1; min-width: 0; }
.mt-card { margin-top: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-title { font-size: 15px; font-weight: 600; }
.array-list { display: flex; flex-direction: column; gap: 12px; }
.array-item-row { display: flex; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px solid var(--c-border-light); }
.array-item-row:last-child { border-bottom: none; }
.array-item-fields { display: flex; align-items: center; gap: 12px; flex: 1; }
.array-item-actions { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.array-card { background: var(--el-fill-color-lighter); border-radius: var(--c-radius-md); padding: 16px; }
.array-card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid var(--c-border-light); }
.array-card-index { font-size: 13px; font-weight: 600; color: var(--c-text-secondary); }
.array-card-form { margin-top: 4px; }
.settings-actions { display: flex; align-items: center; gap: 12px; margin-top: 24px; padding-top: 20px; border-top: 1px solid var(--c-border-light); }

/* Toolbar */
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.toolbar-left { display: flex; align-items: center; gap: 12px; }

/* Table */
.jobs-table { margin-top: 4px; }

/* Pagination */
.pagination-wrapper { display: flex; justify-content: center; margin-top: 24px; padding: 16px 0; }

/* Tags Editor */
.tags-editor { display: flex; flex-direction: column; gap: 8px; }
.tags-editor-row { display: flex; align-items: center; gap: 8px; }
</style>
