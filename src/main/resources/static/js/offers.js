/**
 * JavaScript logic cho phân hệ Offer Management (Screen 30 & Screen 32)
 * Tuân thủ quy chuẩn ARCHITECTURE_GUIDE.md:
 * - Thuần túy bổ trợ UX và tương tác giao diện người dùng.
 * - Toàn bộ luồng dữ liệu chính do Spring MVC và Thymeleaf Server-Side xử lý.
 */

// =========================================================================
// 1. BỘ LỌC VÀ TÌM KIẾM DỮ LIỆU DANH SÁCH (Screen 30 List Filters)
// =========================================================================
function applyOfferFilters() {
    const search = (document.getElementById('searchInput')?.value || '').trim();
    const status = document.getElementById('statusFilter')?.value || 'ALL';
    const timeSort = document.getElementById('timeSortFilter')?.value || 'DEFAULT';

    const url = new URL(window.location.origin + window.location.pathname);
    if (search) {
        url.searchParams.set('search', search);
    } else {
        url.searchParams.delete('search');
    }

    if (status && status !== 'ALL') {
        url.searchParams.set('status', status);
    } else {
        url.searchParams.delete('status');
    }

    if (timeSort && timeSort !== 'DEFAULT') {
        url.searchParams.set('timeSort', timeSort);
    } else {
        url.searchParams.delete('timeSort');
    }

    url.searchParams.set('page', '0'); // Reset về trang đầu khi thay đổi tiêu chí lọc
    window.location.href = url.toString();
}

function handleFilterSearch() {
    applyOfferFilters();
}

function clearSearchInput() {
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.value = '';
    }
    applyOfferFilters();
}

// =========================================================================
// 2. TƯƠNG TÁC BIỂU MẪU TẠO MỚI / CHỈNH SỬA (Form UX Helpers)
// =========================================================================

/**
 * Xử lý khi người dùng chọn ứng viên đỗ phỏng vấn từ dropdown ở màn hình tạo mới
 */
function handleSelectPassedCandidate(appId) {
    const select = document.getElementById('createAppSelect');
    if (!select) return;
    const opt = select.options[select.selectedIndex];
    const grpA = document.getElementById('groupASection');

    if (!appId || !opt || !opt.value) {
        if (grpA) grpA.style.display = 'none';
        return;
    }

    // Điền dữ liệu vào khung preview Group A
    const setElemText = (id, val) => {
        const el = document.getElementById(id);
        if (el) el.textContent = val || '-';
    };

    setElemText('dispCandId', opt.getAttribute('data-cand-id') ? '#' + opt.getAttribute('data-cand-id') : '-');
    setElemText('dispCandName', opt.getAttribute('data-cand-name'));
    setElemText('dispCandEmail', opt.getAttribute('data-email'));
    setElemText('dispCandPhone', opt.getAttribute('data-phone'));
    setElemText('dispPosition', opt.getAttribute('data-pos'));
    setElemText('dispDept', opt.getAttribute('data-dept'));
    setElemText('dispReqId', opt.getAttribute('data-req-id') ? '#' + opt.getAttribute('data-req-id') : '-');
    setElemText('dispHm', opt.getAttribute('data-hm'));

    const recom = opt.getAttribute('data-recom-salary');
    setElemText('dispRecomSalary', recom ? Number(recom).toLocaleString('vi-VN') + ' VND' : 'Chưa có gợi ý');
    setElemText('dispInterviewComments', opt.getAttribute('data-comments') || 'Không có ghi chú thêm.');

    if (grpA) grpA.style.display = 'block';

    // Tự động gợi ý chức danh và mức lương nếu trường đang để trống
    const titleInput = document.getElementById('offeredPositionTitle');
    if (titleInput && !titleInput.value.trim()) {
        titleInput.value = opt.getAttribute('data-pos') || '';
    }

    const proposedInput = document.getElementById('proposedSalary');
    if (proposedInput && (!proposedInput.value || proposedInput.value === '0') && recom) {
        proposedInput.value = recom;
        autoCalculateProbationSalary(true);
    }

    const workLocInput = document.getElementById('workLocation');
    if (workLocInput && !workLocInput.value.trim()) {
        workLocInput.value = opt.getAttribute('data-work-location') || 'Trụ sở chính Mộc RMS';
    }
}

/**
 * Tự động tính toán mức lương thử việc tối thiểu 85% (theo quy định BR-OFF-01)
 */
function autoCalculateProbationSalary(forceSet = false) {
    const proposedEl = document.getElementById('proposedSalary');
    const probationEl = document.getElementById('probationSalary');
    if (!proposedEl || !probationEl) return;

    const proposed = parseFloat(proposedEl.value);
    if (!isNaN(proposed) && proposed > 0) {
        const minProbation = Math.round(proposed * 0.85);
        const currentProbation = parseFloat(probationEl.value);
        if (forceSet || isNaN(currentProbation) || currentProbation === 0) {
            probationEl.value = minProbation;
        }
        validateProbationRuleUi();
    }
}

/**
 * Kiểm tra và hiển thị phản hồi trực quan về tỷ lệ lương thử việc trên giao diện
 */
function validateProbationRuleUi() {
    const proposedEl = document.getElementById('proposedSalary');
    const probationEl = document.getElementById('probationSalary');
    const notice = document.getElementById('probationRuleNotice');
    if (!proposedEl || !probationEl || !notice) return true;

    const proposed = parseFloat(proposedEl.value);
    const probation = parseFloat(probationEl.value);

    if (!isNaN(proposed) && !isNaN(probation)) {
        const minProb = proposed * 0.85;
        if (probation < minProb) {
            notice.style.color = 'var(--error)';
            notice.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i> CẢNH BÁO: Lương thử việc phải đạt tối thiểu 85% lương chính thức (${Math.round(minProb).toLocaleString('vi-VN')} VND).`;
            return false;
        } else if (probation > proposed) {
            notice.style.color = 'var(--error)';
            notice.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i> CẢNH BÁO: Lương thử việc không được vượt quá lương chính thức (${Math.round(proposed).toLocaleString('vi-VN')} VND).`;
            return false;
        } else {
            const pct = (probation / proposed * 100).toFixed(1);
            notice.style.color = 'var(--brand-dark)';
            notice.innerHTML = `<i class="fa-solid fa-circle-check"></i> Hợp lệ: Đạt ${pct}% lương chính thức.`;
            return true;
        }
    }
    return true;
}

// =========================================================================
// 3. KHỞI TẠO RÀNG BUỘC KHI TẢI TRANG (DOM Initialization)
// =========================================================================
document.addEventListener('DOMContentLoaded', function () {
    // 1. Ràng buộc ngày bắt đầu dự kiến: tối thiểu phải lớn hơn ngày hiện tại (từ ngày mai)
    const startDateInput = document.getElementById('expectedStartDate');
    if (startDateInput) {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        const yyyy = tomorrow.getFullYear();
        const mm = String(tomorrow.getMonth() + 1).padStart(2, '0');
        const dd = String(tomorrow.getDate()).padStart(2, '0');
        startDateInput.min = `${yyyy}-${mm}-${dd}`;
    }

    // 2. Nếu đang mở form tạo mới và đã có ứng viên được chọn (hoặc khi validate error quay lại)
    const select = document.getElementById('createAppSelect');
    if (select && select.value) {
        handleSelectPassedCandidate(select.value);
    }

    // 3. Kiểm tra tính hợp lệ của tỷ lệ lương nếu đang có dữ liệu
    validateProbationRuleUi();
});
