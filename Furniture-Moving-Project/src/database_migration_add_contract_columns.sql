-- Migration script to add provider_id and acknowledged_at columns to contracts table
-- Run this script on your database to add the missing columns

USE FurnitureTransportPlatform;
GO

-- Add provider_id column if it doesn't exist
IF COL_LENGTH('contracts', 'provider_id') IS NULL
BEGIN
    ALTER TABLE contracts ADD provider_id INT NULL;
    ALTER TABLE contracts ADD CONSTRAINT FK_contracts_provider 
        FOREIGN KEY (provider_id) REFERENCES providers(provider_id);
    CREATE INDEX IX_contracts_provider ON contracts(provider_id);
    PRINT 'Added provider_id column to contracts table';
END
ELSE
BEGIN
    PRINT 'Column provider_id already exists in contracts table';
END
GO

-- Add acknowledged_at column if it doesn't exist
IF COL_LENGTH('contracts', 'acknowledged_at') IS NULL
BEGIN
    ALTER TABLE contracts ADD acknowledged_at DATETIMEOFFSET NULL;
    PRINT 'Added acknowledged_at column to contracts table';
END
ELSE
BEGIN
    PRINT 'Column acknowledged_at already exists in contracts table';
END
GO

-- Also add contract_id to service_requests if it doesn't exist
IF COL_LENGTH('service_requests', 'contract_id') IS NULL
BEGIN
    ALTER TABLE service_requests ADD contract_id INT NULL;
    ALTER TABLE service_requests ADD CONSTRAINT FK_sr_contract 
        FOREIGN KEY (contract_id) REFERENCES contracts(contract_id);
    CREATE INDEX IX_sr_contract ON service_requests(contract_id);
    PRINT 'Added contract_id column to service_requests table';
END
ELSE
BEGIN
    PRINT 'Column contract_id already exists in service_requests table';
END
GO

-- Add payment_type to payments table if it doesn't exist
IF COL_LENGTH('payments', 'payment_type') IS NULL
BEGIN
    ALTER TABLE payments ADD payment_type VARCHAR(20) NULL;
    PRINT 'Added payment_type column to payments table';
END
ELSE
BEGIN
    PRINT 'Column payment_type already exists in payments table';
END
GO

PRINT 'Migration completed successfully!';
GO


