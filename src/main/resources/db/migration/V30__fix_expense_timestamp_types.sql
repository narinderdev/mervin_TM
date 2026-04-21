-- Align expense timestamp columns with Hibernate Instant expectation (datetimeoffset(7)).

IF OBJECT_ID(N'dbo.expenses', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.expenses', 'created_at') IS NOT NULL
    BEGIN
        UPDATE dbo.expenses
        SET created_at = COALESCE(created_at, SYSDATETIMEOFFSET())
        WHERE created_at IS NULL;

        ALTER TABLE dbo.expenses
            ALTER COLUMN created_at DATETIMEOFFSET(7) NOT NULL;
    END;

    IF COL_LENGTH('dbo.expenses', 'updated_at') IS NOT NULL
    BEGIN
        UPDATE dbo.expenses
        SET updated_at = COALESCE(updated_at, SYSDATETIMEOFFSET())
        WHERE updated_at IS NULL;

        ALTER TABLE dbo.expenses
            ALTER COLUMN updated_at DATETIMEOFFSET(7) NOT NULL;
    END;

    IF COL_LENGTH('dbo.expenses', 'submitted_at') IS NOT NULL
    BEGIN
        ALTER TABLE dbo.expenses
            ALTER COLUMN submitted_at DATETIMEOFFSET(7) NULL;
    END;

    IF COL_LENGTH('dbo.expenses', 'approved_at') IS NOT NULL
    BEGIN
        ALTER TABLE dbo.expenses
            ALTER COLUMN approved_at DATETIMEOFFSET(7) NULL;
    END;
END;
