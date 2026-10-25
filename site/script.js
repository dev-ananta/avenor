/* Avenor web demo — the same engine and screens as the iOS (Swift) and Android (Kotlin) apps.
   Planner ranking, quick-add parsing, Leitner review and the kind streak mirror Engine.swift / Engine.kt. */
(() => {
  "use strict";

  // ---------- Utilities ----------
  const DAY = 86400000;
  const $ = (sel, root = document) => root.querySelector(sel);
  const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
  const startOfDay = (d = new Date()) => { const x = new Date(d); x.setHours(0, 0, 0, 0); return x; };
  const addDays = (d, n) => { const x = new Date(d); x.setDate(x.getDate() + n); return x; };
  const iso = (d) => { const x = startOfDay(d); return `${x.getFullYear()}-${String(x.getMonth() + 1).padStart(2, "0")}-${String(x.getDate()).padStart(2, "0")}`; };
  const fromIso = (s) => { const [y, m, d] = s.split("-").map(Number); return new Date(y, m - 1, d); };
  const daysUntil = (isoDate) => Math.round((fromIso(isoDate) - startOfDay()) / DAY);
  const uid = () => Math.random().toString(36).slice(2, 10);
  const clock = (ms) => { const t = Math.ceil(ms / 1000); return `${String(Math.floor(t / 60)).padStart(2, "0")}:${String(t % 60).padStart(2, "0")}`; };
  const PALETTE = ["#8b9cff", "#65d6a0", "#ffb86b", "#fa8cbe", "#73cdfa", "#c8a0ff", "#ffd666", "#ff7373"];
  const PRIORITY = { low: { label: "Low", w: 1 }, medium: { label: "Medium", w: 2 }, high: { label: "High", w: 3 } };

  // ---------- Storage (per-browser convenience only; the demo works without it) ----------
  const KEY = "avenor-demo-v3";
  const load = () => { try { return JSON.parse(localStorage.getItem(KEY)); } catch { return null; } };
  const save = () => { try { localStorage.setItem(KEY, JSON.stringify(state)); } catch { /* private mode: fine */ } };

  function seed() {
    const t = startOfDay();
    const classes = [
      { id: "bio", name: "Biology", color: 1 }, { id: "math", name: "Math", color: 0 },
      { id: "hist", name: "History", color: 5 }, { id: "eng", name: "English", color: 2 },
    ];
    return {
      classes,
      tasks: [
        { id: uid(), title: "Bio lab report", classId: "bio", due: iso(addDays(t, 1)), priority: "high", minutes: 50, done: false, created: 1, focused: 0 },
        { id: uid(), title: "Algebra 2 worksheet", classId: "math", due: iso(t), priority: "medium", minutes: 30, done: false, created: 2, focused: 0 },
        { id: uid(), title: "APUSH reading ch. 12", classId: "hist", due: iso(addDays(t, 3)), priority: "medium", minutes: 40, done: false, created: 3, focused: 0 },
        { id: uid(), title: "English essay outline", classId: "eng", due: iso(addDays(t, 5)), priority: "low", minutes: 25, done: false, created: 4, focused: 0 },
        { id: uid(), title: "Email coach about Saturday", classId: null, due: null, priority: "low", minutes: 5, done: false, created: 5, focused: 0 },
      ],
      events: [
        { id: uid(), title: "Chemistry quiz", classId: null, day: iso(addDays(t, 2)), time: "10:15 AM" },
        { id: uid(), title: "Robotics club", classId: null, day: iso(t), time: "3:30 PM – 5:00 PM" },
      ],
      cards: [
        { id: uid(), classId: "bio", front: "What does the mitochondria do?", back: "Releases energy from food (cellular respiration) as ATP.", box: 2, due: Date.now() - 1000 },
        { id: uid(), classId: "hist", front: "What year did the Constitution take effect?", back: "1789.", box: 1, due: Date.now() - 1000 },
        { id: uid(), classId: null, front: "What are the two study habits that make memories last?", back: "Testing yourself (retrieval practice) and spacing reviews out over days.", box: 1, due: Date.now() - 1000 },
      ],
      sessions: [],
      reviewLog: {},
      activeDays: [iso(addDays(t, -1)), iso(addDays(t, -2)), iso(addDays(t, -4))],
      profile: { goal: 60, session: 25 },
    };
  }

  let state = load() || seed();
  let ui = { tab: "today", planSeg: "tasks", skipped: [], focusPick: null, focusMinutes: null, custom: "", selectedDay: iso(new Date()), deleted: null };

  // ---------- Engine ----------
  const cls = (id) => state.classes.find((c) => c.id === id);
  const clsColor = (id) => (cls(id) ? PALETTE[cls(id).color % PALETTE.length] : "#969ba6");

  function score(t) {
    let urgency = 0.75;
    if (t.due) {
      const d = daysUntil(t.due);
      urgency = d < 0 ? 6 : d === 0 ? 5 : d === 1 ? 4 : d <= 3 ? 3 : d <= 7 ? 2 : 1;
    }
    const remaining = Math.max(t.minutes - (t.focused || 0), 0);
    return urgency * 2 + PRIORITY[t.priority].w * 1.5 + (remaining <= 15 ? 0.5 : 0);
  }
  const ranked = () => state.tasks.filter((t) => !t.done).sort((a, b) => score(b) - score(a) || a.created - b.created);
  const sessionsFor = (min, len) => Math.max(1, Math.ceil(Math.max(min, 1) / Math.max(len, 5)));
  function bucket(t) {
    if (!t.due) return "No due date";
    const d = daysUntil(t.due);
    return d < 0 ? "Overdue" : d === 0 ? "Today" : d === 1 ? "Tomorrow" : d <= 6 ? "This week" : "Later";
  }
  function dueLabel(isoDate) {
    const d = daysUntil(isoDate), date = fromIso(isoDate);
    if (d < 0) return `Overdue · ${date.toLocaleDateString(undefined, { month: "short", day: "numeric" })}`;
    if (d === 0) return "Today";
    if (d === 1) return "Tomorrow";
    if (d <= 6) return date.toLocaleDateString(undefined, { weekday: "long" });
    return date.toLocaleDateString(undefined, { month: "short", day: "numeric" });
  }
  function reason(t) {
    const parts = [];
    if (t.due) { const d = daysUntil(t.due); parts.push(d < 0 ? "Overdue" : d === 0 ? "Due today" : d === 1 ? "Due tomorrow" : `Due ${dueLabel(t.due)}`); }
    if (t.priority === "high") parts.push("High priority");
    const n = sessionsFor(Math.max(t.minutes - (t.focused || 0), 5), state.profile.session);
    parts.push(n === 1 ? "1 session" : `${n} sessions`);
    return parts.join(" · ");
  }

  // Quick add: "Bio essay fri 45m !"
  const FULL_DAYS = { sunday: 0, monday: 1, tuesday: 2, wednesday: 3, thursday: 4, friday: 5, saturday: 6 };
  const SHORT_DAYS = { sun: 0, mon: 1, tue: 2, tues: 2, wed: 3, weds: 3, thu: 4, thur: 4, thurs: 4, fri: 5, sat: 6 };
  function duration(w) {
    for (const [suf, mult] of [["mins", 1], ["min", 1], ["m", 1], ["hrs", 60], ["hr", 60], ["h", 60]]) {
      if (w.endsWith(suf)) {
        const num = w.slice(0, -suf.length);
        if (/^\d+(\.\d+)?$/.test(num)) { const v = parseFloat(num) * mult; if (v > 0 && v <= 600) return Math.round(v); }
      }
    }
    return null;
  }
  function parseDate(raw) {
    const w = raw.toLowerCase().replace(/[,.;]+$/, "");
    const today = startOfDay();
    if (w === "today" || w === "tonight") return iso(today);
    if (["tomorrow", "tmrw", "tmr"].includes(w)) return iso(addDays(today, 1));
    const allCaps = raw.length > 1 && raw === raw.toUpperCase();   // "SAT prep" is a title, not Saturday
    const wd = w in FULL_DAYS ? FULL_DAYS[w] : (!allCaps && w in SHORT_DAYS ? SHORT_DAYS[w] : null);
    if (wd !== null) return iso(addDays(today, (wd - today.getDay() + 7) % 7));
    const m = w.match(/^(\d{1,2})\/(\d{1,2})(?:\/(\d{2,4}))?$/);
    if (m) {
      const mo = +m[1], d = +m[2];
      if (mo < 1 || mo > 12 || d < 1 || d > 31) return null;
      let y = m[3] ? +m[3] : today.getFullYear(); if (y < 100) y += 2000;
      let date = new Date(y, mo - 1, d);
      if (!m[3] && date < today) date = new Date(y + 1, mo - 1, d);
      return iso(date);
    }
    return null;
  }
  function parseQuick(input) {
    const words = input.trim().split(/\s+/).filter(Boolean);
    const out = { title: "", due: null, minutes: null, priority: null, classId: null };
    const title = [];
    for (let i = 0; i < words.length; i++) {
      const raw = words[i], w = raw.toLowerCase().replace(/[,.;]+$/, "");
      if (["!", "!!", "!!!", "!high", "!h"].includes(w)) out.priority = "high";
      else if (["!low", "!l"].includes(w)) out.priority = "low";
      else if (["!med", "!medium", "!m"].includes(w)) out.priority = "medium";
      else if (duration(w) !== null) out.minutes = duration(w);
      else if (w.startsWith("#") && w.length > 1 && state.classes.find((c) => c.name.toLowerCase().startsWith(w.slice(1)))) out.classId = state.classes.find((c) => c.name.toLowerCase().startsWith(w.slice(1))).id;
      else if (["due", "by", "on"].includes(w) && i + 1 < words.length && parseDate(words[i + 1])) { out.due = parseDate(words[i + 1]); i++; }
      else if (parseDate(raw)) out.due = parseDate(raw);
      else title.push(raw);
    }
    out.title = title.join(" ");
    if (!out.classId) {
      const lower = out.title.toLowerCase(), tokens = new Set(lower.split(/[^\p{L}\p{N}]+/u));
      const hit = state.classes.find((c) => { const n = c.name.toLowerCase(); return tokens.has(n) || (n.length >= 4 && lower.includes(n)) || [...tokens].some((tok) => tok.length >= 3 && n.startsWith(tok)); });
      if (hit) out.classId = hit.id;
    }
    return out;
  }

  // Leitner boxes: 1, 2, 4, 8, 16 days
  const INTERVALS = { 1: 1, 2: 2, 3: 4, 4: 8, 5: 16 };
  function review(card, remembered, relearning) {
    if (remembered) { card.box = relearning ? 1 : Math.min(card.box + 1, 5); card.due = addDays(startOfDay(), INTERVALS[card.box]).getTime(); }
    else { card.box = 1; card.due = Date.now() + 10 * 60000; }
    const k = iso(new Date()); state.reviewLog[k] = (state.reviewLog[k] || 0) + 1; markActive();
  }
  const dueCards = () => state.cards.filter((c) => c.due <= Date.now()).sort((a, b) => a.due - b.due);

  // Kind streak: one missed day forgiven per week of streak
  function streak() {
    const active = new Set(state.activeDays);
    let day = active.has(iso(new Date())) ? startOfDay() : addDays(startOfDay(), -1), count = 0, graces = 0;
    for (let i = 0; i < 3650; i++) {
      if (active.has(iso(day))) count++;
      else if (count > 0 && graces < 1 + Math.floor(count / 7) && active.has(iso(addDays(day, -1)))) graces++;
      else break;
      day = addDays(day, -1);
    }
    return count;
  }
  const markActive = () => { const k = iso(new Date()); if (!state.activeDays.includes(k)) state.activeDays.push(k); };
  const todayFocus = () => state.sessions.filter((s) => iso(new Date(s.start)) === iso(new Date())).reduce((a, s) => a + Math.floor(s.seconds / 60), 0);
  const doneToday = () => state.tasks.filter((t) => t.doneAt && iso(new Date(t.doneAt)) === iso(new Date())).length;

  // ---------- Shared bits of markup ----------
  const chip = (text, color = "#969ba6") => `<span class="chip" style="--c:${color}">${esc(text)}</span>`;
  function taskRow(t, { del = false } = {}) {
    const c = cls(t.classId), overdue = !t.done && t.due && daysUntil(t.due) < 0;
    return `<div class="task-row">
      <button class="check ${t.done ? "done" : ""}" data-act="toggle" data-id="${t.id}" aria-label="${t.done ? "Mark incomplete" : "Mark complete"}: ${esc(t.title)}"><i>${t.done ? "✓" : ""}</i></button>
      <div class="task-main ${t.done ? "done" : ""}" >
        <b>${esc(t.title)}</b>
        <div class="task-meta">${c ? chip(c.name, clsColor(t.classId)) : ""}${!t.done && t.due ? chip(dueLabel(t.due), overdue ? "#ff7373" : "#969ba6") : ""}${!t.done ? chip(t.minutes + "m") : ""}${t.priority === "high" && !t.done ? chip("! High", "#ff7373") : ""}</div>
      </div>
      ${del ? `<button class="del" data-act="focus-task" data-id="${t.id}" aria-label="Focus on ${esc(t.title)}" title="Focus">◷</button><button class="del" data-act="delete" data-id="${t.id}" aria-label="Delete ${esc(t.title)}" title="Delete">✕</button>` : ""}
    </div>`;
  }
  const quickBar = () => `<div class="quick">
      <div class="quick-chips" id="quickChips"></div>
      <form class="quick-field" data-form="quick" autocomplete="off">
        <span class="plus" aria-hidden="true">＋</span>
        <label class="sr-only" for="quickInput" style="position:absolute;left:-9999px">Add a task</label>
        <input id="quickInput" placeholder="Add a task — try “Bio essay fri 45m !”" enterkeyhint="done">
        <button type="submit" aria-label="Add task">↑</button>
      </form>
    </div>`;

  // ---------- Screens ----------
  function renderToday() {
    const r = ranked(), hero = r.find((t) => !ui.skipped.includes(t.id)) || r[0];
    const upNext = r.filter((t) => t !== hero).slice(0, 3);   // Miller's Law
    const focus = todayFocus(), goal = state.profile.goal, p = Math.min(focus / goal, 1), s = streak(), due = dueCards().length;
    const msg = focus === 0 ? "One session gets you started." : focus >= goal ? "Goal reached. Anything more is bonus." : p >= .5 ? `Only ${goal - focus} min to today's goal.` : `Nice start — ${goal - focus} min to go.`;
    const hour = new Date().getHours(), greet = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
    const events = state.events.filter((e) => e.day === iso(new Date())).slice(0, 3);
    return `
      <div><div class="a-title">${greet}</div><div class="a-sub">${new Date().toLocaleDateString(undefined, { weekday: "long", month: "long", day: "numeric" })}</div></div>
      <div class="a-panel a-progress">
        <div class="ring" style="--p:${p}" role="img" aria-label="${focus} of ${goal} focus minutes today"><div><b>${focus}</b><small>of ${goal}m</small></div></div>
        <div><b>${msg}</b><div class="chips">${chip(`🔥 ${s}-day streak`, "#ffb86b")}${chip(`✓ ${doneToday()} done`, "#65d6a0")}</div></div>
      </div>
      <div class="a-hero">
        <div class="eyebrow">FOCUS NOW</div>
        ${hero ? `<h3>${esc(hero.title)}</h3><div class="reason">${esc(reason(hero))}</div>
          <button class="primary-button white full" data-act="start" data-id="${hero.id}">▶ Start ${state.profile.session}-min focus</button>
          <div class="row">
            <button class="link-btn" data-act="toggle" data-id="${hero.id}">✓ Done</button>
            <button class="link-btn" data-act="start5" data-id="${hero.id}">⚡ Just 5 min</button>
            ${r.length > 1 ? `<button class="link-btn" data-act="skip" data-id="${hero.id}">→ Not now</button>` : ""}
          </div>`
        : `<h3>All caught up!</h3><div class="reason">Add something below, or start a free study session.</div>
           <button class="primary-button white full" data-act="start-free">▶ Start free focus</button>`}
      </div>
      ${upNext.length ? `<div class="a-heading">Up next ${r.length > 4 ? `<small>+${r.length - 4} more</small>` : ""}</div>
        <div class="a-panel">${upNext.map((t) => taskRow(t)).join("")}</div>
        <button class="link-btn accent" data-act="tab" data-tab="plan">See your whole plan</button>` : ""}
      ${events.length ? `<div class="a-heading">Today's schedule</div><div class="a-panel">${events.map((e) => `<div style="padding:4px 0"><b>${esc(e.title)}</b><div class="a-sub">${esc(e.time)}</div></div>`).join("")}</div>` : ""}
      ${due ? `<button class="a-panel choice" data-act="tab" data-tab="review"><span style="font-size:22px">🧠</span><span><b>${due} card${due === 1 ? "" : "s"} ready to review</b><small>About ${Math.max(1, Math.floor(due / 4))} min · keeps today's learning from fading</small></span></button>` : ""}
      ${quickBar()}`;
  }

  function renderPlan() {
    const seg = ui.planSeg;
    const header = `<div class="a-title">Plan</div>
      <div class="seg" role="group" aria-label="Plan section">
        ${["tasks", "calendar", "classes"].map((s) => `<button data-act="seg" data-seg="${s}" aria-pressed="${seg === s}">${s[0].toUpperCase() + s.slice(1)}</button>`).join("")}
      </div>`;
    if (seg === "tasks") {
      const r = ranked(), order = ["Overdue", "Today", "Tomorrow", "This week", "Later", "No due date"];
      const groups = order.map((b) => [b, r.filter((t) => bucket(t) === b)]).filter(([, l]) => l.length);
      const done = state.tasks.filter((t) => t.done);
      return header + (groups.length ? groups.map(([b, l]) => `<div class="bucket ${b === "Overdue" ? "overdue" : ""}"><span>${b.toUpperCase()}</span><span>${l.length}</span></div>
          <div class="a-panel">${l.map((t) => taskRow(t, { del: true })).join("")}</div>`).join("")
        : `<div class="a-panel"><b>No tasks yet</b><div class="a-sub">Type one below. Add a day, a time and “!” for important.</div></div>`)
        + (done.length ? `<details class="a-panel"><summary style="cursor:pointer;min-height:44px;display:flex;align-items:center;color:var(--av-muted)">Completed (${done.length})</summary>${done.map((t) => taskRow(t, { del: true })).join("")}</details>` : "")
        + (ui.deleted ? `<div class="toast" role="status"><span>Task deleted</span><button class="link-btn accent" data-act="undo">Undo</button></div>` : "")
        + quickBar();
    }
    if (seg === "calendar") {
      const start = addDays(startOfDay(), -startOfDay().getDay());
      const days = [...Array(7)].map((_, i) => addDays(start, i));
      const sel = ui.selectedDay;
      const evs = state.events.filter((e) => e.day === sel), ts = state.tasks.filter((t) => t.due === sel);
      return header + `<div style="display:grid;grid-template-columns:repeat(7,1fr);gap:5px">
        ${days.map((d) => { const k = iso(d), on = k === sel, has = state.events.some((e) => e.day === k) || state.tasks.some((t) => t.due === k && !t.done);
          return `<button data-act="day" data-day="${k}" aria-pressed="${on}" aria-label="${d.toLocaleDateString(undefined, { weekday: "long", month: "long", day: "numeric" })}"
            style="min-height:60px;border:0;border-radius:14px;cursor:pointer;background:${on ? "var(--av-accent)" : "var(--av-surface)"};color:${on ? "#000" : "var(--av-text)"}">
            <small style="display:block;font-size:10px;opacity:.7">${d.toLocaleDateString(undefined, { weekday: "narrow" })}</small><b>${d.getDate()}</b>
            <i style="display:block;width:5px;height:5px;border-radius:50%;margin:2px auto 0;background:${has ? (on ? "#000" : "var(--av-accent)") : "transparent"}"></i></button>`; }).join("")}
        </div>
        <div class="bucket"><span>${fromIso(sel).toLocaleDateString(undefined, { weekday: "long", month: "long", day: "numeric" }).toUpperCase()}</span></div>
        ${evs.length || ts.length ? `<div class="a-panel">${evs.map((e) => `<div style="padding:6px 0"><b>${esc(e.title)}</b><div class="a-sub">${esc(e.time)}</div></div>`).join("")}${ts.map((t) => taskRow(t)).join("")}</div>`
          : `<div class="a-panel a-sub">Nothing scheduled. Enjoy the space.</div>`}`;
    }
    return header + `<div class="a-panel">${state.classes.map((c) => {
        const open = state.tasks.filter((t) => t.classId === c.id && !t.done).length, cards = state.cards.filter((k) => k.classId === c.id).length;
        return `<div style="display:flex;gap:12px;align-items:center;padding:8px 0"><span style="width:40px;height:40px;border-radius:12px;display:grid;place-items:center;background:color-mix(in srgb, ${PALETTE[c.color % 8]} 18%, transparent);color:${PALETTE[c.color % 8]}">▤</span>
          <span><b>${esc(c.name)}</b><div class="a-sub" style="font-size:12px">${open} open task${open === 1 ? "" : "s"} · ${cards} card${cards === 1 ? "" : "s"}</div></span></div>`; }).join("")}</div>
      <form class="quick-field" data-form="class" autocomplete="off"><span class="plus">＋</span><input id="classInput" placeholder="Add a class" aria-label="Add a class"><button type="submit" aria-label="Add class">↑</button></form>`;
  }

  function renderFocus() {
    const cands = ranked().slice(0, 5);   // Miller's Law
    if (ui.focusPick === null && cands[0]) ui.focusPick = cands[0].id;
    const minutes = ui.focusMinutes || state.profile.session;
    const presets = [1, 15, 25];   // Hick's Law: three choices (1 = demo)
    return `<div><div class="a-title">Focus</div><div class="a-sub">One task. One timer. Phone face-down.</div></div>
      <b>What are you working on?</b>
      <div class="choice-list">${cands.map((t, i) => `<button class="choice" data-act="pick" data-id="${t.id}" aria-pressed="${ui.focusPick === t.id && !ui.custom}">
        <span class="dot"></span><span style="flex:1"><b>${esc(t.title)}</b><small>${esc(reason(t))}</small></span>${i === 0 ? chip("Suggested", "#8b9cff") : ""}</button>`).join("")}
        <input class="text-input" id="customFocus" placeholder="Or type something else…" value="${esc(ui.custom)}" aria-label="Something else to focus on">
      </div>
      <b>How long?</b>
      <div class="presets">${presets.map((m) => `<button class="preset" data-act="minutes" data-min="${m}" aria-pressed="${minutes === m}">${m}<small>${m === 1 ? "min (demo)" : "min"}</small></button>`).join("")}</div>
      <button class="link-btn accent" data-act="focus5">⚡ Stuck? Just start with 5 minutes</button>
      <div class="a-panel a-sub"><b style="color:var(--av-text)">How a round works</b><br>1. Focus until the ring fills. 2. Write one question you can now answer — it becomes a review card. 3. Take a real break, then go again.</div>
      <button class="primary-button full" data-act="focus-go" style="position:sticky;bottom:0">▶ Start ${minutes}-min session</button>`;
  }

  function renderReview() {
    const due = dueCards().length;
    const groups = [...state.classes.map((c) => [c.name, PALETTE[c.color % 8], state.cards.filter((k) => k.classId === c.id)]),
      ["General", "#969ba6", state.cards.filter((k) => !cls(k.classId))]].filter(([, , l]) => l.length);
    const dots = (box, color) => `<span class="dots" style="--c:${color}" role="img" aria-label="Mastery ${box} of 5">${[1, 2, 3, 4, 5].map((n) => `<i class="${n <= box ? "on" : ""}"></i>`).join("")}</span>`;
    return `<div class="a-title">Review</div>
      ${due ? `<div class="a-hero"><div class="eyebrow">READY TO REVIEW</div><h3 style="font-size:30px">${due} card${due === 1 ? "" : "s"}</h3>
          <div class="reason">About ${Math.max(1, Math.floor(due / 4))} min. Answer in your head before you flip.</div>
          <button class="primary-button white full" data-act="review-go">▶ Start review</button></div>`
        : `<div class="a-panel"><b style="color:var(--av-green);font-size:20px">✓ All caught up</b><div class="a-sub">Cards come back right before you'd forget them.</div></div>`}
      ${groups.map(([name, color, list]) => `<div class="bucket"><span>${esc(name.toUpperCase())}</span><span>${list.length}</span></div>
        <div class="a-panel">${list.map((k) => `<div style="display:flex;gap:10px;align-items:center;padding:6px 0"><span style="flex:1"><b style="font-size:14px">${esc(k.front)}</b>
          <div class="a-sub" style="font-size:12px;color:${k.due <= Date.now() ? "var(--av-accent)" : "var(--av-muted)"}">${k.due <= Date.now() ? "Due now" : "Next: " + (Math.round((startOfDay(new Date(k.due)) - startOfDay()) / DAY) <= 1 ? "tomorrow" : `in ${Math.round((startOfDay(new Date(k.due)) - startOfDay()) / DAY)} days`)}</div></span>${dots(k.box, color)}</div>`).join("")}</div>`).join("")}`;
  }

  // ---------- Study Mode (overlay) ----------
  let focus = null;   // { taskId, label, planned, started, banked, segStart, phase }
  let tick = null;
  const elapsed = () => focus.banked + (focus.segStart ? Date.now() - focus.segStart : 0);
  const remaining = () => Math.max(0, focus.planned - elapsed());

  function startFocus(taskId, label, minutes) {
    focus = { taskId, label: label || "Focus session", planned: minutes * 60000, started: Date.now(), banked: 0, segStart: Date.now(), phase: "focusing", rounds: focus ? focus.rounds : 0 };
    renderOverlay();
    clearInterval(tick);
    tick = setInterval(() => {
      if (!focus) return;
      if (focus.phase === "focusing" && remaining() <= 0) return finishFocus();
      updateTimer();
    }, 250);
  }
  function finishFocus() {
    const ms = Math.min(elapsed(), focus.planned);
    focus.banked = ms; focus.segStart = null; focus.phase = "finished";
    focus.reached = ms >= focus.planned - 1000;
    if (focus.reached) focus.rounds++;
    if (ms >= 60000 || focus.reached) {
      state.sessions.push({ taskId: focus.taskId, label: focus.label, seconds: Math.round(ms / 1000), start: focus.started, reached: focus.reached });
      const t = state.tasks.find((x) => x.id === focus.taskId); if (t) t.focused = (t.focused || 0) + Math.floor(ms / 60000);
      if (ms >= 60000) markActive();
      save();
    } else if (!focus.reached) { return closeFocus(); }
    renderOverlay();
  }
  function closeFocus() { focus = null; clearInterval(tick); $("#overlay")?.remove(); render(); }

  const encouragement = (p) => (p < .15 ? "Settle in. Just this one thing." : p < .5 ? "You're in the zone." : p < .75 ? "Past halfway — keep going." : "Final stretch!");
  function updateTimer() {
    if (!focus) return;
    const br = $("#breakText");
    if (br && focus.breakEnds) { const left = focus.breakEnds - Date.now(); br.textContent = left > 0 ? clock(left) : "Ready when you are"; }
    const ring = $("#timerRing"); if (!ring) return;
    const p = Math.min(elapsed() / focus.planned, 1);
    ring.style.setProperty("--p", p);
    $("#timerText").textContent = clock(remaining());
    $("#timerSub").textContent = focus.phase === "paused" ? "Paused" : encouragement(p);
  }

  // Study Mode and review sets cover the whole demo phone, tab bar included — nothing else to tap.
  function overlayEl() {
    let o = $("#overlay");
    if (!o) { o = document.createElement("div"); o.id = "overlay"; o.className = "overlay"; o.setAttribute("role", "dialog"); o.setAttribute("aria-modal", "true"); $("#app").appendChild(o); }
    return o;
  }

  function renderOverlay() {
    const o = overlayEl();
    const task = state.tasks.find((t) => t.id === focus.taskId);
    if (focus.phase === "focusing" || focus.phase === "paused") {
      const paused = focus.phase === "paused";
      o.innerHTML = `<div class="center"><div class="a-sub" style="font-weight:900;letter-spacing:.15em;font-size:11px">FOCUSING ON</div><b style="font-size:19px">${esc(focus.label)}</b></div>
        <div class="timer-ring ${paused ? "paused" : ""}" id="timerRing" style="--p:0" role="timer" aria-live="off"><div><b id="timerText">--:--</b><span id="timerSub"></span></div></div>
        <div class="a-sub center">Phone face-down. Notifications can wait.</div>
        <button class="primary-button full" data-act="pause">${paused ? "▶ Resume" : "❚❚ Pause"}</button>
        <button class="hold" id="holdEnd" aria-label="End session early (hold)"><span>Hold to end early</span></button>`;
      updateTimer(); wireHold();
    } else if (focus.phase === "finished") {
      const mins = Math.floor(focus.banked / 60000), goal = state.profile.goal;
      o.innerHTML = `<div class="big-icon" aria-hidden="true">${focus.reached ? "🎉" : "🌱"}</div>
        <h3 class="center" style="font-size:26px">${focus.reached ? "Session complete!" : "Nice — that still counts."}</h3>
        <p class="a-sub center">${focus.reached ? `${Math.max(mins, 1)} focused minute${mins === 1 ? "" : "s"} on ${esc(focus.label)}.` : `${mins} focused minutes is real progress. Shorter rounds are fine.`}</p>
        <div class="stat-row"><div class="mini-stat"><b style="color:var(--av-accent)">${todayFocus()}m</b><small>today</small></div><div class="mini-stat"><b style="color:var(--av-orange)">${streak()}</b><small>day streak</small></div><div class="mini-stat"><b style="color:var(--av-green)">${Math.round(Math.min(todayFocus() / goal, 9.99) * 100)}%</b><small>daily goal</small></div></div>
        <div class="a-panel"><b>🧠 Lock it in · 30 seconds</b><p class="a-sub" style="margin:6px 0 10px">Write one question you can now answer. Avenor will quiz you on it over the next few days.</p>
          <textarea class="text-input" id="recallQ" placeholder="Question (e.g. What does mitochondria do?)" aria-label="Question"></textarea>
          <textarea class="text-input" id="recallA" placeholder="Answer" aria-label="Answer" style="margin-top:8px"></textarea></div>
        ${task && !task.done ? `<label class="a-panel" style="display:flex;gap:10px;align-items:center;min-height:52px;cursor:pointer"><input type="checkbox" id="markDone" style="width:22px;height:22px"> Mark “${esc(task.title)}” as done</label>` : ""}
        <button class="primary-button full" data-act="break">☕ Take a ${focus.rounds > 0 && focus.rounds % 4 === 0 ? 15 : 5}-min break</button>
        <button class="secondary-button full" data-act="wrap-close">Done for now</button>`;
    } else if (focus.phase === "break") {
      o.innerHTML = `<h3 class="center" style="font-size:26px;margin-top:8px">Break time</h3>
        <div class="center" style="font-size:40px;font-weight:800" id="breakText">--:--</div>
        <div class="a-panel center" id="breathPanel"><b>🌬️ 60-second breathing reset</b><p class="a-sub" style="margin:6px 0 10px">Slow breathing helps you settle before the next round.</p>
          <button class="secondary-button full" data-act="breathe">▶ Start</button></div>
        <div class="a-panel">• Stand up and stretch<br>• Drink some water<br>• Look at something far away</div>
        <p class="a-sub center" style="font-size:13px">Skip the short videos — they make it much harder to start again.</p>
        <button class="primary-button full" data-act="next-round">▶ Start next round</button>
        <button class="secondary-button full" data-act="close-focus">End for now</button>`;
      updateTimer();
    }
  }

  // Fitts's Law in reverse: hold 1.5 s to quit (pointer or keyboard)
  function wireHold() {
    const b = $("#holdEnd"); if (!b) return;
    let timer = null;
    const start = (e) => { e.preventDefault(); b.classList.add("holding"); b.firstChild.textContent = "Keep holding to end…"; timer = setTimeout(endEarly, 1500); };
    const stop = () => { clearTimeout(timer); b.classList.remove("holding"); b.firstChild.textContent = "Hold to end early"; };
    b.addEventListener("pointerdown", start); ["pointerup", "pointerleave", "pointercancel"].forEach((ev) => b.addEventListener(ev, stop));
    b.addEventListener("keydown", (e) => { if ((e.key === "Enter" || e.key === " ") && !e.repeat) start(e); });
    b.addEventListener("keyup", (e) => { if (e.key === "Enter" || e.key === " ") stop(); });
    b.addEventListener("contextmenu", (e) => e.preventDefault());
  }
  function endEarly() { if (!focus) return; if (elapsed() < 60000) closeFocus(); else finishFocus(); }

  function wrapUp() {
    const q = $("#recallQ")?.value.trim(), a = $("#recallA")?.value.trim();
    const task = state.tasks.find((t) => t.id === focus.taskId);
    if (q && a) state.cards.push({ id: uid(), classId: task ? task.classId : null, front: q, back: a, box: 1, due: addDays(startOfDay(), 1).getTime() });
    if ($("#markDone")?.checked && task && !task.done) { task.done = true; task.doneAt = Date.now(); markActive(); }
    save();
  }

  function breathe() {
    const panel = $("#breathPanel"); if (!panel) return;
    panel.innerHTML = `<div class="breath" id="breath"><i></i></div><b id="breathWord" aria-live="polite">Breathe in</b>`;
    const words = ["Breathe in", "Hold", "Breathe out", "Hold"]; let step = 0;
    const el = $("#breath"); requestAnimationFrame(() => el.classList.add("big"));
    const iv = setInterval(() => {
      step++;
      if (!document.body.contains(el)) return clearInterval(iv);
      if (step >= 16) { clearInterval(iv); $("#breathWord").textContent = "Nice. Ready when you are."; el.classList.remove("big"); return; }
      const s = step % 4; $("#breathWord").textContent = words[s];
      if (s === 0) el.classList.add("big"); if (s === 2) el.classList.remove("big");
    }, 4000);
  }

  // ---------- Review set (overlay) ----------
  let rv = null;
  function startReview() { const q = dueCards().slice(0, 20); rv = { queue: q.map((c) => c.id), total: q.length, revealed: false, first: 0, missed: [] }; renderReviewOverlay(); }
  function renderReviewOverlay() {
    const o = overlayEl();
    const card = state.cards.find((c) => c.id === rv.queue[0]);
    if (!card) {
      o.innerHTML = `<div style="margin:auto 0" class="center"><div class="big-icon">🏆</div><h3 style="font-size:26px;margin:10px 0">Review complete!</h3>
        <p>You remembered ${rv.first} of ${rv.total} on the first try.</p>
        ${rv.missed.length ? `<p class="a-sub" style="margin-top:8px">The ${rv.missed.length} you missed will come back tomorrow. Forgetting and re-learning is how memories get stronger.</p>` : ""}</div>
        <div class="stat-row two"><div class="mini-stat"><b style="color:var(--av-violet)">${state.reviewLog[iso(new Date())] || 0}</b><small>reviews today</small></div><div class="mini-stat"><b style="color:var(--av-orange)">${streak()}</b><small>day streak</small></div></div>
        <button class="primary-button full" data-act="review-close">Done</button>`;
      return;
    }
    const done = rv.total - rv.queue.length, c = cls(card.classId);
    o.innerHTML = `<div style="display:flex;gap:10px;align-items:center"><button class="del" data-act="review-close" aria-label="Close review">✕</button><div class="bar"><i style="width:${(done / Math.max(rv.total, 1)) * 100}%"></i></div><span class="a-sub">${done}/${rv.total}</span></div>
      <div class="flash" data-act="reveal" role="button" tabindex="0" aria-label="Card. Tap to show answer">${c ? `<div>${chip(c.name, clsColor(card.classId))}</div>` : ""}<h3>${esc(card.front)}</h3>${rv.revealed ? `<p>${esc(card.back)}</p>` : ""}</div>
      ${rv.revealed ? `<p class="a-sub center">Did you remember it?</p><div class="two-buttons"><button class="secondary-button" data-act="grade" data-ok="0">↺ Not yet</button><button class="primary-button" data-act="grade" data-ok="1" style="background:var(--av-green)">✓ Got it</button></div>`
        : `<p class="a-sub center" style="font-size:13px">Answer in your head first — the effort is what makes it stick.</p><button class="primary-button full" data-act="reveal">Show answer</button>`}`;
  }
  function grade(ok) {
    const id = rv.queue.shift(), card = state.cards.find((c) => c.id === id), relearning = rv.missed.includes(id);
    review(card, ok, relearning);
    if (ok) { if (!relearning) rv.first++; } else { rv.missed.push(id); rv.queue.push(id); }
    rv.revealed = false; save(); renderReviewOverlay();
  }

  // ---------- Render + events ----------
  function render() {
    const screen = $("#appScreen"); if (!screen) return;
    screen.innerHTML = ui.tab === "today" ? renderToday() : ui.tab === "plan" ? renderPlan() : ui.tab === "focus" ? renderFocus() : renderReview();
    document.querySelectorAll(".tabbar button").forEach((b) => b.setAttribute("aria-selected", String(b.dataset.tab === ui.tab)));
    const badge = $("#reviewBadge"), due = dueCards().length;
    badge.hidden = due === 0; badge.textContent = due;
    wireQuick();
  }

  function wireQuick() {
    const input = $("#quickInput"), chips = $("#quickChips"); if (!input) return;
    input.addEventListener("input", () => {
      const p = parseQuick(input.value);
      chips.innerHTML = input.value.trim() ? [chip(p.title || "Add a title", "#f5f5f5"), p.due ? chip("📅 " + dueLabel(p.due), "#8b9cff") : "",
        p.minutes ? chip("⏱ " + p.minutes + " min", "#73cdfa") : "", p.priority ? chip("⚑ " + PRIORITY[p.priority].label, p.priority === "high" ? "#ff7373" : p.priority === "low" ? "#65d6a0" : "#ffb86b") : "",
        p.classId ? chip(cls(p.classId).name, clsColor(p.classId)) : ""].join("") : "";
    });
  }

  function toggleTask(id) {
    const t = state.tasks.find((x) => x.id === id); if (!t) return;
    t.done = !t.done; t.doneAt = t.done ? Date.now() : null; if (t.done) markActive();
    if (navigator.vibrate && t.done) { try { navigator.vibrate(15); } catch { /* ignore */ } }
    save(); render();
  }

  document.addEventListener("DOMContentLoaded", () => {
    const app = $("#app"); if (!app) return;
    render();

    app.addEventListener("click", (e) => {
      const el = e.target.closest("[data-act], [data-tab]"); if (!el || !app.contains(el)) return;
      const act = el.dataset.act, id = el.dataset.id;
      if (!act && el.dataset.tab) { ui.tab = el.dataset.tab; return render(); }
      switch (act) {
        case "tab": ui.tab = el.dataset.tab; return render();
        case "toggle": return toggleTask(id);
        case "skip": ui.skipped.push(id); if (ranked().every((t) => ui.skipped.includes(t.id))) ui.skipped = []; return render();
        case "start": { const t = state.tasks.find((x) => x.id === id); return startFocus(id, t.title, state.profile.session); }
        case "start5": { const t = state.tasks.find((x) => x.id === id); return startFocus(id, t.title, 5); }
        case "start-free": return startFocus(null, "Free study", state.profile.session);
        case "focus-task": { const t = state.tasks.find((x) => x.id === id); return startFocus(id, t.title, state.profile.session); }
        case "delete": { const i = state.tasks.findIndex((x) => x.id === id); ui.deleted = { task: state.tasks[i], index: i }; state.tasks.splice(i, 1); save(); render();
          clearTimeout(ui.undoTimer); ui.undoTimer = setTimeout(() => { ui.deleted = null; render(); }, 5000); return; }
        case "undo": if (ui.deleted) { state.tasks.splice(ui.deleted.index, 0, ui.deleted.task); ui.deleted = null; save(); } return render();
        case "seg": ui.planSeg = el.dataset.seg; return render();
        case "day": ui.selectedDay = el.dataset.day; return render();
        case "pick": ui.focusPick = id; ui.custom = ""; return render();
        case "minutes": ui.focusMinutes = +el.dataset.min; return render();
        case "focus5": case "focus-go": {
          const custom = ($("#customFocus")?.value || "").trim(), t = state.tasks.find((x) => x.id === ui.focusPick);
          return startFocus(custom ? null : ui.focusPick, custom || (t ? t.title : "Free study"), act === "focus5" ? 5 : (ui.focusMinutes || state.profile.session));
        }
        case "pause": if (focus.phase === "focusing") { focus.banked = elapsed(); focus.segStart = null; focus.phase = "paused"; } else { focus.segStart = Date.now(); focus.phase = "focusing"; } return renderOverlay();
        case "break": wrapUp(); focus.phase = "break"; focus.breakEnds = Date.now() + (focus.rounds > 0 && focus.rounds % 4 === 0 ? 15 : 5) * 60000; return renderOverlay();
        case "wrap-close": wrapUp(); return closeFocus();
        case "close-focus": return closeFocus();
        case "next-round": { const planned = focus.planned / 60000; return startFocus(focus.taskId, focus.label, planned <= 5 ? state.profile.session : planned); }
        case "breathe": return breathe();
        case "review-go": return startReview();
        case "reveal": rv.revealed = true; return renderReviewOverlay();
        case "grade": return grade(el.dataset.ok === "1");
        case "review-close": rv = null; $("#overlay")?.remove(); return render();
      }
    });

    app.addEventListener("keydown", (e) => { if (e.target.classList?.contains("flash") && (e.key === "Enter" || e.key === " ")) { e.preventDefault(); rv.revealed = true; renderReviewOverlay(); } });

    app.addEventListener("input", (e) => { if (e.target.id === "customFocus") ui.custom = e.target.value; });

    app.addEventListener("submit", (e) => {
      e.preventDefault();
      if (e.target.dataset.form === "quick") {
        const input = $("#quickInput"), p = parseQuick(input.value);
        if (!p.title.trim()) return;
        state.tasks.unshift({ id: uid(), title: p.title.trim(), classId: p.classId, due: p.due, priority: p.priority || "medium", minutes: p.minutes || state.profile.session, done: false, created: Date.now(), focused: 0 });
        save(); render(); $("#quickInput")?.focus();
      } else if (e.target.dataset.form === "class") {
        const name = $("#classInput").value.trim();
        if (name && !state.classes.some((c) => c.name.toLowerCase() === name.toLowerCase())) { state.classes.push({ id: uid(), name, color: state.classes.length }); save(); }
        render();
      }
    });
  });

  // Exposed for the automated checks in tests.html
  window.AvenorEngine = { parseQuick, parseDate, duration, score, streak, review, sessionsFor, reset: () => { state = seed(); save(); render(); } };
})();
