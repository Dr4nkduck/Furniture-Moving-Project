// /admin/js/admin-payments.js
(function () {
  const tbody = document.getElementById("paymentsTbody");
  const btnRefresh = document.getElementById("btnRefresh");

  if (!tbody) return;

  const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
  const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
  const csrfHeader = csrfHeaderMeta && csrfHeaderMeta.content;
  const csrfToken = csrfTokenMeta && csrfTokenMeta.content;

  function withCsrf(headers) {
    const h = Object.assign({}, headers || {});
    if (csrfHeader && csrfToken) {
      h[csrfHeader] = csrfToken;
    }
    return h;
  }

  function fmtMoney(v) {
    if (v == null) return "—";
    try {
      return new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
      }).format(v);
    } catch (e) {
      return v;
    }
  }

  function friendlyReqStatus(code) {
    switch (code) {
      case "pending":
        return "Chờ xác nhận";
      case "ready_to_pay":
        return "Chờ thanh toán";
      case "paid":
        return "Đã thanh toán";
      case "in_progress":
        return "Đang vận chuyển";
      case "completed":
        return "Hoàn thành";
      case "cancelled":
        return "Đã huỷ";
      case "declined":
        return "Bị từ chối";
      default:
        return code || "";
    }
  }

  function friendlyPaymentStatus(code) {
    switch (code) {
      case "PENDING":
        return "Chưa xác nhận";
      case "PAID":
        return "Đã thanh toán";
      case "FAILED":
        return "Thất bại";
      default:
        return code || "";
    }
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

  function renderRows(items) {
    tbody.innerHTML = "";

    if (!items || items.length === 0) {
      const tr = document.createElement("tr");
      tr.innerHTML =
        '<td colspan="9" class="text-center text-muted py-3">Không có đơn nào.</td>';
      tbody.appendChild(tr);
      return;
    }

    items.forEach((row) => {
      const tr = document.createElement("tr");

      const isPaid = row.paymentStatus === "PAID";

      tr.innerHTML = `
        <td>#${row.requestId}</td>
        <td><code>${row.paymentRef || ("REQ" + row.requestId)}</code></td>
        <td>${row.customerName || "—"}</td>
        <td>${row.providerName || "—"}</td>
        <td>${row.preferredDate || "—"}</td>
        <td>${fmtMoney(row.amount)}</td>
        <td>${friendlyReqStatus(row.requestStatus)}</td>
        <td>${friendlyPaymentStatus(row.paymentStatus)}</td>
        <td class="text-end">
          <button class="btn btn-sm btn-success btn-confirm" ${
            isPaid ? "disabled" : ""
          }>
            Đã nhận tiền
          </button>
        </td>
      `;

      const btn = tr.querySelector(".btn-confirm");
      btn.addEventListener("click", async () => {
        if (isPaid) return;

        if (
          !confirm(
            `Xác nhận đã nhận đủ tiền cho đơn REQ${row.requestId}?`
          )
        ) {
          return;
        }

        btn.disabled = true;
        btn.innerText = "Đang lưu...";

        try {
          const updated = await fetchJSON(
            `/api/admin/payments/${row.requestId}/confirm`,
            {
              method: "PUT",
              headers: withCsrf({ "Content-Type": "application/json" }),
            }
          );

          // Update dòng với dữ liệu mới
          row.requestStatus = updated.requestStatus;
          row.paymentStatus = updated.paymentStatus;
          row.amount = updated.amount;

          renderRows(
            Array.from(tbody.children).map((trEl) => {
              // đơn giản: reload toàn bộ list sau confirm
              return row;
            })
          );
          // cách đơn giản hơn: gọi load() lại
          await load();
        } catch (e) {
          alert(e.message || "Xác nhận thanh toán thất bại");
          btn.disabled = false;
          btn.innerText = "Đã nhận tiền";
        }
      });

      tbody.appendChild(tr);
    });
  }

  async function load() {
    try {
      const data = await fetchJSON("/api/admin/payments");
      renderRows(data || []);
    } catch (e) {
      console.error(e);
      tbody.innerHTML =
        '<tr><td colspan="9" class="text-center text-danger py-3">Lỗi tải dữ liệu</td></tr>';
    }
  }

  btnRefresh && btnRefresh.addEventListener("click", load);

  load();
})();
