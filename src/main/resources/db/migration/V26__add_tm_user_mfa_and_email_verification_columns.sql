IF OBJECT_ID(N'dbo.tm_users', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.tm_users', 'mfa_enabled') IS NULL
    BEGIN
        ALTER TABLE dbo.tm_users ADD mfa_enabled BIT NOT NULL CONSTRAINT df_tm_users_mfa_enabled DEFAULT 0;
    END;

    IF COL_LENGTH('dbo.tm_users', 'mfa_secret') IS NULL
    BEGIN
        ALTER TABLE dbo.tm_users ADD mfa_secret NVARCHAR(512) NULL;
    END;

    IF COL_LENGTH('dbo.tm_users', 'mfa_secret_temp') IS NULL
    BEGIN
        ALTER TABLE dbo.tm_users ADD mfa_secret_temp NVARCHAR(512) NULL;
    END;

    IF COL_LENGTH('dbo.tm_users', 'mfa_email_otp') IS NULL
    BEGIN
        ALTER TABLE dbo.tm_users ADD mfa_email_otp NVARCHAR(10) NULL;
    END;

    IF COL_LENGTH('dbo.tm_users', 'mfa_email_otp_expires_at') IS NULL
    BEGIN
        ALTER TABLE dbo.tm_users ADD mfa_email_otp_expires_at DATETIMEOFFSET(7) NULL;
    END;

    IF COL_LENGTH('dbo.tm_users', 'mfa_email_verified') IS NULL
    BEGIN
        ALTER TABLE dbo.tm_users ADD mfa_email_verified BIT NOT NULL CONSTRAINT df_tm_users_mfa_email_verified DEFAULT 0;
    END;
END;
