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

test('status confirmation cancels safely and submits the original scoped form exactly once', () => {
    const handlers = {};
    const trigger = { focus() { this.focused = true; } };
    const dialog = {
        open: false,
        showModal() { this.open = true; },
        close() { this.open = false; handlers.close(); },
        addEventListener: (name, callback) => { handlers[name] = callback; },
    };
    const cancel = { focus() { this.focused = true; }, addEventListener: (_, callback) => { handlers.cancel = callback; } };
    const confirm = { addEventListener: (_, callback) => { handlers.confirm = callback; } };
    const title = {};
    const description = {};
    let submits = 0;
    let submit;
    const form = {
        action: '/admin/candidate-accounts/7/activate',
        csrf: 'test-token',
        dataset: { confirmTitle: 'Restore test account', confirmMessage: 'Preserve history.', confirmLabel: 'Restore', confirmDanger: 'false' },
        querySelector: () => trigger,
        addEventListener: (_, callback) => { submit = callback; },
        requestSubmit(button) {
            let prevented = false;
            submit({ submitter: button, preventDefault() { prevented = true; } });
            if (!prevented) submits++;
        },
    };
    loadScript('status-confirmation.js', {
        'status-dialog': dialog, 'status-title': title, 'status-description': description,
        'status-cancel': cancel, 'status-confirm': confirm,
    }, [form]);
    const request = () => submit({ submitter: trigger, preventDefault() {} });
    request();
    assert.equal(dialog.open, true);
    assert.equal(cancel.focused, true);
    assert.equal(title.textContent, 'Restore test account');
    handlers.cancel();
    assert.equal(submits, 0);
    assert.equal(trigger.focused, true);
    request(); handlers.confirm(); handlers.confirm();
    assert.equal(submits, 1);
    assert.equal(form.action, '/admin/candidate-accounts/7/activate');
    assert.equal(form.csrf, 'test-token');
    assert.equal(dialog.open, false);
});
