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

function initOfferForm() {
    initOfferFormExpectedStartDateMin();

    const select = document.getElementById('createAppSelect');
    if (select && select.value) {
        handleSelectPassedCandidate(select.value);
    }

    validateProbationRuleUi();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initOfferForm);
} else {
    initOfferForm();
}

window.handleSelectPassedCandidate = handleSelectPassedCandidate;
window.autoCalculateProbationSalary = autoCalculateProbationSalary;
window.validateProbationRuleUi = validateProbationRuleUi;
window.updateMinExpectedStartDate = updateMinExpectedStartDate;
