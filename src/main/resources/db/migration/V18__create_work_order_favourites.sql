-- Store technician-specific favourite work orders in TM DB.

IF OBJECT_ID(N'dbo.work_order_favourites', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.work_order_favourites (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        technician_id BIGINT NOT NULL,
        work_order_id BIGINT NOT NULL,
        created_at DATETIME2 NOT NULL
            CONSTRAINT df_work_order_favourites_created_at DEFAULT SYSUTCDATETIME()
    );
END;

IF OBJECT_ID(N'dbo.work_order_favourites', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.work_order_favourites', 'created_at') IS NULL
        ALTER TABLE dbo.work_order_favourites
            ADD created_at DATETIME2 NOT NULL
                CONSTRAINT df_work_order_favourites_created_at DEFAULT SYSUTCDATETIME();
END;

IF OBJECT_ID(N'dbo.work_order_favourites', N'U') IS NOT NULL
   AND OBJECT_ID(N'dbo.technicians', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.foreign_keys
        WHERE name = N'fk_work_order_favourites_technician'
          AND parent_object_id = OBJECT_ID(N'dbo.work_order_favourites')
   )
BEGIN
    ALTER TABLE dbo.work_order_favourites
        ADD CONSTRAINT fk_work_order_favourites_technician
        FOREIGN KEY (technician_id) REFERENCES dbo.technicians (id);
END;

IF OBJECT_ID(N'dbo.work_order_favourites', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'uk_work_order_favourites_technician_work_order'
          AND object_id = OBJECT_ID(N'dbo.work_order_favourites')
   )
BEGIN
    CREATE UNIQUE INDEX uk_work_order_favourites_technician_work_order
        ON dbo.work_order_favourites (technician_id, work_order_id);
END;

IF OBJECT_ID(N'dbo.work_order_favourites', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'idx_work_order_favourites_technician_created'
          AND object_id = OBJECT_ID(N'dbo.work_order_favourites')
   )
BEGIN
    CREATE INDEX idx_work_order_favourites_technician_created
        ON dbo.work_order_favourites (technician_id, created_at, id);
END;
