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
        // Mã thanh toán REQ(id)
        paymentRef: document.getElementById('d-paymentRef')
    };

    // ===== KHU VỰC XỬ LÝ YÊU CẦU HỦY (GIAI ĐOẠN 3) =====
    const cancelSec = {
        wrapper: document.getElementById('cancelSection'),
        statusText: document.getElementById('cancelStatusText'),
        reasonText: document.getElementById('cancelReasonText'),
        decisionNoteText: document.getElementById('cancelDecisionNoteText'),
        btnApprove: document.getElementById('btnApproveCancel'),
        btnReject: document.getElementById('btnRejectCancel')
    };

    const btnConfirmPaid = document.getElementById('btnConfirmPaid');

    let currentOrderId = null;
    let currentCancellationId = null;

    // ===== DIALOG OVERLAY (đẹp, theo theme) =====
    const dlg = {
        backdrop: document.getElementById('dialog-backdrop'),
        title: document.getElementById('dialog-title'),
        message: document.getElementById('dialog-message'),
        input: document.getElementById('dialog-input'),
        error: document.getElementById('dialog-error'),
        btnOk: document.getElementById('dialog-btn-ok'),
        btnCancel: document.getElementById('dialog-btn-cancel')
    };

    function showDialog(options) {
        const {
            title = 'Xác nhận',
            message = '',
            showInput = false,
            placeholder = '',
            required = false,
            okText = 'Xác nhận',
            cancelText = 'Hủy'
        } = options || {};

        return new Promise(resolve => {
            // Nếu chưa có HTML dialog thì fallback về confirm gốc
            if (!dlg.backdrop) {
                const ok = window.confirm(message || title);
                return resolve({ confirmed: ok, value: null });
            }

            dlg.title.textContent = title;
            dlg.message.textContent = message;
            dlg.error.textContent = '';
            dlg.error.classList.add('d-none');

            dlg.input.value = '';
            dlg.input.placeholder = placeholder || '';
            dlg.input.classList.toggle('d-none', !showInput);

            dlg.btnOk.textContent = okText;
            dlg.btnCancel.textContent = cancelText;

            dlg.backdrop.classList.remove('d-none');

            const onCancel = () => {
                cleanup();
                resolve({ confirmed: false, value: null });
            };

            const onOk = () => {
                let val = dlg.input.value.trim();
                if (showInput && required && !val) {
                    dlg.error.textContent = 'Vui lòng nhập nội dung.';
                    dlg.error.classList.remove('d-none');
                    dlg.input.focus();
                    return;
                }
                cleanup();
                resolve({ confirmed: true, value: showInput ? val : null });
            };

            function onKeyDown(e) {
                if (e.key === 'Escape') {
                    e.preventDefault();
                    onCancel();
                } else if (e.key === 'Enter' && !showInput) {
                    e.preventDefault();
                    onOk();
                }
            }

            function cleanup() {
                dlg.backdrop.classList.add('d-none');
                dlg.btnOk.onclick = null;
                dlg.btnCancel.onclick = null;
                document.removeEventListener('keydown', onKeyDown);
            }

            dlg.btnOk.onclick = onOk;
            dlg.btnCancel.onclick = onCancel;
            document.addEventListener('keydown', onKeyDown);

            if (showInput) {
                dlg.input.focus();
            }
        });
    }

    function showConfirm(message, title) {
        return showDialog({
            title: title || 'Xác nhận',
            message,
            showInput: false
        }).then(r => r.confirmed);
    }

    function showPrompt(message, opts) {
        const {
            title = 'Nhập nội dung',
            placeholder = '',
            required = true
        } = opts || {};
        return showDialog({
            title,
            message,
            showInput: true,
            placeholder,
            required
        }).then(r => (r.confirmed ? r.value : null));
    }

    // ===== COMMON HELPERS =====
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
        switch ((s || '').toLowerCase()) {
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

    function humanCancelStatus(s) {
        s = (s || '').toLowerCase();
        switch (s) {
            case 'requested':
                return 'Đang chờ bạn xử lý';
            case 'approved':
                return 'Bạn đã chấp nhận hủy đơn';
            case 'rejected':
                return 'Bạn đã từ chối yêu cầu hủy';
            default:
                return 'Không có yêu cầu hủy';
        }
    }

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

    function showMessage(msg, type) {
        if (typeof notify === 'function') {
            notify(msg, type || 'info');
        } else {
            alert(msg);
        }
    }

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
            default:
                return [];
        }
    }

    // ===== LOAD LIST =====
    async function loadList() {
        const q = searchInput ? searchInput.value.trim() : '';
        const status = statusFilter ? statusFilter.value : '';
        const url = `/api/providers/${providerId}/orders?` + new URLSearchParams({q, status}).toString();
        const rows = await fetchJSON(url);
        if (!tbody) return;
        tbody.innerHTML = '';

        (rows || []).forEach(r => {
            const tr = document.createElement('tr');
            const paymentRef = `REQ${r.requestId}`;
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

    // ===== LOAD DETAIL =====
    async function loadDetail(orderId) {
        const dto = await fetchJSON(`/api/providers/${providerId}/orders/${orderId}`);
        currentOrderId = orderId;

        if (!dWrap || !dEmpty) return;
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

        // Timeline
        d.timeline.innerHTML = '';
        const steps = ['pending', 'accepted', 'ready_to_pay', 'paid', 'in_progress', 'completed'];
        const idx = steps.indexOf(dto.status);
        steps.forEach((s, i) => {
            const div = document.createElement('div');
            div.className = 't' + (idx >= i ? ' active' : '');
            div.textContent = humanStatus(s);
            d.timeline.appendChild(div);
        });

        // ===== KHU YÊU CẦU HỦY TRONG DETAIL =====
        currentCancellationId = dto.cancellationId || null;
        let hasPendingCancellation = false;

        if (cancelSec.wrapper) {
            if (dto.cancellationStatus) {
                cancelSec.wrapper.classList.remove('d-none');

                if (cancelSec.statusText) {
                    cancelSec.statusText.textContent = humanCancelStatus(dto.cancellationStatus);
                }
                if (cancelSec.reasonText) {
                    cancelSec.reasonText.textContent = dto.cancellationReason || '(không có lý do)';
                }
                if (cancelSec.decisionNoteText) {
                    cancelSec.decisionNoteText.textContent = dto.cancellationDecisionNote
                        ? ('Ghi chú xử lý: ' + dto.cancellationDecisionNote)
                        : '';
                }

                hasPendingCancellation = dto.cancellationStatus === 'requested';

                if (cancelSec.btnApprove) cancelSec.btnApprove.disabled = !hasPendingCancellation;
                if (cancelSec.btnReject)  cancelSec.btnReject.disabled  = !hasPendingCancellation;

            } else if (dto.status === 'cancelled' && dto.cancelReason) {
                cancelSec.wrapper.classList.remove('d-none');

                if (cancelSec.statusText) {
                    cancelSec.statusText.textContent = 'Đơn đã bị khách hủy trước khi thanh toán.';
                }
                if (cancelSec.reasonText) {
                    cancelSec.reasonText.textContent = dto.cancelReason;
                }
                if (cancelSec.decisionNoteText) {
                    cancelSec.decisionNoteText.textContent = '';
                }

                if (cancelSec.btnApprove) cancelSec.btnApprove.disabled = true;
                if (cancelSec.btnReject)  cancelSec.btnReject.disabled  = true;
            } else {
                cancelSec.wrapper.classList.add('d-none');

                if (cancelSec.statusText) cancelSec.statusText.textContent = '';
                if (cancelSec.reasonText) cancelSec.reasonText.textContent = '';
                if (cancelSec.decisionNoteText) cancelSec.decisionNoteText.textContent = '';

                currentCancellationId = null;
            }
        }

        // Khi có pending cancellation -> khóa các nút status khác
        let allowed = new Set(allowedActionsForStatus(dto.status));
        if (hasPendingCancellation) {
            allowed = new Set(); // disable hết
        }

        // ===== Wire các action trạng thái =====
        document.querySelectorAll('.actions [data-act]').forEach(btn => {
            const act = btn.getAttribute('data-act');
            const isAllowed = allowed.has(act);

            btn.disabled = !isAllowed;
            btn.classList.toggle('disabled', !isAllowed);

            if (hasPendingCancellation) {
                btn.title = 'Đã có yêu cầu hủy đơn. Vui lòng xử lý yêu cầu hủy trước.';
            } else {
                btn.title = !isAllowed
                    ? 'Trạng thái hiện tại không cho phép thao tác này.'
                    : '';
            }

            btn.onclick = !isAllowed ? null : async () => {
                let body = {status: act};
                if (act === 'cancelled') {
                    const reason = await showPrompt(
                        'Nhập lý do hủy (không bắt buộc, sẽ hiển thị cho khách).',
                        {
                            title: 'Lý do hủy đơn',
                            placeholder: 'Ví dụ: Khách thay đổi kế hoạch',
                            required: false
                        }
                    );
                    if (reason) body.cancelReason = reason;
                }
                try {
                    const res = await fetch(
                        `/api/providers/${providerId}/orders/${orderId}/status`,
                        {
                            method: 'PUT',
                            headers: withCsrf({
                                'Content-Type': 'application/json',
                                'Accept': 'application/json'
                            }),
                            body: JSON.stringify(body)
                        }
                    );
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

        // ===== NÚT "XÁC NHẬN ĐÃ THANH TOÁN" =====
        if (btnConfirmPaid) {
            console.log('[ConfirmPaid] dto.status =', dto.status);

            const st = (dto.status || '').toLowerCase();
            const canConfirm = st === 'ready_to_pay';

            btnConfirmPaid.disabled = !canConfirm;
            btnConfirmPaid.title = canConfirm
                ? ''
                : 'Chỉ xác nhận khi đơn đang ở trạng thái "Chờ khách thanh toán".';

            btnConfirmPaid.onclick = async () => {
                if (!currentOrderId) return;

                let ok = false;
                try {
                    ok = await showConfirm(
                        `Bạn đã kiểm tra sao kê và xác nhận đơn #${currentOrderId} đã được thanh toán?`,
                        'Xác nhận đã thanh toán'
                    );
                } catch (e) {
                    console.error('[ConfirmPaid] showConfirm error, fallback window.confirm', e);
                    ok = window.confirm(
                        `Bạn đã kiểm tra sao kê và xác nhận đơn #${currentOrderId} đã được thanh toán?`
                    );
                }
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

    // ===== GỌI API APPROVE / REJECT YÊU CẦU HỦY =====
    async function callApproveCancellation(cancellationId) {
        const note = await showPrompt(
            'Nếu cần, hãy nhập ghi chú khi chấp nhận hủy (không bắt buộc).',
            {
                title: 'Ghi chú khi chấp nhận hủy',
                placeholder: 'Ví dụ: Sẽ hoàn tiền 90% cho khách',
                required: false
            }
        ) || '';

        try {
            const res = await fetch(`/api/provider/cancellations/${cancellationId}/approve`, {
                method: 'POST',
                headers: withCsrf({'Content-Type': 'application/json', 'Accept': 'application/json'}),
                body: JSON.stringify({ note })
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok || !data.success) {
                const msg = data.message || 'Không thể chấp nhận yêu cầu hủy.';
                showMessage(msg, res.status === 409 ? 'warn' : 'error');
                return;
            }
            showMessage(data.message || 'Đã chấp nhận yêu cầu hủy.', 'success');
            if (currentOrderId) {
                await loadDetail(currentOrderId);
                await loadList();
            }
        } catch (e) {
            console.error(e);
            showMessage('Có lỗi khi gọi API chấp nhận hủy.', 'error');
        }
    }

    async function callRejectCancellation(cancellationId, note) {
        try {
            const res = await fetch(`/api/provider/cancellations/${cancellationId}/reject`, {
                method: 'POST',
                headers: withCsrf({'Content-Type': 'application/json', 'Accept': 'application/json'}),
                body: JSON.stringify({ note })
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok || !data.success) {
                const msg = data.message || 'Không thể từ chối yêu cầu hủy.';
                showMessage(msg, res.status === 409 ? 'warn' : 'error');
                return;
            }
            showMessage(data.message || 'Đã từ chối yêu cầu hủy.', 'success');
            if (currentOrderId) {
                await loadDetail(currentOrderId);
                await loadList();
            }
        } catch (e) {
            console.error(e);
            showMessage('Có lỗi khi gọi API từ chối hủy.', 'error');
        }
    }

    // ===== EVENT cho 2 nút xử lý yêu cầu hủy =====
    if (cancelSec.btnApprove) {
        cancelSec.btnApprove.addEventListener('click', async () => {
            if (!currentCancellationId) {
                showMessage('Không tìm thấy yêu cầu hủy cho đơn này.', 'warn');
                return;
            }
            const ok = await showConfirm(
                'Chấp nhận yêu cầu hủy đơn này? Đơn sẽ chuyển sang trạng thái ĐÃ HỦY.',
                'Chấp nhận yêu cầu hủy'
            );
            if (!ok) return;

            await callApproveCancellation(currentCancellationId);
        });
    }

    if (cancelSec.btnReject) {
        cancelSec.btnReject.addEventListener('click', async () => {
            if (!currentCancellationId) {
                showMessage('Không tìm thấy yêu cầu hủy cho đơn này.', 'warn');
                return;
            }

            const note = await showPrompt(
                'Nhập lý do từ chối yêu cầu hủy (bắt buộc, sẽ hiển thị cho khách).',
                {
                    title: 'Từ chối yêu cầu hủy',
                    placeholder: 'Ví dụ: Đơn đã chuẩn bị xong, không thể hủy',
                    required: true
                }
            );
            if (note == null) return;

            await callRejectCancellation(currentCancellationId, note);
        });
    }

    // ===== EVENTS lọc / tìm =====
    btnRefresh && btnRefresh.addEventListener('click', loadList);
    searchInput && searchInput.addEventListener('keydown', e => {
        if (e.key === 'Enter') loadList();
    });

    // ===== INIT =====
    loadList();
})();
