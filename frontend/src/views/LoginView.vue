<template>
  <div class="login-page">
    <div class="login-box">
      <div class="login-brand">
        <span class="logo">学</span>
        <h1>学而刷题</h1>
        <p>大学生在线刷题系统</p>
      </div>

      <div class="tabs">
        <button
          :class="['tab', { active: mode === 'login' }]"
          @click="switchMode('login')"
        >登录</button>
        <button
          :class="['tab', { active: mode === 'register' }]"
          @click="switchMode('register')"
        >注册</button>
      </div>

      <div v-if="error" class="alert alert-error">{{ error }}</div>

      <form @submit.prevent="submit">
        <div class="field">
          <label>用户名</label>
          <input v-model.trim="form.username" class="input" placeholder="请输入用户名" autocomplete="username" />
        </div>

        <div class="field">
          <label>密码</label>
          <input
            v-model="form.password"
            type="password"
            class="input"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </div>

        <div v-if="mode === 'register'" class="field">
          <label>昵称（可选）</label>
          <input v-model.trim="form.nickname" class="input" placeholder="显示名称" />
        </div>

        <button class="btn btn-primary btn-block" type="submit" :disabled="loading">
          {{ loading ? '处理中...' : (mode === 'login' ? '登录' : '注册并登录') }}
        </button>
      </form>

      <p class="tip">
        {{ mode === 'login' ? '还没有账号？' : '已有账号？' }}
        <a @click="switchMode(mode === 'login' ? 'register' : 'login')">
          {{ mode === 'login' ? '立即注册' : '去登录' }}
        </a>
      </p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const mode = ref('login')
const loading = ref(false)
const error = ref('')

const form = reactive({ username: '', password: '', nickname: '' })

function switchMode(next) {
  mode.value = next
  error.value = ''
}

async function submit() {
  error.value = ''

  if (!form.username) { error.value = '请输入用户名'; return }
  if (!form.password) { error.value = '请输入密码'; return }
  if (mode.value === 'register' && form.password.length < 6) {
    error.value = '密码至少 6 个字符'
    return
  }

  loading.value = true
  try {
    if (mode.value === 'login') {
      await auth.login(form.username, form.password)
    } else {
      await auth.register(form.username, form.password, form.nickname)
    }
    const redirect = route.query.redirect
    router.push(redirect || { name: 'home' })
  } catch (e) {
    error.value = e.message || '操作失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f4e79 0%, #2e6da4 100%);
  padding: 20px;
}

.login-box {
  width: 100%;
  max-width: 400px;
  background: var(--card);
  border-radius: 14px;
  padding: 32px 28px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, .18);
}

.login-brand { text-align: center; margin-bottom: 22px; }

.logo {
  display: inline-flex;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--primary);
  color: #fff;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 10px;
}

.login-brand h1 { font-size: 20px; margin-bottom: 4px; }
.login-brand p { margin: 0; font-size: 13px; color: var(--text-soft); }

.tabs {
  display: flex;
  gap: 4px;
  background: var(--bg);
  padding: 4px;
  border-radius: 9px;
  margin-bottom: 20px;
}

.tab {
  flex: 1;
  padding: 8px;
  border: none;
  border-radius: 7px;
  background: transparent;
  font-size: 14px;
  color: var(--text-soft);
  transition: all .15s;
}

.tab.active { background: var(--card); color: var(--primary); font-weight: 600; box-shadow: var(--shadow); }

.tip { text-align: center; font-size: 13px; color: var(--text-soft); margin: 16px 0 0; }
.tip a { cursor: pointer; }
</style>