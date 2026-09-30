/**
 * =============================================================================
 * candidate-offer.js - Logic REST API cho Cổng Ứng Viên (Màn hình 40, 41, 42)
 * SWP_RMS_Group-2 | Recruitment Management System
 * =============================================================================
 */

document.addEventListener('DOMContentLoaded', function () {
    // --- Extract Offer ID ---
    function resolveOfferId() {
        // 1. From Thymeleaf model attribute
        const hiddenSpan = document.getElementById('pageOfferId');
        if (hiddenSpan && hiddenSpan.textContent && !isNaN(parseInt(hiddenSpan.textContent))) {
            return parseInt(hiddenSpan.textContent);
        }

        // 2. From URL Query Param (?offerId=123)
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.has('offerId')) {
            return parseInt(urlParams.get('offerId'));
        }

        // 3. From Path (/offers/candidate/123)
        const parts = window.location.pathname.split('/');
        const lastPart = parts[parts.length - 1];
        if (!isNaN(parseInt(lastPart))) {
            return parseInt(lastPart);
        }

        // Default demo fallback ID
        return 1;
    }

    const offerId = resolveOfferId();

    // --- DOM Elements ---
    const badgeHeader = document.getElementById('offerStatusHeaderBadge');
    const statusAlertBanner = document.getElementById('statusAlertBanner');
    const statusAlertMessage = document.getElementById('statusAlertMessage');

    const letterRefCode = document.getElementById('letterRefCode');
    const letterIssueDate = document.getElementById('letterIssueDate');
    const candidateNameTitle = document.getElementById('candidateNameTitle');

    const termPosition = document.getElementById('termPosition');
    const termJobTitle = document.getElementById('termJobTitle');
    const termLocation = document.getElementById('termLocation');
    const termProposedSalary = document.getElementById('termProposedSalary');
    const termProbationSalary = document.getElementById('termProbationSalary');
    const termProbationDays = document.getElementById('termProbationDays');

    const offerActionBar = document.getElementById('offerActionBar');
    const candidateNegotiationBox = document.getElementById('candidateNegotiationBox');
    const candidateNegotiationList = document.getElementById('candidateNegotiationList');

    // HR Response Callout
    const hrResponseAlertBox = document.getElementById('hrResponseAlertBox');
    const hrResponseText = document.getElementById('hrResponseText');
    const hrResponseDateBadge = document.getElementById('hrResponseDateBadge');

    // Modals
    const modalAccept = document.getElementById('modalAccept');
    const modalNegotiate = document.getElementById('modalNegotiate');
    const modalDecline = document.getElementById('modalDecline');

    const btnOpenAcceptModal = document.getElementById('btnOpenAcceptModal');
    const btnOpenNegotiateModal = document.getElementById('btnOpenNegotiateModal');
    const btnOpenDeclineModal = document.getElementById('btnOpenDeclineModal');

    const btnConfirmAccept = document.getElementById('btnConfirmAccept');
    const formNegotiate = document.getElementById('formNegotiate');
    const negotiateCounterSalary = document.getElementById('negotiateCounterSalary');
    const negotiateCandidateNotes = document.getElementById('negotiateCandidateNotes');
    const btnConfirmDecline = document.getElementById('btnConfirmDecline');
    const declineReasonInput = document.getElementById('declineReasonInput');

    // Kích hoạt bộ định dạng tiền tệ chống nhảy số cho ô thương lượng lương
    if (window.RMS_UI && window.RMS_UI.attachMoneyInput && negotiateCounterSalary) {
        RMS_UI.attachMoneyInput(negotiateCounterSalary);
    }

    // Close buttons for modals
    document.querySelectorAll('.btn-close-modal').forEach(btn => {
        btn.addEventListener('click', () => {
            modalAccept.classList.remove('active');
            modalNegotiate.classList.remove('active');
            modalDecline.classList.remove('active');
        });
    });

    [modalAccept, modalNegotiate, modalDecline].forEach(modal => {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) modal.classList.remove('active');
        });
    });

    // --- Format Currency (VNĐ) ---
    function formatCurrency(amount) {
        if (amount == null) return '0 ₫';
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
    }

    // --- Format Date Time ---
    function formatDateTime(dateStr) {
        if (!dateStr) return '--/--/----';
        const d = new Date(dateStr);
        return d.toLocaleDateString('vi-VN', {
            day: '2-digit', month: '2-digit', year: 'numeric'
        });
    }

    // --- Toast Notification ---
    function showToast(message, type = 'success') {
        const container = document.getElementById('toastContainer');
        const toast = document.createElement('div');
        toast.className = `toast-msg toast-${type}`;
        const icon = type === 'success' ? 'bi-check-circle-fill' : (type === 'error' ? 'bi-exclamation-triangle-fill' : 'bi-info-circle-fill');
        toast.innerHTML = `<i class="bi ${icon}"></i> <span>${message}</span>`;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transition = 'opacity 0.4s ease';
            setTimeout(() => toast.remove(), 400);
        }, 4500);
    }

    // --- Load Offer Letter Data ---
    async function loadOfferLetter() {
        try {
            const res = await fetch(`/api/v1/offers/${offerId}`);
            if (!res.ok) {
                throw new Error(`Không tìm thấy thư mời làm việc với ID #${offerId}`);
            }

            const apiRes = await res.json();
            const offer = apiRes.data;

            renderOfferLetter(offer);

        } catch (err) {
            console.error(err);
            badgeHeader.className = 'badge-status status-rejected';
            badgeHeader.textContent = 'Không tìm thấy';

            statusAlertBanner.style.display = 'flex';
            statusAlertBanner.style.background = '#fef2f2';
            statusAlertBanner.style.border = '1px solid #fecaca';
            statusAlertBanner.style.color = '#991b1b';
            statusAlertMessage.innerHTML = `<i class="bi bi-exclamation-diamond-fill"></i> <span>${err.message}</span>`;

            offerActionBar.style.display = 'none';
        }
    }

    // --- Render Details & Action State ---
    function renderOfferLetter(offer) {
        letterRefCode.textContent = `#OFFER-${offer.offerId}`;
        letterIssueDate.textContent = formatDateTime(offer.createdAt);
        candidateNameTitle.textContent = offer.candidateName || `Ứng viên (Hồ sơ #${offer.applicationId})`;

        termPosition.textContent = offer.proposedPosition || offer.jobTitle || 'Chuyên viên phần mềm';
        termJobTitle.textContent = offer.jobTitle || 'Tin tuyển dụng công nghệ';
        termLocation.textContent = offer.workLocation || 'Văn phòng RMS';

        const proposed = offer.proposedSalary || 0;
        const probation = offer.probationSalary || 0;
        const ratio = proposed > 0 ? ((probation / proposed) * 100).toFixed(0) : 0;

        termProposedSalary.textContent = formatCurrency(proposed);
        termProbationSalary.textContent = `${formatCurrency(probation)} (${ratio}% lương chính thức)`;
        termProbationDays.textContent = `${offer.probationDays || 60} ngày`;

        // Update Status & Action controls based on status
        const status = (offer.offerStatus || '').toUpperCase();

        if (status === 'ACCEPTED') {
            badgeHeader.className = 'badge-status status-accepted';
            badgeHeader.textContent = 'Đã chấp nhận';

            statusAlertBanner.style.display = 'flex';
            statusAlertBanner.style.background = '#ecfdf5';
            statusAlertBanner.style.border = '1px solid #a7f3d0';
            statusAlertBanner.style.color = '#065f46';
            statusAlertMessage.innerHTML = `
                <i class="bi bi-check-circle-fill text-success" style="font-size: 1.5rem;"></i>
                <div>
                    <strong>Bạn đã đồng ý nhận lời mời làm việc!</strong>
                    <div style="font-size: 0.85rem; margin-top: 0.2rem;">Hồ sơ của bạn đang chờ Quản lý tuyển dụng (Hiring Manager) xác nhận bước Tuyển dụng chính thức. Bạn sẽ nhận được thông báo ngay khi quy trình hoàn tất.</div>
                </div>
            `;
            offerActionBar.style.display = 'none';

        } else if (status === 'HIRED') {
            badgeHeader.className = 'badge-status status-accepted';
            badgeHeader.textContent = 'Đã tuyển dụng';

            statusAlertBanner.style.display = 'flex';
            statusAlertBanner.style.background = '#ecfdf5';
            statusAlertBanner.style.border = '1px solid #a7f3d0';
            statusAlertBanner.style.color = '#065f46';
            statusAlertMessage.innerHTML = `
                <i class="bi bi-trophy-fill text-success" style="font-size: 1.5rem;"></i>
                <div>
                    <strong>Chúc mừng bạn đã chính thức được tuyển dụng vào công ty!</strong>
                    <div style="font-size: 0.85rem; margin-top: 0.2rem;">Quá trình tuyển dụng của bạn đã hoàn tất thành công với kết quả <strong>Đã tuyển dụng (Hired)</strong>. Bộ phận Nhân sự (HR) đã nhận thông báo và sẽ sớm liên hệ trực tiếp với bạn để hoàn tất thủ tục Onboarding.</div>
                </div>
            `;
            offerActionBar.style.display = 'none';

        } else if (status === 'DECLINED') {
            badgeHeader.className = 'badge-status status-declined';
            badgeHeader.textContent = offer.offerStatus;

            statusAlertBanner.style.display = 'flex';
            statusAlertBanner.style.background = '#f8fafc';
            statusAlertBanner.style.border = '1px solid #cbd5e1';
            statusAlertBanner.style.color = '#475569';
            statusAlertMessage.innerHTML = `
                <i class="bi bi-x-circle-fill" style="font-size: 1.3rem;"></i>
                <span>Bạn đã từ chối thư mời nhận việc này. Cảm ơn bạn đã dành thời gian quan tâm tới RMS.</span>
            `;
            offerActionBar.style.display = 'none';

        } else if (status === 'NEGOTIATING') {
            badgeHeader.className = 'badge-status status-negotiating';
            badgeHeader.textContent = offer.offerStatus;

            statusAlertBanner.style.display = 'flex';
            statusAlertBanner.style.background = '#faf5ff';
            statusAlertBanner.style.border = '1px solid #e9d5ff';
            statusAlertBanner.style.color = '#6b21a8';
            statusAlertMessage.innerHTML = `
                <i class="bi bi-chat-left-dots-fill" style="font-size: 1.3rem;"></i>
                <div>
                    <strong>Đề xuất thương lượng của bạn đã được gửi tới Ban Tuyển Dụng!</strong>
                    <div style="font-size: 0.85rem;">Phòng Nhân sự và Hiring Manager đang xem xét lại các đề xuất của bạn.</div>
                </div>
            `;
            // Khi đang đàm phán, tạm ẩn thanh thao tác vì gói Offer chưa được Giám đốc duyệt lại
            offerActionBar.style.display = 'none';

        } else if (status.includes('SENT')) {
            // ĐÃ PHÁT HÀNH BỞI HR: ĐÂY LÀ TRƯỜNG HỢP DUY NHẤT ỨNG VIÊN ĐƯỢC XEM & PHẢN HỒI THƯ MỜI
            badgeHeader.className = 'badge-status status-sent';
            badgeHeader.textContent = 'Đã phát hành';

            statusAlertBanner.style.display = 'flex';
            statusAlertBanner.style.background = '#eff6ff';
            statusAlertBanner.style.border = '1px solid #bfdbfe';
            statusAlertBanner.style.color = '#1e40af';
            statusAlertMessage.innerHTML = `
                <i class="bi bi-clock-fill" style="font-size: 1.3rem;"></i>
                <div>
                    <strong>Thư mời nhận việc chính thức đang chờ phản hồi từ bạn!</strong>
                    <div style="font-size: 0.85rem; margin-top: 0.2rem;">Thư mời đã được Bộ phận Nhân sự phát hành chính thức. Vui lòng xem xét các điều khoản và lựa chọn phản hồi ở cuối trang.</div>
                </div>
            `;
            offerActionBar.style.display = 'flex';

        } else if (status === 'APPROVED') {
            // ĐÃ ĐƯỢC GIÁM ĐỐC DUYỆT NHƯNG HR CHƯA PHÁT HÀNH
            badgeHeader.className = 'badge-status status-approved';
            badgeHeader.textContent = 'Chờ HR phát hành';

            statusAlertBanner.style.display = 'flex';
            statusAlertBanner.style.background = '#f0fdf4';
            statusAlertBanner.style.border = '1px solid #bbf7d0';
            statusAlertBanner.style.color = '#166534';
            statusAlertMessage.innerHTML = `
                <i class="bi bi-hourglass-split" style="font-size: 1.3rem;"></i>
                <div>
                    <strong>Đề xuất tuyển dụng đã được Giám đốc phê duyệt nội bộ!</strong>
                    <div style="font-size: 0.85rem; margin-top: 0.2rem;">Hệ thống đang chuyển thông tin tới Bộ phận Nhân sự (HR). Thư mời làm việc sẽ chính thức gửi qua email tới bạn sau khi HR xác nhận phát hành.</div>
                </div>
            `;
            offerActionBar.style.display = 'none';

        } else if (status.includes('PENDING')) {
            badgeHeader.className = 'badge-status status-pending';
            badgeHeader.textContent = 'Chờ duyệt nội bộ';

            statusAlertBanner.style.display = 'flex';
            statusAlertBanner.style.background = '#fffbeb';
            statusAlertBanner.style.border = '1px solid #fde68a';
            statusAlertBanner.style.color = '#92400e';
            statusAlertMessage.innerHTML = `
                <i class="bi bi-hourglass-split" style="font-size: 1.3rem;"></i>
                <div>
                    <strong>Thư mời đang trong quá trình phê duyệt nội bộ!</strong>
                    <div style="font-size: 0.85rem; margin-top: 0.2rem;">Đề xuất đang chờ Ban Giám Đốc xem xét phê duyệt trước khi chuyển cho HR phát hành.</div>
                </div>
            `;
            offerActionBar.style.display = 'none';

        } else if (status === 'REJECTED') {
            badgeHeader.className = 'badge-status status-rejected';
            badgeHeader.textContent = 'Chưa thông qua';

            statusAlertBanner.style.display = 'flex';
            statusAlertBanner.style.background = '#fef2f2';
            statusAlertBanner.style.border = '1px solid #fecaca';
            statusAlertBanner.style.color = '#991b1b';
            statusAlertMessage.innerHTML = `
                <i class="bi bi-x-circle-fill" style="font-size: 1.3rem;"></i>
                <div>
                    <strong>Thư mời làm việc đang được điều chỉnh nội bộ!</strong>
                    <div style="font-size: 0.85rem; margin-top: 0.2rem;">Đề xuất chưa được thông qua và đang được Quản lý tuyển dụng cập nhật lại.</div>
                </div>
            `;
            offerActionBar.style.display = 'none';

        } else {
            // DRAFT hoặc các trạng thái khởi tạo khác
            badgeHeader.className = 'badge-status status-pending';
            badgeHeader.textContent = 'Bản nháp';

            statusAlertBanner.style.display = 'flex';
            statusAlertBanner.style.background = '#f8fafc';
            statusAlertBanner.style.border = '1px solid #e2e8f0';
            statusAlertBanner.style.color = '#475569';
            statusAlertMessage.innerHTML = `
                <i class="bi bi-file-earmark-lock-fill" style="font-size: 1.3rem;"></i>
                <div>
                    <strong>Thư mời làm việc đang ở dạng bản thảo nội bộ!</strong>
                    <div style="font-size: 0.85rem; margin-top: 0.2rem;">Thư mời chưa được kích hoạt phát hành chính thức tới ứng viên.</div>
                </div>
            `;
            offerActionBar.style.display = 'none';
        }

        // Render HR Response Alert Box if any negotiation has official HR response
        const negotiations = offer.negotiationHistory || [];
        const latestWithHr = negotiations.find(n => n.hrResponseNotes && n.hrResponseNotes.trim() !== '');

        if (latestWithHr && hrResponseAlertBox) {
            hrResponseAlertBox.style.display = 'block';
            if (hrResponseText) hrResponseText.textContent = latestWithHr.hrResponseNotes;
            if (hrResponseDateBadge) hrResponseDateBadge.textContent = formatDateTime(latestWithHr.negotiationDate);

            const isReject = (latestWithHr.hrResponseNotes || '').includes('[Từ chối') || (latestWithHr.hrResponseNotes || '').toLowerCase().includes('từ chối');
            if (isReject) {
                hrResponseAlertBox.style.background = '#fffbeb';
                hrResponseAlertBox.style.borderColor = '#fcd34d';
                const headerStrong = hrResponseAlertBox.querySelector('strong');
                if (headerStrong) {
                    headerStrong.style.color = '#b45309';
                    headerStrong.innerHTML = `<i class="bi bi-info-circle-fill me-1"></i> Phản hồi từ Phòng Nhân sự (HR): Từ chối thương lượng`;
                }
            } else {
                hrResponseAlertBox.style.background = '#f0fdf4';
                hrResponseAlertBox.style.borderColor = '#86efac';
                const headerStrong = hrResponseAlertBox.querySelector('strong');
                if (headerStrong) {
                    headerStrong.style.color = '#166534';
                    headerStrong.innerHTML = `<i class="bi bi-patch-check-fill me-1"></i> Phản hồi chính thức từ Phòng Nhân sự (HR):`;
                }
            }
        } else if (hrResponseAlertBox) {
            hrResponseAlertBox.style.display = 'none';
        }

        // Render Candidate Negotiation History if any
        if (negotiations.length > 0) {
            candidateNegotiationBox.style.display = 'block';
            candidateNegotiationList.innerHTML = negotiations.map(n => `
                <div style="padding: 0.85rem 0; border-bottom: 1px solid #f3e8ff;">
                    <div style="display: flex; justify-content: space-between; font-size: 0.9rem; flex-wrap: wrap; gap: 0.5rem;">
                        <strong>Mức lương bạn đã đề xuất: <span style="color: #7c3aed;">${formatCurrency(n.candidateCounterSalary)}</span></strong>
                        <span class="tabular-nums" style="color: var(--color-text-muted); font-size: 0.8rem;">${formatDateTime(n.negotiationDate)}</span>
                    </div>
                    <div style="color: #475569; font-size: 0.85rem; font-style: italic; margin-top: 0.35rem;">
                        "${n.candidateNotes || ''}"
                    </div>
                    ${n.hrResponseNotes ? `
                        <div style="margin-top: 0.6rem; padding: 0.6rem 0.85rem; background: #ecfdf5; border-left: 3px solid #10b981; border-radius: 6px;">
                            <strong style="color: #065f46; font-size: 0.85rem;"><i class="bi bi-patch-check-fill me-1"></i> Phản hồi từ Phòng Nhân sự (HR):</strong>
                            <div style="color: #047857; font-size: 0.9rem; margin-top: 0.25rem; line-height: 1.5;">${n.hrResponseNotes}</div>
                        </div>
                    ` : `
                        <div style="margin-top: 0.5rem; font-size: 0.8rem; color: #9333ea; font-style: italic;">
                            <i class="bi bi-hourglass-split"></i> Đang chờ Ban Tuyển Dụng xem xét và phản hồi...
                        </div>
                    `}
                </div>
            `).join('');
        } else {
            candidateNegotiationBox.style.display = 'none';
        }
    }

    // --- Modal Openers ---
    btnOpenAcceptModal.addEventListener('click', () => modalAccept.classList.add('active'));
    btnOpenNegotiateModal.addEventListener('click', () => modalNegotiate.classList.add('active'));
    btnOpenDeclineModal.addEventListener('click', () => modalDecline.classList.add('active'));

    // --- 1. ACTION: ACCEPT OFFER ---
    btnConfirmAccept.addEventListener('click', async function () {
        btnConfirmAccept.disabled = true;
        btnConfirmAccept.innerHTML = `<i class="bi bi-arrow-repeat spin"></i> Đang xử lý...`;

        try {
            const res = await fetch(`/api/v1/offers/${offerId}/candidate-response`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ response: 'Accept' })
            });

            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Xác nhận đồng ý thất bại');

            modalAccept.classList.remove('active');
            showToast('Bạn đã đồng ý nhận việc thành công! Hệ thống đã ghi nhận và chuyển tới Quản lý tuyển dụng để xác nhận Tuyển dụng chính thức.', 'success');
            loadOfferLetter();

        } catch (err) {
            console.error(err);
            showToast(err.message, 'error');
        } finally {
            btnConfirmAccept.disabled = false;
            btnConfirmAccept.innerHTML = `<i class="bi bi-check-lg"></i> Đồng ý chính thức`;
        }
    });

    // --- 2. ACTION: NEGOTIATE OFFER (Màn 42) ---
    formNegotiate.addEventListener('submit', async function (e) {
        e.preventDefault();

        const counterSalary = (typeof negotiateCounterSalary.getRawValue === 'function')
            ? negotiateCounterSalary.getRawValue()
            : (parseFloat(negotiateCounterSalary.value.replace(/\D/g, '')) || 0);
        const notes = negotiateCandidateNotes.value.trim();

        if (isNaN(counterSalary) || counterSalary <= 0) {
            showToast('Vui lòng nhập mức lương đề xuất hợp lệ', 'error');
            return;
        }

        const btnSubmit = document.getElementById('btnConfirmNegotiate');
        btnSubmit.disabled = true;
        btnSubmit.innerHTML = `<i class="bi bi-arrow-repeat spin"></i> Đang gửi...`;

        try {
            const res = await fetch(`/api/v1/offers/${offerId}/candidate-response`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    response: 'Negotiate',
                    counterSalary: counterSalary,
                    candidateNotes: notes
                })
            });

            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Gửi đề xuất thương lượng thất bại');

            modalNegotiate.classList.remove('active');
            showToast('Đã gửi đề xuất thương lượng thành công tới HR!', 'success');
            loadOfferLetter();

        } catch (err) {
            console.error(err);
            showToast(err.message, 'error');
        } finally {
            btnSubmit.disabled = false;
            btnSubmit.innerHTML = `<i class="bi bi-send"></i> Gửi đề xuất thương lượng`;
        }
    });

    // --- 3. ACTION: DECLINE OFFER ---
    btnConfirmDecline.addEventListener('click', async function () {
        btnConfirmDecline.disabled = true;
        btnConfirmDecline.innerHTML = `<i class="bi bi-arrow-repeat spin"></i> Đang xử lý...`;

        try {
            const res = await fetch(`/api/v1/offers/${offerId}/candidate-response`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    response: 'Decline',
                    candidateNotes: declineReasonInput.value.trim()
                })
            });

            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Từ chối thất bại');

            modalDecline.classList.remove('active');
            showToast('Bạn đã từ chối thư mời nhận việc.', 'info');
            loadOfferLetter();

        } catch (err) {
            console.error(err);
            showToast(err.message, 'error');
        } finally {
            btnConfirmDecline.disabled = false;
            btnConfirmDecline.innerHTML = `Xác nhận Từ chối`;
        }
    });

    // Initial Load
    loadOfferLetter();
});
