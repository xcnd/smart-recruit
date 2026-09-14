<template>
  <div class="profile-root">
    <NavBar />
    <div class="profile-container">
      <h1 class="page-title">个人信息</h1>
      <p class="page-subtitle">管理你的账户信息</p>

      <div class="profile-card" v-if="userStore.userInfo">
        <div class="avatar-section">
          <el-avatar :size="80" :src="userStore.userInfo.avatar" class="user-avatar">
            {{ userStore.displayInitial }}
          </el-avatar>
          <div class="avatar-info">
            <h2>{{ userStore.displayName }}</h2>
            <p class="user-phone">{{ maskPhone(userStore.userInfo.mobile || userStore.userInfo.username || '') }}</p>
          </div>
        </div>

        <el-divider />

        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户ID">{{ userStore.userInfo.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ userStore.userInfo.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ userStore.userInfo.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ maskPhone(userStore.userInfo.mobile || '') }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ userStore.userInfo.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatDate(userStore.userInfo.createTime || '') }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="profile-card" v-else-if="userStore.isLoggedIn">
        <el-skeleton :rows="5" animated />
      </div>

      <div class="profile-card not-logged-in" v-else>
        <div class="empty-state">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="var(--color-text-muted)" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
          </svg>
          <h3>请先登录</h3>
          <p>登录后可查看和管理个人信息</p>
          <router-link to="/phone-login" class="login-link">手机号登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { maskPhone, formatDate } from '@/utils/format'
import NavBar from '@/components/NavBar.vue'

const userStore = useUserStore()

onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.initialized) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      // error handled by store
    }
  }
})
</script>

<style scoped>
.profile-root {
  min-height: 100vh;
  background: var(--color-bg-alt);
}

.profile-container {
  max-width: 720px;
  margin: 0 auto;
  padding: 40px 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--color-text);
  letter-spacing: -0.5px;
  margin-bottom: 6px;
}

.page-subtitle {
  font-size: 15px;
  color: var(--color-text-secondary);
  margin-bottom: 32px;
}

.profile-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: 32px;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 8px;
}

.user-avatar {
  flex-shrink: 0;
  font-size: 32px;
}

.avatar-info h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}

.user-phone {
  font-size: 14px;
  color: var(--color-text-muted);
}

.empty-state {
  text-align: center;
  padding: 48px 24px;
}

.empty-state h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  margin: 16px 0 8px;
}

.empty-state p {
  font-size: 14px;
  color: var(--color-text-muted);
  margin-bottom: 24px;
}

.login-link {
  display: inline-block;
  padding: 10px 28px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #1677ff, var(--color-primary));
  border-radius: var(--radius-full);
  transition: opacity var(--transition);
}

.login-link:hover {
  opacity: 0.9;
}

@media (max-width: 768px) {
  .profile-container {
    padding: 24px 16px;
  }

  .profile-card {
    padding: 20px;
  }

  .avatar-section {
    flex-direction: column;
    text-align: center;
  }
}
</style>
