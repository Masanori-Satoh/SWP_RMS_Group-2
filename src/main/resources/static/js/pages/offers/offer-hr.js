/**
 * offer-hr.js — Phân hệ Offer Management (SWP391 — Mộc RMS Group 2)
 * Trách nhiệm luồng nghiệp vụ của Chuyên viên Tuyển dụng (HR / Recruiter):
 * - SECTION A: Quản lý danh sách, bộ lọc, chọn hàng loạt và xóa bản nháp Offer (HR List)
 * - SECTION B: Biểu mẫu tạo mới / chỉnh sửa, autofill ứng viên, tính 85% lương và ngày bắt đầu động (Form)
 * - SECTION C: Quản lý hộp thoại xuất Excel, tùy biến 25 cột dữ liệu và tải Blob (Excel Export)
 * - SECTION D: Khởi tạo sự kiện theo sự hiện diện của phần tử DOM (Initialization)
 */

// =========================================================================
// SECTION A — HR OFFER LIST (Screen 30 List Filters & Row Selection)
// =========================================================================

/**
 * Áp dụng tiêu chí tìm kiếm, lọc trạng thái và sắp xếp thời gian cho danh sách HR
 */
function applyOfferFilters() {
    const search = (document.getElementById('searchInput')?.value || '').trim();
    const status = document.getElementById('statusFilter')?.value || 'ALL';
    const timeSort = document.getElementById('timeSortFilter')?.value || 'DEFAULT';

    const url = new URL(window.location.origin + window.location.pathname);
    const filterMap = {
        search: search || null,
        status: (status && status !== 'ALL') ? status : null,
        timeSort: (timeSort && timeSort !== 'DEFAULT') ? timeSort : null
    };

    Object.entries(filterMap).forEach(([key, val]) => {
        if (val) url.searchParams.set(key, val);
        else url.searchParams.delete(key);
    });

    url.searchParams.set('page', '0'); // Reset về trang đầu khi thay đổi tiêu chí lọc
    window.location.href = url.toString();
}

function filterOffersByStatus(status) {
    const statusFilter = document.getElementById('statusFilter');
    if (statusFilter) {
        statusFilter.value = status || 'ALL';
    }
    applyOfferFilters();
}

function clearSearchInput() {
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.value = '';
    }
    applyOfferFilters();
}

/**
 * Xác nhận và gọi API xóa bản nháp đề xuất (Chỉ áp dụng cho trạng thái Draft)
 */
function confirmDeleteOffer(offerId) {
    if (!offerId) return;
    if (!confirm('Bạn có chắc chắn muốn xóa bản nháp đề xuất này khỏi hệ thống?')) {
        return;
    }

    const csrf = (typeof getCsrfInfo === 'function') ? getCsrfInfo() : { token: '', header: 'X-CSRF-TOKEN' };
    const headers = { 'Accept': 'application/json' };
    if (csrf.token) {
        headers[csrf.header] = csrf.token;
    }

    fetch(`/offers/${offerId}`, {
        method: 'DELETE',
        headers: headers
    })
        .then(async res => {
            const isJson = res.headers.get('content-type')?.includes('application/json');
            const data = isJson ? await res.json() : null;
            if (!res.ok) {
                throw new Error(data?.message || `Lỗi xóa bản nháp (${res.status})`);
            }
            return data;
        })
        .then(res => {
            if (res && res.success) {
                try {
                    sessionStorage.setItem(OFFER_PENDING_TOAST_KEY, JSON.stringify({
                        message: res.message || 'Đã xóa bản nháp đề xuất thành công.',
                        type: 'success'
                    }));
                } catch (e) { }
                window.location.reload();
            } else if (typeof showFlashToast === 'function') {
                showFlashToast(res?.message || 'Lỗi xóa bản nháp.', 'error');
            }
        })
        .catch(err => {
            if (typeof showFlashToast === 'function') {
                showFlashToast(err.message, 'error');
            }
        });
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
    document.querySelectorAll('.offer-row-checkbox').forEach(cb => {
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
    document.querySelectorAll('.offer-row-checkbox, #selectAllOffers').forEach(cb => {
        cb.checked = false;
        if (cb.indeterminate) cb.indeterminate = false;
    });
    updateSelectionToolbar();
}

/**
 * Cập nhật hiển thị Selection Toolbar và đếm số lượng bản ghi đã chọn
 */
function updateSelectionToolbar() {
    const count = getSelectedOfferIds().length;

    ['selectedOffersCount', 'selectedOffersBtnCount', 'scopeSelectedCount'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.textContent = count;
    });

    const toolbar = document.getElementById('selectionToolbar');
    if (toolbar) {
        toolbar.classList.toggle('active', count > 0);
        toolbar.hidden = count === 0;
    }

    const selectedRadio = document.querySelector('input[name="exportScope"][value="SELECTED"]');
    const selectedWrapper = document.getElementById('scopeSelectedWrapper');
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

// =========================================================================
// SECTION B — CREATE / EDIT OFFER FORM (Screen 30 Form Logic)
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
        if (grpA) grpA.hidden = true;
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
    setElemText('dispRecomSalary', recom ? Number(recom).toLocaleString('vi-VN') + ' VNĐ' : 'Chưa có gợi ý');
    setElemText('dispInterviewComments', opt.getAttribute('data-comments') || 'Không có ghi chú thêm.');

    const defaultWorkLoc = opt.getAttribute('data-work-location') || 'Trụ sở chính Mộc RMS';
    setElemText('dispWorkLocation', defaultWorkLoc);

    if (grpA) grpA.hidden = false;

    // Tự động gợi ý chức danh và mức lương nếu trường đang để trống
    const titleInput = document.getElementById('offeredPositionTitle');
    if (titleInput && !titleInput.value.trim()) {
        titleInput.value = opt.getAttribute('data-pos') || '';
    }

    const proposedInput = document.getElementById('proposedSalary');
    if (proposedInput && recom) {
        const numericVal = Math.round(parseFloat(recom));
        proposedInput.value = isNaN(numericVal) ? recom : numericVal;
        autoCalculateProbationSalary(true);
    }

    const workLocInput = document.getElementById('workLocation');
    if (workLocInput && !workLocInput.value.trim()) {
        workLocInput.value = defaultWorkLoc;
    }
}

/**
 * Tự động tính toán mức lương thử việc tối thiểu 85% (theo quy định BR-OFF-01 & Luật Lao Động 2019)
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

const PROBATION_ICONS = {
    warn: '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="m10.29 3.86-8.58 15A2 2 0 0 0 3.43 22h17.14a2 2 0 0 0 1.72-3.14l-8.58-15a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>',
    ok: '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>'
};

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
            notice.classList.add('ofw-rule-notice--invalid');
            notice.style.color = 'var(--error)';
            notice.innerHTML = `${PROBATION_ICONS.warn} <span>CẢNH BÁO: Lương thử việc phải đạt tối thiểu 85% lương chính thức (${Math.round(minProb).toLocaleString('vi-VN')} VNĐ).</span>`;
            return false;
        } else if (probation > proposed) {
            notice.classList.add('ofw-rule-notice--invalid');
            notice.style.color = 'var(--error)';
            notice.innerHTML = `${PROBATION_ICONS.warn} <span>CẢNH BÁO: Lương thử việc không được vượt quá lương chính thức (${Math.round(proposed).toLocaleString('vi-VN')} VNĐ).</span>`;
            return false;
        } else {
            const pct = (probation / proposed * 100).toFixed(1);
            notice.classList.remove('ofw-rule-notice--invalid');
            notice.style.color = 'var(--brand-dark)';
            notice.innerHTML = `${PROBATION_ICONS.ok} <span>Hợp lệ: Đạt ${pct}% lương chính thức.</span>`;
            return true;
        }
    }
    return true;
}

/**
 * Cập nhật động ngày bắt đầu tối thiểu: min = hôm nay + thời hạn phản hồi + 1 ngày
 */
function updateMinExpectedStartDate() {
    const periodInput = document.getElementById('responsePeriodDays');
    const startDateInput = document.getElementById('expectedStartDate');
    if (!startDateInput) return;

    const days = periodInput ? parseInt(periodInput.value, 10) : 5;
    const validDays = (!isNaN(days) && days > 0) ? days : 5;

    const minDate = new Date();
    minDate.setDate(minDate.getDate() + validDays + 1);
    const yyyy = minDate.getFullYear();
    const mm = String(minDate.getMonth() + 1).padStart(2, '0');
    const dd = String(minDate.getDate()).padStart(2, '0');
    startDateInput.min = `${yyyy}-${mm}-${dd}`;
}

function initOfferFormExpectedStartDateMin() {
    updateMinExpectedStartDate();
    const periodInput = document.getElementById('responsePeriodDays');
    if (periodInput) {
        periodInput.addEventListener('change', updateMinExpectedStartDate);
    }
}

// =========================================================================
// SECTION C — EXCEL EXPORT (Excel Export Modal & Blob Download)
// =========================================================================

/**
 * Mở modal xuất Excel
 */
function openExportModal() {
    const modal = document.getElementById('offerExportModal');
    if (modal) {
        if (!modal.open) modal.showModal();
    }
    const errDiv = document.getElementById('exportModalError');
    if (errDiv) errDiv.hidden = true;
}

/**
 * Mở modal xuất Excel và tự động chọn phạm vi 'SELECTED'
 */
function openExportModalWithScope(scope) {
    openExportModal();
    if (scope) {
        const radio = document.querySelector(`input[name="exportScope"][value="${scope}"]`);
        if (radio && !radio.disabled) {
            radio.checked = true;
            handleScopeChange();
        }
    }
}

function closeExportModal() {
    const modal = document.getElementById('offerExportModal');
    if (modal) {
        modal.close();
    }
}

function handleExportModalBackdropClick(e) {
    if (e.target && e.target.id === 'offerExportModal') {
        closeExportModal();
    }
}

function handleScopeChange() {
    // Được kích hoạt bởi radio scope để đồng bộ giao diện nếu cần
}

function selectAllColumns() {
    document.querySelectorAll('input[name="exportCol"]').forEach(cb => cb.checked = true);
}

function deselectAllColumns() {
    document.querySelectorAll('input[name="exportCol"]').forEach(cb => cb.checked = false);
}

/**
 * Khôi phục 12 cột xuất mặc định
 */
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
 * Gửi yêu cầu AJAX POST JSON tới /offers/export và tải file nhị phân Blob về máy
 */
function executeOfferExport() {
    const errDiv = document.getElementById('exportModalError');
    const errText = document.getElementById('exportModalErrorText');
    const showErr = (msg) => {
        if (errDiv && errText) {
            errText.textContent = msg;
            errDiv.hidden = false;
        }
    };

    if (errDiv) errDiv.hidden = true;

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
        showErr('Bạn chưa chọn đề xuất nào trong bảng danh sách để xuất.');
        return;
    }

    // 2. Thu thập bộ lọc hiện tại nếu là FILTERED
    let search = null;
    let status = null;
    let timeSort = null;

    if (scope === 'FILTERED') {
        search = (document.getElementById('searchInput')?.value || '').trim() || null;
        const statusVal = document.getElementById('statusFilter')?.value;
        status = (statusVal && statusVal !== 'ALL') ? statusVal : null;
        const timeSortVal = document.getElementById('timeSortFilter')?.value;
        timeSort = (timeSortVal && timeSortVal !== 'DEFAULT') ? timeSortVal : null;
    }

    // 3. Chuẩn bị payload DTO OfferExportRequest
    const payload = {
        scope: scope,
        selectedIds: scope === 'SELECTED' ? selectedIds : [],
        search: search,
        status: status,
        timeSort: timeSort,
        columns: selectedCols
    };

    const csrf = (typeof getCsrfInfo === 'function') ? getCsrfInfo() : { token: '', header: 'X-CSRF-TOKEN' };
    const btn = document.getElementById('btnConfirmExport');
    const originalBtnText = btn ? btn.innerHTML : 'Tải xuống';
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<span class="ofw-spinner-sm" aria-hidden="true"></span> Đang xuất...';
    }

    fetch('/offers/export', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': csrf.token || ''
        },
        body: JSON.stringify(payload)
    })
        .then(async (res) => {
            if (!res.ok) {
                let errMsg = `Lỗi máy chủ (${res.status})`;
                try {
                    const errJson = await res.json();
                    if (errJson && errJson.message) errMsg = errJson.message;
                } catch (e) { }
                throw new Error(errMsg);
            }

            const disposition = res.headers.get('Content-Disposition');
            let filename = 'Danh_sach_de_xuat_nhan_viec.xlsx';
            if (disposition && disposition.includes('filename=')) {
                const match = disposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                if (match && match[1]) {
                    filename = decodeURIComponent(match[1].replace(/['"]/g, ''));
                }
            }

            return res.blob().then(blob => ({ blob, filename }));
        })
        .then(({ blob, filename }) => {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            a.remove();

            closeExportModal();
            if (typeof showFlashToast === 'function') {
                showFlashToast('Xuất danh sách đề xuất thành công!', 'success');
            }
        })
        .catch((err) => {
            showErr(err.message || 'Có lỗi xảy ra khi tạo file Excel.');
        })
        .finally(() => {
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = originalBtnText;
            }
        });
}

// =========================================================================
// SECTION D — PAGE-SPECIFIC INITIALIZATION (DOMContentLoaded)
// =========================================================================

function initOfferHrPage() {
    // 1. Khởi tạo ràng buộc form nếu đang ở trang Form
    const startDateInput = document.getElementById('expectedStartDate');
    if (startDateInput) {
        initOfferFormExpectedStartDateMin();

        const select = document.getElementById('createAppSelect');
        if (select && select.value) {
            handleSelectPassedCandidate(select.value);
        }

        validateProbationRuleUi();
    }

    // 2. Khởi tạo toolbar nếu đang ở trang List
    if (document.querySelectorAll('.offer-row-checkbox').length > 0) {
        updateSelectionToolbar();
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initOfferHrPage);
} else {
    initOfferHrPage();
}

// =========================================================================
// SECTION E — WINDOW GLOBAL EXPORTS
// =========================================================================
window.applyOfferFilters = applyOfferFilters;
window.filterOffersByStatus = filterOffersByStatus;
window.clearSearchInput = clearSearchInput;
window.confirmDeleteOffer = confirmDeleteOffer;
window.getSelectedOfferIds = getSelectedOfferIds;
window.toggleSelectAllOffers = toggleSelectAllOffers;
window.handleOfferRowSelectionChange = handleOfferRowSelectionChange;
window.deselectAllOffers = deselectAllOffers;
window.updateSelectionToolbar = updateSelectionToolbar;

window.handleSelectPassedCandidate = handleSelectPassedCandidate;
window.autoCalculateProbationSalary = autoCalculateProbationSalary;
window.validateProbationRuleUi = validateProbationRuleUi;
window.updateMinExpectedStartDate = updateMinExpectedStartDate;
window.initOfferFormExpectedStartDateMin = initOfferFormExpectedStartDateMin;

window.openExportModal = openExportModal;
window.openExportModalWithScope = openExportModalWithScope;
window.closeExportModal = closeExportModal;
window.handleExportModalBackdropClick = handleExportModalBackdropClick;
window.handleScopeChange = handleScopeChange;
window.selectAllColumns = selectAllColumns;
window.deselectAllColumns = deselectAllColumns;
window.resetDefaultColumns = resetDefaultColumns;
window.executeOfferExport = executeOfferExport;
