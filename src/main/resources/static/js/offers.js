/**
 * JavaScript logic cho phân hệ Offer Management (Screen 30 & Screen 32)
 * Tuân thủ quy chuẩn ARCHITECTURE_GUIDE.md (tách rời script khỏi templates HTML).
 */

// Áp dụng bộ lọc 3 tiêu chí trên toàn bộ dữ liệu hệ thống (Server-side Filtering & Sorting)
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

    url.searchParams.set('page', '0'); // Reset về trang đầu khi đổi điều kiện lọc
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

// Modal Helpers
function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('active');
}

function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove('active');
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

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

// Helper lấy ngày hiện tại và ngày mai định dạng YYYY-MM-DD theo giờ địa phương
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
// 1. CREATE OFFER WORKFLOW
// =========================================================================
function openCreateOfferModal() {
    const form = document.getElementById('createOfferForm');
    if (form) form.reset();
    const grpA = document.getElementById('groupASection');
    if (grpA) grpA.style.display = 'none';

    // Ràng buộc lịch: Ngày bắt đầu dự kiến phải > ngày hiện tại (tối thiểu là ngày mai)
    const startDateInput = document.getElementById('createStartDate');
    if (startDateInput) {
        startDateInput.min = getTomorrowDateString();
    }
    const probationDaysInput = document.getElementById('createProbationDays');
    if (probationDaysInput) {
        probationDaysInput.value = '60';
    }
    const workLocInput = document.getElementById('createWorkLocation');
    if (workLocInput) {
        workLocInput.value = '';
    }
    const overrideNotice = document.getElementById('createOverrideNotice');
    if (overrideNotice) {
        overrideNotice.style.display = 'none';
    }

    openModal('createOfferModal');
}

function handleSelectPassedCandidate(appId) {
    const select = document.getElementById('createAppSelect');
    if (!select) return;
    const opt = select.options[select.selectedIndex];

    if (!appId || !opt) {
        const grpA = document.getElementById('groupASection');
        if (grpA) grpA.style.display = 'none';
        const workLocInput = document.getElementById('createWorkLocation');
        if (workLocInput) workLocInput.value = '';
        const overrideNotice = document.getElementById('createOverrideNotice');
        if (overrideNotice) overrideNotice.style.display = 'none';
        return;
    }

    // Fill Group A (Read-only)
    document.getElementById('dispCandId').textContent = opt.getAttribute('data-cand-id') || 'N/A';
    document.getElementById('dispCandName').textContent = opt.getAttribute('data-cand-name') || 'N/A';
    document.getElementById('dispCandEmail').textContent = opt.getAttribute('data-email') || 'N/A';
    document.getElementById('dispCandPhone').textContent = opt.getAttribute('data-phone') || 'N/A';
    document.getElementById('dispPosition').textContent = opt.getAttribute('data-pos') || 'N/A';
    document.getElementById('dispDept').textContent = opt.getAttribute('data-dept') || 'N/A';
    document.getElementById('dispReqId').textContent = opt.getAttribute('data-req-id') || 'N/A';
    document.getElementById('dispHm').textContent = opt.getAttribute('data-hm') || 'N/A';

    const workLoc = opt.getAttribute('data-work-location') || 'Trụ sở chính Mộc RMS';
    const dispWorkLoc = document.getElementById('dispWorkLocation');
    if (dispWorkLoc) dispWorkLoc.textContent = workLoc;

    const recom = opt.getAttribute('data-recom-salary');
    document.getElementById('dispRecomSalary').textContent = recom ? Number(recom).toLocaleString('vi-VN') + ' VND' : 'Chưa có gợi ý';
    document.getElementById('dispInterviewComments').textContent = opt.getAttribute('data-comments') || 'Không có ghi chú thêm.';

    // Hiển thị khung thông tin ứng viên ngay lập tức
    const grpA = document.getElementById('groupASection');
    if (grpA) grpA.style.display = 'block';

    // Gợi ý luôn vào form
    const titleInput = document.getElementById('createOfferedTitle');
    if (titleInput) titleInput.value = opt.getAttribute('data-pos') || '';
    if (recom) {
        const proposedInput = document.getElementById('createProposedSalary');
        if (proposedInput) proposedInput.value = recom;
        autoCalculateProbationSalary(true);
    }

    // Tự động điền Địa điểm làm việc theo database WorkLocation của ứng viên, HR vẫn có thể tự sửa
    const workLocationInput = document.getElementById('createWorkLocation');
    if (workLocationInput) {
        workLocationInput.value = workLoc;
    }
}

function autoCalculateProbationSalary(forceSet = false) {
    const proposed = parseFloat(document.getElementById('createProposedSalary').value);
    if (!isNaN(proposed) && proposed > 0) {
        const minProbation = Math.round(proposed * 0.85);
        const currentProbation = parseFloat(document.getElementById('createProbationSalary').value);
        if (forceSet || isNaN(currentProbation) || currentProbation === 0) {
            document.getElementById('createProbationSalary').value = minProbation;
        }
        validateProbationRuleUi();
    }
}

function validateProbationRuleUi() {
    const proposed = parseFloat(document.getElementById('createProposedSalary').value);
    const probation = parseFloat(document.getElementById('createProbationSalary').value);
    const notice = document.getElementById('probationRuleNotice');

    if (!isNaN(proposed) && !isNaN(probation)) {
        const minProb = proposed * 0.85;
        if (probation < minProb) {
            if (notice) {
                notice.style.color = 'var(--error)';
                notice.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i> CẢNH BÁO: Lương thử việc phải đạt tối thiểu 85% lương chính thức (${minProb.toLocaleString('vi-VN')} VND).`;
            }
            return false;
        } else if (probation > proposed) {
            if (notice) {
                notice.style.color = 'var(--error)';
                notice.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i> CẢNH BÁO: Lương thử việc không được vượt quá lương chính thức (${proposed.toLocaleString('vi-VN')} VND).`;
            }
            return false;
        } else {
            if (notice) {
                notice.style.color = 'var(--brand-dark)';
                notice.innerHTML = `<i class="fa-solid fa-circle-check"></i> Hợp lệ: Đạt ${(probation / proposed * 100).toFixed(1)}% lương chính thức.`;
            }
            return true;
        }
    }
    return true;
}

function submitCreateOffer(isDraft) {
    const appId = document.getElementById('createAppSelect').value;
    const title = (document.getElementById('createOfferedTitle').value || '').trim();
    const proposed = parseFloat(document.getElementById('createProposedSalary').value);
    const probation = parseFloat(document.getElementById('createProbationSalary').value);
    const probationDays = parseInt(document.getElementById('createProbationDays').value);
    const startDate = document.getElementById('createStartDate').value;
    const workLocation = (document.getElementById('createWorkLocation').value || '').trim();
    const benefits = document.getElementById('createBenefits').value || '';

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
            showToast('Mức lương thử việc không tuân thủ luật (>= 85%).', 'danger');
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
        applicationId: parseInt(appId),
        offeredPositionTitle: title,
        proposedSalary: proposed,
        probationSalary: probation,
        probationDays: probationDays,
        expectedStartDate: startDate,
        workLocation: workLocation,
        benefitsPackage: benefits,
        isDraft: isDraft
    };

    fetch('/api/v1/hr/offers', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
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
            showToast('Lỗi: ' + err.message, 'danger');
        });
}

// =========================================================================
// 2. VIEW DETAIL WORKFLOW (Screen 32)
// =========================================================================
function viewOfferDetail(offerId) {
    openModal('detailOfferModal');
    document.getElementById('detailModalTitle').textContent = `Chi Tiết Đề Xuất Offer ${offerId}`;

    fetch(`/api/v1/hr/offers/${offerId}`)
        .then(res => res.json())
        .then(res => {
            if (!res.success) {
                document.getElementById('detailModalBody').innerHTML = `<p style="color:var(--error);">${res.message}</p>`;
                return;
            }
            const d = res.data;
            renderDetailModalHtml(d);
        })
        .catch(err => {
            document.getElementById('detailModalBody').innerHTML = `<p style="color:var(--error);">Lỗi tải dữ liệu: ${err.message}</p>`;
        });
}

function renderDetailModalHtml(d) {
    const body = document.getElementById('detailModalBody');
    const footer = document.getElementById('detailModalFooter');

    const formatVND = num => num ? Number(num).toLocaleString('vi-VN') + ' VND' : 'N/A';
    const pct = (d.proposedSalary && d.probationSalary) ? (d.probationSalary / d.proposedSalary * 100).toFixed(1) : 0;

    let approvalLogsHtml = '<p style="color:var(--muted); font-size:13px;">Chưa có lịch sử phê duyệt của Giám đốc.</p>';
    if (d.approvalHistory && d.approvalHistory.length > 0) {
        approvalLogsHtml = '<div class="history-timeline">' + d.approvalHistory.map(a => `
            <div class="timeline-item">
                <div class="timeline-dot ${a.status === 'Approved' ? 'dot-approved' : 'dot-rejected'}"></div>
                <div class="timeline-header">
                    ${a.directorName || 'Giám đốc'} - <span style="font-weight:700; color:${a.status === 'Approved' ? 'var(--brand-dark)' : 'var(--error)'}">${a.status}</span>
                </div>
                <div class="timeline-time">${a.approvedAt ? a.approvedAt.replace('T', ' ') : ''}</div>
                <div class="timeline-content">${a.directorComments || 'Không có ghi chú thêm.'}</div>
            </div>
        `).join('') + '</div>';
    }


    body.innerHTML = `
        <!-- Group A -->
        <div class="detail-section">
            <div class="section-title"><i class="fa-solid fa-user"></i> Hồ Sơ Ứng Viên & Kết Quả Phỏng Vấn</div>
            <div class="detail-grid-3">
                <div><div class="detail-item-label">Họ tên ứng viên</div><div class="detail-item-value" style="font-weight:700;">${d.candidateName || 'N/A'}</div></div>
                <div><div class="detail-item-label">Email</div><div class="detail-item-value">${d.candidateEmail || 'N/A'}</div></div>
                <div><div class="detail-item-label">Số điện thoại</div><div class="detail-item-value">${d.candidatePhone || 'N/A'}</div></div>
                <div><div class="detail-item-label">Vị trí ứng tuyển</div><div class="detail-item-value">${d.appliedPosition || 'N/A'}</div></div>
                <div><div class="detail-item-label">Phòng ban</div><div class="detail-item-value">${d.departmentName || 'N/A'}</div></div>
                <div><div class="detail-item-label">Mã Job Requisition</div><div class="detail-item-value">${d.requisitionId || 'N/A'}</div></div>
            </div>
            <div style="margin-top:12px; padding-top:10px; border-top:1px dashed var(--line);" class="detail-grid">
                <div>
                    <div class="detail-item-label">Kết quả Hội đồng phỏng vấn:</div>
                    <div class="detail-item-value" style="color:var(--brand-dark); font-weight:700;"><i class="fa-solid fa-circle-check"></i> ${d.finalDecision || 'Passed'}</div>
                    <div style="font-size:12.5px; color:var(--muted); margin-top:2px;">${d.interviewSummaryComments || ''}</div>
                </div>
                <div>
                    <div class="detail-item-label">Lương khuyến nghị từ Hiring Manager:</div>
                    <div class="detail-item-value" style="color:var(--brand-dark); font-weight:700;">${formatVND(d.recommendedSalary)}</div>
                </div>
            </div>
        </div>

        <!-- Group B -->
        <div class="detail-section">
            <div class="section-title"><i class="fa-solid fa-coins"></i> Gói Đãi Ngộ & Điều Khoản Offer</div>
            <div class="detail-grid-3">
                <div><div class="detail-item-label">Chức danh đề xuất</div><div class="detail-item-value" style="font-weight:700;">${d.offeredPositionTitle || '-'}</div></div>
                <div><div class="detail-item-label">Lương chính thức</div><div class="detail-item-value" style="color:var(--ink); font-weight:700;">${formatVND(d.proposedSalary)}</div></div>
                <div>
                    <div class="detail-item-label">Lương thử việc</div>
                    <div class="detail-item-value" style="color:var(--brand-dark); font-weight:700;">
                        ${formatVND(d.probationSalary)} 
                        <span class="salary-badge-pct">${pct}%</span>
                    </div>
                </div>
                <div><div class="detail-item-label">Thời gian thử việc</div><div class="detail-item-value">${d.probationDays || 60} ngày</div></div>
                <div><div class="detail-item-label">Ngày bắt đầu dự kiến</div><div class="detail-item-value">${d.expectedStartDate || 'Thỏa thuận'}</div></div>
                <div><div class="detail-item-label">Địa điểm làm việc</div><div class="detail-item-value">${d.workLocation || 'Trụ sở chính Mộc RMS'}</div></div>
            </div>
            <div style="margin-top:12px;">
                <div class="detail-item-label">Chế độ phúc lợi:</div>
                <div class="detail-item-value" style="background:var(--surface); padding:8px 12px; border:1px solid var(--line); border-radius:4px; font-size:13px;">${d.benefitsPackage || 'Theo quy chế công ty.'}</div>
            </div>
        </div>

        <!-- Audit Logs: Lịch Sử Phê Duyệt Của Director -->
        <div class="detail-section">
            <div class="section-title"><i class="fa-solid fa-stamp"></i> Lịch Sử Phê Duyệt Của Director</div>
            ${approvalLogsHtml}
        </div>
    `;

    // Contextual Action Buttons in Footer
    let actionButtonsHtml = '<button type="button" class="btn btn-secondary" onclick="closeModal(\'detailOfferModal\')">Đóng</button>';

    if (d.offerStatus === 'Draft') {
        actionButtonsHtml += `
            <button type="button" class="btn btn-danger" style="background:#dc2626; border-color:#dc2626; color:#fff;" onclick="closeModal('detailOfferModal'); confirmDeleteOffer(${d.offerId});">
                <i class="fa-solid fa-trash-can" style="margin-right:6px;"></i> Xóa Bản Thảo (Delete)
            </button>
            <button type="button" class="btn btn-primary" onclick="closeModal('detailOfferModal'); openEditOfferModal(${d.offerId});">
                <i class="fa-solid fa-pen-to-square" style="margin-right:6px;"></i> Chỉnh Sửa (Update)
            </button>
        `;
    } else if (d.offerStatus === 'Director_Approved' || d.offerStatus === 'Approved') {
        actionButtonsHtml += `
            <button type="button" class="btn btn-primary" onclick="closeModal('detailOfferModal'); confirmSendOffer(${d.offerId});">
                <i class="fa-solid fa-paper-plane" style="margin-right:6px;"></i> Phát Hành Offer Letter Tới Ứng Viên
            </button>
        `;
    }

    footer.innerHTML = actionButtonsHtml;
}

// =========================================================================
// 3. UPDATE OFFER WORKFLOW
// =========================================================================
function openEditOfferModal(offerId) {
    fetch(`/api/v1/hr/offers/${offerId}`)
        .then(res => res.json())
        .then(res => {
            if (!res.success) { showToast(res.message, 'danger'); return; }
            const d = res.data;

            if (d.offerStatus !== 'Draft') {
                showToast('Chỉ duy nhất đề xuất ở trạng thái Bản thảo (Draft) mới được phép chỉnh sửa.', 'warning');
                return;
            }

            document.getElementById('editOfferId').value = d.offerId;
            document.getElementById('editModalTitle').textContent = `Chỉnh Sửa Offer Proposal ${d.offerId}`;
            document.getElementById('editOfferedTitle').value = d.offeredPositionTitle || '';
            document.getElementById('editProposedSalary').value = d.proposedSalary || '';
            document.getElementById('editProbationSalary').value = d.probationSalary || '';
            document.getElementById('editProbationDays').value = d.probationDays || 60;
            document.getElementById('editStartDate').value = d.expectedStartDate || '';
            document.getElementById('editWorkLocation').value = d.workLocation || '';
            document.getElementById('editBenefits').value = d.benefitsPackage || '';

            // Giới hạn ngày chọn cho editStartDate: phải lớn hơn ngày hiện tại
            const editStartDateInput = document.getElementById('editStartDate');
            if (editStartDateInput) {
                editStartDateInput.min = getTomorrowDateString();
            }

            // Nếu bị Director_Rejected hoặc Declined -> hiển thị lý do / hướng dẫn
            const rejectBanner = document.getElementById('editRejectBanner');
            if (d.offerStatus === 'Director_Rejected' || d.offerStatus === 'Rejected') {
                const latestReject = (d.approvalHistory && d.approvalHistory.length > 0) ? d.approvalHistory[0].directorComments : 'Vui lòng điều chỉnh lại mức lương/định biên theo yêu cầu.';
                document.getElementById('editRejectComment').textContent = 'Director từ chối: ' + latestReject;
                rejectBanner.style.display = 'flex';
            } else if (d.offerStatus === 'Declined') {
                document.getElementById('editRejectComment').textContent = 'Ứng viên đã từ chối thư mời trước đó. HR phát hành lại gói Offer mới theo quy tắc GBR-07.';
                rejectBanner.style.display = 'flex';
            } else {
                rejectBanner.style.display = 'none';
            }

            editValidateProbation();
            openModal('editOfferModal');
        })
        .catch(err => showToast('Lỗi nạp dữ liệu: ' + err.message, 'danger'));
}

function editAutoCalculateProbation(forceSet = false) {
    const proposed = parseFloat(document.getElementById('editProposedSalary').value);
    if (!isNaN(proposed) && proposed > 0) {
        const minProb = Math.round(proposed * 0.85);
        const currentProb = parseFloat(document.getElementById('editProbationSalary').value);
        if (forceSet || isNaN(currentProb) || currentProb === 0) {
            document.getElementById('editProbationSalary').value = minProb;
        }
        editValidateProbation();
    }
}

function editValidateProbation() {
    const proposed = parseFloat(document.getElementById('editProposedSalary').value);
    const probation = parseFloat(document.getElementById('editProbationSalary').value);
    const notice = document.getElementById('editProbationNotice');

    if (!isNaN(proposed) && !isNaN(probation)) {
        const minProb = proposed * 0.85;
        if (probation < minProb) {
            if (notice) {
                notice.style.color = 'var(--error)';
                notice.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i> CẢNH BÁO: Lương thử việc tối thiểu phải là ${minProb.toLocaleString('vi-VN')} VND (85%).`;
            }
            return false;
        } else if (probation > proposed) {
            if (notice) {
                notice.style.color = 'var(--error)';
                notice.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i> CẢNH BÁO: Lương thử việc không được vượt quá lương chính thức (${proposed.toLocaleString('vi-VN')} VND).`;
            }
            return false;
        } else {
            if (notice) {
                notice.style.color = 'var(--brand-dark)';
                notice.innerHTML = `<i class="fa-solid fa-circle-check"></i> Đạt ${(probation / proposed * 100).toFixed(1)}% lương chính thức.`;
            }
            return true;
        }
    }
    return true;
}

function submitUpdateOffer(isDraft = false) {
    const id = document.getElementById('editOfferId').value;
    const title = (document.getElementById('editOfferedTitle').value || '').trim();
    const proposed = parseFloat(document.getElementById('editProposedSalary').value);
    const probation = parseFloat(document.getElementById('editProbationSalary').value);
    const probationDays = parseInt(document.getElementById('editProbationDays').value);
    const startDate = document.getElementById('editStartDate').value;
    const workLocation = (document.getElementById('editWorkLocation').value || '').trim();
    const benefits = document.getElementById('editBenefits').value || '';

    // 1. Chức danh bắt buộc
    if (!title) {
        showToast('Vị trí chức danh đề xuất không được để trống.', 'warning');
        return;
    }

    // 2. Lương chính thức phải > 0
    if (isNaN(proposed) || proposed <= 0) {
        showToast('Mức lương chính thức phải lớn hơn 0.', 'warning');
        return;
    }

    // 3. Lương thử việc phải > 0
    if (isNaN(probation) || probation <= 0) {
        showToast('Mức lương thử việc phải lớn hơn 0.', 'warning');
        return;
    }

    // 4. Tuân thủ quy định: Lương thử việc >= 85% và <= 100% lương chính thức
    if (!editValidateProbation()) {
        if (probation > proposed) {
            showToast('Lương thử việc không được vượt quá lương chính thức.', 'danger');
        } else {
            showToast('Lương thử việc chưa đạt tối thiểu 85% lương chính thức.', 'danger');
        }
        return;
    }

    // 5. Thời gian thử việc phải > 0
    if (isNaN(probationDays) || probationDays <= 0) {
        showToast('Thời gian thử việc phải lớn hơn 0 ngày.', 'warning');
        return;
    }

    // 6. Ngày bắt đầu dự kiến bắt buộc và phải > ngày hiện tại
    if (!startDate) {
        showToast('Ngày bắt đầu dự kiến không được để trống.', 'warning');
        return;
    }
    if (startDate <= getTodayDateString()) {
        showToast('Ngày bắt đầu dự kiến phải lớn hơn ngày hiện tại.', 'danger');
        return;
    }

    // 7. Địa điểm làm việc bắt buộc
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
        isDraft: isDraft
    };

    fetch(`/api/v1/hr/offers/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
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
                const msg = isDraft ? 'Đã lưu cập nhật bản thảo Offer thành công!' : 'Đã nộp trình đề xuất Offer lên Giám đốc thành công!';
                showToast(msg, 'success');
                closeModal('editOfferModal');
                setTimeout(() => window.location.reload(), 800);
            } else {
                showToast(res?.message || 'Lỗi cập nhật Offer', 'danger');
            }
        })
        .catch(err => showToast('Lỗi kết nối: ' + err.message, 'danger'));
}

// =========================================================================
// 4. SEND OFFER WORKFLOW (Khi Status == Director_Approved)
// =========================================================================
function confirmSendOffer(offerId) {
    if (!confirm(`Bạn có chắc chắn muốn phát hành Thư mời làm việc (Offer Letter) cho gói Offer ${offerId} tới ứng viên không?\n\nTrạng thái sẽ được chuyển sang 'Sent_Candidate' và đơn ứng tuyển sẽ được cập nhật.`)) {
        return;
    }

    fetch(`/api/v1/hr/offers/${offerId}/send`, { method: 'POST' })
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
                showToast('Đã phát hành và gửi Offer Letter tới ứng viên thành công!', 'success');
                setTimeout(() => window.location.reload(), 800);
            } else {
                showToast(res?.message || 'Lỗi khi gửi Offer', 'danger');
            }
        })
        .catch(err => showToast('Lỗi kết nối: ' + err.message, 'danger'));
}

// =========================================================================
// 5. DELETE DRAFT OFFER WORKFLOW (Chỉ khi Status == Draft)
// =========================================================================
function confirmDeleteOffer(offerId) {
    if (!confirm(`Bạn có chắc chắn muốn xóa bản thảo Offer ${offerId} này khỏi bảng không? (Dữ liệu sẽ được ẩn khỏi bảng danh sách nhưng vẫn lưu lại trong hệ thống)`)) {
        return;
    }

    fetch(`/api/v1/hr/offers/${offerId}`, { method: 'DELETE' })
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
                showToast('Đã xóa bản thảo Offer thành công.', 'success');
                setTimeout(() => window.location.reload(), 800);
            } else {
                showToast(res?.message || 'Lỗi khi xóa Offer', 'danger');
            }
        })
        .catch(err => showToast('Lỗi kết nối: ' + err.message, 'danger'));
}
