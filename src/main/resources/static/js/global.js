/* ============================================================
   Mộc Careers — Global JS
   ============================================================ */

document.addEventListener('DOMContentLoaded', function() {
    // 1. Shared User Menu Dropdown Logic
    const userMenus = document.querySelectorAll('.user-menu');
    
    userMenus.forEach(menu => {
        const trigger = menu.querySelector('.user-trigger');
        const dropdown = menu.querySelector('.user-dropdown');
        if (!trigger || !dropdown) return;
        
        trigger.addEventListener('click', function(e) {
            e.stopPropagation();
            const isOpen = dropdown.classList.toggle('open');
            trigger.setAttribute('aria-expanded', String(isOpen));
        });
        
        dropdown.addEventListener('click', function(e) { 
            e.stopPropagation(); 
        });
    });
    
    document.addEventListener('click', function() {
        document.querySelectorAll('.user-dropdown').forEach(d => d.classList.remove('open'));
        document.querySelectorAll('.user-trigger').forEach(t => t.setAttribute('aria-expanded', 'false'));
    });
});
