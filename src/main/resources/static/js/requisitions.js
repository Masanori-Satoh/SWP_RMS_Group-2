/**
 * =============================================================================
 * REQUISITIONS.JS
 * Module JavaScript xử lý tương tác giao diện phân hệ Yêu cầu Tuyển dụng (Job Requisition)
 * Phụ trách: Nguyễn Huy Hoàng (HoangNH) - Iteration 1 & 2
 *
 * Chức năng chính:
 *   1. Định vị và hiển thị Menu thao tác Popover ba chấm (...)
 *   2. Quản lý Modal Dialog xác nhận xóa yêu cầu tuyển dụng
 *   3. Bộ đếm độ dài ký tự thời gian thực cho Textarea
 *   4. Quản lý bảng tiêu chí sàng lọc động (Thêm hàng, Xóa hàng, Đồng bộ index Spring)
 *   5. Kiểm tra tính hợp lệ dữ liệu Client-side (Validation tổng trọng số 100%, không trùng tên)
 *   6. Xác thực lý do từ chối yêu cầu của Giám đốc (Director Decision Review)
 *   7. Chống gửi dữ liệu trùng lặp (Double-submit prevention) trên toàn bộ Form
 * =============================================================================
 */

document.addEventListener('DOMContentLoaded', () => {

    /* -------------------------------------------------------------------------
     * 1. MENU THAO TÁC POPOVER BA CHẤM (...)
     * ------------------------------------------------------------------------- */

    /**
     * Tự động tính toán tọa độ để menu popover luôn hiển thị ngay sát nút bấm
     * và không bị tràn ra ngoài màn hình (viewport).
     * @param {HTMLElement} button Nút kích hoạt menu
     * @param {HTMLElement} menu Khối menu popover cần hiển thị
     */
    const positionMenu = (button, menu) => {
        const rect = button.getBoundingClientRect();
        // Nếu nút bấm nằm ngoài khung nhìn (khi cuộn trang), ẩn menu
        if (rect.bottom < 0 || rect.top > window.innerHeight || rect.right < 0 || rect.left > window.innerWidth) {
            menu.hidePopover();
            return;
        }
        // Canh lề trái/phải sao cho menu nằm trọn trong viewport
        menu.style.left = Math.max(8, Math.min(rect.right - menu.offsetWidth, window.innerWidth - menu.offsetWidth - 8)) + 'px';
        // Hiển thị bên dưới nút; nếu sát đáy màn hình thì nhảy lên trên nút
        menu.style.top = (rect.bottom + menu.offsetHeight + 8 > window.innerHeight ? Math.max(8, rect.top - menu.offsetHeight - 6) : rect.bottom + 6) + 'px';
    };

    // Đăng ký sự kiện toggle hiển thị cho tất cả các nút mở menu thao tác
    document.querySelectorAll('[data-action-menu]').forEach(button => {
        const menu = document.getElementById(button.getAttribute('popovertarget'));
        if (menu) {
            menu.addEventListener('toggle', event => {
                if (event.newState !== 'open') return;
                positionMenu(button, menu);
            });
        }
    });

    /**
     * Đóng tất cả các menu popover đang mở trên trang
     */
    const closeMenus = () => document.querySelectorAll('.action-popover:popover-open').forEach(menu => menu.hidePopover());

    /**
     * Cập nhật lại vị trí các menu đang mở khi người dùng cuộn trang hoặc thay đổi kích thước màn hình
     */
    const repositionMenus = () => document.querySelectorAll('[data-action-menu]').forEach(button => {
        const menu = document.getElementById(button.getAttribute('popovertarget'));
        if (menu && menu.matches(':popover-open')) positionMenu(button, menu);
    });

    window.addEventListener('resize', repositionMenus);
    document.addEventListener('scroll', repositionMenus, true);

    /* -------------------------------------------------------------------------
     * 2. HỘP THOẠI XÁC NHẬN XÓA (DELETE CONFIRMATION DIALOG)
     * ------------------------------------------------------------------------- */
    const dialog = document.getElementById('delete-dialog');
    let deleteTrigger;

    // Lắng nghe sự kiện click vào nút Xóa (trong menu popover hoặc trên trang)
    document.querySelectorAll('[data-delete-requisition]').forEach(button => button.addEventListener('click', () => {
        const menu = button.closest('.action-popover');
        deleteTrigger = menu ? document.querySelector(`[popovertarget="${menu.id}"]`) : button;
        closeMenus();

        // Gán tên bản ghi và đường dẫn xóa vào hộp thoại
        const deleteNameEl = document.getElementById('delete-name');
        const deleteFormEl = document.getElementById('delete-form');
        if (deleteNameEl) deleteNameEl.textContent = button.dataset.title;
        if (deleteFormEl) deleteFormEl.action = button.dataset.deleteUrl;

        // Mở dialog dạng modal và focus vào nút Hủy
        if (dialog) {
            dialog.showModal();
            dialog.querySelector('[data-close-dialog]')?.focus();
        }
    }));

    // Đóng dialog khi nhấn nút Hủy hoặc click ra ngoài vùng backdrop
    dialog?.querySelector('[data-close-dialog]')?.addEventListener('click', () => dialog.close());
    dialog?.addEventListener('close', () => deleteTrigger?.focus());
    dialog?.addEventListener('click', event => { if (event.target === dialog) dialog.close(); });

    /* -------------------------------------------------------------------------
     * 3. XỬ LÝ BIỂU MẪU YÊU CẦU TUYỂN DỤNG (#requisition-form)
     * ------------------------------------------------------------------------- */
    const form = document.getElementById('requisition-form');
    if (form) {
        // Bộ đếm ký tự thời gian thực cho các ô Textarea
        form.querySelectorAll('.textarea-custom').forEach(input => {
            const counter = input.parentElement?.querySelector('.char-counter');
            if (counter) {
                const update = () => { counter.textContent = input.value.length + '/' + input.maxLength; };
                input.addEventListener('input', update);
                update();
            }
        });

        /* =========================================================================
         * 3.1. VALIDATION THỜI GIAN THỰC TỨC THÌ (INSTANT REAL-TIME VALIDATION)
         * Tương tự cơ chế của HR (báo đỏ viền và hiển thị lỗi ngay khi nhập)
         * ========================================================================= */
        const MIN_SALARY_THRESHOLD = 1000000;
        const MAX_PROBATION_DAYS = 180;

        const titleInput = document.getElementById('title');
        const titleError = document.getElementById('title-error');

        const deptInput = document.getElementById('departmentId');
        const deptError = document.getElementById('departmentId-error');

        const empTypeInput = document.getElementById('employmentType');
        const empTypeError = document.getElementById('employmentType-error');

        const workModelInput = document.getElementById('workModel');
        const workModelError = document.getElementById('workModel-error');

        const startDateInput = document.getElementById('expectedStartDate');
        const startDateError = document.getElementById('expectedStartDate-error');

        const probationInput = document.getElementById('probationDuration');
        const probationError = document.getElementById('probationDuration-error');

        const positionsInput = document.getElementById('numberOfPositions');
        const positionsError = document.getElementById('numberOfPositions-error');

        const locationInput = document.getElementById('workLocation');
        const locationError = document.getElementById('workLocation-error');

        const minSalaryInput = document.getElementById('minSalary');
        const minSalaryError = document.getElementById('minSalary-error');
        const maxSalaryInput = document.getElementById('maxSalary');
        const maxSalaryError = document.getElementById('maxSalary-error');

        const reasonInput = document.getElementById('reasonForHiring');
        const reasonError = document.getElementById('reasonForHiring-error');

        const jdInput = document.getElementById('jobDescription');
        const jdError = document.getElementById('jobDescription-error');

        const reqsInput = document.getElementById('requirementDetails');
        const reqsError = document.getElementById('requirementDetails-error');

        /**
         * Thiết lập trạng thái hợp lệ hoặc báo lỗi đỏ cho ô nhập liệu
         */
        function setFieldState(input, errorEl, message) {
            if (!input) return;
            if (message) {
                input.classList.add('is-invalid');
                input.setAttribute('aria-invalid', 'true');
                if (errorEl) {
                    errorEl.textContent = message;
                    errorEl.style.display = 'block';
                }
            } else {
                input.classList.remove('is-invalid');
                input.removeAttribute('aria-invalid');
                if (errorEl) {
                    errorEl.textContent = '';
                    errorEl.style.display = 'none';
                }
            }
        }

        function validateTitle() {
            if (!titleInput) return true;
            const val = titleInput.value.trim();
            if (!val) {
                setFieldState(titleInput, titleError, 'Vị trí tuyển dụng không được để trống.');
                return false;
            }
            if (val.toLowerCase() === 'untitled requisition') {
                setFieldState(titleInput, titleError, 'Vui lòng nhập vị trí tuyển dụng cụ thể.');
                return false;
            }
            setFieldState(titleInput, titleError, '');
            return true;
        }

        function validateDepartment() {
            if (!deptInput || deptInput.tagName !== 'SELECT') return true;
            if (!deptInput.value) {
                setFieldState(deptInput, deptError, 'Vui lòng chọn phòng ban.');
                return false;
            }
            setFieldState(deptInput, deptError, '');
            return true;
        }

        function validateEmploymentType() {
            if (!empTypeInput) return true;
            if (!empTypeInput.value) {
                setFieldState(empTypeInput, empTypeError, 'Vui lòng chọn hình thức làm việc.');
                return false;
            }
            setFieldState(empTypeInput, empTypeError, '');
            return true;
        }

        function validateWorkModel() {
            if (!workModelInput) return true;
            if (!workModelInput.value) {
                setFieldState(workModelInput, workModelError, 'Vui lòng chọn mô hình làm việc.');
                return false;
            }
            setFieldState(workModelInput, workModelError, '');
            return true;
        }

        function validateStartDate() {
            if (!startDateInput) return true;
            const val = startDateInput.value;
            if (!val) {
                setFieldState(startDateInput, startDateError, 'Vui lòng chọn ngày bắt đầu dự kiến.');
                return false;
            }
            const today = new Date().toISOString().split('T')[0];
            if (val < today) {
                setFieldState(startDateInput, startDateError, 'Ngày bắt đầu dự kiến phải là hôm nay hoặc trong tương lai.');
                return false;
            }
            setFieldState(startDateInput, startDateError, '');
            return true;
        }

        function validateProbation() {
            if (!probationInput) return true;
            const val = probationInput.value.trim();
            if (!val) {
                setFieldState(probationInput, probationError, 'Vui lòng nhập thời gian thử việc.');
                return false;
            }
            const days = parseInt(val, 10);
            if (isNaN(days) || days < 1 || days > MAX_PROBATION_DAYS) {
                setFieldState(probationInput, probationError, 'Thời gian thử việc phải là số ngày nguyên trong khoảng 1-180.');
                return false;
            }
            setFieldState(probationInput, probationError, '');
            return true;
        }

        function validatePositions() {
            if (!positionsInput) return true;
            const val = parseInt(positionsInput.value, 10);
            if (isNaN(val) || val < 1) {
                setFieldState(positionsInput, positionsError, 'Số lượng tuyển dụng phải là số nguyên lớn hơn 0.');
                return false;
            }
            setFieldState(positionsInput, positionsError, '');
            return true;
        }

        function validateLocation() {
            if (!locationInput) return true;
            if (!locationInput.value.trim()) {
                setFieldState(locationInput, locationError, 'Địa điểm làm việc không được để trống.');
                return false;
            }
            setFieldState(locationInput, locationError, '');
            return true;
        }

        function validateSalary() {
            let isValid = true;
            const minRaw = minSalaryInput ? minSalaryInput.value.trim() : '';
            const maxRaw = maxSalaryInput ? maxSalaryInput.value.trim() : '';

            const minVal = minRaw !== '' ? parseFloat(minRaw) : null;
            const maxVal = maxRaw !== '' ? parseFloat(maxRaw) : null;

            // Xóa lỗi cũ
            setFieldState(minSalaryInput, minSalaryError, '');
            setFieldState(maxSalaryInput, maxSalaryError, '');

            // Kiểm tra Lương tối thiểu
            if (minVal !== null) {
                if (minVal < 0) {
                    setFieldState(minSalaryInput, minSalaryError, 'Mức lương không được âm.');
                    isValid = false;
                } else if (minVal > 0 && minVal < MIN_SALARY_THRESHOLD) {
                    setFieldState(minSalaryInput, minSalaryError, 'Mức lương tối thiểu từ 1,000,000 VNĐ');
                    isValid = false;
                }
            }

            // Kiểm tra Lương tối đa
            if (maxVal !== null) {
                if (maxVal < 0) {
                    setFieldState(maxSalaryInput, maxSalaryError, 'Mức lương không được âm.');
                    isValid = false;
                } else if (maxVal > 0 && maxVal < MIN_SALARY_THRESHOLD) {
                    setFieldState(maxSalaryInput, maxSalaryError, 'Mức lương tối thiểu từ 1,000,000 VNĐ');
                    isValid = false;
                }
            }

            // Kiểm tra Max >= Min
            if (minVal !== null && maxVal !== null && isValid) {
                if (maxVal < minVal) {
                    setFieldState(maxSalaryInput, maxSalaryError, 'Mức lương tối đa phải lớn hơn hoặc bằng mức lương tối thiểu.');
                    isValid = false;
                }
            }

            return isValid;
        }

        function validateReason() {
            if (!reasonInput) return true;
            if (!reasonInput.value.trim()) {
                setFieldState(reasonInput, reasonError, 'Lý do tuyển dụng không được để trống.');
                return false;
            }
            setFieldState(reasonInput, reasonError, '');
            return true;
        }

        function validateJobDescription() {
            if (!jdInput) return true;
            if (!jdInput.value.trim()) {
                setFieldState(jdInput, jdError, 'Mô tả công việc không được để trống.');
                return false;
            }
            setFieldState(jdInput, jdError, '');
            return true;
        }

        function validateRequirementDetails() {
            if (!reqsInput) return true;
            if (!reqsInput.value.trim()) {
                setFieldState(reqsInput, reqsError, 'Yêu cầu ứng viên không được để trống.');
                return false;
            }
            setFieldState(reqsInput, reqsError, '');
            return true;
        }

        // Đăng ký sự kiện kiểm tra tức thì khi nhập (input) và khi rời khỏi ô (blur)
        if (titleInput) {
            titleInput.addEventListener('input', validateTitle);
            titleInput.addEventListener('blur', validateTitle);
        }
        if (deptInput && deptInput.tagName === 'SELECT') {
            deptInput.addEventListener('change', validateDepartment);
            deptInput.addEventListener('blur', validateDepartment);
        }
        if (empTypeInput) {
            empTypeInput.addEventListener('change', validateEmploymentType);
            empTypeInput.addEventListener('blur', validateEmploymentType);
        }
        if (workModelInput) {
            workModelInput.addEventListener('change', validateWorkModel);
            workModelInput.addEventListener('blur', validateWorkModel);
        }
        if (startDateInput) {
            startDateInput.addEventListener('change', validateStartDate);
            startDateInput.addEventListener('blur', validateStartDate);
        }
        if (probationInput) {
            probationInput.addEventListener('input', validateProbation);
            probationInput.addEventListener('blur', validateProbation);
        }
        if (positionsInput) {
            positionsInput.addEventListener('input', validatePositions);
            positionsInput.addEventListener('blur', validatePositions);
        }
        if (locationInput) {
            locationInput.addEventListener('input', validateLocation);
            locationInput.addEventListener('blur', validateLocation);
        }
        if (minSalaryInput) {
            minSalaryInput.addEventListener('input', validateSalary);
            minSalaryInput.addEventListener('blur', validateSalary);
        }
        if (maxSalaryInput) {
            maxSalaryInput.addEventListener('input', validateSalary);
            maxSalaryInput.addEventListener('blur', validateSalary);
        }
        if (reasonInput) {
            reasonInput.addEventListener('input', validateReason);
            reasonInput.addEventListener('blur', validateReason);
        }
        if (jdInput) {
            jdInput.addEventListener('input', validateJobDescription);
            jdInput.addEventListener('blur', validateJobDescription);
        }
        if (reqsInput) {
            reqsInput.addEventListener('input', validateRequirementDetails);
            reqsInput.addEventListener('blur', validateRequirementDetails);
        }

        /* =========================================================================
         * 3.2. BẢNG TIÊU CHÍ SÀNG LỌC ĐỘNG
         * ========================================================================= */
        const rows = document.getElementById('criteria-rows');
        const error = document.getElementById('criteria-error');

        /**
         * Kiểm tra và cập nhật tổng trọng số tiêu chí theo thời gian thực
         */
        const updateCriteriaWeightFeedback = () => {
            if (!rows) return;
            const weightInputs = [...rows.querySelectorAll('[name$=".weight"]')];
            if (!weightInputs.length) return;
            const total = weightInputs.reduce((sum, input) => sum + (parseFloat(input.value) || 0), 0);
            const rounded = Math.round(total * 100) / 100;

            if (error) {
                if (rounded === 100) {
                    error.hidden = false;
                    error.style.display = 'block';
                    error.style.color = '#15803d';
                    error.textContent = '✓ Tổng trọng số của các tiêu chí đạt đúng 100%.';
                } else if (rounded > 0) {
                    error.hidden = false;
                    error.style.display = 'block';
                    error.style.color = '#dc2626';
                    error.textContent = `Tổng trọng số hiện tại là ${rounded}%. Cần đạt đúng 100% trước khi gửi duyệt.`;
                } else {
                    error.hidden = true;
                    error.style.display = 'none';
                }
            }
        };

        /**
         * Đồng bộ lại chỉ số mảng (index) cho Spring Data binding: screeningCriteria[0], screeningCriteria[1]...
         * Đồng thời cập nhật nhãn trợ năng và trạng thái chữ switch (Có/Không)
         */
        const syncRows = () => rows?.querySelectorAll('tr').forEach((row, index) => {
            const rowIndexEl = row.querySelector('.row-index');
            if (rowIndexEl) rowIndexEl.textContent = index + 1;

            row.querySelectorAll('[name]').forEach(input => {
                input.name = input.name.replace(/screeningCriteria\[(?:\d+|__INDEX__)\]/, 'screeningCriteria[' + index + ']');
                if (input.type !== 'hidden') {
                    input.setAttribute('aria-label', ({
                        criteriaName: 'Tên tiêu chí',
                        criteriaType: 'Loại tiêu chí',
                        requiredValue: 'Giá trị yêu cầu',
                        weight: 'Trọng số (%)',
                        isMandatory: 'Bắt buộc'
                    })[input.name.split('.').pop()] || 'Tiêu chí');
                }
            });

            const checkbox = row.querySelector('input[type="checkbox"]');
            const switchText = row.querySelector('.switch-text');
            if (checkbox && switchText) {
                switchText.textContent = checkbox.checked ? 'Có' : 'Không';
            }
        });

        // Nút thêm mới tiêu chí sàng lọc (+ Thêm tiêu chí sàng lọc)
        document.getElementById('btn-add-criteria')?.addEventListener('click', () => {
            if (rows && rows.children.length >= 50) {
                if (error) {
                    error.hidden = false;
                    error.style.display = 'block';
                    error.style.color = '#dc2626';
                    error.textContent = 'Tối đa 50 tiêu chí sàng lọc.';
                }
                return;
            }
            const template = document.getElementById('criteria-template');
            if (template && rows) {
                const row = template.content.firstElementChild.cloneNode(true);
                rows.appendChild(row);
                syncRows();
                updateCriteriaWeightFeedback();
                row.querySelector('input:not([type="hidden"])')?.focus();
            }
        });

        // Xóa một dòng tiêu chí khi bấm nút thùng rác
        rows?.addEventListener('click', event => {
            const button = event.target.closest('[data-remove-criterion]');
            if (button) {
                button.closest('tr')?.remove();
                syncRows();
                updateCriteriaWeightFeedback();
            }
        });

        // Lắng nghe thay đổi checkbox và trọng số trong bảng tiêu chí
        rows?.addEventListener('change', () => {
            syncRows();
            updateCriteriaWeightFeedback();
        });
        rows?.addEventListener('input', updateCriteriaWeightFeedback);
        syncRows();

        // Kiểm tra tính hợp lệ toàn bộ form trước khi submit
        form.addEventListener('submit', event => {
            if (error) error.hidden = true;
            const isDraft = event.submitter?.value === 'draft';

            // Nếu là Lưu bản nháp (Draft), chỉ kiểm tra định dạng nếu người dùng có nhập
            if (isDraft) {
                const isSalaryValid = validateSalary();
                let isProbationValid = true;
                if (probationInput && probationInput.value.trim()) {
                    isProbationValid = validateProbation();
                }
                if (!isSalaryValid || !isProbationValid) {
                    event.preventDefault();
                    document.querySelector('.form-control-custom.is-invalid, .textarea-custom.is-invalid')?.focus();
                }
                return;
            }

            // Nếu là Gửi duyệt (Submit): Kiểm tra toàn bộ các trường bắt buộc
            const vTitle = validateTitle();
            const vDept = validateDepartment();
            const vEmp = validateEmploymentType();
            const vWorkModel = validateWorkModel();
            const vDate = validateStartDate();
            const vProbation = validateProbation();
            const vPos = validatePositions();
            const vLoc = validateLocation();
            const vSalary = validateSalary();
            const vReason = validateReason();
            const vJd = validateJobDescription();
            const vReqs = validateRequirementDetails();

            let isFormValid = vTitle && vDept && vEmp && vWorkModel && vDate && vProbation &&
                              vPos && vLoc && vSalary && vReason && vJd && vReqs;

            // Kiểm tra bảng tiêu chí sàng lọc
            if (rows) {
                const names = [...rows.querySelectorAll('[name$=".criteriaName"]')].map(input => input.value.trim().toLowerCase());
                const total = [...rows.querySelectorAll('[name$=".weight"]')].reduce((sum, input) => sum + Math.round((Number(input.value) || 0) * 100), 0);
                let criteriaMessage = '';

                if (!names.length || names.some(name => !name)) {
                    criteriaMessage = 'Vui lòng nhập đầy đủ ít nhất một tiêu chí sàng lọc.';
                } else if (new Set(names).size !== names.length) {
                    criteriaMessage = 'Tên các tiêu chí sàng lọc không được trùng lặp.';
                } else if (total !== 10000) {
                    criteriaMessage = 'Tổng trọng số của các tiêu chí sàng lọc phải bằng đúng 100%.';
                }

                if (criteriaMessage) {
                    isFormValid = false;
                    if (error) {
                        error.textContent = criteriaMessage;
                        error.style.color = '#dc2626';
                        error.hidden = false;
                        error.style.display = 'block';
                    }
                }
            }

            if (!isFormValid) {
                event.preventDefault();
                // Focus vào phần tử lỗi đầu tiên
                const firstInvalid = document.querySelector('.form-control-custom.is-invalid, .textarea-custom.is-invalid');
                if (firstInvalid) {
                    firstInvalid.focus();
                    firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
                } else if (error && !error.hidden) {
                    error.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            }
        });

        // Tự động focus lên danh sách lỗi nếu có lỗi từ server trả về
        document.getElementById('validation-summary')?.focus();
    }

    /* -------------------------------------------------------------------------
     * 4. FORM QUYẾT ĐỊNH CỦA GIÁM ĐỐC (DIRECTOR DECISION REVIEW)
     * ------------------------------------------------------------------------- */
    const decision = document.getElementById('decision-form');
    const feedback = document.getElementById('decision-comment');

    // Xóa thông báo lỗi khi người dùng chọn nút hành động hoặc bắt đầu gõ lý do
    decision?.querySelectorAll('button[name="decision"]').forEach(button => button.addEventListener('click', () => feedback?.setCustomValidity('')));
    feedback?.addEventListener('input', () => feedback.setCustomValidity(''));

    // Bắt buộc Giám đốc phải nhập lý do khi bấm Từ chối (Reject)
    decision?.addEventListener('submit', event => {
        if (event.submitter?.value === 'reject' && feedback && !feedback.value.trim()) {
            event.preventDefault();
            feedback.setCustomValidity('Vui lòng nêu rõ lý do từ chối để người tạo yêu cầu chỉnh sửa.');
            feedback.reportValidity();
        }
    });

    /* -------------------------------------------------------------------------
     * 5. CHỐNG GỬI FORM LẶP LẠI (DOUBLE-SUBMIT PREVENTION)
     * ------------------------------------------------------------------------- */
    document.querySelectorAll('#requisition-form,#delete-form,#decision-form,.withdraw-form').forEach(target => target.addEventListener('submit', event => {
        if (event.defaultPrevented) return;
        if (target.dataset.submitting === 'true') {
            event.preventDefault();
            return;
        }
        target.dataset.submitting = 'true';
        target.setAttribute('aria-busy', 'true');
    }));

    // Khôi phục trạng thái nút bấm khi quay lại trang bằng nút Back của trình duyệt
    window.addEventListener('pageshow', () => document.querySelectorAll('[data-submitting]').forEach(target => {
        delete target.dataset.submitting;
        target.removeAttribute('aria-busy');
    }));
});
