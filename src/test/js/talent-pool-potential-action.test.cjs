const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {resolve} = require('node:path');
const {runInNewContext} = require('node:vm');

function element() {
    const handlers = new Map();
    return {hidden:false, disabled:false, textContent:'', value:'', children:[], attributes:{},
        append(...nodes) { this.children.push(...nodes); }, replaceChildren() { this.children=[]; },
        setAttribute(key,value) { this.attributes[key]=value; }, setCustomValidity(value) { this.validation=value; },
        reportValidity() { this.validated=true; return !this.validation; }, focus() { this.focused=true; },
        addEventListener(name,handler) { handlers.set(name,handler); },
        fire(name) { return handlers.get(name)?.({preventDefault() {}}); }};
}
function fixture({saved=false, notes=[], readFailure=false, writeFailure=false, falseSave=false, token=true}={}) {
    const elements = new Map(), requests=[];
    const get=key => { if(!elements.has(key)) elements.set(key,element()); return elements.get(key); };
    get('[data-tp-form]').action='/talent-pool/applications/218/bookmark';
    get('[data-tp-panel]').hidden=true;
    let server={candidateId:8,saved,notes:[...notes]}, failRead=readFailure;
    const root={querySelector:get,setAttribute() {}};
    const document={querySelectorAll:()=>[root],createElement:element,
        querySelector:selector=>selector.includes('_csrf_header')?{content:'X-CSRF-TOKEN'}:token?{content:'csrf-token'}:null};
    class FormData { constructor() { this.content=get('[data-tp-content]').value; } }
    runInNewContext(readFileSync(resolve(__dirname,'../../main/resources/static/js/pages/talent-pool-potential-action.js'),'utf8'),{
        document,FormData,AbortSignal,
        fetch:async(url,options)=>{
            requests.push({url,...options});
            const failure=options.method==='POST'?writeFailure:failRead;
            if(options.method==='POST' && !failure) server={...server,saved:!falseSave,
                notes:options.body.content.trim()?[{noteId:2,author:'HM Tuấn',createdAt:'2026-10-17T14:00:00',content:options.body.content.trim()},...server.notes]:server.notes};
            return {ok:!failure,status:failure?400:200,headers:{get:()=> 'application/json'},
                json:async()=>failure?{message:'Không lưu được ghi chú.'}:structuredClone(server)};
        },
    });
    return {get,requests,server:()=>server,allowRead:()=>{failRead=false;}};
}
const tick=()=>new Promise(setImmediate);

test('previously saved candidate loads real notes without writing a bookmark or copying interview evaluation',async()=>{
    const ui=fixture({saved:true,notes:[{author:'HR Lan',createdAt:'2026-10-16T14:00:00',content:'Ghi chú trước'}]}); await tick();
    assert.equal(ui.get('[data-tp-saved]').hidden,false); assert.equal(ui.get('[data-tp-open]').hidden,true);
    assert.equal(ui.get('[data-tp-notes]').disabled,false); assert.equal(ui.get('[data-tp-submit]').disabled,true);
    assert.equal(ui.get('[data-tp-note-list]').children[0].children[1].textContent,'Ghi chú trước');
    assert.equal(ui.requests.length,1); assert.equal(ui.requests[0].method,undefined);
});
test('unsaved candidate can save without a note, with CSRF, and duplicate submit is blocked',async()=>{
    const ui=fixture(); await tick(); assert.equal(ui.get('[data-tp-open-label]').textContent,'Lưu ứng viên tiềm năng');
    ui.get('[data-tp-open]').fire('click'); await tick();
    assert.equal(ui.get('[data-tp-panel]').hidden,false);
    const saving=ui.get('[data-tp-form]').fire('submit'); ui.get('[data-tp-form]').fire('submit');
    assert.equal(ui.get('[data-tp-submit]').disabled,true); await saving;
    const writes=ui.requests.filter(request=>request.method==='POST'); assert.equal(writes.length,1);
    assert.equal(writes[0].url,'/talent-pool/applications/218/bookmark'); assert.equal(writes[0].headers['X-CSRF-TOKEN'],'csrf-token');
    assert.equal(writes[0].body.content,''); assert.equal(ui.get('[data-tp-saved]').hidden,false);
    assert.match(ui.get('[data-tp-feedback]').textContent,/Đã lưu ứng viên/);
});
test('adding a shared note preserves history and renders text literally',async()=>{
    const ui=fixture({saved:true,notes:[{author:'HR Lan',content:'Ghi chú cũ'}]}); await tick();
    ui.get('[data-tp-content]').value='<script>not evaluation</script>'; ui.get('[data-tp-content]').fire('input');
    assert.equal(ui.get('[data-tp-submit]').disabled,false); await ui.get('[data-tp-form]').fire('submit');
    const list=ui.get('[data-tp-note-list]').children;
    assert.equal(list.length,2); assert.equal(list[0].children[1].textContent,'<script>not evaluation</script>');
    assert.equal(list[1].children[1].textContent,'Ghi chú cũ'); assert.equal(ui.get('[data-tp-content]').value,'');
    assert.equal(ui.requests.filter(r=>r.method==='POST').length,1);
});
test('failed save leaves candidate state and note draft intact, requiring read-only verification before retry',async()=>{
    const ui=fixture({writeFailure:true}); await tick();
    ui.get('[data-tp-content]').value='Giữ bản nháp'; await ui.get('[data-tp-form]').fire('submit');
    assert.equal(ui.get('[data-tp-saved]').hidden,true); assert.equal(ui.get('[data-tp-content]').value,'Giữ bản nháp');
    assert.equal(ui.get('[data-tp-feedback]').textContent,''); assert.equal(ui.get('[data-tp-submit]').disabled,true);
    assert.equal(ui.get('[data-tp-retry]').hidden,false); assert.match(ui.get('[data-tp-error]').textContent,/Không lưu được/);
    await ui.get('[data-tp-form]').fire('submit'); assert.equal(ui.requests.filter(r=>r.method==='POST').length,1);
    await ui.get('[data-tp-retry]').fire('click'); assert.equal(ui.get('[data-tp-submit]').disabled,false);
});
test('read failure never guesses unsaved state and retry does not perform a write',async()=>{
    const ui=fixture({saved:true,readFailure:true}); await tick();
    assert.equal(ui.get('[data-tp-open]').disabled,true); assert.equal(ui.get('[data-tp-open-label]').textContent,'Chưa tải được trạng thái');
    ui.allowRead(); await ui.get('[data-tp-retry]').fire('click');
    assert.equal(ui.get('[data-tp-saved]').hidden,false); assert.equal(ui.requests.filter(r=>r.method==='POST').length,0);
});
test('already saved candidate cannot append an empty note or trigger bookmark removal',async()=>{
    const ui=fixture({saved:true}); await tick(); ui.get('[data-tp-content]').value='   ';
    await ui.get('[data-tp-form]').fire('submit'); assert.equal(ui.requests.filter(r=>r.method==='POST').length,0);
    ui.get('[data-tp-notes]').fire('click'); await tick(); ui.get('[data-tp-close]').fire('click');
    assert.ok(ui.requests.every(r=>r.url==='/talent-pool/applications/218/bookmark')); assert.equal(ui.server().saved,true);
});
test('missing CSRF token blocks POST rather than reporting success',async()=>{
    const ui=fixture({token:false}); await tick(); await ui.get('[data-tp-form]').fire('submit');
    assert.equal(ui.requests.filter(r=>r.method==='POST').length,0); assert.equal(ui.get('[data-tp-feedback]').textContent,'');
    assert.match(ui.get('[data-tp-error]').textContent,/Thiếu thông tin phiên bảo mật/);
});
test('unexpected saved=false after POST is not reported as a successful bookmark',async()=>{
    const ui=fixture({falseSave:true}); await tick(); await ui.get('[data-tp-form]').fire('submit');
    assert.equal(ui.get('[data-tp-saved]').hidden,true); assert.equal(ui.get('[data-tp-feedback]').textContent,'');
    assert.match(ui.get('[data-tp-error]').textContent,/chưa xác nhận lưu/);
});
