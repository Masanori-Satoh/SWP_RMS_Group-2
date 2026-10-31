const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {resolve} = require('node:path');
const {runInNewContext} = require('node:vm');
const source = readFileSync(resolve(__dirname, '../../main/resources/static/js/pages/talent-pool-workspace.js'), 'utf8');
function fixture(options) {
    let handler;
    const select = {title: '', selectedOptions: options, addEventListener: (name, fn) => { assert.equal(name, 'change'); handler = fn; }};
    runInNewContext(source, {document: {querySelector: () => ({querySelectorAll: selector => {
        assert.equal(selector, '.tpw-toolbar select'); return [select]; }})}});
    return {select, change: () => handler()};
}
test('a long selected job title stays available as the hover label of its filter', () => {
    const title = 'QA Automation Engineer (Playwright / Selenium / API) — Nền tảng quản trị tuyển dụng';
    const ui = fixture([{textContent: `  ${title}  `}]);
    assert.equal(ui.select.title, title);
});
test('changing a filter refreshes its hover label', () => {
    const ui = fixture([{textContent: 'Tất cả'}]);
    ui.select.selectedOptions = [{textContent: 'Chuyên viên Phát triển Kinh doanh B2B'}];
    ui.change(); assert.equal(ui.select.title, 'Chuyên viên Phát triển Kinh doanh B2B');
});
test('a missing option or a non-Talent Pool page does not break existing UI', () => {
    const ui = fixture([]); assert.equal(ui.select.title, '');
    assert.doesNotThrow(() => runInNewContext(source, {document: {querySelector: () => null}}));
});
