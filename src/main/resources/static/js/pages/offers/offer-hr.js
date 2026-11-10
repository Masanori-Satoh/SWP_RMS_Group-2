/**
 * offer-hr.js — Phân hệ Offer Management (SWP391 — Mộc RMS Group 2)
 * Trách nhiệm luồng nghiệp vụ của Chuyên viên Tuyển dụng (HR / Recruiter):
 * - SECTION A: Quản lý danh sách, bộ lọc, chọn hàng loạt và xóa bản nháp Offer (HR List)
 * - SECTION C: Quản lý hộp thoại xuất Excel, chọn cột dữ liệu theo danh sách cho phép và tải Blob (Excel Export)
 * - SECTION D: Khởi tạo sự kiện theo sự hiện diện của phần tử DOM (Initialization)
 */

// =========================================================================
// SECTION A — HR OFFER LIST (Screen 30 List Filters & Row Selection)
// =========================================================================

/**
 * Xác nhận và gọi API xóa bản nháp đề xuất (Chỉ áp dụng cho trạng thái Draft)
 */
function confirmDeleteOffer(offerId) {
    const dialog = document.getElementById('deleteOfferDialog');
    if (!offerId || !dialog || dialog.open) return;
    dialog.dataset.offerId = offerId;
    dialog.showModal();
}

/**
 * Xóa nháp bằng DELETE sau khi người dùng xác nhận; server mới quyết định bản ghi có được xóa hay không.
 * Khóa nút trong lúc chờ, mở lại khi hoàn tất; chỉ reload khi ApiResponse báo thành công.
 */
function executeDeleteOffer(button) {
    const dialog = button.closest('dialog');
    const offerId = dialog.dataset.offerId;
    if (!offerId || button.disabled) return;
    button.disabled = true;

    offerRequest(`/offers/${offerId}`, { method: 'DELETE' })
        .then(res => {
            queueOfferToast(res.message || 'Đã xóa bản nháp thư mời thành công.');
            window.location.reload();
        })
        .catch(err => {
            if (typeof showFlashToast === 'function') {
                showFlashToast(err.message, 'error');
            }
        })
        .finally(() => {
            button.disabled = false;
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
 * Đồng bộ số dòng được chọn, thanh tác vụ và quyền chọn phạm vi SELECTED trong modal Excel.
 * Checkbox trên trang chỉ chọn các dòng đang có trong DOM, không mặc nhiên chọn mọi trang kết quả.
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
 * Ghép đúng hợp đồng backend: scope, offerIds, filters={keyword,status,sort}, columns.
 * SELECTED gửi ID checkbox; FILTERED gửi bộ lọc hiện tại; ALL không dùng hai bộ dữ liệu đó.
 * Đọc phản hồi dạng Blob để tải .xlsx; không đi qua helper JSON offerRequest.
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
        showErr('Bạn chưa chọn thư mời nào trong bảng danh sách để xuất.');
        return;
    }

    // 2. Xuất theo URL đã áp dụng, không lấy giá trị đang gõ dở trong toolbar.
    let search = null;
    let status = null;
    let timeSort = null;

    if (scope === 'FILTERED') {
        const applied = new URLSearchParams(window.location.search);
        search = applied.get('search')?.trim() || null;
        const statusVal = applied.get('status');
        status = (statusVal && statusVal !== 'ALL') ? statusVal : null;
        const timeSortVal = applied.get('timeSort');
        timeSort = (timeSortVal && timeSortVal !== 'DEFAULT') ? timeSortVal : null;
    }

    // 3. Chuẩn bị payload DTO OfferExportRequest
    const payload = {
        scope: scope,
        offerIds: scope === 'SELECTED' ? selectedIds : [],
        filters: scope === 'FILTERED' ? { keyword: search, status, sort: timeSort } : null,
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
            [csrf.header]: csrf.token || ''
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
                showFlashToast('Xuất danh sách thư mời thành công!', 'success');
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
window.confirmDeleteOffer = confirmDeleteOffer;
window.executeDeleteOffer = executeDeleteOffer;
window.getSelectedOfferIds = getSelectedOfferIds;
window.toggleSelectAllOffers = toggleSelectAllOffers;
window.handleOfferRowSelectionChange = handleOfferRowSelectionChange;
window.deselectAllOffers = deselectAllOffers;
window.updateSelectionToolbar = updateSelectionToolbar;


window.openExportModal = openExportModal;
window.openExportModalWithScope = openExportModalWithScope;
window.closeExportModal = closeExportModal;
window.handleExportModalBackdropClick = handleExportModalBackdropClick;
window.selectAllColumns = selectAllColumns;
window.deselectAllColumns = deselectAllColumns;
window.resetDefaultColumns = resetDefaultColumns;
window.executeOfferExport = executeOfferExport;
