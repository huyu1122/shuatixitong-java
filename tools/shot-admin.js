/**
 * 管理后台截图工具
 * 登录管理员账号后截取「总览」与「题目管理」页面
 */
const http = require("http");
const fs = require("fs");
const os = require("os");
const path = require("path");
const { spawn } = require("child_process");

const CDP_PORT = 9431;
const BASE = "http://localhost:5173";
const OUT_DIR = path.join(__dirname, "..", "docs", "screenshots");
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function httpGet(port, p) {
  return new Promise((res) => {
    const q = http.get({ host: "127.0.0.1", port, path: p, timeout: 2000 }, (r) => {
      let s = "";
      r.on("data", (d) => (s += d));
      r.on("end", () => res(s));
    });
    q.on("error", () => res(""));
    q.on("timeout", () => { q.destroy(); res(""); });
  });
}

function findBrowser() {
  for (const p of [
    "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
    "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe"
  ]) {
    try { if (fs.existsSync(p)) return p; } catch {}
  }
  return null;
}

(async () => {
  const browser = findBrowser();
  if (!browser) { console.log("未找到浏览器"); process.exit(1); }

  const profile = fs.mkdtempSync(path.join(os.tmpdir(), "shotadm-"));
  const bp = spawn(browser, [
    "--headless=new", "--disable-gpu", "--no-first-run",
    `--remote-debugging-port=${CDP_PORT}`, `--user-data-dir=${profile}`,
    "--window-size=1280,1000", "about:blank"
  ], { stdio: "ignore" });

  let wsUrl = null;
  for (let i = 0; i < 60; i++) {
    const raw = await httpGet(CDP_PORT, "/json/list");
    try {
      const pg = JSON.parse(raw).find((t) => t.type === "page");
      if (pg && pg.webSocketDebuggerUrl) { wsUrl = pg.webSocketDebuggerUrl; break; }
    } catch {}
    await sleep(300);
  }
  if (!wsUrl) { console.log("CDP 连接失败"); bp.kill(); process.exit(1); }

  const WebSocket = require("ws");
  const ws = new WebSocket(wsUrl, { maxPayload: 256 * 1024 * 1024 });
  await new Promise((r) => ws.on("open", r));

  let id = 0;
  const pending = new Map();
  ws.on("message", (d) => {
    let m;
    try { m = JSON.parse(d.toString()); } catch { return; }
    if (m.id !== undefined && pending.has(m.id)) {
      pending.get(m.id)(m.result);
      pending.delete(m.id);
    }
  });

  const send = (method, params = {}) =>
    new Promise((resolve) => {
      const i = ++id;
      pending.set(i, resolve);
      ws.send(JSON.stringify({ id: i, method, params }));
    });

  const evalJs = async (expr) =>
    (await send("Runtime.evaluate", { expression: expr, returnByValue: true, awaitPromise: true })).result?.value;

  await send("Runtime.enable");
  await send("Page.enable");

  /* 登录管理员 */
  await send("Page.navigate", { url: BASE + "/login" });
  await sleep(3000);
  await evalJs(`(function(){
    var inputs = document.querySelectorAll('input');
    var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
    setter.call(inputs[0], 'admin');  inputs[0].dispatchEvent(new Event('input', { bubbles: true }));
    setter.call(inputs[1], 'admin123'); inputs[1].dispatchEvent(new Event('input', { bubbles: true }));
    var btn = document.querySelector('form button[type=submit]');
    if (btn) btn.click();
  })()`);
  await sleep(4500);

  /* 进入后台 */
  await send("Page.navigate", { url: BASE + "/admin" });
  await sleep(4500);

  const curPath = await evalJs("location.pathname");
  console.log("当前路径:", curPath, curPath === "/admin" ? "(正确)" : "(错误：未进入后台)");

  fs.mkdirSync(OUT_DIR, { recursive: true });

  const shot = await send("Page.captureScreenshot", { format: "png" });
  fs.writeFileSync(path.join(OUT_DIR, "admin.png"), Buffer.from(shot.data, "base64"));
  console.log("后台总览截图:", (Buffer.from(shot.data, "base64").length / 1024).toFixed(0), "KB");

  /* 切到题目管理 */
  await evalJs(`(function(){
    var tabs = document.querySelectorAll('.tab');
    for (var i = 0; i < tabs.length; i++) {
      if (tabs[i].innerText.trim() === '题目管理') { tabs[i].click(); return; }
    }
  })()`);
  await sleep(3500);

  const shot2 = await send("Page.captureScreenshot", { format: "png" });
  fs.writeFileSync(path.join(OUT_DIR, "admin-questions.png"), Buffer.from(shot2.data, "base64"));
  console.log("题目管理截图:", (Buffer.from(shot2.data, "base64").length / 1024).toFixed(0), "KB");

  ws.close();
  bp.kill();
  await sleep(500);
  try { fs.rmSync(profile, { recursive: true, force: true }); } catch {}
})();