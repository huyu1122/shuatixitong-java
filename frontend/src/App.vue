<template>
  <div class="app-shell">
    <AppHeader v-if="!isLoginPage" />
    <main>
      <router-view v-slot="{ Component }">
        <component :is="Component" />
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from './components/AppHeader.vue'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const auth = useAuthStore()

const isLoginPage = computed(() => route.name === 'login')

onMounted(() => {
  // 刷新页面后校验 token 是否仍然有效
  auth.refreshMe()
})
</script>

<style scoped>
.app-shell { min-height: 100%; display: flex; flex-direction: column; }
main { flex: 1; }
</style>