IF OBJECT_ID(N'dbo.tm_users', N'U') IS NOT NULL
BEGIN
    -- Move any remaining data from legacy camelCase columns into the canonical snake_case columns.
    IF COL_LENGTH('dbo.tm_users', 'firstName') IS NOT NULL
       AND COL_LENGTH('dbo.tm_users', 'first_name') IS NOT NULL
    BEGIN
        UPDATE dbo.tm_users
        SET first_name = COALESCE(first_name, firstName);
    END;

    IF COL_LENGTH('dbo.tm_users', 'lastName') IS NOT NULL
       AND COL_LENGTH('dbo.tm_users', 'last_name') IS NOT NULL
    BEGIN
        UPDATE dbo.tm_users
        SET last_name = COALESCE(last_name, lastName);
    END;

    IF COL_LENGTH('dbo.tm_users', 'passwordHash') IS NOT NULL
       AND COL_LENGTH('dbo.tm_users', 'password_hash') IS NOT NULL
    BEGIN
        UPDATE dbo.tm_users
        SET password_hash = COALESCE(password_hash, passwordHash);
    END;

    IF COL_LENGTH('dbo.tm_users', 'createdAt') IS NOT NULL
       AND COL_LENGTH('dbo.tm_users', 'created_at') IS NOT NULL
    BEGIN
        UPDATE dbo.tm_users
        SET created_at = COALESCE(created_at, CAST(createdAt AS DATETIME2(7)));
    END;

    IF COL_LENGTH('dbo.tm_users', 'updatedAt') IS NOT NULL
       AND COL_LENGTH('dbo.tm_users', 'updated_at') IS NOT NULL
    BEGIN
        UPDATE dbo.tm_users
        SET updated_at = COALESCE(updated_at, CAST(updatedAt AS DATETIME2(7)));
    END;

    -- Drop the legacy camelCase columns if they still exist.
    IF COL_LENGTH('dbo.tm_users', 'firstName') IS NOT NULL
    BEGIN
        ALTER TABLE dbo.tm_users DROP COLUMN firstName;
    END;

    IF COL_LENGTH('dbo.tm_users', 'lastName') IS NOT NULL
    BEGIN
        ALTER TABLE dbo.tm_users DROP COLUMN lastName;
    END;

    IF COL_LENGTH('dbo.tm_users', 'passwordHash') IS NOT NULL
    BEGIN
        ALTER TABLE dbo.tm_users DROP COLUMN passwordHash;
    END;

    IF COL_LENGTH('dbo.tm_users', 'createdAt') IS NOT NULL
    BEGIN
        ALTER TABLE dbo.tm_users DROP COLUMN createdAt;
    END;

    IF COL_LENGTH('dbo.tm_users', 'updatedAt') IS NOT NULL
    BEGIN
        ALTER TABLE dbo.tm_users DROP COLUMN updatedAt;
    END;
END;
