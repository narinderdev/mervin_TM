-- Add work-order display name provided by frontend for expense read responses.

IF OBJECT_ID(N'dbo.expenses', N'U') IS NOT NULL
   AND COL_LENGTH('dbo.expenses', 'work_order_name') IS NULL
BEGIN
    ALTER TABLE dbo.expenses
        ADD work_order_name NVARCHAR(255) NULL;
END;
