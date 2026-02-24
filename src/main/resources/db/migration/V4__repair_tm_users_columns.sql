IF OBJECT_ID(N'dbo.tm_users', N'U') IS NOT NULL
BEGIN
    -- Rename legacy camelCase columns to the expected snake_case names.
    IF COL_LENGTH('dbo.tm_users', 'first_name') IS NULL
       AND COL_LENGTH('dbo.tm_users', 'firstName') IS NOT NULL
    BEGIN
        EXEC sp_rename N'dbo.tm_users.firstName', N'first_name', N'COLUMN';
    END;

    IF COL_LENGTH('dbo.tm_users', 'last_name') IS NULL
       AND COL_LENGTH('dbo.tm_users', 'lastName') IS NOT NULL
    BEGIN
        EXEC sp_rename N'dbo.tm_users.lastName', N'last_name', N'COLUMN';
    END;

    IF COL_LENGTH('dbo.tm_users', 'password_hash') IS NULL
       AND COL_LENGTH('dbo.tm_users', 'passwordHash') IS NOT NULL
    BEGIN
        EXEC sp_rename N'dbo.tm_users.passwordHash', N'password_hash', N'COLUMN';
    END;

    IF COL_LENGTH('dbo.tm_users', 'created_at') IS NULL
       AND COL_LENGTH('dbo.tm_users', 'createdAt') IS NOT NULL
    BEGIN
        EXEC sp_rename N'dbo.tm_users.createdAt', N'created_at', N'COLUMN';
    END;

    IF COL_LENGTH('dbo.tm_users', 'updated_at') IS NULL
       AND COL_LENGTH('dbo.tm_users', 'updatedAt') IS NOT NULL
    BEGIN
        EXEC sp_rename N'dbo.tm_users.updatedAt', N'updated_at', N'COLUMN';
    END;

    -- Add missing columns as nullable first so migration succeeds on non-empty tables.
    IF COL_LENGTH('dbo.tm_users', 'first_name') IS NULL
    BEGIN
        ALTER TABLE dbo.tm_users ADD first_name NVARCHAR(50) NULL;
    END;

    IF COL_LENGTH('dbo.tm_users', 'last_name') IS NULL
    BEGIN
        ALTER TABLE dbo.tm_users ADD last_name NVARCHAR(50) NULL;
    END;

    IF COL_LENGTH('dbo.tm_users', 'password_hash') IS NULL
    BEGIN
        ALTER TABLE dbo.tm_users ADD password_hash NVARCHAR(255) NULL;
    END;

    IF COL_LENGTH('dbo.tm_users', 'created_at') IS NULL
    BEGIN
        ALTER TABLE dbo.tm_users ADD created_at DATETIME2(7) NULL;
    END;

    IF COL_LENGTH('dbo.tm_users', 'updated_at') IS NULL
    BEGIN
        ALTER TABLE dbo.tm_users ADD updated_at DATETIME2(7) NULL;
    END;
END;
GO

IF OBJECT_ID(N'dbo.tm_users', N'U') IS NOT NULL
BEGIN
    -- Copy data from legacy camelCase columns when both exist (dynamic SQL avoids compile errors on older schemas).
    IF COL_LENGTH('dbo.tm_users', 'first_name') IS NOT NULL
       AND COL_LENGTH('dbo.tm_users', 'firstName') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.tm_users SET first_name = firstName WHERE first_name IS NULL AND firstName IS NOT NULL;';
    END;

    IF COL_LENGTH('dbo.tm_users', 'last_name') IS NOT NULL
       AND COL_LENGTH('dbo.tm_users', 'lastName') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.tm_users SET last_name = lastName WHERE last_name IS NULL AND lastName IS NOT NULL;';
    END;

    IF COL_LENGTH('dbo.tm_users', 'password_hash') IS NOT NULL
       AND COL_LENGTH('dbo.tm_users', 'passwordHash') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.tm_users SET password_hash = passwordHash WHERE password_hash IS NULL AND passwordHash IS NOT NULL;';
    END;

    IF COL_LENGTH('dbo.tm_users', 'created_at') IS NOT NULL
       AND COL_LENGTH('dbo.tm_users', 'createdAt') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.tm_users SET created_at = createdAt WHERE created_at IS NULL AND createdAt IS NOT NULL;';
    END;

    IF COL_LENGTH('dbo.tm_users', 'updated_at') IS NOT NULL
       AND COL_LENGTH('dbo.tm_users', 'updatedAt') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.tm_users SET updated_at = updatedAt WHERE updated_at IS NULL AND updatedAt IS NOT NULL;';
    END;

    -- Backfill any remaining nulls, then enforce NOT NULL constraints (only after columns exist).
    IF COL_LENGTH('dbo.tm_users', 'first_name') IS NOT NULL
    BEGIN
        UPDATE dbo.tm_users SET first_name = N'' WHERE first_name IS NULL;
        ALTER TABLE dbo.tm_users ALTER COLUMN first_name NVARCHAR(50) NOT NULL;
    END;

    IF COL_LENGTH('dbo.tm_users', 'last_name') IS NOT NULL
    BEGIN
        UPDATE dbo.tm_users SET last_name = N'' WHERE last_name IS NULL;
        ALTER TABLE dbo.tm_users ALTER COLUMN last_name NVARCHAR(50) NOT NULL;
    END;

    IF COL_LENGTH('dbo.tm_users', 'password_hash') IS NOT NULL
    BEGIN
        UPDATE dbo.tm_users SET password_hash = N'LEGACY_MISSING' WHERE password_hash IS NULL;
        ALTER TABLE dbo.tm_users ALTER COLUMN password_hash NVARCHAR(255) NOT NULL;
    END;

    IF COL_LENGTH('dbo.tm_users', 'created_at') IS NOT NULL
    BEGIN
        UPDATE dbo.tm_users SET created_at = SYSUTCDATETIME() WHERE created_at IS NULL;
        ALTER TABLE dbo.tm_users ALTER COLUMN created_at DATETIME2(7) NOT NULL;
    END;

    IF COL_LENGTH('dbo.tm_users', 'updated_at') IS NOT NULL
    BEGIN
        UPDATE dbo.tm_users SET updated_at = SYSUTCDATETIME() WHERE updated_at IS NULL;
        ALTER TABLE dbo.tm_users ALTER COLUMN updated_at DATETIME2(7) NOT NULL;
    END;
END;
