// /provider/js/provider-reviews.js
(() => {
  const tbody = document.querySelector("[data-review-list]");
  const countEl = document.getElementById("review-count");
  const avgEl = document.getElementById("review-average");

  if (!tbody) return;

  function renderStars(rating) {
    // rating: 1..5
    let html = "";
    for (let i = 1; i <= 5; i++) {
      if (i <= rating) {
        html += '<i class="fa fa-star text-warning"></i>';
      } else {
        html += '<i class="fa fa-star text-secondary"></i>';
      }
    }
    return html;
  }

  function loadReviews() {
    fetch("/api/provider/reviews", {
      headers: { "Accept": "application/json" }
    })
      .then(res => {
        if (!res.ok) throw new Error("Cannot load reviews");
        return res.json();
      })
      .then(list => {
        tbody.innerHTML = "";

        if (!Array.isArray(list) || list.length === 0) {
          tbody.innerHTML = `
            <tr>
              <td colspan="5" class="text-center text-muted">
                Chưa có đánh giá nào từ khách hàng.
              </td>
            </tr>`;
          if (countEl) countEl.textContent = "0";
          if (avgEl) avgEl.textContent = "—";
          return;
        }

        let total = 0;

        list.forEach((item, index) => {
          total += item.rating || 0;

          const tr = document.createElement("tr");

          tr.innerHTML = `
            <td>${index + 1}</td>
            <td>
              #${item.requestId ?? ""}
            </td>
            <td>
              ${renderStars(item.rating || 0)}
              <span class="ml-1 small text-muted">(${item.rating || 0}/5)</span>
            </td>
            <td>${item.comment ? item.comment : '<span class="text-muted">Không có</span>'}</td>
            <td>${item.createdAt || ""}</td>
          `;

          tbody.appendChild(tr);
        });

        if (countEl) countEl.textContent = String(list.length);

        if (avgEl) {
          const avg = total / list.length;
          avgEl.textContent = avg.toFixed(1).replace(".", ",") + " / 5";
        }
      })
      .catch(err => {
        console.error(err);
        tbody.innerHTML = `
          <tr>
            <td colspan="5" class="text-center text-danger">
              Không thể tải danh sách đánh giá. Vui lòng thử lại sau.
            </td>
          </tr>`;
      });
  }

  loadReviews();
})();
