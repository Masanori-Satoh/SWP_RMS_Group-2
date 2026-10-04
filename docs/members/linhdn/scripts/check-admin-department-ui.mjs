// Browser QA of actual Thymeleaf output. Confirmed POSTs intercepted: no DB writes or live E2E claim.
import {browser, pause} from './career-browser.mjs';
import {mkdir, writeFile} from 'node:fs/promises';
import {resolve} from 'node:path';
const base=process.argv[2]||'http://127.0.0.1:8773';
const round=process.argv[3]||'round1';
if(!['round1','round2'].includes(round)) throw Error('Use round1 or round2');
const output=resolve('docs/members/linhdn/tests/assets/2026-10-04-admin-departments');
await mkdir(output,{recursive:true});
const checks=[];
const check=(name,pass,evidence)=>checks.push({name,pass:!!pass,evidence});
const chrome=await browser(9263);
try {
    for(const name of ['departments','new','edit','validation','accounts','candidates','dashboard']) {
        const page=await chrome.tab(`${base}/preview/${name}/`); await page.evaluate('document.fonts.ready');
        for(const width of [1440,1024,768,375]) {
            await page.viewport(width,900);
            const info=await page.evaluate(`(()=>{
                const visible=e=>!!(e.getBoundingClientRect().width&&e.getBoundingClientRect().height);
                return {width:innerWidth,scroll:document.documentElement.scrollWidth,lang:document.documentElement.lang,
                    fonts:[getComputedStyle(document.querySelector('h1')).fontFamily,getComputedStyle(document.body).fontFamily],
                    missing:[...document.querySelectorAll('[aria-controls],[aria-labelledby],[aria-describedby]')].flatMap(e=>['aria-controls','aria-labelledby','aria-describedby'].flatMap(a=>(e.getAttribute(a)||'').split(' ').filter(Boolean).filter(id=>!document.getElementById(id)))),
                    unlabeled:[...document.querySelectorAll('input:not([type=hidden]),select')].filter(e=>!e.labels.length).map(e=>e.id),
                    small:[...document.querySelectorAll('button,input:not([type=hidden]),select,summary')].filter(visible).filter(e=>e.getBoundingClientRect().height<43).map(e=>e.id||e.textContent.trim()),
                    csrf:[...document.querySelectorAll('form[method=post]')].every(f=>!!f.querySelector('input[name=_csrf]')),
                    departmentLink:!!document.querySelector('a[href="/admin/departments"]'),
                    requisition:!!document.querySelector('a[href="/requisitions"]'),
                    styles:[...document.styleSheets].every(s=>{try{return s.cssRules.length>0}catch{return false}})};
            })()`);
            check(`${name}/${width}: no page overflow and shared typography`,info.scroll<=width&&info.fonts[0].includes('Lora')&&info.fonts[1].includes('Source Sans 3'),info);
            check(`${name}/${width}: Vietnamese navigation/CSRF/assets`,info.lang==='vi'&&info.departmentLink&&!info.requisition&&info.csrf&&info.styles,info);
            check(`${name}/${width}: labeled controls, ARIA references, tap size`,!info.missing.length&&!info.unlabeled.length&&!info.small.length,info);
            if(width===1440||width===375) await page.screenshot(resolve(output,`${round}-${name}-${width}.png`));
        }
        if(['departments','accounts','candidates'].includes(name)) {
            await page.evaluate(`window.submitted=[]; document.addEventListener('submit',event=>{
                if(event.target.matches('form[data-status-confirm]')&&!event.defaultPrevented){window.submitted.push({url:new URL(event.target.action).pathname,csrf:event.target.querySelector('input[name=_csrf]').value});event.preventDefault();}
            });`);
            for(const kind of ['deactivate','activate']) {
                const selector=`form[action$="/${kind}"] button`;
                await page.click(selector);
                check(`${name}/${kind}: native modal and safe initial focus`,await page.evaluate(`document.getElementById('status-dialog').open&&document.activeElement.id==='status-cancel'`));
                await page.key('Escape'); await pause(50);
                check(`${name}/${kind}: Escape retains data and restores trigger`,await page.evaluate(`!document.getElementById('status-dialog').open&&document.activeElement.matches(${JSON.stringify(selector)})`));
                await page.key('Enter'); await page.click('#status-cancel');
                check(`${name}/${kind}: keyboard open and cancel never POST`,await page.evaluate(`!document.getElementById('status-dialog').open&&document.activeElement.matches(${JSON.stringify(selector)})&&window.submitted.length===${kind==='deactivate'?0:1}`));
                await page.click(selector); await page.click('#status-confirm');
                check(`${name}/${kind}: confirm uses original scoped route with CSRF`,await page.evaluate(`window.submitted.length===${kind==='deactivate'?1:2}&&window.submitted.at(-1).url.endsWith('/${kind}')&&window.submitted.at(-1).csrf==='visual-fixture-only'`));
            }
        }
        check(`${name}: no JavaScript runtime errors`,!page.events.some(e=>e.method==='Runtime.exceptionThrown'));
    }
} finally { await chrome.close(); }
const result={round,scope:'sanitized MockMvc fixture, confirmed POST intercepted; not live DB/browser E2E',passed:checks.filter(c=>c.pass).length,total:checks.length,checks};
await writeFile(resolve(output,`${round}.json`),JSON.stringify(result,null,2));
console.log(JSON.stringify({passed:result.passed,total:result.total,failed:checks.filter(c=>!c.pass)},null,2));
if(result.passed!==result.total) process.exitCode=1;
