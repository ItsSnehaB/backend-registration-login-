# Registration & Login Microservices Suite

A complete, secure, runnable Registration & Login distributed system built with **Java 17**, **Spring Boot 3**, **MySQL**, **JWT (JJWT)**, and vanilla **HTML5 / CSS3 / JavaScript**.

---

## 1. Requirement Analysis & PRD

### Product Requirements Document (PRD)
- **Goal:** Provide a decoupled, secure authentication and user management system separating write-heavy user registration from high-throughput token authentication and verification.
- **Applications:**
  1. `FrontendRegLogin` (`http://localhost:3036`): Pure HTML/CSS/JS user interface. Never directly accesses MySQL and never stores JWTs in `localStorage` or `sessionStorage`.
  2. `UserService` (`http://localhost:8081`): Manages user registration, input validation, duplicate checks, and BCrypt password hashing.
  3. `AuthenticationService` (`http://localhost:8082`): Validates user credentials via BCrypt, issues JWT signed with HMAC-SHA256, stores tokens in MySQL `jwt_token`, and manages HttpOnly session cookies.

### System Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                 FrontendRegLogin (Port 3036)                 │
│                 Signup | Login | Home (Vanilla JS)           │
└──────────────┬───────────────────────────────┬──────────────┘
               │ POST /api/reg                 │ POST /api/login, /api/me, /api/logout
               ▼ (CORS: 3036)                  ▼ (CORS: 3036 + HttpOnly Cookie)
┌──────────────────────────────┐ ┌──────────────────────────────┐
│    UserService (Port 8081)   │ │ AuthenticationService (8082) │
│ - Validation (Jakarta)       │ │ - BCrypt Password Matcher    │
│ - BCrypt Hash (Salt = 10)    │ │ - JWT Generator & Signer     │
│ - Duplicate Name/Email Check │ │ - HttpOnly Cookie Dispatcher │
│ - Spring Data JPA Entity     │ │ - Token Revocation Engine    │
└──────────────┬───────────────┘ └──────────────┬───────────────┘
               │                                │
               ▼                                ▼
┌─────────────────────────────────────────────────────────────┐
│                 MySQL Database (Port 3306)                  │
│                     Database: `reglogin`                    │
│   - `user` (id, name, password_hash, email, phone)          │
│   - `jwt_token` (tid, uid, token, creation_time, expiry)    │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Database Design (MySQL)

Database name: `reglogin` on `localhost:3306`

```sql
CREATE DATABASE IF NOT EXISTS reglogin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE reglogin;

CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- Stored as BCrypt hash, never plain text
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    INDEX idx_user_name (name),
    INDEX idx_user_email (email)
) ENGINE=InnoDB;

CREATE TABLE jwt_token (
    tid BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    creation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expiry_time TIMESTAMP NOT NULL,
    INDEX idx_jwt_token (token(255)),
    INDEX idx_jwt_uid (uid),
    CONSTRAINT fk_jwt_user FOREIGN KEY (uid) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB;
```

---

## 3. API Design

### UserService (Port 8081)
- **POST `/api/reg`**
  - **Request Body:**
    ```json
    {
      "name": "john_doe",
      "email": "john@example.com",
      "phone": "+1 555-0199",
      "password": "Password123!",
      "confirmPassword": "Password123!"
    }
    ```
  - **Success Response (201 Created):**
    ```json
    {
      "id": 1,
      "name": "john_doe",
      "email": "john@example.com",
      "phone": "+1 555-0199",
      "message": "User registered successfully"
    }
    ```
  - **Error Response (400 Bad Request / 409 Conflict):**
    ```json
    {
      "status": 409,
      "error": "Conflict",
      "message": "Username 'john_doe' is already registered",
      "timestamp": "2026-09-17T20:30:00"
    }
    ```

### AuthenticationService (Port 8082)
- **POST `/api/login`**
  - **Request Body:**
    ```json
    {
      "name": "john_doe",
      "password": "Password123!"
    }
    ```
  - **Headers Sent in Response:**
    `Set-Cookie: jwt_token=<token>; Max-Age=86400; Path=/; HttpOnly; SameSite=Lax`
  - **Success Response (200 OK):**
    ```json
    {
      "id": 1,
      "name": "john_doe",
      "email": "john@example.com",
      "message": "Login successful"
    }
    ```
- **POST `/api/logout`**
  - Invalidates token in `jwt_token` database table.
  - Clears cookie: `Set-Cookie: jwt_token=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax`
- **GET `/api/me`**
  - Validates HttpOnly cookie and queries active token in MySQL.
  - Returns authenticated user profile.

---

## 4. Setup & Running in IntelliJ IDEA / Eclipse

### Prerequisites
- JDK 17 or higher
- MySQL Server 8.0+ running on `localhost:3306`
- Maven 3.8+ (or bundled Maven in IntelliJ/Eclipse)
- Node.js / npm (or Python / any HTTP static server for Frontend)

### Step 1: Initialize the MySQL Database
1. Open MySQL CLI or Workbench:
   ```bash
   mysql -u root -p < database/schema.sql
   ```
2. Verify tables:
   ```sql
   USE reglogin;
   SHOW TABLES; -- Should display `user` and `jwt_token`
   ```

### Step 2: Running in IntelliJ IDEA
1. Open IntelliJ IDEA -> `File` -> `Open...`
2. Select `UserService` directory -> Open as Maven Project.
   - Wait for Maven to download dependencies.
   - Open `src/main/resources/application.properties` and verify your MySQL password.
   - Right click `UserServiceApplication.java` -> `Run 'UserServiceApplication'`.
   - Console logs will show `Tomcat started on port 8081`.
3. Open a new IntelliJ Window -> `File` -> `Open...`
4. Select `AuthenticationService` directory -> Open as Maven Project.
   - Verify `application.properties` database settings.
   - Right click `AuthenticationServiceApplication.java` -> `Run 'AuthenticationServiceApplication'`.
   - Console logs will show `Tomcat started on port 8082`.

### Step 3: Running in Eclipse IDE
1. Open Eclipse IDE -> `File` -> `Import...` -> `Maven` -> `Existing Maven Projects`.
2. Browse to `UserService` and import.
3. Browse to `AuthenticationService` and import.
4. For each project: Right click -> `Run As` -> `Spring Boot App` (or `Java Application`).

### Step 4: Running FrontendRegLogin on Port 3036
In your terminal, navigate to `FrontendRegLogin`:
```bash
cd FrontendRegLogin
# Using npx serve:
npx serve -l 3036 .
# OR using python3:
python3 -m http.server 3036
```
Open your browser at `http://localhost:3036/login.html`.

---

## 5. End-to-End Verification & Testing

### Test 1: User Registration
```bash
curl -X POST http://localhost:8081/api/reg \
  -H "Content-Type: application/json" \
  -d '{
    "name": "alice_test",
    "email": "alice@test.com",
    "phone": "1234567890",
    "password": "Password123",
    "confirmPassword": "Password123"
  }'
```
Expected output: `201 Created` with `name`, `email`, `phone` and no password.

### Test 2: Duplicate Username & Password Mismatch Verification
```bash
# Duplicate Username:
curl -X POST http://localhost:8081/api/reg \
  -H "Content-Type: application/json" \
  -d '{
    "name": "alice_test",
    "email": "another@test.com",
    "phone": "9876543210",
    "password": "Password123",
    "confirmPassword": "Password123"
  }'
```
Expected output: `409 Conflict` with `Username 'alice_test' is already registered`.

### Test 3: User Login & Cookie Acquisition
```bash
curl -i -X POST http://localhost:8082/api/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "name": "alice_test",
    "password": "Password123"
  }'
```
Expected output:
- HTTP status: `200 OK`
- Header: `Set-Cookie: jwt_token=...; Path=/; HttpOnly; SameSite=Lax`

### Test 4: Access Protected Profile via Cookie
```bash
curl -i -X GET http://localhost:8082/api/me \
  -b cookies.txt
```
Expected output: `200 OK` with user details.

### Test 5: Logout & Invalidation
```bash
curl -i -X POST http://localhost:8082/api/logout \
  -b cookies.txt \
  -c cookies.txt
```
Expected output: `Set-Cookie: jwt_token=; Max-Age=0` and token record deleted from `jwt_token` database table.

---

## 6. Troubleshooting
- **CORS Errors in Browser (`Access to fetch at ... from origin 'http://localhost:3036' has been blocked by CORS policy`):**
  Ensure you are accessing the frontend from `http://localhost:3036`. Both Spring Boot microservices are configured with `.allowedOrigins("http://localhost:3036")` and `.allowCredentials(true)`.
- **Database Connection Refused (`CommunicationsException: Communications link failure`):**
  Ensure MySQL service is active on port `3306`. Check username/password in `application.properties`.
- **Cookies not saved in Browser:**
  When making cross-origin requests between `localhost:3036` and `localhost:8082`, `credentials: 'include'` must be present in the `fetch()` call (this is pre-configured in `login.js` and `home.js`).
