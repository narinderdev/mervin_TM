-- Move expense workflow away from DRAFT and make PENDING the default status.

IF OBJECT_ID(N'dbo.expenses', N'U') IS NOT NULL
BEGIN
    UPDATE dbo.expenses
    SET status = N'PENDING',
        submitted_at = COALESCE(submitted_at, SYSDATETIMEOFFSET()),
        updated_at = SYSDATETIMEOFFSET()
    WHERE UPPER(LTRIM(RTRIM(status))) = N'DRAFT';

    DECLARE @statusDefaultConstraintName NVARCHAR(128);

    SELECT @statusDefaultConstraintName = dc.name
    FROM sys.default_constraints dc
    INNER JOIN sys.columns c
            ON c.default_object_id = dc.object_id
    WHERE dc.parent_object_id = OBJECT_ID(N'dbo.expenses')
      AND c.name = N'status';

    IF @statusDefaultConstraintName IS NOT NULL
    BEGIN
        DECLARE @dropConstraintSql NVARCHAR(400);
        SET @dropConstraintSql = N'ALTER TABLE dbo.expenses DROP CONSTRAINT [' + @statusDefaultConstraintName + N']';
        EXEC sp_executesql @dropConstraintSql;
    END;

    ALTER TABLE dbo.expenses
        ADD CONSTRAINT df_expenses_status DEFAULT N'PENDING' FOR status;
END;
