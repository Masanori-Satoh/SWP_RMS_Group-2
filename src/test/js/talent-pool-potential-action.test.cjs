const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {resolve} = require('node:path');
const {runInNewContext} = require('node:vm');

function element() {
    const handlers=new Map(), classes=new Set();
    return {hidden:false,disabled:false,value:'',textContent:'',children:[],dataset:{},attributes:{},open:false,
        classList:{toggle(name,on) { if(on) classes.add(name); else classes.delete(name); },contains:name=>classes.has(name)},
        append(...nodes) { this.children.push(...nodes); },replaceChildren() { this.children=[]; },
        setAttribute(key,value) { this.attributes[key]=value; },setCustomValidity(value) { this.validation=value; },
        reportValidity() { return !this.validation; },focus() { this.focused=true; },
        showModal() { this.open=true; },close() { this.open=false; handlers.get('close')?.(); },
        querySelectorAll() { return []; },closest() { return null; },
        addEventListener(name,handler) { handlers.set(name,handler); },
        fire(name) { return handlers.get(name)?.({preventDefault() {}}); }};
}
function fixture({saved=false,notes=[],readFailure=false,writeFailure=false,falseSave=false,falseRemove=false,embedded=true,invalidRead=false}={}) {
    const ids=new Map(), requests=[];
    const get=id=>{ if(!ids.has(id)) ids.set(id,element()); return ids.get(id); };
    const button=element(),label=element(),page=element(),dialog=get('app-bookmark');
    button.dataset={candidateKey:'candidate-8',name:'Ứng viên An',bookmarkUrl:'/talent-pool/applications/218/bookmark'};
    if(embedded) Object.assign(button.dataset,{savedLabel:'Đã lưu tiềm năng',unsavedLabel:'Lưu ứng viên tiềm năng'});
    else button.dataset.saved=String(saved);
    button.querySelector=()=>label;
    const close=get('close'); close.closest=()=>dialog;
    page.querySelectorAll=selector=>selector==='[data-app-close]'?[close]
        :selector==='[data-app-bookmark]'?[button]
        :selector==='[data-app-bookmark][data-bookmark-load]'?(embedded?[button]:[]):[];
    page.contains=()=>true;
    dialog.querySelectorAll=()=>[close,get('app-bookmark-remove'),get('app-bookmark-remove-submit'),get('app-bookmark-remove-cancel')];
    get('app-bookmark-remove-confirm').hidden=true;
    get('app-bookmark-submit').disabled=true;
    let server={candidateId:8,saved,notes:[...notes]}, failRead=readFailure;
    class FormData { constructor() { this.content=get('app-bookmark-content').value; this._csrf='csrf-token'; } }
    const document={getElementById:id=>id.startsWith('app-preview')?null:get(id),createElement:element,
        querySelector:selector=>selector==='.app-page'?page:null};
    runInNewContext(readFileSync(resolve(__dirname,'../../main/resources/static/js/pages/applications.js'),'utf8'),{
        document,FormData,setTimeout,clearTimeout,AbortController,AbortSignal,
        fetch:async(url,options)=>{
            requests.push({url,...options}); const writing=options.method==='POST', removing=url.endsWith('/remove');
            const failure=writing?writeFailure:failRead;
            if(writing&&!failure) server=removing?{...server,saved:falseRemove}:{...server,saved:!falseSave,
                notes:options.body.content.trim()?[{author:'HM Tuấn',createdAt:'2026-10-17T15:00:00',content:options.body.content.trim()},...server.notes]:server.notes};
            return {ok:!failure,status:failure?403:200,headers:{get:()=> 'application/json'},
                json:async()=>failure?{message:'Không thể lưu.'}:invalidRead?{saved:true}:structuredClone(server)};
        }
    });
    return {get,button,label,dialog,close,requests,server:()=>server,allowRead:()=>{failRead=false;}};
}
const tick=()=>new Promise(setImmediate);
const writes=ui=>ui.requests.filter(r=>r.method==='POST');
async function open(ui) { ui.button.fire('click'); await tick(); }

test('embedded interview uses shared modal and loads the actual saved label without writing',async()=>{
    const ui=fixture({saved:true}); await tick();
    assert.equal(ui.label.textContent,'Đã lưu tiềm năng'); assert.equal(ui.button.disabled,false);
    assert.equal(ui.button.classList.contains('app-bookmark-button--saved'),true);
    assert.equal(ui.requests.length,1); assert.equal(writes(ui).length,0); assert.equal(ui.dialog.open,false);
});
test('one button opens history and optional-note form; opening never saves or removes',async()=>{
    const ui=fixture(); await tick(); await open(ui);
    assert.equal(ui.dialog.open,true); assert.equal(writes(ui).length,0);
    assert.equal(ui.get('app-bookmark-content').required,false);
    assert.equal(ui.get('app-bookmark-submit').textContent,'Lưu ứng viên');
    assert.match(ui.get('app-bookmark-status').textContent,/Chưa có ghi chú/);
});
test('unsaved candidate saves without a note through the existing form, once with CSRF',async()=>{
    const ui=fixture(); await tick(); await open(ui);
    const pending=ui.get('app-bookmark-form').fire('submit'); ui.get('app-bookmark-form').fire('submit');
    assert.equal(ui.get('app-bookmark-submit').disabled,true); await pending;
    assert.equal(writes(ui).length,1); assert.equal(writes(ui)[0].body._csrf,'csrf-token');
    assert.equal(writes(ui)[0].url,'/talent-pool/applications/218/bookmark');
    assert.equal(ui.label.textContent,'Đã lưu tiềm năng'); assert.equal(ui.dialog.open,false);
});
test('saved candidate opens real history with text escaping, without toggling the bookmark',async()=>{
    const ui=fixture({saved:true,notes:[{author:'HR Lan',content:'<script>literal</script>',createdAt:'2026-10-16T14:00:00'}]});
    await tick(); await open(ui);
    assert.equal(ui.get('app-bookmark-notes').children[0].children[1].textContent,'<script>literal</script>');
    assert.equal(ui.get('app-bookmark-count').textContent,'1'); assert.equal(ui.get('app-bookmark-content').required,true);
    assert.equal(ui.get('app-bookmark-submit').textContent,'Thêm ghi chú'); assert.equal(writes(ui).length,0);
});
test('append note preserves prior history and the interview button saved label',async()=>{
    const ui=fixture({saved:true,notes:[{author:'HR Lan',content:'Ghi chú cũ'}]}); await tick(); await open(ui);
    ui.get('app-bookmark-content').value='Ghi chú mới'; await ui.get('app-bookmark-form').fire('submit');
    assert.equal(ui.server().notes.length,2); assert.equal(ui.server().notes[1].content,'Ghi chú cũ');
    assert.equal(ui.label.textContent,'Đã lưu tiềm năng'); await open(ui);
    assert.equal(ui.get('app-bookmark-notes').children.length,2);
});
test('saved candidate cannot submit an empty note or accidentally remove through the note form',async()=>{
    const ui=fixture({saved:true}); await tick(); await open(ui); ui.get('app-bookmark-content').value='   ';
    await ui.get('app-bookmark-form').fire('submit');
    assert.equal(writes(ui).length,0); assert.match(ui.get('app-bookmark-content').validation,/nhập ghi chú/);
});
test('failed write preserves the draft and state, locking repeated writes until GET retry',async()=>{
    const ui=fixture({writeFailure:true}); await tick(); await open(ui); ui.get('app-bookmark-content').value='Bản nháp';
    await ui.get('app-bookmark-form').fire('submit'); await ui.get('app-bookmark-form').fire('submit');
    assert.equal(writes(ui).length,1); assert.equal(ui.dialog.open,true); assert.equal(ui.get('app-bookmark-content').value,'Bản nháp');
    assert.equal(ui.button.dataset.saved,'false'); assert.equal(ui.get('app-bookmark-submit').disabled,true);
    assert.equal(ui.get('app-feedback').textContent,''); ui.get('app-bookmark-retry').fire('click'); await tick();
    assert.equal(ui.get('app-bookmark-submit').disabled,false); assert.equal(ui.get('app-bookmark-content').value,'Bản nháp');
});
test('initial read failure still allows opening the existing retry UI without inventing saved state',async()=>{
    const ui=fixture({readFailure:true,saved:true}); await tick();
    assert.equal(ui.label.textContent,'Xem ghi chú ứng viên'); assert.equal(ui.button.dataset.saved,undefined);
    await open(ui); assert.equal(ui.get('app-bookmark-submit').disabled,true);
    assert.equal(ui.get('app-bookmark-retry').hidden,false); ui.allowRead();
    ui.get('app-bookmark-retry').fire('click'); await tick();
    assert.equal(ui.label.textContent,'Đã lưu tiềm năng'); assert.equal(writes(ui).length,0);
});
test('existing in-popup remove confirmation preserves notes and allows saving again',async()=>{
    const ui=fixture({saved:true,notes:[{author:'HR Lan',content:'Giữ ghi chú'}]}); await tick(); await open(ui);
    ui.get('app-bookmark-remove').fire('click'); assert.equal(ui.get('app-bookmark-remove-confirm').hidden,false);
    assert.equal(writes(ui).length,0); ui.get('app-bookmark-remove-cancel').fire('click');
    assert.equal(ui.get('app-bookmark-remove-confirm').hidden,true);
    ui.get('app-bookmark-remove').fire('click'); await ui.get('app-bookmark-remove-submit').fire('click');
    assert.equal(ui.server().saved,false); assert.equal(ui.server().notes.length,1);
    assert.equal(writes(ui)[0].url,'/talent-pool/applications/218/bookmark/remove');
    await open(ui); await ui.get('app-bookmark-form').fire('submit'); assert.equal(ui.server().saved,true);
});
test('HTTP 200 with unexpected saved=false cannot report a successful save',async()=>{
    const ui=fixture({falseSave:true}); await tick(); await open(ui); await ui.get('app-bookmark-form').fire('submit');
    assert.equal(ui.dialog.open,true); assert.equal(ui.button.dataset.saved,'false');
    assert.equal(ui.get('app-feedback').textContent,''); assert.equal(ui.get('app-bookmark-submit').disabled,true);
});
test('HTTP 200 with unexpected saved=true cannot report a successful removal',async()=>{
    const ui=fixture({saved:true,falseRemove:true}); await tick(); await open(ui);
    ui.get('app-bookmark-remove').fire('click'); await ui.get('app-bookmark-remove-submit').fire('click');
    assert.equal(ui.dialog.open,true); assert.equal(ui.button.dataset.saved,'true'); assert.equal(ui.get('app-feedback').textContent,'');
});
test('existing Application page keeps its normal label and does not preload extra requests',async()=>{
    const ui=fixture({saved:true,embedded:false}); await tick(); assert.equal(ui.requests.length,0);
    await open(ui); assert.equal(ui.label.textContent,'Thêm ghi chú'); assert.equal(ui.dialog.open,true);
});
test('invalid response leaves the shared form locked rather than fabricating note history',async()=>{
    const ui=fixture({invalidRead:true}); await tick(); await open(ui);
    assert.equal(ui.button.dataset.saved,undefined); assert.equal(ui.get('app-bookmark-submit').disabled,true);
    assert.equal(writes(ui).length,0);
});
