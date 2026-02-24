IF OBJECT_ID(N'dbo.tm_users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.tm_users (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        first_name NVARCHAR(50) NOT NULL,
        last_name NVARCHAR(50) NOT NULL,
        email NVARCHAR(150) NOT NULL,
        password_hash NVARCHAR(255) NOT NULL,
        role NVARCHAR(50) NOT NULL,
        active BIT NOT NULL CONSTRAINT df_tm_users_active DEFAULT 1,
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL,
        CONSTRAINT uk_tm_users_email UNIQUE (email)
    );
END;

IF OBJECT_ID(N'dbo.timesheets', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.timesheets (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        period_start_date DATE NOT NULL,
        period_end_date DATE NOT NULL,
        view_type NVARCHAR(50) NOT NULL,
        technician_id BIGINT NOT NULL
    );
END;

IF OBJECT_ID(N'dbo.timesheet_rows', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.timesheet_rows (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        timesheet_id BIGINT NOT NULL,
        date DATE NOT NULL,
        day_of_week NVARCHAR(20) NOT NULL,
        pay_code NVARCHAR(50) NOT NULL,
        hours DECIMAL(8,2) NOT NULL,
        daily_total DECIMAL(8,2) NOT NULL,
        accounting_unit NVARCHAR(100) NOT NULL,
        ferc NVARCHAR(100) NOT NULL,
        activity NVARCHAR(255) NULL,
        comment NVARCHAR(2000) NULL,
        is_deleted BIT NOT NULL CONSTRAINT df_timesheet_rows_is_deleted DEFAULT 0,
        CONSTRAINT fk_timesheet_rows_timesheet
            FOREIGN KEY (timesheet_id) REFERENCES dbo.timesheets(id) ON DELETE CASCADE
    );
END;
