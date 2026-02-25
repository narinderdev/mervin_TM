-- Remove camelCase duplicate columns for timesheets and timesheet_rows and enforce snake_case naming.

----------------------------------------------------------------------
-- timesheets
----------------------------------------------------------------------
IF OBJECT_ID(N'dbo.timesheets', N'U') IS NOT NULL
BEGIN
    -- period start date
    IF COL_LENGTH('dbo.timesheets', 'period_start_date') IS NULL
       AND COL_LENGTH('dbo.timesheets', 'periodStartDate') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheets.periodStartDate'', ''period_start_date'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheets', 'period_start_date') IS NOT NULL
         AND COL_LENGTH('dbo.timesheets', 'periodStartDate') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheets SET period_start_date = COALESCE(periodStartDate, period_start_date);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheets DROP COLUMN periodStartDate;';
    END;

    -- period end date
    IF COL_LENGTH('dbo.timesheets', 'period_end_date') IS NULL
       AND COL_LENGTH('dbo.timesheets', 'periodEndDate') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheets.periodEndDate'', ''period_end_date'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheets', 'period_end_date') IS NOT NULL
         AND COL_LENGTH('dbo.timesheets', 'periodEndDate') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheets SET period_end_date = COALESCE(periodEndDate, period_end_date);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheets DROP COLUMN periodEndDate;';
    END;

    -- view type
    IF COL_LENGTH('dbo.timesheets', 'view_type') IS NULL
       AND COL_LENGTH('dbo.timesheets', 'viewType') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheets.viewType'', ''view_type'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheets', 'view_type') IS NOT NULL
         AND COL_LENGTH('dbo.timesheets', 'viewType') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheets SET view_type = COALESCE(viewType, view_type);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheets DROP COLUMN viewType;';
    END;

    -- technician id
    IF COL_LENGTH('dbo.timesheets', 'technician_id') IS NULL
       AND COL_LENGTH('dbo.timesheets', 'technicianId') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheets.technicianId'', ''technician_id'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheets', 'technician_id') IS NOT NULL
         AND COL_LENGTH('dbo.timesheets', 'technicianId') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheets SET technician_id = COALESCE(technicianId, technician_id);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheets DROP COLUMN technicianId;';
    END;

    -- total worked
    IF COL_LENGTH('dbo.timesheets', 'total_worked') IS NULL
       AND COL_LENGTH('dbo.timesheets', 'totalWorked') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheets.totalWorked'', ''total_worked'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheets', 'total_worked') IS NOT NULL
         AND COL_LENGTH('dbo.timesheets', 'totalWorked') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheets SET total_worked = COALESCE(totalWorked, total_worked);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheets DROP COLUMN totalWorked;';
    END;

    -- total non worked
    IF COL_LENGTH('dbo.timesheets', 'total_non_worked') IS NULL
       AND COL_LENGTH('dbo.timesheets', 'totalNonWorked') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheets.totalNonWorked'', ''total_non_worked'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheets', 'total_non_worked') IS NOT NULL
         AND COL_LENGTH('dbo.timesheets', 'totalNonWorked') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheets SET total_non_worked = COALESCE(totalNonWorked, total_non_worked);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheets DROP COLUMN totalNonWorked;';
    END;

    -- total premium
    IF COL_LENGTH('dbo.timesheets', 'total_premium') IS NULL
       AND COL_LENGTH('dbo.timesheets', 'totalPremium') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheets.totalPremium'', ''total_premium'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheets', 'total_premium') IS NOT NULL
         AND COL_LENGTH('dbo.timesheets', 'totalPremium') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheets SET total_premium = COALESCE(totalPremium, total_premium);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheets DROP COLUMN totalPremium;';
    END;
END;

----------------------------------------------------------------------
-- timesheet_rows
----------------------------------------------------------------------
IF OBJECT_ID(N'dbo.timesheet_rows', N'U') IS NOT NULL
BEGIN
    -- day of week
    IF COL_LENGTH('dbo.timesheet_rows', 'day_of_week') IS NULL
       AND COL_LENGTH('dbo.timesheet_rows', 'dayOfWeek') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheet_rows.dayOfWeek'', ''day_of_week'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheet_rows', 'day_of_week') IS NOT NULL
         AND COL_LENGTH('dbo.timesheet_rows', 'dayOfWeek') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheet_rows SET day_of_week = COALESCE(dayOfWeek, day_of_week);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows DROP COLUMN dayOfWeek;';
    END;

    -- pay code
    IF COL_LENGTH('dbo.timesheet_rows', 'pay_code') IS NULL
       AND COL_LENGTH('dbo.timesheet_rows', 'payCode') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheet_rows.payCode'', ''pay_code'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheet_rows', 'pay_code') IS NOT NULL
         AND COL_LENGTH('dbo.timesheet_rows', 'payCode') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheet_rows SET pay_code = COALESCE(payCode, pay_code);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows DROP COLUMN payCode;';
    END;

    -- daily total
    IF COL_LENGTH('dbo.timesheet_rows', 'daily_total') IS NULL
       AND COL_LENGTH('dbo.timesheet_rows', 'dailyTotal') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheet_rows.dailyTotal'', ''daily_total'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheet_rows', 'daily_total') IS NOT NULL
         AND COL_LENGTH('dbo.timesheet_rows', 'dailyTotal') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheet_rows SET daily_total = COALESCE(dailyTotal, daily_total);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows DROP COLUMN dailyTotal;';
    END;

    -- accounting unit
    IF COL_LENGTH('dbo.timesheet_rows', 'accounting_unit') IS NULL
       AND COL_LENGTH('dbo.timesheet_rows', 'accountingUnit') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheet_rows.accountingUnit'', ''accounting_unit'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheet_rows', 'accounting_unit') IS NOT NULL
         AND COL_LENGTH('dbo.timesheet_rows', 'accountingUnit') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheet_rows SET accounting_unit = COALESCE(accountingUnit, accounting_unit);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows DROP COLUMN accountingUnit;';
    END;

    -- is deleted
    IF COL_LENGTH('dbo.timesheet_rows', 'is_deleted') IS NULL
       AND COL_LENGTH('dbo.timesheet_rows', 'isDeleted') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'EXEC sp_rename ''dbo.timesheet_rows.isDeleted'', ''is_deleted'', ''COLUMN'';';
    END
    ELSE IF COL_LENGTH('dbo.timesheet_rows', 'is_deleted') IS NOT NULL
         AND COL_LENGTH('dbo.timesheet_rows', 'isDeleted') IS NOT NULL
    BEGIN
        EXEC sp_executesql N'UPDATE dbo.timesheet_rows SET is_deleted = COALESCE(isDeleted, is_deleted);';
        EXEC sp_executesql N'ALTER TABLE dbo.timesheet_rows DROP COLUMN isDeleted;';
    END;
END;
