/* ai-quote.js — Furniture Moving (AI Quote) */

/* =========================
   Element hooks
========================= */
const chatBody = document.querySelector(".chat-body");
const messageInput = document.querySelector(".message-input");
const sendMessage = document.querySelector("#send-message");
const fileInput = document.querySelector("#file-input");
const fileUploadBtn = document.querySelector("#file-upload");     // mở file picker
const fileUploadWrapper = document.querySelector(".file-upload-wrapper");
const fileCancelButton = fileUploadWrapper ? fileUploadWrapper.querySelector("#file-cancel") : null;
const chatbotToggler = document.querySelector("#chatbot-toggler");
const closeChatbot = document.querySelector("#close-chatbot");

/* ================
   API setup
================ */
const API_KEY = "AIzaSyCQZaLPV5xXjs65vh2f8L6HlwHAn8ouSoc"; // demo key
let MODEL = "gemini-2.0-flash";
let FALLBACK_MODEL = "gemini-2.0-flash-lite";
const buildApiUrl = () =>
  `https://generativelanguage.googleapis.com/v1/models/${MODEL}:generateContent?key=${API_KEY}`;

/* =========================
   Session state (volatile)
========================= */
const chatHistory = [];
const userUploads = []; // { data, mime_type, previewUrl } — tối đa 10
const initialInputHeight = messageInput ? messageInput.scrollHeight : 0;

/* =========================
   Config (persist in localStorage)
========================= */
const CFG_KEY = "ai_quote_cfg_v2";

function defaultPriceList() {
  const base = [
    ["bàn", 150000], ["bàn làm việc", 200000], ["bàn học", 180000],
    ["bàn ăn", 220000], ["bàn trà", 120000], ["bàn gấp", 90000],
    ["ghế", 80000], ["ghế xoay", 100000], ["ghế sofa", 300000],
    ["ghế băng", 150000], ["ghế đôn", 50000], ["ghế ăn", 70000],
    ["sofa 2 chỗ", 350000], ["sofa 3 chỗ", 450000], ["sofa góc", 550000],
    ["giường đơn", 300000], ["giường đôi", 400000], ["giường tầng", 500000],
    ["nệm", 120000], ["tủ quần áo", 350000], ["tủ 3 cánh", 450000],
    ["tủ 4 cánh", 550000], ["tủ đầu giường", 80000], ["tủ giày", 120000],
    ["kệ sách", 150000], ["kệ TV", 150000], ["tủ bếp nhỏ", 300000],
    ["tủ bếp lớn", 500000], ["bếp từ", 120000], ["bếp gas", 100000],
    ["tủ lạnh mini", 200000], ["tủ lạnh 2 cánh", 350000], ["tủ lạnh side-by-side", 600000],
    ["máy giặt", 300000], ["máy sấy", 300000], ["máy rửa chén", 350000],
    ["máy lạnh treo tường", 300000], ["máy lạnh đứng", 500000], ["quạt điện", 50000],
    ["quạt cây", 70000], ["quạt bàn", 40000], ["tivi 32 inch", 120000],
    ["tivi 43 inch", 160000], ["tivi 55 inch", 220000], ["dàn loa", 150000],
    ["máy tính để bàn", 150000], ["màn hình máy tính", 80000], ["máy in", 120000],
    ["case PC", 100000], ["bàn phím", 20000], ["chuột máy tính", 20000],
    ["lò vi sóng", 100000], ["nồi chiên không dầu", 80000], ["nồi cơm điện", 60000],
    ["bình nước nóng", 80000], ["bàn ủi", 40000], ["máy hút bụi", 90000],
    ["đèn bàn", 30000], ["đèn cây", 50000], ["gương đứng", 80000],
    ["gương treo tường", 60000], ["tranh treo tường", 50000], ["đồng hồ treo tường", 30000],
    ["cây cảnh nhỏ", 50000], ["cây cảnh lớn", 120000], ["chậu cây gốm", 70000],
    ["valy nhỏ", 50000], ["valy lớn", 70000], ["thùng carton nhỏ", 20000],
    ["thùng carton lớn", 30000], ["kệ kho sắt", 250000], ["kệ nhựa", 100000],
    ["bàn trang điểm", 220000], ["ghế trang điểm", 70000], ["tủ thuốc", 80000],
    ["lều", 50000], ["xe đạp", 150000], ["xe đạp điện", 220000],
    ["máy chạy bộ", 400000], ["máy tập đa năng", 450000], ["tạ tay", 50000],
    ["bàn bida mini", 400000], ["đàn piano cơ", 1500000], ["đàn organ", 200000],
    ["đàn guitar", 60000], ["đàn ukulele", 40000], ["loa kéo", 100000],
    ["tủ hồ sơ", 200000], ["bàn họp", 350000], ["ghế họp", 80000],
    ["máy chiếu", 80000], ["màn chiếu", 70000], ["máy photocopy", 500000],
    ["két sắt nhỏ", 200000], ["két sắt lớn", 450000], ["bàn thờ", 350000],
    ["tủ thờ", 450000], ["chậu rửa chén", 120000], ["bình lọc nước", 90000],
    ["xe đẩy em bé", 100000], ["nôi em bé", 150000], ["cũi em bé", 200000],
    ["bình gas", 90000], ["bếp nướng điện", 80000], ["máy xay sinh tố", 40000],
    ["máy ép trái cây", 60000], ["lò nướng", 120000], ["máy pha cà phê", 120000]
  ];
  const out = base.slice();
  for (let i = base.length + 1; i <= 100; i++) out.push([`Vật dụng #${i}`, 0]);
  return out.map(([name, price]) => ({ name, price }));
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
  } catch {
    return defaultSettings();
  }
}

/* =========================
   Helpers
========================= */
const delay = (ms) => new Promise((r) => setTimeout(r, ms));
const toast = (msg) => alert(msg);

/* =========================
   Robust fetch + fallback
========================= */
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
    const notFound = status === 404 || /not found|not supported/i.test(msg);

    if ((notFound || overload) && !switched && MODEL !== FALLBACK_MODEL) {
      MODEL = FALLBACK_MODEL; switched = true; await delay(250); continue;
    }
    if (overload && attempt < maxRetries) {
      attempt++; const wait = baseDelay * Math.pow(2, attempt - 1) + Math.floor(Math.random() * 250);
      await delay(wait); continue;
    }
    const err = new Error(msg); err.status = status; err.payload = data; throw err;
  }
}

/* =========================
   Preflight model
========================= */
async function preflight() {
  const url = `https://generativelanguage.googleapis.com/v1/models?key=${API_KEY}`;
  const r = await fetch(url);
  const data = await r.json().catch(() => ({}));
  if (!r.ok) throw new Error(`API key check failed: ${data?.error?.message || `HTTP ${r.status}`}`);
  const names = (data.models || data).map(m => m.name?.replace(/^models\//, "")).filter(Boolean);
  if (names.includes("gemini-2.0-flash")) MODEL = "gemini-2.0-flash";
  else if (names.includes("gemini-2.0-flash-lite")) MODEL = "gemini-2.0-flash-lite";
  else if (names.length) MODEL = names[0];
}

/* =========================
   Compose prompt (no JSON)
========================= */
function buildPromptText(userText) {
  const s = loadSettings();
  const priceLines = s.items.map(it => `- ${it.name}: ${Number(it.price).toLocaleString()} ${s.currency}`).join("\n");
  return (
    s.basePrompt +
    "\n\nBảng đơn giá tham khảo (đồ vật → đơn giá):\n" + priceLines +
    "\n\nHãy căn cứ ảnh + tin nhắn của khách và bảng giá trên để ước lượng số lượng & tạm tính. " +
    "Nếu không chắc 1 món nào đó trong ảnh, hãy ghi “(không rõ)” và hỏi lại. " +
    "Tuyệt đối KHÔNG trả JSON hay mã, chỉ trả lời văn bản tự nhiên.\n\n" +
    (userText || "")
  );
}

/* ==================================
   Create message element
================================== */
const createMessageElement = (content, ...classes) => {
  const div = document.createElement("div");
  div.classList.add("message", ...classes);
  div.innerHTML = content;
  return div;
};

/* ==================================
   Render selected images preview
================================== */
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
      <button type="button" class="thumb-remove" data-idx="${idx}" title="Xoá ảnh">&times;</button>
    `;
    grid.appendChild(item);
  });
  fileUploadWrapper.classList.toggle("file-uploaded", userUploads.length > 0);
}

/* ==================================
   File input (allow up to 10 images)
================================== */
if (fileInput) fileInput.multiple = true;
if (fileUploadBtn && fileInput) fileUploadBtn.addEventListener("click", () => fileInput.click());

if (fileInput) {
  fileInput.addEventListener("change", () => {
    const files = Array.from(fileInput.files || []);
    fileInput.value = "";
    if (!files.length) return;

    if (userUploads.length + files.length > 10) { toast("Bạn chỉ có thể tải lên tối đa 10 ảnh cho mỗi cuộc trao đổi."); return; }

    const images = files.filter(f => /^image\//i.test(f.type));
    if (images.length !== files.length) toast("Một số tệp không phải hình ảnh nên đã bị bỏ qua.");

    const readers = images.map(file => new Promise(resolve => {
      const reader = new FileReader();
      reader.onload = e => {
        const previewUrl = e.target.result;
        const base64 = previewUrl.split(",")[1];
        userUploads.push({ data: base64, mime_type: file.type, previewUrl });
        resolve();
      };
      reader.readAsDataURL(file);
    }));

    Promise.all(readers).then(renderUploadPreview);
  });
}

/* remove one */
if (fileUploadWrapper) {
  fileUploadWrapper.addEventListener("click", (e) => {
    const btn = e.target.closest(".thumb-remove");
    if (!btn) return;
    const idx = +btn.getAttribute("data-idx");
    if (idx >= 0) { userUploads.splice(idx, 1); renderUploadPreview(); }
  });
}

/* cancel all */
if (fileCancelButton) {
  fileCancelButton.addEventListener("click", () => { userUploads.splice(0, userUploads.length); renderUploadPreview(); });
}

/* ==================================
   Send message & call API
================================== */
async function generateBotResponse(incomingMessageDiv, userText) {
  const messageElement = incomingMessageDiv.querySelector(".message-text");
  const fullPrompt = buildPromptText(userText);

  const parts = [{ text: fullPrompt }];
  userUploads.forEach(img => parts.push({ inline_data: { data: img.data, mime_type: img.mime_type } }));

  chatHistory.push({ role: "user", parts });
  const trimmed = chatHistory.slice(-8);

  const requestOptions = { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ contents: trimmed }) };

  try {
    const data = await fetchWithBackoff(requestOptions, { maxRetries: 3, baseDelay: 700 });
    const apiText = data?.candidates?.[0]?.content?.parts?.[0]?.text?.replace(/\*\*(.*?)\*\*/g, "$1").trim() ?? "Mình chưa đọc được nội dung, vui lòng thử lại.";
    messageElement.innerText = apiText;
    chatHistory.push({ role: "model", parts: [{ text: apiText }] });
  } catch (error) {
    console.error(error);
    if (/API key not valid|API_KEY_INVALID/i.test(error.message)) {
      messageElement.innerText = "API key không hợp lệ cho endpoint này. Tạo một Gemini API key trong Google AI Studio và dán vào script.";
    } else if (error.status === 404 || /not found|not supported/i.test(error.message)) {
      messageElement.innerText = "Model alias hiện không khả dụng. Đã thử chuyển sang alias nhẹ hơn.";
    } else if (error.status === 429 || error.status === 503 || /overloaded/i.test(error.message)) {
      messageElement.innerText = "Model đang bận. Mình đã retry + fallback nhưng vẫn quá tải. Vui lòng thử lại sau.";
    } else {
      messageElement.innerText = error.message || "Có lỗi khi gọi API.";
    }
    messageElement.style.color = "#ff0000";
  } finally {
    userUploads.splice(0, userUploads.length); // clear ảnh sau mỗi lượt gửi
    renderUploadPreview();
    incomingMessageDiv.classList.remove("thinking");
    chatBody && chatBody.scrollTo({ top: chatBody.scrollHeight, behavior: "smooth" });
  }
}

function handleOutgoingMessage(e) {
  e.preventDefault();
  const text = (messageInput && messageInput.value.trim()) || "";
  if (!text && userUploads.length === 0) return;

  if (messageInput) { messageInput.value = ""; messageInput.dispatchEvent(new Event("input")); }

  const content = `
    <div class="message-text"></div>
    ${userUploads.length ? `<div class="attachment-strip">${userUploads.map(u => `<img src="${u.previewUrl}" class="attachment">`).join("")}</div>` : ""}`;
  const outgoing = createMessageElement(content, "user-message");
  outgoing.querySelector(".message-text").innerText = text || "(ảnh đính kèm)";
  chatBody.appendChild(outgoing);
  chatBody.scrollTo({ top: chatBody.scrollHeight, behavior: "smooth" });

  setTimeout(() => {
    const botContent = `
      <svg class="bot-avatar" xmlns="http://www.w3.org/2000/svg" width="50" height="50" viewBox="0 0 1024 1024">
        <path d="M738.3 287.6H285.7c-59 0-106.8 47.8-106.8 106.8v303.1c0 59 47.8 106.8 106.8 106.8h81.5v111.1c0 .7.8 1.1 1.4.7l166.9-110.6 41.8-.8h117.4l43.6-.4c59 0 106.8-47.8 106.8-106.8V394.5c0-59-47.8-106.9-106.8-106.9zM351.7 448.2c0-29.5 23.9-53.5 53.5-53.5s53.5 23.9 53.5 53.5-23.9 53.5-53.5 53.5-53.5-23.9-53.5-53.5zm157.9 267.1c-67.8 0-123.8-47.5-132.3-109h264.6c-8.6 61.5-64.5 109-132.3 109zm110-213.7c-29.5 0-53.5-23.9-53.5-53.5s23.9-53.5 53.5-53.5 53.5 23.9 53.5 53.5-23.9 53.5-53.5 53.5zM867.2 644.5V453.1h26.5c19.4 0 35.1 15.7 35.1 35.1v121.1c0 19.4-15.7 35.1-35.1 35.1h-26.5zM95.2 609.4V488.2c0-19.4 15.7-35.1 35.1-35.1h26.5v191.3h-26.5c-19.4 0-35.1-15.7-35.1-35.1zM561.5 149.6c0 23.4-15.6 43.3-36.9 49.7v44.9h-30v-44.9c-21.4-6.5-36.9-26.3-36.9-49.7 0-28.6 23.3-51.9 51.9-51.9s51.9 23.3 51.9 51.9z"/>
      </svg>
      <div class="message-text">
        <div class="thinking-indicator">
          <div class="dot"></div><div class="dot"></div><div class="dot"></div>
        </div>
      </div>`;
    const incoming = createMessageElement(botContent, "bot-message", "thinking");
    chatBody.appendChild(incoming);
    chatBody.scrollTo({ top: chatBody.scrollHeight, behavior: "smooth" });

    generateBotResponse(incoming, text);
  }, 400);
}

/* ==================================
   Input autosize & events
================================== */
if (messageInput) {
  messageInput.addEventListener("input", () => {
    messageInput.style.height = `${initialInputHeight}px`;
    messageInput.style.height = `${messageInput.scrollHeight}px`;
    const form = document.querySelector(".chat-form");
    if (form) form.style.borderRadius = messageInput.scrollHeight > initialInputHeight ? "15px" : "32px";
  });

  messageInput.addEventListener("keydown", (e) => {
    const userText = e.target.value.trim();
    if (e.key === "Enter" && !e.shiftKey && (userText || userUploads.length) && window.innerWidth > 768) {
      handleOutgoingMessage(e);
    }
  });
}

if (sendMessage) sendMessage.addEventListener("click", (e) => handleOutgoingMessage(e));
if (closeChatbot) closeChatbot.addEventListener("click", () => document.body.classList.remove("show-chatbot"));
if (chatbotToggler) chatbotToggler.addEventListener("click", () => document.body.classList.toggle("show-chatbot"));

/* ==================================
   Buttons under chat panel
================================== */
function bindActionButtons() {
  const confirmBtn =
    document.querySelector("#btn-confirm-enough") ||
    document.querySelector("#btn-confirm-items") ||
    document.querySelector("#btn-confirm");

  const addManualBtn =
    document.querySelector("#btn-add-manual") ||
    document.querySelector("#btn-add-manual-items");

  if (confirmBtn) confirmBtn.addEventListener("click", () => toast("Xác nhận thành công! Đơn báo giá đã được ghi nhận (demo)."));
  if (addManualBtn) addManualBtn.addEventListener("click", () => toast("Tính năng thêm hàng thủ công: Sắp ra mắt."));
}
bindActionButtons();

/* ==================================
   Floating buttons: chỉ giữ NÚT BẢNG GIÁ
   (ĐÃ BỎ nút cấu hình #ai-settings-fab)
================================== */
function ensureFloatingButtons() {
  // nút xem bảng giá
  let priceBtn = document.querySelector("#ai-pricelist-fab");
  if (!priceBtn) {
    priceBtn = document.createElement("button");
    priceBtn.id = "ai-pricelist-fab";
    priceBtn.className = "btn btn-info";
    priceBtn.style.position = "fixed";
    priceBtn.style.right = "18px";
    priceBtn.style.bottom = "30px";
    priceBtn.style.zIndex = "1031";
    priceBtn.textContent = "Bảng giá";
    priceBtn.title = "Xem bảng giá vận chuyển";
    document.body.appendChild(priceBtn);
    priceBtn.addEventListener("click", openPriceViewModal);
  }
}
ensureFloatingButtons();

/* ==================================
   Price List (read-only) modal
================================== */
function openPriceViewModal() {
  const s = loadSettings();
  let modal = document.querySelector("#aiPriceViewModal");
  if (!modal) {
    modal = document.createElement("div");
    modal.id = "aiPriceViewModal";
    modal.className = "modal fade";
    modal.tabIndex = -1;
    modal.innerHTML = `
      <div class="modal-dialog modal-lg modal-dialog-scrollable">
        <div class="modal-content">
          <div class="modal-header bg-dark text-white">
            <h5 class="modal-title"><i class="fas fa-list mr-2"></i>Bảng giá vận chuyển (tham khảo)</h5>
            <button type="button" class="close text-white" data-dismiss="modal" aria-label="Close"><span>&times;</span></button>
          </div>
          <div class="modal-body">
            <div class="table-responsive">
              <table class="table table-bordered table-sm mb-0">
                <thead class="thead-light">
                  <tr><th>Tên đồ vật</th><th class="text-right">Đơn giá</th></tr>
                </thead>
                <tbody id="price-view-tbody"></tbody>
              </table>
            </div>
            <small class="text-muted d-block mt-2">* Giá có thể thay đổi theo khoảng cách, tầng, tháo lắp, thời điểm...</small>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" data-dismiss="modal" type="button">Đóng</button>
          </div>
        </div>
      </div>`;
    document.body.appendChild(modal);
  }

  const tbody = modal.querySelector("#price-view-tbody");
  tbody.innerHTML = s.items.map(it => `<tr><td>${it.name}</td><td class="text-right">${Number(it.price).toLocaleString()} ${s.currency}</td></tr>`).join("");

  if (window.jQuery && $("#aiPriceViewModal").modal) $('#aiPriceViewModal').modal('show');
  else modal.style.display = "block";
}

/* ==================================
   Kick off preflight
================================== */
preflight().catch((err) => {
  console.error(err);
  alert("API key Gemini không hợp lệ với endpoint này. Tạo key mới trong Google AI Studio và dán vào script.js.");
});

/* ==================================
   Clear volatile data on leave
================================== */
window.addEventListener("beforeunload", () => { userUploads.splice(0, userUploads.length); });

/* Ensure thumb grid exists */
(function ensureThumbGrid() {
  if (!fileUploadWrapper) return;
  if (!fileUploadWrapper.querySelector(".thumb-grid")) {
    const grid = document.createElement("div");
    grid.className = "thumb-grid";
    fileUploadWrapper.appendChild(grid);
  }
})();
