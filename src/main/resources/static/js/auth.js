(() => {
  'use strict';
  const page = document.querySelector('body.auth-page');
  if (!page) return;
  document.title = document.title.replace(' — Mộc Careers', ' — Mộc Tuyển dụng');
  page.querySelector('.auth-header .wordmark')?.setAttribute('aria-label', 'Trang chủ Mộc Tuyển dụng');
  if (page.classList.contains('auth-botanical')) {
    const pauseDecoration = () => page.toggleAttribute('data-auth-paused', document.hidden);
    document.addEventListener('visibilitychange', pauseDecoration);
    pauseDecoration();
    page.querySelectorAll('input').forEach(input => {
      input.addEventListener('input', () => input.setCustomValidity(''));
      input.addEventListener('invalid', () => {
        input.setCustomValidity('');
        if (input.validity.valueMissing) input.setCustomValidity('Vui lòng nhập thông tin này.');
        else if (input.validity.typeMismatch) input.setCustomValidity('Vui lòng nhập email hợp lệ.');
        else if (input.validity.tooShort) input.setCustomValidity(`Vui lòng nhập ít nhất ${input.minLength} ký tự.`);
        else if (input.validity.tooLong) input.setCustomValidity(`Vui lòng nhập tối đa ${input.maxLength} ký tự.`);
        else if (input.validity.patternMismatch) input.setCustomValidity('Vui lòng nhập mã xác minh gồm 6 chữ số.');
      });
    });
  }

  const otpValue = page.querySelector('[data-otp-value]');
  const otpBoxes = page.querySelector('[data-otp-boxes]');
  const digits = [...page.querySelectorAll('[data-otp-digit]')];
  if (otpValue && otpBoxes && digits.length === 6) {
    // One backend field; six visible controls. The regular input remains the no-JS fallback.
    otpValue.id = 'otp-value';
    otpValue.type = 'hidden';
    digits[0].id = 'otp';
    otpBoxes.querySelector('label').htmlFor = 'otp';
    otpBoxes.hidden = false;
    const sync = () => { otpValue.value = digits.map(input => input.value).join(''); };
    const distribute = (index, text) => {
      const numbers = text.replace(/[^0-9]/g, '');
      if (!numbers) {
        digits[index].value = '';
        sync();
        return;
      }
      const start = numbers.length >= digits.length ? 0 : index;
      [...numbers].slice(0, digits.length - start).forEach((number, offset) => {
        digits[start + offset].value = number;
      });
      sync();
      digits[Math.min(start + numbers.length, digits.length - 1)].focus();
    };
    digits.forEach((input, index) => {
      input.required = true;
      input.addEventListener('focus', () => input.select());
      input.addEventListener('input', event => {
        const text = event.data?.length > 1 ? event.data : input.value;
        distribute(index, text);
      });
      input.addEventListener('paste', event => {
        event.preventDefault();
        distribute(index, event.clipboardData.getData('text'));
      });
      input.addEventListener('keydown', event => {
        if (event.key === 'Backspace') {
          event.preventDefault();
          if (input.value) input.value = '';
          else if (index > 0) {
            digits[index - 1].value = '';
            digits[index - 1].focus();
          }
          sync();
        } else if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
          event.preventDefault();
          const next = index + (event.key === 'ArrowLeft' ? -1 : 1);
          digits[Math.max(0, Math.min(next, digits.length - 1))].focus();
        } else if (event.key.length === 1 && !/[0-9]/.test(event.key) && !event.ctrlKey && !event.metaKey && !event.altKey) {
          event.preventDefault();
        }
      });
    });
    sync();
  }

  page.querySelectorAll('[data-password-toggle]').forEach(button => {
    const input = page.querySelector(`#${button.getAttribute('aria-controls')}`);
    if (!input) return;
    const showLabel = button.dataset.showLabel;
    const hideLabel = button.dataset.hideLabel;
    button.hidden = false;
    button.addEventListener('click', () => {
      const visible = input.type === 'password';
      input.type = visible ? 'text' : 'password';
      const label = button.querySelector('[data-password-toggle-label]');
      if (label) label.textContent = visible ? 'Ẩn' : 'Hiện';
      else button.textContent = visible ? 'Ẩn' : 'Hiện';
      button.setAttribute('aria-label', visible ? hideLabel : showLabel);
      button.setAttribute('aria-pressed', String(visible));
    });
  });

  const errorSummary = page.querySelector('[data-auth-errors]');
  if (errorSummary) errorSummary.focus();

  const forms = [...page.querySelectorAll('[data-auth-form]')];
  forms.forEach(form => {
    const button = form.querySelector('[data-auth-submit]');
    if (!button) return;
    const label = button.querySelector('[data-auth-submit-label]') || button;
    button.dataset.idleLabel = label.textContent;
    form.addEventListener('submit', event => {
      if (form.dataset.submitting === 'true') {
        event.preventDefault();
        return;
      }
      form.dataset.submitting = 'true';
      form.setAttribute('aria-busy', 'true');
      button.disabled = true;
      label.textContent = button.dataset.busyLabel;
    });
  });

  // This display uses only a server-supplied retry interval. The server enforces it.
  page.querySelectorAll('[data-retry-countdown]').forEach(timer => {
    const seconds = Number(timer.dataset.retryAfterSeconds);
    if (!Number.isFinite(seconds) || seconds <= 0) return;
    const form = timer.closest('form');
    const button = form?.querySelector('[data-auth-submit]');
    if (!button) return;
    const note = timer.parentElement;
    const status = document.createElement('p');
    status.className = 'auth-visually-hidden';
    status.setAttribute('role', 'status');
    status.setAttribute('data-retry-status', '');
    note.after(status);
    const deadline = Date.now() + seconds * 1000;
    button.disabled = true;
    const update = () => {
      const remaining = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
      if (remaining === 0) {
        note.textContent = 'Bạn có thể yêu cầu gửi lại mã xác minh.';
        status.textContent = note.textContent;
        if (form.dataset.submitting !== 'true') button.disabled = false;
        clearInterval(interval);
        return;
      }
      timer.textContent = `${Math.floor(remaining / 60)}:${String(remaining % 60).padStart(2, '0')}`;
    };
    const interval = setInterval(update, 1000);
    update();
  });

  // Restore submit controls when returning through the browser's back/forward cache.
  window.addEventListener('pageshow', () => {
    forms.forEach(form => {
      if (form.dataset.submitting !== 'true') return;
      delete form.dataset.submitting;
      form.removeAttribute('aria-busy');
      const button = form.querySelector('[data-auth-submit]');
      button.disabled = false;
      const label = button.querySelector('[data-auth-submit-label]') || button;
      label.textContent = button.dataset.idleLabel;
    });
  });
})();
