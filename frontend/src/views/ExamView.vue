<template>
  <div class="page">
    <h1 class="page-title">模拟考试</h1>
    <p class="page-desc">限时随机抽题，交卷后立即出分并查看逐题解析</p>

    <!-- 考试配置 -->
    <div v-if="phase === 'config'" class="card">
      <div class="config-grid">
        <div class="field mb-0">
          <label>考试范围</label>
          <select v-model="config.subject" class="select">
            <option value="">综合卷（全部学科）</option>
            <option v-for="s in subjects" :key="s" :value="s">{{ s }}</option>
          </select>
        </div>

        <div class="field mb-0">
          <label>题目数量</label>
          <select v-model.number="config.count" class="select">
            <option :value="10">10 题</option>
            <option :value="20">20 题</option>
            <option :value="30">30 题</option>
          </select>
        </div>

        <div class="field mb-0">
          <label>限时（分钟）</label>
          <select v-model.number="config.minutes" class="select">
            <option :value="10">10 分钟</option>
            <option :value="15">15 分钟</option>
            <option :value="30">30 分钟</option>
          </select>
        </div>

        <button class="btn btn-primary" @click="startExam" :disabled="loading">
          {{ loading ? '准备中...' : '开始考试' }}
        </button>
      </div>
    </div>

    <!-- 考试中 -->
    <div v-if="phase === 'exam'">
      <div class="exam-bar">
        <div class="row">
          <span class="tag">{{ config.subject || '综合卷' }}</span>
          <span class="text-sm">第 {{ index + 1 }} / {{ questions.length }} 题</span>
          <span class="text-sm text-soft">已答 {{ answeredCount }} 题</span>
        </div>
        <div class="timer" :class="{ urgent: remaining <= 60 }">
          ⏱ {{ formattedTime }}
        </div>
      </div>

      <QuestionCard
        :question="questions[index]"
        :disabled="true"
        @answer="onAnswer"
      />

      <div class="actions">
        <button class="btn" :disabled="index === 0" @click="index--">上一题</button>
        <button class="btn" :disabled="index >= questions.length - 1" @click="index++">下一题</button>
        <button class="btn btn-primary" @click="confirmSubmit">交卷</button>
      </div>

      <!-- 答题卡 -->
      <div class="card mt-16">
        <h3 class="mb-0">答题卡</h3>
        <div class="sheet">
          <button
            v-for="(q, i) in questions"
            :key="q.id"
            class="sheet-cell"
            :class="{ answered: answers[q.id] !== undefined, current: i === index }"
            @click="index = i"
          >{{ i + 1 }}</button>
        </div>
      </div>
    </div>

    <!-- 成绩单 -->
    <div v-if="phase === 'result'">
      <div class="card score-card">
        <div class="score-ring">
          <span class="score-value">{{ score }}</span>
          <span class="score-label">分</span>
        </div>
        <div class="score-meta">
          <h3 class="mb-0">考试成绩单</h3>
          <p class="text-soft">{{ config.subject || '综合卷' }} · {{ questions.length }} 题 · 用时 {{ usedText }}</p>
          <div class="score-stats">
            <div><strong class="text-success">{{ correctTotal }}</strong><span>答对</span></div>
            <div><strong class="text-danger">{{ questions.length - correctTotal }}</strong><span>答错</span></div>
            <div><strong>{{ Math.round(correctTotal / Math.max(questions.length, 1) * 100) }}%</strong><span>正确率</span></div>
          </div>
          <div class="row mt-16">
            <button class="btn btn-primary" @click="resetExam">再考一次</button>
            <router-link :to="{ name: 'statistics' }" class="btn">查看统计</router-link>
          </div>
        </div>
      </div>

      <div class="card">
        <h3 class="mb-0">逐题解析</h3>
        <div v-for="(q, i) in questions" :key="q.id" class="review-item">
          <div class="review-head">
            <span :class="reviewResult(q).correct ? 'text-success' : 'text-danger'">
              {{ reviewResult(q).correct ? '✓' : '✗' }}
            </span>
            <span class="text-soft">{{ i + 1 }}.</span>
            <MathText :text="q.stem" />
          </div>
          <div class="review-body">
            <div class="text-sm">你的答案：<span :class="reviewResult(q).correct ? 'text-success' : 'text-danger'">
              {{ reviewResult(q).userText }}</span></div>
            <div class="text-sm">正确答案：<span class="text-success">{{ reviewResult(q).correctText }}</span></div>
            <div v-if="q.analysis" class="mt-8 text-sm text-soft">
              <MathText :text="q.analysis" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import QuestionCard from '../components/QuestionCard.vue'
import MathText from '../components/MathText.vue'
import { metaApi, practiceApi } from '../api'

const subjects = ref([])
const phase = ref('config')   // config | exam | result
const loading = ref(false)

const config = ref({ subject: '', count: 10, minutes: 10 })

const questions = ref([])
const index = ref(0)
const answers = ref({})       // questionId -> 用户答案
const results = ref({})       // questionId -> 判分结果

const remaining = ref(0)
const startAt = ref(0)
const usedSeconds = ref(0)
const score = ref(0)
let timer = null

onMounted(async () => {
  try {
    subjects.value = await metaApi.subjects()
  } catch (e) {
    console.error(e)
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const answeredCount = computed(() => Object.keys(answers.value).length)

const formattedTime = computed(() => {
  const m = Math.floor(remaining.value / 60)
  const s = remaining.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const usedText = computed(() => {
  const m = Math.floor(usedSeconds.value / 60)
  const s = usedSeconds.value % 60
  return `${m} 分 ${s} 秒`
})

const correctTotal = computed(() =>
  Object.values(results.value).filter(r => r && r.correct).length
)

async function startExam() {
  loading.value = true
  try {
    const list = await metaApi.random(config.value.count, config.value.subject || undefined)
    if (!list.length) {
      alert('题库中没有可用题目')
      return
    }
    questions.value = list
    index.value = 0
    answers.value = {}
    results.value = {}
    remaining.value = config.value.minutes * 60
    startAt.value = Date.now()
    phase.value = 'exam'
    startTimer()
  } catch (e) {
    alert(e.message)
  } finally {
    loading.value = false
  }
}

function startTimer() {
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    remaining.value--
    if (remaining.value <= 0) {
      clearInterval(timer)
      submitExam(true)
    }
  }, 1000)
}

/** 考试中只记录答案，不即时判分 */
function onAnswer(payload) {
  const q = questions.value[index.value]
  if (!q) return
  answers.value = { ...answers.value, [q.id]: payload.value }
}

async function confirmSubmit() {
  const unanswered = questions.value.length - answeredCount.value
  const msg = unanswered > 0
    ? `还有 ${unanswered} 题未作答，确定交卷吗？`
    : '确定交卷吗？'
  if (window.confirm(msg)) {
    await submitExam(false)
  }
}

async function submitExam(auto) {
  if (timer) clearInterval(timer)

  usedSeconds.value = Math.round((Date.now() - startAt.value) / 1000)

  // 逐题判分
  const judged = {}
  for (const q of questions.value) {
    const ans = answers.value[q.id]
    if (ans === undefined) {
      judged[q.id] = { correct: false, unanswered: true }
      continue
    }
    try {
      const res = await practiceApi.submit(q.id, ans)
      judged[q.id] = res
    } catch {
      judged[q.id] = { correct: false }
    }
  }
  results.value = judged

  const correct = Object.values(judged).filter(r => r.correct).length
  score.value = Math.round(correct * 100 / questions.value.length)

  // 保存成绩
  try {
    await practiceApi.saveExam({
      scope: (config.value.subject || '综合卷') + ' · 模拟考',
      total: questions.value.length,
      correct,
      durationSeconds: usedSeconds.value
    })
  } catch (e) {
    console.error(e)
  }

  phase.value = 'result'
}

function parseOptions(q) {
  try {
    const raw = q.options
    if (!raw) return []
    const arr = typeof raw === 'string' ? JSON.parse(raw) : raw
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

function parseAnswer(q) {
  try {
    const raw = q.answer
    if (!raw) return []
    const arr = typeof raw === 'string' ? JSON.parse(raw) : raw
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

function reviewResult(q) {
  const r = results.value[q.id] || { correct: false }
  const userAns = answers.value[q.id]
  const opts = parseOptions(q)
  const correctIdx = parseAnswer(q)

  let userText = '未作答'
  if (q.type === 'fill') {
    userText = userAns === undefined ? '未作答' : String(userAns)
  } else if (Array.isArray(userAns)) {
    userText = userAns.map(i => opts[i] ?? `选项${i + 1}`).join('，') || '未作答'
  }

  const correctText = q.type === 'fill'
    ? (correctIdx[0] ?? '')
    : correctIdx.map(i => opts[i] ?? `选项${i + 1}`).join('，')

  return { correct: !!r.correct, userText, correctText }
}

function resetExam() {
  phase.value = 'config'
  questions.value = []
  answers.value = {}
  results.value = {}
  index.value = 0
}
</script>

<style scoped>
.config-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 14px;
  align-items: end;
}

.exam-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  background: var(--card);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  margin-bottom: 14px;
  gap: 12px;
  flex-wrap: wrap;
}

.timer {
  font-size: 20px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--primary);
}

.timer.urgent { color: var(--danger); animation: blink 1s infinite; }

@keyframes blink { 50% { opacity: .45; } }

.actions { display: flex; gap: 10px; margin-top: 16px; flex-wrap: wrap; }

.sheet {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(42px, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.sheet-cell {
  height: 40px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--card);
  font-size: 13px;
  transition: all .15s;
}

.sheet-cell:hover { border-color: var(--primary-light); }
.sheet-cell.answered { background: var(--primary-bg); border-color: #bfdbfe; color: var(--primary); font-weight: 600; }
.sheet-cell.current { border-color: var(--primary); box-shadow: 0 0 0 2px rgba(31, 78, 121, .18); }

.score-card { display: flex; gap: 28px; align-items: center; flex-wrap: wrap; }

.score-ring {
  width: 130px;
  height: 130px;
  border-radius: 50%;
  border: 8px solid var(--primary-bg);
  border-top-color: var(--primary);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.score-value { font-size: 38px; font-weight: 700; color: var(--primary); line-height: 1; }
.score-label { font-size: 12px; color: var(--text-soft); }

.score-meta { flex: 1; min-width: 240px; }

.score-stats { display: flex; gap: 32px; margin-top: 14px; }
.score-stats div { display: flex; flex-direction: column; }
.score-stats strong { font-size: 20px; }
.score-stats span { font-size: 12px; color: var(--text-soft); }

.review-item { padding: 16px 0; border-bottom: 1px solid var(--border); }
.review-item:last-child { border-bottom: none; }

.review-head { display: flex; gap: 8px; font-size: 15px; line-height: 1.7; margin-bottom: 8px; }
.review-body { padding-left: 24px; }
</style>