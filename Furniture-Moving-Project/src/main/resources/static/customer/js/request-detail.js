// /customer/js/request-detail.js
(() => {
  const root = document.querySelector("[data-request-id]");
  if (!root) return;

  const requestId = root.getAttribute("data-request-id");

  const elStatus = root.querySelector(".js-status");
  const elPayment = root.querySelector(".js-payment-status");
  const elPaymentEmpty = root.querySelector(".js-payment-empty"); // khi paymentStatus null
  const elPaymentType = root.querySelector(".js-payment-type");   // hình thức thanh toán

  const elPaidAt = root.querySelector(".js-paid-at");
  const elTotal = root.querySelector(".js-total");
  const elDeposit = root.querySelector(".js-deposit");

  const elContractId = root.querySelector(".js-contract-id");
  const elContractStatus = root.querySelector(".js-contract-status");
  const elContractSigned = root.querySelector(".js-contract-signed");
  const elContractAck = root.querySelector(".js-contract-ack");

  // ===== Friendly text mapping =====
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

  function friendlyPaymentType(code) {
    switch (code) {
      case "DEPOSIT_20": return "Đặt cọc 20%";
      case "FULL": return "Thanh toán toàn bộ";
      default: return code || "";
    }
  }

  function updateBadgeClass(el, status) {
    if (!el) return;
    el.classList.forEach((cls) => {
      if (cls.startsWith("status-")) el.classList.remove(cls);
    });
    if (status) el.classList.add("status-" + status);
  }

  function refreshOnce() {
    fetch(`/api/customer/request/${requestId}`, {
      headers: { "X-Requested-With": "XMLHttpRequest" }
    })
      .then(r => r.json())
      .then(d => {
        if (!d) return;

        // ---- REQUEST STATUS ----
        if (elStatus) {
          updateBadgeClass(elStatus, d.status);
          const span = elStatus.querySelector("span:last-child");
          if (span) span.textContent = friendlyRequestStatus(d.status);
        }

        // ---- PAYMENT STATUS ----
        if (elPayment || elPaymentEmpty) {
          if (d.paymentStatus) {
            // đã có trạng thái thanh toán
            if (elPayment) {
              elPayment.style.display = "inline-flex";
              const span = elPayment.querySelector("span:last-child");
              if (span) span.textContent = friendlyPaymentStatus(d.paymentStatus);
            }
            if (elPaymentEmpty) {
              elPaymentEmpty.style.display = "none";
            }
          } else {
            // chưa có trạng thái thanh toán
            if (elPayment) {
              elPayment.style.display = "none";
            }
            if (elPaymentEmpty) {
              elPaymentEmpty.style.display = "inline";
              elPaymentEmpty.textContent = "—";
            }
          }
        }

        // ---- PAYMENT TYPE (Đặt cọc / Full) ----
        if (elPaymentType) {
          if (d.paymentType) {
            elPaymentType.textContent = friendlyPaymentType(d.paymentType);
          } else {
            elPaymentType.textContent = "—";
          }
        }

        // ---- TIỀN & THỜI GIAN ----
        if (elPaidAt) elPaidAt.textContent = d.paidAtFormatted || "—";
        if (elTotal) elTotal.textContent = d.totalCostFormatted || "—";
        if (elDeposit) elDeposit.textContent = d.depositFormatted || "—";

        // ---- CONTRACT ----
        if (elContractId) elContractId.textContent = d.contractId || "—";

        if (elContractStatus) {
          if (d.contractStatus) {
            elContractStatus.style.display = "inline-flex";
            const span = elContractStatus.querySelector("span:last-child");
            if (span) span.textContent = friendlyContractStatus(d.contractStatus);
          } else {
            elContractStatus.style.display = "none";
          }
        }

        if (elContractSigned) {
          elContractSigned.textContent = d.contractSignedAtFormatted || "—";
        }

        if (elContractAck) {
          elContractAck.textContent = d.contractAckAtFormatted || "—";
        }
      })
      .catch(err => console.debug("Realtime detail error:", err));
  }

  refreshOnce();
  setInterval(refreshOnce, 8000);
})();
