// ===== CONFIG =====
const qs = new URLSearchParams(location.search);
const providerId = qs.get('providerId');
if (!providerId) alert("Thiếu providerId trên URL (?providerId=...)");

const API_BASE = `/api/providers/pricing/${providerId}`;

// CSRF (nếu bật)
const CSRF_TOKEN  = document.querySelector('meta[name="_csrf"]')?.content;
const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;

// ===== DOM =====
const packageSelect = document.getElementById('packageSelect');
const pricePerKm    = document.getElementById('pricePerKm');
const itemsBody     = document.getElementById('itemsBody');
const filterInput   = document.getElementById('filterInput');
const saveAllBtn    = document.getElementById('saveAllBtn');
const saveTableBtn  = document.getElementById('saveTableBtn');

// ===== STATE =====
let currentPackageId = null;
let currentItems = []; // [{furnitureItemId, furnitureItemName, price}]

// ===== Helpers =====
function currency(v){ return v?.toLocaleString('vi-VN'); }

async function apiGet(url){
    const res = await fetch(url, {credentials:'same-origin'});
    if(!res.ok) throw new Error(await res.text());
    return res.json();
}
async function apiPut(url, body){
    const headers = {'Content-Type':'application/json'};
    if (CSRF_HEADER && CSRF_TOKEN) headers[CSRF_HEADER] = CSRF_TOKEN;
    const res = await fetch(url, {method:'PUT', headers, credentials:'same-origin', body: JSON.stringify(body)});
    if(!res.ok) throw new Error(await res.text());
    return true;
}
function numberInput(value){
    const el = document.createElement('input');
    el.type='number'; el.min='0'; el.step='1000';
    el.className='form-control';
    if(value!=null) el.value=value;
    el.placeholder='để trống = xoá giá riêng';
    return el;
}
function renderItems(list){
    itemsBody.innerHTML='';
    list.forEach(it=>{
        const tr=document.createElement('tr');
        const td1=document.createElement('td');
        td1.textContent=it.furnitureItemName;
        const td2=document.createElement('td');
        const inp=numberInput(it.price);
        inp.dataset.itemId=it.furnitureItemId;
        td2.appendChild(inp);
        tr.append(td1,td2);
        itemsBody.appendChild(tr);
    });
}
function filtered(){
    const q = filterInput.value.trim().toLowerCase();
    return q? currentItems.filter(x=>x.furnitureItemName.toLowerCase().includes(q)) : currentItems;
}

// ===== Load =====
async function loadPackages(){
    const data = await apiGet(`${API_BASE}/packages`);
    packageSelect.innerHTML='';
    data.forEach(p=>{
        const opt=document.createElement('option');
        opt.value=p.packageId;
        opt.textContent = `${p.packageName}${p.pricePerKm!=null?' • '+currency(p.pricePerKm)+' đ/km':''}`;
        packageSelect.appendChild(opt);
    });
    if(data.length){
        packageSelect.value=data[0].packageId;
        await onPackageChange();
    }
}
async function onPackageChange(){
    currentPackageId = parseInt(packageSelect.value,10);
    const d = await apiGet(`${API_BASE}/packages/${currentPackageId}`);
    pricePerKm.value = d.pricePerKm ?? '';
    currentItems = d.furniturePrices || [];
    renderItems(filtered());
}

// ===== Save =====
function collectPayload(includePerKm=true){
    const items = Array.from(itemsBody.querySelectorAll('input[type="number"]')).map(inp=>{
        const val = inp.value === '' ? null : Number(inp.value);
        return { furnitureItemId: Number(inp.dataset.itemId), furnitureItemName: undefined, price: val };
    });
    return {
        providerId: Number(providerId),
        packageId: Number(currentPackageId),
        pricePerKm: includePerKm ? (pricePerKm.value===''? null : Number(pricePerKm.value)) : undefined,
        furniturePrices: items
    };
}
async function saveAll(){
    const body = collectPayload(true);
    await apiPut(`${API_BASE}/packages/${currentPackageId}`, body);
    alert('Đã lưu cấu hình gói!');
    await loadPackages();
}
async function saveTableOnly(){
    const body = collectPayload(false);
    await apiPut(`${API_BASE}/packages/${currentPackageId}`, body);
    alert('Đã lưu bảng giá đồ!');
}

// ===== Bind =====
packageSelect?.addEventListener('change', onPackageChange);
filterInput?.addEventListener('input', ()=>renderItems(filtered()));
saveAllBtn?.addEventListener('click', ()=>saveAll().catch(e=>alert('Lỗi: '+e.message)));
saveTableBtn?.addEventListener('click', ()=>saveTableOnly().catch(e=>alert('Lỗi: '+e.message)));

loadPackages().catch(e=>alert('Lỗi tải dữ liệu: '+e.message));
