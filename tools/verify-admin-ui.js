/**
 * 管理后台界面验证（真实浏览器）
 * 覆盖：管理员登录 → 后台入口可见 → 总览 → 题目管理 → 用户管理
 *       以及学生账号看不到后台入口
 */
const http = require("http");
const fs = require("fs");
const os = require("os");
const path = require("path");
const { spawn } = require("child_process");

const BASE = "http://localhost:5173";
const CDP_PORT = 9421;

let pass = 0, fail = 0;
const failures = [];
function check(name, cond, detail) {
  if (cond) { pass++; console.log(`  ✓ ${name}`); }
  else { fail++; failures.push(name); console.log(`  ✗ ${name}${detail ? "  → " + detail : ""}`); }
}
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

function findBrowser() {
  for (const p of [
    "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
    "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe"
  ]) { try { if (fs.existsSync(p)) return p; } catch {} }
  return null;
}

function httpGet(port, p) {
  return new Promise((res) => {
    const q = http.get({ host: "127.0.0.1", port, path: p, timeout: 2000 }, (r) => {
      let s = ""; r.on("data", d => s += d); r.on("end", () => res(s));
    });
    q.on("error", () => res(""));
    q.on("timeout", () => { q.destroy(); res(""); });
  });
}

class CDP {
  constructor(url) { this.url = url; this.id = 0; this.pending = new Map(); this.errors = []; }
  async connect() {
    const WebSocket = require("ws");
    this.ws = new WebSocket(this.url, { maxPayload: 256 * 1024 * 1024 });
    await new Promise((r, j) => { this.ws.on("open", r); this.ws.on("error", j); });
    this.ws.on("message", (data) => {
      let m; try { m = JSON.parse(data.toString()); } catch { return; }
      if (m.id !== undefined && this.pending.has(m.id)) {
        const { resolve } = this.pending.get(m.id); this.pending.delete(m.id); resolve(m.result);
      } else if (m.method === "Runtime.exceptionThrown") {
        const d = m.params.exceptionDetails;
        this.errors.push(d?.exception?.description || d?.text || "unknown");
      }
    });
  }
  send(method, params = {}) {
    const id = ++this.id;
    return new Promise((resolve) => {
      this.pending.set(id, { resolve });
      this.ws.send(JSON.stringify({ id, method, params }));
      setTimeout(() => { if (this.pending.has(id)) { this.pending.delete(id); resolve({}); } }, 20000);
    });
  }
  async eval(expr) {
    const r = await this.send("Runtime.evaluate", { expression: expr, returnByValue: true, awaitPromise: true });
    if (r.exceptionDetails) return { __err: r.exceptionDetails.exception?.description || r.exceptionDetails.text };
    return r.result?.value;
  }
  close() { try { this.ws && this.ws.close(); } catch {} }
}

/* 通过页面输入框登录，避免直接改 localStorage */
async function loginAs(cdp, username, password) {
  await cdp.send("Page.navigate", { url: BASE + "/login" });
  await sleep(2500);

  await cdp.eval(`(function(){
    var inputs = document.querySelectorAll('input');
    var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
    if (inputs[0]) { setter.call(inputs[0], ${JSON.stringify(username)}); inputs[0].dispatchEvent(new Event('input',{bubbles:true})); }
    if (inputs[1]) { setter.call(inputs[1], ${JSON.stringify(password)}); inputs[1].dispatchEvent(new Event('input',{bubbles:true})); }
  })()`);
  await sleep(300);

  await cdp.eval(`(function(){
    var btn = document.querySelector('form button[type=submit]');
    if (btn) btn.click();
  })()`);
  await sleep(3500);
  return await cdp.eval("location.pathname");
}

async function main() {
  const browser = findBrowser();
  if (!browser) { console.log("未找到浏览器"); process.exit(1); }

  const profile = fs.mkdtempSync(path.join(os.tmpdir(), "pwadm-"));
  const bp = spawn(browser, [
    "--headless=new", "--disable-gpu", "--no-first-run", "--no-default-browser-check",
    `--remote-debugging-port=${CDP_PORT}`, `--user-data-dir=${profile}`,
    "--window-size=1280,900", "about:blank"
  ], { stdio: "ignore" });

  let wsUrl = null;
  for (let i = 0; i < 60; i++) {
    const raw = await httpGet(CDP_PORT, "/json/list");
    try {
      const list = JSON.parse(raw);
      const pg = list.find(t => t.type === "page");
      if (pg && pg.webSocketDebuggerUrl) { wsUrl = pg.webSocketDebuggerUrl; break; }
    } catch {}
    await sleep(300);
  }
  if (!wsUrl) { console.log("CDP 连接失败"); bp.kill(); process.exit(1); }

  const cdp = new CDP(wsUrl);
  await cdp.connect();
  await cdp.send("Runtime.enable");
  await cdp.send("Page.enable");

  console.log("=".repeat(64));
  console.log("管理后台界面验证（真实浏览器）");
  console.log("=".repeat(64));

  /* ---------- 1. 管理员登录 ---------- */
  console.log("\n【1】管理员登录");
  const p1 = await loginAs(cdp, "admin", "admin123");
  check("管理员登录后进入首页", p1 === "/", `路径 ${p1}`);

  const navText = await cdp.eval("document.body.innerText");
  check("导航显示后台管理入口", /后台管理/.test(navText || ""));

  /* ---------- 2. 进入后台总览 ---------- */
  console.log("\n【2】后台总览");
  await cdp.eval(`(function(){
    var links = document.querySelectorAll('.nav-item');
    for (var i = 0; i < links.length; i++) {
      if (links[i].innerText.trim() === '后台管理') { links[i].click(); return; }
    }
  })()`);
  await sleep(3000);

  const adminPath = await cdp.eval("location.pathname");
  check("进入后台管理页", adminPath === "/admin", `路径 ${adminPath}`);

  const dashText = await cdp.eval("document.body.innerText");
  check("显示总览指标", /注册用户/.test(dashText || ""));
  check("显示题目总数", /题目总数/.test(dashText || ""));
  check("显示题库构成", /题库构成/.test(dashText || ""));
  check("显示按题型分布", /按题型/.test(dashText || ""));

  const statCards = await cdp.eval("document.querySelectorAll('.stat-card').length");
  check("总览卡片渲染 6 个", statCards === 6, `实际 ${statCards}`);

  /* ---------- 3. 题目管理 ---------- */
  console.log("\n【3】题目管理");
  await cdp.eval(`(function(){
    var tabs = document.querySelectorAll('.tab');
    for (var i = 0; i < tabs.length; i++) {
      if (tabs[i].innerText.trim() === '题目管理') { tabs[i].click(); return; }
    }
  })()`);
  await sleep(2500);

  const qText = await cdp.eval("document.body.innerText");
  check("题目管理页渲染", /共 \d+ 道题/.test(qText || ""), (qText || "").slice(0, 60));

  const rows = await cdp.eval("document.querySelectorAll('tbody tr').length");
  check("题目列表有数据行", rows > 0, `行数 ${rows}`);

  const hasEditBtn = await cdp.eval(`(function(){
    var btns = document.querySelectorAll('button');
    for (var i = 0; i < btns.length; i++) if (btns[i].innerText.trim() === '编辑') return true;
    return false;
  })()`);
  check("列表有编辑按钮", hasEditBtn === true);

  /* 打开新增表单 */
  await cdp.eval(`(function(){
    var btns = document.querySelectorAll('button');
    for (var i = 0; i < btns.length; i++) {
      if (btns[i].innerText.trim() === '新增题目') { btns[i].click(); return; }
    }
  })()`);
  await sleep(1200);

  const editorOpen = await cdp.eval("!!document.querySelector('.editor')");
  check("新增题目表单打开", editorOpen === true);

  const editorText = await cdp.eval(`(function(){
    var el = document.querySelector('.editor');
    return el ? el.innerText : '';
  })()`);
  check("表单含题干输入", /题干/.test(editorText || ""));
  check("表单含选项输入", /选项/.test(editorText || ""));
  check("表单含答案输入", /正确答案/.test(editorText || ""));

  /* 关闭表单，避免影响后续 */
  await cdp.eval(`(function(){
    var el = document.querySelector('.editor');
    if (!el) return;
    var btns = el.querySelectorAll('button');
    for (var i = 0; i < btns.length; i++) {
      if (btns[i].innerText.trim() === '取消') { btns[i].click(); return; }
    }
  })()`);
  await sleep(800);

  /* ---------- 4. 用户管理 ---------- */
  console.log("\n【4】用户管理");
  await cdp.eval(`(function(){
    var tabs = document.querySelectorAll('.tab');
    for (var i = 0; i < tabs.length; i++) {
      if (tabs[i].innerText.trim() === '用户管理') { tabs[i].click(); return; }
    }
  })()`);
  await sleep(2500);

  const uText = await cdp.eval("document.body.innerText");
  check("用户管理页渲染", /用户名/.test(uText || ""));
  check("显示 admin 账号", /admin/.test(uText || ""));
  check("显示角色列", /管理员|学生/.test(uText || ""));

  /* ---------- 5. 学生看不到后台 ---------- */
  console.log("\n【5】学生权限");
  // 退出登录
  await cdp.eval(`(function(){
    var btns = document.querySelectorAll('button');
    for (var i = 0; i < btns.length; i++) {
      if (btns[i].innerText.trim() === '退出') { btns[i].click(); return; }
    }
  })()`);
  await sleep(2500);

  // 注册一个学生账号
  const stuName = "uivis" + Date.now().toString().slice(-8);
  await cdp.send("Page.navigate", { url: BASE + "/login" });
  await sleep(2000);

  await cdp.eval(`(function(){
    var tabs = document.querySelectorAll('.tab');
    if (tabs[1]) tabs[1].click();
  })()`);
  await sleep(600);

  await cdp.eval(`(function(){
    var inputs = document.querySelectorAll('input');
    var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
    setter.call(inputs[0], ${JSON.stringify(stuName)}); inputs[0].dispatchEvent(new Event('input',{bubbles:true}));
    setter.call(inputs[1], 'test123456'); inputs[1].dispatchEvent(new Event('input',{bubbles:true}));
  })()`);
  await sleep(300);
  await cdp.eval(`(function(){
    var btn = document.querySelector('form button[type=submit]');
    if (btn) btn.click();
  })()`);
  await sleep(3500);

  const stuNav = await cdp.eval("document.body.innerText");
  check("学生导航无后台管理入口", !/后台管理/.test(stuNav || ""));

  // 直接访问 /admin 应被重定向
  await cdp.eval(`window.location.href = '/admin'`);
  await sleep(2500);
  const redirected = await cdp.eval("location.pathname");
  check("学生直接访问 /admin 被重定向", redirected === "/", `路径 ${redirected}`);

  /* ---------- 6. 异常检查 ---------- */
  console.log("\n【6】运行时异常");
  const realErrors = cdp.errors.filter(e => !/favicon|net::ERR/i.test(String(e)));
  check("无未捕获 JS 异常", realErrors.length === 0,
        realErrors.slice(0, 2).map(e => String(e).split("\n")[0]).join(" | "));

  /* ---------- 7. 截图 ---------- */
  console.log("\n【7】保存截图");
  await loginAs(cdp, "admin", "admin123");
  await cdp.eval(`window.location.href = '/admin'`);
  await sleep(3000);
  let size = 0;
  try {
    const s = await cdp.send("Page.captureScreenshot", { format: "png" });
    if (s.data) {
      const buf = Buffer.from(s.data, "base64");
      const outDir = path.join(__dirname, "..", "docs", "screenshots");
      fs.mkdirSync(outDir, { recursive: true });
      fs.writeFileSync(path.join(outDir, "admin.png"), buf);
      size = buf.length;
    }
  } catch {}
  check("后台截图成功", size > 10000, `${size} 字节`);

  console.log("\n" + "=".repeat(64));
  console.log(`通过 ${pass}  失败 ${fail}`);
  if (fail) { console.log("失败项:"); failures.forEach(f => console.log("  - " + f)); }
  console.log("=".repeat(64));

  cdp.close();
  try { bp.kill(); } catch {}
  await sleep(500);
  try { fs.rmSync(profile, { recursive: true, force: true }); } catch {}
  process.exit(fail ? 1 : 0);
}

main().catch(e => { console.error("异常:", e.message); process.exit(1); });