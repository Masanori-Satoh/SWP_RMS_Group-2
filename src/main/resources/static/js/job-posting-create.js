/**
 * =============================================================================
 * JOB-POSTING-CREATE.JS
 * Module JavaScript xử lý biểu mẫu tạo tin tuyển dụng nội bộ
 * =============================================================================
 */

document.addEventListener('DOMContentLoaded', function () {
    // Ngưỡng hằng số cấu hình nghiệp vụ
    const MIN_SALARY_THRESHOLD = 1000000; // Mức lương tối thiểu 1.000.000 VNĐ
    const MAX_PROBATION_DAYS = 180;        // Thời gian thử việc tối đa 180 ngày

    let currentStep = 1;
    const totalSteps = 5;

    // Các phần tử DOM cốt lõi
    const form = document.getElementById('jobPostingForm');
    const stepItems = document.querySelectorAll('.jp-step-item');
    const stepDividers = document.querySelectorAll('.jp-step-divider');
    const stepPanels = document.querySelectorAll('.jp-step-panel');

    // Các ô nhập liệu Bước 1 (Thông tin cơ bản)
    const titleInput = document.getElementById('postingTitle');
    const titleError = document.getElementById('titleError');

    const positionsInput = document.getElementById('numberOfPositions');
    const positionsControl = document.getElementById('positionsControl');
    const positionsError = document.getElementById('positionsError');
    const btnPositionsMinus = document.getElementById('btnPositionsMinus');
    const btnPositionsPlus = document.getElementById('btnPositionsPlus');

    const locationInput = document.getElementById('workLocation');
    const locationError = document.getElementById('locationError');

    const minSalaryInput = document.getElementById('minSalary');
    const maxSalaryInput = document.getElementById('maxSalary');
    const minSalaryError = document.getElementById('minSalaryError');
    const maxSalaryError = document.getElementById('maxSalaryError');

    const probationInput = document.getElementById('probationDuration');
    const probationError = document.getElementById('probationError');

    const deadlineInput = document.getElementById('recruitmentDeadline');
    const deadlineError = document.getElementById('deadlineError');
    const continuousCheckbox = document.getElementById('isContinuousRecruitment');

    // Các ô nhập liệu Bước 2 (Mô tả công việc)
    const jdInput = document.getElementById('jobDescription');
    const jdError = document.getElementById('jdError');

    // Các ô nhập liệu Bước 3 (Yêu cầu ứng viên)
    const reqsInput = document.getElementById('jobRequirements');
    const reqsError = document.getElementById('reqsError');

    /* -------------------------------------------------------------------------
     * 1. CÁC HÀM KIỂM TRA HỢP LỆ THEO THỜI GIAN THỰC (INSTANT VALIDATION)
     * ------------------------------------------------------------------------- */

    /**
     * Kiểm tra số lượng cần tuyển: phải là số nguyên >= 1
     */
    function validatePositions() {
        if (!positionsInput) return true;
        const val = parseInt(positionsInput.value, 10);
        if (isNaN(val) || val < 1) {
            positionsControl.classList.add('is-invalid');
            positionsError.textContent = 'Số lượng cần tuyển không được nhỏ hơn 1';
            positionsError.classList.add('show');
            return false;
        } else {
            positionsControl.classList.remove('is-invalid');
            positionsError.classList.remove('show');
            return true;
        }
    }

    /**
     * Kiểm tra khoảng mức lương:
     * - Cả min và max (nếu nhập) phải >= 1.000.000 VNĐ
     * - Nếu nhập cả hai, mức lương Đến phải lớn hơn mức lương Từ
     */
    function validateSalary() {
        let isValid = true;
        const minVal = minSalaryInput && minSalaryInput.value.trim() !== '' ? parseFloat(minSalaryInput.value.replace(/,/g, '')) : null;
        const maxVal = maxSalaryInput && maxSalaryInput.value.trim() !== '' ? parseFloat(maxSalaryInput.value.replace(/,/g, '')) : null;

        // Reset errors
        if (minSalaryInput) minSalaryInput.classList.remove('is-invalid');
        if (minSalaryError) minSalaryError.classList.remove('show');
        if (maxSalaryInput) maxSalaryInput.classList.remove('is-invalid');
        if (maxSalaryError) maxSalaryError.classList.remove('show');

        // Check Min Salary
        if (minVal !== null) {
            if (minVal < MIN_SALARY_THRESHOLD) {
                minSalaryInput.classList.add('is-invalid');
                minSalaryError.textContent = 'Mức lương tối thiểu từ 1,000,000 VNĐ';
                minSalaryError.classList.add('show');
                isValid = false;
            }
        }

        // Check Max Salary
        if (maxVal !== null) {
            if (maxVal < MIN_SALARY_THRESHOLD) {
                maxSalaryInput.classList.add('is-invalid');
                maxSalaryError.textContent = 'Mức lương tối thiểu từ 1,000,000 VNĐ';
                maxSalaryError.classList.add('show');
                isValid = false;
            }
        }

        // Check Max >= Min
        if (minVal !== null && maxVal !== null && isValid) {
            if (maxVal <= minVal) {
                maxSalaryInput.classList.add('is-invalid');
                maxSalaryError.textContent = 'Giá trị Đến phải lớn hơn giá trị Từ';
                maxSalaryError.classList.add('show');
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Kiểm tra thời gian thử việc: không để trống, phải là số nguyên từ 1 đến 180 ngày
     */
    function validateProbation() {
        if (!probationInput) return true;
        const raw = probationInput.value.trim();
        if (raw === '') {
            probationInput.classList.add('is-invalid');
            probationError.textContent = 'Thời gian thử việc không được để trống.';
            probationError.classList.add('show');
            return false;
        }

        const days = parseInt(raw, 10);
        if (isNaN(days) || days <= 0 || days > MAX_PROBATION_DAYS) {
            probationInput.classList.add('is-invalid');
            probationError.textContent = 'Thời gian thử việc phải là số ngày nguyên trong khoảng 1-180.';
            probationError.classList.add('show');
            return false;
        }

        probationInput.classList.remove('is-invalid');
        probationError.classList.remove('show');
        return true;
    }

    let savedDeadlineValue = deadlineInput ? deadlineInput.value : '';

    /**
     * Bật/tắt chế độ Tuyển dụng liên tục:
     * Nếu bật: khóa ô chọn ngày, lưu giá trị cũ và hiển thị placeholder 'Chọn thời gian'
     * Nếu tắt: khôi phục ô chọn ngày date picker và giá trị trước đó
     */
    function toggleContinuousRecruitment() {
        if (!deadlineInput) return;
        const isContinuous = continuousCheckbox && continuousCheckbox.checked;

        if (isContinuous) {
            // Lưu lại ngày đã chọn (nếu có) trước khi xóa
            if (deadlineInput.type === 'date' && deadlineInput.value) {
                savedDeadlineValue = deadlineInput.value;
            }
            // Chuyển sang text để không hiển thị ngày dd/mm/yyyy mà hiển thị placeholder "Chọn thời gian"
            deadlineInput.type = 'text';
            deadlineInput.value = '';
            deadlineInput.placeholder = 'Chọn thời gian';
            deadlineInput.disabled = true;
            deadlineInput.classList.add('jp-input-disabled-continuous');
            deadlineInput.classList.remove('is-invalid');
            if (deadlineError) deadlineError.classList.remove('show');
        } else {
            deadlineInput.type = 'date';
            deadlineInput.placeholder = '';
            deadlineInput.disabled = false;
            deadlineInput.classList.remove('jp-input-disabled-continuous');
            if (savedDeadlineValue) {
                deadlineInput.value = savedDeadlineValue;
            }
        }
    }

    /**
     * Kiểm tra thời gian hạn nộp hồ sơ tuyển dụng
     */
    function validateDeadline() {
        if (!deadlineInput) return true;
        const isContinuous = continuousCheckbox && continuousCheckbox.checked;

        if (isContinuous) {
            deadlineInput.classList.remove('is-invalid');
            if (deadlineError) deadlineError.classList.remove('show');
            return true;
        } else {
            if (!deadlineInput.value) {
                deadlineInput.classList.add('is-invalid');
                if (deadlineError) {
                    deadlineError.textContent = 'Thời gian cần tuyển xong không được để trống';
                    deadlineError.classList.add('show');
                }
                return false;
            } else {
                deadlineInput.classList.remove('is-invalid');
                if (deadlineError) deadlineError.classList.remove('show');
                return true;
            }
        }
    }

    function validateTitle() {
        if (!titleInput) return true;
        if (!titleInput.value.trim()) {
            titleInput.classList.add('is-invalid');
            titleError.textContent = 'Vị trí tuyển dụng không được để trống.';
            titleError.classList.add('show');
            return false;
        }
        titleInput.classList.remove('is-invalid');
        titleError.classList.remove('show');
        return true;
    }

    function validateLocation() {
        if (!locationInput) return true;
        if (!locationInput.value.trim()) {
            locationInput.classList.add('is-invalid');
            locationError.textContent = 'Địa điểm làm việc không được để trống.';
            locationError.classList.add('show');
            return false;
        }
        locationInput.classList.remove('is-invalid');
        locationError.classList.remove('show');
        return true;
    }

    function validateStep1() {
        const vTitle = validateTitle();
        const vPositions = validatePositions();
        const vLocation = validateLocation();
        const vSalary = validateSalary();
        const vProbation = validateProbation();
        const vDeadline = validateDeadline();
        return vTitle && vPositions && vLocation && vSalary && vProbation && vDeadline;
    }

    function validateStep2() {
        if (!jdInput) return true;
        if (!jdInput.value.trim()) {
            jdInput.classList.add('is-invalid');
            jdError.textContent = 'Mô tả công việc không được để trống.';
            jdError.classList.add('show');
            return false;
        }
        jdInput.classList.remove('is-invalid');
        jdError.classList.remove('show');
        return true;
    }

    function validateStep3() {
        if (!reqsInput) return true;
        if (!reqsInput.value.trim()) {
            reqsInput.classList.add('is-invalid');
            reqsError.textContent = 'Yêu cầu ứng viên không được để trống.';
            reqsError.classList.add('show');
            return false;
        }
        reqsInput.classList.remove('is-invalid');
        reqsError.classList.remove('show');
        return true;
    }

    /* -------------------------------------------------------------------------
     * 2. LẮNG NGHE SỰ KIỆN ĐỂ VALIDATE TỨC THÌ (REAL-TIME EVENT LISTENERS)
     * ------------------------------------------------------------------------- */

    // Nút tăng / giảm số lượng tuyển dụng [-] [+]
    if (btnPositionsMinus) {
        btnPositionsMinus.addEventListener('click', function () {
            let val = parseInt(positionsInput.value, 10) || 1;
            positionsInput.value = Math.max(0, val - 1);
            validatePositions();
        });
    }

    if (btnPositionsPlus) {
        btnPositionsPlus.addEventListener('click', function () {
            let val = parseInt(positionsInput.value, 10) || 0;
            positionsInput.value = val + 1;
            validatePositions();
        });
    }

    if (positionsInput) {
        positionsInput.addEventListener('input', validatePositions);
    }

    // Salary Input Events
    if (minSalaryInput) {
        minSalaryInput.addEventListener('input', validateSalary);
        minSalaryInput.addEventListener('blur', validateSalary);
    }
    if (maxSalaryInput) {
        maxSalaryInput.addEventListener('input', validateSalary);
        maxSalaryInput.addEventListener('blur', validateSalary);
    }

    // Probation Input Events
    if (probationInput) {
        probationInput.addEventListener('input', validateProbation);
        probationInput.addEventListener('blur', validateProbation);
    }

    // Deadline & Continuous Recruitment Checkbox
    if (continuousCheckbox) {
        continuousCheckbox.addEventListener('change', function () {
            toggleContinuousRecruitment();
            validateDeadline();
        });
    }

    if (deadlineInput) {
        deadlineInput.addEventListener('change', validateDeadline);
        deadlineInput.addEventListener('blur', validateDeadline);
    }

    // Title & Location
    if (titleInput) {
        titleInput.addEventListener('input', validateTitle);
        titleInput.addEventListener('blur', validateTitle);
    }

    if (locationInput) {
        locationInput.addEventListener('input', validateLocation);
        locationInput.addEventListener('blur', validateLocation);
    }

    // Step 2 & 3 textareas
    if (jdInput) {
        jdInput.addEventListener('input', validateStep2);
    }
    if (reqsInput) {
        reqsInput.addEventListener('input', validateStep3);
    }

    /* -------------------------------------------------------------------------
     * 3. ĐIỀU HƯỚNG CÁC BƯỚC WIZARD STEPPER (STEPPER NAVIGATION)
     * ------------------------------------------------------------------------- */

    /**
     * Chuyển đổi giữa các bước nhập liệu
     * Tự động validate các bước trước trước khi cho phép tiến tới bước tiếp theo
     * @param {number} targetStep Bước đích muốn chuyển đến (1 - 4)
     */
    function switchStep(targetStep) {
        if (targetStep < 1 || targetStep > totalSteps) return;

        // If navigating forward, validate previous steps
        if (targetStep > currentStep) {
            if (currentStep === 1 && !validateStep1()) return;
            if (currentStep === 2 && !validateStep2()) return;
            if (currentStep === 3 && !validateStep3()) return;
        }

        currentStep = targetStep;

        // Update step panels
        stepPanels.forEach(panel => {
            const stepNum = parseInt(panel.getAttribute('data-step'), 10);
            if (stepNum === currentStep) {
                panel.classList.add('active');
            } else {
                panel.classList.remove('active');
            }
        });

        // Update stepper indicators
        stepItems.forEach(item => {
            const stepNum = parseInt(item.getAttribute('data-step'), 10);
            item.classList.remove('active', 'completed');
            if (stepNum === currentStep) {
                item.classList.add('active');
            } else if (stepNum < currentStep) {
                item.classList.add('completed');
            }
        });

        // Update step dividers
        stepDividers.forEach((divider, index) => {
            if (index < currentStep - 1) {
                divider.classList.add('active');
            } else {
                divider.classList.remove('active');
            }
        });

        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    // Stepper header item clicks
    stepItems.forEach(item => {
        item.addEventListener('click', function () {
            const stepNum = parseInt(item.getAttribute('data-step'), 10);
            if (stepNum > totalSteps) {
                // Future pipeline steps (e.g. Step 5 Interview, Step 6 Offer)
                alert('Giai đoạn này sẽ được cấu hình sau khi tin tuyển dụng được duyệt và bắt đầu nhận hồ sơ ứng viên.');
                return;
            }
            switchStep(stepNum);
        });
    });

    // Next / Prev button triggers
    document.querySelectorAll('[data-next-step]').forEach(btn => {
        btn.addEventListener('click', function () {
            const nextStep = parseInt(btn.getAttribute('data-next-step'), 10);
            switchStep(nextStep);
        });
    });

    document.querySelectorAll('[data-prev-step]').forEach(btn => {
        btn.addEventListener('click', function () {
            const prevStep = parseInt(btn.getAttribute('data-prev-step'), 10);
            switchStep(prevStep);
        });
    });

    /* -------------------------------------------------------------------------
     * 4. KIỂM TRA HỢP LỆ VÀ XỬ LÝ SUBMIT TOÀN BỘ FORM (#jobPostingForm)
     * ------------------------------------------------------------------------- */
    if (form) {
        form.addEventListener('submit', function (e) {
            const isStep1Valid = validateStep1();
            const isStep2Valid = validateStep2();
            const isStep3Valid = validateStep3();

            if (!isStep1Valid) {
                switchStep(1);
                e.preventDefault();
                return;
            }
            if (!isStep2Valid) {
                switchStep(2);
                e.preventDefault();
                return;
            }
            if (!isStep3Valid) {
                switchStep(3);
                e.preventDefault();
                return;
            }

            // Nếu tích chọn tuyển liên tục, gửi giá trị rỗng lên server
            if (continuousCheckbox && continuousCheckbox.checked) {
                deadlineInput.disabled = false;
                deadlineInput.value = '';
            }
        });
    }

    // Initialize state
    if (continuousCheckbox && continuousCheckbox.checked) {
        toggleContinuousRecruitment();
    }
    validateDeadline();
});
