-- Align tm_user_invites timestamp columns with Hibernate expectations (datetimeoffset(7))

IF OBJECT_ID(N'dbo.tm_user_invites', N'U') IS NOT NULL
BEGIN
    -- Drop default constraint on created_at if present, then alter to datetimeoffset
    DECLARE @df_created NVARCHAR(200);
    SELECT @df_created = dc.name
    FROM sys.default_constraints dc
    INNER JOIN sys.columns c ON dc.parent_object_id = c.object_id AND dc.parent_column_id = c.column_id
    WHERE dc.parent_object_id = OBJECT_ID('dbo.tm_user_invites')
      AND c.name = 'created_at';
    IF @df_created IS NOT NULL
    BEGIN
        DECLARE @sqlCreated NVARCHAR(400);
        SET @sqlCreated = N'ALTER TABLE dbo.tm_user_invites DROP CONSTRAINT ' + QUOTENAME(@df_created) + N';';
        EXEC sp_executesql @sqlCreated;
    END;

    ALTER TABLE dbo.tm_user_invites ALTER COLUMN created_at DATETIMEOFFSET(7) NOT NULL;
    -- Re-add default
    ALTER TABLE dbo.tm_user_invites ADD CONSTRAINT df_tm_user_invites_created DEFAULT SYSDATETIMEOFFSET() FOR created_at;

    -- expires_at should also be datetimeoffset
    DECLARE @df_expires NVARCHAR(200);
    SELECT @df_expires = dc.name
    FROM sys.default_constraints dc
    INNER JOIN sys.columns c ON dc.parent_object_id = c.object_id AND dc.parent_column_id = c.column_id
    WHERE dc.parent_object_id = OBJECT_ID('dbo.tm_user_invites')
      AND c.name = 'expires_at';
    IF @df_expires IS NOT NULL
    BEGIN
        DECLARE @sqlExpires NVARCHAR(400);
        SET @sqlExpires = N'ALTER TABLE dbo.tm_user_invites DROP CONSTRAINT ' + QUOTENAME(@df_expires) + N';';
        EXEC sp_executesql @sqlExpires;
    END;

    ALTER TABLE dbo.tm_user_invites ALTER COLUMN expires_at DATETIMEOFFSET(7) NOT NULL;
END;
