-- Add Expired without removing any status accepted by the existing constraint.
-- No offer or application rows are updated by this migration.
SET XACT_ABORT ON;
SET NOCOUNT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @definition nvarchar(max);
    SELECT @definition = definition FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'dbo.OfferProposal') AND name = N'CK_OfferProposal_Status';
    IF @definition IS NULL
        THROW 50001, 'OfferProposal status constraint was not found; inspect the schema before migrating.', 1;
    IF CHARINDEX(N'''Expired''', @definition) = 0
    BEGIN
        ALTER TABLE dbo.OfferProposal DROP CONSTRAINT CK_OfferProposal_Status;
        DECLARE @sql nvarchar(max) = N'ALTER TABLE dbo.OfferProposal WITH CHECK ADD CONSTRAINT CK_OfferProposal_Status CHECK (('
                + @definition + N') OR [OfferStatus] = N''Expired'')';
        EXEC sys.sp_executesql @sql;
    END;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
