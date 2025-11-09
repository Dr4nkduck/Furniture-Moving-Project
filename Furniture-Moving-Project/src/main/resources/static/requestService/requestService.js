/* ===================== CẤU HÌNH ===================== */
const API_BASE  = 'http://localhost:8080';
const DRAFT_KEY = 'FT_REQUEST_DRAFT';       // draft chuyển sang distanceCalculation
let   itemCount = 1;
let   currentUser = null;

/* ===================== MOCK LOGIN ===================== */
/* Đọc localStorage để có { userId, customerId } cho backend sau này */
function mockLogin() {
    const key = 'currentUser';
    const existing = localStorage.getItem(key);
    if (existing) return JSON.parse(existing);
    const mock = { userId: 2, customerId: 1, username: 'test_customer' };
    localStorage.setItem(key, JSON.stringify(mock));
    return mock;
}

/* ===================== UI HELPERS ===================== */
function showAlert(message, type) {
    const alertBox = document.getElementById('alertBox');
    alertBox.textContent = message;
    alertBox.className = `alert alert-${type}`;
    alertBox.style.display = 'block';
    setTimeout(() => { alertBox.style.display = 'none'; }, 5000);
    alertBox.scrollIntoView({ behavior: 'smooth', block: 'start' });
}
function setSubmitting(isSubmitting) {
    const submitBtn = document.querySelector('.btn-submit');
    submitBtn.disabled = isSubmitting;
    submitBtn.textContent = isSubmitting ? '⏳ Đang gửi...' : '📤 Gửi Yêu Cầu';
}

/* ===================== PROVIDERS ===================== */
async function loadProviders() {
    try {
        const res = await fetch(`${API_BASE}/api/providers`);
        const txt = await res.text();
        let data; try { data = JSON.parse(txt); } catch { data = null; }
        const list = Array.isArray(data) ? data : (data && Array.isArray(data.data)) ? data.data : [];
        const sel = document.getElementById('providerId');
        sel.innerHTML = '<option value="">-- Chọn sau --</option>';
        list.forEach(p => {
            const opt = document.createElement('option');
            const id   = p.providerId ?? p.id ?? p.provider_id;
            const name = p.companyName ?? p.name ?? p.company_name ?? 'Nhà cung cấp';
            const rating = (p.rating != null) ? ` ⭐ ${p.rating}` : '';
            opt.value = id;
            opt.textContent = `${name}${rating}`;
            sel.appendChild(opt);
        });
    } catch (e) {
        console.error('loadProviders error:', e);
    }
}

/* ===================== ITEMS ===================== */
function addFurnitureItem() {
    itemCount++;
    const container = document.getElementById('furnitureItems');
    const div = document.createElement('div');
    div.className = 'item-card';
    div.innerHTML = `
    <div class="card-header">
      <span class="card-title">🪑 Món đồ #${itemCount}</span>
      <button type="button" class="btn btn-remove" onclick="this.closest('.item-card').remove()">Xóa</button>
    </div>
    <div class="row">
      <div class="form-group">
        <label>Loại Đồ <span class="required">*</span></label>
        <select class="item-type" required>
          <option value="">-- Chọn loại --</option>
          <option value="Sofa">Sofa</option><option value="Bed">Giường</option>
          <option value="Table">Bàn</option><option value="Chair">Ghế</option>
          <option value="Cabinet">Tủ</option><option value="Wardrobe">Tủ quần áo</option>
          <option value="Other">Khác</option>
        </select>
      </div>
      <div class="form-group">
        <label>Tên Món Đồ</label>
        <input type="text" class="item-name" placeholder="Ví dụ: Sofa 3 chỗ">
      </div>
    </div>
    <div class="row">
      <div class="form-group"><label>Số Lượng <span class="required">*</span></label>
        <input type="number" class="quantity" min="1" value="1" required>
      </div>
      <div class="form-group"><label>Kích Thước</label>
        <select class="size">
          <option value="">-- Chọn --</option>
          <option value="Small">Nhỏ</option><option value="Medium">Trung bình</option>
          <option value="Large">Lớn</option><option value="Extra Large">Rất lớn</option>
        </select>
      </div>
    </div>
    <div class="checkbox-group">
      <input type="checkbox" class="is-fragile" id="fragile${itemCount}">
      <label for="fragile${itemCount}">Đồ dễ vỡ</label>
    </div>
    <div class="form-group">
      <label>Ghi Chú Đặc Biệt</label>
      <textarea class="special-handling-notes" placeholder="Ví dụ: Cần bọc cẩn thận..."></textarea>
    </div>`;
    container.appendChild(div);
}

/* ===================== DATES & UTILS ===================== */
function isFutureOrToday(dateStr) {
    if (!dateStr) return false;
    const d = new Date(dateStr + 'T00:00:00');
    const today = new Date(); today.setHours(0,0,0,0);
    return d.getTime() >= today.getTime();
}
function requiredStr(s) { return s && s.trim().length > 0; }
const debounce = (fn, ms=400) => { let t; return (...a)=>{ clearTimeout(t); t=setTimeout(()=>fn(...a), ms);} };

/* ===================== OSM / NOMINATIM ===================== */
const NOM_BASE = 'https://nominatim.openstreetmap.org';
const VN_BOUNDS = L.latLngBounds([8.179, 102.144],[23.393, 109.469]);
const VIEWBOX   = [102.144, 23.393, 109.469, 8.179].join(',');
const VN_CENTER = [16.047, 108.206];

function initLeafletMap(id) {
    const map = L.map(id, { zoomControl: true, minZoom: 5, maxZoom: 18, maxBounds: VN_BOUNDS, maxBoundsViscosity: 1.0 });
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '&copy; OpenStreetMap contributors' }).addTo(map);
    map.fitBounds(VN_BOUNDS);
    return map;
}

async function searchVN(q) {
    if (!q || q.trim().length < 3) return [];
    const url = `${NOM_BASE}/search?format=jsonv2&addressdetails=1&limit=8&countrycodes=vn&viewbox=${VIEWBOX}&bounded=1&q=${encodeURIComponent(q)}`;
    const res = await fetch(url, { headers: { 'Accept-Language': 'vi,en' } });
    if (!res.ok) return [];
    const data = await res.json();
    return data.filter(x => x?.address?.country_code === 'vn');
}
async function reverseVN(lat, lon) {
    const url = `${NOM_BASE}/reverse?format=jsonv2&addressdetails=1&zoom=18&lat=${lat}&lon=${lon}`;
    const res = await fetch(url, { headers: { 'Accept-Language': 'vi,en' } });
    if (!res.ok) return null;
    const data = await res.json();
    if (data?.address?.country_code !== 'vn') return null;
    return data;
}

/* ===================== PROVINCES ===================== */
const VN_PROVINCES = [
    "An Giang","Bà Rịa - Vũng Tàu","Bắc Giang","Bắc Kạn","Bạc Liêu","Bắc Ninh","Bến Tre","Bình Dương",
    "Bình Định","Bình Phước","Bình Thuận","Cà Mau","Cần Thơ","Cao Bằng","Đà Nẵng","Đắk Lắk","Đắk Nông",
    "Điện Biên","Đồng Nai","Đồng Tháp","Gia Lai","Hà Giang","Hà Nam","Hà Nội","Hà Tĩnh","Hải Dương",
    "Hải Phòng","Hậu Giang","Hòa Bình","Hưng Yên","Khánh Hòa","Kiên Giang","Kon Tum","Lai Châu","Lâm Đồng",
    "Lạng Sơn","Lào Cai","Long An","Nam Định","Nghệ An","Ninh Bình","Ninh Thuận","Phú Thọ","Phú Yên",
    "Quảng Bình","Quảng Nam","Quảng Ngãi","Quảng Ninh","Quảng Trị","Sóc Trăng","Sơn La","Tây Ninh","Thái Bình",
    "Thái Nguyên","Thanh Hóa","Thừa Thiên Huế","Tiền Giang","TP Hồ Chí Minh","Trà Vinh","Tuyên Quang",
    "Vĩnh Long","Vĩnh Phúc","Yên Bái","Bà Rịa–Vũng Tàu","Bắc Ninh"
].filter((v,i,a)=>a.indexOf(v)===i).sort((a,b)=>a.localeCompare(b,'vi'));

function populateProvinces(selectEl) {
    selectEl.innerHTML = '<option value="">-- Chọn tỉnh/thành --</option>' +
        VN_PROVINCES.map(p => `<option value="${p}">${p}</option>`).join('');
}

/* ===================== ADDRESS HELPERS ===================== */
function normalizeVI(s=''){ return s.normalize('NFD').replace(/\p{Diacritic}/gu,'').toLowerCase(); }
function bestProvince(addr) { return addr.state || addr.region || addr.city || ''; }
function bestDistrict(addr) { return addr.city_district || addr.county || addr.state_district || addr.district || addr.town || ''; }

function fillAddressFromReverse(card, addr) {
    const road = addr.road || addr.residential || addr.pedestrian || addr.path || '';
    const houseNo = addr.house_number || '';
    const postcode = addr.postcode ? String(addr.postcode).trim() : '';

    card.querySelector('.road').value = road;
    card.querySelector('.house-number').value = houseNo;
    card.querySelector('.zip').value = postcode || card.querySelector('.zip').value || '00000';

    const provSel = card.querySelector('.province');
    const prov = bestProvince(addr);
    if (prov) {
        const normProv = normalizeVI(prov);
        let matched = VN_PROVINCES.find(p => normalizeVI(p) === normProv) ||
            VN_PROVINCES.find(p => normalizeVI(p).includes(normProv) || normProv.includes(normalizeVI(p)));
        if (!matched && /thanh pho ho chi minh|ho chi minh/i.test(normProv)) matched = "TP Hồ Chí Minh";
        if (matched) provSel.value = matched;
    }
    const districtInput = card.querySelector('.district');
    districtInput.value = bestDistrict(addr) || districtInput.value;
}

async function suggestDistricts(provinceName, keyword) {
    if (!provinceName || !keyword || keyword.trim().length < 1) return [];
    const url = `${NOM_BASE}/search?format=jsonv2&addressdetails=1&limit=20&countrycodes=vn&viewbox=${VIEWBOX}&bounded=1&q=${encodeURIComponent(keyword + ', ' + provinceName)}`;
    const res = await fetch(url, { headers: { 'Accept-Language': 'vi,en' } });
    if (!res.ok) return [];
    const data = await res.json();
    const normProv = normalizeVI(provinceName);
    const names = new Set();
    const out = [];
    for (const r of data) {
        const a = r.address || {};
        const prov = bestProvince(a);
        if (prov && normalizeVI(prov).indexOf(normProv) === -1) continue;
        const d = bestDistrict(a);
        if (!d) continue;
        const key = normalizeVI(d);
        if (!names.has(key)) { names.add(key); out.push({ name: d }); }
        if (out.length >= 20) break;
    }
    return out;
}

async function geocodeByForm({ house, road, district, province }) {
    if (!province || (!road && !district)) return null;
    const street = `${(house||'').trim()} ${road||''}`.trim();
    const params = new URLSearchParams({
        format: 'jsonv2', addressdetails: '1', limit: '1', country: 'Việt Nam',
        street, county: district || '', state: province
    });
    const url = `${NOM_BASE}/search?${params.toString()}`;
    let res = await fetch(url, { headers: { 'Accept-Language': 'vi,en' } });
    let data = res.ok ? await res.json() : [];
    if (!data?.length && road) {
        const p2 = new URLSearchParams({
            format: 'jsonv2', addressdetails: '1', limit: '1', country: 'Việt Nam',
            street: road, county: district || '', state: province
        });
        res = await fetch(`${NOM_BASE}/search?${p2.toString()}`, { headers: { 'Accept-Language': 'vi,en' } });
        data = res.ok ? await res.json() : [];
    }
    if (!data?.length) return null;
    const hit = data[0];
    if (hit?.address?.country_code !== 'vn') return null;
    return hit;
}

/* ===================== CARD STATE & MODE ===================== */
const cardState = new WeakMap(); // { mode, marker, lastRev }

function setMode(card, mode) {
    const state = cardState.get(card); state.mode = mode;

    const quickBlock   = card.querySelector('.quick-block');
    const quickInput   = quickBlock?.querySelector('.search-input');
    const quickBtn     = card.querySelector('button[data-action="search-quick"]');
    const useMarkerBtn = card.querySelector('button[data-action="use-marker"]');
    const geoBtn       = card.querySelector('button[data-action="geocode-form"]');
    const quickSuggest = quickBlock?.querySelector('.suggest-list');

    if (mode === 'quick') {
        quickBlock?.classList.remove('disabled');
        if (quickInput) quickInput.disabled = false;
        quickBtn && (quickBtn.disabled = false);
        useMarkerBtn && (useMarkerBtn.disabled = false);
        geoBtn && (geoBtn.disabled = true);
    } else {
        quickBlock?.classList.add('disabled');
        if (quickInput) quickInput.disabled = true;
        quickBtn && (quickBtn.disabled = true);
        useMarkerBtn && (useMarkerBtn.disabled = true);
        geoBtn && (geoBtn.disabled = false);
        if (quickSuggest) { quickSuggest.style.display = 'none'; quickSuggest.innerHTML = ''; }
    }
}

/* ===================== COLLECT DRAFT (GỬI QUA TRANG 2) ===================== */
function collectAddressForDraft(card) {
    const st = cardState.get(card) || {};
    const lat = st.marker ? +st.marker.getLatLng().lat.toFixed(8) : null;
    const lng = st.marker ? +st.marker.getLatLng().lng.toFixed(8) : null;

    return {
        houseNumber: (card.querySelector('.house-number')?.value || '').trim(),
        road:        (card.querySelector('.road')?.value || '').trim(),
        district:    (card.querySelector('.district')?.value || '').trim(),
        province:    (card.querySelector('.province')?.value || '').trim(),
        zip:         (card.querySelector('.zip')?.value || '00000').trim(),
        contactName: (card.querySelector('.contact-name')?.value || '').trim(),
        contactPhone:(card.querySelector('.contact-phone')?.value || '').trim(),
        specialInstructions: (card.querySelector('.special-instructions')?.value || '').trim(),
        lat, lng
    };
}

/* ===================== SETUP MỖI ADDRESS CARD ===================== */
function setupAddressCard(card, map) {
    let marker = null;
    cardState.set(card, { mode: 'quick', marker: null, lastRev: null });

    populateProvinces(card.querySelector('.province'));

    const type            = card.dataset.type || 'X';
    const modeRadios      = card.querySelectorAll(`input[name="mode-${type}"]`);
    modeRadios.forEach(r => r.addEventListener('change', e => setMode(card, e.target.value)));

    const quickBlock      = card.querySelector('.quick-block');
    const searchInput     = quickBlock.querySelector('.search-input');
    const suggestWrap     = quickBlock.querySelector('.suggest-list');
    const searchBtn       = card.querySelector('button[data-action="search-quick"]');
    const useMarkerBtn    = card.querySelector('button[data-action="use-marker"]');
    const geocodeBtn      = card.querySelector('button[data-action="geocode-form"]');
    const districtInput   = card.querySelector('.district');
    const districtSuggest = card.querySelector('.district-suggest');
    const zipInput        = card.querySelector('.zip');

    function clearSuggest(el){ if(!el) return; el.style.display='none'; el.innerHTML=''; }
    function setMarker(lat, lon, z=16){
        if(!marker){ marker=L.marker([lat,lon],{draggable:true}).addTo(map); }
        else { marker.setLatLng([lat,lon]); }
        map.setView([lat,lon], z);
        cardState.get(card).marker = marker;
    }

    // QUICK search
    const doQuickSearch = async () => {
        const q = searchInput?.value || '';
        const results = await searchVN(q);
        if (!results.length) {
            clearSuggest(suggestWrap);
            showAlert('❌ Không tìm thấy địa chỉ trong Việt Nam. Vui lòng nhập lại.', 'error');
            return;
        }
        if (results.length === 1) {
            const r = results[0];
            const lat = parseFloat(r.lat), lon = parseFloat(r.lon);
            setMarker(lat, lon, 17);
            const rev = await reverseVN(lat, lon);
            if (rev) {
                fillAddressFromReverse(card, rev.address);
                cardState.get(card).lastRev = rev;
                if (rev.address?.postcode) zipInput.value = String(rev.address.postcode).trim();
            }
            showAlert('✅ Đã đặt marker theo tìm nhanh.', 'success');
        } else {
            suggestWrap.innerHTML = results.map(r => `<div class="suggest-item" data-lat="${r.lat}" data-lon="${r.lon}">${r.display_name}</div>`).join('');
            suggestWrap.style.display = 'block';
        }
    };

    searchInput?.addEventListener('keydown', (e)=>{ if(e.key==='Enter'){ e.preventDefault(); doQuickSearch(); } });
    searchBtn?.addEventListener('click', doQuickSearch);

    searchInput?.addEventListener('input', debounce(async (e) => {
        const q = e.target.value;
        const results = await searchVN(q);
        if (!results.length) { clearSuggest(suggestWrap); return; }
        suggestWrap.innerHTML = results.map(r => `<div class="suggest-item" data-lat="${r.lat}" data-lon="${r.lon}">${r.display_name}</div>`).join('');
        suggestWrap.style.display = 'block';
    }, 450));

    // CLICK suggestion → đặt marker + fill form + zip
    suggestWrap?.addEventListener('click', async (e) => {
        const item = e.target.closest('.suggest-item'); if (!item) return;
        clearSuggest(suggestWrap);
        const lat = parseFloat(item.getAttribute('data-lat'));
        const lon = parseFloat(item.getAttribute('data-lon'));
        setMarker(lat, lon, 17);
        const rev = await reverseVN(lat, lon);
        if (!rev) { showAlert('Chỉ hỗ trợ địa chỉ tại Việt Nam.', 'error'); return; }
        fillAddressFromReverse(card, rev.address);
        cardState.get(card).lastRev = rev;
        if (rev.address?.postcode) zipInput.value = String(rev.address.postcode).trim();
        showAlert('✅ Đã đặt marker theo tìm nhanh.', 'success');
    });

    // MAP CLICK → fill form + zip + chuyển sang Form
    map.on('click', async (e) => {
        const { lat, lng } = e.latlng;
        if (!VN_BOUNDS.contains([lat, lng])) { showAlert('Ngoài lãnh thổ Việt Nam.', 'error'); return; }
        setMarker(lat, lng, 16);
        const rev = await reverseVN(lat, lng);
        if (!rev) { showAlert('Chỉ hỗ trợ địa chỉ tại Việt Nam.', 'error'); return; }
        fillAddressFromReverse(card, rev.address);
        cardState.get(card).lastRev = rev;
        if (rev.address?.postcode) zipInput.value = String(rev.address.postcode).trim();
        const formRadio = card.querySelector(`input[name="mode-${type}"][value="form"]`);
        if (formRadio) { formRadio.checked = true; setMode(card, 'form'); }
        showAlert('✅ Đã lấy địa chỉ từ map và điền vào form.', 'success');
    });

    // Kéo marker → cập nhật form + zip
    map.on('layeradd', (e) => {
        if (e.layer === marker && marker) {
            marker.on('dragend', async () => {
                const { lat, lng } = marker.getLatLng();
                const rev = await reverseVN(lat, lng);
                if (!rev) { showAlert('Chỉ hỗ trợ địa chỉ tại Việt Nam.', 'error'); return; }
                fillAddressFromReverse(card, rev.address);
                cardState.get(card).lastRev = rev;
                if (rev.address?.postcode) zipInput.value = String(rev.address.postcode).trim();
            });
        }
    });

    // Dùng vị trí marker → fill form + zip
    card.querySelector('button[data-action="use-marker"]')?.addEventListener('click', async () => {
        if (!cardState.get(card).marker) { showAlert('Chưa có marker trên bản đồ.', 'error'); return; }
        const { lat, lng } = cardState.get(card).marker.getLatLng();
        const rev = await reverseVN(lat, lng);
        if (!rev) { showAlert('Chỉ hỗ trợ địa chỉ tại Việt Nam.', 'error'); return; }
        fillAddressFromReverse(card, rev.address);
        cardState.get(card).lastRev = rev;
        if (rev.address?.postcode) zipInput.value = String(rev.address.postcode).trim();
        showAlert('✅ Đã dùng vị trí marker để điền địa chỉ.', 'success');
    });

    // Gợi ý quận/huyện
    districtInput?.addEventListener('input', debounce(async (e) => {
        const kw = e.target.value;
        const province = card.querySelector('.province').value;
        const list = province ? await suggestDistricts(province, kw) : [];
        if (!list.length) { districtSuggest.style.display = 'none'; districtSuggest.innerHTML = ''; return; }
        districtSuggest.innerHTML = list.map(x => `<div class="suggest-item">${x.name}</div>`).join('');
        districtSuggest.style.display = 'block';
    }, 350));
    districtSuggest?.addEventListener('click', (e) => {
        const item = e.target.closest('.suggest-item'); if (!item) return;
        districtInput.value = item.textContent.trim();
        districtSuggest.style.display = 'none';
        districtSuggest.innerHTML = '';
    });

    setMode(card, 'quick');
}

/* ===================== VALIDATION ===================== */
function validateAddressCard(card) {
    const st = cardState.get(card);
    const marker = st?.marker;
    const mode = st?.mode;
    const province = card.querySelector('.province').value.trim();

    // Bắt buộc có marker để có toạ độ cho tính quãng đường
    if (!marker) { showAlert('❌ Hãy đặt marker trên bản đồ cho địa chỉ này.', 'error'); return false; }
    // Nếu người dùng đang ở mode form, bắt buộc chọn tỉnh/thành (để map đúng vào DB sau này)
    if (mode === 'form' && !province) { showAlert('❌ (Form) Vui lòng chọn Tỉnh/Thành.', 'error'); return false; }
    return true;
}

/* ===================== BOOT ===================== */
document.addEventListener('DOMContentLoaded', () => {
    currentUser = mockLogin();

    // min date
    const d = new Date(); document.getElementById('preferredDate').min = d.toISOString().split('T')[0];

    loadProviders();

    // maps
    const pickupMap   = initLeafletMap('pickupMap');
    const deliveryMap = initLeafletMap('deliveryMap');
    pickupMap.setView(VN_CENTER, 5);
    deliveryMap.setView(VN_CENTER, 5);

    // cards
    setupAddressCard(document.querySelector('#pickupAddresses .address-card'),   pickupMap);
    setupAddressCard(document.querySelector('#deliveryAddresses .address-card'), deliveryMap);

    // items
    document.getElementById('btnAddItem').addEventListener('click', addFurnitureItem);

    // submit
    document.getElementById('requestForm').addEventListener('submit', async (e) => {
        e.preventDefault();

        const preferredDate = document.getElementById('preferredDate').value;
        if (!isFutureOrToday(preferredDate)) {
            showAlert('❌ Ngày vận chuyển phải hôm nay hoặc tương lai.', 'error'); return;
        }

        const pickupCardEl   = document.querySelector('#pickupAddresses .address-card');
        const deliveryCardEl = document.querySelector('#deliveryAddresses .address-card');
        if (!validateAddressCard(pickupCardEl) || !validateAddressCard(deliveryCardEl)) return;

        // Validate items
        const cards = [...document.querySelectorAll('#furnitureItems .item-card')];
        if (cards.length === 0) { showAlert('❌ Vui lòng thêm ít nhất 1 món đồ.', 'error'); return; }
        for (const c of cards) {
            const type = c.querySelector('.item-type')?.value;
            const qty  = parseInt(c.querySelector('.quantity')?.value || '0', 10);
            if (!requiredStr(type)) { showAlert('❌ Mỗi món đồ phải chọn Loại Đồ.', 'error'); return; }
            if (!(qty >= 1)) { showAlert('❌ Số lượng món đồ phải ≥ 1.', 'error'); return; }
        }

        // Gom items
        const furnitureItems = [];
        document.querySelectorAll('#furnitureItems .item-card').forEach(card => {
            furnitureItems.push({
                itemType: card.querySelector('.item-type').value,
                size: card.querySelector('.size')?.value || null,
                quantity: parseInt(card.querySelector('.quantity').value),
                isFragile: card.querySelector('.is-fragile').checked,
                specialHandling: card.querySelector('.special-handling-notes').value || null
            });
        });

        // Gom draft (đưa qua trang tính giá)
        const providerIdStr = document.getElementById('providerId').value;
        const draft = {
            customerId: currentUser.customerId, // để backend dùng
            preferredDate,
            providerId: providerIdStr ? parseInt(providerIdStr, 10) : null,
            vehicleType: "",            // sẽ chọn ở trang distanceCalculation
            hasElevator: false,         // sẽ tick ở trang distanceCalculation
            pickup:   collectAddressForDraft(pickupCardEl),
            delivery: collectAddressForDraft(deliveryCardEl),
            items: furnitureItems
        };

        // Lưu và chuyển trang
        try {
            setSubmitting(true);
            sessionStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
            window.location.href = "/distanceCalculation";
        } finally {
            setSubmitting(false);
        }
    });
});
