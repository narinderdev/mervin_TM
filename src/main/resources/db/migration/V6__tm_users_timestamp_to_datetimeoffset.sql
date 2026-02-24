-- Align tm_users timestamp columns with Hibernate expectation (datetimeoffset(7) for Instant).
IF OBJECT_ID(N'dbo.tm_users', N'U') IS NOT NULL
BEGIN
    -- Ensure NOT NULL columns can be altered by filling any accidental nulls.
    UPDATE dbo.tm_users
    SET created_at = SYSUTCDATETIME()
    WHERE created_at IS NULL;

    UPDATE dbo.tm_users
    SET updated_at = SYSUTCDATETIME()
    WHERE updated_at IS NULL;

    -- Alter column types to datetimeoffset(7). Implicit conversion will add +00:00 offset.
    ALTER TABLE dbo.tm_users ALTER COLUMN created_at DATETIMEOFFSET(7) NOT NULL;
    ALTER TABLE dbo.tm_users ALTER COLUMN updated_at DATETIMEOFFSET(7) NOT NULL;
END;
