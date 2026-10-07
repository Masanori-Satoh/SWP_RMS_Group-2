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
// 2. MODAL & TOAST UX HELPERS
// =========================================================================
function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('active');
}

function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove('active');
}

function handleCreateModalBackdropClick(event) {
    if (event.target && event.target.id === 'createOfferModal') {
        closeModal('createOfferModal');
    }
}

function showToast(message, type = 'info') {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    let icon = 'fa-info-circle';
    if (type === 'success') icon = 'fa-check-circle';
    else if (type === 'danger') icon = 'fa-exclamation-circle';
    else if (type === 'warning') icon = 'fa-triangle-exclamation';

    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 200);
    }, 3000);
}

function getTodayDateString() {
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
}

function getTomorrowDateString() {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const yyyy = tomorrow.getFullYear();
    const mm = String(tomorrow.getMonth() + 1).padStart(2, '0');
    const dd = String(tomorrow.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
}

// =========================================================================
// 3. TƯƠNG TÁC BIỂU MẪU TẠO MỚI / CHỈNH SỬA & POP-UP CREATE MODAL
// =========================================================================

/**
 * Mở Pop-up Modal Tạo mới Offer Proposal (Screen 30 Create Modal)
 */
function openCreateOfferModal() {
    const form = document.getElementById('createOfferForm');
    if (form) form.reset();
    const grpA = document.getElementById('groupASection');
    if (grpA) grpA.style.display = 'none';

    // Ràng buộc lịch: Ngày bắt đầu dự kiến phải > ngày hiện tại (tối thiểu là ngày mai)
    const startDateInput = document.getElementById('createStartDate') || document.getElementById('expectedStartDate');
    if (startDateInput) {
        startDateInput.min = getTomorrowDateString();
    }
    const probationDaysInput = document.getElementById('createProbationDays') || document.getElementById('probationDays');
    if (probationDaysInput) {
        probationDaysInput.value = '60';
    }
    const workLocInput = document.getElementById('createWorkLocation') || document.getElementById('workLocation');
    if (workLocInput) {
        workLocInput.value = '';
    }

    openModal('createOfferModal');
}

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

    const defaultWorkLoc = opt.getAttribute('data-work-location') || 'Trụ sở chính Mộc RMS';
    setElemText('dispWorkLocation', defaultWorkLoc);

    if (grpA) grpA.style.display = 'block';

    // Tự động gợi ý chức danh và mức lương nếu trường đang để trống
    const titleInput = document.getElementById('createOfferedTitle') || document.getElementById('offeredPositionTitle');
    if (titleInput && (!titleInput.value.trim() || titleInput.value.trim() === '')) {
        titleInput.value = opt.getAttribute('data-pos') || '';
    }

    const proposedInput = document.getElementById('createProposedSalary') || document.getElementById('proposedSalary');
    if (proposedInput && recom) {
        proposedInput.value = recom;
        autoCalculateProbationSalary(true);
    }

    const workLocInput = document.getElementById('createWorkLocation') || document.getElementById('workLocation');
    if (workLocInput && (!workLocInput.value.trim() || workLocInput.value.trim() === '')) {
        workLocInput.value = defaultWorkLoc;
    }
}

/**
 * Tự động tính toán mức lương thử việc tối thiểu 85% (theo quy định BR-OFF-01)
 */
function autoCalculateProbationSalary(forceSet = false) {
    const proposedEl = document.getElementById('createProposedSalary') || document.getElementById('proposedSalary');
    const probationEl = document.getElementById('createProbationSalary') || document.getElementById('probationSalary');
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
    const proposedEl = document.getElementById('createProposedSalary') || document.getElementById('proposedSalary');
    const probationEl = document.getElementById('createProbationSalary') || document.getElementById('probationSalary');
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

/**
 * Nộp dữ liệu tạo Offer Proposal từ Pop-up Modal (Save Draft hoặc Submit Director)
 */
function submitCreateOffer(isDraft) {
    const select = document.getElementById('createAppSelect');
    const appId = select ? select.value : '';
    const titleInput = document.getElementById('createOfferedTitle') || document.getElementById('offeredPositionTitle');
    const title = titleInput ? titleInput.value.trim() : '';
    const proposedEl = document.getElementById('createProposedSalary') || document.getElementById('proposedSalary');
    const proposed = proposedEl ? parseFloat(proposedEl.value) : NaN;
    const probationEl = document.getElementById('createProbationSalary') || document.getElementById('probationSalary');
    const probation = probationEl ? parseFloat(probationEl.value) : NaN;
    const probationDaysEl = document.getElementById('createProbationDays') || document.getElementById('probationDays');
    const probationDays = probationDaysEl ? parseInt(probationDaysEl.value, 10) : NaN;
    const startDateEl = document.getElementById('createStartDate') || document.getElementById('expectedStartDate');
    const startDate = startDateEl ? startDateEl.value : '';
    const workLocEl = document.getElementById('createWorkLocation') || document.getElementById('workLocation');
    const workLocation = workLocEl ? workLocEl.value.trim() : '';
    const benefitsEl = document.getElementById('createBenefits') || document.getElementById('benefitsPackage');
    const benefits = benefitsEl ? benefitsEl.value : '';

    // 1. Bắt buộc chọn ứng viên
    if (!appId) {
        showToast('Vui lòng chọn ứng viên đỗ phỏng vấn.', 'warning');
        return;
    }

    // 2. Bắt buộc nhập vị trí chức danh
    if (!title) {
        showToast('Vị trí chức danh đề xuất không được để trống.', 'warning');
        return;
    }

    // 3. Mức lương chính thức phải > 0
    if (isNaN(proposed) || proposed <= 0) {
        showToast('Mức lương chính thức phải lớn hơn 0.', 'warning');
        return;
    }

    // 4. Mức lương thử việc phải > 0
    if (isNaN(probation) || probation <= 0) {
        showToast('Mức lương thử việc phải lớn hơn 0.', 'warning');
        return;
    }

    // 5. Tuân thủ quy định: Lương thử việc >= 85% và <= 100% lương chính thức
    if (!validateProbationRuleUi()) {
        if (probation > proposed) {
            showToast('Lương thử việc không được vượt quá lương chính thức.', 'danger');
        } else {
            showToast('Mức lương thử việc không tuân thủ quy định tối thiểu 85%.', 'danger');
        }
        return;
    }

    // 6. Thời gian thử việc phải > 0
    if (isNaN(probationDays) || probationDays <= 0) {
        showToast('Thời gian thử việc phải lớn hơn 0 ngày.', 'warning');
        return;
    }

    // 7. Ngày bắt đầu dự kiến bắt buộc và phải > ngày hiện tại
    if (!startDate) {
        showToast('Ngày bắt đầu dự kiến không được để trống.', 'warning');
        return;
    }
    if (startDate <= getTodayDateString()) {
        showToast('Ngày bắt đầu dự kiến phải lớn hơn ngày hiện tại.', 'danger');
        return;
    }

    // 8. Địa điểm làm việc bắt buộc
    if (!workLocation) {
        showToast('Địa điểm làm việc không được để trống.', 'warning');
        return;
    }

    const payload = {
        applicationId: parseInt(appId, 10),
        offeredPositionTitle: title,
        proposedSalary: proposed,
        probationSalary: probation,
        probationDays: probationDays,
        expectedStartDate: startDate,
        workLocation: workLocation,
        benefitsPackage: benefits,
        isDraft: Boolean(isDraft)
    };

    const csrf = getCsrfInfo();
    const headers = {
        'Content-Type': 'application/json'
    };
    if (csrf.token) {
        headers[csrf.header] = csrf.token;
    }

    fetch('/offers/create', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(payload)
    })
    .then(async res => {
        const isJson = res.headers.get('content-type')?.includes('application/json');
        const data = isJson ? await res.json() : null;
        if (!res.ok) {
            const msg = data?.message || `Lỗi máy chủ (${res.status})`;
            throw new Error(msg);
        }
        return data;
    })
    .then(res => {
        if (res && res.success) {
            showToast(res.message, 'success');
            closeModal('createOfferModal');
            setTimeout(() => window.location.reload(), 800);
        } else {
            showToast(res?.message || 'Lỗi khi tạo Offer', 'danger');
        }
    })
    .catch(err => {
        showToast(err.message, 'danger');
    });
}

// =========================================================================
// 4. KHỞI TẠO RÀNG BUỘC KHI TẢI TRANG (DOM Initialization)
// =========================================================================
document.addEventListener('DOMContentLoaded', function () {
    // 1. Ràng buộc ngày bắt đầu dự kiến: tối thiểu phải lớn hơn ngày hiện tại (từ ngày mai)
    const startDateInput = document.getElementById('createStartDate') || document.getElementById('expectedStartDate');
    if (startDateInput) {
        startDateInput.min = getTomorrowDateString();
    }

    // 2. Nếu đang mở form tạo mới và đã có ứng viên được chọn (hoặc khi validate error quay lại)
    const select = document.getElementById('createAppSelect');
    if (select && select.value) {
        handleSelectPassedCandidate(select.value);
    }

    // 3. Kiểm tra tính hợp lệ của tỷ lệ lương nếu đang có dữ liệu
    validateProbationRuleUi();
});

// =========================================================================
// 4. QUẢN LÝ CHỌN OFFER & XUẤT EXCEL (Excel Export & Selection UX)
// =========================================================================

/**
 * Lấy CSRF token và header name từ thẻ meta tag
 */
function getCsrfInfo() {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    return { token, header };
}

/**
 * Lấy danh sách ID các Offer đang được chọn qua checkbox
 */
function getSelectedOfferIds() {
    const checkboxes = document.querySelectorAll('.offer-row-checkbox:checked');
    return Array.from(checkboxes).map(cb => parseInt(cb.value, 10)).filter(id => !isNaN(id));
}

/**
 * Xử lý khi click checkbox "Chọn tất cả" trên header bảng
 */
function toggleSelectAllOffers(isChecked) {
    const rowCheckboxes = document.querySelectorAll('.offer-row-checkbox');
    rowCheckboxes.forEach(cb => {
        cb.checked = isChecked;
    });
    updateSelectionToolbar();
}

/**
 * Xử lý khi trạng thái checkbox từng dòng thay đổi
 */
function handleOfferRowSelectionChange() {
    const total = document.querySelectorAll('.offer-row-checkbox').length;
    const checked = document.querySelectorAll('.offer-row-checkbox:checked').length;
    const headerCheckbox = document.getElementById('selectAllOffers');

    if (headerCheckbox) {
        headerCheckbox.checked = (total > 0 && total === checked);
        headerCheckbox.indeterminate = (checked > 0 && checked < total);
    }

    updateSelectionToolbar();
}

/**
 * Bỏ chọn tất cả các Offer
 */
function deselectAllOffers() {
    const checkboxes = document.querySelectorAll('.offer-row-checkbox, #selectAllOffers');
    checkboxes.forEach(cb => {
        cb.checked = false;
        if (cb.indeterminate) cb.indeterminate = false;
    });
    updateSelectionToolbar();
}

/**
 * Cập nhật hiển thị Selection Toolbar và đếm số lượng bản ghi đã chọn
 */
function updateSelectionToolbar() {
    const selectedIds = getSelectedOfferIds();
    const count = selectedIds.length;

    const toolbar = document.getElementById('selectionToolbar');
    const countText = document.getElementById('selectedOffersCount');
    const btnCountText = document.getElementById('selectedOffersBtnCount');
    const modalScopeCount = document.getElementById('scopeSelectedCount');
    const selectedRadio = document.querySelector('input[name="exportScope"][value="SELECTED"]');
    const selectedWrapper = document.getElementById('scopeSelectedWrapper');

    if (countText) countText.textContent = count;
    if (btnCountText) btnCountText.textContent = count;
    if (modalScopeCount) modalScopeCount.textContent = count;

    if (toolbar) {
        toolbar.style.display = (count > 0) ? 'flex' : 'none';
    }

    if (selectedRadio && selectedWrapper) {
        if (count === 0) {
            selectedRadio.disabled = true;
            selectedWrapper.classList.add('disabled');
            if (selectedRadio.checked) {
                const filteredRadio = document.querySelector('input[name="exportScope"][value="FILTERED"]');
                if (filteredRadio) filteredRadio.checked = true;
            }
        } else {
            selectedRadio.disabled = false;
            selectedWrapper.classList.remove('disabled');
        }
    }
}

/**
 * Mở modal xuất Excel
 */
function openExportModal() {
    updateSelectionToolbar();

    const errDiv = document.getElementById('exportModalError');
    if (errDiv) errDiv.style.display = 'none';

    // Cập nhật mô tả điều kiện lọc hiện tại
    const urlParams = new URLSearchParams(window.location.search);
    const search = urlParams.get('search');
    const status = urlParams.get('status');
    const filteredDesc = document.getElementById('scopeFilteredDesc');
    if (filteredDesc) {
        const parts = [];
        if (search) parts.push(`Từ khóa: "${search}"`);
        if (status && status !== 'ALL') parts.push(`Trạng thái: ${status}`);
        if (parts.length > 0) {
            filteredDesc.textContent = `Xuất toàn bộ kết quả lọc (${parts.join(', ')})`;
        } else {
            filteredDesc.textContent = 'Xuất tất cả đề xuất phù hợp với bộ lọc hiện tại (bỏ phân trang)';
        }
    }

    const modal = document.getElementById('offerExportModal');
    if (modal) {
        modal.classList.add('active');
        modal.style.display = 'flex';
    }
}

/**
 * Mở modal xuất Excel và chọn sẵn scope
 */
function openExportModalWithScope(scope) {
    openExportModal();
    const targetRadio = document.querySelector(`input[name="exportScope"][value="${scope}"]`);
    if (targetRadio && !targetRadio.disabled) {
        targetRadio.checked = true;
    }
}

/**
 * Đóng modal xuất Excel
 */
function closeExportModal() {
    const modal = document.getElementById('offerExportModal');
    if (modal) {
        modal.classList.remove('active');
        modal.style.display = 'none';
    }
}

function handleExportModalBackdropClick(e) {
    if (e.target && e.target.id === 'offerExportModal') {
        closeExportModal();
    }
}

function handleScopeChange() {
    // Có thể bổ sung hiệu ứng highlight tùy chọn nếu cần
}

/**
 * Helper chọn nhanh các checkbox cột dữ liệu
 */
function selectAllColumns() {
    document.querySelectorAll('input[name="exportCol"]').forEach(cb => cb.checked = true);
}

function deselectAllColumns() {
    document.querySelectorAll('input[name="exportCol"]').forEach(cb => cb.checked = false);
}

function resetDefaultColumns() {
    const defaultCols = new Set([
        'offerId', 'candidateName', 'candidateEmail', 'offeredPositionTitle',
        'proposedSalary', 'probationSalary', 'probationPercentage', 'offerStatus',
        'expectedStartDate', 'workLocation', 'createdAt', 'updatedAt'
    ]);
    document.querySelectorAll('input[name="exportCol"]').forEach(cb => {
        cb.checked = defaultCols.has(cb.value);
    });
}

/**
 * Thực hiện xuất file Excel qua AJAX và tải file nhị phân Blob về máy
 */
function executeOfferExport() {
    const errDiv = document.getElementById('exportModalError');
    const errText = document.getElementById('exportModalErrorText');
    const showErr = (msg) => {
        if (errDiv && errText) {
            errText.textContent = msg;
            errDiv.style.display = 'flex';
        }
    };

    if (errDiv) errDiv.style.display = 'none';

    // 1. Thu thập dữ liệu form modal
    const scopeRadio = document.querySelector('input[name="exportScope"]:checked');
    const scope = scopeRadio ? scopeRadio.value : 'FILTERED';

    const selectedCols = Array.from(document.querySelectorAll('input[name="exportCol"]:checked')).map(cb => cb.value);
    if (selectedCols.length === 0) {
        showErr('Vui lòng chọn ít nhất một cột dữ liệu để xuất.');
        return;
    }

    const selectedIds = getSelectedOfferIds();
    if (scope === 'SELECTED' && selectedIds.length === 0) {
        showErr('Vui lòng chọn ít nhất một đề xuất qua checkbox để xuất theo phạm vi này.');
        return;
    }

    // 2. Thu thập bộ lọc hiện tại từ URL query params
    const urlParams = new URLSearchParams(window.location.search);
    const filters = {
        keyword: urlParams.get('search') || '',
        status: urlParams.get('status') || 'ALL',
        sort: urlParams.get('timeSort') || 'DEFAULT'
    };

    const payload = {
        scope: scope,
        offerIds: (scope === 'SELECTED') ? selectedIds : [],
        filters: filters,
        columns: selectedCols
    };

    // 3. Cập nhật trạng thái loading cho nút bấm
    const btn = document.getElementById('btnExecuteExport');
    const originalBtnHtml = btn ? btn.innerHTML : '';
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang tạo file...';
    }

    // 4. Gửi request POST
    const { token, header } = getCsrfInfo();
    const reqHeaders = {
        'Content-Type': 'application/json'
    };
    if (token) {
        reqHeaders[header] = token;
    }

    fetch('/offers/export', {
        method: 'POST',
        headers: reqHeaders,
        body: JSON.stringify(payload)
    })
    .then(async response => {
        if (!response.ok) {
            let errorMsg = 'Đã xảy ra lỗi khi tạo tệp Excel.';
            try {
                const json = await response.json();
                if (json && json.message) errorMsg = json.message;
            } catch (e) {
                if (response.status === 403) {
                    errorMsg = 'Bạn không có quyền hạn xuất dữ liệu Offer.';
                }
            }
            throw new Error(errorMsg);
        }

        // Đọc tên file từ Content-Disposition nếu có
        let filename = 'offers.xlsx';
        const disposition = response.headers.get('Content-Disposition');
        if (disposition && disposition.indexOf('filename=') !== -1) {
            const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(disposition);
            if (matches != null && matches[1]) {
                filename = matches[1].replace(/['"]/g, '');
            }
        }

        return response.blob().then(blob => ({ blob, filename }));
    })
    .then(({ blob, filename }) => {
        // Kích hoạt browser download file nhị phân
        const downloadUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = downloadUrl;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(downloadUrl);
        a.remove();

        closeExportModal();
        showOfferToast('Xuất dữ liệu Excel thành công!', 'success');
    })
    .catch(err => {
        showErr(err.message || 'Lỗi khi xuất file Excel.');
        showOfferToast(err.message || 'Lỗi khi xuất file Excel.', 'danger');
    })
    .finally(() => {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = originalBtnHtml;
        }
    });
}

/**
 * Hiển thị thông báo Toast góc dưới màn hình
 */
function showOfferToast(message, type = 'info') {
    let container = document.getElementById('offerToastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'offerToastContainer';
        container.style.position = 'fixed';
        container.style.bottom = '24px';
        container.style.right = '24px';
        container.style.zIndex = '99999';
        container.style.display = 'flex';
        container.style.flexDirection = 'column';
        container.style.gap = '8px';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `custom-toast toast-${type}`;
    const iconClass = (type === 'success') ? 'fa-circle-check' :
                      (type === 'danger')  ? 'fa-triangle-exclamation' : 'fa-circle-info';

    toast.innerHTML = `<i class="fa-solid ${iconClass}"></i><span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}
