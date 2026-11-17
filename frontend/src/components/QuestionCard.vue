<template>
  <div class="q-card">
    <!-- 题头信息 -->
    <div class="q-head">
      <span class="tag">{{ question.subject }}</span>
      <span class="tag">{{ question.chapter }}</span>
      <span class="tag" :class="typeClass">{{ typeLabel }}</span>
      <span class="tag" :class="difficultyClass">难度 {{ difficultyLabel }}</span>
    </div>

    <!-- 题干 -->
    <div class="q-stem">
      <MathText :text="question.stem" />
    </div>

    <!-- 选项 -->
    <div v-if="options.length" class="options">
      <button
        v-for="(opt, idx) in options"
        :key="idx"
        class="option"
        :class="optionClass(idx)"
        :disabled="disabled"
        @click="select(idx)"
      >
        <span class="option-key">{{ 'ABCD'[idx] || idx + 1 }}</span>
        <span class="option-text"><MathText :text="opt" /></span>
      </button>
      <p v-if="question.type === 'multiple'" class="hint">多选题，请选择所有正确选项</p>
    </div>

    <!-- 填空题 -->
    <div v-else class="fill-area">
      <input
        v-model="fillValue"
        class="input"
        :disabled="disabled"
        placeholder="请输入答案"
        @keyup.enter="emitFill"
      />
    </div>

    <!-- 判分结果 -->
    <div v-if="result" class="result" :class="result.correct ? 'ok' : 'bad'">
      <div class="result-title">
        {{ result.correct ? '✓ 回答正确' : '✗ 回答错误' }}
      </div>
      <div class="result-body">
        <div>正确答案：<MathText :text="correctAnswerText" /></div>
        <div v-if="result.analysis" class="mt-8">
          <div class="text-soft text-sm">解析</div>
          <MathText :text="result.analysis" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import MathText from './MathText.vue'

const props = defineProps({
  question: { type: Object, required: true },
  result: { type: Object, default: null },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['answer'])

const selected = ref([])
const fillValue = ref('')

/* 题目切换时重置作答状态 */
watch(() => props.question?.id, () => {
  selected.value = []
  fillValue.value = ''
})

const options = computed(() => {
  try {
    const raw = props.question?.options
    if (!raw) return []
    const arr = typeof raw === 'string' ? JSON.parse(raw) : raw
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
})

const typeLabel = computed(() => ({
  single: '单选题',
  multiple: '多选题',
  judge: '判断题',
  fill: '填空题'
}[props.question?.type] || '题目'))

const typeClass = computed(() => props.question?.type === 'fill' ? 'tag-warning' : '')

const difficultyLabel = computed(() => ({
  1: '基础', 2: '中等', 3: '困难'
}[props.question?.difficulty] || '基础'))

const difficultyClass = computed(() => {
  const d = props.question?.difficulty
  return d === 3 ? 'tag-danger' : d === 2 ? 'tag-warning' : 'tag-success'
})

/** 正确答案的可读文本 */
const correctAnswerText = computed(() => {
  try {
    const raw = props.question?.answer
    if (!raw) return ''
    if (props.question.type === 'fill') {
      const v = typeof raw === 'string' ? JSON.parse(raw) : raw
      return String(v)
    }
    const arr = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (!Array.isArray(arr)) return String(arr)
    return arr.map(i => options.value[i] ?? `选项${i + 1}`).join('，')
  } catch {
    return String(props.question?.answer ?? '')
  }
})

function optionClass(idx) {
  const cls = []
  if (selected.value.includes(idx)) cls.push('picked')

  if (props.result) {
    // 已判分：标出正确答案与用户误选项
    const correctIdx = correctIndexes.value
    if (correctIdx.includes(idx)) cls.push('correct')
    else if (selected.value.includes(idx)) cls.push('wrong')
  }
  return cls
}

const correctIndexes = computed(() => {
  try {
    const raw = props.question?.answer
    if (!raw) return []
    const arr = typeof raw === 'string' ? JSON.parse(raw) : raw
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
})

function select(idx) {
  if (props.disabled) return

  if (props.question.type === 'multiple') {
    const pos = selected.value.indexOf(idx)
    if (pos >= 0) selected.value.splice(pos, 1)
    else selected.value.push(idx)
    // 多选题允许随时继续勾选，点「提交」时再判定
    emit('answer', { multi: true, value: [...selected.value].sort((a, b) => a - b), ready: selected.value.length > 0 })
  } else {
    selected.value = [idx]
    emit('answer', { multi: false, value: [idx], ready: true })
  }
}

function emitFill() {
  if (props.disabled) return
  emit('answer', { multi: false, value: fillValue.value.trim(), ready: !!fillValue.value.trim() })
}

defineExpose({ fillValue, selected })
</script>

<style scoped>
.q-card { background: var(--card); border-radius: var(--radius); padding: 22px; box-shadow: var(--shadow); }

.q-head { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 16px; }

.options { display: flex; flex-direction: column; gap: 10px; }

.option {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: var(--card);
  text-align: left;
  font-size: 15px;
  line-height: 1.7;
  transition: all .15s;
  width: 100%;
}

.option:hover:not(:disabled) { border-color: var(--primary-light); background: #fafcff; }
.option:disabled { cursor: default; }

.option.picked { border-color: var(--primary-light); background: var(--primary-bg); }

.option.correct {
  border-color: var(--success);
  background: var(--success-bg);
}

.option.wrong {
  border-color: var(--danger);
  background: var(--danger-bg);
}

.option-key {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-soft);
  margin-top: 2px;
}

.option.picked .option-key { background: var(--primary); border-color: var(--primary); color: #fff; }
.option.correct .option-key { background: var(--success); border-color: var(--success); color: #fff; }
.option.wrong .option-key { background: var(--danger); border-color: var(--danger); color: #fff; }

.option-text { flex: 1; }

.hint { font-size: 12px; color: var(--text-soft); margin: 4px 0 0 2px; }

.fill-area { margin-top: 4px; }

.result {
  margin-top: 20px;
  padding: 16px;
  border-radius: 9px;
  border: 1px solid;
}

.result.ok { background: var(--success-bg); border-color: #bbf7d0; }
.result.bad { background: var(--danger-bg); border-color: #fecaca; }

.result-title { font-weight: 600; margin-bottom: 8px; }
.result.ok .result-title { color: var(--success); }
.result.bad .result-title { color: var(--danger); }

.result-body { font-size: 14px; line-height: 1.75; }
</style>