-- Sentry AI Database Initialization Script
-- This script runs automatically when the PostgreSQL container starts for the first time

-- Create extension for UUID generation (optional)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table will be created by Hibernate, but we can add indexes here
-- The actual table creation is handled by Spring Boot JPA

-- Create indexes for better query performance (run after Hibernate creates tables)
-- These will be skipped if tables don't exist yet

DO $$
BEGIN
    -- Check if alerts table exists before creating indexes
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'alerts') THEN
        -- Index for faster alert queries by camera
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_alerts_camera_id') THEN
            CREATE INDEX idx_alerts_camera_id ON alerts(camera_id);
        END IF;
        
        -- Index for faster queries on unacknowledged alerts
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_alerts_acknowledged') THEN
            CREATE INDEX idx_alerts_acknowledged ON alerts(acknowledged);
        END IF;
        
        -- Index for time-based queries
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_alerts_created_at') THEN
            CREATE INDEX idx_alerts_created_at ON alerts(created_at DESC);
        END IF;
    END IF;
    
    -- Check if cameras table exists
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'cameras') THEN
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_cameras_status') THEN
            CREATE INDEX idx_cameras_status ON cameras(status);
        END IF;
    END IF;
END $$;

-- Log that initialization is complete
DO $$
BEGIN
    RAISE NOTICE 'Sentry AI database initialization complete';
END $$;
