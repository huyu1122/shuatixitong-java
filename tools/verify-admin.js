/**
 * 后台管理接口验证
 * 覆盖：管理员登录 → 总览 → 题目增删改查 → 用户管理 → 权限隔离
 */
const http = require("http");

const HOST = "127.0.0.1", PORT = 8080;
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
      let raw = ""; res.setEncoding("utf8");
      res.on("data", c => raw += c);
      res.on("end", () => {
        let json = null;
        try { json = JSON.parse(raw); } catch {}
        resolve({ status: res.statusCode, json, raw });
      });
    });
    req.on("error", e => resolve({ status: 0, error: e.message }));
    req.on("timeout", () => { req.destroy(); resolve({ status: 0, error: "timeout" }); });
    if (data) req.write(data);
    req.end();
  });
}

async function main() {
  console.log("=".repeat(64));
  console.log("后台管理接口验证");
  console.log("=".repeat(64));

  /* ---------- 1. 管理员登录 ---------- */
  console.log("\n【1】管理员登录");
  const adminLogin = await request("POST", "/api/auth/login", { username: "admin", password: "admin123" });
  check("默认管理员可登录", adminLogin.json?.code === 0, adminLogin.json?.message);
  const adminToken = adminLogin.json?.data?.token;
  check("返回管理员角色", adminLogin.json?.data?.user?.role === "ADMIN");

  /* ---------- 2. 总览 ---------- */
  console.log("\n【2】后台总览");
  const dash = await request("GET", "/api/admin/dashboard", null, adminToken);
  check("总览接口返回 200", dash.status === 200);
  check("统计用户数", typeof dash.json?.data?.userCount === "number");
  check("管理员数 >= 1", dash.json?.data?.adminCount >= 1);

  // 基准题目数：动态获取，避免受历史测试数据影响
  const baseCount = dash.json?.data?.questionCount;
  check("题目数大于 0", baseCount > 0, `实际 ${baseCount}`);

  // 管理员自己的 id，用于测试「不能操作自己」
  const adminMe = await request("GET", "/api/auth/me", null, adminToken);
  const adminId = adminMe.json?.data?.id;
  check("取到管理员 id", typeof adminId === "number");

  const qstats = await request("GET", "/api/admin/questions/stats", null, adminToken);
  check("题库统计返回学科数", qstats.json?.data?.subjectCount >= 12,
        `实际 ${qstats.json?.data?.subjectCount}`);
  check("题库统计返回题型分布", !!qstats.json?.data?.byType);

  /* ---------- 3. 普通学生无权限 ---------- */
  console.log("\n【3】权限隔离");
  const stuName = "stu" + Date.now().toString().slice(-8);
  const reg = await request("POST", "/api/auth/register", { username: stuName, password: "test123456" });
  const stuToken = reg.json?.data?.token;
  check("注册普通学生", reg.json?.code === 0);

  const forbidden = await request("GET", "/api/admin/dashboard", null, stuToken);
  check("学生访问后台返回 403", forbidden.status === 403, `实际 ${forbidden.status}`);

  const noAuth = await request("GET", "/api/admin/dashboard");
  check("未登录访问后台返回 401", noAuth.status === 401);

  /* ---------- 4. 题目增加 ---------- */
  console.log("\n【4】新增题目");
  const newQ = {
    subject: "测试学科",
    chapter: "测试章节",
    type: "single",
    difficulty: 2,
    stem: "以下是自动化测试创建的题目，$1+1=$？",
    options: JSON.stringify(["1", "2", "3", "4"]),
    answer: JSON.stringify([1]),
    analysis: "1 + 1 = 2",
    knowledge: JSON.stringify(["加法"])
  };
  const created = await request("POST", "/api/admin/questions", newQ, adminToken);
  check("新增题目成功", created.json?.code === 0, created.json?.message);
  const newId = created.json?.data?.id;
  const newCode = created.json?.data?.code;
  check("自动生成了题目编码", typeof newCode === "string" && newCode.length > 0, newCode);

  const totalAfter = await request("GET", "/api/admin/dashboard", null, adminToken);
  check("题目总数 +1", totalAfter.json?.data?.questionCount === baseCount + 1,
        `${baseCount} → ${totalAfter.json?.data?.questionCount}`);

  /* ---------- 5. 校验规则 ---------- */
  console.log("\n【5】数据校验");
  const badType = await request("POST", "/api/admin/questions",
      { ...newQ, type: "unknown" }, adminToken);
  check("拒绝非法题型", badType.json?.code !== 0, badType.json?.message);

  const badAnswer = await request("POST", "/api/admin/questions",
      { ...newQ, answer: JSON.stringify([9]) }, adminToken);
  check("拒绝答案下标越界", badAnswer.json?.code !== 0, badAnswer.json?.message);

  const noStem = await request("POST", "/api/admin/questions",
      { ...newQ, stem: "" }, adminToken);
  check("拒绝空题干", noStem.json?.code !== 0);

  const singleMulti = await request("POST", "/api/admin/questions",
      { ...newQ, answer: JSON.stringify([0, 1]) }, adminToken);
  check("拒绝单选题多个答案", singleMulti.json?.code !== 0);

  /* ---------- 6. 题目修改 ---------- */
  console.log("\n【6】修改题目");
  const updated = await request("PUT", `/api/admin/questions/${newId}`,
      { stem: "修改后的题干内容", difficulty: 3 }, adminToken);
  check("修改题干成功", updated.json?.data?.stem === "修改后的题干内容");
  check("修改难度成功", updated.json?.data?.difficulty === 3);

  /* ---------- 7. 学生可以做这道题 ---------- */
  console.log("\n【7】新增题目可用于答题");
  const studentList = await request("GET", "/api/meta/questions?subject=" + encodeURIComponent("测试学科"), null, stuToken);
  check("学生能查到新题目", studentList.json?.data?.total >= 1,
        `实际 ${studentList.json?.data?.total}`);

  const submit = await request("POST", "/api/practice/submit", { questionId: newId, answer: [1] }, stuToken);
  check("学生作答新题目并判分正确", submit.json?.data?.correct === true);

  /* ---------- 8. 用户管理 ---------- */
  console.log("\n【8】用户管理");
  const users = await request("GET", "/api/admin/users", null, adminToken);
  check("用户列表返回", Array.isArray(users.json?.data) && users.json.data.length >= 2);

  const target = users.json.data.find(u => u.username === stuName);
  check("找到测试学生", !!target);

  const promote = await request("PUT", `/api/admin/users/${target.id}/role`, { role: "ADMIN" }, adminToken);
  check("提升学生为管理员", promote.json?.data?.role === "ADMIN");

  const demote = await request("PUT", `/api/admin/users/${target.id}/role`, { role: "STUDENT" }, adminToken);
  check("降回学生角色", demote.json?.data?.role === "STUDENT");

  /*
   * 「不能操作自己」的测试必须用真实的管理员 id。
   * 早期脚本硬编码为 1，而实际 admin 的 id 是 2 —— 测试打错了目标，
   * 反而误判为「有保护」。现在改为从 /auth/me 动态取。
   */
  const selfDemote = await request("PUT", `/api/admin/users/${adminId}/role`, { role: "STUDENT" }, adminToken);
  check("管理员不能降级自己", selfDemote.json?.code !== 0,
        `code=${selfDemote.json?.code} msg=${selfDemote.json?.message}`);
  const stillAdmin = await request("GET", "/api/auth/me", null, adminToken);
  check("降级自己失败后仍是管理员", stillAdmin.json?.data?.role === "ADMIN");

  const resetPwd = await request("POST", `/api/admin/users/${target.id}/reset-password`,
      { password: "newpass123" }, adminToken);
  check("重置密码成功", resetPwd.json?.code === 0);

  const oldLogin = await request("POST", "/api/auth/login", { username: stuName, password: "test123456" });
  check("旧密码失效", oldLogin.json?.code !== 0);
  const newLogin = await request("POST", "/api/auth/login", { username: stuName, password: "newpass123" });
  check("新密码可登录", newLogin.json?.code === 0);

  /* ---------- 9. 删除 ---------- */
  console.log("\n【9】删除操作");
  const delQ = await request("DELETE", `/api/admin/questions/${newId}`, null, adminToken);
  check("删除题目成功", delQ.json?.code === 0);

  const afterDel = await request("GET", "/api/admin/dashboard", null, adminToken);
  check("题目数回到基准", afterDel.json?.data?.questionCount === baseCount,
        `期望 ${baseCount} 实际 ${afterDel.json?.data?.questionCount}`);

  const delUser = await request("DELETE", `/api/admin/users/${target.id}`, null, adminToken);
  check("删除用户成功", delUser.json?.code === 0);

  const selfDelete = await request("DELETE", `/api/admin/users/${adminId}`, null, adminToken);
  check("管理员不能删除自己", selfDelete.json?.code !== 0,
        `code=${selfDelete.json?.code} msg=${selfDelete.json?.message}`);

  console.log("\n" + "=".repeat(64));
  console.log(`通过 ${pass}  失败 ${fail}`);
  if (fail) { console.log("失败项:"); failures.forEach(f => console.log("  - " + f)); }
  console.log("=".repeat(64));
  process.exit(fail ? 1 : 0);
}

main().catch(e => { console.error("异常:", e); process.exit(1); });