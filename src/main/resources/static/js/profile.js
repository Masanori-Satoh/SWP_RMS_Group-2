/**
 * User Profile & Change Password JavaScript
 * Features:
 * - Dirty Checking on Profile Form (detects real changes vs whitespace/accidental spaces)
 * - Confirmation Modal before saving Profile changes
 * - Discard Changes (reverts to original values)
 * - Safe exit warning (beforeunload) when unsaved changes exist
 * - Native Dialog modal management
 * - Asynchronous AJAX password change via Fetch API with UTF-8 support
 * - Zero page reload to prevent losing unsaved form fields in the main Profile form
 * - Comprehensive inline validation and Toast notifications
 */
document.addEventListener('DOMContentLoaded', () => {
    // -------------------------------------------------------------
    // 1. Elements & Variables: Change Password Modal (AJAX)
    // -------------------------------------------------------------
    const passwordModal = document.getElementById('changePasswordModal');
    const passwordForm = document.getElementById('changePasswordForm');
    const passwordAlertBox = document.getElementById('changePasswordAlert');
    const btnSubmitPassword = document.getElementById('btnSubmitPassword');
    const toastContainer = document.getElementById('toastContainer');
    // Chữ của nút lấy từ trang (trang ứng viên viết tiếng Việt); data-busy-label là chữ lúc đang gửi
    const submitPasswordLabel = btnSubmitPassword ? btnSubmitPassword.textContent : '';

    // -------------------------------------------------------------
    // 2. Elements & Variables: Profile Form & Confirmation
    // -------------------------------------------------------------
    const profileForm = document.getElementById('profileForm');
    const fullNameInput = document.getElementById('full-name');
    // Mọi ô được theo dõi thay đổi có data-profile-field (/profile và /portal/profile dùng chung file này)
    const profileFields = profileForm ? Array.from(profileForm.querySelectorAll('[data-profile-field]')) : [];
    const btnSaveProfile = document.getElementById('btnSaveProfile');
    const btnDiscardProfile = document.getElementById('btnDiscardProfile');
    const confirmSaveModal = document.getElementById('confirmSaveModal');
    const btnConfirmSave = document.getElementById('btnConfirmSave');

    let isSubmittingProfile = false;

    // Ẩn/hiện bằng cả thuộc tính hidden (trang mới không dùng style nội tuyến; global.css: [hidden] !important)
    // lẫn style.display (trang /profile cũ khởi tạo bằng style="display: none")
    function setVisible(el, visible, display = 'block') {
        if (!el) return;
        el.hidden = !visible;
        el.style.display = visible ? display : 'none';
    }

    // Snapshot of initial values (trimmed for reliable comparison)
    const initialProfileValues = new Map(profileFields.map(field => [field, field.value.trim()]));

    // Helper: Determine if profile has real changes compared to initial state
    function isProfileDirty() {
        return profileFields.some(field => field.value.trim() !== initialProfileValues.get(field));
    }

    // Update UI state based on dirty status
    function updateProfileFormState() {
        const dirty = isProfileDirty();
        if (btnSaveProfile) {
            btnSaveProfile.disabled = !dirty;
        }
        if (btnDiscardProfile) {
            setVisible(btnDiscardProfile, dirty, 'inline-block');
        }
    }

    // Attach dirty-checking listeners to profile inputs
    profileFields.forEach(input => {
        input.addEventListener('input', updateProfileFormState);
        input.addEventListener('change', updateProfileFormState);

        // When leaving input (blur), auto-clean accidental whitespace if value equals initial
        input.addEventListener('blur', () => {
            if (input.value.trim() === initialProfileValues.get(input)) {
                input.value = initialProfileValues.get(input);
            }
            updateProfileFormState();
        });
    });

    // Discard changes button
    if (btnDiscardProfile) {
        btnDiscardProfile.addEventListener('click', () => {
            profileFields.forEach(field => { field.value = initialProfileValues.get(field); });
            updateProfileFormState();
            showToast('Đã hoàn tác, hồ sơ trở về giá trị ban đầu.');
        });
    }

    // When clicking "Save Profile", trigger client validation then show Confirmation Modal
    if (btnSaveProfile) {
        btnSaveProfile.addEventListener('click', (e) => {
            e.preventDefault();
            if (!isProfileDirty()) return;

            // Clear any prior client error on fullName
            const fullNameErr = document.getElementById('full-name-error');
            if (fullNameErr) {
                fullNameErr.textContent = '';
                setVisible(fullNameErr, false);
            }
            if (fullNameInput) fullNameInput.classList.remove('is-invalid');

            const val = fullNameInput ? fullNameInput.value.trim() : '';
            const namePattern = /^[\p{L}][\p{L}\s.'-]*$/u;

            if (!val) {
                if (fullNameErr) {
                    fullNameErr.textContent = 'Vui lòng nhập họ tên.';
                    setVisible(fullNameErr, true);
                }
                if (fullNameInput) {
                    fullNameInput.classList.add('is-invalid');
                    fullNameInput.focus();
                }
                return;
            }

            if (!namePattern.test(val)) {
                if (fullNameErr) {
                    fullNameErr.textContent = 'Họ tên bắt đầu bằng chữ cái, chỉ gồm chữ cái, khoảng trắng, dấu chấm, dấu gạch nối và dấu nháy.';
                    setVisible(fullNameErr, true);
                }
                if (fullNameInput) {
                    fullNameInput.classList.add('is-invalid');
                    fullNameInput.focus();
                }
                return;
            }

            // Trigger native HTML5 validation UI if invalid
            if (profileForm && !profileForm.checkValidity()) {
                profileForm.reportValidity();
                return;
            }

            // Open confirmation dialog
            if (confirmSaveModal && !confirmSaveModal.open) {
                confirmSaveModal.showModal();
            }
        });
    }

    // Inside confirmation dialog: Confirm Save
    if (btnConfirmSave && profileForm) {
        btnConfirmSave.addEventListener('click', () => {
            isSubmittingProfile = true;
            if (confirmSaveModal) confirmSaveModal.close();

            // Clean values before submit
            profileFields.forEach(field => { field.value = field.value.trim(); });

            profileForm.submit();
        });
    }

    // Prevent accidental tab closing / navigation if profile has unsaved edits
    window.addEventListener('beforeunload', (e) => {
        if (!isSubmittingProfile && isProfileDirty()) {
            e.preventDefault();
            e.returnValue = '';
        }
    });

    // -------------------------------------------------------------
    // 3. Change Password Modal & AJAX Submission
    // -------------------------------------------------------------
    function clearPasswordErrors() {
        if (passwordAlertBox) {
            setVisible(passwordAlertBox, false);
            passwordAlertBox.textContent = '';
        }
        ['currentPassword', 'newPassword', 'confirmPassword'].forEach(fieldId => {
            const input = document.getElementById(fieldId);
            const errSpan = document.getElementById(`${fieldId}-error`);
            if (input) {
                input.classList.remove('is-invalid');
                input.removeAttribute('aria-invalid');
            }
            if (errSpan) {
                setVisible(errSpan, false);
                errSpan.textContent = '';
            }
        });
    }

    function setPasswordFieldError(fieldId, message) {
        const input = document.getElementById(fieldId);
        const errSpan = document.getElementById(`${fieldId}-error`);
        if (input) {
            input.classList.add('is-invalid');
            input.setAttribute('aria-invalid', 'true');
        }
        if (errSpan) {
            errSpan.textContent = message;
            setVisible(errSpan, true);
        }
    }

    function showPasswordAlert(message) {
        if (passwordAlertBox) {
            passwordAlertBox.textContent = message;
            setVisible(passwordAlertBox, true);
        }
    }

    function showToast(message, type = 'success') {
        if (!toastContainer) return;

        const toast = document.createElement('div');
        toast.className = `profile-toast-message ${type === 'error' ? 'profile-toast-error' : ''}`;
        toast.setAttribute('role', 'alert');

        const body = document.createElement('div');
        body.className = 'profile-toast-body';

        const icon = document.createElement('span');
        icon.className = 'profile-toast-check-icon';
        icon.textContent = type === 'error' ? '!' : '✓';

        const text = document.createElement('span');
        text.className = 'profile-toast-text';
        text.textContent = message;

        body.appendChild(icon);
        body.appendChild(text);

        const closeBtn = document.createElement('button');
        closeBtn.className = 'profile-toast-close';
        closeBtn.type = 'button';
        closeBtn.setAttribute('aria-label', 'Đóng thông báo');
        closeBtn.innerHTML = '&times;';
        closeBtn.onclick = () => removeToast(toast);

        toast.appendChild(body);
        toast.appendChild(closeBtn);
        toastContainer.appendChild(toast);

        const timer = setTimeout(() => removeToast(toast), 4000);

        function removeToast(el) {
            clearTimeout(timer);
            el.classList.add('profile-toast-fade-out');
            setTimeout(() => el.remove(), 300);
        }
    }

    // Modal open / close handlers
    function openPasswordModal() {
        clearPasswordErrors();
        if (passwordForm) passwordForm.reset();
        if (passwordModal && !passwordModal.open) passwordModal.showModal();
    }

    function closeAllModals() {
        clearPasswordErrors();
        if (passwordForm) passwordForm.reset();
        if (passwordModal && passwordModal.open) passwordModal.close();
        if (confirmSaveModal && confirmSaveModal.open) confirmSaveModal.close();
    }

    document.querySelectorAll('[data-open-modal="changePasswordModal"]').forEach(btn =>
        btn.addEventListener('click', openPasswordModal));
    document.querySelectorAll('[data-close-modal]').forEach(btn =>
        btn.addEventListener('click', closeAllModals));

    if (passwordModal && passwordModal.dataset.openOnLoad === 'true') {
        openPasswordModal();
    }

    // Handle AJAX submission of Change Password form
    if (passwordForm) {
        passwordForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            clearPasswordErrors();

            const currentPassword = passwordForm.currentPassword ? passwordForm.currentPassword.value : '';
            const newPassword = passwordForm.newPassword ? passwordForm.newPassword.value : '';
            const confirmPassword = passwordForm.confirmPassword ? passwordForm.confirmPassword.value : '';

            // Client-side validation checks
            let hasError = false;
            let firstInvalidField = null;

            if (!currentPassword) {
                setPasswordFieldError('currentPassword', 'Vui lòng nhập mật khẩu hiện tại.');
                hasError = true;
                if (!firstInvalidField) firstInvalidField = passwordForm.currentPassword;
            }

            if (!newPassword) {
                setPasswordFieldError('newPassword', 'Vui lòng nhập mật khẩu mới.');
                hasError = true;
                if (!firstInvalidField) firstInvalidField = passwordForm.newPassword;
            } else if (newPassword.length < 8 || newPassword.length > 32) {
                setPasswordFieldError('newPassword', 'Mật khẩu mới dài 8–32 ký tự.');
                hasError = true;
                if (!firstInvalidField) firstInvalidField = passwordForm.newPassword;
            }

            if (!confirmPassword) {
                setPasswordFieldError('confirmPassword', 'Vui lòng nhập lại mật khẩu mới.');
                hasError = true;
                if (!firstInvalidField) firstInvalidField = passwordForm.confirmPassword;
            } else if (newPassword && confirmPassword !== newPassword) {
                setPasswordFieldError('confirmPassword', 'Mật khẩu nhập lại không khớp.');
                hasError = true;
                if (!firstInvalidField) firstInvalidField = passwordForm.confirmPassword;
            }

            if (hasError) {
                showPasswordAlert('Vui lòng sửa các ô được đánh dấu.');
                if (firstInvalidField) firstInvalidField.focus();
                return;
            }

            // Prepare CSRF Token
            const csrfToken = passwordForm.querySelector('input[name="_csrf"]')?.value
                || document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content')
                || 'X-CSRF-TOKEN';

            const headers = {
                'Content-Type': 'application/json; charset=UTF-8',
                'Accept': 'application/json'
            };
            if (csrfToken) {
                headers[csrfHeader] = csrfToken;
            }

            const payload = {
                currentPassword: currentPassword,
                newPassword: newPassword,
                confirmPassword: confirmPassword
            };

            if (btnSubmitPassword) {
                btnSubmitPassword.disabled = true;
                btnSubmitPassword.textContent = btnSubmitPassword.dataset.busyLabel || 'Đang lưu...';
            }

            try {
                const actionUrl = passwordForm.getAttribute('action') || '/profile/change-password';
                const response = await fetch(actionUrl, {
                    method: 'POST',
                    headers: headers,
                    body: JSON.stringify(payload)
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    passwordForm.reset();
                    clearPasswordErrors();
                    if (passwordModal) passwordModal.close();
                    showToast(data.message || 'Đã đổi mật khẩu.', 'success');
                } else {
                    showPasswordAlert(data.message || 'Chưa đổi được mật khẩu.');
                    if (data.errors) {
                        for (const [field, errorMsg] of Object.entries(data.errors)) {
                            setPasswordFieldError(field, errorMsg);
                        }
                    }
                }
            } catch (err) {
                showPasswordAlert('Lỗi kết nối mạng. Vui lòng kiểm tra kết nối rồi thử lại.');
            } finally {
                if (btnSubmitPassword) {
                    btnSubmitPassword.disabled = false;
                    btnSubmitPassword.textContent = submitPasswordLabel;
                }
            }
        });
    }

    // -------------------------------------------------------------
    // 4. Avatar upload: kiểm trước ở trình duyệt; máy chủ vẫn kiểm lại byte đầu của file
    // -------------------------------------------------------------
    const avatarForm = document.querySelector('[data-avatar-form]');
    if (avatarForm) {
        const avatarInput = avatarForm.querySelector('input[type="file"]');
        const avatarError = avatarForm.querySelector('.form-error');
        const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
        const maxBytes = 2 * 1024 * 1024;

        // Ảnh là nhãn của ô file (không còn nút "Tải ảnh lên"): chọn xong thì gửi luôn, vẫn qua bước kiểm bên dưới
        if (avatarInput) {
            avatarInput.addEventListener('change', () => {
                if (avatarInput.files && avatarInput.files.length) avatarForm.requestSubmit();
            });
        }

        avatarForm.addEventListener('submit', (e) => {
            const file = avatarInput && avatarInput.files ? avatarInput.files[0] : null;
            let message = '';
            if (!file) {
                message = 'Vui lòng chọn ảnh.';
            } else if (!allowedTypes.includes(file.type)) {
                message = 'Chỉ nhận ảnh JPEG, PNG hoặc WebP.';
            } else if (file.size > maxBytes) {
                message = 'Ảnh tối đa 2 MB.';
            }
            if (message) {
                e.preventDefault();
                if (avatarError) {
                    avatarError.textContent = message;
                    avatarError.hidden = false;
                }
                if (avatarInput) avatarInput.setAttribute('aria-invalid', 'true');
            }
        });
    }

    // Initialize profile form button state on load
    updateProfileFormState();
});
