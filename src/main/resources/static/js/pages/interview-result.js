/* =============================================================================
   PAGES/INTERVIEW-RESULT.JS — Các pop-up trên interview/result.html
   - Phiếu đánh giá (SRS 5.1.29): #iev-dialog
   - Chốt kết quả của Hiring Manager: #ifr-dialog
   Khuôn chung:
     nút mở   [data-dialog-open="<id dialog>"]
     form     [data-ajax-form] data-action (URL POST), data-success-param (tham số thêm vào URL khi tải lại),
              data-number-fields (tên ô gửi dạng số, cách nhau bởi dấu cách)
     trong form: [data-dialog-close], [data-form-alert], [data-form-submit], [data-error-for="<tên ô>"]
   Gửi JSON kèm header CSRF (meta _csrf / _csrf_header). Server trả ApiResponse { success, message, data: { tênÔ: lỗi } }.
   ============================================================================= */
(function () {
    'use strict';

    document.querySelectorAll('[data-dialog-open]').forEach(function (button) {
        const dialog = document.getElementById(button.getAttribute('data-dialog-open'));
        if (!dialog) {
            return;
        }
        button.addEventListener('click', function () {
            const form = dialog.querySelector('[data-ajax-form]');
            if (form) {
                clearErrors(form);
            }
            dialog.showModal();
        });
    });

    document.querySelectorAll('[data-ajax-form]').forEach(function (form) {
        const dialog = form.closest('dialog');
        const closeButton = form.querySelector('[data-dialog-close]');
        if (dialog && closeButton) {
            closeButton.addEventListener('click', function () {
                dialog.close();
            });
        }
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            submit(form);
        });
    });

    function clearErrors(form) {
        const alertBox = form.querySelector('[data-form-alert]');
        if (alertBox) {
            alertBox.hidden = true;
            alertBox.textContent = '';
        }
        form.querySelectorAll('[data-error-for]').forEach(function (error) {
            error.hidden = true;
            error.textContent = '';
        });
    }

    function showAlert(form, message) {
        const alertBox = form.querySelector('[data-form-alert]');
        if (alertBox) {
            alertBox.textContent = message;
            alertBox.hidden = false;
        }
    }

    function showFieldError(form, field, message) {
        const error = form.querySelector('[data-error-for="' + field + '"]');
        if (error) {
            error.textContent = message;
            error.hidden = false;
        }
    }

    function readPayload(form) {
        const numberFields = (form.dataset.numberFields || '').split(/\s+/).filter(Boolean);
        const data = new FormData(form);
        const payload = {};
        form.querySelectorAll('[name]').forEach(function (element) {
            const name = element.name;
            if (name === '_csrf' || Object.prototype.hasOwnProperty.call(payload, name)) {
                return;
            }
            const value = data.get(name);
            if (numberFields.includes(name)) {
                payload[name] = value === null || value === '' ? null : Number(value);
            } else {
                payload[name] = value === null ? '' : value;
            }
        });
        return payload;
    }

    /** Ô [required] còn trống: hiện lỗi ngay, không gửi lên server. */
    function validateRequired(form, payload) {
        let valid = true;
        const checked = new Set();
        form.querySelectorAll('[required][name]').forEach(function (element) {
            const name = element.name;
            if (checked.has(name)) {
                return;
            }
            checked.add(name);
            if (payload[name] === null || payload[name] === '') {
                showFieldError(form, name, element.getAttribute('data-required-message') || 'Vui lòng nhập mục này.');
                valid = false;
            }
        });
        return valid;
    }

    function messageForStatus(status) {
        if (status === 403) {
            return 'Bạn không có quyền thực hiện thao tác này, hoặc phiên đăng nhập đã hết hạn.';
        }
        if (status === 404) {
            return 'Không tìm thấy lịch phỏng vấn.';
        }
        return 'Không gửi được (mã lỗi ' + status + '). Vui lòng thử lại.';
    }

    async function submit(form) {
        clearErrors(form);
        const payload = readPayload(form);
        if (!validateRequired(form, payload)) {
            showAlert(form, 'Vui lòng kiểm tra lại các ô được đánh dấu.');
            return;
        }

        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
        const headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
        if (token) {
            headers[header] = token;
        }

        const submitButton = form.querySelector('[data-form-submit]');
        const submitLabel = submitButton ? submitButton.textContent : '';
        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = 'Đang gửi...';
        }
        try {
            const response = await fetch(form.dataset.action, {
                method: 'POST',
                headers: headers,
                credentials: 'same-origin',
                body: JSON.stringify(payload)
            });
            const body = await response.json().catch(function () {
                return null;
            });

            if (response.ok && body && body.success) {
                const url = new URL(window.location.href);
                url.searchParams.delete('evaluated');
                url.searchParams.delete('finalized');
                if (form.dataset.successParam) {
                    url.searchParams.set(form.dataset.successParam, '1');
                }
                window.location.replace(url.toString());
                return;
            }

            if (body && body.data) {
                Object.entries(body.data).forEach(function (entry) {
                    showFieldError(form, entry[0], entry[1]);
                });
            }
            showAlert(form, body && body.message ? body.message : messageForStatus(response.status));
        } catch (error) {
            showAlert(form, 'Không kết nối được máy chủ. Vui lòng thử lại.');
        } finally {
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.textContent = submitLabel;
            }
        }
    }
})();
