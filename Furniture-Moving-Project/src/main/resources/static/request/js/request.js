// ========= Helpers =========
const $  = (sel, ctx = document) => ctx.querySelector(sel);
const $$ = (sel, ctx = document) => Array.from(ctx.querySelectorAll(sel));

// ========= Toast helpers =========
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

// ========= Providers: load once on page load =========
let providerMarkupSnapshot = null;

async function loadProviders() {
  const sel = document.getElementById('providerId');
  if (!sel) return;

  sel.innerHTML = '<option value="">— Đang tải nhà cung cấp… —</option>';

  try {
    const res = await fetch('/api/providers/available');
    if (!res.ok) throw new Error(res.status + ' ' + res.statusText);
    const json = await res.json();
    const arr  = Array.isArray(json) ? json : (json.data || []);

    sel.innerHTML = '<option value="">— Không chọn —</option>';
    arr.forEach(p => {
      const opt = document.createElement('option');
      opt.value = p.providerId;
      const rating = (p.rating != null) ? ` · ⭐ ${p.rating}` : '';
      opt.textContent = `${p.companyName}${rating}`;
      sel.appendChild(opt);
    });

    // Lưu snapshot để dùng lại khi reset (không cần re-fetch)
    providerMarkupSnapshot = sel.innerHTML;
  } catch (err) {
    console.error('loadProviders error:', err);
    sel.innerHTML = '<option value="">— Không tải được danh sách —</option>';
    providerMarkupSnapshot = sel.innerHTML;
  }
}

// ========= Images preview (multi + remove) =========
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


// ========= Items table =========
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
  // preset theo data
  $$('input', tr).forEach(inp => {
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

// ========= Summary & estimate =========
function calcEstimate() {
  const base = 200_000;     // VND
  const perItem = 50_000;   // VND
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
function money(n){ return n ? n.toLocaleString('vi-VN') + '₫' : '—'; }

function updateSummary() {
  const pkgEl   = $('#servicePackage');
  const dateEl  = $('#preferredDate');
  const sumPkg   = $('#sumPkg');
  const sumItems = $('#sumItems');
  const sumDate  = $('#sumDate');
  const sumCost  = $('#sumCost');

  if (sumPkg)   sumPkg.textContent   = pkgEl?.selectedOptions?.[0]?.textContent || '—';
  if (sumItems) sumItems.textContent = `${$('#itemsBody')?.querySelectorAll('tr').length || 0} món`;
  if (sumDate)  sumDate.textContent  = dateEl?.value || '—';
  if (sumCost)  sumCost.textContent  = money(calcEstimate());
}
['change','keyup'].forEach(ev => document.addEventListener(ev, updateSummary));

// ========= Save draft (localStorage) =========
const draftKey = 'reqDraft_v1';
$('#saveDraft')?.addEventListener('click', () => {
  const data = serialize();
  localStorage.setItem(draftKey, JSON.stringify(data));
  notify('Đã lưu nháp trên trình duyệt.', 'success');
});

// ========= Load draft if any + init =========
document.addEventListener('DOMContentLoaded', async () => {
  await loadProviders(); // nạp providers trước để có option

  const raw = localStorage.getItem(draftKey);
  if (!raw) {
    addItemRow({ name: 'Bàn làm việc', qty: 1 });
    updateSummary();
    return;
  }
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
  } catch (_) { /* ignore */ }
  updateSummary();
});

// ========= Serialize to JSON cho API =========
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
    estimate: calcEstimate()
  };
}

// ===== Submit/reset helpers =====
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

// ========= Submit handler =========
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
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });

    let json = null;
    try { json = await res.json(); } catch(_) {}

    if (res.ok && json?.success) {
      notify(`Đã tạo đơn #${json.data.requestId} thành công!`, 'success', 3000);
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

// ===== Focus input theo tên field BE trả về
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
