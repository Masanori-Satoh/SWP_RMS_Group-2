/* A per-tab draft only. This store never represents a successful server submission. */
(() => {
    'use strict';
    class TalentPoolSelection {
        constructor(storage, key, limit = 20) {
            this.storage = storage;
            this.key = key;
            this.limit = limit;
            this.items = new Map();
            this.persistenceAvailable = Boolean(storage && key);
            if (!this.persistenceAvailable) return;
            try {
                const draft = JSON.parse(storage.getItem(key) || '[]');
                if (!Array.isArray(draft)) return;
                draft.slice(0, limit).filter(TalentPoolSelection.valid).forEach(item => {
                    this.items.set(item.candidateId, TalentPoolSelection.copy(item));
                });

                this.persist();
            } catch (error) {
                if (!(error instanceof SyntaxError)) this.persistenceAvailable = false;
            }
        }
        static valid(item) {
            return item && Number.isSafeInteger(item.candidateId) && item.candidateId > 0
                && Number.isSafeInteger(item.sourceId) && item.sourceId > 0;
        }
        static copy(item) {
            return {candidateId: item.candidateId, sourceId: item.sourceId,
                name: typeof item.name === 'string' ? item.name : '',
                cvLabel: typeof item.cvLabel === 'string' ? item.cvLabel : ''};
        }
        set(item) {
            if (!TalentPoolSelection.valid(item)) throw new Error('Không xác định được ứng viên và CV nguồn.');
            if (!this.items.has(item.candidateId) && this.items.size >= this.limit)
                throw new Error(`Mỗi lần chọn tối đa ${this.limit} ứng viên.`);
            this.items.set(item.candidateId, TalentPoolSelection.copy(item));
            this.persist();
        }
        get(candidateId) {
            const item = this.items.get(candidateId);
            return item ? TalentPoolSelection.copy(item) : null;
        }
        remove(candidateId) { this.items.delete(candidateId); this.persist(); }
        clear() { this.items.clear(); this.persist(); }
        values() { return [...this.items.values()].map(TalentPoolSelection.copy); }
        get size() { return this.items.size; }
        persist() {
            if (!this.persistenceAvailable) return;
            try {
                this.storage.setItem(this.key, JSON.stringify(this.values().map(item => ({
                    candidateId: item.candidateId, sourceId: item.sourceId
                }))));
            }
            catch { this.persistenceAvailable = false; }
        }
    }
    globalThis.TalentPoolSelection = TalentPoolSelection;
    if (typeof module !== 'undefined' && module.exports) module.exports = TalentPoolSelection;
})();
