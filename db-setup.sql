-- =============================================================
-- STEP 1: MySQL Setup (for auth-service and doctor-service)
-- Run these commands in your MySQL client as root:
-- =============================================================

CREATE DATABASE IF NOT EXISTS a_auth_db;
CREATE DATABASE IF NOT EXISTS a_doctordb;

-- If you want dedicated users instead of root:
-- CREATE USER 'auth_user'@'localhost' IDENTIFIED BY 'auth_pass';
-- GRANT ALL PRIVILEGES ON a_auth_db.* TO 'auth_user'@'localhost';
-- CREATE USER 'doctor_user'@'localhost' IDENTIFIED BY 'doctor_pass';
-- GRANT ALL PRIVILEGES ON a_doctordb.* TO 'doctor_user'@'localhost';
-- FLUSH PRIVILEGES;


-- =============================================================
-- STEP 2: PostgreSQL Setup (for patient, appointment, payment)
-- Run these commands as the postgres superuser:
-- psql -U postgres
-- =============================================================

-- Patient service DB
CREATE DATABASE patientdb;
CREATE USER patient_user WITH PASSWORD 'patient_pass';
GRANT ALL PRIVILEGES ON DATABASE patientdb TO patient_user;
\c patientdb
GRANT ALL ON SCHEMA public TO patient_user;

-- Appointment service DB
CREATE DATABASE appointmentdb;
CREATE USER appointment_user WITH PASSWORD 'appointment_pass';
GRANT ALL PRIVILEGES ON DATABASE appointmentdb TO appointment_user;
\c appointmentdb
GRANT ALL ON SCHEMA public TO appointment_user;

-- Payment service DB
CREATE DATABASE paymentdb;
CREATE USER payment_user WITH PASSWORD 'payment_pass';
GRANT ALL PRIVILEGES ON DATABASE paymentdb TO payment_user;
\c paymentdb
GRANT ALL ON SCHEMA public TO payment_user;


-- =============================================================
-- STEP 3: Optional — Create appointments_view in patientdb
-- This is needed for patient-service's AppointmentView entity.
-- Since appointment data lives in a different DB (appointmentdb),
-- for local dev you can create a simple empty table as a stub:
-- =============================================================

\c patientdb

CREATE TABLE IF NOT EXISTS appointments_view (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    date DATE,
    start_time TIME,
    status VARCHAR(50)
);
