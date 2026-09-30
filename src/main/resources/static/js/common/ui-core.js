/**
 * =============================================================================
 * ui-core.js - Nền tảng UI/UX dùng chung cho toàn bộ hệ thống RMS
 * Group 2 | Recruitment Management System (SWP_RMS_Group-2)
 * =============================================================================
 * Cung cấp:
 *  1. RMS_STATUS_MAP: Chuẩn hóa mapping Enum Backend -> Nhãn tiếng Việt 100% + Màu sắc + Icon
 *  2. RMS_FORMATTER: Định dạng tiền tệ (vi-VN ₫), ngày giờ (dd/MM/yyyy HH:mm), tỷ lệ %, chuỗi rỗng
 *  3. RMS_UI:
 *     - createStatusBadge: Component hiển thị huy hiệu trạng thái thống nhất
 *     - attachMoneyInput: Component nhập tiền tệ phân cách hàng nghìn (hiển thị 25.000.000 ₫, lưu 25000000)
 *     - openModal / closeModal: Điều khiển Modal sticky, khóa cuộn trang (body scroll lock), ESC key
 *     - showToast: Thông báo nổi (Toast) theo chuẩn WCAG AA
 *     - createEmptyState / createLoadingSkeleton: Xử lý trạng thái rỗng và đang tải
 * =============================================================================
 */

(function (window) {
    'use strict';

    // =========================================================================
    // 1. RMS_STATUS_MAP - Chuẩn hóa trạng thái Enum -> Tiếng Việt 100%
    // =========================================================================
    const RMS_STATUS_MAP = {
        // --- Trạng thái Đề xuất Offer (OfferProposal) ---
        'DRAFT': {
            label: 'Draft',
            cssClass: 'status-draft',
            icon: 'bi-file-earmark-text',
            description: 'Đề xuất đang soạn thảo bởi Quản lý tuyển dụng, chưa gửi duyệt'
        },
        'PENDING_DIRECTOR_APPROVAL': {
            label: 'Chờ Giám đốc duyệt',
            cssClass: 'status-pending',
            icon: 'bi-hourglass-split',
            description: 'Đã gửi lên Ban Giám Đốc chờ thẩm định ngân sách'
        },
        'PENDING_DIRECTOR': {
            label: 'Chờ Giám đốc duyệt',
            cssClass: 'status-pending',
            icon: 'bi-hourglass-split',
            description: 'Đã gửi lên Ban Giám Đốc chờ thẩm định ngân sách'
        },
        'APPROVED': {
            label: 'Đã phê duyệt',
            cssClass: 'status-approved',
            icon: 'bi-check2-circle',
            description: 'Giám đốc đã phê duyệt, sẵn sàng gửi ứng viên'
        },
        'SENT_TO_CANDIDATE': {
            label: 'Đã gửi ứng viên',
            cssClass: 'status-sent',
            icon: 'bi-send-check',
            description: 'Thư mời làm việc đã gửi tới ứng viên, chờ phản hồi'
        },
        'SENT_CANDIDATE': {
            label: 'Đã gửi ứng viên',
            cssClass: 'status-sent',
            icon: 'bi-send-check',
            description: 'Thư mời làm việc đã gửi tới ứng viên, chờ phản hồi'
        },
        'SENT': {
            label: 'Đã gửi ứng viên',
            cssClass: 'status-sent',
            icon: 'bi-send-check',
            description: 'Thư mời làm việc đã gửi tới ứng viên, chờ phản hồi'
        },
        'ACCEPTED': {
            label: 'Đã chấp nhận',
            cssClass: 'status-accepted',
            icon: 'bi-person-check-fill',
            description: 'Ứng viên đã chấp nhận thư mời nhận việc'
        },
        'REJECTED': {
            label: 'Đã từ chối',
            cssClass: 'status-rejected',
            icon: 'bi-x-circle-fill',
            description: 'Đề xuất bị Giám đốc từ chối hoặc yêu cầu điều chỉnh'
        },
        'NEGOTIATING': {
            label: 'Đang thương lượng',
            cssClass: 'status-negotiating',
            icon: 'bi-chat-left-dots-fill',
            description: 'Ứng viên đề xuất thương lượng lại mức lương hoặc điều khoản'
        },
        'DECLINED': {
            label: 'Ứng viên từ chối',
            cssClass: 'status-declined',
            icon: 'bi-x-octagon-fill',
            description: 'Ứng viên từ chối thư mời làm việc'
        },
        'EXPIRED': {
            label: 'Đã hết hạn',
            cssClass: 'status-expired',
            icon: 'bi-clock-history',
            description: 'Thư mời làm việc đã quá thời hạn phản hồi'
        },
        'VOIDED': {
            label: 'Đã hủy',
            cssClass: 'status-voided',
            icon: 'bi-slash-circle',
            description: 'Bản đề xuất đã bị thay thế hoặc hủy bỏ'
        },

        // --- Trạng thái Hồ sơ ứng tuyển (Application) ---
        'APPLIED': {
            label: 'Mới ứng tuyển',
            cssClass: 'status-pending',
            icon: 'bi-inbox-fill',
            description: 'Hồ sơ mới nộp vào hệ thống'
        },
        'SCREENING': {
            label: 'Đang sàng lọc',
            cssClass: 'status-pending',
            icon: 'bi-funnel-fill',
            description: 'Nhân sự đang đánh giá hồ sơ'
        },
        'INTERVIEW_PENDING': {
            label: 'Chờ xếp lịch PV',
            cssClass: 'status-pending',
            icon: 'bi-calendar-event',
            description: 'Hồ sơ đạt tiêu chuẩn, chờ xếp lịch phỏng vấn'
        },
        'INTERVIEWING': {
            label: 'Đang phỏng vấn',
            cssClass: 'status-negotiating',
            icon: 'bi-camera-video',
            description: 'Ứng viên đang tham gia các vòng phỏng vấn'
        },
        'OFFERED': {
            label: 'Đang gửi Offer',
            cssClass: 'status-sent',
            icon: 'bi-envelope-paper-fill',
            description: 'Đã gửi đề xuất tuyển dụng'
        },
        'HIRED': {
            label: 'Đã tuyển dụng',
            cssClass: 'status-accepted',
            icon: 'bi-award-fill',
            description: 'Tuyển dụng thành công, chuyển sang tiếp nhận nhân sự'
        }
    };

    /**
     * Tra cứu thông tin hiển thị trạng thái chuẩn hóa
     * @param {string} rawStatus - Chuỗi trạng thái từ Backend/Database
     * @returns {Object} { label, cssClass, icon, description, rawStatus }
     */
    function resolveStatus(rawStatus) {
        if (!rawStatus || typeof rawStatus !== 'string') {
            return {
                label: 'Không xác định',
                cssClass: 'status-voided',
                icon: 'bi-question-circle',
                description: 'Trạng thái không xác định',
                rawStatus: ''
            };
        }
        const key = rawStatus.trim().toUpperCase().replace(/\s+/g, '_');
        const match = RMS_STATUS_MAP[key];
        if (match) {
            return { ...match, rawStatus };
        }
        // Fallback thân thiện nếu gặp trạng thái mới chưa kịp cập nhật map
        const formattedLabel = rawStatus
            .replace(/_/g, ' ')
            .replace(/\b\w/g, c => c.toUpperCase());
        return {
            label: formattedLabel,
            cssClass: 'status-pending',
            icon: 'bi-info-circle',
            description: formattedLabel,
            rawStatus
        };
    }

    // =========================================================================
    // 2. RMS_FORMATTER - Các hàm định dạng dùng chung (vi-VN)
    // =========================================================================
    const currencyFormatter = new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND',
        maximumFractionDigits: 0
    });

    const numberFormatter = new Intl.NumberFormat('vi-VN', {
        maximumFractionDigits: 0
    });

    const RMS_FORMATTER = {
        /**
         * Định dạng số tiền thành "10.000.000 ₫"
         * @param {number|string} amount
         * @param {string} fallback
         * @returns {string}
         */
        currency: function (amount, fallback = '0 ₫') {
            if (amount == null || amount === '' || isNaN(amount)) return fallback;
            const num = parseFloat(amount);
            return currencyFormatter.format(num);
        },

        /**
         * Định dạng số nguyên có dấu chấm "25.000.000"
         * @param {number|string} num
         * @returns {string}
         */
        number: function (num) {
            if (num == null || num === '' || isNaN(num)) return '0';
            return numberFormatter.format(parseFloat(num));
        },

        /**
         * Định dạng ngày "dd/MM/yyyy"
         * @param {string|Date} dateStr
         * @param {string} fallback
         * @returns {string}
         */
        date: function (dateStr, fallback = '—') {
            if (!dateStr) return fallback;
            try {
                const d = new Date(dateStr);
                if (isNaN(d.getTime())) return fallback;
                return d.toLocaleDateString('vi-VN', {
                    day: '2-digit',
                    month: '2-digit',
                    year: 'numeric'
                });
            } catch (e) {
                return fallback;
            }
        },

        /**
         * Định dạng ngày giờ "dd/MM/yyyy HH:mm"
         * @param {string|Date} dateStr
         * @param {string} fallback
         * @returns {string}
         */
        dateTime: function (dateStr, fallback = '—') {
            if (!dateStr) return fallback;
            try {
                const d = new Date(dateStr);
                if (isNaN(d.getTime())) return fallback;
                return d.toLocaleDateString('vi-VN', {
                    day: '2-digit',
                    month: '2-digit',
                    year: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit'
                });
            } catch (e) {
                return fallback;
            }
        },

        /**
         * Định dạng tỷ lệ phần trăm "85.0%"
         * @param {number|string} ratio
         * @param {number} digits
         * @returns {string}
         */
        percent: function (ratio, digits = 1) {
            if (ratio == null || isNaN(ratio)) return '0%';
            return `${parseFloat(ratio).toFixed(digits)}%`;
        },

        /**
         * Trả về chuỗi an toàn hoặc ký tự gạch ngang dài "—" khi rỗng/null
         * @param {string} str
         * @param {string} fallback
         * @returns {string}
         */
        text: function (str, fallback = '—') {
            if (str == null || str === undefined) return fallback;
            const trimmed = String(str).trim();
            return trimmed.length > 0 ? trimmed : fallback;
        }
    };

    // =========================================================================
    // 3. RMS_UI - Các component và tiện ích giao diện dùng chung
    // =========================================================================
    const RMS_UI = {
        /**
         * Tạo HTML Huy hiệu trạng thái (StatusBadge) chuẩn hóa
         * @param {string} rawStatus - Trạng thái từ database
         * @param {boolean} showIcon - Có hiển thị icon không (mặc định true)
         * @returns {string} Mã HTML chuỗi
         */
        createStatusBadge: function (rawStatus, showIcon = true) {
            const info = resolveStatus(rawStatus);
            const iconHtml = showIcon ? `<i class="bi ${info.icon}" aria-hidden="true"></i>` : '';
            return `<span class="badge-status ${info.cssClass}" title="${info.description}">
                ${iconHtml}
                <span>${info.label}</span>
            </span>`;
        },

        /**
         * Gắn bộ xử lý tiền tệ tự động cho thẻ <input> (MoneyInput)
         * Người dùng nhìn thấy số có phân cách: "25.000.000"
         * Giá trị lấy qua `inputEl.dataset.rawValue` hoặc hàm callback trả về số nguyên thuần
         * @param {HTMLInputElement} inputEl
         * @param {Function} onChangeCallback - (rawValue: number) => void
         */
        attachMoneyInput: function (inputEl, onChangeCallback) {
            if (!inputEl) return;

            // Chuyển input sang type text để hiển thị dấu chấm phân cách hàng nghìn
            inputEl.type = 'text';
            inputEl.inputMode = 'numeric';
            inputEl.classList.add('tabular-nums');
            // Gỡ bỏ text-right nếu có để tránh hiện tượng toàn bộ số bị dồn nhảy sang trái khi gõ
            inputEl.classList.remove('text-right');

            let isComposing = false;

            function formatAndPosition() {
                const currentVal = inputEl.value;
                const cursor = inputEl.selectionStart || 0;

                // 1. Đếm số lượng chữ số nằm bên trái con trỏ trước khi format
                const digitsBeforeCursor = currentVal.slice(0, cursor).replace(/\D/g, '').length;

                // 2. Lọc chỉ lấy chữ số
                let cleanDigits = currentVal.replace(/\D/g, '');

                if (!cleanDigits) {
                    inputEl.dataset.rawValue = '0';
                    inputEl.value = '';
                    if (typeof onChangeCallback === 'function') onChangeCallback(0);
                    return;
                }

                // 3. Chuẩn hóa số 0 thừa ở đầu nếu theo sau là các chữ số khác (VD: "05" -> "5")
                if (cleanDigits.length > 1 && cleanDigits.startsWith('0')) {
                    cleanDigits = cleanDigits.replace(/^0+/, '') || '0';
                }

                const rawNum = parseInt(cleanDigits, 10) || 0;
                inputEl.dataset.rawValue = rawNum.toString();

                // 4. Format theo chuẩn hàng nghìn bằng dấu chấm (VD: 25.000.000)
                const formatted = cleanDigits.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
                inputEl.value = formatted;

                // 5. Xác định vị trí con trỏ mới:
                // Con trỏ cần đứng ngay sau chữ số thứ digitsBeforeCursor trong chuỗi formatted
                let newCursor = formatted.length;
                if (digitsBeforeCursor <= 0) {
                    newCursor = 0;
                } else if (digitsBeforeCursor >= cleanDigits.length) {
                    newCursor = formatted.length;
                } else {
                    let count = 0;
                    for (let i = 0; i < formatted.length; i++) {
                        if (formatted[i] >= '0' && formatted[i] <= '9') {
                            count++;
                            if (count === digitsBeforeCursor) {
                                newCursor = i + 1;
                                // Nếu ký tự kế tiếp là dấu chấm vừa sinh ra, tự động đưa con trỏ qua dấu chấm
                                if (newCursor < formatted.length && formatted[newCursor] === '.') {
                                    newCursor++;
                                }
                                break;
                            }
                        }
                    }
                }

                inputEl.setSelectionRange(newCursor, newCursor);

                if (typeof onChangeCallback === 'function') onChangeCallback(rawNum);
            }

            // Lắng nghe sự kiện gõ phím
            inputEl.addEventListener('input', function () {
                if (isComposing) return;
                formatAndPosition();
            });

            // Hỗ trợ bộ gõ tiếng Việt (Unikey / EVKey / Windows Telex)
            inputEl.addEventListener('compositionstart', function () {
                isComposing = true;
            });
            inputEl.addEventListener('compositionend', function () {
                isComposing = false;
                formatAndPosition();
            });

            // Xử lý thông minh khi bấm Backspace và Delete gần dấu chấm phân cách
            inputEl.addEventListener('keydown', function (e) {
                const cursor = inputEl.selectionStart;
                const selectionEnd = inputEl.selectionEnd;
                // Nếu đang bôi đen nhiều ký tự thì để trình duyệt xóa bình thường
                if (cursor !== selectionEnd) return;

                // Khi bấm Backspace ngay sau dấu chấm (VD: 22.|250.000)
                // Dịch con trỏ về trước dấu chấm (22|.250.000) và KHÔNG chặn default,
                // Trình duyệt sẽ xóa ký tự số '2' trước dấu chấm một cách tự nhiên mà không làm nhảy con trỏ
                if (e.key === 'Backspace' && cursor > 0 && inputEl.value[cursor - 1] === '.') {
                    inputEl.setSelectionRange(cursor - 1, cursor - 1);
                }

                // Khi bấm Delete ngay trước dấu chấm (VD: 22|.250.000)
                // Dịch con trỏ qua dấu chấm (22.|250.000) và KHÔNG chặn default,
                // Trình duyệt sẽ xóa chữ số ngay sau dấu chấm mà không bị kẹt dấu chấm
                if (e.key === 'Delete' && cursor < inputEl.value.length && inputEl.value[cursor] === '.') {
                    inputEl.setSelectionRange(cursor + 1, cursor + 1);
                }
            });

            // Hàm set giá trị bằng code (cho Edit modal, load data)
            inputEl.setRawValue = function (val) {
                if (val == null || val === '' || isNaN(val) || Number(val) === 0) {
                    inputEl.dataset.rawValue = '0';
                    inputEl.value = '';
                } else {
                    const clean = String(val).replace(/\D/g, '');
                    const num = parseInt(clean, 10) || 0;
                    inputEl.dataset.rawValue = num.toString();
                    inputEl.value = num > 0 ? clean.replace(/\B(?=(\d{3})+(?!\d))/g, '.') : '';
                }
                if (typeof onChangeCallback === 'function') {
                    onChangeCallback(parseInt(inputEl.dataset.rawValue || '0', 10));
                }
            };

            // Hàm lấy số nguyên thuần
            inputEl.getRawValue = function () {
                return parseInt(inputEl.dataset.rawValue || '0', 10);
            };
        },

        /**
         * Mở Modal với hiệu ứng, khóa cuộn trang chính và bẫy phím ESC
         * @param {HTMLElement|string} modalElementOrId
         */
        openModal: function (modalElementOrId) {
            const modal = typeof modalElementOrId === 'string'
                ? document.getElementById(modalElementOrId)
                : modalElementOrId;
            if (!modal) return;

            modal.classList.add('active');
            document.body.classList.add('modal-open');

            // Focus vào input đầu tiên nếu có
            const firstInput = modal.querySelector('input:not([type="hidden"]), select, textarea, button.btn-primary');
            if (firstInput) {
                setTimeout(() => firstInput.focus(), 100);
            }

            // Gán sự kiện ESC để đóng
            function handleKeyDown(e) {
                if (e.key === 'Escape') {
                    RMS_UI.closeModal(modal);
                    document.removeEventListener('keydown', handleKeyDown);
                }
            }
            document.addEventListener('keydown', handleKeyDown);
            modal._escHandler = handleKeyDown;
        },

        /**
         * Đóng Modal và mở khóa cuộn trang
         * @param {HTMLElement|string} modalElementOrId
         */
        closeModal: function (modalElementOrId) {
            const modal = typeof modalElementOrId === 'string'
                ? document.getElementById(modalElementOrId)
                : modalElementOrId;
            if (!modal) return;

            modal.classList.remove('active');

            // Kiểm tra xem còn modal nào đang mở không, nếu hết thì mở lại cuộn body
            const anyActiveModal = document.querySelector('.custom-modal-backdrop.active');
            if (!anyActiveModal) {
                document.body.classList.remove('modal-open');
            }

            if (modal._escHandler) {
                document.removeEventListener('keydown', modal._escHandler);
                modal._escHandler = null;
            }
        },

        /**
         * Hiển thị Toast thông báo nổi đạt chuẩn WCAG AA
         * @param {string} message - Nội dung thông báo
         * @param {'success'|'error'|'info'|'warning'} type
         * @param {number} duration - Thời gian hiển thị (ms)
         */
        showToast: function (message, type = 'success', duration = 4000) {
            let container = document.getElementById('toastContainer');
            if (!container) {
                container = document.createElement('div');
                container.id = 'toastContainer';
                container.className = 'toast-container';
                container.setAttribute('aria-live', 'polite');
                document.body.appendChild(container);
            }

            const toast = document.createElement('div');
            toast.className = `toast-msg toast-${type}`;
            toast.setAttribute('role', 'alert');

            let iconClass = 'bi-check-circle-fill';
            if (type === 'error') iconClass = 'bi-exclamation-triangle-fill';
            else if (type === 'warning') iconClass = 'bi-exclamation-circle-fill';
            else if (type === 'info') iconClass = 'bi-info-circle-fill';

            toast.innerHTML = `
                <i class="bi ${iconClass}" aria-hidden="true" style="font-size: 1.15rem; flex-shrink: 0;"></i>
                <div style="flex: 1; font-size: 0.9rem; line-height: 1.4;">${message}</div>
                <button type="button" aria-label="Đóng thông báo" style="background: none; border: none; color: white; opacity: 0.7; cursor: pointer; padding: 2px 4px; font-size: 1.1rem; line-height: 1;">&times;</button>
            `;

            const closeBtn = toast.querySelector('button');
            function removeToast() {
                toast.style.opacity = '0';
                toast.style.transform = 'translateY(10px)';
                toast.style.transition = 'all 0.25s ease';
                setTimeout(() => toast.remove(), 250);
            }

            closeBtn.addEventListener('click', removeToast);
            container.appendChild(toast);

            setTimeout(removeToast, duration);
        },

        /**
         * Tạo mã HTML Empty State trực quan khi bảng/danh sách không có dữ liệu
         * @param {Object} options { icon, title, description, actionText, actionId }
         * @returns {string}
         */
        createEmptyStateHtml: function (options = {}) {
            const icon = options.icon || 'bi-inbox';
            const title = options.title || 'Không tìm thấy dữ liệu phù hợp';
            const desc = options.description || 'Vui lòng thử thay đổi từ khóa tìm kiếm hoặc điều chỉnh lại bộ lọc trạng thái.';
            const actionBtn = options.actionText && options.actionId
                ? `<button type="button" id="${options.actionId}" class="btn btn-secondary mt-3">
                       <i class="bi bi-arrow-clockwise" aria-hidden="true"></i> ${options.actionText}
                   </button>`
                : '';

            return `
                <div class="empty-state-box">
                    <div class="empty-state-icon">
                        <i class="bi ${icon}" aria-hidden="true"></i>
                    </div>
                    <h5 class="empty-state-title">${title}</h5>
                    <p class="empty-state-desc">${desc}</p>
                    ${actionBtn}
                </div>
            `;
        },

        /**
         * Tạo mã HTML Loading Skeleton cho bảng
         * @param {number} cols
         * @param {number} rows
         * @returns {string}
         */
        createTableSkeletonHtml: function (cols = 8, rows = 5) {
            let html = '';
            for (let r = 0; r < rows; r++) {
                html += `<tr>`;
                for (let c = 0; c < cols; c++) {
                    const widthPercent = 40 + Math.floor(Math.random() * 45);
                    html += `
                        <td>
                            <div class="skeleton-shimmer" style="height: 16px; width: ${widthPercent}%; border-radius: 4px;"></div>
                        </td>
                    `;
                }
                html += `</tr>`;
            }
            return html;
        }
    };

    // Xuất ra phạm vi toàn cục (global)
    window.RMS_STATUS_MAP = RMS_STATUS_MAP;
    window.RMS_FORMATTER = RMS_FORMATTER;
    window.RMS_UI = RMS_UI;

})(window);
