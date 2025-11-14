// /customer/js/my-requests.js
(() => {
  const root = document.querySelector(".fm-request-list");
  if (!root) return;

  const selectedStatus = root.getAttribute("data-selected-status") || "ALL";

  const STATUS_CLASS_PREFIX = "status-";

  // ===== Helper: map code -> text tiếng Việt =====
  function friendlyRequestStatus(code) {
    switch (code) {
      case "pending": return "Đang chờ xác nhận";
      case "ready_to_pay": return "Chờ thanh toán";
      case "paid": return "Đã thanh toán";
      case "in_progress": return "Đang thực hiện";
      case "completed": return "Hoàn tất dịch vụ";
      case "cancelled": return "Đã huỷ";
      default: return code || "";
    }
  }

  function friendlyPaymentStatus(code) {
    switch (code) {
      case "PENDING": return "Chưa thanh toán";
      case "PAID": return "Đã thanh toán";
      case "FAILED": return "Thanh toán thất bại";
      default: return code || "";
    }
  }

  function friendlyContractStatus(code) {
    switch (code) {
      case "draft": return "Chưa ký";
      case "signed": return "Đã ký";
      case "acknowledged": return "đã xác nhận";
      case "cancelled": return "Hợp đồng bị huỷ";
      default: return code || "";
    }
  }

  function updateBadgeStatus(badgeEl, status) {
    if (!badgeEl) return;
    badgeEl.classList.forEach((cls) => {
      if (cls.startsWith(STATUS_CLASS_PREFIX)) badgeEl.classList.remove(cls);
    });
    if (status) badgeEl.classList.add(STATUS_CLASS_PREFIX + status);

    const textSpan = badgeEl.querySelector("span:last-child");
    if (textSpan) textSpan.textContent = friendlyRequestStatus(status);
  }

  // ===== Refresh =====
  function refreshOnce() {
    const url = `/api/customer/requests?status=${encodeURIComponent(selectedStatus)}`;

    fetch(url, {
      headers: { "X-Requested-With": "XMLHttpRequest" }
    })
      .then((r) => r.json())
      .then((data) => {
        if (!Array.isArray(data)) return;

        data.forEach((item) => {
          const row = root.querySelector(`.fm-request-item[data-request-id="${item.requestId}"]`);
          if (!row) return;

          // --- REQUEST STATUS ---
          const statusBadge = row.querySelector(".js-status-badge");
          updateBadgeStatus(statusBadge, item.status);

          // --- PAYMENT BADGE ---
          const paymentBadge = row.querySelector(".js-payment-badge");
          if (paymentBadge) {
            if (item.paymentStatus) {
              paymentBadge.style.display = "inline-flex";
              paymentBadge.querySelector("span:last-child").textContent =
                "Thanh toán: " + friendlyPaymentStatus(item.paymentStatus);
            } else {
              paymentBadge.style.display = "none";
            }
          }

          // --- CONTRACT BADGE ---
          const contractBadgeText = row.querySelector(".js-contract-text");
          if (contractBadgeText) {
            if (item.contractId && item.contractStatus) {
              contractBadgeText.textContent =
                friendlyContractStatus(item.contractStatus) + " #" + item.contractId;
            } else {
              contractBadgeText.textContent = "Chưa tạo";
            }
          }

          // --- SIGNED DATE ---
          const signedAtEl = row.querySelector(".js-contract-signed-at");
          if (signedAtEl) {
            signedAtEl.textContent = item.contractSignedAtFormatted || "—";
          }

          // --- TOTAL COST ---
          const totalCostEl = row.querySelector(".js-total-cost");
          if (totalCostEl) totalCostEl.textContent = item.totalCostFormatted || "—";
        });
      })
      .catch((err) => console.debug("Refresh requests failed:", err));
  }

  refreshOnce();
  setInterval(refreshOnce, 8000);
})();
