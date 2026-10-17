const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {resolve} = require('node:path');
const {runInNewContext} = require('node:vm');

function fixture() {
    const handlers = new Map(), pageHandlers = new Map(), children = [];
    const buttons = [{value: 'Fail', textContent: 'Từ chối'}, {value: 'Pass', textContent: 'Chấp nhận'}];
    const form = {
        busy: false, querySelectorAll: () => buttons,
        hasAttribute: name => name === 'data-pool-decision',
        addEventListener: (name, handler) => handlers.set(name, handler),
        setAttribute: () => { form.busy = true; }, removeAttribute: () => { form.busy = false; },
        append: input => { children.push(input); },
        querySelector: () => children[0] || null,
    };
    const document = {getElementById: () => null, querySelectorAll: selector => selector === '[data-pool-withdraw-dialog]' ? [] : [form], createElement: () => ({dataset: {}, remove() { children.splice(children.indexOf(this), 1); }})};
    runInNewContext(readFileSync(resolve(__dirname, '../../main/resources/static/js/pages/talent-pool-recommendations.js'), 'utf8'),
        {document, window: {addEventListener: (name, handler) => pageHandlers.set(name, handler)}});
    return {buttons, children, form, pageHandlers, submit(button) {
        const event = {submitter: button, prevented: false, preventDefault() { this.prevented = true; }};
        handlers.get('submit')(event); return event;
    }};
}
test('accept and reject preserve the chosen decision in a hidden field before disabling buttons', () => {
    for (let index = 0; index < 2; index++) {
        const ui = fixture(); const event = ui.submit(ui.buttons[index]);
        assert.equal(event.prevented, false); assert.equal(ui.form.busy, true);
        assert.equal(ui.children[0].name, 'decision'); assert.equal(ui.children[0].value, index ? 'Pass' : 'Fail');
        assert.ok(ui.buttons.every(button => button.disabled));
    }
});
test('double submit is blocked and a browser history return restores a usable form', () => {
    const ui = fixture(); ui.submit(ui.buttons[1]);
    assert.equal(ui.submit(ui.buttons[0]).prevented, true); assert.equal(ui.children.length, 1);
    ui.pageHandlers.get('pageshow')();
    assert.equal(ui.children.length, 0); assert.equal(ui.form.busy, false);
    assert.equal(ui.buttons[1].textContent, 'Chấp nhận'); assert.equal(ui.buttons[1].disabled, false);
    assert.equal(ui.submit(ui.buttons[0]).prevented, false);
});
test('an invalid or missing submitter cannot send an accidental acceptance', () => {
    const ui = fixture(); assert.equal(ui.submit(undefined).prevented, true);
    assert.equal(ui.submit({value:'Invalid'}).prevented, true); assert.equal(ui.children.length,0);
});

test('withdraw names the candidate, cancels without a write and submits the original CSRF form once', () => {
    const handlers = {}, dialogHandlers = {}, confirmHandlers = {}, cancelHandlers = {};
    let opened = 0, focused = 0, submitted = 0;
    const form = {requestSubmit: () => submitted++};
    const button = {dataset: {name:'An Võ'}, addEventListener: (name, cb) => handlers[name] = cb,
        focus: () => focused++, closest: selector => { assert.equal(selector,'form'); return form; }};
    const confirm = {disabled:false, addEventListener: (name, cb) => confirmHandlers[name] = cb};
    const cancel = {addEventListener: (name, cb) => cancelHandlers[name] = cb};
    const description = {};
    const dialog = {showModal: () => opened++, addEventListener: (name, cb) => dialogHandlers[name] = cb,
        close: () => dialogHandlers.close(), querySelector: selector => selector === '[data-pool-withdraw-confirm]' ? confirm
            : selector === '[data-pool-withdraw-cancel]' ? cancel : description};
    const document = {querySelectorAll: selector => selector === '[data-pool-withdraw-dialog]' ? [button] : [], getElementById: () => dialog};
    runInNewContext(readFileSync(resolve(__dirname, '../../main/resources/static/js/pages/talent-pool-recommendations.js'), 'utf8'), {document});
    assert.equal(opened,0); handlers.click(); assert.equal(opened,1); assert.match(description.textContent,/An Võ/);
    cancelHandlers.click(); assert.equal(submitted,0); assert.equal(focused,1);
    handlers.click(); confirmHandlers.click(); confirmHandlers.click();
    assert.equal(submitted,1); assert.equal(focused,2); assert.equal(confirm.disabled,true);
});
