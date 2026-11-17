<template>
  <div class="page">
    <h1 class="page-title">分类练习</h1>
    <p class="page-desc">选择学科与章节，逐题练习并即时查看解析</p>

    <!-- 筛选条件 -->
    <div class="card">
      <div class="filters">
        <div class="field mb-0">
          <label>学科</label>
          <select v-model="subject" class="select" @change="onSubjectChange">
            <option value="">全部学科</option>
            <option v-for="s in subjects" :key="s" :value="s">{{ s }}</option>
          </select>
        </div>

        <div class="field mb-0">
          <label>章节</label>
          <select v-model="chapter" class="select" :disabled="!chapters.length">
            <option value="">全部章节</option>
            <option v-for="c in chapters" :key="c" :value="c">{{ c }}</option>
          </select>
        </div>

        <div class="field mb-0">
          <label>题型</label>
          <select v-model="type" class="select">
            <option value="">全部题型</option>
            <option value="single">单选题</option>
            <option value="multiple">多选题</option>
            <option value="judge">判断题</option>
            <option value="fill">填空题</option>
          </select>
        </div>

        <div class="field mb-0">
          <label>难度</label>
          <select v-model="difficulty" class="select">
            <option value="">全部难度</option>
            <option :value="1">基础</option>
            <option :value="2">中等</option>
            <option :value="3">困难</option>
          </select>
        </div>

        <button class="btn btn-primary" @click="startFromFilter">开始练习</button>
      </div>
    </div>

    <!-- 练习区 -->
    <div v-if="list.length">
      <div class="progress-bar">
        <div class="progress-info">
          <span>第 {{ index + 1 }} / {{ list.length }} 题</span>
          <span class="text-soft">已答 {{ answeredCount }} 题 · 答对 {{ correctCount }} 题</span>
        </div>
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: ((index + 1) / list.length * 100) + '%' }"></div>
        </div>
      </div>

      <QuestionCard
        ref="cardRef"
        :question="list[index]"
        :result="result"
        @answer="onAnswer"
      />

      <div class="actions">
        <button class="btn" :disabled="index === 0" @click="prev">上一题</button>

        <button
          v-if="list[index]?.type === 'multiple' && pendingMulti"
          class="btn btn-primary"
          @click="submitMulti"
        >提交答案</button>

        <button
          v-if="!result && !pendingMulti"
          class="btn btn-primary"
          :disabled="!pendingValue !== undefined && false"
          @click="submitCurrent"
        >提交答案</button>

        <button v-else-if="result" class="btn btn-primary" :disabled="index >= list.length - 1" @click="next">
          下一题
        </button>

        <button class="btn" @click="toggleFavorite" :disabled="favoriting">
          {{ isFavorited ? '★ 已收藏' : '☆ 收藏' }}
        </button>

        <button class="btn" :disabled="index >= list.length - 1" @click="next">跳过</button>
      </div>

      <div v-if="index >= list.length - 1 && result" class="card mt-16 done-card">
        <h3 class="mb-0">本轮练习完成 🎉</h3>
        <p class="text-soft">共 {{ list.length }} 题，答对 {{ correctCount }} 题，正确率
          {{ Math.round(correctCount / Math.max(answeredCount, 1) * 100) }}%</p>
        <div class="row mt-8">
          <button class="btn btn-primary" @click="restart">再练一轮</button>
          <router-link :to="{ name: 'home' }" class="btn">返回首页</router-link>
        </div>
      </div>
    </div>

    <div v-else-if="loading" class="loading">正在加载题目...</div>
    <div v-else class="card empty">
      <div class="empty-icon">📚</div>
      <p>没有符合条件的题目，试试调整筛选条件</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import QuestionCard from '../components/QuestionCard.vue'
import { metaApi, practiceApi } from '../api'

const route = useRoute()
const router = useRouter()

const subjects = ref([])
const chapters = ref([])
const subject = ref('')
const chapter = ref('')
const type = ref('')
const difficulty = ref('')

const list = ref([])
const index = ref(0)
const result = ref(null)
const loading = ref(true)

const answeredCount = ref(0)
const correctCount = ref(0)
const isFavorited = ref(false)
const favoriting = ref(false)

/* 多选题暂存 */
const pendingMulti = ref(false)
const pendingValue = ref(null)

const cardRef = ref(null)

onMounted(async () => {
  try {
    subjects.value = await metaApi.subjects()

    // 从首页带学科参数进入时自动开始
    if (route.query.subject) {
      subject.value = route.query.subject
      await onSubjectChange()
      await startFromFilter()
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

async function onSubjectChange() {
  chapter.value = ''
  chapters.value = []
  if (subject.value) {
    try {
      chapters.value = await metaApi.chapters(subject.value)
    } catch (e) {
      console.error(e)
    }
  }
}

async function startFromFilter() {
  loading.value = true
  index.value = 0
  result.value = null
  answeredCount.value = 0
  correctCount.value = 0
  pendingMulti.value = false
  pendingValue.value = null

  try {
    const data = await metaApi.questions({
      subject: subject.value || undefined,
      chapter: chapter.value || undefined,
      type: type.value || undefined,
      difficulty: difficulty.value || undefined,
      page: 0,
      size: 100
    })
    list.value = data.content || []
    await refreshFavorite()
  } catch (e) {
    console.error(e)
    list.value = []
  } finally {
    loading.value = false
  }
}

const current = computed(() => list.value[index.value])

async function refreshFavorite() {
  if (!current.value) return
  try {
    const list = await practiceApi.favorites()
    isFavorited.value = (list || []).some(item => item.question?.id === current.value.id)
  } catch {
    isFavorited.value = false
  }
}

watch(index, () => {
  result.value = null
  pendingMulti.value = false
  pendingValue.value = null
  refreshFavorite()
})

/** 用户点选选项 / 输入填空 */
function onAnswer(payload) {
  pendingValue.value = payload.value

  if (payload.multi) {
    pendingMulti.value = true
    return
  }

  if (payload.ready) {
    submitAnswer(payload.value)
  }
}

function submitMulti() {
  if (pendingValue.value && pendingValue.value.length) {
    submitAnswer(pendingValue.value)
  }
}

function submitCurrent() {
  if (pendingValue.value !== null && pendingValue.value !== '') {
    submitAnswer(pendingValue.value)
  }
}

async function submitAnswer(value) {
  if (!current.value || result.value) return

  try {
    const res = await practiceApi.submit(current.value.id, value)
    result.value = res
    answeredCount.value++
    if (res.correct) correctCount.value++
    pendingMulti.value = false
  } catch (e) {
    alert(e.message)
  }
}

function prev() {
  if (index.value > 0) index.value--
}

function next() {
  if (index.value < list.value.length - 1) index.value++
}

async function toggleFavorite() {
  if (!current.value || favoriting.value) return
  favoriting.value = true
  try {
    const res = await practiceApi.toggleFavorite(current.value.id)
    isFavorited.value = res.favorited
  } catch (e) {
    alert(e.message)
  } finally {
    favoriting.value = false
  }
}

function restart() {
  index.value = 0
  result.value = null
  answeredCount.value = 0
  correctCount.value = 0
}
</script>

<style scoped>
.filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 14px;
  align-items: end;
}

.progress-bar { margin-bottom: 14px; }

.progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 6px;
}

.progress-track {
  height: 6px;
  background: #e5e7eb;
  border-radius: 999px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--primary-light);
  border-radius: 999px;
  transition: width .2s;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.done-card { text-align: center; }
</style>