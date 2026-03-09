-- Separate draft store for technician in-progress timesheet edits.
-- Draft rows should not appear in submitted timesheet APIs.

IF OBJECT_ID(N'dbo.timesheet_drafts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.timesheet_drafts (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        period_start_date DATE NOT NULL,
        period_end_date DATE NOT NULL,
        view_type NVARCHAR(50) NOT NULL,
        technician_id BIGINT NOT NULL,
        total_worked DECIMAL(8,2) NOT NULL,
        total_non_worked DECIMAL(8,2) NOT NULL,
        total_premium DECIMAL(8,2) NOT NULL,
        save_as_template BIT NOT NULL CONSTRAINT df_timesheet_drafts_save_as_template DEFAULT 0
    );
END;

IF OBJECT_ID(N'dbo.timesheet_drafts', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'uk_timesheet_drafts_technician_period'
          AND object_id = OBJECT_ID(N'dbo.timesheet_drafts')
   )
BEGIN
    CREATE UNIQUE INDEX uk_timesheet_drafts_technician_period
        ON dbo.timesheet_drafts (technician_id, period_start_date, period_end_date);
END;

IF OBJECT_ID(N'dbo.timesheet_draft_days', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.timesheet_draft_days (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        timesheet_draft_id BIGINT NOT NULL,
        date DATE NOT NULL,
        day_of_week NVARCHAR(20) NOT NULL,
        daily_total DECIMAL(8,2) NULL,
        CONSTRAINT fk_timesheet_draft_days_draft FOREIGN KEY (timesheet_draft_id)
            REFERENCES dbo.timesheet_drafts(id) ON DELETE CASCADE,
        CONSTRAINT uq_timesheet_draft_days_draft_date UNIQUE (timesheet_draft_id, date)
    );
END;

IF OBJECT_ID(N'dbo.timesheet_draft_rows', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.timesheet_draft_rows (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        timesheet_draft_day_id BIGINT NOT NULL,
        pay_code NVARCHAR(50) NOT NULL,
        hours DECIMAL(8,2) NOT NULL,
        accounting_unit NVARCHAR(100) NOT NULL,
        ferc NVARCHAR(100) NOT NULL,
        activity NVARCHAR(255) NULL,
        comment NVARCHAR(2000) NULL,
        is_deleted BIT NOT NULL CONSTRAINT df_timesheet_draft_rows_is_deleted DEFAULT 0,
        CONSTRAINT fk_timesheet_draft_rows_day FOREIGN KEY (timesheet_draft_day_id)
            REFERENCES dbo.timesheet_draft_days(id) ON DELETE CASCADE
    );
END;
