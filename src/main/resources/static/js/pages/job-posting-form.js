/* =============================================================================
   JOB-POSTING-FORM.JS — Form Tạo / Sửa tin tuyển dụng (job-postings/create.html)

   JS chỉ giúp người dùng thấy lỗi SỚM và xem trước tin. Server (JobPostingValidator#normalizeAndValidate)
   luôn kiểm tra lại, nên quy tắc ở đây phải GIỐNG server, không thêm quy tắc riêng.

   Các phần:
     1. Tiện ích nhỏ (ngày tháng, escape HTML)
     2. Địa điểm làm việc
     3. Lương và số lượng
     4. Ngày đăng và hạn nộp
     5. Xem trước "Ứng viên sẽ thấy" + tab trên điện thoại
     6. Kiểm tra trước khi gửi + khóa nút
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
    const escapeHtml = text => String(text ?? '').replace(/[&<>"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[char]));
    const pad = number => String(number).padStart(2, '0');
    /** yyyy-MM-dd theo giờ máy người dùng (không dùng toISOString vì trả giờ UTC). */
    const isoDate = date => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
    /** "2026-10-20" hoặc "2026-10-20T08:00" → "20/10/2026". */
    const viDate = value => value ? value.slice(0, 10).split('-').reverse().join('/') : '';
    const addDays = (isoValue, days) => {
        const [y, m, d] = isoValue.slice(0, 10).split('-').map(Number);
        return isoDate(new Date(y, m - 1, d + days));
    };
    const today = form.dataset.today || isoDate(new Date());
    const isRemote = () => $('workModel').value === 'Remote';
    const mapToggle = $('jpf-map-toggle');

    function renderFromMarkers() {
        form.querySelectorAll('[data-from]').forEach(marker => {
            const field = $(marker.dataset.from);
            if (!field) return;
            marker.hidden = marker.dataset.from === 'workLocation' && isRemote();
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
        if (!field) return;
        field.value = form.querySelector(`[data-from="${restore.dataset.restore}"]`).dataset.source || '';
        field.dispatchEvent(new Event('input', { bubbles: true }));
        field.dispatchEvent(new Event('change', { bubbles: true }));
    });

    // Đổi yêu cầu nguồn: chọn xong thì mở lại form với yêu cầu đó
    document.querySelectorAll('[data-auto-submit]').forEach(select =>
        select.addEventListener('change', () => select.form.submit()));

    /* ---------------------------------------------------------------------
     * 2. ĐỊA ĐIỂM: làm việc từ xa thì khóa ô địa điểm
     * --------------------------------------------------------------------- */
    function syncWorkLocation() {
        const remote = isRemote();
        $('workLocation').disabled = remote;
        if (remote) {
            $('workLocation').value = '';
            mapToggle.open = false;
            lastMapAddress = null;
            $('jpf-map').replaceChildren();
            showError('workLocation', '');
        }
        mapToggle.hidden = remote;
    }

    const mapsLink = address => 'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(address);
    const mapsEmbed = address => 'https://www.google.com/maps?q=' + encodeURIComponent(address) + '&output=embed';
    let mapTimer = null;
    let lastMapAddress = null;
    function renderMap() {
        if (!mapToggle.open) return;
        const address = isRemote() ? '' : $('workLocation').value.trim();
        if (address === lastMapAddress) return;
        lastMapAddress = address;
        $('jpf-map').innerHTML = address
            ? `<iframe title="Bản đồ ${escapeHtml(address)}" src="${mapsEmbed(address)}" loading="lazy"
                referrerpolicy="no-referrer-when-downgrade"></iframe>
                <a class="jpf-map-link" href="${mapsLink(address)}" target="_blank" rel="noopener">Mở Google Maps ↗</a>`
            : '<p class="jpf-map-empty">Nhập địa chỉ để xem bản đồ</p>';
    }
    mapToggle.addEventListener('toggle', renderMap);
    $('workLocation').addEventListener('input', () => {
        clearTimeout(mapTimer);
        mapTimer = setTimeout(renderMap, 800);
    });

    /* ---------------------------------------------------------------------
     * 3. LƯƠNG VÀ SỐ LƯỢNG
     * --------------------------------------------------------------------- */
    function salaryText() {
        return $('salaryCustom').value.trim();
    }


    /* ---------------------------------------------------------------------
     * 4. NGÀY ĐĂNG VÀ HẠN NỘP
     * --------------------------------------------------------------------- */
    const postingDay = () => today;

    const deadline = $('applicationDeadline');
    form.querySelectorAll('[data-days]').forEach(button => button.addEventListener('click', () => {
        deadline.value = addDays(postingDay(), Number(button.dataset.days));
        deadline.dispatchEvent(new Event('input', { bubbles: true }));
    }));

    /* ---------------------------------------------------------------------
     * 5. XEM TRƯỚC "ỨNG VIÊN SẼ THẤY" + TAB TRÊN ĐIỆN THOẠI
     * --------------------------------------------------------------------- */
    function previewSection(title, text) {
        const value = text.trim();
        return `<section><h4>${title}</h4>${value ? `<p>${escapeHtml(value)}</p>` : '<p class="jpf-empty">Chưa có nội dung</p>'}</section>`;
    }
    function renderPreview() {
        const title = $('postingTitle').value.trim();
        const address = isRemote() ? '' : $('workLocation').value.trim();
        // Nhãn đang chọn ở ô Hình thức / Mô hình (HR đổi được), cộng phòng ban của yêu cầu
        const selectedLabel = id => $(id).selectedOptions[0]?.textContent.trim();
        const tags = [selectedLabel('employmentType'), selectedLabel('workModel'), form.dataset.department].filter(Boolean);
        const facts = [['Mức lương', salaryText() || '—'], ['Hạn ứng tuyển', deadline.value ? viDate(deadline.value) : '—']];
        if ($('numberOfPositionsDisplay').value.trim()) {
            facts.push(['Số lượng', `${$('numberOfPositionsDisplay').value.trim()} người`]);
        }
        const workingHours = $('workingHours').value.trim();
        // Mục "Địa điểm và thời gian" giống trang /jobs/{id}: chỉ hiện dòng nào HR đã nhập
        const placeTime = (address ? `<h5>Địa điểm làm việc</h5><a href="${mapsLink(address)}" target="_blank" rel="noopener">${escapeHtml(address)} ↗</a>` : '')
            + (workingHours ? `<h5>Thời gian làm việc</h5><p>${escapeHtml(workingHours)}</p>` : '');

        $('jpf-preview').innerHTML = `
            <header><h3>${title ? escapeHtml(title) : '<span class="jpf-empty">Tiêu đề tin</span>'}</h3>
                <div class="jpf-tags">${tags.map(tag => `<span>${escapeHtml(tag)}</span>`).join('')}</div></header>
            <h4>Tổng quan</h4>
            <dl class="jpf-facts">${facts.map(([label, value]) => `<div><dt>${label}</dt><dd>${escapeHtml(value)}</dd></div>`).join('')}</dl>
            <div class="jpf-body">
                ${previewSection('Mô tả công việc', $('jobDescription').value)}
                ${previewSection('Yêu cầu ứng viên', $('jobRequirements').value)}
                ${$('benefits').value.trim() ? previewSection('Quyền lợi ứng viên', $('benefits').value) : ''}
                ${placeTime ? `<section><h4>Địa điểm và thời gian</h4>${placeTime}</section>` : ''}
            </div>
            <footer><span>${form.dataset.postingDate
                ? `Ngày đăng: ${viDate(form.dataset.postingDate)}`
                : 'Ngày đăng sẽ được xác định khi đăng tin'}</span></footer>`;
    }

    form.querySelectorAll('[data-tab]').forEach(tab => tab.addEventListener('click', () => {
        form.dataset.tab = tab.dataset.tab;
        form.querySelectorAll('[data-tab]').forEach(other => other.setAttribute('aria-selected', String(other === tab)));
    }));

    /* ---------------------------------------------------------------------
     * 6. KIỂM TRA TRƯỚC KHI GỬI (giống JobPostingValidator#normalizeAndValidate) + KHÓA NÚT
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

    // Các ô được kiểm tra (độ dài tối đa do maxlength + js/text-limit.js lo)
    const CHECKED_FIELDS = ['postingTitle', 'jobDescription', 'jobRequirements', 'salaryCustom',
        'numberOfPositionsDisplay', 'applicationDeadline'];

    /** Câu lỗi của một ô ('' = hợp lệ); publishing = kiểm tra theo luật "Đăng tin". */
    function checkField(field, publishing) {
        const empty = !$(field).value.trim();
        if (field === 'postingTitle') {
            return empty ? 'Vui lòng nhập tiêu đề tin.' : '';
        }
        if (field === 'jobDescription') {
            return empty ? 'Vui lòng nhập mô tả công việc.' : '';
        }
        if (field === 'jobRequirements') {
            return empty ? 'Vui lòng nhập yêu cầu ứng viên.' : '';
        }
        if (field === 'salaryCustom') {
            return empty ? 'Vui lòng nhập mức lương.' : '';
        }
        if (field === 'numberOfPositionsDisplay') {
            if (empty) return 'Vui lòng nhập số lượng.';
            const input = $(field);
            return input.validity.badInput || !/^[0-9]+$/.test(input.value) || Number(input.value) < 1
                || Number(input.value) > 2147483647
                ? 'Số lượng phải lớn hơn hoặc bằng 1.' : '';
        }
        // Còn lại: hạn nộp hồ sơ
        if (deadline.validity.badInput) {
            return 'Hạn ứng tuyển không hợp lệ.';
        }
        if (empty) {
            return publishing ? 'Vui lòng chọn hạn nộp hồ sơ.' : '';
        }
        if (deadline.value < today) {
            return 'Hạn nộp hồ sơ phải từ hôm nay trở đi.';
        }
        if (publishing && deadline.value < postingDay()) {
            return 'Hạn nộp hồ sơ phải sau ngày đăng.';
        }
        return '';
    }

    /** Kiểm tra cả form khi bấm nút lưu; trả về các ô lỗi theo thứ tự trên form. */
    function validate(publishing) {
        const errors = {};
        CHECKED_FIELDS.forEach(field => {
            const message = checkField(field, publishing);
            showError(field, message);
            if (message) errors[field] = message;
        });
        return errors;
    }

    // Rời ô thì kiểm tra ngay theo luật "Đăng tin" (giống form yêu cầu tuyển dụng); Lưu nháp vẫn kiểm tra lại theo luật nháp
    form.addEventListener('focusout', event => {
        if (CHECKED_FIELDS.includes(event.target.id)) {
            showError(event.target.id, checkField(event.target.id, true));
        }
    });

    // Ô đang báo lỗi thì kiểm tra lại khi người dùng sửa, đúng rồi thì dòng chữ đỏ tự mất
    function recheckInvalidFields() {
        CHECKED_FIELDS.filter(field => $(field).classList.contains('is-invalid'))
            .forEach(field => showError(field, checkField(field, true)));
    }
    form.addEventListener('input', recheckInvalidFields);
    form.addEventListener('change', recheckInvalidFields);
    deadline.addEventListener('change', () => showError('applicationDeadline', checkField('applicationDeadline', true)));

    let submitting = false;
    form.addEventListener('submit', event => {
        if (submitting) {
            event.preventDefault();
            return;
        }
        const action = event.submitter?.value || 'draft';
        // Giống server: tin không còn là nháp (đã đăng / tạm dừng) thì "Lưu thay đổi" cũng phải đủ hạn nộp
        const publishing = action === 'publish' || form.dataset.draft !== 'true';
        const errors = validate(publishing);
        // Ô còn báo "Nội dung quá N ký tự" (text-limit.js) cũng không cho lưu
        const tooLong = form.querySelector('.is-too-long');
        const first = $(Object.keys(errors)[0]) || tooLong;
        if (first) {
            event.preventDefault();
            form.dataset.tab = 'edit';
            first.scrollIntoView({ behavior: 'smooth', block: 'center' });
            first.focus({ preventScroll: true });
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
        syncWorkLocation();
        renderFromMarkers();
        renderPreview();
    }
    form.addEventListener('input', refresh);
    form.addEventListener('change', refresh);

    form.dataset.tab = 'edit';
    refresh();
    $('jpf-summary')?.focus();
})();
