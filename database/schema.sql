-- ====================================================================
-- Database: reglogin
-- Host: localhost:3306
-- ====================================================================

CREATE DATABASE IF NOT EXISTS reglogin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE reglogin;

-- 1. User Table
-- Stores user registration profile. Passwords MUST be stored as BCrypt hashes.
DROP TABLE IF EXISTS jwt_token;
DROP TABLE IF EXISTS user;

CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    INDEX idx_user_name (name),
    INDEX idx_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. JWT Token Table
-- Tracks active JWT tokens for user sessions with expiration and audit timestamps.
-- uid references user.id with ON DELETE CASCADE.
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
