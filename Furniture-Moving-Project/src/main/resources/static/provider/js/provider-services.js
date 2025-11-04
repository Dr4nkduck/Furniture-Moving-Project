/* PV-002 Provider Services JS
 * Endpoints dùng:
 *  - GET  /api/providers/me -> { providerId }
 *  - GET  /api/providers/{pid}/service-packages
 *  - GET  /api/providers/{pid}/service-packages/{packageId}
 *  - PUT  /api/providers/{pid}/service-packages/{packageId}
 */

(() => {
    // ---------- DOM ----------
    const $configuredList = document.getElementById('configuredList');
    const $configuredCount = document.getElementById('configuredCount');
    const $searchConfigured = document.getElementById('searchConfigured');

    const $btnRefresh = document.getElementById('btnRefresh');
    const $btnOpenAdd = document.getElementById('btnOpenAdd');

    const $detailForm = document.getElementById('detailForm');
    const $emptyHint = document.getElementById('emptyHint');
    const $packageNameSnapshot = document.getElementById('packageNameSnapshot');
    const $pricePerKm = document.getElementById('pricePerKm');
    const $btnAddRow = document.getElementById('btnAddRow');
    const $btnSave = document.getElementById('btnSave');
    const $btnReset = document.getElementById('btnReset');
    const $itemsTableBody = document.querySelector('#itemsTable tbody');

    // modal
    const addModalEl = document.getElementById('addPkgModal');
    const addModal = addModalEl ? new bootstrap.Modal(addModalEl) : null;
    const $modalPackageSelect = document.getElementById('modalPackageSelect');
    const $modalPerKm = document.getElementById('modalPerKm');
    const $btnCreatePkg = document.getElementById('btnCreatePkg');

    // toasts
    const $toastBox = document.getElementById('toastBox');

    // ---------- STATE ----------
    const state = {
        providerId: getProviderIdFromMeta(),
        allPackages: [],      // tất cả gói trong hệ thống
        configured: [],       // các gói đã cấu hình (perKm != null)
        unconfigured: [],     // các gói chưa cấu hình
        current: {
            packageId: null,
            packageName: null,
            perKm: null,
            items: [] // { furnitureItemId, furnitureItemName, price }
        },
        lastLoadedSnapshotName: null
    };

    // ---------- UTIL ----------
    function getProviderIdFromMeta() {
        const meta = document.querySelector('meta[name="provider-id"]');
        if (meta && meta.content) {
            const v = parseInt(meta.content, 10);
            if (!Number.isNaN(v)) return v;
        }
        return null;
    }

    function toast(msg, type = 'info', timeoutMs = 3000) {
        const div = document.createElement('div');
        div.className = `alert alert-${type === 'error' ? 'danger' : type} border-0 shadow mb-2`;
        div.innerHTML = `<div class="d-flex align-items-center">
        <i class="bi ${typeIcon(type)} me-2"></i>
        <div>${msg}</div>
      </div>`;
        $toastBox.appendChild(div);
        setTimeout(() => { div.remove(); }, timeoutMs);
    }

    function typeIcon(type) {
        switch (type) {
            case 'success': return 'bi-check-circle';
            case 'warning': return 'bi-exclamation-triangle';
            case 'error': return 'bi-x-circle';
            default: return 'bi-info-circle';
        }
    }

    async function apiGet(url) {
        const r = await fetch(url);
        if (!r.ok) throw new Error(`GET ${url} -> ${r.status}`);
        return r.json();
    }

    async function apiPut(url, body) {
        const r = await fetch(url, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
        if (!r.ok) throw new Error(`PUT ${url} -> ${r.status}`);
        return r.json();
    }

    function moneyOrEmpty(v) {
        if (v === null || v === undefined) return '';
        const num = Number(v);
        return Number.isNaN(num) ? '' : num.toLocaleString('vi-VN');
    }

    // ---------- RENDER LEFT LIST ----------
    function renderConfiguredList(filterText = '') {
        const rows = state.configured
            .filter(p => p.packageName.toLowerCase().includes(filterText.toLowerCase()))
            .map(p => {
                const active = (state.current.packageId === p.packageId) ? ' active' : '';
                const km = p.pricePerKm ? `${moneyOrEmpty(p.pricePerKm)} đ/km` : 'Chưa đặt giá/km';
                return `<button type="button" class="list-group-item list-group-item-action${active}" data-id="${p.packageId}">
                  <div class="d-flex justify-content-between align-items-center">
                    <div>
                      <div class="fw-semibold">${escapeHtml(p.packageName)}</div>
                      <div class="small muted">${km}</div>
                    </div>
                    <i class="bi bi-chevron-right"></i>
                  </div>
                </button>`;
            }).join('');

        $configuredList.innerHTML = rows || `<div class="small muted">Chưa có gói nào. Bấm <b>Thêm gói</b> để khởi tạo.</div>`;
        $configuredCount.textContent = state.configured.length;

        // bind click
        $configuredList.querySelectorAll('button[data-id]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const pkgId = parseInt(btn.getAttribute('data-id'), 10);
                await openPackage(pkgId);
            });
        });
    }

    function escapeHtml(s) {
        return (s || '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    }

    // ---------- RENDER DETAIL ----------
    function renderItemsTable() {
        const html = state.current.items.map((row, idx) => {
            const name = row.furnitureItemName ?? '';
            const price = (row.price ?? '') === '' ? '' : row.price;
            const idInfo = row.furnitureItemId ? `<div class="form-text muted">ID: ${row.furnitureItemId}</div>` : '';
            return `<tr data-idx="${idx}">
        <td>
          <input class="form-control form-control-sm js-name" value="${escapeHtml(name)}" placeholder="Tên (VD: Sofa)">
          ${idInfo}
        </td>
        <td><input class="form-control form-control-sm js-price" type="number" min="0" step="1000" value="${price}"></td>
        <td class="text-nowrap">
          <button class="btn btn-sm btn-outline-warning js-clear" title="Xoá giá (giữ item)"><i class="bi bi-eraser"></i></button>
          <button class="btn btn-sm btn-outline-danger js-del" title="Xoá dòng"><i class="bi bi-x"></i></button>
        </td>
      </tr>`;
        }).join('');
        $itemsTableBody.innerHTML = html;

        // bind row events
        $itemsTableBody.querySelectorAll('tr').forEach(tr => {
            const idx = parseInt(tr.getAttribute('data-idx'), 10);
            tr.querySelector('.js-name').addEventListener('input', e => {
                state.current.items[idx].furnitureItemName = e.target.value;
            });
            tr.querySelector('.js-price').addEventListener('input', e => {
                const v = e.target.value;
                state.current.items[idx].price = (v === '' ? null : Number(v));
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

    function showDetailForm(on) {
        $detailForm.classList.toggle('d-none', !on);
        $emptyHint.classList.toggle('d-none', on);
    }

    // ---------- DATA LOADERS ----------
    async function ensureProviderId() {
        if (state.providerId) return;
        try {
            const me = await apiGet('/api/providers/me');
            state.providerId = me.providerId || null;
        } catch (_) { /* ignore */ }
    }

    async function loadPackages() {
        await ensureProviderId();
        if (!state.providerId) {
            toast('Không xác định được Provider. Hãy đăng nhập bằng tài khoản Provider.', 'error');
            return;
        }
        const all = await apiGet(`/api/providers/${state.providerId}/service-packages`);
        state.allPackages = all;

        // tách đã cấu hình vs chưa cấu hình
        state.configured = all.filter(p => p.pricePerKm !== null && p.pricePerKm !== undefined);
        const configuredIds = new Set(state.configured.map(p => p.packageId));
        state.unconfigured = all.filter(p => !configuredIds.has(p.packageId));

        renderConfiguredList($searchConfigured.value || '');
        await afterPackagesLoaded();
    }

    async function afterPackagesLoaded() {
        // Nếu đang mở gói mà không còn trong danh sách, đóng form
        if (state.current.packageId !== null) {
            const stillExists = state.allPackages.some(p => p.packageId === state.current.packageId);
            if (!stillExists) {
                clearCurrent();
            }
        }
        if (state.current.packageId === null) {
            showDetailForm(false);
        }
    }

    async function openPackage(packageId) {
        await ensureProviderId();
        const d = await apiGet(`/api/providers/${state.providerId}/service-packages/${packageId}`);

        state.current.packageId = packageId;
        const pkgMeta = state.allPackages.find(p => p.packageId === packageId);
        state.current.packageName = pkgMeta ? pkgMeta.packageName : (d.packageNameSnapshot || '');
        state.current.perKm = d.pricePerKm ?? null;
        state.current.items = (d.furniturePrices || []).map(x => ({
            furnitureItemId: x.furnitureItemId,
            furnitureItemName: x.furnitureItemName,
            price: x.price
        }));

        // render
        $packageNameSnapshot.value = d.packageNameSnapshot || '';
        $pricePerKm.value = (state.current.perKm ?? '') === '' ? '' : state.current.perKm;
        renderItemsTable();
        showDetailForm(true);
        highlightSelectedOnLeft(packageId);
        state.lastLoadedSnapshotName = $packageNameSnapshot.value || '';
    }

    function highlightSelectedOnLeft(packageId) {
        $configuredList.querySelectorAll('button[data-id]').forEach(btn => {
            btn.classList.toggle('active', parseInt(btn.getAttribute('data-id'), 10) === packageId);
        });
    }

    function clearCurrent() {
        state.current.packageId = null;
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
    function validateBeforeSave() {
        const errs = [];

        if (!state.providerId) errs.push('Không xác định được Provider.');
        if (!state.current.packageId) errs.push('Chưa chọn gói để lưu.');

        // perKm
        const vkm = $pricePerKm.value;
        if (vkm !== '' && (isNaN(Number(vkm)) || Number(vkm) < 0)) {
            errs.push('Giá mỗi km không hợp lệ.');
        }

        // items
        const namesSeen = new Set();
        for (let i = 0; i < state.current.items.length; i++) {
            const it = state.current.items[i];
            const name = (it.furnitureItemName || '').trim();

            if (!it.furnitureItemId && !name) {
                errs.push(`Dòng ${i + 1}: yêu cầu tên nội thất (khi thêm mới).`);
            }
            if (name) {
                const key = name.toLowerCase();
                if (namesSeen.has(key)) errs.push(`Dòng ${i + 1}: tên nội thất bị trùng.`);
                namesSeen.add(key);
            }
            if (it.price !== null && (isNaN(Number(it.price)) || Number(it.price) < 0)) {
                errs.push(`Dòng ${i + 1}: giá không hợp lệ.`);
            }
        }

        return errs;
    }

    // ---------- SAVE / RESET ----------
    async function saveCurrent() {
        const errs = validateBeforeSave();
        if (errs.length) {
            toast(errs[0], 'warning', 4000);
            return;
        }
        const body = {
            providerId: state.providerId,
            packageId: state.current.packageId,
            packageName: ($packageNameSnapshot.value || null),
            pricePerKm: ($pricePerKm.value === '' ? null : Number($pricePerKm.value)),
            furniturePrices: state.current.items.map(x => ({
                furnitureItemId: x.furnitureItemId || null,
                furnitureItemName: x.furnitureItemId ? null : (x.furnitureItemName || null),
                price: (x.price === '' || x.price === null || x.price === undefined) ? null : Number(x.price)
            }))
        };

        await apiPut(`/api/providers/${state.providerId}/service-packages/${state.current.packageId}`, body);
        toast('Đã lưu cấu hình gói.', 'success');

        // reload lists (để cập nhật “đã cấu hình/chưa cấu hình”)
        await loadPackages();
        // mở lại gói vừa lưu
        await openPackage(state.current.packageId);
    }

    async function resetCurrent() {
        if (!state.current.packageId) return;
        const ok = confirm('Đặt lại giá: xoá toàn bộ giá nội thất & để trống giá/km?');
        if (!ok) return;

        // set rỗng local và gửi save
        $pricePerKm.value = '';
        state.current.items.forEach(it => it.price = null);

        await saveCurrent();
        toast('Đã đặt lại cấu hình giá của gói.', 'success');
    }

    // ---------- ADD PACKAGE FLOW ----------
    function openAddModal() {
        // build select danh sách chưa cấu hình
        const opts = state.unconfigured.map(p => `<option value="${p.packageId}">${escapeHtml(p.packageName)}</option>`).join('');
        $modalPackageSelect.innerHTML = opts || `<option disabled>(Không còn gói trống)</option>`;
        $modalPerKm.value = '';
        addModal && addModal.show();
    }

    async function createPackage() {
        const pkgId = parseInt($modalPackageSelect.value || '0', 10);
        if (!pkgId) { toast('Chưa chọn gói.', 'warning'); return; }

        const perKmInit = $modalPerKm.value === '' ? 0 : Number($modalPerKm.value);
        if (isNaN(perKmInit) || perKmInit < 0) { toast('Giá mỗi km ban đầu không hợp lệ.', 'warning'); return; }

        const body = {
            providerId: state.providerId,
            packageId: pkgId,
            packageName: null, // sẽ snapshot bằng tên hệ thống lần đầu
            pricePerKm: perKmInit,
            furniturePrices: []
        };
        await apiPut(`/api/providers/${state.providerId}/service-packages/${pkgId}`, body);
        toast('Đã tạo gói & đặt giá/km ban đầu.', 'success');

        addModal && addModal.hide();
        await loadPackages();
        await openPackage(pkgId);
    }

    // ---------- EVENTS ----------
    $btnRefresh.addEventListener('click', loadPackages);
    $btnOpenAdd.addEventListener('click', openAddModal);
    $btnCreatePkg && $btnCreatePkg.addEventListener('click', createPackage);

    $searchConfigured.addEventListener('input', e => {
        renderConfiguredList(e.target.value || '');
    });

    $btnAddRow.addEventListener('click', () => {
        state.current.items.push({ furnitureItemId: null, furnitureItemName: '', price: null });
        renderItemsTable();
    });

    $btnSave.addEventListener('click', saveCurrent);
    $btnReset.addEventListener('click', resetCurrent);

    // ---------- INIT ----------
    (async function init() {
        try {
            await loadPackages();
            toast('Đã tải danh sách gói.', 'success', 1800);
        } catch (e) {
            toast(e.message, 'error', 5000);
        }
    })();

})();
