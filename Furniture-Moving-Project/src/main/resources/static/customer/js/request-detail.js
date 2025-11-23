// /customer/js/request-detail.js
(() => {
  const root = document.querySelector("[data-request-id]");
  if (!root) return;

  const requestId = root.getAttribute("data-request-id");

  // ====== STEPPER (5 BƯỚC TRÊN ĐẦU) ======
  const stepEls = root.querySelectorAll(".fm-stepper .fm-step");

  function mapStatusToStepIndex(code) {
    const s = (code || "").toLowerCase();
    if (s === "pending") return 0;                         // 1. Hợp đồng & Yêu cầu
    if (s === "ready_to_pay" || s === "paid") return 1;    // 2. Thanh toán
    if (s === "in_progress") return 2;                     // 3. Thực hiện
    if (s === "completed") return 3;                       // 4. Hoàn tất
    // nếu sau này có trạng thái "reviewed" thì có thể return 4 cho bước Đánh giá
    return -1;
  }

  function applyStepper(status) {
    if (!stepEls || !stepEls.length) return;
    const idx = mapStatusToStepIndex(status);

    stepEls.forEach((el, i) => {
      // hiển thị 1 step active duy nhất
      el.classList.toggle("fm-step--active", i === idx);
      // nếu muốn có "đã hoàn thành" thì thêm class khác, VD:
      // el.classList.toggle("fm-step--done", i < idx);
    });
  }

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

  // ✅ Nút thanh toán (được show/hide realtime)
  const elPaymentBtn = root.querySelector(".js-payment-btn");

  // ✅ Nút HỦY ĐƠN trực tiếp (giai đoạn 1 – tùy backend cho phép)
  const btnCancel = root.querySelector(".js-cancel-request");

  // ✅ Nút YÊU CẦU HỦY ĐƠN (giai đoạn 2 – sau thanh toán, chờ provider duyệt)
  const btnCancelPaid = root.querySelector(".js-cancel-request-paid");

  // ===== RATING ELEMENTS =====
  const elRatingButton =
    root.querySelector(".js-rating-open") ||
    root.querySelector("#open-rating-btn");
  const elRatingSection =
    root.querySelector(".js-rating-section") ||
    root.querySelector("#rating-section");
  const ratingStars = root.querySelectorAll(
    ".js-rating-stars .star, #rating-stars .star"
  );
  const elRatingComment =
    root.querySelector(".js-rating-comment") ||
    root.querySelector("#rating-comment");
  const elRatingStatus =
    root.querySelector(".js-rating-status") ||
    root.querySelector("#rating-status");
  const elRatingSubmit =
    root.querySelector(".js-rating-submit") ||
    root.querySelector("#rating-submit-btn");
  const elRatingClose = root.querySelector(".js-rating-close");

  let currentRating = 0;
  let ratingInitialized = false;

  // ===== CSRF helpers =====
  const csrfToken =
    document.querySelector('meta[name="_csrf"]')?.content || null;
  const csrfHeaderName =
    document.querySelector('meta[name="_csrf_header"]')?.content || null;

  function buildJsonHeaders() {
    const headers = { "Content-Type": "application/json" };
    if (csrfToken && csrfHeaderName) {
      headers[csrfHeaderName] = csrfToken;
    }
    return headers;
  }

  // ===== Friendly text mapping =====
  function friendlyRequestStatus(code) {
    switch ((code || "").toLowerCase()) {
      case "pending":
        return "Đang chờ xác nhận";
      case "ready_to_pay":
        return "Chờ thanh toán";
      case "paid":
        return "Đã thanh toán";
      case "in_progress":
        return "Đang thực hiện";
      case "completed":
        return "Hoàn tất dịch vụ";
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
        return "Đơn vị vận chuyển đã xác nhận";
      case "cancelled":
        return "Hợp đồng bị huỷ";
      default:
        return code || "";
    }
  }

  function friendlyPaymentType(code) {
    switch (code) {
      case "DEPOSIT_20":
        return "Đặt cọc 20%";
      case "FULL":
        return "Thanh toán toàn bộ";
      default:
        return code || "";
    }
  }

  function updateBadgeClass(el, status) {
    if (!el) return;
    el.classList.forEach((cls) => {
      if (cls.startsWith("status-")) el.classList.remove(cls);
    });
    if (status) el.classList.add("status-" + status);
  }

  // ===== PAYMENT BUTTON LOGIC (realtime) =====
  // Tính xem ở thời điểm hiện tại có cho phép thanh toán không
  function canPayFromDetail(d) {
    if (!d) return false;

    const status = (d.status || "").toLowerCase();
    const contractStatus = (d.contractStatus || "").toLowerCase();
    const paymentStatus = d.paymentStatus || null;

    // Logic FE:
    // - Request phải READY_TO_PAY
    // - Hợp đồng đã được acknowledged (đơn vị vận chuyển xác nhận)
    // - Chưa PAID
    const readyToPay = status === "ready_to_pay";
    const contractOk = contractStatus === "acknowledged"; // hoặc 'signed' tuỳ bạn
    const notPaid = !paymentStatus || paymentStatus !== "PAID";

    return readyToPay && contractOk && notPaid;
  }

  function updatePaymentButton(d) {
    if (!elPaymentBtn) return;

    if (canPayFromDetail(d)) {
      elPaymentBtn.style.display = "inline-flex";
    } else {
      elPaymentBtn.style.display = "none";
    }
  }

  // ===== RATING: UI helpers =====
  function updateStarUI() {
    if (!ratingStars || ratingStars.length === 0) return;
    ratingStars.forEach((star) => {
      const value = parseInt(star.getAttribute("data-value"), 10);
      if (!isNaN(value) && value <= currentRating) {
        star.classList.add("active");
      } else {
        star.classList.remove("active");
      }
    });
  }

  function loadExistingRating() {
    if (!elRatingButton) return;

    fetch(`/api/customer/request/${requestId}/rating`, {
      headers: { Accept: "application/json" },
    })
      .then((res) => {
        if (res.status === 404) {
          // chưa có rating
          if (elRatingStatus) elRatingStatus.textContent = "";
          return null;
        }
        if (!res.ok) {
          throw new Error("Failed to load rating");
        }
        return res.json();
      })
      .then((data) => {
        if (!data) return;

        currentRating = data.rating;
        updateStarUI();

        if (elRatingComment) {
          elRatingComment.value = data.comment || "";
        }
        if (elRatingStatus) {
          elRatingStatus.textContent =
            "Bạn đã đánh giá đơn này, có thể chỉnh sửa nếu muốn.";
        }
        if (elRatingSection) {
          elRatingSection.style.display = "";
        }
        if (elRatingButton) {
          elRatingButton.textContent = "Sửa đánh giá";
        }
      })
      .catch((err) => console.debug("Rating load error:", err));
  }

  function handleRatingVisibility(detail) {
    if (!elRatingButton) return;
    const status = (detail.status || "").toLowerCase();
    if (status === "completed") {
      // Đơn đã hoàn thành => cho phép đánh giá
      elRatingButton.style.display = "inline-flex";
      if (!ratingInitialized) {
        ratingInitialized = true;
        loadExistingRating();
      }
    } else {
      // Các trạng thái khác => ẩn rating
      elRatingButton.style.display = "none";
      if (elRatingSection) {
        elRatingSection.style.display = "none";
      }
    }
  }

  function attachRatingEvents() {
    // Mở form khi bấm nút "Đánh giá"
    if (elRatingButton && elRatingSection) {
      elRatingButton.addEventListener("click", () => {
        elRatingSection.style.display = ""; // luôn mở ra
      });
    }

    // Đóng form khi bấm nút "Đóng"
    if (elRatingClose && elRatingSection) {
      elRatingClose.addEventListener("click", () => {
        elRatingSection.style.display = "none";
      });
    }

    // Click chọn sao
    if (ratingStars && ratingStars.length > 0) {
      ratingStars.forEach((star) => {
        star.addEventListener("click", () => {
          const value = parseInt(star.getAttribute("data-value"), 10);
          if (!isNaN(value)) {
            currentRating = value;
            updateStarUI();
          }
        });
      });
    }

    // Submit rating
    if (elRatingSubmit) {
      elRatingSubmit.addEventListener("click", () => {
        if (!currentRating) {
          if (elRatingStatus) {
            elRatingStatus.textContent =
              "Vui lòng chọn số sao từ 1 đến 5.";
          }
          return;
        }

        const payload = {
          rating: currentRating,
          comment: elRatingComment ? elRatingComment.value.trim() : "",
        };

        fetch(`/api/customer/request/${requestId}/rating`, {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(payload),
        })
          .then((res) => {
            if (!res.ok) {
              if (elRatingStatus) {
                if (res.status === 400) {
                  elRatingStatus.textContent =
                    "Đơn chưa hoàn thành hoặc dữ liệu không hợp lệ.";
                } else {
                  elRatingStatus.textContent =
                    "Không thể lưu đánh giá. Vui lòng thử lại.";
                }
              }
              throw new Error("Failed to save rating");
            }
            return res.json();
          })
          .then((data) => {
            currentRating = data.rating;
            updateStarUI();
            if (elRatingStatus) {
              elRatingStatus.textContent =
                "Đã lưu đánh giá. Cảm ơn bạn!";
            }
            if (elRatingButton) {
              elRatingButton.textContent = "Sửa đánh giá";
            }
          })
          .catch((err) => console.debug("Rating save error:", err));
      });
    }
  }

  // ===== CANCEL: Hủy đơn trực tiếp (giai đoạn 1) =====
  function setupCancel() {
    if (!btnCancel) return;

    btnCancel.addEventListener("click", () => {
      if (!confirm("Bạn có chắc chắn muốn hủy đơn?")) return;

      const reason = (prompt("Nhập lý do hủy (không bắt buộc):") || "").trim();

      fetch(`/api/customer/request/${requestId}/cancel`, {
        method: "POST",
        headers: buildJsonHeaders(),
        body: JSON.stringify({ reason }),
      })
        .then((res) =>
          res
            .json()
            .catch(() => ({}))
            .then((data) => ({ ok: res.ok, status: res.status, data }))
        )
        .then(({ ok, data }) => {
          const msg =
            data.message ||
            (ok
              ? "Hủy đơn thành công."
              : "Không thể hủy đơn. Vui lòng thử lại.");
          alert(msg);

          if (ok && data.success) {
            window.location.reload();
          }
        })
        .catch((err) => {
          console.error("Cancel request error:", err);
          alert("Có lỗi xảy ra. Vui lòng thử lại sau.");
        });
    });
  }

  // ===== CANCEL REQUEST: Yêu cầu hủy đơn (giai đoạn 2) =====
  function setupCancelPaid() {
    if (!btnCancelPaid) return;

    btnCancelPaid.addEventListener("click", () => {
      if (
        !confirm(
          "Bạn muốn gửi yêu cầu hủy đơn? Đơn vị vận chuyển sẽ xem xét và phản hồi."
        )
      ) {
        return;
      }

      // Lý do hủy: BẮT BUỘC
      let reason = "";
      while (!reason) {
        const input = prompt("Nhập lý do hủy (bắt buộc):") || "";
        reason = input.trim();
        if (reason) break;

        const retry = confirm(
          "Lý do hủy không được để trống. Bạn có muốn nhập lại không?"
        );
        if (!retry) {
          return;
        }
      }

      fetch(`/api/customer/request/${requestId}/cancel-request`, {
        method: "POST",
        headers: buildJsonHeaders(),
        body: JSON.stringify({ reason }),
      })
        .then((res) =>
          res
            .json()
            .catch(() => ({}))
            .then((data) => ({ ok: res.ok, status: res.status, data }))
        )
        .then(({ ok, data }) => {
          const msg =
            data.message ||
            (ok
              ? "Đã gửi yêu cầu hủy đơn. Vui lòng chờ đơn vị vận chuyển xét duyệt."
              : "Không thể gửi yêu cầu hủy. Vui lòng thử lại.");
          alert(msg);

          if (ok && data.success) {
            // Reload để hiển thị hasPendingCancel + ẩn nút yêu cầu hủy
            window.location.reload();
          }
        })
        .catch((err) => {
          console.error("Cancel-request error:", err);
          alert("Có lỗi xảy ra. Vui lòng thử lại sau.");
        });
    });
  }

  // Gắn event cho rating + cancel
  attachRatingEvents();
  setupCancel();
  setupCancelPaid();

  function refreshOnce() {
    fetch(`/api/customer/request/${requestId}`, {
      headers: { "X-Requested-With": "XMLHttpRequest" },
    })
      .then((r) => r.json())
      .then((d) => {
        if (!d) return;

        // ====== CẬP NHẬT STEPPER THEO STATUS ======
        applyStepper(d.status);

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
              if (span)
                span.textContent = friendlyPaymentStatus(d.paymentStatus);
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
            if (span)
              span.textContent = friendlyContractStatus(d.contractStatus);
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

        // ---- RATING VISIBILITY ----
        handleRatingVisibility(d);

        // ---- PAYMENT BUTTON REALTIME ----
        updatePaymentButton(d);
      })
      .catch((err) => console.debug("Realtime detail error:", err));
  }

  refreshOnce();
  setInterval(refreshOnce, 8000);
})();
