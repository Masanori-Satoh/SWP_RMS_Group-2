// Independent Assessment A interaction evidence. No screenshots or form submissions.
import {writeFile} from 'node:fs/promises';
import {browser, pause} from '../../../../scripts/career-browser.mjs';

const routes = ['login','register','dashboard','accounts','create','edit','login-error','login-logout','register-invalid','create-invalid','edit-invalid','forgot','forgot-sent','reset','reset-expired','monitoring'];
const widths = [1440,1024,768,375];
const result = {method:'Independent fresh Chrome CDP tabs; read-only preview plus reversible controls; no screenshots; no POST',pages:[],controls:[],errors:[]};
const chrome = await browser(9251);
try {
  for (const route of routes) {
    const tab = await chrome.tab(`http://127.0.0.1:8768/preview/${route}/`);
    await tab.evaluate('document.fonts.ready.then(() => true)');
    await pause(100);
    for (const width of widths) {
      await tab.viewport(width,900,width===375);
      const info = await tab.evaluate(`(() => {
        const main=document.querySelector('main');
        const bounds=main.getBoundingClientRect();
        const table=document.querySelector('.table-wrapper');
        return {title:document.title,h1:main.querySelector('h1')?.textContent.trim(),bodyClass:document.body.className,pageWidth:Math.max(document.body.scrollWidth,document.documentElement.scrollWidth),viewport:innerWidth,main:{x:bounds.x,y:bounds.y,width:bounds.width,height:bounds.height},fontsLoaded:document.fonts.check('600 36px Lora')&&document.fonts.check('400 16px "Source Sans 3"'),table:table?{client:table.clientWidth,scroll:table.scrollWidth}:null,focus:document.activeElement?.id||document.activeElement?.tagName,alerts:[...document.querySelectorAll('[role="alert"],[role="status"]')].map(e=>e.textContent.trim()),navigation:[...document.querySelectorAll('.workspace-nav a')].map(e=>({label:e.textContent.trim(),current:e.getAttribute('aria-current')}))};
      })()`);
      result.pages.push({route,width,...info});
    }
    if (await tab.evaluate('Boolean(document.querySelector(".password-toggle"))')) {
      await tab.click('.password-toggle');
      const shown=await tab.evaluate('({type:document.querySelector(".password-field input").type,pressed:document.querySelector(".password-toggle").getAttribute("aria-pressed"),label:document.querySelector(".password-toggle").getAttribute("aria-label")})');
      await tab.click('.password-toggle');
      const hidden=await tab.evaluate('document.querySelector(".password-field input").type');
      result.controls.push({route,control:'Show / Hide password',shown,hidden});
    }
    if (await tab.evaluate('Boolean(document.querySelector("[data-workspace-menu]"))')) {
      await tab.click('[data-workspace-menu]');
      const opened=await tab.evaluate('!document.querySelector("#workspace-navigation").hidden');
      await tab.key('Escape');
      const dismissed=await tab.evaluate('({hidden:document.querySelector("#workspace-navigation").hidden,focus:document.activeElement===document.querySelector("[data-workspace-menu]")})');
      result.controls.push({route,control:'Mobile Menu / Escape',opened,dismissed});
    }
    if (await tab.evaluate('Boolean(document.querySelector("[data-validation-summary] a"))')) {
      await tab.click('[data-validation-summary] a');
      const target=await tab.evaluate('({focus:document.activeElement.id,invalid:document.activeElement.getAttribute("aria-invalid"),summaryLinks:document.querySelectorAll("[data-validation-summary] a").length})');
      result.controls.push({route,control:'Validation summary error link',target});
    }
    if (route==='accounts') {
      await tab.click('[data-deactivate-account]');
      const opened=await tab.evaluate('({open:document.querySelector("#deactivate-dialog").open,focus:document.activeElement.id,name:document.querySelector("#deactivate-account-name").textContent,explanation:document.querySelector("#deactivate-description").textContent.trim(),submit:document.querySelector("#deactivate-form button[type=submit]").textContent.trim()})');
      await tab.click('#deactivate-cancel');
      const cancelled=await tab.evaluate('({open:document.querySelector("#deactivate-dialog").open,focusReturned:document.activeElement===document.querySelector("[data-deactivate-account]")})');
      await tab.click('[data-deactivate-account]');
      await tab.key('Escape');
      const escaped=await tab.evaluate('({open:document.querySelector("#deactivate-dialog").open,focusReturned:document.activeElement===document.querySelector("[data-deactivate-account]")})');
      result.controls.push({route,control:'Delete opens confirmation; Cancel and Escape close; no submit',opened,cancelled,escaped});
    }
    result.errors.push(...tab.events.filter(e=>e.method==='Runtime.exceptionThrown').map(e=>({route,error:e.params.exceptionDetails.text})));
  }
} finally {
  await chrome.close();
  result.cleanup='Chrome stopped; validated temporary profile removed';
  await writeFile(new URL('./assessment-a-browser-check.json',import.meta.url),JSON.stringify(result,null,2));
}
console.log(JSON.stringify({pages:result.pages.length,controls:result.controls.length,errors:result.errors.length,overflow:result.pages.filter(p=>p.pageWidth>p.viewport+1).map(p=>({route:p.route,width:p.width,pageWidth:p.pageWidth})),cleanup:result.cleanup}));
