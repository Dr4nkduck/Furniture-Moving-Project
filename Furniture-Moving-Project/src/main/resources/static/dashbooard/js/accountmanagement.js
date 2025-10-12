(() => {
    const $ = (id) => document.getElementById(id);
    const tbody = $('am-tbody');
    const qInput = $('am-search');
    const qBtn = $('am-search-btn');
    const pageLbl = $('am-page');
    const prevBtn = $('am-prev');
    const nextBtn = $('am-next');
    const sizeSel = $('am-size');

    let page = 0, size = parseInt(sizeSel.value, 10), totalPages = 1, q = '';

    // --- If you keep CSRF enabled (NOT ignoring /api/admin/**), uncomment:
    /*
    function csrf() {
      const tokenEl = document.querySelector('meta[name="_csrf"]');
      const headerEl = document.querySelector('meta[name="_csrf_header"]');
      return tokenEl && headerEl ? { header: headerEl.content, token: tokenEl.content } : null;
    }
    */

    function badge(status) {
        const cls = status === 'ACTIVE' ? 'bg-success'
            : status === 'SUSPENDED' ? 'bg-warning text-dark'
                : status === 'DELETED' ? 'bg-danger'
                    : 'bg-secondary';
        return `<span class="badge ${cls}">${status}</span>`;
    }

    function rolePill(role) {
        return role ? `<span class="badge bg-dark">${role}</span>`
            : `<span class="badge bg-secondary">N/A</span>`;
    }

    function actionBtns(u) {
        const dis = (s) => u.status === s ? 'disabled' : '';
        // PENDING nút không hiển thị vì yêu cầu hiện tại chỉ ACTIVE/SUSPENDED/DELETED
        return `
      <div class="d-flex flex-wrap gap-1">
        <button class="btn btn-sm btn-success" ${dis('ACTIVE')} data-act="ACTIVE" data-id="${u.id}">Kích hoạt</button>
        <button class="btn btn-sm btn-warning" ${dis('SUSPENDED')} data-act="SUSPENDED" data-id="${u.id}">Tạm khóa</button>
        <button class="btn btn-sm btn-outline-danger" ${dis('DELETED')} data-del data-id="${u.id}">Xóa</button>
      </div>`;
    }

    function row(u) {
        return `
      <tr>
        <td>${u.id}</td>
        <td>${u.username ?? ''}</td>
        <td>${u.email ?? ''}</td>
        <td>${u.fullName ?? ''}</td>
        <td>${u.phone ?? ''}</td>
        <td>${rolePill(u.primaryRole)}</td>
        <td>${badge(u.status)}</td>
        <td>${actionBtns(u)}</td>
      </tr>`;
    }

    async function fetchPage() {
        const params = new URLSearchParams({ page, size });
        if (q) params.set('q', q);

        const res = await fetch(`/api/admin/users?${params.toString()}`, {
            headers: { 'Accept': 'application/json' }
        });
        if (!res.ok) throw new Error(`Fetch failed: ${res.status}`);
        return res.json(); // Spring Page
    }

    async function load() {
        tbody.innerHTML = `<tr><td colspan="8">Đang tải...</td></tr>`;
        try {
            const pg = await fetchPage();
            const rows = pg.content || [];
            tbody.innerHTML = rows.length ? rows.map(row).join('') :
                `<tr><td colspan="8" class="text-center text-muted">Không có dữ liệu</td></tr>`;

            // paging
            totalPages = pg.totalPages || 1;
            pageLbl.textContent = `${(pg.number ?? 0) + 1}/${totalPages}`;
            prevBtn.disabled = !!pg.first;
            nextBtn.disabled = !!pg.last;

            // bind row buttons
            bindRowActions();
        } catch (e) {
            console.error(e);
            tbody.innerHTML = `<tr><td colspan="8" class="text-danger">Lỗi tải dữ liệu</td></tr>`;
        }
    }

    function bindRowActions() {
        tbody.querySelectorAll('button[data-act]').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                const status = e.currentTarget.getAttribute('data-act');
                if (!confirm(`Đổi trạng thái #${id} -> ${status}?`)) return;

                const options = {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ status })
                };
                // If CSRF enabled:
                // const c = csrf(); if (c) options.headers[c.header] = c.token;

                const res = await fetch(`/api/admin/users/${id}/status`, options);
                if (!res.ok) { alert('Không thể đổi trạng thái'); return; }
                load();
            });
        });

        tbody.querySelectorAll('button[data-del]').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                if (!confirm(`Xóa (soft delete) tài khoản #${id}?`)) return;

                const options = { method: 'DELETE' };
                // If CSRF enabled:
                // const c = csrf(); if (c) options.headers = { [c.header]: c.token };

                const res = await fetch(`/api/admin/users/${id}`, options);
                if (!res.ok) { alert('Không thể xóa'); return; }
                load();
            });
        });
    }

    // events
    qBtn.addEventListener('click', () => { q = qInput.value.trim(); page = 0; load(); });
    qInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') { q = qInput.value.trim(); page = 0; load(); }});
    prevBtn.addEventListener('click', () => { if (page > 0) { page--; load(); }});
    nextBtn.addEventListener('click', () => { if (page < totalPages - 1) { page++; load(); }});
    sizeSel.addEventListener('change', () => { size = parseInt(sizeSel.value, 10); page = 0; load(); });

    // init
    document.addEventListener('DOMContentLoaded', load);
})();
