-- Remove configuration inserted by the retired chatbot migration (010_chatbot_config.sql).
-- No schema changes. Run once on databases where that migration was applied.
USE RitirementManagement2;
GO

DELETE FROM dbo.SystemConfig
WHERE ConfigKey IN (N'CHATBOT_ENABLED', N'CHATBOT_FAQ', N'HR_CONTACT_EMAIL');
GO
