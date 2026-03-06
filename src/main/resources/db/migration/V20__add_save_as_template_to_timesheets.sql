-- Add template flag to timesheets for frontend autofill workflows.

IF COL_LENGTH('dbo.timesheets', 'save_as_template') IS NULL
BEGIN
    EXEC sp_executesql N'ALTER TABLE dbo.timesheets ADD save_as_template BIT NULL;';
END;

IF COL_LENGTH('dbo.timesheets', 'save_as_template') IS NOT NULL
BEGIN
    EXEC sp_executesql N'
        UPDATE dbo.timesheets
        SET save_as_template = 0
        WHERE save_as_template IS NULL;';
END;

IF COL_LENGTH('dbo.timesheets', 'save_as_template') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.default_constraints dc
        JOIN sys.columns c
          ON c.object_id = dc.parent_object_id
         AND c.column_id = dc.parent_column_id
        WHERE dc.parent_object_id = OBJECT_ID(N'dbo.timesheets')
          AND c.name = N'save_as_template'
   )
BEGIN
    EXEC sp_executesql N'
        ALTER TABLE dbo.timesheets
            ADD CONSTRAINT df_timesheets_save_as_template DEFAULT 0 FOR save_as_template;';
END;

IF COL_LENGTH('dbo.timesheets', 'save_as_template') IS NOT NULL
BEGIN
    EXEC sp_executesql N'ALTER TABLE dbo.timesheets ALTER COLUMN save_as_template BIT NOT NULL;';
END;
