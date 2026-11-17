<template>
  <div class="page">
    <h1 class="page-title">后台管理</h1>
    <p class="page-desc">管理员专属：题目管理、用户管理</p>

    <div class="tabs">
      <button :class="['tab', { active: tab === 'dashboard' }]" @click="tab = 'dashboard'">总览</button>
      <button :class="['tab', { active: tab === 'questions' }]" @click="tab = 'questions'">题目管理</button>
      <button :class="['tab', { active: tab === 'users' }]" @click="tab = 'users'">用户管理</button>
    </div>

    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <!-- ==================== 总览 ==================== -->
    <div v-if="tab === 'dashboard'">
      <div v-if="loading" class="loading">加载中...</div>
      <template v-else-if="dash">
        <div class="stat-grid">
          <div class="stat-card"><div class="v">{{ dash.userCount }}</div><div class="l">注册用户</div></div>
          <div class="stat-card"><div class="v">{{ dash.adminCount }}</div><div class="l">管理员</div></div>
          <div class="stat-card"><div class="v">{{ dash.newUsersThisWeek }}</div><div class="l">近 7 天新增</div></div>
          <div class="stat-card"><div class="v">{{ dash.questionCount }}</div><div class="l">题目总数</div></div>
          <div class="stat-card"><div class="v">{{ dash.examCount }}</div><div class="l">考试记录</div></div>
          <div class="stat-card"><div class="v">{{ dash.recordCount }}</div><div class="l">答题记录</div></div>
        </div>

        <div v-if="qstats" class="card">
          <h3 class="mb-0">题库构成</h3>
          <div class="dist">
            <div>
              <div class="dist-title">按题型</div>
              <div v-for="(cnt, t) in qstats.byType" :key="t" class="dist-row">
                <span>{{ typeLabel(t) }}</span>
                <div class="bar"><div class="bar-fill" :style="{ width: pct(cnt, qstats.total) + '%' }"></div></div>
                <span class="num">{{ cnt }}</span>
              </div>
            </div>
            <div>
              <div class="dist-title">按难度</div>
              <div v-for="(cnt, d) in qstats.byDifficulty" :key="d" class="dist-row">
                <span>{{ diffLabel(d) }}</span>
                <div class="bar"><div class="bar-fill" :style="{ width: pct(cnt, qstats.total) + '%' }"></div></div>
                <span class="num">{{ cnt }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- ==================== 题目管理 ==================== -->
    <div v-if="tab === 'questions'">
      <div class="card">
        <div class="toolbar">
          <input v-model.trim="qKeyword" class="input" placeholder="搜索题干关键词" @keyup.enter="loadQuestions(0)" />
          <select v-model="qSubject" class="select" @change="loadQuestions(0)">
            <option value="">全部学科</option>
            <option v-for="s in subjects" :key="s" :value="s">{{ s }}</option>
          </select>
          <button class="btn" @click="loadQuestions(0)">查询</button>
          <button class="btn btn-primary" @click="openCreate">新增题目</button>
        </div>
      </div>

      <!-- 编辑表单 -->
      <div v-if="editing" class="card editor">
        <div class="row-between" style="margin-bottom: 14px">
          <h3 class="mb-0">{{ editing.id ? '编辑题目 #' + editing.id : '新增题目' }}</h3>
          <button class="btn btn-sm" @click="editing = null">取消</button>
        </div>

        <div class="form-grid">
          <div class="field">
            <label>学科</label>
            <input v-model.trim="editing.subject" class="input" list="subject-list" placeholder="如 高等数学" />
            <datalist id="subject-list">
              <option v-for="s in subjects" :key="s" :value="s" />
            </datalist>
          </div>
          <div class="field">
            <label>章节</label>
            <input v-model.trim="editing.chapter" class="input" placeholder="如 极限与连续" />
          </div>
          <div class="field">
            <label>题型</label>
            <select v-model="editing.type" class="select">
              <option value="single">单选题</option>
              <option value="multiple">多选题</option>
              <option value="judge">判断题</option>
              <option value="fill">填空题</option>
            </select>
          </div>
          <div class="field">
            <label>难度</label>
            <select v-model.number="editing.difficulty" class="select">
              <option :value="1">基础</option>
              <option :value="2">中等</option>
              <option :value="3">困难</option>
            </select>
          </div>
        </div>

        <div class="field">
          <label>题干（支持 LaTeX，用 $...$ 包裹）</label>
          <textarea v-model="editing.stem" class="input textarea" rows="3"></textarea>
        </div>

        <div v-if="editing.type !== 'fill'" class="field">
          <label>选项（每行一个）</label>
          <textarea v-model="editing.optionsText" class="input textarea" rows="4"
                    placeholder="A 选项&#10;B 选项&#10;C 选项&#10;D 选项"></textarea>
        </div>

        <div class="field">
          <label>
            {{ editing.type === 'fill' ? '正确答案（文本）' :
               editing.type === 'multiple' ? '正确答案（多个，逗号分隔，从 0 开始：如 0,2）'
                                           : '正确答案（从 0 开始）' }}
          </label>
          <input v-model.trim="editing.answerText" class="input"
                 :placeholder="editing.type === 'fill' ? '输入答案文本' : '如 1'" />
        </div>

        <div class="field">
          <label>解析（可选）</label>
          <textarea v-model="editing.analysis" class="input textarea" rows="2"></textarea>
        </div>

        <div class="field">
          <label>知识点（逗号分隔，可选）</label>
          <input v-model.trim="editing.knowledgeText" class="input" placeholder="如 重要极限,等价无穷小" />
        </div>

        <div class="row">
          <button class="btn btn-primary" @click="saveQuestion" :disabled="saving">
            {{ saving ? '保存中...' : '保存' }}
          </button>
          <button class="btn" @click="editing = null">取消</button>
        </div>
      </div>

      <!-- 题目列表 -->
      <div class="card">
        <div class="row-between" style="margin-bottom: 12px">
          <span>共 {{ qTotal }} 道题</span>
          <span class="text-soft text-sm">
            第 {{ qPage + 1 }} / {{ Math.max(qTotalPages, 1) }} 页
          </span>
        </div>

        <div v-if="qLoading" class="loading">加载中...</div>
        <table v-else class="table">
          <thead>
            <tr>
              <th style="width:70px">编号</th>
              <th>题干</th>
              <th style="width:100px">学科</th>
              <th style="width:80px">题型</th>
              <th style="width:70px">难度</th>
              <th style="width:150px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="q in qList" :key="q.id">
              <td class="mono">{{ q.code }}</td>
              <td class="stem-cell">{{ truncate(q.stem, 60) }}</td>
              <td>{{ q.subject }}</td>
              <td>{{ typeLabel(q.type) }}</td>
              <td>{{ diffLabel(q.difficulty) }}</td>
              <td>
                <div class="row-actions">
                  <button class="btn btn-sm" @click="openEdit(q)">编辑</button>
                  <button class="btn btn-sm btn-danger" @click="removeQuestion(q)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="pager">
          <button class="btn btn-sm" :disabled="qPage <= 0" @click="loadQuestions(qPage - 1)">上一页</button>
          <button class="btn btn-sm" :disabled="qPage >= qTotalPages - 1" @click="loadQuestions(qPage + 1)">下一页</button>
        </div>
      </div>
    </div>

    <!-- ==================== 用户管理 ==================== -->
    <div v-if="tab === 'users'">
      <div class="card">
        <div class="toolbar">
          <input v-model.trim="uKeyword" class="input" placeholder="搜索用户名或昵称" @keyup.enter="loadUsers" />
          <button class="btn" @click="loadUsers">查询</button>
        </div>
      </div>

      <div class="card">
        <div v-if="uLoading" class="loading">加载中...</div>
        <table v-else class="table">
          <thead>
            <tr>
              <th style="width:60px">ID</th>
              <th>用户名</th>
              <th>昵称</th>
              <th style="width:90px">角色</th>
              <th style="width:90px">答题数</th>
              <th style="width:90px">状态</th>
              <th style="width:240px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in uList" :key="u.id">
              <td>{{ u.id }}</td>
              <td>{{ u.username }}</td>
              <td>{{ u.nickname || '-' }}</td>
              <td>
                <span class="tag" :class="u.role === 'ADMIN' ? 'tag-warning' : ''">
                  {{ u.role === 'ADMIN' ? '管理员' : '学生' }}
                </span>
              </td>
              <td>{{ u.attempts }}</td>
              <td>
                <span :class="u.locked ? 'text-danger' : 'text-success'">
                  {{ u.locked ? '已锁定' : '正常' }}
                </span>
              </td>
              <td>
                <div class="row-actions">
                  <button class="btn btn-sm" @click="toggleRole(u)">
                    {{ u.role === 'ADMIN' ? '降为学生' : '设为管理' }}
                  </button>
                  <button class="btn btn-sm" @click="resetPwd(u)">重置密码</button>
                  <button class="btn btn-sm btn-danger" @click="removeUser(u)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!uList.length && !uLoading" class="empty">没有匹配的用户</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import adminApi from '../api/admin'
import { metaApi } from '../api'

const tab = ref('dashboard')
const error = ref('')
const loading = ref(false)

/* 总览 */
const dash = ref(null)
const qstats = ref(null)

/* 题目 */
const subjects = ref([])
const qList = ref([])
const qTotal = ref(0)
const qPage = ref(0)
const qSize = 10
const qTotalPages = computed(() => Math.ceil(qTotal.value / qSize))
const qKeyword = ref('')
const qSubject = ref('')
const qLoading = ref(false)
const editing = ref(null)
const saving = ref(false)

/* 用户 */
const uList = ref([])
const uKeyword = ref('')
const uLoading = ref(false)

onMounted(async () => {
  try {
    subjects.value = await metaApi.subjects()
  } catch (e) {
    console.error(e)
  }
  await refresh()
})

watch(tab, async (t) => {
  error.value = ''
  if (t === 'dashboard') await loadDashboard()
  if (t === 'questions' && !qList.value.length) await loadQuestions(0)
  if (t === 'users' && !uList.value.length) await loadUsers()
})

async function refresh() {
  await loadDashboard()
}

async function loadDashboard() {
  loading.value = true
  try {
    const [d, s] = await Promise.all([adminApi.dashboard(), adminApi.questionStats()])
    dash.value = d
    qstats.value = s
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

/* ---------- 题目 ---------- */
async function loadQuestions(page) {
  qLoading.value = true
  error.value = ''
  try {
    const data = await adminApi.questions({
      subject: qSubject.value || undefined,
      keyword: qKeyword.value || undefined,
      page: page ?? qPage.value,
      size: qSize
    })
    qList.value = data.content || []
    qTotal.value = data.total || 0
    qPage.value = data.page || 0
  } catch (e) {
    error.value = e.message
  } finally {
    qLoading.value = false
  }
}

function parseJsonList(raw) {
  if (!raw) return []
  try {
    const v = typeof raw === 'string' ? JSON.parse(raw) : raw
    return Array.isArray(v) ? v : []
  } catch {
    return []
  }
}

function openCreate() {
  editing.value = {
    id: null, code: '', subject: '', chapter: '', type: 'single', difficulty: 1,
    stem: '', optionsText: '', answerText: '', analysis: '', knowledgeText: ''
  }
}

function openEdit(q) {
  const opts = parseJsonList(q.options)
  const ans = parseJsonList(q.answer)
  const know = parseJsonList(q.knowledge)

  editing.value = {
    id: q.id,
    code: q.code,
    subject: q.subject,
    chapter: q.chapter,
    type: q.type,
    difficulty: q.difficulty,
    stem: q.stem,
    optionsText: opts.join('\n'),
    answerText: q.type === 'fill'
      ? (typeof q.answer === 'string' && q.answer.startsWith('[') ? (ans[0] ?? '') : String(q.answer ?? ''))
      : ans.join(','),
    analysis: q.analysis || '',
    knowledgeText: know.join(',')
  }
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function saveQuestion() {
  const f = editing.value
  if (!f) return

  error.value = ''

  const payload = {
    subject: f.subject,
    chapter: f.chapter,
    type: f.type,
    difficulty: f.difficulty,
    stem: f.stem,
    analysis: f.analysis || null
  }

  if (f.type === 'fill') {
    payload.answer = JSON.stringify([f.answerText])
    payload.options = null
  } else {
    const opts = f.optionsText.split('\n').map(s => s.trim()).filter(Boolean)
    const ans = f.answerText.split(',').map(s => Number(s.trim())).filter(n => !Number.isNaN(n))
    payload.options = JSON.stringify(opts)
    payload.answer = JSON.stringify(ans)
  }

  const know = f.knowledgeText.split(',').map(s => s.trim()).filter(Boolean)
  payload.knowledge = know.length ? JSON.stringify(know) : null

  saving.value = true
  try {
    if (f.id) {
      await adminApi.updateQuestion(f.id, payload)
    } else {
      await adminApi.createQuestion(payload)
    }
    editing.value = null
    await loadQuestions(f.id ? qPage.value : 0)
    await loadDashboard()
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

async function removeQuestion(q) {
  if (!window.confirm(`确定删除题目 ${q.code} 吗？相关答题记录也会一并删除。`)) return
  try {
    await adminApi.deleteQuestion(q.id)
    await loadQuestions(qPage.value)
    await loadDashboard()
  } catch (e) {
    error.value = e.message
  }
}

function truncate(s, n) {
  const t = String(s || '').replace(/\s+/g, ' ')
  return t.length > n ? t.slice(0, n) + '…' : t
}

/* ---------- 用户 ---------- */
async function loadUsers() {
  uLoading.value = true
  error.value = ''
  try {
    uList.value = await adminApi.users(uKeyword.value || undefined)
  } catch (e) {
    error.value = e.message
  } finally {
    uLoading.value = false
  }
}

async function toggleRole(u) {
  const next = u.role === 'ADMIN' ? 'STUDENT' : 'ADMIN'
  const label = next === 'ADMIN' ? '设为管理员' : '降级为学生'
  if (!window.confirm(`确定将 ${u.username} ${label}吗？`)) return
  try {
    await adminApi.setRole(u.id, next)
    await loadUsers()
  } catch (e) {
    error.value = e.message
  }
}

async function resetPwd(u) {
  const pwd = window.prompt(`为 ${u.username} 设置新密码（至少 6 位）：`)
  if (!pwd) return
  try {
    await adminApi.resetPassword(u.id, pwd)
    window.alert('密码已重置，该用户需要重新登录')
  } catch (e) {
    error.value = e.message
  }
}

async function removeUser(u) {
  if (!window.confirm(`确定删除用户 ${u.username} 吗？其答题记录与错题本会一并删除，且不可恢复。`)) return
  try {
    await adminApi.deleteUser(u.id)
    await loadUsers()
    await loadDashboard()
  } catch (e) {
    error.value = e.message
  }
}

/* ---------- 展示辅助 ---------- */
function typeLabel(t) {
  return { single: '单选题', multiple: '多选题', judge: '判断题', fill: '填空题' }[t] || t
}

function diffLabel(d) {
  return { 1: '基础', 2: '中等', 3: '困难' }[d] || `难度${d}`
}

function pct(n, total) {
  return total ? Math.round(n / total * 100) : 0
}
</script>

<style scoped>
.tabs {
  display: flex;
  gap: 4px;
  background: var(--bg);
  padding: 4px;
  border-radius: 9px;
  margin-bottom: 16px;
  max-width: 420px;
}

.tab {
  flex: 1;
  padding: 8px;
  border: none;
  border-radius: 7px;
  background: transparent;
  font-size: 14px;
  color: var(--text-soft);
}

.tab.active { background: var(--card); color: var(--primary); font-weight: 600; box-shadow: var(--shadow); }

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  background: var(--card);
  border-radius: var(--radius);
  padding: 18px;
  box-shadow: var(--shadow);
}

.stat-card .v { font-size: 24px; font-weight: 700; }
.stat-card .l { font-size: 13px; color: var(--text-soft); }

.dist { display: grid; grid-template-columns: 1fr 1fr; gap: 28px; margin-top: 12px; }
.dist-title { font-size: 13px; color: var(--text-soft); margin-bottom: 8px; }

.dist-row {
  display: grid;
  grid-template-columns: 68px 1fr 40px;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 13px;
}

.bar { height: 8px; background: #eef1f5; border-radius: 999px; overflow: hidden; }
.bar-fill { height: 100%; background: var(--primary-light); border-radius: 999px; }
.num { text-align: right; color: var(--text-soft); }

.toolbar { display: flex; gap: 10px; flex-wrap: wrap; }
.toolbar .input, .toolbar .select { max-width: 220px; }

.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 14px; }

.textarea {
  font-family: inherit;
  line-height: 1.6;
  resize: vertical;
}

.mono { font-family: ui-monospace, Consolas, monospace; font-size: 12px; color: var(--text-soft); }
.stem-cell { max-width: 380px; }

/* 操作按钮横排，不换行 —— 列宽不足时会堆叠成两行，显得拥挤 */
.row-actions {
  display: flex;
  gap: 6px;
  flex-wrap: nowrap;
  align-items: center;
}

.row-actions .btn { padding: 4px 10px; font-size: 12px; }

.pager { display: flex; gap: 10px; justify-content: flex-end; margin-top: 12px; }

@media (max-width: 720px) {
  .dist { grid-template-columns: 1fr; }
}
</style>