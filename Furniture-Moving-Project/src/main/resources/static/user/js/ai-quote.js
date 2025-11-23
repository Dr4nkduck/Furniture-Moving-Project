/* ===== AI Quote – Chat + Cart + Slot-filling + OSM/OSRM (NO KEY) =====
 * - Chat + AI xem ảnh, đếm hàng, thêm vào giỏ
 * - Slot-filling: hỏi Họ tên, SĐT, địa chỉ đi/đến, ngày, giờ
 * - Geocode bằng Nominatim + Photon, tính km bằng OSRM (không cần API key)
 * - Tạo draft hợp đồng + lưu vào sessionStorage.aiquote_draft
 */

/* ========= DOM hooks ========= */
const chatBody = document.querySelector(".chat-body");
const messageInput = document.querySelector(".message-input");
const sendMessage = document.querySelector("#send-message");
const fileInput = document.querySelector("#file-input");
const fileUploadBtn = document.querySelector("#file-upload");
const fileUploadWrapper = document.querySelector(".file-upload-wrapper");
const fileCancelButton = fileUploadWrapper ? fileUploadWrapper.querySelector("#file-cancel") : null;
const chatForm = document.querySelector(".chat-form");
const chatCard = document.querySelector(".chat-card");

if (fileUploadBtn && fileInput) fileUploadBtn.addEventListener("click", () => fileInput.click());

/* ====== Thanh chọn ngày chỉ hiện khi hỏi NGÀY ====== */
let slotDateBar = null;
let slotDateInput = null;
let ASKING_DATE = false; // chỉ TRUE khi đang hỏi slot 'date'

if (chatForm && chatCard) {
  slotDateBar = document.createElement("div");
  slotDateBar.className = "slot-date-bar d-flex align-items-center px-3 py-2";
  slotDateBar.style.borderTop = "1px dashed rgba(0,0,0,.06)";
  slotDateBar.style.background = "#fff";
  slotDateBar.style.display = "none";
  slotDateBar.innerHTML = `
    <i class="far fa-calendar-alt mr-2"></i>
    <small class="text-muted mr-2">Chọn ngày trên lịch:</small>
    <input type="date" class="slot-date-input form-control form-control-sm" style="max-width: 190px;">
  `;
  chatCard.insertBefore(slotDateBar, chatForm);
  slotDateInput = slotDateBar.querySelector(".slot-date-input");

  if (slotDateInput) {
    slotDateInput.addEventListener("change", () => {
      if (!slotDateInput.value || !messageInput) return;
      const [y, m, d] = slotDateInput.value.split("-");
      if (!y || !m || !d) return;
      const text = `${d}/${m}/${y}`;
      messageInput.value = text;
      handleOutgoingMessage({ preventDefault(){} });
      slotDateInput.value = "";
    });
  }
}
function updateSlotDateBarVisibility() {
  if (!slotDateBar) return;
  slotDateBar.style.display = (SLOT.mode === "collect" && ASKING_DATE) ? "flex" : "none";
}

/* ========= OSM stack: Geocode đa nguồn + OSRM (no key) ========= */

function fetchWithTimeout(url, opts = {}, ms = 6000) {
  return new Promise((resolve, reject) => {
    const ctrl = new AbortController();
    const id = setTimeout(() => { ctrl.abort(); reject(new Error("Geocode timeout")); }, ms);
    fetch(url, { ...opts, signal: ctrl.signal })
      .then(r => { clearTimeout(id); resolve(r); })
      .catch(err => { clearTimeout(id); reject(err); });
  });
}

/* Chuẩn hóa parts địa chỉ về chung 1 schema (VN-friendly) */
function mapAddressPartsFromNominatim(obj){
  const a = obj?.address || {};
  return {
    houseNumber: a.house_number || "",
    street: a.road || a.pedestrian || a.footway || a.cycleway || "",
    hamlet: a.hamlet || a.village || "",
    ward: a.suburb || a.neighbourhood || a.quarter || a.ward || "",
    commune: a.town || a.village || a.municipality || a.suburb || "",
    district: a.city_district || a.county || a.district || "",
    city: a.city || a.town || "",
    province: a.state || a.region || "",
    postcode: a.postcode || "",
    country: a.country || ""
  };
}
function mapAddressPartsFromPhoton(f){
  const p = f?.properties || {};
  return {
    houseNumber: p.housenumber || "",
    street: p.street || p.name || "",
    hamlet: p.locality || "",
    ward: p.suburb || "",
    commune: p.city || p.town || p.village || "",
    district: p.district || p.county || "",
    city: p.city || "",
    province: p.state || "",
    postcode: p.postcode || "",
    country: p.country || ""
  };
}

function normPlace(obj, provider) {
  if (!obj) return { ok:false };
  if (provider === "nominatim") {
    const lat = parseFloat(obj.lat), lng = parseFloat(obj.lon);
    if (Number.isNaN(lat) || Number.isNaN(lng)) return { ok:false };
    return { ok:true, formatted: obj.display_name || "", lat, lng, provider, parts: mapAddressPartsFromNominatim(obj) };
  }
  if (provider === "photon") {
    const g = obj.geometry, p = obj.properties || {};
    const coords = Array.isArray(g?.coordinates) ? g.coordinates : null; // [lng, lat]
    if (!coords || coords.length < 2) return { ok:false };
    const lng = Number(coords[0]), lat = Number(coords[1]);
    if (Number.isNaN(lat) || Number.isNaN(lng)) return { ok:false };
    const composed =
      [p.name, p.street, p.city, p.state, p.country].filter(Boolean).join(", ");
    return { ok:true, formatted: composed || p.formatted || p.label || "", lat, lng, provider, parts: mapAddressPartsFromPhoton(obj) };
  }
  return { ok:false };
}

/* --- Fallback helpers cho địa chỉ --- */
function stripHouseNumber(addr) {
  if (!addr) return "";
  let s = String(addr).trim();
  s = s.replace(/^(số nhà|so nha|số|so)\s*\d+[A-Za-z0-9\/\-]*\s*,?\s*/i, "");
  const commaIdx = s.indexOf(",");
  if (commaIdx > -1 && commaIdx < s.length - 3) {
    const after = s.slice(commaIdx + 1).trim();
    if (after.length >= 3) return after;
  }
  return s.trim();
}

/* Cắt địa chỉ từ cấp thôn/xóm/ấp/tổ... trở về sau (VN-friendly) */
function stripToHamletLevel(addr) {
  if (!addr) return "";
  let parts = String(addr).split(",").map(s => s.trim()).filter(Boolean);
  if (parts.length <= 1) {
    const tokens = String(addr).trim().split(/\s+/);
    parts = [];
    let buf = [];
    for (const tk of tokens) {
      buf.push(tk);
      if (/(tỉnh|thành|thành phố|tp\.?|quận|huyện|thị xã|thị trấn|phường|xã|thôn|xóm|ấp|khóm|bản|buôn|tổ|khu|làng)/i.test(tk)) {
        parts.push(buf.join(" "));
        buf = [];
      }
    }
    if (buf.length) parts.push(buf.join(" "));
  }

  const hamletRx = /(thôn|xóm|ấp|khóm|bản|buôn|tổ|khu|làng)/i;

  let startIdx = parts.findIndex(p => hamletRx.test(p));
  if (startIdx === -1) {
    startIdx = parts.findIndex(p => /(phường|xã|thị trấn)/i.test(p));
  }
  if (startIdx === -1) {
    startIdx = parts.findIndex(p => /(quận|huyện|thị xã)/i.test(p));
  }
  if (startIdx === -1) {
    startIdx = parts.findIndex(p => /(tỉnh|thành|thành phố|tp\.?)/i.test(p));
  }

  if (startIdx === -1) {
    return String(addr).trim();
  }
  return parts.slice(startIdx).join(", ").trim();
}

/* ========= Helper normalize & chấm điểm địa chỉ theo cấp hành chính ========= */
// Bỏ dấu tiếng Việt để so sánh
function stripVNAccents(str) {
  return String(str || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D");
}

// Dùng trong địa chỉ / từ khoá
function _normalize(s) {
  return stripVNAccents(String(s || "")).toLowerCase().trim();
}

function extractAdminHintsFromQuery(q){
  const t = _normalize(q);

  const mHamlet   = t.match(/\b(thôn|xóm|ấp|khóm|bản|buôn|tổ|khu|làng)\s+([a-z0-9\s\-]+)/i);
  const mWard     = t.match(/\b(phường|xã|thị trấn)\s+([a-z0-9\s\-]+)/i);
  const mDistrict = t.match(/\b(quận|huyện|thị xã)\s+([a-z0-9\s\-]+)/i);
  const mCityProv = t.match(/\b(thành phố|tỉnh|tp\.?)\s+([a-z0-9\s\.\-]+)/i);

  return {
    hamlet:   mHamlet   ? _normalize(mHamlet[2])   : "",
    ward:     mWard     ? _normalize(mWard[2])     : "",
    district: mDistrict ? _normalize(mDistrict[2]) : "",
    cityprov: mCityProv ? _normalize(mCityProv[2]) : ""
  };
}

function scorePlaceByParts(parts, qHints){
  const hamlet   = _normalize(parts.hamlet   || "");
  const ward     = _normalize(parts.ward     || "");
  const commune  = _normalize(parts.commune  || "");
  const district = _normalize(parts.district || "");
  const city     = _normalize(parts.city     || "");
  const prov     = _normalize(parts.province || "");

  if (!hamlet && !ward && !commune && !district && !city && !prov) {
    return -1e9;
  }

  let s = 10;

  if (ward || commune) s += 15;
  if (hamlet) s += 8;

  if (qHints.hamlet && hamlet && hamlet.includes(qHints.hamlet)) {
    s += 35;
  }

  if (qHints.ward) {
    if ((ward && ward.includes(qHints.ward)) ||
        (commune && commune.includes(qHints.ward))) {
      s += 25;
    }
  }

  if (qHints.district && district && district.includes(qHints.district)) {
    s += 15;
  }

  if (qHints.cityprov && ((city && city.includes(qHints.cityprov)) ||
                          (prov && prov.includes(qHints.cityprov)))) {
    s += 10;
  }

  if (district) s += 4;
  if (city)     s += 2;
  if (prov)     s += 2;

  return s;
}

// Chỉ chấp nhận địa chỉ thuộc Hà Nội
function isHanoiParts(parts) {
  const city = _normalize(parts.city || "");
  const prov = _normalize(parts.province || "");
  const district = _normalize(parts.district || "");

  const combo = `${city} ${prov} ${district}`;
  return combo.includes("ha noi");
}

/* ========= Lấy NHIỀU ứng viên từ Nominatim/Photon ========= */
async function geocodeNominatimMulti(q, limit = 6){
  const url = `https://nominatim.openstreetmap.org/search?format=json&limit=${limit}&addressdetails=1&accept-language=vi&q=${encodeURIComponent(q)}`;
  try {
    const r = await fetchWithTimeout(url, {}, 7000);
    if (!r.ok) throw new Error(`Nominatim HTTP ${r.status}`);
    const arr = await r.json();
    return (arr || []).map(x => normPlace(x, "nominatim")).filter(p => p.ok);
  } catch {
    return [];
  }
}

async function geocodePhotonMulti(q, limit = 6){
  const url = `https://photon.komoot.io/api/?q=${encodeURIComponent(q)}&lang=vi&limit=${limit}`;
  try {
    const r = await fetchWithTimeout(url, {}, 7000);
    if (!r.ok) throw new Error(`Photon HTTP ${r.status}`);
    const data = await r.json();
    const feats = Array.isArray(data?.features) ? data.features : [];
    return feats.map(x => normPlace(x, "photon")).filter(p => p.ok);
  } catch {
    return [];
  }
}

/* ========= Geocode address: chọn best match, chấp nhận từ cấp thôn trở lên ========= */
async function geocodeAddress(query) {
  const q = String(query || "").trim();
  if (q.length < 3) return { ok:false };

  const hints = extractAdminHintsFromQuery(q);

  const pool = [
    ...(await geocodeNominatimMulti(q, 6)),
    ...(await geocodePhotonMulti(q, 6))
  ];

  const candidates = pool.filter(p => {
    const parts = p.parts || {};
    return !!(parts.hamlet || parts.ward || parts.commune ||
              parts.district || parts.city || parts.province);
  });

  if (!candidates.length) return { ok:false };

  candidates.sort((a,b) => {
    const pa = scorePlaceByParts(a.parts || {}, hints);
    const pb = scorePlaceByParts(b.parts || {}, hints);
    return pb - pa;
  });

  return candidates[0] || { ok:false };
}

/* ========= Geocode với fallback + ép Hà Nội ========= */
async function resolveAddressWithFallback(addr) {
  const raw = String(addr || "").trim();
  if (!raw) return { ok: false };

  function packResult(res, usedQuery, tag) {
    if (!res || !res.ok) return res || { ok: false };

    const parts = res.parts || {};
    const hasAdmin = !!(
      parts.hamlet || parts.ward || parts.commune ||
      parts.district || parts.city || parts.province
    );
    if (!hasAdmin) return { ok: false };

    // chỉ nhận địa chỉ trong Hà Nội
    if (!isHanoiParts(parts)) {
      return { ok: false, reason: "out_of_area" };
    }

    return {
      ok: true,
      ...res,
      missingHouse: !parts.houseNumber,
      usedQuery,
      fallback: tag
    };
  }

  // 1) Full chuỗi
  const primary = await geocodeAddress(raw);
  const r1 = packResult(primary, raw, "none");
  if (r1.ok || r1.reason === "out_of_area") return r1;

  // 2) Cắt từ thôn/xóm/ấp trở lên
  const hamletOnly = stripToHamletLevel(raw);
  if (hamletOnly && hamletOnly !== raw) {
    const hRes = await geocodeAddress(hamletOnly);
    const r2 = packResult(hRes, hamletOnly, "hamlet");
    if (r2.ok || r2.reason === "out_of_area") return r2;
  }

  // 3) Bỏ số nhà / phần đầu rồi thử lại
  const stripped = stripHouseNumber(raw);
  if (stripped && stripped !== raw && stripped !== hamletOnly) {
    const fRes = await geocodeAddress(stripped);
    const r3 = packResult(fRes, stripped, "strip");
    if (r3.ok || r3.reason === "out_of_area") return r3;
  }

  // 4) Không tìm thấy gì ổn
  return { ok: false };
}

/* Tính khoảng cách lái xe bằng OSRM */
async function calcDistance(orig, dest) {
  if (!orig || !dest) return { ok: false };
  const toLngLat = (p)=>`${Number(p.lng)},${Number(p.lat)}`;
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
    return { ok: true, km, durationText, routeText, seconds };
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

/* ========= Gemini ========= */
const API_KEY = "AIzaSyCQZaLPV5xXjs65vh2f8L6HlwHAn8ouSoc"; // <-- Nên chuyển về BE/proxy
let MODEL = "gemini-2.0-flash";
const FALLBACK_MODEL = "gemini-2.0-flash-lite";
const buildApiUrl = () => `https://generativelanguage.googleapis.com/v1/models/${MODEL}:generateContent?key=${API_KEY}`;

/* ========= State ========= */
const chatHistory = [];
const userUploads = [];
const initialInputHeight = messageInput ? messageInput.scrollHeight : 0;

// Cờ và util
function hasAtLeastOneItem() {
  const t = (window.AIQUOTE?.getTotals?.() || { qty:0 }).qty;
  return t > 0;
}
function hasAllRequiredSlots() {
  const d = SLOT.data || {};
  return !!(d.name && d.phone && d.fromPlace && d.toPlace && d.date && d.time);
}
function isDraftReady() {
  return !!SLOT.data?.draftReady;
}

// Tạo payload hợp đồng nháp (lưu sang sessionStorage để contract.html đọc)
function buildDraftPayload() {
  const d = SLOT.data;
  const totals = (window.AIQUOTE?.getTotals?.() || { qty:0, amount:0 });
  const weight = (window.AIQUOTE?.getWeightSummary?.() || []);
  return {
    customer: { name: d.name, phone: d.phone },
    pickup: {
      raw: d.fromAddr, formatted: d.fromPlace?.formatted,
      lat: d.fromPlace?.lat, lng: d.fromPlace?.lng, parts: d.fromParts
    },
    dropoff: {
      raw: d.toAddr, formatted: d.toPlace?.formatted,
      lat: d.toPlace?.lat, lng: d.toPlace?.lng, parts: d.toParts
    },
    schedule: { date: d.date, time: d.time, datetime: d.datetime },
    route: {
      km: d.km, durationText: d.durationText, routeText: d.routeText, seconds: d.routeSeconds
    },
    cart: {
      totalQty: totals.qty,
      itemsAmount: totals.amount,
      weightSummary: weight
    },
    currency: (loadSettings().currency || "VND"),
    createdAt: new Date().toISOString()
  };
}

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
    fromAddr: null, toAddr: null,
    fromPlace: null, toPlace: null,
    fromParts: null, toParts: null,
    date: null, time: null, datetime: null,
    _dateObj: null,
    _datetimeObj: null,
    km: null,
    durationText: null,
    routeText: null,
    routeSeconds: null
  }
};

const SLOT_STEPS = ["name", "phone", "fromAddr", "toAddr", "date", "time"];

function resetSlot() {
  SLOT.mode = "idle"; SLOT.step = 0;
  SLOT.data = {
    name:null, phone:null,
    fromAddr:null, toAddr:null,
    fromPlace:null, toPlace:null,
    fromParts:null, toParts:null,
    date:null, time:null, datetime:null,
    _dateObj:null, _datetimeObj:null,
    km:null, durationText:null, routeText:null, routeSeconds:null
  };
  ASKING_DATE = false;
  updateSlotDateBarVisibility();
}

function nextMissingKey() {
  for (const k of SLOT_STEPS) if (!SLOT.data[k]) return k;
  return null;
}
function askQuestionFor(key) {
  const Q = {
    name: "Cho mình xin <b>HỌ TÊN</b> người liên hệ?",
    phone: "Số điện thoại liên hệ là gì ạ? (vd: 0912345678 hoặc +84 912345678)",
    fromAddr: "Địa chỉ <b>ĐI</b> (nơi <b>LẤY HÀNG</b>) ở đâu? Bạn cứ nhập tự do (vd: 'số 12 thôn 4 Hòa Lạc, Thạch Thất').",
    toAddr: "Địa chỉ <b>ĐẾN</b> (nơi <b>GIAO HÀNG</b>) ở đâu? Cứ nhập tự do, mình sẽ kiểm tra địa chỉ thật giúp bạn.",
    date: "Bạn muốn vận chuyển vào <b>NGÀY</b> nào? (vd: 12/11/2025). Bạn có thể gõ 'ngày mai', 'ngày 28 tháng này' hoặc chọn trong lịch phía dưới.",
    time: 'Bạn muốn vận chuyển vào <b>GIỜ</b> nào? (vd: "9h kém 5", "5h chiều", "12 giờ rưỡi", "9:15",…).'
  };
  return Q[key] || "";
}
function askSlotQuestion(key) {
  ASKING_DATE = (key === "date");
  renderSlotReply(askQuestionFor(key));
  updateSlotDateBarVisibility();
}

/* ========= Cho phép SỬA GIỮA CHỪNG ========= */
function detectAndApplyCorrections(rawText){
  const t = (rawText||"").trim();
  if (!t) return false;

  let changed = false;
  let updatedPhoneThisTurn = false;
  let updatedNameThisTurn = false;
  let updatedFromThisTurn = false;
  let updatedToThisTurn = false;
  let updatedDateThisTurn = false;
  let updatedTimeThisTurn = false;

  // đổi/sửa tên
  let m =
    t.match(/\b(?:đổi|sửa)\s*tên\s*(?:thành|lại)?\s*[:\-]?\s*(.+)$/i) ||
    t.match(/\b(?:tên|họ\s*tên)\s*(của tôi|của em|của anh|của chị|là|:)\s*(.+)$/i);
  if (m) {
    const name = (m[2] || m[1] || "").trim();
    if (name) {
      SLOT.data.name = name.slice(0,80);
      updatedNameThisTurn = true;
      changed = true;
      renderSlotReply(`Đã cập nhật tên thành: <b>${escapeHTML(SLOT.data.name)}</b>.`);
    }
  }

  // đổi/sửa số điện thoại (có số mới)
  m =
    t.match(/\b(?:đổi|sửa)\s*(?:số\s*điện\s*thoại|sđt)\s*(?:thành|lại)?\s*[:\-]?\s*([+0-9()\s\.\-]+)/i) ||
    t.match(/\b(?:sđt|số\s*điện\s*thoại)\s*(?:mới\s*|đúng\s*|là|:)\s*([+0-9()\s\.\-]+)/i);
  if (m) {
    const phone = normalizePhone(m[1]);
    if (phone) {
      SLOT.data.phone = phone;
      updatedPhoneThisTurn = true;
      changed = true;
      renderSlotReply(`Đã cập nhật SĐT: <b>${escapeHTML(phone)}</b>.`);
    } else {
      renderSlotReply("SĐT bạn cung cấp chưa hợp lệ, bạn nhập lại giúp mình nhé (vd: 0912345678 hoặc +84 912345678).");
    }
  }

  // đổi ngày (có ngày mới)
  m = t.match(/\b(?:đổi|sửa)\s*ngày\s*(?:thành|sang)?\s*(.+)$/i);
  if (m) {
    const parsed = parseFlexibleDate(m[1]);
    if (parsed) {
      const { dt } = parsed;
      SLOT.data._dateObj = dt;
      SLOT.data.date = formatDateOnlyVN(dt);
      SLOT.data._datetimeObj = null;
      updatedDateThisTurn = true;
      changed = true;
      renderSlotReply(`Đã cập nhật <b>ngày</b> thành: ${escapeHTML(SLOT.data.date)}.`);
    } else {
      renderSlotReply("Mình chưa đọc được ngày mới, bạn nhập dd/mm/yyyy hoặc 'ngày mai' nhé.");
    }
  }

  // đổi giờ (có giờ mới)
  m = t.match(/\b(?:đổi|sửa)\s*giờ\s*(?:thành|sang)?\s*(.+)$/i);
  if (m) {
    const ti = parseTimeFromText(m[1]);
    if (ti) {
      if (!SLOT.data._dateObj) {
        SLOT.data.time = formatTimeVN(ti.hour, ti.minute);
        SLOT.data.datetime = null;
        SLOT.data._datetimeObj = null;
      } else {
        const dt = new Date(SLOT.data._dateObj.getTime());
        dt.setHours(ti.hour, ti.minute, 0, 0);
        SLOT.data.time = formatTimeVN(ti.hour, ti.minute);
        SLOT.data.datetime = formatDateTimeVN(dt);
        SLOT.data._datetimeObj = dt;
      }
      updatedTimeThisTurn = true;
      changed = true;
      renderSlotReply(`Đã cập nhật <b>giờ</b> thành: ${escapeHTML(SLOT.data.time)}.`);
    } else {
      renderSlotReply("Mình chưa đọc được giờ mới, bạn thử dạng '9h', '9:15', '5h chiều'…");
    }
  }

  // đổi địa chỉ đi
  m = t.match(/\b(?:đổi|sửa)\s*(?:địa\s*chỉ\s*đi|địa\s*chỉ\s*lấy|đi|lấy)\s*(?:thành|sang)?\s*[:\-]?\s*(.+)$/i);
  if (m) {
    const addr = m[1].trim();
    if (addr.length >= 4) {
      changed = true;
      updatedFromThisTurn = true;
      renderSlotReply("Đang kiểm tra địa chỉ LẤY hàng mới…");
      resolveAddressWithFallback(addr).then(g=>{
        if (!g.ok) {
          if (g.reason === "out_of_area") {
            renderSlotReply(
              "Hiện tại hệ thống chỉ hỗ trợ địa chỉ trong <b>Hà Nội</b>. " +
              "Bạn nhập lại giúp mình một địa chỉ thuộc Hà Nội nhé."
            );
          } else {
            renderSlotReply("Địa chỉ lấy hàng chưa tìm thấy trên bản đồ. Bạn mô tả rõ hơn nhé.");
          }
          return;
        }
        SLOT.data.fromAddr = addr;
        SLOT.data.fromPlace = { formatted: g.formatted, lat: g.lat, lng: g.lng };
        SLOT.data.fromParts = g.parts || null;
        const note = g.missingHouse
          ? `Chưa xác định số nhà, sẽ lấy khu vực gần: <b>${escapeHTML(g.formatted)}</b>`
          : `Địa chỉ lấy hàng đã xác thực: <b>${escapeHTML(g.formatted)}</b>`;
        renderSlotReply(note);
      });
    }
  }

  // đổi địa chỉ đến
  m = t.match(/\b(?:đổi|sửa)\s*(?:địa\s*chỉ\s*đến|địa\s*chỉ\s*giao|đến|giao)\s*(?:thành|sang)?\s*[:\-]?\s*(.+)$/i);
  if (m) {
    const addr = m[1].trim();
    if (addr.length >= 4) {
      changed = true;
      updatedToThisTurn = true;
      renderSlotReply("Đang kiểm tra địa chỉ GIAO hàng mới…");
      resolveAddressWithFallback(addr).then(g=>{
        if (!g.ok) {
          if (g.reason === "out_of_area") {
            renderSlotReply(
              "Hiện tại hệ thống chỉ hỗ trợ địa chỉ trong <b>Hà Nội</b>. " +
              "Bạn nhập lại giúp mình một địa chỉ thuộc Hà Nội nhé."
            );
          } else {
            renderSlotReply("Địa chỉ giao hàng chưa tìm thấy trên bản đồ. Bạn mô tả rõ hơn nhé.");
          }
          return;
        }
        SLOT.data.toAddr = addr;
        SLOT.data.toPlace = { formatted: g.formatted, lat: g.lat, lng: g.lng };
        SLOT.data.toParts = g.parts || null;
        const note = g.missingHouse
          ? `Chưa xác định số nhà, sẽ lấy khu vực gần: <b>${escapeHTML(g.formatted)}</b>`
          : `Địa chỉ giao hàng đã xác thực: <b>${escapeHTML(g.formatted)}</b>`;
        renderSlotReply(note);
      });
    }
  }

  const hasWrongWord = /\b(sai|nhầm|nhập sai|nhập nhầm|bị nhầm|bị sai)\b/i.test(t);

  if (!updatedNameThisTurn && hasWrongWord && /\b(tên|họ\s*tên)\b/i.test(t)) {
    SLOT.data.name = null;
    changed = true;
    renderSlotReply("Không sao, bạn cho mình xin lại <b>HỌ TÊN chính xác</b> nhé.");
  }

  if (!updatedPhoneThisTurn && hasWrongWord && /\b(sđt|số\s*điện\s*thoại)\b/i.test(t)) {
    SLOT.data.phone = null;
    changed = true;
    renderSlotReply("Không sao, bạn gửi lại giúp mình <b>SĐT đúng</b> nhé (vd: 0912345678 hoặc +84 912345678).");
  }

  if (!updatedFromThisTurn && hasWrongWord &&
      /\b(địa\s*chỉ\s*(đi|lấy)|chỗ lấy|lấy hàng)\b/i.test(t)) {
    SLOT.data.fromAddr = null;
    SLOT.data.fromPlace = null;
    SLOT.data.fromParts = null;
    changed = true;
    renderSlotReply("Bạn gửi lại giúp mình <b>địa chỉ LẤY hàng</b> mới (số nhà, thôn/xã/phường, huyện/quận, tỉnh/thành) nhé.");
  }

  if (!updatedToThisTurn && hasWrongWord &&
      /\b(địa\s*chỉ\s*(đến|giao)|chỗ giao|giao hàng)\b/i.test(t)) {
    SLOT.data.toAddr = null;
    SLOT.data.toPlace = null;
    SLOT.data.toParts = null;
    changed = true;
    renderSlotReply("Bạn gửi lại giúp mình <b>địa chỉ GIAO hàng</b> mới (số nhà, thôn/xã/phường, huyện/quận, tỉnh/thành) nhé.");
  }

  if (!updatedDateThisTurn && hasWrongWord && /\b(ngày)\b/i.test(t)) {
    SLOT.data.date = null;
    SLOT.data._dateObj = null;
    changed = true;
    renderSlotReply("Không sao, bạn nhập lại giúp mình <b>NGÀY vận chuyển</b> (dd/mm/yyyy hoặc 'ngày mai', 'ngày 28 tháng này'...).");
  }

  if (!updatedTimeThisTurn && hasWrongWord && /\b(giờ|time|thời gian)\b/i.test(t)) {
    SLOT.data.time = null;
    SLOT.data.datetime = null;
    changed = true;
    renderSlotReply("Không sao, bạn nhập lại giúp mình <b>GIỜ vận chuyển</b> (vd: '9h', '9:15', '5h chiều', '10 rưỡi'...).");
  }

  SLOT.data.draftReady = false;
  try { sessionStorage.removeItem('aiquote_draft'); } catch {}

  return changed;
}

/* ========= Date/Time helpers ========= */
function parseDateOnlyFromText(text) {
  const t = (text || "").trim();
  const m = t.match(/(\d{1,2})[\/\-](\d{1,2})[\/\-](\d{2,4})/);
  if (!m) return null;
  let [, dd, mm, yy] = m;
  let day = +dd;
  let month = +mm - 1;
  let year = +yy;
  if (year < 100) year += 2000;
  const dt = new Date(year, month, day, 0, 0, 0, 0);
  if (isNaN(dt.getTime())) return null;
  return { dt };
}

function parseFlexibleDate(text) {
  const direct = parseDateOnlyFromText(text);
  if (direct) return direct;

  const raw = (text || "").toLowerCase();
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0,0,0,0);

  if (/\b(ngày\s*)?mai\b/.test(raw)) {
    const dt = new Date(today.getTime()); dt.setDate(dt.getDate() + 1);
    return { dt };
  }
  if (/\b(ngày\s*)?mốt\b/.test(raw)) {
    const dt = new Date(today.getTime()); dt.setDate(dt.getDate() + 2);
    return { dt };
  }
  if (/\b(ngày\s*)?kia\b/.test(raw)) {
    const dt = new Date(today.getTime()); dt.setDate(dt.getDate() + 3);
    return { dt };
  }

  const m = raw.match(/ngày\s*(\d{1,2})\s*(?:tháng\s*này)?/);
  if (m) {
    const day = parseInt(m[1], 10);
    if (Number.isNaN(day)) return null;
    let dt = new Date(today.getFullYear(), today.getMonth(), day, 0,0,0,0);
    if (dt.getTime() < today.getTime()) {
      dt = new Date(today.getFullYear(), today.getMonth() + 1, day, 0,0,0,0);
    }
    return { dt };
  }
  return null;
}

function parseTimeFromText(text) {
  const raw = (text || "").toLowerCase().trim();
  if (!raw) return null;
  let t = raw.replace(/giờ/g, "h").replace(/\s+/g, " ");
  let m = t.match(/(\d{1,2})\s*h\s*kém\s*(\d{1,2})/i);
  if (m) {
    let hour = +m[1];
    const minus = +m[2];
    let minute = (60 - minus)%60;
    hour = (hour - 1 + 24)%24;
    return { hour, minute };
  }
  m = t.match(/(\d{1,2})[:\.](\d{1,2})/);
  if (m) {
    const hour = +m[1], minute = +m[2];
    if (hour>23||minute>59) return null;
    return { hour, minute };
  }
  m = t.match(/(\d{1,2})\s*h\s*rưỡi/);
  if (m) {
    const hour = +m[1];
    if (hour>23) return null;
    return { hour, minute: 30 };
  }
  m = t.match(/(\d{1,2})\s*h(?:\s*(sáng|trưa|chiều|tối|pm|am))?/i);
  if (m) {
    let hour = +m[1];
    const desc = (m[2]||"").toLowerCase();
    if (desc === "chiều" || desc === "tối" || desc === "pm") {
      if (hour < 12) hour += 12;
    } else if (desc === "sáng" || desc === "am") {
      if (hour === 12) hour = 0;
    } else if (desc === "trưa") {
      if (hour < 10) hour += 12;
    }
    return { hour: hour%24, minute: 0 };
  }
  m = t.match(/(\d{1,2})\s*h\s*(\d{1,2})/);
  if (m) {
    const hour = +m[1], minute = +m[2];
    if (hour>23||minute>59) return null;
    return { hour, minute };
  }
  m = t.match(/^(\d{1,2})$/);
  if (m) {
    const hour = +m[1];
    if (hour>23) return null;
    return { hour, minute: 0 };
  }
  return null;
}

function formatDateOnlyVN(dt){
  const dd=String(dt.getDate()).padStart(2,"0");
  const mm=String(dt.getMonth()+1).padStart(2,"0");
  const yyyy=dt.getFullYear();
  return `${dd}/${mm}/${yyyy}`;
}
function formatTimeVN(h,m){
  return `${String(h).padStart(2,"0")}:${String(m).padStart(2,"0")}`;
}
function formatDateTimeVN(dt){
  const dd=String(dt.getDate()).padStart(2,"0");
  const mm=String(dt.getMonth()+1).padStart(2,"0");
  const yyyy=dt.getFullYear();
  const hh=String(dt.getHours()).padStart(2,"0");
  const mi=String(dt.getMinutes()).padStart(2,"0");
  return `${dd}/${mm}/${yyyy} ${hh}:${mi}`;
}

/* ====== Phone normalize ====== */
function normalizePhone(phoneText) {
  const digits = String(phoneText||"").replace(/[^\d+]/g,"");
  if (!digits) return null;

  let d = digits;
  if (d.startsWith("+84")) d = "0" + d.slice(3);
  if (d.startsWith("84") && d.length===11) d = "0" + d.slice(2);

  const only = d.replace(/[^\d]/g,"");
  if (only.length < 9 || only.length > 11) return null;
  if (!only.startsWith("0")) return null;
  return only;
}

/* ====== Escape để tránh XSS khi chèn HTML ====== */
function escapeHTML(s){ return String(s??"").replace(/[&<>"']/g, m=>({ "&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[m])); }

/* ====== Extract theo step (bổ sung) ====== */
function extractInStep(key, text) {
  const t = String(text || "").trim();
  if (!t) return "";
  if (key === "name") {
    const m = t.match(/\b(?:tên|họ\s*tên)\s*(?:là|:)\s*(.+)$/i) || t.match(/^([A-ZÀ-Ỵa-zà-ỹ\s\.]+)$/i);
    return (m ? (m[1] || m[0]) : t).trim();
  }
  if (key === "phone") {
    const p = normalizePhone(t);
    return p ? p : t;
  }
  if (key === "fromAddr" || key === "toAddr") {
    return t;
  }
  if (key === "date") {
    return t;
  }
  if (key === "time") {
    return t;
  }
  return t;
}

/* ====== UI helpers ====== */
function createMessageElement(content, ...classes) {
  const div = document.createElement("div");
  div.classList.add("message", ...classes);
  div.innerHTML = content;
  return div;
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
function clearChatCTA(){
  document.querySelectorAll(".chat-cta").forEach(el=>el.remove());
}
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

// GIÁ SẢN PHẨM BÂY GIỜ LẤY TỪ BACKEND, KHÔNG DÙNG products.json NỮA
const PRODUCTS_URL = "/api/ai/products-price";

/* Các keyword nhận diện đồ cồng kềnh / sách / ... (không dùng random giá nữa) */
const BULKY_KEYWORDS = [
  "sofa","ghế sofa","sofa góc","giường","giường đơn","giường đôi","giường tầng",
  "tủ quần áo","tủ 3 cánh","tủ 4 cánh","tủ 5 cánh","tủ bếp lớn",
  "tủ lạnh side-by-side","tủ lạnh 4 cánh",
  "máy chạy bộ","máy tập đa năng","máy photocopy","két sắt lớn",
  "máy giặt","máy sấy","máy rửa chén","máy lạnh đứng",
  "tivi 55 inch","tivi 65 inch","tivi 75 inch",
  "bàn họp","kệ kho sắt","bàn bida","bàn bida mini","tủ thờ","bàn thờ","cây cảnh lớn"
];

const SIZE_TAGS = ["mini","nhỏ","vừa","lớn","cao cấp"];
const MATERIALS = ["gỗ","gỗ sồi","gỗ thông","gỗ công nghiệp","nhựa","inox","thép","hợp kim","vải","da","da PU"];
const COLORS = ["trắng","đen","xám","nâu","be","xanh","đỏ"];

// Tách tên chính & kích thước từ chuỗi tên
const SIZE_WORDS = ["mini","nhỏ","vừa","lớn","cao cấp"];
function splitNameAndSize(fullName) {
  const full = String(fullName || "").trim();
  if (!full) return { base: "", size: "" };

  const mParen = full.match(/\((mini|nhỏ|vừa|lớn|cao cấp)\)\s*$/i);
  if (mParen) {
    return {
      base: full.replace(/\((mini|nhỏ|vừa|lớn|cao cấp)\)\s*$/i, "").trim(),
      size: mParen[1]
    };
  }

  const lower = full.toLowerCase();
  for (const w of SIZE_WORDS) {
    if (lower.endsWith(" " + w)) {
      return {
        base: full.slice(0, full.length - w.length).trim(),
        size: w
      };
    }
  }

  return { base: full, size: "" };
}

/* Hạng mục tính theo cân nặng (ví dụ: sách)
 * Chỉ dùng để ƯỚC LƯỢNG CÂN NẶNG, KHÔNG tính tiền ở FE nữa.
 */
const WEIGHT_RULES = [
  {
    keywords: ["sách", "quyển sách", "cuốn sách", "thùng sách", "chồng sách", "sách vở"],
    unitLabel: "quyển",
    kgPerUnit: 0.5
  }
];

function findWeightRuleForName(name) {
  if (!name) return null;
  const n = String(name).toLowerCase();
  for (const rule of WEIGHT_RULES) {
    if (rule.keywords.some(k => n.includes(k.toLowerCase()))) return rule;
  }
  return null;
}

/* ========= Config ========= */
const CFG_KEY = "ai_quote_cfg_v8_books_append_cart_corrections";
function defaultSettings(){ 
  return {
    currency:"VND",
    pricePerKm: 10000,
    minFare: 50000,
    basePrompt:
`Bạn là “Trợ lý Báo giá Vận chuyển”.
Chỉ trả lời trong phạm vi vận chuyển. Không trả JSON/code.

Người dùng có thể nói chuyện tự nhiên, dùng xưng hô (em, anh, chị, mình...), nói lan man.
Hãy bỏ qua phần xã giao, chỉ tập trung vào nội dung liên quan tới:
- Đồ vật cần chuyển, số lượng, đặc điểm (to, nhỏ, nặng, cồng kềnh...).
- Thông tin vận chuyển (ngày giờ, địa chỉ đi/đến, tên, số điện thoại).

Nhiệm vụ chính khi có ảnh:
- Nhận diện đồ nội thất, thiết bị, thùng carton, đồ gia dụng trong ảnh.
- Đếm số lượng từng hạng mục và gộp theo loại (vd: "tủ quần áo 3 cánh", "giường đôi", "bàn làm việc", "ghế xoay", "thùng carton nhỏ"...).

ĐẶC BIỆT với SÁCH:
- Nhận diện sách, quyển sách, cuốn sách, thùng sách, chồng sách,...
- Ước lượng SỐ QUYỂN SÁCH (không cần chính xác 100%, chỉ cần hợp lý).
- KHÔNG gộp sách chung với đồ khác.

Nếu không chắc tên, ghi tên mô tả (vd: "thùng carton nhỏ", "tủ gỗ 3 cánh", "kệ để đồ", "sách lẫn lộn trên bàn").

Định dạng trả về:
- Với mỗi hạng mục, in 1 dòng:

  <Tên>: <SL> cái — đơn giá <X> VND — tạm tính <Y> VND

  (Đối với sách và đồ tính theo kg, vẫn dùng định dạng trên; FE sẽ tự quy đổi số lượng sang kg và tính tiền theo kg.)

- Cuối cùng in dòng:
  Tổng tạm tính: <Số tiền> VND

QUY TẮC:
- KHÔNG dùng Markdown đậm hoặc bullet đặc biệt.
- KHÔNG in JSON, KHÔNG in code.
- KHÔNG trả lời các câu hỏi ngoài chủ đề vận chuyển.

Sau khi liệt kê xong, kết thúc bằng một câu gợi ý người dùng kiểm tra lại danh sách và xác nhận.`,
    items:[]
  };
}
function loadSettings(){
  try { 
    return JSON.parse(localStorage.getItem(CFG_KEY)) || defaultSettings();
  } catch { 
    return defaultSettings();
  }
}
function saveSettings(s){
  try { localStorage.setItem(CFG_KEY, JSON.stringify(s)); } catch {}
}
const currency = () => loadSettings().currency || "VND";
const fmtMoney = (n) => Number(n || 0).toLocaleString() + " " + currency();

/* ========= Helpers ========= */
const delay = (ms) => new Promise(r => setTimeout(r, ms));

async function fetchWithBackoff(url, options, { maxRetries = 3, baseDelay = 700 } = {}) {
  let attempt = 0, switched = false;
  while (true) {
    const res = await fetch(url, options);
    const text = await res.text();
    let data;
    try { data = JSON.parse(text); } catch { data = { raw: text }; }
    if (res.ok) return data;

    const status = res.status;
    const msg = data?.error?.message || `HTTP ${status}`;
    const overload = status === 429 || status === 503 || /overloaded/i.test(msg);
    const notFoundOrUnsupported = status === 404 || /not found|not supported/i.test(msg);

    if (url.includes("generativelanguage") && (notFoundOrUnsupported || overload) && !switched && MODEL !== FALLBACK_MODEL) {
      MODEL = FALLBACK_MODEL;
      switched = true;
      await delay(250);
      continue;
    }
    if (overload && attempt < maxRetries) {
      attempt++;
      const wait = baseDelay * Math.pow(2, attempt - 1) + Math.floor(Math.random()*250);
      await delay(wait);
      continue;
    }
    const err = new Error(msg);
    err.status = status;
    err.payload = data;
    throw err;
  }
}

/* ========= Parse AI text -> [{name, qty}] ========= */
function parseItemsFromAiText(text) {
  if (!text) return [];
  const lines = text.split(/\r?\n/);
  const results = [];
  const unitWords = "(?:cái|bộ|chiếc|thùng carton|thùng|kg|m3|m²|m|bức|tấm|cây|cuộn|ghế|bàn|thanh|kiện|bao|túi|con|quyển|cuốn)";
  const qtyRegex = new RegExp(`(\\d+[\\d.,]*)\\s*${unitWords}\\b`, "i");
  for (let raw of lines) {
    let line = (raw || "").trim().replace(/^[-•*]\s*/, "");
    if (!line) continue;
    const lower = line.toLowerCase();
    if (
      lower.startsWith("chào ") ||
      lower.includes("đây có phải") ||
      lower.includes("hãy gửi") ||
      lower.includes("tổng tạm tính") ||
      lower.startsWith("lưu ý") ||
      lower.startsWith("ghi chú") ||
      /[\?؟]+$/.test(lower)
    ) continue;
    const m = line.match(qtyRegex);
    if (!m) continue;
    const qty = parseInt(m[1].replace(/[^\d]/g, ""), 10) || 1;
    let name = line.split(/[—:]/)[0]
      .replace(/\((.*?)\)/g, "")
      .replace(/\s+/g, " ")
      .trim();
    if (!name || /^((bạn|mình|tôi|anh|chị|bên mình|vui lòng|xin vui lòng)\b)/i.test(name)) continue;
    if (name.length < 2) continue;
    results.push({ name, qty });
  }
  return results;
}

/* ========= Cart / pricing ========= */
(function () {
  const itemsTbody = document.querySelector("#items-tbody");
  const sumQtyEl = document.querySelector("#sum-qty");
  const sumAmountEl = document.querySelector("#sum-amount");
  if (!itemsTbody || !sumQtyEl || !sumAmountEl) return;

  let items = [];
  let priceIndexExact = null;
  let priceIndexList = null;

  function buildPriceIndex() {
    const list = loadSettings().items || [];
    priceIndexExact = new Map();
    priceIndexList = [];
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
    const n = (name || "").toLowerCase().trim();
    if (!n) return 0;

    // KHÔNG có fallback giá sách / random nữa – chỉ dùng giá từ DB
    if (priceIndexExact.has(n)) return priceIndexExact.get(n);

    const escapeRegExp = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    for (const [nm, price] of priceIndexList) {
      const rx = new RegExp(`\\b${escapeRegExp(nm)}\\b`, "i");
      if (rx.test(n)) return Number(price) || 0;
    }
    return 0;
  }

  const fmt = (n) => Number(n || 0).toLocaleString() + " " + currency();

  function computeShipFee() {
    const km = Number(window?.AIQUOTE_SLOT?.data?.km || 0);
    const cfg = loadSettings();
    if (!km) return 0;
    return Math.max(cfg.minFare, Math.round(km * (cfg.pricePerKm || 0)));
  }

  function normalizeNameForCompare(name) {
    if (!name) return "";
    let t = String(name).toLowerCase().trim();
    t = t.replace(/^(?:cái|chiếc|bộ|quyển|cuốn|thùng carton|thùng|hộp|valy|vali|bao|túi)\s+/i, "");
    t = t.replace(/\s+/g, " ");
    return t;
  }

  function render() {
    itemsTbody.innerHTML = "";

    if (!items.length) {
      const tr = document.createElement("tr");
      tr.className = "empty-row";
      tr.innerHTML = `
        <td colspan="5" class="text-muted text-center py-3">
          Chưa có sản phẩm. Gửi ảnh để AI nhận diện hoặc thêm thủ công.
        </td>`;
      itemsTbody.appendChild(tr);
    } else {
      for (const it of items) {
        const hasPrice = Number(it.price) > 0;
        const priceHtml = hasPrice
          ? `${fmt(it.price)}`
          : `<span class="text-muted font-italic">báo giá sau</span>`;
        const subHtml = hasPrice
          ? `Tạm tính: ${fmt(it.price * it.qty)}`
          : `<span class="text-muted">Tạm tính: —</span>`;

        let weightExtraHtml = "";
        const rule = findWeightRuleForName(it.name);
        if (rule) {
          const qty = Number(it.qty) || 0;
          const totalKg = qty * rule.kgPerUnit;
          weightExtraHtml = `<br><small class="text-muted">≈ ${totalKg.toFixed(1)} kg</small>`;
        }

        const { base, size } = splitNameAndSize(it.name);

        const tr = document.createElement("tr");
        tr.dataset.id = it.id;
        tr.innerHTML = `
          <td>${escapeHTML(base)}</td>
          <td class="text-center">
            ${size
              ? `<span class="badge badge-light">${escapeHTML(size)}</span>`
              : `<span class="text-muted">—</span>`}
          </td>
          <td class="text-center">
            <div class="qty-group">
              <button class="btn-minus" type="button" aria-label="Giảm">−</button>
              <input class="qty-input" value="${it.qty}" inputmode="numeric">
              <button class="btn-plus" type="button" aria-label="Tăng">+</button>
            </div>
          </td>
          <td class="text-right">
            <div>${priceHtml}${weightExtraHtml}</div>
            <small class="text-muted">${subHtml}</small>
          </td>
          <td class="text-right">
            <button class="btn btn-sm btn-outline-danger btn-del" title="Xoá">
              <i class="fas fa-trash"></i>
            </button>
          </td>`;
        itemsTbody.appendChild(tr);
      }
    }

    const totalQty = items.reduce((s,i)=>s+Number(i.qty||0),0);
    const itemsAmount = items
      .filter(i => Number(i.price) > 0)
      .reduce((s,i)=>s+Number(i.qty||0)*Number(i.price||0),0);

    const shipFee    = computeShipFee();
    const grandTotal = itemsAmount + shipFee;

    sumQtyEl.textContent = String(totalQty);
    sumAmountEl.textContent = Number(grandTotal).toLocaleString() + " " + currency();
  }

  function setItems(list) {
    items = (list || []).map(it => {
      const name = String(it.name || "").trim();
      const cleanQty = Math.max(1, Number(String(it.qty || 1).replace(/[^\d]/g, "")) || 1);
      return {
        id: "i_" + Math.random().toString(36).slice(2, 9),
        name,
        price: lookupPrice(name),
        qty: cleanQty
      };
    }).filter(it => it.name);
    render();
    window.AIQUOTE.rerender = render;
  }

  function appendItemsFromAi(list) {
    if (!Array.isArray(list) || !list.length) return;
    for (const it of list) {
      const name = String(it?.name || "").trim();
      const qty = Math.max(1, Number(String(it?.qty || 1).replace(/[^\d]/g,"")) || 1);
      if (!name) continue;
      const idx = findIndexByName(name);
      if (idx >= 0) {
        items[idx].qty = Number(items[idx].qty || 0) + qty;
      } else {
        items.push({
          id: "i_" + Math.random().toString(36).slice(2,9),
          name,
          price: lookupPrice(name),
          qty
        });
      }
    }
    render();
  }

  function findIndexByName(name) {
    const keyNorm = normalizeNameForCompare(name);
    if (!keyNorm) return -1;
    let idx = items.findIndex(x => normalizeNameForCompare(x.name) === keyNorm);
    if (idx >= 0) return idx;
    idx = items.findIndex(x => normalizeNameForCompare(x.name).includes(keyNorm));
    if (idx >= 0) return idx;
    idx = items.findIndex(x => keyNorm.includes(normalizeNameForCompare(x.name)));
    return idx;
  }

  function removeItemByName(name) {
    const idx = findIndexByName(name);
    if (idx >= 0) {
      items.splice(idx, 1);
      render();
      return true;
    }
    return false;
  }

  function decrementItemByName(name, qty = 1) {
    const idx = findIndexByName(name);
    if (idx < 0) return false;
    items[idx].qty = Math.max(0, Number(items[idx].qty || 0) - Number(qty || 1));
    if (items[idx].qty === 0) items.splice(idx, 1);
    render();
    return true;
  }

  function upsertItem(name, qty = 1) {
    const cleanName = String(name || "").trim();
    const cleanQty = Math.max(1, Number(qty) || 1);
    if (!cleanName) return false;
    const idx = findIndexByName(cleanName);
    if (idx >= 0) {
      items[idx].qty = Number(items[idx].qty || 0) + cleanQty;
    } else {
      items.push({
        id: "i_" + Math.random().toString(36).slice(2, 9),
        name: cleanName,
        price: lookupPrice(cleanName),
        qty: cleanQty
      });
    }
    render();
    return true;
  }

  window.AIQUOTE = window.AIQUOTE || {};
  window.AIQUOTE.rerender = render; 
  window.AIQUOTE.setItems = setItems;
  window.AIQUOTE.appendItemsFromAi = appendItemsFromAi;
  window.AIQUOTE.upsertItem = upsertItem;
  window.AIQUOTE.removeItemByName = removeItemByName;
  window.AIQUOTE.decrementItemByName = decrementItemByName;
  window.AIQUOTE.getTotals = () => {
    const qty = items.reduce((s,i)=>s+Number(i.qty||0),0);
    const amount = items
      .filter(i=>Number(i.price)>0)
      .reduce((s,i)=>s+Number(i.qty||0)*Number(i.price||0),0);
    return { qty, amount };
  };
  window.AIQUOTE.getWeightSummary = () => {
    const summary = [];
    for (const it of items) {
      const rule = findWeightRuleForName(it.name);
      if (!rule) continue;
      const qty = Number(it.qty) || 0;
      const kg = qty * rule.kgPerUnit;
      summary.push({ name: it.name, qty, kg });
    }
    return summary;
  };

  itemsTbody.addEventListener("click",(e)=>{
    const tr = e.target.closest("tr");
    if (!tr) return;
    const id = tr.dataset.id;
    const it = id && items.find(x => x.id === id);
    if (!it) return;
    if (e.target.closest(".btn-plus"))  {
      it.qty = Number(it.qty) + 1;
      render();
    }
    if (e.target.closest(".btn-minus")) {
      it.qty = Math.max(1, Number(it.qty) - 1);
      render();
    }
    if (e.target.closest(".btn-del")) {
      const ok = confirm(`Bạn có muốn xoá "${it.name}" khỏi danh sách không?`);
      if (ok) {
        items = items.filter(x => x.id !== id);
        render();
      }
    }
  });

  itemsTbody.addEventListener("input",(e)=>{
    if (!e.target.classList.contains("qty-input")) return;
    const tr = e.target.closest("tr");
    const id = tr?.dataset?.id;
    const it = items.find(x => x.id === id);
    if (!it) return;
    const v = parseInt(e.target.value.replace(/[^\d]/g,"") || "1", 10);
    it.qty = Math.max(1, v);
    render();
  });

  render();

  window.AIQUOTE.exportItems = () =>
    items.map(it => ({ name: it.name, qty: Number(it.qty||0), price: Number(it.price||0) }));
})();

/* ========= Parse lệnh thêm/xoá ========= */
const VI_NUM = {
  "một":1,"hai":2,"ba":3,"bốn":4,"tư":4,"năm":5,"lăm":5,
  "sáu":6,"bảy":7,"bẩy":7,"tám":8,"chín":9,"mười":10
};
const toNum = (s) => {
  const n = parseInt(String(s||"").replace(/[^\d]/g,""),10);
  if (!isNaN(n)) return n;
  const w = String(s||"").toLowerCase().trim();
  return VI_NUM[w] ?? NaN;
};
const LEADING_GARBAGE_RX = /^(?:cho|giúp|giup|làm|lam|thêm giúp|thêm giùm|please|pls)\s+(?:tôi|em|anh|chị|minh|mình)\s*/i;
const TRAILING_GARBAGE_RX = /\s*(giúp với|giúp em|giúp anh|giúp chị|giùm|dùm|với|nhé|nha|ạ|ạ nhé|cảm ơn|thanks|thank you)\s*$/i;

function parseUserAddCommand(text) {
  const results = [];
  if (!text) return results;
  const t = text.trim();
  if (/(\d+)\s*\+\s*(\d+)/.test(t) || /[=≈]/.test(t)) return results;

  const rePlus = /(?:^|,\s*)\+\s*(\d+)\s+(.*?)(?=(?:\s*,|$))/gi;
  let mp;
  while ((mp = rePlus.exec(t))) {
    const qty = Math.max(1, parseInt(mp[1], 10));
    let name = (mp[2] || "")
      .trim()
      .replace(/[.]+$/,"")
      .replace(LEADING_GARBAGE_RX,"")
      .replace(TRAILING_GARBAGE_RX,"")
      .trim();
    if (name) results.push({ name, qty });
  }
  if (results.length) return results;

  const verbRx = /\b(thêm vào|thêm|cộng|add)\b/i;
  if (!verbRx.test(t)) return results;
  const afterVerb = t.split(verbRx).pop();
  const chunks = afterVerb
    .split(/\s*(?:,|và)\s*/i)
    .map(s=>s.trim())
    .filter(Boolean);
  const unitRx = "(?:cái|bộ|chiếc|ghế|bàn|giường|tủ|thùng carton|thùng|kg|m3|m|tấm|bức|cuộn|kiện|bao|túi|con|quyển|cuốn)?";

  for (let c of chunks) {
    c = c.replace(LEADING_GARBAGE_RX,"").trim();
    let m = c.match(new RegExp(
      `^(\\d+|một|hai|ba|bốn|tư|năm|lăm|sáu|bảy|bẩy|tám|chín|mười)\\s*${unitRx}\\s+(.+)$`,
      "i"
    ));
    if (!m) {
      const m2 = c.match(new RegExp(
        `.*?(\\d+|một|hai|ba|bốn|tư|năm|lăm|sáu|bảy|bẩy|tám|chín|mười)\\s*${unitRx}\\s+(.+)$`,
        "i"
      ));
      if (m2) m = m2;
    }
    if (m) {
      const qty = toNum(m[1]);
      let name = (m[2]||"")
        .trim()
        .replace(/[.]+$/,"")
        .replace(TRAILING_GARBAGE_RX,"")
        .trim();
      if (!isNaN(qty) && qty>0 && name) {
        results.push({ name, qty });
        continue;
      }
    }
    if (!/^(?:cho|giúp|giup)\b/i.test(c)) {
      const name = c
        .replace(/[.]+$/,"")
        .replace(TRAILING_GARBAGE_RX,"")
        .trim();
      if (name && !/\d/.test(name)) {
        results.push({ name, qty:1 });
      }
    }
  }
  return results;
}

function parseUserRemoveCommand(text) {
  const results = [];
  if (!text) return results;
  const t = text.trim();

  const reMinus = /(?:^|,\s*)-\s*(\d+)\s+([^\,-]+?)(?=(?:\s*,|$))/g;
  let m;
  while ((m = reMinus.exec(t))) {
    const qty = Math.max(1, parseInt(m[1], 10));
    const name = m[2]
      .trim()
      .replace(/[.]+$/,"")
      .replace(TRAILING_GARBAGE_RX,"")
      .trim();
    if (name) results.push({ name, qty, mode: "decrement" });
  }

  const hasVerb = /\b(xoá|xóa|bớt|trừ|giảm|bỏ|delete|remove)\b/i.test(t);
  if (hasVerb) {
    t.split(/\b(?:và|,)\b/i).forEach(p=>{
      const mm = p.match(
        /\b(xoá|xóa|bớt|trừ|giảm|bỏ|delete|remove)\b\s*(\d+|một|hai|ba|bốn|tư|năm|lăm|sáu|bảy|bẩy|tám|chín|mười)?\s*(?:cái|bộ|chiếc|ghế|bàn|thùng carton|thùng|kg|m3|m|tấm|bức|cuộn|kiện|bao|túi|con|quyển|cuốn)?\s+(.+)/i
      );
      if (mm) {
        const qty = mm[2] ? toNum(mm[2]) : null;
        const name = mm[3]
          .trim()
          .replace(/[.]+$/,"")
          .replace(TRAILING_GARBAGE_RX,"")
          .trim();
        if (name) {
          results.push({ name, qty: qty ?? 0, mode: (qty ? "decrement" : "remove") });
        }
      }
    });
    if (!results.length) {
      const mm = t.match(/\b(xoá|xóa|bỏ|delete|remove)\b\s+(.+)/i);
      if (mm) {
        const name = mm[2]
          .trim()
          .replace(/[.]+$/,"")
          .replace(TRAILING_GARBAGE_RX,"")
          .trim();
        if (name) {
          results.push({ name, qty: 0, mode: "remove" });
        }
      }
    }
  }
  return results;
}

/* ========= Intent ========= */
function detectShippingIntent(text) {
  if (!text) return false;
  const t = text.toLowerCase();
  return /\b(chuyển|chuyen|vận chuyển|van chuyen|chuyển nhà|ship|gửi hàng|gui hang|gui do)\b/.test(t);
}
function parseItemsFromShippingSentence(text) {
  const results = [];
  if (!text) return results;
  const unitRx = "(?:cái|bộ|chiếc|ghế|bàn|giường|tủ|thùng carton|thùng|kg|m3|m|tấm|bức|cuộn|kiện|bao|túi|con|quyển|cuốn)?";
  const re = new RegExp(
    `(\\d+|một|hai|ba|bốn|tư|năm|lăm|sáu|bảy|bẩy|tám|chín|mười)\\s*${unitRx}\\s+(.+?)(?:\\s+(?:đi|tới|đến|ra|sang)\\b|$)`,
    "i"
  );
  const m = text.match(re);
  if (m) {
    const qty = toNum(m[1]);
    let name = (m[2] || "").trim().replace(/[.]+$/, "");
    if (!isNaN(qty) && qty > 0 && name) {
      results.push({ name, qty });
    }
  }
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

/* ========= Autofill slot từ câu dài ========= */
function autofillSlotFromFreeText(text) {
  if (!text) return;
  const raw = text.trim();

  if (!SLOT.data.name) {
    const nameGuess = extractInStep("name", raw);
    if (nameGuess && nameGuess.length >= 2 && nameGuess.length <= 80 && !/\d{3,}/.test(nameGuess)) {
      SLOT.data.name = nameGuess.trim();
    }
  }

  if (!SLOT.data.phone) {
    const phone = normalizePhone(raw);
    if (phone) SLOT.data.phone = phone;
  }

  if (!SLOT.data.fromAddr || !SLOT.data.toAddr) {
    const t = raw.toLowerCase();
    const m = t.match(/\btừ\s+(.+?)\s+(?:đến|tới)\s+(.+)/i);
    if (m) {
      const fromTxt = text.slice(m.index).match(/\btừ\s+(.+?)\s+(?:đến|tới)\s+(.+)/i);
      if (fromTxt) {
        if (!SLOT.data.fromAddr) SLOT.data.fromAddr = fromTxt[1].trim();
        if (!SLOT.data.toAddr) SLOT.data.toAddr = fromTxt[2].trim();
      }
    } else {
      if (!SLOT.data.toAddr) {
        const m2 = raw.match(/\b(?:đến|tới)\s+(.+)/i);
        if (m2) {
          const idx = m2.index;
          const toFull = text.slice(idx).match(/\b(?:đến|tới)\s+(.+)/i);
          if (toFull) SLOT.data.toAddr = toFull[1].trim();
        }
      }
    }
  }

  const existingDate = SLOT.data.date;
  const existingTime = SLOT.data.time;
  const dateParsed = parseFlexibleDate(raw);
  const timeParsed = parseTimeFromText(raw);

  if (!existingDate && dateParsed) {
    const dt = dateParsed.dt;
    SLOT.data._dateObj = dt;
    SLOT.data.date = formatDateOnlyVN(dt);
  }

  if (!existingTime && timeParsed) {
    const baseDate = SLOT.data._dateObj || (dateParsed && dateParsed.dt);
    if (baseDate) {
      const dt = new Date(baseDate.getTime());
      dt.setHours(timeParsed.hour, timeParsed.minute, 0, 0);
      SLOT.data.time = formatTimeVN(timeParsed.hour, timeParsed.minute);
      SLOT.data.datetime = formatDateTimeVN(dt);
      SLOT.data._datetimeObj = dt;
    } else {
      SLOT.data.time = formatTimeVN(timeParsed.hour, timeParsed.minute);
      SLOT.data._datetimeObj = null;
    }
  }
}

/* ========= AI call ========= */
async function generateBotResponse(incomingMessageDiv, userText, opts = {}) {
  const { allowAutofill = false } = opts;
  const messageElement = incomingMessageDiv.querySelector(".message-text");
  const parts = [{ text: buildPromptText(userText) }];
  userUploads.forEach(img =>
    parts.push({ inline_data: { data: img.data, mime_type: img.mime_type } })
  );
  chatHistory.push({ role: "user", parts });

  const requestOptions = {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      contents: chatHistory.slice(-8),
      generationConfig: {
        temperature: 0.1,
        topK: 40,
        topP: 0.9,
        candidateCount: 1,
        maxOutputTokens: 1024
      }
    })
  };

  try {
    const data = await fetchWithBackoff(buildApiUrl(), requestOptions, {
      maxRetries: 3,
      baseDelay: 700
    });
    const apiText =
      data?.candidates?.[0]?.content?.parts?.[0]?.text
        ?.replace(/\*\*(.*?)\*\*/g, "$1")
        .trim() ||
      "Mình chưa đọc được nội dung, vui lòng thử lại.";
    if (messageElement) messageElement.innerText = apiText;
    chatHistory.push({ role: "model", parts: [{ text: apiText }] });

    const parsed = parseItemsFromAiText(apiText);
    if (allowAutofill && parsed.length) {
      if (window.AIQUOTE?.appendItemsFromAi) {
        window.AIQUOTE.appendItemsFromAi(parsed);
      }
    }
    if (allowAutofill) {
      showConfirmCTA();
    }
  } catch (error) {
    console.error(error);
    if (messageElement) {
      messageElement.innerText = error.message || "Có lỗi khi gọi AI.";
      messageElement.style.color = "#ff0000";
    }
  } finally {
    userUploads.splice(0, userUploads.length);
    renderUploadPreview();
    incomingMessageDiv.classList.remove("thinking");
    if (chatBody) {
      chatBody.scrollTo({ top: chatBody.scrollHeight, behavior: "smooth" });
    }
  }
}

/* ========= Tính toán + in 1 bảng tổng hợp ========= */
async function renderFinalCombinedSummary() {
  const d = SLOT.data;

  if (!d.fromPlace || !d.toPlace) {
    renderSlotReply("Mình chưa đủ địa chỉ lấy hàng và giao hàng để tính quãng đường. Bạn kiểm tra lại giúp mình nhé.");
    return;
  }

  if (!d.km) {
    const dist = await calcDistance(d.fromPlace, d.toPlace);
    if (!dist.ok) {
      renderSlotReply("Mình chưa tính được quãng đường giữa 2 địa chỉ. Bạn kiểm tra lại địa chỉ giúp mình nhé.");
      return;
    }
    d.km = dist.km;
    d.durationText = dist.durationText;
    d.routeText = dist.routeText;
    d.routeSeconds = dist.seconds || null;

    window.AIQUOTE?.rerender?.();
  }

  const { amount: itemsAmount } = (window.AIQUOTE?.getTotals?.() || { amount: 0 });
  const cfg = loadSettings();
  const shipFee = Math.max(cfg.minFare, Math.round(d.km * (cfg.pricePerKm || 0)));
  const grandTotal = itemsAmount + shipFee;

  const fromFmt = d.fromPlace?.formatted || d.fromAddr || "—";
  const toFmt   = d.toPlace?.formatted   || d.toAddr   || "—";

  let pickupText = "—";
  if (d.datetime) {
    pickupText = d.datetime;
  } else if (d._datetimeObj) {
    pickupText = formatDateTimeVN(d._datetimeObj);
  } else if (d.date || d.time) {
    pickupText = [d.date || "", d.time || ""].join(" ").trim();
  }

  let etaText = "—";
  if (d._datetimeObj && d.routeSeconds) {
    const etaDate = new Date(d._datetimeObj.getTime() + d.routeSeconds * 1000);
    etaText = formatDateTimeVN(etaDate);
  }

  const html = `
  <b>TÓM TẮT YÊU CẦU & BÁO GIÁ</b>
  <table class="table table-sm mt-2 mb-2">
    <tbody>
      <tr><td>Tên khách hàng</td><td><b>${escapeHTML(d.name || "")}</b></td></tr>
      <tr><td>Địa chỉ nhận hàng</td><td>${escapeHTML(fromFmt)}</td></tr>
      <tr><td>Địa chỉ giao hàng</td><td>${escapeHTML(toFmt)}</td></tr>
      <tr><td>Thời gian lấy hàng</td><td>${escapeHTML(pickupText || "—")}</td></tr>
      <tr><td>Quãng đường ước tính</td><td><b>${d.km.toFixed(2)} km</b></td></tr>
      <tr><td>Thời gian ước tính</td><td>${escapeHTML(d.durationText || "—")}</td></tr>
      <tr><td><b>TỔNG SỐ TIỀN</b></td><td><b>${fmtMoney(grandTotal)}</b> (gồm hàng hóa tạm tính + phí ship)</td></tr>
    </tbody>
  </table>
  <small class="text-muted d-block mb-1">SĐT liên hệ: ${escapeHTML(d.phone || "—")}</small>
  <small class="text-muted">
    Giá trên được tính theo danh sách hiện tại và quãng đường dự kiến. 
    Đây là mức giá ước tính, có thể thay đổi tùy vào nhà cung cấp, gói, phát sinh,...
  </small>
`;

  const exportedItems = (window.AIQUOTE?.exportItems?.() || []);

  const draft = {
    customer: { name: d.name || "", phone: d.phone || "" },
    pickup:   { raw: d.fromAddr || "", formatted: d.fromPlace?.formatted || "", lat: d.fromPlace?.lat, lng: d.fromPlace?.lng, parts: d.fromParts || null },
    dropoff:  { raw: d.toAddr   || "", formatted: d.toPlace?.formatted   || "", lat: d.toPlace?.lat, lng: d.toPlace?.lng, parts: d.toParts  || null },

    schedule: { date: d.date, time: d.time, datetime: d.datetime },

    route: {
      km: d.km,
      distanceKm: d.km,
      durationText: d.durationText,
      routeText: d.routeText,
      seconds: d.routeSeconds
    },

    cart: {
      totalQty: (window.AIQUOTE?.getTotals?.() || { qty:0 }).qty,
      itemsAmount: itemsAmount,
      items: exportedItems
    },

    pricing: {
      moveAmount: itemsAmount,
      shipFee: shipFee,
      grandTotal: grandTotal
    },

    itemsAmount: itemsAmount,
    shipFee: shipFee,
    grandTotal: grandTotal,

    currency: (loadSettings().currency || "VND"),
    createdAt: new Date().toISOString()
  };

  try { sessionStorage.setItem('aiquote_draft', JSON.stringify(draft)); } catch {}
  SLOT.data.draftReady = true;
  window.AIQUOTE_DRAFT_READY = true;

  renderSlotReply(html);

  renderSlotReply(
    '<i class="far fa-file-alt mr-1"></i> Hợp đồng nháp đã sẵn sàng. ' +
    'Bạn có thể bấm nút <b>Xác nhận vận chuyển</b> ở khung bên phải để xem chi tiết và in/chia sẻ.'
  );
  document.querySelector('#btn-confirm-shipment')?.classList.add('ready');
}

/* ========= Prompt build ========= */
function buildPromptText(userText) {
  const s = loadSettings();
  const priceLines = (s.items || [])
    .slice(0, 200)
    .map(it => `- ${it.name}: ${Number(it.price).toLocaleString()} ${s.currency}`)
    .join("\n");
  return s.basePrompt +
    "\n\nBảng đơn giá tham khảo (một phần):\n" +
    priceLines +
    "\n\nHãy liệt kê hạng mục theo định dạng đã yêu cầu.\n\n" +
    (userText || "");
}

/* ========= Upload preview ========= */
function renderUploadPreview() {
  if (!fileUploadWrapper) return;
  const grid = fileUploadWrapper.querySelector(".thumb-grid");
  if (!grid) return;
  grid.innerHTML = "";
  userUploads.forEach((u, idx) => {
    const item = document.createElement("div");
    item.className = "thumb";
    item.innerHTML = `
      <img src="${u.previewUrl}" alt="upload ${idx + 1}">
      <button type="button" class="thumb-remove" data-idx="${idx}" title="Xoá ảnh">&times;</button>`;
    grid.appendChild(item);
  });
  fileUploadWrapper.classList.toggle("file-uploaded", userUploads.length > 0);
}
if (fileInput) {
  fileInput.multiple = true;
  fileInput.addEventListener("change", () => {
    const files = Array.from(fileInput.files || []);
    fileInput.value = "";
    if (!files.length) return;
    if (userUploads.length + files.length > 10) {
      alert("Bạn chỉ có thể tải lên tối đa 10 ảnh.");
      return;
    }
    const images = files.filter(f => /^image\//i.test(f.type));
    if (images.length !== files.length) {
      alert("Một số tệp không phải hình ảnh nên đã bị bỏ qua.");
    }
    Promise.all(
      images.map(file => new Promise(resolve => {
        const reader = new FileReader();
        reader.onload = e => {
          const previewUrl = e.target.result;
          const base64 = previewUrl.split(",")[1];
          userUploads.push({ data: base64, mime_type: file.type, previewUrl });
          resolve();
        };
        reader.readAsDataURL(file);
      }))
    ).then(renderUploadPreview);
  });
}
if (fileUploadWrapper) {
  fileUploadWrapper.addEventListener("click", (e) => {
    const btn = e.target.closest(".thumb-remove");
    if (!btn) return;
    const idx = +btn.getAttribute("data-idx");
    if (idx >= 0) {
      userUploads.splice(idx, 1);
      renderUploadPreview();
    }
  });
}
if (fileCancelButton) {
  fileCancelButton.addEventListener("click", () => {
    userUploads.splice(0, userUploads.length);
    renderUploadPreview();
  });
}

/* ========= Handle send ========= */
function handleOutgoingMessage(e) {
  e.preventDefault();
  const text = (messageInput && messageInput.value.trim()) || "";
  if (!text && userUploads.length === 0) return;
  if (messageInput) {
    messageInput.value = "";
    messageInput.dispatchEvent(new Event("input"));
  }
  if (!chatBody) return;

  detectAndApplyCorrections(text);

  if (SLOT.mode !== "collect") {
    autofillSlotFromFreeText(text);
  }

  const content = `
    <div class="message-text"></div>
    ${
      userUploads.length
        ? `<div class="attachment-strip">${userUploads
            .map(u => `<img src="${u.previewUrl}" class="attachment">`)
            .join("")}</div>`
        : ""
    }`;

  const outgoing = createMessageElement(content, "user-message");
  outgoing.querySelector(".message-text").innerText =
    text || "(ảnh đính kèm)";
  chatBody.appendChild(outgoing);
  chatBody.scrollTo({ top: chatBody.scrollHeight, behavior: "smooth" });

  if (SLOT.mode === "collect") {
    const key = nextMissingKey();
    if (key) {
      const raw = extractInStep(key, text);

      if (key === "date") {
        const parsed = parseFlexibleDate(raw) || parseFlexibleDate(text);
        if (!parsed) {
          renderSlotReply(
            'Mình chưa đọc được <b>ngày</b>. Bạn nhập <b>dd/mm/yyyy</b> (vd: 12/11/2025), hoặc "ngày mai", "ngày 28 tháng này"...'
          );
          return;
        }
        const { dt } = parsed;
        const now = new Date();
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0,0,0,0);
        const limit = new Date(today.getTime());
        limit.setMonth(limit.getMonth() + 1);
        if (dt.getTime() < today.getTime()) {
          renderSlotReply("Ngày bạn chọn đang ở <b>trong quá khứ</b>. Chọn ngày từ hôm nay trở đi giúp mình.");
          return;
        }
        if (dt.getTime() > limit.getTime()) {
          renderSlotReply("Chỉ nhận lịch trong vòng <b>1 tháng</b> tới.");
          return;
        }
        SLOT.data._dateObj = dt;
        SLOT.data.date = formatDateOnlyVN(dt);

        const timeParsedInline = parseTimeFromText(text);
        if (timeParsedInline && !SLOT.data.time) {
          const dtTime = new Date(dt.getTime());
          dtTime.setHours(timeParsedInline.hour, timeParsedInline.minute, 0, 0);
          const now2 = new Date();
          const limit2 = new Date(now2.getTime());
          limit2.setMonth(limit2.getMonth() + 1);
          if (dtTime.getTime() < now2.getTime()) {
            renderSlotReply("Giờ bạn chọn ở <b>trước thời điểm hiện tại</b>. Chọn giờ muộn hơn nhé.");
            return;
          }
          if (dtTime.getTime() > limit2.getTime()) {
            renderSlotReply("Chỉ nhận lịch trong vòng 1 tháng tới.");
            return;
          }
          SLOT.data.time = formatTimeVN(timeParsedInline.hour, timeParsedInline.minute);
          SLOT.data.datetime = formatDateTimeVN(dtTime);
        }

        ASKING_DATE = false;
        updateSlotDateBarVisibility();

      } else if (key === "time") {
        const parsedTime = parseTimeFromText(raw) || parseTimeFromText(text);
        if (!parsedTime) {
          renderSlotReply(
            'Mình chưa đọc được <b>giờ</b>. Bạn nhập như: "9h", "9:15", "9h kém 5", "5h chiều", "12 giờ rưỡi",...'
          );
          return;
        }
        const baseDate = SLOT.data._dateObj;
        if (!baseDate) {
          renderSlotReply("Mình chưa có <b>ngày</b>. Bạn nhập ngày (dd/mm/yyyy) trước nhé.");
          SLOT.data.date = null;
          SLOT.data._dateObj = null;
          ASKING_DATE = true;
          updateSlotDateBarVisibility();
          return;
        }
        const dt = new Date(baseDate.getTime());
        dt.setHours(parsedTime.hour, parsedTime.minute, 0, 0);
        const now = new Date();
        const limit = new Date(now.getTime());
        limit.setMonth(limit.getMonth() + 1);
        if (dt.getTime() < now.getTime()) {
          renderSlotReply("Giờ bạn chọn ở <b>trước thời điểm hiện tại</b>. Chọn giờ muộn hơn nhé.");
          return;
        }
        if (dt.getTime() > limit.getTime()) {
          renderSlotReply("Chỉ nhận lịch trong vòng 1 tháng tới.");
          return;
        }
        SLOT.data.time = formatTimeVN(parsedTime.hour, parsedTime.minute);
        SLOT.data.datetime = formatDateTimeVN(dt);

      } else if (key === "phone") {
        const phone = normalizePhone(raw || text);
        if (!phone) {
          renderSlotReply("Số điện thoại chưa hợp lệ. Ví dụ: 0912345678 hoặc +84 912345678.");
          return;
        }
        SLOT.data.phone = phone;

      } else if (key === "fromAddr" || key === "toAddr") {
        const addr = raw || text;
        if (!addr || addr.length < 4) {
          renderSlotReply("Bạn nhập địa chỉ rõ hơn giúp mình nhé (có số/đường/thôn/xã...).");
          return;
        }
        renderSlotReply("Đang kiểm tra địa chỉ trên bản đồ…");
        resolveAddressWithFallback(addr).then(g => {
          if (!g.ok) {
            if (g.reason === "out_of_area") {
              renderSlotReply(
                "Hiện tại hệ thống chỉ hỗ trợ địa chỉ trong <b>Hà Nội</b>. " +
                "Bạn nhập lại giúp mình một địa chỉ thuộc Hà Nội nhé."
              );
            } else {
              renderSlotReply(
                "Địa chỉ chưa tìm thấy trên bản đồ. " +
                "Bạn mô tả chi tiết hơn (số nhà, thôn/xã/phường, huyện/quận, tỉnh/thành) nhé."
              );
            }
            return;
          }
          SLOT.data[key] = addr.trim();
          const placeKey = key === "fromAddr" ? "fromPlace" : "toPlace";
          const partsKey = key === "fromAddr" ? "fromParts" : "toParts";
          SLOT.data[placeKey] = { formatted: g.formatted, lat: g.lat, lng: g.lng };
          SLOT.data[partsKey] = g.parts || null;

          if (g.missingHouse) {
            renderSlotReply(
              `Mình chưa định vị chính xác được số nhà, nên sẽ lấy khu vực gần: <b>${escapeHTML(g.formatted)}</b>.<br>` +
              `Khi đến nơi tài xế sẽ gọi bạn để xác nhận chi tiết địa chỉ.`
            );
          } else {
            renderSlotReply(
              `${key === "fromAddr" ? "Địa chỉ lấy hàng" : "Địa chỉ giao hàng"} đã xác thực: ` +
              `<b>${escapeHTML(g.formatted)}</b>`
            );
          }
          const nextKey2 = nextMissingKey();
          if (nextKey2) askSlotQuestion(nextKey2);
          else renderFinalCombinedSummary();
        });
        return;
      } else if (key === "name") {
        const val = extractInStep(key, text);
        if (!val || val.length < 2) {
          renderSlotReply("Mình chưa rõ tên bạn. Bạn ghi lại giúp mình <b>HỌ TÊN đầy đủ</b> được không?");
          return;
        }
        SLOT.data.name = val.trim();
      }

      const nextKey3 = nextMissingKey();
      if (nextKey3) {
        askSlotQuestion(nextKey3);
      } else {
        ASKING_DATE = false;
        updateSlotDateBarVisibility();
        renderFinalCombinedSummary();
      }
    }
    return;
  }

  const toAdd = parseUserAddCommand(text);
  const toRemove = parseUserRemoveCommand(text);
  const hasShippingIntent = detectShippingIntent(text);
  const intentItems = (!toAdd.length && hasShippingIntent)
    ? parseItemsFromShippingSentence(text)
    : [];

  if (toAdd.length && window.AIQUOTE?.upsertItem) {
    toAdd.forEach(it => window.AIQUOTE.upsertItem(it.name, it.qty));
  }
  if (toRemove.length && (window.AIQUOTE?.decrementItemByName || window.AIQUOTE?.removeItemByName)) {
    toRemove.forEach(it => {
      if (it.mode === "decrement" && it.qty > 0 && window.AIQUOTE.decrementItemByName) {
        window.AIQUOTE.decrementItemByName(it.name, it.qty);
      } else if (window.AIQUOTE.removeItemByName) {
        window.AIQUOTE.removeItemByName(it.name);
      }
    });
  }
  if (intentItems.length && window.AIQUOTE?.upsertItem) {
    intentItems.forEach(it => window.AIQUOTE.upsertItem(it.name, it.qty));
  }

  if ((toAdd.length || toRemove.length || hasShippingIntent) && userUploads.length === 0) {
    showConfirmCTA();
    if (hasShippingIntent && !toAdd.length && !toRemove.length && !intentItems.length) {
      renderSlotReply(
        'Mình đã ghi nhận nhu cầu vận chuyển của bạn. Bạn có thể mô tả thêm đồ đạc (vd: "thêm 1 cái tủ, 2 cái ghế") hoặc bấm <b>Xác nhận vận chuyển</b> để mình hỏi thông tin giao nhận.'
      );
    }
    return;
  }

  if (!userUploads.length) {
    if (isSmallTalkOrGreeting(text)) {
      renderSlotReply(
        'Chào bạn 👋 Mình là trợ lý hỗ trợ <b>vận chuyển</b>. Bạn cho mình biết bạn đang muốn chuyển những đồ gì hoặc từ đâu đến đâu nhé (vd: "chuyển 1 cái giường từ Cầu Giấy lên Hoà Lạc" hoặc "thêm 3 thùng carton").'
      );
    } else {
      renderSlotReply(
        DOMAIN_ONLY_MESSAGE +
          ' Ví dụ: bạn có thể nói <br>• "chuyển 1 cái giường từ Cầu Giấy lên Hoà Lạc"<br>• hoặc "thêm 2 thùng carton, bớt 1 cái tủ"...'
      );
    }
    return;
  }

  setTimeout(() => {
    if (!chatBody) return;
    const botContent = `
      <svg class="bot-avatar" xmlns="http://www.w3.org/2000/svg" width="50" height="50" viewBox="0 0 1024 1024">
        <path d="M738.3 287.6H285.7c-59 0-106.8 47.8-106.8 106.8v303.1c0 59 47.8 106.8 106.8 106.8h81.5v111.1c0 .7.8 1.1 1.4.7l166.9-110.6 41.8-.8h117.4l43.6-.4c59 0 106.8-47.8 106.8-106.8V394.5c0-59-47.8-106.9-106.8-106.9z"/>
      </svg>
      <div class="message-text">
        <div class="thinking-indicator">
          <div class="dot"></div>
          <div class="dot"></div>
          <div class="dot"></div>
        </div>
      </div>`;
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
    if (
      e.key === "Enter" &&
      !e.shiftKey &&
      (userUploads.length || userText) &&
      window.innerWidth > 768
    ) {
      handleOutgoingMessage(e);
    }
  });
}
if (sendMessage) {
  sendMessage.addEventListener("click", (e) => handleOutgoingMessage(e));
}

/* ========= Bootstrap products (lấy trực tiếp từ DB) ========= */
(async function bootstrapProducts() {
  const s = loadSettings();
  let loaded = [];
  try {
    const r = await fetch(PRODUCTS_URL, { cache: "no-store" });
    if (r.ok) {
      loaded = await r.json();
    } else {
      console.warn("Load products_price API failed, status =", r.status);
    }
  } catch (e) {
    console.warn("Load products_price API error:", e);
  }

  const normalized = [];
  const seen = new Set();

  for (const it of loaded || []) {
    const name = String(it && it.name ? it.name : "").trim();
    if (!name) continue;
    const key = name.toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);

    const price = Number(it.price != null ? it.price : it.unitPrice) || 0;
    normalized.push({ name, price });
  }

  s.items = normalized;
  saveSettings(s);
})();

/* ========= CTA click ========= */
if (chatBody) {
  chatBody.addEventListener("click", (e) => {
    const btn = e.target.closest(".btn-chat-confirm");
    if (!btn) return;
    clearChatCTA();
    SLOT.mode = "collect";
    SLOT.step = 0;
    renderSlotReply("Cảm ơn bạn đã xác nhận. Mình sẽ hỏi vài thông tin để tạo Hợp đồng nháp.");
    const k = nextMissingKey() || "name";
    askSlotQuestion(k);
  });
}

/* ========= Wire nút bên card phải ========= */
document.querySelector("#btn-confirm-shipment")?.addEventListener("click", () => {
  if (isDraftReady()) {
    const url = (typeof CONTRACT_URL !== "undefined" && CONTRACT_URL) ? CONTRACT_URL : "/contract";
    window.location.href = url;
    return;
  }

  if (!hasAtLeastOneItem()) {
    renderSlotReply("Bạn chưa có hạng mục nào trong danh sách. Gửi ảnh để AI nhận diện hoặc thêm thủ công nhé.");
    return;
  }

  clearChatCTA();
  SLOT.mode = "collect"; 
  SLOT.step = 0;
  renderSlotReply("Cảm ơn bạn. Mình sẽ hỏi vài thông tin để tạo Hợp đồng nháp.");
  askSlotQuestion(nextMissingKey() || "name");
});

document.querySelector("#btn-open-manual")?.addEventListener("click", ()=>{
  $('#manualItemModal').modal('show');
});
document.querySelector("#mi-save")?.addEventListener("click", ()=>{
  const n = document.querySelector("#mi-name")?.value?.trim();
  const qRaw = document.querySelector("#mi-qty")?.value?.trim();
  const q = Math.max(1, parseInt((qRaw||"").replace(/[^\d]/g,""),10)||1);
  if (!n) {
    alert("Nhập tên hạng mục.");
    return;
  }
  if (window.AIQUOTE?.upsertItem) {
    window.AIQUOTE.upsertItem(n, q);
  }
  document.querySelector("#mi-name").value = "";
  document.querySelector("#mi-qty").value = "1";
  $('#manualItemModal').modal('hide');
});

/* ========= Expose for BE validation hints ========= */
window.AIQUOTE_SLOT = SLOT;
