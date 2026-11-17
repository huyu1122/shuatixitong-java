<template>
  <div class="page">
    <h1 class="page-title">学习统计</h1>
    <p class="page-desc">查看累计学习数据与各学科掌握情况</p>

    <div v-if="loading" class="loading">加载中...</div>

    <template v-else-if="stats">
      <!-- 关键指标 -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-value">{{ stats.totalAttempts }}</div>
          <div class="stat-label">累计答题次数</div>
        </div>
        <div class="stat-card">
          <div class="stat-value text-success">{{ stats.accuracy }}%</div>
          <div class="stat-label">总正确率</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ stats.practicedQuestions }} / {{ stats.totalQuestions }}</div>
          <div class="stat-label">已练题目 / 题库总量</div>
        </div>
        <div class="stat-card">
          <div class="stat-value text-danger">{{ stats.wrongCount }}</div>
          <div class="stat-label">待订正错题</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ stats.examCount }}</div>
          <div class="stat-label">模拟考试次数</div>
        </div>
      </div>

      <!-- 学科掌握情况 -->
      <div class="card">
        <h3 class="mb-0">学科掌握情况</h3>
        <p class="text-soft text-sm" style="margin: 4px 0 14px">按答题次数排序，正确率越低说明越需要加强</p>

        <div v-if="!stats.subjectStats?.length" class="empty">
          <p>还没有答题记录，去练习几道题吧</p>
        </div>

        <div v-else class="subject-list">
          <div v-for="s in stats.subjectStats" :key="s.subject" class="subject-row">
            <div class="subject-head">
              <span class="subject-name">{{ s.subject }}</span>
              <span class="text-soft text-sm">
                答 {{ s.attempts }} 次 · 正确率
                <strong :class="accuracyClass(s.accuracy)">{{ s.accuracy }}%</strong>
              </span>
            </div>
            <div class="bar-track">
              <div
                class="bar-fill"
                :class="accuracyClass(s.accuracy)"
                :style="{ width: s.accuracy + '%' }"
              ></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 考试历史 -->
      <div class="card">
        <h3 class="mb-0">模拟考试记录</h3>

        <div v-if="!exams.length" class="empty">
          <p>还没有考试记录</p>
        </div>

        <table v-else class="table mt-8">
          <thead>
            <tr>
              <th>考试范围</th>
              <th>题数</th>
              <th>答对</th>
              <th>得分</th>
              <th>用时</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="e in exams" :key="e.id">
              <td>{{ e.scope }}</td>
              <td>{{ e.total }}</td>
              <td>{{ e.correctCount }}</td>
              <td>
                <strong :class="e.score >= 60 ? 'text-success' : 'text-danger'">{{ e.score }}</strong>
              </td>
              <td>{{ formatDuration(e.durationSeconds) }}</td>
              <td class="text-soft text-sm">{{ formatTime(e.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { practiceApi } from '../api'

const stats = ref(null)
const exams = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const [st, hist] = await Promise.all([
      practiceApi.statistics(),
      practiceApi.examHistory()
    ])
    stats.value = st
    exams.value = hist || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

function accuracyClass(v) {
  if (v >= 85) return 'text-success'
  if (v >= 60) return 'text-warning'
  return 'text-danger'
}

function formatDuration(sec) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}分${s}秒`
}

function formatTime(iso) {
  if (!iso) return ''
  return iso.replace('T', ' ').slice(0, 16)
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

.stat-value { font-size: 24px; font-weight: 700; line-height: 1.25; }
.stat-label { font-size: 13px; color: var(--text-soft); margin-top: 2px; }

.subject-list { display: flex; flex-direction: column; gap: 16px; }

.subject-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  gap: 12px;
  flex-wrap: wrap;
}

.subject-name { font-weight: 500; }

.bar-track {
  height: 8px;
  background: #eef1f5;
  border-radius: 999px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width .3s;
}

.bar-fill.text-success { background: var(--success); }
.bar-fill.text-warning { background: var(--warning); }
.bar-fill.text-danger { background: var(--danger); }

.text-warning { color: var(--warning); }
</style>