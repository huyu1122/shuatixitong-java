<template>
  <div class="page">
    <h1 class="page-title">收藏夹</h1>
    <p class="page-desc">练习时点击「☆ 收藏」即可把题目存到这里，方便集中复习</p>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="!items.length" class="card empty">
      <div class="empty-icon">⭐</div>
      <p>还没有收藏任何题目</p>
      <router-link :to="{ name: 'practice' }" class="btn btn-primary mt-8">去练习</router-link>
    </div>

    <div v-else>
      <div class="card">
        <span>共收藏 {{ items.length }} 道题</span>
      </div>

      <div v-for="item in items" :key="item.question.id" class="card mark-item">
        <div class="mark-head">
          <div class="row wrap">
            <span class="tag">{{ item.question.subject }}</span>
            <span class="tag">{{ item.question.chapter }}</span>
          </div>
          <button class="btn btn-sm btn-danger" @click="remove(item.question.id)">取消收藏</button>
        </div>

        <div class="q-stem">
          <MathText :text="item.question.stem" />
        </div>

        <div v-if="optionsOf(item.question).length" class="opts">
          <div v-for="(o, i) in optionsOf(item.question)" :key="i" class="opt-line">
            <span class="opt-key">{{ 'ABCD'[i] || i + 1 }}</span>
            <MathText :text="o" />
          </div>
        </div>

        <div class="answer-row">
          <span class="text-sm text-soft">正确答案：</span>
          <span class="text-sm text-success"><MathText :text="answerText(item.question)" /></span>
        </div>

        <div v-if="item.question.analysis" class="q-analysis">
          <MathText :text="item.question.analysis" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import MathText from '../components/MathText.vue'
import { practiceApi } from '../api'

const items = ref([])
const loading = ref(true)

onMounted(load)

async function load() {
  loading.value = true
  try {
    items.value = (await practiceApi.favorites()) || []
  } catch (e) {
    console.error(e)
    items.value = []
  } finally {
    loading.value = false
  }
}

function optionsOf(q) {
  try {
    const raw = q.options
    if (!raw) return []
    const arr = typeof raw === 'string' ? JSON.parse(raw) : raw
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

function answerText(q) {
  try {
    const raw = q.answer
    if (!raw) return ''
    const arr = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (q.type === 'fill') return String(arr)
    if (!Array.isArray(arr)) return String(arr)
    return arr.map(i => optionsOf(q)[i] ?? `选项${i + 1}`).join('，')
  } catch {
    return String(q.answer ?? '')
  }
}

async function remove(questionId) {
  try {
    await practiceApi.removeMark(questionId, 'FAVORITE')
    await load()
  } catch (e) {
    alert(e.message)
  }
}
</script>

<style scoped>
.mark-item { padding: 18px 20px; }

.mark-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.q-stem { font-size: 15px; margin-bottom: 12px; }

.opts { display: flex; flex-direction: column; gap: 6px; margin-bottom: 12px; }

.opt-line { display: flex; gap: 10px; font-size: 14px; line-height: 1.7; }

.opt-key {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: var(--text-soft);
}

.answer-row { display: flex; align-items: center; gap: 4px; margin-bottom: 8px; }
</style>