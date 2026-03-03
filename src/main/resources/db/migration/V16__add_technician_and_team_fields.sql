-- Ensure technician and technician-team schemas exist for CRUD APIs.

IF OBJECT_ID(N'dbo.technicians', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.technicians (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        technician_id NVARCHAR(64) NOT NULL,
        badge_number NVARCHAR(64) NOT NULL,
        first_name NVARCHAR(100) NOT NULL,
        last_name NVARCHAR(100) NOT NULL,
        full_name NVARCHAR(200) NULL,
        technician_type NVARCHAR(32) NOT NULL CONSTRAINT df_technicians_technician_type DEFAULT 'FULL_TIME',
        skills NVARCHAR(MAX) NULL,
        phone_number NVARCHAR(20) NULL,
        email NVARCHAR(100) NULL,
        address NVARCHAR(MAX) NULL,
        status NVARCHAR(32) NOT NULL CONSTRAINT df_technicians_status DEFAULT 'ACTIVE',
        hire_date DATE NULL,
        work_shift NVARCHAR(64) NULL,
        technician_photo_url NVARCHAR(512) NULL,
        certificate_url NVARCHAR(512) NULL,
        certificate_issue_date DATE NULL,
        certificate_expiry_date DATE NULL,
        termination_date DATE NULL,
        certifications NVARCHAR(MAX) NULL,
        notes NVARCHAR(MAX) NULL,
        is_deleted BIT NOT NULL CONSTRAINT df_technicians_is_deleted DEFAULT 0
    );
END;

IF OBJECT_ID(N'dbo.technicians', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.technicians', 'technician_id') IS NULL
        ALTER TABLE dbo.technicians ADD technician_id NVARCHAR(64) NULL;

    IF COL_LENGTH('dbo.technicians', 'badge_number') IS NULL
        ALTER TABLE dbo.technicians ADD badge_number NVARCHAR(64) NULL;

    IF COL_LENGTH('dbo.technicians', 'first_name') IS NULL
        ALTER TABLE dbo.technicians ADD first_name NVARCHAR(100) NULL;

    IF COL_LENGTH('dbo.technicians', 'last_name') IS NULL
        ALTER TABLE dbo.technicians ADD last_name NVARCHAR(100) NULL;

    IF COL_LENGTH('dbo.technicians', 'full_name') IS NULL
        ALTER TABLE dbo.technicians ADD full_name NVARCHAR(200) NULL;

    IF COL_LENGTH('dbo.technicians', 'technician_type') IS NULL
    BEGIN
        ALTER TABLE dbo.technicians ADD technician_type NVARCHAR(32) NULL;
        UPDATE dbo.technicians SET technician_type = 'FULL_TIME' WHERE technician_type IS NULL;
        ALTER TABLE dbo.technicians ALTER COLUMN technician_type NVARCHAR(32) NOT NULL;
    END;

    IF COL_LENGTH('dbo.technicians', 'skills') IS NULL
        ALTER TABLE dbo.technicians ADD skills NVARCHAR(MAX) NULL;

    IF COL_LENGTH('dbo.technicians', 'phone_number') IS NULL
        ALTER TABLE dbo.technicians ADD phone_number NVARCHAR(20) NULL;

    IF COL_LENGTH('dbo.technicians', 'email') IS NULL
        ALTER TABLE dbo.technicians ADD email NVARCHAR(100) NULL;

    IF COL_LENGTH('dbo.technicians', 'address') IS NULL
        ALTER TABLE dbo.technicians ADD address NVARCHAR(MAX) NULL;

    IF COL_LENGTH('dbo.technicians', 'status') IS NULL
    BEGIN
        ALTER TABLE dbo.technicians ADD status NVARCHAR(32) NULL;
        UPDATE dbo.technicians SET status = 'ACTIVE' WHERE status IS NULL;
        ALTER TABLE dbo.technicians ALTER COLUMN status NVARCHAR(32) NOT NULL;
    END;

    IF COL_LENGTH('dbo.technicians', 'hire_date') IS NULL
        ALTER TABLE dbo.technicians ADD hire_date DATE NULL;

    IF COL_LENGTH('dbo.technicians', 'work_shift') IS NULL
        ALTER TABLE dbo.technicians ADD work_shift NVARCHAR(64) NULL;

    IF COL_LENGTH('dbo.technicians', 'technician_photo_url') IS NULL
        ALTER TABLE dbo.technicians ADD technician_photo_url NVARCHAR(512) NULL;

    IF COL_LENGTH('dbo.technicians', 'certificate_url') IS NULL
        ALTER TABLE dbo.technicians ADD certificate_url NVARCHAR(512) NULL;

    IF COL_LENGTH('dbo.technicians', 'certificate_issue_date') IS NULL
        ALTER TABLE dbo.technicians ADD certificate_issue_date DATE NULL;

    IF COL_LENGTH('dbo.technicians', 'certificate_expiry_date') IS NULL
        ALTER TABLE dbo.technicians ADD certificate_expiry_date DATE NULL;

    IF COL_LENGTH('dbo.technicians', 'termination_date') IS NULL
        ALTER TABLE dbo.technicians ADD termination_date DATE NULL;

    IF COL_LENGTH('dbo.technicians', 'certifications') IS NULL
        ALTER TABLE dbo.technicians ADD certifications NVARCHAR(MAX) NULL;

    IF COL_LENGTH('dbo.technicians', 'notes') IS NULL
        ALTER TABLE dbo.technicians ADD notes NVARCHAR(MAX) NULL;

    IF COL_LENGTH('dbo.technicians', 'is_deleted') IS NULL
    BEGIN
        ALTER TABLE dbo.technicians ADD is_deleted BIT NULL;
        UPDATE dbo.technicians SET is_deleted = 0 WHERE is_deleted IS NULL;
        ALTER TABLE dbo.technicians ALTER COLUMN is_deleted BIT NOT NULL;
    END;

    UPDATE dbo.technicians
    SET full_name = NULLIF(LTRIM(RTRIM(COALESCE(first_name, '') + ' ' + COALESCE(last_name, ''))), '')
    WHERE (full_name IS NULL OR LTRIM(RTRIM(full_name)) = '')
      AND (first_name IS NOT NULL OR last_name IS NOT NULL);
END;

IF OBJECT_ID(N'dbo.technician_teams', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.technician_teams (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        team_name NVARCHAR(100) NOT NULL,
        team_description NVARCHAR(MAX) NULL,
        status NVARCHAR(32) NOT NULL CONSTRAINT df_technician_teams_status DEFAULT 'ACTIVE',
        start_date DATE NULL,
        end_date DATE NULL,
        notes NVARCHAR(MAX) NULL
    );
END;

IF OBJECT_ID(N'dbo.technician_teams', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.technician_teams', 'team_name') IS NULL
        ALTER TABLE dbo.technician_teams ADD team_name NVARCHAR(100) NULL;

    IF COL_LENGTH('dbo.technician_teams', 'team_description') IS NULL
        ALTER TABLE dbo.technician_teams ADD team_description NVARCHAR(MAX) NULL;

    IF COL_LENGTH('dbo.technician_teams', 'status') IS NULL
    BEGIN
        ALTER TABLE dbo.technician_teams ADD status NVARCHAR(32) NULL;
        UPDATE dbo.technician_teams SET status = 'ACTIVE' WHERE status IS NULL;
        ALTER TABLE dbo.technician_teams ALTER COLUMN status NVARCHAR(32) NOT NULL;
    END;

    IF COL_LENGTH('dbo.technician_teams', 'start_date') IS NULL
        ALTER TABLE dbo.technician_teams ADD start_date DATE NULL;

    IF COL_LENGTH('dbo.technician_teams', 'end_date') IS NULL
        ALTER TABLE dbo.technician_teams ADD end_date DATE NULL;

    IF COL_LENGTH('dbo.technician_teams', 'notes') IS NULL
        ALTER TABLE dbo.technician_teams ADD notes NVARCHAR(MAX) NULL;
END;

IF OBJECT_ID(N'dbo.technician_team_members', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.technician_team_members (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        team_id BIGINT NOT NULL,
        technician_id BIGINT NOT NULL,
        team_leader BIT NOT NULL CONSTRAINT df_technician_team_members_leader DEFAULT 0
    );
END;

IF OBJECT_ID(N'dbo.technician_team_members', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.technician_team_members', 'team_id') IS NULL
        ALTER TABLE dbo.technician_team_members ADD team_id BIGINT NULL;

    IF COL_LENGTH('dbo.technician_team_members', 'technician_id') IS NULL
        ALTER TABLE dbo.technician_team_members ADD technician_id BIGINT NULL;

    IF COL_LENGTH('dbo.technician_team_members', 'team_leader') IS NULL
    BEGIN
        ALTER TABLE dbo.technician_team_members ADD team_leader BIT NULL;
        UPDATE dbo.technician_team_members SET team_leader = 0 WHERE team_leader IS NULL;
        ALTER TABLE dbo.technician_team_members ALTER COLUMN team_leader BIT NOT NULL;
    END;
END;

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'uk_technicians_technician_id'
      AND object_id = OBJECT_ID(N'dbo.technicians')
)
BEGIN
    DROP INDEX uk_technicians_technician_id ON dbo.technicians;
END;

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'uk_technicians_badge_number'
      AND object_id = OBJECT_ID(N'dbo.technicians')
)
BEGIN
    DROP INDEX uk_technicians_badge_number ON dbo.technicians;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'uk_technicians_identifier'
      AND object_id = OBJECT_ID(N'dbo.technicians')
)
BEGIN
    CREATE UNIQUE INDEX uk_technicians_identifier ON dbo.technicians (technician_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'uk_technicians_badge'
      AND object_id = OBJECT_ID(N'dbo.technicians')
)
BEGIN
    CREATE UNIQUE INDEX uk_technicians_badge ON dbo.technicians (badge_number);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'uk_technicians_email'
      AND object_id = OBJECT_ID(N'dbo.technicians')
)
BEGIN
    CREATE UNIQUE INDEX uk_technicians_email
        ON dbo.technicians (email);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'idx_technicians_status'
      AND object_id = OBJECT_ID(N'dbo.technicians')
)
BEGIN
    CREATE INDEX idx_technicians_status ON dbo.technicians (status);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'idx_technicians_badge'
      AND object_id = OBJECT_ID(N'dbo.technicians')
)
BEGIN
    CREATE INDEX idx_technicians_badge ON dbo.technicians (badge_number);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'idx_technicians_identifier'
      AND object_id = OBJECT_ID(N'dbo.technicians')
)
BEGIN
    CREATE INDEX idx_technicians_identifier ON dbo.technicians (technician_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'uk_technician_teams_team_name'
      AND object_id = OBJECT_ID(N'dbo.technician_teams')
)
BEGIN
    CREATE UNIQUE INDEX uk_technician_teams_team_name ON dbo.technician_teams (team_name);
END;

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'uq_technician_team_members_team_technician'
      AND object_id = OBJECT_ID(N'dbo.technician_team_members')
)
BEGIN
    DROP INDEX uq_technician_team_members_team_technician ON dbo.technician_team_members;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'uk_technician_team_members_team_technician'
      AND object_id = OBJECT_ID(N'dbo.technician_team_members')
)
BEGIN
    CREATE UNIQUE INDEX uk_technician_team_members_team_technician
        ON dbo.technician_team_members (team_id, technician_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'idx_technician_team_members_team_id'
      AND object_id = OBJECT_ID(N'dbo.technician_team_members')
)
BEGIN
    CREATE INDEX idx_technician_team_members_team_id ON dbo.technician_team_members (team_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'idx_technician_team_members_technician_id'
      AND object_id = OBJECT_ID(N'dbo.technician_team_members')
)
BEGIN
    CREATE INDEX idx_technician_team_members_technician_id ON dbo.technician_team_members (technician_id);
END;

IF OBJECT_ID(N'dbo.fk_technician_team_members_team', N'F') IS NULL
BEGIN
    ALTER TABLE dbo.technician_team_members
        ADD CONSTRAINT fk_technician_team_members_team
        FOREIGN KEY (team_id) REFERENCES dbo.technician_teams (id) ON DELETE CASCADE;
END;

IF OBJECT_ID(N'dbo.fk_technician_team_members_technician', N'F') IS NULL
BEGIN
    ALTER TABLE dbo.technician_team_members
        ADD CONSTRAINT fk_technician_team_members_technician
        FOREIGN KEY (technician_id) REFERENCES dbo.technicians (id);
END;
