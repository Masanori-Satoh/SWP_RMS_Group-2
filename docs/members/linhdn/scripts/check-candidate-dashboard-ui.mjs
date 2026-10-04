// Visual/interaction QA of real Thymeleaf output from MockMvc, not live application E2E.
import {browser, pause} from './career-browser.mjs';
import {mkdir, writeFile} from 'node:fs/promises';
import {resolve} from 'node:path';
const base = process.argv[2] || 'http://127.0.0.1:8772';
const round = process.argv[3] || 'round1';
if (!['round1', 'round2'].includes(round)) throw Error('Use round1 or round2');
const out = resolve('docs/members/linhdn/tests/assets/2026-10-04-candidate-dashboard');
await mkdir(out, {recursive:true});
const checks = [];
const check = (name, pass, evidence) => checks.push({name, pass:!!pass, evidence});
const chrome = await browser(9262);
try {
  for (const state of ['populated', 'empty']) {
    const page = await chrome.tab(`${base}/preview/${state}/`);
    await page.evaluate('document.fonts.ready');
    for (const width of [1440, 1024, 768, 375]) {
      await page.viewport(width, 900);
      const info = await page.evaluate(`(() => {
        const visible = e => !!(e.getBoundingClientRect().width && e.getBoundingClientRect().height);
        return {width:innerWidth, scroll:document.documentElement.scrollWidth, lang:document.documentElement.lang,
          title:document.title, fonts:[getComputedStyle(document.querySelector('h1')).fontFamily,getComputedStyle(document.body).fontFamily],
          small:[...document.querySelectorAll('button,input:not([type=hidden]),select,summary')].filter(visible).filter(e=>e.getBoundingClientRect().height<43).map(e=>e.id||e.textContent.trim()),
          unlabeled:[...document.querySelectorAll('input:not([type=hidden]),select')].filter(e=>!e.labels.length).map(e=>e.id),
          missing:[...document.querySelectorAll('[aria-controls],[aria-labelledby],[aria-describedby]')].flatMap(e=>['aria-controls','aria-labelledby','aria-describedby'].flatMap(a=>(e.getAttribute(a)||'').split(' ').filter(Boolean).filter(id=>!document.getElementById(id)))),
          csrf:!!document.querySelector('form[action="/logout"] input[name="_csrf"]'),
          tables:[...document.querySelectorAll('.table-wrapper')].map(e=>({width:e.clientWidth,scroll:e.scrollWidth,tab:e.tabIndex})),
          assets:[...document.styleSheets].every(s=>{try{return s.cssRules.length>0}catch{return false}}),
          menu:{visible:visible(document.querySelector('[data-workspace-menu]')),hidden:document.getElementById('workspace-navigation').hidden}};
      })()`);
      check(`${state}/${width}: layout/fonts/locale`,info.scroll<=width && info.lang==='vi' && info.fonts[0].includes('Lora') && info.fonts[1].includes('Source Sans 3'),info);
      check(`${state}/${width}: labels/targets/ARIA/CSRF/assets`,!info.small.length && !info.unlabeled.length && !info.missing.length && info.csrf && info.assets,info);
      check(`${state}/${width}: table containment`,info.tables.every(t=>t.width<=width && t.tab===0),info.tables);
      check(`${state}/${width}: responsive navigation`,info.menu.visible===(width<=768) && info.menu.hidden===(width<=768),info.menu);
      await page.screenshot(resolve(out,`${round}-${state}-${width}.png`));
    }
    await page.viewport(1440,900);
    if (state==='populated') {
      const type = async (selector, text) => { await page.click(selector); await page.send('Input.insertText',{text}); await pause(60); };
      await type('#application-search','ky su');
      check('Vietnamese accent insensitive search',await page.evaluate(`document.querySelectorAll('#applications [data-filter-row]:not([hidden])').length===1 && document.querySelector('#applications [data-filter-row]:not([hidden])').dataset.title==='Kỹ sư phần mềm'`));
      await page.click('#applications [data-filter-clear]');
      check('Clear restores rows and keeps contextual focus',await page.evaluate(`document.querySelectorAll('#applications [data-filter-row]:not([hidden])').length===2 && document.activeElement.matches('[data-filter-clear]')`));
      await page.click('#application-status'); await page.key('Home'); await page.key('ArrowDown'); await page.key('Enter');
      check('Status filter works',await page.evaluate(`document.querySelector('#application-status').value!=='' && document.querySelectorAll('#applications [data-filter-row]:not([hidden])').length===1`));
      await page.click('#applications [data-filter-clear]');
      await type('#application-search','dieu phoi');
      check('Vietnamese D stroke normalization',await page.evaluate(`document.querySelector('#applications [data-filter-row]:not([hidden])').dataset.title==='Điều phối dự án'`));
      await page.click('#applications [data-filter-clear]');
      await type('#application-search','no-result-position');
      check('No-results state',await page.evaluate(`!document.querySelector('#applications [data-filter-empty]').hidden && document.querySelector('#applications .table-wrapper').hidden`));
      await page.click('#applications [data-filter-clear]');
      await page.click('.candidate-offer-details summary');
      check('Native offer details opens',await page.evaluate(`document.querySelector('.candidate-offer-details').open`));
      await page.key('Enter');
      check('Native offer details closes by keyboard',await page.evaluate(`!document.querySelector('.candidate-offer-details').open`));
      await type('#offer-search','khong-co');
      check('Offers search independent from applications',await page.evaluate(`!document.querySelector('#offers [data-filter-empty]').hidden && document.querySelectorAll('#applications [data-filter-row]:not([hidden])').length===2`));
      await page.click('#offers [data-filter-clear]');
    }
    await page.click('form[data-confirm-logout] button');
    check(`${state}: logout dialog/default focus`,await page.evaluate(`document.querySelector('#candidate-logout-dialog').open && document.activeElement.matches('[data-logout-cancel]')`));
    await page.key('Escape');
    check(`${state}: logout cancel/focus restore`,await page.evaluate(`!document.querySelector('#candidate-logout-dialog').open && document.activeElement.matches('form[data-confirm-logout] button')`));
    await page.viewport(375,900);
    await page.click('[data-workspace-menu]');
    check(`${state}: mobile menu`,await page.evaluate(`!document.querySelector('#workspace-navigation').hidden && document.querySelector('[data-workspace-menu]').getAttribute('aria-expanded')==='true'`));
    await page.key('Escape');
    check(`${state}: Escape closes menu`,await page.evaluate(`document.querySelector('#workspace-navigation').hidden`));
    await page.send('Emulation.setEmulatedMedia',{features:[{name:'prefers-reduced-motion',value:'reduce'}]});
    check(`${state}: reduced motion`,await page.evaluate(`getComputedStyle(document.documentElement).scrollBehavior==='auto'`));
    const errors = page.events.filter(e=>e.method==='Runtime.exceptionThrown');
    check(`${state}: no browser JS exceptions`,errors.length===0,errors);
  }
} finally { await chrome.close(); }
await writeFile(resolve(out,`${round}-checks.json`),JSON.stringify(checks,null,2));
console.log(JSON.stringify({round,total:checks.length,passed:checks.filter(c=>c.pass).length,failed:checks.filter(c=>!c.pass)},null,2));
if (checks.some(c=>!c.pass)) process.exitCode=1;
