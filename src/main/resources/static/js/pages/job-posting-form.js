/* =============================================================================
   JOB-POSTING-FORM.JS — Form Tạo / Sửa tin tuyển dụng (job-postings/create.html)

   JS chỉ giúp người dùng thấy lỗi SỚM và xem trước tin. Server (JobPostingValidator#normalizeAndValidate)
   luôn kiểm tra lại, nên quy tắc ở đây phải GIỐNG server, không thêm quy tắc riêng.

   Các phần:
     1. Tiện ích nhỏ (ngày tháng, escape HTML)
     2. "Lấy từ yêu cầu" / "Khôi phục bản gốc"
     3. Bản đồ địa điểm
     4. Lương, số lượng, quyền lợi
     5. Ngày đăng và hạn nộp
     6. Xem trước "Ứng viên sẽ thấy" + tab trên điện thoại
     7. Kiểm tra trước khi gửi + khóa nút
   ============================================================================= */
(() => {
    'use strict';

    const form = document.getElementById('posting-form');
    if (!form) {
        return;
    }
    const $ = id => document.getElementById(id);

    /* ---------------------------------------------------------------------
     * 1. TIỆN ÍCH
     * --------------------------------------------------------------------- */
    const esc = text => String(text ?? '').replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
    const pad = n => String(n).padStart(2, '0');
    /** yyyy-MM-dd theo giờ máy người dùng (không dùng toISOString vì trả giờ UTC). */
    const isoDate = date => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
    /** "2026-10-20" hoặc "2026-10-20T08:00" → "20/10/2026". */
    const viDate = value => value ? value.slice(0, 10).split('-').reverse().join('/') : '';
    const addDays = (isoValue, days) => {
        const [y, m, d] = isoValue.slice(0, 10).split('-').map(Number);
        return isoDate(new Date(y, m - 1, d + days));
    };
    const daysBetween = (fromIso, toIso) => {
        const [a, b] = [fromIso, toIso].map(v => v.slice(0, 10).split('-').map(Number));
        return Math.round((new Date(b[0], b[1] - 1, b[2]) - new Date(a[0], a[1] - 1, a[2])) / 864e5);
    };
    const today = form.dataset.today || isoDate(new Date());
    const radioValue = name => form.querySelector(`input[name="${name}"]:checked`)?.value || '';

    // Đổi yêu cầu nguồn: chọn xong thì mở lại form với yêu cầu đó
    document.querySelectorAll('[data-auto-submit]').forEach(select =>
        select.addEventListener('change', () => select.form.submit()));

    /* ---------------------------------------------------------------------
     * 2. "LẤY TỪ YÊU CẦU" / "KHÔI PHỤC BẢN GỐC"
     *    Mỗi ô điền sẵn có một nhãn [data-from] mang giá trị gốc (data-source) của yêu cầu.
     * --------------------------------------------------------------------- */
    function renderFromMarkers() {
        document.querySelectorAll('[data-from]').forEach(marker => {
            const field = $(marker.dataset.from);
            if (!field) return;
            const same = field.value.trim() === (marker.dataset.source || '').trim();
            marker.innerHTML = same
                ? 'Lấy từ yêu cầu'
                : `Đã chỉnh sửa · <button type="button" data-restore="${marker.dataset.from}">Khôi phục bản gốc</button>`;
        });
    }
    form.addEventListener('click', event => {
        const restore = event.target.closest('[data-restore]');
        if (!restore) return;
        const field = $(restore.dataset.restore);
        field.value = form.querySelector(`[data-from="${restore.dataset.restore}"]`).dataset.source || '';
        field.dispatchEvent(new Event('input', { bubbles: true }));
        field.focus();
    });

    /* ---------------------------------------------------------------------
     * 3. BẢN ĐỒ: thu gọn mặc định; bấm "Xem bản đồ" mới nhúng Google Maps theo địa chỉ
     *    (cần mạng) + link mở Google Maps làm dự phòng
     * --------------------------------------------------------------------- */
    const mapToggle = $('jpf-map-toggle');
    const mapsLink = address => 'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(address);
    const mapsEmbed = address => 'https://www.google.com/maps?q=' + encodeURIComponent(address) + '&output=embed';
    function mapHtml(address) {
        if (!address) {
            return '<p class="jpf-map-empty">Nhập địa chỉ để xem bản đồ</p>';
        }
        return `<iframe title="Bản đồ ${esc(address)}" src="${mapsEmbed(address)}" loading="lazy"
                    referrerpolicy="no-referrer-when-downgrade"></iframe>
                <a class="jpf-map-link" href="${mapsLink(address)}" target="_blank" rel="noopener">Mở Google Maps ↗</a>`;
    }
    let mapTimer = null;
    let lastMapAddress = null;
    function renderMap() {
        // Khung bản đồ đang thu gọn thì không tải Google Maps
        if (!mapToggle.open) return;
        const address = $('workLocation').value.trim();
        if (address === lastMapAddress) return;
        lastMapAddress = address;
        $('jpf-map').innerHTML = mapHtml(address);
    }
    // Bung khung thì tải bản đồ theo địa chỉ hiện tại
    mapToggle.addEventListener('toggle', renderMap);
    // Đợi người dùng ngừng gõ 0,8 giây mới tải lại bản đồ (tránh gọi Google Maps mỗi phím)
    $('workLocation').addEventListener('input', () => {
        clearTimeout(mapTimer);
        mapTimer = setTimeout(renderMap, 800);
    });

    /* ---------------------------------------------------------------------
     * 4. LƯƠNG, SỐ LƯỢNG, QUYỀN LỢI
     * --------------------------------------------------------------------- */
    // Bấm vào ô "Tự nhập" thì tự chọn lựa chọn "Tự nhập"
    $('salaryCustom').addEventListener('focus', () => {
        form.querySelector('input[name="salaryMode"][value="custom"]').checked = true;
        refresh();
    });
    function salaryText() {
        const mode = radioValue('salaryMode');
        if (mode === 'range') return form.dataset.salaryRange || 'Thỏa thuận';
        if (mode === 'custom') return $('salaryCustom').value.trim();
        return 'Thỏa thuận';
    }

    const showPositions = $('showPositions1') || form.querySelector('input[type="checkbox"][name="showPositions"]');
    function syncPositions() {
        $('numberOfPositionsDisplay').disabled = !showPositions.checked;
    }
    showPositions.addEventListener('change', syncPositions);

    // Nút "+ quyền lợi": thêm một dòng vào ô Quyền lợi; nút của quyền lợi đã có thì mờ đi
    const benefitBox = $('benefits');
    function syncBenefitChips() {
        const lines = benefitBox.value.split('\n').map(line => line.replace(/^[-•]\s*/, '').trim());
        form.querySelectorAll('[data-benefit]').forEach(chip => {
            chip.disabled = lines.includes(chip.dataset.benefit);
        });
    }
    form.querySelectorAll('[data-benefit]').forEach(chip => chip.addEventListener('click', () => {
        const current = benefitBox.value.trimEnd();
        benefitBox.value = (current ? current + '\n' : '') + '- ' + chip.dataset.benefit;
        benefitBox.dispatchEvent(new Event('input', { bubbles: true }));
    }));

    /* ---------------------------------------------------------------------
     * 5. NGÀY ĐĂNG VÀ HẠN NỘP
     * --------------------------------------------------------------------- */
    const scheduledAt = $('scheduledAt');
    const publishButton = $('jpf-publish');
    const later = () => radioValue('publishMode') === 'later';
    /** Ngày đăng dự kiến (yyyy-MM-dd): ngày hẹn nếu chọn hẹn đăng, ngược lại là hôm nay. */
    const postingDay = () => (later() && scheduledAt?.value) ? scheduledAt.value.slice(0, 10) : today;

    if (scheduledAt) {
        // Bấm vào ô thời điểm thì tự chọn "Hẹn ngày đăng"; ô đang trống thì gợi ý 8 giờ sáng mai
        scheduledAt.min = `${today}T00:00`;
        scheduledAt.addEventListener('focus', () => {
            form.querySelector('input[name="publishMode"][value="later"]').checked = true;
            if (!scheduledAt.value) scheduledAt.value = `${addDays(today, 1)}T08:00`;
            refresh();
        });
    }

    const deadline = $('applicationDeadline');
    form.querySelectorAll('[data-days]').forEach(button => button.addEventListener('click', () => {
        deadline.value = addDays(postingDay(), Number(button.dataset.days));
        deadline.dispatchEvent(new Event('input', { bubbles: true }));
    }));

    function renderDeadlineHelp() {
        const value = deadline.value;
        const days = value ? daysBetween(postingDay(), value) : null;
        $('deadline-help').textContent = days !== null && days > 0
            ? `Nhận hồ sơ trong ${days} ngày, đến hết ${viDate(value)}.` : '';
        if (publishButton) {
            publishButton.textContent = later() ? 'Lên lịch đăng' : 'Đăng tin';
        }
    }

    /* ---------------------------------------------------------------------
     * 6. XEM TRƯỚC "ỨNG VIÊN SẼ THẤY" + TAB TRÊN ĐIỆN THOẠI
     * --------------------------------------------------------------------- */
    function block(title, text) {
        const value = text.trim();
        return `<section><h4>${title}</h4>${value ? `<p>${esc(value)}</p>` : '<p class="jpf-empty">Chưa có nội dung</p>'}</section>`;
    }
    function renderPreview() {
        const title = $('postingTitle').value.trim();
        const address = $('workLocation').value.trim();
        const tags = [form.dataset.employment, form.dataset.workModel, form.dataset.department].filter(Boolean);
        const facts = [['Mức lương', salaryText() || '—'], ['Hạn nộp', deadline.value ? viDate(deadline.value) : '—']];
        if (showPositions.checked && $('numberOfPositionsDisplay').value.trim()) {
            facts.push(['Số lượng', $('numberOfPositionsDisplay').value.trim()]);
        }
        facts.push(['Địa điểm', address || '—']);

        $('jpf-preview').innerHTML = `
            <header><h3>${title ? esc(title) : '<span class="jpf-empty">Tiêu đề tin</span>'}</h3>
                <div class="jpf-tags">${tags.map(t => `<span>${esc(t)}</span>`).join('')}</div></header>
            <dl class="jpf-facts">${facts.map(([k, v]) => `<div><dt>${k}</dt><dd>${esc(v)}</dd></div>`).join('')}</dl>
            <div class="jpf-body">
                ${block('Mô tả công việc', $('jobDescription').value)}
                ${block('Yêu cầu ứng viên', $('jobRequirements').value)}
                ${benefitBox.value.trim() ? block('Quyền lợi', benefitBox.value) : ''}
                ${address ? `<section><h4>Nơi làm việc</h4><a href="${mapsLink(address)}" target="_blank" rel="noopener">${esc(address)} ↗</a></section>` : ''}
            </div>
            <footer><span>Đăng ${viDate(postingDay())}</span><span class="btn btn--primary btn--sm" aria-hidden="true">Ứng tuyển ngay</span></footer>`;
    }

    form.querySelectorAll('[data-tab]').forEach(tab => tab.addEventListener('click', () => {
        form.dataset.tab = tab.dataset.tab;
        form.querySelectorAll('[data-tab]').forEach(other => other.setAttribute('aria-selected', String(other === tab)));
    }));

    /* ---------------------------------------------------------------------
     * 7. KIỂM TRA TRƯỚC KHI GỬI (giống JobPostingValidator#normalizeAndValidate) + KHÓA NÚT
     * --------------------------------------------------------------------- */
    function showError(field, message) {
        const slot = $(field + '-error');
        if (slot) slot.textContent = message;
        const input = $(field) || $(field + '-group');
        if (input) {
            input.toggleAttribute('aria-invalid', Boolean(message));
            input.classList.toggle('is-invalid', Boolean(message));
        }
    }

    function validate(publishing) {
        const errors = {};
        // Độ dài tối đa: maxlength trên ô đã chặn không cho nhập thêm, js/text-limit.js báo "Nội dung quá N ký tự"
        const required = (field, message) => {
            if (!$(field).value.trim()) errors[field] = message;
        };
        required('postingTitle', 'Vui lòng nhập tiêu đề tin.');
        required('jobDescription', 'Vui lòng nhập mô tả công việc.');
        required('jobRequirements', 'Vui lòng nhập yêu cầu ứng viên.');
        if (radioValue('salaryMode') === 'custom') {
            required('salaryCustom', 'Nhập mức lương hiển thị hoặc chọn một lựa chọn khác.');
        }
        if (showPositions.checked) {
            required('numberOfPositionsDisplay', 'Nhập số lượng hiển thị hoặc tắt hiển thị số lượng.');
        }
        if (publishing && later()) {
            const value = scheduledAt?.value;
            if (!value) errors.scheduledAt = 'Vui lòng chọn thời điểm đăng.';
            else if (new Date(value) <= new Date()) errors.scheduledAt = 'Thời điểm hẹn đăng phải sau thời điểm hiện tại.';
        }
        if (!deadline.value) {
            if (publishing) errors.applicationDeadline = 'Vui lòng chọn hạn nộp hồ sơ.';
        } else if (deadline.value < today) {
            errors.applicationDeadline = 'Hạn nộp hồ sơ phải từ hôm nay trở đi.';
        } else if (publishing && deadline.value < postingDay()) {
            errors.applicationDeadline = 'Hạn nộp hồ sơ phải sau ngày đăng.';
        }

        ['postingTitle', 'jobDescription', 'jobRequirements', 'salaryCustom', 'numberOfPositionsDisplay',
            'scheduledAt', 'applicationDeadline'].forEach(field => showError(field, errors[field] || ''));
        return errors;
    }

    let submitting = false;
    form.addEventListener('submit', event => {
        if (submitting) {
            event.preventDefault();
            return;
        }
        const action = event.submitter?.value || 'draft';
        // Giống server: tin không còn là nháp (đã đăng / hẹn đăng / tạm dừng) thì "Lưu thay đổi" cũng phải đủ hạn nộp
        const publishing = action === 'publish' || form.dataset.draft !== 'true';
        const errors = validate(publishing);
        const first = Object.keys(errors)[0];
        if (first) {
            event.preventDefault();
            form.dataset.tab = 'edit';
            ($(first) || $(first + '-group'))?.scrollIntoView({ behavior: 'smooth', block: 'center' });
            $(first)?.focus({ preventScroll: true });
            return;
        }
        submitting = true;
        // Khóa nút sau khi trình duyệt đã lấy giá trị nút được bấm (action=draft/publish)
        setTimeout(() => form.querySelectorAll('button[type="submit"]').forEach(button => { button.disabled = true; }), 0);
    });
    window.addEventListener('pageshow', () => {
        submitting = false;
        form.querySelectorAll('button[type="submit"]').forEach(button => { button.disabled = false; });
    });

    /* ---------------------------------------------------------------------
     * Cập nhật mọi phần phụ thuộc khi người dùng gõ / chọn
     * --------------------------------------------------------------------- */
    function refresh() {
        renderFromMarkers();
        syncBenefitChips();
        renderDeadlineHelp();
        renderPreview();
    }
    form.addEventListener('input', refresh);
    form.addEventListener('change', refresh);

    form.dataset.tab = 'edit';
    syncPositions();
    renderMap();
    refresh();
    $('jpf-summary')?.focus();
})();
