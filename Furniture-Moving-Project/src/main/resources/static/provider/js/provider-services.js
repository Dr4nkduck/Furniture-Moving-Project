// PV-002 • Provider Service & Price Catalog
(function(){
    document.addEventListener('DOMContentLoaded', init);

    async function init(){
        // --------- Shorthand ----------
        const $  = (s)=>document.querySelector(s);
        const $$ = (s)=>Array.from(document.querySelectorAll(s));

        // --------- Elements ----------
        const packageSelect = $('#packageSelect');
        const packageSelectFeedback = $('#packageSelectFeedback');

        const pricePerKm = $('#pricePerKm');
        const pricePerKmFeedback = $('#pricePerKmFeedback');

        const filterInput = $('#filterInput');
        const bulkValue   = $('#bulkValue');
        const applyBulkBtn= $('#applyBulkBtn');
        const clearAllBtn = $('#clearAllBtn');

        const addItemName = $('#addItemName');
        const addItemNameFeedback = $('#addItemNameFeedback');
        const addItemPrice = $('#addItemPrice');
        const addItemPriceFeedback = $('#addItemPriceFeedback');
        const addItemBtn   = $('#addItemBtn');

        const itemsBody    = $('#itemsBody');
        const saveAllBtn   = $('#saveAllBtn');
        const saveTableBtn = $('#saveTableBtn');
        const loading      = $('#loadingOverlay');

        // --------- State ----------
        let providerId = null;
        let currentPackageId = null;
        let fullItemList = []; // { furnitureItemId|null, furnitureItemName, price|null }
        let catalogCache = null;
        let dirty = false; const setDirty=(v=true)=>dirty=v;

        // --------- CSRF headers (if enabled) ----------
        const csrf = {
            token:  document.querySelector('meta[name="_csrf"]')?.content,
            header: document.querySelector('meta[name="_csrf_header"]')?.content
        };
        const JSON_HEADERS = {'Content-Type':'application/json'};
        if (csrf.token && csrf.header) JSON_HEADERS[csrf.header] = csrf.token;

        // --------- Guard before unload ----------
        window.addEventListener('beforeunload', (e)=>{ if(dirty){ e.preventDefault(); e.returnValue=''; } });

        // --------- Events ----------
        packageSelect.addEventListener('change', ()=>onPackageChange().catch(errHandler));
        filterInput.addEventListener('input', applyFilter);
        applyBulkBtn.addEventListener('click', onApplyBulk);
        clearAllBtn.addEventListener('click', onClearAll);
        addItemBtn.addEventListener('click', onAddNewItem);
        saveAllBtn.addEventListener('click', onSaveAll);
        saveTableBtn.addEventListener('click', onSaveAll);

        // --------- Init flow ----------
        try{
            showLoading(true);
            providerId = await resolveProviderId();
            if(!providerId){ toast('Không xác định được Provider. Thêm ?providerId=... vào URL hoặc cấu hình meta provider-id.', 'danger'); disableAll(); return; }
            await loadPackages();              // load list packages & select first
            await onPackageChange();           // load perKm + furniture prices for selected
            setDirty(false);
        }catch(e){ errHandler(e); }
        finally{ showLoading(false); }

        // ======================================================================
        // API helpers
        async function apiGet(url){
            const r = await fetch(url, {credentials:'same-origin'});
            if(!r.ok){
                const text = await r.text().catch(()=>r.statusText);
                throw new Error(`[GET ${r.status}] ${text||r.statusText}`);
            }
            // Some endpoints may return 204; handle gracefully
            if(r.status===204) return null;
            return r.json();
        }
        async function apiPut(url, body){
            const r = await fetch(url, {method:'PUT', credentials:'same-origin', headers: JSON_HEADERS, body: JSON.stringify(body)});
            if(!r.ok){
                let text;
                try{ text = await r.text(); }catch(_){}
                throw new Error(`[PUT ${r.status}] ${text || r.statusText}`);
            }
            return true;
        }

        // ======================================================================
        // ProviderId resolution (không đụng backend /me)
        async function resolveProviderId(){
            // 1) URL ?providerId=
            const q = new URLSearchParams(location.search);
            const qid = q.get('providerId');
            if(validId(qid)) return qid;

            // 2) Meta injection
            const meta = document.querySelector('meta[name="provider-id"]')?.content;
            if(validId(meta)) return meta;

            // 3) localStorage
            const saved = localStorage.getItem('pv_providerId');
            if(validId(saved)) return saved;

            // 4) prompt
            const entered = window.prompt('Nhập Provider ID:', '');
            if(validId(entered)){
                localStorage.setItem('pv_providerId', entered.trim());
                return entered.trim();
            }
            return null;

            function validId(v){ return v && /^\d+$/.test(String(v).trim()); }
        }

        // ======================================================================
        // Packages
        async function loadPackages(){
            try{
                showLoading(true);
                disableTopControls(true);
                const list = await apiGet(`/api/providers/${providerId}/service-packages`);
                packageSelect.innerHTML = '';

                if(!Array.isArray(list) || list.length===0){
                    // Không có gói → feedback + disable
                    packageSelect.classList.add('is-invalid');
                    packageSelectFeedback.classList.remove('d-none');
                    packageSelect.insertAdjacentHTML('beforeend', `<option value="">(Chưa có gói)</option>`);
                    pricePerKm.value = '';
                    renderTable([]);
                    return;
                }

                packageSelect.classList.remove('is-invalid');
                packageSelectFeedback.classList.add('d-none');

                // render options
                for(const p of list){
                    const opt = document.createElement('option');
                    opt.value = p.packageId;
                    opt.textContent = p.packageName + (p.pricePerKm!=null ? ` • ${Number(p.pricePerKm).toLocaleString('vi-VN')} đ/km` : '');
                    packageSelect.appendChild(opt);
                }
                // keep selection if any
                const want = String(currentPackageId||'');
                if (want && list.some(x => String(x.packageId)===want)){
                    packageSelect.value = want;
                }else{
                    packageSelect.value = String(list[0].packageId);
                }
            } finally {
                disableTopControls(false);
                showLoading(false);
            }
        }

        async function onPackageChange(){
            currentPackageId = Number(packageSelect.value||0);
            if(!currentPackageId){
                pricePerKm.value = '';
                renderTable([]);
                return;
            }
            // load detail
            try{
                showLoading(true);
                const detail = await apiGet(`/api/providers/${providerId}/service-packages/${currentPackageId}`);
                // per-km
                pricePerKm.value = (detail && detail.pricePerKm!=null) ? Number(detail.pricePerKm) : '';

                // catalog
                if(!catalogCache){
                    try{
                        catalogCache = await apiGet('/api/furniture-items'); // [{furnitureItemId, furnitureItemName}]
                    }catch(e){
                        catalogCache = []; // fallback
                    }
                }
                const catalog = Array.isArray(catalogCache) ? catalogCache : [];
                const priced  = Array.isArray(detail?.furniturePrices) ? detail.furniturePrices : [];
                fullItemList = mergeCatalogWithPrices(catalog, priced);
                applyFilter();
                setDirty(false);
            } finally{
                showLoading(false);
            }
        }

        function mergeCatalogWithPrices(catalog, priced){
            const byId = new Map();
            priced.forEach(p => { if(p.furnitureItemId!=null) byId.set(p.furnitureItemId, p.price ?? null); });

            const merged = [];
            for(const c of catalog){
                merged.push({
                    furnitureItemId: c.furnitureItemId ?? null,
                    furnitureItemName: (c.furnitureItemName||'').trim(),
                    price: byId.has(c.furnitureItemId) ? byId.get(c.furnitureItemId) : null
                });
            }
            // các item được thêm tay (không id)
            for(const p of priced){
                if(p.furnitureItemId==null && p.furnitureItemName){
                    merged.push({
                        furnitureItemId: null,
                        furnitureItemName: p.furnitureItemName.trim(),
                        price: p.price ?? null
                    });
                }
            }
            // sort: có giá trước, theo alpha
            merged.sort((a,b)=>{
                const aw = a.price==null ? 1:0, bw = b.price==null ? 1:0;
                return aw-bw || a.furnitureItemName.localeCompare(b.furnitureItemName,'vi');
            });
            return merged;
        }

        // ======================================================================
        // Table render & interactions
        function applyFilter(){
            const q = (filterInput.value||'').trim().toLowerCase();
            if(!q) return renderTable(fullItemList);
            renderTable(fullItemList.filter(x => (x.furnitureItemName||'').toLowerCase().includes(q)));
        }

        function renderTable(rows){
            itemsBody.innerHTML = '';
            rows.forEach(r=>{
                const tr = document.createElement('tr');
                tr.dataset.itemId = r.furnitureItemId ?? '';
                tr.dataset.itemName = r.furnitureItemName ?? '';

                const tdName = document.createElement('td');
                tdName.textContent = r.furnitureItemName;

                const tdPrice = document.createElement('td');
                const input = document.createElement('input');
                input.type='number'; input.min='0'; input.step='1000'; input.className='form-control';
                if(r.price!=null) input.value = Number(r.price);
                input.placeholder = 'để trống = xoá';
                input.addEventListener('input', ()=>onPriceEdit(input));

                tdPrice.appendChild(input);
                tr.append(tdName, tdPrice);
                itemsBody.appendChild(tr);
            });
        }

        function onPriceEdit(input){
            const tr = input.closest('tr');
            const id = tr.dataset.itemId ? Number(tr.dataset.itemId) : null;
            const name = tr.dataset.itemName || '';
            const raw = input.value;

            // Validation: empty = delete, else >= 0 number
            if(raw !== '' && (!/^\d+(\.\d+)?$/.test(raw) || Number(raw) < 0)){
                input.classList.add('is-invalid');
                input.setAttribute('title', 'Giá phải là số ≥ 0 hoặc để trống để xoá');
                return;
            } else {
                input.classList.remove('is-invalid');
                input.removeAttribute('title');
            }

            const val = (raw==='' ? null : Number(raw));
            const idx = fullItemList.findIndex(x =>
                x.furnitureItemId ? x.furnitureItemId===id : (!x.furnitureItemId && x.furnitureItemName===name)
            );
            if(idx>=0){
                fullItemList[idx].price = val;
                setDirty(true);
                // nếu là item thêm tay (id=null) và user xoá giá → xoá luôn hàng
                if(fullItemList[idx].furnitureItemId==null && val==null){
                    fullItemList.splice(idx,1); applyFilter();
                }
            }
        }

        // ======================================================================
        // Add new item row (validation)
        function onAddNewItem(){
            // name validation
            const name = (addItemName.value||'').trim();
            const priceRaw = (addItemPrice.value||'').trim();

            let ok = true;
            // validate name
            if(!name || name.length>100){
                addItemName.classList.add('is-invalid'); addItemNameFeedback.classList.remove('d-none'); ok=false;
            } else {
                addItemName.classList.remove('is-invalid'); addItemNameFeedback.classList.add('d-none');
            }
            // validate price
            if(priceRaw!=='' && (!/^\d+(\.\d+)?$/.test(priceRaw) || Number(priceRaw)<0)){
                addItemPrice.classList.add('is-invalid'); addItemPriceFeedback.classList.remove('d-none'); ok=false;
            } else {
                addItemPrice.classList.remove('is-invalid'); addItemPriceFeedback.classList.add('d-none');
            }
            if(!ok){ toast('Vui lòng sửa lỗi trước khi thêm.', 'warning'); return; }

            // duplicate check (case-insensitive)
            const dup = fullItemList.find(x => (x.furnitureItemName||'').toLowerCase() === name.toLowerCase());
            const priceNum = (priceRaw===''?null:Number(priceRaw));

            if(dup){
                dup.price = priceNum;
                toast('Nội thất đã có, mình đã cập nhật giá.', 'info');
            }else{
                fullItemList.push({ furnitureItemId:null, furnitureItemName:name, price:priceNum });
                toast('Đã thêm nội thất mới vào bảng (chưa lưu).', 'success');
            }
            addItemName.value=''; addItemPrice.value='';
            setDirty(true); applyFilter();
        }

        // ======================================================================
        // Bulk & Clear
        function onApplyBulk(){
            const raw = bulkValue.value;
            if(raw==='' || !/^\d+(\.\d+)?$/.test(raw) || Number(raw)<0){
                toast('Giá áp hàng loạt phải là số ≥ 0.', 'warning');
                bulkValue.focus(); return;
            }
            const v = Number(raw);
            $$('#itemsBody input[type="number"]').forEach(inp=>{
                inp.value = v;
                inp.dispatchEvent(new Event('input',{bubbles:true}));
            });
            toast('Đã áp dụng giá cho tất cả nội thất (chưa lưu).', 'info');
        }

        function onClearAll(){
            if(!confirm('Xoá toàn bộ giá trong bảng? Hành động không thể hoàn tác.')) return;
            $$('#itemsBody input[type="number"]').forEach(inp=>{
                inp.value='';
                inp.dispatchEvent(new Event('input',{bubbles:true}));
            });
            toast('Đã xoá giá (chưa lưu).', 'info');
        }

        // ======================================================================
        // Save
        function buildPayload(){
            const furniturePrices = fullItemList
                .filter(x => !(x.furnitureItemId==null && (x.price==null || x.price==='')))
                .map(x => ({
                    furnitureItemId: x.furnitureItemId ?? undefined,
                    furnitureItemName: x.furnitureItemId ? undefined : x.furnitureItemName,
                    price: (x.price==='' ? null : x.price)
                }));

            return {
                providerId: Number(providerId),
                packageId:  Number(currentPackageId),
                pricePerKm: (pricePerKm.value==='' ? null : Number(pricePerKm.value)),
                furniturePrices
            };
        }

        function validateTop(){
            let ok = true;

            // package required
            if(!currentPackageId){
                packageSelect.classList.add('is-invalid');
                packageSelectFeedback.classList.remove('d-none');
                ok = false;
            } else {
                packageSelect.classList.remove('is-invalid');
                packageSelectFeedback.classList.add('d-none');
            }

            // per-km optional but if filled must be >=0
            const raw = pricePerKm.value.trim();
            if(raw !== '' && (!/^\d+(\.\d+)?$/.test(raw) || Number(raw)<0)){
                pricePerKm.classList.add('is-invalid');
                pricePerKmFeedback.classList.remove('d-none');
                ok = false;
            } else {
                pricePerKm.classList.remove('is-invalid');
                pricePerKmFeedback.classList.add('d-none');
            }

            // table rows validation (each non-empty must be >=0)
            const invalidRow = $$('#itemsBody input[type="number"]').find(inp=>{
                const v = inp.value.trim();
                return v!=='' && (!/^\d+(\.\d+)?$/.test(v) || Number(v)<0);
            });
            if(invalidRow){
                invalidRow.classList.add('is-invalid');
                invalidRow.scrollIntoView({behavior:'smooth', block:'center'});
                toast('Có giá nội thất không hợp lệ. Sửa trước khi lưu.', 'warning');
                ok=false;
            }
            return ok;
        }

        async function onSaveAll(){
            if(!validateTop()) return;
            if(!dirty){
                toast('Không có thay đổi để lưu.', 'info');
                return;
            }
            try{
                showLoading(true); disableTopControls(true);
                const body = buildPayload();
                await apiPut(`/api/providers/${providerId}/service-packages/${currentPackageId}`, body);
                toast('Đã lưu cấu hình.', 'success');
                setDirty(false);
                // refresh để đồng bộ giá trị đã lưu
                await onPackageChange();
            }catch(e){
                errHandler(e);
            }finally{
                disableTopControls(false); showLoading(false);
            }
        }

        // ======================================================================
        // Utilities
        function toast(msg, type='primary'){
            let wrap = document.querySelector('.toast-wrap');
            if(!wrap){ wrap = document.createElement('div'); wrap.className='toast-wrap'; document.body.appendChild(wrap); }
            const el = document.createElement('div');
            el.className = `toast align-items-center text-bg-${type} border-0 show`;
            el.role='alert'; el.ariaLive='assertive'; el.ariaAtomic='true';
            el.innerHTML = `<div class="d-flex">
        <div class="toast-body">${msg}</div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
      </div>`;
            wrap.appendChild(el);
            setTimeout(()=>el.remove(), 4200);
        }

        function errHandler(e){
            console.error(e);
            toast('Lỗi: ' + (e?.message || e), 'danger');
        }

        function showLoading(v){
            if(!loading) return;
            loading.classList.toggle('show', !!v);
        }

        function disableTopControls(disabled){
            [packageSelect, pricePerKm, saveAllBtn, saveTableBtn, addItemBtn, applyBulkBtn, clearAllBtn].forEach(el=>{
                if(el) el.disabled = !!disabled;
            });
        }

        function disableAll(){
            disableTopControls(true);
            [filterInput, bulkValue, addItemName, addItemPrice].forEach(el => el && (el.disabled = true));
        }
    }
})();
