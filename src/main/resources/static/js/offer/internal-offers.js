/**
 * =============================================================================
 * internal-offers.js - Logic Quản lý đề xuất tuyển dụng (Offer Management)
 * SWP_RMS_Group-2 | Recruitment Management System
 * =============================================================================
 */

document.addEventListener('DOMContentLoaded', function () {
    // --- State variables ---
    let currentPage = 0;
    const pageSize = 10;
    let currentOfferId = null;
    let currentOfferData = null;

    // --- DOM Elements ---
    const tableBody = document.getElementById('offersTableBody');
    const searchInput = document.getElementById('searchInput');
    const filterStatusSelect = document.getElementById('filterStatusSelect');
    const btnRefresh = document.getElementById('btnRefresh');
    const btnResetFilter = document.getElementById('btnResetFilter');
    const btnExportCsv = document.getElementById('btnExportCsv');
    const btnPrevPage = document.getElementById('btnPrevPage');
    const btnNextPage = document.getElementById('btnNextPage');
    const paginationInfo = document.getElementById('paginationInfo');
    const currentPageBadge = document.getElementById('currentPageBadge');
    const currentRoleSelect = document.getElementById('currentRoleSelect');
    const roleSubtitle = document.getElementById('roleSubtitle');
    const btnOpenCreateModal = document.getElementById('btnOpenCreateModal');

    // Stats
    const statDraft = document.getElementById('statDraftOffers');
    const statPending = document.getElementById('statPendingOffers');
    const statApproved = document.getElementById('statApprovedOffers');
    const statNegotiating = document.getElementById('statNegotiatingOffers');
    const statAccepted = document.getElementById('statAcceptedOffers');

    // Create Modal
    const modalCreate = document.getElementById('modalCreateOffer');
    const formCreateOffer = document.getElementById('formCreateOffer');
    const inputProposedSalary = document.getElementById('createProposedSalary');
    const inputProbationSalary = document.getElementById('createProbationSalary');
    const salaryRatioFill = document.getElementById('salaryRatioFill');
    const salaryRatioText = document.getElementById('salaryRatioText');
    const btnSet85Percent = document.getElementById('btnSet85Percent');
    const btnSet100Percent = document.getElementById('btnSet100Percent');
    const createProposedById = document.getElementById('createProposedById');
    const btnSaveDraftOffer = document.getElementById('btnSaveDraftOffer');
    const btnSubmitCreateOffer = document.getElementById('btnSubmitCreateOffer');

    // Edit Modal
    const modalEdit = document.getElementById('modalEditOffer');
    const formEditOffer = document.getElementById('formEditOffer');
    const editOfferId = document.getElementById('editOfferId');
    const editProposedPosition = document.getElementById('editProposedPosition');
    const editWorkLocation = document.getElementById('editWorkLocation');
    const editProposedSalary = document.getElementById('editProposedSalary');
    const editProbationSalary = document.getElementById('editProbationSalary');
    const editProbationDays = document.getElementById('editProbationDays');
    const editSalaryRatioFill = document.getElementById('editSalaryRatioFill');
    const editSalaryRatioText = document.getElementById('editSalaryRatioText');
    const btnEditSet85Percent = document.getElementById('btnEditSet85Percent');
    const btnEditSet100Percent = document.getElementById('btnEditSet100Percent');

    // Detail Modal
    const modalDetail = document.getElementById('modalDetailOffer');
    const detailCandidateName = document.getElementById('detailCandidateName');
    const detailCandidateSub = document.getElementById('detailCandidateSub');
    const detailPosition = document.getElementById('detailPosition');
    const detailLocation = document.getElementById('detailLocation');
    const detailProposedSalary = document.getElementById('detailProposedSalary');
    const detailProbationSalary = document.getElementById('detailProbationSalary');
    const detailProbationDays = document.getElementById('detailProbationDays');

    // HM Action Section
    const hmActionSection = document.getElementById('hmActionSection');
    const hmActionNotice = document.getElementById('hmActionNotice');
    const hmActionButtonsContainer = document.getElementById('hmActionButtonsContainer');

    // Director Approval Section
    const directorApprovalSection = document.getElementById('directorApprovalSection');
    const directorCommentsInput = document.getElementById('directorCommentsInput');
    const btnApproveOffer = document.getElementById('btnApproveOffer');
    const btnRejectOffer = document.getElementById('btnRejectOffer');

    // Candidate Link Section
    const candidateLinkSection = document.getElementById('candidateLinkSection');
    const btnOpenCandidateView = document.getElementById('btnOpenCandidateView');

    // Timelines
    const approvalHistoryContainer = document.getElementById('approvalHistoryContainer');
    const negotiationHistoryContainer = document.getElementById('negotiationHistoryContainer');

    // HR Negotiation Response Modal
    const modalHrNegotiationResponse = document.getElementById('modalHrNegotiationResponse');
    const formHrNegotiationResponse = document.getElementById('formHrNegotiationResponse');
    const hrResponseOfferId = document.getElementById('hrResponseOfferId');
    const hrResponseNegotiationId = document.getElementById('hrResponseNegotiationId');
    const hrModalCandidateCounterSalary = document.getElementById('hrModalCandidateCounterSalary');
    const hrModalCandidateNotes = document.getElementById('hrModalCandidateNotes');
    const hrResponseNotesInput = document.getElementById('hrResponseNotesInput');
    const btnSubmitHrResponse = document.getElementById('btnSubmitHrResponse');

    // Attach money input formatters
    if (window.RMS_UI && window.RMS_UI.attachMoneyInput) {
        RMS_UI.attachMoneyInput(inputProposedSalary);
        RMS_UI.attachMoneyInput(inputProbationSalary);
        RMS_UI.attachMoneyInput(editProposedSalary);
        RMS_UI.attachMoneyInput(editProbationSalary);
    }

    // Helper: read numeric value safely from input
    function getNumericValue(input) {
        if (!input) return 0;
        if (typeof input.getRawValue === 'function') {
            return input.getRawValue();
        }
        if (input.dataset && input.dataset.rawValue !== undefined) {
            return parseFloat(input.dataset.rawValue) || 0;
        }
        const clean = (input.value || '').toString().replace(/[^\d.-]/g, '');
        return parseFloat(clean) || 0;
    }

    // Helper: Escape HTML
    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    // Close buttons for modals using RMS_UI
    document.querySelectorAll('.btn-close-modal').forEach(btn => {
        btn.addEventListener('click', () => {
            if (window.RMS_UI) {
                RMS_UI.closeModal(modalCreate);
                RMS_UI.closeModal(modalEdit);
                RMS_UI.closeModal(modalDetail);
                if (modalHrNegotiationResponse) RMS_UI.closeModal(modalHrNegotiationResponse);
            } else {
                modalCreate.classList.remove('active');
                modalEdit.classList.remove('active');
                modalDetail.classList.remove('active');
                if (modalHrNegotiationResponse) modalHrNegotiationResponse.classList.remove('active');
            }
        });
    });

    [modalCreate, modalEdit, modalDetail, modalHrNegotiationResponse].forEach(modal => {
        if (modal) {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    if (window.RMS_UI) RMS_UI.closeModal(modal);
                    else modal.classList.remove('active');
                }
            });
        }
    });

    // --- Role Switcher & Dynamic UI adjustments ---
    function getCurrentRole() {
        return currentRoleSelect ? currentRoleSelect.value : 'HM';
    }

    function applyRoleUI(notify = false) {
        const role = getCurrentRole();
        const selectedOption = currentRoleSelect ? currentRoleSelect.options[currentRoleSelect.selectedIndex] : null;
        const userId = selectedOption ? (selectedOption.getAttribute('data-user-id') || 2) : 2;
        if (createProposedById) createProposedById.value = userId;

        if (role === 'HM') {
            if (btnOpenCreateModal) btnOpenCreateModal.style.display = 'inline-flex';
            if (roleSubtitle) {
                roleSubtitle.innerHTML = `Đang ở vai trò <strong>Quản lý tuyển dụng (Hiring Manager)</strong>: Tạo, chỉnh sửa bản thảo, trình duyệt Giám đốc &amp; Xác nhận tuyển dụng.`;
            }
        } else if (role === 'HR') {
            // HR không có nút Tạo đề xuất Offer mới (HM phụ trách tạo đề xuất)
            if (btnOpenCreateModal) btnOpenCreateModal.style.display = 'none';
            if (roleSubtitle) {
                roleSubtitle.innerHTML = `Đang ở vai trò <strong>Chuyên viên nhân sự (HR)</strong>: Phát hành Offer đã duyệt, tiếp nhận &amp; phản hồi đàm phán từ ứng viên, theo dõi danh sách &amp; xuất báo cáo.`;
            }
        } else if (role === 'Director') {
            if (btnOpenCreateModal) btnOpenCreateModal.style.display = 'none';
            if (roleSubtitle) {
                roleSubtitle.innerHTML = `Đang ở vai trò <strong>Giám đốc (Director)</strong>: Thẩm định và Phê duyệt các đề xuất ngân sách Offer.`;
            }
        }

        if (notify && window.RMS_UI && selectedOption) {
            RMS_UI.showToast(`Đã chuyển sang vai trò: ${selectedOption.text}`, 'info');
        }
    }

    if (currentRoleSelect) {
        currentRoleSelect.addEventListener('change', function () {
            applyRoleUI(true);
            loadOffers();
        });
    }

    // --- Load distinct Offer Statuses from Database ---
    async function loadOfferStatuses() {
        try {
            const res = await fetch('/api/v1/offers/statuses');
            if (res.ok) {
                const apiRes = await res.json();
                const dbStatuses = apiRes.data || [];
                const allStatuses = ['Draft', ...dbStatuses.filter(s => s.toUpperCase() !== 'DRAFT')];
                const currentVal = filterStatusSelect.value;
                filterStatusSelect.innerHTML = '<option value="">Tất cả trạng thái</option>';
                allStatuses.forEach(st => {
                    const opt = document.createElement('option');
                    opt.value = st;
                    const mapping = window.RMS_STATUS_MAP ? (RMS_STATUS_MAP[st] || RMS_STATUS_MAP[st.toUpperCase()]) : null;
                    opt.textContent = mapping ? mapping.label : st;
                    filterStatusSelect.appendChild(opt);
                });
                if (currentVal && allStatuses.includes(currentVal)) {
                    filterStatusSelect.value = currentVal;
                }
            }
        } catch (e) {
            console.error('Không thể nạp trạng thái từ database:', e);
        }
    }

    // --- Load Offers from REST API ---
    async function loadOffers() {
        const query = searchInput ? searchInput.value.trim() : '';
        const status = filterStatusSelect ? filterStatusSelect.value.trim() : '';

        if (window.RMS_UI) {
            tableBody.innerHTML = RMS_UI.createTableSkeletonHtml(8, 5);
        } else {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align: center; padding: 2rem; color: var(--color-text-muted);">
                        <i class="bi bi-arrow-repeat spin" style="font-size: 1.5rem; display: inline-block;"></i> Đang tải dữ liệu...
                    </td>
                </tr>
            `;
        }

        try {
            const params = new URLSearchParams({
                page: currentPage,
                size: pageSize
            });
            if (query) params.append('candidateName', query);
            if (status) params.append('status', status);

            const res = await fetch(`/api/v1/offers?${params.toString()}`);
            if (!res.ok) {
                const errorData = await res.json().catch(() => null);
                const msg = (errorData && errorData.message) ? errorData.message : `Lỗi ${res.status}: ${res.statusText}`;
                throw new Error(msg);
            }

            const apiRes = await res.json();
            const pageData = apiRes.data;
            const offers = pageData.content || [];

            renderTable(offers);
            updatePagination(pageData);
            updateStats(offers);

        } catch (err) {
            console.error(err);
            tableBody.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align: center; padding: 2rem; color: var(--color-danger);">
                        <i class="bi bi-exclamation-circle me-1"></i> Có lỗi xảy ra khi tải dữ liệu: ${escapeHtml(err.message)}
                    </td>
                </tr>
            `;
        }
    }

    // --- Render Table Rows with Role-Specific Actions ---
    function renderTable(offers) {
        if (!offers || offers.length === 0) {
            if (window.RMS_UI) {
                tableBody.innerHTML = `<tr><td colspan="8">${RMS_UI.createEmptyStateHtml({
                    icon: 'bi-inbox',
                    title: 'Không tìm thấy lời mời làm việc nào',
                    message: 'Vui lòng thay đổi bộ lọc tìm kiếm hoặc tạo đề xuất mới.'
                })}</td></tr>`;
            } else {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="8" style="text-align: center; padding: 3rem; color: var(--color-text-muted);">
                            <i class="bi bi-inbox" style="font-size: 2rem; display: block; margin-bottom: 0.5rem;"></i>
                            Không tìm thấy lời mời làm việc nào phù hợp.
                        </td>
                    </tr>
                `;
            }
            return;
        }

        const role = getCurrentRole();

        tableBody.innerHTML = offers.map(item => {
            const propSal = item.proposedSalary || 0;
            const probSal = item.probationSalary || 0;
            const ratio = propSal > 0 ? ((probSal / propSal) * 100).toFixed(1) : 0;
            const isRatioValid = ratio >= 85 && ratio <= 100;
            const ratioColor = isRatioValid ? 'color: #16a34a;' : 'color: #dc2626; font-weight: bold;';
            const statusUpper = (item.offerStatus || '').toUpperCase();

            // Build Action Buttons based on Role & Status
            let actionButtons = `
                <button class="btn btn-sm btn-outline-secondary btn-view-offer" data-offer-id="${item.offerId}" title="Xem chi tiết">
                    <i class="bi bi-eye"></i> Chi tiết
                </button>
            `;

            if (role === 'HM' || role === 'Admin') {
                if (statusUpper === 'DRAFT') {
                    actionButtons = `
                        <button class="btn btn-sm btn-outline-primary btn-submit-director" data-offer-id="${item.offerId}" title="Trình duyệt Giám đốc">
                            <i class="bi bi-send"></i> Trình duyệt
                        </button>
                        <button class="btn btn-sm btn-outline-warning btn-edit-offer" data-offer-id="${item.offerId}" title="Chỉnh sửa bản thảo">
                            <i class="bi bi-pencil"></i> Sửa
                        </button>
                        <button class="btn btn-sm btn-outline-danger btn-delete-offer" data-offer-id="${item.offerId}" title="Xóa bản thảo">
                            <i class="bi bi-trash"></i> Xóa
                        </button>
                        ${actionButtons}
                    `;
                } else if (statusUpper === 'REJECTED') {
                    actionButtons = `
                        <button class="btn btn-sm btn-outline-warning btn-edit-offer" data-offer-id="${item.offerId}" title="Chỉnh sửa lại đề xuất">
                            <i class="bi bi-arrow-repeat"></i> Sửa lại
                        </button>
                        ${actionButtons}
                    `;
                } else if (statusUpper === 'APPROVED') {
                    if (role === 'Admin') {
                        actionButtons = `
                            <button class="btn btn-sm btn-success btn-send-offer" data-offer-id="${item.offerId}" title="Phát hành và gửi thư mời làm việc tới ứng viên">
                                <i class="bi bi-send-check"></i> Phát hành Offer
                            </button>
                            ${actionButtons}
                        `;
                    }
                    // Lưu ý: Hiring Manager KHÔNG có quyền phát hành offer tới ứng viên (chỉ HR mới có quyền)
                } else if (statusUpper === 'NEGOTIATING' || statusUpper === 'NEGOTIATE') {
                    actionButtons = `
                        <button class="btn btn-sm btn-outline-warning btn-edit-offer" data-offer-id="${item.offerId}" title="Cập nhật lại đề xuất sau đàm phán">
                            <i class="bi bi-pencil-square"></i> Cập nhật đề xuất
                        </button>
                        ${actionButtons}
                    `;
                } else if (statusUpper === 'ACCEPTED') {
                    actionButtons = `
                        <button class="btn btn-sm btn-success btn-confirm-hiring" data-offer-id="${item.offerId}" title="Xác nhận tuyển dụng chính thức">
                            <i class="bi bi-person-check"></i> Tuyển dụng
                        </button>
                        ${actionButtons}
                    `;
                }
            } else if (role === 'HR') {
                if (statusUpper === 'APPROVED') {
                    actionButtons = `
                        <button class="btn btn-sm btn-success btn-send-offer" data-offer-id="${item.offerId}" title="Phát hành và gửi thư mời làm việc tới ứng viên">
                            <i class="bi bi-send-check"></i> Phát hành Offer
                        </button>
                        ${actionButtons}
                    `;
                } else if (statusUpper === 'NEGOTIATING' || statusUpper === 'NEGOTIATE') {
                    actionButtons = `
                        <button class="btn btn-sm btn-negotiate-response" data-offer-id="${item.offerId}" style="background: #7c3aed; color: #ffffff; border-color: #7c3aed;" title="Phản hồi yêu cầu đàm phán từ ứng viên">
                            <i class="bi bi-chat-dots"></i> Phản hồi Đàm phán
                        </button>
                        ${actionButtons}
                    `;
                } else if (statusUpper === 'ACCEPTED') {
                    actionButtons = `
                        <button class="btn btn-sm btn-success btn-confirm-hiring" data-offer-id="${item.offerId}" title="Xác nhận tuyển dụng chính thức">
                            <i class="bi bi-person-check"></i> Tuyển dụng
                        </button>
                        ${actionButtons}
                    `;
                }
            } else if (role === 'Director') {
                if (statusUpper.includes('PENDING')) {
                    actionButtons = `
                        <button class="btn btn-sm btn-warning btn-director-review" data-offer-id="${item.offerId}" title="Phê duyệt hoặc Từ chối" style="font-weight: 600;">
                            <i class="bi bi-shield-check"></i> Phê duyệt
                        </button>
                        ${actionButtons}
                    `;
                }
            }

            const formattedPropSal = window.RMS_FORMATTER ? RMS_FORMATTER.currency(propSal) : propSal.toLocaleString('vi-VN') + ' ₫';
            const formattedProbSal = window.RMS_FORMATTER ? RMS_FORMATTER.currency(probSal) : probSal.toLocaleString('vi-VN') + ' ₫';
            const formattedDate = window.RMS_FORMATTER ? RMS_FORMATTER.dateTime(item.createdAt) : item.createdAt;
            const statusBadgeHtml = window.RMS_UI ? RMS_UI.createStatusBadge(item.offerStatus) : `<span class="badge-status">${item.offerStatus}</span>`;

            return `
                <tr>
                    <td class="tabular-nums" style="font-weight: 600; color: var(--color-text-muted);">#${item.offerId}</td>
                    <td style="max-width: 220px;">
                        <div class="truncate" style="font-weight: 600; color: var(--color-text-primary);" title="${escapeHtml(item.candidateName || '')}">${escapeHtml(item.candidateName || 'Ứng viên #' + (item.candidateId || item.applicationId))}</div>
                        <div class="truncate" style="font-size: 0.8rem; color: var(--color-text-secondary);" title="${escapeHtml(item.proposedPosition || item.jobTitle || '')}">${escapeHtml(item.proposedPosition || item.jobTitle || 'Chưa rõ vị trí')}</div>
                    </td>
                    <td class="text-right tabular-nums" style="font-weight: 600;">${formattedPropSal}</td>
                    <td class="text-right tabular-nums">
                        <div>${formattedProbSal}</div>
                        <div style="font-size: 0.75rem; ${ratioColor}">
                            ${ratio}% lương chính thức
                        </div>
                    </td>
                    <td style="text-align: center;">${statusBadgeHtml}</td>
                    <td>
                        <div class="truncate" style="max-width: 150px; font-size: 0.85rem;" title="${escapeHtml(item.proposedByName || '')}">${escapeHtml(item.proposedByName || ('Nhân sự #' + (item.proposedById || '-')))}</div>
                    </td>
                    <td class="tabular-nums" style="font-size: 0.85rem; color: var(--color-text-secondary);">${formattedDate}</td>
                    <td style="text-align: right;">
                        <div class="nowrap" style="display: inline-flex; gap: 0.35rem; justify-content: flex-end; align-items: center;">
                            ${actionButtons}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

        // Wire Event Listeners to dynamic buttons
        document.querySelectorAll('.btn-view-offer').forEach(btn => {
            btn.addEventListener('click', function () {
                openDetailModal(this.getAttribute('data-offer-id'));
            });
        });

        document.querySelectorAll('.btn-director-review').forEach(btn => {
            btn.addEventListener('click', function () {
                openDetailModal(this.getAttribute('data-offer-id'));
            });
        });

        document.querySelectorAll('.btn-edit-offer').forEach(btn => {
            btn.addEventListener('click', function () {
                openEditModal(this.getAttribute('data-offer-id'));
            });
        });

        document.querySelectorAll('.btn-submit-director').forEach(btn => {
            btn.addEventListener('click', function () {
                handleSubmitToDirector(this.getAttribute('data-offer-id'));
            });
        });

        document.querySelectorAll('.btn-delete-offer').forEach(btn => {
            btn.addEventListener('click', function () {
                handleDeleteOffer(this.getAttribute('data-offer-id'));
            });
        });

        document.querySelectorAll('.btn-confirm-hiring').forEach(btn => {
            btn.addEventListener('click', function () {
                handleConfirmHiring(this.getAttribute('data-offer-id'));
            });
        });

        document.querySelectorAll('.btn-send-offer').forEach(btn => {
            btn.addEventListener('click', function () {
                handleSendOfferToCandidate(this.getAttribute('data-offer-id'));
            });
        });

        document.querySelectorAll('.btn-negotiate-response').forEach(btn => {
            btn.addEventListener('click', function () {
                openHrNegotiationModal(this.getAttribute('data-offer-id'));
            });
        });
    }

    // --- Update Pagination ---
    function updatePagination(pageData) {
        const total = pageData.totalElements || 0;
        const totalPages = pageData.totalPages || 1;
        const current = pageData.number || 0;

        if (paginationInfo) {
            paginationInfo.textContent = `Hiển thị ${pageData.numberOfElements || 0} trên ${total} đề xuất (Trang ${current + 1}/${totalPages})`;
        }
        if (currentPageBadge) {
            currentPageBadge.textContent = `Trang ${current + 1} / ${totalPages}`;
        }

        if (btnPrevPage) btnPrevPage.disabled = current === 0;
        if (btnNextPage) btnNextPage.disabled = current >= totalPages - 1;
    }

    // --- Update Quick Stat Counters ---
    function updateStats(offers) {
        let draft = 0, pending = 0, approved = 0, sent = 0, accepted = 0;
        offers.forEach(o => {
            const s = (o.offerStatus || '').toUpperCase();
            if (s === 'DRAFT') draft++;
            else if (s.includes('PENDING')) pending++;
            else if (s === 'APPROVED') approved++;
            else if (s.includes('SENT')) sent++;
            else if (s === 'ACCEPTED') accepted++;
        });

        if (statDraft) statDraft.textContent = draft;
        if (statPending) statPending.textContent = pending;
        if (statApproved) statApproved.textContent = approved;
        const statSent = document.getElementById('statSentOffers');
        if (statSent) statSent.textContent = sent;
        if (statAccepted) statAccepted.textContent = accepted;
    }

    // --- Pagination Event Listeners ---
    if (btnPrevPage) {
        btnPrevPage.addEventListener('click', () => {
            if (currentPage > 0) {
                currentPage--;
                loadOffers();
            }
        });
    }

    if (btnNextPage) {
        btnNextPage.addEventListener('click', () => {
            currentPage++;
            loadOffers();
        });
    }

    // --- Search & Filter Debounce / Change ---
    let searchTimeout;
    if (searchInput) {
        searchInput.addEventListener('input', () => {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                currentPage = 0;
                loadOffers();
            }, 350);
        });
    }

    if (filterStatusSelect) {
        filterStatusSelect.addEventListener('change', () => {
            currentPage = 0;
            loadOffers();
        });
    }

    if (btnRefresh) {
        btnRefresh.addEventListener('click', () => {
            loadOfferStatuses();
            loadOffers();
            if (window.RMS_UI) RMS_UI.showToast('Đã làm mới dữ liệu đề xuất', 'info');
        });
    }

    // --- Reset Filter ---
    if (btnResetFilter) {
        btnResetFilter.addEventListener('click', () => {
            if (searchInput) searchInput.value = '';
            if (filterStatusSelect) filterStatusSelect.value = '';
            currentPage = 0;
            loadOffers();
            if (window.RMS_UI) RMS_UI.showToast('Đã đặt lại tìm kiếm và bộ lọc trạng thái', 'info');
        });
    }

    // --- Export Excel / CSV ---
    if (btnExportCsv) {
        btnExportCsv.addEventListener('click', async () => {
            const query = searchInput ? searchInput.value.trim() : '';
            const status = filterStatusSelect ? filterStatusSelect.value.trim() : '';

            const oldHtml = btnExportCsv.innerHTML;
            btnExportCsv.disabled = true;
            btnExportCsv.innerHTML = `<i class="bi bi-arrow-repeat spin"></i> Đang xuất...`;

            try {
                const params = new URLSearchParams({
                    page: 0,
                    size: 1000 // Tải toàn bộ kết quả theo bộ lọc hiện tại
                });
                if (query) params.append('candidateName', query);
                if (status) params.append('status', status);

                const res = await fetch(`/api/v1/offers?${params.toString()}`);
                if (!res.ok) throw new Error('Không thể tải dữ liệu để xuất file');

                const apiRes = await res.json();
                const items = (apiRes.data && apiRes.data.content) ? apiRes.data.content : [];

                if (items.length === 0) {
                    if (window.RMS_UI) RMS_UI.showToast('Không có dữ liệu đề xuất nào phù hợp để xuất file!', 'warning');
                    return;
                }

                // Tiêu đề các cột dữ liệu
                const headers = [
                    'Mã đề xuất',
                    'Mã hồ sơ',
                    'Tên ứng viên',
                    'Vị trí đề xuất',
                    'Lương chính thức (VNĐ)',
                    'Lương thử việc (VNĐ)',
                    'Tỷ lệ thử việc',
                    'Trạng thái',
                    'Người đề xuất',
                    'Ngày tạo'
                ];

                const escapeCsv = (val) => {
                    if (val == null) return '""';
                    return `"${String(val).replace(/"/g, '""')}"`;
                };

                const csvLines = [headers.map(escapeCsv).join(',')];

                items.forEach(it => {
                    const propSal = it.proposedSalary || 0;
                    const probSal = it.probationSalary || 0;
                    const ratio = propSal > 0 ? ((probSal / propSal) * 100).toFixed(1) + '%' : '0%';
                    const statusObj = window.RMS_STATUS_MAP ? (RMS_STATUS_MAP[it.offerStatus] || RMS_STATUS_MAP[(it.offerStatus || '').toUpperCase()]) : null;
                    const statusLabel = statusObj ? statusObj.label : (it.offerStatus || '');
                    const dateStr = window.RMS_FORMATTER ? RMS_FORMATTER.dateTime(it.createdAt) : (it.createdAt || '');

                    const line = [
                        it.offerId || '',
                        it.applicationId || '',
                        it.candidateName || `Ứng viên #${it.candidateId || it.applicationId}`,
                        it.proposedPosition || it.jobTitle || '',
                        propSal,
                        probSal,
                        ratio,
                        statusLabel,
                        it.proposedByName || `Nhân sự #${it.proposedById || ''}`,
                        dateStr
                    ];
                    csvLines.push(line.map(escapeCsv).join(','));
                });

                // Sử dụng BOM \uFEFF để Excel mở trực tiếp file CSV không bị lỗi font UTF-8
                const csvContent = '\uFEFF' + csvLines.join('\r\n');
                const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
                const url = URL.createObjectURL(blob);
                const now = new Date();
                const dateStamp = now.getFullYear().toString() +
                    String(now.getMonth() + 1).padStart(2, '0') +
                    String(now.getDate()).padStart(2, '0') + '_' +
                    String(now.getHours()).padStart(2, '0') +
                    String(now.getMinutes()).padStart(2, '0');

                const a = document.createElement('a');
                a.href = url;
                a.download = `Danh_sach_Offer_${dateStamp}.csv`;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                URL.revokeObjectURL(url);

                if (window.RMS_UI) RMS_UI.showToast(`Đã xuất thành công ${items.length} bản ghi ra file CSV/Excel!`, 'success');

            } catch (err) {
                console.error(err);
                if (window.RMS_UI) RMS_UI.showToast(err.message, 'error');
            } finally {
                btnExportCsv.disabled = false;
                btnExportCsv.innerHTML = oldHtml;
            }
        });
    }

    // =============================================================================
    // REALTIME SALARY CALCULATOR & VALIDATION (CREATE MODAL)
    // =============================================================================
    function calculateSalaryRatio() {
        const proposed = getNumericValue(inputProposedSalary);
        const probation = getNumericValue(inputProbationSalary);

        if (proposed <= 0) {
            salaryRatioFill.style.width = '0%';
            salaryRatioText.textContent = 'Vui lòng nhập mức lương chính thức';
            salaryRatioText.style.color = 'var(--color-text-muted)';
            return false;
        }

        const ratio = (probation / proposed) * 100;
        const cappedWidth = Math.min(Math.max(ratio, 0), 100);
        salaryRatioFill.style.width = `${cappedWidth}%`;

        if (ratio > 100) {
            salaryRatioFill.className = 'salary-progress-fill invalid';
            salaryRatioText.textContent = `${ratio.toFixed(1)}% - Không hợp lệ (Lương thử việc phải thấp hơn hoặc bằng lương chính thức)`;
            salaryRatioText.style.color = 'var(--color-danger)';
            return false;
        } else if (ratio >= 85) {
            salaryRatioFill.className = 'salary-progress-fill valid';
            salaryRatioText.textContent = `${ratio.toFixed(1)}% - Hợp lệ (Từ 85% đến 100% lương chính thức)`;
            salaryRatioText.style.color = 'var(--color-success)';
            return true;
        } else {
            salaryRatioFill.className = 'salary-progress-fill invalid';
            salaryRatioText.textContent = `${ratio.toFixed(1)}% - Chưa đạt tối thiểu 85% lương chính thức`;
            salaryRatioText.style.color = 'var(--color-danger)';
            return false;
        }
    }

    if (inputProposedSalary) inputProposedSalary.addEventListener('input', calculateSalaryRatio);
    if (inputProbationSalary) inputProbationSalary.addEventListener('input', calculateSalaryRatio);

    if (btnSet85Percent) {
        btnSet85Percent.addEventListener('click', () => {
            const proposed = getNumericValue(inputProposedSalary);
            if (proposed > 0) {
                const val = Math.round(proposed * 0.85);
                if (typeof inputProbationSalary.setRawValue === 'function') {
                    inputProbationSalary.setRawValue(val);
                } else {
                    inputProbationSalary.value = window.RMS_FORMATTER ? RMS_FORMATTER.number(val) : val;
                    inputProbationSalary.dataset.rawValue = val;
                }
                calculateSalaryRatio();
            } else {
                if (window.RMS_UI) RMS_UI.showToast('Vui lòng nhập mức lương chính thức trước', 'error');
            }
        });
    }

    if (btnSet100Percent) {
        btnSet100Percent.addEventListener('click', () => {
            const proposed = getNumericValue(inputProposedSalary);
            if (proposed > 0) {
                if (typeof inputProbationSalary.setRawValue === 'function') {
                    inputProbationSalary.setRawValue(proposed);
                } else {
                    inputProbationSalary.value = window.RMS_FORMATTER ? RMS_FORMATTER.number(proposed) : proposed;
                    inputProbationSalary.dataset.rawValue = proposed;
                }
                calculateSalaryRatio();
            } else {
                if (window.RMS_UI) RMS_UI.showToast('Vui lòng nhập mức lương chính thức trước', 'error');
            }
        });
    }

    // Open Create Modal
    if (btnOpenCreateModal) {
        btnOpenCreateModal.addEventListener('click', () => {
            formCreateOffer.reset();
            if (typeof inputProposedSalary.setRawValue === 'function') {
                inputProposedSalary.setRawValue('');
            } else {
                inputProposedSalary.value = '';
                inputProposedSalary.dataset.rawValue = '';
            }
            if (typeof inputProbationSalary.setRawValue === 'function') {
                inputProbationSalary.setRawValue('');
            } else {
                inputProbationSalary.value = '';
                inputProbationSalary.dataset.rawValue = '';
            }
            const selectedOption = currentRoleSelect.options[currentRoleSelect.selectedIndex];
            createProposedById.value = selectedOption.getAttribute('data-user-id') || 2;
            calculateSalaryRatio();
            if (window.RMS_UI) RMS_UI.openModal(modalCreate);
            else modalCreate.classList.add('active');
        });
    }

    // Execute Create Offer (either Draft or Submit)
    async function executeCreateOffer(isDraft) {
        const proposed = getNumericValue(inputProposedSalary);
        const probation = getNumericValue(inputProbationSalary);

        if (!proposed || proposed <= 0) {
            if (window.RMS_UI) RMS_UI.showToast('Vui lòng nhập mức lương chính thức hợp lệ!', 'error');
            return;
        }

        if (probation > proposed) {
            if (window.RMS_UI) RMS_UI.showToast('Lương thử việc không được cao hơn mức lương chính thức (phải thấp hơn hoặc bằng)!', 'error');
            return;
        }

        if (probation < proposed * 0.85) {
            if (window.RMS_UI) RMS_UI.showToast('Lương thử việc phải đạt tối thiểu 85% lương chính thức theo quy định!', 'error');
            return;
        }

        const payload = {
            applicationId: parseInt(document.getElementById('createApplicationId').value),
            proposedPosition: document.getElementById('createProposedPosition').value.trim(),
            workLocation: document.getElementById('createWorkLocation').value.trim(),
            proposedSalary: proposed,
            probationSalary: probation,
            probationDays: parseInt(document.getElementById('createProbationDays').value) || 60,
            proposedById: parseInt(createProposedById.value),
            isDraft: isDraft
        };

        const targetBtn = isDraft ? btnSaveDraftOffer : btnSubmitCreateOffer;
        targetBtn.disabled = true;
        const oldHtml = targetBtn.innerHTML;
        targetBtn.innerHTML = `<i class="bi bi-arrow-repeat spin"></i> Đang xử lý...`;

        try {
            const res = await fetch('/api/v1/offers', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Tạo đề xuất thất bại');

            if (isDraft) {
                if (window.RMS_UI) RMS_UI.showToast('Đã lưu bản thảo đề xuất thành công!', 'success');
            } else {
                if (window.RMS_UI) RMS_UI.showToast('Đã tạo và gửi đề xuất tới Giám đốc phê duyệt!', 'success');
            }

            if (window.RMS_UI) RMS_UI.closeModal(modalCreate);
            else modalCreate.classList.remove('active');

            loadOfferStatuses();
            loadOffers();

        } catch (err) {
            console.error(err);
            if (window.RMS_UI) RMS_UI.showToast(err.message, 'error');
        } finally {
            targetBtn.disabled = false;
            targetBtn.innerHTML = oldHtml;
        }
    }

    if (formCreateOffer) {
        formCreateOffer.addEventListener('submit', (e) => {
            e.preventDefault();
            executeCreateOffer(false); // Lưu & Trình duyệt
        });
    }

    if (btnSaveDraftOffer) {
        btnSaveDraftOffer.addEventListener('click', () => {
            executeCreateOffer(true); // Lưu bản thảo
        });
    }

    // =============================================================================
    // EDIT MODAL (Update Offer Proposal)
    // =============================================================================
    function calculateEditSalaryRatio() {
        const proposed = getNumericValue(editProposedSalary);
        const probation = getNumericValue(editProbationSalary);

        if (proposed <= 0) {
            editSalaryRatioFill.style.width = '0%';
            editSalaryRatioText.textContent = 'Vui lòng nhập mức lương chính thức';
            editSalaryRatioText.style.color = 'var(--color-text-muted)';
            return false;
        }

        const ratio = (probation / proposed) * 100;
        const cappedWidth = Math.min(Math.max(ratio, 0), 100);
        editSalaryRatioFill.style.width = `${cappedWidth}%`;

        if (ratio > 100) {
            editSalaryRatioFill.className = 'salary-progress-fill invalid';
            editSalaryRatioText.textContent = `${ratio.toFixed(1)}% - Không hợp lệ (Lương thử việc phải thấp hơn hoặc bằng lương chính thức)`;
            editSalaryRatioText.style.color = 'var(--color-danger)';
            return false;
        } else if (ratio >= 85) {
            editSalaryRatioFill.className = 'salary-progress-fill valid';
            editSalaryRatioText.textContent = `${ratio.toFixed(1)}% - Hợp lệ (Từ 85% đến 100% lương chính thức)`;
            editSalaryRatioText.style.color = 'var(--color-success)';
            return true;
        } else {
            editSalaryRatioFill.className = 'salary-progress-fill invalid';
            editSalaryRatioText.textContent = `${ratio.toFixed(1)}% - Chưa đạt tối thiểu 85% lương chính thức`;
            editSalaryRatioText.style.color = 'var(--color-danger)';
            return false;
        }
    }

    if (editProposedSalary) editProposedSalary.addEventListener('input', calculateEditSalaryRatio);
    if (editProbationSalary) editProbationSalary.addEventListener('input', calculateEditSalaryRatio);

    if (btnEditSet85Percent) {
        btnEditSet85Percent.addEventListener('click', () => {
            const proposed = getNumericValue(editProposedSalary);
            if (proposed > 0) {
                const val = Math.round(proposed * 0.85);
                if (typeof editProbationSalary.setRawValue === 'function') {
                    editProbationSalary.setRawValue(val);
                } else {
                    editProbationSalary.value = window.RMS_FORMATTER ? RMS_FORMATTER.number(val) : val;
                    editProbationSalary.dataset.rawValue = val;
                }
                calculateEditSalaryRatio();
            }
        });
    }

    if (btnEditSet100Percent) {
        btnEditSet100Percent.addEventListener('click', () => {
            const proposed = getNumericValue(editProposedSalary);
            if (proposed > 0) {
                if (typeof editProbationSalary.setRawValue === 'function') {
                    editProbationSalary.setRawValue(proposed);
                } else {
                    editProbationSalary.value = window.RMS_FORMATTER ? RMS_FORMATTER.number(proposed) : proposed;
                    editProbationSalary.dataset.rawValue = proposed;
                }
                calculateEditSalaryRatio();
            }
        });
    }

    async function openEditModal(offerId) {
        try {
            const res = await fetch(`/api/v1/offers/${offerId}`);
            if (!res.ok) throw new Error('Không thể tải chi tiết đề xuất');

            const apiRes = await res.json();
            const offer = apiRes.data;

            editOfferId.value = offer.offerId;
            editProposedPosition.value = offer.proposedPosition || '';
            editWorkLocation.value = offer.workLocation || '';

            const propSal = offer.proposedSalary || 0;
            const probSal = offer.probationSalary || 0;
            if (typeof editProposedSalary.setRawValue === 'function') {
                editProposedSalary.setRawValue(propSal);
            } else {
                editProposedSalary.value = window.RMS_FORMATTER ? RMS_FORMATTER.number(propSal) : propSal;
                editProposedSalary.dataset.rawValue = propSal;
            }
            if (typeof editProbationSalary.setRawValue === 'function') {
                editProbationSalary.setRawValue(probSal);
            } else {
                editProbationSalary.value = window.RMS_FORMATTER ? RMS_FORMATTER.number(probSal) : probSal;
                editProbationSalary.dataset.rawValue = probSal;
            }
            editProbationDays.value = offer.probationDays || 60;

            calculateEditSalaryRatio();

            // Hiển thị thông tin yêu cầu thương lượng nếu Offer đang ở trạng thái đàm phán
            const editNegotiationInfoBox = document.getElementById('editNegotiationInfoBox');
            const editCounterSalaryText = document.getElementById('editCounterSalaryText');
            const editCounterNotesText = document.getElementById('editCounterNotesText');
            const editHrNotesText = document.getElementById('editHrNotesText');
            const btnApplyCounterSalary = document.getElementById('btnApplyCounterSalary');

            const isNeg = (offer.offerStatus || '').toUpperCase().includes('NEGOTIAT');
            const negotiations = offer.negotiationHistory || [];
            const latestNeg = negotiations.length > 0 ? negotiations[0] : null;

            if (isNeg && latestNeg && editNegotiationInfoBox) {
                editNegotiationInfoBox.style.display = 'block';
                const counterSal = latestNeg.candidateCounterSalary || 0;
                if (editCounterSalaryText) {
                    editCounterSalaryText.textContent = window.RMS_FORMATTER ? RMS_FORMATTER.currency(counterSal) : counterSal.toLocaleString('vi-VN') + ' ₫';
                }
                if (editCounterNotesText) {
                    editCounterNotesText.textContent = latestNeg.candidateNotes ? `Lý do từ ứng viên: "${latestNeg.candidateNotes}"` : '(Ứng viên không nhập lý do)';
                }
                if (editHrNotesText) {
                    editHrNotesText.textContent = latestNeg.hrResponseNotes ? `Ý kiến từ HR: "${latestNeg.hrResponseNotes}"` : 'Đang chờ HR duyệt';
                }

                if (btnApplyCounterSalary) {
                    btnApplyCounterSalary.onclick = function () {
                        if (counterSal > 0) {
                            editProposedSalary.value = window.RMS_FORMATTER ? RMS_FORMATTER.number(counterSal) : counterSal;
                            editProposedSalary.dataset.rawValue = counterSal;
                            const probation85 = Math.round(counterSal * 0.85);
                            editProbationSalary.value = window.RMS_FORMATTER ? RMS_FORMATTER.number(probation85) : probation85;
                            editProbationSalary.dataset.rawValue = probation85;
                            calculateEditSalaryRatio();
                            if (window.RMS_UI) RMS_UI.showToast('Đã áp dụng mức lương ứng viên đề xuất và tự động tính 85% lương thử việc!', 'info');
                        }
                    };
                }
            } else if (editNegotiationInfoBox) {
                editNegotiationInfoBox.style.display = 'none';
            }

            if (window.RMS_UI) {
                RMS_UI.closeModal(modalDetail);
                RMS_UI.openModal(modalEdit);
            } else {
                modalDetail.classList.remove('active');
                modalEdit.classList.add('active');
            }

        } catch (err) {
            if (window.RMS_UI) RMS_UI.showToast(err.message, 'error');
        }
    }

    if (formEditOffer) {
        formEditOffer.addEventListener('submit', async function (e) {
            e.preventDefault();
            const id = editOfferId.value;
            const proposed = getNumericValue(editProposedSalary);
            const probation = getNumericValue(editProbationSalary);

            if (probation > proposed) {
                if (window.RMS_UI) RMS_UI.showToast('Lương thử việc không được cao hơn mức lương chính thức (phải thấp hơn hoặc bằng)!', 'error');
                return;
            }

            if (probation < proposed * 0.85) {
                if (window.RMS_UI) RMS_UI.showToast('Lương thử việc phải đạt tối thiểu 85% lương chính thức theo quy định!', 'error');
                return;
            }

            const payload = {
                proposedPosition: editProposedPosition.value.trim(),
                workLocation: editWorkLocation.value.trim(),
                proposedSalary: proposed,
                probationSalary: probation,
                probationDays: parseInt(editProbationDays.value) || 60
            };

            const btnSubmit = document.getElementById('btnSubmitEditOffer');
            btnSubmit.disabled = true;
            btnSubmit.innerHTML = `<i class="bi bi-arrow-repeat spin"></i> Đang cập nhật...`;

            try {
                const res = await fetch(`/api/v1/offers/${id}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                const data = await res.json();
                if (!res.ok) throw new Error(data.message || 'Cập nhật đề xuất thất bại');

                const updatedOffer = data.data;
                const isPending = updatedOffer && ((updatedOffer.offerStatus || '').toUpperCase().includes('PENDING'));
                const successMsg = isPending
                    ? 'Đã cập nhật đề xuất thành công và tự động trình duyệt lại tới Giám đốc phê duyệt!'
                    : 'Cập nhật đề xuất thành công!';
                if (window.RMS_UI) RMS_UI.showToast(successMsg, 'success');
                if (window.RMS_UI) RMS_UI.closeModal(modalEdit);
                else modalEdit.classList.remove('active');

                loadOfferStatuses();
                loadOffers();

            } catch (err) {
                console.error(err);
                if (window.RMS_UI) RMS_UI.showToast(err.message, 'error');
            } finally {
                btnSubmit.disabled = false;
                btnSubmit.innerHTML = `<i class="bi bi-check-lg"></i> Cập Nhật Đề Xuất`;
            }
        });
    }

    // =============================================================================
    // HM ACTIONS: SUBMIT TO DIRECTOR, DELETE, CONFIRM HIRING
    // =============================================================================
    async function handleSubmitToDirector(offerId) {
        if (!confirm('Bạn có chắc chắn muốn trình duyệt đề xuất này lên Giám đốc phê duyệt?')) return;

        try {
            const res = await fetch(`/api/v1/offers/${offerId}/submit`, {
                method: 'PUT'
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Trình duyệt thất bại');

            if (window.RMS_UI) RMS_UI.showToast('Đã trình duyệt đề xuất lên Giám đốc thành công!', 'success');
            if (modalDetail.classList.contains('active')) {
                if (window.RMS_UI) RMS_UI.closeModal(modalDetail);
                else modalDetail.classList.remove('active');
            }
            loadOfferStatuses();
            loadOffers();

        } catch (err) {
            if (window.RMS_UI) RMS_UI.showToast(err.message, 'error');
        }
    }

    async function handleDeleteOffer(offerId) {
        if (!confirm(`Bạn có chắc chắn muốn xóa bản thảo #${offerId} không? Thao tác này không thể hoàn tác.`)) return;

        try {
            const res = await fetch(`/api/v1/offers/${offerId}`, {
                method: 'DELETE'
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Xóa bản thảo thất bại');

            if (window.RMS_UI) RMS_UI.showToast('Đã xóa bản thảo thành công!', 'info');
            if (modalDetail.classList.contains('active')) {
                if (window.RMS_UI) RMS_UI.closeModal(modalDetail);
                else modalDetail.classList.remove('active');
            }
            loadOfferStatuses();
            loadOffers();

        } catch (err) {
            if (window.RMS_UI) RMS_UI.showToast(err.message, 'error');
        }
    }

    async function handleConfirmHiring(offerId) {
        if (!confirm('Xác nhận tuyển dụng chính thức ứng viên này? Quá trình tuyển dụng sẽ kết thúc và hệ thống sẽ tự động gửi thông báo tới HR cùng Ứng viên.')) return;

        try {
            const res = await fetch(`/api/v1/offers/${offerId}/confirm-hiring`, {
                method: 'PUT'
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Xác nhận tuyển dụng thất bại');

            if (window.RMS_UI) RMS_UI.showToast('Xác nhận tuyển dụng thành công! Quá trình tuyển dụng đã kết thúc, đã gửi thông báo tới HR và Ứng viên.', 'success');
            if (modalDetail && modalDetail.classList.contains('active')) {
                if (window.RMS_UI) RMS_UI.closeModal(modalDetail);
                else modalDetail.classList.remove('active');
            }
            loadOfferStatuses();
            loadOffers();

        } catch (err) {
            if (window.RMS_UI) RMS_UI.showToast(err.message, 'error');
        }
    }

    async function handleSendOfferToCandidate(offerId) {
        const role = getCurrentRole();
        if (role !== 'HR' && role !== 'Admin') {
            if (window.RMS_UI) RMS_UI.showToast('Chỉ Bộ phận Nhân sự (HR) mới có quyền phát hành Offer tới ứng viên!', 'error');
            return;
        }

        if (!confirm(`Xác nhận phát hành và gửi thư mời làm việc tới ứng viên cho đề xuất #${offerId}?`)) return;

        const selectedOption = currentRoleSelect ? currentRoleSelect.options[currentRoleSelect.selectedIndex] : null;
        const hrUserId = selectedOption ? (selectedOption.getAttribute('data-user-id') || 3) : 3;

        try {
            const res = await fetch(`/api/v1/offers/${offerId}/send?hrUserId=${hrUserId}`, {
                method: 'POST'
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Phát hành Offer thất bại');

            if (window.RMS_UI) RMS_UI.showToast('Đã phát hành và gửi thư mời làm việc tới ứng viên thành công!', 'success');
            if (modalDetail && modalDetail.classList.contains('active')) {
                if (window.RMS_UI) RMS_UI.closeModal(modalDetail);
                else modalDetail.classList.remove('active');
            }
            loadOfferStatuses();
            loadOffers();

        } catch (err) {
            console.error(err);
            if (window.RMS_UI) RMS_UI.showToast(err.message, 'error');
        }
    }

    async function openHrNegotiationModal(offerId, negotiationId = null) {
        try {
            let offer = currentOfferData;
            if (!offer || offer.offerId != offerId) {
                const res = await fetch(`/api/v1/offers/${offerId}`);
                if (!res.ok) throw new Error('Không thể tải thông tin đề xuất');
                const apiRes = await res.json();
                offer = apiRes.data;
            }

            const negotiations = offer.negotiationHistory || [];
            let targetNeg = null;
            if (negotiationId) {
                targetNeg = negotiations.find(n => n.negotiationId == negotiationId);
            }
            if (!targetNeg && negotiations.length > 0) {
                targetNeg = [...negotiations].reverse().find(n => !n.hrResponseNotes) || negotiations[negotiations.length - 1];
            }

            if (!targetNeg) {
                targetNeg = {
                    negotiationId: 1,
                    candidateCounterSalary: offer.proposedSalary || 0,
                    candidateNotes: 'Ứng viên đề xuất thương lượng mức đãi ngộ'
                };
            }

            if (hrResponseOfferId) hrResponseOfferId.value = offerId;
            if (hrResponseNegotiationId) hrResponseNegotiationId.value = targetNeg.negotiationId;

            const salaryText = window.RMS_FORMATTER ? RMS_FORMATTER.currency(targetNeg.candidateCounterSalary) : (targetNeg.candidateCounterSalary || 0).toLocaleString('vi-VN') + ' ₫';
            if (hrModalCandidateCounterSalary) hrModalCandidateCounterSalary.textContent = salaryText;
            if (hrModalCandidateNotes) hrModalCandidateNotes.textContent = targetNeg.candidateNotes || '(Không có ghi chú thêm từ ứng viên)';
            if (hrResponseNotesInput) {
                hrResponseNotesInput.value = targetNeg.hrResponseNotes || '';
                setTimeout(() => hrResponseNotesInput.focus(), 150);
            }

            if (window.RMS_UI) RMS_UI.openModal(modalHrNegotiationResponse);
            else if (modalHrNegotiationResponse) modalHrNegotiationResponse.classList.add('active');

        } catch (err) {
            console.error(err);
            if (window.RMS_UI) RMS_UI.showToast(err.message, 'error');
        }
    }

    async function submitHrNegotiationDecision(action) {
        const offerId = hrResponseOfferId ? hrResponseOfferId.value : null;
        const negotiationId = hrResponseNegotiationId ? hrResponseNegotiationId.value : null;
        const responseNotes = hrResponseNotesInput ? hrResponseNotesInput.value.trim() : '';

        if (!responseNotes) {
            if (window.RMS_UI) RMS_UI.showToast('Vui lòng nhập ý kiến chỉ đạo hoặc lý do phản hồi từ HR!', 'warning');
            if (hrResponseNotesInput) hrResponseNotesInput.focus();
            return;
        }

        const isApprove = (action === 'APPROVE');
        const confirmMsg = isApprove
            ? 'Xác nhận DUYỆT yêu cầu thương lượng? Hệ thống sẽ chuyển thông tin cho Hiring Manager để sửa lại đề xuất lương mới.'
            : 'Xác nhận TỪ CHỐI yêu cầu thương lượng? Thư mời làm việc sẽ giữ nguyên mức lương ban đầu và gửi lý do từ chối tới ứng viên.';

        if (!confirm(confirmMsg)) return;

        const selectedOption = currentRoleSelect ? currentRoleSelect.options[currentRoleSelect.selectedIndex] : null;
        const hrUserId = selectedOption ? (selectedOption.getAttribute('data-user-id') || 3) : 3;

        const btnApprove = document.getElementById('btnHrApproveNegotiation');
        const btnReject = document.getElementById('btnHrRejectNegotiation');
        const targetBtn = isApprove ? btnApprove : btnReject;
        const origHtml = targetBtn ? targetBtn.innerHTML : '';

        if (btnApprove) btnApprove.disabled = true;
        if (btnReject) btnReject.disabled = true;
        if (targetBtn) {
            targetBtn.innerHTML = `<i class="bi bi-arrow-repeat spin"></i> Đang xử lý...`;
        }

        try {
            const res = await fetch(`/api/v1/offers/${offerId}/negotiations/${negotiationId}/hr-response`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    action: action,
                    hrResponseNotes: responseNotes,
                    hrUserId: parseInt(hrUserId)
                })
            });

            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Xử lý phản hồi thương lượng thất bại');

            const successMsg = isApprove
                ? 'Đã duyệt yêu cầu thương lượng thành công và chuyển thông tin cho Hiring Manager điều chỉnh lương mới!'
                : 'Đã từ chối yêu cầu thương lượng! Thư mời giữ nguyên mức lương ban đầu và đã gửi phản hồi tới ứng viên.';

            if (window.RMS_UI) RMS_UI.showToast(successMsg, isApprove ? 'success' : 'info');
            if (window.RMS_UI) RMS_UI.closeModal(modalHrNegotiationResponse);
            else if (modalHrNegotiationResponse) modalHrNegotiationResponse.classList.remove('active');

            if (modalDetail && modalDetail.classList.contains('active')) {
                openDetailModal(offerId);
            }
            loadOfferStatuses();
            loadOffers();

        } catch (err) {
            console.error(err);
            if (window.RMS_UI) RMS_UI.showToast(err.message, 'error');
        } finally {
            if (btnApprove) btnApprove.disabled = false;
            if (btnReject) btnReject.disabled = false;
            if (targetBtn) targetBtn.innerHTML = origHtml;
        }
    }

    const btnHrApproveNegotiation = document.getElementById('btnHrApproveNegotiation');
    if (btnHrApproveNegotiation) {
        btnHrApproveNegotiation.addEventListener('click', () => submitHrNegotiationDecision('APPROVE'));
    }

    const btnHrRejectNegotiation = document.getElementById('btnHrRejectNegotiation');
    if (btnHrRejectNegotiation) {
        btnHrRejectNegotiation.addEventListener('click', () => submitHrNegotiationDecision('REJECT'));
    }

    // =============================================================================
    // DETAIL MODAL & ROLE SEPARATION
    // =============================================================================
    async function openDetailModal(offerId) {
        currentOfferId = offerId;
        directorCommentsInput.value = '';

        try {
            const res = await fetch(`/api/v1/offers/${offerId}`);
            if (!res.ok) throw new Error('Không thể tải chi tiết đề xuất');

            const apiRes = await res.json();
            const offer = apiRes.data;
            currentOfferData = offer;

            // Fill header info
            const badgeContainer = document.getElementById('modalOfferBadgeContainer');
            if (badgeContainer) {
                badgeContainer.innerHTML = window.RMS_UI ? RMS_UI.createStatusBadge(offer.offerStatus) : `<span class="badge-status">${offer.offerStatus}</span>`;
            }
            detailCandidateName.textContent = offer.candidateName || `Ứng viên (Mã hồ sơ: #${offer.candidateId || offer.applicationId})`;
            detailCandidateSub.textContent = `Hồ sơ: #${offer.applicationId} | Mã đề xuất: #${offer.offerId}`;
            detailPosition.textContent = offer.proposedPosition || offer.jobTitle || 'Chưa đặt tiêu đề';
            detailLocation.innerHTML = `<i class="bi bi-geo-alt"></i> ${escapeHtml(offer.workLocation || 'Tại văn phòng')}`;

            detailProposedSalary.textContent = window.RMS_FORMATTER ? RMS_FORMATTER.currency(offer.proposedSalary) : (offer.proposedSalary || 0).toLocaleString('vi-VN') + ' ₫';
            detailProbationSalary.textContent = window.RMS_FORMATTER ? RMS_FORMATTER.currency(offer.probationSalary) : (offer.probationSalary || 0).toLocaleString('vi-VN') + ' ₫';
            detailProbationDays.textContent = `${offer.probationDays || 60} ngày`;

            const role = getCurrentRole();
            const statusUpper = (offer.offerStatus || '').toUpperCase();

            // 1. HM & HR ACTION SECTION
            if (role === 'HM' || role === 'HR' || role === 'Admin') {
                hmActionSection.style.display = 'block';
                let hmButtons = '';

                if (statusUpper === 'DRAFT') {
                    if (role === 'HR') {
                        hmActionNotice.innerHTML = `Đề xuất đang ở trạng thái <strong>Bản thảo</strong> (do Quản lý tuyển dụng phụ trách khởi tạo).`;
                        hmButtons = ``;
                    } else {
                        hmActionNotice.innerHTML = `Đề xuất đang ở trạng thái <strong>Bản thảo</strong>. Bạn có thể kiểm tra lại, chỉnh sửa hoặc trình duyệt lên Giám đốc để phê duyệt ngân sách.`;
                        hmButtons = `
                            <button type="button" class="btn btn-primary" onclick="window.triggerSubmitToDirector(${offer.offerId})">
                                <i class="bi bi-send-check"></i> Trình duyệt Giám đốc
                            </button>
                            <button type="button" class="btn btn-warning" onclick="window.triggerOpenEditModal(${offer.offerId})">
                                <i class="bi bi-pencil-square"></i> Chỉnh sửa đề xuất
                            </button>
                            <button type="button" class="btn btn-outline-danger" onclick="window.triggerDeleteOffer(${offer.offerId})">
                                <i class="bi bi-trash"></i> Xóa bản thảo
                            </button>
                        `;
                    }
                } else if (statusUpper === 'REJECTED') {
                    if (role === 'HR') {
                        hmActionNotice.innerHTML = `<span class="text-danger"><i class="bi bi-exclamation-triangle"></i> Đề xuất này đã bị Giám đốc yêu cầu điều chỉnh. Quản lý tuyển dụng sẽ cập nhật lại.</span>`;
                        hmButtons = ``;
                    } else {
                        hmActionNotice.innerHTML = `<span class="text-danger"><i class="bi bi-exclamation-triangle"></i> Đề xuất này đã bị Giám đốc yêu cầu điều chỉnh. Hãy cập nhật lại mức đãi ngộ trước khi gửi duyệt lại.</span>`;
                        hmButtons = `
                            <button type="button" class="btn btn-warning" onclick="window.triggerOpenEditModal(${offer.offerId})">
                                <i class="bi bi-arrow-repeat"></i> Chỉnh sửa lại đề xuất
                            </button>
                        `;
                    }
                } else if (statusUpper === 'APPROVED') {
                    if (role === 'HR' || role === 'Admin') {
                        hmActionNotice.innerHTML = `<span class="text-success"><i class="bi bi-check-circle-fill"></i> Đề xuất đã được Giám đốc phê duyệt. HR hãy kiểm tra thông tin và bấm <strong>"Phát hành Offer"</strong> để chính thức gửi thư mời tới ứng viên.</span>`;
                        hmButtons = `
                            <button type="button" class="btn btn-success" style="font-weight: 600;" onclick="window.triggerSendOfferToCandidate(${offer.offerId})">
                                <i class="bi bi-send-check-fill"></i> Phát hành Offer
                            </button>
                        `;
                    } else {
                        // Hiring Manager KHÔNG có quyền phát hành offer tới ứng viên
                        hmActionNotice.innerHTML = `<span class="text-success"><i class="bi bi-check-circle-fill"></i> Đề xuất đã được Giám đốc phê duyệt! Đang chờ <strong>Bộ phận Nhân sự (HR)</strong> kiểm tra và chính thức phát hành thư mời làm việc tới ứng viên.</span>`;
                        hmButtons = ``;
                    }
                } else if (statusUpper === 'NEGOTIATING' || statusUpper === 'NEGOTIATE') {
                    const negotiations = offer.negotiationHistory || [];
                    const latestNeg = negotiations.length > 0 ? negotiations[0] : null;
                    const isApprovedByHr = latestNeg && latestNeg.hrResponseNotes && !latestNeg.hrResponseNotes.includes('[Từ chối');
                    const counterSalaryFormatted = latestNeg && latestNeg.candidateCounterSalary
                        ? (window.RMS_FORMATTER ? RMS_FORMATTER.currency(latestNeg.candidateCounterSalary) : (latestNeg.candidateCounterSalary).toLocaleString('vi-VN') + ' ₫')
                        : 'theo yêu cầu của ứng viên';

                    if (role === 'HR') {
                        if (!isApprovedByHr) {
                            hmActionNotice.innerHTML = `
                                <div style="color: #7c3aed; font-weight: 700; margin-bottom: 0.35rem;">
                                    <i class="bi bi-chat-quote-fill"></i> Ứng viên đang đề xuất thương lượng mức đãi ngộ:
                                </div>
                                <div style="font-size: 0.9rem; margin-bottom: 0.25rem;">
                                    Mức lương mong muốn: <strong style="color: #7c3aed; font-size: 1.05rem;">${counterSalaryFormatted}</strong>
                                </div>
                                <div style="font-size: 0.85rem; color: var(--color-text-secondary);">
                                    Bộ phận Nhân sự (HR) cần xét duyệt yêu cầu: <strong>Duyệt</strong> để chuyển về cho Hiring Manager điều chỉnh mức lương mới, hoặc <strong>Từ chối</strong> để giữ nguyên mức lương ban đầu.
                                </div>
                            `;
                            hmButtons = `
                                <button type="button" class="btn btn-primary" style="background: #7c3aed; border-color: #7c3aed; font-weight: 600;" onclick="window.triggerRespondToNegotiation(${offer.offerId})">
                                    <i class="bi bi-shield-check"></i> Xét duyệt Thương lượng
                                </button>
                            `;
                        } else {
                            hmActionNotice.innerHTML = `
                                <div style="color: #15803d; font-weight: 700; margin-bottom: 0.35rem;">
                                    <i class="bi bi-check-circle-fill"></i> HR đã DUYỆT yêu cầu thương lượng!
                                </div>
                                <div style="font-size: 0.9rem; margin-bottom: 0.25rem;">
                                    Mức lương đề xuất mới: <strong style="color: #7c3aed;">${counterSalaryFormatted}</strong>
                                </div>
                                <div style="font-size: 0.85rem; color: var(--color-text-secondary);">
                                    Hệ thống đã gửi thông báo cho Quản lý tuyển dụng (Hiring Manager) để sửa lại đề xuất lương mới và gửi Giám đốc phê duyệt lần cuối.
                                </div>
                            `;
                            hmButtons = ``;
                        }
                    } else {
                        // Role HM hoặc Admin
                        if (isApprovedByHr) {
                            hmActionNotice.innerHTML = `
                                <div style="color: #15803d; font-weight: 700; margin-bottom: 0.35rem;">
                                    <i class="bi bi-check-circle-fill"></i> HR đã DUYỆT yêu cầu thương lượng của ứng viên!
                                </div>
                                <div style="font-size: 0.9rem; margin-bottom: 0.25rem;">
                                    Mức lương ứng viên đề xuất: <strong style="color: #7c3aed; font-size: 1.05rem;">${counterSalaryFormatted}</strong>
                                </div>
                                <div style="font-size: 0.85rem; color: #475569; margin-bottom: 0.35rem;">
                                    Ghi chú từ HR: <em>"${escapeHtml(latestNeg?.hrResponseNotes || '')}"</em>
                                </div>
                                <div style="font-size: 0.85rem; color: #d97706; font-weight: 600;">
                                    Đề nghị Quản lý tuyển dụng bấm "Cập nhật đề xuất Offer" bên dưới để sửa lại lương theo yêu cầu mới và trình Giám đốc phê duyệt lần cuối.
                                </div>
                            `;
                            hmButtons = `
                                <button type="button" class="btn btn-warning" style="font-weight: 700;" onclick="window.triggerOpenEditModal(${offer.offerId})">
                                    <i class="bi bi-pencil-square"></i> Cập nhật đề xuất Offer
                                </button>
                            `;
                        } else {
                            hmActionNotice.innerHTML = `
                                <div style="color: #d97706; font-weight: 700; margin-bottom: 0.35rem;">
                                    <i class="bi bi-hourglass-split"></i> Ứng viên đang đề xuất thương lượng mức đãi ngộ:
                                </div>
                                <div style="font-size: 0.9rem; margin-bottom: 0.25rem;">
                                    Mức lương đề xuất lại: <strong style="color: #7c3aed; font-size: 1.05rem;">${counterSalaryFormatted}</strong>
                                </div>
                                <div style="font-size: 0.85rem; color: var(--color-text-secondary);">
                                    Đang chờ Bộ phận Nhân sự (HR) tiếp nhận và xét duyệt trước khi bạn có thể điều chỉnh lại đề xuất lương.
                                </div>
                            `;
                            hmButtons = ``;
                        }
                    }
                } else if (statusUpper === 'ACCEPTED') {
                    hmActionNotice.innerHTML = `<span class="text-success"><i class="bi bi-check-circle"></i> Ứng viên đã đồng ý nhận việc! Hãy bấm xác nhận tuyển dụng để kết thúc quá trình tuyển dụng của ứng viên và gửi thông báo tới HR cùng Ứng viên.</span>`;
                    hmButtons = `
                        <button type="button" class="btn btn-success" style="font-weight: 700; padding: 0.5rem 1.25rem;" onclick="window.triggerConfirmHiring(${offer.offerId})">
                            <i class="bi bi-person-check-fill"></i> Xác nhận Tuyển dụng chính thức
                        </button>
                    `;
                } else if (statusUpper === 'HIRED') {
                    hmActionNotice.innerHTML = `<span class="text-success" style="font-weight: 600;"><i class="bi bi-award-fill"></i> Ứng viên đã được tuyển dụng thành công! Quá trình tuyển dụng của ứng viên này đã hoàn tất và hệ thống đã gửi thông báo tới HR cùng Ứng viên.</span>`;
                    hmButtons = ``;
                } else {
                    const statusObj = window.RMS_STATUS_MAP ? (RMS_STATUS_MAP[statusUpper] || { label: statusUpper }) : { label: statusUpper };
                    hmActionNotice.innerHTML = `Đề xuất đang ở trạng thái: <strong>${statusObj.label}</strong>. Không có thao tác cần xử lý lúc này.`;
                    hmButtons = ``;
                }
                hmActionButtonsContainer.innerHTML = hmButtons;
            } else {
                hmActionSection.style.display = 'none';
            }

            // 2. DIRECTOR APPROVAL SECTION
            if (role === 'Director' && statusUpper.includes('PENDING')) {
                directorApprovalSection.style.display = 'block';
            } else {
                directorApprovalSection.style.display = 'none';
            }

            // 3. CANDIDATE LINK SECTION
            const isApproved = ['APPROVED', 'ACCEPTED', 'NEGOTIATING', 'SENT_TO_CANDIDATE', 'SENT_CANDIDATE', 'SENT'].includes(statusUpper);
            if (isApproved) {
                candidateLinkSection.style.display = 'block';
                btnOpenCandidateView.href = `/offers/candidate/${offer.offerId}`;
            } else {
                candidateLinkSection.style.display = 'none';
            }

            // Render Approval History
            const approvals = offer.approvalHistory || [];
            if (approvals.length === 0) {
                approvalHistoryContainer.innerHTML = `<div style="color: var(--color-text-muted); font-size: 0.85rem; padding-left: 0.5rem;">Chưa có bản ghi phê duyệt nào.</div>`;
            } else {
                approvalHistoryContainer.innerHTML = approvals.map(a => {
                    const isApp = (a.status || '').toUpperCase() === 'APPROVED';
                    const markerClass = isApp ? 'success' : 'danger';
                    const badge = window.RMS_UI ? (isApp ? RMS_UI.createStatusBadge('APPROVED') : RMS_UI.createStatusBadge('REJECTED')) : (isApp ? '<span class="badge badge-success">Đã duyệt</span>' : '<span class="badge badge-danger">Từ chối</span>');
                    const approvalDate = window.RMS_FORMATTER ? RMS_FORMATTER.dateTime(a.approvedAt) : a.approvedAt;

                    return `
                        <div class="timeline-item">
                            <div class="timeline-marker ${markerClass}"></div>
                            <div class="timeline-content">
                                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.25rem;">
                                    <strong>${escapeHtml(a.directorName || ('Giám đốc #' + a.directorId))}</strong>
                                    ${badge}
                                </div>
                                <div class="tabular-nums" style="color: var(--color-text-secondary); font-size: 0.8rem; margin-bottom: 0.35rem;">
                                    ${approvalDate}
                                </div>
                                <div style="color: var(--color-text-primary);">
                                    ${a.directorComments ? `<em>"${escapeHtml(a.directorComments)}"</em>` : '<span style="color: var(--color-text-muted);">Không có nhận xét thêm</span>'}
                                </div>
                            </div>
                        </div>
                    `;
                }).join('');
            }

            // Render Negotiation History
            const negotiations = offer.negotiationHistory || [];
            if (negotiations.length === 0) {
                negotiationHistoryContainer.innerHTML = `<div style="color: var(--color-text-muted); font-size: 0.85rem; padding-left: 0.5rem;">Chưa có yêu cầu thương lượng nào từ ứng viên.</div>`;
            } else {
                negotiationHistoryContainer.innerHTML = negotiations.map(n => {
                    const counterSal = window.RMS_FORMATTER ? RMS_FORMATTER.currency(n.candidateCounterSalary) : (n.candidateCounterSalary || 0).toLocaleString('vi-VN') + ' ₫';
                    const negDate = window.RMS_FORMATTER ? RMS_FORMATTER.dateTime(n.negotiationDate) : n.negotiationDate;
                    const canRespond = (role === 'HR' || role === 'HM' || role === 'Admin') && !n.hrResponseNotes;

                    return `
                        <div class="timeline-item">
                            <div class="timeline-marker warning"></div>
                            <div class="timeline-content">
                                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.25rem;">
                                    <strong>Ứng viên đề xuất mức lương:</strong>
                                    <span class="tabular-nums" style="font-weight: 700; color: #7c3aed;">${counterSal}</span>
                                </div>
                                <div class="tabular-nums" style="color: var(--color-text-secondary); font-size: 0.8rem; margin-bottom: 0.35rem;">
                                    ${negDate}
                                </div>
                                <div style="color: var(--color-text-primary); font-style: italic;">
                                    "${escapeHtml(n.candidateNotes || 'Không có ghi chú thêm')}"
                                </div>
                                ${n.hrResponseNotes ? `
                                    <div style="margin-top: 0.6rem; padding: 0.5rem 0.75rem; background: #f0fdf4; border-left: 3px solid #16a34a; border-radius: 4px;">
                                        <div style="font-weight: 600; font-size: 0.8rem; color: #166534;"><i class="bi bi-reply-fill"></i> HR đã phản hồi:</div>
                                        <div style="font-size: 0.85rem; color: #14532d; margin-top: 0.2rem;">${escapeHtml(n.hrResponseNotes)}</div>
                                    </div>
                                ` : ''}
                                ${canRespond ? `
                                    <div style="margin-top: 0.6rem;">
                                        <button type="button" class="btn btn-sm btn-outline-primary" onclick="window.triggerRespondToNegotiation(${offer.offerId}, ${n.negotiationId})">
                                            <i class="bi bi-reply"></i> Phản hồi thương lượng
                                        </button>
                                    </div>
                                ` : ''}
                            </div>
                        </div>
                    `;
                }).join('');
            }

            if (window.RMS_UI) RMS_UI.openModal(modalDetail);
            else modalDetail.classList.add('active');

        } catch (err) {
            console.error(err);
            if (window.RMS_UI) RMS_UI.showToast(err.message, 'error');
        }
    }

    // Expose handlers to window for inline onclicks in modal
    window.triggerSubmitToDirector = (id) => handleSubmitToDirector(id);
    window.triggerOpenEditModal = (id) => openEditModal(id);
    window.triggerDeleteOffer = (id) => handleDeleteOffer(id);
    window.triggerConfirmHiring = (id) => handleConfirmHiring(id);
    window.triggerSendOfferToCandidate = (id) => handleSendOfferToCandidate(id);
    window.triggerRespondToNegotiation = (offerId, negId) => openHrNegotiationModal(offerId, negId);
    window.openHrNegotiationModal = openHrNegotiationModal;

    // Process Director Approval / Rejection
    async function handleDirectorDecision(decision) {
        if (!currentOfferId) return;

        const directorId = 1;
        const comments = directorCommentsInput.value.trim();

        if (decision === 'Rejected' && !comments) {
            if (window.RMS_UI) RMS_UI.showToast('Vui lòng ghi rõ lý do nhận xét khi từ chối đề xuất!', 'error');
            directorCommentsInput.focus();
            return;
        }

        const payload = {
            directorId: directorId,
            status: decision,
            directorComments: comments || (decision === 'Approved' ? 'Đã phê duyệt đề xuất đãi ngộ.' : 'Từ chối đề xuất.')
        };

        try {
            const res = await fetch(`/api/v1/offers/${currentOfferId}/approval`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Xử lý phê duyệt thất bại');

            if (window.RMS_UI) RMS_UI.showToast(`Đã ${decision === 'Approved' ? 'phê duyệt' : 'từ chối'} đề xuất thành công!`, 'success');
            if (window.RMS_UI) RMS_UI.closeModal(modalDetail);
            else modalDetail.classList.remove('active');

            loadOffers();

        } catch (err) {
            console.error(err);
            if (window.RMS_UI) RMS_UI.showToast(err.message, 'error');
        }
    }

    if (btnApproveOffer) btnApproveOffer.addEventListener('click', () => handleDirectorDecision('Approved'));
    if (btnRejectOffer) btnRejectOffer.addEventListener('click', () => handleDirectorDecision('Rejected'));

    // Initial Load
    applyRoleUI(false);
    loadOfferStatuses();
    loadOffers();
});
