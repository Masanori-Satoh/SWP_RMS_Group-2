// MockMvc-rendered fixtures only. No POSTs, account writes, or fake backend responses.
import {browser, pause} from './career-browser.mjs';
import {mkdir, writeFile} from 'node:fs/promises';
import {resolve} from 'node:path';
const base=process.argv[2]||'http://127.0.0.1:8770';
const round=process.argv[3]||'round1';
if(!['round1','round2'].includes(round))throw Error('Use round1 or round2');
const out=resolve('docs/tests/assets/2026-10-02-accounts');
await mkdir(out,{recursive:true});
const checks=[];
const check=(name,pass,evidence)=>checks.push({name,pass:!!pass,evidence});
const chrome=await browser(9260);
try {
  for(const name of ['accounts','candidate-accounts','dashboard','create','edit']) {
    const page=await chrome.tab(`${base}/preview/${name}/`);
    await page.send('Page.bringToFront');
    await page.evaluate('document.fonts.ready');
    check(`${name}: assets`,await page.evaluate(`[...document.styleSheets].every(s=>{try{return s.cssRules.length>0}catch{return false}})`));
    for(const width of [1440,1024,768,375]) {
      await page.viewport(width,900); await pause(80);
      const info=await page.evaluate(`(()=>{
        const visible=e=>{const r=e.getBoundingClientRect();return r.width>0&&r.height>0};
        const controls=[...document.querySelectorAll('button,input:not([type=hidden]),select')].filter(visible);
        const table=document.querySelector('.table-wrapper');
        return {width:innerWidth,scroll:document.documentElement.scrollWidth,
          font:getComputedStyle(document.querySelector('h1')).fontFamily,body:getComputedStyle(document.body).fontFamily,
          small:controls.filter(e=>e.getBoundingClientRect().height<43).map(e=>e.id||e.textContent.trim()),
          unlabelled:controls.filter(e=>e.matches('input,select')&&!e.labels.length).map(e=>e.id),
          broken:[...document.querySelectorAll('[aria-describedby],[aria-labelledby],[aria-controls]')].flatMap(e=>['aria-describedby','aria-labelledby','aria-controls'].flatMap(a=>(e.getAttribute(a)||'').split(' ').filter(Boolean).filter(id=>!document.getElementById(id)))),
          csrf:[...document.querySelectorAll('form[method=post]')].every(f=>f.querySelector('input[name=_csrf]')),
          table:table?{width:table.clientWidth,scroll:table.scrollWidth,named:!!table.getAttribute('aria-labelledby'),focusable:table.tabIndex===0}:null,
          bodyText:document.querySelector('main').innerText,roles:[...document.querySelectorAll('select[name=roleId] option')].map(e=>e.textContent),
          department:!!document.querySelector('select[name=departmentId]'),departmentRequired:document.querySelector('select[name=departmentId]')?.required,
          summaries:document.querySelectorAll('.dashboard-account-summary').length,
          columns:document.querySelector('.dashboard-account-counts')?getComputedStyle(document.querySelector('.dashboard-account-counts')).gridTemplateColumns.split(' ').length:null,
          menu:document.querySelector('[data-workspace-menu]')?{visible:visible(document.querySelector('[data-workspace-menu]')),hidden:document.getElementById('workspace-navigation').hidden}:null};
      })()`);
      check(`${name}/${width}: layout/type`,info.scroll<=width&&info.font.includes('Lora')&&info.body.includes('Source Sans 3'),info);
      check(`${name}/${width}: labels/targets/references/csrf`,!info.small.length&&!info.unlabelled.length&&!info.broken.length&&info.csrf,info);
      if(info.table)check(`${name}/${width}: local table scroll`,info.table.width<=width&&info.table.named&&info.table.focusable,info.table);
      if(info.menu)check(`${name}/${width}: existing responsive nav`,info.menu.visible===(width<=768)&&info.menu.hidden===(width<=768),info.menu);
      if(name==='candidate-accounts')check(`${name}/${width}: candidate context`,!info.department&&!info.roles.length&&info.bodyText.includes('Linked · #17')&&info.bodyText.includes('Profile missing')&&!info.bodyText.includes('Create Candidate'),info.bodyText);
      if(['accounts','create','edit'].includes(name))check(`${name}/${width}: internal context`,!info.roles.includes('Candidate')&&info.bodyText.includes('Internal Account'),info.roles);
      if(['create','edit'].includes(name))check(`${name}/${width}: native department required`,info.departmentRequired,info.departmentRequired);
      if(name==='edit')check(`${name}/${width}: existing selections preserved`,await page.evaluate(`document.querySelector('#role').value==='5'&&document.querySelector('#department').value==='1'`));
      if(name==='accounts'&&width===1440) {
        const baseline=await page.evaluate(`['account-search','account-role','account-status','account-department','account-sort'].map(id=>document.getElementById(id).getBoundingClientRect().top)`);
        check(`${name}/${width}: five filters share baseline`,Math.max(...baseline)-Math.min(...baseline)<2,baseline);
      }
      if(name==='dashboard')check(`${name}/${width}: two groups and separate health`,info.summaries===2&&info.columns===(width<=560?2:4)&&info.bodyText.includes('System Health'),info);
      await page.screenshot(resolve(out,`${round}-${name}-${width}.png`));
    }
    if(['accounts','candidate-accounts'].includes(name)) {
      await page.click('[data-deactivate-account]');
      const state=await page.evaluate(`({open:document.querySelector('dialog').open,focus:document.activeElement.id,action:document.getElementById('deactivate-form').getAttribute('action'),name:document.getElementById('deactivate-account-username').textContent})`);
      check(`${name}: confirmation opens correct target and Cancel focus`,state.open&&state.focus==='deactivate-cancel'&&state.action.includes(name==='accounts'?'/admin/accounts/':'/admin/candidate-accounts/')&&!!state.name,state);
      await page.screenshot(resolve(out,`${round}-${name}-modal-375.png`));
      await page.key('Escape');
      check(`${name}: Escape restores focus`,await page.evaluate(`!document.querySelector('dialog').open&&document.activeElement.matches('[data-deactivate-account]')`));
      await page.click('[data-deactivate-account]');await page.click('#deactivate-cancel');
      check(`${name}: Cancel restores focus`,await page.evaluate(`!document.querySelector('dialog').open&&document.activeElement.matches('[data-deactivate-account]')`));
    }
    if(name==='candidate-accounts') {
      await page.click('[data-workspace-menu]');
      const nav=await page.evaluate(`({width:document.querySelector('.workspace-nav').getBoundingClientRect().width,groups:[...document.querySelectorAll('.workspace-nav-group')].map(e=>e.getBoundingClientRect().width)})`);
      check(`${name}: mobile group labels span navigation`,nav.groups.length===2&&nav.groups.every(w=>Math.abs(w-nav.width)<2),nav);
      await page.screenshot(resolve(out,`${round}-menu-375.png`));
      await page.key('Escape');
      check(`${name}: menu Escape restores focus`,await page.evaluate(`document.getElementById('workspace-navigation').hidden&&document.activeElement.matches('[data-workspace-menu]')`));
    }
    const errors=page.events.filter(e=>e.method==='Runtime.exceptionThrown');
    check(`${name}: no JS exceptions`,errors.length===0,errors);
  }
} finally {await chrome.close();}
await writeFile(resolve(out,`${round}-results.json`),JSON.stringify({checks,passed:checks.filter(c=>c.pass).length,total:checks.length},null,2));
console.log(`${checks.filter(c=>c.pass).length}/${checks.length} checks PASS`);
if(checks.some(c=>!c.pass))process.exitCode=1;
