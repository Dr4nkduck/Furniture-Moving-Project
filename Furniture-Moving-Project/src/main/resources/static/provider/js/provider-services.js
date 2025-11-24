/* PV-002 Provider Services JS
 * Backend endpoints (không đổi):
 *  - GET  /api/providers/{pid}/service-packages
 *        -> [ { packageId, packageName, basePackageName?, pricePerKm } ]
 *  - GET  /api/providers/{pid}/service-packages/{packageId}
 *        -> { packageNameSnapshot, pricePerKm, furniturePrices: [{furnitureItemId,furnitureItemName,price}] }
 *  - PUT  /api/providers/{pid}/service-packages/{packageId}
 *        -> lưu snapshot (pricePerKm + furniturePrices). Nếu muốn "xóa", gửi pricePerKm=null và furniturePrices=[]
 *  - (tuỳ) GET /api/providers/me -> lấy providerId; ưu tiên <meta name="provider-id">
 */

(() => {
    // ==================== 1. DOM ====================
    const $configuredList   = document.getElementById('configuredList');
    const $configuredCount  = document.getElementById('configuredCount');
    const $searchConfigured = document.getElementById('searchConfigured');
    const $filterByPackage  = document.getElementById('filterByPackage');

    const $btnRefresh = document.getElementById('btnRefresh');
    const $btnOpenAdd = document.getElementById('btnOpenAdd');

    const $detailForm = document.getElementById('detailForm');
    const $emptyHint  = document.getElementById('emptyHint');
    const $packageNameSnapshot = document.getElementById('packageNameSnapshot');
    const $pricePerKm = document.getElementById('pricePerKm');
    const $btnAddRow  = document.getElementById('btnAddRow');
    const $btnSave    = document.getElementById('btnSave');
    const $btnReset   = document.getElementById('btnReset');
    const $btnDelete  = document.getElementById('btnDelete');
    const $itemsTableBody = document.querySelector('#itemsTable tbody');

    // Modal thêm gói
    const addModalEl = document.getElementById('addPkgModal');
    const addModal = addModalEl ? new bootstrap.Modal(addModalEl) : null;
    const $modalPackageSelect = document.getElementById('modalPackageSelect');
    const $modalPerKm = document.getElementById('modalPerKm');
    const $btnCreatePkg = document.getElementById('btnCreatePkg');

    const $toastBox = document.getElementById('toastBox');

    // ==================== 2. STATE ====================
    const state = {
        providerId: getProviderIdFromMeta(),
        allPackages: [],   // toàn bộ packages/snapshot từ API
        configured: [],    // list hiển thị cột trái
        current: {
            packageId: null,
            basePackageName: null,
            packageName: null,
            perKm: null,
            items: []      // [{furnitureItemId,furnitureItemName,price}]
        }
    };

    // ==================== 3. UTIL ====================
    function getProviderIdFromMeta() {
        const meta = document.querySelector('meta[name="provider-id"]');
        if (meta && meta.content) {
            const v = parseInt(meta.content, 10);
            if (!Number.isNaN(v)) return v;
        }
        return null;
    }

    function toast(msg, type = 'info', timeoutMs = 3000) {
        if (!$toastBox) {
            alert(msg);
            return;
        }
        const div = document.createElement('div');
        div.className = `alert alert-${type === 'error' ? 'danger' : type} border-0 shadow mb-2`;
        div.innerHTML = `
          <div class="d-flex align-items-center">
              <i class="bi ${typeIcon(type)} me-2"></i>
              <div>${msg}</div>
          </div>`;
        $toastBox.appendChild(div);
        setTimeout(() => div.remove(), timeoutMs);
    }

    function typeIcon(type) {
        switch (type) {
            case 'success': return 'bi-check-circle';
            case 'warning': return 'bi-exclamation-triangle';
            case 'error':   return 'bi-x-circle';
            default:        return 'bi-info-circle';
        }
    }

    async function apiGet(url) {
        const r = await fetch(url);
        if (!r.ok) throw new Error(await errorText(r, `GET ${url} -> ${r.status}`));
        return r.json();
    }

    async function apiPut(url, body) {
        const r = await fetch(url, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (!r.ok) throw new Error(await errorText(r, `PUT ${url} -> ${r.status}`));
        const ct = r.headers.get('content-type') || '';
        return ct.includes('application/json') ? r.json() : (await r.text());
    }

    async function errorText(resp, fallback) {
        try {
            const ct = resp.headers.get('content-type') || '';
            if (ct.includes('application/json')) {
                const j = await resp.json();
                return j.message || j.error || fallback;
            }
            return await resp.text() || fallback;
        } catch { return fallback; }
    }

    function escapeHtml(s) {
        return (s || '').replace(/[&<>"']/g, c => ({
            '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
        }[c]));
    }

    function toggleInvalid(input, invalid) {
        input.classList.toggle('is-invalid', invalid);
        input.classList.toggle('is-valid', !invalid);
    }

    // ==================== 4. RENDER LEFT (LIST) ====================
    function renderConfiguredList() {
        const text = ($searchConfigured.value || '').toLowerCase().trim();
        const pkgFilter = $filterByPackage.value.toLowerCase();

        const rows = state.configured
            .filter(p => {
                const base = (p.basePackageName || p.packageName || '').toLowerCase();
                const snap = (p.packageNameSnapshot || p.packageName || '').toLowerCase();
                const passText = !text || snap.includes(text);
                const passPkg  = !pkgFilter || base === pkgFilter;
                return passText && passPkg;
            })
            .map(p => {
                const active = (state.current.packageId === p.packageId) ? ' active' : '';
                const baseName = escapeHtml(p.basePackageName || p.packageName);
                const kmVal = p.pricePerKm != null ? Number(p.pricePerKm).toLocaleString('vi-VN') + ' đ/km' : 'chưa cấu hình';
                const labelName = escapeHtml(p.packageNameSnapshot || p.packageName);

                return `<button type="button" class="list-group-item list-group-item-action${active}" data-id="${p.packageId}">
          <div class="d-flex justify-content-between align-items-center">
            <div>
              <div class="fw-semibold">${labelName}</div>
              <div class="small muted">Gói gốc: ${baseName} • ${kmVal}</div>
            </div>
            <i class="bi bi-chevron-right"></i>
          </div>
        </button>`;
            }).join('');

        $configuredList.innerHTML = rows || `<div class="small muted">Chưa có gói nào. Hãy kiểm tra API /service-packages.</div>`;
        $configuredCount.textContent = state.configured.length;

        $configuredList.querySelectorAll('button[data-id]').forEach(btn => {
            btn.addEventListener('click', () => openPackage(parseInt(btn.getAttribute('data-id'),10)));
        });
    }

    function buildPackageFilterOptions() {
        const uniques = new Set();
        state.allPackages.forEach(p => {
            const base = (p.basePackageName || p.packageName || '').trim();
            if (base) uniques.add(base);
        });
        const opts = ['<option value="">Lọc theo gói gốc (Tất cả)</option>']
            .concat(Array.from(uniques).sort().map(n => `<option value="${escapeHtml(n)}">${escapeHtml(n)}</option>`));
        $filterByPackage.innerHTML = opts.join('');
    }

    function highlightSelectedOnLeft(packageId) {
        $configuredList.querySelectorAll('button[data-id]').forEach(btn => {
            btn.classList.toggle('active', parseInt(btn.getAttribute('data-id'),10) === packageId);
        });
    }

    // ==================== 5. RENDER RIGHT (DETAIL) ====================
    function showDetailForm(on) {
        $detailForm.classList.toggle('d-none', !on);
        $emptyHint.classList.toggle('d-none', on);
    }

    function renderItemsTable() {
        const html = state.current.items.map((row, idx) => {
            const name = row.furnitureItemName ?? '';
            const price = (row.price ?? '') === '' ? '' : row.price;
            const idInfo = row.furnitureItemId ? `<div class="form-text muted">ID: ${row.furnitureItemId}</div>` : '';
            return `<tr data-idx="${idx}">
        <td>
          <input class="form-control form-control-sm js-name" maxlength="120"
                 value="${escapeHtml(name)}" placeholder="Tên (VD: Sofa)">
          ${idInfo}
        </td>
        <td>
          <input class="form-control form-control-sm js-price" type="number" min="0" step="1000"
                 value="${price}" placeholder="VNĐ">
        </td>
        <td class="text-nowrap">
          <button class="btn btn-sm btn-outline-warning js-clear" title="Xoá giá (giữ item)">
            <i class="bi bi-eraser"></i>
          </button>
          <button class="btn btn-sm btn-outline-danger js-del" title="Xoá dòng">
            <i class="bi bi-x"></i>
          </button>
        </td>
      </tr>`;
        }).join('');
        $itemsTableBody.innerHTML = html;

        $itemsTableBody.querySelectorAll('tr').forEach(tr => {
            const idx = parseInt(tr.getAttribute('data-idx'),10);
            const it = state.current.items[idx];
            const $name  = tr.querySelector('.js-name');
            const $price = tr.querySelector('.js-price');

            $name.addEventListener('input', e => {
                it.furnitureItemName = e.target.value;
                toggleInvalid($name, !validNameOrEmpty(e.target.value, !!it.furnitureItemId));
            });
            $price.addEventListener('input', e => {
                const v = e.target.value;
                it.price = (v === '' ? null : Number(v));
                toggleInvalid($price, !validPriceOrEmpty(v));
            });

            tr.querySelector('.js-clear').addEventListener('click', () => {
                it.price = null;
                renderItemsTable();
            });
            tr.querySelector('.js-del').addEventListener('click', () => {
                state.current.items.splice(idx, 1);
                renderItemsTable();
            });
        });
    }

    // ==================== 6. LOAD DATA ====================
    async function ensureProviderId() {
        if (state.providerId) return;
        try {
            const me = await apiGet('/api/providers/me');
            state.providerId = me.providerId || null;
        } catch (_) {
            // bỏ qua, sẽ báo lỗi ở loadPackages
        }
    }

    async function loadPackages() {
        await ensureProviderId();
        if (!state.providerId) {
            toast('Không xác định được Provider (meta provider-id hoặc /api/providers/me).', 'error', 8000);
            return;
        }

        try {
            const raw = await apiGet(`/api/providers/${state.providerId}/service-packages`);
            const all = Array.isArray(raw) ? raw : [];

            state.allPackages = all.map(p => ({
                ...p,
                pricePerKm: p.pricePerKm ?? p.perKm ?? p.per_km ?? null,
                packageNameSnapshot: p.packageNameSnapshot ?? p.snapshotName ?? null
            }));

            state.configured = state.allPackages.slice();

            buildPackageFilterOptions();
            renderConfiguredList();

            if (state.current.packageId != null) {
                const still = state.allPackages.some(p => p.packageId === state.current.packageId);
                if (!still) clearCurrent();
            }
            if (state.current.packageId == null) showDetailForm(false);

        } catch (e) {
            console.error(e);
            toast('Không tải được danh sách gói dịch vụ: ' + e.message, 'error', 8000);
        }
    }

    async function openPackage(packageId) {
        await ensureProviderId();
        if (!state.providerId) return;

        const meta = state.allPackages.find(p => p.packageId === packageId) || {};
        const d = await apiGet(`/api/providers/${state.providerId}/service-packages/${packageId}`);

        const perKm = d.pricePerKm ?? d.perKm ?? d.per_km ?? null;
        let furniture = d.furniturePrices || d.items || [];

        state.current.packageId = packageId;
        state.current.basePackageName = meta.basePackageName || meta.packageName || null;
        state.current.packageName = meta.packageName || null;
        state.current.perKm = perKm;

        // ==== CHỈ LOAD TỪ DB ====
        // Nếu DB chưa có record nào -> tạo 1 dòng trống để provider tự nhập
        if (!Array.isArray(furniture) || furniture.length === 0) {
            state.current.items = [{
                furnitureItemId: null,
                furnitureItemName: '',
                price: null
            }];
        } else {
            state.current.items = furniture.map(x => ({
                furnitureItemId: x.furnitureItemId ?? x.furnitureTypeId ?? x.id ?? null,
                furnitureItemName: x.furnitureItemName ?? x.furnitureTypeName ?? x.name ?? '',
                price: x.price
            }));
        }

        $packageNameSnapshot.value = d.packageNameSnapshot || meta.packageNameSnapshot || '';
        $pricePerKm.value = perKm == null ? '' : perKm;

        $packageNameSnapshot.classList.remove('is-invalid','is-valid');
        $pricePerKm.classList.remove('is-invalid','is-valid');

        renderItemsTable();
        showDetailForm(true);
        highlightSelectedOnLeft(packageId);
    }

    function clearCurrent() {
        state.current.packageId = null;
        state.current.basePackageName = null;
        state.current.packageName = null;
        state.current.perKm = null;
        state.current.items = [];
        $packageNameSnapshot.value = '';
        $pricePerKm.value = '';
        $itemsTableBody.innerHTML = '';
        showDetailForm(false);
        highlightSelectedOnLeft(-1);
    }

    // ==================== 7. VALIDATION ====================
    function validSnapshotNameOrEmpty(v) {
        if (!v) return true;
        return typeof v === 'string' && v.trim().length <= 120;
    }

    function validPricePerKmOrEmpty(v) {
        if (v === '' || v === null || v === undefined) return true;
        const n = Number(v);
        return !Number.isNaN(n) && n >= 0 && n <= 1e12;
    }

    function validNameOrEmpty(name, hasId) {
        if (hasId) return true;
        const v = (name || '').trim();
        return v.length > 0 && v.length <= 120;
    }

    function validPriceOrEmpty(v) {
        if (v === '' || v === null || v === undefined) return true;
        const n = Number(v);
        return !Number.isNaN(n) && n >= 0 && n <= 1e12;
    }

    function validateBeforeSave() {
        const errs = [];

        if (!state.providerId) errs.push('Không xác định được Provider.');
        if (!state.current.packageId) errs.push('Chưa chọn gói để lưu.');

        if (!validSnapshotNameOrEmpty($packageNameSnapshot.value)) {
            errs.push('Tên snapshot quá dài (<= 120 ký tự).');
            toggleInvalid($packageNameSnapshot, true);
        } else {
            toggleInvalid($packageNameSnapshot, false);
        }

        if (!validPricePerKmOrEmpty($pricePerKm.value)) {
            errs.push('Giá mỗi km không hợp lệ.');
            toggleInvalid($pricePerKm, true);
        } else {
            toggleInvalid($pricePerKm, false);
        }

        $itemsTableBody.querySelectorAll('tr').forEach(tr => {
            const idx = parseInt(tr.getAttribute('data-idx'),10);
            const it = state.current.items[idx];
            const $name = tr.querySelector('.js-name');
            const $price = tr.querySelector('.js-price');

            const nameOk  = validNameOrEmpty(it.furnitureItemName, !!it.furnitureItemId);
            const priceOk = validPriceOrEmpty(it.price);

            toggleInvalid($name, !nameOk);
            toggleInvalid($price, !priceOk);

            if (!nameOk)  errs.push(`Dòng ${idx+1}: tên nội thất bắt buộc (khi thêm mới).`);
            if (!priceOk) errs.push(`Dòng ${idx+1}: giá không hợp lệ.`);
        });

        return errs;
    }

    // ==================== 8. SAVE / RESET / DELETE ====================
    async function saveCurrent() {
        const errs = validateBeforeSave();
        if (errs.length) {
            toast(errs[0], 'warning', 4500);
            return;
        }

        const body = {
            providerId: state.providerId,
            packageId: state.current.packageId,
            packageNameSnapshot: ($packageNameSnapshot.value || null),
            pricePerKm: ($pricePerKm.value === '' ? null : Number($pricePerKm.value)),
            furniturePrices: state.current.items.map(x => ({
                furnitureItemId: x.furnitureItemId || null,
                furnitureItemName: x.furnitureItemId ? null : ((x.furnitureItemName || '').trim() || null),
                price: (x.price === '' || x.price === null || x.price === undefined) ? null : Number(x.price)
            }))
        };

        const url = `/api/providers/${state.providerId}/service-packages/${state.current.packageId}`;
        await apiPut(url, body);
        toast('Đã lưu cấu hình gói & bảng giá nội thất.', 'success');

        await loadPackages();
        await openPackage(state.current.packageId);
    }

    async function resetCurrent() {
        if (!state.current.packageId) return;
        if (!confirm('Đặt lại: xoá toàn bộ giá nội thất & để trống giá/km?')) return;

        $pricePerKm.value = '';
        state.current.items.forEach(it => it.price = null);
        await saveCurrent();
        toast('Đã đặt lại cấu hình.', 'success');
    }

    async function deleteCurrent() {
        if (!state.current.packageId) return;
        if (!confirm('Xoá snapshot này? Dữ liệu giá sẽ bị xoá.')) return;

        const url = `/api/providers/${state.providerId}/service-packages/${state.current.packageId}`;
        await apiPut(url, {
            providerId: state.providerId,
            packageId: state.current.packageId,
            packageNameSnapshot: null,
            pricePerKm: null,
            furniturePrices: []
        });

        toast('Đã xoá snapshot.', 'success');
        clearCurrent();
        await loadPackages();
    }

    // ==================== 9. CREATE SNAPSHOT MỚI ====================
    function openAddModal() {
        const opts = state.allPackages
            .map(p => `<option value="${p.packageId}">${escapeHtml(p.basePackageName || p.packageName)}</option>`)
            .join('');
        $modalPackageSelect.innerHTML = opts || `<option disabled>(Chưa có dữ liệu gói)</option>`;
        $modalPerKm.value = '';
        addModal && addModal.show();
    }

    async function createPackage() {
        const pkgId = parseInt($modalPackageSelect.value || '0', 10);
        if (!pkgId) {
            toast('Chưa chọn gói gốc.', 'warning');
            return;
        }

        if (!validPricePerKmOrEmpty($modalPerKm.value)) {
            toast('Giá mỗi km ban đầu không hợp lệ.', 'warning');
            return;
        }

        const perKmInit = $modalPerKm.value === '' ? null : Number($modalPerKm.value);

        const body = {
            providerId: state.providerId,
            packageId: pkgId,
            packageNameSnapshot: null,
            pricePerKm: perKmInit,
            furniturePrices: []
        };

        await apiPut(`/api/providers/${state.providerId}/service-packages/${pkgId}`, body);
        toast('Đã tạo/khởi tạo snapshot cho gói.', 'success');

        addModal && addModal.hide();
        await loadPackages();
        await openPackage(pkgId);
    }

    // ==================== 10. EVENTS ====================
    $btnRefresh.addEventListener('click', loadPackages);
    $btnOpenAdd.addEventListener('click', openAddModal);
    $btnCreatePkg && $btnCreatePkg.addEventListener('click', createPackage);

    $filterByPackage.addEventListener('change', renderConfiguredList);
    $searchConfigured.addEventListener('input', renderConfiguredList);

    $btnAddRow.addEventListener('click', () => {
        state.current.items.push({ furnitureItemId: null, furnitureItemName: '', price: null });
        renderItemsTable();
    });

    $btnSave.addEventListener('click', saveCurrent);
    $btnReset.addEventListener('click', resetCurrent);
    $btnDelete.addEventListener('click', deleteCurrent);

    // ==================== 11. INIT ====================
    (async function init(){
        try {
            await loadPackages();
            toast('Đã tải danh sách gói.', 'success', 1800);
        } catch (e) {
            console.error(e);
            toast('Lỗi khởi tạo trang PV-002: ' + e.message, 'error', 8000);
        }
    })();

})();
