-- Introduce standalone expense approval workflow for technicians and managers.

IF OBJECT_ID(N'dbo.expenses', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.expenses (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        expense_date DATE NOT NULL,
        expense_code NVARCHAR(100) NOT NULL,
        description NVARCHAR(2000) NOT NULL,
        amount DECIMAL(12,2) NOT NULL,
        user_id BIGINT NOT NULL,
        work_order_id BIGINT NULL,
        status NVARCHAR(20) NOT NULL CONSTRAINT df_expenses_status DEFAULT 'DRAFT',
        submitted_at DATETIME2 NULL,
        approved_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL
    );
END;

IF OBJECT_ID(N'dbo.expenses', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'idx_expenses_user_id'
          AND object_id = OBJECT_ID(N'dbo.expenses')
   )
BEGIN
    CREATE INDEX idx_expenses_user_id
        ON dbo.expenses (user_id);
END;

IF OBJECT_ID(N'dbo.expenses', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'idx_expenses_status'
          AND object_id = OBJECT_ID(N'dbo.expenses')
   )
BEGIN
    CREATE INDEX idx_expenses_status
        ON dbo.expenses (status);
END;
