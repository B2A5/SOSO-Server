-- SOSO Server - MySQL Database Initialization
-- This script runs automatically when MySQL container starts for the first time

-- Set timezone to Seoul
SET time_zone = '+09:00';

-- Create additional database if needed
-- CREATE DATABASE IF NOT EXISTS soso_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Grant additional permissions if needed
-- GRANT ALL PRIVILEGES ON soso_test.* TO 'soso_user'@'%';

-- Set MySQL configurations for better performance
SET GLOBAL innodb_buffer_pool_size = 134217728; -- 128MB
SET GLOBAL max_connections = 100;
SET GLOBAL wait_timeout = 28800; -- 8 hours
SET GLOBAL interactive_timeout = 28800; -- 8 hours

-- Optimize for small server
SET GLOBAL table_open_cache = 64;
SET GLOBAL thread_cache_size = 8;
SET GLOBAL query_cache_size = 16777216; -- 16MB

-- Log slow queries for monitoring
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;

-- Ensure proper UTF-8 support
SET GLOBAL character_set_server = 'utf8mb4';
SET GLOBAL collation_server = 'utf8mb4_0900_ai_ci';

-- Flush privileges to ensure all changes take effect
FLUSH PRIVILEGES;

-- Create a health check user (optional)
CREATE USER IF NOT EXISTS 'healthcheck'@'%' IDENTIFIED BY 'healthcheck123';
GRANT SELECT ON *.* TO 'healthcheck'@'%';
FLUSH PRIVILEGES;

-- Display initialization info
SELECT 'SOSO MySQL Database initialized successfully!' AS MESSAGE;
SELECT @@version AS MySQL_Version;
SELECT @@time_zone AS TimeZone;