(function () {
    const providerId = new URLSearchParams(location.search).get('providerId') || '';

    async function api(path) {
        const sep = path.includes('?') ? '&' : '?';
        const url = providerId ? (path + sep + 'providerId=' + providerId) : path;
        const res = await fetch(url);
        if (!res.ok) throw new Error('HTTP ' + res.status);
        return res.json();
    }

    function setKpi(id, v) {
        document.getElementById('kpi-' + id).textContent = v;
    }

    async function loadSummary() {
        try {
            const d = await api('/api/provider/dashboard/summary');
            setKpi('total', d.total || 0);
            setKpi('assigned', d.assigned || 0);
            setKpi('progress', d.inProgress || 0);
            setKpi('completed', d.completed || 0);
        } catch (_) {
            try {
                const list = await api('/api/providers/orders');
                const by = s => list.filter(o => (o.status || '').toLowerCase() === s).length;
                setKpi('total', list.length);
                setKpi('assigned', by('assigned'));
                setKpi('progress', by('in_progress'));
                setKpi('completed', by('completed'));
            } catch (__) {
            }
        }
    }

    async function loadRecent() {
        try {
            const list = await api('/api/providers/orders');
            const rec = document.getElementById('recent');
            rec.innerHTML = '';
            (list || []).slice(0, 5).forEach(o => {
                const div = document.createElement('div');
                div.className = 'row-item';
                div.innerHTML = `
          <div>
            <div><b>#${o.requestId ?? o.id ?? '-'}</b> · ${o.routeSummary || 'Pickup → Delivery'}</div>
            <div class="sub">${(o.status || '').toUpperCase()} · ${o.scheduledAt ?? ''} · ${o.customerShort ?? ''}</div>
          </div>
          <a class="btn btn-sm btn-outline-secondary" href="/provider/orders/${o.requestId ?? o.id}">Chi tiết</a>
        `;
                rec.appendChild(div);
            });
        } catch (_) {
        }
    }

    function drawMiniChart() {
        if (typeof Chart === 'undefined') return; // guard
        const ctx = document.getElementById('miniChart');
        const vals = [3, 4, 2, 5, 6, 4, 7];
        new Chart(ctx, {
            type: 'line',
            data: {labels: ['', '', '', '', '', '', ''], datasets: [{data: vals, tension: .35, fill: false}]},
            options: {
                plugins: {legend: {display: false}},
                scales: {x: {display: false}, y: {display: false}},
                elements: {point: {radius: 0}}
            }
        });
    }

    (async function init() {
        await loadSummary();
        await loadRecent();
        drawMiniChart();
    })();
})();
