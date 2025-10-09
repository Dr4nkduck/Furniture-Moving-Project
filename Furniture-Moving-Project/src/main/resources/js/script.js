let map = L.map('map').setView([21.0285, 105.8542], 13);

L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; OpenStreetMap, &copy; CARTO',
    subdomains: 'abcd',
    maxZoom: 20
}).addTo(map);

let routingControl;
let selectedService = "";
let aiExtraCost = 0;
let lastDistance = 0, lastTime = 0, totalCost = 0;
let hasElevator = false;
let pickupAlley = 0, deliveryAlley = 0;

const basePrices = {
    "Xe bán tải nhỏ": 200000,
    "Xe 3.5 tấn": 300000,
    "Xe 5 tấn": 350000
};

// 🧠 Ước tính ngõ hẹp dựa theo bản đồ OSRM (đường đứt)
async function estimateAlleyLengthOSRM(lat, lon) {
    try {
        // tạo một đoạn nhỏ 100m gần điểm để kiểm tra loại đường
        const url = `https://router.project-osrm.org/route/v1/driving/${lon},${lat};${lon + 0.001},${lat}?overview=full&steps=true`;
        const res = await fetch(url);
        const data = await res.json();

        if (!data.routes || data.routes.length === 0) return 0;
        const route = data.routes[0];
        let alleyLength = 0;

        for (const leg of route.legs) {
            for (const step of leg.steps) {
                const name = (step.name || "").toLowerCase();
                const dist = step.distance / 1000;
                // nếu là đường nhỏ hoặc có ngõ/hẻm
                if (name.includes("ngõ") || name.includes("ngách") || name.includes("hẻm") || name.includes("hem") ||
                    step.mode !== "driving" || dist < 0.1) {
                    alleyLength += dist;
                }
            }
        }

        return Math.min(alleyLength, 1.5); // giới hạn 1.5km
    } catch (e) {
        console.error("Lỗi OSRM ngõ:", e);
        return 0;
    }
}

async function getCoords(address) {
    const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(address)}&format=json&limit=1`;
    const res = await fetch(url, { headers: { 'User-Agent': 'ship-app-demo' } });
    const data = await res.json();
    if (data.length === 0) throw new Error("Không tìm thấy địa chỉ: " + address);
    return { lat: parseFloat(data[0].lat), lon: parseFloat(data[0].lon) };
}

async function findRoute() {
    const pickup = document.getElementById('pickup').value;
    const destination = document.getElementById('destination').value;
    if (!pickup || !destination) {
        alert("Vui lòng nhập đầy đủ địa chỉ!");
        return;
    }

    try {
        const pickupCoords = await getCoords(pickup);
        const destCoords = await getCoords(destination);

        // Ước lượng ngõ hẹp thật theo bản đồ
        pickupAlley = await estimateAlleyLengthOSRM(pickupCoords.lat, pickupCoords.lon);
        deliveryAlley = await estimateAlleyLengthOSRM(destCoords.lat, destCoords.lon);

        if (routingControl) map.removeControl(routingControl);
        document.getElementById('result').innerHTML = "🔄 Đang tính toán...";
        document.getElementById('payment').innerHTML = "💰 Tổng thanh toán: 0 đ";

        routingControl = L.Routing.control({
            waypoints: [
                L.latLng(pickupCoords.lat, pickupCoords.lon),
                L.latLng(destCoords.lat, destCoords.lon)
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
        lastDistance = route.distance / 1000;
        lastTime = route.duration / 60;

        updateResult();
    } catch (err) {
        console.error(err);
        alert("Có lỗi: " + err.message);
    }
}

function updateService() {
    selectedService = document.getElementById('serviceSelect').value;
    updateResult();
}

function toggleElevator(has) {
    hasElevator = has;
    updateResult();
}

function analyzeImage() {
    const file = document.getElementById('uploadImage').files[0];
    if (!file) return;
    aiExtraCost = Math.floor(Math.random() * 3) * 50000;
    document.getElementById('aiNote').textContent = `AI ước lượng chi phí bốc vác: ${aiExtraCost.toLocaleString('vi-VN')}đ`;
    updateResult();
}

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

function getPickupTime() {
    return document.getElementById('pickupTime').value;
}

function calcSmallTruckFee(km) {
    if (km <= 0) return 0;
    if (km < 1) return 100000;
    return 100000 + (km - 1) * 20000;
}

function updateResult() {
    if (lastDistance === 0 || lastTime === 0) return;
    const pickupTime = getPickupTime();
    let baseCost = calculatePrice(lastDistance, selectedService);

    // 🚙 Tính phí xe nhỏ 2 bên
    const pickupFee = calcSmallTruckFee(pickupAlley);
    const deliveryFee = calcSmallTruckFee(deliveryAlley);
    const totalSmallTruck = pickupFee + deliveryFee;

    let adjustedAiCost = aiExtraCost;
    if (aiExtraCost > 0 && hasElevator) adjustedAiCost = aiExtraCost * 0.8;

    totalCost = baseCost + totalSmallTruck + adjustedAiCost;

    let priceNote = "";
    if (lastDistance <= 10)
        priceNote = `🚕 Cước nội thành: ${basePrices[selectedService]?.toLocaleString('vi-VN')}đ (4km đầu), +50.000đ/km sau.`;
    else
        priceNote = `🚛 Ngoại thành: ${calculatePrice(10, selectedService).toLocaleString('vi-VN')}đ (10km đầu) + đơn giá/km sau.`;

    document.getElementById('result').innerHTML = `
📍 Quãng đường: ${lastDistance.toFixed(2)} km<br>
⏱️ Thời gian ước tính: ${lastTime.toFixed(1)} phút<br>
🚚 Dịch vụ: ${selectedService || "Chưa chọn"}<br>
🗓️ Ngày giờ vận chuyển: ${pickupTime || "Chưa chọn"}<br>
🚧 Phát hiện ngõ hẹp:<br>
- Điểm lấy hàng: ${pickupAlley.toFixed(2)} km (xe lớn không vào được)<br>
- Điểm giao hàng: ${deliveryAlley.toFixed(2)} km (xe lớn không vào được)<br>
🚙 Phí xe nhỏ trung chuyển:<br>
- Bên lấy hàng: ${pickupFee.toLocaleString('vi-VN')} đ<br>
- Bên giao hàng: ${deliveryFee.toLocaleString('vi-VN')} đ<br>
👉 Tổng phí trung chuyển: <b>${totalSmallTruck.toLocaleString('vi-VN')} đ</b><br>
💰 Cước chính: ${baseCost.toLocaleString('vi-VN')} đ<br>
${aiExtraCost > 0 ? `🧱 Phí bốc vác (AI): ${adjustedAiCost.toLocaleString('vi-VN')} đ<br>` : ""}
🏢 Có thang máy: ${hasElevator ? "Có (giảm 20%)" : "Không"}<br>
<hr>${priceNote}<br>
🔹 <b>Tổng chi phí tạm tính: ${totalCost.toLocaleString('vi-VN')} đ</b>
`;

    document.getElementById('payment').innerHTML =
        `💰 Tổng thanh toán: ${totalCost.toLocaleString('vi-VN')} đ`;
}
