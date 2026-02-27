-- Create tables for TM teams and memberships
IF OBJECT_ID(N'dbo.tm_teams', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.tm_teams (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        team_name NVARCHAR(150) NOT NULL UNIQUE,
        team_description NVARCHAR(500) NULL,
        status NVARCHAR(20) NOT NULL CONSTRAINT df_tm_teams_status DEFAULT 'ACTIVE',
        created_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_tm_teams_created_at DEFAULT SYSDATETIMEOFFSET(),
        updated_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_tm_teams_updated_at DEFAULT SYSDATETIMEOFFSET()
    );
END;

IF OBJECT_ID(N'dbo.tm_team_members', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.tm_team_members (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        team_id BIGINT NOT NULL,
        technician_id BIGINT NOT NULL,
        team_leader BIT NOT NULL CONSTRAINT df_tm_team_members_leader DEFAULT 0,
        CONSTRAINT fk_tm_team_members_team FOREIGN KEY (team_id) REFERENCES dbo.tm_teams(id) ON DELETE CASCADE,
        CONSTRAINT uq_tm_team_members UNIQUE (team_id, technician_id)
    );
END;
