-- Add work_order_id to timesheet draft rows for idempotent work-order sync.

IF OBJECT_ID(N'dbo.timesheet_draft_rows', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.timesheet_draft_rows', 'work_order_id') IS NULL
    BEGIN
        ALTER TABLE dbo.timesheet_draft_rows
            ADD work_order_id BIGINT NULL;
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'idx_timesheet_draft_rows_work_order_id'
          AND object_id = OBJECT_ID(N'dbo.timesheet_draft_rows')
    )
    BEGIN
        CREATE INDEX idx_timesheet_draft_rows_work_order_id
            ON dbo.timesheet_draft_rows (work_order_id);
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'idx_timesheet_draft_rows_day_work_order'
          AND object_id = OBJECT_ID(N'dbo.timesheet_draft_rows')
    )
    BEGIN
        CREATE INDEX idx_timesheet_draft_rows_day_work_order
            ON dbo.timesheet_draft_rows (timesheet_draft_day_id, work_order_id);
    END;
END;
