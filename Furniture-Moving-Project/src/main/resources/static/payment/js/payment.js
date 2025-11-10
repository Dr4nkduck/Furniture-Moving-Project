// payment.js — hỗ trợ cả VNPay (payUrl/txnRef) và VietQR (vietqrImageUrl)
(() => {
  const meta = (n) => document.querySelector(`meta[name="${n}"]`)?.getAttribute("content");

  const requestId = meta("request-id");
  const csrfHeader = meta("_csrf_header");
  const csrfToken  = meta("_csrf");

  const amountEl    = document.getElementById("amountValue");
  const statusEl    = document.getElementById("statusText");
  const countdownEl = document.getElementById("countdown");
  const qrBox       = document.getElementById("qrBox");
  const retryBtn    = document.getElementById("retryBtn");

  let pollTimer = null;
  let cdTimer   = null;

  function setStatus(msg) {
    if (statusEl) statusEl.textContent = msg;
  }

  function formatCurrency(v) {
    const n = Number(v);
    if (!Number.isFinite(n)) return String(v);
    return n.toLocaleString("vi-VN") + " đ";
  }

  function clearTimers() {
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
    if (cdTimer)   { clearInterval(cdTimer);   cdTimer   = null; }
  }

  function startCountdown(expireAtIso) {
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
        // Ngừng poll trạng thái nếu đang chạy
        if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
        return;
      }
      const m = Math.floor(remain / 60000);
      const s = Math.floor((remain % 60000) / 1000);
      countdownEl.textContent = `${String(m).padStart(2,"0")}:${String(s).padStart(2,"0")}`;
    }, 500);
  }

  function renderVietQR(imgUrl) {
    qrBox.innerHTML = "";
    const img = new Image();
    img.src = imgUrl;
    img.alt = "VietQR";
    img.width = 220;
    img.height = 220;
    img.decoding = "async";
    qrBox.appendChild(img);
  }

  function renderVNPayQRCode(payUrl) {
    qrBox.innerHTML = "";
    // Yêu cầu QRCodeJS đã được include trong payment.html
    /* global QRCode */
    new QRCode(qrBox, {
      text: payUrl,
      width: 220,
      height: 220,
      correctLevel: QRCode.CorrectLevel.M
    });
  }

  function startPolling(txnRefOrNull) {
    // Nếu BE yêu cầu txnRef → truyền; nếu không có (VietQR) → gọi status theo requestId
    pollTimer && clearInterval(pollTimer);
    pollTimer = setInterval(async () => {
      try {
        const url = txnRefOrNull
          ? `/payment/${requestId}/status?txnRef=${encodeURIComponent(txnRefOrNull)}`
          : `/payment/${requestId}/status`;

        const res = await fetch(url, { headers: { "Accept": "application/json" } });
        if (!res.ok) return; // bỏ qua vòng này
        const data = await res.json();
        // data: { status: 'PENDING|PAID|FAILED|EXPIRED', amount, paidAt? }
        if (data?.amount != null && amountEl) {
          amountEl.textContent = formatCurrency(data.amount);
        }

        const st = String(data?.status || "").toUpperCase();
        if (st === "PAID") {
          setStatus("Thanh toán thành công.");
          clearInterval(pollTimer);
        } else if (st === "FAILED") {
          setStatus("Thanh toán thất bại. Vui lòng thử lại.");
          clearInterval(pollTimer);
        } else if (st === "EXPIRED") {
          setStatus("Phiên thanh toán đã hết hạn.");
          clearInterval(pollTimer);
        } else {
          // PENDING → giữ nguyên
        }
      } catch (_) {
        // im lặng, thử lại ở chu kỳ sau
      }
    }, 5000);
  }

  async function initPayment() {
    clearTimers();
    if (qrBox) qrBox.innerHTML = `<div class="qr-skeleton">Đang khởi tạo phiên thanh toán...</div>`;
    setStatus("");

    try {
      const headers = { "Accept": "application/json" };
      if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

      const res = await fetch(`/payment/${requestId}/init`, {
        method: "POST",
        headers
      });

      if (!res.ok) {
        setStatus("Không khởi tạo được phiên thanh toán.");
        return;
      }

      const data = await res.json();
      const { payUrl, txnRef, expireAt, amount, vietqrImageUrl } = data || {};

      // Hiển thị số tiền nếu có
      if (amountEl && amount != null) {
        amountEl.textContent = formatCurrency(amount);
      }

      // Countdown nếu có
      if (expireAt) startCountdown(expireAt);

      // Nhánh VietQR (ảnh tĩnh)
      if (vietqrImageUrl) {
        renderVietQR(vietqrImageUrl);
        setStatus("Quét mã VietQR bằng ứng dụng ngân hàng để thanh toán.");
        startPolling(null);
        return;
      }

      // Nhánh VNPay
      if (payUrl && txnRef) {
        renderVNPayQRCode(payUrl);
        setStatus("Quét mã VNPay QR để thanh toán.");
        startPolling(txnRef);
        return;
      }

      // Không đúng format mong đợi
      setStatus("Dữ liệu thanh toán trả về không hợp lệ.");
    } catch (err) {
      setStatus("Có lỗi khi khởi tạo thanh toán.");
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    retryBtn?.addEventListener("click", initPayment);
    initPayment();
  });
})();
