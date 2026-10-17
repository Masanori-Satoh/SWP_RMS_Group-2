const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {resolve} = require('node:path');
const {runInNewContext} = require('node:vm');

function fixture(contentType = 'application/pdf') {
    const handlers = {};
    const frame = {removeAttribute() {}, hidden: true};
    const preview = {open: false, classList: {remove() {}},
        addEventListener(name, handler) { handlers[name] = handler; }, showModal() { this.open = true; }};
    const ids = {'app-preview': preview, 'app-preview-frame': frame,
        'app-preview-title': {}, 'app-preview-job': {}, 'app-preview-status': {},
        'app-preview-original': {}, 'app-preview-detail': {}};
    const page = {querySelectorAll: () => [], contains: () => true,
        addEventListener(name, handler) { handlers['page:' + name] = handler; }};
    let requested;
    runInNewContext(readFileSync(resolve(__dirname, '../../main/resources/static/js/pages/applications.js'), 'utf8'), {
        document: {querySelector: () => page, getElementById: id => ids[id] || null},
        URL: {createObjectURL: () => 'blob:test-pdf', revokeObjectURL() {}},
        fetch: async (url) => { requested = url; return {ok: true, headers: {get: () => contentType}, blob: async () => ({})}; },
        setTimeout, clearTimeout, AbortController,
    });
    return {ids, frame, requested: () => requested,
        click: async (dataset) => { const button = {dataset}; return handlers['page:click']({target: {closest: () => button}}); }};
}

test('delegated CV preview supports dynamically created confirmation buttons and preserves the selected source', async () => {
    const ui = fixture();
    await ui.click({name: 'Ứng viên', job: 'CV nguồn thứ hai', cvUrl: '/talent-pool/15/cv/102', detailUrl: '/talent-pool/15?page=1'});
    assert.equal(ui.requested(), '/talent-pool/15/cv/102');
    assert.equal(ui.ids['app-preview-job'].textContent, 'CV nguồn thứ hai');
    assert.equal(ui.frame.hidden, false);
    assert.equal(ui.frame.src, 'blob:test-pdf#view=FitH&navpanes=0');
    assert.equal(ui.ids['app-preview-detail'].hidden, false);
});

test('profile preview hides the self-link, while a later list preview restores it', async () => {
    const ui = fixture();
    await ui.click({name: 'Ứng viên', cvUrl: '/talent-pool/15/cv/101', hideDetailLink: 'true'});
    assert.equal(ui.ids['app-preview-detail'].hidden, true);
    await ui.click({name: 'Ứng viên', cvUrl: '/applications/101/cv', detailUrl: '/applications/101'});
    assert.equal(ui.ids['app-preview-detail'].hidden, false);
    assert.equal(ui.requested(), '/applications/101/cv');
});

test('unsupported preview keeps the fallback and explains the problem', async () => {
    const ui = fixture('text/html');
    await ui.click({name: 'Ứng viên', cvUrl: '/talent-pool/15/cv/101'});
    assert.equal(ui.frame.hidden, true);
    assert.match(ui.ids['app-preview-status'].textContent, /không hỗ trợ xem nhanh PDF/);
    assert.equal(ui.ids['app-preview-original'].href, '/talent-pool/15/cv/101');
});
