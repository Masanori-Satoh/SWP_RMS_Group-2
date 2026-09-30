const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { runInNewContext } = require('node:vm');

function loadScript(name, elements, buttons = []) {
    const document = {
        addEventListener: (_, ready) => ready(),
        getElementById: (id) => elements[id] ?? null,
        querySelectorAll: () => buttons,
    };
    runInNewContext(readFileSync(resolve(__dirname, '../../main/resources/static/js', name), 'utf8'), { document });
}

test('account form updates required department without the removed menu button', () => {
    let changeRole;
    const role = {
        options: [
            { value: '', dataset: {} },
            { value: '1', dataset: { roleName: 'HR' } },
            { value: '2', dataset: { roleName: 'Candidate' } },
        ],
        selectedIndex: 1,
        addEventListener: (_, callback) => { changeRole = callback; },
    };
    const department = {};
    loadScript('account-form.js', { role, department });
    assert.equal(department.required, true);
    role.selectedIndex = 2;
    changeRole();
    assert.equal(department.required, false);
    role.selectedIndex = 0;
    changeRole();
    assert.equal(department.required, false);
});

test('account list opens and cancels deactivation without the removed menu button', () => {
    let deactivate;
    let cancel;
    const dialog = {
        open: false,
        showModal() { this.open = true; },
        close() { this.open = false; },
    };
    const form = {};
    const name = {};
    loadScript('account-list.js', {
        'deactivate-dialog': dialog,
        'deactivate-form': form,
        'deactivate-account-name': name,
        'deactivate-cancel': { addEventListener: (_, callback) => { cancel = callback; } },
    }, [{
        dataset: { accountName: 'Test account', deactivateUrl: '/admin/accounts/7/deactivate' },
        addEventListener: (_, callback) => { deactivate = callback; },
    }]);
    deactivate();
    assert.equal(dialog.open, true);
    assert.equal(name.textContent, 'Test account');
    assert.equal(form.action, '/admin/accounts/7/deactivate');
    cancel();
    assert.equal(dialog.open, false);
});
