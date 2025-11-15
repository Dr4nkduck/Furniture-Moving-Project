(function () {
    const meta = document.querySelector('meta[name="provider-id"]');
    const providerId = meta && meta.content ? meta.content :
        (new URLSearchParams(location.search).get('providerId') || 1);

    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    const csrfTokenMeta  = document.querySelector('meta[name="_csrf"]');
    const csrfHeader = csrfHeaderMeta && csrfHeaderMeta.content;
    const csrfToken  = csrfTokenMeta && csrfTokenMeta.content;

    const tbody = document.getElementById('ordersTbody');
    const searchInput = document.getElementById('searchInput');
    const statusFilter = document.getElementById('statusFilter');
    const btnRefresh = document.getElementById('btnRefresh');

    const dWrap = document.getElementById('detailContent');
    const dEmpty = document.getElementById('emptyDetail');
    const d = {
        id: document.getElementById('d-id'),
        status: document.getElementById('d-status'),
        customer: document.getElementById('d-customer'),
        contact: document.getElementById('d-contact'),
        date: document.getElementById('d-date'),
        cost: document.getElementById('d-cost'),
        pickup: document.getElementById('d-pickup'),
        delivery: document.getElementById('d-delivery'),
        items: document.getElementById('d-items'),
        timeline: document.getElementById('d-timeline'),
        // 🔹 Thêm element hiển thị mã thanh toán REQ(id) ở panel chi tiết
        paymentRef: document.getElementById('d-paymentRef')
    };

    const btnConfirmPaid = document.getElementById('btnConfirmPaid');

    let currentOrderId = null;

    function withCsrf(headers) {
        const h = Object.assign({}, headers || {});
        if (csrfHeader && csrfToken) {
            h[csrfHeader] = csrfToken;
        }
        return h;
    }

    async function fetchJSON(url, options) {
        const res = await fetch(url, options);
        if (!res.ok) {
            const txt = await res.text().catch(() => '');
            throw new Error(txt || 'Request failed');
        }
        const ct = res.headers.get('content-type') || '';
        return ct.includes('application/json') ? res.json() : null;
    }

    function fmtMoney(v) {
        if (v == null) return '—';
        return new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'}).format(v);
    }

    function setBadge(el, status) {
        el.textContent = humanStatus(status);
        el.dataset.status = status;
        el.className = 'badge';
        el.setAttribute('data-status', status);
    }

    function humanStatus(s) {
        switch (s) {
            case 'pending':
                return 'Chờ xác nhận';
            case 'accepted':
                return 'Đã ghi nhận hợp đồng';
            case 'ready_to_pay':
                return 'Chờ khách thanh toán';
            case 'paid':
                return 'Đã thanh toán';
            case 'in_progress':
                return 'Đang vận chuyển';
            case 'completed':
                return 'Hoàn thành';
            case 'declined':
                return 'Đã từ chối';
            case 'cancelled':
                return 'Đã hủy';
            default:
                return s || '';
        }
    }

    // 🔹 Helper: đọc message lỗi từ backend (JSON hoặc text)
    async function readErrorMessage(res, fallback) {
        let msg = fallback || 'Đã xảy ra lỗi. Vui lòng thử lại.';
        const ct = res.headers.get('content-type') || '';
        try {
            if (ct.includes('application/json')) {
                const data = await res.json();
                if (data && typeof data.message === 'string' && data.message.trim()) {
                    msg = data.message.trim();
                }
            } else {
                const text = await res.text();
                if (text && text.trim()) {
                    msg = text.trim();
                }
            }
        } catch (e) {
            // ignore
        }
        return msg;
    }

    // 🔹 Helper: show toast nếu có, fallback alert
    function showMessage(msg, type) {
        if (typeof notify === 'function') {
            notify(msg, type || 'info');
        } else {
            alert(msg);
        }
    }

    // 🔹 Luật action theo status (đồng bộ với backend)
    function allowedActionsForStatus(status) {
        const s = (status || '').toLowerCase();
        switch (s) {
            case 'pending':
                return ['accepted', 'declined', 'cancelled'];
            case 'ready_to_pay':
            case 'paid':
                return ['in_progress', 'cancelled'];
            case 'in_progress':
                return ['completed', 'cancelled'];
            // completed / cancelled / declined: không cho thao tác gì thêm
            default:
                return [];
        }
    }

    async function loadList() {
        const q = searchInput.value.trim();
        const status = statusFilter.value;
        const url = `/api/providers/${providerId}/orders?` + new URLSearchParams({q, status}).toString();
        const rows = await fetchJSON(url);
        tbody.innerHTML = '';
        (rows || []).forEach(r => {
            const tr = document.createElement('tr');
            const paymentRef = `REQ${r.requestId}`; // 🔹 Mã thanh toán chuẩn cho đối soát sao kê
            tr.innerHTML = `
        <td class="text-muted">#${r.requestId}</td>
        <td>${r.customerName || ''}</td>
        <td>${r.pickupAddress || ''}</td>
        <td>${r.deliveryAddress || ''}</td>
        <td>${r.preferredDate || ''}</td>
        <td><span class="badge" data-status="${r.status}">${humanStatus(r.status)}</span></td>
        <td>${paymentRef}</td>
        <td class="text-end">${fmtMoney(r.totalCost)}</td>
      `;
            tr.addEventListener('click', () => loadDetail(r.requestId));
            tbody.appendChild(tr);
        });
    }

    async function loadDetail(orderId) {
        const dto = await fetchJSON(`/api/providers/${providerId}/orders/${orderId}`);
        currentOrderId = orderId;

        dEmpty.classList.add('d-none');
        dWrap.classList.remove('d-none');

        d.id.textContent = dto.requestId;
        setBadge(d.status, dto.status);
        d.customer.textContent = dto.customerName || '';
        d.contact.textContent = [dto.customerPhone, dto.customerEmail].filter(Boolean).join(' • ');
        d.date.textContent = dto.preferredDate || '';
        d.cost.textContent = fmtMoney(dto.totalCostEstimate);
        d.pickup.textContent = dto.pickupFull || '';
        d.delivery.textContent = dto.deliveryFull || '';

        // 🔹 Hiển thị REQ(id) trong phần chi tiết
        if (d.paymentRef) {
            d.paymentRef.textContent = `REQ${dto.requestId}`;
        }

        d.items.innerHTML = '';
        (dto.items || []).forEach(i => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
        <td>${i.itemType || ''}</td>
        <td>${i.size || ''}</td>
        <td>${i.quantity || 0}</td>
        <td>${i.fragile ? 'Có' : 'Không'}</td>
      `;
            d.items.appendChild(tr);
        });

        // Simple visual timeline
        d.timeline.innerHTML = '';
        const steps = ['pending', 'accepted', 'ready_to_pay', 'paid', 'in_progress', 'completed'];
        const idx = steps.indexOf(dto.status);
        steps.forEach((s, i) => {
            const div = document.createElement('div');
            div.className = 't' + (idx >= i ? ' active' : '');
            div.textContent = humanStatus(s);
            d.timeline.appendChild(div);
        });

        // 🔹 Wire actions: status transitions (theo allowedActionsForStatus)
        const allowed = new Set(allowedActionsForStatus(dto.status));

        document.querySelectorAll('.actions [data-act]').forEach(btn => {
            const act = btn.getAttribute('data-act');
            const isAllowed = allowed.has(act);

            btn.disabled = !isAllowed;
            btn.classList.toggle('disabled', !isAllowed);
            btn.title = !isAllowed
                ? 'Trạng thái hiện tại không cho phép thao tác này.'
                : '';

            btn.onclick = !isAllowed ? null : async () => {
                let body = {status: act};
                if (act === 'cancelled') {
                    const reason = prompt('Lý do hủy (tuỳ chọn):', 'Khách thay đổi kế hoạch');
                    if (reason) body.cancelReason = reason;
                }
                try {
                    const res = await fetch(`/api/providers/${providerId}/orders/${orderId}/status`, {
                        method: 'PUT',
                        headers: withCsrf({'Content-Type': 'application/json', 'Accept': 'application/json'}),
                        body: JSON.stringify(body)
                    });
                    if (!res.ok) {
                        const msg = await readErrorMessage(
                            res,
                            'Cập nhật trạng thái thất bại. Vui lòng thử lại.'
                        );
                        showMessage(msg, res.status === 409 ? 'warn' : 'error');
                        return;
                    }
                    showMessage('Cập nhật trạng thái thành công.', 'success');
                    await loadDetail(orderId);
                    await loadList();
                } catch (e) {
                    console.error(e);
                    showMessage('Có lỗi xảy ra khi cập nhật trạng thái. Vui lòng thử lại.', 'error');
                }
            };
        });

        // 🔹 Wire nút "Xác nhận đã thanh toán"
        if (btnConfirmPaid) {
            const canConfirm = dto.status === 'ready_to_pay';
            btnConfirmPaid.disabled = !canConfirm;
            btnConfirmPaid.title = canConfirm
                ? ''
                : 'Chỉ xác nhận khi đơn đang ở trạng thái "Chờ khách thanh toán".';

            btnConfirmPaid.onclick = async () => {
                if (!currentOrderId) return;
                const ok = confirm(`Bạn đã kiểm tra sao kê và xác nhận đơn #${currentOrderId} đã được thanh toán?`);
                if (!ok) return;

                try {
                    const res = await fetch(
                        `/api/providers/${providerId}/orders/${currentOrderId}/confirm-payment`,
                        {
                            method: 'POST',
                            headers: withCsrf({'Accept': 'application/json'})
                        }
                    );
                    if (!res.ok) {
                        const msg = await readErrorMessage(
                            res,
                            'Không thể xác nhận thanh toán. Vui lòng thử lại.'
                        );
                        showMessage(msg, res.status === 409 ? 'warn' : 'error');
                        return;
                    }
                    showMessage(`Đã đánh dấu đơn #${currentOrderId} là ĐÃ THANH TOÁN.`, 'success');
                    await loadDetail(currentOrderId);
                    await loadList();
                } catch (e) {
                    console.error(e);
                    showMessage('Có lỗi xảy ra khi gọi API xác nhận thanh toán.', 'error');
                }
            };
        }
    }

    // Events
    btnRefresh && btnRefresh.addEventListener('click', loadList);
    searchInput && searchInput.addEventListener('keydown', e => {
        if (e.key === 'Enter') loadList();
    });

    // Init
    loadList();
})();
