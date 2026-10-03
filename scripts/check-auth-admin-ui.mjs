// Browser QA of MockMvc-rendered fixtures. No form POSTs, external probes, or DB writes.
import {browser, pause} from './career-browser.mjs';
import {mkdir, writeFile} from 'node:fs/promises';
import {resolve} from 'node:path';

const base=process.argv[2] || 'http://127.0.0.1:8768';
const round=process.argv[3] || 'round1';
if(!/^(round[12]|round1-recovery)$/.test(round)) throw Error('Use round1, round1-recovery or round2');
const output=resolve('docs/tests/assets/2026-10-01-auth-admin');
await mkdir(output,{recursive:true});
const checks=[];
const record=(name,ok,evidence)=>checks.push({name,pass:Boolean(ok),evidence});
const chrome=await browser(9250);
const pages=['login','register','forgot','reset','dashboard','accounts','create','edit','monitoring'];
const widths=[1440,1024,768,375];
const inspected=[];
let fault;
try {
  for(const name of pages){
    const page=await chrome.tab(`${base}/preview/${name}/`);
    const assets=await page.evaluate(`({css:[...document.styleSheets].map(s=>{try{return {href:s.href,rules:s.cssRules.length}}catch{return {href:s.href,rules:0}}}),script:!!document.querySelector('.password-toggle') || !document.querySelector('input[type=password]')})`);
    record(`${name}: page styles and scripts loaded`,assets.css.every(s=>s.rules>0) && assets.script,assets);
    if(!assets.css.every(s=>s.rules>0) || !assets.script)throw Error(`${name}: UI assets missing; do not trust layout checks`);
    for(const width of widths){
      await page.viewport(width,900);
      await page.evaluate('document.fonts.ready');
      await pause(80);
      const info=await page.evaluate(`(() => {
        const visible=e=>{const r=e.getBoundingClientRect();return r.width>1 && r.height>1 && getComputedStyle(e).visibility!=='hidden' && !e.closest('[hidden]')};
        const controls=[...document.querySelectorAll('input:not([type=hidden]),select,button')].filter(visible);
        const table=document.querySelector('.table-wrapper');
        const grid=document.querySelector('.account-form-grid');
        return {width:innerWidth,scrollWidth:document.documentElement.scrollWidth,
          h1:getComputedStyle(document.querySelector('h1')).fontFamily,body:getComputedStyle(document.body).fontFamily,
          background:getComputedStyle(document.body).backgroundColor,
          smallControls:controls.filter(e=>e.getBoundingClientRect().height<43).map(e=>e.id||e.textContent.trim()),
          unlabelled:controls.filter(e=>e.matches('input,select') && !e.labels.length && !e.getAttribute('aria-label')).map(e=>e.id),
          brokenReferences:[...document.querySelectorAll('[aria-describedby],[aria-labelledby],[aria-controls]')].flatMap(e=>['aria-describedby','aria-labelledby','aria-controls'].flatMap(a=>(e.getAttribute(a)||'').split(' ').filter(Boolean).filter(id=>!document.getElementById(id)))),
          duplicateIds:[...document.querySelectorAll('[id]')].map(e=>e.id).filter((id,i,a)=>a.indexOf(id)!==i),
          logout:document.querySelectorAll('form[action="/logout"]').length,
          csrf:[...document.querySelectorAll('form[method="post"]')].every(f=>f.querySelector('input[name="_csrf"]')),
          main:document.querySelectorAll('main#main').length,
          table:table?{width:table.clientWidth,scroll:table.scrollWidth,named:!!table.getAttribute('aria-labelledby'),focusable:table.tabIndex===0}:null,
          grid:grid?getComputedStyle(grid).gridTemplateColumns.split(' ').length:null,
          metrics:document.querySelector('.dashboard-grid')?getComputedStyle(document.querySelector('.dashboard-grid')).gridTemplateColumns.split(' ').filter(track=>parseFloat(track)>1).length:null,
          disclosure:document.querySelector('.account-filter-details')?document.querySelector('.account-filter-details').open:null,
          scrollHint:document.querySelector('.table-scroll-note')?document.querySelector('.table-scroll-note').hidden:null,
          menu:document.querySelector('[data-workspace-menu]')?{visible:visible(document.querySelector('[data-workspace-menu]')),navHidden:document.getElementById('workspace-navigation').hidden}:null,
          gradients:[...document.querySelectorAll('*')].filter(visible).filter(e=>getComputedStyle(e).backgroundImage.includes('gradient')).length,
          tokenStyles:[...document.styleSheets].map(s=>s.href).filter(Boolean)};
      })()`);
      record(`${name}/${width}: layout and shared typography`,info.scrollWidth<=width && info.main===1 && info.h1.includes('Lora') && info.body.includes('Source Sans 3') && info.background==='rgb(250, 249, 246)' && info.gradients===0,info);
      record(`${name}/${width}: form labels, references and target sizes`,!info.smallControls.length && !info.unlabelled.length && !info.brokenReferences.length && !info.duplicateIds.length && info.csrf,info);
      if(info.menu)record(`${name}/${width}: responsive shell`,info.logout===1 && info.menu.visible===(width<=768) && info.menu.navHidden===(width<=768),info.menu);
      if(info.table)record(`${name}/${width}: local table scrolling`,info.table.named && info.table.focusable && info.table.width<=width,info.table);
      if(info.grid)record(`${name}/${width}: form columns`,info.grid===(width===375?1:2),info.grid);
      if(info.metrics)record(`${name}/${width}: overview grouping`,info.metrics===(width<=768?2:4),info.metrics);
      if(info.disclosure!==null)record(`${name}/${width}: contextual advanced filters`,info.disclosure===(width>600),info.disclosure);
      if(info.table)record(`${name}/${width}: contextual table cue`,info.scrollHint===(info.table.scroll<=info.table.width),info.scrollHint);
      const contrast=await page.evaluate(`(() => {
        const rgba=c=>(c.match(/[0-9.]+/g)||[]).map(Number);
        const blend=(fg,bg)=>{const a=fg[3]??1;return [0,1,2].map(i=>fg[i]*a+bg[i]*(1-a))};
        const bg=e=>{const stack=[];for(let p=e;p;p=p.parentElement)stack.unshift(rgba(getComputedStyle(p).backgroundColor));return stack.reduce((a,c)=>blend(c,a),[255,255,255])};
        const lum=a=>a.slice(0,3).map(v=>{v/=255;return v<=.04045?v/12.92:((v+.055)/1.055)**2.4}).reduce((s,v,i)=>s+v*[.2126,.7152,.0722][i],0);
        const ratio=(a,b)=>{const x=lum(a),y=lum(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05)};
        const fails=[];let minimum=100;let count=0;
        const walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);
        while(walker.nextNode()) {const n=walker.currentNode,e=n.parentElement;if(!n.textContent.trim()||e.closest('script,style,.sr-only,[hidden]'))continue;
          const r=e.getBoundingClientRect(),s=getComputedStyle(e);if(r.width<2||r.height<2||r.bottom<0||s.visibility==='hidden'||s.display==='none')continue;
          const value=ratio(blend(rgba(s.color),bg(e)),bg(e));const required=parseFloat(s.fontSize)>=24 || (parseFloat(s.fontSize)>=18.666 && parseFloat(s.fontWeight)>=700)?3:4.5;
          minimum=Math.min(minimum,value);count++;if(value+.01<required)fails.push({text:n.textContent.trim().slice(0,64),value,required});}
        const borders=[...document.querySelectorAll('input:not([type=hidden]),select,.btn-secondary,.btn-danger')].filter(e=>e.getBoundingClientRect().width>1).map(e=>({id:e.id||e.textContent.trim(),ratio:ratio(rgba(getComputedStyle(e).borderTopColor),bg(e))}));
        return {count,minimum,fails,borderFails:borders.filter(b=>b.ratio+.01<3)};
      })()`);
      record(`${name}/${width}: text and control contrast`,!contrast.fails.length && !contrast.borderFails.length,contrast);
      await page.screenshot(resolve(output,`${round}-${name}-${width}.png`));
    }
    inspected.push({name,page});
  }
  const get=name=>inspected.find(x=>x.name===name).page;
  const login=get('login');
  await login.evaluate(`document.getElementById('password').value='visual-only-password'`);
  const before=await login.evaluate(`(()=>{const e=document.getElementById('password');return {name:e.name,value:e.value,autocomplete:e.autocomplete}})()`);
  await login.click('.password-toggle');
  const shown=await login.evaluate(`(()=>{const e=document.getElementById('password'),b=document.querySelector('.password-toggle');return {type:e.type,name:e.name,value:e.value,autocomplete:e.autocomplete,pressed:b.getAttribute('aria-pressed')}})()`);
  await login.key('Enter');
  record('Password toggle activation preserves input contract',shown.type==='text' && shown.pressed==='true' && shown.name===before.name && shown.value===before.value && shown.autocomplete===before.autocomplete && await login.evaluate(`document.getElementById('password').type==='password'`),{shown:shown.type,pressed:shown.pressed,autocomplete:shown.autocomplete});
  await login.evaluate(`document.getElementById('password').value=''; document.querySelector('.skip-link').focus()`);
  await login.key('Enter');
  record('Skip link keyboard activation',await login.evaluate(`document.activeElement.id==='main'`));
  const list=get('accounts');
  await list.send('Page.bringToFront');
  await list.click('.account-filter-details summary');
  record('Native More Filters exposes unchanged controls',await list.evaluate(`document.querySelector('.account-filter-details').open && document.getElementById('account-department').name==='departmentId' && document.getElementById('account-sort').name==='sort'`));
  await list.key('Enter');
  record('More Filters keyboard close',await list.evaluate(`!document.querySelector('.account-filter-details').open`));
  await list.click('[data-workspace-menu]');
  record('Mobile menu opens by activation',await list.evaluate(`document.getElementById('workspace-navigation').hidden===false && document.querySelector('[data-workspace-menu]').getAttribute('aria-expanded')==='true'`));
  await list.key('Escape');
  record('Mobile menu Escape closes and restores focus',await list.evaluate(`document.getElementById('workspace-navigation').hidden && document.activeElement.matches('[data-workspace-menu]')`));
  await list.evaluate(`const region=document.querySelector('.table-wrapper');region.scrollLeft=0;region.focus()`);
  await list.send('Input.dispatchKeyEvent',{type:'keyDown',key:'ArrowRight',code:'ArrowRight',windowsVirtualKeyCode:39,nativeVirtualKeyCode:39});
  await list.send('Input.dispatchKeyEvent',{type:'keyUp',key:'ArrowRight',code:'ArrowRight',windowsVirtualKeyCode:39,nativeVirtualKeyCode:39});
  await pause(250);
  record('Named table scrolls by keyboard',await list.evaluate(`document.querySelector('.table-wrapper').scrollLeft>0`));
  record('Table identity remains visible while scrolling',await list.evaluate(`(()=>{const t=document.querySelector('.table-wrapper'),c=document.querySelector('tbody td:first-child');t.scrollLeft=t.scrollWidth-t.clientWidth;return Math.abs(c.getBoundingClientRect().left-t.getBoundingClientRect().left)<2})()`));
  await list.click('[data-deactivate-account]');
  record('Delete opens native confirmation with actual identity, route and safe focus',await list.evaluate(`document.getElementById('deactivate-dialog').open && document.activeElement.id==='deactivate-cancel' && new URL(document.getElementById('deactivate-form').action).pathname==='/admin/accounts/42/deactivate' && document.getElementById('deactivate-account-name').textContent==='Nguyễn Minh Anh' && document.getElementById('deactivate-account-username').textContent==='minhanh'`));
  await list.screenshot(resolve(output,`${round}-delete-375.png`),false);
  await list.click('#deactivate-cancel');
  record('Delete Cancel retains record and trigger focus',await list.evaluate(`!document.getElementById('deactivate-dialog').open && document.activeElement.matches('[data-deactivate-account]') && document.querySelectorAll('tbody tr').length===3`));
  await list.key('Enter');await list.key('Escape');
  record('Delete keyboard activation and Escape',await list.evaluate(`!document.getElementById('deactivate-dialog').open && document.activeElement.matches('[data-deactivate-account]')`));
  const create=get('create');
  const departmentRequired=async value=>create.evaluate(`(()=>{const e=document.getElementById('role');e.value=${JSON.stringify(value)};e.dispatchEvent(new Event('change'));return document.getElementById('department').required})()`);
  record('Role selection retains current department rule',await departmentRequired('2') && !(await departmentRequired('6')));
  record('Create/Edit retain field contracts',await create.evaluate(`document.querySelector('[name=password]') && document.querySelector('[name=confirmPassword]') && !document.querySelector('[name=accountStatus]')`) && await get('edit').evaluate(`document.getElementById('username-readonly').readOnly && !document.querySelector('[name=username]') && !document.querySelector('[name=password]') && document.querySelector('[name=accountStatus]')`));
  const filtered=await chrome.tab(`${base}/preview/accounts-filtered/`);
  await filtered.viewport(375);
  record('Server-selected advanced filters remain expanded on mobile',await filtered.evaluate(`document.querySelector('.account-filter-details').open && document.querySelector('.account-filter-details').dataset.active==='true' && document.getElementById('account-department').value==='1' && document.getElementById('account-sort').value==='newest' && document.querySelector('.account-filter-details summary').textContent.includes('active')`));
  inspected.push({name:'accounts-filtered',page:filtered});
  for(const state of ['login-error','login-logout','forgot-sent','reset-expired','register-invalid','create-invalid','edit-invalid']){
    const page=await chrome.tab(`${base}/preview/${state}/`);
    for(const width of [1440,375]){
      await page.viewport(width,900);await page.evaluate('document.fonts.ready');
      record(`${state}/${width}: feedback fits viewport`,await page.evaluate(`document.documentElement.scrollWidth<=innerWidth && !!document.querySelector('[role=alert],[role=status]')`));
      await page.screenshot(resolve(output,`${round}-${state}-${width}.png`));
    }
    if(state.endsWith('invalid')){
      record(`${state}: server error summary gets focus`,await page.evaluate(`document.activeElement.matches('[data-validation-summary]') && document.querySelector('[data-validation-summary] a')`));
      const id=await page.evaluate(`document.querySelector('[data-validation-summary] a').hash.slice(1)`);
      await page.click('[data-validation-summary] a');
      record(`${state}: linked error focuses associated field`,await page.evaluate(`document.activeElement.id===${JSON.stringify(id)} && document.activeElement.getAttribute('aria-invalid')==='true' && document.activeElement.getAttribute('aria-describedby').split(' ').every(id=>document.getElementById(id))`));
    }
    inspected.push({name:state,page});
  }
  const runtime=inspected.flatMap(({name,page})=>page.events.filter(e=>e.method==='Runtime.exceptionThrown'||(e.method==='Log.entryAdded' && e.params.entry.level==='error')).map(e=>({name,event:e})));
  record('No runtime or failed-resource errors across all inspected views',runtime.length===0,runtime);
} catch(error){fault=String(error);record('Browser driver completion',false,fault);}
finally {await chrome.close();}
const result={round,base,provenance:'Auth/Admin fictional MockMvc fixture HTML, real local UI assets; no form POST or live authentication.',total:checks.length,passed:checks.filter(c=>c.pass).length,failed:checks.filter(c=>!c.pass).length,checks,...(fault?{fault}:{})};
await writeFile(resolve(output,`${round}-results.json`),JSON.stringify(result,null,2));
console.log(JSON.stringify({total:result.total,passed:result.passed,failed:result.failed,fault},null,2));
for(const check of checks.filter(c=>!c.pass))console.log(JSON.stringify(check));
if(result.failed)process.exitCode=1;
