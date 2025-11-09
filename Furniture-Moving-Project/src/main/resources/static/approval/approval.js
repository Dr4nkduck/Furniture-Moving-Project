document.addEventListener('DOMContentLoaded', () => {
    const root = document.getElementById('invoiceRoot');
    if (!root) return;

    const statusBadge = document.getElementById('statusBadge');
    const approveBtn  = document.getElementById('approveBtn');

    const requestId = root.dataset.requestId;   // yêu cầu controller set requestId
    let status      = root.dataset.status;      // 'pending' | 'completed' | ...

    function setStatusUI(newStatus) {
        status = newStatus;
        root.dataset.status = newStatus;

        statusBadge.classList.remove('status-pending', 'status-completed', 'status-other');

        if (newStatus === 'pending') {
            statusBadge.classList.add('status-pending');
            statusBadge.textContent = 'Chờ duyệt';
        } else if (newStatus === 'completed') {
            statusBadge.classList.add('status-completed');
            statusBadge.textContent = 'Đã duyệt';
        } else {
            statusBadge.classList.add('status-other');
            statusBadge.textContent = newStatus;
        }
    }

    function toast(msg, isError = false) {
        const t = document.createElement('div');
        t.className = 'toast ' + (isError ? 'toast-error' : 'toast-ok');
        t.textContent = msg;
        document.body.appendChild(t);
        setTimeout(() => t.remove(), 3000);
    }

    if (approveBtn) {
        approveBtn.addEventListener('click', async () => {
            if (!requestId) {
                toast('Thiếu requestId để cập nhật.', true);
                return;
            }
            approveBtn.disabled = true;
            approveBtn.classList.add('is-loading');

            try {
                const res = await fetch(`/api/service-requests/${requestId}/approve`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' }
                });

                if (!res.ok) {
                    const text = await res.text();
                    throw new Error(text || 'Cập nhật thất bại');
                }

                // OK -> đổi UI
                setStatusUI('completed');
                toast('Đã cập nhật trạng thái: hoàn thành');
                approveBtn.remove(); // ẩn nút vì đã duyệt xong
            } catch (e) {
                toast('Không thể duyệt: ' + e.message, true);
            } finally {
                approveBtn.disabled = false;
                approveBtn.classList.remove('is-loading');
            }
        });
    }

    // đảm bảo UI đúng với trạng thái ban đầu server gửi về
    if (status) setStatusUI(status);
});
