-- Rename legacy timesheet_rows columns to the new business terminology.
-- accounting_unit -> department
-- ferc -> account
-- activity -> project

IF OBJECT_ID(N'dbo.timesheet_rows', N'U') IS NOT NULL
BEGIN
    -- account (formerly ferc)
    IF COL_LENGTH('dbo.timesheet_rows', 'account') IS NULL
       AND COL_LENGTH('dbo.timesheet_rows', 'ferc') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheet_rows.ferc'', ''account'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheet_rows', 'account') IS NOT NULL
         AND COL_LENGTH('dbo.timesheet_rows', 'ferc') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheet_rows SET account = COALESCE(ferc, account);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows DROP COLUMN ferc;';
    END;

    -- project (formerly activity)
    IF COL_LENGTH('dbo.timesheet_rows', 'project') IS NULL
       AND COL_LENGTH('dbo.timesheet_rows', 'activity') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheet_rows.activity'', ''project'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheet_rows', 'project') IS NOT NULL
         AND COL_LENGTH('dbo.timesheet_rows', 'activity') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheet_rows SET project = COALESCE(activity, project);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows DROP COLUMN activity;';
    END;

    -- department (formerly accounting_unit)
    IF COL_LENGTH('dbo.timesheet_rows', 'department') IS NULL
       AND COL_LENGTH('dbo.timesheet_rows', 'accounting_unit') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheet_rows.accounting_unit'', ''department'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheet_rows', 'department') IS NOT NULL
         AND COL_LENGTH('dbo.timesheet_rows', 'accounting_unit') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheet_rows SET department = COALESCE(accounting_unit, department);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows DROP COLUMN accounting_unit;';
    END;
END;
