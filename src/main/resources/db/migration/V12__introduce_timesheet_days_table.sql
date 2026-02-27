-- Normalize timesheet structure: timesheets -> timesheet_days -> timesheet_rows.
-- 1) Create timesheet_days table.
-- 2) Drop legacy FK from timesheet_rows to timesheets to avoid cascade path conflicts.
-- 3) Add timesheet_day_id to timesheet_rows and backfill from existing date/day data.
-- 4) Drop redundant columns from timesheet_rows.

---------------------------------------------------------------------
-- 1. Create timesheet_days table (id, timesheet_id, date, day_of_week, daily_total)
---------------------------------------------------------------------
IF OBJECT_ID(N'dbo.timesheet_days', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.timesheet_days (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        timesheet_id BIGINT NOT NULL,
        date DATE NOT NULL,
        day_of_week NVARCHAR(20) NOT NULL,
        daily_total DECIMAL(8,2) NULL,
        CONSTRAINT fk_timesheet_days_timesheet FOREIGN KEY (timesheet_id)
            REFERENCES dbo.timesheets(id) ON DELETE CASCADE,
        CONSTRAINT uq_timesheet_days_timesheet_date UNIQUE (timesheet_id, date)
    );
END;
ELSE
BEGIN
    -- Ensure unique constraint exists
    IF NOT EXISTS (
        SELECT 1 FROM sys.objects o
        WHERE o.type = 'UQ' AND o.name = 'uq_timesheet_days_timesheet_date'
    )
    BEGIN
        ALTER TABLE dbo.timesheet_days
            ADD CONSTRAINT uq_timesheet_days_timesheet_date UNIQUE (timesheet_id, date);
    END;
END;

---------------------------------------------------------------------
-- 2. Add timesheet_day_id to timesheet_rows and backfill
---------------------------------------------------------------------
-- 2a. Drop legacy FK rows -> timesheets first to avoid multiple cascade paths
IF EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'fk_timesheet_rows_timesheet' AND parent_object_id = OBJECT_ID('dbo.timesheet_rows')
)
BEGIN
    ALTER TABLE dbo.timesheet_rows DROP CONSTRAINT fk_timesheet_rows_timesheet;
END;

-- 2b. Add new column for the day relationship
IF COL_LENGTH('dbo.timesheet_rows', 'timesheet_day_id') IS NULL
BEGIN
    ALTER TABLE dbo.timesheet_rows ADD timesheet_day_id BIGINT NULL;
END;

-- 2c. Insert missing day records based on existing row data
INSERT INTO dbo.timesheet_days (timesheet_id, date, day_of_week, daily_total)
SELECT DISTINCT r.timesheet_id, r.date, r.day_of_week, r.daily_total
FROM dbo.timesheet_rows r
LEFT JOIN dbo.timesheet_days d
    ON d.timesheet_id = r.timesheet_id AND d.date = r.date
WHERE d.id IS NULL;

-- 2d. Backfill timesheet_day_id on rows
UPDATE r
SET timesheet_day_id = d.id
FROM dbo.timesheet_rows r
JOIN dbo.timesheet_days d
    ON d.timesheet_id = r.timesheet_id AND d.date = r.date;

-- 2e. Make the new column NOT NULL
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.timesheet_rows') AND name = 'timesheet_day_id')
BEGIN
    ALTER TABLE dbo.timesheet_rows ALTER COLUMN timesheet_day_id BIGINT NOT NULL;
END;

-- 2f. Add FK from rows to days (cascade delete rows when day removed)
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'fk_timesheet_rows_day' AND parent_object_id = OBJECT_ID('dbo.timesheet_rows')
)
BEGIN
    ALTER TABLE dbo.timesheet_rows
        ADD CONSTRAINT fk_timesheet_rows_day FOREIGN KEY (timesheet_day_id)
            REFERENCES dbo.timesheet_days(id) ON DELETE CASCADE;
END;

---------------------------------------------------------------------
-- 3. Drop redundant columns/constraints from timesheet_rows now that data moved to timesheet_days
---------------------------------------------------------------------
-- Drop redundant columns after migration
IF COL_LENGTH('dbo.timesheet_rows', 'date') IS NOT NULL
BEGIN
    ALTER TABLE dbo.timesheet_rows DROP COLUMN date;
END;

IF COL_LENGTH('dbo.timesheet_rows', 'day_of_week') IS NOT NULL
BEGIN
    ALTER TABLE dbo.timesheet_rows DROP COLUMN day_of_week;
END;

IF COL_LENGTH('dbo.timesheet_rows', 'daily_total') IS NOT NULL
BEGIN
    ALTER TABLE dbo.timesheet_rows DROP COLUMN daily_total;
END;

IF COL_LENGTH('dbo.timesheet_rows', 'timesheet_id') IS NOT NULL
BEGIN
    ALTER TABLE dbo.timesheet_rows DROP COLUMN timesheet_id;
END;
