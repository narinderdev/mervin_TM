-- Replace entry_type with work_order_type and add company_number in timesheet rows.
-- Keep legacy expense_amount column for backward compatibility/history.

IF OBJECT_ID(N'dbo.timesheet_rows', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.timesheet_rows', 'entry_type') IS NOT NULL
       AND COL_LENGTH('dbo.timesheet_rows', 'work_order_type') IS NULL
    BEGIN
        EXEC sp_rename 'dbo.timesheet_rows.entry_type', 'work_order_type', 'COLUMN';
    END;

    IF COL_LENGTH('dbo.timesheet_rows', 'work_order_type') IS NULL
    BEGIN
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows ADD work_order_type NVARCHAR(100) NULL;';
    END;

    IF COL_LENGTH('dbo.timesheet_rows', 'work_order_type') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows ALTER COLUMN work_order_type NVARCHAR(100) NULL;';
    END;

    IF COL_LENGTH('dbo.timesheet_rows', 'company_number') IS NULL
    BEGIN
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows ADD company_number NVARCHAR(100) NULL;';

        IF COL_LENGTH('dbo.timesheet_rows', 'expense_amount') IS NOT NULL
        BEGIN
            EXEC sp_executesql N'
                UPDATE dbo.timesheet_rows
                SET company_number = CONVERT(NVARCHAR(100), expense_amount)
                WHERE company_number IS NULL
                  AND expense_amount IS NOT NULL;';
        END;
    END;
END;

IF OBJECT_ID(N'dbo.timesheet_draft_rows', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.timesheet_draft_rows', 'entry_type') IS NOT NULL
       AND COL_LENGTH('dbo.timesheet_draft_rows', 'work_order_type') IS NULL
    BEGIN
        EXEC sp_rename 'dbo.timesheet_draft_rows.entry_type', 'work_order_type', 'COLUMN';
    END;

    IF COL_LENGTH('dbo.timesheet_draft_rows', 'work_order_type') IS NULL
    BEGIN
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_draft_rows ADD work_order_type NVARCHAR(100) NULL;';
    END;

    IF COL_LENGTH('dbo.timesheet_draft_rows', 'work_order_type') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_draft_rows ALTER COLUMN work_order_type NVARCHAR(100) NULL;';
    END;

    IF COL_LENGTH('dbo.timesheet_draft_rows', 'company_number') IS NULL
    BEGIN
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_draft_rows ADD company_number NVARCHAR(100) NULL;';

        IF COL_LENGTH('dbo.timesheet_draft_rows', 'expense_amount') IS NOT NULL
        BEGIN
            EXEC sp_executesql N'
                UPDATE dbo.timesheet_draft_rows
                SET company_number = CONVERT(NVARCHAR(100), expense_amount)
                WHERE company_number IS NULL
                  AND expense_amount IS NOT NULL;';
        END;
    END;
END;
