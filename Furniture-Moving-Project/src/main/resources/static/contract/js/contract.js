(function () {
  const agree = document.getElementById('agree');
  const btnAccept = document.getElementById('btnAccept');
  const btnPrint = document.getElementById('btnPrint');
  const toast = document.getElementById('toast');
  const paper = document.getElementById('contractBody');

  // Enable/disable nút "Tôi đồng ý"
  function updateButton() {
    btnAccept.disabled = !agree.checked;
  }
  if (agree && btnAccept) {
    agree.addEventListener('change', updateButton);
    updateButton();
  }

  // In hợp đồng
  if (btnPrint) {
    btnPrint.addEventListener('click', () => {
      try {
        window.print();
      } catch (e) {
        showToast('Không thể in trên trình duyệt này.');
      }
    });
  }

  // Lưu & khôi phục vị trí cuộn nội dung hợp đồng (UX nhỏ)
  const SCROLL_KEY = 'contract_scroll_top';
  if (paper) {
    const saved = parseInt(localStorage.getItem(SCROLL_KEY) || '0', 10);
    if (!Number.isNaN(saved)) paper.scrollTop = saved;
    paper.addEventListener('scroll', () => {
      localStorage.setItem(SCROLL_KEY, String(paper.scrollTop));
    }, { passive: true });
  }

  // Toast helper
  function showToast(msg, ms = 1800) {
    if (!toast) return;
    toast.textContent = msg;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), ms);
  }

  // Nếu người dùng cố submit khi chưa tick (không-JS thì HTML5 required sẽ chặn)
  const form = document.querySelector('form.actions');
  if (form) {
    form.addEventListener('submit', (e) => {
      if (!agree || !agree.checked) {
        e.preventDefault();
        showToast('Bạn cần đồng ý điều khoản để tiếp tục.');
        try { paper && paper.focus(); } catch (_) {}
      }
    });
  }

  // CSRF: nếu bạn submit bằng fetch/AJAX (ở đây không cần) thì đọc meta:
  // const token = document.querySelector('meta[name="_csrf"]')?.content;
  // const header = document.querySelector('meta[name="_csrf_header"]')?.content;
})();
