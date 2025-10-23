(() => {
    const $ = (id) => document.getElementById(id);
    const fromI = $('from'), toI = $('to'), filterBtn = $('filter'), exportBtn = $('exportCsv'), themeBtn = $('theme-toggle');
    const sSearch = $('st-search'), sQuote = $('st-quote'), sBook = $('st-book'), sQR = $('st-qr'), sBR = $('st-br'), sAOV = $('st-aov');
    const ulCorr = $('ct-corridors'), ulReasons = $('ct-reasons');

    function setTheme(next){ const r=document.documentElement; if(next==='dark') r.classList.add('dark'); else r.classList.remove('dark'); localStorage.setItem('theme',next); themeBtn.textContent = next==='dark'?'☀️ Light':'🌙 Dark'; }
    function initTheme(){ const s=localStorage.getItem('theme'); const d = s ? (s==='dark') : document.documentElement.classList.contains('dark'); setTheme(d?'dark':'light'); }

    function params(){
        const p = new URLSearchParams();
        if (fromI.value) p.set('from', new Date(fromI.value).toISOString());
        if (toI.value)   p.set('to',   new Date(toI.value).toISOString());
        return p;
    }

    function li(text, right){ return `<li class="list-group-item d-flex justify-content-between"><span>${text}</span><strong>${right}</strong></li>`; }

    async function load(){
        sSearch.textContent = sQuote.textContent = sBook.textContent = '...';
        const res = await fetch(`/api/admin/analytics/customers?${params().toString()}`);
        if (!res.ok) { sSearch.textContent='!'; return; }
        const d = await res.json();

        sSearch.textContent = d.stageSearch ?? 0;
        sQuote.textContent  = d.stageQuote ?? 0;
        sBook .textContent  = d.stageBooking ?? 0;
        sQR.textContent = `${(d.funnelQuoteRate ?? 0).toFixed(1)}%`;
        sBR.textContent = `${(d.funnelBookRate ?? 0).toFixed(1)}%`;
        sAOV.textContent = d.averageOrderValue == null ? '' : Number(d.averageOrderValue).toLocaleString();

        ulCorr.innerHTML = (d.topCorridors && d.topCorridors.length)
            ? d.topCorridors.map(c => li(c.corridor, c.count)).join('')
            : '<li class="list-group-item text-muted">Không có dữ liệu</li>';

        ulReasons.innerHTML = (d.cancelReasons && d.cancelReasons.length)
            ? d.cancelReasons.map(c => li(c.reason, c.count)).join('')
            : '<li class="list-group-item text-muted">Không có dữ liệu</li>';
    }

    filterBtn.addEventListener('click', load);
    exportBtn.addEventListener('click', () => { window.location = `/api/admin/analytics/customers.csv?${params().toString()}`; });
    themeBtn.addEventListener('click', () => { const isDark = document.documentElement.classList.contains('dark'); setTheme(isDark?'light':'dark'); });

    document.addEventListener('DOMContentLoaded', () => { initTheme(); load(); });
})();
