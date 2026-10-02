// Public routes can be checked live; --fixture additionally inspects MockMvc account/apply HTML.
// No authentication simulation. No packages. Chrome profile cleanup is guarded in career-browser.mjs.
import {browser,pause} from './career-browser.mjs';
import {mkdir,writeFile} from 'node:fs/promises';
import {resolve} from 'node:path';

const base=(process.argv[2]||'http://127.0.0.1:8766').replace(/\/$/,'');
const fixture=process.argv.includes('--fixture');
const capture=!process.argv.includes('--checks-only');
const round=process.argv.find(v=>v.startsWith('--round='))?.split('=')[1]||new Date().toISOString().replace(/[:.]/g,'-');
const out=resolve('docs/tests/assets/2026-10-01-career-integration');
await mkdir(out,{recursive:true});
const results={base,fixture,round,checks:[],viewports:[],errors:[]};
const check=(name,pass,details)=>results.checks.push({name,pass:Boolean(pass),details});
const b=await browser(9240);
const screenshot=async(page,path,full=true)=>{if(capture)await page.screenshot(path,full);};
let page;
try {
  page=await b.tab(base+'/');
  const ev=page.evaluate;
  await ev('document.fonts.ready');
  const titles=await ev(`[...document.querySelectorAll('.job-card h3')].map(e=>e.textContent)`);
  for(const width of [1440,1024,768,375]) {
    await page.viewport(width,900,width===375);
    await ev('scrollTo(0,0)');await pause(100);
    const v=await ev(`(()=>{const cs=s=>getComputedStyle(document.querySelector(s));return {width:innerWidth,scrollWidth:document.documentElement.scrollWidth,header:cs('.site-header').position,headerHeight:document.querySelector('.site-header').getBoundingClientRect().height,jobCols:cs('.job-grid').gridTemplateColumns.split(' ').length,processCols:cs('.process-list').gridTemplateColumns.split(' ').length,pillsWrap:cs('.filter-pills').flexWrap,careers:cs('.brand-sub').display,heroHeight:document.querySelector('.hero').getBoundingClientRect().height}})()`);
    results.viewports.push(v);
    check(`No page overflow ${width}`,v.scrollWidth<=width,v);
    check(`Grid columns ${width}`,v.jobCols===(width>1100?3:width>600?2:1)&&v.processCols===(width>1100?4:width>600?2:1),v);
    check(`Sticky header and brand ${width}`,v.header==='sticky'&&v.careers!=='none');
    if(width===375)check('Mobile pills stay in one scroll row',v.pillsWrap==='nowrap');
    check(`Department scroll hint ${width}`,await ev(`document.querySelector('.department-scroll-hint').hidden === !(innerWidth<=600 && document.querySelector('.filter-pills').scrollWidth>document.querySelector('.filter-pills').clientWidth)`));
    await screenshot(page,resolve(out,`${round}-home-${width}.png`));
    if(width===375) {
      await ev(`document.getElementById('roles').scrollIntoView({block:'start',behavior:'instant'})`);await pause(100);
      await screenshot(page,resolve(out,`${round}-positions-375.png`),false);
      check('Anchor clears sticky header',await ev(`document.getElementById('roles-title').getBoundingClientRect().top >= document.querySelector('.site-header').getBoundingClientRect().bottom`));
      await page.click('.mobile-menu summary');
      check('Mobile account entry points',await ev(`document.querySelector('.mobile-menu').open && [...document.querySelectorAll('.mobile-menu a')].some(a=>a.getAttribute('href')==='/login') && [...document.querySelectorAll('.mobile-menu a')].some(a=>a.getAttribute('href')==='/register')`));
      await page.key('Escape');
      check('Escape returns menu focus',await ev(`!document.querySelector('.mobile-menu').open && document.activeElement.tagName==='SUMMARY'`));
    }
  }
  const setQuery=async value=>{await page.click('#keyword');await ev(`document.getElementById('keyword').value='';document.getElementById('keyword').dispatchEvent(new Event('input'))`);await page.send('Input.insertText',{text:value});await pause(100);};
  await setQuery('NoMatchingRoleZZZ');
  check('Empty filtering state is visible',await ev(`!document.getElementById('empty-state').hidden && document.querySelectorAll('.job-card:not([hidden])').length===0 && !document.getElementById('clear-filters').hidden`));
  const before=await ev('scrollY');await page.click('#clear-filters');
  check('Clear stays in results context',await ev(`document.activeElement.classList.contains('department-filter') && document.querySelectorAll('.job-card:not([hidden])').length===${titles.length}`),{before,after:await ev('scrollY')});
  if(titles.length) {
    const ascii=titles[0].normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').replace(/Đ/g,'D');
    await setQuery(ascii);
    check('Diacritic-insensitive title search',await ev(`document.querySelector('.job-card:not([hidden]) h3')?.textContent===${JSON.stringify(titles[0])}`));
    await page.click('#clear-filters');
    check('Zero-count departments are muted',await ev(`[...document.querySelectorAll('.department-filter')].every(p=>Number(p.querySelector('span:last-child').textContent)!==0 || p.classList.contains('is-empty'))`));
    check('Departments with openings precede empty departments',await ev(`(()=>{let empty=false;return [...document.querySelectorAll('.department-filter')].every(p=>{const count=Number(p.querySelector('span:last-child').textContent);if(!count)empty=true;return !empty || count===0})})()`));
    const location=await ev(`document.querySelector('.job-card').dataset.location`);
    await ev(`document.getElementById('location').value=${JSON.stringify(location)};document.getElementById('location').dispatchEvent(new Event('change'))`);
    check('Location filters DOM-backed data',await ev(`[...document.querySelectorAll('.job-card:not([hidden])')].every(c=>c.dataset.location===${JSON.stringify(location)})`));
    await page.click('#clear-filters');
  }
  results.contrast=await ev(`(()=>{const rgb=c=>c.match(/[0-9.]+/g).slice(0,3).map(Number);const lum=c=>rgb(c).map(v=>{v/=255;return v<=.04045?v/12.92:((v+.055)/1.055)**2.4}).reduce((s,v,i)=>s+v*[.2126,.7152,.0722][i],0);const ratio=(a,b)=>(Math.max(lum(a),lum(b))+.05)/(Math.min(lum(a),lum(b))+.05);const bg=e=>{while(e){const c=getComputedStyle(e).backgroundColor;if(c!=='rgba(0, 0, 0, 0)'&&c!=='transparent')return c;e=e.parentElement}return 'rgb(255,255,255)'};const text=[...document.querySelectorAll('body *')].filter(e=>e.getClientRects().length&&[...e.childNodes].some(n=>n.nodeType===3&&n.textContent.trim())).map(e=>{const c=getComputedStyle(e);return {text:e.textContent.trim().slice(0,40),ratio:ratio(c.color,bg(e)),required:parseFloat(c.fontSize)>=24||(parseFloat(c.fontSize)>=18.66&&Number(c.fontWeight)>=600)?3:4.5}});const controls=[...document.querySelectorAll('input,select,.department-filter:not([aria-pressed="true"])')].map(e=>({name:e.id||e.textContent.trim().slice(0,40),ratio:ratio(getComputedStyle(e).borderTopColor,bg(e))}));return {minText:Math.min(...text.map(t=>t.ratio)),textFailures:text.filter(t=>t.ratio<t.required),controlFailures:controls.filter(t=>t.ratio<3)}})()`);
  check('Homepage text and control contrast',results.contrast.textFailures.length===0&&results.contrast.controlFailures.length===0,results.contrast);
  await page.send('Emulation.setEmulatedMedia',{features:[{name:'prefers-reduced-motion',value:'reduce'}]});
  check('Reduced motion keeps instant scrolling',await ev(`getComputedStyle(document.documentElement).scrollBehavior==='auto'`));
  const ax=await page.send('Accessibility.getFullAXTree');
  check('Accessible names for practical filters',ax.nodes.some(n=>n.name?.value==='Job Title or Skill')&&ax.nodes.some(n=>n.name?.value==='Location'));
  if(titles.length) {
    const path=await ev(`document.querySelector('.job-link').getAttribute('href')`);
    await page.click('.job-card',{x:12,y:100});await pause(300);
    check('Actual card activation navigates to role',await ev(`(location.pathname.endsWith('/')?location.pathname.slice(0,-1):location.pathname)===${JSON.stringify(path)} && !!document.querySelector('.job-description')`));
    for(const width of [1440,1024,768,375]) {
      await page.viewport(width,900,width===375);await ev('scrollTo(0,0)');
      check(`Detail overflow ${width}`,await ev('document.documentElement.scrollWidth<=innerWidth'));
      await screenshot(page,resolve(out,`${round}-detail-${width}.png`));
    }
    check('Detail keeps metadata and structured sections',await ev(`document.querySelector('.role-facts').textContent.includes('Salary') && document.querySelector('.role-facts').textContent.includes('Posted Date') && document.body.textContent.includes('Overview & Responsibilities') && document.body.textContent.includes('Requirements')`));
    check('Detail browser title is evaluated',await ev(`document.title===document.querySelector('h1').textContent.trim()+' | Mộc Careers'`));
    check('Submission availability is disclosed before login',await ev(`document.body.textContent.includes('Online submission is not available yet')`));
    if(!fixture) {
      await page.click('.role-page-top .button');await pause(200);
      check('Guest apply redirects to real login',await ev(`location.pathname==='/login' && !!document.querySelector('input[name="password"]')`));
    }
  }
  if(fixture) {
    const auth=await b.tab(base+'/preview-authenticated.html');await auth.viewport(1440);
    await auth.evaluate('document.fonts.ready');
    check('Authenticated fixture header has real form markup',await auth.evaluate(`document.body.textContent.includes('Taylor Nguyen') && !document.querySelector('.desktop-account a[href="/login"]') && document.querySelector('.desktop-account form').getAttribute('action')==='/logout' && !!document.querySelector('.desktop-account input[name="_csrf"]')`));
    await screenshot(auth,resolve(out,`${round}-authenticated-1440.png`),false);
    await auth.viewport(375);await auth.click('.mobile-menu summary');
    check('Mobile logged-in controls are visible',await auth.evaluate(`document.querySelector('.mobile-menu').open && document.querySelector('.mobile-account').textContent.includes('Log Out')`));
    await screenshot(auth,resolve(out,`${round}-authenticated-375.png`),false);
    const apply=await b.tab(base+'/preview-apply.html');
    await apply.evaluate('document.fonts.ready');
    for(const width of [1440,1024,768,375]) {
      await apply.viewport(width,900,width===375);
      check(`Application layout ${width}`,await apply.evaluate(`document.documentElement.scrollWidth<=innerWidth && getComputedStyle(document.querySelector('.application-fields')).gridTemplateColumns.split(' ').length===${width<=600?1:2}`));
      await screenshot(apply,resolve(out,`${round}-apply-${width}.png`));
    }
    check('Prefill and honest disabled submission',await apply.evaluate(`document.getElementById('full-name').value==='Taylor Nguyen' && document.getElementById('full-name').readOnly && document.getElementById('cv').disabled && document.querySelector('.application-actions button').disabled && document.body.textContent.includes('Online submission is not available yet')`));
    check('Application title and read-only missing-data state',await apply.evaluate(`document.title==='Apply for Senior Java Engineer | Mộc Careers' && document.querySelector('.account-review legend').textContent.includes('Read-only') && document.getElementById('portfolio').placeholder==='Not provided' && !document.getElementById('portfolio').value`));
    results.errors.push(...auth.events,...apply.events);
    results.errors=results.errors.filter(e=>e.method==='Runtime.exceptionThrown'||(e.method==='Log.entryAdded'&&e.params.entry.level==='error'));
  }
  results.errors.push(...page.events.filter(e=>e.method==='Runtime.exceptionThrown'||(e.method==='Log.entryAdded'&&e.params.entry.level==='error')));
  check('No page runtime errors',results.errors.length===0,results.errors);
} catch(error) {results.errors.push(String(error));check('Harness completes',false,String(error));}
finally {await writeFile(resolve(out,`${round}-results.json`),JSON.stringify(results,null,2));await b.close();}
console.log(JSON.stringify({round,pass:results.checks.filter(c=>c.pass).length,fail:results.checks.filter(c=>!c.pass)},null,2));
process.exitCode=results.checks.some(c=>!c.pass)?1:0;
