(function () {
    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;

    const pickup = document.getElementById('pickup');
    const dropoff = document.getElementById('dropoff');
    const scheduledAt = document.getElementById('scheduledAt');
    const route = document.getElementById('route');
    const total = document.getElementById('total');
    const cname = document.getElementById('cname');
    const cphone = document.getElementById('cphone');
    const cemail = document.getElementById('cemail');
    const eta = document.getElementById('eta');

    function load() {
        fetch(`/provider/api/orders/${ORDER_ID}`, { headers: { [header]: token } })
            .then(r => r.json()).then(d => {
            pickup.textContent = d.pickup;
            dropoff.textContent = d.dropoff;
            scheduledAt.textContent = d.scheduledAt?.replace('T',' ') ?? '';
            route.textContent = `${d.distanceKm ?? ''} km / ${d.durationMin ?? ''} min`;
            total.textContent = d.totalPrice ?? '';
            cname.textContent = d.customerNameMasked ?? '';
            cphone.textContent = d.customerPhoneMasked ?? '';
            cemail.textContent = d.customerEmailMasked ?? '';
            eta.textContent = d.etaText ?? '';
        });
    }

    document.body.addEventListener('click', (e) => {
        const to = e.target.getAttribute('data-to');
        if (!to) return;
        const params = new URLSearchParams();
        params.append('to', to);
        const etaVal = document.getElementById('fEta').value.trim();
        const noteVal = document.getElementById('fNote').value.trim();
        if (etaVal) params.append('eta', etaVal);
        if (noteVal) params.append('note', noteVal);

        fetch(`/provider/api/orders/${ORDER_ID}/status`, {
            method: 'PATCH',
            headers: { [header]: token, 'Content-Type': 'application/x-www-form-urlencoded' },
            body: params.toString()
        }).then(() => load());
    });

    load();
})();
