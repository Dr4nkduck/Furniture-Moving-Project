(function () {
    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;

    const tbody = document.querySelector('#tblItems tbody');
    const btnAdd = document.getElementById('btnAdd');
    const modal = document.getElementById('itemModal');
    const modalTitle = document.getElementById('modalTitle');
    const btnCancel = document.getElementById('btnCancel');
    const btnSave = document.getElementById('btnSave');

    let editingId = null;

    function openModal(title, data) {
        modalTitle.textContent = title;
        editingId = data?.id ?? null;
        document.getElementById('fName').value = data?.name ?? '';
        document.getElementById('fBase').value = data?.baseFee ?? 0;
        document.getElementById('fKm').value = data?.perKm ?? 0;
        document.getElementById('fMin').value = data?.perMin ?? 0;
        document.getElementById('fStairs').value = data?.surchargeStairs ?? 0;
        document.getElementById('fNoElev').value = data?.surchargeNoElevator ?? 0;
        document.getElementById('fAlley').value = data?.surchargeNarrowAlley ?? 0;
        document.getElementById('fWeekend').value = data?.surchargeWeekend ?? 0;
        modal.hidden = false;
    }

    function closeModal() { modal.hidden = true; }

    function row(d) {
        const tr = document.createElement('tr');
        tr.innerHTML = `
      <td>${d.name}</td>
      <td>${d.baseFee}</td>
      <td>${d.perKm}</td>
      <td>${d.perMin}</td>
      <td>${d.surchargeStairs}</td>
      <td>${d.surchargeNoElevator}</td>
      <td>${d.surchargeNarrowAlley}</td>
      <td>${d.surchargeWeekend}</td>
      <td class="pv-actions">
        <button class="pv-link" data-edit="${d.id}">Edit</button>
        <button class="pv-link pv-danger" data-del="${d.id}">Delete</button>
      </td>`;
        return tr;
    }

    function refresh() {
        fetch('/provider/api/services', { headers: { [header]: token } })
            .then(r => r.json()).then(list => {
            tbody.innerHTML = '';
            list.forEach(d => tbody.appendChild(row(d)));
        });
    }

    btnAdd.addEventListener('click', () => openModal('New Item'));

    btnCancel.addEventListener('click', closeModal);

    btnSave.addEventListener('click', () => {
        const dto = {
            name: document.getElementById('fName').value.trim(),
            baseFee: +document.getElementById('fBase').value || 0,
            perKm: +document.getElementById('fKm').value || 0,
            perMin: +document.getElementById('fMin').value || 0,
            surchargeStairs: +document.getElementById('fStairs').value || 0,
            surchargeNoElevator: +document.getElementById('fNoElev').value || 0,
            surchargeNarrowAlley: +document.getElementById('fAlley').value || 0,
            surchargeWeekend: +document.getElementById('fWeekend').value || 0
        };
        const method = editingId ? 'PUT' : 'POST';
        const url = editingId ? `/provider/api/services/${editingId}` : '/provider/api/services';
        fetch(url, {
            method, headers: { 'Content-Type':'application/json', [header]: token },
            body: JSON.stringify(dto)
        }).then(() => { closeModal(); refresh(); });
    });

    tbody.addEventListener('click', (e) => {
        const idEdit = e.target.getAttribute('data-edit');
        const idDel = e.target.getAttribute('data-del');
        if (idEdit) {
            // load current row values from table (simpler than calling API again)
            const tr = e.target.closest('tr').children;
            openModal('Edit Item', {
                id: +idEdit,
                name: tr[0].textContent,
                baseFee: tr[1].textContent,
                perKm: tr[2].textContent,
                perMin: tr[3].textContent,
                surchargeStairs: tr[4].textContent,
                surchargeNoElevator: tr[5].textContent,
                surchargeNarrowAlley: tr[6].textContent,
                surchargeWeekend: tr[7].textContent
            });
        } else if (idDel) {
            if (confirm('Delete this item?')) {
                fetch(`/provider/api/services/${idDel}`, { method:'DELETE', headers: { [header]: token } })
                    .then(() => refresh());
            }
        }
    });

    refresh();
})();
