/* ===== AI Quote – upload, chat, parse AI → right table (internal pricing) =====
 * - Chỉ đổ vào danh sách khi AI nhận diện từ ảnh (autofill = true nếu lượt gửi có ảnh)
 * - Món không có giá => hiển thị "báo giá sau", không cộng tổng tiền
 * - Bỏ toàn bộ preflight/popup key
 * - Bảng giá tách ra file JSON: /user/data/products.json
 * - Tự sinh thêm sản phẩm để đủ TARGET_COUNT (mặc định 10.000)
 */

/* ========= DOM hooks ========= */
const chatBody = document.querySelector(".chat-body");
const messageInput = document.querySelector(".message-input");
const sendMessage = document.querySelector("#send-message");
const fileInput = document.querySelector("#file-input");
const fileUploadBtn = document.querySelector("#file-upload");
const fileUploadWrapper = document.querySelector(".file-upload-wrapper");
const fileCancelButton = fileUploadWrapper ? fileUploadWrapper.querySelector("#file-cancel") : null;

/* open picker */
if (fileUploadBtn && fileInput) fileUploadBtn.addEventListener("click", () => fileInput.click());

/* ========= API (Gemini) ========= */
/* Lưu ý: ở production không để key ở FE; dùng proxy BE */
const API_KEY = "AIzaSyCQZaLPV5xXjs65vh2f8L6HlwHAn8ouSoc"; // hoặc để trống nếu gọi qua BE/proxy
let MODEL = "gemini-2.0-flash";
const FALLBACK_MODEL = "gemini-2.0-flash-lite";
const buildApiUrl = () =>
  `https://generativelanguage.googleapis.com/v1/models/${MODEL}:generateContent?key=${API_KEY}`;

/* ========= State ========= */
const chatHistory = [];
const userUploads = [];
const initialInputHeight = messageInput ? messageInput.scrollHeight : 0;

/* ========= External price list loading ========= */
const PRODUCTS_URL = "/user/data/products.json"; // <<— file riêng
const TARGET_COUNT = 10000; // tổng mục tiêu (bao gồm cả file json)
const BULKY_KEYWORDS = [
  "sofa","ghế sofa","sofa góc","giường","giường đơn","giường đôi","giường tầng",
  "tủ quần áo","tủ 3 cánh","tủ 4 cánh","tủ 5 cánh",
  "tủ bếp lớn","tủ lạnh side-by-side","tủ lạnh 4 cánh",
  "máy chạy bộ","máy tập đa năng","máy photocopy","két sắt lớn",
  "máy giặt","máy sấy","máy rửa chén","máy lạnh đứng",
  "tivi 55 inch","tivi 65 inch","tivi 75 inch",
  "bàn họp","kệ kho sắt","bàn bida","bàn bida mini","tủ thờ","bàn thờ","cây cảnh lớn"
];
const BASE_NAMES = [
  "nồi cơm điện","ấm đun siêu tốc","bếp hồng ngoại","bếp từ mini","quạt treo tường","quạt đứng","quạt hộp",
  "máy sấy tóc","bàn ủi hơi nước","máy xay thịt","máy ép trái cây","máy xay sinh tố","lò nướng mini",
  "nồi chiên không dầu","máy lọc không khí","máy hút bụi cầm tay","robot hút bụi","máy lau nhà",
  "tủ lạnh 1 cánh","tủ lạnh 2 cánh","tủ lạnh 4 cánh","tủ lạnh side-by-side","tủ đông mini","tủ mát",
  "máy lạnh treo tường","máy lạnh đứng","quạt điều hoà","tivi 32 inch","tivi 43 inch","tivi 50 inch",
  "tivi 55 inch","tivi 65 inch","loa bluetooth","loa kéo","dàn karaoke","amply","đầu DVD",
  "bàn làm việc","bàn học","bàn ăn","bàn trà","bàn gấp","bàn họp","bàn trang điểm",
  "tủ đầu giường","tủ sách","kệ sách","kệ TV","tủ quần áo","tủ 3 cánh","tủ 4 cánh","tủ thờ",
  "ghế xoay","ghế nhân viên","ghế giám đốc","tủ hồ sơ","két sắt nhỏ","két sắt lớn",
  "bộ nồi inox","bộ chén đĩa","nồi áp suất","nồi lẩu điện","chảo chống dính","bếp gas đôi","bếp gas âm",
  "máy rửa chén bàn","lò vi sóng","máy hút mùi","tủ bếp nhỏ","tủ bếp lớn","máy pha cà phê",
  "xe đạp","xe đạp địa hình","máy chạy bộ","máy tập đa năng","tạ tay","thảm yoga","máy chèo thuyền",
  "bóng rổ","bóng đá","bàn bida","bàn bida mini","ghế xếp du lịch","bàn xếp dã ngoại",
  "lều 2 người","lều 4 người","bếp nướng than","bếp nướng điện","bình nước du lịch","võng xếp","bạt che nắng",
  "kệ giày","tủ giày","kệ nhà tắm","giá phơi đồ","cây lau nhà","đèn bàn","đèn cây",
  "gương đứng","gương treo tường","tranh treo tường","đồng hồ treo tường",
  "nôi em bé","cũi em bé","xe đẩy em bé","ghế ăn dặm","bồn tắm em bé","máy hâm sữa","máy tiệt trùng bình sữa",
  "đàn organ","đàn guitar","đàn ukulele","đàn piano điện","đàn piano cơ","trống cajon","trống điện tử",
  "khoan điện","máy cưa bàn","máy cắt sắt","máy hàn mini","máy rửa xe","máy bơm nước",
  "thang nhôm 3 bậc","thang nhôm 5 bậc","thùng carton nhỏ","thùng carton lớn","thùng nhựa",
  "kệ nhựa","kệ kho sắt","tủ nhựa 5 tầng","tủ nhựa 3 tầng"
];
const SIZE_TAGS = ["mini","nhỏ","vừa","lớn","cao cấp"];
const MATERIALS = ["gỗ","gỗ sồi","gỗ thông","gỗ công nghiệp","nhựa","inox","thép","hợp kim","vải","da","da PU"];
const COLORS = ["trắng","đen","xám","nâu","be","xanh","đỏ"];

/* ========= Config & UI ========= */
const CFG_KEY = "ai_quote_cfg_v2";
function defaultSettings() {
  return {
    currency: "VND",
    basePrompt:
      "Bạn là trợ lý báo giá vận chuyển. Hãy đọc ảnh/nhắn tin của khách, đếm số lượng đồ. " +
      "Trả lời bằng tiếng Việt tự nhiên (KHÔNG JSON), dạng liệt kê từng dòng, ví dụ:\n" +
      "• Bàn: 2 cái — đơn giá 150.000 VND — tạm tính 300.000 VND\n" +
      "• Ghế: 3 cái — đơn giá 80.000 VND — tạm tính 240.000 VND\n" +
      "Cuối cùng in tổng tạm tính. Hỏi lại: “Đây có phải là những gì bạn cần vận chuyển không? Nếu đúng, bấm Xác nhận; nếu chưa đúng, hãy nói rõ để mình chỉnh.”",
    items: [] // sẽ load từ JSON
  };
}
function loadSettings() {
  try {
    const s = JSON.parse(localStorage.getItem(CFG_KEY));
    if (!s) return defaultSettings();
    return s;
  } catch { return defaultSettings(); }
}
function saveSettings(s) {
  try { localStorage.setItem(CFG_KEY, JSON.stringify(s)); } catch {}
}

/* ========= Helpers ========= */
const delay = (ms) => new Promise(r => setTimeout(r, ms));
const isBulky = (name="") => BULKY_KEYWORDS.some(k => (name||"").toLowerCase().includes(k.toLowerCase()));
const rand = (arr) => arr[Math.floor(Math.random()*arr.length)];
const randomInt = (min, max, step=5000) => {
  const a = Math.ceil(min/step), b = Math.floor(max/step);
  return (Math.floor(Math.random()*(b-a+1))+a)*step;
};
const cheapPrice = () => randomInt(15_000, 150_000, 5000);
const bulkyPrice = () => randomInt(250_000, 900_000, 5000);
const decorateName = (base) => {
  const bits = [];
  if (Math.random()<0.6) bits.push(rand(SIZE_TAGS));
  if (Math.random()<0.4) bits.push(rand(MATERIALS));
  if (Math.random()<0.4) bits.push(rand(COLORS));
  return bits.length ? `${base} (${bits.join(", ")})` : base;
};

/* ========= Robust fetch/backoff for AI ========= */
async function fetchWithBackoff(options, { maxRetries = 3, baseDelay = 700 } = {}) {
  let attempt = 0, switched = false;
  while (true) {
    const res = await fetch(buildApiUrl(), options);
    const text = await res.text();
    let data; try { data = JSON.parse(text); } catch { data = { raw: text }; }
    if (res.ok) return data;
    const status = res.status;
    const msg = data?.error?.message || `HTTP ${status}`;
    const overload = status === 429 || status === 503 || /overloaded/i.test(msg);
    const notFoundOrUnsupported = status === 404 || /not found|not supported/i.test(msg);
    if ((notFoundOrUnsupported || overload) && !switched && MODEL !== FALLBACK_MODEL) {
      MODEL = FALLBACK_MODEL; switched = true; await delay(250); continue;
    }
    if (overload && attempt < maxRetries) {
      attempt++; const wait = baseDelay * Math.pow(2, attempt - 1) + Math.floor(Math.random()*250);
      await delay(wait); continue;
    }
    const err = new Error(msg); err.status = status; err.payload = data; throw err;
  }
}

/* ========= Prompt ========= */
function buildPromptText(userText) {
  const s = loadSettings();
  const priceLines = (s.items||[]).slice(0,200) // tránh gửi quá dài
    .map(it => `- ${it.name}: ${Number(it.price).toLocaleString()} ${s.currency}`).join("\n");
  return s.basePrompt +
    "\n\nBảng đơn giá tham khảo (một phần):\n" + priceLines +
    "\n\nHãy căn cứ ảnh + tin nhắn của khách và bảng giá trên để ước lượng số lượng & tạm tính. " +
    "Nếu không chắc 1 món nào đó trong ảnh, hãy ghi “(không rõ)” và hỏi lại. " +
    "Tuyệt đối KHÔNG trả JSON hay mã, chỉ trả lời văn bản tự nhiên.\n\n" + (userText || "");
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
    item.innerHTML = `<img src="${u.previewUrl}" alt="upload ${idx+1}">
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
    if (userUploads.length + files.length > 10) { alert("Bạn chỉ có thể tải lên tối đa 10 ảnh."); return; }
    const images = files.filter(f => /^image\//i.test(f.type));
    if (images.length !== files.length) alert("Một số tệp không phải hình ảnh nên đã bị bỏ qua.");
    Promise.all(images.map(file => new Promise(resolve => {
      const reader = new FileReader();
      reader.onload = e => { const previewUrl = e.target.result;
        const base64 = previewUrl.split(",")[1]; userUploads.push({ data: base64, mime_type: file.type, previewUrl }); resolve(); };
      reader.readAsDataURL(file);
    }))).then(renderUploadPreview);
  });
}
if (fileUploadWrapper) {
  fileUploadWrapper.addEventListener("click", (e) => {
    const btn = e.target.closest(".thumb-remove"); if (!btn) return;
    const idx = +btn.getAttribute("data-idx"); if (idx >= 0) { userUploads.splice(idx, 1); renderUploadPreview(); }
  });
}
if (fileCancelButton) fileCancelButton.addEventListener("click", () => {
  userUploads.splice(0, userUploads.length); renderUploadPreview();
});

/* ========= Parse AI text -> [{name, qty}] ========= */
function parseItemsFromAiText(text) {
  if (!text) return [];
  const lines = text.split(/\r?\n/);
  const results = [];

  // bắt buộc có số lượng (2 cái, 3 bộ, 4 chiếc, ...)
  const unitWords = "(?:cái|bộ|chiếc|thùng carton|thùng|kg|m3|m²|m|bức|tấm|cây|cuộn|ghế|bàn|thanh|kiện|bao|túi)";
  const qtyRegex = new RegExp(`(\\d+[\\d.,]*)\\s*${unitWords}\\b`, "i");

  for (let raw of lines) {
    let line = (raw || "").trim().replace(/^[-•*]\s*/, "");
    if (!line) continue;

    const lower = line.toLowerCase();
    // bỏ câu hỏi/điều hướng/kết luận
    if (
      lower.startsWith("chào ") ||
      lower.includes("đây có phải") ||
      lower.includes("hãy gửi") ||
      lower.includes("tổng tạm tính") ||
      lower.startsWith("lưu ý") ||
      lower.startsWith("ghi chú") ||
      /[\?؟]+$/.test(lower)
    ) continue;

    // cần có số lượng
    const mQty = line.match(qtyRegex);
    if (!mQty) continue;
    const qty = parseInt(mQty[1].replace(/[^\d]/g, ""), 10) || 1;

    // tên món: phần trước ":" hoặc "—"
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

/* ========= Right table (internal pricing) ========= */
(function () {
  const itemsTbody = document.querySelector("#items-tbody");
  const sumQtyEl = document.querySelector("#sum-qty");
  const sumAmountEl = document.querySelector("#sum-amount");
  if (!itemsTbody || !sumQtyEl || !sumAmountEl) return;

  let items = []; // {id,name,price,qty}

  // —— price index —— (build sau khi load products)
  let priceIndexExact = null; // Map<nameLower, price>
  let priceIndexList = null;  // Array<[nameLower, price]>

  function buildPriceIndex() {
    const list = (loadSettings().items || []);
    priceIndexExact = new Map();
    priceIndexList = [];
    for (const it of list) {
      const nm = (it.name || "").toLowerCase().trim();
      const price = Number(it.price) || 0;
      if (!nm) continue;
      priceIndexExact.set(nm, price);
      priceIndexList.push([nm, price]);
    }
    priceIndexList.sort((a,b)=> b[0].length - a[0].length); // dài trước
  }

  function lookupPrice(name) {
    if (!priceIndexExact) buildPriceIndex();
    const n = (name || "").toLowerCase().trim();
    if (!n) return 0;
    if (priceIndexExact.has(n)) return priceIndexExact.get(n);
    const escapeRegExp = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    for (const [nm, price] of priceIndexList) {
      const rx = new RegExp(`\\b${escapeRegExp(nm)}\\b`, "i");
      if (rx.test(n)) return Number(price) || 0;
    }
    return 0;
  }

  const currency = () => (loadSettings().currency || "VND");
  const fmt = (n) => Number(n || 0).toLocaleString() + " " + currency();

  function render() {
    itemsTbody.innerHTML = "";
    if (!items.length) {
      const tr = document.createElement("tr");
      tr.className = "empty-row";
      tr.innerHTML = `<td colspan="4" class="text-muted text-center py-3">Chưa có sản phẩm. Gửi ảnh để AI nhận diện hoặc thêm thủ công.</td>`;
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

        const tr = document.createElement("tr");
        tr.dataset.id = it.id;
        tr.innerHTML = `
          <td>${it.name}</td>
          <td class="text-center">
            <div class="qty-group">
              <button class="btn-minus" type="button" aria-label="Giảm">−</button>
              <input class="qty-input" value="${it.qty}" inputmode="numeric">
              <button class="btn-plus" type="button" aria-label="Tăng">+</button>
            </div>
          </td>
          <td class="text-right">
            <div>${priceHtml}</div>
            <small class="text-muted">${subHtml}</small>
          </td>
          <td class="text-right">
            <button class="btn btn-sm btn-outline-danger btn-del" title="Xoá"><i class="fas fa-trash"></i></button>
          </td>`;
        itemsTbody.appendChild(tr);
      }
    }
    const totalQty = items.reduce((s,i)=>s+Number(i.qty||0),0);
    const totalAmount = items
      .filter(i => Number(i.price) > 0)
      .reduce((s,i)=>s+Number(i.qty||0)*Number(i.price||0),0);
    sumQtyEl.textContent = String(totalQty);
    sumAmountEl.textContent = Number(totalAmount).toLocaleString() + " " + currency();
  }

  function setItems(list) {
    items = (list || []).map(it => {
      const name = String(it.name || "").trim();
      return {
        id: "i_" + Math.random().toString(36).slice(2,9),
        name,
        price: lookupPrice(name),
        qty: Math.max(1, Number(String(it.qty||1).replace(/[^\d]/g,"")) || 1)
      };
    }).filter(it => it.name);
    render();
  }

  // add manual item
  function addManualItem(name, qty) {
    const cleanName = String(name || "").trim();
    const cleanQty = Math.max(1, parseInt(String(qty||1).replace(/[^\d]/g,""), 10) || 1);
    if (!cleanName) return;

    const idx = items.findIndex(x => x.name.toLowerCase().trim() === cleanName.toLowerCase().trim());
    if (idx >= 0) {
      items[idx].qty = Number(items[idx].qty || 0) + cleanQty;
    } else {
      items.push({
        id: "i_" + Math.random().toString(36).slice(2,9),
        name: cleanName,
        price: lookupPrice(cleanName),
        qty: cleanQty
      });
    }
    render();
  }

  // qty +/- / input / delete
  itemsTbody.addEventListener("click",(e)=>{
    const tr = e.target.closest("tr"); if (!tr) return;
    const id = tr.dataset.id;
    const it = (id && (items.find(x=>x.id===id))); if (!it) return;
    if (e.target.closest(".btn-plus"))  { it.qty = Number(it.qty) + 1; render(); }
    if (e.target.closest(".btn-minus")) { it.qty = Math.max(1, Number(it.qty) - 1); render(); }
    if (e.target.closest(".btn-del")) {
      const ok = confirm(`Bạn có muốn xoá "${it.name}" khỏi danh sách không?`);
      if (ok) { items = items.filter(x => x.id !== id); render(); }
    }
  });
  itemsTbody.addEventListener("input",(e)=>{
    if (!e.target.classList.contains("qty-input")) return;
    const tr = e.target.closest("tr"); const id = tr?.dataset?.id;
    const it = items.find(x=>x.id===id); if (!it) return;
    const v = parseInt(e.target.value.replace(/[^\d]/g,"") || "1", 10);
    it.qty = Math.max(1, v); render();
  });

  // expose cho chat + modal
  window.AIQUOTE = window.AIQUOTE || {};
  window.AIQUOTE.setItems = setItems;
  window.AIQUOTE.addManualItem = addManualItem;

  render();
})();

/* ========= Call AI & (conditionally) fill list ========= */
async function generateBotResponse(incomingMessageDiv, userText, opts = {}) {
  const { allowAutofill = false } = opts; // chỉ true nếu lượt gửi này có ảnh
  const messageElement = incomingMessageDiv.querySelector(".message-text");
  const parts = [{ text: buildPromptText(userText) }];
  userUploads.forEach(img => parts.push({ inline_data: { data: img.data, mime_type: img.mime_type } }));
  chatHistory.push({ role: "user", parts });

  const requestOptions = {
    method:"POST",
    headers:{ "Content-Type":"application/json" },
    body: JSON.stringify({ contents: chatHistory.slice(-8) })
  };

  try {
    const data = await fetchWithBackoff(requestOptions, { maxRetries:3, baseDelay:700 });
    const apiText =
      data?.candidates?.[0]?.content?.parts?.[0]?.text?.replace(/\*\*(.*?)\*\*/g,"$1").trim()
      ?? "Mình chưa đọc được nội dung, vui lòng thử lại.";
    messageElement.innerText = apiText;
    chatHistory.push({ role:"model", parts:[{ text: apiText }] });

    const parsed = parseItemsFromAiText(apiText);
    if (allowAutofill && parsed.length && window.AIQUOTE?.setItems) {
      window.AIQUOTE.setItems(parsed);
    }
  } catch (error) {
    console.error(error);
    messageElement.innerText = error.message || "Có lỗi khi gọi AI.";
    messageElement.style.color = "#ff0000";
  } finally {
    userUploads.splice(0, userUploads.length);
    renderUploadPreview();
    incomingMessageDiv.classList.remove("thinking");
    chatBody && chatBody.scrollTo({ top: chatBody.scrollHeight, behavior: "smooth" });
  }
}

/* ========= Chat UI ========= */
function createMessageElement(content, ...classes) {
  const div = document.createElement("div");
  div.classList.add("message", ...classes);
  div.innerHTML = content;
  return div;
}
function handleOutgoingMessage(e) {
  e.preventDefault();
  const text = (messageInput && messageInput.value.trim()) || "";
  if (!text && userUploads.length === 0) return;

  if (messageInput) { messageInput.value = ""; messageInput.dispatchEvent(new Event("input")); }

  const content = `
    <div class="message-text"></div>
    ${userUploads.length ? `<div class="attachment-strip">${userUploads.map(u=>`<img src="${u.previewUrl}" class="attachment">`).join("")}</div>` : ""}
  `;
  const outgoing = createMessageElement(content, "user-message");
  outgoing.querySelector(".message-text").innerText = text || "(ảnh đính kèm)";
  chatBody.appendChild(outgoing);
  chatBody.scrollTo({ top: chatBody.scrollHeight, behavior: "smooth" });

  // CHỈ autofill khi lượt gửi này có ảnh
  const allowAutofillThisTurn = userUploads.length > 0;

  setTimeout(() => {
    const botContent = `
      <svg class="bot-avatar" xmlns="http://www.w3.org/2000/svg" width="50" height="50" viewBox="0 0 1024 1024">
        <path d="M738.3 287.6H285.7c-59 0-106.8 47.8-106.8 106.8v303.1c0 59 47.8 106.8 106.8 106.8h81.5v111.1c0 .7.8 1.1 1.4.7l166.9-110.6 41.8-.8h117.4l43.6-.4c59 0 106.8-47.8 106.8-106.8V394.5c0-59-47.8-106.9-106.8-106.9zM351.7 448.2c0-29.5 23.9-53.5 53.5-53.5s53.5 23.9 53.5 53.5-23.9 53.5-53.5 53.5-53.5-23.9-53.5-53.5zm157.9 267.1c-67.8 0-123.8-47.5-132.3-109h264.6c-8.6 61.5-64.5 109-132.3 109zm110-213.7c-29.5 0-53.5-23.9-53.5-53.5s23.9-53.5 53.5-53.5 53.5 23.9 53.5 53.5-23.9 53.5-53.5 53.5zM867.2 644.5V453.1h26.5c19.4 0 35.1 15.7 35.1 35.1v121.1c0 19.4-15.7 35.1-35.1 35.1h-26.5zM95.2 609.4V488.2c0-19.4 15.7-35.1 35.1-35.1h26.5v191.3h-26.5c-19.4 0-35.1-15.7-35.1-35.1zM561.5 149.6c0 23.4-15.6 43.3-36.9 49.7v44.9h-30v-44.9c-21.4-6.5-36.9-26.3-36.9-49.7 0-28.6 23.3-51.9 51.9-51.9s51.9 23.3 51.9 51.9z"/>
      </svg>
      <div class="message-text"><div class="thinking-indicator"><div class="dot"></div><div class="dot"></div><div class="dot"></div></div></div>`;
    const incoming = createMessageElement(botContent, "bot-message", "thinking");
    chatBody.appendChild(incoming);
    chatBody.scrollTo({ top: chatBody.scrollHeight, behavior: "smooth" });
    generateBotResponse(incoming, text, { allowAutofill: allowAutofillThisTurn });
  }, 400);
}
if (messageInput) {
  messageInput.addEventListener("input", () => {
    messageInput.style.height = `${initialInputHeight}px`;
    messageInput.style.height = `${messageInput.scrollHeight}px`;
  });
  messageInput.addEventListener("keydown", (e) => {
    const userText = e.target.value.trim();
    if (e.key === "Enter" && !e.shiftKey && (userText || userUploads.length) && window.innerWidth > 768) handleOutgoingMessage(e);
  });
}
if (sendMessage) sendMessage.addEventListener("click", (e) => handleOutgoingMessage(e));

/* ========= Load external products.json + generator ========= */
(async function bootstrapProducts() {
  const s = loadSettings();
  let loaded = [];
  try {
    const r = await fetch(PRODUCTS_URL, { cache: "no-store" });
    if (r.ok) loaded = await r.json();
  } catch (e) { console.warn("Load products.json failed", e); }

  // sanitize
  const normalized = [];
  const seen = new Set();
  for (const it of (loaded||[])) {
    const name = String(it?.name || "").trim();
    if (!name) continue;
    const key = name.toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    let price = Number(it?.price) || 0;
    normalized.push({ name, price });
  }

  // tự sinh thêm đến khi đủ TARGET_COUNT
  while (normalized.length < TARGET_COUNT) {
    const baseName = BASE_NAMES[Math.floor(Math.random()*BASE_NAMES.length)];
    const full = decorateName(baseName);
    const key = full.toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    const price = isBulky(full) ? bulkyPrice() : cheapPrice();
    normalized.push({ name: full, price });
  }

  s.items = normalized;
  saveSettings(s);
})();
