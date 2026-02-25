-- Add status column to timesheets with default 'PENDING'
IF COL_LENGTH('dbo.timesheets', 'status') IS NULL
BEGIN
    ALTER TABLE dbo.timesheets
        ADD status NVARCHAR(20) NOT NULL CONSTRAINT df_timesheets_status DEFAULT 'PENDING';
END;
