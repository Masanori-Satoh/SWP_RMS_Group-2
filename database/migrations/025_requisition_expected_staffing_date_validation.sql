-- Backfill legacy requisitions from the latest known recruitment milestone,
-- then require an estimated staffing date for every submitted requisition.
-- The inferred date is 30 days after the latest of CreatedAt, SubmittedAt,
-- and any posting's ApplicationDeadline. Existing non-NULL dates are preserved.
SET XACT_ABORT ON;
SET NOCOUNT ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.JobRequisition', N'U') IS NULL
       OR COL_LENGTH(N'dbo.JobRequisition', N'ExpectedStaffingDate') IS NULL
        THROW 50001, 'JobRequisition.ExpectedStaffingDate is missing; run migration 024 first.', 1;

    IF EXISTS (SELECT 1 FROM dbo.JobRequisition
               WHERE ExpectedStaffingDate IS NOT NULL
                 AND (ExpectedStaffingDate < CONVERT(DATE, CreatedAt)
                      OR (SubmittedAt IS NOT NULL
                          AND ExpectedStaffingDate < CONVERT(DATE, SubmittedAt))))
        THROW 50002, 'Existing staffing dates precede creation or submission; review them first.', 1;

    UPDATE r
       SET ExpectedStaffingDate = DATEADD(DAY, 30, milestone.BaseDate)
    FROM dbo.JobRequisition r
    OUTER APPLY (SELECT MAX(j.ApplicationDeadline) AS LatestDeadline
                 FROM dbo.JobPosting j WHERE j.RequisitionId = r.RequisitionId) posting
    CROSS APPLY (SELECT MAX(d.EventDate) AS BaseDate
                 FROM (VALUES (CONVERT(DATE, r.CreatedAt)),
                              (CONVERT(DATE, r.SubmittedAt)),
                              (CONVERT(DATE, posting.LatestDeadline))) d(EventDate)) milestone
    WHERE r.ApprovalStatus <> N'Draft'
      AND r.ExpectedStaffingDate IS NULL;

    DECLARE @Backfilled INT = @@ROWCOUNT;

    IF EXISTS (SELECT 1 FROM dbo.JobRequisition
               WHERE ApprovalStatus <> N'Draft' AND ExpectedStaffingDate IS NULL)
        THROW 50003, 'A submitted requisition still has no staffing date.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
                   WHERE parent_object_id = OBJECT_ID(N'dbo.JobRequisition')
                     AND name = N'CK_JobRequisition_ExpectedStaffingDate_Required')
        ALTER TABLE dbo.JobRequisition WITH CHECK ADD
            CONSTRAINT CK_JobRequisition_ExpectedStaffingDate_Required
            CHECK (ApprovalStatus = N'Draft' OR ExpectedStaffingDate IS NOT NULL);

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
                   WHERE parent_object_id = OBJECT_ID(N'dbo.JobRequisition')
                     AND name = N'CK_JobRequisition_ExpectedStaffingDate_Chronology')
        ALTER TABLE dbo.JobRequisition WITH CHECK ADD
            CONSTRAINT CK_JobRequisition_ExpectedStaffingDate_Chronology
            CHECK (ExpectedStaffingDate IS NULL OR
                (ExpectedStaffingDate >= CONVERT(DATE, CreatedAt)
                 AND (SubmittedAt IS NULL OR ExpectedStaffingDate >= CONVERT(DATE, SubmittedAt))));

    COMMIT TRANSACTION;
    SELECT @Backfilled AS BackfilledRequisitions;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
