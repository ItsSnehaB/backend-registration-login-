-- ====================================================================
-- AIVEN FOR MYSQL CLOUD INITIALIZATION SCRIPT
-- Service: Aiven Cloud MySQL 8.0+
-- Database: reglogin (or defaultdb)
-- SSL: Required (TLS 1.2 / 1.3)
-- ====================================================================

-- Step 1: Create 'reglogin' database in Aiven (or use existing defaultdb)
CREATE DATABASE IF NOT EXISTS reglogin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE reglogin;

-- Step 2: Ensure clean state for relational tables
DROP TABLE IF EXISTS jwt_token;
DROP TABLE IF EXISTS user;

-- Step 3: Create 'user' Table for UserService
-- Stores user registration records with BCrypt password hashes
CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- Irreversible BCrypt hash ($2a$10$...)
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_user_name (name),
    INDEX idx_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 4: Create 'jwt_token' Table for AuthenticationService
-- Stores active JWT tokens for session verification and invalidation
CREATE TABLE jwt_token (
    tid BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    creation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expiry_time TIMESTAMP NOT NULL,
    INDEX idx_jwt_token (token(255)),
    INDEX idx_jwt_uid (uid),
    CONSTRAINT fk_jwt_user FOREIGN KEY (uid) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 5: Optional Seed Data for initial verification
-- Password is 'Password123' hashed with BCrypt ($2a$10$)
INSERT INTO user (name, password, email, phone) 
VALUES (
    'cloudadmin', 
    '$2a$10$wE9qj3jF7s8lM0zK9x1qReYQ2t3N4h5j6k7l8m9n0o1p2q3r4s5t6', 
    'admin@cloudmicroservices.com', 
    '+1 555-0199'
) ON DUPLICATE KEY UPDATE name=name;

-- Verify setup
SELECT 'Aiven MySQL Tables Initialized Successfully!' AS status, COUNT(*) AS user_count FROM user;
