(function () {
  const agree = document.getElementById('agree');
  const btnAccept = document.getElementById('btnAccept');
  const btnPrint = document.getElementById('btnPrint');
  const toast = document.getElementById('toast');
  const paper = document.getElementById('contractBody');

  // Enable/disable nút "Tôi đồng ý"
  function updateButton() {
    if (!agree || !btnAccept) return;
    btnAccept.disabled = !agree.checked;
  }
  agree && agree.addEventListener('change', updateButton);
  updateButton();

  // In hợp đồng
  btnPrint && btnPrint.addEventListener('click', () => {
    try { window.print(); } catch { showToast('Không thể in trên trình duyệt này.'); }
  });

  // Lưu & khôi phục vị trí cuộn nội dung hợp đồng
  const SCROLL_KEY = 'contract_scroll_top';
  if (paper) {
    const saved = parseInt(localStorage.getItem(SCROLL_KEY) || '0', 10);
    if (!Number.isNaN(saved)) paper.scrollTop = saved;
    paper.addEventListener('scroll', () => {
      localStorage.setItem(SCROLL_KEY, String(paper.scrollTop));
    }, { passive: true });
  }

  function showToast(msg, ms = 1800) {
    if (!toast) return;
    toast.textContent = msg;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), ms);
  }

  // Submit: chỉ chặn khi chưa tick đồng ý; CÒN LẠI để form POST về /contract/accept
  const form = document.querySelector('form.actions');
  if (form) {
    form.addEventListener('submit', (e) => {
      if (!agree || !agree.checked) {
        e.preventDefault();
        showToast('Bạn cần đồng ý điều khoản để tiếp tục.');
        try { paper && paper.focus(); } catch {}
      }
      // ✅ Không redirect bằng JS; để server xử lý và redirect sang /request
    });
  }
})();
