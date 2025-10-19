// ====== CẤU HÌNH ======
const API_BASE = 'http://localhost:8080'; // đổi nếu backend khác cổng

let itemCount = 1;
let currentUser = null;

// ==== MOCK LOGIN (demo) ====
function mockLogin() {
  const key = 'currentUser';
  const exists = localStorage.getItem(key);
  if (exists) return JSON.parse(exists);
  const mock = { userId: 2, customerId: 1, username: 'test_customer' };
  localStorage.setItem(key, JSON.stringify(mock));
  return mock;
}

// ==== UI helpers ====
function showAlert(message, type) {
  const box = document.getElementById('alertBox');
  box.textContent = message;
  box.className = `alert ${type === 'success' ? 'alert--success' : 'alert--error'}`;
  box.style.display = 'block';
  setTimeout(() => { box.style.display = 'none'; }, 5000);
  box.scrollIntoView({ behavior: 'smooth', block: 'start' });
}
function setSubmitting(isSubmitting){
  const btn = document.getElementById('btnSubmit');
  btn.disabled = isSubmitting;
  btn.textContent = isSubmitting ? '⏳ Đang gửi...' : '📤 Gửi Yêu Cầu';
}

// ==== Providers ====
async function loadProviders(){
  try{
    const res = await fetch(`${API_BASE}/api/providers`);
    const text = await res.text();
    let result;
    try{ result = JSON.parse(text); }catch{ result = null; }

    const select = document.getElementById('providerId');
    select.innerHTML = '<option value="">-- Chọn sau --</option>';

    const list = Array.isArray(result) ? result :
      (result && Array.isArray(result.data)) ? result.data : [];

    list.forEach(p=>{
      const opt = document.createElement('option');
      const id = p.providerId ?? p.id ?? p.provider_id;
      const name = p.companyName ?? p.name ?? p.company_name ?? 'Nhà cung cấp';
      const rating = (p.rating != null) ? ` ⭐ ${p.rating}` : '';
      opt.value = id;
      opt.textContent = `${name}${rating}`;
      select.appendChild(opt);
    });
  }catch(e){ console.error('Load providers error:', e); }
}

// ==== Default pickup address ====
async function loadDefaultPickupAddress(){
  try{
    if(!currentUser?.customerId) return;
    const res = await fetch(`${API_BASE}/api/addresses?userId=${currentUser.customerId}`);
    if(!res.ok) return;
    const text = await res.text();
    let json; try{ json = JSON.parse(text); }catch{ json = null; }
    const list = Array.isArray(json) ? json :
      (json && Array.isArray(json.data)) ? json.data : [];
    if(!list.length) return;

    const pick = list.find(a => a.isDefault) ||
                 list.find(a => (a.addressType ?? a.address_type) === 'home') ||
                 list[0];
    const pickupCard = document.querySelector('#pickupAddresses .address-card');
    pickupCard.querySelector('.street-address').value = pick.streetAddress ?? pick.street_address ?? '';
    pickupCard.querySelector('.city').value           = pick.city ?? '';
    pickupCard.querySelector('.state').value          = pick.state ?? pick.district ?? '';
  }catch(e){ console.warn('Không thể nạp địa chỉ mặc định:', e); }
}

// ==== Add item card ====
function addFurnitureItem(){
  itemCount++;
  const container = document.getElementById('furnitureItems');
  const el = document.createElement('div');
  el.className = 'item-card';
  el.innerHTML = `
    <div class="row">
      <div class="form-group">
        <label>Loại Đồ <span class="required">*</span></label>
        <select class="item-type" required>
          <option value="">-- Chọn loại --</option>
          <option value="Sofa">Sofa</option>
          <option value="Bed">Giường</option>
          <option value="Table">Bàn</option>
          <option value="Chair">Ghế</option>
          <option value="Cabinet">Tủ</option>
          <option value="Wardrobe">Tủ quần áo</option>
          <option value="Other">Khác</option>
        </select>
      </div>
      <div class="form-group">
        <label>Tên Món Đồ</label>
        <input type="text" class="item-name" placeholder="Ví dụ: Sofa 3 chỗ">
      </div>
    </div>
    <div class="row">
      <div class="form-group">
        <label>Số Lượng <span class="required">*</span></label>
        <input type="number" class="quantity" min="1" value="1" required>
      </div>
      <div class="form-group">
        <label>Kích Thước</label>
        <select class="size">
          <option value="">-- Chọn --</option>
          <option value="Small" data-dimensions="40x40x40">Nhỏ (40×40×40)</option>
          <option value="Medium" data-dimensions="80x60x60">Trung bình (80×60×60)</option>
          <option value="Large" data-dimensions="120x80x80">Lớn (120×80×80)</option>
          <option value="Extra Large" data-dimensions="160x100x100">Rất lớn (160×100×100)</option>
        </select>
        <div class="size-hint">Số đo: <span></span> cm</div>
      </div>
    </div>
    <div class="checkbox-group">
      <input type="checkbox" class="is-fragile" id="fragile${itemCount}">
      <label for="fragile${itemCount}">Đồ dễ vỡ</label>
    </div>
    <div class="form-group">
      <label>Ghi Chú Đặc Biệt</label>
      <textarea class="special-handling-notes" placeholder="Ví dụ: Cần bọc cẩn thận..."></textarea>
    </div>
    <div>
      <button type="button" class="btn btn--primary" data-remove>✖ Xóa món đồ</button>
    </div>
  `;
  container.appendChild(el);
  wireSizeHints(el);
  el.querySelector('[data-remove]').addEventListener('click', () => el.remove());
}

// ==== Size hint ====
function wireSizeHints(root=document){
  root.querySelectorAll('.item-card').forEach(card=>{
    const select = card.querySelector('.size');
    const hint   = card.querySelector('.size-hint');
    const span   = hint?.querySelector('span');
    if(!select || !hint || !span) return;
    const update = ()=>{
      const dims = select.selectedOptions[0]?.getAttribute('data-dimensions');
      if(dims){ span.textContent = dims; hint.style.display = 'block'; }
      else { span.textContent=''; hint.style.display='none'; }
    };
    select.removeEventListener('change', update);
    select.addEventListener('change', update);
    update();
  });
}

// ==== Validation ====
function isFutureOrToday(dateStr){
  if(!dateStr) return false;
  const d = new Date(dateStr + 'T00:00:00');
  const today = new Date(); today.setHours(0,0,0,0);
  return d.getTime() >= today.getTime();
}
function validateForm(){
  const preferredDate = document.getElementById('preferredDate').value;
  if(!isFutureOrToday(preferredDate)){
    showAlert('❌ Ngày vận chuyển phải hôm nay hoặc tương lai.', 'error'); return false;
  }
  const pickupCard = document.querySelector('#pickupAddresses .address-card');
  const deliveryCard = document.querySelector('#deliveryAddresses .address-card');
  if(!pickupCard || !deliveryCard){
    showAlert('❌ Thiếu địa chỉ lấy hàng hoặc giao hàng.', 'error'); return false;
  }
  const req = s => s && s.trim().length>0;
  const pickup = {
    street: pickupCard.querySelector('.street-address')?.value,
    city: pickupCard.querySelector('.city')?.value,
    state: pickupCard.querySelector('.state')?.value
  };
  const delivery = {
    street: deliveryCard.querySelector('.street-address')?.value,
    city: deliveryCard.querySelector('.city')?.value,
    state: deliveryCard.querySelector('.state')?.value
  };
  if(![pickup.street,pickup.city,pickup.state].every(req)){
    showAlert('❌ Vui lòng nhập đủ địa chỉ LẤY HÀNG (đường, thành phố, quận/huyện).', 'error'); return false;
  }
  if(![delivery.street,delivery.city,delivery.state].every(req)){
    showAlert('❌ Vui lòng nhập đủ địa chỉ GIAO HÀNG (đường, thành phố, quận/huyện).', 'error'); return false;
  }
  const items = [...document.querySelectorAll('#furnitureItems .item-card')];
  if(items.length===0){ showAlert('❌ Vui lòng thêm ít nhất 1 món đồ.', 'error'); return false; }
  for(const c of items){
    const type = c.querySelector('.item-type')?.value;
    const qty = parseInt(c.querySelector('.quantity')?.value || '0', 10);
    if(!req(type)){ showAlert('❌ Mỗi món đồ phải chọn Loại Đồ.', 'error'); return false; }
    if(!(qty>=1)){ showAlert('❌ Số lượng món đồ phải ≥ 1.', 'error'); return false; }
  }
  return true;
}

// ==== API helpers ====
async function createAddress(addr, type){
  const payload = {
    userId: currentUser.customerId,
    addressType: (type === 'pickup') ? 'home' : 'office',
    streetAddress: addr.streetAddress,
    city: addr.city,
    state: addr.state,
    zipCode: null, latitude: null, longitude: null, isDefault: false
  };
  const res = await fetch(`${API_BASE}/api/addresses`, {
    method:'POST', headers:{'Content-Type':'application/json'},
    body: JSON.stringify(payload)
  });
  const result = await res.json();
  if(!res.ok || !result.success || !result.data?.addressId){
    throw new Error(result.message || 'Create address failed');
  }
  return result.data.addressId;
}

// ==== Submit ====
async function handleSubmit(e){
  e.preventDefault();
  if(!validateForm()) return;

  setSubmitting(true);
  const pickupCard = document.querySelector('#pickupAddresses .address-card');
  const deliveryCard = document.querySelector('#deliveryAddresses .address-card');

  const pickupAddress = {
    streetAddress: pickupCard.querySelector('.street-address').value,
    city: pickupCard.querySelector('.city').value,
    state: pickupCard.querySelector('.state').value
  };
  const deliveryAddress = {
    streetAddress: deliveryCard.querySelector('.street-address').value,
    city: deliveryCard.querySelector('.city').value,
    state: deliveryCard.querySelector('.state').value
  };

  const furnitureItems = [];
  document.querySelectorAll('#furnitureItems .item-card').forEach(card=>{
    const sizeEl = card.querySelector('.size');
    const sizeVal = sizeEl?.value || null;
    const dims = sizeEl?.selectedOptions?.[0]?.getAttribute('data-dimensions') || null;
    furnitureItems.push({
      itemType: card.querySelector('.item-type').value,
      size: sizeVal,
      sizeDimensions: dims,
      quantity: parseInt(card.querySelector('.quantity').value),
      isFragile: card.querySelector('.is-fragile').checked,
      specialHandling: card.querySelector('.special-handling-notes').value || null
    });
  });

  try{
    const [pickupAddressId, deliveryAddressId] = await Promise.all([
      createAddress(pickupAddress, 'pickup'),
      createAddress(deliveryAddress, 'delivery')
    ]);

    const providerIdStr = document.getElementById('providerId').value;
    const payload = {
      customerId: currentUser.customerId,
      providerId: providerIdStr ? parseInt(providerIdStr) : null,
      pickupAddressId, deliveryAddressId,
      preferredDate: document.getElementById('preferredDate').value,
      status: 'pending',
      furnitureItems
    };

    const res = await fetch(`${API_BASE}/api/requests`, {
      method:'POST', headers:{'Content-Type':'application/json'},
      body: JSON.stringify(payload)
    });
    const result = await res.json();
    if(res.ok && result.success){
      showAlert(`✅ Yêu cầu tạo thành công! Mã: #${result.data.requestId}`, 'success');
      setTimeout(()=>{
        document.getElementById('requestForm').reset();
        document.querySelectorAll('#furnitureItems .item-card').forEach((c,i)=>{ if(i>0) c.remove(); });
        itemCount = 1;
        wireSizeHints();
      }, 800);
    }else{
      throw new Error(result.message || 'Có lỗi khi tạo yêu cầu');
    }
  }catch(err){
    console.error(err);
    showAlert(`❌ ${err.message || 'Không thể tạo địa chỉ/yêu cầu. Kiểm tra backend!'}`, 'error');
  }finally{
    setSubmitting(false);
  }
}

// ==== INIT ====
(function init(){
  currentUser = mockLogin(); // thay bằng /api/me nếu có session
  const d = new Date(); document.getElementById('preferredDate').min = d.toISOString().split('T')[0];
  document.getElementById('btnAddItem').addEventListener('click', addFurnitureItem);
  document.getElementById('requestForm').addEventListener('submit', handleSubmit);
  loadProviders();
  loadDefaultPickupAddress();
  wireSizeHints(); // card #1
})();
