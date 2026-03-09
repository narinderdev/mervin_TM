-- Add entry-type support for time and expense rows on both final and draft timesheets.

IF COL_LENGTH('dbo.timesheet_rows', 'entry_type') IS NULL
BEGIN
    EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows ADD entry_type NVARCHAR(20) NULL;';
END;

IF COL_LENGTH('dbo.timesheet_rows', 'entry_type') IS NOT NULL
BEGIN
    EXEC sp_executesql N'
        UPDATE dbo.timesheet_rows
        SET entry_type = ''TIME''
        WHERE entry_type IS NULL;';
END;

IF COL_LENGTH('dbo.timesheet_rows', 'entry_type') IS NOT NULL
BEGIN
    EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows ALTER COLUMN entry_type NVARCHAR(20) NOT NULL;';
END;

IF COL_LENGTH('dbo.timesheet_rows', 'entry_type') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.default_constraints dc
        JOIN sys.columns c
          ON c.object_id = dc.parent_object_id
         AND c.column_id = dc.parent_column_id
        WHERE dc.parent_object_id = OBJECT_ID(N'dbo.timesheet_rows')
          AND c.name = N'entry_type'
   )
BEGIN
    EXEC sp_executesql N'
        ALTER TABLE dbo.timesheet_rows
            ADD CONSTRAINT df_timesheet_rows_entry_type DEFAULT ''TIME'' FOR entry_type;';
END;

IF COL_LENGTH('dbo.timesheet_rows', 'expense_code') IS NULL
BEGIN
    EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows ADD expense_code NVARCHAR(100) NULL;';
END;

IF COL_LENGTH('dbo.timesheet_rows', 'expense_amount') IS NULL
BEGIN
    EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows ADD expense_amount DECIMAL(12,2) NULL;';
END;

IF COL_LENGTH('dbo.timesheet_draft_rows', 'entry_type') IS NULL
BEGIN
    EXEC sp_executesql N'ALTER TABLE dbo.timesheet_draft_rows ADD entry_type NVARCHAR(20) NULL;';
END;

IF COL_LENGTH('dbo.timesheet_draft_rows', 'entry_type') IS NOT NULL
BEGIN
    EXEC sp_executesql N'
        UPDATE dbo.timesheet_draft_rows
        SET entry_type = ''TIME''
        WHERE entry_type IS NULL;';
END;

IF COL_LENGTH('dbo.timesheet_draft_rows', 'entry_type') IS NOT NULL
BEGIN
    EXEC sp_executesql N'ALTER TABLE dbo.timesheet_draft_rows ALTER COLUMN entry_type NVARCHAR(20) NOT NULL;';
END;

IF COL_LENGTH('dbo.timesheet_draft_rows', 'entry_type') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.default_constraints dc
        JOIN sys.columns c
          ON c.object_id = dc.parent_object_id
         AND c.column_id = dc.parent_column_id
        WHERE dc.parent_object_id = OBJECT_ID(N'dbo.timesheet_draft_rows')
          AND c.name = N'entry_type'
   )
BEGIN
    EXEC sp_executesql N'
        ALTER TABLE dbo.timesheet_draft_rows
            ADD CONSTRAINT df_timesheet_draft_rows_entry_type DEFAULT ''TIME'' FOR entry_type;';
END;

IF COL_LENGTH('dbo.timesheet_draft_rows', 'expense_code') IS NULL
BEGIN
    EXEC sp_executesql N'ALTER TABLE dbo.timesheet_draft_rows ADD expense_code NVARCHAR(100) NULL;';
END;

IF COL_LENGTH('dbo.timesheet_draft_rows', 'expense_amount') IS NULL
BEGIN
    EXEC sp_executesql N'ALTER TABLE dbo.timesheet_draft_rows ADD expense_amount DECIMAL(12,2) NULL;';
END;
