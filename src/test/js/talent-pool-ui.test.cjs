const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { runInNewContext } = require('node:vm');
const Selection = require('../../main/resources/static/js/pages/talent-pool-selection.js');

function element() {
    const handlers = new Map();
    const children = [];
    return {
        children, attributes: new Map(),
        dataset: {}, hidden: false, disabled: false, checked: false, textContent: '',
        classList: {toggle() {}},
        setAttribute(name, value) { this.attributes.set(name, value); },
        append(...nodes) { children.push(...nodes); },
        replaceChildren(...nodes) { children.splice(0, children.length, ...nodes); },
        showModal() { this.open = true; },
        close() { this.open = false; this.fire('close'); },
        focus() { this.focused = true; },
        addEventListener(name, callback) {
            handlers.set(name, [...(handlers.get(name) || []), callback]);
        },
        fire(name, extra = {}) {
            const event = {target: this, prevented: false, preventDefault() { this.prevented = true; }, ...extra};
            (handlers.get(name) || []).forEach(callback => callback(event));
            return event;
        },
    };
}

function fixture({missingSecondCv = false, brokenStorage = false, prior = [], submitFailure = false, invalidSource = false, hrMode = false} = {}) {
    const key = 'rms:talent-selection:v1:5:8';
    const data = new Map([[key, JSON.stringify(prior)]]);
    const storage = {getItem: key => data.get(key) || null, setItem: (key, value) => {
        if (brokenStorage) throw new Error('Unavailable');
        data.set(key, value);
    }};
    const rows = [1, 2].map(id => {
        const row = element();
        row.dataset = {poolCandidate: String(id), poolName: `Ứng viên ${id}`};
        const checkbox = element();
        checkbox.dataset.eligible = id === 2 && missingSecondCv ? 'false' : 'true';
        const preview = element();
        const selectedLabel = element();
        const fullLabel = element();
        const select = element();
        select.options = [1, 2].map(index => ({value: String(id * 100 + index), textContent: `Vị trí ${id}-${index} · 17/10/2026`, dataset: {cvUrl: `/talent-pool/${id}/cv/${id * 100 + index}`, jobTitle: `Vị trí ${id}-${index}`}}));
        let value = select.options[0].value;
        Object.defineProperty(select, 'value', {get: () => value, set: next => { value = next; }});
        Object.defineProperty(select, 'selectedOptions', {get: () => select.options.filter(option => option.value === value)});
        select.closest = selector => selector === '.tp-page' ? page : row;
        row.querySelector = selector => ({'[data-pool-cv]': id === 2 && missingSecondCv ? null : select,
            '[data-pool-select]': checkbox, '[data-app-preview]': preview,
            '[data-pool-selected-label]': selectedLabel, '[data-pool-cv-label]': fullLabel}[selector] || null);
        return {row, checkbox, select, preview, fullLabel};
    });
    const ids = new Map();
    const byId = id => {
        if (!ids.has(id)) ids.set(id, element());
        return ids.get(id);
    };
    const filter = element();
    const nextPage = element();
    const profileLink = element();
    const page = {
        dataset: {poolScope: '5', poolJob: '8', poolFlow: hrMode ? 'hr' : 'hm'},
        querySelector: selector => selector === '.tp-filter' ? filter : null,
        querySelectorAll: selector => selector === '[data-pool-candidate]' ? rows.map(item => item.row)
            : selector === '.pagination-wrapper a, .tp-filter a, [data-pool-detail], #tp-notes-detail, #app-preview-detail'
                ? [nextPage, profileLink, byId('tp-notes-detail'), byId('app-preview-detail')] : [],
    };
    const document = {
        querySelector: selector => selector === '.tp-page' ? page : {content: 'csrf-token'},
        querySelectorAll: selector => selector === '[data-pool-cv]'
            ? rows.filter((_, index) => !(index === 1 && missingSecondCv)).map(item => item.select) : [],
        getElementById: byId,
        createElement: () => element(),
        createElementNS: () => element(),
    };
    byId('tp-selection-form').action = hrMode ? '/talent-pool/jobs/8/recommend' : '/talent-pool/jobs/8/suggest';
    byId('tp-selection-form').dataset = {reviewUrl: '/talent-pool/jobs/8/selection-review', suggestionsUrl: hrMode ? '/talent-pool/jobs/8/recommendations' : '/talent-pool/jobs/8/suggestions'};
    let lastRequest;
    const requests = [];
    runInNewContext(readFileSync(resolve(__dirname, '../../main/resources/static/js/pages/talent-pool.js'), 'utf8'), {
        document, window: {sessionStorage: storage, location: {search: '?jobPostingId=8&keyword=Java&page=1'}}, TalentPoolSelection: Selection,
        setTimeout, clearTimeout, AbortController, URLSearchParams,
        fetch: async (url, options) => {
            lastRequest = {url, ...options};
            requests.push(lastRequest);
            if (url.endsWith('/suggest') || url.endsWith('/recommend')) {
                return {ok: !submitFailure, status: submitFailure ? 400 : 200,
                    headers: {get: () => 'application/json'},
                    json: async () => submitFailure ? {success: false, message: 'Ứng viên đã có đề xuất.'}
                        : {success: true, data: {jobPostingId: 8, addedCount: JSON.parse(options.body).sourceApplicationIds.length}}};
            }
            return {ok: true, headers: {get: () => 'application/json'}, json: async () => ({success: true, data: {
                submissionSupported: true,
                items: JSON.parse(options.body).sourceApplicationIds.map(sourceId => ({
                    candidateId: Math.floor(sourceId / 100), sourceApplicationId: sourceId,
                    candidateName: `Ứng viên ${Math.floor(sourceId / 100)}`, cvTitle: `Vị trí của CV ${sourceId}`,
                    submittedAt: '2026-10-17T08:30:00', error: invalidSource ? 'CV không hợp lệ.' : null,
                })),
            }})};
        },
    });
    return {rows, byId, filter, nextPage, profileLink, saved: () => JSON.parse(data.get(key)), request: () => lastRequest, requests};
}

test('row selection and changing the CV update both persisted mapping and preview URL', () => {
    const ui = fixture();
    const first = ui.rows[0];
    first.checkbox.checked = true;
    first.checkbox.fire('change');
    assert.equal(ui.byId('tp-selection-count').textContent, 'Đã chọn 1 ứng viên');
    first.select.value = '102';
    first.select.fire('change');
    assert.equal(first.checkbox.value, '102');
    assert.equal(first.preview.dataset.cvUrl, '/talent-pool/1/cv/102');
    assert.equal(first.preview.dataset.job, 'Vị trí 1-2');
    assert.equal(first.select.title, 'Vị trí 1-2 · 17/10/2026');
    assert.deepEqual(ui.saved().map(item => [item.candidateId, item.sourceId]), [[1, 102]]);
});

test('select all skips a candidate without CV and clears only eligible rows on that page', () => {
    const ui = fixture({missingSecondCv: true});
    assert.equal(ui.rows[1].checkbox.disabled, true);
    const all = ui.byId('tp-select-page');
    all.checked = true; all.fire('change');
    assert.deepEqual(ui.saved().map(item => item.candidateId), [1]);
    assert.equal(ui.rows[1].checkbox.checked, false);
    all.checked = false; all.fire('change');
    assert.deepEqual(ui.saved(), []);
});

test('restores the chosen source on a return visit without losing selections from other pages', () => {
    const ui = fixture({prior: [
        {candidateId: 1, sourceId: 102, name: 'Ứng viên 1', cvLabel: 'CV cũ'},
        {candidateId: 99, sourceId: 9901, name: 'Trang khác', cvLabel: 'CV trang khác'},
    ]});
    assert.equal(ui.rows[0].select.value, '102');
    assert.equal(ui.rows[0].checkbox.checked, true);
    assert.equal(ui.rows[0].preview.dataset.cvUrl, '/talent-pool/1/cv/102');
    const all = ui.byId('tp-select-page');
    all.checked = false; all.fire('change');
    assert.deepEqual(ui.saved().map(item => item.candidateId), [99]);
});

test('when browser storage fails, navigation is guarded instead of silently losing selection', () => {
    const ui = fixture({brokenStorage: true});
    ui.rows[0].checkbox.checked = true; ui.rows[0].checkbox.fire('change');
    assert.equal(ui.nextPage.fire('click').prevented, true);
    assert.equal(ui.filter.fire('submit').prevented, true);
    assert.equal(ui.profileLink.fire('click').prevented, true);
    assert.equal(ui.byId('tp-notes-detail').fire('click').prevented, true);
    assert.equal(ui.byId('app-preview-detail').fire('click').prevented, true);
    ui.byId('tp-clear-selection').fire('click');
    assert.equal(ui.nextPage.fire('click').prevented, false);
});

test('review hydrates ID-only drafts and previews the exact selected CV with a separate submission date', async () => {
    const ui = fixture({prior: [{candidateId: 1, sourceId: 102}, {candidateId: 99, sourceId: 9901}]});
    ui.byId('tp-review-selection').fire('click');
    await new Promise(setImmediate);
    const items = ui.byId('tp-confirm-list').children;
    assert.equal(ui.byId('tp-confirm-title').textContent, 'Xác nhận thêm 2 ứng viên');
    assert.equal(items[1].children[0].children[0].textContent, 'Ứng viên 99');
    assert.match(items[1].children[0].children[2].textContent, /CV từ lần ứng tuyển:/);
    const preview = items[1].children[1].children[0];
    assert.equal(preview.dataset.cvUrl, '/talent-pool/99/cv/9901');
    assert.equal(preview.dataset.detailUrl, '/talent-pool/99?jobPostingId=8&keyword=Java&page=1');
    assert.equal(preview.disabled, false);
    assert.equal(ui.byId('tp-confirm-submit').disabled, false);
    assert.equal(ui.request().headers['X-CSRF-TOKEN'], 'csrf-token');
    assert.equal(ui.request().url, '/talent-pool/jobs/8/selection-review');
    items[1].children[1].children[1].fire('click');
    await new Promise(setImmediate);
    assert.deepEqual(JSON.parse(ui.request().body).sourceApplicationIds, [102]);
    assert.equal(ui.byId('tp-confirm-title').focused, true);
});

test('confirmation saves the exact sources once with CSRF, then clears the draft only after success', async () => {
    const ui = fixture({prior: [{candidateId: 1, sourceId: 102}, {candidateId: 99, sourceId: 9901}]});
    ui.byId('tp-review-selection').fire('click'); await new Promise(setImmediate);
    ui.byId('tp-selection-form').fire('submit');
    ui.byId('tp-selection-form').fire('submit');
    assert.equal(ui.byId('tp-confirm').fire('cancel').prevented, true);
    await new Promise(setImmediate);
    const writes = ui.requests.filter(r => r.url.endsWith('/suggest'));
    assert.equal(writes.length, 1);
    assert.deepEqual(JSON.parse(writes[0].body), {sourceApplicationIds: [102, 9901]});
    assert.equal(writes[0].headers['X-CSRF-TOKEN'], 'csrf-token');
    assert.deepEqual(ui.saved(), []);
    assert.equal(ui.byId('tp-confirm').open, false);
    assert.match(ui.byId('tp-feedback').textContent, /Đã thêm 2 ứng viên/);
    assert.equal(ui.byId('tp-feedback').children[0].href, '/talent-pool/jobs/8/suggestions');
});

test('server rejection preserves draft and exposes retry instead of reporting success', async () => {
    const ui = fixture({prior: [{candidateId: 1, sourceId: 102}], submitFailure: true});
    ui.byId('tp-review-selection').fire('click'); await new Promise(setImmediate);
    ui.byId('tp-selection-form').fire('submit'); await new Promise(setImmediate);
    assert.deepEqual(ui.saved(), [{candidateId: 1, sourceId: 102}]);
    assert.equal(ui.byId('tp-confirm').open, true);
    assert.equal(ui.byId('tp-confirm-submit').disabled, true);
    assert.equal(ui.byId('tp-recheck').hidden, false);
    assert.equal(ui.byId('tp-review-status').children.at(-1).textContent, 'Ứng viên đã có đề xuất.');
    assert.equal(ui.byId('tp-feedback').textContent, '');
});

test('invalid preflight cannot submit and retains all selected sources', async () => {
    const ui = fixture({prior: [{candidateId: 1, sourceId: 102}], invalidSource: true});
    ui.byId('tp-review-selection').fire('click'); await new Promise(setImmediate);
    assert.equal(ui.byId('tp-confirm-submit').disabled, true);
    ui.byId('tp-selection-form').fire('submit'); await new Promise(setImmediate);
    assert.equal(ui.requests.filter(r => r.url.endsWith('/suggest')).length, 0);
    assert.deepEqual(ui.saved(), [{candidateId: 1, sourceId: 102}]);
});

test('HR selection submits recommendations with its own copy and destination without calling HM suggest', async () => {
    const ui = fixture({hrMode: true, prior: [{candidateId: 1, sourceId: 102}]});
    assert.equal(ui.byId('tp-review-selection').textContent, 'Gửi 1 gợi ý cho HM');
    ui.byId('tp-review-selection').fire('click'); await new Promise(setImmediate);
    assert.equal(ui.byId('tp-confirm-submit').textContent, 'Gửi 1 gợi ý cho HM');
    ui.byId('tp-selection-form').fire('submit'); await new Promise(setImmediate);
    const writes = ui.requests.filter(r => r.url.endsWith('/recommend'));
    assert.equal(writes.length, 1); assert.equal(writes[0].headers['X-CSRF-TOKEN'], 'csrf-token');
    assert.deepEqual(JSON.parse(writes[0].body), {sourceApplicationIds: [102]});
    assert.equal(ui.requests.filter(r => r.url.endsWith('/suggest')).length, 0);
    assert.match(ui.byId('tp-feedback').textContent, /Đã gửi 1 gợi ý cho HM/);
    assert.equal(ui.byId('tp-feedback').children[0].href, '/talent-pool/jobs/8/recommendations');
    assert.deepEqual(ui.saved(), []);
});
