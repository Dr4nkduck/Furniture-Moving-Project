(function () {
    const providerId = new URLSearchParams(location.search).get('providerId') || '';

    function toast(msg, ok = true) {
        Toastify({
            text: msg, duration: 2200, gravity: "bottom", position: "right",
            backgroundColor: ok ? "#22c55e" : "#ef4444"
        }).showToast();
    }

    async function api(path, opts) {
        const sep = path.includes('?') ? '&' : '?';
        const url = providerId ? (path + sep + 'providerId=' + providerId) : path;
        const res = await fetch(url, opts);
        if (!res.ok) throw new Error('HTTP ' + res.status);
        return res.status === 204 ? null : res.json();
    }

    const pkgSelect = document.getElementById('packageSelect');

    async function loadPackages() {
        const list = await api('/api/providers/packages');
        pkgSelect.innerHTML = '';
        list.forEach(p => {
            const opt = document.createElement('option');
            opt.value = p.packageId;
            opt.textContent = p.name;
            pkgSelect.appendChild(opt);
        });
        if (window.jQuery && jQuery.fn && jQuery.fn.select2) jQuery(pkgSelect).select2({placeholder: 'Chọn gói dịch vụ'});
        if (list.length) {
            await loadPerKm();
        }
    }

    async function loadPerKm() {
        const d = await api('/api/providers/packages/pricing?packageId=' + pkgSelect.value);
        document.getElementById('perKm').value = (d && d.perKm) ? d.perKm : 0;
    }

    async function savePerKm() {
        const dto = {packageId: +pkgSelect.value, perKm: +document.getElementById('perKm').value};
        await api('/api/providers/packages/pricing', {
            method: 'PUT', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(dto)
        });
        toast('Đã lưu giá theo km.');
    }

    async function loadFurniture() {
        const list = await api('/api/providers/furniture-prices');
        const body = document.getElementById('furnitureBody');
        body.innerHTML = '';
        list.forEach(it => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
        <td>${it.furnitureName}</td>
        <td class="text-muted">${it.unit || ''}</td>
        <td>
          <div class="input-group">
            <span class="input-group-text">₫</span>
            <input type="number" class="form-control" min="0" step="1000" value="${it.price || 0}" data-id="${it.furnitureTypeId}">
          </div>
        </td>`;
            body.appendChild(tr);
        });
    }

    async function saveFurniture() {
        const rows = [...document.querySelectorAll('#furnitureBody input')];
        const items = rows.map(i => ({furnitureTypeId: +i.dataset.id, price: +i.value}));
        await api('/api/providers/furniture-prices', {
            method: 'PUT', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(items)
        });
        toast('Đã lưu bảng giá đồ.');
    }

    pkgSelect.addEventListener('change', () => loadPerKm().catch(() => toast('Lỗi tải giá/km', false)));
    document.getElementById('btnSavePerKm').addEventListener('click', e => {
        e.preventDefault();
        savePerKm().catch(() => toast('Lưu giá/km thất bại', false));
    });
    document.getElementById('btnSaveFurniture').addEventListener('click', e => {
        e.preventDefault();
        saveFurniture().catch(() => toast('Lưu bảng giá đồ thất bại', false));
    });

    (async function init() {
        try {
            await loadPackages();
            await loadPerKm();
            await loadFurniture();
        } catch (err) {
            toast('Không tải được dữ liệu. Thử thêm ?providerId=... để test.', false);
        }
    })();
})();
