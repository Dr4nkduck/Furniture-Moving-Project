/* ================== CONFIG ================== */
const API_BASE = '';               // same-origin: fetch('/api/...') — đổi nếu cần
const OSRM_PROFILE = 'driving';    // 'driving' mặc định
const basePrices = {
    "Xe bán tải nhỏ": 200000,
    "Xe 3.5 tấn": 300000,
    "Xe 5 tấn": 350000
};

/* ================== STATE ================== */
let map, routingControl;
let selectedService = "";
let aiExtraCost = 0;
let lastDistance = 0, lastTime = 0, totalCost = 0;
let hasElevator = false;
let pickupAlley = 0, deliveryAlley = 0;
let LAST_QUOTE = null;
let LAST_QUOTE_FROM = null;
let LAST_QUOTE_TO   = null;

/* ================== HELPERS ================== */
function csrfHeaders() {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const headerName = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    return (token && headerName) ? { [headerName]: token } : {};
}
function showAlert(msg, type = 'success') {
    const box = document.getElementById('alert');
    if (!box) return;
    box.style.display = 'block';
    box.style.padding = '12px';
    box.style.borderRadius = '8px';
    box.style.marginTop = '12px';
    if (type === 'error') {
        box.style.background = '#fee2e2';
        box.style.color = '#991b1b';
    } else {
        box.style.background = '#dcfce7';
        box.style.color = '#166534';
    }
    box.textContent = msg;
    setTimeout(() => { box.style.display = 'none'; }, 4500);
}
function toVND(n) { return Number(n).toLocaleString('vi-VN') + ' đ'; }
function formatLocalISO(datetimeLocal) { // yyyy-MM-ddTHH:mm
    const d = new Date(datetimeLocal);
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0,16);
}

/* ================== MAP INIT ================== */
function initMap() {
    map = L.map('map').setView([21.0285, 105.8542], 13);
    L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
        attribution: '&copy; OpenStreetMap, &copy; CARTO',
        subdomains: 'abcd',
        maxZoom: 20
    }).addTo(map);
}

/* ================== GEOCODE & ROUTE ================== */
async function getCoords(address) {
    const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(address)}&format=json&limit=1`;
    const res = await fetch(url, { headers: { 'User-Agent': 'ship-app-demo' } });
    const data = await res.json();
    if (!Array.isArray(data) || data.length === 0) throw new Error("Không tìm thấy địa chỉ: " + address);
    return { lat: parseFloat(data[0].lat), lon: parseFloat(data[0].lon), label: data[0].display_name };
}

// Ước tính ngõ hẹp: dựa vào step tên “ngõ/ngách/hẻm” – demo
async function estimateAlleyLengthOSRM(lat, lon) {
    try {
        const url = `https://router.project-osrm.org/route/v1/driving/${lon},${lat};${lon + 0.001},${lat}?overview=full&steps=true`;
        const res = await fetch(url);
        const data = await res.json();
        if (!data.routes || data.routes.length === 0) return 0;

        let alleyLength = 0;
        for (const leg of data.routes[0].legs) {
            for (const step of leg.steps) {
                const name = (step.name || "").toLowerCase();
                const dist = step.distance / 1000;
                if (name.includes("ngõ") || name.includes("ngách") || name.includes("hẻm") || name.includes("hem") ||
                    step.mode !== "driving" || dist < 0.1) {
                    alleyLength += dist;
                }
            }
        }
        return Math.min(alleyLength, 1.5);
    } catch {
        return 0;
    }
}

async function findRoute() {
    const pickup = document.getElementById('pickup').value.trim();
    const destination = document.getElementById('destination').value.trim();
    if (!pickup || !destination) {
        showAlert("Vui lòng nhập đầy đủ địa chỉ!", 'error');
        return;
    }
    if (!document.getElementById('serviceSelect').value) {
        // cho phép không chọn dịch vụ — vẫn hiện quãng đường; nhưng sẽ chưa bật thanh toán
        showAlert("Bạn chưa chọn dịch vụ. Có thể chọn sau để tính giá.");
    }

    try {
        const pickupCoords = await getCoords(pickup);
        const destCoords   = await getCoords(destination);

        pickupAlley   = await estimateAlleyLengthOSRM(pickupCoords.lat, pickupCoords.lon);
        deliveryAlley = await estimateAlleyLengthOSRM(destCoords.lat, destCoords.lon);

        if (routingControl) map.removeControl(routingControl);
        document.getElementById('result').innerHTML = "🔄 Đang tính toán...";
        document.getElementById('payment').innerHTML = "💰 Tổng thanh toán: 0 đ";

        routingControl = L.Routing.control({
            waypoints: [
                L.latLng(pickupCoords.lat, pickupCoords.lon),
                L.latLng(destCoords.lat,   destCoords.lon)
            ],
            routeWhileDragging: false,
            addWaypoints: false,
            show: false
        }).addTo(map);

        const osrmUrl = `https://router.project-osrm.org/route/v1/driving/${pickupCoords.lon},${pickupCoords.lat};${destCoords.lon},${destCoords.lat}?overview=false`;
        const osrmRes = await fetch(osrmUrl);
        const osrmData = await osrmRes.json();
        if (!osrmData.routes || osrmData.routes.length === 0) throw new Error("Không tính được tuyến đường");

        const route = osrmData.routes[0];
        lastDistance = route.distance / 1000; // km
        lastTime = route.duration / 60;       // phút

        LAST_QUOTE_FROM = { lat: pickupCoords.lat, lng: pickupCoords.lon, label: pickupCoords.label };
        LAST_QUOTE_TO   = { lat: destCoords.lat,   lng: destCoords.lon,   label: destCoords.label };

        updateResult();
        showAlert('Đã tính toán lộ trình.');
    } catch (err) {
        showAlert("Có lỗi: " + (err.message || 'Không xác định'), 'error');
    }
}

/* ================== PRICING ================== */
function calculatePrice(distance, service) {
    if (!service || !basePrices[service]) return 0;

    if (distance <= 10) {
        if (distance <= 4) return basePrices[service];
        return basePrices[service] + (distance - 4) * 50000;
    }
    switch (service) {
        case "Xe bán tải nhỏ":
            return 800000 + (distance - 10) * 35000;
        case "Xe 3.5 tấn":
            return 1000000 + (distance - 10) * 50000;
        case "Xe 5 tấn":
            return 1200000 + (distance - 10) * 60000;
        default:
            return 1000000 + (distance - 10) * 50000;
    }
}
function calcSmallTruckFee(km) {
    if (km <= 0) return 0;
    if (km < 1) return 100000;
    return 100000 + (km - 1) * 20000;
}

function updateService() {
    selectedService = document.getElementById('serviceSelect').value;
    updateResult();
}
function updateResult() {
    if (lastDistance === 0 || lastTime === 0) return;

    const pickupTimeVal = document.getElementById('pickupTime').value;
    hasElevator = !!document.getElementById('hasElevator').checked;

    let baseCost = calculatePrice(lastDistance, selectedService);

    const pickupFee = calcSmallTruckFee(pickupAlley);
    const deliveryFee = calcSmallTruckFee(deliveryAlley);
    const totalSmallTruck = pickupFee + deliveryFee;

    let adjustedAiCost = aiExtraCost;
    if (aiExtraCost > 0 && hasElevator) adjustedAiCost = Math.round(aiExtraCost * 0.8);

    totalCost = baseCost + totalSmallTruck + adjustedAiCost;

    let priceNote = "";
    if (selectedService) {
        if (lastDistance <= 10)
            priceNote = `🚕 Cước nội thành: ${toVND(basePrices[selectedService] || 0)} (4km đầu), +50.000đ/km sau.`;
        else
            priceNote = `🚛 Ngoại thành: ${toVND(calculatePrice(10, selectedService))} (10km đầu) + đơn giá/km sau.`;
    }

    document.getElementById('result').innerHTML = `
📍 Quãng đường: ${lastDistance.toFixed(2)} km<br>
⏱️ Thời gian ước tính: ${lastTime.toFixed(1)} phút<br>
🚚 Dịch vụ: ${selectedService || "Chưa chọn"}<br>
🗓️ Ngày giờ vận chuyển: ${pickupTimeVal || "Chưa chọn"}<br>
🚧 Phát hiện ngõ hẹp:<br>
- Điểm lấy hàng: ${pickupAlley.toFixed(2)} km<br>
- Điểm giao hàng: ${deliveryAlley.toFixed(2)} km<br>
🚙 Phí xe nhỏ trung chuyển: ${toVND(totalSmallTruck)} (chi tiết: ${toVND(pickupFee)} + ${toVND(deliveryFee)})<br>
💰 Cước chính: ${toVND(baseCost)}<br>
${aiExtraCost > 0 ? `🧱 Phí bốc vác (AI): ${toVND(adjustedAiCost)}<br>` : ""}
🏢 Có thang máy: ${hasElevator ? "Có (giảm 20%)" : "Không"}<br>
${priceNote ? `<hr>${priceNote}` : ""}
<hr><b>Tổng chi phí tạm tính: ${toVND(totalCost)}</b>
`;
    document.getElementById('payment').textContent = `Tổng thanh toán: ${toVND(totalCost)}`;

    // Lưu quote cho confirm
    LAST_QUOTE = {
        serviceType: selectedService || null,
        distanceKm: Number(lastDistance.toFixed(2)),
        durationSec: Math.round(lastTime * 60),
        quotedPrice: Math.round(totalCost),
        hasElevator: hasElevator
    };

    // Bật/tắt nút confirm
    const canConfirm = !!(LAST_QUOTE && LAST_QUOTE.serviceType && document.getElementById('pickupTime').value);
    const btn = document.getElementById('btnConfirm');
    if (btn) btn.disabled = !canConfirm;
}

function analyzeImage() {
    const file = document.getElementById('uploadImage').files[0];
    if (!file) return;
    aiExtraCost = Math.floor(Math.random() * 3) * 50000; // demo
    document.getElementById('aiNote').textContent = `AI ước lượng chi phí bốc vác: ${aiExtraCost.toLocaleString('vi-VN')}đ`;
    updateResult();
}

/* ================== BACKEND INTEGRATION (OPTION: xác nhận thanh toán) ================== */
function getCurrentCustomerId(){ return 1; } // TODO: render thật từ session
async function createAddressAPI({ userId, addressType, addressLine, coord }) {
    const payload = {
        userId,
        addressType, // "Pickup" | "Delivery"
        streetAddress: addressLine,
        city: "", state: "", zipCode: "",
        latitude: coord?.lat ?? null,
        longitude: coord?.lng ?? null,
        isDefault: false
    };
    const res = await fetch(`${API_BASE}/api/addresses`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', ...csrfHeaders() },
        body: JSON.stringify(payload)
    });
    const text = await res.text();
    let json = null; try { json = JSON.parse(text); } catch {}
    if (!res.ok) throw new Error((json && (json.message||json.error)) || text || 'Tạo địa chỉ thất bại');
    const id =
        (json && json.data && (json.data.addressId ?? json.data.id)) ??
        (json && (json.addressId ?? json.id)) ??
        (Number.isFinite(+text) ? +text : null);
    if (!id) throw new Error('Không đọc được addressId từ response');
    return id;
}
async function createRequestAPI(dto) {
    const res = await fetch(`${API_BASE}/api/requests`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', ...csrfHeaders() },
        body: JSON.stringify(dto)
    });
    const text = await res.text();
    let json = null; try { json = JSON.parse(text); } catch {}
    if (!res.ok) throw new Error((json && (json.message||json.error)) || text || 'Tạo yêu cầu thất bại');
    return json;
}
async function confirmPayment() {
    if (!LAST_QUOTE || !LAST_QUOTE.serviceType) {
        showAlert('Vui lòng chọn dịch vụ và tính báo giá trước.', 'error'); return;
    }
    const pickupStr = document.getElementById('pickup').value.trim();
    const destStr   = document.getElementById('destination').value.trim();
    const whenStr   = document.getElementById('pickupTime').value;
    if (!pickupStr || !destStr || !whenStr) {
        showAlert('Thiếu địa chỉ hoặc thời gian.', 'error'); return;
    }

    const customerId = getCurrentCustomerId();
    try {
        const [pickupAddressId, deliveryAddressId] = await Promise.all([
            createAddressAPI({ userId: customerId, addressType: 'Pickup',   addressLine: pickupStr, coord: LAST_QUOTE_FROM }),
            createAddressAPI({ userId: customerId, addressType: 'Delivery', addressLine: destStr,   coord: LAST_QUOTE_TO })
        ]);

        const dto = {
            customerId,
            providerId: null,
            pickupAddressId,
            deliveryAddressId,
            preferredDate: formatLocalISO(whenStr),
            status: 'PENDING',
            serviceType: LAST_QUOTE.serviceType,
            distanceKm: LAST_QUOTE.distanceKm,
            durationSec: LAST_QUOTE.durationSec,
            quotedPrice: LAST_QUOTE.quotedPrice,
            hasElevator: LAST_QUOTE.hasElevator
        };

        const result = await createRequestAPI(dto);
        const reqId = (result && (result.requestId || (result.data && result.data.requestId))) || '(không rõ)';
        showAlert(`✅ Đặt yêu cầu thành công! Mã: #${reqId}. Trạng thái: PENDING`);
        document.getElementById('btnConfirm').disabled = true;
    } catch (err) {
        showAlert(err.message || 'Không thể tạo yêu cầu', 'error');
    }
}

/* ================== INIT (NO INLINE JS) ================== */
function wireEvents() {
    // submit form → gọi findRoute
    const form = document.getElementById('addressForm');
    form?.addEventListener('submit', (e) => { e.preventDefault(); findRoute(); });

    document.getElementById('serviceSelect')?.addEventListener('change', updateService);
    document.getElementById('hasElevator')?.addEventListener('change', () => { hasElevator = !!document.getElementById('hasElevator').checked; updateResult(); });
    document.getElementById('uploadImage')?.addEventListener('change', analyzeImage);
    document.getElementById('btnConfirm')?.addEventListener('click', confirmPayment);
}

function initMinDatetime() {
    const el = document.getElementById('pickupTime');
    if (!el) return;
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    el.min = now.toISOString().slice(0,16);
}

/* Đọc query & tự fill + tự chạy findRoute */
function initFromURL(){
    const q = new URLSearchParams(window.location.search);
    const pickup      = q.get('pickup') || '';
    const destination = q.get('destination') || '';
    const date        = q.get('date') || '';
    const service     = q.get('service') || '';
    const elevator    = q.get('elevator') === '1';

    if (pickup)      document.getElementById('pickup').value = pickup;
    if (destination) document.getElementById('destination').value = destination;

    if (date) {
        // nếu chỉ có yyyy-MM-dd → đặt giờ mặc định 08:00
        const val = date.length === 10 ? `${date}T08:00` : date;
        const dt = document.getElementById('pickupTime');
        if (dt) dt.value = val;
    }

    if (service) {
        const sel = document.getElementById('serviceSelect');
        if (sel) { sel.value = service; selectedService = service; }
    }

    const chk = document.getElementById('hasElevator');
    if (chk) chk.checked = !!elevator;

    if (pickup && destination) {
        findRoute();
    }
}

/* Boot */
(function boot(){
    initMap();
    wireEvents();
    initMinDatetime();
    initFromURL();
})();
// --- THÊM CUỐI FILE script.js ---
document.getElementById('btnConfirm').addEventListener('click', async () => {
    try {
        const pickup = document.getElementById('pickup').value.trim();
        const destination = document.getElementById('destination').value.trim();
        const service = document.getElementById('serviceSelect').value;
        const hasElevator = document.getElementById('hasElevator').checked;
        const pickupTime = document.getElementById('pickupTime').value; // yyyy-MM-ddTHH:mm
        const aiNote = (document.getElementById('aiNote').innerText || '').trim();

        if (!pickup || !destination || !service || !pickupTime) {
            alert('Vui lòng nhập đủ địa chỉ, chọn dịch vụ và thời gian.');
            return;
        }

        // Lấy customer_id (bạn có thể render sẵn vào #authArea data-customer-id)
        const el = document.getElementById('authArea');
        const cid = el?.getAttribute('data-customer-id');
        const customerId = cid ? parseInt(cid, 10) : 1; // demo fallback: 1

        // Tách DATE cho cột preferred_date (DB là DATE)
        const preferredDate = pickupTime.substring(0, 10);

        // totalCost: dùng biến tính của bạn; nếu chưa có, thay bằng con số bạn đã hiện ở UI
        // Ở đây ví dụ lấy từ text "Tổng thanh toán: 1.234.567 đ"
        const payText = document.getElementById('payment').innerText;
        const totalCost = Number(payText.replace(/\D/g, '')) || 0;

        const payload = {
            customerId: customerId,
            pickupAddress: {
                streetAddress: pickup,
                city: "Unknown",
                state: "VN",
                zipCode: "00000"
            },
            deliveryAddress: {
                streetAddress: destination,
                city: "Unknown",
                state: "VN",
                zipCode: "00000"
            },
            serviceType: service,
            hasElevator: hasElevator,
            preferredDate: preferredDate,
            totalCost: totalCost,
            furnitureItems: [
                {
                    itemType: "Unknown (from form)",
                    size: null,
                    quantity: 1,
                    fragile: false,
                    specialHandling: aiNote || null
                }
            ]
        };

        const res = await fetch('/api/requests/checkout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const msg = await res.text();
            throw new Error(msg || ('HTTP ' + res.status));
        }

        const data = await res.json();
        alert(`Đã tạo yêu cầu #${data.requestId} (trạng thái: ${data.status}).`);
    } catch (e) {
        console.error(e);
        alert('Không tạo được yêu cầu: ' + e.message);
    }
});
