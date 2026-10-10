/* =============================================================================
   PUBLIC-TOPBAR.JS — làm sáng mục menu topbar công khai
   - Khi bấm: sáng ngay mục vừa bấm.
   - Khi cuộn (trang landing): sáng mục ứng với section đang nằm dưới topbar.
   - Trang không có các section (job-detail): giữ nguyên mục server đã đánh dấu.
   ============================================================================= */
(() => {
  'use strict';
  const nav = document.querySelector('[data-public-nav]');
  if (!nav) return;
  const links = [...nav.querySelectorAll('a[data-section]')];
  const sections = links
    .map(link => document.getElementById(link.dataset.section))
    .filter(Boolean);

  const setActive = id => {
    links.forEach(link => {
      const on = link.dataset.section === id;
      link.classList.toggle('active', on);
      if (on) link.setAttribute('aria-current', 'location');
      else link.removeAttribute('aria-current');
    });
  };

  links.forEach(link => link.addEventListener('click', () => {
    if (sections.length) setActive(link.dataset.section);
  }));

  if (!sections.length) return;

  const topbar = nav.closest('.public-topbar');
  const update = () => {
    // Mốc đọc: ngay dưới topbar + 1/3 chiều cao phần còn lại của màn hình
    const offset = (topbar ? topbar.offsetHeight : 0);
    const probe = offset + (window.innerHeight - offset) / 3;
    let current = '';
    sections.forEach(section => {
      if (section.getBoundingClientRect().top <= probe) current = section.id;
    });
    // Cuộn chạm đáy trang: sáng mục cuối
    if (window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 2) {
      current = sections[sections.length - 1].id;
    }
    setActive(current);
  };

  let ticking = false;
  window.addEventListener('scroll', () => {
    if (ticking) return;
    ticking = true;
    requestAnimationFrame(() => { update(); ticking = false; });
  }, { passive: true });
  window.addEventListener('resize', update);
  update();
})();
