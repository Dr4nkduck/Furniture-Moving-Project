(() => {
    const qs = (id) => document.getElementById(id);
    const tbody = qs('am-tbody');
    const searchInput = qs('am-search');
    const searchBtn = qs('am-search-btn');
    const pageSpan = qs('am-page');
    const prevBtn = qs('am-prev');
    const nextBtn = qs('am-next');
    const sizeSel = qs('am-size');

    let page = 0;
    let size = parseInt(sizeSel.value, 10);
    let totalPages = 1;
    let q = '';

    function badge(status) {
        const cls =
            status === 'ACTIVE' ? 'bg-success' :
                status === 'SUSPENDED' ? 'bg-warning text-dark' :
                    status === 'DELETED' ? 'bg-danger' : 'bg-secondary';
        return `<span class="badge ${cls}">${status}</span>`;
    }

    function rolePill(role) {
        if (!role) return '<span class="badge bg-secondary">N/A</span>';
        return `<span class="badge bg-dark">${role}</span>`;
    }

    function actionBtns(u) {
        const dis = (s) => u.status === s ? 'disabled' : '';
        return `
      <div class="d-flex flex-wrap gap-1">
        <button class="btn btn-sm btn-success" ${dis('ACTIVE')} data-act="ACTIVE" data-id="${u.id}">Kích hoạt</button>
        <button class="btn btn-sm btn-warning" ${dis('SUSPENDED')} data-act="SUSPENDED" data-id="${u.id}">Tạm khóa</button>
        <button class="btn btn-sm btn-info" ${dis('PENDING')} data-act="PENDING" data-id="${u.id}">Chờ duyệt</button>
        <button class="btn btn-sm btn-outline-danger" ${dis('DELETED')} data-del data-id="${u.id}">Xóa</button>
      </div>`;
    }

    function render(rows) {
        if (!rows || rows.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">Không có dữ liệu</td></tr>`;
            return;
        }
        tbody.innerHTML = rows.map(u => `
      <tr>
        <td>${u.id}</td>
        <td>${u.username ?? ''}</td>
        <td>${u.email ?? ''}</td>
        <td>${u.fullName ?? ''}</td>
        <td>${u.phone ?? ''}</td>
        <td>${rolePill(u.primaryRole)}</td>
        <td>${badge(u.status)}</td>
        <td>${actionBtns(u)}</td>
      </tr>
    `).join('');
    }

    function bindActions() {
        tbody.querySelectorAll('button[data-act]').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                const status = e.currentTarget.getAttribute('data-act');
                if (!confirm(`Đổi trạng thái #${id} -> ${status}?`)) return;
                const res = await fetch(`/api/admin/users/${id}/status`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ status })
                });
                if (!res.ok) return alert('Không thể đổi trạng thái');
                load();
            });
        });
        tbody.querySelectorAll('button[data-del]').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                if (!confirm(`Xóa (soft delete) tài khoản #${id}?`)) return;
                const res = await fetch(`/api/admin/users/${id}`, { method: 'DELETE' });
                if (!res.ok) return alert('Không thể xóa');
                load();
            });
        });
    }

    async function fetchPage() {
        const params = new URLSearchParams();
        if (q) params.set('q', q);
        params.set('page', page);
        params.set('size', size);
        const res = await fetch(`/api/admin/users?${params.toString()}`, { headers: { 'Accept':'application/json' }});
        if (!res.ok) throw new Error('Fetch failed');
        return res.json(); // Spring Page
    }

    async function load() {
        tbody.innerHTML = `<tr><td colspan="8">Đang tải...</td></tr>`;
        try {
            const pg = await fetchPage();
            render(pg.content);
            pageSpan.textContent = `${pg.number + 1}/${pg.totalPages || 1}`;
            totalPages = pg.totalPages || 1;
            prevBtn.disabled = pg.first;
            nextBtn.disabled = pg.last;
            bindActions();
        } catch (e) {
            console.error(e);
            tbody.innerHTML = `<tr><td colspan="8" class="text-danger">Lỗi tải dữ liệu</td></tr>`;
        }
    }

    // events
    searchBtn.addEventListener('click', () => { q = searchInput.value.trim(); page = 0; load(); });
    searchInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') { q = searchInput.value.trim(); page = 0; load(); }});
    prevBtn.addEventListener('click', () => { if (page > 0) { page--; load(); }});
    nextBtn.addEventListener('click', () => { if (page < totalPages - 1) { page++; load(); }});
    sizeSel.addEventListener('change', () => { size = parseInt(sizeSel.value, 10); page = 0; load(); });

    // init
    document.addEventListener('DOMContentLoaded', load);
})();
