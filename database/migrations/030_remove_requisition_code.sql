-- RequisitionId is the identifier; the redundant display code is no longer stored.
IF COL_LENGTH('dbo.JobRequisition', 'RequisitionCode') IS NOT NULL
BEGIN
    ALTER TABLE dbo.JobRequisition DROP COLUMN RequisitionCode;
END;
GO

-- Remove old display codes from notifications already stored before this migration.
;WITH Matches AS (
    SELECT NotificationId, [Content], PATINDEX('%REQ-[0-9]%', [Content]) AS CodeStart
    FROM dbo.[Notification]
    WHERE [Content] LIKE '%REQ-[0-9]%'
), Codes AS (
    SELECT NotificationId,
           SUBSTRING([Content], CodeStart,
               4 + PATINDEX('%[^0-9]%', SUBSTRING([Content], CodeStart + 4, 50) + 'X') - 1) AS Code
    FROM Matches
)
UPDATE notification
SET [Content] = REPLACE(
                    REPLACE(
                        REPLACE(notification.[Content], N' (Mã: ' + codes.Code + N', Phòng ban:', N' (Phòng ban:'),
                        N' (Mã: ' + codes.Code + N')', N''),
                    N' (cho ' + codes.Code + N')', N'')
FROM dbo.[Notification] AS notification
JOIN Codes AS codes ON codes.NotificationId = notification.NotificationId;
GO
