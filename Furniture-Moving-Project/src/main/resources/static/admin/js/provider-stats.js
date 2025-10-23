(() => {
    const $ = (id) => document.getElementById(id);
    const tbody = $('ps-tbody');
    const fromI = $('from'), toI = $('to'), filterBtn = $('filter'), exportBtn = $('exportCsv'), themeBtn = $('theme-toggle');

    function setTheme(next){ const r=document.documentElement; if(next==='dark') r.classList.add('dark'); else r.classList.remove('dark'); localStorage.setItem('theme',next); themeBtn.textContent = next==='dark'?'☀️ Light':'🌙 Dark'; }
    function initTheme(){ const s=localStorage.getItem('theme'); const d = s ? (s==='dark') : document.documentElement.classList.contains('dark'); setTheme(d?'dark':'light'); }

    function fmt(n){ return n==null ? '' : Number(n).toLocaleString(); }
    function pct(n){ return `${(n??0).toFixed(1)}%`; }

    function row(r){
        return `<tr>
      <td>${r.providerName ?? ('#'+r.providerId)}</td>
      <td>${fmt(r.jobsTotal)}</td>
      <td>${fmt(r.jobsAccepted)}</td>
      <td>${fmt(r.jobsCanceled)}</td>
      <td>${pct(r.acceptanceRate)}</td>
      <td>${pct(r.cancelRate)}</td>
      <td>${r.avgRating==null?'':Number(r.avgRating).toFixed(2)}</td>
      <td>${r.revenueEstimate==null?'':fmt(r.revenueEstimate)}</td>
    </tr>`;
    }

    function iso(dt){ return dt ? new Date(dt).toISOString() : null; }
    function params(){
        const p = new URLSearchParams();
        if (fromI.value) p.set('from', new Date(fromI.value).toISOString());
        if (toI.value)   p.set('to',   new Date(toI.value).toISOString());
        return p;
    }

    async function load(){
        tbody.innerHTML = `<tr><td colspan="8">Đang tải...</td></tr>`;
        const res = await fetch(`/api/admin/analytics/providers?${params().toString()}`);
        if (!res.ok) { tbody.innerHTML = `<tr><td colspan="8" class="text-danger">Lỗi tải dữ liệu</td></tr>`; return; }
        const data = await res.json();
        tbody.innerHTML = data.length ? data.map(row).join('') : `<tr><td colspan="8" class="text-center text-muted">Không có dữ liệu</td></tr>`;
    }

    filterBtn.addEventListener('click', load);
    exportBtn.addEventListener('click', () => { window.location = `/api/admin/analytics/providers.csv?${params().toString()}`; });
    themeBtn.addEventListener('click', () => { const isDark = document.documentElement.classList.contains('dark'); setTheme(isDark?'light':'dark'); });

    document.addEventListener('DOMContentLoaded', () => { initTheme(); load(); });
})();
