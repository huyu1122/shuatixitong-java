<template>
  <div class="page">
    <h1 class="page-title">欢迎回来，{{ auth.nickname }}</h1>
    <p class="page-desc">今天也来刷几道题吧</p>

    <!-- 数据概览 -->
    <div class="stats-grid" v-if="stats">
      <div class="stat-card">
        <div class="stat-value">{{ stats.totalAttempts }}</div>
        <div class="stat-label">累计答题</div>
      </div>
      <div class="stat-card">
        <div class="stat-value text-success">{{ stats.accuracy }}%</div>
        <div class="stat-label">总正确率</div>
      </div>
      <div class="stat-card">
        <div class="stat-value text-danger">{{ stats.wrongCount }}</div>
        <div class="stat-label">待订正错题</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.practicedQuestions }}/{{ stats.totalQuestions }}</div>
        <div class="stat-label">已练题目</div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="card">
      <h3 class="mb-0">快捷开始</h3>
      <div class="quick-actions">
        <router-link :to="{ name: 'practice' }" class="action-card">
          <span class="action-icon">📖</span>
          <div>
            <strong>分类练习</strong>
            <p>按学科与章节专项攻克</p>
          </div>
        </router-link>
        <router-link :to="{ name: 'exam' }" class="action-card">
          <span class="action-icon">📝</span>
          <div>
            <strong>模拟考试</strong>
            <p>限时随机抽题检验水平</p>
          </div>
        </router-link>
        <router-link :to="{ name: 'wrong' }" class="action-card">
          <span class="action-icon">📕</span>
          <div>
            <strong>错题本</strong>
            <p>{{ stats ? stats.wrongCount : 0 }} 道待订正</p>
          </div>
        </router-link>
        <router-link :to="{ name: 'statistics' }" class="action-card">
          <span class="action-icon">📊</span>
          <div>
            <strong>学习统计</strong>
            <p>查看各学科掌握情况</p>
          </div>
        </router-link>
      </div>
    </div>

    <!-- 学科列表 -->
    <div class="card">
      <div class="row-between" style="margin-bottom: 14px">
        <h3 class="mb-0">按学科练习</h3>
        <span class="text-soft text-sm">{{ subjects.length }} 个学科</span>
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else class="subject-grid">
        <button
          v-for="s in subjects"
          :key="s"
          class="subject-card"
          @click="startPractice(s)"
        >
          <span class="subject-name">{{ s }}</span>
          <span class="subject-arrow">→</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { metaApi, practiceApi } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

const subjects = ref([])
const stats = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const [subs, st] = await Promise.all([metaApi.subjects(), practiceApi.statistics()])
    subjects.value = subs || []
    stats.value = st
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

function startPractice(subject) {
  router.push({ name: 'practice', query: { subject } })
}
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  background: var(--card);
  border-radius: var(--radius);
  padding: 18px;
  box-shadow: var(--shadow);
}

.stat-value { font-size: 26px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 13px; color: var(--text-soft); margin-top: 2px; }

.quick-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 9px;
  color: var(--text);
  transition: all .15s;
}

.action-card:hover {
  border-color: var(--primary-light);
  background: #fafcff;
  transform: translateY(-1px);
}

.action-icon { font-size: 26px; }
.action-card strong { display: block; font-size: 15px; }
.action-card p { margin: 2px 0 0; font-size: 12px; color: var(--text-soft); }

.subject-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
}

.subject-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: var(--card);
  font-size: 14px;
  transition: all .15s;
  text-align: left;
}

.subject-card:hover {
  border-color: var(--primary-light);
  background: var(--primary-bg);
  color: var(--primary);
}

.subject-name { font-weight: 500; }
.subject-arrow { color: var(--text-soft); }
.subject-card:hover .subject-arrow { color: var(--primary); }
</style>