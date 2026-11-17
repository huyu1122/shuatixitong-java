/**
 * 真实浏览器端到端验证（Edge headless + CDP）
 *
 * 覆盖：页面加载 → 注册 → 首页数据 → 分类练习答题判分 → 公式渲染 →
 *       错题本 → 统计 → 模拟考 → 各页面无 JS 异常
 */
const http = require("http");
const fs = require("fs");
const os = require("os");
const path = require("path");
const { spawn } = require("child_process");

const BASE = "http://localhost:5173";
const CDP_PORT = 9411;

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
        const { resolve } = this.pending.get(m.id); this.pending.delete(m.id);
        resolve(m.result);
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
    const r = await this.send("Runtime.evaluate", {
      expression: expr, returnByValue: true, awaitPromise: true
    });
    if (r.exceptionDetails) {
      return { __err: r.exceptionDetails.exception?.description || r.exceptionDetails.text };
    }
    return r.result?.value;
  }
  close() { try { this.ws && this.ws.close(); } catch {} }
}

async function main() {
  const browser = findBrowser();
  if (!browser) { console.log("未找到浏览器"); process.exit(1); }

  const profile = fs.mkdtempSync(path.join(os.tmpdir(), "pw-"));
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
  await cdp.send("Network.enable");

  console.log("=".repeat(64));
  console.log("真实浏览器端到端验证");
  console.log("=".repeat(64));

  /* ---------- 1. 登录页 ---------- */
  console.log("\n【1】登录页加载");
  await cdp.send("Page.navigate", { url: BASE + "/login" });
  await sleep(3000);

  const loginText = await cdp.eval("document.body.innerText");
  check("登录页渲染出品牌名", /学而刷题/.test(loginText || ""));
  check("显示登录/注册切换", /登录/.test(loginText) && /注册/.test(loginText));

  /* ---------- 2. 注册 ---------- */
  console.log("\n【2】注册新账号");
  const uname = "browser" + Date.now().toString().slice(-8);

  await cdp.eval(`(function(){
    var inputs = document.querySelectorAll('input');
    var setVal = function(el, v){
      var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
      setter.call(el, v);
      el.dispatchEvent(new Event('input', { bubbles: true }));
    };
    setVal(inputs[0], '${uname}');
    setVal(inputs[1], 'test123456');
  })()`);
  await sleep(300);

  // 切到注册模式
  await cdp.eval(`(function(){
    var tabs = document.querySelectorAll('.tab');
    if (tabs[1]) tabs[1].click();
  })()`);
  await sleep(600);

  await cdp.eval(`(function(){
    var inputs = document.querySelectorAll('input');
    var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
    setter.call(inputs[0], '${uname}');
    inputs[0].dispatchEvent(new Event('input', { bubbles: true }));
    setter.call(inputs[1], 'test123456');
    inputs[1].dispatchEvent(new Event('input', { bubbles: true }));
  })()`);
  await sleep(300);

  const submitClicked = await cdp.eval(`(function(){
    var btn = document.querySelector('form button[type=submit]');
    if (btn) { btn.click(); return true; }
    return false;
  })()`);
  check("注册按钮可点击", submitClicked === true);
  await sleep(4000);

  const afterReg = await cdp.eval("location.pathname");
  check("注册成功后进入首页", afterReg === "/", `当前路径 ${afterReg}`);

  /* ---------- 3. 首页 ---------- */
  console.log("\n【3】首页数据");
  await sleep(1500);
  const homeText = await cdp.eval("document.body.innerText");
  check("首页显示欢迎语", /欢迎回来/.test(homeText || ""));
  check("首页显示学科列表", /高等数学/.test(homeText || ""));
  check("首页显示学习数据", /累计答题/.test(homeText || ""));

  const subjectCards = await cdp.eval("document.querySelectorAll('.subject-card').length");
  check("学科卡片数量为 12", subjectCards === 12, `实际 ${subjectCards}`);

  /* ---------- 4. 分类练习 ---------- */
  console.log("\n【4】分类练习与判分");
  await cdp.eval(`(function(){
    var cards = document.querySelectorAll('.subject-card');
    for (var i = 0; i < cards.length; i++) {
      if (cards[i].innerText.indexOf('高等数学') >= 0) { cards[i].click(); return; }
    }
    if (cards[0]) cards[0].click();
  })()`);
  await sleep(3500);

  const practiceText = await cdp.eval("document.body.innerText");
  check("进入练习页并加载题目", /第\s*1\s*\/\s*\d+\s*题/.test(practiceText || ""), 
        (practiceText || "").slice(0, 80));

  const optCount = await cdp.eval("document.querySelectorAll('.option').length");
  check("题目渲染出选项", optCount >= 2, `选项数 ${optCount}`);

  /* 公式渲染检查 */
  const katexCount = await cdp.eval("document.querySelectorAll('.katex').length");
  check("题干公式已渲染为 KaTeX", katexCount > 0, `katex 元素 ${katexCount}`);

  const rawDollar = await cdp.eval(`(function(){
    var stems = document.querySelectorAll('.q-stem');
    for (var i = 0; i < stems.length; i++) {
      if (/\\$[^$]+\\$/.test(stems[i].innerText)) return true;
    }
    return false;
  })()`);
  check("题干无未渲染的 $...$ 残留", rawDollar === false);

  /* 选项里的公式也要渲染（此前遇到过的 bug） */
  const optRawDollar = await cdp.eval(`(function(){
    var opts = document.querySelectorAll('.option-text');
    for (var i = 0; i < opts.length; i++) {
      if (/\\$[^$]+\\$/.test(opts[i].innerText)) return true;
    }
    return false;
  })()`);
  check("选项无未渲染的 $...$ 残留", optRawDollar === false);

  /* 作答 */
  await cdp.eval(`(function(){
    var opts = document.querySelectorAll('.option');
    if (opts[0]) opts[0].click();
  })()`);
  await sleep(2500);

  const resultText = await cdp.eval("document.body.innerText");
  check("作答后显示判分结果", /回答(正确|错误)/.test(resultText || ""));
  check("判分结果显示正确答案", /正确答案/.test(resultText || ""));
  check("判分结果显示解析", /解析/.test(resultText || ""));

  /* ---------- 5. 错题本 ---------- */
  console.log("\n【5】错题本");
  await cdp.eval(`(function(){
    var links = document.querySelectorAll('.nav-item');
    for (var i = 0; i < links.length; i++) {
      if (links[i].innerText.trim() === '错题本') { links[i].click(); return; }
    }
  })()`);
  await sleep(2500);

  const wrongText = await cdp.eval("document.body.innerText");
  check("错题本页面正常渲染", /错题本/.test(wrongText || ""));
  check("错题本有内容或空状态提示", /道错题|错题本是空的/.test(wrongText || ""));

  /* ---------- 6. 学习统计 ---------- */
  console.log("\n【6】学习统计");
  await cdp.eval(`(function(){
    var links = document.querySelectorAll('.nav-item');
    for (var i = 0; i < links.length; i++) {
      if (links[i].innerText.trim() === '学习统计') { links[i].click(); return; }
    }
  })()`);
  await sleep(2500);

  const statText = await cdp.eval("document.body.innerText");
  check("统计页渲染", /学习统计/.test(statText || ""));
  check("显示累计答题次数", /累计答题次数/.test(statText || ""));
  check("显示学科掌握情况", /学科掌握情况/.test(statText || ""));
  check("显示考试记录", /模拟考试记录/.test(statText || ""));

  /* ---------- 7. 模拟考试 ---------- */
  console.log("\n【7】模拟考试");
  await cdp.eval(`(function(){
    var links = document.querySelectorAll('.nav-item');
    for (var i = 0; i < links.length; i++) {
      if (links[i].innerText.trim() === '模拟考试') { links[i].click(); return; }
    }
  })()`);
  await sleep(2000);

  const examConfig = await cdp.eval("document.body.innerText");
  check("考试配置页渲染", /模拟考试/.test(examConfig || ""));

  const started = await cdp.eval(`(function(){
    var btns = document.querySelectorAll('button');
    for (var i = 0; i < btns.length; i++) {
      if (btns[i].innerText.indexOf('开始考试') >= 0) { btns[i].click(); return true; }
    }
    return false;
  })()`);
  check("点击开始考试", started === true);
  await sleep(4000);

  const inExam = await cdp.eval("document.body.innerText");
  check("进入考试并显示倒计时", /⏱|答题卡/.test(inExam || ""));

  const sheetCells = await cdp.eval("document.querySelectorAll('.sheet-cell').length");
  check("答题卡渲染题号", sheetCells >= 10, `格数 ${sheetCells}`);

  /* ---------- 8. 逐页检查 JS 异常 ---------- */
  console.log("\n【8】保存截图");
  let shotSize = 0;
  try {
    const s = await cdp.send("Page.captureScreenshot", { format: "png" });
    if (s.data) {
      const buf = Buffer.from(s.data, "base64");
      const outDir = path.join(__dirname, "..", "docs", "screenshots");
      fs.mkdirSync(outDir, { recursive: true });
      fs.writeFileSync(path.join(outDir, "exam.png"), buf);
      shotSize = buf.length;
    }
  } catch {}
  check("截图成功", shotSize > 10000, `${shotSize} 字节`);

  /* ---------- 9. JS 异常 ---------- */
  console.log("\n【9】运行时异常检查");
  const realErrors = cdp.errors.filter(e =>
    !/favicon|net::ERR/i.test(String(e))
  );
  check("页面无未捕获 JS 异常", realErrors.length === 0,
        realErrors.slice(0, 2).map(e => String(e).split("\n")[0]).join(" | "));

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