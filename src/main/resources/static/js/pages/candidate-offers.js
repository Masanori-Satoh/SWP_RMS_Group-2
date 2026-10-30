/*
 * Trang "Thư mời nhận việc" (Danh sách & Chi tiết):
 * - Danh sách: Bấm nhận / từ chối mở hộp thoại xác nhận; cho phép chọn chip lý do nhanh.
 * - Chi tiết: Bấm nút trên thanh tác vụ mở hộp thoại xác nhận riêng biệt tương ứng.
 * - Nút chip lý do: bấm chip để tự động điền lý do phổ biến vào ô phản hồi.
 * - Khóa nút xác nhận khi đang gửi để tránh gửi hai lần (double-submit).
 */
(function () {
  'use strict';

  // 1. Khởi tạo chức năng Chips lý do nhanh (dùng chung cho cả danh sách và chi tiết)
  function initReasonChips(container) {
    if (!container) return;
    var chips = container.querySelectorAll('.cof-chip[data-reason]');
    var textarea = container.querySelector('textarea');
    if (!chips.length || !textarea) return;

    chips.forEach(function (chip) {
      chip.addEventListener('click', function () {
        var reason = chip.dataset.reason;
        var current = textarea.value.trim();

        if (chip.classList.contains('cof-chip--active')) {
          chip.classList.remove('cof-chip--active');
          if (current === reason) {
            textarea.value = '';
          }
        } else {
          chips.forEach(function (c) { c.classList.remove('cof-chip--active'); });
          chip.classList.add('cof-chip--active');
          textarea.value = reason;
        }
        textarea.focus();
      });
    });

    textarea.addEventListener('input', function () {
      var current = textarea.value.trim();
      chips.forEach(function (chip) {
        chip.classList.toggle('cof-chip--active', chip.dataset.reason === current);
      });
    });
  }

  // 2. Logic hộp thoại trên trang Danh sách (/portal/offers)
  var listDialog = document.getElementById('cof-dialog');
  if (listDialog && typeof listDialog.showModal === 'function') {
    var titleSlots = listDialog.querySelectorAll('[data-offer-title]');
    var copies = listDialog.querySelectorAll('[data-offer-copy]');
    var cancelButton = listDialog.querySelector('[data-offer-cancel]');
    var confirmButton = listDialog.querySelector('[data-offer-confirm]');
    var reasonInput = listDialog.querySelector('#cof-dialog-reason-input');
    var pendingForm = null;
    var trigger = null;

    initReasonChips(listDialog);

    function showCopy(action) {
      copies.forEach(function (node) {
        node.hidden = node.dataset.offerCopy !== action;
      });
      confirmButton.classList.toggle('btn--danger', action === 'decline');
      confirmButton.classList.toggle('btn--primary', action !== 'decline');
      if (reasonInput) {
        reasonInput.value = '';
        listDialog.querySelectorAll('.cof-chip').forEach(function (c) {
          c.classList.remove('cof-chip--active');
        });
      }
    }

    document.querySelectorAll('form[data-offer-form]').forEach(function (form) {
      form.addEventListener('submit', function (event) {
        event.preventDefault();
        pendingForm = form;
        trigger = form.querySelector('button[type="submit"]');
        showCopy(form.dataset.offerForm);
        titleSlots.forEach(function (slot) {
          slot.textContent = form.dataset.postingTitle || '';
        });
        confirmButton.disabled = false;
        listDialog.showModal();
        cancelButton.focus();
      });
    });

    if (cancelButton) {
      cancelButton.addEventListener('click', function () {
        listDialog.close();
      });
    }

    if (confirmButton) {
      confirmButton.addEventListener('click', function () {
        if (!pendingForm || confirmButton.disabled) {
          return;
        }
        confirmButton.disabled = true;

        // Nếu là từ chối và có lý do, chèn tham số declineReason vào form
        if (pendingForm.dataset.offerForm === 'decline' && reasonInput && reasonInput.value.trim()) {
          var hiddenInput = pendingForm.querySelector('input[name="declineReason"]');
          if (!hiddenInput) {
            hiddenInput = document.createElement('input');
            hiddenInput.type = 'hidden';
            hiddenInput.name = 'declineReason';
            pendingForm.appendChild(hiddenInput);
          }
          hiddenInput.value = reasonInput.value.trim();
        }

        pendingForm.submit();
      });
    }

    listDialog.addEventListener('close', function () {
      if (trigger && !confirmButton.disabled) {
        trigger.focus();
      }
      pendingForm = null;
    });
  }

  // 3. Logic hộp thoại trên trang Chi tiết (/portal/offers/{id})
  var acceptDialog = document.getElementById('cof-accept-dialog');
  var declineDialog = document.getElementById('cof-decline-dialog');

  if (declineDialog) {
    initReasonChips(declineDialog);
  }

  // 4. Logic trang Từ chối riêng (/portal/offers/{id}/decline)
  var declinePageContainer = document.querySelector('.cof-decline-form');
  if (declinePageContainer) {
    initReasonChips(declinePageContainer);
  }

  // Mở modal từ nút kích hoạt trên thanh tác vụ trang chi tiết
  document.querySelectorAll('[data-detail-trigger]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var action = btn.dataset.detailTrigger;
      if (action === 'accept' && acceptDialog && typeof acceptDialog.showModal === 'function') {
        acceptDialog.showModal();
      } else if (action === 'decline' && declineDialog && typeof declineDialog.showModal === 'function') {
        declineDialog.showModal();
      }
    });
  });

  // Đóng modal chi tiết
  document.querySelectorAll('[data-detail-close]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var targetId = btn.dataset.detailClose;
      var target = document.getElementById(targetId);
      if (target && typeof target.close === 'function') {
        target.close();
      }
    });
  });

  // Chống double-submit cho các form chi tiết
  document.querySelectorAll('.cof-detail-form').forEach(function (form) {
    form.addEventListener('submit', function () {
      var submitBtn = form.querySelector('button[type="submit"]');
      if (submitBtn) {
        submitBtn.disabled = true;
      }
    });
  });

  // Phục hồi trạng thái nút khi bấm Back trên trình duyệt
  window.addEventListener('pageshow', function () {
    document.querySelectorAll('button[type="submit"]').forEach(function (b) {
      b.disabled = false;
    });
    if (typeof confirmButton !== 'undefined' && confirmButton) {
      confirmButton.disabled = false;
    }
  });

  // 5. Đồng hồ đếm ngược hạn phản hồi thư mời
  function initCountdowns() {
    var countdownEls = document.querySelectorAll('.cof-countdown[data-deadline]');
    if (!countdownEls.length) return;

    function pad(n) {
      return n < 10 ? '0' + n : n;
    }

    function update() {
      var now = new Date().getTime();
      countdownEls.forEach(function (el) {
        var deadlineStr = el.dataset.deadline;
        if (!deadlineStr) return;
        var deadlineTime = new Date(deadlineStr).getTime();
        var diff = deadlineTime - now;

        if (isNaN(diff)) return;

        if (diff <= 0) {
          el.textContent = el.classList.contains('cof-top-bar__val') ? 'Đã hết hạn' : '⏱ Đã hết hạn phản hồi';
          el.classList.add('cof-countdown--expired');
        } else {
          var days = Math.floor(diff / (1000 * 60 * 60 * 24));
          var hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
          var minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
          var seconds = Math.floor((diff % (1000 * 60)) / 1000);

          var parts = [];
          if (days > 0) {
            parts.push(days + ' ngày');
          }
          parts.push(pad(hours) + 'h ' + pad(minutes) + 'm ' + pad(seconds) + 's');
          el.textContent = el.classList.contains('cof-top-bar__val') ? parts.join(' ') : ('⏱ Còn lại: ' + parts.join(' '));
        }
      });
    }

    update();
    setInterval(update, 1000);
  }

  initCountdowns();

  // 6. Đóng modal thông báo chấp nhận thành công theo Mockup
  window.closeAcceptSuccessModal = function () {
    var modalBackdrop = document.getElementById('cof-accept-success-modal');
    if (modalBackdrop) {
      modalBackdrop.style.transition = 'opacity 0.2s ease';
      modalBackdrop.style.opacity = '0';
      setTimeout(function () {
        if (modalBackdrop.parentNode) {
          modalBackdrop.parentNode.removeChild(modalBackdrop);
        }
      }, 200);
    }
  };

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      window.closeAcceptSuccessModal();
    }
  });

  var successBackdrop = document.getElementById('cof-accept-success-modal');
  if (successBackdrop) {
    successBackdrop.addEventListener('click', function (e) {
      if (e.target === successBackdrop) {
        window.closeAcceptSuccessModal();
      }
    });
  }

  // 7. Xử lý nút Tải xuống PDF (in trang / lưu thành PDF)
  document.querySelectorAll('[data-action="download-pdf"]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      window.print();
    });
  });
})();

