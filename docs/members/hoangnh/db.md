# Recruitment Management System (RMS) - Database Schema Specification

- **Target Database Name:** `RitirementManagement2`
- **DBMS:** Microsoft SQL Server (T-SQL)
- **Reference Doc:** `docs/database/model.md` (Section 3.1 - 3.20)

---

## 1. Tổng quan các Bảng (Tables Overview)

| STT | Bảng (Table Name) | Mô tả |
| :--- | :--- | :--- |
| 1 | `Role` | Vai trò hệ thống |
| 2 | `Department` | Phòng ban / Tổ chức |
| 3 | `User` | Tài khoản người dùng |
| 4 | `AuditLog` | Nhật ký kiểm toán hành vi hệ thống |
| 5 | `SystemConfig` | Cấu hình tham số hệ thống |
| 6 | `JobRequisition` | Phiếu yêu cầu tuyển dụng (hỗ trợ nhiều đợt tuyển & liên kết đợt gốc) |
| 7 | `RequisitionApproval` | Lịch sử phê duyệt yêu cầu tuyển dụng |
| 8 | `ScreeningCriteria` | Tiêu chí sàng lọc CV |
| 9 | `JobPosting` | Tin tuyển dụng công khai (hỗ trợ nhiều lần đăng & tùy chọn hiển thị số lượng) |
| 10 | `Candidate` | Hồ sơ ứng viên |
| 11 | `TalentPoolNote` | Ghi chú ứng viên tiềm năng |
| 12 | `Application` | Hồ sơ ứng tuyển |
| 13 | `ApplicationReview` | Đánh giá hồ sơ bởi HR & Hiring Manager |
| 14 | `AIScreeningResult` | Kết quả AI chấm điểm CV |
| 15 | `InterviewSchedule` | Lịch phỏng vấn |
| 16 | `InterviewPanel` | Hội đồng phỏng vấn |
| 17 | `InterviewEvaluation` | Phiếu đánh giá phỏng vấn cá nhân |
| 18 | `InterviewFinalResult` | Quyết định kết quả phỏng vấn cuối cùng |
| 19 | `OfferProposal` | Đề xuất chế độ và lương cho ứng viên |
| 20 | `OfferApproval` | Phê duyệt đề xuất Offer |
| 21 | `Notification` | Quản lý thông báo nội bộ (hỗ trợ người gửi ActorId & thời gian đọc ReadAt) |

---

## 2. Khởi tạo Cơ sở Dữ liệu

```sql
USE master;
GO

IF EXISTS (SELECT 1 FROM sys.databases WHERE name = N'RitirementManagement2')
BEGIN
    ALTER DATABASE RitirementManagement2 SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE RitirementManagement2;
END
GO

CREATE DATABASE RitirementManagement2;
GO

USE RitirementManagement2;
GO
```

---

## 3. Chi tiết Cấu trúc các Bảng

### 3.1 Role (Vai trò hệ thống)
Lưu trữ thông tin các vai trò trong hệ thống (HR, Hiring Manager, Candidate, Director,...).

```sql
CREATE TABLE [Role] (
    RoleId INT IDENTITY(1,1) PRIMARY KEY,
    RoleName NVARCHAR(50) NOT NULL CONSTRAINT UQ_Role_RoleName UNIQUE,
    [Description] NVARCHAR(255) NULL
);
GO
```

---

### 3.2 Department (Phòng ban tổ chức)
Lưu trữ thông tin phòng ban trong công ty.

```sql
CREATE TABLE Department (
    DepartmentId INT IDENTITY(1,1) PRIMARY KEY,
    DepartmentName NVARCHAR(100) NOT NULL CONSTRAINT UQ_Department_Name UNIQUE,
    ManagerId INT NULL,
    DepartmentStatus NVARCHAR(20) NOT NULL CONSTRAINT DF_Department_Status DEFAULT N'Active',

    CONSTRAINT CK_Department_Status
        CHECK (DepartmentStatus IN (N'Active', N'Inactive'))
);
GO
```

---

### 3.3 User (Tài khoản người dùng)
Quản lý thông tin tài khoản người dùng đăng nhập hệ thống.

```sql
CREATE TABLE [User] (
    UserId INT IDENTITY(1,1) PRIMARY KEY,
    RoleId INT NOT NULL,
    Username NVARCHAR(50) NOT NULL CONSTRAINT UQ_User_Username UNIQUE,
    PasswordHash NVARCHAR(255) NOT NULL,
    Email NVARCHAR(150) NOT NULL CONSTRAINT UQ_User_Email UNIQUE,
    FullName NVARCHAR(100) NOT NULL,
    PhoneNumber NVARCHAR(20) NULL,
    AvatarUrl NVARCHAR(500) NULL,
    DepartmentId INT NULL,
    AccountStatus NVARCHAR(20) NOT NULL CONSTRAINT DF_User_AccountStatus DEFAULT N'Active',
    CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_User_CreatedAt DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,

    CONSTRAINT FK_User_Role
        FOREIGN KEY (RoleId) REFERENCES [Role](RoleId),

    CONSTRAINT FK_User_Department
        FOREIGN KEY (DepartmentId) REFERENCES Department(DepartmentId),

    CONSTRAINT CK_User_AccountStatus
        CHECK (AccountStatus IN (N'Active', N'Inactive', N'Blocked'))
);
GO

-- Thiết lập FK cho ManagerId của bảng Department trỏ về [User](UserId)
ALTER TABLE Department
    ADD CONSTRAINT FK_Department_Manager
    FOREIGN KEY (ManagerId) REFERENCES [User](UserId);
GO
```

---

### 3.4 Audit Log (Nhật ký kiểm toán)
Theo dõi các hoạt động thay đổi dữ liệu chính trong hệ thống.

```sql
CREATE TABLE AuditLog (
    AuditLogId BIGINT IDENTITY(1,1) PRIMARY KEY,
    UserId INT NOT NULL,
    [Action] NVARCHAR(50) NOT NULL,
    EntityName NVARCHAR(100) NOT NULL,
    EntityId NVARCHAR(50) NOT NULL,
    OldValue NVARCHAR(MAX) NULL,
    NewValue NVARCHAR(MAX) NULL,
    IpAddress NVARCHAR(45) NULL,
    [Timestamp] DATETIME2 NOT NULL CONSTRAINT DF_AuditLog_Timestamp DEFAULT SYSDATETIME(),

    CONSTRAINT FK_AuditLog_User
        FOREIGN KEY (UserId) REFERENCES [User](UserId),

    CONSTRAINT CK_AuditLog_Action
        CHECK ([Action] IN (N'CREATE', N'UPDATE', N'DELETE', N'LOGIN'))
);
GO
```

---

### 3.5 System Config (Cấu hình tham số hệ thống)

```sql
CREATE TABLE SystemConfig (
    ConfigKey NVARCHAR(100) PRIMARY KEY,
    ConfigValue NVARCHAR(MAX) NOT NULL,
    [Description] NVARCHAR(255) NULL,
    UpdatedAt DATETIME2 NOT NULL CONSTRAINT DF_SystemConfig_UpdatedAt DEFAULT SYSDATETIME()
);
GO
```

---

### 3.6 Job Requisition (Phiếu yêu cầu tuyển dụng)

```sql
CREATE TABLE JobRequisition (
    RequisitionId          INT IDENTITY(1,1) PRIMARY KEY,
    Title                  NVARCHAR(200) NOT NULL,
    RequisitionCode        NVARCHAR(50) NULL,
    DepartmentId           INT NOT NULL,
    HiringManagerId        INT NOT NULL,
    CreatedBy              INT NULL,                -- [MỚI] user tạo phiếu
    RecruitmentRound       INT NOT NULL CONSTRAINT DF_JobRequisition_Round DEFAULT 1,
    OriginalRequisitionId  INT NULL,                -- [MỚI] NULL = đợt 1
    HiringType             NVARCHAR(20) NULL,       -- [MỚI] New / Replacement
    NumberOfPositions      INT NOT NULL,
    EmploymentType         NVARCHAR(50) NOT NULL,
    MinSalary              DECIMAL(18,2) NULL,
    MaxSalary              DECIMAL(18,2) NULL,
    ReasonForHiring        NVARCHAR(2000) NULL,
    JobDescription         NVARCHAR(MAX) NOT NULL,
    RequirementDetails     NVARCHAR(MAX) NOT NULL,
    RequiredGender         NVARCHAR(20) NOT NULL CONSTRAINT DF_JobRequisition_RequiredGender DEFAULT N'Any',
    ProbationDays          INT NULL,                -- số ngày thử việc (1..180)
    WorkModel              NVARCHAR(20) NOT NULL CONSTRAINT DF_JobRequisition_WorkModel DEFAULT N'On-site',
    WorkLocation           NVARCHAR(255) NULL,
    ExpectedStartDate      DATE NULL,
    ApprovalStatus         NVARCHAR(30) NOT NULL CONSTRAINT DF_JobRequisition_ApprovalStatus DEFAULT N'Draft',
    SubmittedAt            DATETIME2 NULL,          -- [MỚI] Thời điểm nộp phê duyệt
    ClosedAt               DATETIME2 NULL,          -- [MỚI] Thời điểm đóng đợt tuyển
    CreatedAt              DATETIME2 NOT NULL CONSTRAINT DF_JobRequisition_CreatedAt DEFAULT SYSDATETIME(),
    UpdatedAt              DATETIME2 NULL,

    CONSTRAINT FK_JobRequisition_Department
        FOREIGN KEY (DepartmentId) REFERENCES Department(DepartmentId),

    CONSTRAINT FK_JobRequisition_HiringManager
        FOREIGN KEY (HiringManagerId) REFERENCES [User](UserId),

    CONSTRAINT FK_JobRequisition_CreatedBy
        FOREIGN KEY (CreatedBy) REFERENCES [User](UserId),

    CONSTRAINT FK_JobRequisition_Original
        FOREIGN KEY (OriginalRequisitionId) REFERENCES JobRequisition(RequisitionId),

    CONSTRAINT CK_JobRequisition_RecruitmentRound
        CHECK (RecruitmentRound > 0),

    CONSTRAINT CK_JobRequisition_RoundOrigin CHECK (
        (RecruitmentRound = 1 AND OriginalRequisitionId IS NULL)
        OR (RecruitmentRound > 1 AND OriginalRequisitionId IS NOT NULL)
    ),

    CONSTRAINT CK_JobRequisition_HiringType
        CHECK (HiringType IS NULL OR HiringType IN (N'New', N'Replacement')),

    CONSTRAINT CK_JobRequisition_NumberOfPositions
        CHECK (NumberOfPositions > 0),
 
    CONSTRAINT CK_JobRequisition_ProbationDays
        CHECK (ProbationDays IS NULL OR ProbationDays BETWEEN 1 AND 180),

    CONSTRAINT CK_JobRequisition_EmploymentType
        CHECK (EmploymentType IN (N'Full-time', N'Part-time', N'Internship', N'Contract')),

    CONSTRAINT CK_JobRequisition_RequiredGender
        CHECK (RequiredGender IN (N'Male', N'Female', N'Any')),

    CONSTRAINT CK_JobRequisition_WorkModel
        CHECK (WorkModel IN (N'On-site', N'Remote', N'Hybrid')),

    CONSTRAINT CK_JobRequisition_ApprovalStatus
        CHECK (ApprovalStatus IN (
            N'Draft',
            N'Pending_Director',
            N'Approved', 
            N'Rejected',
            N'Closed',
            N'Cancelled'
        )),

    CONSTRAINT CK_JobRequisition_Salary_Range
        CHECK (MinSalary IS NULL OR MaxSalary IS NULL OR MinSalary <= MaxSalary),

    CONSTRAINT CK_JobRequisition_Salary_NonNegative
        CHECK ((MinSalary IS NULL OR MinSalary >= 0) AND (MaxSalary IS NULL OR MaxSalary >= 0))
);
GO
```

---

### 3.7 Requisition Approval (Lịch sử phê duyệt yêu cầu tuyển dụng)

```sql
CREATE TABLE RequisitionApproval (
    ApprovalId INT IDENTITY(1,1) PRIMARY KEY,
    RequisitionId INT NOT NULL,
    DirectorId INT NOT NULL,
    [Status] NVARCHAR(20) NOT NULL,
    Comments NVARCHAR(1000) NULL,
    ApprovalDate DATETIME2 NOT NULL CONSTRAINT DF_RequisitionApproval_Date DEFAULT SYSDATETIME(),

    CONSTRAINT FK_RequisitionApproval_Requisition
        FOREIGN KEY (RequisitionId) REFERENCES JobRequisition(RequisitionId),

    CONSTRAINT FK_RequisitionApproval_Director
        FOREIGN KEY (DirectorId) REFERENCES [User](UserId),

    CONSTRAINT CK_RequisitionApproval_Status
        CHECK ([Status] IN (N'Approved', N'Rejected'))
);
GO
```

---

### 3.8 Screening Criteria (Tiêu chí sàng lọc CV)

```sql
CREATE TABLE ScreeningCriteria (
    CriteriaId INT IDENTITY(1,1) PRIMARY KEY,
    RequisitionId INT NOT NULL,
    CriteriaName NVARCHAR(150) NOT NULL,
    CriteriaType NVARCHAR(30) NOT NULL,
    RequiredValue NVARCHAR(255) NOT NULL,
    [Weight] DECIMAL(5,2) NOT NULL CONSTRAINT DF_ScreeningCriteria_Weight DEFAULT 1.00,
    IsMandatory BIT NOT NULL CONSTRAINT DF_ScreeningCriteria_IsMandatory DEFAULT 0,

    CONSTRAINT FK_ScreeningCriteria_Requisition
        FOREIGN KEY (RequisitionId) REFERENCES JobRequisition(RequisitionId),

    CONSTRAINT CK_ScreeningCriteria_Type
        CHECK (CriteriaType IN (N'Education', N'Experience', N'Skill', N'Knockout')),

    CONSTRAINT CK_ScreeningCriteria_Weight
        CHECK ([Weight] > 0),

    CONSTRAINT UQ_ScreeningCriteria_Requisition_Name
        UNIQUE (RequisitionId, CriteriaName)
);
GO
```

---

### 3.9 Job Posting (Tin tuyển dụng công khai)

```sql
CREATE TABLE JobPosting (
    JobPostingId         INT IDENTITY(1,1) PRIMARY KEY,
    RequisitionId        INT NOT NULL,
    PostingNo            INT NOT NULL CONSTRAINT DF_JobPosting_PostingNo DEFAULT 1,  -- [MỚI] lần đăng
    PostingTitle         NVARCHAR(200) NOT NULL,
    JobDescription       NVARCHAR(MAX) NOT NULL,
    JobRequirements      NVARCHAR(MAX) NOT NULL,
    Benefits             NVARCHAR(MAX) NULL,
    SalaryDisplay        NVARCHAR(100) NULL,
    NumberOfPositionsDisplay     NVARCHAR(50) NULL,      -- [MỚI] NULL = không hiển thị số lượng
    WorkLocation         NVARCHAR(255) NULL,
    PostingDate          DATETIME2 NULL,
    ApplicationDeadline  DATETIME2 NULL,
    PostingStatus        NVARCHAR(20) NOT NULL CONSTRAINT DF_JobPosting_PostingStatus DEFAULT N'Draft',
    ClosedAt             DATETIME2 NULL,         -- [MỚI] Thời điểm đóng tin
    CreatedBy            INT NOT NULL,
    CreatedAt            DATETIME2 NOT NULL CONSTRAINT DF_JobPosting_CreatedAt DEFAULT SYSDATETIME(),
    UpdatedAt            DATETIME2 NULL,

    CONSTRAINT FK_JobPosting_Requisition
        FOREIGN KEY (RequisitionId) REFERENCES JobRequisition(RequisitionId),

    CONSTRAINT FK_JobPosting_CreatedBy
        FOREIGN KEY (CreatedBy) REFERENCES [User](UserId),

    CONSTRAINT UQ_JobPosting_Requisition_PostingNo
        UNIQUE (RequisitionId, PostingNo),

    CONSTRAINT CK_JobPosting_PostingNo
        CHECK (PostingNo > 0),

    CONSTRAINT CK_JobPosting_Status
        CHECK (PostingStatus IN (N'Draft', N'Published', N'Paused', N'Closed')),

    CONSTRAINT CK_JobPosting_Deadline
        CHECK (ApplicationDeadline IS NULL OR PostingDate IS NULL OR ApplicationDeadline > PostingDate)
);
GO
```

---

### 3.10 Candidate (Hồ sơ ứng viên)
*Lưu ý: Quan hệ 1-1 với `[User]`. Thông tin `FullName`, `Email`, `PhoneNumber` nằm tại bảng `[User]`.*

```sql
CREATE TABLE Candidate (
    CandidateId INT IDENTITY(1,1) PRIMARY KEY,
    UserId INT NOT NULL CONSTRAINT UQ_Candidate_UserId UNIQUE,
    DateOfBirth DATE NULL,
    Gender NVARCHAR(20) NULL,
    Address NVARCHAR(255) NULL,
    LinkedInUrl NVARCHAR(500) NULL,
    PortfolioUrl NVARCHAR(500) NULL,
    CandidateSource NVARCHAR(50) NULL,
    IsPotential BIT NOT NULL CONSTRAINT DF_Candidate_IsPotential DEFAULT 0,
    CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Candidate_CreatedAt DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,

    CONSTRAINT FK_Candidate_User
        FOREIGN KEY (UserId) REFERENCES [User](UserId)
);
GO
```

---

### 3.11 TalentPoolNote (Ghi chú cho ứng viên tiềm năng)

```sql
CREATE TABLE TalentPoolNote (
    NoteId INT IDENTITY(1,1) PRIMARY KEY,
    CandidateId INT NOT NULL,
    UserId INT NOT NULL,                  -- Bắt buộc: ID của HR/HM thực hiện ghi chú
    NoteContent NVARCHAR(1000) NOT NULL,  -- Nội dung ghi chú
    CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_TalentPoolNote_CreatedAt DEFAULT SYSDATETIME(),

    CONSTRAINT FK_TalentPoolNote_Candidate
        FOREIGN KEY (CandidateId) REFERENCES Candidate(CandidateId),

    CONSTRAINT FK_TalentPoolNote_User
        FOREIGN KEY (UserId) REFERENCES [User](UserId)
);
GO
```

---

### 3.12 Application (Hồ sơ ứng tuyển)

```sql
CREATE TABLE Application (
    ApplicationId INT IDENTITY(1,1) PRIMARY KEY,
    CandidateId INT NOT NULL,
    JobPostingId INT NOT NULL,
    AppliedCvUrl NVARCHAR(500) NOT NULL,
    SubmissionDate DATETIME2 NOT NULL CONSTRAINT DF_Application_SubmissionDate DEFAULT SYSDATETIME(),
    ApplicationStatus NVARCHAR(30) NOT NULL CONSTRAINT DF_Application_Status DEFAULT N'Applied',
    OverallScore DECIMAL(5,2) NULL,
    CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Application_CreatedAt DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,

    CONSTRAINT FK_Application_Candidate
        FOREIGN KEY (CandidateId) REFERENCES Candidate(CandidateId),

    CONSTRAINT FK_Application_JobPosting
        FOREIGN KEY (JobPostingId) REFERENCES JobPosting(JobPostingId),

    CONSTRAINT CK_Application_Status
        CHECK (ApplicationStatus IN (
            N'Applied',
            N'AI_Screened',
            N'HR_Passed',
            N'HM_Passed',
            N'HM_Suggested',
            N'Interviewing',
            N'Offered',
            N'Hired',
            N'Rejected'
        ))
);
GO
```

---

### 3.13 Application Review (Đánh giá hồ sơ bởi HR & Hiring Manager)

```sql
CREATE TABLE ApplicationReview (
    ReviewId INT IDENTITY(1,1) PRIMARY KEY,
    ApplicationId INT NOT NULL,
    ReviewerId INT NOT NULL,
    ReviewerRole NVARCHAR(30) NOT NULL,
    Decision NVARCHAR(20) NOT NULL,
    Comments NVARCHAR(1000) NULL,
    ReviewedAt DATETIME2 NOT NULL CONSTRAINT DF_ApplicationReview_ReviewedAt DEFAULT SYSDATETIME(),

    CONSTRAINT FK_ApplicationReview_Application
        FOREIGN KEY (ApplicationId) REFERENCES Application(ApplicationId),

    CONSTRAINT FK_ApplicationReview_Reviewer
        FOREIGN KEY (ReviewerId) REFERENCES [User](UserId),

    CONSTRAINT CK_ApplicationReview_Role
        CHECK (ReviewerRole IN (N'HR', N'HiringManager', N'Director')),

    CONSTRAINT CK_ApplicationReview_Decision
        CHECK (Decision IN (N'Pass', N'Fail', N'Hold'))
);
GO
```

---

### 3.14 AI Screening Result (Kết quả AI chấm điểm và so khớp CV)

```sql
CREATE TABLE AIScreeningResult (
    AIScreeningId INT IDENTITY(1,1) PRIMARY KEY,
    ApplicationId INT NOT NULL,
    AIMatchScore DECIMAL(5,2) NOT NULL,
    ScreenedAt DATETIME2 NOT NULL CONSTRAINT DF_AIScreeningResult_ScreenedAt DEFAULT SYSDATETIME(),

    CONSTRAINT FK_AIScreeningResult_Application
        FOREIGN KEY (ApplicationId) REFERENCES Application(ApplicationId),

    CONSTRAINT CK_AIScreeningResult_Score
        CHECK (AIMatchScore >= 0 AND AIMatchScore <= 100)
);
GO
```

---

### 3.15 Interview Schedule (Lịch phỏng vấn)

```sql
CREATE TABLE InterviewSchedule (
    InterviewId INT IDENTITY(1,1) PRIMARY KEY,
    ApplicationId INT NOT NULL,
    InterviewFormat NVARCHAR(30) NOT NULL,
    StartTime DATETIME2 NOT NULL,
    EndTime DATETIME2 NOT NULL,
    LocationOrLink NVARCHAR(500) NULL,
    InterviewStatus NVARCHAR(30) NOT NULL CONSTRAINT DF_InterviewSchedule_Status DEFAULT N'Scheduled',
    CreatedBy INT NOT NULL,
    CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_InterviewSchedule_CreatedAt DEFAULT SYSDATETIME(),

    CONSTRAINT FK_InterviewSchedule_Application
        FOREIGN KEY (ApplicationId) REFERENCES Application(ApplicationId),

    CONSTRAINT FK_InterviewSchedule_CreatedBy
        FOREIGN KEY (CreatedBy) REFERENCES [User](UserId),

    CONSTRAINT CK_InterviewSchedule_Time
        CHECK (EndTime > StartTime),

    CONSTRAINT CK_InterviewSchedule_Format
        CHECK (InterviewFormat IN (N'Online_GoogleMeet', N'Offline_Office')),

    CONSTRAINT CK_InterviewSchedule_Status
        CHECK (InterviewStatus IN (N'Scheduled', N'Completed', N'Cancelled', N'Rescheduled'))
);
GO
```

---

### 3.16 Interview Panel (Hội đồng phỏng vấn)
*Composite Primary Key: `(InterviewId, InterviewerId)`*

```sql
CREATE TABLE InterviewPanel (
    InterviewId INT NOT NULL,
    InterviewerId INT NOT NULL,
    RoleInPanel NVARCHAR(30) NOT NULL,

    CONSTRAINT PK_InterviewPanel
        PRIMARY KEY (InterviewId, InterviewerId),

    CONSTRAINT FK_InterviewPanel_Interview
        FOREIGN KEY (InterviewId) REFERENCES InterviewSchedule(InterviewId),

    CONSTRAINT FK_InterviewPanel_User
        FOREIGN KEY (InterviewerId) REFERENCES [User](UserId),

    CONSTRAINT CK_InterviewPanel_Role
        CHECK (RoleInPanel IN (N'HR', N'HM', N'Interviewer', N'Director'))
);
GO
```

---

### 3.17 Interview Evaluation (Phiếu đánh giá phỏng vấn)

```sql
CREATE TABLE InterviewEvaluation (
    EvaluationId INT IDENTITY(1,1) PRIMARY KEY,
    InterviewId INT NOT NULL,
    InterviewerId INT NOT NULL,
    TechnicalScore INT NULL,
    SoftSkillScore INT NULL,
    CulturalFitScore INT NULL,
    Strengths NVARCHAR(MAX) NULL,
    Weaknesses NVARCHAR(MAX) NULL,
    Recommendation NVARCHAR(20) NOT NULL,
    Comments NVARCHAR(MAX) NULL,
    EvaluatedAt DATETIME2 NOT NULL CONSTRAINT DF_InterviewEvaluation_EvaluatedAt DEFAULT SYSDATETIME(),

    CONSTRAINT FK_InterviewEvaluation_Interview
        FOREIGN KEY (InterviewId) REFERENCES InterviewSchedule(InterviewId),

    CONSTRAINT FK_InterviewEvaluation_Interviewer
        FOREIGN KEY (InterviewerId) REFERENCES [User](UserId),

    CONSTRAINT CK_InterviewEvaluation_TechScore
        CHECK (TechnicalScore IS NULL OR (TechnicalScore >= 1 AND TechnicalScore <= 5)),

    CONSTRAINT CK_InterviewEvaluation_SoftScore
        CHECK (SoftSkillScore IS NULL OR (SoftSkillScore >= 1 AND SoftSkillScore <= 5)),

    CONSTRAINT CK_InterviewEvaluation_CultureScore
        CHECK (CulturalFitScore IS NULL OR (CulturalFitScore >= 1 AND CulturalFitScore <= 5)),

    CONSTRAINT CK_InterviewEvaluation_Recommendation
        CHECK (Recommendation IN (N'Hire', N'No_Hire', N'Consider'))
);
GO
```

---

### 3.18 Interview Final Result (Quyết định kết quả phỏng vấn cuối cùng)
*Quan hệ 1-1 với `InterviewSchedule`*

```sql
CREATE TABLE InterviewFinalResult (
    FinalResultId INT IDENTITY(1,1) PRIMARY KEY,
    InterviewId INT NOT NULL CONSTRAINT UQ_InterviewFinalResult_Interview UNIQUE,
    HiringManagerId INT NOT NULL,
    FinalDecision NVARCHAR(20) NOT NULL,
    RecommendedSalary DECIMAL(18,2) NULL,
    FinalSummaryComments NVARCHAR(MAX) NULL,
    ApprovedAt DATETIME2 NOT NULL CONSTRAINT DF_InterviewFinalResult_ApprovedAt DEFAULT SYSDATETIME(),

    CONSTRAINT FK_InterviewFinalResult_Interview
        FOREIGN KEY (InterviewId) REFERENCES InterviewSchedule(InterviewId),

    CONSTRAINT FK_InterviewFinalResult_HiringManager
        FOREIGN KEY (HiringManagerId) REFERENCES [User](UserId),

    CONSTRAINT CK_InterviewFinalResult_FinalDecision
        CHECK (FinalDecision IN (N'Passed', N'Failed')),

    CONSTRAINT CK_InterviewFinalResult_RecommendedSalary
        CHECK (RecommendedSalary IS NULL OR RecommendedSalary >= 0)
);
GO
```

---

### 3.19 Offer Proposal (Đề xuất chế độ và lương cho ứng viên)
*Quan hệ 1-1 với `Application`*

```sql
CREATE TABLE OfferProposal (
    OfferId INT IDENTITY(1,1) PRIMARY KEY,
    ApplicationId INT NOT NULL CONSTRAINT UQ_OfferProposal_Application UNIQUE,
    OfferedPositionTitle NVARCHAR(200) NOT NULL,
    ProposedSalary DECIMAL(18,2) NOT NULL,
    ProbationSalary DECIMAL(18,2) NOT NULL,
    ExpectedStartDate DATE NULL,
    WorkLocation NVARCHAR(255) NULL,
    BenefitsPackage NVARCHAR(MAX) NULL,
    OfferStatus NVARCHAR(40) NOT NULL CONSTRAINT DF_OfferProposal_Status DEFAULT N'Draft',
    DeclineReason NVARCHAR(1000) NULL,
    IsDeleted BIT NOT NULL CONSTRAINT DF_OfferProposal_IsDeleted DEFAULT 0,  -- Xóa mềm đề xuất Offer
    ProposedBy INT NOT NULL,
    CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_OfferProposal_CreatedAt DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,

    CONSTRAINT FK_OfferProposal_Application
        FOREIGN KEY (ApplicationId) REFERENCES Application(ApplicationId),

    CONSTRAINT FK_OfferProposal_ProposedBy
        FOREIGN KEY (ProposedBy) REFERENCES [User](UserId),

    CONSTRAINT CK_OfferProposal_Status
        CHECK (OfferStatus IN (
            N'Draft',
            N'Pending_Director',
            N'Director_Approved',
            N'Director_Rejected',
            N'Sent_Candidate',
            N'Accepted',
            N'Declined'
        )),

    CONSTRAINT CK_OfferProposal_Salaries
        CHECK (
            ProposedSalary >= 0 
            AND ProbationSalary >= 0 
            AND ProbationSalary >= (ProposedSalary * 0.85)
        )
);
GO
```

---

### 3.20 Offer Approval (Phê duyệt đề xuất offer bởi Director)

```sql
CREATE TABLE OfferApproval (
    OfferApprovalId INT IDENTITY(1,1) PRIMARY KEY,
    OfferId INT NOT NULL,
    DirectorId INT NOT NULL,
    [Status] NVARCHAR(20) NOT NULL,
    DirectorComments NVARCHAR(MAX) NULL,
    ApprovedAt DATETIME2 NOT NULL CONSTRAINT DF_OfferApproval_ApprovedAt DEFAULT SYSDATETIME(),

    CONSTRAINT FK_OfferApproval_Offer
        FOREIGN KEY (OfferId) REFERENCES OfferProposal(OfferId),

    CONSTRAINT FK_OfferApproval_Director
        FOREIGN KEY (DirectorId) REFERENCES [User](UserId),

    CONSTRAINT CK_OfferApproval_Status
        CHECK ([Status] IN (N'Approved', N'Rejected'))
);
GO
```

---

### 3.21 Notification (Quản lý thông báo nội bộ hệ thống)

```sql
CREATE TABLE [dbo].[Notification] (
    [NotificationId] BIGINT IDENTITY(1,1) NOT NULL,
    [RecipientId]    INT NOT NULL,
    [ActorId]        INT NULL,                 -- [MỚI] NULL = hệ thống tự sinh
    [Title]          NVARCHAR(255) NOT NULL,
    [Content]        NVARCHAR(1000) NOT NULL,
    [EventType]      VARCHAR(50) NOT NULL,
    [ReferenceType]  VARCHAR(50) NULL,
    [ReferenceId]    VARCHAR(50) NULL,
    [LinkUrl]        NVARCHAR(255) NULL,
    [IsRead]         BIT NOT NULL CONSTRAINT [DF_Notification_IsRead] DEFAULT (0),
    [ReadAt]         DATETIME2 NULL,           -- [MỚI] Thời điểm đọc thông báo
    [EventId]        VARCHAR(100) NULL,
    [CreatedAt]      DATETIME2 NOT NULL CONSTRAINT [DF_Notification_CreatedAt] DEFAULT (SYSDATETIME()),

    CONSTRAINT [PK_Notification] PRIMARY KEY CLUSTERED ([NotificationId] ASC),
    CONSTRAINT [FK_Notification_User]  FOREIGN KEY ([RecipientId]) REFERENCES [dbo].[User] ([UserId]),
    CONSTRAINT [FK_Notification_Actor] FOREIGN KEY ([ActorId])     REFERENCES [dbo].[User] ([UserId])
);
GO
```

---

## 4. Tối ưu hóa Chỉ mục (Indexes)

### 4.1 Non-Clustered Indexes trên Foreign Keys
Tối ưu hiệu năng truy vấn cho các liên kết bảng thường xuyên:

```sql
CREATE NONCLUSTERED INDEX IX_User_RoleId ON [User](RoleId);
CREATE NONCLUSTERED INDEX IX_User_DepartmentId ON [User](DepartmentId);
CREATE NONCLUSTERED INDEX IX_Department_ManagerId ON Department(ManagerId);
CREATE NONCLUSTERED INDEX IX_AuditLog_UserId ON AuditLog(UserId);
```

### 4.2 Indexes cho Job Requisition
Tối ưu hóa tìm kiếm, phân quyền phòng ban và chống trùng lặp đợt tuyển:

```sql
CREATE NONCLUSTERED INDEX IX_JobRequisition_DepartmentId ON JobRequisition(DepartmentId);
CREATE NONCLUSTERED INDEX IX_JobRequisition_HiringManagerId ON JobRequisition(HiringManagerId);
CREATE NONCLUSTERED INDEX IX_JobRequisition_CreatedBy ON JobRequisition(CreatedBy);

-- Chặn tạo trùng "đợt 2" cho cùng một vị trí (Yêu cầu có cột OriginalRequisitionId trong bảng)
CREATE UNIQUE NONCLUSTERED INDEX UX_JobRequisition_Original_Round
ON JobRequisition(OriginalRequisitionId, RecruitmentRound)
WHERE OriginalRequisitionId IS NOT NULL;
GO
```

### 4.3 Indexes cho Job Posting
Đảm bảo tính duy nhất của tin đang đăng và tối ưu trang danh sách công khai:

```sql
CREATE NONCLUSTERED INDEX IX_JobPosting_RequisitionId ON JobPosting(RequisitionId);
CREATE NONCLUSTERED INDEX IX_JobPosting_CreatedBy ON JobPosting(CreatedBy);

-- Mỗi requisition chỉ có tối đa 1 tin đang công khai
CREATE UNIQUE NONCLUSTERED INDEX UX_JobPosting_ActivePerRequisition
ON JobPosting(RequisitionId)
WHERE PostingStatus = N'Published';

-- Trang danh sách tin tuyển dụng công khai
CREATE NONCLUSTERED INDEX IX_JobPosting_Public
ON JobPosting(PostingStatus, ApplicationDeadline)
INCLUDE (PostingTitle, WorkLocation, SalaryDisplay, NumberOfPositionsDisplay, PostingDate);
GO
```

### 4.4 Indexes cho Hồ sơ & Quy trình tuyển dụng
Tối ưu cho luồng ứng tuyển, phỏng vấn và phê duyệt Offer:

```sql
CREATE NONCLUSTERED INDEX IX_RequisitionApproval_RequisitionId ON RequisitionApproval(RequisitionId);
CREATE NONCLUSTERED INDEX IX_RequisitionApproval_DirectorId ON RequisitionApproval(DirectorId);
CREATE NONCLUSTERED INDEX IX_ScreeningCriteria_RequisitionId ON ScreeningCriteria(RequisitionId);
CREATE NONCLUSTERED INDEX IX_Candidate_IsPotential ON Candidate(IsPotential) WHERE IsPotential = 1;
CREATE NONCLUSTERED INDEX IX_Application_CandidateId ON Application(CandidateId);
CREATE NONCLUSTERED INDEX IX_Application_JobPostingId ON Application(JobPostingId);
CREATE NONCLUSTERED INDEX IX_ApplicationReview_ApplicationId ON ApplicationReview(ApplicationId);
CREATE NONCLUSTERED INDEX IX_ApplicationReview_ReviewerId ON ApplicationReview(ReviewerId);
CREATE NONCLUSTERED INDEX IX_InterviewSchedule_CreatedBy ON InterviewSchedule(CreatedBy);
CREATE NONCLUSTERED INDEX IX_InterviewPanel_InterviewerId ON InterviewPanel(InterviewerId);
CREATE NONCLUSTERED INDEX IX_InterviewEvaluation_InterviewerId ON InterviewEvaluation(InterviewerId);
CREATE NONCLUSTERED INDEX IX_InterviewFinalResult_HiringManagerId ON InterviewFinalResult(HiringManagerId);
CREATE NONCLUSTERED INDEX IX_OfferProposal_ProposedBy ON OfferProposal(ProposedBy);
CREATE NONCLUSTERED INDEX IX_OfferApproval_OfferId ON OfferApproval(OfferId);
CREATE NONCLUSTERED INDEX IX_OfferApproval_DirectorId ON OfferApproval(DirectorId);
CREATE NONCLUSTERED INDEX IX_TalentPoolNote_CandidateId ON TalentPoolNote(CandidateId);
CREATE NONCLUSTERED INDEX IX_TalentPoolNote_UserId ON TalentPoolNote(UserId);
```

### 4.5 Indexes chuyên biệt cho Notification
Tối ưu hiệu năng lấy danh sách thông báo và đếm thông báo chưa đọc trên Topbar:

```sql
CREATE NONCLUSTERED INDEX [IX_Notification_Recipient_CreatedAt]
ON [dbo].[Notification] ([RecipientId] ASC, [CreatedAt] DESC)
INCLUDE ([IsRead], [Title], [EventType], [ReferenceId], [LinkUrl]);
GO

CREATE NONCLUSTERED INDEX [IX_Notification_Unread_Recipient]
ON [dbo].[Notification] ([RecipientId])
WHERE [IsRead] = 0;
GO

CREATE UNIQUE NONCLUSTERED INDEX [UX_Notification_Recipient_EventId]
ON [dbo].[Notification] ([RecipientId], [EventId])
WHERE [EventId] IS NOT NULL;
GO
```

---

## 5. Bảng tổng hợp các thay đổi mới (Schema Changelog)

| Bảng (Table) | Cột / Ràng buộc mới | Kiểu dữ liệu / Thuộc tính | Ý nghĩa nghiệp vụ |
| :--- | :--- | :--- | :--- |
| **`JobRequisition`** | `CreatedBy` | `INT NULL, FK -> [User](UserId)` | Lưu vết người tạo phiếu yêu cầu tuyển dụng |
| | `OriginalRequisitionId` | `INT NULL, FK -> JobRequisition(RequisitionId)` | Liên kết về phiếu yêu cầu đợt 1 (gốc) khi mở đợt tuyển tiếp theo |
| | `HiringType` | `NVARCHAR(20) NULL, CHECK (New/Replacement)` | Loại nhu cầu: Tuyển mới (`New`) hoặc Tuyển thay thế (`Replacement`) |
| | `SubmittedAt` | `DATETIME2 NULL` | Thời điểm nộp đơn lên Giám đốc phê duyệt |
| | `ClosedAt` | `DATETIME2 NULL` | Thời điểm kết thúc/đóng đợt tuyển dụng |
| | `CK_JobRequisition_RoundOrigin` | Ràng buộc kiểm tra logic đợt tuyển | Round = 1 thì OriginalRequisitionId = NULL; Round > 1 thì bắt buộc có OriginalRequisitionId |
| | `UX_JobRequisition_Original_Round` | `UNIQUE FILTERED INDEX` | Chống tạo trùng lặp số đợt tuyển cho cùng một phiếu gốc |
| **`JobPosting`** | `PostingNo` | `INT NOT NULL, DEFAULT 1` | Đánh số thứ tự lần đăng tin của cùng một Requisition |
| | `NumberOfPositionsDisplay` | `NVARCHAR(50) NULL` | Tùy biến chuỗi hiển thị số lượng tuyển trên trang public (NULL = không hiển thị) |
| | `ClosedAt` | `DATETIME2 NULL` | Thời điểm chính thức đóng tin tuyển dụng |
| | `UQ_JobPosting_Requisition_PostingNo` | `UNIQUE (RequisitionId, PostingNo)` | Đảm bảo mỗi lần đăng tin của 1 Requisition có số hiệu duy nhất |
| | `UX_JobPosting_ActivePerRequisition` | `UNIQUE FILTERED INDEX` | Đảm bảo tại 1 thời điểm chỉ có tối đa 1 tin `Published` cho mỗi Requisition |
| | `IX_JobPosting_Public` | `COVERING INDEX` | Tối ưu hóa truy vấn hiển thị trang danh sách việc làm công khai |
| **`Notification`** | `ActorId` | `INT NULL, FK -> [User](UserId)` | Người thực hiện hành động kích hoạt thông báo (NULL = do hệ thống tự sinh) |
| | `ReadAt` | `DATETIME2 NULL` | Thời điểm người dùng thực hiện đọc thông báo |
| | `FK_Notification_Actor` | `FOREIGN KEY` | Ràng buộc khóa ngoại đảm bảo toàn vẹn liên kết với bảng `[User]` |