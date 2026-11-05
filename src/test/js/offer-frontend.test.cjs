// Run with: node --test src/test/js/offer-frontend.test.cjs
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

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
