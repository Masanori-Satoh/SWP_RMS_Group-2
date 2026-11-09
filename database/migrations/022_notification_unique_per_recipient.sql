-- 022: Đồng bộ ràng buộc chống trùng của Notification với db.sql (TC-NOTIF-DECIDE-05).
-- Migration 005 tạo UQ_Notification_EventId = UNIQUE(EventId): cùng một sự kiện báo cho HM và HR
-- (cùng EventId) thì bản ghi thứ hai bị chặn. db.sql (chuẩn) dùng UNIQUE(RecipientId, EventId)
-- WHERE EventId IS NOT NULL: mỗi người nhận một bản cho mỗi sự kiện.
-- Chạy trên database mà app đang cấu hình (không có USE). Chạy lại nhiều lần vẫn an toàn.
SET XACT_ABORT ON;
SET NOCOUNT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    IF EXISTS (SELECT 1 FROM sys.key_constraints
               WHERE name = N'UQ_Notification_EventId' AND parent_object_id = OBJECT_ID(N'dbo.Notification'))
        ALTER TABLE dbo.Notification DROP CONSTRAINT UQ_Notification_EventId;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes
                   WHERE name = N'UX_Notification_Recipient_EventId' AND object_id = OBJECT_ID(N'dbo.Notification'))
        CREATE UNIQUE NONCLUSTERED INDEX UX_Notification_Recipient_EventId
            ON dbo.Notification (RecipientId, EventId)
            WHERE EventId IS NOT NULL;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
