3.1 User

Column;Constraints;Notes
UserId;PRIMARY KEY, IDENTITY(1,1);User identifier
RoleId;NOT NULL, FOREIGN KEY -> Role.RoleId;Assigned role ID
Username;NOT NULL, UNIQUE;Login username
PasswordHash;NOT NULL;Hashed password (BCrypt/Argon2), never store plaintext
Email;NOT NULL, UNIQUE;Work email
FullName;NOT NULL;Full name
PhoneNumber;NULL;Phone number
AvatarUrl;NULL;Avatar image path
DepartmentId;FOREIGN KEY -> Department.DepartmentId, NULL;Department the user belongs to
AccountStatus;NOT NULL;Active, Inactive, Blocked
CreatedAt;NOT NULL, DEFAULT GETDATE();Account creation time
UpdatedAt;NULL;Last update time

3.2 Role

Column;Constraints;Notes
RoleId;PRIMARY KEY, IDENTITY(1,1);Role identifier
RoleName;NOT NULL, UNIQUE;System Admin, HR, Hiring Manager, Director, Interviewer
Description;NULL;Description of the role's scope


3.4 Department

Column;Constraints;Notes
DepartmentId;PRIMARY KEY, IDENTITY(1,1);Department identifier
DepartmentName;NOT NULL;Department name
ManagerId;FOREIGN KEY -> User.UserId, NULL;Department head
Status;NOT NULL, DEFAULT 'Active';Active status

3.5 AuditLog

Column;Constraints;Notes
AuditLogId;PRIMARY KEY, IDENTITY(1,1);Log entry identifier (BIGINT)
UserId;NOT NULL, FOREIGN KEY -> User.UserId;User who performed the action
Action;NOT NULL;CREATE, UPDATE, DELETE, LOGIN, VIEW
EntityName;NOT NULL;Name of the affected entity
EntityId;NOT NULL;Primary key of the affected record
OldValue;NULL;Old data, JSON format
NewValue;NULL;New data, JSON format
IpAddress;NULL;User's IP address
Timestamp;NOT NULL, DEFAULT GETDATE();Time the action occurred

3.6 SystemConfig

Column;Constraints;Notes
ConfigKey;PRIMARY KEY;e.g. AI_SCREENING_ENDPOINT, EMAIL_SMTP
ConfigValue;NOT NULL;Configuration value
Description;NULL;Purpose of the configuration
UpdatedAt;NOT NULL, DEFAULT GETDATE();Last update time

3.7 JobRequisition

Column;Constraints;Notes
RequisitionId;PRIMARY KEY, IDENTITY(1,1);Requisition identifier
Title;NOT NULL;Proposed job title
DepartmentId;NOT NULL, FOREIGN KEY -> Department.DepartmentId;Requesting department
HiringManagerId;NOT NULL, FOREIGN KEY -> User.UserId;Hiring Manager who created the requisition
NumberOfPositions;NOT NULL, CHECK > 0;Number of openings
EmploymentType;NOT NULL;Full-time, Part-time, Internship, Contract
MinSalary;NULL;Proposed minimum salary
MaxSalary;NULL;Proposed maximum salary
ReasonForHiring;NULL;New hire / Replacement
JobDescription;NOT NULL;Job description (JD)
RequirementDetails;NOT NULL;Candidate requirements
ApprovalStatus;NOT NULL;Draft, Pending_Director, Approved, Rejected
CreatedAt;NOT NULL, DEFAULT GETDATE();Creation date
UpdatedAt;NULL;Last update date

3.8 RequisitionApproval

Column;Constraints;Notes
ApprovalId;PRIMARY KEY, IDENTITY(1,1);Approval record identifier
RequisitionId;NOT NULL, FOREIGN KEY -> JobRequisition.RequisitionId;Requisition being approved
DirectorId;NOT NULL, FOREIGN KEY -> User.UserId;Director who approved it
Status;NOT NULL;Approved / Rejected
Comments;NULL;Director's remarks / rejection reason
ApprovalDate;NOT NULL, DEFAULT GETDATE();Time of approval

3.9 ScreeningCriteria

Column;Constraints;Notes
CriteriaId;PRIMARY KEY, IDENTITY(1,1);Criteria identifier
RequisitionId;NOT NULL, FOREIGN KEY -> JobRequisition.RequisitionId;Related requisition
CriteriaName;NOT NULL;e.g. "React experience", "Bachelor's degree"
CriteriaType;NOT NULL;Education, Experience, Skill, Knockout
RequiredValue;NOT NULL;Minimum/required value
Weight;NOT NULL, DEFAULT 1.00;AI scoring weight
IsMandatory;NOT NULL, DEFAULT 0;1 = mandatory, 0 = optional

3.10 JobPosting

Column;Constraints;Notes
JobPostingId;PRIMARY KEY, IDENTITY(1,1);Job posting identifier
RequisitionId;NOT NULL, FOREIGN KEY -> JobRequisition.RequisitionId;Originating approved requisition
PostingTitle;NOT NULL;Public posting title
JobDescription;NOT NULL;Public job description
JobRequirements;NOT NULL;Public job requirements
Benefits;NULL;Benefits & perks
SalaryDisplay;NULL;e.g. "Negotiable", "$1,500 - $2,000"
WorkLocation;NULL;Work location
PostingDate;NULL;Publish date
ApplicationDeadline;NULL;Application deadline
PostingStatus;NOT NULL;Draft, Published, Paused, Closed
CreatedBy;NOT NULL, FOREIGN KEY -> User.UserId;HR owner of the posting
CreatedAt;NOT NULL, DEFAULT GETDATE();Record creation date
UpdatedAt;NULL;Last update date

3.11 Candidate

Column;Constraints;Notes
CandidateId;PRIMARY KEY, IDENTITY(1,1);Candidate identifier
FullName;NOT NULL;Candidate's full name
Email;NOT NULL;Personal email
PhoneNumber;NOT NULL;Phone number
DateOfBirth;NULL;Date of birth
Gender;NULL;Gender
Address;NULL;Home address
LinkedInUrl;NULL;LinkedIn profile link
PortfolioUrl;NULL;Portfolio link
CandidateSource;NULL;Website, TopCV, Referral
CreatedAt;NOT NULL, DEFAULT GETDATE();Profile creation date
UpdatedAt;NULL;Last update date

3.12 ApplicationReview

Column;Constraints;Notes
ResumeId;PRIMARY KEY, IDENTITY(1,1);Resume file identifier
CandidateId;NOT NULL, FOREIGN KEY -> Candidate.CandidateId;Owning candidate
FileName;NOT NULL;Original file name
FilePath;NOT NULL;Storage path on server/cloud
FileType;NULL;PDF, DOCX
FileSize;NULL;File size (bytes)
UploadedAt;NOT NULL, DEFAULT GETDATE();Upload time

3.13 Application

Column;Constraints;Notes
ApplicationId;PRIMARY KEY, IDENTITY(1,1);Application identifier
CandidateId;NOT NULL, FOREIGN KEY -> Candidate.CandidateId;Applying candidate
JobPostingId;NOT NULL, FOREIGN KEY -> JobPosting.JobPostingId;Position applied for
AppliedCvUrl;NOT NULL, TEXT; CV path for this application
SubmissionDate;NOT NULL, DEFAULT GETDATE();Submission timestamp
ApplicationStatus;NOT NULL;Applied ... Interviewing, Offered, Hired
OverallScore;NULL;Overall combined score
HRReviewNotes;NULL;HR's preliminary review notes
HMReviewNotes;NULL;Hiring Manager's technical review notes
ReviewedBy;FOREIGN KEY -> User.UserId, NULL;Most recent reviewer
ReviewedAt;NULL;Most recent review timestamp
CreatedAt;NOT NULL, DEFAULT GETDATE();Record creation date
UpdatedAt;NULL;Last update date

3.14 AIScreeningResult

Column;Constraints;Notes
AIScreeningId;PRIMARY KEY, IDENTITY(1,1);AI result identifier
ApplicationId;NOT NULL, FOREIGN KEY -> Application.ApplicationId;Application scored by AI
AIMatchScore;NOT NULL;AI match score, 0.00 - 100.00%
MatchedSkills;NULL;Matched skills, JSON format
UnmatchedRequirements;NULL;Missing requirements, JSON format
AISummary;NULL;AI-generated summary
AIRecommendation;NOT NULL;Strongly_Recommend, Consider, Reject
ScreenedAt;NOT NULL, DEFAULT GETDATE();Time AI finished processing

3.15 InterviewSchedule

Column;Constraints;Notes
InterviewId;PRIMARY KEY, IDENTITY(1,1);Interview session identifier
ApplicationId;NOT NULL, FOREIGN KEY -> Application.ApplicationId;Application being interviewed
InterviewRound;NOT NULL;Round 1 - HR, Round 2 - Technical, Final
InterviewFormat;NOT NULL;Online_GoogleMeet, Offline_Office
StartTime;NOT NULL;Interview start time
EndTime;NOT NULL, CHECK EndTime > StartTime;Interview end time
LocationOrLink;NULL;Meeting room or video call link
InterviewStatus;NOT NULL;Scheduled, Completed, Cancelled, Rescheduled
CreatedBy;NOT NULL, FOREIGN KEY -> User.UserId;HR who scheduled it
CreatedAt;NOT NULL, DEFAULT GETDATE();Schedule creation date

3.16 InterviewPanel

Column;Constraints;Notes
InterviewId;PRIMARY KEY (composite), FOREIGN KEY -> InterviewSchedule.InterviewId;Interview session
InterviewerId;PRIMARY KEY (composite), FOREIGN KEY -> User.UserId;Panel member
RoleInPanel;NULL;Lead_Interviewer, Member

3.17 InterviewEvaluation

Column;Constraints;Notes
EvaluationId;PRIMARY KEY, IDENTITY(1,1);Evaluation form identifier
InterviewId;NOT NULL, FOREIGN KEY -> InterviewSchedule.InterviewId;Related interview session
InterviewerId;NOT NULL, FOREIGN KEY -> User.UserId;Interviewer submitting the evaluation
TechnicalScore;NULL, CHECK 1-5;Technical skill score
SoftSkillScore;NULL, CHECK 1-5;Soft skill score
CulturalFitScore;NULL, CHECK 1-5;Cultural fit score
Strengths;NULL;Candidate's strengths
Weaknesses;NULL;Candidate's weaknesses
Recommendation;NOT NULL;Hire, No_Hire, Consider
Comments;NULL;Detailed remarks
EvaluatedAt;NOT NULL, DEFAULT GETDATE();Submission time

3.18 InterviewFinalResult

Column;Constraints;Notes
FinalResultId;PRIMARY KEY, IDENTITY(1,1);Final decision identifier
InterviewId;NOT NULL, UNIQUE, FOREIGN KEY -> InterviewSchedule.InterviewId;1-1 relationship with the interview session
HiringManagerId;NOT NULL, FOREIGN KEY -> User.UserId;Hiring Manager who made the decision
FinalDecision;NOT NULL;Passed / Failed
FinalSummaryComments;NULL;Overall interview summary
ApprovedAt;NOT NULL, DEFAULT GETDATE();Decision timestamp

3.19 OfferProposal

Column;Constraints;Notes
OfferId;PRIMARY KEY, IDENTITY(1,1);Offer proposal identifier
ApplicationId;NOT NULL, UNIQUE, FOREIGN KEY -> Application.ApplicationId;1-1 relationship with the application
ProposedSalary;NOT NULL;Proposed official salary
ProbationSalary;NOT NULL, CHECK >= 85% of ProposedSalary;Probation-period salary
ProbationDays;NOT NULL, DEFAULT 60;Number of probation days
ProposedPosition;NOT NULL;Official job title
WorkLocation;NULL;Work location
ProposedBy;NOT NULL, FOREIGN KEY -> User.UserId;Hiring Manager who created the offer
OfferStatus;NOT NULL;Draft ... Sent_To_Candidate, Accepted, Rejected
CreatedAt;NOT NULL, DEFAULT GETDATE();Creation date
UpdatedAt;NULL;Last update date

3.20 OfferApproval

Column;Constraints;Notes
OfferApprovalId;PRIMARY KEY, IDENTITY(1,1);Offer approval record identifier
OfferId;NOT NULL, FOREIGN KEY -> OfferProposal.OfferId;Related offer proposal
DirectorId;NOT NULL, FOREIGN KEY -> User.UserId;Director who approved the offer
Status;NOT NULL;Approved / Rejected
DirectorComments;NULL;Director's remarks
ApprovedAt;NOT NULL, DEFAULT GETDATE();Time of approval

3.21 OfferNegotiation

Column;Constraints;Notes
NegotiationId;PRIMARY KEY, IDENTITY(1,1);Negotiation round identifier
OfferId;NOT NULL, FOREIGN KEY -> OfferProposal.OfferId;Related offer proposal
CandidateCounterSalary;NULL;Candidate's counter-offer salary
CandidateNotes;NULL;Candidate's requests/feedback
HRResponseNotes;NULL;HR's response
NegotiationDate;NOT NULL, DEFAULT GETDATE();Time of exchange