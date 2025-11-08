(function () {
  // --- helpers ---
  const $ = (sel) => document.querySelector(sel);
  const fmtVND = (n) =>
    new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 })
      .format(Number(n || 0)).replace(/\s?₫/, " VND");

  function q() {
    const o = {};
    new URLSearchParams(location.search).forEach((v, k) => (o[k] = v));
    return o;
  }

  function pad(num, len = 6) {
    num = String(num || "");
    return num.length >= len ? num : "0".repeat(len - num.length) + num;
  }

  function copyText(text) {
    if (!text) return;
    navigator.clipboard.writeText(text).then(() => alert("Đã copy: " + text));
  }

  // --- read query ---
  const p = q();

  // Inputs from query (all optional for demo)
  // rid=1 OR code=SR000001, amount=500000, date=2025-10-01
  // pickup=... , delivery=...
  // bankBin=970436, bankAcc=0123456789, bankName=Vietcombank, accName=CONG%20TY...
  // mins=15 (countdown minutes)
  const rid = p.rid ? Number(p.rid) : undefined;
  const orderCode = p.code || (rid ? ("SR" + pad(rid)) : "SR000001");
  const amount = p.amount ? Number(p.amount) : 500000;
  const preferredDate = p.date || "-";
  const pickup = p.pickup || "—";
  const delivery = p.delivery || "—";

  const bankBin = p.bankBin || "970436"; // Vietcombank (demo)
  const bankAcc = p.bankAcc || "0123456789";
  const bankName = p.bankName || "Vietcombank";
  const accName = p.accName || "CONG TY VAN CHUYEN";

  // --- inject UI ---
  $("#orderCode").textContent = orderCode;
  $("#preferredDate").textContent = preferredDate;
  $("#rid").textContent = rid || "-";
  $("#pickup").textContent = pickup;
  $("#delivery").textContent = delivery;

  $("#amountVnd").textContent = fmtVND(amount);
  $("#amountVnd2").textContent = fmtVND(amount);
  $("#amountVnd3").textContent = fmtVND(amount);
  $("#amountRaw").textContent = String(Math.round(amount));

  const transferNote = "PAY-" + orderCode;
  $("#transferNote").textContent = transferNote;
  $("#transferNote2").textContent = transferNote;

  $("#bankName").textContent = bankName;
  $("#bankAcc").textContent = bankAcc;
  $("#accName").textContent = accName;

  // VNPay QR: dùng ảnh placeholder (giữ nguyên)
  // VietQR: tạo URL từ tham số (chỉ hiển thị nếu muốn)
  // Tham khảo định dạng img.vietqr.io; người dùng có thể tắt nếu không online.
  const vietqrUrl =
    `https://img.vietqr.io/image/${encodeURIComponent(bankBin)}-${encodeURIComponent(bankAcc)}-compact.png` +
    `?amount=${encodeURIComponent(Math.round(amount))}` +
    `&addInfo=${encodeURIComponent(transferNote)}` +
    `&accountName=${encodeURIComponent(accName)}`;

  const vietqrImg = $("#vietqrImg");
  vietqrImg.src = vietqrUrl;
  vietqrImg.alt = "VietQR - " + bankName;

  // Copy buttons
  document.querySelectorAll("[data-copy]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const target = btn.getAttribute("data-copy");
      const el = document.querySelector(target);
      if (el) copyText(el.textContent.trim());
    });
  });

  // Countdown
  const mins = p.mins ? Number(p.mins) : 15;
  let remain = Math.max(0, Math.round(mins * 60));
  const elC = $("#countdown");
  const tick = () => {
    const mm = String(Math.floor(remain / 60)).padStart(2, "0");
    const ss = String(remain % 60).padStart(2, "0");
    elC.textContent = `${mm}:${ss}`;
    if (remain > 0) remain--;
  };
  tick();
  setInterval(tick, 1000);
})();
