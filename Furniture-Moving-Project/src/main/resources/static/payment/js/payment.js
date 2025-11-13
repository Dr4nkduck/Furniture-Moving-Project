// payment.js — VietQR ONLY (vietqrImageUrl), poll trạng thái & redirect khi PAID
(() => {
  const meta = (n) => document.querySelector(`meta[name="${n}"]`)?.getAttribute("content");

  const requestId = meta("request-id");
  const csrfHeader = meta("_csrf_header");
  const csrfToken  = meta("_csrf");

  const amountEl    = document.getElementById("amountValue");
  const statusEl    = document.getElementById("statusText");
  const countdownEl = document.getElementById("countdown");
  const qrBox       = document.getElementById("qrBox");
  const retryBtn    = document.getElementById("retryButton"); // khớp HTML

  let pollTimer = null;
  let cdTimer   = null;

  // ============== UI helpers ==============
  const setStatus = (msg) => { if (statusEl) statusEl.textContent = msg; };
  const formatCurrency = (v) => {
    const n = Number(v);
    return Number.isFinite(n) ? n.toLocaleString("vi-VN") + " đ" : String(v);
  };
  const clearTimers = () => {
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
    if (cdTimer)   { clearInterval(cdTimer);   cdTimer   = null; }
  };
  function enableRetry(enabled){
    if (!retryBtn) return;
    retryBtn.disabled = !enabled;
    retryBtn.textContent = enabled ? "Tạo lại QR" : "Đang tạo...";
  }

  // Thanh notification (top bar) khi thanh toán thành công
  function showTopNotification(message = "Thanh toán thành công", ms = 1600) {
    // inject style 1 lần
    if (!document.getElementById("topNotifyStyle")) {
      const style = document.createElement("style");
      style.id = "topNotifyStyle";
      style.textContent = `
        .top-notify {
          position: fixed;
          top: -64px;
          left: 0; right: 0;
          height: 56px;
          display: flex; align-items: center; justify-content: center;
          background: #10b981; /* emerald-500 */
          color: #fff; font-weight: 600; letter-spacing: .2px;
          z-index: 9999;
          box-shadow: 0 8px 20px rgba(0,0,0,.15);
          transition: top .35s ease;
        }
        .top-notify.show { top: 0; }
      `;
      document.head.appendChild(style);
    }
    const bar = document.createElement("div");
    bar.className = "top-notify";
    bar.textContent = message;
    document.body.appendChild(bar);
    requestAnimationFrame(() => bar.classList.add("show"));
    setTimeout(() => {
      bar.classList.remove("show");
      bar.addEventListener("transitionend", () => bar.remove(), { once: true });
    }, ms);
  }

  // ============== countdown ==============
  function startCountdown(expireAtIso){
    if (!expireAtIso || !countdownEl) return;
    const end = new Date(expireAtIso).getTime();
    if (!isFinite(end)) return;

    cdTimer && clearInterval(cdTimer);
    cdTimer = setInterval(() => {
      const remain = end - Date.now();
      if (remain <= 0) {
        clearInterval(cdTimer);
        countdownEl.textContent = "00:00";
        setStatus("Phiên thanh toán đã hết hạn.");
        enableRetry(true);
        if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
        return;
      }
      const m = Math.floor(remain / 60000);
      const s = Math.floor((remain % 60000) / 1000);
      countdownEl.textContent = `${String(m).padStart(2,"0")}:${String(s).padStart(2,"0")}`;
    }, 500);
  }

  // ============== render QR (VietQR) ==============
  function renderVietQR(imgUrl){
    if (!qrBox) return;
    qrBox.innerHTML = "";
    const img = new Image();
    img.src = imgUrl;
    img.alt = "VietQR";
    img.width = 220;
    img.height = 220;
    img.decoding = "async";
    qrBox.appendChild(img);
  }

  // ============== polling status ==============
  function startPolling(){
    if (!requestId) return;
    pollTimer && clearInterval(pollTimer);
    pollTimer = setInterval(async () => {
      try {
        const url = `/payment/${requestId}/status`;
        const res = await fetch(url, { headers: { "Accept": "application/json" } });
        if (!res.ok) return;
        const data = await res.json();

        if (data?.amount != null && amountEl) amountEl.textContent = formatCurrency(data.amount);
        const st = String(data?.status || "").toUpperCase();

        if (st === "PAID") {
          clearTimers();
          setStatus("Thanh toán thành công.");
          enableRetry(false);

          // ✅ Hiện notification trên cùng rồi chuyển về /homepage
          showTopNotification("Đã chuyển khoản thành công");
          setTimeout(() => {
            window.location.href = "/homepage";
          }, 1200);

        } else if (st === "FAILED") {
          clearTimers();
          setStatus("Thanh toán thất bại. Vui lòng thử lại.");
          enableRetry(true);
        } else if (st === "EXPIRED") {
          clearTimers();
          setStatus("Phiên thanh toán đã hết hạn.");
          enableRetry(true);
        }
        // PENDING / READY_TO_PAY -> giữ nguyên, tiếp tục poll
      } catch (_) {
        // bỏ qua lỗi tạm thời
      }
    }, 5000);
  }

  // ============== init payment ==============
  async function initPayment(){
    clearTimers();
    enableRetry(false); // khoá nút trong lúc tạo
    if (qrBox) qrBox.innerHTML = `<div class="qr-skeleton">Đang khởi tạo phiên thanh toán...</div>`;
    setStatus("");

    try {
      // Get selected payment type (FULL / DEPOSIT_20 ...)
      const paymentType = document.querySelector('input[name="paymentType"]:checked')?.value || 'FULL';

      const headers = { "Accept": "application/json" };
      if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

      const res = await fetch(
        `/payment/${requestId}/init?paymentType=${encodeURIComponent(paymentType)}`,
        { method: "POST", headers }
      );
      if (!res.ok) {
        setStatus("Không khởi tạo được phiên thanh toán.");
        enableRetry(true);
        return;
      }

      const { expireAt, amount, vietqrImageUrl } = await res.json();

      if (amountEl && amount != null) amountEl.textContent = formatCurrency(amount);
      if (expireAt) startCountdown(expireAt);

      if (vietqrImageUrl) {
        renderVietQR(vietqrImageUrl);
        setStatus("Quét mã VietQR bằng ứng dụng ngân hàng để thanh toán.");
        enableRetry(true);
        startPolling();
        return;
      }

      setStatus("Dữ liệu thanh toán trả về không hợp lệ.");
      enableRetry(true);
    } catch (err) {
      setStatus("Có lỗi khi khởi tạo thanh toán.");
      enableRetry(true);
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    retryBtn?.addEventListener("click", initPayment);
    initPayment();
  });
})();
