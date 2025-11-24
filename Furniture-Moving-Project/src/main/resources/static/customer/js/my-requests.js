// /customer/js/my-requests.js
(() => {
  const root = document.querySelector(".fm-request-list");
  if (!root) return;

  const selectedStatus = root.getAttribute("data-selected-status") || "ALL";
  const STATUS_CLASS_PREFIX = "status-";

  // ===== Chuẩn hoá status để khớp với Provider =====
  function normalizeStatus(code) {
    if (!code) return "";
    // Các alias cũ, map hết về ready_to_pay
    if (code === "accepted" || code === "provider_accepted") {
      return "ready_to_pay";
    }
    return code;
  }

  // ===== Map code -> text, dùng CHUNG với Provider =====
  function friendlyRequestStatus(codeRaw) {
    const code = normalizeStatus(codeRaw);
    switch (code) {
      case "pending":
        return "Chờ xác nhận";
      case "ready_to_pay":
        return "Ghi nhận hợp đồng (chờ thanh toán)";
      case "paid":
        return "Đã thanh toán";
      case "in_progress":
        return "Đang vận chuyển";
      case "completed":
        return "Hoàn thành";
      case "declined":
        return "Từ chối";
      case "cancelled":
        return "Đã huỷ";
      default:
        return code || "";
    }
  }

  function friendlyPaymentStatus(code) {
    switch (code) {
      case "PENDING":
        return "Chưa thanh toán";
      case "PAID":
        return "Đã thanh toán";
      case "FAILED":
        return "Thanh toán thất bại";
      default:
        return code || "";
    }
  }

  function friendlyContractStatus(code) {
    switch (code) {
      case "draft":
        return "Chưa ký";
      case "signed":
        return "Đã ký";
      case "acknowledged":
        return "đã xác nhận";
      case "cancelled":
        return "Hợp đồng bị huỷ";
      default:
        return code || "";
    }
  }

  // Cập nhật chip status trên UI
  function updateBadgeStatus(badgeEl, statusRaw) {
    if (!badgeEl) return;

    const status = normalizeStatus(statusRaw);

    // Xoá class status-* cũ
    badgeEl.classList.forEach((cls) => {
      if (cls.startsWith(STATUS_CLASS_PREFIX)) {
        badgeEl.classList.remove(cls);
      }
    });

    if (status) {
      badgeEl.classList.add(STATUS_CLASS_PREFIX + status);
      badgeEl.dataset.status = status;
    }

    const textSpan = badgeEl.querySelector("span:last-child");
    if (textSpan) {
      textSpan.textContent = friendlyRequestStatus(status);
    }
  }

  function buildApiUrl() {
    const base = "/api/customer/requests";
    if (!selectedStatus || selectedStatus === "ALL") {
      return base;
    }
    return `${base}?status=${encodeURIComponent(selectedStatus)}`;
  }

  // ===== Refresh 1 lần + auto poll =====
  function refreshOnce() {
    const url = buildApiUrl();

    fetch(url, {
      headers: { "X-Requested-With": "XMLHttpRequest" },
    })
      .then((r) => r.json())
      .then((data) => {
        if (!Array.isArray(data)) return;

        data.forEach((item) => {
          const row = root.querySelector(
            `.fm-request-item[data-request-id="${item.requestId}"]`
          );
          if (!row) return;

          // --- REQUEST STATUS: khớp Provider ---
          const statusBadge = row.querySelector(".js-status-badge");
          updateBadgeStatus(statusBadge, item.status);

          // --- PAYMENT BADGE ---
          const paymentBadge = row.querySelector(".js-payment-badge");
          if (paymentBadge) {
            const st = normalizeStatus(item.status);

            // 🚫 Nếu provider TỪ CHỐI hoặc ĐÃ HUỶ -> không hiện gì về thanh toán nữa
            if (st === "declined" || st === "cancelled") {
              paymentBadge.style.display = "none";
            } else if (item.paymentStatus) {
              paymentBadge.style.display = "inline-flex";
              const span = paymentBadge.querySelector("span:last-child");
              if (span) {
                span.textContent =
                  "Thanh toán: " + friendlyPaymentStatus(item.paymentStatus);
              }
            } else {
              paymentBadge.style.display = "none";
            }
          }

          // --- CONTRACT BADGE ---
          const contractBadgeText = row.querySelector(".js-contract-text");
          if (contractBadgeText) {
            if (item.contractId && item.contractStatus) {
              contractBadgeText.textContent =
                friendlyContractStatus(item.contractStatus) +
                " #" +
                item.contractId;
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
          if (totalCostEl) {
            totalCostEl.textContent = item.totalCostFormatted || "—";
          }
        });
      })
      .catch((err) => console.debug("Refresh requests failed:", err));
  }

  refreshOnce();
  setInterval(refreshOnce, 8000);
})();
