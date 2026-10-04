-- Migration 005: Create Notification table
-- Hỗ trợ thông báo trong hệ thống cho Hiring Manager khi đơn bị từ chối / duyệt / đăng tin
CREATE TABLE Notification (
    NotificationId BIGINT IDENTITY(1,1) PRIMARY KEY,
    RecipientId INT NOT NULL,
    Title NVARCHAR(255) NOT NULL,
    Content NVARCHAR(1000) NOT NULL,
    EventType NVARCHAR(50) NOT NULL,
    ReferenceId NVARCHAR(50) NULL,
    LinkUrl NVARCHAR(255) NULL,
    IsRead BIT NOT NULL CONSTRAINT DF_Notification_IsRead DEFAULT 0,
    EventId NVARCHAR(100) NULL,
    CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Notification_CreatedAt DEFAULT SYSDATETIME(),

    CONSTRAINT FK_Notification_Recipient
        FOREIGN KEY (RecipientId) REFERENCES [User](UserId),

    CONSTRAINT UQ_Notification_EventId
        UNIQUE (EventId)
);

CREATE NONCLUSTERED INDEX IX_Notification_Recipient_IsRead ON Notification(RecipientId, IsRead);
