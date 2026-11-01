/* =============================================================================
   PUBLIC-TOPBAR.JS — Quản lý hành vi topbar và thanh cuộn dọc trang chủ
   - Khi bấm vào mục cuộn: cuộn mượt tới section tương ứng.
   - Khi cuộn trang: tự động sáng icon + label tương ứng trên thanh điều hướng dọc.
   - Mặc định ở đỉnh trang: mục đầu tiên (Việc làm Hot) sáng.
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
      if (on) {
        link.setAttribute('aria-current', 'location');
      } else {
        link.removeAttribute('aria-current');
      }
    });
  };

  // Cuộn mượt khi click vào icon / label
  links.forEach(link => {
    link.addEventListener('click', (e) => {
      const sectionId = link.dataset.section;
      const target = document.getElementById(sectionId);
      if (target) {
        e.preventDefault();
        // Trừ bớt 64px để không bị che bởi floating topbar
        const targetTop = target.getBoundingClientRect().top + window.scrollY - 64;
        window.scrollTo({ top: targetTop, behavior: 'smooth' });
        setActive(sectionId);
      }
    });
  });

  if (!sections.length) return;

  // Lắng nghe cuộn để cập nhật mục đang xem
  const update = () => {
    const scrollPos = window.scrollY + 180;
    let current = '';

    // Nếu cuộn chạm gần đáy trang: sáng mục cuối cùng (perks - Chế độ đãi ngộ)
    if (window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 20) {
      current = sections[sections.length - 1].id;
    } else {
      sections.forEach(section => {
        const top = section.offsetTop;
        const height = section.offsetHeight;
        if (scrollPos >= top && scrollPos < top + height) {
          current = section.id;
        }
      });
    }

    // Nếu ở đỉnh trang (khu vực Hero): mặc định sáng mục đầu tiên (Việc làm Hot)
    if (!current && sections.length > 0 && window.scrollY < sections[0].offsetTop) {
      current = sections[0].id;
    }

    if (current) {
      setActive(current);
    }
  };

  let ticking = false;
  window.addEventListener('scroll', () => {
    if (!ticking) {
      window.requestAnimationFrame(() => {
        update();
        ticking = false;
      });
      ticking = true;
    }
  }, { passive: true });

  window.addEventListener('resize', update);
  // Cập nhật ngay khi trang tải xong
  update();
})();
