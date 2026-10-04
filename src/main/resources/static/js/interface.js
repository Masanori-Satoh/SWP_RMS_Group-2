(() => {
  'use strict';
  const menu = document.querySelector('[data-workspace-menu]');
  const navigation = document.getElementById('workspace-navigation');
  if (menu && navigation) {
    const mobile = matchMedia('(max-width:768px)');
    let open = false;
    const sync = () => {
      menu.hidden = !mobile.matches;
      navigation.hidden = mobile.matches && !open;
      menu.setAttribute('aria-expanded', String(!navigation.hidden));
    };
    menu.addEventListener('click', () => { open = !open; sync(); });
    document.addEventListener('keydown', event => {
      if (event.key === 'Escape' && mobile.matches && open) { open = false; sync(); menu.focus(); }
    });
    mobile.addEventListener('change', () => { open = false; sync(); });
    sync();
  }
  document.querySelectorAll('input[type="password"]').forEach(input => {
    const vi = document.documentElement.lang === 'vi';
    const show = vi ? 'Hiện' : 'Show';
    const hide = vi ? 'Ẩn' : 'Hide';
    const wrapper = document.createElement('div');
    wrapper.className = 'password-field';
    input.before(wrapper);
    wrapper.append(input);
    const button = document.createElement('button');
    button.type = 'button'; button.className = 'password-toggle'; button.textContent = show;
    button.setAttribute('aria-controls', input.id); button.setAttribute('aria-pressed', 'false');
    const label = document.querySelector(`label[for="${input.id}"]`).textContent.replace('*', '').trim().toLowerCase();
    button.setAttribute('aria-label', `${show} ${label}`);
    button.addEventListener('click', () => {
      const visible = input.type === 'password';
      input.type = visible ? 'text' : 'password';
      button.textContent = visible ? hide : show;
      button.setAttribute('aria-pressed', String(visible));
      button.setAttribute('aria-label', `${visible ? hide : show} ${label}`);
    });
    wrapper.append(button);
    input.form?.addEventListener('submit', () => { input.type = 'password'; });
  });
  document.querySelectorAll('.table-wrapper').forEach(table => {
    const hint = table.previousElementSibling;
    if (!hint?.matches('.table-scroll-note')) return;
    const syncHint = () => { hint.hidden = table.scrollWidth <= table.clientWidth; };
    new ResizeObserver(syncHint).observe(table);
    document.fonts.ready.then(syncHint);
    syncHint();
  });
  const summary = document.querySelector('[data-validation-summary]');
  if (summary) {
    document.querySelectorAll('[aria-invalid="true"]').forEach(input => {
      const references = (input.getAttribute('aria-describedby') || '').split(' ');
      const error = references.map(id => document.getElementById(id)).find(node => node?.matches('.form-error,.auth-field-error'));
      const item = [...summary.querySelectorAll('li')].find(li => li.textContent.trim() === error?.textContent.trim());
      if (!item) return;
      const link = document.createElement('a'); link.href = `#${input.id}`;
      const label = document.querySelector(`label[for="${input.id}"]`).textContent.replace('*', '').trim();
      link.textContent = `${label}: ${item.textContent}`;
      link.addEventListener('click', event => { event.preventDefault(); input.focus(); });
      item.replaceChildren(link);
    });
    summary.focus();
  }
})();
