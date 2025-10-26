/* ===== AI Quote – upload, chat, parse AI → right table (internal pricing) ===== */

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

/* ========= API ========= */
/* Lưu ý: ở production không nên để key ở FE. Dùng proxy BE. */
const API_KEY = "AIzaSyCQZaLPV5xXjs65vh2f8L6HlwHAn8ouSoc"; // hoặc để rỗng nếu gọi qua BE
let MODEL = "gemini-2.0-flash";
const FALLBACK_MODEL = "gemini-2.0-flash-lite";
const buildApiUrl = () =>
  `https://generativelanguage.googleapis.com/v1/models/${MODEL}:generateContent?key=${API_KEY}`;

/* ========= State ========= */
const chatHistory = [];
const userUploads = [];
const initialInputHeight = messageInput ? messageInput.scrollHeight : 0;

/* ========= Config & internal pricing ========= */
const CFG_KEY = "ai_quote_cfg_v2";
function defaultPriceList() {
  const base = [
    ["bàn",150000],["bàn làm việc",200000],["bàn học",180000],["bàn ăn",220000],["bàn trà",120000],["bàn gấp",90000],
    ["ghế",80000],["ghế xoay",100000],["ghế sofa",300000],["ghế băng",150000],["ghế đôn",50000],["ghế ăn",70000],
    ["sofa 2 chỗ",350000],["sofa 3 chỗ",450000],["sofa góc",550000],["giường đơn",300000],["giường đôi",400000],["giường tầng",500000],
    ["nệm",120000],["tủ quần áo",350000],["tủ 3 cánh",450000],["tủ 4 cánh",550000],["tủ đầu giường",80000],["tủ giày",120000],
    ["kệ sách",150000],["kệ TV",150000],["tủ bếp nhỏ",300000],["tủ bếp lớn",500000],["bếp từ",120000],["bếp gas",100000],
    ["tủ lạnh mini",200000],["tủ lạnh 2 cánh",350000],["tủ lạnh side-by-side",600000],["máy giặt",300000],["máy sấy",300000],
    ["máy rửa chén",350000],["máy lạnh treo tường",300000],["máy lạnh đứng",500000],["quạt điện",50000],["quạt cây",70000],
    ["quạt bàn",40000],["tivi 32 inch",120000],["tivi 43 inch",160000],["tivi 55 inch",220000],["dàn loa",150000],
    ["máy tính để bàn",150000],["màn hình máy tính",80000],["máy in",120000],["case PC",100000],["bàn phím",20000],["chuột máy tính",20000],
    ["lò vi sóng",100000],["nồi chiên không dầu",80000],["nồi cơm điện",60000],["bình nước nóng",80000],["bàn ủi",40000],
    ["máy hút bụi",90000],["đèn bàn",30000],["đèn cây",50000],["gương đứng",80000],["gương treo tường",60000],
    ["tranh treo tường",50000],["đồng hồ treo tường",30000],["cây cảnh nhỏ",50000],["cây cảnh lớn",120000],["chậu cây gốm",70000],
    ["valy nhỏ",50000],["valy lớn",70000],["thùng carton nhỏ",20000],["thùng carton lớn",30000],["kệ kho sắt",250000],
    ["kệ nhựa",100000],["bàn trang điểm",220000],["ghế trang điểm",70000],["tủ thuốc",80000],["lều",50000],["xe đạp",150000],
    ["xe đạp điện",220000],["máy chạy bộ",400000],["máy tập đa năng",450000],["tạ tay",50000],["bàn bida mini",400000],
    ["đàn piano cơ",1500000],["đàn organ",200000],["đàn guitar",60000],["đàn ukulele",40000],["loa kéo",100000],
    ["tủ hồ sơ",200000],["bàn họp",350000],["ghế họp",80000],["máy chiếu",80000],["màn chiếu",70000],["máy photocopy",500000],
    ["két sắt nhỏ",200000],["két sắt lớn",450000],["bàn thờ",350000],["tủ thờ",450000],["chậu rửa chén",120000],
    ["bình lọc nước",90000],["xe đẩy em bé",100000],["nôi em bé",150000],["cũi em bé",200000],["bình gas",90000],
    ["bếp nướng điện",80000],["máy xay sinh tố",40000],["máy ép trái cây",60000],["lò nướng",120000],["máy pha cà phê",120000]
  ];
  while (base.length < 100) base.push([`Vật dụng #${base.length + 1}`, 0]);
  return base.map(([name, price]) => ({ name, price }));
}
function defaultSettings() {
  return {
    currency: "VND",
    basePrompt:
      "Bạn là trợ lý báo giá vận chuyển. Hãy đọc ảnh/nhắn tin của khách, đếm số lượng đồ. " +
      "Trả lời bằng tiếng Việt tự nhiên (KHÔNG JSON), dạng liệt kê từng dòng, ví dụ:\n" +
      "• Bàn: 2 cái — đơn giá 150.000 VND — tạm tính 300.000 VND\n" +
      "• Ghế: 3 cái — đơn giá 80.000 VND — tạm tính 240.000 VND\n" +
      "Cuối cùng in tổng tạm tính. Hỏi lại: “Đây có phải là những gì bạn cần vận chuyển không? Nếu đúng, bấm Xác nhận; nếu chưa đúng, hãy nói rõ để mình chỉnh.”",
    items: defaultPriceList()
  };
}
function loadSettings() {
  try {
    const s = JSON.parse(localStorage.getItem(CFG_KEY));
    if (!s || !Array.isArray(s.items) || !s.items.length) return defaultSettings();
    return s;
  } catch { return defaultSettings(); }
}

/* ========= Helpers ========= */
const delay = (ms) => new Promise(r => setTimeout(r, ms));

/* ========= Robust fetch/backoff ========= */
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
  const priceLines = s.items.map(it => `- ${it.name}: ${Number(it.price).toLocaleString()} ${s.currency}`).join("\n");
  return s.basePrompt +
    "\n\nBảng đơn giá tham khảo (đồ vật → đơn giá):\n" + priceLines +
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

  // Bắt buộc có số lượng (2 cái, 3 bộ, 4 chiếc, ...)
  const unitWords = "(?:cái|bộ|chiếc|thùng carton|thùng|kg|m3|m²|m|bức|tấm|cây|cuộn|ghế|bàn|thanh|kiện|bao|túi)";
  const qtyRegex = new RegExp(`(\\d+[\\d.,]*)\\s*${unitWords}\\b`, "i");

  for (let raw of lines) {
    let line = (raw || "").trim().replace(/^[-•*]\s*/, "");
    if (!line) continue;

    const lower = line.toLowerCase();

    // Bỏ câu hỏi/điều hướng/kết luận
    if (
      lower.startsWith("chào ") ||
      lower.includes("đây có phải") ||
      lower.includes("hãy gửi") ||
      lower.includes("tổng tạm tính") ||
      lower.startsWith("lưu ý") ||
      lower.startsWith("ghi chú") ||
      /[\?؟]+$/.test(lower) // có dấu hỏi ở cuối
    ) continue;

    // Phải có số lượng
    const mQty = line.match(qtyRegex);
    if (!mQty) continue;
    const qty = parseInt(mQty[1].replace(/[^\d]/g, ""), 10) || 1;

    // Tên món: lấy phần trước ":" hoặc "—"
    let name = line.split(/[—:]/)[0]
      .replace(/\((.*?)\)/g, "")
      .replace(/\s+/g, " ")
      .trim();

    // Loại bỏ các câu “bạn/mình/tôi ...”
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

  function lookupPrice(name) {
    const n = (name || "").toLowerCase().trim();
    const list = loadSettings().items || [];

    const escapeRegExp = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

    // 1) Khớp chính xác
    let found = list.find(it => it.name.toLowerCase().trim() === n);

    // 2) Nếu không, khớp theo "từ nguyên" (word-boundary), chọn chuỗi dài nhất
    if (!found) {
      let best = null;
      let bestLen = 0;
      for (const it of list) {
        const nm = (it.name || "").toLowerCase().trim();
        if (!nm) continue;
        const rx = new RegExp(`\\b${escapeRegExp(nm)}\\b`, "i");
        if (rx.test(n) && nm.length > bestLen) {
          best = it; bestLen = nm.length;
        }
      }
      found = best;
    }

    const val = Number(found?.price);
    return Number.isFinite(val) && val > 0 ? val : 0; // 0 => “báo giá sau”
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
        price: lookupPrice(name), // 0 nếu không có trong bảng giá
        qty: Math.max(1, Number(String(it.qty||1).replace(/[^\d]/g,"")) || 1)
      };
    }).filter(it => it.name);
    render();
  }

  // add manual item (merge same name)
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
  const { allowAutofill = false } = opts; // chỉ true nếu lượt gửi có ảnh
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
    // Hiển thị lỗi trong chat (không popup)
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

  // CHỈ cho phép autofill khi lượt gửi này có ảnh
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

/* ========= Manual Add (modal) + Confirm shipment ========= */
(function wireManualAndConfirm(){
  const openBtn = document.getElementById("btn-open-manual");
  const saveBtn = document.getElementById("mi-save");
  const nameInput = document.getElementById("mi-name");
  const qtyInput = document.getElementById("mi-qty");
  const confirmShipmentBtn = document.getElementById("btn-confirm-shipment");

  // mở modal
  if (openBtn) openBtn.addEventListener("click", () => {
    if (window.$) $("#manualItemModal").modal("show");
    else document.getElementById("manualItemModal").style.display = "block";
    if (nameInput) nameInput.value = "";
    if (qtyInput) qtyInput.value = "1";
    setTimeout(()=> nameInput && nameInput.focus(), 300);
  });

  // lưu trong modal (không popup, thêm thẳng)
  if (saveBtn) saveBtn.addEventListener("click", () => {
    const name = nameInput?.value?.trim();
    const qty = qtyInput?.value ?? "1";
    if (!name) { alert("Vui lòng nhập tên sản phẩm"); nameInput?.focus(); return; }
    if (!qty || isNaN(Number(qty))) { alert("Số lượng không hợp lệ"); qtyInput?.focus(); return; }
    if (window.AIQUOTE?.addManualItem) {
      window.AIQUOTE.addManualItem(name, qty);
      if (window.$) $("#manualItemModal").modal("hide");
      else document.getElementById("manualItemModal").style.display = "none";
    }
  });

  // xác nhận vận chuyển
  if (confirmShipmentBtn) confirmShipmentBtn.addEventListener("click", () => {
    alert("thành công");
  });
})();

/* ========= Misc ========= */
// ĐÃ BỎ preflight và popup
window.addEventListener("beforeunload", () => { userUploads.splice(0, userUploads.length); });
(function ensureThumbGrid(){
  if (!fileUploadWrapper) return;
  if (!fileUploadWrapper.querySelector(".thumb-grid")) {
    const g=document.createElement("div");
    g.className="thumb-grid";
    fileUploadWrapper.appendChild(g);
  }
})();
