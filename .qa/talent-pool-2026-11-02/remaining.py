from api_evidence import *
hr=Client('hr_lan')
original=rows('SELECT IsPotential FROM Candidate WHERE CandidateId=1')[0]['IsPotential']
try:
    evidence('TC-PC-021','Unmark potential candidate successfully',hr,'/talent-pool/applications/1/bookmark/remove',{})
finally:
    sql('UPDATE Candidate SET IsPotential='+str(int(original))+' WHERE CandidateId=1')
evidence('TC-PC-023','Add shared note successfully',hr,'/talent-pool/1/notes',{'content':'Ứng viên có kinh nghiệm Java Spring Boot, ưu tiên phỏng vấn.'})
evidence('TC-PC-026','Add note with exactly 1000 characters',hr,'/talent-pool/1/notes',{'content':'A'*1000})
