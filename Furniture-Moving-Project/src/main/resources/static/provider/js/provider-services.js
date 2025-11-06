/* PV-002 Provider Services JS
 * Endpoints giả định (không đổi backend):
 *  - GET  /api/providers/{pid}/service-packages                 -> danh sách [ { packageId, packageName, basePackageName?, pricePerKm } ]
 *  - GET  /api/providers/{pid}/service-packages/{packageId}      -> chi tiết { packageNameSnapshot, pricePerKm, furniturePrices: [{furnitureItemId,furnitureItemName,price}] }
 *  - PUT  /api/providers/{pid}/service-packages/{packageId}      -> lưu snapshot (pricePerKm + furniturePrices). Nếu muốn "xóa", gửi pricePerKm=null và furniturePrices=[]
 *  - (Tùy chọn) GET /api/providers/me để lấy providerId; ưu tiên <meta name="provider-id">
 */

(() => {
    // ---------- DOM ----------
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

    // Toasts
    const $toastBox = document.getElementById('toastBox');

    // ---------- STATE ----------
    const state = {
        providerId: getProviderIdFromMeta(),
        allPackages: [],   // tất cả snapshot (mỗi entry đại diện 1 snapshot hoặc cấu hình hiện tại của gói)
        configured: [],    // lọc theo rule UI (pricePerKm != null)
        current: {
            packageId: null,
            basePackageName: null, // nếu API trả về, dùng cho filter; nếu không sẽ dùng packageName
            packageName: null,     // hiển thị tại danh sách
            perKm: null,
            items: [] // [{furnitureItemId,furnitureItemName,price}]
        }
    };

    // ---------- UTIL ----------
    /** Lấy providerId từ <meta name="provider-id"> nếu controller đã gắn */
    function getProviderIdFromMeta() {
        const meta = document.querySelector('meta[name="provider-id"]');
        if (meta && meta.content) {
            const v = parseInt(meta.content, 10);
            if (!Number.isNaN(v)) return v;
        }
        return null;
    }

    /** Hiển thị toast thông báo */
    function toast(msg, type = 'info', timeoutMs = 3000) {
        const div = document.createElement('div');
        div.className = `alert alert-${type === 'error' ? 'danger' : type} border-0 shadow mb-2`;
        div.innerHTML = `<div class="d-flex align-items-center">
      <i class="bi ${typeIcon(type)} me-2"></i><div>${msg}</div></div>`;
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

    /** GET helper: throw nếu status != 200 */
    async function apiGet(url) {
        const r = await fetch(url);
        if (!r.ok) throw new Error(await errorText(r, `GET ${url} -> ${r.status}`));
        return r.json();
    }

    /** PUT helper: trả json|text; throw nếu !ok (bắt đc message 500) */
    async function apiPut(url, body) {
        const r = await fetch(url, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (!r.ok) throw new Error(await errorText(r, `PUT ${url} -> ${r.status}`));
        // backend có thể trả body rỗng
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
        return (s || '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    }

    // ---------- RENDER (LEFT) ----------
    /** Vẽ danh sách snapshot đã cấu hình, có filter theo text + gói gốc */
    function renderConfiguredList() {
        const text = ($searchConfigured.value || '').toLowerCase().trim();
        const pkgFilter = $filterByPackage.value; // '' = all, else by base package name

        const rows = state.configured
            .filter(p => {
                const base = (p.basePackageName || p.packageName || '').toLowerCase();
                const snap = (p.packageName || '').toLowerCase();
                const passText = !text || snap.includes(text);
                const passPkg  = !pkgFilter || base === pkgFilter.toLowerCase();
                return passText && passPkg;
            })
            .map(p => {
                const active = (state.current.packageId === p.packageId) ? ' active' : '';
                const baseName = escapeHtml(p.basePackageName || p.packageName);
                const km = (p.pricePerKm != null) ? `${Number(p.pricePerKm).toLocaleString('vi-VN')} đ/km` : '—';
                return `<button type="button" class="list-group-item list-group-item-action${active}" data-id="${p.packageId}">
          <div class="d-flex justify-content-between align-items-center">
            <div>
              <div class="fw-semibold">${escapeHtml(p.packageName)}</div>
              <div class="small muted">Gói gốc: ${baseName} • ${km}</div>
            </div>
            <i class="bi bi-chevron-right"></i>
          </div>
        </button>`;
            }).join('');

        $configuredList.innerHTML = rows || `<div class="small muted">Chưa có snapshot nào. Bấm <b>Thêm gói</b> để tạo.</div>`;
        $configuredCount.textContent = state.configured.length;

        // bind click
        $configuredList.querySelectorAll('button[data-id]').forEach(btn => {
            btn.addEventListener('click', () => openPackage(parseInt(btn.getAttribute('data-id'),10)));
        });
    }

    /** Tạo option filter theo gói gốc */
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

    // ---------- RENDER (RIGHT) ----------
    /** Hiện/ẩn form chi tiết */
    function showDetailForm(on) {
        $detailForm.classList.toggle('d-none', !on);
        $emptyHint.classList.toggle('d-none', on);
    }

    /** Vẽ bảng item (nội thất) */
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
            <i class="bi bi-eraser"></i></button>
          <button class="btn btn-sm btn-outline-danger js-del" title="Xoá dòng">
            <i class="bi bi-x"></i></button>
        </td>
      </tr>`;
        }).join('');
        $itemsTableBody.innerHTML = html;

        // bind events
        $itemsTableBody.querySelectorAll('tr').forEach(tr => {
            const idx = parseInt(tr.getAttribute('data-idx'), 10);
            const $name  = tr.querySelector('.js-name');
            const $price = tr.querySelector('.js-price');

            $name.addEventListener('input', e => {
                state.current.items[idx].furnitureItemName = e.target.value;
                toggleInvalid($name, !validNameOrEmpty(e.target.value, !!state.current.items[idx].furnitureItemId));
            });
            $price.addEventListener('input', e => {
                const v = e.target.value;
                state.current.items[idx].price = (v === '' ? null : Number(v));
                toggleInvalid($price, !validPriceOrEmpty(v));
            });

            tr.querySelector('.js-clear').addEventListener('click', () => {
                state.current.items[idx].price = null;
                renderItemsTable();
            });
            tr.querySelector('.js-del').addEventListener('click', () => {
                state.current.items.splice(idx, 1);
                renderItemsTable();
            });
        });
    }

    function toggleInvalid(input, invalid) {
        input.classList.toggle('is-invalid', invalid);
        input.classList.toggle('is-valid', !invalid);
    }

    // ---------- DATA LOAD ----------
    /** Ưu tiên providerId từ meta; nếu không có sẽ thử gọi /api/providers/me (nếu backend hỗ trợ) */
    async function ensureProviderId() {
        if (state.providerId) return;
        try {
            const me = await apiGet('/api/providers/me');
            state.providerId = me.providerId || null;
        } catch (_) {/* ignore */}
    }

    /** Load danh sách snapshot/gói, build filter, vẽ list */
    async function loadPackages() {
        await ensureProviderId();
        if (!state.providerId) {
            toast('Không xác định được Provider. Hãy truyền providerId vào meta hoặc bật API /api/providers/me.', 'error', 5000);
            return;
        }
        const all = await apiGet(`/api/providers/${state.providerId}/service-packages`);
        state.allPackages = Array.isArray(all) ? all : [];

        // cấu hình coi như "đã dùng" nếu pricePerKm !== null
        state.configured = state.allPackages.filter(p => p.pricePerKm !== null && p.pricePerKm !== undefined);

        buildPackageFilterOptions();
        renderConfiguredList();

        // nếu đang mở mà không còn → đóng form
        if (state.current.packageId !== null) {
            const still = state.allPackages.some(p => p.packageId === state.current.packageId);
            if (!still) clearCurrent();
        }
        if (state.current.packageId === null) showDetailForm(false);
    }

    /** Mở một snapshot theo packageId */
    async function openPackage(packageId) {
        await ensureProviderId();
        const d = await apiGet(`/api/providers/${state.providerId}/service-packages/${packageId}`);

        const meta = state.allPackages.find(p => p.packageId === packageId) || {};
        state.current.packageId = packageId;
        state.current.basePackageName = meta.basePackageName || meta.packageName || null;
        state.current.packageName = meta.packageName || d.packageNameSnapshot || '';
        state.current.perKm = (d.pricePerKm ?? null);
        state.current.items = (d.furniturePrices || []).map(x => ({
            furnitureItemId: x.furnitureItemId,
            furnitureItemName: x.furnitureItemName,
            price: x.price
        }));

        // render UI
        $packageNameSnapshot.value = d.packageNameSnapshot || '';
        $pricePerKm.value = (state.current.perKm ?? '') === '' ? '' : state.current.perKm;
        $packageNameSnapshot.classList.remove('is-invalid','is-valid');
        $pricePerKm.classList.remove('is-invalid','is-valid');

        renderItemsTable();
        showDetailForm(true);
        highlightSelectedOnLeft(packageId);
    }

    function highlightSelectedOnLeft(packageId) {
        $configuredList.querySelectorAll('button[data-id]').forEach(btn => {
            btn.classList.toggle('active', parseInt(btn.getAttribute('data-id'),10) === packageId);
        });
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

    // ---------- VALIDATION ----------
    /** Tên snapshot hợp lệ (cho phép rỗng để dùng tên gốc) */
    function validSnapshotNameOrEmpty(v) {
        if (!v) return true;
        return typeof v === 'string' && v.trim().length <= 120;
    }

    /** Giá/km hợp lệ khi có giá trị (rỗng = null) */
    function validPricePerKmOrEmpty(v) {
        if (v === '' || v === null || v === undefined) return true;
        const n = Number(v);
        return !Number.isNaN(n) && n >= 0 && n <= 1e12;
    }

    /** Tên nội thất: nếu thêm mới (không có id) thì bắt buộc có tên */
    function validNameOrEmpty(name, hasId) {
        if (hasId) return true; // item đã tồn tại, cho phép để trống (không đổi tên)
        const v = (name || '').trim();
        return v.length > 0 && v.length <= 120;
    }

    /** Giá item hợp lệ khi có giá trị */
    function validPriceOrEmpty(v) {
        if (v === '' || v === null || v === undefined) return true;
        const n = Number(v);
        return !Number.isNaN(n) && n >= 0 && n <= 1e12;
    }

    /** Đánh dấu lỗi form tổng hợp & trả về mảng lỗi đầu tiên để toast */
    function validateBeforeSave() {
        const errs = [];

        if (!state.providerId) errs.push('Không xác định được Provider.');
        if (!state.current.packageId) errs.push('Chưa chọn gói để lưu.');

        // snapshot name
        if (!validSnapshotNameOrEmpty($packageNameSnapshot.value)) {
            errs.push('Tên snapshot quá dài (<= 120 ký tự).');
            toggleInvalid($packageNameSnapshot, true);
        } else {
            toggleInvalid($packageNameSnapshot, false);
        }

        // perKm
        if (!validPricePerKmOrEmpty($pricePerKm.value)) {
            errs.push('Giá mỗi km không hợp lệ.');
            toggleInvalid($pricePerKm, true);
        } else {
            toggleInvalid($pricePerKm, false);
        }

        // items
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

    // ---------- SAVE / RESET / DELETE ----------
    /** Lưu snapshot hiện tại (PUT) */
    async function saveCurrent() {
        const errs = validateBeforeSave();
        if (errs.length) { toast(errs[0], 'warning', 4500); return; }

        const body = {
            providerId: state.providerId,
            packageId: state.current.packageId,
            packageName: ($packageNameSnapshot.value || null), // null = dùng tên gốc
            pricePerKm: ($pricePerKm.value === '' ? null : Number($pricePerKm.value)),
            furniturePrices: state.current.items.map(x => ({
                furnitureItemId: x.furnitureItemId || null,
                furnitureItemName: x.furnitureItemId ? null : ((x.furnitureItemName || '').trim() || null),
                price: (x.price === '' || x.price === null || x.price === undefined) ? null : Number(x.price)
            }))
        };

        const url = `/api/providers/${state.providerId}/service-packages/${state.current.packageId}`;
        await apiPut(url, body);
        toast('Đã lưu snapshot.', 'success');

        await loadPackages();
        await openPackage(state.current.packageId);
    }

    /** Đặt lại giá: xóa toàn bộ giá item + để trống giá/km (coi như clear) */
    async function resetCurrent() {
        if (!state.current.packageId) return;
        if (!confirm('Đặt lại: xoá toàn bộ giá nội thất & để trống giá/km?')) return;

        $pricePerKm.value = '';
        state.current.items.forEach(it => it.price = null);

        await saveCurrent();
        toast('Đã đặt lại cấu hình.', 'success');
    }

    /** XÓA snapshot: không cần DELETE; gửi PUT với giá/km=null và furniturePrices=[] */
    async function deleteCurrent() {
        if (!state.current.packageId) return;
        if (!confirm('Xoá snapshot này? Dữ liệu giá sẽ bị xoá.')) return;

        const url = `/api/providers/${state.providerId}/service-packages/${state.current.packageId}`;
        await apiPut(url, {
            providerId: state.providerId,
            packageId: state.current.packageId,
            packageName: null,
            pricePerKm: null,
            furniturePrices: []
        });

        toast('Đã xoá snapshot.', 'success');
        clearCurrent();
        await loadPackages();
    }

    // ---------- ADD SNAPSHOT ----------
    /** Mở modal thêm (KHÔNG chặn nếu "không còn gói trống": cho phép tạo nhiều snapshot cho cùng gói) */
    function openAddModal() {
        // build danh sách từ toàn bộ gói
        const opts = state.allPackages
            .map(p => `<option value="${p.packageId}">${escapeHtml(p.basePackageName || p.packageName)}</option>`)
            .join('');
        $modalPackageSelect.innerHTML = opts || `<option disabled>(Chưa có dữ liệu gói)</option>`;
        $modalPerKm.value = '';
        addModal && addModal.show();
    }

    /** Tạo snapshot mới bằng PUT (ghi đè/cập nhật trên packageId) */
    async function createPackage() {
        const pkgId = parseInt($modalPackageSelect.value || '0', 10);
        if (!pkgId) { toast('Chưa chọn gói.', 'warning'); return; }

        const perKmInit = $modalPerKm.value === '' ? null : Number($modalPerKm.value);
        if (!validPricePerKmOrEmpty($modalPerKm.value)) {
            toast('Giá mỗi km ban đầu không hợp lệ.', 'warning'); return;
        }

        const body = {
            providerId: state.providerId,
            packageId: pkgId,
            packageName: null,
            pricePerKm: perKmInit,
            furniturePrices: [] // bắt đầu rỗng
        };
        await apiPut(`/api/providers/${state.providerId}/service-packages/${pkgId}`, body);
        toast('Đã tạo snapshot.', 'success');

        addModal && addModal.hide();
        await loadPackages();
        await openPackage(pkgId);
    }

    // ---------- EVENTS ----------
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

    // ---------- INIT ----------
    (async function init(){
        try {
            await loadPackages();
            toast('Đã tải danh sách gói.', 'success', 1800);
        } catch (e) {
            toast(e.message, 'error', 6000);
        }
    })();

})();
