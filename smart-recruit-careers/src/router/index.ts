import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: 'SmartRecruit - 加入我们' },
  },
  {
    path: '/social-recruitment',
    name: 'SocialRecruitment',
    component: () => import('@/views/SocialRecruitmentView.vue'),
    meta: { title: '社会招聘 - SmartRecruit' },
  },
  {
    path: '/campus-recruitment',
    name: 'CampusRecruitment',
    component: () => import('@/views/CampusRecruitmentView.vue'),
    meta: { title: '校园招聘 - SmartRecruit' },
  },
  {
    path: '/jobs',
    name: 'Jobs',
    component: () => import('@/views/JobListView.vue'),
    meta: { title: '热招职位' },
  },
  {
    path: '/jobs/:id',
    name: 'JobDetail',
    component: () => import('@/views/JobDetailView.vue'),
    meta: { title: '职位详情' },
  },
  {
    path: '/login',
    redirect: '/phone-login',
  },
  {
    path: '/phone-login',
    name: 'PhoneLogin',
    component: () => import('@/views/auth/PhoneLoginView.vue'),
    meta: { title: '手机号登录' },
  },
  {
    path: '/my-applications',
    name: 'MyApplications',
    component: () => import('@/views/referral/MyApplicationsView.vue'),
    meta: { title: '我的投递' },
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/profile/ProfileView.vue'),
    meta: { title: '个人信息' },
  },
  {
    path: '/p/:token',
    name: 'ReferralLanding',
    component: () => import('@/views/referral/ReferralLandingView.vue'),
    meta: { title: '内推计划' },
  },
  {
    path: '/p/:token/job/:programJobId',
    name: 'ReferralJobLanding',
    component: () => import('@/views/referral/ReferralJobLandingView.vue'),
    meta: { title: '职位详情' },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to) {
    if (to.hash) {
      // Defer hash scrolling to the component (NavBar.handleNavClick)
      // — async HomeView children may not be mounted yet at this point
      return false
    }
    return { top: 0 }
  },
})

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  // Logged-in users visiting phone-login → redirect to requested page or home
  if (to.name === 'PhoneLogin' && userStore.isLoggedIn) {
    const redirect = (to.query.redirect as string) || '/my-applications'
    next(redirect)
    return
  }

  next()
})

export default router
