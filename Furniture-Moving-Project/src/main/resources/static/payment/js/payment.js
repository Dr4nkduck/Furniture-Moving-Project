// payment.js — hỗ trợ VNPay (payUrl/txnRef) & VietQR (vietqrImageUrl)
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
        enableRetry(true);                      // ✅ bật lại khi hết hạn
        if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
        return;
      }
      const m = Math.floor(remain / 60000);
      const s = Math.floor((remain % 60000) / 1000);
      countdownEl.textContent = `${String(m).padStart(2,"0")}:${String(s).padStart(2,"0")}`;
    }, 500);
  }

  function renderVietQR(imgUrl){
    qrBox.innerHTML = "";
    const img = new Image();
    img.src = imgUrl;
    img.alt = "VietQR";
    img.width = 220;
    img.height = 220;
    img.decoding = "async";
    qrBox.appendChild(img);
  }

  function renderVNPayQRCodeOrFallback(payUrl){
    qrBox.innerHTML = "";
    if (window.QRCode) {
      /* global QRCode */
      new QRCode(qrBox, { text: payUrl, width: 220, height: 220, correctLevel: QRCode.CorrectLevel.M });
    } else {
      const a = document.createElement("a");
      a.href = payUrl;
      a.target = "_blank";
      a.rel = "noopener";
      a.className = "btn-open-vnpay";
      a.textContent = "Mở VNPay để thanh toán";
      qrBox.appendChild(a);
    }
  }

  function startPolling(txnRefOrNull){
    pollTimer && clearInterval(pollTimer);
    pollTimer = setInterval(async () => {
      try {
        const url = txnRefOrNull
          ? `/payment/${requestId}/status?txnRef=${encodeURIComponent(txnRefOrNull)}`
          : `/payment/${requestId}/status`;
        const res = await fetch(url, { headers: { "Accept": "application/json" } });
        if (!res.ok) return;
        const data = await res.json();

        if (data?.amount != null && amountEl) amountEl.textContent = formatCurrency(data.amount);
        const st = String(data?.status || "").toUpperCase();

        if (st === "PAID")     { setStatus("Thanh toán thành công."); clearInterval(pollTimer); enableRetry(true); }
        else if (st === "FAILED"){ setStatus("Thanh toán thất bại. Vui lòng thử lại."); clearInterval(pollTimer); enableRetry(true); }
        else if (st === "EXPIRED"){ setStatus("Phiên thanh toán đã hết hạn."); clearInterval(pollTimer); enableRetry(true); }
        // PENDING -> giữ nguyên
      } catch (_) {}
    }, 5000);
  }

  async function initPayment(){
    clearTimers();
    enableRetry(false); // khoá nút trong lúc tạo
    if (qrBox) qrBox.innerHTML = `<div class="qr-skeleton">Đang khởi tạo phiên thanh toán...</div>`;
    setStatus("");

    try {
      // Get selected payment type
      const paymentType = document.querySelector('input[name="paymentType"]:checked')?.value || 'FULL';
      
      const headers = { "Accept": "application/json" };
      if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

      const res = await fetch(`/payment/${requestId}/init?paymentType=${encodeURIComponent(paymentType)}`, { method: "POST", headers });
      if (!res.ok) { setStatus("Không khởi tạo được phiên thanh toán."); enableRetry(true); return; }

      const { payUrl, txnRef, expireAt, amount, vietqrImageUrl } = await res.json();

      if (amountEl && amount != null) amountEl.textContent = formatCurrency(amount);
      if (expireAt) startCountdown(expireAt);

      if (vietqrImageUrl) {
        renderVietQR(vietqrImageUrl);
        setStatus("Quét mã VietQR bằng ứng dụng ngân hàng để thanh toán.");
        enableRetry(true);                   // ✅ bật lại nút khi thành công
        startPolling(null);
        return;
      }

      if (payUrl && txnRef) {
        renderVNPayQRCodeOrFallback(payUrl);
        setStatus("Quét mã VNPay QR hoặc mở VNPay để thanh toán.");
        enableRetry(true);                   // ✅ bật lại nút khi thành công
        startPolling(txnRef);
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
