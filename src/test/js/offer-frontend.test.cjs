// Run with: node --test src/test/js/offer-frontend.test.cjs
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

function loadForm(proposed = '', probation = '') {
    const fields = { proposedSalary: { value: proposed }, probationSalary: { value: probation },
        offeredPositionTitle: { value: 'Entered title' }, workLocation: { value: 'Entered office' },
        groupASection: { hidden: true } };
    const option = { value: '42', getAttribute: name => ({ 'data-recom-salary': '12345678.90',
        'data-pos': 'Suggested title' })[name] || '' };
    fields.createAppSelect = { value: '42', selectedIndex: 0, options: [option] };
    const context = { document: { readyState: 'loading', addEventListener() {},
        getElementById: id => fields[id] || null }, window: {} };
    vm.createContext(context);
    vm.runInContext(fs.readFileSync('src/main/resources/static/js/pages/offers/offer-form.js', 'utf8'), context);
    return { context, fields };
}

test('form reinitialization preserves entered salaries and text', () => {
    const { context, fields } = loadForm('20000000.25', '19000000.75');
    context.initOfferForm();
    assert.equal(fields.proposedSalary.value, '20000000.25');
    assert.equal(fields.probationSalary.value, '19000000.75');
    assert.equal(fields.offeredPositionTitle.value, 'Entered title');
    assert.equal(fields.workLocation.value, 'Entered office');
    assert.equal(fields.groupASection.hidden, false);
});

test('candidate suggestions fill blanks and preserve decimal recommended salary', () => {
    const { context, fields } = loadForm('', '');
    context.handleSelectPassedCandidate('42');
    assert.equal(Number(fields.proposedSalary.value), 12345678.9);
    assert.ok(Number(fields.probationSalary.value) >= 12345678.9 * 0.85);
    fields.proposedSalary.value = '21000000';
    context.handleSelectPassedCandidate('42');
    assert.equal(fields.proposedSalary.value, '21000000');
});

for (const salary of [10000004, 12.34, 0.01, 25000000.25]) {
    test(`probation suggestion never rounds below 85%: ${salary}`, () => {
        const { context, fields } = loadForm(String(salary), '');
        context.autoCalculateProbationSalary();
        assert.ok(Number(fields.probationSalary.value) >= salary * 0.85);
        assert.ok(Number(fields.probationSalary.value) <= salary);
    });
}

function load(fetch) {
    const context = { fetch, console, document: {
        readyState: 'loading', addEventListener() {},
        querySelector(selector) {
            return { getAttribute: () => selector.includes('_csrf_header') ? 'X-TEST-CSRF' : 'test-token' };
        }
    } };
    context.window = context;
    vm.createContext(context);
    vm.runInContext(fs.readFileSync('src/main/resources/static/js/pages/offers/offer-common.js', 'utf8'), context);
    return context;
}

function response(status, data, type = 'application/json') {
    return { ok: status >= 200 && status < 300, status,
        headers: { get: () => type }, json: async () => data };
}

for (const scope of ['FILTERED', 'SELECTED', 'ALL']) {
    test(`export ${scope} uses applied URL filters, not unfinished toolbar edits`, async () => {
        let payload;
        const context = load(async (url, options) => {
            payload = JSON.parse(options.body);
            return response(400, { message: 'Stop after recording request' });
        });
        context.URLSearchParams = URLSearchParams;
        context.location = { search: '?search=Java%20%26%20API&status=Draft&timeSort=LATEST&page=2' };
        context.document.getElementById = id => ({ searchInput: { value: 'Unsubmitted keyword' },
            statusFilter: { value: 'Accepted' }, timeSortFilter: { value: 'EARLIEST' } })[id] || null;
        context.document.querySelector = selector => selector.includes('exportScope') ? { value: scope } : null;
        context.document.querySelectorAll = selector => selector.includes('exportCol')
            ? [{ value: 'offerId' }] : [{ value: '42' }];
        vm.runInContext(fs.readFileSync('src/main/resources/static/js/pages/offers/offer-hr.js', 'utf8'), context);
        context.executeOfferExport();
        await new Promise(setImmediate);
        assert.equal(payload.scope, scope);
        assert.deepEqual(payload.filters, scope === 'FILTERED'
            ? { keyword: 'Java & API', status: 'Draft', sort: 'LATEST' } : null);
        assert.deepEqual(payload.offerIds, scope === 'SELECTED' ? [42] : []);
    });
}

test('JSON actions preserve custom CSRF header, body and returned data', async () => {
    let request;
    const context = load(async (url, options) => {
        request = { url, ...options };
        return response(200, { success: true, data: { emailStatus: 'NOT_SENT' } });
    });
    const result = await context.offerRequest('/offers/42/approve', { body: { comments: 'Approved' } });
    assert.equal(request.url, '/offers/42/approve');
    assert.equal(request.method, 'POST');
    assert.equal(request.headers['X-TEST-CSRF'], 'test-token');
    assert.equal(request.headers['Content-Type'], 'application/json');
    assert.deepEqual(JSON.parse(request.body), { comments: 'Approved' });
    assert.equal(result.data.emailStatus, 'NOT_SENT');
});

test('DELETE actions do not send a JSON body or content type', async () => {
    const context = load(async (url, options) => {
        assert.equal(options.method, 'DELETE');
        assert.equal(options.body, undefined);
        assert.equal(options.headers['Content-Type'], undefined);
        return response(200, { success: true });
    });
    await context.offerRequest('/offers/42', { method: 'DELETE' });
});

for (const [name, status, data, type, expected] of [
    ['permission denied', 403, null, 'text/html', /không có quyền/],
    ['server failure', 500, { message: 'Server failed' }, 'application/json', /Server failed/],
    ['business conflict', 200, { success: false, message: 'Already sent' }, 'application/json', /Already sent/],
    ['login HTML instead of JSON', 200, null, 'text/html', /không thành công/]
]) {
    test(name, async () => {
        const context = load(async () => response(status, data, type));
        await assert.rejects(context.offerRequest('/offers/42/send'), expected);
    });
}

test('malformed JSON is rejected without reporting success', async () => {
    const context = load(async () => ({ ...response(200), json: async () => { throw new SyntaxError(); } }));
    await assert.rejects(context.offerRequest('/offers/42/send'), /không thành công/);
});

test('network errors return a Vietnamese recovery message', async () => {
    const context = load(async () => { throw new TypeError('Failed to fetch'); });
    await assert.rejects(context.offerRequest('/offers/42/send'), /Không thể kết nối/);
});

for (const action of ['send', 'delete', 'approve']) {
    for (const success of [true, false]) {
        test(`${action}: ${success ? 'success reloads once' : 'failure keeps the page and unlocks the button'}`, async () => {
            let requests = 0, navigations = 0;
            const messages = [];
            const context = load(async () => {
                requests++;
                return response(success ? 200 : 403,
                    { success, message: success ? 'Completed' : 'Denied', data: { emailStatus: 'NOT_SENT' } });
            });
            const dialog = { dataset: { offerId: '42' }, close() {}, showModal() {} };
            const button = { disabled: false, innerHTML: 'Confirm', closest: () => dialog };
            context.document.getElementById = id => id === 'btnConfirmSendOfferModal' ? button : dialog;
            context.window.location = { reload() { navigations++; } };
            context.showFlashToast = (message, type) => messages.push({ message, type });
            context.queueOfferToast = (message, type) => messages.push({ message, type });
            if (action === 'send') {
                context.confirmSendOffer(42);
                context.executeSendOfferAction();
            } else {
                const file = action === 'delete' ? 'offer-hr.js' : 'offer-director.js';
                vm.runInContext(fs.readFileSync('src/main/resources/static/js/pages/offers/' + file, 'utf8'), context);
                if (action === 'delete') context.executeDeleteOffer(button);
                else await context.executeDirectorDecisionApi('/offers/42/approve', 'Approved', () => navigations++);
            }
            await new Promise(setImmediate);
            assert.equal(requests, 1);
            assert.equal(navigations, success ? 1 : 0);
            assert.equal(button.disabled, false);
            assert.equal(messages.at(-1).message, success ? 'Completed' : 'Denied');
            if (success && action === 'send') assert.equal(messages.at(-1).type, 'warning');
        });
    }
}
