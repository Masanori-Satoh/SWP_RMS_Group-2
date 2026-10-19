/* =============================================================================
   REQUISITION-FORM.JS — Form Tạo / Sửa / Sao chép yêu cầu tuyển dụng (requisitions/form.html)

   JS chỉ giúp người dùng thấy lỗi SỚM. Server (RequisitionValidator) luôn kiểm tra lại,
   nên mọi quy tắc ở đây phải GIỐNG quy tắc ở server, không được thêm quy tắc riêng.

   Các phần:
     1. Quy tắc kiểm tra từng trường (FIELD_RULES)
     2. Ô tăng/giảm số (stepper), bộ đếm ký tự
     3. Bảng tiêu chí sàng lọc + tổng trọng số
     4. Cột "Sẵn sàng gửi duyệt" (checklist)
     5. Lưu nháp / Gửi duyệt + bước xác nhận
   ============================================================================= */
(() => {
    'use strict';

    const form = document.getElementById('requisition-form');
    if (!form) {
        return;
    }

    /* -------------------------------------------------------------------------
     * 1. QUY TẮC KIỂM TRA TỪNG TRƯỜNG
     *    requiredForDraft: bắt buộc cả khi lưu nháp (cột NOT NULL trong DB)
     *    requiredForSubmit: bắt buộc khi gửi duyệt
     *    check(value): kiểm tra định dạng, trả về câu lỗi hoặc '' nếu hợp lệ
     * ------------------------------------------------------------------------- */
    // Ngày hôm nay dạng yyyy-MM-dd theo giờ máy người dùng.
    // Không dùng toISOString(): hàm đó trả ngày theo giờ UTC, từ 0h đến 7h sáng ở Việt Nam sẽ ra ngày hôm qua.
    const today = () => {
        const now = new Date();
        const pad = number => String(number).padStart(2, '0');
        return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
    };

    const FIELD_RULES = [
        { id: 'title', label: 'Tên vị trí', section: 'rqf-position', requiredForDraft: true },
        {
            id: 'numberOfPositions', label: 'Số lượng', section: 'rqf-position', requiredForDraft: true,
            // Giới hạn đọc từ min/max của ô nhập (server truyền xuống từ RequisitionValidator.POSITIONS_MIN/MAX)
            check: value => {
                const input = document.getElementById('numberOfPositions');
                const min = Number(input.min) || 1;
                const max = Number(input.max) || 30;
                const count = Number(value);
                return Number.isInteger(count) && count >= min && count <= max
                    ? '' : `Số lượng cần tuyển phải từ ${min} đến ${max} người.`;
            }
        },
        { id: 'employmentType', label: 'Hình thức làm việc', section: 'rqf-work', requiredForDraft: true, radio: true },
        { id: 'workModel', label: 'Mô hình làm việc', section: 'rqf-work', requiredForDraft: true, radio: true },
        { id: 'workLocation', label: 'Địa điểm làm việc', section: 'rqf-work', requiredForSubmit: true },
        {
            id: 'expectedStartDate', label: 'Ngày bắt đầu', section: 'rqf-work', requiredForSubmit: true,
            // Giới hạn trên đọc từ max của ô ngày (server truyền xuống từ RequisitionValidator.START_DATE_MAX_YEARS)
            check: value => {
                const max = document.getElementById('expectedStartDate').max;
                return max && value > max ? 'Ngày bắt đầu dự kiến không được quá 1 năm kể từ hôm nay.' : '';
            },
            checkOnSubmit: value => value < today() ? 'Ngày bắt đầu dự kiến phải từ hôm nay trở đi.' : ''
        },
        { id: 'reasonForHiring', label: 'Lý do tuyển dụng', section: 'rqf-content', requiredForSubmit: true },
        { id: 'jobDescription', label: 'Mô tả công việc', section: 'rqf-content', requiredForSubmit: true },
        { id: 'requirementDetails', label: 'Yêu cầu ứng viên', section: 'rqf-content', requiredForSubmit: true }
    ];

    // Ô chọn phòng ban chỉ có trên form của Giám đốc (vai trò khác là ô ẩn).
    // Khớp RequisitionServiceImpl.requireActiveDepartment: bắt buộc cả khi lưu nháp
    if (document.getElementById('departmentId')?.tagName === 'SELECT') {
        FIELD_RULES.splice(1, 0, { id: 'departmentId', label: 'Phòng ban', section: 'rqf-position', requiredForDraft: true });
    }

    /** Lấy giá trị một trường; với nhóm radio thì lấy ô đang được chọn. */
    function valueOf(rule) {
        if (rule.radio) {
            const checked = form.querySelector(`input[name="${rule.id}"]:checked`);
            return checked ? checked.value : '';
        }
        const input = document.getElementById(rule.id);
        return input ? input.value.trim() : '';
    }

    /** Hiện hoặc xóa lỗi của một trường (ô nhập + dòng chữ đỏ có id "<trường>-error"). */
    function showError(fieldId, message) {
        const input = document.getElementById(fieldId) || document.getElementById(fieldId + '-group');
        const errorEl = document.getElementById(fieldId + '-error');
        if (input) {
            input.toggleAttribute('aria-invalid', Boolean(message));
            input.classList.toggle('is-invalid', Boolean(message));
        }
        if (errorEl) {
            errorEl.textContent = message;
        }
    }

    /** Kiểm tra một trường theo hành động (draft / submit). Trả về true nếu hợp lệ. */
    function validateField(rule, action) {
        const value = valueOf(rule);
        const required = rule.requiredForDraft || (action === 'submit' && rule.requiredForSubmit);
        let message = '';

        if (required && !value) {
            if (rule.id === 'title') {
                message = 'Vui lòng nhập tên vị trí.';
            } else if (rule.id === 'departmentId') {
                message = 'Vui lòng chọn phòng ban đang hoạt động.';
            } else if (rule.id === 'workLocation') {
                message = 'Vui lòng nhập địa điểm làm việc.';
            } else if (rule.id === 'numberOfPositions') {
                message = 'Vui lòng nhập số lượng cần tuyển.';
            } else if (rule.id === 'expectedStartDate') {
                message = 'Ngày bắt đầu dự kiến phải từ hôm nay trở đi.';
            } else {
                message = 'Vui lòng nhập trường này.';
            }
        } else if (value) {
            // check: luôn kiểm tra; checkOnSubmit: chỉ kiểm tra khi gửi duyệt
            message = (rule.check ? rule.check(value) : '')
                || (action === 'submit' && rule.checkOnSubmit ? rule.checkOnSubmit(value) : '');
        }
        showError(rule.id, message);
        return !message;
    }

    /** Định dạng số tiền kiểu 20.000.000 khi nhập (cứ 3 số lại có dấu chấm ngăn cách), giữ đúng vị trí con trỏ */
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

    function parseSalary(val) {
        if (!val) return null;
        // Bỏ dấu chấm ngăn cách hàng nghìn: "20.000.000" → 20000000
        const cleaned = String(val).replace(/\./g, '').trim();
        if (!cleaned) return null;
        const num = Number(cleaned);
        return isNaN(num) ? null : num;
    }

    /** Các quy tắc liên quan nhiều ô: thử việc, lương. Khớp RequisitionValidator. */
    function validateNumbers() {
        let valid = true;

        const probation = document.getElementById('probationDays');
        if (probation && probation.value.trim()) {
            const days = Number(probation.value);
            const min = Number(probation.min) || 1;
            const max = Number(probation.max) || 180;
            const message = Number.isInteger(days) && days >= min && days <= max
                ? '' : `Thời gian thử việc phải từ ${min} đến ${max} ngày.`;
            showError('probationDays', message);
            valid = valid && !message;
        } else {
            showError('probationDays', '');
        }

        const minSalaryInput = document.getElementById('minSalary');
        const maxSalaryInput = document.getElementById('maxSalary');
        const minVal = parseSalary(minSalaryInput?.value);
        const maxVal = parseSalary(maxSalaryInput?.value);

        let minSalaryMessage = '';
        let maxSalaryMessage = '';

        // Khớp RequisitionValidator.SALARY_MAX
        const SALARY_MAX = 1000000000;
        const salaryMessage = value => value === null ? ''
            : value <= 0 ? 'Mức lương phải lớn hơn 0 với tối đa 2 chữ số thập phân.'
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

    // Kiểm tra lại ngay khi người dùng sửa một ô đang báo lỗi.
    // Rời ô (blur) thì kiểm tra theo luật gửi duyệt, để người dùng biết sớm ô nào còn thiếu
    // (lưu nháp vẫn được, vì nút Lưu bản nháp kiểm tra lại toàn bộ theo luật nháp).
    FIELD_RULES.forEach(rule => {
        const target = rule.radio ? document.getElementById(rule.id + '-group') : document.getElementById(rule.id);
        target?.addEventListener('change', () => validateField(rule, 'draft'));
        target?.addEventListener('blur', () => validateField(rule, 'submit'), true);
        if (rule.id === 'expectedStartDate') {
            target?.addEventListener('input', () => validateField(rule, 'draft'));
        }
    });

    // Xử lý ô lương: định dạng hàng nghìn kiểu 20.000.000 khi gõ và kiểm tra > 0, ≤ 1 tỷ, tối đa ≥ tối thiểu
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

    document.getElementById('probationDays')?.addEventListener('input', validateNumbers);

    // Sao chép sang đợt mới: tên vị trí khác tên gốc thì là yêu cầu mới, hiện "Đợt 1".
    // Khớp RequisitionServiceImpl.isSameTitle (bỏ khoảng trắng đầu/cuối, không phân biệt hoa/thường)
    const titleInput = document.getElementById('title');
    const roundLabel = document.getElementById('rqf-round');
    const originalTitle = document.getElementById('originalTitle')?.value.trim().toLowerCase();
    if (titleInput && roundLabel && originalTitle) {
        const updateRound = () => {
            const sameTitle = titleInput.value.trim().toLowerCase() === originalTitle;
            roundLabel.textContent = `Đợt ${sameTitle ? roundLabel.dataset.round : 1}`;
        };
        titleInput.addEventListener('input', updateRound);
        updateRound();
    }

    // Loại tuyển dụng không bắt buộc: chưa chọn gì thì chọn sẵn "Chưa xác định"
    if (!form.querySelector('input[name="hiringType"]:checked')) {
        const notSet = form.querySelector('input[name="hiringType"][value=""]');
        if (notSet) {
            notSet.checked = true;
        }
    }

    /* -------------------------------------------------------------------------
     * 2. Ô TĂNG/GIẢM SỐ VÀ BỘ ĐẾM KÝ TỰ
     * ------------------------------------------------------------------------- */
    form.querySelectorAll('[data-step]').forEach(button => button.addEventListener('click', () => {
        const input = document.getElementById(button.dataset.stepTarget);
        if (!input) return;
        const min = input.min === '' ? 1 : Number(input.min);
        const max = input.max === '' ? Infinity : Number(input.max);
        if (input.value === '' && input.dataset.stepStart) {
            // Ô đang trống (ví dụ chưa nhập thử việc): bấm nút thì điền giá trị khởi đầu gợi ý
            input.value = input.dataset.stepStart;
        } else {
            const current = input.value === '' ? min - Number(button.dataset.step) : Number(input.value);
            input.value = Math.min(max, Math.max(min, current + Number(button.dataset.step)));
        }
        input.dispatchEvent(new Event('input', { bubbles: true }));
    }));

    // Báo lỗi "Nội dung quá N ký tự" do js/text-limit.js lo; ở đây chỉ cập nhật bộ đếm
    form.querySelectorAll('textarea[maxlength]').forEach(textarea => {
        const counter = document.getElementById(textarea.id + '-count');
        if (!counter) return;
        const max = textarea.getAttribute('maxlength') || '2000';
        const update = () => {
            counter.textContent = `${textarea.value.length}/${max}`;
        };
        textarea.addEventListener('input', update);
        textarea.addEventListener('keyup', update);
        textarea.addEventListener('change', update);
        textarea.addEventListener('paste', () => setTimeout(update, 0));
        update();
    });

    /* -------------------------------------------------------------------------
     * 3. BẢNG TIÊU CHÍ SÀNG LỌC
     * ------------------------------------------------------------------------- */
    const MAX_CRITERIA = 50;           // khớp RequisitionValidator.MAX_CRITERIA_LIMIT
    const REQUIRED_TOTAL_WEIGHT = 100; // khớp RequisitionValidator.REQUIRED_TOTAL_WEIGHT
    const rows = document.getElementById('criteria-rows');
    const criteriaError = document.getElementById('criteria-error');

    /** Spring cần tên ô dạng screeningCriteria[0].criteriaName, [1]... liên tục: đánh lại sau mỗi lần thêm/xóa dòng. */
    function renumberRows() {
        rows.querySelectorAll('tr').forEach((row, index) => {
            row.querySelector('.row-index').textContent = index + 1;
            row.querySelectorAll('[name]').forEach(input => {
                input.name = input.name.replace(/screeningCriteria\[(?:\d+|__INDEX__)\]/, `screeningCriteria[${index}]`);
            });
        });
    }

    /**
     * Dòng tiêu chí "trống" (giống hệt RequisitionValidator.isBlankCriteria): tên, loại, giá trị đều trống
     * và không bật "Bắt buộc". Trọng số KHÔNG được tính. Dòng trống bị server bỏ qua khi lưu.
     */
    function isBlankRow(row) {
        const text = [...row.querySelectorAll('[name$=".criteriaName"], [name$=".criteriaType"], [name$=".requiredValue"]')]
            .map(input => input.value.trim()).join('');
        const mandatory = row.querySelector('input[type=checkbox]')?.checked;
        return !text && !mandatory;
    }

    /** Tổng trọng số CHỈ tính các dòng đã nhập (dòng trống bị server bỏ qua nên trọng số của nó không có ý nghĩa). */
    function totalWeight() {
        const sum = [...rows.querySelectorAll('tr')]
            .filter(row => !isBlankRow(row))
            .reduce((total, row) => total + Math.round((Number(row.querySelector('.rqf-weight').value) || 0) * 100), 0);
        return sum / 100;
    }

    function updateWeightBox() {
        const total = totalWeight();
        const box = document.getElementById('rqf-weight-box');
        document.getElementById('rqf-weight-total').textContent = `${total}%`;
        document.getElementById('rqf-weight-fill').style.width = `${Math.min(total, 100)}%`;
        box.classList.toggle('is-ok', total === REQUIRED_TOTAL_WEIGHT);
        box.classList.toggle('is-over', total > REQUIRED_TOTAL_WEIGHT);
        document.getElementById('rqf-weight-state').textContent = total === REQUIRED_TOTAL_WEIGHT ? 'Đã đủ'
            : total > REQUIRED_TOTAL_WEIGHT ? `Thừa ${Math.round((total - 100) * 100) / 100}%`
            : `Còn thiếu ${Math.round((100 - total) * 100) / 100}%`;
    }

    /** Kiểm tra bảng tiêu chí. Khớp server: dòng đã nhập phải đủ trường; gửi duyệt cần ≥ 1 dòng, tên không trùng, tổng 100%. */
    function validateCriteria(action) {
        const filled = [...rows.querySelectorAll('tr')].filter(row => !isBlankRow(row));
        let message = '';

        const incomplete = filled.some(row => [...row.querySelectorAll('[name$=".criteriaName"], [name$=".criteriaType"], [name$=".requiredValue"], [name$=".weight"]')]
            .some(input => !input.value.trim()));
        const names = filled.map(row => row.querySelector('[name$=".criteriaName"]').value.trim().toLowerCase());

        if (incomplete) {
            message = 'Mỗi tiêu chí đã nhập cần đủ tên, loại, giá trị yêu cầu và trọng số.';
        } else if (new Set(names).size !== names.length) {
            message = 'Tên các tiêu chí không được trùng nhau.';
        } else if (action === 'submit' && (filled.length === 0 || totalWeight() !== REQUIRED_TOTAL_WEIGHT)) {
            message = 'Vui lòng thêm đầy đủ tiêu chí với tổng trọng số đúng bằng 100%.';
        }
        criteriaError.textContent = message;
        criteriaError.hidden = !message;
        return !message;
    }

    document.getElementById('btn-add-criteria').addEventListener('click', () => {
        if (rows.children.length >= MAX_CRITERIA) {
            criteriaError.textContent = `Chỉ được thêm tối đa ${MAX_CRITERIA} tiêu chí.`;
            criteriaError.hidden = false;
            return;
        }
        const newRow = document.getElementById('criteria-template').content.firstElementChild.cloneNode(true);
        rows.appendChild(newRow);
        renumberRows();
        updateWeightBox();
        refreshChecklist();
        newRow.querySelector('input:not([type=hidden])').focus();
    });

    rows.addEventListener('click', event => {
        if (event.target.closest('[data-remove-criterion]')) {
            event.target.closest('tr').remove();
            renumberRows();
            updateWeightBox();
            refreshChecklist();
        }
    });
    rows.addEventListener('input', updateWeightBox);
    rows.addEventListener('change', updateWeightBox);

    // "Chia đều trọng số": chia 100 cho các dòng đã nhập; dòng cuối nhận phần dư để tổng đúng 100
    document.getElementById('rqf-split-weight').addEventListener('click', () => {
        const inputs = [...rows.querySelectorAll('tr')].filter(row => !isBlankRow(row)).map(row => row.querySelector('.rqf-weight'));
        if (!inputs.length) {
            return;
        }
        const share = Math.floor(10000 / inputs.length) / 100;
        inputs.forEach((input, index) => {
            input.value = index === inputs.length - 1
                ? Math.round((100 - share * (inputs.length - 1)) * 100) / 100
                : share;
        });
        updateWeightBox();
        refreshChecklist();
    });

    /* -------------------------------------------------------------------------
     * 4. CỘT "SẴN SÀNG GỬI DUYỆT"
     *    Mỗi mục bắt buộc khi gửi duyệt là một dòng ✓ / chưa xong, bấm vào thì nhảy tới trường đó.
     * ------------------------------------------------------------------------- */
    function refreshChecklist() {
        const isFieldDone = rule => {
            const val = valueOf(rule);
            if (!val) return false;
            if (rule.check) return !rule.check(val);
            return true;
        };
        const items = FIELD_RULES.map(rule => ({ label: rule.label, target: rule.radio ? rule.id + '-group' : rule.id, done: isFieldDone(rule) }));
        const filledRows = [...rows.querySelectorAll('tr')].filter(row => !isBlankRow(row)).length;
        items.push({ label: 'Tiêu chí đủ 100%', target: 'rqf-criteria', done: filledRows > 0 && totalWeight() === REQUIRED_TOTAL_WEIGHT });

        const doneCount = items.filter(item => item.done).length;
        document.getElementById('rqf-progress').textContent = `${doneCount}/${items.length} mục bắt buộc đã xong`;

        const list = document.getElementById('rqf-checklist');
        list.replaceChildren(...items.map(item => {
            const li = document.createElement('li');
            li.className = item.done ? 'is-done' : '';
            const link = document.createElement('a');
            link.href = '#' + item.target;
            // Vòng tròn trạng thái: dấu tick vẽ bằng SVG (viewBox 16x16) nên luôn nằm chính giữa,
            // không lệch như ký tự "✓" (mỗi font đặt ký tự cao thấp khác nhau)
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

    /* -------------------------------------------------------------------------
     * 5. LƯU NHÁP / GỬI DUYỆT
     * ------------------------------------------------------------------------- */
    /** Kiểm tra toàn bộ form; trả về true nếu được phép gửi lên server. */
    function validateAll(action) {
        const fieldsOk = FIELD_RULES.map(rule => validateField(rule, action)).every(Boolean);
        const numbersOk = validateNumbers();
        const criteriaOk = validateCriteria(action);
        if (!(fieldsOk && numbersOk && criteriaOk)) {
            const firstError = form.querySelector('[aria-invalid="true"], .is-invalid input') || (criteriaOk ? null : criteriaError);
            firstError?.scrollIntoView({ behavior: 'smooth', block: 'center' });
            firstError?.focus?.({ preventScroll: true });
            return false;
        }
        return true;
    }

    const buttons = document.getElementById('rqf-buttons');
    const confirmBox = document.getElementById('rqf-confirm');

    // Bước 1: bấm "Gửi Giám đốc duyệt" → kiểm tra đủ điều kiện → hiện hộp xác nhận
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

    // Bước 2: form thật sự được gửi (Lưu nháp hoặc nút "Gửi duyệt" trong hộp xác nhận)
    let submitting = false;
    form.addEventListener('submit', event => {
        // Chặn bấm 2 lần khi form đang gửi (tránh tạo 2 yêu cầu giống nhau)
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
        // Khóa nút sau khi trình duyệt đã lấy giá trị nút được bấm (action=draft/submit),
        // nếu khóa ngay thì nút bị khóa không được gửi kèm form
        setTimeout(() => form.querySelectorAll('button[type=submit]').forEach(button => { button.disabled = true; }), 0);
    });
    // Quay lại trang bằng nút Back: trình duyệt có thể khôi phục trang cũ, mở khóa lại các nút
    window.addEventListener('pageshow', () => {
        submitting = false;
        form.querySelectorAll('button[type=submit]').forEach(button => { button.disabled = false; });
    });

    // Khởi tạo trạng thái ban đầu.
    // Bản nháp chưa có tiêu chí nào thì thêm sẵn 1 dòng trống cho người dùng nhập (dòng trống không bị lưu).
    if (rows.children.length === 0) {
        rows.appendChild(document.getElementById('criteria-template').content.firstElementChild.cloneNode(true));
    }
    renumberRows();
    updateWeightBox();
    refreshChecklist();
    document.getElementById('validation-summary')?.focus();
})();
