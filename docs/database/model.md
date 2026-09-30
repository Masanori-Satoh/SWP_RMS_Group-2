# Database model reference

`database/schema/db.sql` là nguồn chuẩn cho tên bảng/cột, kiểu dữ liệu và ràng
buộc. Tài liệu này chỉ mô tả nghiệp vụ; khi khác SQL, ưu tiên SQL.

3.1 User
Column;Constraints;Notes
UserId;PRIMARY KEY, IDENTITY(1,1);User identifier
RoleId;NOT NULL, FOREIGN KEY -> Role.RoleId;Assigned role (each user has exactly one)
Username;NOT NULL, UNIQUE;Login username
PasswordHash;NOT NULL;Hashed password (BCrypt/Argon2), never store plaintext
Email;NOT NULL, UNIQUE;Email (work email for staff, personal email for candidates)
FullName;NOT NULL;Full name
PhoneNumber;NULL;Phone number
AvatarUrl;NULL;Avatar image path
DepartmentId;FOREIGN KEY -> Department.DepartmentId, NULL;Department the user belongs to (NULL for candidates)
AccountStatus;NOT NULL;Active, Inactive, Blocked
CreatedAt;NOT NULL, DEFAULT SYSDATETIME();Account creation time
UpdatedAt;NULL;Last update time
3.2 Role
Column;Constraints;Notes
RoleId;PRIMARY KEY, IDENTITY(1,1);Role identifier
RoleName;NOT NULL, UNIQUE;System Admin, HR, Hiring Manager, Director, Interviewer, Candidate
Description;NULL;Description of the role's scope
3.3 Department
Column;Constraints;Notes
DepartmentId;PRIMARY KEY, IDENTITY(1,1);Department identifier
DepartmentName;NOT NULL, UNIQUE;Department name
ManagerId;FOREIGN KEY -> User.UserId, NULL;Department head
DepartmentStatus;NOT NULL;Active, Inactive
3.4 AuditLog
Column;Constraints;Notes
AuditLogId;PRIMARY KEY, IDENTITY(1,1);Log entry identifier (BIGINT)
UserId;NOT NULL, FOREIGN KEY -> User.UserId;User who performed the action
Action;NOT NULL;CREATE, UPDATE, DELETE, LOGIN
EntityName;NOT NULL;Name of the affected entity
EntityId;NOT NULL;Primary key of the affected record
OldValue;NULL;Old data, JSON format
NewValue;NULL;New data, JSON format
IpAddress;NULL;User's IP address
Timestamp;NOT NULL, DEFAULT SYSDATETIME();Time the action occurred
3.5 SystemConfig
Column;Constraints;Notes
ConfigKey;PRIMARY KEY;e.g. AI_SCREENING_ENDPOINT, EMAIL_SMTP
ConfigValue;NOT NULL;Configuration value
Description;NULL;Purpose of the configuration
UpdatedAt;NOT NULL, DEFAULT SYSDATETIME();Last update time
3.6 JobRequisition
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
RequiredGender;NOT NULL, DEFAULT 'Any';Male, Female, Any
ProbationDuration;NULL;Probation period length
WorkModel;NOT NULL;On-site, Remote, Hybrid
WorkLocation;NULL;Work location (nullable for fully remote positions)
ExpectedStartDate;NULL;Expected start/probation date for the position
ApprovalStatus;NOT NULL;Draft, Pending_Director, Approved, Rejected
CreatedAt;NOT NULL, DEFAULT SYSDATETIME();Creation date
UpdatedAt;NULL;Last update date
3.7 RequisitionApproval
Column;Constraints;Notes
ApprovalId;PRIMARY KEY, IDENTITY(1,1);Approval record identifier
RequisitionId;NOT NULL, FOREIGN KEY -> JobRequisition.RequisitionId;Requisition being approved
DirectorId;NOT NULL, FOREIGN KEY -> User.UserId;Director who approved it
Status;NOT NULL;Approved / Rejected
Comments;NULL;Director's remarks / rejection reason
ApprovalDate;NOT NULL, DEFAULT SYSDATETIME();Time of approval
3.8 ScreeningCriteria
Column;Constraints;Notes
CriteriaId;PRIMARY KEY, IDENTITY(1,1);Criteria identifier
RequisitionId;NOT NULL, FOREIGN KEY -> JobRequisition.RequisitionId;Related requisition
CriteriaName;NOT NULL;e.g. "React experience", "Bachelor's degree"
CriteriaType;NOT NULL;Education, Experience, Skill, Knockout
RequiredValue;NOT NULL;Minimum/required value
Weight;NOT NULL, DEFAULT 1.00;AI scoring weight
IsMandatory;NOT NULL, DEFAULT 0;1 = mandatory, 0 = optional
3.9 JobPosting
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
CreatedAt;NOT NULL, DEFAULT SYSDATETIME();Record creation date
UpdatedAt;NULL;Last update date
3.10 Candidate
Column;Constraints;Notes
CandidateId;PRIMARY KEY, IDENTITY(1,1);Candidate identifier
UserId;NOT NULL, UNIQUE, FOREIGN KEY -> User.UserId;1-1 with the account (name, email, phone live in User)
DateOfBirth;NULL;Date of birth
Gender;NULL;Gender
Address;NULL;Home address
LinkedInUrl;NULL;LinkedIn profile link
PortfolioUrl;NULL;Portfolio link
CandidateSource;NULL;Website, Referral, LinkedIn...
CreatedAt;NOT NULL, DEFAULT SYSDATETIME();Profile creation date
UpdatedAt;NULL;Last update date
3.11 Application
Column;Constraints;Notes
ApplicationId;PRIMARY KEY, IDENTITY(1,1);Application identifier
CandidateId;NOT NULL, FOREIGN KEY -> Candidate.CandidateId;Applying candidate
JobPostingId;NOT NULL, FOREIGN KEY -> JobPosting.JobPostingId;Position applied for
AppliedCvUrl;NOT NULL;CV file path/URL used for this application (replaces Resume)
SubmissionDate;NOT NULL, DEFAULT SYSDATETIME();Submission timestamp
ApplicationStatus;NOT NULL;Applied, AI_Screened, HR_Passed, HM_Passed, Interviewing, Offered, Hired, Rejected
OverallScore;NULL;Overall combined score
CreatedAt;NOT NULL, DEFAULT SYSDATETIME();Record creation date
UpdatedAt;NULL;Last update date
3.12 ApplicationReview
Column;Constraints;Notes
ReviewId;PRIMARY KEY, IDENTITY(1,1);Review record identifier
ApplicationId;NOT NULL, FOREIGN KEY -> Application.ApplicationId;Application being reviewed
ReviewerId;NOT NULL, FOREIGN KEY -> User.UserId;User who performed the review
ReviewerRole;NOT NULL;HR, HiringManager
Decision;NOT NULL;Pass, Fail, Hold
Comments;NULL;Reviewer's remarks
ReviewedAt;NOT NULL, DEFAULT SYSDATETIME();Time of review
3.13 AIScreeningResult
Column;Constraints;Notes
AIScreeningId;PRIMARY KEY, IDENTITY(1,1);AI result identifier
ApplicationId;NOT NULL, FOREIGN KEY -> Application.ApplicationId;Application scored by AI, multiple results preserve rescoring history
AIMatchScore;NOT NULL, CHECK 0-100;AI match score (%)
ScreenedAt;NOT NULL, DEFAULT SYSDATETIME();Time AI finished processing
3.14 InterviewSchedule
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
CreatedAt;NOT NULL, DEFAULT SYSDATETIME();Schedule creation date
3.15 InterviewPanel
Column;Constraints;Notes
InterviewId;PRIMARY KEY (composite), FOREIGN KEY -> InterviewSchedule.InterviewId;Interview session
InterviewerId;PRIMARY KEY (composite), FOREIGN KEY -> User.UserId;Panel member
RoleInPanel;NOT NULL;HR (checks culture fit), HM (checks technical skills)
3.16 InterviewEvaluation
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
EvaluatedAt;NOT NULL, DEFAULT SYSDATETIME();Submission time
3.17 InterviewFinalResult
Column;Constraints;Notes
FinalResultId;PRIMARY KEY, IDENTITY(1,1);Final decision identifier
InterviewId;NOT NULL, UNIQUE, FOREIGN KEY -> InterviewSchedule.InterviewId;1-1 with the interview session
HiringManagerId;NOT NULL, FOREIGN KEY -> User.UserId;Hiring Manager who made the decision
FinalDecision;NOT NULL;Passed / Failed
FinalSummaryComments;NULL;Overall interview summary
ApprovedAt;NOT NULL, DEFAULT SYSDATETIME();Decision timestamp
3.18 OfferProposal
Column;Constraints;Notes
OfferId;PRIMARY KEY, IDENTITY(1,1);Offer proposal identifier
ApplicationId;NOT NULL, UNIQUE, FOREIGN KEY -> Application.ApplicationId;1-1 with the application
OfferedPositionTitle;NOT NULL;Proposed job title
ProposedSalary;NOT NULL;Proposed official salary
ProbationSalary;NOT NULL, CHECK >= 85% of ProposedSalary;Probation-period salary
ExpectedStartDate;NULL;Expected start date
WorkLocation;NULL;Work location
BenefitsPackage;NULL;Benefits/bonus package attached to the offer
OfferStatus;NOT NULL;Draft, Pending_Director, Director_Approved, Director_Rejected, Sent_Candidate, Accepted, Declined, Negotiating
ProposedBy;NOT NULL, FOREIGN KEY -> User.UserId;HR who created the offer
CreatedAt;NOT NULL, DEFAULT GETDATE();Creation date
UpdatedAt;NULL;Last update date
3.19 OfferApproval
Column;Constraints;Notes
OfferApprovalId;PRIMARY KEY, IDENTITY(1,1);Offer approval record identifier
OfferId;NOT NULL, FOREIGN KEY -> OfferProposal.OfferId;Related offer proposal
DirectorId;NOT NULL, FOREIGN KEY -> User.UserId;Director who approved the offer
Status;NOT NULL;Approved / Rejected
DirectorComments;NULL;Director's remarks
ApprovedAt;NOT NULL, DEFAULT SYSDATETIME();Time of approval
3.20 OfferNegotiation
Column;Constraints;Notes
NegotiationId;PRIMARY KEY, IDENTITY(1,1);Negotiation round identifier
OfferId;NOT NULL, FOREIGN KEY -> OfferProposal.OfferId;Related offer proposal
CandidateCounterSalary;NULL;Candidate's counter-offer salary
CandidateNotes;NULL;Candidate's requests/feedback
HRResponseNotes;NULL;HR's response
NegotiationDate;NOT NULL, DEFAULT SYSDATETIME();Time of exchange
