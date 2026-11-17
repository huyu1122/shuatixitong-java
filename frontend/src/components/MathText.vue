<template>
  <span class="q-text" v-html="rendered"></span>
</template>

<script setup>
import { computed } from 'vue'
import katex from 'katex'

const props = defineProps({
  text: { type: String, default: '' }
})

/**
 * 把含 LaTeX 的文本渲染为 HTML。
 *
 * 支持的行内定界符：$...$ 与 \(...\)
 * 支持的块级定界符：$$...$$ 与 \[...\]
 *
 * 注意：定界符判定规则为「成对且内部非空」——
 * 这样 $0$、$1$ 这类纯数字公式能正常渲染，
 * 而没有配对 $ 的自然文本不会被误判。
 */
const rendered = computed(() => {
  const src = String(props.text ?? '')
  if (!src) return ''

  let out = ''
  let i = 0

  const escapeHtml = (s) => s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  const renderMath = (tex, displayMode) => {
    try {
      return katex.renderToString(tex, {
        displayMode,
        throwOnError: false,
        trust: false,
        strict: 'ignore'
      })
    } catch {
      return escapeHtml(displayMode ? `$$${tex}$$` : `$${tex}$`)
    }
  }

  while (i < src.length) {
    // 块级 $$...$$
    if (src.startsWith('$$', i)) {
      const end = src.indexOf('$$', i + 2)
      if (end > i + 2) {
        out += renderMath(src.slice(i + 2, end).trim(), true)
        i = end + 2
        continue
      }
    }

    // 块级 \[...\]
    if (src.startsWith('\\[', i)) {
      const end = src.indexOf('\\]', i + 2)
      if (end > i + 2) {
        out += renderMath(src.slice(i + 2, end).trim(), true)
        i = end + 2
        continue
      }
    }

    // 行内 \(...\)
    if (src.startsWith('\\(', i)) {
      const end = src.indexOf('\\)', i + 2)
      if (end > i + 2) {
        out += renderMath(src.slice(i + 2, end).trim(), false)
        i = end + 2
        continue
      }
    }

    // 行内 $...$
    if (src[i] === '$') {
      const end = src.indexOf('$', i + 1)
      if (end > i + 1) {
        out += renderMath(src.slice(i + 1, end).trim(), false)
        i = end + 1
        continue
      }
    }

    // 普通文本：累积到下一个公式起点
    let next = i + 1
    while (
      next < src.length &&
      src[next] !== '$' &&
      !src.startsWith('\\(', next) &&
      !src.startsWith('\\[', next) &&
      !src.startsWith('$$', next)
    ) {
      next++
    }
    out += escapeHtml(src.slice(i, next))
    i = next
  }

  return out
})
</script>

<style scoped>
.q-text { word-break: break-word; }
</style>