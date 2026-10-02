// Windows Chrome CDP check for the standalone Mộc homepage. Start localhost:8766 per the test doc.
// No npm packages. Creates a private temporary Chrome profile and removes only that validated directory.
import {spawn} from 'node:child_process';
import {mkdtemp,writeFile,mkdir,realpath,rm} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join,resolve,dirname,basename} from 'node:path';

export const pause = ms => new Promise(r=>setTimeout(r,ms));
export async function browser(port=9237) {
  const profile=await mkdtemp(join(tmpdir(),'moc-careers-'));
  const proc=spawn('C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',['--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check',`--remote-debugging-port=${port}`,'--remote-debugging-address=127.0.0.1',`--user-data-dir=${profile}`],{windowsHide:true,stdio:'ignore'});
  let endpoint;
  for(let i=0;i<60;i++){try{endpoint=await(await fetch(`http://127.0.0.1:${port}/json/version`)).json();break}catch{await pause(100)}}
  if(!endpoint) throw Error('Chrome CDP did not start');
  const tabs=[];
  async function tab(url){
    const info=await(await fetch(`http://127.0.0.1:${port}/json/new?${encodeURIComponent(url)}`,{method:'PUT'})).json();
    const socket=new WebSocket(info.webSocketDebuggerUrl); await new Promise((r,j)=>{socket.onopen=r;socket.onerror=j});
    let serial=0;const pending=new Map();const events=[];
    socket.onmessage=e=>{const m=JSON.parse(e.data);if(m.id){const p=pending.get(m.id);if(p){pending.delete(m.id);m.error?p.reject(Error(JSON.stringify(m.error))):p.resolve(m.result)}}else events.push(m)};
    const send=(method,params={})=>new Promise((resolve,reject)=>{const id=++serial;pending.set(id,{resolve,reject});socket.send(JSON.stringify({id,method,params}));});
    await send('Page.enable');await send('Runtime.enable');await send('Log.enable');await pause(200);
    const evaluate=async expression=>{const r=await send('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(r.exceptionDetails)throw Error(JSON.stringify(r.exceptionDetails));return r.result.value;};
    const viewport=async(width,height=900,mobile=false)=>{await send('Emulation.setDeviceMetricsOverride',{width,height,deviceScaleFactor:1,mobile});await pause(100);};
    const key=async(key,shift=false)=>{const code={Tab:9,Enter:13,Escape:27,ArrowDown:40,Home:36,End:35}[key]||0;await send('Input.dispatchKeyEvent',{type:'keyDown',key,windowsVirtualKeyCode:code,nativeVirtualKeyCode:code,modifiers:shift?8:0,...(key==='Enter'?{text:'\r',unmodifiedText:'\r'}:{})});await send('Input.dispatchKeyEvent',{type:'keyUp',key,windowsVirtualKeyCode:code,nativeVirtualKeyCode:code,modifiers:shift?8:0});await pause(80);};
    const click=async(selector,point)=>{await evaluate(`document.querySelector(${JSON.stringify(selector)}).scrollIntoView({block:'center',behavior:'instant'})`);const rect=await evaluate(`(()=>{const r=document.querySelector(${JSON.stringify(selector)}).getBoundingClientRect();return {x:r.x,y:r.y,width:r.width,height:r.height}})()`);const x=rect.x+(point?.x??rect.width/2),y=rect.y+(point?.y??rect.height/2);await send('Input.dispatchMouseEvent',{type:'mousePressed',x,y,button:'left',clickCount:1});await send('Input.dispatchMouseEvent',{type:'mouseReleased',x,y,button:'left',clickCount:1});await pause(140);};
    const screenshot=async(path,full=true)=>{const metrics=await send('Page.getLayoutMetrics');const s=metrics.cssContentSize;const r=await send('Page.captureScreenshot',{format:'png',captureBeyondViewport:full,...(full?{clip:{x:0,y:0,width:s.width,height:s.height,scale:1}}:{})});await writeFile(path,Buffer.from(r.data,'base64'));};
    const page={send,evaluate,viewport,key,click,screenshot,events,socket};tabs.push(page);
    let ready=false;
    for(let i=0;i<100;i++){if(await evaluate("document.readyState==='complete' && Boolean(document.querySelector('main'))")){ready=true;break}await pause(100)}
    if(!ready)throw Error('Target page did not finish loading its main element: '+url);
    return page;
  }
  async function close(){
    for(const t of tabs){t.socket.close()}proc.kill();await pause(200);
    const tempRoot=await realpath(tmpdir());const actual=await realpath(profile);
    if(dirname(actual).toLowerCase()!==tempRoot.toLowerCase()||!basename(actual).startsWith('moc-careers-'))throw Error('Refusing cleanup outside this task Chrome profile');
    await rm(actual,{recursive:true,force:true,maxRetries:3,retryDelay:200});
  }
  return {tab,close,profile};
}
