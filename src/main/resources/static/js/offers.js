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
    if (el) {
        el.classList.add('active');
        el.style.display = 'flex';
    }
}

function closeModal(id) {
    const el = document.getElementById(id);
    if (el) {
        el.classList.remove('active');
        el.style.display = 'none';
    }
}

function handleCreateModalBackdropClick(event) {
    if (event.target && event.target.id === 'createOfferModal') {
        closeModal('createOfferModal');
    }
}

function handleDetailModalBackdropClick(event) {
    if (event.target && event.target.id === 'detailOfferModal') {
        closeModal('detailOfferModal');
    }
}

function handleEditModalBackdropClick(event) {
    if (event.target && event.target.id === 'editOfferModal') {
        closeModal('editOfferModal');
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
// 3.1. POP-UP MODAL XEM CHI TIẾT OFFER (Screen 32 View Detail Modal)
// =========================================================================

function escapeOfferHtml(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function formatOfferDate(dateStr) {
    if (!dateStr) return '-';
    try {
        const parts = String(dateStr).split('-');
        if (parts.length === 3) {
            return `${parts[2]}/${parts[1]}/${parts[0]}`;
        }
        const d = new Date(dateStr);
        if (isNaN(d.getTime())) return dateStr;
        return d.toLocaleDateString('vi-VN');
    } catch (e) {
        return dateStr;
    }
}

function formatOfferDateTime(dateTimeStr) {
    if (!dateTimeStr) return '-';
    try {
        const d = new Date(dateTimeStr);
        if (isNaN(d.getTime())) return dateTimeStr;
        const day = String(d.getDate()).padStart(2, '0');
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const year = d.getFullYear();
        const hours = String(d.getHours()).padStart(2, '0');
        const minutes = String(d.getMinutes()).padStart(2, '0');
        return `${day}/${month}/${year} ${hours}:${minutes}`;
    } catch (e) {
        return dateTimeStr;
    }
}

function getStatusBadgeHtml(status) {
    switch (status) {
        case 'Draft':
            return '<span class="status-badge status-draft"><i class="fa-regular fa-file"></i> Bản thảo</span>';
        case 'Pending_Director':
            return '<span class="status-badge status-pending"><i class="fa-regular fa-clock"></i> Chờ duyệt</span>';
        case 'Director_Approved':
        case 'Approved':
            return '<span class="status-badge status-approved"><i class="fa-solid fa-check"></i> Director đã duyệt</span>';
        case 'Director_Rejected':
        case 'Rejected':
            return '<span class="status-badge status-rejected"><i class="fa-solid fa-xmark"></i> Bị từ chối</span>';
        case 'Sent_Candidate':
            return '<span class="status-badge status-sent"><i class="fa-regular fa-paper-plane"></i> Đã gửi ứng viên</span>';
        case 'Accepted':
            return '<span class="status-badge status-accepted"><i class="fa-solid fa-user-check"></i> Đã nhận việc</span>';
        case 'Declined':
            return '<span class="status-badge status-declined"><i class="fa-solid fa-user-xmark"></i> Ứng viên từ chối</span>';
        default:
            return `<span class="status-badge">${escapeOfferHtml(status || 'N/A')}</span>`;
    }
}

function renderDetailModalHtml(d) {
    const candidateName = escapeOfferHtml(d.candidateName || 'N/A');
    const candidateEmail = escapeOfferHtml(d.candidateEmail || 'N/A');
    const candidatePhone = escapeOfferHtml(d.candidatePhone || 'N/A');
    const appliedPosition = escapeOfferHtml(d.appliedPosition || 'N/A');
    const departmentName = escapeOfferHtml(d.departmentName || 'N/A');
    const requisitionId = d.requisitionId ? `#${d.requisitionId}` : 'N/A';
    const finalDecision = escapeOfferHtml(d.finalDecision || 'Passed');
    const interviewComments = escapeOfferHtml(d.interviewSummaryComments || 'Không có ghi chú thêm.');
    const recomSalary = d.recommendedSalary ? Number(d.recommendedSalary).toLocaleString('vi-VN') + ' VND' : 'Chưa có gợi ý';
    const hiringManagerName = escapeOfferHtml(d.hiringManagerName || 'N/A');

    const offeredTitle = escapeOfferHtml(d.offeredPositionTitle || '-');
    const proposedSalary = d.proposedSalary ? Number(d.proposedSalary).toLocaleString('vi-VN') + ' VND' : '-';
    const probationSalary = d.probationSalary ? Number(d.probationSalary).toLocaleString('vi-VN') + ' VND' : '-';

    let probationPctBadge = '';
    if (d.proposedSalary && d.probationSalary && Number(d.proposedSalary) > 0) {
        const pct = Math.round((Number(d.probationSalary) * 100) / Number(d.proposedSalary));
        probationPctBadge = `<span class="salary-badge-pct">${pct}%</span>`;
    }

    const probationDays = d.probationDays ? `${d.probationDays} ngày` : '60 ngày';
    const expectedStartDate = formatOfferDate(d.expectedStartDate);
    const workLocation = escapeOfferHtml(d.workLocation || 'Trụ sở chính Mộc RMS');
    const benefits = escapeOfferHtml(d.benefitsPackage || 'Theo quy chế đãi ngộ chung của công ty.');

    let approvalHtml = '';
    if (d.approvalHistory && d.approvalHistory.length > 0) {
        approvalHtml = '<div class="history-timeline">';
        d.approvalHistory.forEach(a => {
            const isApproved = a.status === 'Approved' || a.status === 'Director_Approved';
            const dotClass = isApproved ? 'dot-approved' : 'dot-rejected';
            const statusColor = isApproved ? 'var(--brand-dark)' : 'var(--error)';
            const director = escapeOfferHtml(a.directorName || 'Giám đốc');
            const statusText = escapeOfferHtml(a.status || '');
            const timeText = formatOfferDateTime(a.approvedAt);
            const commentsText = escapeOfferHtml(a.directorComments || 'Không có ghi chú thêm.');

            approvalHtml += `
                <div class="timeline-item">
                    <div class="timeline-dot ${dotClass}"></div>
                    <div class="timeline-header">
                        <span>${director}</span> - 
                        <span style="font-weight:700; color:${statusColor};">${statusText}</span>
                    </div>
                    <div class="timeline-time">${timeText}</div>
                    <div class="timeline-content">${commentsText}</div>
                </div>
            `;
        });
        approvalHtml += '</div>';
    } else {
        approvalHtml = `
            <div style="color:var(--muted); font-size:13.5px; padding: 12px 0;">
                <i class="fa-regular fa-clock"></i> Chưa có ghi nhận lịch sử phê duyệt của Giám đốc đối với đề xuất này.
            </div>
        `;
    }

    return `
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; padding-bottom:12px; border-bottom:1px solid var(--line);">
            <div>
                <span style="font-size:15px; font-weight:600; color:var(--ink);">${offeredTitle}</span>
                <span style="color:var(--muted); font-size:13px; margin-left:8px;">&bull; Ứng viên: <strong>${candidateName}</strong></span>
                <span style="color:var(--muted); font-size:13px; margin-left:8px;">&bull; Ngày tạo: ${formatOfferDateTime(d.createdAt)}</span>
            </div>
            <div>
                ${getStatusBadgeHtml(d.offerStatus)}
            </div>
        </div>

        <div class="detail-section">
            <div class="section-title">
                <i class="fa-solid fa-user"></i> 1. Hồ Sơ Ứng Viên & Kết Quả Phỏng Vấn
            </div>
            <div class="detail-grid-3">
                <div>
                    <div class="detail-item-label">Họ và tên ứng viên</div>
                    <div class="detail-item-value" style="font-weight:700;">${candidateName}</div>
                </div>
                <div>
                    <div class="detail-item-label">Email liên hệ</div>
                    <div class="detail-item-value">${candidateEmail}</div>
                </div>
                <div>
                    <div class="detail-item-label">Số điện thoại</div>
                    <div class="detail-item-value">${candidatePhone}</div>
                </div>
                <div>
                    <div class="detail-item-label">Vị trí ứng tuyển</div>
                    <div class="detail-item-value">${appliedPosition}</div>
                </div>
                <div>
                    <div class="detail-item-label">Phòng ban</div>
                    <div class="detail-item-value">${departmentName}</div>
                </div>
                <div>
                    <div class="detail-item-label">Mã Yêu cầu tuyển dụng (Requisition ID)</div>
                    <div class="detail-item-value">${requisitionId}</div>
                </div>
            </div>

            <div style="margin-top:14px; padding-top:12px; border-top:1px dashed var(--line);" class="detail-grid">
                <div>
                    <div class="detail-item-label">Kết quả Hội đồng phỏng vấn:</div>
                    <div class="detail-item-value" style="color:var(--brand-dark); font-weight:700;">
                        <i class="fa-solid fa-circle-check"></i> ${finalDecision}
                    </div>
                    <div style="font-size:13px; color:var(--muted); margin-top:4px;">${interviewComments}</div>
                </div>
                <div>
                    <div class="detail-item-label">Lương khuyến nghị từ Hiring Manager:</div>
                    <div class="detail-item-value" style="color:var(--brand-dark); font-weight:700; font-size:14.5px;">${recomSalary}</div>
                    <div style="font-size:13px; color:var(--muted); margin-top:4px;">Người phụ trách: ${hiringManagerName}</div>
                </div>
            </div>
        </div>

        <div class="detail-section">
            <div class="section-title">
                <i class="fa-solid fa-coins"></i> 2. Gói Đãi Ngộ & Điều Khoản Offer
            </div>
            <div class="detail-grid-3">
                <div>
                    <div class="detail-item-label">Chức danh chính thức đề xuất</div>
                    <div class="detail-item-value" style="font-weight:700;">${offeredTitle}</div>
                </div>
                <div>
                    <div class="detail-item-label">Lương chính thức (Gross/tháng)</div>
                    <div class="detail-item-value" style="color:var(--ink); font-weight:700; font-size:15px;">${proposedSalary}</div>
                </div>
                <div>
                    <div class="detail-item-label">Lương thử việc (Gross/tháng)</div>
                    <div class="detail-item-value" style="color:var(--brand-dark); font-weight:700; font-size:15px;">
                        <span>${probationSalary}</span> ${probationPctBadge}
                    </div>
                </div>
                <div>
                    <div class="detail-item-label">Thời gian thử việc</div>
                    <div class="detail-item-value">${probationDays}</div>
                </div>
                <div>
                    <div class="detail-item-label">Ngày bắt đầu dự kiến</div>
                    <div class="detail-item-value">${expectedStartDate}</div>
                </div>
                <div>
                    <div class="detail-item-label">Địa điểm làm việc</div>
                    <div class="detail-item-value">${workLocation}</div>
                </div>
            </div>

            <div style="margin-top:14px;">
                <div class="detail-item-label">Chế độ phúc lợi & Đãi ngộ đặc thù:</div>
                <div class="detail-item-value" style="background:var(--page); padding:10px 14px; border:1px solid var(--line); border-radius:6px; font-size:13.5px; margin-top:4px;">
                    ${benefits}
                </div>
            </div>
        </div>

        <div class="detail-section" style="margin-bottom:0;">
            <div class="section-title">
                <i class="fa-solid fa-stamp"></i> 3. Lịch Sử Phê Duyệt Của Director
            </div>
            ${approvalHtml}
        </div>
    `;
}

function renderDetailFooterHtml(d) {
    let btns = `<button type="button" class="btn btn-secondary" onclick="closeModal('detailOfferModal')">Đóng</button>`;

    if (d.offerStatus === 'Draft') {
        btns += `
            <button type="button" class="btn btn-danger" style="background:#dc2626; border-color:#dc2626; color:#fff;" onclick="confirmDeleteOffer(${d.offerId})">
                <i class="fa-solid fa-trash-can" style="margin-right:4px;"></i> Xóa bản thảo
            </button>
            <button type="button" class="btn btn-primary" onclick="openEditOfferModal(${d.offerId})">
                <i class="fa-solid fa-pen-to-square" style="margin-right:4px;"></i> Chỉnh sửa
            </button>
        `;
    } else if (d.offerStatus === 'Director_Approved' || d.offerStatus === 'Approved') {
        btns += `
            <button type="button" class="btn btn-primary" onclick="confirmSendOffer(${d.offerId})">
                <i class="fa-solid fa-paper-plane" style="margin-right:4px;"></i> Phát hành Offer Letter
            </button>
        `;
    } else if (d.offerStatus === 'Director_Rejected' || d.offerStatus === 'Rejected') {
        btns += `
            <button type="button" class="btn btn-primary" onclick="openEditOfferModal(${d.offerId})">
                <i class="fa-solid fa-pen-to-square" style="margin-right:4px;"></i> Chỉnh sửa & Trình lại
            </button>
        `;
    }

    return btns;
}

function viewOfferDetail(offerId) {
    if (!offerId) return;

    const titleEl = document.getElementById('detailModalTitle');
    const bodyEl = document.getElementById('detailModalBody');
    const footerEl = document.getElementById('detailModalFooter');

    if (titleEl) titleEl.textContent = `Chi Tiết Đề Xuất Offer #${offerId}`;
    if (bodyEl) {
        bodyEl.innerHTML = `
            <div style="text-align:center; padding:40px; color:var(--muted);">
                <i class="fa-solid fa-spinner fa-spin" style="font-size:24px;"></i>
                <p style="margin-top:10px;">Đang tải thông tin chi tiết...</p>
            </div>
        `;
    }
    if (footerEl) {
        footerEl.innerHTML = `<button type="button" class="btn btn-secondary" onclick="closeModal('detailOfferModal')">Đóng</button>`;
    }

    openModal('detailOfferModal');

    fetch(`/offers/${offerId}`, {
        method: 'GET',
        headers: {
            'Accept': 'application/json'
        }
    })
    .then(async res => {
        const isJson = res.headers.get('content-type')?.includes('application/json');
        const data = isJson ? await res.json() : null;
        if (!res.ok) {
            throw new Error(data?.message || `Lỗi tải dữ liệu (${res.status})`);
        }
        return data;
    })
    .then(res => {
        if (res && res.success && res.data) {
            const d = res.data;
            if (titleEl) titleEl.textContent = `Chi Tiết Đề Xuất Offer #${d.offerId}`;
            if (bodyEl) bodyEl.innerHTML = renderDetailModalHtml(d);
            if (footerEl) footerEl.innerHTML = renderDetailFooterHtml(d);
        } else {
            throw new Error(res?.message || 'Không thể hiển thị thông tin Offer.');
        }
    })
    .catch(err => {
        if (bodyEl) {
            bodyEl.innerHTML = `
                <div class="modal-alert modal-alert-danger">
                    <i class="fa-solid fa-circle-exclamation"></i>
                    <span>${escapeOfferHtml(err.message)}</span>
                </div>
            `;
        }
    });
}

// =========================================================================
// 3.2. POP-UP MODAL CHỈNH SỬA OFFER (Screen 30/32 Edit Modal)
// =========================================================================

function openEditOfferModal(offerId) {
    if (!offerId) return;

    closeModal('detailOfferModal');

    const form = document.getElementById('editOfferForm');
    if (form) form.reset();

    const titleEl = document.getElementById('editModalTitle');
    if (titleEl) titleEl.textContent = `Chỉnh Sửa Offer Proposal #${offerId}`;

    const idInput = document.getElementById('editOfferId');
    if (idInput) idInput.value = offerId;

    const startDateInput = document.getElementById('editStartDate');
    if (startDateInput) {
        startDateInput.min = getTomorrowDateString();
    }

    const rejectBanner = document.getElementById('editRejectBanner');
    if (rejectBanner) rejectBanner.style.display = 'none';

    openModal('editOfferModal');

    fetch(`/offers/${offerId}`, {
        method: 'GET',
        headers: {
            'Accept': 'application/json'
        }
    })
    .then(async res => {
        const isJson = res.headers.get('content-type')?.includes('application/json');
        const data = isJson ? await res.json() : null;
        if (!res.ok) {
            throw new Error(data?.message || `Lỗi tải dữ liệu (${res.status})`);
        }
        return data;
    })
    .then(res => {
        if (res && res.success && res.data) {
            const d = res.data;
            const setVal = (id, val) => {
                const el = document.getElementById(id);
                if (el) el.value = val != null ? val : '';
            };

            setVal('editOfferedTitle', d.offeredPositionTitle);
            setVal('editProposedSalary', d.proposedSalary);
            setVal('editProbationSalary', d.probationSalary);
            setVal('editProbationDays', d.probationDays || 60);
            setVal('editStartDate', d.expectedStartDate);
            setVal('editWorkLocation', d.workLocation);
            setVal('editBenefits', d.benefitsPackage);

            if (d.offerStatus === 'Director_Rejected' || d.offerStatus === 'Rejected') {
                let rejectComment = 'Không có ghi chú thêm.';
                if (d.approvalHistory && d.approvalHistory.length > 0) {
                    const lastReject = d.approvalHistory
                        .filter(a => a.status === 'Rejected' || a.status === 'Director_Rejected')
                        .pop();
                    if (lastReject && lastReject.directorComments) {
                        rejectComment = lastReject.directorComments;
                    }
                }
                const commentEl = document.getElementById('editRejectComment');
                if (commentEl) commentEl.textContent = rejectComment;
                if (rejectBanner) rejectBanner.style.display = 'flex';
            }

            editValidateProbation();
        } else {
            throw new Error(res?.message || 'Không thể tải thông tin chỉnh sửa.');
        }
    })
    .catch(err => {
        showToast(err.message, 'danger');
        closeModal('editOfferModal');
    });
}

function editAutoCalculateProbation(forceSet = false) {
    const proposedEl = document.getElementById('editProposedSalary');
    const probationEl = document.getElementById('editProbationSalary');
    if (!proposedEl || !probationEl) return;

    const proposed = parseFloat(proposedEl.value);
    if (!isNaN(proposed) && proposed > 0) {
        const minProbation = Math.round(proposed * 0.85);
        const currentProbation = parseFloat(probationEl.value);
        if (forceSet || isNaN(currentProbation) || currentProbation === 0) {
            probationEl.value = minProbation;
        }
        editValidateProbation();
    }
}

function editValidateProbation() {
    const proposedEl = document.getElementById('editProposedSalary');
    const probationEl = document.getElementById('editProbationSalary');
    const notice = document.getElementById('editProbationNotice');
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

function submitUpdateOffer(isDraft) {
    const offerIdInput = document.getElementById('editOfferId');
    const offerId = offerIdInput ? offerIdInput.value : '';
    if (!offerId) {
        showToast('Không xác định được Offer cần cập nhật.', 'danger');
        return;
    }

    const titleInput = document.getElementById('editOfferedTitle');
    const title = titleInput ? titleInput.value.trim() : '';
    const proposedEl = document.getElementById('editProposedSalary');
    const proposed = proposedEl ? parseFloat(proposedEl.value) : NaN;
    const probationEl = document.getElementById('editProbationSalary');
    const probation = probationEl ? parseFloat(probationEl.value) : NaN;
    const probationDaysEl = document.getElementById('editProbationDays');
    const probationDays = probationDaysEl ? parseInt(probationDaysEl.value, 10) : NaN;
    const startDateEl = document.getElementById('editStartDate');
    const startDate = startDateEl ? startDateEl.value : '';
    const workLocEl = document.getElementById('editWorkLocation');
    const workLocation = workLocEl ? workLocEl.value.trim() : '';
    const benefitsEl = document.getElementById('editBenefits');
    const benefits = benefitsEl ? benefitsEl.value : '';

    if (!title) {
        showToast('Vị trí chức danh đề xuất không được để trống.', 'warning');
        return;
    }

    if (isNaN(proposed) || proposed <= 0) {
        showToast('Mức lương chính thức phải lớn hơn 0.', 'warning');
        return;
    }

    if (isNaN(probation) || probation <= 0) {
        showToast('Mức lương thử việc phải lớn hơn 0.', 'warning');
        return;
    }

    if (!editValidateProbation()) {
        if (probation > proposed) {
            showToast('Lương thử việc không được vượt quá lương chính thức.', 'danger');
        } else {
            showToast('Mức lương thử việc không tuân thủ quy định tối thiểu 85%.', 'danger');
        }
        return;
    }

    if (isNaN(probationDays) || probationDays <= 0) {
        showToast('Thời gian thử việc phải lớn hơn 0 ngày.', 'warning');
        return;
    }

    if (!startDate) {
        showToast('Ngày bắt đầu dự kiến không được để trống.', 'warning');
        return;
    }
    if (startDate <= getTodayDateString()) {
        showToast('Ngày bắt đầu dự kiến phải lớn hơn ngày hiện tại.', 'danger');
        return;
    }

    if (!workLocation) {
        showToast('Địa điểm làm việc không được để trống.', 'warning');
        return;
    }

    const payload = {
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
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    };
    if (csrf.token) {
        headers[csrf.header] = csrf.token;
    }

    fetch(`/offers/${offerId}`, {
        method: 'PUT',
        headers: headers,
        body: JSON.stringify(payload)
    })
    .then(async res => {
        const isJson = res.headers.get('content-type')?.includes('application/json');
        const data = isJson ? await res.json() : null;
        if (!res.ok) {
            throw new Error(data?.message || `Lỗi máy chủ (${res.status})`);
        }
        return data;
    })
    .then(res => {
        if (res && res.success) {
            showToast(res.message, 'success');
            closeModal('editOfferModal');
            setTimeout(() => window.location.reload(), 800);
        } else {
            showToast(res?.message || 'Lỗi khi cập nhật Offer.', 'danger');
        }
    })
    .catch(err => {
        showToast(err.message, 'danger');
    });
}

// =========================================================================
// 3.3. THAO TÁC PHÁT HÀNH & XÓA OFFER (Send, Delete)
// =========================================================================

function confirmSendOffer(offerId) {
    if (!offerId) return;
    if (!confirm('Bạn có chắc chắn muốn phát hành Thư mời làm việc (Offer Letter) tới ứng viên? Trạng thái sẽ được chuyển sang Sent_Candidate.')) {
        return;
    }

    const csrf = getCsrfInfo();
    const headers = {
        'Accept': 'application/json'
    };
    if (csrf.token) {
        headers[csrf.header] = csrf.token;
    }

    fetch(`/offers/${offerId}/send`, {
        method: 'POST',
        headers: headers
    })
    .then(async res => {
        const isJson = res.headers.get('content-type')?.includes('application/json');
        const data = isJson ? await res.json() : null;
        if (!res.ok) {
            throw new Error(data?.message || `Lỗi phát hành Offer (${res.status})`);
        }
        return data;
    })
    .then(res => {
        if (res && res.success) {
            showToast(res.message, 'success');
            closeModal('detailOfferModal');
            setTimeout(() => window.location.reload(), 800);
        } else {
            showToast(res?.message || 'Lỗi phát hành Offer.', 'danger');
        }
    })
    .catch(err => {
        showToast(err.message, 'danger');
    });
}

function confirmDeleteOffer(offerId) {
    if (!offerId) return;
    if (!confirm('Bạn có chắc chắn muốn xóa bản thảo Offer này khỏi hệ thống?')) {
        return;
    }

    const csrf = getCsrfInfo();
    const headers = {
        'Accept': 'application/json'
    };
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
            throw new Error(data?.message || `Lỗi xóa bản thảo (${res.status})`);
        }
        return data;
    })
    .then(res => {
        if (res && res.success) {
            showToast(res.message, 'success');
            closeModal('detailOfferModal');
            setTimeout(() => window.location.reload(), 800);
        } else {
            showToast(res?.message || 'Lỗi xóa bản thảo.', 'danger');
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

// =========================================================================
// 8. DIRECTOR APPROVAL MODAL (Screen 30 & 32)
// =========================================================================

function openDirectorDecisionModal(offerId, actionType) {
    const modal = document.getElementById('directorDecisionModal');
    if (!modal) return;
    
    document.getElementById('directorDecisionOfferId').value = offerId;
    document.getElementById('directorDecisionAction').value = actionType;
    document.getElementById('directorDecisionComments').value = '';
    
    const titleEl = document.getElementById('directorDecisionTitle');
    const btnSubmit = document.getElementById('btnSubmitDecision');
    
    if (actionType === 'APPROVE') {
        titleEl.innerHTML = '<i class="fa-solid fa-check-circle" style="color:var(--success);"></i> Phê Duyệt Đề Xuất Offer';
        btnSubmit.innerHTML = '<i class="fa-solid fa-check"></i> Phê Duyệt';
        btnSubmit.className = 'btn btn-primary';
    } else {
        titleEl.innerHTML = '<i class="fa-solid fa-xmark-circle" style="color:var(--error);"></i> Từ Chối Đề Xuất Offer';
        btnSubmit.innerHTML = '<i class="fa-solid fa-xmark"></i> Từ Chối';
        btnSubmit.className = 'btn btn-danger';
    }
    
    modal.classList.add('active');
    modal.style.display = 'flex';
}

function closeDirectorDecisionModal() {
    const modal = document.getElementById('directorDecisionModal');
    if (modal) {
        modal.classList.remove('active');
        modal.style.display = 'none';
    }
}

function submitDirectorDecision() {
    const offerId = document.getElementById('directorDecisionOfferId').value;
    const actionType = document.getElementById('directorDecisionAction').value;
    const comments = document.getElementById('directorDecisionComments').value.trim();
    
    if (!offerId || !actionType) return;
    
    const btn = document.getElementById('btnSubmitDecision');
    const originalBtnHtml = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang xử lý...';
    
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const url = actionType === 'APPROVE' ? `/offers/${offerId}/approve` : `/offers/${offerId}/reject`;
    
    fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': token || ''
        },
        body: JSON.stringify({ comments: comments })
    })
    .then(async (res) => {
        let data;
        try { data = await res.json(); } catch(e) {}
        if (!res.ok) {
            throw new Error(data?.message || 'Có lỗi xảy ra khi xử lý quyết định.');
        }
        return data;
    })
    .then(data => {
        closeDirectorDecisionModal();
        showOfferToast(data.message || 'Xử lý thành công!', 'success');
        // Refresh trang sau 1s để cập nhật list/detail
        setTimeout(() => {
            window.location.reload();
        }, 1000);
    })
    .catch(err => {
        alert(err.message);
        showOfferToast(err.message, 'danger');
        btn.disabled = false;
        btn.innerHTML = originalBtnHtml;
    });
}
