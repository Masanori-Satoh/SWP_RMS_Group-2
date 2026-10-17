const { test } = require('node:test');
const assert = require('node:assert/strict');
const Selection = require('../../main/resources/static/js/pages/talent-pool-selection.js');

function storage() {
    const data = new Map();
    return {getItem: key => data.get(key) || null, setItem: (key, value) => data.set(key, value)};
}
const candidate = (candidateId, sourceId = candidateId * 10) => ({candidateId, sourceId, name: `Ứng viên ${candidateId}`, cvLabel: `CV ${sourceId}`});

test('selection survives filter and page reload with the exact candidate/source mapping', () => {
    const tab = storage();
    const firstPage = new Selection(tab, 'hm5:job8');
    firstPage.set(candidate(1, 101));
    const secondPage = new Selection(tab, 'hm5:job8');
    secondPage.set(candidate(2, 202));
    const filteredPage = new Selection(tab, 'hm5:job8');
    assert.deepEqual(filteredPage.values().map(item => [item.candidateId, item.sourceId]), [[1, 101], [2, 202]]);
});

test('changing the source updates one candidate rather than duplicating them', () => {
    const state = new Selection(storage(), 'hm5:job8');
    state.set(candidate(1, 101));
    state.set(candidate(1, 102));
    assert.equal(state.size, 1);
    assert.equal(state.get(1).sourceId, 102);
});

test('can remove a candidate selected on another page and clear the whole draft', () => {
    const tab = storage();
    const state = new Selection(tab, 'hm5:job8');
    state.set(candidate(1)); state.set(candidate(2));
    const laterPage = new Selection(tab, 'hm5:job8');
    laterPage.remove(1);
    assert.deepEqual(laterPage.values().map(item => item.candidateId), [2]);
    laterPage.clear();
    assert.equal(new Selection(tab, 'hm5:job8').size, 0);
});

test('drafts are isolated by account and destination job', () => {
    const tab = storage();
    new Selection(tab, 'hm5:job8').set(candidate(1));
    assert.equal(new Selection(tab, 'hm6:job8').size, 0);
    assert.equal(new Selection(tab, 'hm5:job9').size, 0);
});

test('enforces the existing 20-candidate limit without losing prior selections', () => {
    const tab = storage();
    const state = new Selection(tab, 'hm5:job8');
    for (let id = 1; id <= 20; id++) state.set(candidate(id));
    assert.throws(() => state.set(candidate(21)), /20/);
    assert.equal(state.size, 20);
    state.set(candidate(1, 999));
    assert.equal(new Selection(tab, 'hm5:job8').get(1).sourceId, 999);
});

test('rejects invalid IDs and ignores corrupt stored entries', () => {
    const tab = storage();
    tab.setItem('draft', JSON.stringify([candidate(1), candidate(-2), {...candidate(3), sourceId: null}]));
    const state = new Selection(tab, 'draft');
    assert.equal(state.size, 1);
    assert.throws(() => state.set(candidate(0)));
    tab.setItem('broken', '{');
    assert.equal(new Selection(tab, 'broken').size, 0);
});

test('storage failure is explicit and keeps the in-page selection intact', () => {
    const tab = {getItem: () => null, setItem: () => { throw new Error('Quota exceeded'); }};
    const state = new Selection(tab, 'hm5:job8');
    state.set(candidate(1));
    assert.equal(state.persistenceAvailable, false);
    assert.equal(state.get(1).sourceId, 10);
});

test('returned objects cannot accidentally change a persisted selection', () => {
    const state = new Selection(storage(), 'hm5:job8');
    const input = candidate(1, 101);
    state.set(input);
    input.sourceId = 999;
    state.get(1).sourceId = 998;
    state.values()[0].sourceId = 997;
    assert.equal(state.get(1).sourceId, 101);
});

test('persistent drafts contain only identifiers, including after migrating older drafts', () => {
    const tab = storage();
    tab.setItem('draft', JSON.stringify([{...candidate(1), email: 'private@example.com', notes: 'Private note'}]));
    const state = new Selection(tab, 'draft');
    assert.deepEqual(JSON.parse(tab.getItem('draft')), [{candidateId: 1, sourceId: 10}]);
    state.set(candidate(2, 205));
    assert.deepEqual(JSON.parse(tab.getItem('draft')), [{candidateId: 1, sourceId: 10}, {candidateId: 2, sourceId: 205}]);
    const reload = new Selection(tab, 'draft');
    assert.equal(reload.get(2).name, '');
    assert.equal(reload.get(2).sourceId, 205);
});
