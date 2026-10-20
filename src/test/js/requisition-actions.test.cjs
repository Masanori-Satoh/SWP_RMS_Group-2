const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { runInNewContext } = require('node:vm');

function fixture() {
    const handlers = new Map();
    const listen = (key, event, callback) => {
        const name = key + ':' + event;
        handlers.set(name, [...(handlers.get(name) || []), callback]);
    };
    const fire = (key, event, payload = {}) => handlers.get(key + ':' + event)?.forEach(callback => callback(payload));
    const cancel = { focus() { this.focused = true; }, addEventListener: (event, callback) => listen('cancel', event, callback) };
    const dialog = {
        open: false,
        showModal() { this.open = true; },
        close() { this.open = false; fire('dialog', 'close'); },
        querySelector() { return cancel; },
        addEventListener: (event, callback) => listen('dialog', event, callback),
    };
    const toggle = { focus() { this.focused = true; } };
    const details = { open: true, querySelector() { return toggle; }, removeAttribute() { this.open = false; } };
    const button = {
        dataset: { title: 'Vị trí thử', deleteUrl: '/requisitions/delete/7' },
        closest() { return details; },
        focus() { this.focused = true; },
        addEventListener: (event, callback) => listen('button', event, callback),
    };
    const form = {
        dataset: {},
        addEventListener: (event, callback) => listen('form', event, callback),
        setAttribute(name, value) { this[name] = value; },
        removeAttribute(name) { delete this[name]; },
    };
    const decision = {
        dataset: {},
        addEventListener: (event, callback) => listen('decision', event, callback),
        querySelectorAll() { return []; },
        setAttribute(name, value) { this[name] = value; },
        removeAttribute(name) { delete this[name]; },
    };
    const feedback = {
        value: '',
        setCustomValidity(value) { this.validationMessage = value; },
        reportValidity() { this.reported = true; },
        addEventListener: (event, callback) => listen('feedback', event, callback),
    };
    const ids = { 'delete-dialog': dialog, 'delete-name': {}, 'delete-form': form, 'decision-form': decision, 'decision-comment': feedback };
    const document = {
        addEventListener: (event, callback) => event === 'DOMContentLoaded' && callback(),
        getElementById: id => ids[id] ?? null,
        querySelectorAll: selector => {
            if (selector === '[data-delete-requisition]') return [button];
            if (selector === '#delete-form,#decision-form,.withdraw-form') return [form, decision];
            if (selector === '[data-submitting]') return [form, decision].filter(item => item.dataset.submitting);
            return [];
        },
    };
    const window = { addEventListener: (event, callback) => listen('window', event, callback) };
    runInNewContext(readFileSync(resolve(__dirname, '../../main/resources/static/js/pages/requisition-actions.js'), 'utf8'), { document, window });
    const submit = (target, value) => {
        const event = {
            submitter: { value },
            defaultPrevented: false,
            preventDefault() { this.defaultPrevented = true; },
        };
        fire(target, 'submit', event);
        return event;
    };
    return { fire, submit, ids, dialog, details, button, toggle, cancel, form, decision, feedback };
}

test('delete action uses the selected requisition and restores focus after cancel', () => {
    const ui = fixture();
    ui.fire('button', 'click');
    assert.equal(ui.dialog.open, true);
    assert.equal(ui.details.open, false);
    assert.equal(ui.ids['delete-name'].textContent, 'Vị trí thử');
    assert.equal(ui.form.action, '/requisitions/delete/7');
    assert.equal(ui.cancel.focused, true);
    ui.fire('cancel', 'click');
    assert.equal(ui.dialog.open, false);
    assert.equal(ui.toggle.focused, true);
});

test('blank rejection is blocked, then a valid decision can submit only once', () => {
    const ui = fixture();
    const invalid = ui.submit('decision', 'reject');
    assert.equal(invalid.defaultPrevented, true);
    assert.equal(ui.feedback.reported, true);
    assert.equal(ui.decision.dataset.submitting, undefined);

    ui.feedback.value = 'Thiếu thông tin';
    ui.fire('feedback', 'input');
    assert.equal(ui.feedback.validationMessage, '');
    assert.equal(ui.submit('decision', 'reject').defaultPrevented, false);
    assert.equal(ui.decision.dataset.submitting, 'true');
    assert.equal(ui.submit('decision', 'reject').defaultPrevented, true);

    ui.fire('window', 'pageshow');
    assert.equal(ui.decision.dataset.submitting, undefined);
    assert.equal(ui.decision['aria-busy'], undefined);
});