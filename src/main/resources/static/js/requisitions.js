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

        const rows = document.getElementById('criteria-rows');
        const error = document.getElementById('criteria-error');

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
                    error.textContent = 'Tối đa 50 tiêu chí sàng lọc.';
                }
                return;
            }
            const template = document.getElementById('criteria-template');
            if (template && rows) {
                const row = template.content.firstElementChild.cloneNode(true);
                rows.appendChild(row);
                syncRows();
                row.querySelector('input:not([type="hidden"])')?.focus();
            }
        });

        // Xóa một dòng tiêu chí khi bấm nút thùng rác
        rows?.addEventListener('click', event => {
            const button = event.target.closest('[data-remove-criterion]');
            if (button) {
                button.closest('tr')?.remove();
                syncRows();
            }
        });

        // Cập nhật nhãn Có/Không khi thay đổi checkbox
        rows?.addEventListener('change', syncRows);
        syncRows();

        // Kiểm tra tính hợp lệ tiêu chí trước khi submit form
        form.addEventListener('submit', event => {
            if (error) error.hidden = true;
            // Nếu người dùng chọn Lưu bản nháp (Draft), bỏ qua kiểm tra bắt buộc
            if (event.submitter?.value === 'draft') return;

            if (rows) {
                const names = [...rows.querySelectorAll('[name$=".criteriaName"]')].map(input => input.value.trim().toLowerCase());
                const total = [...rows.querySelectorAll('[name$=".weight"]')].reduce((sum, input) => sum + Math.round((Number(input.value) || 0) * 100), 0);
                let message = '';

                if (!names.length || names.some(name => !name)) {
                    message = 'Vui lòng nhập đầy đủ ít nhất một tiêu chí sàng lọc.';
                } else if (new Set(names).size !== names.length) {
                    message = 'Tên các tiêu chí sàng lọc không được trùng lặp.';
                } else if (total !== 10000) {
                    // Tổng trọng số phải bằng đúng 100.00% (10000 đơn vị cơ sở)
                    message = 'Tổng trọng số của các tiêu chí sàng lọc phải bằng đúng 100%.';
                }

                if (message) {
                    event.preventDefault();
                    if (error) {
                        error.textContent = message;
                        error.hidden = false;
                        error.scrollIntoView({ block: 'center' });
                    }
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
