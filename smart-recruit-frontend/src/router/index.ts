import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '注册' },
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/auth/ForgotPasswordView.vue'),
    meta: { title: '找回密码' },
  },
  {
    path: '/phone-login',
    name: 'PhoneLogin',
    component: () => import('@/views/auth/PhoneLoginView.vue'),
    meta: { title: '手机号登录' },
  },
  {
    path: '/offer-confirm/:token',
    name: 'OfferConfirm',
    component: () => import('@/views/offer/OfferConfirmView.vue'),
    meta: { title: 'Offer确认', requiresAuth: false },
  },
  {
    path: '/assessment',
    name: 'Assessment',
    component: () => import('@/views/assessment/AssessmentView.vue'),
    meta: { title: '在线测评' },
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/DashboardView.vue'),
    meta: { title: '工作台', requiresAuth: true },
  },
  {
    path: '/dashboard/activities',
    name: 'Activities',
    component: () => import('@/views/dashboard/ActivitiesView.vue'),
    meta: { title: '最近动态', requiresAuth: true },
  },
  {
    path: '/dashboard/tasks',
    name: 'Tasks',
    component: () => import('@/views/dashboard/TasksView.vue'),
    meta: { title: '待办事项', requiresAuth: true },
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/profile/ProfileView.vue'),
    meta: { title: '个人信息', requiresAuth: true },
  },
  {
    path: '/jobs',
    name: 'Jobs',
    component: () => import('@/views/job/JobListView.vue'),
    meta: { title: '职位管理', requiresAuth: true },
  },
  {
    path: '/jobs/:id',
    name: 'JobDetail',
    component: () => import('@/views/job/JobDetailView.vue'),
    meta: { title: '职位详情', requiresAuth: true },
  },
  {
    path: '/resumes',
    name: 'Resumes',
    component: () => import('@/views/resume/ResumeScreenView.vue'),
    meta: { title: '简历筛选', requiresAuth: true },
  },
  {
    path: '/resumes/:id',
    name: 'ResumeDetail',
    component: () => import('@/views/resume/ResumeDetailView.vue'),
    meta: { title: '简历详情', requiresAuth: true },
  },
  {
    path: '/candidates',
    name: 'Candidates',
    component: () => import('@/views/candidate/CandidateCenterView.vue'),
    meta: { title: '候选人中心', requiresAuth: true },
  },
  {
    path: '/candidates/:id',
    name: 'CandidateDetail',
    component: () => import('@/views/candidate/CandidateDetailView.vue'),
    meta: { title: '候选人详情', requiresAuth: true },
  },
  {
    path: '/interviews',
    name: 'Interviews',
    component: () => import('@/views/interview/InterviewView.vue'),
    meta: { title: '面试管理', requiresAuth: true },
  },
  {
    path: '/interviews/ai',
    name: 'AIInterviews',
    component: () => import('@/views/interview/AIDrivenInterviewView.vue'),
    meta: { title: 'AI智能出题', requiresAuth: true },
  },
  {
    path: '/question-banks',
    name: 'QuestionBanks',
    component: () => import('@/views/questionBank/QuestionBankView.vue'),
    meta: { title: '面试题库', requiresAuth: true },
  },
  {
    path: '/online-assessments',
    name: 'OnlineAssessments',
    component: () => import('@/views/assessment/OnlineAssessmentView.vue'),
    meta: { title: '在线测评', requiresAuth: true },
  },
  {
    path: '/interviews/:id/report',
    name: 'InterviewReport',
    component: () => import('@/views/interview/InterviewReportView.vue'),
    meta: { title: '面试报告', requiresAuth: true },
  },
  {
    // 面试详情链接（消息通知跳转用）：目前以评估报告页承接
    path: '/interviews/:id',
    redirect: (to) => `/interviews/${to.params.id}/report`,
  },
  {
    path: '/talent-pool',
    name: 'TalentPool',
    component: () => import('@/views/talent/TalentPoolView.vue'),
    meta: { title: '人才库', requiresAuth: true },
  },
  {
    path: '/offers',
    name: 'Offers',
    component: () => import('@/views/offer/OfferView.vue'),
    meta: { title: 'Offer管理', requiresAuth: true },
  },
  {
    path: '/offers/approval',
    name: 'OfferApproval',
    component: () => import('@/views/offer/ApprovalView.vue'),
    meta: { title: 'Offer审批', requiresAuth: true },
  },
  {
    path: '/offers/:id',
    name: 'OfferDetail',
    component: () => import('@/views/offer/OfferDetailView.vue'),
    meta: { title: 'Offer详情', requiresAuth: true },
  },
  {
    path: '/offers/:id/approval-review',
    name: 'OfferApprovalReview',
    component: () => import('@/views/offer/OfferApprovalDetailView.vue'),
    meta: { title: 'Offer审批详情', requiresAuth: true },
  },
  {
    path: '/onboarding',
    name: 'Onboarding',
    component: () => import('@/views/onboarding/OnboardingView.vue'),
    meta: { title: '入职管理', requiresAuth: true },
  },
  {
    path: '/onboarding/:id',
    name: 'OnboardingDetail',
    component: () => import('@/views/onboarding/OnboardingDetailView.vue'),
    meta: { title: '入职详情', requiresAuth: true },
  },
  {
    path: '/contracts',
    name: 'Contracts',
    component: () => import('@/views/contract/ContractView.vue'),
    meta: { title: '合同管理', requiresAuth: true },
  },
  {
    path: '/contracts/:id',
    name: 'ContractDetail',
    component: () => import('@/views/contract/ContractDetailView.vue'),
    meta: { title: '合同详情', requiresAuth: true },
  },
  {
    path: '/contract-sign/:token',
    name: 'ContractSign',
    component: () => import('@/views/contract/ContractSignView.vue'),
    meta: { title: '合同签署', requiresAuth: false },
  },
  {
    path: '/p/:token',
    name: 'ReferralLanding',
    component: () => import('@/views/referral/ReferralLandingView.vue'),
    meta: { title: '内推计划详情', requiresAuth: false },
  },
  {
    path: '/p/job/:programJobId',
    name: 'ReferralJobLanding',
    component: () => import('@/views/referral/ReferralJobLandingView.vue'),
    meta: { title: '职位详情', requiresAuth: false },
  },
  {
    path: '/p/:token/job/:programJobId',
    name: 'ReferralJobLandingWithToken',
    component: () => import('@/views/referral/ReferralJobLandingView.vue'),
    meta: { title: '职位详情', requiresAuth: false },
  },
  {
    path: '/my-applications',
    name: 'MyApplications',
    component: () => import('@/views/referral/ReferralMyApplicationsView.vue'),
    meta: { title: '我的投递', requiresAuth: false },
  },
  {
    path: '/referral',
    name: 'ReferralHome',
    component: () => import('@/views/referral/ReferralHomeView.vue'),
    meta: { title: '内推管理', requiresAuth: true },
  },
  {
    path: '/referral/:id/jobs',
    name: 'ReferralProgramJobs',
    component: () => import('@/views/referral/ProgramJobsView.vue'),
    meta: { title: '职位管理', requiresAuth: true },
  },
  {
    path: '/referral/position/:programJobId',
    name: 'ReferralPositionDetail',
    component: () => import('@/views/referral/ReferralPositionDetailView.vue'),
    meta: { title: '职位详情', requiresAuth: false },
  },
  {
    path: '/referral/:id',
    name: 'ReferralDetail',
    component: () => import('@/views/referral/ReferralDetailView.vue'),
    meta: { title: '内推详情', requiresAuth: true },
  },
  {
    path: '/referral/records',
    name: 'ReferralMy',
    component: () => import('@/views/referral/ReferralMyView.vue'),
    meta: { title: '内推记录', requiresAuth: true },
  },
  {
    path: '/referral/leaderboard',
    name: 'ReferralLeaderboard',
    component: () => import('@/views/referral/ReferralLeaderboardView.vue'),
    meta: { title: '内推排行榜', requiresAuth: true },
  },
  {
    path: '/referral/policy',
    name: 'ReferralPolicy',
    component: () => import('@/views/referral/ReferralPolicyView.vue'),
    meta: { title: '内推政策', requiresAuth: true },
  },
  {
    path: '/referral/poster',
    name: 'ReferralPoster',
    component: () => import('@/views/referral/ReferralPosterView.vue'),
    meta: { title: '内推海报', requiresAuth: true },
  },
  {
    path: '/analytics',
    name: 'Analytics',
    component: () => import('@/views/analytics/AnalyticsView.vue'),
    meta: { title: '数据分析', requiresAuth: true },
  },
  {
    path: '/system/users',
    name: 'Users',
    component: () => import('@/views/system/UserView.vue'),
    meta: { title: '用户管理', requiresAuth: true },
  },
  {
    path: '/system/roles',
    name: 'Roles',
    component: () => import('@/views/system/RoleView.vue'),
    meta: { title: '角色管理', requiresAuth: true },
  },
  {
    path: '/system/departments',
    name: 'Departments',
    component: () => import('@/views/system/DepartmentView.vue'),
    meta: { title: '部门管理', requiresAuth: true },
  },
  {
    path: '/system/permissions',
    name: 'Permissions',
    component: () => import('@/views/system/PermissionView.vue'),
    meta: { title: '权限管理', requiresAuth: true },
  },
  {
    path: '/system/logs',
    name: 'AuditLogs',
    component: () => import('@/views/system/AuditLogView.vue'),
    meta: { title: '审计日志查询', requiresAuth: true },
  },
  {
    path: '/system/settings',
    name: 'SystemSettings',
    component: () => import('@/views/system/SystemSettingsView.vue'),
    meta: { title: '系统设置', requiresAuth: true },
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/system/NotificationView.vue'),
    meta: { title: '消息通知', requiresAuth: true },
  },
  {
    path: '/careers/config',
    name: 'CareersConfig',
    component: () => import('@/views/careers/CareersConfigView.vue'),
    meta: { title: '官网配置', requiresAuth: true },
  },
  {
    path: '/careers/hot-jobs',
    name: 'CareersHotJobs',
    component: () => import('@/views/careers/HotJobsConfigView.vue'),
    meta: { title: '热门职位', requiresAuth: true },
  },
  {
    path: '/careers/social',
    name: 'CareersSocialRecruitment',
    component: () => import('@/views/careers/SocialRecruitmentConfigView.vue'),
    meta: { title: '社会招聘', requiresAuth: true },
  },
  {
    path: '/careers/campus',
    name: 'CareersCampusRecruitment',
    component: () => import('@/views/careers/CampusRecruitmentConfigView.vue'),
    meta: { title: '校园招聘', requiresAuth: true },
  },

  {
    path: '/agents',
    name: 'Agents',
    component: () => import('@/views/agent/AgentMonitorView.vue'),
    meta: { title: 'AI智能编排', requiresAuth: true },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  // 已登录但 store 未初始化时，从服务端获取最新用户信息（页面刷新后首次导航触发）
  if (to.meta.requiresAuth && userStore.isLoggedIn && !userStore.initialized) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      // fetchUserInfo 内部已处理登出逻辑
      return
    }
  }

  // 求职者不允许访问后台管理页面，仅允许少数公开页面
  if (userStore.isCandidate) {
    const allowed = to.path === '/dashboard'
      || to.path === '/my-applications'
      || to.path.startsWith('/my-applications/')
      || to.path.startsWith('/p/')
      || to.path.startsWith('/contract-sign/')
      || to.path.startsWith('/referral/position/')
      || to.path.startsWith('/profile')
    if (!allowed) {
      next('/my-applications')
      return
    }
  }

  if (to.name === 'Login' && userStore.isLoggedIn) {
    next(userStore.isCandidate ? '/my-applications' : { name: 'Dashboard' })
    return
  }

  if (to.name === 'Register' && userStore.isLoggedIn) {
    next(userStore.isCandidate ? '/my-applications' : { name: 'Dashboard' })
    return
  }

  if (to.name === 'ForgotPassword' && userStore.isLoggedIn) {
    next(userStore.isCandidate ? '/my-applications' : { name: 'Dashboard' })
    return
  }

  if (to.name === 'PhoneLogin' && userStore.isLoggedIn) {
    next(userStore.isCandidate ? '/my-applications' : { name: 'Dashboard' })
    return
  }

  next()
})

// 路由切换后同步浏览器标签页标题（系统名称取系统配置，实时生效）
router.afterEach((to) => {
  const settings = useSettingsStore()
  const systemName = settings.systemName
  document.title = to.meta.title
    ? `${to.meta.title} - ${systemName}`
    : `${systemName} - 智能招聘管理系统`
})

export default router
