-- Expense rows can omit time-specific fields, so these must be nullable.

IF OBJECT_ID(N'dbo.timesheet_rows', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.timesheet_rows', 'pay_code') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows ALTER COLUMN pay_code NVARCHAR(50) NULL;';
    END;

    IF COL_LENGTH('dbo.timesheet_rows', 'hours') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows ALTER COLUMN hours DECIMAL(8,2) NULL;';
    END;
END;

IF OBJECT_ID(N'dbo.timesheet_draft_rows', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.timesheet_draft_rows', 'pay_code') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_draft_rows ALTER COLUMN pay_code NVARCHAR(50) NULL;';
    END;

    IF COL_LENGTH('dbo.timesheet_draft_rows', 'hours') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_draft_rows ALTER COLUMN hours DECIMAL(8,2) NULL;';
    END;
END;
