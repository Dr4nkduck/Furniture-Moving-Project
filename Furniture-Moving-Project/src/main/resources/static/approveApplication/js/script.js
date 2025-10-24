const items = []
let editingIndex = -1
let isPending = true

// Initialize
document.getElementById("itemForm").addEventListener("submit", saveItem)
document.getElementById("toggleStatusBtn").addEventListener("click", toggleStatus)
updateUI()

function openAddModal() {
    if (!isPending) return
    editingIndex = -1
    document.getElementById("modalTitle").textContent = "Thêm hàng hóa"
    document.getElementById("itemForm").reset()
    document.getElementById("itemModal").style.display = "block"
}

function openEditModal(index) {
    if (!isPending) return
    editingIndex = index
    const item = items[index]
    document.getElementById("modalTitle").textContent = "Chỉnh sửa hàng hóa"
    document.getElementById("itemName").value = item.name
    document.getElementById("itemQuantity").value = item.quantity
    document.getElementById("itemPrice").value = item.price
    document.getElementById("itemModal").style.display = "block"
}

function closeModal() {
    document.getElementById("itemModal").style.display = "none"
}

function saveItem(e) {
    e.preventDefault()
    const name = document.getElementById("itemName").value
    const quantity = Number.parseInt(document.getElementById("itemQuantity").value)
    const price = Number.parseFloat(document.getElementById("itemPrice").value)

    if (editingIndex === -1) {
        items.push({ name, quantity, price })
    } else {
        items[editingIndex] = { name, quantity, price }
    }

    closeModal()
    updateUI()
}

function deleteItem(index) {
    if (!isPending) return
    if (confirm("Bạn có chắc chắn muốn xóa hàng hóa này?")) {
        items.splice(index, 1)
        updateUI()
    }
}

function toggleStatus() {
    isPending = !isPending
    updateUI()
}

function updateUI() {
    // Update status badge
    const badge = document.getElementById("statusBadge")
    const btn = document.getElementById("toggleStatusBtn")
    const addBtn = document.getElementById("addItemBtn")

    if (isPending) {
        badge.textContent = "Chờ xử lý"
        badge.className = "status-badge status-pending"
        btn.textContent = "Đánh dấu hoàn thành"
        addBtn.style.display = "block"
    } else {
        badge.textContent = "Đã hoàn thành"
        badge.className = "status-badge status-completed"
        btn.textContent = "Đánh dấu chờ xử lý"
        addBtn.style.display = "none"
    }

    // Update table
    const tbody = document.getElementById("itemsTable")
    if (items.length === 0) {
        tbody.innerHTML =
            '<tr><td colspan="5" class="empty-row">Chưa có hàng hóa nào. Nhấn "Thêm hàng hóa" để bắt đầu.</td></tr>'
    } else {
        tbody.innerHTML = items
            .map(
                (item, index) => `
            <tr>
                <td>${item.name}</td>
                <td>${item.quantity}</td>
                <td>${item.price.toLocaleString("vi-VN")}</td>
                <td>${(item.quantity * item.price).toLocaleString("vi-VN")}</td>
                <td>
                    <div class="action-buttons" ${!isPending ? 'style="opacity: 0.5; pointer-events: none;"' : ""}>
                        <button class="btn-edit" onclick="openEditModal(${index})" ${!isPending ? "disabled" : ""}>Sửa</button>
                        <button class="btn-delete" onclick="deleteItem(${index})" ${!isPending ? "disabled" : ""}>Xóa</button>
                    </div>
                </td>
            </tr>
        `,
            )
            .join("")
    }

    // Calculate total
    const total = items.reduce((sum, item) => sum + item.quantity * item.price, 0)
    document.getElementById("totalAmount").textContent = total.toLocaleString("vi-VN")
}

// Close modal when clicking outside
window.onclick = (event) => {
    const modal = document.getElementById("itemModal")
    if (event.target === modal) {
        closeModal()
    }
}
