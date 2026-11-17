/**
 * 后端端到端接口验证脚本
 * 覆盖：注册 → 题目查询 → 答题判分 → 错题本 → 收藏 → 统计 → 鉴权拦截
 */
const http = require("http");

const HOST = "127.0.0.1";
const PORT = 8080;

let pass = 0, fail = 0;
const failures = [];

function check(name, cond, detail) {
  if (cond) { pass++; console.log(`  ✓ ${name}`); }
  else { fail++; failures.push(name); console.log(`  ✗ ${name}${detail ? "  → " + detail : ""}`); }
}

function request(method, path, body, token) {
  return new Promise((resolve) => {
    const data = body ? JSON.stringify(body) : null;
    const headers = {};
    if (data) { headers["Content-Type"] = "application/json"; headers["Content-Length"] = Buffer.byteLength(data); }
    if (token) headers["Authorization"] = "Bearer " + token;
    const req = http.request({ host: HOST, port: PORT, path, method, headers, timeout: 15000 }, (res) => {
      let raw = "";
      res.setEncoding("utf8");
      res.on("data", (c) => raw += c);
      res.on("end", () => {
        let json = null;
        try { json = JSON.parse(raw); } catch {}
        resolve({ status: res.statusCode, json, raw });
      });
    });
    req.on("error", (e) => resolve({ status: 0, error: e.message }));
    req.on("timeout", () => { req.destroy(); resolve({ status: 0, error: "timeout" }); });
    if (data) req.write(data);
    req.end();
  });
}

async function main() {
  console.log("=".repeat(64));
  console.log("Spring Boot 后端端到端验证");
  console.log("=".repeat(64));

  /* ---------- 1. 题库 ---------- */
  console.log("\n【1】题库接口");
  const subjects = await request("GET", "/api/meta/subjects");
  check("学科列表返回 200", subjects.status === 200);
  check("学科数量为 12", Array.isArray(subjects.json?.data) && subjects.json.data.length === 12,
        `实际 ${subjects.json?.data?.length}`);

  const qs = await request("GET", "/api/meta/questions?page=0&size=5");
  check("分页查询返回 200", qs.status === 200);
  check("题目总数为 100", qs.json?.data?.total === 100, `实际 ${qs.json?.data?.total}`);
  check("分页返回 5 条", qs.json?.data?.content?.length === 5);

  const one = qs.json?.data?.content?.[0];
  check("题目含 stem 字段", !!one?.stem);
  check("题目含 options 字段", !!one?.options);
  check("题目含 answer 字段", !!one?.answer);

  /* ---------- 2. 注册登录 ---------- */
  console.log("\n【2】注册与登录");
  const uname = "student" + Date.now();
  const reg = await request("POST", "/api/auth/register", {
    username: uname, password: "test123456", nickname: "测试学生"
  });
  check("注册成功", reg.status === 200 && reg.json?.code === 0, reg.raw?.slice(0, 120));
  const token = reg.json?.data?.token;
  check("注册返回 token", typeof token === "string" && token.length === 64);

  const dup = await request("POST", "/api/auth/register", { username: uname, password: "test123456" });
  check("重复用户名被拒绝", dup.json?.code !== 0);

  const badLogin = await request("POST", "/api/auth/login", { username: uname, password: "wrongpass" });
  check("错误密码登录失败", badLogin.json?.code !== 0);

  const login = await request("POST", "/api/auth/login", { username: uname, password: "test123456" });
  check("正确密码登录成功", login.json?.code === 0);
  const token2 = login.json?.data?.token;
  check("登录返回新 token", token2 !== token);

  const me = await request("GET", "/api/auth/me", null, token2);
  check("获取当前用户成功", me.json?.data?.username === uname);

  /* ---------- 3. 鉴权拦截 ---------- */
  console.log("\n【3】鉴权拦截");
  const noAuth = await request("GET", "/api/practice/statistics");
  check("未登录访问受保护接口返回 401", noAuth.status === 401);
  const badToken = await request("GET", "/api/practice/statistics", null, "deadbeef");
  check("非法 token 返回 401", badToken.status === 401);

  /* ---------- 4. 答题判分 ---------- */
  console.log("\n【4】答题与判分");
  // 找一道单选题
  const single = qs.json.data.content.find(q => q.type === "single");
  const correctAnswer = JSON.parse(single.answer); // 形如 [1]
  check("找到单选题用于测试", !!single);

  const right = await request("POST", "/api/practice/submit",
      { questionId: single.id, answer: correctAnswer }, token2);
  check("正确作答判定为 correct=true", right.json?.data?.correct === true,
        JSON.stringify(right.json?.data));

  const wrong = await request("POST", "/api/practice/submit",
      { questionId: single.id, answer: [(correctAnswer[0] + 1) % 4] }, token2);
  check("错误作答判定为 correct=false", wrong.json?.data?.correct === false);
  check("判分返回解析", typeof wrong.json?.data?.analysis === "string");

  /* ---------- 5. 错题本与收藏 ---------- */
  console.log("\n【5】错题本与收藏");
  const wrongList = await request("GET", "/api/practice/wrong", null, token2);
  check("错题本含刚答错的题", Array.isArray(wrongList.json?.data) && wrongList.json.data.length >= 1);

  const fav = await request("POST", `/api/practice/favorite/${single.id}`, null, token2);
  check("收藏成功", fav.json?.data?.favorited === true);
  const favList = await request("GET", "/api/practice/favorites", null, token2);
  check("收藏夹含该题", favList.json?.data?.length === 1);
  const unfav = await request("POST", `/api/practice/favorite/${single.id}`, null, token2);
  check("取消收藏成功", unfav.json?.data?.favorited === false);

  /* ---------- 6. 统计 ---------- */
  console.log("\n【6】学习统计");
  const stat = await request("GET", "/api/practice/statistics", null, token2);
  check("统计接口返回 200", stat.status === 200);
  check("累计答题次数为 2", stat.json?.data?.totalAttempts === 2, `实际 ${stat.json?.data?.totalAttempts}`);
  check("正确数为 1", stat.json?.data?.totalCorrect === 1);
  check("正确率为 50%", stat.json?.data?.accuracy === 50, `实际 ${stat.json?.data?.accuracy}`);
  check("总题数为 100", stat.json?.data?.totalQuestions === 100);
  check("含学科维度统计", Array.isArray(stat.json?.data?.subjectStats));

  /* ---------- 7. 模拟考 ---------- */
  console.log("\n【7】模拟考试");
  const exam = await request("POST", "/api/practice/exam",
      { scope: "综合卷 · 模拟考", total: 10, correct: 8, durationSeconds: 600 }, token2);
  check("保存考试成绩成功", exam.json?.data?.score === 80, `score=${exam.json?.data?.score}`);
  const hist = await request("GET", "/api/practice/exam/history", null, token2);
  check("考试历史返回 1 条", hist.json?.data?.length === 1);

  /* ---------- 8. 登出 ---------- */
  console.log("\n【8】登出");
  const out = await request("POST", "/api/auth/logout", null, token2);
  check("登出成功", out.json?.code === 0);
  const after = await request("GET", "/api/practice/statistics", null, token2);
  check("登出后 token 失效", after.status === 401);

  console.log("\n" + "=".repeat(64));
  console.log(`通过 ${pass}  失败 ${fail}`);
  if (fail) { console.log("失败项:"); failures.forEach(f => console.log("  - " + f)); }
  console.log("=".repeat(64));
  process.exit(fail ? 1 : 0);
}

main().catch(e => { console.error("异常:", e); process.exit(1); });