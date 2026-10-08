/**
 * =============================================================================
 * JOB-POSTING-CREATE.JS
 * Client-side behaviors & instant validation for Job Posting Create / Update form.
 * =============================================================================
 */

document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('jobPostingForm');
    const formActionInput = document.getElementById('formActionInput');
    const btnSaveDraft = document.getElementById('btnSaveDraft');
    const btnPublish = document.getElementById('btnPublish');

    // Form inputs & errors
    const titleInput = document.getElementById('postingTitle');
    const titleError = document.getElementById('postingTitle-error');

    const descInput = document.getElementById('jobDescription');
    const descError = document.getElementById('jobDescription-error');

    const reqsInput = document.getElementById('jobRequirements');
    const reqsError = document.getElementById('jobRequirements-error');

    const deadlineInput = document.getElementById('applicationDeadline');
    const deadlineError = document.getElementById('applicationDeadline-error');
    const continuousCheckbox = document.getElementById('isContinuousRecruitment');
    const deadlineStar = document.getElementById('deadlineStar');

    // Helper: show/hide field error
    function setError(input, errorEl, message) {
        if (!input || !errorEl) return;
        if (message) {
            input.classList.add('is-invalid');
            errorEl.textContent = message;
            errorEl.classList.add('show');
        } else {
            input.classList.remove('is-invalid');
            errorEl.textContent = '';
            errorEl.classList.remove('show');
        }
    }

    // Instant validation for Title
    function validateTitle() {
        if (!titleInput) return true;
        const val = titleInput.value.trim();
        if (!val) {
            setError(titleInput, titleError, 'Tiêu đề tin tuyển dụng không được để trống.');
            return false;
        }
        if (val.length > 200) {
            setError(titleInput, titleError, 'Tiêu đề tin tuyển dụng không được vượt quá 200 ký tự.');
            return false;
        }
        setError(titleInput, titleError, '');
        return true;
    }

    // Instant validation for Description
    function validateDescription() {
        if (!descInput) return true;
        const val = descInput.value.trim();
        if (!val) {
            setError(descInput, descError, 'Mô tả công việc không được để trống.');
            return false;
        }
        setError(descInput, descError, '');
        return true;
    }

    // Instant validation for Requirements
    function validateRequirements() {
        if (!reqsInput) return true;
        const val = reqsInput.value.trim();
        if (!val) {
            setError(reqsInput, reqsError, 'Yêu cầu ứng viên không được để trống.');
            return false;
        }
        setError(reqsInput, reqsError, '');
        return true;
    }

    // Instant validation for Application Deadline
    function validateDeadline() {
        if (!deadlineInput) return true;
        if (continuousCheckbox && continuousCheckbox.checked) {
            setError(deadlineInput, deadlineError, '');
            return true;
        }
        const val = deadlineInput.value;
        if (!val) {
            setError(deadlineInput, deadlineError, 'Vui lòng chọn hạn nộp hồ sơ hoặc tích chọn Tuyển dụng liên tục.');
            return false;
        }
        const selectedDate = new Date(val);
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        if (selectedDate <= today) {
            setError(deadlineInput, deadlineError, 'Hạn nộp hồ sơ phải sau ngày hiện tại.');
            return false;
        }
        setError(deadlineInput, deadlineError, '');
        return true;
    }

    // Checkbox Tuyển liên tục handler
    if (continuousCheckbox && deadlineInput) {
        function updateContinuousState() {
            if (continuousCheckbox.checked) {
                deadlineInput.disabled = true;
                deadlineInput.value = '';
                if (deadlineStar) deadlineStar.style.display = 'none';
                setError(deadlineInput, deadlineError, '');
            } else {
                deadlineInput.disabled = false;
                if (deadlineStar) deadlineStar.style.display = 'inline';
            }
        }

        continuousCheckbox.addEventListener('change', updateContinuousState);
        updateContinuousState();
    }

    // Event listeners for instant typing feedback
    if (titleInput) {
        titleInput.addEventListener('input', validateTitle);
        titleInput.addEventListener('blur', validateTitle);
    }
    if (descInput) {
        descInput.addEventListener('input', validateDescription);
        descInput.addEventListener('blur', validateDescription);
    }
    if (reqsInput) {
        reqsInput.addEventListener('input', validateRequirements);
        reqsInput.addEventListener('blur', validateRequirements);
    }
    if (deadlineInput) {
        deadlineInput.addEventListener('change', validateDeadline);
        deadlineInput.addEventListener('blur', validateDeadline);
    }

    // Draft submit
    if (btnSaveDraft && form && formActionInput) {
        btnSaveDraft.addEventListener('click', function () {
            formActionInput.value = 'draft';
            if (!validateTitle()) {
                titleInput.focus();
                return;
            }
            form.submit();
        });
    }

    // Publish submit
    if (btnPublish && form && formActionInput) {
        btnPublish.addEventListener('click', function () {
            formActionInput.value = 'publish';
            const isTitleValid = validateTitle();
            const isDescValid = validateDescription();
            const isReqsValid = validateRequirements();
            const isDeadlineValid = validateDeadline();

            if (!isTitleValid || !isDescValid || !isReqsValid || !isDeadlineValid) {
                // Focus on first invalid field
                if (!isTitleValid) titleInput.focus();
                else if (!isDescValid) descInput.focus();
                else if (!isReqsValid) reqsInput.focus();
                else if (!isDeadlineValid) deadlineInput.focus();
                return;
            }
            form.submit();
        });
    }
});
