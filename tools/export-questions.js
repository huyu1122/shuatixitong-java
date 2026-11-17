/**
 * 导出题库为 JSON（供 Spring Boot 项目导入）
 *
 * 用法：node tools/export-questions.js [源 questions.js 路径]
 * 默认从 ../quiz-system/js/questions.js 读取，输出到 ./data/questions.json
 */
const fs = require("fs");
const vm = require("vm");
const path = require("path");

const ROOT = path.join(__dirname, "..");
// 默认源路径：同级的 quiz-system 项目；也可用命令行参数覆盖
const SRC = process.argv[2] || path.join(ROOT, "..", "quiz-system", "js", "questions.js");
// 输出到后端 resources，作为题库的唯一权威副本
const OUT = path.join(ROOT, "backend", "src", "main", "resources", "data", "questions.json");

if (!fs.existsSync(SRC)) {
  console.error("找不到题库源文件：", SRC);
  console.error("用法：node tools/export-questions.js <questions.js 路径>");
  process.exit(1);
}

const sb = { window: {} };
vm.createContext(sb);
vm.runInContext(fs.readFileSync(SRC, "utf8"), sb, { filename: "questions.js" });
const bank = sb.window.QUESTION_BANK;

fs.mkdirSync(path.dirname(OUT), { recursive: true });
fs.writeFileSync(OUT, JSON.stringify(bank, null, 2), "utf8");

console.log("题目数:", bank.length);
console.log("字段:", Object.keys(bank[0]).join(", "));
const types = {};
const subjects = new Set();
const chapters = new Set();
for (const q of bank) {
  types[q.type] = (types[q.type] || 0) + 1;
  subjects.add(q.subject);
  chapters.add(q.subject + "/" + q.chapter);
}
console.log("题型:", JSON.stringify(types));
console.log("学科:", subjects.size, "章节:", chapters.size);
console.log("\n示例题:");
console.log(JSON.stringify(bank[0], null, 2));
console.log("\n选择题示例（看 options/answer 结构）:");
const single = bank.find(q => q.type === "single");
console.log(JSON.stringify(single, null, 2));
console.log("\n已写出:", OUT);