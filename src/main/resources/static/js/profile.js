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

    // -------------------------------------------------------------
    // 2. Elements & Variables: Profile Form & Confirmation
    // -------------------------------------------------------------
    const profileForm = document.getElementById('profileForm');
    const fullNameInput = document.getElementById('full-name');
    const phoneInput = document.getElementById('phone-number');
    const avatarInput = document.getElementById('avatar-url');
    const btnSaveProfile = document.getElementById('btnSaveProfile');
    const btnDiscardProfile = document.getElementById('btnDiscardProfile');
    const confirmSaveModal = document.getElementById('confirmSaveModal');
    const btnConfirmSave = document.getElementById('btnConfirmSave');

    let isSubmittingProfile = false;

    // Snapshot of initial values (trimmed for reliable comparison)
    const initialProfileValues = {
        fullName: fullNameInput ? fullNameInput.value.trim() : '',
        phoneNumber: phoneInput ? phoneInput.value.trim() : '',
        avatarUrl: avatarInput ? avatarInput.value.trim() : ''
    };

    // Helper: Determine if profile has real changes compared to initial state
    function isProfileDirty() {
        if (!fullNameInput) return false;
        const currentFullName = fullNameInput.value.trim();
        const currentPhone = phoneInput ? phoneInput.value.trim() : '';
        const currentAvatar = avatarInput ? avatarInput.value.trim() : '';

        return (currentFullName !== initialProfileValues.fullName)
            || (currentPhone !== initialProfileValues.phoneNumber)
            || (currentAvatar !== initialProfileValues.avatarUrl);
    }

    // Update UI state based on dirty status
    function updateProfileFormState() {
        const dirty = isProfileDirty();
        if (btnSaveProfile) {
            btnSaveProfile.disabled = !dirty;
        }
        if (btnDiscardProfile) {
            btnDiscardProfile.style.display = dirty ? 'inline-block' : 'none';
        }
    }

    // Attach dirty-checking listeners to profile inputs
    [fullNameInput, phoneInput, avatarInput].forEach(input => {
        if (!input) return;
        input.addEventListener('input', updateProfileFormState);
        input.addEventListener('change', updateProfileFormState);

        // When leaving input (blur), auto-clean accidental whitespace if value equals initial
        input.addEventListener('blur', () => {
            if (input === fullNameInput && input.value.trim() === initialProfileValues.fullName) {
                input.value = initialProfileValues.fullName;
            }
            if (input === phoneInput && input.value.trim() === initialProfileValues.phoneNumber) {
                input.value = initialProfileValues.phoneNumber;
            }
            if (input === avatarInput && input.value.trim() === initialProfileValues.avatarUrl) {
                input.value = initialProfileValues.avatarUrl;
            }
            updateProfileFormState();
        });
    });

    // Discard changes button
    if (btnDiscardProfile) {
        btnDiscardProfile.addEventListener('click', () => {
            if (fullNameInput) fullNameInput.value = initialProfileValues.fullName;
            if (phoneInput) phoneInput.value = initialProfileValues.phoneNumber;
            if (avatarInput) avatarInput.value = initialProfileValues.avatarUrl;
            updateProfileFormState();
            showToast('Changes discarded. Profile restored to original values.');
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
                fullNameErr.style.display = 'none';
            }
            if (fullNameInput) fullNameInput.classList.remove('is-invalid');

            const val = fullNameInput ? fullNameInput.value.trim() : '';
            const namePattern = /^[\p{L}][\p{L}\s.'-]*$/u;

            if (!val) {
                if (fullNameErr) {
                    fullNameErr.textContent = 'Full name is required.';
                    fullNameErr.style.display = 'block';
                }
                if (fullNameInput) {
                    fullNameInput.classList.add('is-invalid');
                    fullNameInput.focus();
                }
                return;
            }

            if (!namePattern.test(val)) {
                if (fullNameErr) {
                    fullNameErr.textContent = 'Full name must start with a letter and can only contain letters, spaces, hyphens, and apostrophes.';
                    fullNameErr.style.display = 'block';
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
            if (fullNameInput) fullNameInput.value = fullNameInput.value.trim();
            if (phoneInput) phoneInput.value = phoneInput.value.trim();
            if (avatarInput) avatarInput.value = avatarInput.value.trim();

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
            passwordAlertBox.style.display = 'none';
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
                errSpan.style.display = 'none';
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
            errSpan.style.display = 'block';
        }
    }

    function showPasswordAlert(message) {
        if (passwordAlertBox) {
            passwordAlertBox.textContent = message;
            passwordAlertBox.style.display = 'block';
        }
    }

    function showToast(message, type = 'success') {
        if (!toastContainer) return;

        const toast = document.createElement('div');
        toast.className = `toast-message ${type === 'error' ? 'toast-error' : ''}`;
        toast.setAttribute('role', 'alert');

        const body = document.createElement('div');
        body.className = 'toast-body';

        const icon = document.createElement('span');
        icon.className = 'toast-check-icon';
        icon.textContent = type === 'error' ? '!' : '✓';

        const text = document.createElement('span');
        text.className = 'toast-text';
        text.textContent = message;

        body.appendChild(icon);
        body.appendChild(text);

        const closeBtn = document.createElement('button');
        closeBtn.className = 'toast-close';
        closeBtn.type = 'button';
        closeBtn.setAttribute('aria-label', 'Close toast');
        closeBtn.innerHTML = '&times;';
        closeBtn.onclick = () => removeToast(toast);

        toast.appendChild(body);
        toast.appendChild(closeBtn);
        toastContainer.appendChild(toast);

        const timer = setTimeout(() => removeToast(toast), 4000);

        function removeToast(el) {
            clearTimeout(timer);
            el.classList.add('toast-fade-out');
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
                setPasswordFieldError('currentPassword', 'Enter your current password.');
                hasError = true;
                if (!firstInvalidField) firstInvalidField = passwordForm.currentPassword;
            }

            if (!newPassword) {
                setPasswordFieldError('newPassword', 'Enter a new password.');
                hasError = true;
                if (!firstInvalidField) firstInvalidField = passwordForm.newPassword;
            } else if (newPassword.length < 8 || newPassword.length > 32) {
                setPasswordFieldError('newPassword', 'Password must contain 8–32 characters.');
                hasError = true;
                if (!firstInvalidField) firstInvalidField = passwordForm.newPassword;
            }

            if (!confirmPassword) {
                setPasswordFieldError('confirmPassword', 'Confirm your new password.');
                hasError = true;
                if (!firstInvalidField) firstInvalidField = passwordForm.confirmPassword;
            } else if (newPassword && confirmPassword !== newPassword) {
                setPasswordFieldError('confirmPassword', 'Passwords do not match.');
                hasError = true;
                if (!firstInvalidField) firstInvalidField = passwordForm.confirmPassword;
            }

            if (hasError) {
                showPasswordAlert('Please review the highlighted errors.');
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
                btnSubmitPassword.textContent = 'Updating...';
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
                    showToast(data.message || 'Password changed successfully.', 'success');
                } else {
                    showPasswordAlert(data.message || 'Unable to update password.');
                    if (data.errors) {
                        for (const [field, errorMsg] of Object.entries(data.errors)) {
                            setPasswordFieldError(field, errorMsg);
                        }
                    }
                }
            } catch (err) {
                showPasswordAlert('A network error occurred. Please check your connection and try again.');
            } finally {
                if (btnSubmitPassword) {
                    btnSubmitPassword.disabled = false;
                    btnSubmitPassword.textContent = 'Update Password';
                }
            }
        });
    }

    // Initialize profile form button state on load
    updateProfileFormState();
});
