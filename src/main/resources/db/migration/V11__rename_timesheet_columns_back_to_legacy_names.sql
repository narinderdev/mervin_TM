-- Restore original business terminology for timesheet_rows columns.
-- department    -> accounting_unit
-- account       -> ferc
-- project       -> activity

IF OBJECT_ID(N'dbo.timesheet_rows', N'U') IS NOT NULL
BEGIN
    -- accounting_unit (formerly department)
    IF COL_LENGTH('dbo.timesheet_rows', 'accounting_unit') IS NULL
       AND COL_LENGTH('dbo.timesheet_rows', 'department') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheet_rows.department'', ''accounting_unit'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheet_rows', 'accounting_unit') IS NOT NULL
         AND COL_LENGTH('dbo.timesheet_rows', 'department') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheet_rows SET accounting_unit = COALESCE(department, accounting_unit);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows DROP COLUMN department;';
    END;

    -- ferc (formerly account)
    IF COL_LENGTH('dbo.timesheet_rows', 'ferc') IS NULL
       AND COL_LENGTH('dbo.timesheet_rows', 'account') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheet_rows.account'', ''ferc'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheet_rows', 'ferc') IS NOT NULL
         AND COL_LENGTH('dbo.timesheet_rows', 'account') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheet_rows SET ferc = COALESCE(account, ferc);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows DROP COLUMN account;';
    END;

    -- activity (formerly project)
    IF COL_LENGTH('dbo.timesheet_rows', 'activity') IS NULL
       AND COL_LENGTH('dbo.timesheet_rows', 'project') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheet_rows.project'', ''activity'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheet_rows', 'activity') IS NOT NULL
         AND COL_LENGTH('dbo.timesheet_rows', 'project') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheet_rows SET activity = COALESCE(project, activity);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows DROP COLUMN project;';
    END;
END;
