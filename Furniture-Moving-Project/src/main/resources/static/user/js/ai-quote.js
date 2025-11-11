/* ===== AI Quote – Chat + Cart + Slot-filling + OSM/OSRM (NO KEY) =====
 * 1) Ảnh: gọi Gemini → nhận diện hạng mục → đổ bảng phải
 * 2) Địa chỉ free-form: Geocode đa nguồn (Nominatim ➜ Photon) → xác thực (nếu fail báo lại)
 * 3) Tính khoảng cách DRIVING (OSRM public) → phí ship + TỔNG CỘNG
 * 4) FE-only, không cần Google Maps / không cần key
 */

/* ========= DOM hooks ========= */
const chatBody = document.querySelector(".chat-body");
const messageInput = document.querySelector(".message-input");
const sendMessage = document.querySelector("#send-message");
const fileInput = document.querySelector("#file-input");
const fileUploadBtn = document.querySelector("#file-upload");
const fileUploadWrapper = document.querySelector(".file-upload-wrapper");
const fileCancelButton = fileUploadWrapper ? fileUploadWrapper.querySelector("#file-cancel") : null;
if (fileUploadBtn && fileInput) fileUploadBtn.addEventListener("click", () => fileInput.click());

/* ========= OSM stack: Geocode đa nguồn + OSRM (no key) =========
 * Public endpoints có rate limit (dev/demo). Prod: tự host OSRM hoặc dùng dịch vụ có SLA.
 */

/* --- fetch with timeout (cho geocode) --- */
function fetchWithTimeout(url, opts = {}, ms = 6000) {
  return new Promise((resolve, reject) => {
    const ctrl = new AbortController();
    const id = setTimeout(() => { ctrl.abort(); reject(new Error("Geocode timeout")); }, ms);
    fetch(url, { ...opts, signal: ctrl.signal })
      .then(r => { clearTimeout(id); resolve(r); })
      .catch(err => { clearTimeout(id); reject(err); });
  });
}

/* Chuẩn hoá kết quả geocode về { ok, formatted, lat, lng, provider } */
function normPlace(obj, provider) {
  if (!obj) return { ok:false };
  if (provider === "nominatim") {
    const lat = parseFloat(obj.lat), lng = parseFloat(obj.lon);
    if (Number.isNaN(lat) || Number.isNaN(lng)) return { ok:false };
    return { ok:true, formatted: obj.display_name || "", lat, lng, provider };
  }
  if (provider === "photon") {
    const g = obj.geometry, p = obj.properties || {};
    const coords = Array.isArray(g?.coordinates) ? g.coordinates : null; // [lng, lat]
    if (!coords || coords.length < 2) return { ok:false };
    const lng = Number(coords[0]), lat = Number(coords[1]);
    if (Number.isNaN(lat) || Number.isNaN(lng)) return { ok:false };
    const composed =
      [p.name, p.street, p.city, p.state, p.country].filter(Boolean).join(", ");
    return { ok:true, formatted: composed || p.formatted || p.label || "", lat, lng, provider };
  }
  return { ok:false };
}

/* Gọi Nominatim (OSM) */
async function geocodeNominatim(q) {
  const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&addressdetails=1&accept-language=vi&q=${encodeURIComponent(q)}`;
  try {
    const r = await fetchWithTimeout(url, {}, 6000);
    if (!r.ok) throw new Error(`Nominatim HTTP ${r.status}`);
    const arr = await r.json();
    if (!Array.isArray(arr) || arr.length === 0) return { ok:false };
    return normPlace(arr[0], "nominatim");
  } catch (e) {
    console.warn("Nominatim fail:", e?.message || e);
    return { ok:false, error:e };
  }
}

/* Gọi Photon (Komoot) — CORS tốt, không cần key */
async function geocodePhoton(q) {
  const url = `https://photon.komoot.io/api/?q=${encodeURIComponent(q)}&lang=vi&limit=1`;
  try {
    const r = await fetchWithTimeout(url, {}, 6000);
    if (!r.ok) throw new Error(`Photon HTTP ${r.status}`);
    const data = await r.json();
    if (!data || !Array.isArray(data.features) || data.features.length === 0) return { ok:false };
    return normPlace(data.features[0], "photon");
  } catch (e) {
    console.warn("Photon fail:", e?.message || e);
    return { ok:false, error:e };
  }
}

/* Public API: geocodeAddress (thử Nominatim ➜ Photon, retry 2 vòng) */
async function geocodeAddress(query) {
  const q = String(query || "").trim();
  if (q.length < 3) return { ok:false };
  for (let attempt = 1; attempt <= 2; attempt++) {
    const n = await geocodeNominatim(q);
    if (n.ok) return n;
    const p = await geocodePhoton(q);
    if (p.ok) return p;
  }
  return { ok:false };
}

/* Tính khoảng cách lái xe bằng OSRM → { ok, km, durationText, routeText } */
async function calcDistance(orig, dest) {
  if (!orig || !dest) return { ok: false };
  const toLngLat = (p)=>`${Number(p.lng)},${Number(p.lat)}`; // OSRM cần lon,lat
  const url = `https://router.project-osrm.org/route/v1/driving/${toLngLat(orig)};${toLngLat(dest)}?overview=false`;
  try {
    const r = await fetch(url);
    if (!r.ok) return { ok: false };
    const data = await r.json();
    const route = data?.routes?.[0];
    if (!route) return { ok: false };
    const km = (route.distance || 0) / 1000;
    const seconds = route.duration || 0;
    const durationText = humanizeDuration(seconds);
    const routeText = `${km.toFixed(2)} km`;
    return { ok: true, km, durationText, routeText };
  } catch {
    return { ok: false };
  }

  function humanizeDuration(sec){
    const m = Math.round(sec/60);
    if (m < 60) return `${m} phút`;
    const h = Math.floor(m/60), mm = m%60;
    return `${h} giờ ${mm ? mm+" phút" : ""}`.trim();
  }
}

/* ========= Gemini (dev: key ở FE; prod: dùng proxy) ========= */
// (Prod: KHÔNG để key ở FE; dùng proxy BE)
const API_KEY = "AIzaSyCQZaLPV5xXjs65vh2f8L6HlwHAn8ouSoc";
let MODEL = "gemini-2.0-flash";
const FALLBACK_MODEL = "gemini-2.0-flash-lite";
const buildApiUrl = () => `https://generativelanguage.googleapis.com/v1/models/${MODEL}:generateContent?key=${API_KEY}`;

/* ========= State ========= */
const chatHistory = [];
const userUploads = [];
const initialInputHeight = messageInput ? messageInput.scrollHeight : 0;

/* ========= Business guard ========= */
const DOMAIN_ONLY_MESSAGE =
  "Mình chỉ hỗ trợ nghiệp vụ vận chuyển (báo giá, thêm/xoá đồ, xác nhận và hỏi thông tin giao nhận). Bạn hãy mô tả đồ đạc hoặc cung cấp thông tin cần thiết nhé.";

/* ========= Slot-filling ========= */
const SLOT = {
  mode: "idle",
  step: 0,
  data: {
    name: null,
    phone: null,
    fromAddr: null, toAddr: null,          // raw input
    fromPlace: null, toPlace: null,        // geocoded {formatted,lat,lng}
    date: null, time: null, datetime: null, _dateObj: null,
    km: null, durationText: null, routeText: null
  }
};
const SLOT_STEPS = ["name", "phone", "fromAddr", "toAddr", "date", "time"];
function resetSlot() {
  SLOT.mode = "idle"; SLOT.step = 0;
  SLOT.data = { name:null, phone:null, fromAddr:null, toAddr:null, fromPlace:null, toPlace:null,
    date:null, time:null, datetime:null, _dateObj:null, km:null, durationText:null, routeText:null };
}
function nextMissingKey() { for (const k of SLOT_STEPS) if (!SLOT.data[k]) return k; return null; }
function askQuestionFor(key) {
  const Q = {
    name: "Cho mình xin <b>HỌ TÊN</b> người liên hệ?",
    phone: "Số điện thoại liên hệ là gì ạ? (vd: 0912345678 hoặc +84 912345678)",
    fromAddr: "Địa chỉ <b>ĐI</b> (nơi <b>LẤY HÀNG</b>) ở đâu? Bạn cứ nhập tự do (vd: 'số 12 thôn 4 Hòa Lạc, Thạch Thất').",
    toAddr: "Địa chỉ <b>ĐẾN</b> (nơi <b>GIAO HÀNG</b>) ở đâu? Cứ nhập tự do, mình sẽ kiểm tra địa chỉ thật giúp bạn.",
    date: "Bạn muốn vận chuyển vào <b>NGÀY</b> nào? (vd: 12/11/2025). Lưu ý: từ hôm nay đến 1 tháng tới.",
    time: 'Bạn muốn vận chuyển vào <b>GIỜ</b> nào? (vd: "9h kém 5", "5h chiều", "12 giờ rưỡi", "9:15",…).'
  };
  return Q[key] || "";
}
function extractInStep(key, text) {
  const t = (text || "").trim();
  if (key === "name") { const m = t.match(/(?:tên|họ\s*tên|em|anh|chị|tôi)\s*[:\-]?\s*(.+)$/i); return (m ? m[1] : t).replace(/\s{2,}/g, " ").slice(0, 80); }
  if (key === "phone") return t || null;
  if (key === "fromAddr" || key === "toAddr") return t || null;
  if (key === "date" || key === "time") return t || null;
  return null;
}

/* ====== Date/Time helpers ====== */
function parseDateOnlyFromText(text) {
  const t = (text || "").trim(); const m = t.match(/(\d{1,2})[\/\-](\d{1,2})[\/\-](\d{2,4})/);
  if (!m) return null; let [, dd, mm, yy] = m; let day = +dd; let month = +mm - 1; let year = +yy; if (year < 100) year += 2000;
  const dt = new Date(year, month, day, 0, 0, 0, 0); if (isNaN(dt.getTime())) return null; return { dt };
}
function parseTimeFromText(text) {
  const raw = (text || "").toLowerCase().trim(); if (!raw) return null; let t = raw.replace(/giờ/g, "h").replace(/\s+/g, " ");
  let m = t.match(/(\d{1,2})\s*h\s*kém\s*(\d{1,2})/i); if (m) { let hour = +m[1]; const minus = +m[2]; let minute = (60 - minus)%60; hour = (hour - 1 + 24)%24; return { hour, minute }; }
  m = t.match(/(\d{1,2})[:\.](\d{1,2})/); if (m) { const hour = +m[1], minute = +m[2]; if (hour>23||minute>59) return null; return { hour, minute }; }
  m = t.match(/(\d{1,2})\s*h\s*rưỡi/); if (m) { const hour = +m[1]; if (hour>23) return null; return { hour, minute: 30 }; }
  m = t.match(/(\d{1,2})\s*h(?:\s*(sáng|trưa|chiều|tối|pm|am))?/i);
  if (m) { let hour = +m[1]; const desc = (m[2]||"").toLowerCase();
    if (desc === "chiều" || desc === "tối" || desc === "pm") { if (hour < 12) hour += 12; }
    else if (desc === "sáng" || desc === "am") { if (hour === 12) hour = 0; }
    else if (desc === "trưa") { if (hour < 10) hour += 12; }
    return { hour: hour%24, minute: 0 };
  }
  m = t.match(/(\d{1,2})\s*h\s*(\d{1,2})/); if (m) { const hour = +m[1], minute = +m[2]; if (hour>23||minute>59) return null; return { hour, minute }; }
  m = t.match(/^(\d{1,2})$/); if (m) { const hour = +m[1]; if (hour>23) return null; return { hour, minute: 0 }; }
  return null;
}
function formatDateOnlyVN(dt){ const dd=String(dt.getDate()).padStart(2,"0"); const mm=String(dt.getMonth()+1).padStart(2,"0"); const yyyy=dt.getFullYear(); return `${dd}/${mm}/${yyyy}`; }
function formatTimeVN(h,m){ return `${String(h).padStart(2,"0")}:${String(m).padStart(2,"0")}`; }
function formatDateTimeVN(dt){ const dd=String(dt.getDate()).padStart(2,"0"); const mm=String(dt.getMonth()+1).padStart(2,"0"); const yyyy=dt.getFullYear(); const hh=String(dt.getHours()).padStart(2,"0"); const mi=String(dt.getMinutes()).padStart(2,"0"); return `${dd}/${mm}/${yyyy} ${hh}:${mi}`; }

/* ====== Phone normalize ====== */
function normalizePhone(phoneText) {
  const digits = String(phoneText||"").replace(/[^\d]/g,""); if (!digits) return null;
  if (digits.length < 9 || digits.length > 11) return null;
  let normalized = digits;
  if (normalized.startsWith("84") && normalized.length === 11) normalized = "0" + normalized.slice(2);
  if (!normalized.startsWith("0")) normalized = "0" + normalized;
  if (normalized.length < 9 || normalized.length > 11) return null;
  return normalized;
}

/* ====== UI helpers ====== */
function createMessageElement(content, ...classes) {
  const div = document.createElement("div"); div.classList.add("message", ...classes); div.innerHTML = content; return div;
}
function renderSlotReply(msgHtml) {
  if (!chatBody) return;
  const bot = `
    <svg class="bot-avatar" xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 1024 1024">
      <path d="M738.3 287.6H285.7c-59 0-106.8 47.8-106.8 106.8v303.1c0 59 47.8 106.8 106.8 106.8h81.5v111.1c0 .7.8 1.1 1.4.7l166.9-110.6 41.8-.8h117.4l43.6-.4c59 0 106.8-47.8 106.8-106.8V394.5c0-59-47.8-106.9-106.8-106.9z"/>
    </svg>
    <div class="message-text">${msgHtml}</div>`;
  const incoming = createMessageElement(bot, "bot-message");
  chatBody.appendChild(incoming);
  chatBody.scrollTo({ top: chatBody.scrollHeight, behavior: "smooth" });
}
function clearChatCTA(){ document.querySelectorAll(".chat-cta").forEach(el=>el.remove()); }
function showConfirmCTA() {
  if (!chatBody) return;
  clearChatCTA();
  const cta = document.createElement("div");
  cta.className = "chat-cta mt-2";
  cta.innerHTML = `
    <button type="button" class="btn btn-sm btn-success btn-chat-confirm">
      <i class="fas fa-check mr-1"></i> Xác nhận vận chuyển
    </button>
    <small class="d-block text-muted mt-1">
      (Bấm xác nhận để chuyển qua bước hỏi thông tin giao nhận: Họ tên, SĐT, địa chỉ đi/đến, ngày và giờ vận chuyển.)
    </small>`;
  const wrap = createMessageElement(`
    <svg class="bot-avatar" xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 1024 1024">
      <path d="M738.3 287.6H285.7c-59 0-106.8 47.8-106.8 106.8v303.1c0 59 47.8 106.8 106.8 106.8h81.5v111.1c0 .7.8 1.1 1.4.7l166.9-110.6 41.8-.8h117.4l43.6-.4c59 0 106.8-47.8 106.8-106.8V394.5c0-59-47.8-106.9-106.8-106.9z"/>
    </svg>
    <div class="message-text">Đây có phải là những gì bạn cần vận chuyển không? Nếu đúng, bấm <b>Xác nhận vận chuyển</b> để mình hỏi thông tin giao nhận.</div>`,
    "bot-message"
  );
  wrap.appendChild(cta);
  chatBody.appendChild(wrap);
  chatBody.scrollTo({ top: chatBody.scrollHeight, behavior: "smooth" });
}

/* ========= Products & Pricing ========= */
const PRODUCTS_URL = "/user/data/products.json";
const TARGET_COUNT = 10000;
const BULKY_KEYWORDS = ["sofa","ghế sofa","sofa góc","giường","giường đơn","giường đôi","giường tầng","tủ quần áo","tủ 3 cánh","tủ 4 cánh","tủ 5 cánh","tủ bếp lớn","tủ lạnh side-by-side","tủ lạnh 4 cánh","máy chạy bộ","máy tập đa năng","máy photocopy","két sắt lớn","máy giặt","máy sấy","máy rửa chén","máy lạnh đứng","tivi 55 inch","tivi 65 inch","tivi 75 inch","bàn họp","kệ kho sắt","bàn bida","bàn bida mini","tủ thờ","bàn thờ","cây cảnh lớn"];
const BASE_NAMES = ["nồi cơm điện","bàn làm việc","bàn học","bàn ăn","bàn trà","bàn gấp","ghế xoay","kệ sách","tủ quần áo","tivi 55 inch","máy giặt","máy sấy","két sắt nhỏ","thùng carton nhỏ","thùng carton lớn","xe đẩy em bé","đàn guitar","máy hút bụi","loa kéo","máy pha cà phê","lò vi sóng","tủ bếp lớn"];
const SIZE_TAGS = ["mini","nhỏ","vừa","lớn","cao cấp"]; const MATERIALS = ["gỗ","gỗ sồi","gỗ thông","gỗ công nghiệp","nhựa","inox","thép","hợp kim","vải","da","da PU"]; const COLORS = ["trắng","đen","xám","nâu","be","xanh","đỏ"];
const rand = (arr) => arr[Math.floor(Math.random()*arr.length)];
const randomInt = (min,max,step=5000)=>{const a=Math.ceil(min/step), b=Math.floor(max/step); return (Math.floor(Math.random()*(b-a+1))+a)*step;};
const cheapPrice = () => randomInt(15_000, 150_000, 5000);
const bulkyPrice = () => randomInt(250_000, 900_000, 5000);
const isBulky = (name="") => BULKY_KEYWORDS.some(k => (name||"").toLowerCase().includes(k.toLowerCase()));
const decorateName = (base) => { const bits=[]; if(Math.random()<0.6) bits.push(rand(SIZE_TAGS)); if(Math.random()<0.4) bits.push(rand(MATERIALS)); if(Math.random()<0.4) bits.push(rand(COLORS)); return bits.length?`${base} (${bits.join(", ")})`:base; };

/* ========= Config ========= */
const CFG_KEY = "ai_quote_cfg_v4_osm_multisource";
function defaultSettings(){ return {
  currency:"VND",
  pricePerKm: 10000,     // phí ship theo km
  minFare: 50000,        // tối thiểu
  basePrompt:
`Bạn là “Trợ lý Báo giá Vận chuyển”.
Chỉ trả lời trong phạm vi vận chuyển. Không trả JSON/code.

Khi có ảnh, hãy:
- Đếm số lượng từng hạng mục. Ưu tiên nhận diện đồ nội thất: tủ, giường, bàn, ghế, sofa, thùng carton, thiết bị điện máy...
- Nếu không chắc tên, ghi tên mô tả (vd: "thùng carton nhỏ", "tủ gỗ 3 cánh").
- In theo dòng:
  <Tên>: <SL> cái — đơn giá <X> VND — tạm tính <Y> VND
- Cuối cùng in "Tổng tạm tính: <Số tiền> VND"
- KHÔNG dùng Markdown đậm hoặc bullet đặc biệt. KHÔNG in JSON/code.
Sau đó hỏi người dùng xác nhận để tính phí vận chuyển.`,
  items:[]
};}
function loadSettings(){ try { return JSON.parse(localStorage.getItem(CFG_KEY)) || defaultSettings(); } catch { return defaultSettings(); } }
function saveSettings(s){ try { localStorage.setItem(CFG_KEY, JSON.stringify(s)); } catch {} }
const currency = () => loadSettings().currency || "VND";
const fmtMoney = (n) => Number(n || 0).toLocaleString() + " " + currency();

/* ========= Helpers ========= */
const delay = (ms) => new Promise(r => setTimeout(r, ms));

/* ========= Robust fetch/backoff (Gemini) ========= */
async function fetchWithBackoff(url, options, { maxRetries = 3, baseDelay = 700 } = {}) {
  let attempt = 0, switched = false;
  while (true) {
    const res = await fetch(url, options);
    const text = await res.text();
    let data; try { data = JSON.parse(text); } catch { data = { raw: text }; }
    if (res.ok) return data;

    const status = res.status;
    const msg = data?.error?.message || `HTTP ${status}`;
    const overload = status === 429 || status === 503 || /overloaded/i.test(msg);
    const notFoundOrUnsupported = status === 404 || /not found|not supported/i.test(msg);

    if (url.includes("generativelanguage") && (notFoundOrUnsupported || overload) && !switched && MODEL !== FALLBACK_MODEL) {
      MODEL = FALLBACK_MODEL; switched = true; await delay(250); continue;
    }
    if (overload && attempt < maxRetries) {
      attempt++; const wait = baseDelay * Math.pow(2, attempt - 1) + Math.floor(Math.random()*250);
      await delay(wait); continue;
    }
    const err = new Error(msg); err.status = status; err.payload = data; throw err;
  }
}

/* ========= Prompt build ========= */
function buildPromptText(userText) {
  const s = loadSettings();
  const priceLines = (s.items || []).slice(0, 200).map(it => `- ${it.name}: ${Number(it.price).toLocaleString()} ${s.currency}`).join("\n");
  return s.basePrompt + "\n\nBảng đơn giá tham khảo (một phần):\n" + priceLines +
    "\n\nHãy liệt kê hạng mục theo định dạng đã yêu cầu.\n\n" + (userText || "");
}

/* ========= Upload preview ========= */
function renderUploadPreview() {
  if (!fileUploadWrapper) return;
  const grid = fileUploadWrapper.querySelector(".thumb-grid"); if (!grid) return;
  grid.innerHTML = "";
  userUploads.forEach((u, idx) => {
    const item = document.createElement("div");
    item.className = "thumb";
    item.innerHTML = `<img src="${u.previewUrl}" alt="upload ${idx + 1}">
      <button type="button" class="thumb-remove" data-idx="${idx}" title="Xoá ảnh">&times;</button>`;
    grid.appendChild(item);
  });
  fileUploadWrapper.classList.toggle("file-uploaded", userUploads.length > 0);
}
if (fileInput) {
  fileInput.multiple = true;
  fileInput.addEventListener("change", () => {
    const files = Array.from(fileInput.files || []); fileInput.value = "";
    if (!files.length) return;
    if (userUploads.length + files.length > 10) { alert("Bạn chỉ có thể tải lên tối đa 10 ảnh."); return; }
    const images = files.filter(f => /^image\//i.test(f.type));
    if (images.length !== files.length) alert("Một số tệp không phải hình ảnh nên đã bị bỏ qua.");
    Promise.all(images.map(file => new Promise(resolve => {
      const reader = new FileReader();
      reader.onload = e => { const previewUrl = e.target.result; const base64 = previewUrl.split(",")[1];
        userUploads.push({ data: base64, mime_type: file.type, previewUrl }); resolve(); };
      reader.readAsDataURL(file);
    }))).then(renderUploadPreview);
  });
}
if (fileUploadWrapper) {
  fileUploadWrapper.addEventListener("click", (e) => {
    const btn = e.target.closest(".thumb-remove"); if (!btn) return;
    const idx = +btn.getAttribute("data-idx");
    if (idx >= 0) { userUploads.splice(idx, 1); renderUploadPreview(); }
  });
}
if (fileCancelButton) fileCancelButton.addEventListener("click", () => { userUploads.splice(0, userUploads.length); renderUploadPreview(); });

/* ========= Parse AI text -> [{name, qty}] ========= */
function parseItemsFromAiText(text) {
  if (!text) return [];
  const lines = text.split(/\r?\n/); const results = [];
  const unitWords = "(?:cái|bộ|chiếc|thùng carton|thùng|kg|m3|m²|m|bức|tấm|cây|cuộn|ghế|bàn|thanh|kiện|bao|túi|con)";
  const qtyRegex = new RegExp(`(\\d+[\\d.,]*)\\s*${unitWords}\\b`, "i");
  for (let raw of lines) {
    let line = (raw || "").trim().replace(/^[-•*]\s*/, ""); if (!line) continue;
    const lower = line.toLowerCase();
    if (lower.startsWith("chào ") || lower.includes("đây có phải") || lower.includes("hãy gửi") || lower.includes("tổng tạm tính") || lower.startsWith("lưu ý") || lower.startsWith("ghi chú") || /[\?؟]+$/.test(lower)) continue;
    const mQty = line.match(qtyRegex); if (!mQty) continue;
    const qty = parseInt(mQty[1].replace(/[^\d]/g, ""), 10) || 1;
    let name = line.split(/[—:]/)[0].replace(/\((.*?)\)/g, "").replace(/\s+/g, " ").trim();
    if (!name || /^((bạn|mình|tôi|anh|chị|bên mình|vui lòng|xin vui lòng)\b)/i.test(name)) continue;
    if (name.length < 2) continue;
    results.push({ name, qty });
  }
  return results;
}

/* ========= Cart / pricing (right table) ========= */
(function () {
  const itemsTbody = document.querySelector("#items-tbody");
  const sumQtyEl = document.querySelector("#sum-qty");
  const sumAmountEl = document.querySelector("#sum-amount");
  if (!itemsTbody || !sumQtyEl || !sumAmountEl) return;

  let items = []; // {id,name,price,qty}
  let priceIndexExact = null; // Map<lowerName, price>
  let priceIndexList = null; // Array<[lowerName, price]>

  function buildPriceIndex() {
    const list = loadSettings().items || [];
    priceIndexExact = new Map(); priceIndexList = [];
    for (const it of list) {
      const nm = (it.name || "").toLowerCase().trim();
      const price = Number(it.price) || 0;
      if (!nm) continue;
      priceIndexExact.set(nm, price);
      priceIndexList.push([nm, price]);
    }
    priceIndexList.sort((a, b) => b[0].length - a[0].length);
  }
  function lookupPrice(name) {
    if (!priceIndexExact) buildPriceIndex();
    const n = (name || "").toLowerCase().trim(); if (!n) return 0;
    if (priceIndexExact.has(n)) return priceIndexExact.get(n);
    const escapeRegExp = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    for (const [nm, price] of priceIndexList) { const rx = new RegExp(`\\b${escapeRegExp(nm)}\\b`, "i"); if (rx.test(n)) return Number(price) || 0; }
    return 0;
  }
  const fmt = (n) => Number(n || 0).toLocaleString() + " " + currency();

  function render() {
    itemsTbody.innerHTML = "";
    if (!items.length) {
      const tr = document.createElement("tr"); tr.className = "empty-row";
      tr.innerHTML = `<td colspan="4" class="text-muted text-center py-3">Chưa có sản phẩm. Gửi ảnh để AI nhận diện hoặc thêm thủ công.</td>`;
      itemsTbody.appendChild(tr);
    } else {
      for (const it of items) {
        const hasPrice = Number(it.price) > 0;
        const priceHtml = hasPrice ? `${fmt(it.price)}` : `<span class="text-muted font-italic">báo giá sau</span>`;
        const subHtml = hasPrice ? `Tạm tính: ${fmt(it.price * it.qty)}` : `<span class="text-muted">Tạm tính: —</span>`;
        const tr = document.createElement("tr"); tr.dataset.id = it.id;
        tr.innerHTML = `
          <td>${it.name}</td>
          <td class="text-center">
            <div class="qty-group">
              <button class="btn-minus" type="button" aria-label="Giảm">−</button>
              <input class="qty-input" value="${it.qty}" inputmode="numeric">
              <button class="btn-plus" type="button" aria-label="Tăng">+</button>
            </div>
          </td>
          <td class="text-right"><div>${priceHtml}</div><small class="text-muted">${subHtml}</small></td>
          <td class="text-right"><button class="btn btn-sm btn-outline-danger btn-del" title="Xoá"><i class="fas fa-trash"></i></button></td>`;
        itemsTbody.appendChild(tr);
      }
    }
    const totalQty = items.reduce((s,i)=>s+Number(i.qty||0),0);
    const totalAmount = items.filter(i => Number(i.price) > 0).reduce((s,i)=>s+Number(i.qty||0)*Number(i.price||0),0);
    sumQtyEl.textContent = String(totalQty);
    sumAmountEl.textContent = Number(totalAmount).toLocaleString() + " " + currency();
  }
  function setItems(list) {
    items = (list || []).map(it => {
      const name = String(it.name || "").trim();
      return { id: "i_" + Math.random().toString(36).slice(2, 9), name, price: lookupPrice(name),
        qty: Math.max(1, Number(String(it.qty || 1).replace(/[^\d]/g, "")) || 1) };
    }).filter(it => it.name);
    render();
  }
  function findIndexByName(name) {
    const key = String(name || "").toLowerCase().trim();
    return items.findIndex(x => x.name.toLowerCase().trim() === key);
  }
  function removeItemByName(name) { const idx = findIndexByName(name); if (idx >= 0) { items.splice(idx, 1); render(); return true; } return false; }
  function decrementItemByName(name, qty = 1) {
    const idx = findIndexByName(name); if (idx < 0) return false;
    items[idx].qty = Math.max(0, Number(items[idx].qty || 0) - Number(qty || 1));
    if (items[idx].qty === 0) items.splice(idx, 1);
    render(); return true;
  }
  function upsertItem(name, qty = 1) {
    const cleanName = String(name || "").trim(); const cleanQty = Math.max(1, Number(qty) || 1); if (!cleanName) return false;
    const idx = findIndexByName(cleanName);
    if (idx >= 0) items[idx].qty = Number(items[idx].qty || 0) + cleanQty;
    else items.push({ id: "i_" + Math.random().toString(36).slice(2, 9), name: cleanName, price: lookupPrice(cleanName), qty: cleanQty });
    render(); return true;
  }

  // expose public API
  window.AIQUOTE = window.AIQUOTE || {};
  window.AIQUOTE.setItems = setItems;
  window.AIQUOTE.upsertItem = upsertItem;
  window.AIQUOTE.removeItemByName = removeItemByName;
  window.AIQUOTE.decrementItemByName = decrementItemByName;
  window.AIQUOTE.getTotals = () => {
    const qty = items.reduce((s,i)=>s+Number(i.qty||0),0);
    const amount = items.filter(i=>Number(i.price)>0).reduce((s,i)=>s+Number(i.qty||0)*Number(i.price||0),0);
    return { qty, amount };
  };

  // events
  itemsTbody.addEventListener("click",(e)=>{
    const tr = e.target.closest("tr"); if (!tr) return;
    const id = tr.dataset.id; const it = id && items.find(x => x.id === id); if (!it) return;
    if (e.target.closest(".btn-plus"))  { it.qty = Number(it.qty) + 1; render(); }
    if (e.target.closest(".btn-minus")) { it.qty = Math.max(1, Number(it.qty) - 1); render(); }
    if (e.target.closest(".btn-del")) { const ok = confirm(`Bạn có muốn xoá "${it.name}" khỏi danh sách không?`); if (ok) { items = items.filter(x => x.id !== id); render(); } }
  });
  itemsTbody.addEventListener("input",(e)=>{
    if (!e.target.classList.contains("qty-input")) return;
    const tr = e.target.closest("tr"); const id = tr?.dataset?.id; const it = items.find(x => x.id === id); if (!it) return;
    const v = parseInt(e.target.value.replace(/[^\d]/g,"") || "1", 10);
    it.qty = Math.max(1, v); render();
  });

  render();
})();

/* ========= Parse lệnh thêm/xoá ========= */
const VI_NUM = {"một":1,"hai":2,"ba":3,"bốn":4,"tư":4,"năm":5,"lăm":5,"sáu":6,"bảy":7,"bẩy":7,"tám":8,"chín":9,"mười":10};
const toNum = (s) => { const n = parseInt(String(s||"").replace(/[^\d]/g,""),10); if (!isNaN(n)) return n; const w = String(s||"").toLowerCase().trim(); return VI_NUM[w] ?? NaN; };
const LEADING_GARBAGE_RX = /^(?:cho|giúp|giup|làm|lam|thêm giúp|thêm giùm|please|pls)\s+(?:tôi|em|anh|chị|minh|mình)\s*/i;
const TRAILING_GARBAGE_RX = /\s*(giúp với|giúp em|giúp anh|giúp chị|giùm|dùm|với|nhé|nha|ạ|ạ nhé|cảm ơn|thanks|thank you)\s*$/i;

function parseUserAddCommand(text) {
  const results = []; if (!text) return results;
  const t = text.trim();
  if (/(\d+)\s*\+\s*(\d+)/.test(t) || /[=≈]/.test(t)) return results;

  // "+2 ghế, +1 bàn"
  const rePlus = /(?:^|,\s*)\+\s*(\d+)\s+(.*?)(?=(?:\s*,|$))/gi; let mp;
  while ((mp = rePlus.exec(t))) {
    const qty = Math.max(1, parseInt(mp[1], 10));
    let name = (mp[2] || "").trim().replace(/[.]+$/,"").replace(LEADING_GARBAGE_RX,"").replace(TRAILING_GARBAGE_RX,"").trim();
    if (name) results.push({ name, qty });
  }
  if (results.length) return results;

  const verbRx = /\b(thêm vào|thêm|cộng|add)\b/i; if (!verbRx.test(t)) return results;
  const afterVerb = t.split(verbRx).pop();
  const chunks = afterVerb.split(/\s*(?:,|và)\s*/i).map(s=>s.trim()).filter(Boolean);
  const unitRx = "(?:cái|bộ|chiếc|ghế|bàn|thùng carton|thùng|kg|m3|m|tấm|bức|cuộn|kiện|bao|túi|con)?";

  for (let c of chunks) {
    c = c.replace(LEADING_GARBAGE_RX,"").trim();
    let m = c.match(new RegExp(`^(\\d+|một|hai|ba|bốn|tư|năm|lăm|sáu|bảy|bẩy|tám|chín|mười)\\s*${unitRx}\\s+(.+)$`,"i"));
    if (!m) { const m2 = c.match(new RegExp(`.*?(\\d+|một|hai|ba|bốn|tư|năm|lăm|sáu|bảy|bẩy|tám|chín|mười)\\s*${unitRx}\\s+(.+)$`,"i")); if (m2) m = m2; }
    if (m) {
      const qty = toNum(m[1]); let name = (m[2]||"").trim().replace(/[.]+$/,"").replace(TRAILING_GARBAGE_RX,"").trim();
      if (!isNaN(qty) && qty>0 && name) { results.push({ name, qty }); continue; }
    }
    if (!/^(?:cho|giúp|giup)\b/i.test(c)) {
      const name = c.replace(/[.]+$/,"").replace(TRAILING_GARBAGE_RX,"").trim();
      if (name && !/\d/.test(name)) results.push({ name, qty:1 });
    }
  }
  return results;
}
function parseUserRemoveCommand(text) {
  const results = []; if (!text) return results; const t = text.trim();
  const reMinus = /(?:^|,\s*)-\s*(\d+)\s+([^\,-]+?)(?=(?:\s*,|$))/g; let m;
  while ((m = reMinus.exec(t))) {
    const qty = Math.max(1, parseInt(m[1], 10));
    const name = m[2].trim().replace(/[.]+$/,"").replace(TRAILING_GARBAGE_RX,"").trim();
    if (name) results.push({ name, qty, mode: "decrement" });
  }
  const hasVerb = /\b(xoá|xóa|bớt|trừ|giảm|bỏ|delete|remove)\b/i.test(t);
  if (hasVerb) {
    t.split(/\b(?:và|,)\b/i).forEach(p=>{
      const mm = p.match(/\b(xoá|xóa|bớt|trừ|giảm|bỏ|delete|remove)\b\s*(\d+|một|hai|ba|bốn|tư|năm|lăm|sáu|bảy|bẩy|tám|chín|mười)?\s*(?:cái|bộ|chiếc|ghế|bàn|thùng carton|thùng|kg|m3|m|tấm|bức|cuộn|kiện|bao|túi|con)?\s+(.+)/i);
      if (mm) {
        const qty = mm[2] ? toNum(mm[2]) : null;
        const name = mm[3].trim().replace(/[.]+$/,"").replace(TRAILING_GARBAGE_RX,"").trim();
        if (name) results.push({ name, qty: qty ?? 0, mode: (qty ? "decrement" : "remove") });
      }
    });
    if (!results.length) {
      const mm = t.match(/\b(xoá|xóa|bỏ|delete|remove)\b\s+(.+)/i);
      if (mm) {
        const name = mm[2].trim().replace(/[.]+$/,"").replace(TRAILING_GARBAGE_RX,"").trim();
        if (name) results.push({ name, qty: 0, mode: "remove" });
      }
    }
  }
  return results;
}

/* ========= Intent ========= */
function detectShippingIntent(text) { if (!text) return false; const t = text.toLowerCase(); return /\b(chuyển|chuyen|vận chuyển|van chuyen|chuyển nhà|ship|gửi hàng|gui hang|gui do)\b/.test(t); }
function parseItemsFromShippingSentence(text) {
  const results = []; if (!text) return results;
  const unitRx = "(?:cái|bộ|chiếc|ghế|bàn|giường|tủ|thùng carton|thùng|kg|m3|m|tấm|bức|cuộn|kiện|bao|túi|con)?";
  const re = new RegExp(`(\\d+|một|hai|ba|bốn|tư|năm|lăm|sáu|bảy|bẩy|tám|chín|mười)\\s*${unitRx}\\s+(.+?)(?:\\s+(?:đi|tới|đến|ra|sang)\\b|$)`,"i");
  const m = text.match(re);
  if (m) { const qty = toNum(m[1]); let name = (m[2] || "").trim().replace(/[.]+$/, ""); if (!isNaN(qty) && qty > 0 && name) results.push({ name, qty }); }
  return results;
}
function isSmallTalkOrGreeting(text) {
  if (!text) return true;
  const t = text.toLowerCase().trim();
  if (t.length <= 5 && !/\d/.test(t)) return true;
  if (/^(chào|chao|hi|hello|alo|a lô|xin chào|chào bạn|chao ban)\b/.test(t)) return true;
  if (/^(mình muốn|minh muon|em muốn|anh muốn|chị muốn|toi muon|tôi muốn)\b/.test(t)) return true;
  if (/^(ok|oke|okay|uhm|ừ|uh|ờ)\b/.test(t)) return true;
  return false;
}

/* ========= AI call (nếu có ảnh) ========= */
async function generateBotResponse(incomingMessageDiv, userText, opts = {}) {
  const { allowAutofill = false } = opts;
  const messageElement = incomingMessageDiv.querySelector(".message-text");
  const parts = [{ text: buildPromptText(userText) }];
  userUploads.forEach(img => parts.push({ inline_data: { data: img.data, mime_type: img.mime_type } }));
  chatHistory.push({ role: "user", parts });

  const requestOptions = { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({
    contents: chatHistory.slice(-8),
    generationConfig: { temperature: 0.1, topK: 40, topP: 0.9, candidateCount: 1, maxOutputTokens: 1024 }
  }) };
  try {
    const data = await fetchWithBackoff(buildApiUrl(), requestOptions, { maxRetries: 3, baseDelay: 700 });
    const apiText = data?.candidates?.[0]?.content?.parts?.[0]?.text?.replace(/\*\*(.*?)\*\*/g, "$1").trim() || "Mình chưa đọc được nội dung, vui lòng thử lại.";
    if (messageElement) messageElement.innerText = apiText;
    chatHistory.push({ role: "model", parts: [{ text: apiText }] });
    const parsed = parseItemsFromAiText(apiText);
    if (allowAutofill && parsed.length && window.AIQUOTE?.setItems) window.AIQUOTE.setItems(parsed);
    if (allowAutofill) showConfirmCTA();
  } catch (error) {
    console.error(error);
    if (messageElement) { messageElement.innerText = error.message || "Có lỗi khi gọi AI."; messageElement.style.color = "#ff0000"; }
  } finally {
    userUploads.splice(0, userUploads.length); renderUploadPreview();
    incomingMessageDiv.classList.remove("thinking");
    if (chatBody) chatBody.scrollTo({ top: chatBody.scrollHeight, behavior: "smooth" });
  }
}

/* ========= Tổng hợp & in tổng cuối ========= */
async function ensureDistanceAndTotals() {
  const d = SLOT.data;
  if (!d.fromPlace || !d.toPlace) return;

  if (!d.km) {
    const dist = await calcDistance(d.fromPlace, d.toPlace);
    if (!dist.ok) {
      renderSlotReply("Mình chưa tính được quãng đường giữa 2 địa chỉ. Bạn kiểm tra lại địa chỉ giúp mình nhé.");
      return;
    }
    d.km = dist.km; d.durationText = dist.durationText; d.routeText = dist.routeText;
  }

  const { amount: itemsAmount } = (window.AIQUOTE?.getTotals?.() || { amount: 0 });
  const cfg = loadSettings();
  const shipFee = Math.max(cfg.minFare, Math.round(d.km * (cfg.pricePerKm || 0)));
  const grandTotal = itemsAmount + shipFee;

  renderSlotReply(`
    <b>Khoảng cách & chi phí vận chuyển</b><br>
    • Từ: ${d.fromPlace.formatted}<br>
    • Đến: ${d.toPlace.formatted}<br>
    • Quãng đường ước tính: <b>${d.km.toFixed(2)} km</b> (${d.durationText || "—"})<br>
    • Phí vận chuyển: <b>${fmtMoney(shipFee)}</b> (đơn giá ${fmtMoney(cfg.pricePerKm)}/km, tối thiểu ${fmtMoney(cfg.minFare)})<br>
    • Giá hàng hoá (tạm tính từ danh sách): <b>${fmtMoney(itemsAmount)}</b><br><br>
    <b>TỔNG CỘNG: ${fmtMoney(grandTotal)}</b>
  `);
}

/* ========= Handle send ========= */
function handleOutgoingMessage(e) {
  e.preventDefault();
  const text = (messageInput && messageInput.value.trim()) || "";
  if (!text && userUploads.length === 0) return;
  if (messageInput) { messageInput.value = ""; messageInput.dispatchEvent(new Event("input")); }

  if (!chatBody) return;

  // Bubble user
  const content = `
    <div class="message-text"></div>
    ${userUploads.length ? `<div class="attachment-strip">${userUploads.map(u => `<img src="${u.previewUrl}" class="attachment">`).join("")}</div>` : ""}`;
  const outgoing = createMessageElement(content, "user-message");
  outgoing.querySelector(".message-text").innerText = text || "(ảnh đính kèm)";
  chatBody.appendChild(outgoing);
  chatBody.scrollTo({ top: chatBody.scrollHeight, behavior: "smooth" });

  // Slot-filling
  if (SLOT.mode === "collect") {
    const key = nextMissingKey();
    if (key) {
      const raw = extractInStep(key, text);

      if (key === "date") {
        const parsed = parseDateOnlyFromText(raw) || parseDateOnlyFromText(text);
        if (!parsed) { renderSlotReply('Mình chưa đọc được <b>ngày</b>. Bạn nhập <b>dd/mm/yyyy</b> nhé (vd: 12/11/2025).'); return; }
        const { dt } = parsed;
        const now = new Date();
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0,0,0,0);
        const limit = new Date(today.getTime()); limit.setMonth(limit.getMonth() + 1);
        if (dt.getTime() < today.getTime()) { renderSlotReply("Ngày bạn chọn đang ở <b>trong quá khứ</b>. Chọn ngày từ hôm nay trở đi giúp mình."); return; }
        if (dt.getTime() > limit.getTime()) { renderSlotReply("Hệ thống chỉ nhận lịch trong vòng <b>1 tháng</b> tới."); return; }
        SLOT.data._dateObj = dt; SLOT.data.date = formatDateOnlyVN(dt);

      } else if (key === "time") {
        const parsedTime = parseTimeFromText(raw) || parseTimeFromText(text);
        if (!parsedTime) { renderSlotReply('Mình chưa đọc được <b>giờ</b>. Bạn nhập như: "9h", "9:15", "9h kém 5", "5h chiều", "12 giờ rưỡi",...'); return; }
        const baseDate = SLOT.data._dateObj;
        if (!baseDate) { renderSlotReply("Mình chưa có <b>ngày</b>. Bạn nhập ngày (dd/mm/yyyy) trước nhé."); SLOT.data.date = null; SLOT.data._dateObj = null; return; }
        const dt = new Date(baseDate.getTime()); dt.setHours(parsedTime.hour, parsedTime.minute, 0, 0);
        const now = new Date(); const limit = new Date(now.getTime()); limit.setMonth(limit.getMonth() + 1);
        if (dt.getTime() < now.getTime()) { renderSlotReply("Giờ bạn chọn ở <b>trước thời điểm hiện tại</b>. Chọn giờ muộn hơn nhé."); return; }
        if (dt.getTime() > limit.getTime()) { renderSlotReply("Chỉ nhận lịch trong vòng 1 tháng tới."); return; }
        SLOT.data.time = formatTimeVN(parsedTime.hour, parsedTime.minute); SLOT.data.datetime = formatDateTimeVN(dt);

      } else if (key === "phone") {
        const phone = normalizePhone(raw);
        if (!phone) { renderSlotReply("Số điện thoại chưa hợp lệ. Ví dụ: 0912345678 hoặc +84 912345678."); return; }
        SLOT.data.phone = phone;

      } else if (key === "fromAddr" || key === "toAddr") {
        const addr = raw;
        if (!addr || addr.length < 4) { renderSlotReply("Bạn nhập địa chỉ rõ hơn giúp mình nhé (có số/đường/thôn/xã...)."); return; }
        renderSlotReply("Đang kiểm tra địa chỉ trên bản đồ…");
        geocodeAddress(addr).then(g => {
          if (!g.ok) {
            renderSlotReply("Địa chỉ chưa tìm thấy trên bản đồ. Bạn mô tả chi tiết hơn (số nhà, thôn/xã/phường, huyện/quận, tỉnh/thành) nhé.");
            return;
          }
          SLOT.data[key] = addr.trim();
          SLOT.data[key === "fromAddr" ? "fromPlace" : "toPlace"] = { formatted: g.formatted, lat: g.lat, lng: g.lng };
          renderSlotReply(`${key === "fromAddr" ? "Địa chỉ lấy hàng" : "Địa chỉ giao hàng"} đã xác thực: <b>${g.formatted}</b>`);
          const nextKey = nextMissingKey();
          if (nextKey) renderSlotReply(askQuestionFor(nextKey));
          else { renderSummaryAndFinish(); ensureDistanceAndTotals(); }
        });
        return; // chờ geocode
      } else if (key === "name") {
        const val = extractInStep(key, text);
        if (!val || val.length < 2) { renderSlotReply("Mình chưa rõ tên bạn. Bạn ghi lại giúp mình <b>HỌ TÊN đầy đủ</b> được không?"); return; }
        SLOT.data.name = val.trim();
      }

      const nextKey = nextMissingKey();
      if (nextKey) renderSlotReply(askQuestionFor(nextKey));
      else { renderSummaryAndFinish(); ensureDistanceAndTotals(); }
    }
    return;
  }

  // ❶ Parse lệnh thêm/xoá + intent vận chuyển
  const toAdd = parseUserAddCommand(text);
  const toRemove = parseUserRemoveCommand(text);
  const hasShippingIntent = detectShippingIntent(text);
  const intentItems = (!toAdd.length && hasShippingIntent) ? parseItemsFromShippingSentence(text) : [];

  if (toAdd.length && window.AIQUOTE?.upsertItem) toAdd.forEach(it => window.AIQUOTE.upsertItem(it.name, it.qty));
  if (toRemove.length && (window.AIQUOTE?.decrementItemByName || window.AIQUOTE?.removeItemByName)) {
    toRemove.forEach(it => { if (it.mode === "decrement" && it.qty > 0 && window.AIQUOTE.decrementItemByName) window.AIQUOTE.decrementItemByName(it.name, it.qty);
      else if (window.AIQUOTE.removeItemByName) window.AIQUOTE.removeItemByName(it.name); });
  }
  if (intentItems.length && window.AIQUOTE?.upsertItem) intentItems.forEach(it => window.AIQUOTE.upsertItem(it.name, it.qty));

  // Nếu có lệnh thêm/xoá HOẶC intent vận chuyển (text-only) mà không có ảnh → gắn CTA
  if ((toAdd.length || toRemove.length || hasShippingIntent) && userUploads.length === 0) {
    showConfirmCTA();
    if (hasShippingIntent && !toAdd.length && !toRemove.length && !intentItems.length) {
      renderSlotReply('Mình đã ghi nhận nhu cầu vận chuyển của bạn. Bạn có thể mô tả thêm đồ đạc (vd: "thêm 1 cái tủ, 2 cái ghế") hoặc bấm <b>Xác nhận vận chuyển</b> để mình hỏi thông tin giao nhận.');
    }
    return;
  }

  // Không có lệnh & không có intent & không có ảnh
  if (!userUploads.length) {
    if (isSmallTalkOrGreeting(text)) {
      renderSlotReply('Chào bạn 👋 Mình là trợ lý hỗ trợ <b>vận chuyển</b>. Bạn cho mình biết bạn đang muốn chuyển những đồ gì hoặc từ đâu đến đâu nhé (vd: "chuyển 1 cái giường từ Cầu Giấy lên Hoà Lạc" hoặc "thêm 3 thùng carton").');
    } else {
      renderSlotReply(DOMAIN_ONLY_MESSAGE + ' Ví dụ: bạn có thể nói <br>• "chuyển 1 cái giường từ Cầu Giấy lên Hoà Lạc"<br>• hoặc "thêm 2 thùng carton, bớt 1 cái tủ"...');
    }
    return;
  }

  // Có ảnh → gọi AI
  setTimeout(() => {
    if (!chatBody) return;
    const botContent = `
      <svg class="bot-avatar" xmlns="http://www.w3.org/2000/svg" width="50" height="50" viewBox="0 0 1024 1024">
        <path d="M738.3 287.6H285.7c-59 0-106.8 47.8-106.8 106.8v303.1c0 59 47.8 106.8 106.8 106.8h81.5v111.1c0 .7.8 1.1 1.4.7l166.9-110.6 41.8-.8h117.4l43.6-.4c59 0 106.8-47.8 106.8-106.8V394.5c0-59-47.8-106.9-106.8-106.9zM351.7 448.2c0-29.5 23.9-53.5 53.5-53.5s53.5 23.9 53.5 53.5-23.9 53.5-53.5 53.5-53.5-23.9-53.5-53.5zm157.9 267.1c-67.8 0-123.8-47.5-132.3-109h264.6c-8.6 61.5-64.5 109-132.3 109zm110-213.7c-29.5 0-53.5-23.9-53.5-53.5s23.9-53.5 53.5-53.5 53.5 23.9 53.5 53.5-23.9 53.5-53.5 53.5zM867.2 644.5V453.1h26.5c19.4 0 35.1 15.7 35.1 35.1v121.1c0 19.4-15.7 35.1-35.1 35.1h-26.5zM95.2 609.4V488.2c0-19.4 15.7-35.1 35.1-35.1h26.5v191.3h-26.5c-19.4 0-35.1-15.7-35.1-35.1zM561.5 149.6c0 23.4-15.6 43.3-36.9 49.7v44.9h-30v-44.9c-21.4-6.5-36.9-26.3-36.9-49.7 0-28.6 23.3-51.9 51.9-51.9s51.9 23.3 51.9 51.9z"/>
      </svg>
      <div class="message-text"><div class="thinking-indicator"><div class="dot"></div><div class="dot"></div><div class="dot"></div></div></div>`;
    const incoming = createMessageElement(botContent, "bot-message", "thinking");
    chatBody.appendChild(incoming);
    chatBody.scrollTo({ top: chatBody.scrollHeight, behavior: "smooth" });
    generateBotResponse(incoming, text, { allowAutofill: true });
  }, 200);
}

/* ========= Textarea ========= */
if (messageInput) {
  messageInput.addEventListener("input", () => {
    messageInput.style.height = `${initialInputHeight}px`;
    messageInput.style.height = `${messageInput.scrollHeight}px`;
  });
  messageInput.addEventListener("keydown", (e) => {
    const userText = e.target.value.trim();
    if (e.key === "Enter" && !e.shiftKey && (userUploads.length || userText) && window.innerWidth > 768) handleOutgoingMessage(e);
  });
}
if (sendMessage) sendMessage.addEventListener("click", (e) => handleOutgoingMessage(e));

/* ========= Bootstrap products ========= */
(async function bootstrapProducts() {
  const s = loadSettings(); let loaded = [];
  try { const r = await fetch(PRODUCTS_URL, { cache: "no-store" }); if (r.ok) loaded = await r.json(); } catch (e) { console.warn("Load products.json failed", e); }
  const normalized = []; const seen = new Set();
  for (const it of loaded || []) {
    const name = String(it?.name || "").trim(); if (!name) continue;
    const key = name.toLowerCase(); if (seen.has(key)) continue; seen.add(key);
    const price = Number(it?.price) || 0; normalized.push({ name, price });
  }
  while (normalized.length < TARGET_COUNT) {
    const baseName = BASE_NAMES[Math.floor(Math.random() * BASE_NAMES.length)];
    const full = decorateName(baseName); const key = full.toLowerCase(); if (seen.has(key)) continue; seen.add(key);
    const price = isBulky(full) ? bulkyPrice() : cheapPrice(); normalized.push({ name: full, price });
  }
  s.items = normalized; saveSettings(s);
})();

/* ========= CTA click ========= */
if (chatBody) {
  chatBody.addEventListener("click", (e) => {
    const btn = e.target.closest(".btn-chat-confirm"); if (!btn) return;
    clearChatCTA();
    SLOT.mode = "collect"; SLOT.step = 0;
    renderSlotReply("Cảm ơn bạn đã xác nhận. Mình sẽ hỏi vài thông tin để tạo Hợp đồng nháp.");
    renderSlotReply(askQuestionFor("name"));
  });
}

/* ========= Summary UI ========= */
function renderSummaryAndFinish() {
  const d = SLOT.data;
  const fromFmt = d.fromPlace?.formatted || d.fromAddr || "—";
  const toFmt = d.toPlace?.formatted || d.toAddr || "—";
  const html = `
    <b>TÓM TẮT YÊU CẦU</b><br>
    • Liên hệ: <b>${d.name || ""}</b> – <b>${d.phone || ""}</b><br>
    • Lấy hàng (đi): ${fromFmt}<br>
    • Giao hàng (đến): ${toFmt}<br>
    • Ngày: <b>${d.date || ""}</b><br>
    • Giờ: <b>${d.time || ""}</b><br><br>
    Thời gian chi tiết: <b>${d.datetime || ""}</b><br>
    ${d.km ? `• Quãng đường: <b>${d.km.toFixed(2)} km</b> (${d.durationText || ""})<br>` : ""}`;
  renderSlotReply(html);
}
