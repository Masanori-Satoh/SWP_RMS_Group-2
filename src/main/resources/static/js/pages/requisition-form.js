(() => {
    'use strict';

    const form = document.getElementById('requisition-form');
    if (!form) {
        return;
    }

    const today = () => {
        const now = new Date();
        const pad = number => String(number).padStart(2, '0');
        return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
    };

    const FIELD_RULES = [
        {
            id: 'title', label: 'Tên vị trí tuyển dụng', section: 'rqf-position', requiredForDraft: true,
            errorOf: value => value.length < 3 ? 'Tên vị trí tuyển dụng phải có ít nhất 3 ký tự.' : ''
        },
        {
            id: 'numberOfPositions', label: 'Số lượng', section: 'rqf-position', requiredForDraft: true,
            errorOf: value => {
                const input = document.getElementById('numberOfPositions');
                const min = Number(input.min) || 1;
                const max = Number(input.max) || 30;
                const count = Number(value);
                return Number.isInteger(count) && count >= min && count <= max
                    ? '' : `Số lượng cần tuyển phải từ ${min} đến ${max} người.`;
            }
        },
        { id: 'employmentType', label: 'Hình thức làm việc', section: 'rqf-work', requiredForDraft: true },
        { id: 'workModel', label: 'Mô hình làm việc', section: 'rqf-work', requiredForDraft: true },
        { id: 'workLocation', label: 'Địa điểm làm việc', section: 'rqf-work', requiredForSubmit: true },
        { id: 'reasonForHiring', label: 'Lý do tuyển dụng', section: 'rqf-content', requiredForSubmit: true },
        { id: 'jobDescription', label: 'Mô tả công việc', section: 'rqf-content', requiredForSubmit: true },
        { id: 'requirementDetails', label: 'Yêu cầu ứng viên', section: 'rqf-content', requiredForSubmit: true }
    ];

    if (document.getElementById('departmentId')?.tagName === 'SELECT') {
        FIELD_RULES.splice(1, 0, { id: 'departmentId', label: 'Phòng ban tuyển dụng', section: 'rqf-position', requiredForDraft: true });
    }

    const workLocation = document.getElementById('workLocation');
    const workModel = document.getElementById('workModel');
    function isRemote() {
        return workModel.value === 'Remote';
    }

    function syncWorkLocation() {
        const remote = isRemote();
        workLocation.disabled = remote;
        if (remote) {
            workLocation.value = '';
            showError('workLocation', '');
        }
        document.getElementById('workLocation-required').hidden = remote;
        refreshChecklist();
    }

    function readFieldValue(rule) {
        const input = document.getElementById(rule.id);
        return input ? input.value.trim() : '';
    }

    function showError(fieldId, message) {
        const input = document.getElementById(fieldId);
        const errorEl = document.getElementById(fieldId + '-error');
        if (input) {
            input.toggleAttribute('aria-invalid', Boolean(message));
            input.classList.toggle('is-invalid', Boolean(message));
        }
        if (errorEl) {
            errorEl.textContent = message;
        }
    }

    function validateField(rule, action) {
        if (rule.id === 'workLocation' && isRemote()) {
            showError(rule.id, '');
            return true;
        }
        const value = readFieldValue(rule);
        const required = rule.requiredForDraft || (action === 'submit' && rule.requiredForSubmit);
        let message = '';

        if (required && !value) {
            if (rule.id === 'title') {
                message = 'Tên vị trí không được để trống.';
            } else if (rule.id === 'departmentId') {
                message = 'Vui lòng chọn phòng ban đang hoạt động.';
            } else if (rule.id === 'workLocation') {
                message = 'Địa điểm làm việc không được để trống.';
            } else if (rule.id === 'numberOfPositions') {
                message = 'Vui lòng nhập số lượng cần tuyển.';
            } else {
                message = 'Vui lòng nhập trường này.';
            }
        } else if (value) {
            message = (rule.errorOf ? rule.errorOf(value) : '')
                || (action === 'submit' && rule.submitErrorOf ? rule.submitErrorOf(value) : '');
        }
        showError(rule.id, message);
        return !message;
    }

    function formatCurrencyInput(input) {
        if (!input) return;
        const raw = input.value;
        if (!raw) return;

        const cursor = input.selectionStart || 0;
        const digitsBeforeCursor = raw.slice(0, cursor).replace(/\D/g, '').length;

        const clean = raw.replace(/\D/g, '');
        if (!clean) {
            input.value = '';
            return;
        }

        const formatted = clean.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
        input.value = formatted;

        let newCursor = 0;
        let digitCount = 0;
        for (let i = 0; i < formatted.length; i++) {
            if (/\d/.test(formatted[i])) {
                digitCount++;
            }
            if (digitCount === digitsBeforeCursor) {
                newCursor = i + 1;
                break;
            }
        }
        if (digitsBeforeCursor === 0) newCursor = 0;
        input.setSelectionRange(newCursor, newCursor);
    }

    function parseSalary(text) {
        if (!text) return null;
        const cleaned = String(text).replace(/\./g, '').trim();
        if (!cleaned) return null;
        const num = Number(cleaned);
        return isNaN(num) ? null : num;
    }

    function validateNumbers() {
        let valid = true;

        const minSalaryInput = document.getElementById('minSalary');
        const maxSalaryInput = document.getElementById('maxSalary');
        const minVal = parseSalary(minSalaryInput?.value);
        const maxVal = parseSalary(maxSalaryInput?.value);

        let minSalaryMessage = '';
        let maxSalaryMessage = '';

        const SALARY_MAX = 1000000000;
        const salaryMessage = value => value === null ? ''
            : value <= 0 ? 'Mức lương phải lớn hơn 0.'
                : value > SALARY_MAX ? 'Mức lương không được vượt quá 1.000.000.000 VND/tháng.' : '';
        minSalaryMessage = salaryMessage(minVal);
        maxSalaryMessage = salaryMessage(maxVal);

        if (minVal > 0 && maxVal > 0) {
            if (maxVal < minVal) {
                maxSalaryMessage = 'Mức lương tối đa phải lớn hơn hoặc bằng mức lương tối thiểu.';
            }
        }

        showError('minSalary', minSalaryMessage);
        showError('maxSalary', maxSalaryMessage);

        const salariesOk = !minSalaryMessage && !maxSalaryMessage;
        return valid && salariesOk;
    }

    FIELD_RULES.forEach(rule => {
        const target = document.getElementById(rule.id);
        target?.addEventListener('change', () => validateField(rule, 'draft'));
        target?.addEventListener('blur', () => validateField(rule, 'submit'), true);
    });
    workModel.addEventListener('change', syncWorkLocation);

    ['minSalary', 'maxSalary'].forEach(id => {
        const input = document.getElementById(id);
        if (input) {
            if (input.value) {
                formatCurrencyInput(input);
            }
            input.addEventListener('input', () => {
                formatCurrencyInput(input);
                validateNumbers();
            });
            input.addEventListener('blur', validateNumbers);
        }
    });

    form.querySelectorAll('[data-step]').forEach(button => button.addEventListener('click', () => {
        const input = document.getElementById(button.dataset.stepTarget);
        if (!input) return;
        const min = input.min === '' ? 1 : Number(input.min);
        const max = input.max === '' ? Infinity : Number(input.max);
        if (input.value === '' && input.dataset.stepStart) {
            input.value = input.dataset.stepStart;
        } else {
            const current = input.value === '' ? min - Number(button.dataset.step) : Number(input.value);
            input.value = Math.min(max, Math.max(min, current + Number(button.dataset.step)));
        }
        input.dispatchEvent(new Event('input', { bubbles: true }));
    }));

    form.querySelectorAll('textarea[maxlength]').forEach(textarea => {
        const counter = document.getElementById(textarea.id + '-count');
        if (!counter) return;
        const max = textarea.getAttribute('maxlength') || '2000';
        const updateCounter = () => {
            counter.textContent = `${textarea.value.length}/${max}`;
        };
        textarea.addEventListener('input', updateCounter);
        textarea.addEventListener('keyup', updateCounter);
        textarea.addEventListener('change', updateCounter);
        textarea.addEventListener('paste', () => setTimeout(updateCounter, 0));
        updateCounter();
    });

    function refreshChecklist() {
        const isFieldDone = rule => {
            const value = readFieldValue(rule);
            if (!value) return false;
            if (rule.errorOf) return !rule.errorOf(value);
            return true;
        };
        const items = FIELD_RULES.filter(rule => rule.id !== 'workLocation' || !isRemote())
            .map(rule => ({ label: rule.label, target: rule.id, done: isFieldDone(rule) }));

        const doneCount = items.filter(item => item.done).length;
        document.getElementById('rqf-progress').textContent = `${doneCount}/${items.length} mục bắt buộc đã xong`;

        const list = document.getElementById('rqf-checklist');
        list.replaceChildren(...items.map(item => {
            const li = document.createElement('li');
            li.className = item.done ? 'is-done' : '';
            const link = document.createElement('a');
            link.href = '#' + item.target;
            const dot = document.createElement('span');
            dot.className = 'rqf-dot';
            dot.setAttribute('aria-hidden', 'true');
            dot.innerHTML = '<svg viewBox="0 0 16 16" width="10" height="10"><path d="M3.5 8.5l3 3 6-7" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
            link.append(dot, item.label);
            li.appendChild(link);
            return li;
        }));
    }

    form.addEventListener('input', refreshChecklist);
    form.addEventListener('change', refreshChecklist);

    // Đợt tuyển dụng không bắt buộc (trống = đợt 1), nhưng đã nhập thì phải là số nguyên trong khoảng min..max.
    const roundInput = document.getElementById('recruitmentRound');
    function validateRound() {
        if (!roundInput) return true;
        const value = roundInput.value.trim();
        const round = Number(value);
        const ok = roundInput.validity.valid && (!value || (Number.isInteger(round)
            && round >= Number(roundInput.min || 1) && round <= Number(roundInput.max || 99)));
        showError('recruitmentRound', ok ? '' : `Đợt tuyển dụng phải từ ${roundInput.min || 1} đến ${roundInput.max || 99}.`);
        return ok;
    }
    roundInput?.addEventListener('input', validateRound);

    // Chọn "Khác" ở thời gian thử việc thì phải nhập nội dung.
    function validateProbation() {
        const choice = document.getElementById('probationChoice');
        const input = document.getElementById('probationPeriod');
        const missing = choice?.value === '__other__' && !input?.value.trim();
        showError('probationPeriod', missing ? 'Vui lòng nhập thời gian thử việc.' : '');
        return !missing;
    }

    function validateAll(action) {
        const fieldsOk = FIELD_RULES.map(rule => validateField(rule, action)).every(Boolean);
        const numbersOk = [validateNumbers(), validateRound(), validateProbation()].every(Boolean);
        if (!(fieldsOk && numbersOk)) {
            const firstError = form.querySelector('[aria-invalid="true"], .is-invalid input');
            firstError?.scrollIntoView({ behavior: 'smooth', block: 'center' });
            firstError?.focus?.({ preventScroll: true });
            return false;
        }
        return true;
    }

    const buttons = document.getElementById('rqf-buttons');
    const confirmBox = document.getElementById('rqf-confirm');

    document.getElementById('rqf-open-confirm').addEventListener('click', () => {
        if (validateAll('submit')) {
            buttons.hidden = true;
            confirmBox.hidden = false;
            confirmBox.querySelector('button[type=submit]').focus();
        }
    });

    document.getElementById('rqf-cancel-confirm').addEventListener('click', () => {
        confirmBox.hidden = true;
        buttons.hidden = false;
    });


    let submitting = false;
    form.addEventListener('submit', event => {
        if (submitting) {
            event.preventDefault();
            return;
        }

        const action = event.submitter?.value === 'submit' ? 'submit' : 'draft';
        if (!validateAll(action)) {
            event.preventDefault();
            confirmBox.hidden = true;
            buttons.hidden = false;
            return;
        }
        submitting = true;
        setTimeout(() => form.querySelectorAll('button[type=submit]').forEach(button => { button.disabled = true; }), 0);
    });
    window.addEventListener('pageshow', () => {
        submitting = false;
        form.querySelectorAll('button[type=submit]').forEach(button => { button.disabled = false; });
    });

    // Thời gian thử việc: chọn mẫu thì ghi thẳng vào ô probationPeriod (ẩn); "Khác" thì hiện ô để nhập tay.
    const probationChoice = document.getElementById('probationChoice');
    const probationInput = document.getElementById('probationPeriod');
    if (probationChoice && probationInput) {
        const syncProbation = userChanged => {
            const isOther = probationChoice.value === '__other__';
            probationInput.hidden = !isOther;
            if (!isOther) {
                probationInput.value = probationChoice.value;
            } else if (userChanged) {
                probationInput.value = '';
                probationInput.focus();
            }
        };
        probationChoice.addEventListener('change', () => syncProbation(true));
        syncProbation(false);
    }

    syncWorkLocation();
    document.getElementById('validation-summary')?.focus();
})();
