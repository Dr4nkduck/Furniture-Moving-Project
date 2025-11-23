// /provider/js/provider-orders.js
(function () {
  const meta = document.querySelector('meta[name="provider-id"]');
  const providerId =
    (meta && meta.content) ||
    new URLSearchParams(location.search).get("providerId") ||
    1;

  const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
  const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
  const csrfHeader = csrfHeaderMeta && csrfHeaderMeta.content;
  const csrfToken = csrfTokenMeta && csrfTokenMeta.content;

  const tbody = document.getElementById("ordersTbody");
  const searchInput = document.getElementById("searchInput");
  const statusFilter = document.getElementById("statusFilter");
  const btnRefresh = document.getElementById("btnRefresh");

  const dWrap = document.getElementById("detailContent");
  const dEmpty = document.getElementById("emptyDetail");
  const d = {
    id: document.getElementById("d-id"),
    status: document.getElementById("d-status"),
    customer: document.getElementById("d-customer"),
    contact: document.getElementById("d-contact"),
    date: document.getElementById("d-date"),
    cost: document.getElementById("d-cost"),
    pickup: document.getElementById("d-pickup"),
    delivery: document.getElementById("d-delivery"),
    items: document.getElementById("d-items"),
    timeline: document.getElementById("d-timeline"),
    paymentRef: document.getElementById("d-paymentRef"),
  };

  let currentOrderId = null;

  function withCsrf(headers) {
    const h = Object.assign({}, headers || {});
    if (csrfHeader && csrfToken) h[csrfHeader] = csrfToken;
    return h;
  }

  async function fetchJSON(url, options) {
    const res = await fetch(url, options);
    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      throw new Error(txt || "Request failed");
    }
    const ct = res.headers.get("content-type") || "";
    return ct.includes("application/json") ? res.json() : null;
  }

  function fmtMoney(v) {
    if (v == null) return "—";
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(v);
  }

  function humanStatus(s) {
    switch (s) {
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
        return "Đã hủy";
      default:
        return s || "";
    }
  }

  async function loadList() {
    const q = (searchInput && searchInput.value.trim()) || "";
    const status = statusFilter && statusFilter.value;
    const params = new URLSearchParams();
    if (q) params.set("q", q);
    if (status) params.set("status", status);

    const url =
      `/api/providers/${providerId}/orders` +
      (params.toString() ? `?${params.toString()}` : "");
    const rows = await fetchJSON(url);
    tbody.innerHTML = "";

    (rows || []).forEach((r) => {
      const tr = document.createElement("tr");
      const paymentRef = `REQ${r.requestId}`;
      tr.innerHTML = `
        <td class="text-muted">#${r.requestId}</td>
        <td>${r.customerName || ""}</td>
        <td>${r.pickupAddress || ""}</td>
        <td>${r.deliveryAddress || ""}</td>
        <td>${r.preferredDate || ""}</td>
        <td><span class="badge" data-status="${r.status}">${humanStatus(
          r.status
        )}</span></td>
        <td>${paymentRef}</td>
        <td class="text-end">${fmtMoney(r.totalCost)}</td>
      `;
      tr.addEventListener("click", () => loadDetail(r.requestId));
      tbody.appendChild(tr);
    });
  }

  function syncActionButtons(status) {
    const btnAccept = document.querySelector('.actions [data-act="accepted"]');
    const btnStart = document.querySelector(
      '.actions [data-act="in_progress"]'
    );
    const btnComplete = document.querySelector(
      '.actions [data-act="completed"]'
    );
    const btnCancel = document.querySelector(
      '.actions [data-act="cancelled"]'
    );
    const btnDecline = document.querySelector(
      '.actions [data-act="declined"]'
    );

    const all = [btnAccept, btnStart, btnComplete, btnCancel, btnDecline];
    all.forEach((b) => b && (b.disabled = false));

    switch (status) {
      case "pending":
        // chỉ cho phép: ghi nhận HĐ, hủy, từ chối
        btnStart && (btnStart.disabled = true);
        btnComplete && (btnComplete.disabled = true);
        break;
      case "ready_to_pay":
        // Đã ghi nhận HĐ, chờ khách chuyển tiền: không cho vận chuyển
        btnAccept && (btnAccept.disabled = true);
        btnStart && (btnStart.disabled = true);
        btnComplete && (btnComplete.disabled = true);
        btnDecline && (btnDecline.disabled = true);
        // vẫn cho hủy trong giai đoạn chưa thanh toán
        break;
      case "paid":
        // ✅ Đã thanh toán: KHÔNG được hủy / từ chối, chỉ được bắt đầu vận chuyển
        btnAccept && (btnAccept.disabled = true);
        btnDecline && (btnDecline.disabled = true);
        btnCancel && (btnCancel.disabled = true); // <-- chặn hủy sau khi thanh toán
        btnComplete && (btnComplete.disabled = true); // chỉ in_progress mới được hoàn thành
        break;
      case "in_progress":
        // Đang vận chuyển (sau khi đã thanh toán): chỉ cho Hoàn thành
        btnAccept && (btnAccept.disabled = true);
        btnStart && (btnStart.disabled = true);
        btnDecline && (btnDecline.disabled = true);
        btnCancel && (btnCancel.disabled = true); // <-- không cho hủy nữa
        break;
      default:
        // completed / cancelled / declined: khoá hết
        all.forEach((b) => b && (b.disabled = true));
    }
  }

  async function loadDetail(orderId) {
    const dto = await fetchJSON(
      `/api/providers/${providerId}/orders/${orderId}`
    );
    currentOrderId = orderId;

    dEmpty.classList.add("d-none");
    dWrap.classList.remove("d-none");

    d.id.textContent = dto.requestId;
    d.status.textContent = humanStatus(dto.status);
    d.status.dataset.status = dto.status || "";
    d.customer.textContent = dto.customerName || "";
    d.contact.textContent = [dto.customerPhone, dto.customerEmail]
      .filter(Boolean)
      .join(" • ");
    d.date.textContent = dto.preferredDate || "";
    d.cost.textContent = fmtMoney(dto.totalCostEstimate);
    d.pickup.textContent = dto.pickupFull || "";
    d.delivery.textContent = dto.deliveryFull || "";

    if (d.paymentRef) d.paymentRef.textContent = `REQ${dto.requestId}`;

    d.items.innerHTML = "";
    (dto.items || []).forEach((i) => {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${i.itemType || ""}</td>
        <td>${i.size || ""}</td>
        <td>${i.quantity || 0}</td>
        <td>${i.fragile ? "Có" : "Không"}</td>
      `;
      d.items.appendChild(tr);
    });

    d.timeline.innerHTML = "";
    const steps = ["pending", "ready_to_pay", "paid", "in_progress", "completed"];
    const idx = steps.indexOf(dto.status);
    steps.forEach((s, i) => {
      const div = document.createElement("div");
      div.className = "t" + (idx >= i && idx !== -1 ? " active" : "");
      div.textContent = humanStatus(s);
      d.timeline.appendChild(div);
    });

    // Cập nhật enable/disable action buttons tuỳ trạng thái
    syncActionButtons(dto.status);

    // Wire actions
    document.querySelectorAll(".actions [data-act]").forEach((btn) => {
      btn.onclick = async () => {
        const act = btn.getAttribute("data-act");

        // accepted -> ready_to_pay (ghi nhận HĐ)
        const targetStatus = act === "accepted" ? "ready_to_pay" : act;

        const currentStatus = d.status.dataset.status || "";

        // ===== EXTRA GUARD: chặn hủy / từ chối sau khi đã thanh toán =====
        if (
          (currentStatus === "paid" || currentStatus === "in_progress" || currentStatus === "completed") &&
          (targetStatus === "cancelled" || targetStatus === "declined")
        ) {
          alert("Đơn đã thanh toán, không thể hủy hoặc từ chối nữa.");
          return;
        }

        let body = { status: targetStatus };

        if (targetStatus === "cancelled") {
          const reason = prompt(
            "Lý do hủy (tuỳ chọn):",
            "Khách thay đổi kế hoạch"
          );
          if (reason) body.cancelReason = reason;
        }

        try {
          const res = await fetch(
            `/api/providers/${providerId}/orders/${orderId}/status`,
            {
              method: "PUT",
              headers: withCsrf({ "Content-Type": "application/json" }),
              body: JSON.stringify(body),
            }
          );
          if (!res.ok) {
            const msg = await res.text();
            alert(msg || "Cập nhật trạng thái thất bại");
            return;
          }
          await loadDetail(orderId);
          await loadList();
        } catch (e) {
          alert(e.message || "Cập nhật trạng thái thất bại");
        }
      };
    });
  }

  btnRefresh && btnRefresh.addEventListener("click", loadList);
  searchInput &&
    searchInput.addEventListener("keydown", (e) => {
      if (e.key === "Enter") loadList();
    });

  loadList();
})();
