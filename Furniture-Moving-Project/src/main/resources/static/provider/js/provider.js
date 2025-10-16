const $ = (s) => document.querySelector(s);
const $$ = (s) => Array.from(document.querySelectorAll(s));

document.addEventListener('DOMContentLoaded', () => {
    // tabs
    $$('#header nav button, nav button').forEach(btn=>{
        btn.addEventListener('click', () => {
            $$('.tab').forEach(t => t.classList.remove('active'));
            $('#tab-' + btn.dataset.tab).classList.add('active');
        });
    });

    // Catalog actions
    $('#btn-new-item')?.addEventListener('click', async () => {
        const payload = {
            id:null, name: prompt('Name?'), description:'',
            baseFee: prompt('Base fee?') || 0, perKm: 0, perMin: 0,
            stairsFee:0, noElevatorFee:0, narrowAlleyFee:0, weekendFee:0, active:true
        };
        const r = await fetch('/provider/api/catalog', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(payload)});
        if (r.ok) location.reload();
    });

    $$('#catalog-table .del').forEach(b=>{
        b.addEventListener('click', async ()=>{
            if(!confirm('Delete item?')) return;
            const id = b.dataset.id;
            const r = await fetch(`/provider/api/catalog/${id}`, {method:'DELETE'});
            if (r.ok) location.reload();
        })
    });

    // Orders
    $('#reload-orders')?.addEventListener('click', loadOrders);
    $('#order-state')?.addEventListener('change', loadOrders);
    $$('#orders-tbody .view').forEach(b => b.addEventListener('click', ()=> showDetail(b.dataset.id)));

    async function loadOrders(){
        const state = $('#order-state').value;
        const r = await fetch(`/provider/api/orders?state=${state}`);
        const data = await r.json();
        const tbody = $('#orders-tbody');
        tbody.innerHTML = '';
        data.forEach(o=>{
            const tr = document.createElement('tr');
            tr.innerHTML = `
        <td>${o.id}</td>
        <td>${o.status}</td>
        <td>${o.pickupAddress} → ${o.dropoffAddress}</td>
        <td>${o.scheduledAt ?? ''}</td>
        <td>${o.quotedPrice ?? ''}</td>
        <td><button class="view" data-id="${o.id}">View</button></td>`;
            tbody.appendChild(tr);
            tr.querySelector('.view').addEventListener('click', ()=> showDetail(o.id));
        });
    }

    async function showDetail(id){
        const r = await fetch(`/provider/api/orders/${id}`);
        const o = await r.json();
        $('#order-detail').hidden = false;
        $('#od-id').textContent = o.id;
        $('#od-status').textContent = o.status;
        $('#od-pickup').textContent = o.pickupAddress;
        $('#od-dropoff').textContent = o.dropoffAddress;
        $('#od-scheduled').textContent = o.scheduledAt ?? '';
        $('#od-cust-name').textContent = o.customerDisplayName;
        $('#od-cust-phone').textContent = o.customerPhoneMasked;
        $('#od-cust-email').textContent = o.customerEmailMasked;
        $('#od-notes').textContent = o.providerNotes ?? '';
        $('#od-eta').textContent = o.etaArrival ?? '';
        $('#od-image').src = o.inventoryImageUrl || '';
        attachActions(id);
    }

    function attachActions(id){
        $$('#od-actions button').forEach(btn=>{
            btn.onclick = async ()=>{
                const action = btn.dataset.act;
                let body = { action };
                if (action === 'STATUS') body.toStatus = btn.dataset.to;
                if (action === 'ETA') {
                    const v = prompt('ETA (ISO 8601, ví dụ 2025-10-20T14:00:00Z)?');
                    if (!v) return; body.etaArrival = v;
                }
                if (action === 'NOTE') {
                    const v = prompt('Note?'); if (!v) return; body.note = v;
                }
                const r = await fetch(`/provider/api/orders/${id}/act`, {
                    method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)
                });
                if (r.ok) showDetail(id); else alert('Action failed');
            }
        })
    }
});
