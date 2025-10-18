(function () {
    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;

    const listRfp = document.getElementById('listRfp');
    const listAssigned = document.getElementById('listAssigned');
    const listInProgress = document.getElementById('listInProgress');

    function li(order) {
        const a = document.createElement('a');
        a.href = `/provider/orders/${order.id}`;
        a.textContent = `#${order.id} · ${order.pickup} → ${order.dropoff} · ${order.routeSummary}`;
        a.className = 'pv-list-item';
        return a;
    }

    function load(statuses, container) {
        fetch('/provider/api/orders?statuses=' + statuses, { headers: { [header]: token } })
            .then(r => r.json()).then(list => {
            container.innerHTML = '';
            list.forEach(o => container.appendChild(li(o)));
        });
    }

    function refresh() {
        load('PENDING_OFFER', listRfp);
        load('ASSIGNED', listAssigned);
        load('IN_PROGRESS', listInProgress);
    }

    refresh();
})();
