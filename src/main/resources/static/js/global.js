/* ============================================================
   Mộc Careers — Global JS
   ============================================================ */

(function () {
    function initUserMenus() {
        const userMenus = document.querySelectorAll('.user-menu');
        
        userMenus.forEach(menu => {
            const trigger = menu.querySelector('.user-trigger');
            const dropdown = menu.querySelector('.user-dropdown');
            if (!trigger || !dropdown) return;

            // Avoid binding duplicate listeners
            if (trigger.dataset.dropdownBound === 'true') return;
            trigger.dataset.dropdownBound = 'true';
            
            trigger.addEventListener('click', function(e) {
                e.stopPropagation();
                const willOpen = !dropdown.classList.contains('open');

                // Close any other open dropdowns first
                document.querySelectorAll('.user-dropdown.open').forEach(d => {
                    if (d !== dropdown) d.classList.remove('open');
                });
                document.querySelectorAll('.user-trigger[aria-expanded="true"]').forEach(t => {
                    if (t !== trigger) t.setAttribute('aria-expanded', 'false');
                });

                if (willOpen) {
                    dropdown.classList.add('open');
                    trigger.setAttribute('aria-expanded', 'true');
                } else {
                    dropdown.classList.remove('open');
                    trigger.setAttribute('aria-expanded', 'false');
                }
            });
            
            dropdown.addEventListener('click', function(e) { 
                e.stopPropagation(); 
            });
        });
    }

    // Document-level handlers for outside clicks and Escape key
    function initGlobalListeners() {
        if (window.__globalMenuListenersInitialized) return;
        window.__globalMenuListenersInitialized = true;

        document.addEventListener('click', function() {
            document.querySelectorAll('.user-dropdown.open').forEach(d => d.classList.remove('open'));
            document.querySelectorAll('.user-trigger[aria-expanded="true"]').forEach(t => t.setAttribute('aria-expanded', 'false'));
        });

        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' || e.key === 'Esc') {
                document.querySelectorAll('.user-dropdown.open').forEach(d => d.classList.remove('open'));
                document.querySelectorAll('.user-trigger[aria-expanded="true"]').forEach(t => t.setAttribute('aria-expanded', 'false'));
            }
        });
    }

    initGlobalListeners();

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initUserMenus);
    } else {
        initUserMenus();
    }
})();

