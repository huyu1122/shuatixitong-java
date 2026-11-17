<template>
  <header class="header">
    <div class="header-inner">
      <router-link to="/" class="brand">
        <span class="brand-logo">学</span>
        <span class="brand-text">
          <strong>学而刷题</strong>
          <small>大学生在线刷题系统</small>
        </span>
      </router-link>

      <nav class="nav">
        <router-link
          v-for="item in navItems"
          :key="item.name"
          :to="{ name: item.name }"
          class="nav-item"
          active-class="active"
        >{{ item.label }}</router-link>
        <!-- 仅管理员可见 -->
        <router-link
          v-if="isAdmin"
          :to="{ name: 'admin' }"
          class="nav-item nav-admin"
          active-class="active"
        >后台管理</router-link>
      </nav>

      <div class="user-area">
        <span class="nickname">{{ auth.nickname }}</span>
        <button class="btn btn-sm" @click="handleLogout">退出</button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

const isAdmin = computed(() => auth.user?.role === 'ADMIN')

const navItems = [
  { name: 'home', label: '首页' },
  { name: 'practice', label: '开始练习' },
  { name: 'exam', label: '模拟考试' },
  { name: 'wrong', label: '错题本' },
  { name: 'favorites', label: '收藏夹' },
  { name: 'statistics', label: '学习统计' }
]

async function handleLogout() {
  await auth.logout()
  router.push({ name: 'login' })
}
</script>

<style scoped>
.header {
  background: var(--card);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 50;
}

.header-inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.brand { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }

.brand-logo {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
}

.brand-text { display: flex; flex-direction: column; line-height: 1.2; }
.brand-text strong { font-size: 15px; color: var(--text); }
.brand-text small { font-size: 11px; color: var(--text-soft); }

.nav { display: flex; gap: 4px; flex: 1; overflow-x: auto; }

.nav-item {
  padding: 6px 12px;
  border-radius: 7px;
  font-size: 14px;
  color: var(--text-soft);
  white-space: nowrap;
  transition: all .15s;
}

.nav-item:hover { background: var(--primary-bg); color: var(--primary); }
.nav-item.active { background: var(--primary-bg); color: var(--primary); font-weight: 600; }

/* 管理入口用不同颜色区分，避免与学生功能混淆 */
.nav-admin { color: var(--warning); }
.nav-admin:hover, .nav-admin.active { background: var(--warning-bg); color: var(--warning); }

.user-area { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.nickname { font-size: 13px; color: var(--text-soft); }

@media (max-width: 860px) {
  .header-inner { height: auto; flex-wrap: wrap; padding: 10px 16px; gap: 12px; }
  .nav { order: 3; width: 100%; }
  .brand-text small { display: none; }
}
</style>