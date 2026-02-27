-- Invitations for technicians to join TM
IF OBJECT_ID(N'dbo.tm_user_invites', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.tm_user_invites (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        first_name NVARCHAR(50) NULL,
        last_name NVARCHAR(50) NULL,
        email NVARCHAR(150) NOT NULL,
        token NVARCHAR(64) NOT NULL,
        role NVARCHAR(50) NOT NULL CONSTRAINT df_tm_user_invites_role DEFAULT 'Technician',
        expires_at DATETIME2 NOT NULL,
        accepted BIT NOT NULL CONSTRAINT df_tm_user_invites_accepted DEFAULT 0,
        created_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_tm_user_invites_created DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT uq_tm_user_invites_email UNIQUE (email),
        CONSTRAINT uq_tm_user_invites_token UNIQUE (token)
    );
END;
