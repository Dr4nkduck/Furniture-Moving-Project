(function () {
  function toast(msg) { alert(msg); }

  var aiStart = document.getElementById('btn-ai-start');
  var aiGuide = document.getElementById('btn-ai-guide');
  var manualStart = document.getElementById('btn-manual-start');
  var manualTemplate = document.getElementById('btn-manual-template');

  if (aiStart) aiStart.addEventListener('click', function () {
    toast('Tính năng AI sẽ sớm ra mắt. Hiện tại vui lòng dùng Báo giá thủ công.');
  });

  if (aiGuide) aiGuide.addEventListener('click', function () {
    toast('Hướng dẫn nhanh: Chụp ảnh tổng quan (rõ), AI sẽ đếm & gợi ý giá. Sắp mở!');
  });

  if (manualStart) manualStart.addEventListener('click', function () {
    toast('Mẫu nhập báo giá thủ công sẽ được bật trong bản kế tiếp.');
  });

  if (manualTemplate) manualTemplate.addEventListener('click', function () {
    toast('Bạn sẽ xem được một mẫu bảng giá tham khảo ở bản kế tiếp.');
  });
})();


