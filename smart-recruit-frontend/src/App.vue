<template>
  <MainLayout v-if="showLayout">
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </MainLayout>
  <router-view v-else />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import MainLayout from '@/layouts/MainLayout.vue'

const route = useRoute()
const userStore = useUserStore()

/** Routes that render standalone (no admin layout). */
const publicRouteNames = new Set([
  'Login',
  'Register',
  'ForgotPassword',
  'PhoneLogin',
  'Assessment',
  'ReferralLanding',
  'ReferralJobLanding',
  'ReferralJobLandingWithToken',
  'ReferralPositionDetail',
  'MyApplications',
  'ContractSign',
])

const showLayout = computed(() => {
  if (userStore.isCandidate) return false
  return !publicRouteNames.has(route.name as string)
})
</script>
