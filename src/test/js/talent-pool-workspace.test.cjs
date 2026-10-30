const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {resolve} = require('node:path');
const {runInNewContext} = require('node:vm');
const source = readFileSync(resolve(__dirname, '../../main/resources/static/js/pages/talent-pool-workspace.js'), 'utf8');
function fixture(options) {
    const caption = {textContent: ''}, date = {textContent: ''}, summary = {};
    const picker = {querySelector: key => ({'[data-tpw-source-caption]': caption, '[data-tpw-source-date]': date, summary})[key]};
    let handler;
    const select = {value: '101', selectedOptions: options, closest: () => picker, addEventListener: (name, fn) => { assert.equal(name, 'change'); handler = fn; }};
    runInNewContext(source, {document: {querySelector: () => ({querySelectorAll: () => [select]})}});
    return {caption, date, summary, select, change: () => handler()};
}
test('long CV title and date stay complete, without changing the selected source', () => {
    const title = 'QA Automation Engineer (Playwright / Selenium / API) — Nền tảng quản trị tuyển dụng';
    const ui = fixture([{dataset: {jobTitle: title, submittedDate: '28/10/2026'}, textContent: title}]);
    assert.equal(ui.caption.textContent, title); assert.equal(ui.date.textContent, '28/10/2026'); assert.equal(ui.select.value, '101');
});
test('changing the native source selector refreshes both visible metadata and full hover label', () => {
    const ui = fixture([{dataset: {jobTitle: 'CV trước', submittedDate: '01/10/2026'}, textContent: 'CV trước'}]);
    ui.select.selectedOptions = [{dataset: {jobTitle: 'CV đã chọn', submittedDate: '28/10/2026'}, textContent: 'CV đã chọn'}];
    ui.change(); assert.equal(ui.caption.textContent, 'CV đã chọn'); assert.equal(ui.summary.title, 'CV đã chọn · 28/10/2026');
});
test('CV metadata is written as text, including markup-like titles', () => {
    const title = '<img src=x onerror=alert(1)>';
    const ui = fixture([{dataset: {jobTitle: title}, textContent: title}]);
    assert.equal(ui.caption.textContent, title); assert.equal(ui.caption.innerHTML, undefined); assert.equal(ui.date.textContent, 'Chưa có ngày nộp');
});
test('missing option or a non-Talent Pool page does not break existing UI', () => {
    const ui = fixture([]); assert.equal(ui.caption.textContent, '');
    assert.doesNotThrow(() => runInNewContext(source, {document: {querySelector: () => null}}));
});
