package com.example.data.model

data class TemplateDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val iconType: String,
    val accentColor: Long,
    val defaultPackageName: String,
    val files: Map<String, String> // filename to content
)

object StarterTemplates {
    val templates: List<TemplateDefinition> = listOf(
        TemplateDefinition(
            id = "neon_arcade",
            name = "Neon Space Arcade",
            description = "High-octane HTML5 Canvas spaceship game with touch controls, Web Audio sounds, and particle FX.",
            category = "Game",
            iconType = "gamepad",
            accentColor = 0xFF00E5FF,
            defaultPackageName = "com.hopweb.app.neonarcade",
            files = mapOf(
                "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <title>Neon Space Arcade</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div id="game-container">
    <div id="hud">
      <div class="hud-item">SCORE: <span id="score">0</span></div>
      <div class="hud-item">SHIELD: <span id="shield">100%</span></div>
      <div class="hud-item">HI: <span id="hi-score">0</span></div>
    </div>
    <canvas id="gameCanvas"></canvas>
    <div id="game-over-overlay" class="overlay hidden">
      <h1>GAME OVER</h1>
      <p>Final Score: <span id="final-score">0</span></p>
      <button id="restart-btn">PLAY AGAIN</button>
    </div>
    <div id="touch-controls">
      <button class="t-btn" id="btn-left">◀</button>
      <button class="t-btn fire" id="btn-fire">⚡ FIRE</button>
      <button class="t-btn" id="btn-right">▶</button>
    </div>
  </div>
  <script src="script.js"></script>
</body>
</html>""",
                "style.css" to """* {
  box-sizing: border-box;
  user-select: none;
  -webkit-user-select: none;
  margin: 0;
  padding: 0;
}
body {
  background: #090b14;
  color: #fff;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace;
  overflow: hidden;
  touch-action: manipulation;
  height: 100vh;
  display: flex;
  flex-direction: column;
}
#game-container {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}
#hud {
  position: absolute;
  top: 10px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 1px;
  text-shadow: 0 0 8px #00e5ff;
  z-index: 10;
}
#score { color: #00e5ff; }
#shield { color: #00ff88; }
#hi-score { color: #ff007f; }
#gameCanvas {
  width: 100%;
  flex: 1;
  background: radial-gradient(circle at center, #11182d 0%, #06080e 100%);
}
.overlay {
  position: absolute;
  inset: 0;
  background: rgba(8, 11, 20, 0.9);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  z-index: 20;
}
.overlay h1 {
  font-size: 32px;
  color: #ff007f;
  text-shadow: 0 0 16px #ff007f;
}
.hidden { display: none !important; }
#restart-btn {
  background: linear-gradient(135deg, #00e5ff, #7928ca);
  color: #fff;
  border: none;
  padding: 12px 32px;
  font-size: 16px;
  font-weight: bold;
  border-radius: 24px;
  cursor: pointer;
  box-shadow: 0 0 16px rgba(0, 229, 255, 0.6);
}
#touch-controls {
  height: 70px;
  background: #0d1222;
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 8px 12px;
  border-top: 1px solid #1f2942;
}
.t-btn {
  background: #19223c;
  color: #00e5ff;
  border: 1px solid #00e5ff;
  border-radius: 12px;
  padding: 12px 24px;
  font-size: 18px;
  font-weight: bold;
  touch-action: none;
}
.t-btn:active {
  background: #00e5ff;
  color: #090b14;
}
.t-btn.fire {
  background: #ff007f;
  color: #fff;
  border-color: #ff007f;
  padding: 12px 36px;
  box-shadow: 0 0 12px rgba(255, 0, 127, 0.5);
}""",
                "script.js" to """console.log("Neon Space Arcade Initializing...");
const canvas = document.getElementById("gameCanvas");
const ctx = canvas.getContext("2d");
const scoreEl = document.getElementById("score");
const shieldEl = document.getElementById("shield");
const hiScoreEl = document.getElementById("hi-score");
const finalScoreEl = document.getElementById("final-score");
const overlay = document.getElementById("game-over-overlay");
const restartBtn = document.getElementById("restart-btn");

let width, height;
function resize() {
  width = canvas.width = canvas.clientWidth;
  height = canvas.height = canvas.clientHeight;
}
window.addEventListener("resize", resize);
resize();

// Web Audio API Sound Synthesizer
let audioCtx;
function initAudio() {
  if (!audioCtx) audioCtx = new (window.AudioContext || window.webkitAudioContext)();
}
function playTone(freq, type = 'sine', duration = 0.1) {
  try {
    initAudio();
    if (!audioCtx) return;
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.type = type;
    osc.frequency.setValueAtTime(freq, audioCtx.currentTime);
    gain.gain.setValueAtTime(0.1, audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + duration);
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    osc.start();
    osc.stop(audioCtx.currentTime + duration);
  } catch (e) {
    console.warn("Audio not initialized yet:", e.message);
  }
}

let player = { x: 150, y: 0, vx: 0, shield: 100 };
let bullets = [];
let enemies = [];
let stars = [];
let particles = [];
let score = 0;
let hiScore = localStorage.getItem("neon_hi") || 0;
hiScoreEl.innerText = hiScore;
let isGameOver = false;

for (let i = 0; i < 50; i++) {
  stars.push({ x: Math.random() * 400, y: Math.random() * 800, speed: Math.random() * 2 + 1 });
}

function spawnEnemy() {
  if (isGameOver) return;
  enemies.push({
    x: Math.random() * (width - 40) + 20,
    y: -30,
    radius: 14 + Math.random() * 10,
    speed: 1.5 + Math.random() * 2.5,
    color: Math.random() > 0.5 ? '#ff007f' : '#ffaa00'
  });
}
setInterval(spawnEnemy, 1200);

function shoot() {
  if (isGameOver) return;
  bullets.push({ x: player.x, y: player.y - 15, vy: -7 });
  playTone(660, 'square', 0.08);
}

function createExplosion(x, y, color) {
  for (let i = 0; i < 15; i++) {
    const angle = Math.random() * Math.PI * 2;
    const speed = Math.random() * 4 + 1;
    particles.push({
      x, y,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed,
      life: 1.0,
      color
    });
  }
  playTone(150, 'sawtooth', 0.2);
}

// Touch Input
let moveDir = 0;
document.getElementById("btn-left").addEventListener("touchstart", (e) => { e.preventDefault(); moveDir = -1; });
document.getElementById("btn-left").addEventListener("touchend", () => { if (moveDir === -1) moveDir = 0; });
document.getElementById("btn-right").addEventListener("touchstart", (e) => { e.preventDefault(); moveDir = 1; });
document.getElementById("btn-right").addEventListener("touchend", () => { if (moveDir === 1) moveDir = 0; });
document.getElementById("btn-fire").addEventListener("touchstart", (e) => { e.preventDefault(); shoot(); });

// Mouse fallback for emulator testing
document.getElementById("btn-left").addEventListener("mousedown", () => moveDir = -1);
document.getElementById("btn-left").addEventListener("mouseup", () => { if (moveDir === -1) moveDir = 0; });
document.getElementById("btn-right").addEventListener("mousedown", () => moveDir = 1);
document.getElementById("btn-right").addEventListener("mouseup", () => { if (moveDir === 1) moveDir = 0; });
document.getElementById("btn-fire").addEventListener("click", () => shoot());

window.addEventListener("keydown", (e) => {
  if (e.key === "ArrowLeft") moveDir = -1;
  if (e.key === "ArrowRight") moveDir = 1;
  if (e.key === " " || e.key === "z") shoot();
});
window.addEventListener("keyup", (e) => {
  if (e.key === "ArrowLeft" && moveDir === -1) moveDir = 0;
  if (e.key === "ArrowRight" && moveDir === 1) moveDir = 0;
});

function resetGame() {
  score = 0;
  player.shield = 100;
  player.x = width / 2;
  bullets = [];
  enemies = [];
  particles = [];
  isGameOver = false;
  scoreEl.innerText = "0";
  shieldEl.innerText = "100%";
  overlay.classList.add("hidden");
  console.log("Game restarted!");
}
restartBtn.addEventListener("click", resetGame);

function loop() {
  ctx.clearRect(0, 0, width, height);

  // Background stars
  ctx.fillStyle = "#ffffff";
  for (let s of stars) {
    s.y += s.speed;
    if (s.y > height) { s.y = 0; s.x = Math.random() * width; }
    ctx.fillRect(s.x, s.y, 2, 2);
  }

  if (!isGameOver) {
    player.y = height - 50;
    player.x += moveDir * 5;
    if (player.x < 25) player.x = 25;
    if (player.x > width - 25) player.x = width - 25;

    // Draw player ship
    ctx.shadowBlur = 15;
    ctx.shadowColor = "#00e5ff";
    ctx.fillStyle = "#00e5ff";
    ctx.beginPath();
    ctx.moveTo(player.x, player.y - 20);
    ctx.lineTo(player.x - 18, player.y + 12);
    ctx.lineTo(player.x + 18, player.y + 12);
    ctx.closePath();
    ctx.fill();
    ctx.shadowBlur = 0;
  }

  // Update Bullets
  ctx.fillStyle = "#00ff88";
  for (let i = bullets.length - 1; i >= 0; i--) {
    let b = bullets[i];
    b.y += b.vy;
    ctx.fillRect(b.x - 2, b.y - 6, 4, 12);
    if (b.y < -10) bullets.splice(i, 1);
  }

  // Update Enemies
  for (let i = enemies.length - 1; i >= 0; i--) {
    let en = enemies[i];
    en.y += en.speed;
    ctx.shadowBlur = 10;
    ctx.shadowColor = en.color;
    ctx.fillStyle = en.color;
    ctx.beginPath();
    ctx.arc(en.x, en.y, en.radius, 0, Math.PI * 2);
    ctx.fill();
    ctx.shadowBlur = 0;

    // Collision with bullets
    for (let j = bullets.length - 1; j >= 0; j--) {
      let b = bullets[j];
      let dist = Math.hypot(b.x - en.x, b.y - en.y);
      if (dist < en.radius + 4) {
        createExplosion(en.x, en.y, en.color);
        enemies.splice(i, 1);
        bullets.splice(j, 1);
        score += 25;
        scoreEl.innerText = score;
        if (score > hiScore) {
          hiScore = score;
          hiScoreEl.innerText = hiScore;
          localStorage.setItem("neon_hi", hiScore);
        }
        break;
      }
    }

    // Collision with player
    if (!isGameOver && Math.hypot(player.x - en.x, player.y - en.y) < en.radius + 18) {
      createExplosion(en.x, en.y, "#ff007f");
      enemies.splice(i, 1);
      player.shield -= 35;
      if (player.shield <= 0) {
        player.shield = 0;
        isGameOver = true;
        finalScoreEl.innerText = score;
        overlay.classList.remove("hidden");
        console.error("Game Over! Final Score:", score);
      }
      shieldEl.innerText = player.shield + "%";
    }

    if (en.y > height + 40) enemies.splice(i, 1);
  }

  // Update Particles
  for (let i = particles.length - 1; i >= 0; i--) {
    let p = particles[i];
    p.x += p.vx;
    p.y += p.vy;
    p.life -= 0.03;
    if (p.life <= 0) {
      particles.splice(i, 1);
    } else {
      ctx.globalAlpha = p.life;
      ctx.fillStyle = p.color;
      ctx.beginPath();
      ctx.arc(p.x, p.y, 3, 0, Math.PI * 2);
      ctx.fill();
      ctx.globalAlpha = 1.0;
    }
  }

  requestAnimationFrame(loop);
}
requestAnimationFrame(loop);
"""
            )
        ),
        TemplateDefinition(
            id = "cyber_calc",
            name = "Cyberpunk Calculator",
            description = "Glassmorphic calculator with tactile sound feedback, calculation history, and smooth animations.",
            category = "Utility",
            iconType = "calc",
            accentColor = 0xFFA855F7,
            defaultPackageName = "com.hopweb.app.cybercalc",
            files = mapOf(
                "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <title>Cyber Calc</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div class="calculator-card">
    <div class="brand">CYBER//CALC <span class="indicator">● RUNNING</span></div>
    <div class="display-container">
      <div id="history-display" class="history-line"></div>
      <div id="main-display" class="main-line">0</div>
    </div>
    <div class="keypad">
      <button class="key action" data-val="C">AC</button>
      <button class="key action" data-val="DEL">⌫</button>
      <button class="key op" data-val="%">%</button>
      <button class="key op" data-val="/">÷</button>

      <button class="key" data-val="7">7</button>
      <button class="key" data-val="8">8</button>
      <button class="key" data-val="9">9</button>
      <button class="key op" data-val="*">×</button>

      <button class="key" data-val="4">4</button>
      <button class="key" data-val="5">5</button>
      <button class="key" data-val="6">6</button>
      <button class="key op" data-val="-">−</button>

      <button class="key" data-val="1">1</button>
      <button class="key" data-val="2">2</button>
      <button class="key" data-val="3">3</button>
      <button class="key op" data-val="+">+</button>

      <button class="key zero" data-val="0">0</button>
      <button class="key" data-val=".">.</button>
      <button class="key equals" data-val="=">=</button>
    </div>
  </div>
  <script src="script.js"></script>
</body>
</html>""",
                "style.css" to """* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
  user-select: none;
  -webkit-user-select: none;
}
body {
  background: radial-gradient(circle at top, #1e1233 0%, #0a0614 100%);
  color: #fff;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
.calculator-card {
  width: 100%;
  max-width: 360px;
  background: rgba(25, 18, 45, 0.7);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(168, 85, 247, 0.25);
  border-radius: 28px;
  padding: 20px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.6), 0 0 24px rgba(168, 85, 247, 0.2);
}
.brand {
  font-size: 11px;
  letter-spacing: 2px;
  color: #a855f7;
  font-weight: 800;
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}
.indicator {
  color: #10b981;
  font-size: 10px;
}
.display-container {
  background: rgba(10, 6, 20, 0.85);
  border: 1px solid rgba(168, 85, 247, 0.15);
  border-radius: 18px;
  padding: 16px;
  text-align: right;
  margin-bottom: 20px;
  min-height: 90px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}
.history-line {
  font-size: 14px;
  color: #94a3b8;
  min-height: 20px;
  word-break: break-all;
}
.main-line {
  font-size: 38px;
  font-weight: 700;
  color: #f1f5f9;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.keypad {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.key {
  height: 60px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.05);
  color: #e2e8f0;
  font-size: 20px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.1s ease;
  touch-action: manipulation;
}
.key:active {
  transform: scale(0.94);
  background: rgba(168, 85, 247, 0.3);
}
.key.action {
  color: #f43f5e;
  background: rgba(244, 63, 94, 0.12);
  border-color: rgba(244, 63, 94, 0.25);
}
.key.op {
  color: #38bdf8;
  background: rgba(56, 189, 248, 0.12);
  border-color: rgba(56, 189, 248, 0.25);
}
.key.equals {
  background: linear-gradient(135deg, #a855f7, #6366f1);
  color: #fff;
  border: none;
  box-shadow: 0 4px 16px rgba(168, 85, 247, 0.4);
}
.key.zero {
  grid-column: span 2;
}""",
                "script.js" to """console.log("Cyber Calc ready!");
let currentInput = "0";
let previousInput = "";
let operation = null;
let shouldResetScreen = false;

const mainDisplay = document.getElementById("main-display");
const historyDisplay = document.getElementById("history-display");

let audioCtx;
function playClick(freq = 440) {
  try {
    if (!audioCtx) audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.frequency.setValueAtTime(freq, audioCtx.currentTime);
    gain.gain.setValueAtTime(0.05, audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.06);
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    osc.start();
    osc.stop(audioCtx.currentTime + 0.06);
  } catch (e) {}
}

function updateDisplay() {
  mainDisplay.textContent = currentInput;
  if (operation) {
    historyDisplay.textContent = previousInput + " " + operation;
  } else {
    historyDisplay.textContent = "";
  }
}

function appendNumber(num) {
  playClick(500);
  if (currentInput === "0" || shouldResetScreen) {
    currentInput = num;
    shouldResetScreen = false;
  } else {
    if (num === "." && currentInput.includes(".")) return;
    currentInput += num;
  }
  updateDisplay();
}

function chooseOperation(op) {
  playClick(650);
  if (currentInput === "") return;
  if (previousInput !== "") {
    calculate();
  }
  operation = op;
  previousInput = currentInput;
  shouldResetScreen = true;
  updateDisplay();
}

function calculate() {
  playClick(800);
  let computation;
  const prev = parseFloat(previousInput);
  const current = parseFloat(currentInput);
  if (isNaN(prev) || isNaN(current)) return;

  switch (operation) {
    case "+": computation = prev + current; break;
    case "-": computation = prev - current; break;
    case "*": computation = prev * current; break;
    case "/":
      if (current === 0) {
        console.error("Division by zero attempted!");
        currentInput = "Error";
        updateDisplay();
        return;
      }
      computation = prev / current;
      break;
    case "%": computation = (prev * current) / 100; break;
    default: return;
  }

  console.log("Calculated:", prev, operation, current, "=", computation);
  currentInput = Math.round(computation * 100000000) / 100000000 + "";
  operation = null;
  previousInput = "";
  shouldResetScreen = true;
  updateDisplay();
}

function clearAll() {
  playClick(300);
  currentInput = "0";
  previousInput = "";
  operation = null;
  updateDisplay();
}

function deleteDigit() {
  playClick(350);
  if (currentInput.length === 1 || currentInput === "Error") {
    currentInput = "0";
  } else {
    currentInput = currentInput.slice(0, -1);
  }
  updateDisplay();
}

document.querySelectorAll(".key").forEach(button => {
  button.addEventListener("click", () => {
    const val = button.dataset.val;
    if (!isNaN(val) || val === ".") {
      appendNumber(val);
    } else if (val === "C") {
      clearAll();
    } else if (val === "DEL") {
      deleteDigit();
    } else if (val === "=") {
      calculate();
    } else {
      chooseOperation(val);
    }
  });
});
"""
            )
        ),
        TemplateDefinition(
            id = "neonotes_pwa",
            name = "NeoNotes Sticky PWA",
            description = "Offline responsive markdown note pad with localStorage, tagging, and quick search.",
            category = "Productivity",
            iconType = "palette",
            accentColor = 0xFF10B981,
            defaultPackageName = "com.hopweb.app.neonotes",
            files = mapOf(
                "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <title>NeoNotes PWA</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <header>
    <div class="logo">⚡ NeoNotes</div>
    <div class="actions">
      <input type="text" id="search-box" placeholder="Search notes...">
    </div>
  </header>
  <main>
    <div class="note-creator">
      <input type="text" id="note-title" placeholder="Note Title...">
      <textarea id="note-body" placeholder="Write your ideas here (supports simple markdown)..."></textarea>
      <div class="creator-footer">
        <div class="colors">
          <span class="color-dot active" data-color="#1e293b" style="background:#1e293b"></span>
          <span class="color-dot" data-color="#064e3b" style="background:#064e3b"></span>
          <span class="color-dot" data-color="#1e3a8a" style="background:#1e3a8a"></span>
          <span class="color-dot" data-color="#701a75" style="background:#701a75"></span>
        </div>
        <button id="add-note-btn">+ SAVE NOTE</button>
      </div>
    </div>
    <div id="notes-grid" class="notes-grid"></div>
  </main>
  <script src="script.js"></script>
</body>
</html>""",
                "style.css" to """* { box-sizing: border-box; margin: 0; padding: 0; }
body {
  background: #0f172a;
  color: #f8fafc;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  min-height: 100vh;
  padding: 12px;
}
header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid #1e293b;
}
.logo {
  font-weight: 800;
  font-size: 18px;
  color: #10b981;
}
#search-box {
  background: #1e293b;
  border: 1px solid #334155;
  color: #fff;
  border-radius: 20px;
  padding: 8px 16px;
  font-size: 13px;
  outline: none;
}
.note-creator {
  background: #1e293b;
  border-radius: 16px;
  padding: 14px;
  margin: 14px 0;
  border: 1px solid #334155;
}
#note-title {
  width: 100%;
  background: transparent;
  border: none;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 8px;
  outline: none;
}
#note-body {
  width: 100%;
  height: 80px;
  background: transparent;
  border: none;
  color: #cbd5e1;
  font-size: 14px;
  resize: none;
  outline: none;
}
.creator-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}
.colors { display: flex; gap: 8px; }
.color-dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
}
.color-dot.active { border-color: #fff; }
#add-note-btn {
  background: #10b981;
  color: #0f172a;
  border: none;
  font-weight: 700;
  border-radius: 12px;
  padding: 8px 18px;
  cursor: pointer;
}
.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}
.note-card {
  padding: 14px;
  border-radius: 14px;
  position: relative;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
  border: 1px solid rgba(255,255,255,0.06);
}
.note-card h3 { font-size: 15px; margin-bottom: 6px; }
.note-card p { font-size: 13px; color: #e2e8f0; white-space: pre-wrap; }
.note-del {
  position: absolute;
  top: 8px;
  right: 8px;
  background: transparent;
  border: none;
  color: #94a3b8;
  font-size: 14px;
  cursor: pointer;
}""",
                "script.js" to """console.log("NeoNotes ready!");
let selectedColor = "#1e293b";
let notes = JSON.parse(localStorage.getItem("hopweb_notes") || "[]");

if (notes.length === 0) {
  notes.push({
    id: Date.now(),
    title: "Welcome to HopWeb IDE",
    body: "You can edit this HTML/CSS/JS project live, preview changes immediately, and export it into an APK!",
    color: "#064e3b"
  });
  saveNotes();
}

function saveNotes() {
  localStorage.setItem("hopweb_notes", JSON.stringify(notes));
  renderNotes();
}

function renderNotes(filter = "") {
  const container = document.getElementById("notes-grid");
  container.innerHTML = "";
  const list = notes.filter(n =>
    n.title.toLowerCase().includes(filter.toLowerCase()) ||
    n.body.toLowerCase().includes(filter.toLowerCase())
  );

  list.forEach(note => {
    const card = document.createElement("div");
    card.className = "note-card";
    card.style.background = note.color;
    card.innerHTML = `
      <h3>${"$"}{note.title}</h3>
      <p>${"$"}{note.body}</p>
      <button class="note-del" onclick="deleteNote(${'$'}{note.id})">✕</button>
    `;
    container.appendChild(card);
  });
}

window.deleteNote = function(id) {
  notes = notes.filter(n => n.id !== id);
  console.log("Deleted note:", id);
  saveNotes();
};

document.querySelectorAll(".color-dot").forEach(dot => {
  dot.addEventListener("click", () => {
    document.querySelectorAll(".color-dot").forEach(d => d.classList.remove("active"));
    dot.classList.add("active");
    selectedColor = dot.dataset.color;
  });
});

document.getElementById("add-note-btn").addEventListener("click", () => {
  const title = document.getElementById("note-title").value.trim();
  const body = document.getElementById("note-body").value.trim();
  if (!title && !body) return;

  notes.unshift({
    id: Date.now(),
    title: title || "Untitled Note",
    body: body,
    color: selectedColor
  });

  document.getElementById("note-title").value = "";
  document.getElementById("note-body").value = "";
  console.log("Note added:", title);
  saveNotes();
});

document.getElementById("search-box").addEventListener("input", (e) => {
  renderNotes(e.target.value);
});

renderNotes();
"""
            )
        ),
        TemplateDefinition(
            id = "drum_synth",
            name = "Audio Pad Synthesizer",
            description = "Interactive Web Audio touch synthesizer with 8 sound pads and beat visualizer.",
            category = "Creative",
            iconType = "rocket",
            accentColor = 0xFFF59E0B,
            defaultPackageName = "com.hopweb.app.drumpad",
            files = mapOf(
                "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <title>Audio Pad Synth</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div class="synth-container">
    <h2>⚡ AUDIO SYNTH PADS</h2>
    <div id="visualizer"></div>
    <div class="pad-grid">
      <button class="pad" data-freq="130" data-type="sine" style="--c:#38bdf8">KICK<br><span>130Hz</span></button>
      <button class="pad" data-freq="220" data-type="triangle" style="--c:#818cf8">SNARE<br><span>220Hz</span></button>
      <button class="pad" data-freq="330" data-type="sawtooth" style="--c:#c084fc">CLAP<br><span>330Hz</span></button>
      <button class="pad" data-freq="440" data-type="square" style="--c:#f472b6">HI-HAT<br><span>440Hz</span></button>
      <button class="pad" data-freq="550" data-type="sine" style="--c:#fb7185">LEAD A<br><span>550Hz</span></button>
      <button class="pad" data-freq="660" data-type="sawtooth" style="--c:#f59e0b">LEAD B<br><span>660Hz</span></button>
      <button class="pad" data-freq="880" data-type="triangle" style="--c:#10b981">CHORD<br><span>880Hz</span></button>
      <button class="pad" data-freq="1100" data-type="sine" style="--c:#06b6d4">BELL<br><span>1.1kHz</span></button>
    </div>
  </div>
  <script src="script.js"></script>
</body>
</html>""",
                "style.css" to """* { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }
body {
  background: #090a0f;
  color: #fff;
  font-family: monospace;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
.synth-container {
  width: 100%;
  max-width: 380px;
  background: #12141f;
  border-radius: 24px;
  padding: 20px;
  border: 1px solid #23273c;
  box-shadow: 0 10px 30px rgba(0,0,0,0.7);
}
h2 {
  font-size: 14px;
  text-align: center;
  color: #f59e0b;
  margin-bottom: 14px;
  letter-spacing: 2px;
}
#visualizer {
  height: 36px;
  background: #090a0f;
  border-radius: 8px;
  margin-bottom: 16px;
  border: 1px solid #1f2338;
  display: flex;
  align-items: flex-end;
  gap: 3px;
  padding: 4px 8px;
}
.bar {
  flex: 1;
  background: #f59e0b;
  border-radius: 2px;
  transition: height 0.08s ease;
}
.pad-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.pad {
  height: 75px;
  border-radius: 14px;
  background: #191c2b;
  border: 2px solid var(--c);
  color: #fff;
  font-weight: bold;
  font-size: 14px;
  cursor: pointer;
  touch-action: manipulation;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
}
.pad span { font-size: 10px; color: #94a3b8; font-weight: normal; }
.pad:active {
  background: var(--c);
  color: #000;
  transform: scale(0.96);
}""",
                "script.js" to """console.log("Audio Synth Pads active!");
let audioCtx;
const visualizer = document.getElementById("visualizer");
for (let i = 0; i < 16; i++) {
  const bar = document.createElement("div");
  bar.className = "bar";
  bar.style.height = "10%";
  visualizer.appendChild(bar);
}
const bars = document.querySelectorAll(".bar");

function triggerPad(freq, type) {
  try {
    if (!audioCtx) audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.type = type;
    osc.frequency.setValueAtTime(freq, audioCtx.currentTime);
    gain.gain.setValueAtTime(0.3, audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.35);
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    osc.start();
    osc.stop(audioCtx.currentTime + 0.35);

    bars.forEach(b => {
      b.style.height = (Math.random() * 80 + 20) + "%";
      setTimeout(() => b.style.height = "10%", 150);
    });
    console.log("Triggered:", type, freq + "Hz");
  } catch(e) {
    console.warn("Audio Context init required on interaction:", e.message);
  }
}

document.querySelectorAll(".pad").forEach(pad => {
  pad.addEventListener("click", () => {
    triggerPad(parseFloat(pad.dataset.freq), pad.dataset.type);
  });
});
"""
            )
        )
    )
}
