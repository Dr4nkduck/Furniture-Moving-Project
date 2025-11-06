(function () {
    const meta = (name) => document.querySelector(`meta[name="${name}"]`)?.content || "";
    const providerId = meta("provider-id");
    const company = meta("provider-company");

    const els = {
        list: document.getElementById("summaryList"),
        detail: document.getElementById("detailPanel"),
        q: document.getElementById("searchBox"),
        status: document.getElementById("statusFilter"),
        btnSearch: document.getElementById("btnSearch"),
        modal: document.getElementById("statusModal"),
        statusSelect: document.getElementById("statusSelect"),
        cancelReason: document.getElementById("cancelReason"),
        modalCancel: document.getElementById("modalCancel"),
        modalSave: document.getElementById("modalSave"),
    };

    let pagination = {page: 0, size: 10, totalPages: 0};
    let currentOrderId = null;

    function viStatus(st) {
        switch (st) {
            case "pending":
                return "Chờ vận chuyển";
            case "assigned":
                return "Đã phân công";
            case "in_progress":
                return "Đang vận chuyển";
            case "completed":
                return "Vận chuyển hoàn";
            case "cancelled":
                return "Đã hủy";
            default:
                return st;
        }
    }

    async function fetchOrders() {
        const params = new URLSearchParams({
            q: els.q.value.trim(),
            status: els.status.value,
            page: pagination.page,
            size: pagination.size
        });
        const url = `/provider/orders/api/${providerId}?` + params.toString();
        const res = await fetch(url);
        const data = await res.json();
        pagination.totalPages = data.totalPages;
        renderList(data.content);
    }

    function renderList(items) {
        els.list.innerHTML = "";
        if (!items.length) {
            els.list.innerHTML = `<div class="empty">Không có đơn nào</div>`;
            return;
        }
        for (const it of items) {
            const card = document.createElement("div");
            card.className = "order-card";
            card.innerHTML = `
        <div class="row1">
          <b>#${it.requestId}</b>
          <span class="badge ${it.status}">${viStatus(it.status)}</span>
          <span class="date">${it.preferredDate}</span>
        </div>
        <div class="row2">
          <div class="name">${it.customerName}</div>
          <div class="route">
            <span title="Điểm lấy">${it.pickupAddress}</span>
            <span class="arrow">→</span>
            <span title="Điểm giao">${it.deliveryAddress}</span>
          </div>
        </div>
        <div class="row3">
          <div class="cost">Ước tính: ${it.totalCost != null ? it.totalCost : "—"}</div>
          <button data-id="${it.requestId}" class="btn btn-detail">Xem chi tiết</button>
        </div>
      `;
            card.querySelector(".btn-detail").onclick = () => loadDetail(it.requestId);
            els.list.appendChild(card);
        }

        // simple pager
        const pager = document.createElement("div");
        pager.className = "pager";
        pager.innerHTML = `
      <button ${pagination.page <= 0 ? 'disabled' : ''} id="prev">◀</button>
      <span>Trang ${pagination.page + 1}/${Math.max(1, pagination.totalPages)}</span>
      <button ${pagination.page >= pagination.totalPages - 1 ? 'disabled' : ''} id="next">▶</button>
    `;
        els.list.appendChild(pager);
        pager.querySelector("#prev").onclick = () => {
            pagination.page--;
            fetchOrders();
        };
        pager.querySelector("#next").onclick = () => {
            pagination.page++;
            fetchOrders();
        };
    }

    async function loadDetail(id) {
        const res = await fetch(`/provider/orders/api/${providerId}/${id}`);
        const d = await res.json();
        currentOrderId = id;

        els.detail.innerHTML = `
      <div class="detail-head">
        <div>
          <h2>Đơn #${d.requestId} <span class="badge ${d.status}">${viStatus(d.status)}</span></h2>
          <div class="sub">Ngày yêu cầu: ${d.requestDate} • Ngày chuyển: ${d.preferredDate}</div>
          <div class="route">
            <div><b>Điểm lấy:</b> ${d.pickupAddress}</div>
            <div><b>Điểm giao:</b> ${d.deliveryAddress}</div>
          </div>
        </div>
        <div class="actions">
          <button class="btn" id="btnUpdate">Cập nhật trạng thái</button>
        </div>
      </div>

      <div class="grid">
        <div class="card">
          <h3>Khách hàng</h3>
          <p><b>${d.customerName}</b></p>
          <p>${d.customerPhone || ""}</p>
          <p>${d.customerEmail || ""}</p>
        </div>
        <div class="card">
          <h3>Ước tính</h3>
          <p class="total">${d.totalCost != null ? d.totalCost : "—"}</p>
          ${d.cancelReason ? `<p class="cancel">Lý do hủy: ${d.cancelReason}</p>` : ""}
        </div>
      </div>

      <div class="card">
        <h3>Nội thất vận chuyển</h3>
        <table class="items">
          <thead><tr><th>Tên</th><th>Kích thước</th><th>Số lượng</th><th>Hàng dễ vỡ</th></tr></thead>
          <tbody>
            ${d.items.map(x => `
              <tr>
                <td>${x.itemType || ""}</td>
                <td>${x.size || ""}</td>
                <td>${x.quantity}</td>
                <td>${x.fragile ? "✔" : ""}</td>
              </tr>`).join("")}
          </tbody>
        </table>
      </div>

      <div class="timeline card">
        <h3>Lịch trình</h3>
        <ul>
          <li><b>Tạo đơn:</b> ${d.requestDate}</li>
          <li><b>Ngày chuyển dự kiến:</b> ${d.preferredDate}</li>
          <li><b>Trạng thái hiện tại:</b> ${viStatus(d.status)}</li>
        </ul>
      </div>
    `;

        document.getElementById("btnUpdate").onclick = openModal;
    }

    function openModal() {
        els.modal.classList.remove("hidden");
        els.cancelReason.value = "";
    }

    function closeModal() {
        els.modal.classList.add("hidden");
    }

    els.modalCancel.onclick = closeModal;
    els.modalSave.onclick = async function () {
        if (!currentOrderId) return;
        const body = {
            status: els.statusSelect.value,
            cancelReason: els.cancelReason.value.trim() || null
        };
        const res = await fetch(`/provider/orders/api/${providerId}/${currentOrderId}/status`, {
            method: "PUT", headers: {"Content-Type": "application/json"},
            body: JSON.stringify(body)
        });
        if (res.ok) {
            closeModal();
            fetchOrders();
            loadDetail(currentOrderId);
        } else {
            alert("Không thể cập nhật trạng thái.");
        }
    };

    els.btnSearch.onclick = () => {
        pagination.page = 0;
        fetchOrders();
    };

    // init
    if (!providerId) {
        els.list.innerHTML = `<div class="empty">Thiếu providerId. Gọi trang với ?providerId=...</div>`;
    } else {
        fetchOrders();
    }
})();
