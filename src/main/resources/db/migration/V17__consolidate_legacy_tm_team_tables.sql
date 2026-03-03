-- Consolidate legacy TM team tables into technician team tables and remove duplicates.

IF OBJECT_ID(N'dbo.tm_teams', N'U') IS NOT NULL
   AND OBJECT_ID(N'dbo.technician_teams', N'U') IS NOT NULL
BEGIN
    INSERT INTO dbo.technician_teams (team_name, team_description, status, start_date, end_date, notes)
    SELECT
        tm.team_name,
        CAST(tm.team_description AS NVARCHAR(MAX)),
        COALESCE(NULLIF(tm.status, ''), 'ACTIVE'),
        NULL,
        NULL,
        NULL
    FROM dbo.tm_teams tm
    LEFT JOIN dbo.technician_teams tt
        ON LOWER(tt.team_name) = LOWER(tm.team_name)
    WHERE tt.id IS NULL;
END;

IF OBJECT_ID(N'dbo.tm_team_members', N'U') IS NOT NULL
   AND OBJECT_ID(N'dbo.tm_teams', N'U') IS NOT NULL
   AND OBJECT_ID(N'dbo.technician_team_members', N'U') IS NOT NULL
   AND OBJECT_ID(N'dbo.technician_teams', N'U') IS NOT NULL
   AND OBJECT_ID(N'dbo.technicians', N'U') IS NOT NULL
BEGIN
    INSERT INTO dbo.technician_team_members (team_id, technician_id, team_leader)
    SELECT
        tt.id AS team_id,
        m.technician_id,
        m.team_leader
    FROM dbo.tm_team_members m
    INNER JOIN dbo.tm_teams tm
        ON tm.id = m.team_id
    INNER JOIN dbo.technician_teams tt
        ON LOWER(tt.team_name) = LOWER(tm.team_name)
    INNER JOIN dbo.technicians tech
        ON tech.id = m.technician_id
       AND tech.is_deleted = 0
    LEFT JOIN dbo.technician_team_members existing
        ON existing.team_id = tt.id
       AND existing.technician_id = m.technician_id
    WHERE existing.id IS NULL;
END;

IF OBJECT_ID(N'dbo.tm_team_members', N'U') IS NOT NULL
BEGIN
    DROP TABLE dbo.tm_team_members;
END;

IF OBJECT_ID(N'dbo.tm_teams', N'U') IS NOT NULL
BEGIN
    DROP TABLE dbo.tm_teams;
END;
