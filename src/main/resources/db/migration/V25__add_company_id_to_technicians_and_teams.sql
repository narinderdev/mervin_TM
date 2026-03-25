-- Scope technicians and technician teams by EAM company.

IF OBJECT_ID(N'dbo.technicians', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.technicians', 'company_id') IS NULL
        ALTER TABLE dbo.technicians ADD company_id BIGINT NULL;
END;

IF OBJECT_ID(N'dbo.technician_teams', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.technician_teams', 'company_id') IS NULL
        ALTER TABLE dbo.technician_teams ADD company_id BIGINT NULL;
END;

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'uk_technicians_identifier'
      AND object_id = OBJECT_ID(N'dbo.technicians')
)
BEGIN
    DROP INDEX uk_technicians_identifier ON dbo.technicians;
END;

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'uk_technicians_badge'
      AND object_id = OBJECT_ID(N'dbo.technicians')
)
BEGIN
    DROP INDEX uk_technicians_badge ON dbo.technicians;
END;

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'uk_technicians_email'
      AND object_id = OBJECT_ID(N'dbo.technicians')
)
BEGIN
    DROP INDEX uk_technicians_email ON dbo.technicians;
END;

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'uk_technician_teams_team_name'
      AND object_id = OBJECT_ID(N'dbo.technician_teams')
)
BEGIN
    DROP INDEX uk_technician_teams_team_name ON dbo.technician_teams;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'uk_technicians_company_identifier'
      AND object_id = OBJECT_ID(N'dbo.technicians')
)
BEGIN
    CREATE UNIQUE INDEX uk_technicians_company_identifier
        ON dbo.technicians (company_id, technician_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'uk_technicians_company_badge'
      AND object_id = OBJECT_ID(N'dbo.technicians')
)
BEGIN
    CREATE UNIQUE INDEX uk_technicians_company_badge
        ON dbo.technicians (company_id, badge_number);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'uk_technicians_company_email'
      AND object_id = OBJECT_ID(N'dbo.technicians')
)
BEGIN
    CREATE UNIQUE INDEX uk_technicians_company_email
        ON dbo.technicians (company_id, email)
        WHERE email IS NOT NULL;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'uk_technician_teams_company_team_name'
      AND object_id = OBJECT_ID(N'dbo.technician_teams')
)
BEGIN
    CREATE UNIQUE INDEX uk_technician_teams_company_team_name
        ON dbo.technician_teams (company_id, team_name);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'idx_technicians_company_id'
      AND object_id = OBJECT_ID(N'dbo.technicians')
)
BEGIN
    CREATE INDEX idx_technicians_company_id ON dbo.technicians (company_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'idx_technician_teams_company_id'
      AND object_id = OBJECT_ID(N'dbo.technician_teams')
)
BEGIN
    CREATE INDEX idx_technician_teams_company_id ON dbo.technician_teams (company_id);
END;
