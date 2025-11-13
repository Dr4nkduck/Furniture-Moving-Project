/* PV-002 Provider Services JS
 * Backend endpoints (không đổi):
 *  - GET  /api/providers/{pid}/service-packages
 *        -> [ { packageId, packageName, basePackageName?, pricePerKm?/perKm } ]
 *  - GET  /api/providers/{pid}/service-packages/{packageId}
 *        -> { packageNameSnapshot, pricePerKm?/perKm, furniturePrices: [{furnitureItemId,furnitureItemName,price}] }
 *  - PUT  /api/providers/{pid}/service-packages/{packageId}
 *        -> lưu snapshot (pricePerKm + furniturePrices). Nếu muốn "xóa", gửi pricePerKm=null và furniturePrices=[]
 *  - (tuỳ) GET /api/providers/me -> lấy providerId; ưu tiên <meta name="provider-id">
 */

(() => {
    // ==================== 0. MASTER JSON NỘI THẤT (mặc định) ====================
    // Nếu package chưa có furniturePrices, sẽ auto sinh từ list này để provider sửa giá.
    const MASTER_FURNITURE = [
        { "name": "bàn", "price": 15000 },
        { "name": "bàn làm việc", "price": 20000 },
        { "name": "bàn học", "price": 18000 },
        { "name": "bàn ăn", "price": 22000 },
        { "name": "bàn trà", "price": 12000 },
        { "name": "bàn gấp", "price": 9000 },
        { "name": "ghế", "price": 8000 },
        { "name": "ghế xoay", "price": 10000 },
        { "name": "ghế sofa", "price": 300000 },
        { "name": "ghế băng", "price": 15000 },
        { "name": "ghế đôn", "price": 5000 },
        { "name": "ghế ăn", "price": 7000 },
        { "name": "ghế đơn", "price": 10000 },
        { "name": "sofa 2 chỗ", "price": 350000 },
        { "name": "sofa 3 chỗ", "price": 450000 },
        { "name": "sofa góc", "price": 550000 },
        { "name": "giường đơn", "price": 300000 },
        { "name": "giường đôi", "price": 400000 },
        { "name": "giường tầng", "price": 500000 },
        { "name": "nệm", "price": 12000 },
        { "name": "tủ quần áo", "price": 350000 },
        { "name": "tủ 3 cánh", "price": 450000 },
        { "name": "tủ 4 cánh", "price": 550000 },
        { "name": "tủ đầu giường", "price": 8000 },
        { "name": "tủ giày", "price": 12000 },
        { "name": "kệ sách", "price": 15000 },
        { "name": "kệ TV", "price": 15000 },
        { "name": "tủ bếp nhỏ", "price": 30000 },
        { "name": "tủ bếp lớn", "price": 500000 },
        { "name": "bếp từ", "price": 12000 },
        { "name": "bếp gas", "price": 10000 },
        { "name": "tủ lạnh mini", "price": 20000 },
        { "name": "tủ lạnh 2 cánh", "price": 350000 },
        { "name": "tủ lạnh side-by-side", "price": 600000 },
        { "name": "máy giặt", "price": 300000 },
        { "name": "máy sấy", "price": 300000 },
        { "name": "máy rửa chén", "price": 350000 },
        { "name": "máy lạnh treo tường", "price": 300000 },
        { "name": "máy lạnh đứng", "price": 500000 },
        { "name": "quạt điện", "price": 5000 },
        { "name": "quạt cây", "price": 7000 },
        { "name": "quạt bàn", "price": 4000 },
        { "name": "tivi 32 inch", "price": 12000 },
        { "name": "tivi 43 inch", "price": 16000 },
        { "name": "tivi 55 inch", "price": 220000 },
        { "name": "dàn loa", "price": 15000 },
        { "name": "máy tính để bàn", "price": 15000 },
        { "name": "màn hình máy tính", "price": 8000 },
        { "name": "máy in", "price": 12000 },
        { "name": "case PC", "price": 10000 },
        { "name": "bàn phím", "price": 2000 },
        { "name": "chuột máy tính", "price": 2000 },
        { "name": "lò vi sóng", "price": 10000 },
        { "name": "nồi chiên không dầu", "price": 8000 },
        { "name": "nồi cơm điện", "price": 6000 },
        { "name": "bình nước nóng", "price": 8000 },
        { "name": "bàn ủi", "price": 4000 },
        { "name": "máy hút bụi", "price": 9000 },
        { "name": "đèn", "price": 5000 },
        { "name": "đèn bàn", "price": 3000 },
        { "name": "đèn cây", "price": 5000 },
        { "name": "gương đứng", "price": 8000 },
        { "name": "gương treo tường", "price": 6000 },
        { "name": "tranh treo tường", "price": 5000 },
        { "name": "đồng hồ treo tường", "price": 3000 },
        { "name": "cây cảnh nhỏ", "price": 5000 },
        { "name": "cây cảnh lớn", "price": 120000 },
        { "name": "chậu cây gốm", "price": 7000 },
        { "name": "valy nhỏ", "price": 5000 },
        { "name": "valy lớn", "price": 7000 },
        { "name": "thùng carton nhỏ", "price": 2000 },
        { "name": "thùng carton lớn", "price": 3000 },
        { "name": "kệ kho sắt", "price": 250000 },
        { "name": "kệ nhựa", "price": 10000 },
        { "name": "bàn trang điểm", "price": 22000 },
        { "name": "ghế trang điểm", "price": 7000 },
        { "name": "tủ thuốc", "price": 8000 },
        { "name": "lều", "price": 5000 },
        { "name": "xe đạp", "price": 150000 },
        { "name": "xe đạp điện", "price": 220000 },
        { "name": "máy chạy bộ", "price": 400000 },
        { "name": "máy tập đa năng", "price": 450000 },
        { "name": "tạ tay", "price": 5000 },
        { "name": "bàn bida mini", "price": 400000 },
        { "name": "đàn piano cơ", "price": 1500000 },
        { "name": "đàn organ", "price": 20000 },
        { "name": "đàn guitar", "price": 6000 },
        { "name": "đàn ukulele", "price": 4000 },
        { "name": "loa kéo", "price": 10000 },
        { "name": "tủ hồ sơ", "price": 20000 },
        { "name": "bàn họp", "price": 350000 },
        { "name": "ghế họp", "price": 8000 },
        { "name": "máy chiếu", "price": 8000 },
        { "name": "màn chiếu", "price": 7000 },
        { "name": "máy photocopy", "price": 500000 },
        { "name": "két sắt nhỏ", "price": 200000 },
        { "name": "két sắt lớn", "price": 450000 },
        { "name": "bàn thờ", "price": 350000 },
        { "name": "tủ thờ", "price": 450000 },
        { "name": "chậu rửa chén", "price": 12000 },
        { "name": "bình lọc nước", "price": 9000 },
        { "name": "xe đẩy em bé", "price": 10000 },
        { "name": "nôi em bé", "price": 15000 },
        { "name": "cũi em bé", "price": 20000 },
        { "name": "bình gas", "price": 9000 },
        { "name": "bếp nướng điện", "price": 8000 },
        { "name": "máy xay sinh tố", "price": 4000 },
        { "name": "máy ép trái cây", "price": 6000 },
        { "name": "lò nướng", "price": 12000 },
        { "name": "máy pha cà phê", "price": 12000 }
    ];

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
        // cấu hình cho UI (đưa hết allPackages để luôn thấy dữ liệu)
        configured: [],
        current: {
            packageId: null,
            basePackageName: null,
            packageName: null,
            perKm: null,
            items: [] // [{furnitureItemId,furnitureItemName,price}]
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
            // Chuẩn hoá key pricePerKm và packageNameSnapshot cho frontend
            state.allPackages = all.map(p => ({
                ...p,
                pricePerKm: p.pricePerKm ?? p.perKm ?? p.per_km ?? null,
                packageNameSnapshot: p.packageNameSnapshot ?? p.snapshotName ?? null
            }));

            // ➜ FIX: luôn đưa ALL packages ra UI (trước đây filter pricePerKm => rỗng)
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

        // Chuẩn hoá detail
        const perKm = d.pricePerKm ?? d.perKm ?? d.per_km ?? null;
        let furniture = d.furniturePrices || d.items || [];

        // Nếu backend chưa có bảng giá, auto seed từ MASTER_FURNITURE
        if (!furniture || !furniture.length) {
            furniture = MASTER_FURNITURE.map(it => ({
                furnitureItemId: null,
                furnitureItemName: it.name,
                price: it.price
            }));
        }

        state.current.packageId = packageId;
        state.current.basePackageName = meta.basePackageName || meta.packageName || null;
        state.current.packageName = meta.packageName || null;
        state.current.perKm = perKm;
        state.current.items = furniture.map(x => ({
            furnitureItemId: x.furnitureItemId || x.id || null,
            furnitureItemName: x.furnitureItemName || x.name || '',
            price: x.price
        }));

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
            // backend đang dùng packageNameSnapshot
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

        // Tạo snapshot rỗng với giá/km ban đầu, bảng giá trống (khi mở sẽ auto seed MASTER_FURNITURE)
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
