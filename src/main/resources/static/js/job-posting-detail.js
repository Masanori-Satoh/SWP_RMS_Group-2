/**
 * JOB-POSTING-DETAIL.JS
 * Client-side behaviors for Internal Job Posting Detail View.
 * Handles dropdown menus and delete confirmation dialog safely without inline JS.
 */

document.addEventListener('DOMContentLoaded', function () {
    const moreBtn = document.getElementById('jpMoreBtn');
    const moreDropdown = document.getElementById('jpMoreDropdown');
    const deleteModal = document.getElementById('jpDeleteModal');
    const deleteBtn = document.getElementById('jpTriggerDeleteBtn');
    const cancelDeleteBtn = document.getElementById('jpCancelDeleteBtn');
    const closeDeleteBtn = document.getElementById('jpCloseDeleteBtn');

    // Toggle ⋮ more options dropdown
    if (moreBtn && moreDropdown) {
        moreBtn.addEventListener('click', function (e) {
            e.stopPropagation();
            const isHidden = moreDropdown.hasAttribute('hidden');
            if (isHidden) {
                moreDropdown.removeAttribute('hidden');
            } else {
                moreDropdown.setAttribute('hidden', '');
            }
        });

        // Close dropdown when clicking outside
        document.addEventListener('click', function (e) {
            if (!moreDropdown.contains(e.target) && e.target !== moreBtn) {
                moreDropdown.setAttribute('hidden', '');
            }
        });
    }

    // Modal helpers
    function openModal() {
        if (deleteModal) {
            deleteModal.removeAttribute('hidden');
        }
        if (moreDropdown) {
            moreDropdown.setAttribute('hidden', '');
        }
    }

    function closeModal() {
        if (deleteModal) {
            deleteModal.setAttribute('hidden', '');
        }
    }

    if (deleteBtn) {
        deleteBtn.addEventListener('click', openModal);
    }

    if (cancelDeleteBtn) {
        cancelDeleteBtn.addEventListener('click', closeModal);
    }

    if (closeDeleteBtn) {
        closeDeleteBtn.addEventListener('click', closeModal);
    }

    if (deleteModal) {
        deleteModal.addEventListener('click', function (e) {
            if (e.target === deleteModal) {
                closeModal();
            }
        });
    }
});
