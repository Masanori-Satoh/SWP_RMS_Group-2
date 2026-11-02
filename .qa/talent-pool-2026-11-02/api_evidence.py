"""Execute real local RMS requests and save reviewable HTTP/DB evidence."""
import sys, re, json, subprocess, html
from pathlib import Path
from datetime import datetime
from zoneinfo import ZoneInfo
import requests

ROOT = Path(__file__).resolve().parent
BASE = 'http://localhost:8081'
DB = 'RitirementManagement2'

def sql(query):
    out = subprocess.check_output(['sqlcmd','-S','localhost','-E','-C','-d',DB,'-f','65001','-y','0','-Q','SET QUOTED_IDENTIFIER ON; SET ANSI_NULLS ON; SET NOCOUNT ON; '+query], text=True, encoding='utf-8')
    if re.search(r'^Msg \d+', out, re.M): raise RuntimeError(out)
    return out.strip()

def rows(query):
    return json.loads(sql(query+' FOR JSON PATH') or '[]')

def snapshot():
    return {
        'candidates': rows('SELECT CandidateId,IsPotential FROM Candidate WHERE CandidateId IN (1,3,4,7)'),
        'notes': rows('SELECT NoteId,CandidateId,UserId,LEN(NoteContent) AS Characters FROM TalentPoolNote WHERE CandidateId IN (1,3,4)'),
        'applications': rows('SELECT ApplicationId,CandidateId,JobPostingId,ApplicationStatus FROM Application WHERE CandidateId IN (1,3,4)'),
        'audit': rows("SELECT AuditLogId,UserId,EntityName,EntityId,NewValue FROM AuditLog WHERE NewValue IN ('TalentPoolMarked','TalentPoolUnmarked','TalentPoolSuggested','TalentPoolHrRecommended')")
    }

class Client:
    def __init__(self, user):
        self.user=user
        self.s=requests.Session()
        page=self.s.get(BASE+'/login',timeout=20)
        token=self.token(page.text)
        password=re.search(r"-- Password: '([^']+)'",(ROOT.parents[1]/'database/seeds/seed_data.sql').read_text(encoding='utf-8')).group(1)
        r=self.s.post(BASE+'/login',data={'username':user,'password':password,'_csrf':token},allow_redirects=False,timeout=30)
        if r.status_code!=302 or '/login' in r.headers.get('Location',''): raise RuntimeError('Login failed for '+user)
    @staticmethod
    def token(source):
        match=re.search(r'<input[^>]*name="_csrf"[^>]*value="([^"]+)"',source)
        if not match: match=re.search(r'<meta[^>]*name="_csrf"[^>]*content="([^"]+)"',source)
        if not match: raise RuntimeError('CSRF not found')
        return html.unescape(match.group(1))
    def post(self,path,data=None,payload=None):
        page=self.s.get(BASE+'/profile',timeout=20)
        csrf=self.token(page.text)
        if payload is not None:
            return self.s.post(BASE+path,json=payload,headers={'X-CSRF-TOKEN':csrf},allow_redirects=False,timeout=30)
        return self.s.post(BASE+path,data={**(data or {}),'_csrf':csrf},allow_redirects=False,timeout=30)

def evidence(case,title,client,path,data=None,payload=None,repeats=1):
    before=snapshot(); responses=[]
    for _ in range(repeats):
        r=client.post(path,data,payload)
        response={'status':r.status_code,'location':r.headers.get('Location'),'content_type':r.headers.get('Content-Type')}
        try: response['body']=r.json()
        except ValueError:
            text=re.sub('<[^>]+>',' ',r.text)
            response['body']=' '.join(html.unescape(text).split())[:2200]
        if r.is_redirect:
            try:
                follow=client.s.get(requests.compat.urljoin(BASE,response['location']),timeout=10)
                messages=re.findall(r'(?:Vui lòng nhập ghi chú\.|Ghi chú tối đa [\d.,]+ ký tự\.|Hãy đánh dấu ứng viên tiềm năng trước khi thêm ghi chú\.|Đã thêm ghi chú\.)',follow.text)
                response['redirect_messages']=list(dict.fromkeys(messages))
            except requests.exceptions.RequestException as error:
                response['redirect_error']=str(error)
        responses.append(response)
    after=snapshot()
    record={'case':case,'title':title,'time':datetime.now().astimezone().isoformat(),'base_url':BASE,'account':client.user,'method':'POST','path':path,'payload':payload if payload is not None else {k:('A × '+str(len(v)) if k=='content' and len(v)>100 else v) for k,v in (data or {}).items()},'responses':responses,'db_before':before,'db_after':after,'unchanged':before==after}
    (ROOT/(case+'.json')).write_text(json.dumps(record,ensure_ascii=False,indent=2),encoding='utf-8')
    unchanged=before==after
    changed={k:{'before':before[k],'after':after[k]} for k in before if before[k]!=after[k]}
    body=f'''<!doctype html><html lang="vi"><meta charset="utf-8"><title>{case} — real HTTP evidence</title><style>body{{font:18px system-ui;background:#f4f6f3;color:#19251e;margin:32px auto;max-width:1440px;padding:0 32px}}h1{{font-size:28px}}.card{{background:white;border:1px solid #cad4cc;border-radius:8px;padding:22px;margin:14px 0}}pre{{white-space:pre-wrap;overflow-wrap:anywhere;font:16px/1.5 Consolas,monospace}}.meta{{display:flex;gap:32px;color:#41574a}}h2{{font-size:20px}}.badge{{padding:6px 14px;background:#e6eee8;display:inline-block;border-radius:5px}}</style><h1>{case} · {html.escape(title)}</h1><div class="meta"><span>RMS local · {BASE}</span><span>Account: <b>{client.user}</b></span><span>{record['time']}</span></div><div class="card"><h2>Actual request — authenticated session with valid CSRF</h2><pre>POST {html.escape(path)}\n{html.escape(json.dumps(record['payload'],ensure_ascii=False,indent=2))}</pre></div><div class="card"><h2>Actual server response</h2><pre>{html.escape(json.dumps(responses,ensure_ascii=False,indent=2))}</pre></div><div class="card"><h2>SQL Server verification</h2><div class="badge">{'Candidate flags, notes, applications and mark/suggest audit records unchanged' if unchanged else 'Database changes captured below'}</div><pre>{html.escape(json.dumps(changed if changed else {'candidates':after['candidates'],'note_count':len(after['notes']),'application_count':len(after['applications']),'mark_suggest_audit_count':len(after['audit'])},ensure_ascii=False,indent=2))}</pre></div><p>API integration evidence from live requests; this page is an evidence viewer, not an RMS product screen. Full before/after snapshots are stored in {case}.json.</p></html>'''
    (ROOT/(case+'.html')).write_text(body,encoding='utf-8')
    print(case, [r['status'] for r in responses], 'DB unchanged:',unchanged,flush=True)
    return record

if __name__=='__main__':
    requested=set(sys.argv[1:])
    clients={}
    def client(u):
        if u not in clients: clients[u]=Client(u)
        return clients[u]
    cases={
      '017':('Candidate cannot mark potential candidate','phong.nguyen','/talent-pool/applications/4/mark',{},None,1),
      '018':('Interviewer cannot mark potential candidate','interviewer_nam','/talent-pool/applications/4/mark',{},None,1),
      '019':('Mark with nonexistent Application ID','hr_lan','/talent-pool/applications/999999/mark',{},None,1),
      '020':('Mark candidate already marked potential','hr_lan','/talent-pool/applications/1/mark',{},None,2),
      '022':('Unmark candidate who is not potential','hr_lan','/talent-pool/applications/4/bookmark/remove',{},None,1),
      '025':('Whitespace-only note reaches server','hr_lan','/talent-pool/1/notes',{'content':'   '},None,1),
      '027':('Reject note exceeding 1000 characters','hr_lan','/talent-pool/1/notes',{'content':'A'*1001},None,1),
      '028':('Reject note for unmarked candidate','hr_lan','/talent-pool/4/notes',{'content':'Liên hệ phỏng vấn tuần sau'},None,1),
      '033':('Cannot suggest candidate already applied','hm_tuan','/talent-pool/jobs/1/suggest',None,{'sourceApplicationIds':[199]},1),
      '034':('Cannot suggest unmarked candidate','hm_tuan','/talent-pool/jobs/2/suggest',None,{'sourceApplicationIds':[4]},1),
      '035':('Cannot suggest outside department','hm_tuan','/talent-pool/jobs/4/suggest',None,{'sourceApplicationIds':[3]},1),
      '036':('Cannot suggest to Paused posting','hm_tuan','/talent-pool/jobs/16/suggest',None,{'sourceApplicationIds':[1]},1),
      '038':('Cannot suggest to Closed posting','hm_khanh','/talent-pool/jobs/15/suggest',None,{'sourceApplicationIds':[1]},1),
    }
    (ROOT/'baseline.json').write_text(json.dumps(snapshot(),ensure_ascii=False,indent=2),encoding='utf-8') if not (ROOT/'baseline.json').exists() else None
    for n in requested:
        if n not in cases: raise ValueError(n)
        title,user,path,data,payload,repeats=cases[n]
        if n in {'022','028','034'}:
            original=rows('SELECT IsPotential FROM Candidate WHERE CandidateId=4')[0]['IsPotential']
            sql('UPDATE Candidate SET IsPotential=0 WHERE CandidateId=4')
            try: evidence('TC-PC-'+n,title,client(user),path,data,payload,repeats)
            finally: sql('UPDATE Candidate SET IsPotential='+str(int(original))+' WHERE CandidateId=4')
        else: evidence('TC-PC-'+n,title,client(user),path,data,payload,repeats)
