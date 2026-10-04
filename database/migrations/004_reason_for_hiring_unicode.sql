-- Explicitly approved change to the existing column; no new columns or tables.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
ALTER TABLE JobRequisition ALTER COLUMN ReasonForHiring NVARCHAR(2000) NULL;
COMMIT TRANSACTION;
