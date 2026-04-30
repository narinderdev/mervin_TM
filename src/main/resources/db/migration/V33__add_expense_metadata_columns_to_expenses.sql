-- Allow frontend to submit additional expense metadata.

IF OBJECT_ID(N'dbo.expenses', N'U') IS NOT NULL
   AND COL_LENGTH('dbo.expenses', 'work_order_type') IS NULL
BEGIN
    ALTER TABLE dbo.expenses
        ADD work_order_type NVARCHAR(100) NULL;
END;

IF OBJECT_ID(N'dbo.expenses', N'U') IS NOT NULL
   AND COL_LENGTH('dbo.expenses', 'department') IS NULL
BEGIN
    ALTER TABLE dbo.expenses
        ADD department NVARCHAR(100) NULL;
END;

IF OBJECT_ID(N'dbo.expenses', N'U') IS NOT NULL
   AND COL_LENGTH('dbo.expenses', 'account') IS NULL
BEGIN
    ALTER TABLE dbo.expenses
        ADD account NVARCHAR(100) NULL;
END;

IF OBJECT_ID(N'dbo.expenses', N'U') IS NOT NULL
   AND COL_LENGTH('dbo.expenses', 'expense_type') IS NULL
BEGIN
    ALTER TABLE dbo.expenses
        ADD expense_type NVARCHAR(100) NULL;
END;
