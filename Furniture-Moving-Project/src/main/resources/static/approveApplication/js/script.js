(function () {
    "use strict";

    const root = document.getElementById("invoiceRoot");
    const statusBadge = document.getElementById("statusBadge");
    const providerNameEl = document.getElementById("providerName");
    const itemsTableBody = document.getElementById("itemsTable");
    const totalAmountEl = document.getElementById("totalAmount");

    // Lưu ý: CSS chỉ định nghĩa .status-pending và .status-completed
    const STATUS_META = {
        pending:   { label: "Chờ xử lý",    className: "status-badge status-pending" },
        completed: { label: "Đã hoàn thành", className: "status-badge status-completed" }
    };

    function resolveStatus() {
        return (root.getAttribute("data-status") || "pending").trim();
    }

    function resolveProviderName() {
        const s = (root.getAttribute("data-provider-name") || "").trim();
        return s || null;
    }

    function initStatusAndProvider() {
        const st = resolveStatus();
        const meta = STATUS_META[st] || STATUS_META.pending;
        statusBadge.textContent = meta.label;
        statusBadge.className = meta.className;

        const provider = resolveProviderName();
        providerNameEl.textContent = provider ?? "null";
    }

    function readItemsFromSeed() {
        try {
            const raw = document.getElementById("itemsSeed")?.textContent || "[]";
            const arr = JSON.parse(raw);
            return Array.isArray(arr) ? arr : [];
        } catch (e) {
            return [];
        }
    }

    function formatMoney(n) {
        return Number(n || 0).toLocaleString("vi-VN");
    }

    function escapeHtml(str) {
        return String(str)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    function renderItems() {
        const items = readItemsFromSeed();

        if (!items.length) {
            itemsTableBody.innerHTML =
                '<tr><td colspan="5" class="empty-row">Chưa có hàng hóa nào.</td></tr>';
            totalAmountEl.textContent = "0";
            return;
        }

        // Map DB record -> row hiển thị (đơn giá tạm = 0, vì DB chưa có)
        const rows = items.map((r) => {
            let name = r.item_name || r.item_type || "Unknown";
            if (r.size) name += ` (${r.size})`;
            if (r.is_fragile) name += " • Dễ vỡ";
            const quantity = parseInt(r.quantity, 10) || 1;
            const price = 0;
            return { name, quantity, price };
        });

        itemsTableBody.innerHTML = rows
            .map(
                (it) => `
        <tr>
          <td>${escapeHtml(it.name)}</td>
          <td>${it.quantity}</td>
          <td>${formatMoney(it.price)}</td>
          <td>${formatMoney(it.quantity * it.price)}</td>
          <td><!-- không hành động --></td>
        </tr>`
            )
            .join("");

        const total = rows.reduce((sum, it) => sum + it.quantity * it.price, 0);
        totalAmountEl.textContent = formatMoney(total);
    }

    // Khởi chạy
    initStatusAndProvider();
    renderItems();
})();
// (function () {
//     "use strict";
//
//     const root = document.getElementById("invoiceRoot");
//     const statusBadge = document.getElementById("statusBadge");
//     const providerNameEl = document.getElementById("providerName");
//     const itemsTableBody = document.getElementById("itemsTable");
//     const totalAmountEl = document.getElementById("totalAmount");
//
//     // Provider-only controls (rendered server-side bằng sec:authorize)
//     const providerActions = document.getElementById("providerActions");
//     const acceptBtn = document.getElementById("acceptBtn");
//     const rejectBtn = document.getElementById("rejectBtn");
//
//     // CSS hiện có: .status-pending, .status-completed
//     // Bổ sung thêm class để bạn style nếu muốn: .status-accepted, .status-rejected
//     const STATUS_META = {
//         pending:   { label: "Chờ xử lý",     className: "status-badge status-pending" },
//         accepted:  { label: "Đã chấp nhận",  className: "status-badge status-accepted" },
//         rejected:  { label: "Đã từ chối",    className: "status-badge status-rejected" },
//         completed: { label: "Đã hoàn thành", className: "status-badge status-completed" }
//     };
//
//     const normalize = (s) => (s ? s.toString().trim().toLowerCase() : "pending");
//
//     function currentStatus() {
//         return normalize(root.getAttribute("data-status"));
//     }
//
//     function setStatus(newStatus) {
//         const key = normalize(newStatus);
//         const meta = STATUS_META[key] || STATUS_META.pending;
//         statusBadge.textContent = meta.label;
//         statusBadge.className = meta.className;
//         root.setAttribute("data-status", key);
//
//         // Provider controls chỉ hiện khi đang pending
//         if (providerActions) {
//             providerActions.style.display = key === "pending" ? "" : "none";
//         }
//     }
//
//     function resolveProviderName() {
//         const s = (root.getAttribute("data-provider-name") || "").trim();
//         return s || null;
//     }
//
//     function readItemsFromSeed() {
//         try {
//             const raw = document.getElementById("itemsSeed")?.textContent || "[]";
//             const arr = JSON.parse(raw);
//             return Array.isArray(arr) ? arr : [];
//         } catch {
//             return [];
//         }
//     }
//
//     function formatMoney(n) {
//         return Number(n || 0).toLocaleString("vi-VN");
//     }
//
//     function escapeHtml(str) {
//         return String(str)
//             .replaceAll("&", "&amp;")
//             .replaceAll("<", "&lt;")
//             .replaceAll(">", "&gt;")
//             .replaceAll('"', "&quot;")
//             .replaceAll("'", "&#039;");
//     }
//
//     function renderItems() {
//         const items = readItemsFromSeed();
//
//         if (!items.length) {
//             itemsTableBody.innerHTML =
//                 '<tr><td colspan="5" class="empty-row">Chưa có hàng hóa nào.</td></tr>';
//             totalAmountEl.textContent = "0";
//             return;
//         }
//
//         // Map DB record -> row hiển thị (đơn giá tạm = 0 nếu DB chưa có)
//         const rows = items.map((r) => {
//             let name = r.item_name || r.item_type || "Unknown";
//             if (r.size) name += ` (${r.size})`;
//             if (r.is_fragile) name += " • Dễ vỡ";
//             const quantity = parseInt(r.quantity, 10) || 1;
//             const price = Number(r.price ?? 0); // nếu DB có price thì dùng
//             return { name, quantity, price };
//         });
//
//         itemsTableBody.innerHTML = rows
//             .map(
//                 (it) => `
//         <tr>
//           <td>${escapeHtml(it.name)}</td>
//           <td>${it.quantity}</td>
//           <td>${formatMoney(it.price)}</td>
//           <td>${formatMoney(it.quantity * it.price)}</td>
//           <td><!-- không hành động --></td>
//         </tr>`
//             )
//             .join("");
//
//         const total = rows.reduce((sum, it) => sum + it.quantity * it.price, 0);
//         totalAmountEl.textContent = formatMoney(total);
//     }
//
//     // ====== Provider: Cập nhật trạng thái ======
//     function resolveUpdateUrl() {
//         // Ưu tiên data-update-url (bind sẵn bằng Thymeleaf)
//         const bound = root.getAttribute("data-update-url");
//         if (bound && bound !== "null") return bound;
//
//         // Fallback: tự build theo data-invoice-id hoặc invoiceNo text
//         const id =
//             root.getAttribute("data-invoice-id") ||
//             document.getElementById("invoiceNo")?.textContent?.trim();
//         if (!id) return null;
//         return `/api/invoices/${encodeURIComponent(id)}/status`;
//     }
//
//     function getCsrfHeader() {
//         const token = document.querySelector('meta[name="_csrf"]')?.content;
//         const header = document.querySelector('meta[name="_csrf_header"]')?.content;
//         if (token && header) return { header, token };
//         return null;
//     }
//
//     async function updateStatusRemote(newStatus) {
//         const url = resolveUpdateUrl();
//         if (!url) throw new Error("Thiếu endpoint cập nhật trạng thái (data-update-url hoặc data-invoice-id).");
//
//         const headers = { "Content-Type": "application/json" };
//         const csrf = getCsrfHeader();
//         if (csrf) headers[csrf.header] = csrf.token;
//
//         const resp = await fetch(url, {
//             method: "PUT",
//             headers,
//             body: JSON.stringify({ status: newStatus.toUpperCase() }) // ví dụ: ACCEPTED / REJECTED
//         });
//
//         if (!resp.ok) {
//             const text = await resp.text().catch(() => "");
//             throw new Error(text || `HTTP ${resp.status}`);
//         }
//     }
//
//     async function confirmAndUpdate(newKey) {
//         const labels = { accepted: "Chấp nhận", rejected: "Từ chối" };
//         if (!confirm(`Xác nhận ${labels[newKey] || newKey}?`)) return;
//
//         try {
//             await updateStatusRemote(newKey);
//             setStatus(newKey);
//             alert(`Đã ${labels[newKey] || newKey.toUpperCase()} đơn.`);
//         } catch (err) {
//             console.error(err);
//             alert("Cập nhật trạng thái thất bại: " + err.message);
//         }
//     }
//
//     function initStatusAndProvider() {
//         setStatus(currentStatus());
//
//         const provider = resolveProviderName();
//         if (providerNameEl) providerNameEl.textContent = provider ?? "null";
//
//         // Bind sự kiện cho Provider nếu block tồn tại (tức là có role)
//         if (providerActions) {
//             providerActions.style.display = currentStatus() === "pending" ? "" : "none";
//             acceptBtn?.addEventListener("click", () => confirmAndUpdate("accepted"));
//             rejectBtn?.addEventListener("click", () => confirmAndUpdate("rejected"));
//         }
//     }
//
//     // Khởi chạy
//     initStatusAndProvider();
//     renderItems();
// })();
