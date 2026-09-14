<template>
  <div class="careers-settings-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-title">
        <el-icon :size="22"><House /></el-icon>
        <span>官网配置</span>
      </div>
      <el-text type="info" size="small">管理招聘官网各板块的展示内容，修改后立即生效</el-text>
    </div>

    <!-- Layout: Left Menu + Right Content -->
    <div class="settings-layout">
      <!-- Left Category Menu -->
      <div class="settings-sidebar">
        <el-menu
          :default-active="activeCategory"
          class="category-menu"
          @select="onCategorySelect"
        >
          <el-menu-item v-for="cat in categories" :key="cat.key" :index="cat.key">
            <el-icon :size="16"><component :is="cat.icon" /></el-icon>
            <span>{{ cat.label }}</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- Right Content Area -->
      <div class="settings-content">
        <!-- ========================
             Hero 区域
        ========================== -->
        <template v-if="activeCategory === 'hero'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">Hero 区域</span>
                <el-tag size="small" effect="plain" type="primary">首页</el-tag>
              </div>
            </template>
            <el-form label-width="120px" label-position="left">
              <el-form-item label="徽章文字">
                <el-input v-model="form.heroBadge" maxlength="128" style="max-width: 520px;" />
                <div class="field-desc">Hero 顶部徽章内显示的文字</div>
              </el-form-item>
              <el-form-item label="主标题">
                <el-input v-model="form.heroTitleMain" maxlength="64" style="max-width: 520px;" />
              </el-form-item>
              <el-form-item label="渐变标题">
                <el-input v-model="form.heroTitleGradient" maxlength="64" style="max-width: 520px;" />
                <div class="field-desc">渐变色彩渲染的副标题</div>
              </el-form-item>
              <el-form-item label="描述文字">
                <el-input v-model="form.heroDesc" type="textarea" :rows="3" style="max-width: 600px;" />
              </el-form-item>
              <el-form-item label="主按钮文字">
                <el-input v-model="form.heroCtaPrimaryLabel" maxlength="32" style="max-width: 320px;" />
              </el-form-item>
              <el-form-item label="次按钮文字">
                <el-input v-model="form.heroCtaOutlineLabel" maxlength="32" style="max-width: 320px;" />
              </el-form-item>
            </el-form>
          </el-card>

        </template>

        <!-- ========================
             文化价值观
        ========================== -->
        <template v-if="activeCategory === 'culture'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">板块标题</span>
              </div>
            </template>
            <el-form label-width="120px" label-position="left">
              <el-form-item label="标题">
                <el-input v-model="form.cultureTitle" maxlength="64" style="max-width: 420px;" />
              </el-form-item>
              <el-form-item label="副标题">
                <el-input v-model="form.cultureSubtitle" maxlength="128" style="max-width: 520px;" />
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never" class="mt-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">文化卡片</span>
                <el-button size="small" type="primary" text @click="addArrayItem('cultureCards', cultureCardDefaults)">
                  <el-icon><Plus /></el-icon>添加
                </el-button>
              </div>
            </template>
            <div class="array-list">
              <div v-for="(item, idx) in form.cultureCards" :key="idx" class="array-card">
                <div class="array-card-header">
                  <span class="array-card-index">卡片 #{{ idx + 1 }}</span>
                  <div class="array-item-actions">
                    <el-button size="small" :disabled="idx === 0" @click="moveArrayItem('cultureCards', idx, -1)">
                      <el-icon><Top /></el-icon>
                    </el-button>
                    <el-button size="small" :disabled="idx === form.cultureCards.length - 1" @click="moveArrayItem('cultureCards', idx, 1)">
                      <el-icon><Bottom /></el-icon>
                    </el-button>
                    <el-button size="small" type="danger" text :disabled="form.cultureCards.length <= 1" @click="removeArrayItem('cultureCards', idx)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
                <el-form label-width="80px" label-position="left" class="array-card-form">
                  <el-form-item label="标题">
                    <el-input v-model="item.title" maxlength="32" />
                  </el-form-item>
                  <el-form-item label="描述">
                    <el-input v-model="item.description" type="textarea" :rows="2" />
                  </el-form-item>
                  <el-form-item label="图标色">
                    <el-select v-model="item.iconBg" style="width: 200px;">
                      <el-option v-for="o in cultureIconOptions" :key="o.value" :label="o.label" :value="o.value" />
                    </el-select>
                  </el-form-item>
                </el-form>
              </div>
            </div>
          </el-card>
        </template>

        <!-- ========================
             薪酬福利
        ========================== -->
        <template v-if="activeCategory === 'benefits'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">板块标题</span>
              </div>
            </template>
            <el-form label-width="120px" label-position="left">
              <el-form-item label="标题">
                <el-input v-model="form.benefitsTitle" maxlength="64" style="max-width: 420px;" />
              </el-form-item>
              <el-form-item label="副标题">
                <el-input v-model="form.benefitsSubtitle" maxlength="128" style="max-width: 520px;" />
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never" class="mt-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">福利条目</span>
                <el-button size="small" type="primary" text @click="addArrayItem('benefitsItems', benefitsItemDefaults)">
                  <el-icon><Plus /></el-icon>添加
                </el-button>
              </div>
            </template>
            <div class="array-list">
              <div v-for="(item, idx) in form.benefitsItems" :key="idx" class="array-card">
                <div class="array-card-header">
                  <span class="array-card-index">条目 #{{ idx + 1 }}</span>
                  <div class="array-item-actions">
                    <el-button size="small" :disabled="idx === 0" @click="moveArrayItem('benefitsItems', idx, -1)">
                      <el-icon><Top /></el-icon>
                    </el-button>
                    <el-button size="small" :disabled="idx === form.benefitsItems.length - 1" @click="moveArrayItem('benefitsItems', idx, 1)">
                      <el-icon><Bottom /></el-icon>
                    </el-button>
                    <el-button size="small" type="danger" text :disabled="form.benefitsItems.length <= 1" @click="removeArrayItem('benefitsItems', idx)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
                <el-form label-width="80px" label-position="left" class="array-card-form">
                  <el-form-item label="标题">
                    <el-input v-model="item.title" maxlength="32" />
                  </el-form-item>
                  <el-form-item label="描述">
                    <el-input v-model="item.description" type="textarea" :rows="2" />
                  </el-form-item>
                  <el-form-item label="颜色">
                    <el-select v-model="item.colorClass" style="width: 200px;">
                      <el-option v-for="o in colorClassOptions" :key="o.value" :label="o.label" :value="o.value" />
                    </el-select>
                  </el-form-item>
                </el-form>
              </div>
            </div>
          </el-card>
        </template>

        <!-- ========================
             员工心声
        ========================== -->
        <template v-if="activeCategory === 'testimonials'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">板块标题</span>
              </div>
            </template>
            <el-form label-width="120px" label-position="left">
              <el-form-item label="标题">
                <el-input v-model="form.testimonialsTitle" maxlength="64" style="max-width: 420px;" />
              </el-form-item>
              <el-form-item label="副标题">
                <el-input v-model="form.testimonialsSubtitle" maxlength="128" style="max-width: 520px;" />
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never" class="mt-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">员工心声</span>
                <el-button size="small" type="primary" text @click="addArrayItem('testimonials', testimonialDefaults)">
                  <el-icon><Plus /></el-icon>添加
                </el-button>
              </div>
            </template>
            <div class="array-list">
              <div v-for="(item, idx) in form.testimonials" :key="idx" class="array-card">
                <div class="array-card-header">
                  <span class="array-card-index">心声 #{{ idx + 1 }}</span>
                  <div class="array-item-actions">
                    <el-button size="small" :disabled="idx === 0" @click="moveArrayItem('testimonials', idx, -1)">
                      <el-icon><Top /></el-icon>
                    </el-button>
                    <el-button size="small" :disabled="idx === form.testimonials.length - 1" @click="moveArrayItem('testimonials', idx, 1)">
                      <el-icon><Bottom /></el-icon>
                    </el-button>
                    <el-button size="small" type="danger" text :disabled="form.testimonials.length <= 1" @click="removeArrayItem('testimonials', idx)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
                <el-form label-width="80px" label-position="left" class="array-card-form">
                  <el-form-item label="引语">
                    <el-input v-model="item.quote" type="textarea" :rows="3" />
                  </el-form-item>
                  <el-form-item label="姓名">
                    <el-input v-model="item.name" maxlength="16" />
                  </el-form-item>
                  <el-form-item label="缩写">
                    <el-input v-model="item.initials" maxlength="4" style="width: 120px;" />
                  </el-form-item>
                  <el-form-item label="角色">
                    <el-input v-model="item.role" maxlength="64" />
                  </el-form-item>
                  <el-form-item label="头像色">
                    <el-select v-model="item.avatarClass" style="width: 200px;">
                      <el-option v-for="o in avatarClassOptions" :key="o.value" :label="o.label" :value="o.value" />
                    </el-select>
                  </el-form-item>
                </el-form>
              </div>
            </div>
          </el-card>
        </template>

        <!-- ========================
             常见问题
        ========================== -->
        <template v-if="activeCategory === 'faq'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">板块标题</span>
              </div>
            </template>
            <el-form label-width="120px" label-position="left">
              <el-form-item label="标题">
                <el-input v-model="form.faqTitle" maxlength="64" style="max-width: 420px;" />
              </el-form-item>
              <el-form-item label="副标题">
                <el-input v-model="form.faqSubtitle" maxlength="128" style="max-width: 520px;" />
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never" class="mt-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">FAQ 条目</span>
                <el-button size="small" type="primary" text @click="addArrayItem('faqItems', faqItemDefaults)">
                  <el-icon><Plus /></el-icon>添加
                </el-button>
              </div>
            </template>
            <div class="array-list">
              <div v-for="(item, idx) in form.faqItems" :key="idx" class="array-card">
                <div class="array-card-header">
                  <span class="array-card-index">问题 #{{ idx + 1 }}</span>
                  <div class="array-item-actions">
                    <el-button size="small" :disabled="idx === 0" @click="moveArrayItem('faqItems', idx, -1)">
                      <el-icon><Top /></el-icon>
                    </el-button>
                    <el-button size="small" :disabled="idx === form.faqItems.length - 1" @click="moveArrayItem('faqItems', idx, 1)">
                      <el-icon><Bottom /></el-icon>
                    </el-button>
                    <el-button size="small" type="danger" text :disabled="form.faqItems.length <= 1" @click="removeArrayItem('faqItems', idx)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
                <el-form label-width="80px" label-position="left" class="array-card-form">
                  <el-form-item label="问题">
                    <el-input v-model="item.question" maxlength="128" />
                  </el-form-item>
                  <el-form-item label="答案">
                    <el-input v-model="item.answer" type="textarea" :rows="3" />
                  </el-form-item>
                </el-form>
              </div>
            </div>
          </el-card>
        </template>

        <!-- ========================
             团队风采
        ========================== -->
        <template v-if="activeCategory === 'gallery'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">板块标题</span>
              </div>
            </template>
            <el-form label-width="120px" label-position="left">
              <el-form-item label="标题">
                <el-input v-model="form.galleryTitle" maxlength="64" style="max-width: 420px;" />
              </el-form-item>
              <el-form-item label="副标题">
                <el-input v-model="form.gallerySubtitle" maxlength="128" style="max-width: 520px;" />
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never" class="mt-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">团队风采条目</span>
                <el-button size="small" type="primary" text @click="addArrayItem('galleryItems', galleryItemDefaults)">
                  <el-icon><Plus /></el-icon>添加
                </el-button>
              </div>
            </template>
            <div class="array-list">
              <div v-for="(item, idx) in form.galleryItems" :key="idx" class="array-card">
                <div class="array-card-header">
                  <span class="array-card-index">条目 #{{ idx + 1 }}</span>
                  <div class="array-item-actions">
                    <el-button size="small" :disabled="idx === 0" @click="moveArrayItem('galleryItems', idx, -1)">
                      <el-icon><Top /></el-icon>
                    </el-button>
                    <el-button size="small" :disabled="idx === form.galleryItems.length - 1" @click="moveArrayItem('galleryItems', idx, 1)">
                      <el-icon><Bottom /></el-icon>
                    </el-button>
                    <el-button size="small" type="danger" text :disabled="form.galleryItems.length <= 1" @click="removeArrayItem('galleryItems', idx)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
                <el-form label-width="80px" label-position="left" class="array-card-form">
                  <el-form-item label="标题">
                    <el-input v-model="item.caption" maxlength="64" />
                  </el-form-item>
                  <el-form-item label="副标题">
                    <el-input v-model="item.subcaption" maxlength="128" />
                  </el-form-item>
                  <el-form-item label="渐变色">
                    <el-select v-model="item.gradientClass" style="width: 200px;">
                      <el-option v-for="o in gradientClassOptions" :key="o.value" :label="o.label" :value="o.value" />
                    </el-select>
                  </el-form-item>
                </el-form>
              </div>
            </div>
          </el-card>
        </template>

        <!-- ========================
             统计数据
        ========================== -->
        <template v-if="activeCategory === 'stats'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">统计数据条目</span>
                <el-button size="small" type="primary" text @click="addArrayItem('statsItems', statsItemDefaults)">
                  <el-icon><Plus /></el-icon>添加
                </el-button>
              </div>
            </template>
            <div class="array-list">
              <div v-for="(item, idx) in form.statsItems" :key="idx" class="array-item-row">
                <div class="array-item-fields">
                  <el-input-number v-model="item.number" :min="0" style="width: 120px;" placeholder="数字" />
                  <el-input v-model="item.suffix" maxlength="8" style="width: 80px;" placeholder="后缀" />
                  <el-input v-model="item.label" maxlength="32" style="width: 200px;" placeholder="标签" />
                </div>
                <div class="array-item-actions">
                  <el-button size="small" :disabled="idx === 0" @click="moveArrayItem('statsItems', idx, -1)">
                    <el-icon><Top /></el-icon>
                  </el-button>
                  <el-button size="small" :disabled="idx === form.statsItems.length - 1" @click="moveArrayItem('statsItems', idx, 1)">
                    <el-icon><Bottom /></el-icon>
                  </el-button>
                  <el-button size="small" type="danger" text :disabled="form.statsItems.length <= 1" @click="removeArrayItem('statsItems', idx)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </el-card>
        </template>

        <!-- ========================
             行动号召
        ========================== -->
        <template v-if="activeCategory === 'cta'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">行动号召区域</span>
                <el-tag size="small" effect="plain" type="primary">首页</el-tag>
              </div>
            </template>
            <el-form label-width="120px" label-position="left">
              <el-form-item label="标题">
                <el-input v-model="form.ctaTitle" maxlength="128" style="max-width: 520px;" />
              </el-form-item>
              <el-form-item label="描述">
                <el-input v-model="form.ctaDesc" type="textarea" :rows="3" style="max-width: 600px;" />
              </el-form-item>
              <el-form-item label="按钮文字">
                <el-input v-model="form.ctaBtnLabel" maxlength="32" style="max-width: 320px;" />
              </el-form-item>
              <el-form-item label="按钮链接">
                <el-input v-model="form.ctaBtnLink" maxlength="128" style="max-width: 420px;" />
              </el-form-item>
            </el-form>
          </el-card>
        </template>

        <!-- ========================
             页脚配置
        ========================== -->
        <template v-if="activeCategory === 'footer'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">品牌信息</span>
              </div>
            </template>
            <el-form label-width="120px" label-position="left">
              <el-form-item label="品牌描述">
                <el-input v-model="form.footerBrandDesc" type="textarea" :rows="3" style="max-width: 600px;" />
              </el-form-item>
              <el-form-item label="版权信息">
                <el-input v-model="form.footerCopyright" maxlength="128" style="max-width: 420px;" />
              </el-form-item>
              <el-form-item label="联系邮箱">
                <el-input v-model="form.footerContactEmail" maxlength="128" style="max-width: 420px;" />
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never" class="mt-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">办公城市</span>
                <el-button size="small" type="primary" text @click="addArrayItem('footerContactCities', footerCityDefaults)">
                  <el-icon><Plus /></el-icon>添加
                </el-button>
              </div>
            </template>
            <div class="array-list">
              <div v-for="(item, idx) in form.footerContactCities" :key="idx" class="array-item-row">
                <div class="array-item-fields">
                  <el-input v-model="item.label" maxlength="64" style="width: 200px;" placeholder="城市名称" />
                </div>
                <div class="array-item-actions">
                  <el-button size="small" :disabled="idx === 0" @click="moveArrayItem('footerContactCities', idx, -1)">
                    <el-icon><Top /></el-icon>
                  </el-button>
                  <el-button size="small" :disabled="idx === form.footerContactCities.length - 1" @click="moveArrayItem('footerContactCities', idx, 1)">
                    <el-icon><Bottom /></el-icon>
                  </el-button>
                  <el-button size="small" type="danger" text :disabled="form.footerContactCities.length <= 1" @click="removeArrayItem('footerContactCities', idx)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </el-card>

          <el-card shadow="never" class="mt-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">职位分类</span>
              </div>
            </template>
            <el-form label-width="120px" label-position="left">
              <el-form-item label="栏目标题">
                <el-input v-model="form.footerCategoryTitle" maxlength="32" style="max-width: 320px;" />
              </el-form-item>
            </el-form>
            <div class="array-list" style="margin-top: 8px;">
              <div class="card-header" style="margin-bottom: 12px;">
                <span style="font-size: 14px; color: var(--c-text-secondary);">分类链接</span>
                <el-button size="small" type="primary" text @click="addArrayItem('footerCategoryLinks', footerLinkDefaults)">
                  <el-icon><Plus /></el-icon>添加
                </el-button>
              </div>
              <div v-for="(item, idx) in form.footerCategoryLinks" :key="idx" class="array-item-row">
                <div class="array-item-fields">
                  <el-input v-model="item.label" maxlength="32" style="width: 150px;" placeholder="名称" />
                  <el-input v-model="item.href" maxlength="128" style="width: 250px;" placeholder="链接" />
                </div>
                <div class="array-item-actions">
                  <el-button size="small" :disabled="idx === 0" @click="moveArrayItem('footerCategoryLinks', idx, -1)">
                    <el-icon><Top /></el-icon>
                  </el-button>
                  <el-button size="small" :disabled="idx === form.footerCategoryLinks.length - 1" @click="moveArrayItem('footerCategoryLinks', idx, 1)">
                    <el-icon><Bottom /></el-icon>
                  </el-button>
                  <el-button size="small" type="danger" text :disabled="form.footerCategoryLinks.length <= 1" @click="removeArrayItem('footerCategoryLinks', idx)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </el-card>

          <el-card shadow="never" class="mt-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">关于我们</span>
              </div>
            </template>
            <el-form label-width="120px" label-position="left">
              <el-form-item label="栏目标题">
                <el-input v-model="form.footerAboutTitle" maxlength="32" style="max-width: 320px;" />
              </el-form-item>
            </el-form>
            <div class="array-list" style="margin-top: 8px;">
              <div class="card-header" style="margin-bottom: 12px;">
                <span style="font-size: 14px; color: var(--c-text-secondary);">关于我们链接</span>
                <el-button size="small" type="primary" text @click="addArrayItem('footerAboutLinks', footerLinkDefaults)">
                  <el-icon><Plus /></el-icon>添加
                </el-button>
              </div>
              <div v-for="(item, idx) in form.footerAboutLinks" :key="idx" class="array-item-row">
                <div class="array-item-fields">
                  <el-input v-model="item.label" maxlength="32" style="width: 150px;" placeholder="名称" />
                  <el-input v-model="item.href" maxlength="128" style="width: 250px;" placeholder="链接" />
                </div>
                <div class="array-item-actions">
                  <el-button size="small" :disabled="idx === 0" @click="moveArrayItem('footerAboutLinks', idx, -1)">
                    <el-icon><Top /></el-icon>
                  </el-button>
                  <el-button size="small" :disabled="idx === form.footerAboutLinks.length - 1" @click="moveArrayItem('footerAboutLinks', idx, 1)">
                    <el-icon><Bottom /></el-icon>
                  </el-button>
                  <el-button size="small" type="danger" text :disabled="form.footerAboutLinks.length <= 1" @click="removeArrayItem('footerAboutLinks', idx)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </el-card>
        </template>

        <!-- ========================
             导航栏
        ========================== -->
        <template v-if="activeCategory === 'nav'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">基本设置</span>
              </div>
            </template>
            <el-form label-width="120px" label-position="left">
              <el-form-item label="Logo 文字">
                <el-input v-model="form.navLogoText" maxlength="32" style="max-width: 320px;" />
              </el-form-item>
              <el-form-item label="CTA 按钮文字">
                <el-input v-model="form.navCtaLabel" maxlength="16" style="max-width: 320px;" />
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never" class="mt-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">导航链接</span>
                <el-button size="small" type="primary" text @click="addArrayItem('navLinks', navLinkDefaults)">
                  <el-icon><Plus /></el-icon>添加
                </el-button>
              </div>
            </template>
            <div class="array-list">
              <div v-for="(item, idx) in form.navLinks" :key="idx" class="array-card">
                <div class="array-card-header">
                  <span class="array-card-index">链接 #{{ idx + 1 }}</span>
                  <div class="array-item-actions">
                    <el-button size="small" :disabled="idx === 0" @click="moveArrayItem('navLinks', idx, -1)">
                      <el-icon><Top /></el-icon>
                    </el-button>
                    <el-button size="small" :disabled="idx === form.navLinks.length - 1" @click="moveArrayItem('navLinks', idx, 1)">
                      <el-icon><Bottom /></el-icon>
                    </el-button>
                    <el-button size="small" type="danger" text :disabled="form.navLinks.length <= 1" @click="removeArrayItem('navLinks', idx)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
                <el-form label-width="80px" label-position="left" class="array-card-form">
                  <el-form-item label="类型">
                    <el-select v-model="item.type" style="width: 160px;">
                      <el-option label="页面锚点" value="hash" />
                      <el-option label="路由页面" value="route" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="标识 key">
                    <el-input v-model="item.key" maxlength="32" />
                  </el-form-item>
                  <el-form-item label="显示文字">
                    <el-input v-model="item.label" maxlength="16" />
                  </el-form-item>
                  <el-form-item v-if="item.type === 'hash'" label="锚点 #">
                    <el-input v-model="item.hash" maxlength="32" />
                  </el-form-item>
                  <el-form-item v-if="item.type === 'route'" label="路由路径">
                    <el-input v-model="item.route" maxlength="128" />
                  </el-form-item>
                </el-form>
              </div>
            </div>
          </el-card>
        </template>

        <!-- Save Actions -->
        <div class="settings-actions">
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="!hasChanges"
            @click="handleSave"
          >
            <el-icon v-if="!submitting"><Check /></el-icon>
            <span>{{ submitting ? '保存中...' : '保存设置' }}</span>
          </el-button>
          <el-button
            :disabled="!hasChanges || submitting"
            @click="handleReset"
          >
            重置
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  House, Check, Plus, Top, Bottom, Delete,
  Picture, Trophy, Present, ChatLineSquare, QuestionFilled, Camera,
  DataAnalysis, Promotion, Document, Connection,
} from '@element-plus/icons-vue'
import { getConfigs, updateConfigs } from '@/api/system'
import { parseJsonArray, normalizeStatItems, normalizeObjectArray } from '@/utils/json'
import type { SysConfigVO } from '@/types/models'

// --------------- Category definitions ---------------
interface CategoryItem {
  key: string
  label: string
  icon: any
}

const categories: CategoryItem[] = [
  { key: 'hero', label: 'Hero 区域', icon: Picture },
  { key: 'culture', label: '文化价值观', icon: Trophy },
  { key: 'benefits', label: '薪酬福利', icon: Present },
  { key: 'testimonials', label: '员工心声', icon: ChatLineSquare },
  { key: 'faq', label: '常见问题', icon: QuestionFilled },
  { key: 'gallery', label: '团队风采', icon: Camera },
  { key: 'stats', label: '统计数据', icon: DataAnalysis },
  { key: 'cta', label: '行动号召', icon: Promotion },
  { key: 'footer', label: '页脚配置', icon: Document },
  { key: 'nav', label: '导航栏', icon: Connection },
]

const activeCategory = ref('hero')

// --------------- Form state ---------------
interface HeroStat {
  number: number
  suffix: string
  label: string
}

interface CultureCardItem {
  title: string
  description: string
  iconBg: string
}

interface BenefitsItem {
  title: string
  description: string
  colorClass: string
}

interface TestimonialItem {
  quote: string
  name: string
  initials: string
  role: string
  avatarClass: string
}

interface FaqItem {
  question: string
  answer: string
}

interface GalleryItem {
  caption: string
  subcaption: string
  gradientClass: string
}

interface StatsItem { number: number; suffix: string; label: string }
interface FooterCityItem { label: string; icon: string }
interface FooterLinkItem { label: string; href: string }
interface NavLinkItem { type: string; key: string; label: string; hash?: string; route?: string }

interface CareersForm {
  // Hero
  heroBadge: string
  heroTitleMain: string
  heroTitleGradient: string
  heroDesc: string
  heroCtaPrimaryLabel: string
  heroCtaOutlineLabel: string
  heroStats: HeroStat[]
  // Culture
  cultureTitle: string
  cultureSubtitle: string
  cultureCards: CultureCardItem[]
  // Benefits
  benefitsTitle: string
  benefitsSubtitle: string
  benefitsItems: BenefitsItem[]
  // Testimonials
  testimonialsTitle: string
  testimonialsSubtitle: string
  testimonials: TestimonialItem[]
  // FAQ
  faqTitle: string
  faqSubtitle: string
  faqItems: FaqItem[]
  // Gallery
  galleryTitle: string
  gallerySubtitle: string
  galleryItems: GalleryItem[]
  // Stats
  statsItems: StatsItem[]
  // CTA
  ctaTitle: string
  ctaDesc: string
  ctaBtnLabel: string
  ctaBtnLink: string
  // Footer
  footerBrandDesc: string
  footerCopyright: string
  footerContactEmail: string
  footerContactCities: FooterCityItem[]
  footerCategoryTitle: string
  footerCategoryLinks: FooterLinkItem[]
  footerAboutTitle: string
  footerAboutLinks: FooterLinkItem[]
  // Nav
  navLogoText: string
  navCtaLabel: string
  navLinks: NavLinkItem[]
}

// Default values for array items
/** Hero 统计数据默认示例：与官网首页硬编码兜底保持一致，避免配置缺失时页面空白。 */
const defaultHeroStats: HeroStat[] = [
  { number: 1200, suffix: '', label: '全球员工' },
  { number: 18, suffix: '', label: '全球办公城市' },
  { number: 500, suffix: '万+', label: '服务企业客户' },
  { number: 96, suffix: '%', label: '员工推荐率' },
]
const cultureCardDefaults: CultureCardItem = { title: '', description: '', iconBg: 'icon-purple' }
const benefitsItemDefaults: BenefitsItem = { title: '', description: '', colorClass: 'c1' }
const testimonialDefaults: TestimonialItem = { quote: '', name: '', initials: '', role: '', avatarClass: 'ta1' }
const faqItemDefaults: FaqItem = { question: '', answer: '' }
const galleryItemDefaults: GalleryItem = { caption: '', subcaption: '', gradientClass: 'g1' }
const statsItemDefaults: StatsItem = { number: 0, suffix: '', label: '' }
/** 统计数据条目默认示例：与官网首页 StatsBar 兜底保持一致。 */
const defaultStatsItems: StatsItem[] = [
  { number: 1200, suffix: '', label: '员工' },
  { number: 18, suffix: '', label: '城市' },
  { number: 500, suffix: '万+', label: '企业客户' },
  { number: 96, suffix: '%', label: '推荐率' },
]
/** 文化卡片默认示例：与官网首页兜底保持一致，避免配置缺失时页面空白。 */
const defaultCultureCards: CultureCardItem[] = [
  { title: '技术驱动创新', description: '深度投入 AI / LLM 前沿技术领域，每年技术投入占比营收 25%+，让工程师站在技术浪潮之巅。', iconBg: 'icon-purple' },
  { title: '高速成长通道', description: '双阶梯晋升体系（管理 + 专家），每半年一次晋升窗口，优秀人才不受年限破格提拔。', iconBg: 'icon-rose' },
  { title: '开放包容文化', description: '扁平化组织，坦诚清晰沟通，CEO 定期全员 AMA。多元背景人才汇聚，尊重每一种声音。', iconBg: 'icon-amber' },
  { title: '数据驱动决策', description: 'A/B 实验文化深入骨髓，从产品功能到内部流程，一切以数据说话，拒绝拍脑袋。', iconBg: 'icon-emerald' },
  { title: '顶级工具与资源', description: 'MacBook Pro + 4K 显示器标配，正版 IDE / AI 工具全报销。提供丰富技术会议与培训资源。', iconBg: 'icon-cyan' },
  { title: '创业精神永续', description: '保持 Day 1 心态，鼓励内部创业与孵化项目。优秀内部项目可获得种子投资与独立运营机会。', iconBg: 'icon-indigo' },
]
/** 薪酬福利默认示例。 */
const defaultBenefitsItems: BenefitsItem[] = [
  { title: '具有竞争力的薪酬', description: '16 薪起 + 年度调薪，优秀人才享签字费与签约奖金，薪资水平对标一线大厂。', colorClass: 'c1' },
  { title: '全方位健康保障', description: '六险一金（含补充商业保险），年度高端体检，家属体检折扣，EAP 心理援助计划。', colorClass: 'c2' },
  { title: '弹性工作与假期', description: '弹性上下班 + 混合办公（每周可选 2 天远程），12 天带薪年假起，带薪病假不限额。', colorClass: 'c3' },
  { title: '成长学习基金', description: '年度 8,000 元学习基金，覆盖课程 / 书籍 / 会议。内部技术分享 + 外部专家讲座常态化。', colorClass: 'c4' },
  { title: '股权激励计划', description: '核心岗位授予期权 / RSU，与公司共享成长红利。每年新增授予以持续激励。', colorClass: 'c5' },
  { title: '安居乐业支持', description: '安家补贴 + 租房补贴，首次入职异地搬迁全额报销。购房免息借款助力安家。', colorClass: 'c6' },
  { title: '专利与论文激励', description: '专利申请奖 10,000 元起，顶级会议论文奖金 20,000 元。鼓励技术创新与学术贡献。', colorClass: 'c7' },
  { title: '全球轮岗机会', description: '北京 / 上海 / 深圳 / 杭州等城市自由轮岗，未来开放海外办公室短期交换机会。', colorClass: 'c8' },
]
/** 员工心声默认示例。 */
const defaultTestimonials: TestimonialItem[] = [
  { quote: '加入 SmartRecruit 三年，从一名普通工程师成长为 AI 算法负责人。这里不仅有顶尖的技术氛围，更重要的是给了我充分的自主权和试错空间。每年两次的晋升窗口让成长路径非常清晰。', name: '张明远', initials: '张', role: 'AI 算法专家 · 2023 年加入', avatarClass: 'ta1' },
  { quote: '作为两个孩子的妈妈，我最看重的就是工作与生活的平衡。SmartRecruit 的弹性工作制和混合办公政策让我既能全心投入工作，又不错过孩子的成长。公司对女性员工的关怀非常到位。', name: '林晓萌', initials: '林', role: '产品总监 · 2022 年加入', avatarClass: 'ta2' },
  { quote: '在上一家公司做了五年螺丝钉，来到 SmartRecruit 最大的感受是「被看见」。你的想法会被认真对待，做出的成果会被认可。扁平化的组织让新人也有机会直接跟 VP 级别的前辈交流。', name: '陈浩然', initials: '陈', role: '高级后端工程师 · 2024 年加入', avatarClass: 'ta3' },
]
/** 常见问题默认示例。 */
const defaultFaqItems: FaqItem[] = [
  { question: '招聘流程是怎样的？', answer: '简历投递 → 简历筛选（1-3 个工作日）→ 技术面试（2-3 轮，通常含编程考察与系统设计）→ HR 面试（文化匹配与职业规划）→ Offer 沟通 → 正式入职。整体流程通常 2-3 周内完成。' },
  { question: '是否支持远程办公？', answer: '我们采用混合办公模式，每周可选 2 天远程办公，核心协作时间（如重要会议、团队站会）需到岗参与。部分岗位（如标注、客服）支持全远程。' },
  { question: '技术面试主要考察什么？', answer: '我们不考八股文，更关注计算机基础（数据结构、算法复杂度）、工程能力（代码质量、系统设计、调试思路）以及领域深度。面试过程是双向交流，也欢迎你反向考察团队。' },
  { question: '应届生有培训体系吗？', answer: '有的。「星火计划」是我们为新人和应届生设计的系统培养体系，包括：① 1 对 1 Mentor 带教；② 6 个月轮岗了解公司核心业务；③ 定期的技术沙龙与软技能培训；④ 毕业答辩 + 定岗定级。' },
  { question: '可以同时投递多个岗位吗？', answer: '最多同时投递 3 个岗位。我们建议根据自身兴趣与能力精准投递，HR 也会在筛选阶段结合你的背景推荐最合适的岗位方向。' },
]
/** 团队风采默认示例。 */
const defaultGalleryItems: GalleryItem[] = [
  { caption: '黑客马拉松 2026', subcaption: '48 小时极限编程挑战', gradientClass: 'g1' },
  { caption: '团队 Offsite 团建', subcaption: '在山水间凝聚团队力量', gradientClass: 'g2' },
  { caption: '开放式办公环境', subcaption: '激发创造力的灵感空间', gradientClass: 'g3' },
  { caption: 'AI 技术分享会', subcaption: '与大咖共话技术前沿', gradientClass: 'g4' },
  { caption: '年会盛典', subcaption: '年度高光时刻', gradientClass: 'g5' },
  { caption: '新员工训练营', subcaption: '星火计划 · 融入之旅', gradientClass: 'g1' },
]
const footerCityDefaults: FooterCityItem = { label: '', icon: 'location' }
const footerLinkDefaults: FooterLinkItem = { label: '', href: '#' }
const navLinkDefaults: NavLinkItem = { type: 'hash', key: '', label: '', hash: '' }
/** 页脚办公城市默认示例。 */
const defaultFooterCities: FooterCityItem[] = [
  { label: '北京 · 海淀区', icon: 'location' },
  { label: '上海 · 浦东新区', icon: 'location' },
  { label: '深圳 · 南山区', icon: 'location' },
  { label: '杭州 · 余杭区', icon: 'location' },
]
/** 页脚分类链接默认示例。 */
const defaultFooterCategoryLinks: FooterLinkItem[] = [
  { label: '技术研发', href: '/jobs?cat=tech' },
  { label: '产品与设计', href: '/jobs?cat=product' },
  { label: '数据与 AI', href: '/jobs?cat=data' },
  { label: '市场与销售', href: '/jobs?cat=market' },
  { label: '运营与职能', href: '/jobs?cat=ops' },
]
/** 页脚关于我们链接默认示例。 */
const defaultFooterAboutLinks: FooterLinkItem[] = [
  { label: '企业文化', href: '#' },
  { label: '薪酬福利', href: '#' },
  { label: '工作生活', href: '#' },
  { label: '常见问题', href: '#' },
  { label: '联系我们', href: '#' },
]
/** 导航栏链接默认示例。 */
const defaultNavLinks: NavLinkItem[] = [
  { type: 'hash', key: 'culture', label: '企业文化', hash: 'culture' },
  { type: 'route', key: 'social', label: '社会招聘', route: '/social-recruitment' },
  { type: 'route', key: 'campus', label: '校园招聘', route: '/campus-recruitment' },
  { type: 'hash', key: 'benefits', label: '薪酬福利', hash: 'benefits' },
  { type: 'hash', key: 'life', label: '工作生活', hash: 'life' },
  { type: 'hash', key: 'faq', label: '常见问题', hash: 'faq' },
]

const form = reactive<CareersForm>({
  heroBadge: '',
  heroTitleMain: '',
  heroTitleGradient: '',
  heroDesc: '',
  heroCtaPrimaryLabel: '',
  heroCtaOutlineLabel: '',
  heroStats: defaultHeroStats.map((s) => ({ ...s })),
  cultureTitle: '',
  cultureSubtitle: '',
  cultureCards: defaultCultureCards.map((c) => ({ ...c })),
  benefitsTitle: '',
  benefitsSubtitle: '',
  benefitsItems: defaultBenefitsItems.map((b) => ({ ...b })),
  testimonialsTitle: '',
  testimonialsSubtitle: '',
  testimonials: defaultTestimonials.map((t) => ({ ...t })),
  faqTitle: '',
  faqSubtitle: '',
  faqItems: defaultFaqItems.map((f) => ({ ...f })),
  galleryTitle: '',
  gallerySubtitle: '',
  galleryItems: defaultGalleryItems.map((g) => ({ ...g })),
  statsItems: defaultStatsItems.map((s) => ({ ...s })),
  ctaTitle: '',
  ctaDesc: '',
  ctaBtnLabel: '',
  ctaBtnLink: '',
  footerBrandDesc: '',
  footerCopyright: '',
  footerContactEmail: '',
  footerContactCities: defaultFooterCities.map((c) => ({ ...c })),
  footerCategoryTitle: '',
  footerCategoryLinks: defaultFooterCategoryLinks.map((l) => ({ ...l })),
  footerAboutTitle: '',
  footerAboutLinks: defaultFooterAboutLinks.map((l) => ({ ...l })),
  navLogoText: '',
  navCtaLabel: '',
  navLinks: defaultNavLinks.map((l) => ({ ...l })),
})

// Select options
const cultureIconOptions = [
  { value: 'icon-purple', label: '紫色' },
  { value: 'icon-rose', label: '玫瑰红' },
  { value: 'icon-amber', label: '琥珀' },
  { value: 'icon-emerald', label: '翠绿' },
  { value: 'icon-cyan', label: '青色' },
  { value: 'icon-indigo', label: '靛蓝' },
]

const colorClassOptions = [
  { value: 'c1', label: '蓝色' },
  { value: 'c2', label: '红色' },
  { value: 'c3', label: '橙色' },
  { value: 'c4', label: '绿色' },
  { value: 'c5', label: '蓝色(深)' },
  { value: 'c6', label: '青色' },
  { value: 'c7', label: '紫色' },
  { value: 'c8', label: '亮绿' },
]

const avatarClassOptions = [
  { value: 'ta1', label: '蓝紫' },
  { value: 'ta2', label: '红粉' },
  { value: 'ta3', label: '绿' },
]

const gradientClassOptions = [
  { value: 'g1', label: '紫蓝' },
  { value: 'g2', label: '粉红' },
  { value: 'g3', label: '天蓝' },
  { value: 'g4', label: '绿青' },
  { value: 'g5', label: '橙黄' },
]

const submitting = ref(false)

// --------------- change detection ---------------
const originalValues = reactive<Record<string, string>>({})

const jsonArrayFields = ['heroStats', 'cultureCards', 'benefitsItems', 'testimonials', 'faqItems', 'galleryItems', 'statsItems', 'footerContactCities', 'footerCategoryLinks', 'footerAboutLinks', 'navLinks']

const hasChanges = computed(() => {
  const fields = categoryFields[activeCategory.value]
  if (!fields) return false
  return fields.some(f => {
    const v1 = jsonArrayFields.includes(f)
      ? JSON.stringify((form as any)[f])
      : String((form as any)[f] ?? '')
    const v2 = originalValues[f] ?? ''
    return v1 !== v2
  })
})

// Map category to form fields
const categoryFields: Record<string, string[]> = {
  hero: ['heroBadge', 'heroTitleMain', 'heroTitleGradient', 'heroDesc', 'heroCtaPrimaryLabel', 'heroCtaOutlineLabel', 'heroStats'],
  culture: ['cultureTitle', 'cultureSubtitle', 'cultureCards'],
  benefits: ['benefitsTitle', 'benefitsSubtitle', 'benefitsItems'],
  testimonials: ['testimonialsTitle', 'testimonialsSubtitle', 'testimonials'],
  faq: ['faqTitle', 'faqSubtitle', 'faqItems'],
  gallery: ['galleryTitle', 'gallerySubtitle', 'galleryItems'],
  stats: ['statsItems'],
  cta: ['ctaTitle', 'ctaDesc', 'ctaBtnLabel', 'ctaBtnLink'],
  footer: ['footerBrandDesc', 'footerCopyright', 'footerContactEmail', 'footerContactCities', 'footerCategoryTitle', 'footerCategoryLinks', 'footerAboutTitle', 'footerAboutLinks'],
  nav: ['navLogoText', 'navCtaLabel', 'navLinks'],
}

// Field-to-configKey mapping
const fieldToKey: Record<string, string> = {
  heroBadge: 'careers_hero_badge',
  heroTitleMain: 'careers_hero_title_main',
  heroTitleGradient: 'careers_hero_title_gradient',
  heroDesc: 'careers_hero_desc',
  heroCtaPrimaryLabel: 'careers_hero_cta_primary_label',
  heroCtaOutlineLabel: 'careers_hero_cta_outline_label',
  heroStats: 'careers_hero_stats',
  cultureTitle: 'careers_culture_title',
  cultureSubtitle: 'careers_culture_subtitle',
  cultureCards: 'careers_culture_cards',
  benefitsTitle: 'careers_benefits_title',
  benefitsSubtitle: 'careers_benefits_subtitle',
  benefitsItems: 'careers_benefits_items',
  testimonialsTitle: 'careers_testimonials_title',
  testimonialsSubtitle: 'careers_testimonials_subtitle',
  testimonials: 'careers_testimonials',
  faqTitle: 'careers_faq_title',
  faqSubtitle: 'careers_faq_subtitle',
  faqItems: 'careers_faq_items',
  galleryTitle: 'careers_gallery_title',
  gallerySubtitle: 'careers_gallery_subtitle',
  galleryItems: 'careers_gallery_items',
  statsItems: 'careers_stats_items',
  ctaTitle: 'careers_cta_title',
  ctaDesc: 'careers_cta_desc',
  ctaBtnLabel: 'careers_cta_btn_label',
  ctaBtnLink: 'careers_cta_btn_link',
  footerBrandDesc: 'careers_footer_desc',
  footerCopyright: 'careers_footer_copyright',
  footerContactEmail: 'careers_footer_contact_email',
  footerContactCities: 'careers_footer_contact_cities',
  footerCategoryTitle: 'careers_footer_category_title',
  footerCategoryLinks: 'careers_footer_category_links',
  footerAboutTitle: 'careers_footer_about_title',
  footerAboutLinks: 'careers_footer_about_links',
  navLogoText: 'careers_nav_logo_text',
  navCtaLabel: 'careers_nav_cta_label',
  navLinks: 'careers_nav_links',
}

const keyToField = Object.fromEntries(
  Object.entries(fieldToKey).map(([k, v]) => [v, k])
)

// --------------- Array item operations ---------------
function addArrayItem(field: string, defaults: any) {
  ;(form as any)[field].push({ ...defaults })
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

// --------------- Lifecycle ---------------
onMounted(async () => {
  try {
    const configs: SysConfigVO[] = await getConfigs()
    for (const config of configs) {
      const field = keyToField[config.configKey]
      if (!field) continue

      if (jsonArrayFields.includes(field)) {
        const fallback = (form as any)[field] as any[]
        const parsed = parseJsonArray<any>(config.configValue, fallback)
        if (field === 'heroStats' || field === 'statsItems') {
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
  } catch {
    // fallback to defaults
  }

  snapshotAll()
})

function snapshotAll() {
  for (const field of Object.keys(fieldToKey)) {
    if (jsonArrayFields.includes(field)) {
      originalValues[field] = JSON.stringify((form as any)[field])
    } else {
      originalValues[field] = String((form as any)[field] ?? '')
    }
  }
}

// --------------- Category switch ---------------
function onCategorySelect(key: string) {
  activeCategory.value = key
}

// --------------- Save / Reset ---------------
async function handleSave() {
  submitting.value = true
  try {
    const fields = categoryFields[activeCategory.value]
    const items = fields.map(f => ({
      configKey: fieldToKey[f],
      configValue: jsonArrayFields.includes(f)
        ? JSON.stringify((form as any)[f])
        : String((form as any)[f] ?? ''),
    }))

    await updateConfigs(items)

    // Update snapshot for current category
    for (const f of fields) {
      if (jsonArrayFields.includes(f)) {
        originalValues[f] = JSON.stringify((form as any)[f])
      } else {
        originalValues[f] = String((form as any)[f] ?? '')
      }
    }

    ElMessage.success(`${categories.find(c => c.key === activeCategory.value)?.label} 保存成功`)
  } catch {
    // HTTP interceptor handles errors
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  const fields = categoryFields[activeCategory.value]
  for (const f of fields) {
    if (jsonArrayFields.includes(f)) {
      try {
        (form as any)[f] = JSON.parse(originalValues[f])
      } catch {
        // keep as-is
      }
    } else {
      (form as any)[f] = originalValues[f]
    }
  }
}
</script>

<style scoped>
.careers-settings-page {
  max-width: 1100px;
}

/* Page Header */
.page-header {
  margin-bottom: 24px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--c-text);
  margin-bottom: 4px;
}

/* Layout */
.settings-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* Left Sidebar */
.settings-sidebar {
  flex-shrink: 0;
  width: 180px;
}

.category-menu {
  border-right: none;
  border-radius: var(--c-radius-lg);
  overflow: hidden;
}

.category-menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  font-size: 14px;
}

.category-menu :deep(.el-menu-item .el-icon) {
  margin-right: 8px;
}

/* Right Content */
.settings-content {
  flex: 1;
  min-width: 0;
}

.mt-card {
  margin-top: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
}

/* Form */
.field-desc {
  font-size: 12px;
  color: var(--c-text-muted);
  margin-top: 4px;
  margin-left: 12px;
  line-height: 1.5;
}

/* Array list */
.array-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.array-item-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--c-border-light);
}

.array-item-row:last-child {
  border-bottom: none;
}

.array-item-fields {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.array-item-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.array-card {
  background: var(--el-fill-color-lighter);
  border-radius: var(--c-radius-md);
  padding: 16px;
}

.array-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--c-border-light);
}

.array-card-index {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text-secondary);
}

.array-card-form {
  margin-top: 4px;
}

/* Actions */
.settings-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--c-border-light);
}
</style>
