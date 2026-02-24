IF COL_LENGTH('dbo.timesheets', 'total_worked') IS NULL
BEGIN
    ALTER TABLE dbo.timesheets
    ADD total_worked DECIMAL(8,2) NOT NULL
        CONSTRAINT df_timesheets_total_worked DEFAULT 0;
END;

IF COL_LENGTH('dbo.timesheets', 'total_non_worked') IS NULL
BEGIN
    ALTER TABLE dbo.timesheets
    ADD total_non_worked DECIMAL(8,2) NOT NULL
        CONSTRAINT df_timesheets_total_non_worked DEFAULT 0;
END;

IF COL_LENGTH('dbo.timesheets', 'total_premium') IS NULL
BEGIN
    ALTER TABLE dbo.timesheets
    ADD total_premium DECIMAL(8,2) NOT NULL
        CONSTRAINT df_timesheets_total_premium DEFAULT 0;
END;
