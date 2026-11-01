const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {resolve} = require('node:path');
const {runInNewContext} = require('node:vm');
const script = readFileSync(resolve(__dirname, '../../main/resources/static/js/pages/potential-candidate-toggle.js'), 'utf8');

function fixture({saved = true, fail = false, confirmed = true, keepSource = true} = {}) {
    let click, finish;
    const request = new Promise(resolve => { finish = resolve; });
    const calls = [], redirects = [];
    const label = {textContent: saved ? 'Bỏ lưu tiềm năng' : 'Thêm vào kho tiềm năng'};
    const feedback = {hidden: true};
    const button = {closest: () => ({querySelector: () => feedback}), dataset: {name: 'Candidate A', saved: saved == null ? undefined : String(saved), sourceId: keepSource ? '218' : undefined, bookmarkUrl: '/talent-pool/applications/218/bookmark'},
        disabled: false, attributes: {}, querySelector: () => label,
        setAttribute(k, v) {this.attributes[k] = v;}, removeAttribute(k) {delete this.attributes[k];},
        addEventListener(_, handler) {click = handler;}};
    const document = {querySelector(selector) {
        return ({'[data-potential-toggle]': button, '[data-potential-toggle-feedback]': feedback,
            'meta[name="_csrf"]': {content: 'csrf-token'}, 'meta[name="_csrf_header"]': {content: 'X-CSRF-TOKEN'}})[selector] || null;
    }, querySelectorAll: selector => selector === '[data-potential-toggle]' ? [button] : [], addEventListener() {}};
    runInNewContext(script, {document, URL, URLSearchParams, AbortSignal,
        window: {location: {href: 'http://localhost/talent-pool/8?keyword=Java', replace: url => redirects.push(url)}, addEventListener() {}},
        fetch: async (url, options) => {
            calls.push({url, options}); await request;
            return {ok: !fail, headers: {get: () => 'application/json'},
                json: async () => fail ? {message: 'Bạn không có quyền.'} : {saved: !options.method ? true : (confirmed ? !saved : saved)}};
        }});
    return {button, label, feedback, calls, redirects, click: () => click(), finish};
}

test('profile removal prevents duplicate writes and preserves the authorized reload context', async () => {
    const ui = fixture();
    const pending = ui.click(); await ui.click();
    assert.equal(ui.calls.length, 1);
    assert.equal(ui.button.disabled, true);
    assert.equal(ui.calls[0].url, '/talent-pool/applications/218/bookmark/remove');
    assert.equal(ui.calls[0].options.headers['X-CSRF-TOKEN'], 'csrf-token');
    ui.finish(); await pending;
    const destination = new URL(ui.redirects[0]);
    assert.equal(destination.searchParams.get('sourceApplicationId'), '218');
    assert.equal(destination.searchParams.get('keyword'), 'Java');
});

test('profile can add a removed candidate back without creating a note', async () => {
    const ui = fixture({saved: false}); const pending = ui.click(); ui.finish(); await pending;
    assert.equal(ui.calls[0].url, '/talent-pool/applications/218/bookmark');
    assert.equal(ui.calls[0].options.body.get('content'), '');
    assert.equal(ui.redirects.length, 1);
});

test('failed or unconfirmed writes retain the current state and allow recovery', async () => {
    for (const options of [{fail: true}, {confirmed: false}]) {
        const ui = fixture(options); const pending = ui.click(); ui.finish(); await pending;
        assert.equal(ui.redirects.length, 0);
        assert.equal(ui.button.disabled, false);
        assert.equal(ui.button.dataset.saved, 'true');
        assert.equal(ui.label.textContent, 'Bỏ lưu tiềm năng');
        assert.equal(ui.feedback.hidden, false);
        assert.ok(ui.feedback.textContent);
    }
});

test('interview action reads its state before enabling any write', async () => {
    const ui = fixture({saved: null, keepSource: false});
    await ui.click();
    assert.equal(ui.calls.length, 1);
    assert.equal(ui.calls[0].options.method, undefined);
    assert.equal(ui.button.disabled, true);
    ui.finish();
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(ui.button.dataset.saved, 'true');
    assert.equal(ui.button.disabled, false);
    assert.equal(ui.redirects.length, 0);
});

test('application row reload keeps list filters without introducing profile context', async () => {
    const ui = fixture({keepSource: false});
    const pending = ui.click(); ui.finish(); await pending;
    assert.equal(new URL(ui.redirects[0]).searchParams.has('sourceApplicationId'), false);
    assert.equal(new URL(ui.redirects[0]).searchParams.get('keyword'), 'Java');
});
