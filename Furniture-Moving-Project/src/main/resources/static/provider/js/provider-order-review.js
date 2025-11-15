// /provider/js/provider-order-review.js
(() => {
  const btn = document.getElementById("btnViewReview");
  const box = document.getElementById("reviewBox");
  const starsEl = document.getElementById("reviewStars");
  const timeEl = document.getElementById("reviewTime");
  const commentEl = document.getElementById("reviewComment");

  if (!btn || !box || !starsEl || !commentEl) return;

  function renderStars(rating) {
    const r = Number(rating) || 0;
    let html = "";
    for (let i = 1; i <= 5; i++) {
      if (i <= r) {
        html += '<i class="fa fa-star text-warning"></i>';
      } else {
        html += '<i class="fa fa-star text-secondary"></i>';
      }
      if (i < 5) html += " ";
    }
    return html;
  }

  function loadReview() {
    const idSpan = document.getElementById("d-id");
    const idText = idSpan ? idSpan.textContent.trim() : "";

    if (!idText) {
      box.classList.remove("d-none");
      starsEl.innerHTML = "";
      timeEl.textContent = "";
      commentEl.textContent = "Vui lòng chọn một đơn để xem đánh giá.";
      return;
    }

    fetch(`/api/provider/reviews/by-request/${idText}`, {
      headers: { "Accept": "application/json" }
    })
      .then(res => {
        if (res.status === 404) {
          // Chưa có review cho đơn này
          box.classList.remove("d-none");
          starsEl.innerHTML = "";
          timeEl.textContent = "";
          commentEl.textContent = "Khách hàng chưa đánh giá đơn này.";
          return null;
        }
        if (!res.ok) {
          throw new Error("Cannot load review");
        }
        return res.json();
      })
      .then(data => {
        if (!data) return;
        box.classList.remove("d-none");

        starsEl.innerHTML = renderStars(data.rating);
        timeEl.textContent = data.createdAt
          ? `Đánh giá lúc ${data.createdAt}`
          : "";

        if (data.comment && data.comment.trim() !== "") {
          commentEl.textContent = data.comment;
        } else {
          commentEl.textContent = "Khách hàng không để lại lời nhắn.";
        }
      })
      .catch(err => {
        console.debug("Load review error:", err);
        box.classList.remove("d-none");
        starsEl.innerHTML = "";
        timeEl.textContent = "";
        commentEl.textContent = "Không thể tải đánh giá. Vui lòng thử lại sau.";
      });
  }

  // Bấm nút -> toggle mở/đóng khung review (mỗi lần mở sẽ reload)
  btn.addEventListener("click", () => {
    const isVisible = !box.classList.contains("d-none");
    if (isVisible) {
      box.classList.add("d-none");
    } else {
      loadReview();
    }
  });
})();
