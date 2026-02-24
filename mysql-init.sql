-- ============================================================
-- DOCTOR APPOINTMENT SYSTEM - ALL-MYSQL SETUP
-- Run this against your MySQL root user once.
-- All services now use MySQL (was: auth/doctor=MySQL, others=PostgreSQL)
-- ============================================================

-- Auth Service DB
CREATE DATABASE IF NOT EXISTS a_auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Doctor Service DB
CREATE DATABASE IF NOT EXISTS a_doctordb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Patient Service DB  (was PostgreSQL — now MySQL)
CREATE DATABASE IF NOT EXISTS a_patientdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Appointment Service DB  (was PostgreSQL — now MySQL)
CREATE DATABASE IF NOT EXISTS a_appointmentdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Payment Service DB  (was PostgreSQL — now MySQL)
CREATE DATABASE IF NOT EXISTS a_paymentdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- Grant privileges to app user (optional — using root in dev)
-- Uncomment and modify if you want a dedicated app user:
-- ============================================================
-- CREATE USER IF NOT EXISTS 'appuser'@'%' IDENTIFIED BY 'apppassword';
-- GRANT ALL PRIVILEGES ON a_auth_db.* TO 'appuser'@'%';
-- GRANT ALL PRIVILEGES ON a_doctordb.* TO 'appuser'@'%';
-- GRANT ALL PRIVILEGES ON a_patientdb.* TO 'appuser'@'%';
-- GRANT ALL PRIVILEGES ON a_appointmentdb.* TO 'appuser'@'%';
-- GRANT ALL PRIVILEGES ON a_paymentdb.* TO 'appuser'@'%';
-- FLUSH PRIVILEGES;

-- ============================================================
-- Verify
-- ============================================================
SHOW DATABASES LIKE 'a_%';
