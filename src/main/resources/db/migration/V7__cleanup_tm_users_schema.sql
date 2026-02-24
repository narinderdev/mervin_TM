-- Cleanup for production: remove camelCase duplicate columns and ensure timestamps use datetimeoffset(7).
IF OBJECT_ID(N'dbo.tm_users', N'U') IS NOT NULL
BEGIN
    -- Copy data from legacy camelCase columns into the canonical snake_case columns when both exist.
    IF COL_LENGTH('dbo.tm_users', 'firstName') IS NOT NULL
       AND COL_LENGTH('dbo.tm_users', 'first_name') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.tm_users SET first_name = COALESCE(first_name, firstName);';
    END;

    IF COL_LENGTH('dbo.tm_users', 'lastName') IS NOT NULL
       AND COL_LENGTH('dbo.tm_users', 'last_name') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.tm_users SET last_name = COALESCE(last_name, lastName);';
    END;

    IF COL_LENGTH('dbo.tm_users', 'passwordHash') IS NOT NULL
       AND COL_LENGTH('dbo.tm_users', 'password_hash') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.tm_users SET password_hash = COALESCE(password_hash, passwordHash);';
    END;

    IF COL_LENGTH('dbo.tm_users', 'createdAt') IS NOT NULL
       AND COL_LENGTH('dbo.tm_users', 'created_at') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.tm_users SET created_at = COALESCE(created_at, CAST(createdAt AS DATETIME2(7)));';
    END;

    IF COL_LENGTH('dbo.tm_users', 'updatedAt') IS NOT NULL
       AND COL_LENGTH('dbo.tm_users', 'updated_at') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.tm_users SET updated_at = COALESCE(updated_at, CAST(updatedAt AS DATETIME2(7)));';
    END;

    -- Drop legacy camelCase columns if they still exist.
    IF COL_LENGTH('dbo.tm_users', 'firstName') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'ALTER TABLE dbo.tm_users DROP COLUMN firstName;';
    END;

    IF COL_LENGTH('dbo.tm_users', 'lastName') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'ALTER TABLE dbo.tm_users DROP COLUMN lastName;';
    END;

    IF COL_LENGTH('dbo.tm_users', 'passwordHash') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'ALTER TABLE dbo.tm_users DROP COLUMN passwordHash;';
    END;

    IF COL_LENGTH('dbo.tm_users', 'createdAt') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'ALTER TABLE dbo.tm_users DROP COLUMN createdAt;';
    END;

    IF COL_LENGTH('dbo.tm_users', 'updatedAt') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'ALTER TABLE dbo.tm_users DROP COLUMN updatedAt;';
    END;

    -- Normalize timestamp types to datetimeoffset(7) as expected by Hibernate for Instant.
    IF COL_LENGTH('dbo.tm_users', 'created_at') IS NOT NULL
    BEGIN
        UPDATE dbo.tm_users SET created_at = COALESCE(created_at, SYSUTCDATETIME());
        EXEC sp_executesql N'ALTER TABLE dbo.tm_users ALTER COLUMN created_at DATETIMEOFFSET(7) NOT NULL;';
    END;

    IF COL_LENGTH('dbo.tm_users', 'updated_at') IS NOT NULL
    BEGIN
        UPDATE dbo.tm_users SET updated_at = COALESCE(updated_at, SYSUTCDATETIME());
        EXEC sp_executesql N'ALTER TABLE dbo.tm_users ALTER COLUMN updated_at DATETIMEOFFSET(7) NOT NULL;';
    END;
END;
