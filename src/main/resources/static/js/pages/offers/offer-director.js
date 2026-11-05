/**
 * offer-director.js — Phân hệ Offer Management (SWP391 — Mộc RMS Group 2)
 * Trách nhiệm luồng nghiệp vụ của Giám đốc (Director Approval Workflows):
 * - SECTION A: Hộp thoại phê duyệt / từ chối nhanh (Quick Decision Modal trên List & Director List)
 * - SECTION B: Bộ lọc, tìm kiếm và phân trang danh sách đề xuất của Giám đốc (Director List)
 * - SECTION C: Các thao tác ra quyết định trên màn hình xem chi tiết (Offer Detail Decision)
 * - SECTION D: Xuất khẩu toàn cục ra window để tương thích thuộc tính inline của Thymeleaf
 */

// =========================================================================
// SECTION A — DIRECTOR QUICK DECISION MODAL (Screen 30 & Screen 30D)
// =========================================================================

/**
 * Mở hộp thoại phê duyệt hoặc từ chối nhanh đề xuất thư mời
 * @param {number|string} offerId
 * @param {'APPROVE'|'REJECT'} actionType
 */
function openDirectorDecisionModal(offerId, actionType) {
    const modal = document.getElementById('directorDecisionModal');
    if (!modal) return;

    const offerIdInput = document.getElementById('directorDecisionOfferId');
    const actionInput = document.getElementById('directorDecisionAction');
    if (offerIdInput) offerIdInput.value = offerId;
    if (actionInput) actionInput.value = actionType;

    const commentsEl = document.getElementById('directorDecisionComments');
    if (commentsEl) {
        commentsEl.value = '';
        commentsEl.classList.remove('is-invalid');
    }

    const counterEl = document.getElementById('directorDecisionCommentCount');
    if (counterEl) {
        counterEl.textContent = '0';
    }

    const errorEl = document.getElementById('directorDecisionCommentsError');
    if (errorEl) {
        errorEl.hidden = true;
    }

    const subnoteEl = document.getElementById('directorDecisionSubnote');
    const labelEl = document.getElementById('directorDecisionCommentsLabel');
    const titleEl = document.getElementById('directorDecisionTitle');
    const btnSubmit = document.getElementById('btnSubmitDecision');

    if (actionType === 'APPROVE') {
        if (titleEl) {
            titleEl.innerHTML = '<span class="ofw-modal-title-icon ofw-modal-title-icon--success"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><polyline points="20 6 9 17 4 12"/></svg></span> Phê duyệt đề xuất thư mời?';
        }
        if (btnSubmit) {
            btnSubmit.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true" class="ofw-btn-icon-inline"><polyline points="20 6 9 17 4 12"/></svg> Xác nhận phê duyệt';
            btnSubmit.className = 'btn btn--primary';
        }
        if (labelEl) {
            labelEl.innerHTML = 'Phản hồi / Ghi chú của Giám đốc (Không bắt buộc)';
        }
        if (commentsEl) {
            commentsEl.placeholder = 'Nhập ghi chú hoặc ý kiến phê duyệt...';
            commentsEl.removeAttribute('required');
        }
        if (subnoteEl) {
            subnoteEl.hidden = true;
        }
    } else {
        if (titleEl) {
            titleEl.innerHTML = '<span class="ofw-modal-title-icon ofw-modal-title-icon--danger"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg></span> Từ chối đề xuất thư mời?';
        }
        if (btnSubmit) {
            btnSubmit.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true" class="ofw-btn-icon-inline"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg> Xác nhận từ chối';
            btnSubmit.className = 'btn btn--danger';
        }
        if (labelEl) {
            labelEl.innerHTML = 'Lý do từ chối của Giám đốc <span class="ofw-required-mark">*</span>';
        }
        if (commentsEl) {
            commentsEl.placeholder = 'Vui lòng nhập lý do từ chối đề xuất thư mời (bắt buộc)...';
            commentsEl.setAttribute('required', 'required');
        }
        if (subnoteEl) {
            subnoteEl.hidden = false;
        }
    }

    if (!modal.open) modal.showModal();
}

/**
 * Cập nhật số lượng ký tự nhập vào ô nhận xét modal và xóa viền cảnh báo lỗi
 */
function handleDirectorDecisionCommentInput(textarea) {
    if (!textarea) return;
    const counterEl = document.getElementById('directorDecisionCommentCount');
    if (counterEl) {
        counterEl.textContent = textarea.value.length;
    }
    if (textarea.value.trim().length > 0) {
        textarea.classList.remove('is-invalid');
        const errorEl = document.getElementById('directorDecisionCommentsError');
        if (errorEl) {
            errorEl.hidden = true;
        }
    }
}

/**
 * Đóng hộp thoại quyết định nhanh và xóa sạch trạng thái lỗi
 */
function closeDirectorDecisionModal() {
    const modal = document.getElementById('directorDecisionModal');
    if (modal) {
        modal.close();
    }
    const commentsEl = document.getElementById('directorDecisionComments');
    if (commentsEl) {
        commentsEl.classList.remove('is-invalid');
    }
    const errorEl = document.getElementById('directorDecisionCommentsError');
    if (errorEl) {
        errorEl.hidden = true;
    }
}

/**
 * Nộp quyết định của Giám đốc qua AJAX Fetch JSON tới backend
 */
function submitDirectorDecision() {
    const offerIdEl = document.getElementById('directorDecisionOfferId');
    const actionTypeEl = document.getElementById('directorDecisionAction');
    const offerId = offerIdEl ? offerIdEl.value : '';
    const actionType = actionTypeEl ? actionTypeEl.value : '';
    const commentsEl = document.getElementById('directorDecisionComments');
    const comments = commentsEl ? commentsEl.value.trim() : '';

    if (!offerId || !actionType) return;

    // Yêu cầu nhập lý do bắt buộc khi Giám đốc Từ chối (bảo vệ BR-OFF-03)
    if (actionType === 'REJECT' && !comments) {
        if (commentsEl) {
            commentsEl.classList.add('is-invalid');
            commentsEl.focus();
        }
        const errorEl = document.getElementById('directorDecisionCommentsError');
        if (errorEl) {
            errorEl.hidden = false;
        }
        if (typeof showFlashToast === 'function') {
            showFlashToast('Vui lòng nhập lý do từ chối vào ô nhận xét trước khi tiếp tục.', 'warning');
        }
        return;
    }

    const btn = document.getElementById('btnSubmitDecision');
    if (btn?.disabled) return;
    const originalBtnHtml = btn?.innerHTML;
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<span class="ofw-spinner-sm" aria-hidden="true"></span> Đang xử lý...';
    }

    const url = actionType === 'APPROVE' ? `/offers/${offerId}/approve` : `/offers/${offerId}/reject`;

    executeDirectorDecisionApi(url, comments, () => {
        closeDirectorDecisionModal();
        window.location.reload();
    }).finally(() => {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = originalBtnHtml;
        }
    });
}

// =========================================================================
// SECTION C — DIRECTOR DETAIL WORKFLOW (Screen 32 Detail Decisions)
// =========================================================================

/**
 * Cập nhật bộ đếm ký tự trên ô nhận xét trang chi tiết (Hỗ trợ tối đa 2000 ký tự theo DTO backend)
 */
function updateDirectorDetailCommentCounter(el) {
    if (!el) return;
    const len = el.value.length;
    const cnt = document.getElementById('directorDetailCommentCount');
    if (cnt) cnt.innerText = len;
    if (el.value.trim().length > 0) {
        el.classList.remove('is-invalid');
        const errorMsg = document.getElementById('directorCommentsError');
        if (errorMsg) errorMsg.hidden = true;
    }
}

/**
 * Cuộn mượt màn hình tới khung Quyết định của Giám đốc
 */
function scrollToDirectorDecision() {
    const sec = document.getElementById('directorDecisionSection');
    if (sec) {
        sec.scrollIntoView({ behavior: 'smooth', block: 'center' });
        const comments = document.getElementById('directorDetailComments');
        if (comments) comments.focus();
    }
}

function showDirectorRejectReasonRequiredAlert() {
    const commentsEl = document.getElementById('directorDetailComments') || document.getElementById('rejectReasonInput');
    if (commentsEl) {
        commentsEl.classList.add('is-invalid');
        commentsEl.focus();
        const errorMsg = document.getElementById('directorCommentsError');
        if (errorMsg) errorMsg.hidden = false;
    }
    if (typeof showFlashToast === 'function') {
        showFlashToast('Vui lòng nhập lý do từ chối vào ô nhận xét trước khi tiếp tục.', 'warning');
    }
}

let pendingDirectorDecision = null;

/**
 * Điều phối khi Giám đốc bấm nút Duyệt hoặc Từ chối trên màn hình chi tiết
 */
function submitDirectorDecisionFromScreen(offerId, action) {
    if (!offerId) return;
    const commentsEl = document.getElementById('directorDetailComments');
    const comments = commentsEl ? commentsEl.value.trim() : '';

    pendingDirectorDecision = {
        offerId: offerId,
        action: action,
        comments: comments
    };

    if (action === 'REJECT') {
        if (!comments) {
            showDirectorRejectReasonRequiredAlert();
            return;
        }

        const rejectDialog = document.getElementById('directorRejectConfirmDialog');
        if (rejectDialog && typeof rejectDialog.showModal === 'function') {
            const descEl = document.getElementById('directorRejectDialogDesc');
            if (descEl) descEl.innerText = `Bạn có chắc chắn muốn từ chối đề xuất #${offerId}?`;
            rejectDialog.showModal();
        } else {
            executeDirectorDecisionApi(`/offers/${offerId}/reject`, comments);
        }
    } else if (action === 'APPROVE') {
        const approveDialog = document.getElementById('directorApproveConfirmDialog');
        if (approveDialog && typeof approveDialog.showModal === 'function') {
            approveDialog.showModal();
        } else {
            executeDirectorDecisionApi(`/offers/${offerId}/approve`, comments || 'Phê duyệt trực tiếp');
        }
    }
}

function confirmDirectorApproveAction() {
    const approveDialog = document.getElementById('directorApproveConfirmDialog');
    if (approveDialog) approveDialog.close();

    if (pendingDirectorDecision && pendingDirectorDecision.offerId) {
        const { offerId, comments } = pendingDirectorDecision;
        executeDirectorDecisionApi(`/offers/${offerId}/approve`, comments || 'Phê duyệt trực tiếp');
    }
}

function confirmDirectorRejectAction() {
    const rejectDialog = document.getElementById('directorRejectConfirmDialog');
    if (rejectDialog) rejectDialog.close();

    if (pendingDirectorDecision && pendingDirectorDecision.offerId) {
        const { offerId, comments } = pendingDirectorDecision;
        executeDirectorDecisionApi(`/offers/${offerId}/reject`, comments);
    }
}

async function executeDirectorDecisionApi(url, comments, onSuccess = () => { window.location.href = '/offers'; }) {
    try {
        const data = await offerRequest(url, { body: { comments } });
        queueOfferToast(data.message || 'Xử lý quyết định thành công!');
        onSuccess();
    } catch (error) {
        showFlashToast(error.message, 'error');
    }
}

// =========================================================================
// SECTION D — WINDOW GLOBAL EXPORTS
// =========================================================================
window.openDirectorDecisionModal = openDirectorDecisionModal;
window.handleDirectorDecisionCommentInput = handleDirectorDecisionCommentInput;
window.closeDirectorDecisionModal = closeDirectorDecisionModal;
window.submitDirectorDecision = submitDirectorDecision;


window.updateDirectorDetailCommentCounter = updateDirectorDetailCommentCounter;
window.scrollToDirectorDecision = scrollToDirectorDecision;
window.submitDirectorDecisionFromScreen = submitDirectorDecisionFromScreen;
window.confirmDirectorApproveAction = confirmDirectorApproveAction;
window.confirmDirectorRejectAction = confirmDirectorRejectAction;
