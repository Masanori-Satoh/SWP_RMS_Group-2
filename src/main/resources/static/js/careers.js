(() => {
  'use strict';
  const menu = document.querySelector('.mobile-menu');
  if (menu) {
    menu.addEventListener('click', event => {
      const link = event.target.closest('a');
      if (!link) return;
      menu.open = false;
      const url = new URL(link.href);
      if (url.pathname === location.pathname && url.hash) {
        const section = document.getElementById(url.hash.slice(1));
        const heading = section?.querySelector('h2');
        if (heading) {
          heading.setAttribute('tabindex', '-1');
          requestAnimationFrame(() => heading.focus({preventScroll:true}));
        }
      }
    });
    document.addEventListener('click', event => {
      if (menu.open && !menu.contains(event.target)) menu.open = false;
    });
    document.addEventListener('keydown', event => {
      if (event.key === 'Escape' && menu.open) {
        menu.open = false;
        menu.querySelector('summary').focus();
      }
    });
  }

  const keyword = document.getElementById('keyword');
  if (keyword) {
    const locationSelect = document.getElementById('location');
    const cards = [...document.querySelectorAll('.job-card')];
    const pills = [...document.querySelectorAll('.department-filter')];
    const clear = document.getElementById('clear-filters');
    const strip = document.querySelector('.filter-pills');
    const scrollHint = document.querySelector('.department-scroll-hint');
    const updateScrollHint = () => {
      scrollHint.hidden = !matchMedia('(max-width:600px)').matches || strip.scrollWidth <= strip.clientWidth;
    };
    let department = '';
    const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim();
    const update = () => {
      const query = normalize(keyword.value);
      const tokens = query.split(/\s+/).filter(Boolean);
      const location = locationSelect.value;
      const matching = cards.filter(card => tokens.every(token => normalize(card.dataset.search).includes(token)) && (!location || card.dataset.location === location));
      cards.forEach(card => { card.hidden = !matching.includes(card) || (department !== '' && card.dataset.department !== department); });
      pills.forEach(pill => {
        const id = pill.dataset.department;
        const count = matching.filter(card => id === '' || card.dataset.department === id).length;
        pill.querySelector('span:last-child').textContent = String(count);
        pill.classList.toggle('is-empty', count === 0);
        pill.setAttribute('aria-pressed', String(id === department));
        const label = id === '' ? 'All Departments' : pill.querySelector('.department-label').textContent;
        pill.setAttribute('aria-label', `${label}, ${count} ${count === 1 ? 'position' : 'positions'}`);
      });
      const count = cards.filter(card => !card.hidden).length;
      document.getElementById('result-count').textContent = `${count} open ${count === 1 ? 'position' : 'positions'}`;
      document.getElementById('empty-state').hidden = count > 0;
      clear.hidden = !query && !location && department === '';
    };
    keyword.addEventListener('input', update);
    locationSelect.addEventListener('change', update);
    pills.forEach(pill => pill.addEventListener('click', () => { department = pill.dataset.department; update(); }));
    clear.addEventListener('click', () => {
      keyword.value = ''; locationSelect.value = ''; department = '';
      // Keep keyboard focus in the results context when this button becomes hidden.
      pills[0].focus({preventScroll:true});
      update();
    });
    update();
    window.addEventListener('resize', updateScrollHint);
    document.fonts.ready.then(updateScrollHint);
    updateScrollHint();
  }

  const share = document.getElementById('share-role');
  if (share && navigator.clipboard && window.isSecureContext) {
    share.hidden = false;
    share.addEventListener('click', async () => {
      const status = document.getElementById('share-status');
      try { await navigator.clipboard.writeText(location.href); status.textContent = 'Role link copied.'; }
      catch { status.textContent = 'Copy the page address from your browser to share this role.'; }
    });
  }
  document.getElementById('application-form')?.addEventListener('submit', event => event.preventDefault());
})();
