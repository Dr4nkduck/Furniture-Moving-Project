/* ========= Helpers ========= */
const $  = (sel, ctx = document) => ctx.querySelector(sel);
const $$ = (sel, ctx = document) => Array.from(ctx.querySelectorAll(sel));

/* ========= Toast helpers ========= */
function ensureToastContainer() {
  if (!document.getElementById('toastContainer')) {
    const c = document.createElement('div');
    c.id = 'toastContainer';
    c.className = 'toast-container';
    document.body.appendChild(c);
  }
}
function notify(msg, type = 'success', ms = 2500) {
  ensureToastContainer();
  const n = document.createElement('div');
  n.className = `toast ${type}`;
  n.textContent = msg;
  document.getElementById('toastContainer').appendChild(n);
  requestAnimationFrame(() => n.classList.add('show'));
  setTimeout(() => {
    n.classList.remove('show');
    n.addEventListener('transitionend', () => n.remove(), { once: true });
  }, ms);
}

/* ========= Providers: load once on page load ========= */
let providerMarkupSnapshot = null;

async function loadProviders() {
  const sel = document.getElementById('providerId');
  if (!sel) return;
  try {
    const res = await fetch('/api/providers'); // mặc định ?status=verified
    if (!res.ok) throw new Error('HTTP ' + res.status);
    const payload = await res.json();
    const list = payload.data || [];

    sel.innerHTML = '<option value="">— Hệ thống tự gợi ý —</option>';
    list.forEach(p => {
      const opt = document.createElement('option');
      opt.value = p.providerId;
      opt.textContent = p.companyName;
      sel.appendChild(opt);
    });
    providerMarkupSnapshot = sel.innerHTML;
  } catch (e) {
    sel.innerHTML = '<option value="">— Không tải được danh sách —</option>';
    console.error('Load providers failed:', e);
  }
}
document.addEventListener('DOMContentLoaded', loadProviders);

/* ========= Images preview (multi + remove) ========= */
const fileInput = $('#images');
const thumbs = $('#thumbs');

let selectedFiles = []; // giữ danh sách ảnh đã chọn
const fileKey = (f) => `${f.name}::${f.size}::${f.lastModified}`;

function renderThumbs() {
  if (!thumbs) return;
  // revoke URL cũ để tránh leak
  thumbs.querySelectorAll('img[data-url]').forEach(img => {
    try { URL.revokeObjectURL(img.dataset.url); } catch {}
  });
  thumbs.innerHTML = '';

  // chỉ preview tối đa 12 ảnh cho nhẹ, vẫn upload đủ
  const toShow = selectedFiles.slice(0, 12);
  for (const f of toShow) {
    const url = URL.createObjectURL(f);
    const div = document.createElement('div');
    div.className = 'thumb';

    const img = document.createElement('img');
    img.src = url;
    img.alt = f.name;
    img.dataset.url = url;

    const rm = document.createElement('button');
    rm.type = 'button';
    rm.className = 'rm';
    rm.setAttribute('aria-label', 'Xoá ảnh');
    rm.textContent = '×';
    rm.addEventListener('click', () => {
      // xoá file khỏi danh sách và render lại
      selectedFiles = selectedFiles.filter(x => fileKey(x) !== fileKey(f));
      try { URL.revokeObjectURL(url); } catch {}
      renderThumbs();
      const sumImgs = $('#sumImgs'); if (sumImgs) sumImgs.textContent = String(selectedFiles.length);
    });

    div.appendChild(img);
    div.appendChild(rm);
    thumbs.appendChild(div);
  }

  const sumImgs = $('#sumImgs');
  if (sumImgs) sumImgs.textContent = String(selectedFiles.length);
}

function addFiles(files) {
  // gộp + loại trùng (theo name+size+lastModified)
  const map = new Map(selectedFiles.map(x => [fileKey(x), x]));
  for (const f of files) map.set(fileKey(f), f);
  selectedFiles = Array.from(map.values());
  renderThumbs();
}

fileInput?.addEventListener('change', (e) => {
  const files = Array.from(e.target.files || []);
  if (!files.length) return;
  addFiles(files);
  // clear để lần sau có thể chọn lại cùng tên
  fileInput.value = '';
});

/* ========= Items table ========= */
const itemsBody = $('#itemsBody');
const addItemBtn = $('#addItem');
const clearItemsBtn = $('#clearItems');

function addItemRow(data = {}) {
  if (!itemsBody) return;
  const tr = document.createElement('tr');
  tr.className = 'item-row';
  tr.innerHTML = `
    <td><input type="text"  name="items.name" placeholder="VD: Tủ quần áo" required /></td>
    <td><input type="number" name="items.qty"  min="1" value="1" required style="width:80px"></td>
    <td><input type="number" name="items.len"  min="0" placeholder="0" style="width:90px"></td>
    <td><input type="number" name="items.wid"  min="0" placeholder="0" style="width:90px"></td>
    <td><input type="number" name="items.hgt"  min="0" placeholder="0" style="width:90px"></td>
    <td><input type="number" name="items.wgt"  min="0" placeholder="0" style="width:90px"></td>
    <td style="text-align:center"><input type="checkbox" name="items.fragile"></td>
    <td style="text-align:right"><button class="btn" type="button" aria-label="Xoá">Xoá</button></td>
  `;

  // gán data nếu có
  const inputs = $$('input', tr);
  inputs.forEach(inp => {
    const key = inp.name.split('.').pop();
    if (data[key] != null) {
      if (inp.type === 'checkbox') inp.checked = !!data[key];
      else inp.value = data[key];
    }
  });

  tr.querySelector('button')?.addEventListener('click', () => {
    tr.remove();
    updateSummary();
  });

  itemsBody.appendChild(tr);
  updateSummary();
}

addItemBtn?.addEventListener('click', () => addItemRow());
clearItemsBtn?.addEventListener('click', () => {
  if (!itemsBody) return;
  itemsBody.innerHTML = '';
  updateSummary();
});

/* ========= Summary & estimate ========= */

// hiển thị tiền
function money(n){ return n ? n.toLocaleString('vi-VN') + '₫' : '—'; }

// tách riêng 2 loại chi phí: vận chuyển đồ (nhân công, không thang máy, số món…)
function calcMovingEstimate() {
  const base = 200_000;     // VND
  const perItem = 50_000;   // VND / món
  const items = $$('#itemsBody tr');
  let total = base + perItem * items.length;

  const pickupFloor   = Number($('#pickupFloor')?.value || 0);
  const dropFloor     = Number($('#dropFloor')?.value || 0);
  const pickupElevEl  = $('#pickupElevator');
  const dropElevEl    = $('#dropElevator');
  const noElevPickup  = !(pickupElevEl?.checked) && pickupFloor > 0;
  const noElevDrop    = !(dropElevEl?.checked)   && dropFloor > 0;

  if (noElevPickup) total += pickupFloor * 20_000;
  if (noElevDrop)   total += dropFloor   * 20_000;

  return total;
}

// lấy khoảng cách do AI tính nếu có
function getDistanceFromAIQuote() {
  try {
    const raw = JSON.parse(sessionStorage.getItem('aiquote_draft') || 'null');
    if (!raw) return null;
    const candidates = [
      Number(raw?.route?.distanceKm),
      Number(raw?.distanceKm),
      Number(raw?.meta?.distanceKm)
    ].filter(v => Number.isFinite(v) && v > 0);
    return candidates.length ? candidates[0] : null;
  } catch { return null; }
}

// Ước tính phí ship theo quãng đường (ưu tiên số km từ AI)
function calcShippingEstimate() {
  const aiKm = getDistanceFromAIQuote();
  let km = aiKm;

  if (!Number.isFinite(km)) {
    const pc = ($('#pickupCity')?.value || '').trim().toLowerCase();
    const dc = ($('#dropCity')?.value || '').trim().toLowerCase();
    if (pc && dc) km = (pc === dc) ? 10 : 120; // ước lượng nội thành vs liên tỉnh
    else km = 10;
  }

  const perKm = 12_000; // đơn giá
  const fee = Math.max(0, Math.round(km) * perKm);
  return { fee, km, perKm };
}

function updateSummary() {
  const pkgEl   = $('#servicePackage');
  const dateEl  = $('#preferredDate');
  const sumPkg   = $('#sumPkg');
  const sumItems = $('#sumItems');
  const sumDate  = $('#sumDate');
  const sumMove  = $('#sumMoveCost');
  const sumShip  = $('#sumShipCost');
  const sumShipMeta = $('#sumShipMeta');
  const sumTotal = $('#sumCost');

  if (sumPkg)   sumPkg.textContent   = pkgEl?.selectedOptions?.[0]?.textContent || '—';
  if (sumItems) sumItems.textContent = `${$('#itemsBody')?.querySelectorAll('tr').length || 0} món`;
  if (sumDate)  sumDate.textContent  = dateEl?.value || '—';

  const move = calcMovingEstimate();
  const { fee: ship, km, perKm } = calcShippingEstimate();

  if (sumMove) sumMove.textContent = money(move);
  if (sumShip) sumShip.textContent = money(ship);
  if (sumShipMeta) sumShipMeta.textContent = `(~${km} km × ${perKm.toLocaleString('vi-VN')}đ/km)`;

  if (sumTotal) sumTotal.textContent = money(move + ship);
}
['change','keyup','input'].forEach(ev => document.addEventListener(ev, updateSummary));

/* ========= Save draft (localStorage) ========= */
const draftKey = 'reqDraft_v1';
$('#saveDraft')?.addEventListener('click', () => {
  const data = serialize();
  localStorage.setItem(draftKey, JSON.stringify(data));
  notify('Đã lưu nháp trên trình duyệt.', 'success');
});

/* =======================
   Prefill từ ai-quote (sessionStorage)
   ======================= */

// chỉ điền khi input đang trống (tránh ghi đè nếu user đã chỉnh)
function prefillIfEmpty(el, val) {
  if (!el) return;
  const v = String(val ?? '').trim();
  if (!v) return;
  if (!el.value || el.value.trim() === '') {
    el.value = v;
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
  }
}
function firstNonEmpty(...vals) {
  for (const v of vals) {
    const s = (v == null) ? '' : String(v).trim();
    if (s) return s;
  }
  return '';
}
function toIsoDateFromVN(ddmmyyyy) {
  if (!ddmmyyyy) return '';
  const m = String(ddmmyyyy).match(/(\d{1,2})[\/\-](\d{1,2})[\/\-](\d{2,4})/);
  if (!m) return '';
  let d = +m[1], mo = +m[2], y = +m[3];
  if (y < 100) y += 2000;
  return `${String(y)}-${String(mo).padStart(2,'0')}-${String(d).padStart(2,'0')}`;
}
function toTimeHHMM(s) {
  if (!s) return '';
  let t = String(s).trim().toLowerCase().replace(/\s+/g,' ');
  t = t.replace(/giờ/g,'h');
  let m;
  m = t.match(/(\d{1,2})\s*h\s*kém\s*(\d{1,2})/i);
  if (m) {
    let h = +m[1], minus = +m[2];
    let mm = (60 - minus) % 60;
    h = (h - 1 + 24) % 24;
    return `${String(h).padStart(2,'0')}:${String(mm).padStart(2,'0')}`;
  }
  m = t.match(/(\d{1,2})[:\.](\d{1,2})/);
  if (m) {
    const h = +m[1], mm = +m[2];
    if (h>23||mm>59) return '';
    return `${String(h).padStart(2,'0')}:${String(mm).padStart(2,'0')}`;
  }
  m = t.match(/(\d{1,2})\s*h\s*rưỡi/);
  if (m) {
    const h = +m[1];
    if (h>23) return '';
    return `${String(h).padStart(2,'0')}:30`;
  }
  m = t.match(/(\d{1,2})\s*h(?:\s*(sáng|trưa|chiều|tối|pm|am))?/i);
  if (m) {
    let h = +m[1], desc = (m[2]||'').toLowerCase();
    if (desc === 'chiều' || desc === 'tối' || desc === 'pm') { if (h < 12) h += 12; }
    else if (desc === 'sáng' || desc === 'am') { if (h === 12) h = 0; }
    else if (desc === 'trưa') { if (h < 10) h += 12; }
    return `${String(h%24).padStart(2,'0')}:00`;
  }
  m = t.match(/^(\d{1,2})$/);
  if (m) {
    const h = +m[1];
    if (h>23) return '';
    return `${String(h).padStart(2,'0')}:00`;
  }
  return '';
}

function normalizeAiquoteDraft(raw) {
  if (!raw || typeof raw !== 'object') return null;

  const name  = firstNonEmpty(raw?.customer?.name, raw?.customerName);
  const phone = firstNonEmpty(raw?.customer?.phone, raw?.phone);

  const pickup = raw.pickup || raw.from || {};
  const drop   = raw.dropoff || raw.to   || {};

  const pickupLine = firstNonEmpty(pickup.formatted, pickup.raw);
  const dropLine   = firstNonEmpty(drop.formatted, drop.raw);

  const pickupParts = pickup.parts || raw.fromParts || {};
  const dropParts   = drop.parts   || raw.toParts   || {};

  const pickupDistrict = firstNonEmpty(pickupParts.district, pickup.district);
  const pickupCity     = firstNonEmpty(pickupParts.city, pickupParts.province, pickup.city);
  const dropDistrict   = firstNonEmpty(dropParts.district, drop.district);
  const dropCity       = firstNonEmpty(dropParts.city, dropParts.province, drop.city);

  const dateVN = firstNonEmpty(raw?.schedule?.date, raw?.date);
  const timeAny = firstNonEmpty(raw?.schedule?.time, raw?.time);

  // items: [{ name, qty, len, wid, hgt, wgt, fragile }]
  const items = Array.isArray(raw.items) ? raw.items : [];

  // distance
  const distanceKm = getDistanceFromAIQuote();

  return {
    name, phone,
    pickupLine, pickupDistrict, pickupCity,
    dropLine, dropDistrict, dropCity,
    dateVN, timeAny,
    items,
    distanceKm
  };
}

function prefillFromAiquoteDraft() {
  let draft = null;
  try {
    draft = JSON.parse(sessionStorage.getItem('aiquote_draft') || 'null');
  } catch(_) {}

  if (!draft) return;

  const d = normalizeAiquoteDraft(draft);
  if (!d) return;

  // Lấy hàng
  prefillIfEmpty($('#pickupLine1'),  d.pickupLine);
  prefillIfEmpty($('#pickupDistrict'), d.pickupDistrict);
  prefillIfEmpty($('#pickupCity'),     d.pickupCity);
  prefillIfEmpty($('#pickupContact'),  d.name);
  prefillIfEmpty($('#pickupPhone'),    d.phone);

  // Giao hàng
  prefillIfEmpty($('#dropLine1'),   d.dropLine);
  prefillIfEmpty($('#dropDistrict'), d.dropDistrict);
  prefillIfEmpty($('#dropCity'),     d.dropCity);
  prefillIfEmpty($('#dropContact'),  d.name);
  prefillIfEmpty($('#dropPhone'),    d.phone);

  // Lịch hẹn
  const isoDate = toIsoDateFromVN(d.dateVN);
  const hhmm    = toTimeHHMM(d.timeAny);
  prefillIfEmpty($('#preferredDate'), isoDate);
  prefillIfEmpty($('#preferredTime'), hhmm);

  // Summary nhanh
  const sumDate = $('#sumDate');
  if (sumDate && isoDate) {
    const [y, m, dd] = isoDate.split('-');
    sumDate.textContent = `${dd}/${m}/${y}`;
  }

  // ✅ TỰ FILL đồ đạc từ AI nếu bảng đang trống
  if (d.items.length && itemsBody && !itemsBody.children.length) {
    d.items.forEach(it => addItemRow({
      name: it.name ?? it.item ?? '',
      qty:  Math.max(1, Number(it.qty || it.quantity) || 1),
      len:  Number(it.len || it.lengthCm || 0),
      wid:  Number(it.wid || it.widthCm  || 0),
      hgt:  Number(it.hgt || it.heightCm || 0),
      wgt:  Number(it.wgt || it.weightKg || 0),
      fragile: !!it.fragile
    }));
  }

  // sau khi prefill, cập nhật lại tổng
  updateSummary();
}

/* ========= Load draft/bridge + init ========= */
document.addEventListener('DOMContentLoaded', async () => {
  await loadProviders(); // nạp providers trước để có option

  // Prefill từ ai-quote (ưu tiên ngay sau khi vào trang)
  prefillFromAiquoteDraft();

  // === Hydrate từ AI Quote (bridge qua localStorage) ===
  const bridgeKey = 'aiquote_payload_v1';
  let bridged = null;
  try { bridged = JSON.parse(localStorage.getItem(bridgeKey) || 'null'); } catch(_) {}

  if (bridged && Array.isArray(bridged.items) && bridged.items.length && itemsBody) {
    itemsBody.innerHTML = '';
    bridged.items.forEach(it => {
      addItemRow({ name: it.name, qty: Math.max(1, Number(it.qty)||1) });
    });
    localStorage.removeItem(bridgeKey);
    updateSummary();
  } else {
    // === Không có bridge → thử nháp request riêng
    const raw = localStorage.getItem(draftKey);
    if (!raw) {
      addItemRow({ name: 'Bàn làm việc', qty: 1 });
      updateSummary();
    } else {
      try {
        const data = JSON.parse(raw);
        if (data.servicePackageCode) $('#servicePackage').value = data.servicePackageCode;
        if (data.notes) $('#notes').value = data.notes;

        if (data.pickup) {
          for (const k in data.pickup) {
            const el = document.querySelector(`[name="pickup.${k}"]`);
            if (el) { if (el.type === 'checkbox') el.checked = !!data.pickup[k]; else el.value = data.pickup[k]; }
          }
        }
        if (data.drop) {
          for (const k in data.drop) {
            const el = document.querySelector(`[name="drop.${k}"]`);
            if (el) { if (el.type === 'checkbox') el.checked = !!data.drop[k]; else el.value = data.drop[k]; }
          }
        }
        if (Array.isArray(data.items) && itemsBody) {
          itemsBody.innerHTML = '';
          data.items.forEach(it => addItemRow(it));
        }
        if (data.preferredDate) $('#preferredDate').value = data.preferredDate;
        if (data.preferredTime) $('#preferredTime').value = data.preferredTime;

        // Nếu nháp có providerId, set sau khi đã loadProviders xong
        if (data.providerId && providerMarkupSnapshot) {
          const sel = $('#providerId');
          if (sel) sel.value = String(data.providerId);
        }
        updateSummary();
      } catch (_) {
        addItemRow({ name: 'Bàn làm việc', qty: 1 });
        updateSummary();
      }
    }
  }
});

/* ========= Serialize to JSON cho API ========= */
function serialize(){
  const fd = new FormData($('#reqForm'));
  const items = [];
  $$('#itemsBody tr').forEach(tr => {
    const obj = {};
    $$('input', tr).forEach(inp => {
      const key = inp.name.split('.').pop();
      obj[key] = (inp.type === 'checkbox') ? inp.checked : (inp.value || '');
    });
    items.push(obj);
  });

  return {
    servicePackageCode: fd.get('servicePackageCode') || '',
    providerId: fd.get('providerId') || null,
    notes: fd.get('notes') || '',
    pickup: {
      addressLine1: fd.get('pickup.addressLine1') || '',
      district:     fd.get('pickup.district')     || '',
      city:         fd.get('pickup.city')         || '',
      floor: Number(fd.get('pickup.floor') || 0),
      hasElevator: !!fd.get('pickup.hasElevator'),
      contactName:  fd.get('pickup.contactName')  || '',
      contactPhone: fd.get('pickup.contactPhone') || ''
    },
    drop: {
      addressLine1: fd.get('drop.addressLine1') || '',
      district:     fd.get('drop.district')     || '',
      city:         fd.get('drop.city')         || '',
      floor: Number(fd.get('drop.floor') || 0),
      hasElevator: !!fd.get('drop.hasElevator'),
      contactName:  fd.get('drop.contactName')  || '',
      contactPhone: fd.get('drop.contactPhone') || ''
    },
    items,
    preferredDate: fd.get('preferredDate') || '',
    preferredTime: fd.get('preferredTime') || '',
    // tổng tạm tính (để hiển thị; BE vẫn nên tự tính lại)
    estimate: (function(){
      const move = calcMovingEstimate();
      const { fee: ship } = calcShippingEstimate();
      return move + ship;
    })()
  };
}

/* ===== CSRF helper (tuỳ chọn, chỉ thêm header nếu có token trong DOM) ===== */
function getCsrfHeaders() {
  const meta = document.querySelector('meta[name="_csrf"]');
  const input = document.querySelector('input[name="_csrf"]');
  const token = meta?.content || input?.value;
  return token ? { 'X-CSRF-TOKEN': token } : {};
}

/* ===== Submit/reset helpers ===== */
const form = $('#reqForm');
const submitBtn = form?.querySelector('[type="submit"]');

function setSubmitting(isOn){
  if (!submitBtn) return;
  submitBtn.disabled = isOn;
  submitBtn.dataset._origText ??= submitBtn.textContent;
  submitBtn.textContent = isOn ? 'Đang gửi…' : submitBtn.dataset._origText;
}
function clearDraft(){ localStorage.removeItem(draftKey); }

function resetFormUI(){
  form?.reset();
  if (itemsBody) {
    itemsBody.innerHTML = '';
    addItemRow({ name: 'Bàn làm việc', qty: 1 });
  }
  if (fileInput) fileInput.value = '';
  if (thumbs) thumbs.innerHTML = '';
  selectedFiles = []; // <<< đảm bảo xoá danh sách ảnh đã chọn
  const sumImgs = $('#sumImgs');
  if (sumImgs) sumImgs.textContent = '0';

  // Dùng lại providers đã nạp (không re-fetch)
  const providerSel = $('#providerId');
  if (providerSel && providerMarkupSnapshot) {
    providerSel.innerHTML = providerMarkupSnapshot;
    providerSel.selectedIndex = 0; // “— Không chọn —”
  }
  updateSummary();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function buildFullRequestPayload(viewPayload){
  return {
    pickupAddress: {
      addressLine1: viewPayload.pickup.addressLine1,
      district:     viewPayload.pickup.district,
      city:         viewPayload.pickup.city,
      floor:        Number(viewPayload.pickup.floor || 0),
      hasElevator:  !!viewPayload.pickup.hasElevator,
      contactName:  viewPayload.pickup.contactName,
      contactPhone: viewPayload.pickup.contactPhone
    },
    deliveryAddress: {
      addressLine1: viewPayload.drop.addressLine1,
      district:     viewPayload.drop.district,
      city:         viewPayload.drop.city,
      floor:        Number(viewPayload.drop.floor || 0),
      hasElevator:  !!viewPayload.drop.hasElevator,
      contactName:  viewPayload.drop.contactName,
      contactPhone: viewPayload.drop.contactPhone
    },
    request: {
      customerId: Number($('#customerId')?.value || 1), // thay bằng session nếu có
      providerId: ($('#providerId')?.value ? Number($('#providerId').value) : null),
      preferredDate: viewPayload.preferredDate,
      notes: viewPayload.notes || ''
    },
    furnitureItems: (viewPayload.items || []).map(it => ({
      name: it.name,
      quantity: Number(it.qty || 1),
      lengthCm: Number(it.len || 0),
      widthCm:  Number(it.wid || 0),
      heightCm: Number(it.hgt || 0),
      weightKg: Number(it.wgt || 0),
      fragile: !!it.fragile
    }))
  };
}

/* ===== Upload images API ===== */
async function uploadImagesForRequest(requestId) {
  if (!selectedFiles?.length) return { ok: true, saved: 0 };

  const fd = new FormData();
  selectedFiles.forEach(f => fd.append('images', f)); // name="images" khớp BE

  const res = await fetch(`/api/requests/${requestId}/images`, {
    method: 'POST',
    headers: { ...getCsrfHeaders() }, // KHÔNG set Content-Type khi dùng FormData
    body: fd
  });

  let json = null;
  try { json = await res.json(); } catch(_) {}

  if (!res.ok || !json?.success) {
    const msg = (json && json.message) ? json.message : `${res.status} ${res.statusText}`;
    throw new Error(`Upload ảnh thất bại: ${msg}`);
  }
  return { ok: true, saved: json.data?.saved ?? 0 };
}

/* ========= Submit handler ========= */
$('#reqForm')?.addEventListener('submit', async (e) => {
  e.preventDefault();

  const payload = serialize();
  const body = buildFullRequestPayload(payload);

  // Pre-check tối thiểu
  const errs = [];
  if (!body.request.preferredDate) errs.push('Chọn ngày dự kiến (preferredDate)');
  ['addressLine1','district','city'].forEach(k => {
    if (!body.pickupAddress[k])   errs.push(`Pickup: thiếu ${k}`);
    if (!body.deliveryAddress[k]) errs.push(`Delivery: thiếu ${k}`);
  });
  if (!Array.isArray(body.furnitureItems) || body.furnitureItems.length === 0) {
    errs.push('Thêm ít nhất 1 món đồ.');
  }
  if (errs.length) {
    alert('Không hợp lệ:\n- ' + errs.join('\n- '));
    notify('Vui lòng điền đủ các trường bắt buộc.', 'error', 4000);
    focusByField(preferField(errs));
    return;
  }

  try {
    setSubmitting(true);
    const res = await fetch('/api/requests/full', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getCsrfHeaders() },
      body: JSON.stringify(body)
    });

    let json = null;
    try { json = await res.json(); } catch(_) {}

    if (res.ok && json?.success) {
      const id = json.data.requestId;

      // 1) Thử upload ảnh (nếu có)
      try {
        const up = await uploadImagesForRequest(id);
        if (up.saved > 0) {
          notify(`Đã tạo đơn #${id} và upload ${up.saved} ảnh ✔`, 'success', 3500);
        } else {
          notify(`Đã tạo đơn #${id}. (Không có ảnh để upload)`, 'success', 3000);
        }
      } catch (e) {
        alert(e.message);
        notify('Upload ảnh bị lỗi. Bạn có thể thử lại trong trang chi tiết đơn.', 'error', 4000);
      }

      // 2) Reset UI
      clearDraft();
      resetFormUI();
      return;
    }

    let msg = (json && json.message)
      ? `Tạo đơn thất bại: ${json.message}`
      : `Tạo đơn thất bại: ${res.status} ${res.statusText}`;
    if (json?.errors?.length) {
      const lines = json.errors.map(e => `• ${e.field}: ${e.message}`);
      msg += '\n' + lines.join('\n');
      focusByField(json.errors[0]?.field);
    }
    alert(msg);
    notify('Vui lòng kiểm tra các trường bị lỗi.', 'error', 4000);
  } catch (err) {
    alert('Không gọi được API: ' + err.message);
    notify('Lỗi mạng hoặc BE không phản hồi.', 'error', 4000);
  } finally {
    setSubmitting(false);
  }
});

/* ===== Focus input theo tên field BE trả về ===== */
function focusByField(field){
  if (!field) return;
  const map = {
    'request.customerId': '#customerId',
    'request.providerId': '#providerId',
    'request.preferredDate': '#preferredDate',
    'pickupAddress.addressLine1': '[name="pickup.addressLine1"]',
    'pickupAddress.district':     '[name="pickup.district"]',
    'pickupAddress.city':         '[name="pickup.city"]',
    'deliveryAddress.addressLine1':'[name="drop.addressLine1"]',
    'deliveryAddress.district':    '[name="drop.district"]',
    'deliveryAddress.city':        '[name="drop.city"]',
    'furnitureItems[0].name':      'input[name="items.name"]'
  };
  const sel = map[field];
  if (sel) {
    const el = document.querySelector(sel);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el.focus();
      el.classList.add('error-blink');
      setTimeout(() => el.classList.remove('error-blink'), 1200);
    }
  }
}

// Chọn field gợi ý để focus khi chỉ có pre-check FE
function preferField(errs){
  if (!errs?.length) return null;
  if (errs.some(x => x.includes('preferredDate'))) return 'request.preferredDate';
  if (errs.some(x => x.includes('Pickup: thiếu addressLine1'))) return 'pickupAddress.addressLine1';
  if (errs.some(x => x.includes('Delivery: thiếu addressLine1'))) return 'deliveryAddress.addressLine1';
  if (errs.some(x => x.includes('Pickup: thiếu district'))) return 'pickupAddress.district';
  if (errs.some(x => x.includes('Delivery: thiếu district'))) return 'deliveryAddress.district';
  if (errs.some(x => x.includes('Pickup: thiếu city'))) return 'pickupAddress.city';
  if (errs.some(x => x.includes('Delivery: thiếu city'))) return 'deliveryAddress.city';
  return null;
}
