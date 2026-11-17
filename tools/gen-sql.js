/**
 * 把题库 JSON 生成 MySQL INSERT 脚本
 *
 * 数据源使用后端 resources 中的权威副本，
 * 避免同一份题库在仓库里存两份（早期曾有两份，容易改一处忘另一处）。
 */
const fs = require("fs");
const path = require("path");

const ROOT = path.join(__dirname, "..");
const SRC = path.join(ROOT, "backend", "src", "main", "resources", "data", "questions.json");
const OUT = path.join(ROOT, "docs", "sql", "data-questions.sql");

const bank = JSON.parse(fs.readFileSync(SRC, "utf8"));

/** SQL 字符串转义 */
function esc(s) {
  if (s === null || s === undefined) return "NULL";
  return "'" + String(s)
    .replace(/\\/g, "\\\\")
    .replace(/'/g, "''")
    .replace(/\n/g, "\\n")
    .replace(/\r/g, "\\r") + "'";
}

const lines = [];
lines.push("-- ============================================================");
lines.push("--  学而刷题 —— 题库初始化数据");
lines.push(`--  共 ${bank.length} 道题，由 questions.json 自动生成`);
lines.push("--  使用前请先执行 schema.sql");
lines.push("-- ============================================================");
lines.push("");
lines.push("USE quiz_system;");
lines.push("");

const CHUNK = 50;
for (let i = 0; i < bank.length; i += CHUNK) {
  const chunk = bank.slice(i, i + CHUNK);
  lines.push("INSERT INTO quiz_question (code, subject, chapter, type, difficulty, stem, options, answer, analysis, knowledge) VALUES");
  const rows = chunk.map((q, idx) => {
    const options = q.options ? JSON.stringify(q.options) : null;
    const answer = JSON.stringify(q.answer);
    const knowledge = q.knowledge ? JSON.stringify(q.knowledge) : null;
    const tail = idx === chunk.length - 1 ? ";" : ",";
    return `(${esc(q.id)}, ${esc(q.subject)}, ${esc(q.chapter)}, ${esc(q.type)}, ${q.difficulty}, ${esc(q.stem)}, ${esc(options)}, ${esc(answer)}, ${esc(q.analysis)}, ${esc(knowledge)})${tail}`;
  });
  lines.push(...rows);
  lines.push("");
}

lines.push("-- 完成");
fs.mkdirSync(path.dirname(OUT), { recursive: true });
fs.writeFileSync(OUT, lines.join("\n"), "utf8");

const size = fs.statSync(OUT).size;
console.log(`已生成 ${OUT}`);
console.log(`  题目数: ${bank.length}`);
console.log(`  文件大小: ${(size / 1024).toFixed(1)} KB`);
console.log(`  INSERT 批次: ${Math.ceil(bank.length / CHUNK)}`);