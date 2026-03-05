-- Add pay-period controls to timesheets:
-- deadline_date = period_end_date + grace_days (backfill uses default 2)
-- lock_date = deadline_date + 1
-- admin_unlocked allows explicit admin override.

IF COL_LENGTH('dbo.timesheets', 'deadline_date') IS NULL
BEGIN
    ALTER TABLE dbo.timesheets ADD deadline_date DATE NULL;
END;

IF COL_LENGTH('dbo.timesheets', 'lock_date') IS NULL
BEGIN
    ALTER TABLE dbo.timesheets ADD lock_date DATE NULL;
END;

IF COL_LENGTH('dbo.timesheets', 'admin_unlocked') IS NULL
BEGIN
    ALTER TABLE dbo.timesheets
        ADD admin_unlocked BIT NOT NULL
            CONSTRAINT df_timesheets_admin_unlocked DEFAULT 0;
END;

-- Use dynamic SQL so SQL Server resolves newly-added columns at execution time.
EXEC sp_executesql N'
UPDATE dbo.timesheets
SET deadline_date = DATEADD(day, 2, period_end_date)
WHERE deadline_date IS NULL
  AND period_end_date IS NOT NULL;';

EXEC sp_executesql N'
UPDATE dbo.timesheets
SET lock_date = DATEADD(day, 1, deadline_date)
WHERE lock_date IS NULL
  AND deadline_date IS NOT NULL;';

-- Enforce non-null once backfilled.
IF COL_LENGTH('dbo.timesheets', 'deadline_date') IS NOT NULL
BEGIN
    EXEC sp_executesql N'ALTER TABLE dbo.timesheets ALTER COLUMN deadline_date DATE NOT NULL;';
END;

IF COL_LENGTH('dbo.timesheets', 'lock_date') IS NOT NULL
BEGIN
    EXEC sp_executesql N'ALTER TABLE dbo.timesheets ALTER COLUMN lock_date DATE NOT NULL;';
END;
