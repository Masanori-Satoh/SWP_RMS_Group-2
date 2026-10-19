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
        // Menu ⋯ dạng <details> của catalog (trang danh sách yêu cầu): đóng lại trước khi mở hộp thoại
        button.closest('details.more-menu')?.removeAttribute('open');

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
     * 3. FORM TẠO / SỬA YÊU CẦU: đã chuyển sang js/pages/requisition-form.js
     * ------------------------------------------------------------------------- */

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
            feedback.setCustomValidity('Vui lòng nhập lý do để Hiring Manager chỉnh sửa.');
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

