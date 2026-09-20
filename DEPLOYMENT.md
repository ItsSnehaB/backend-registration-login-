# Cloud Deployment Guide: Vercel + Render + Aiven

This guide provides end-to-end instructions for deploying the 3-tier microservices suite to production:
- **Database**: Aiven for MySQL (Managed Cloud Database with TLS/SSL)
- **Backend Microservices**: Render (UserService & AuthenticationService Docker Web Services)
- **Frontend**: Vercel (Static Web Application with HttpOnly Cookie Proxy)

---

## Architecture Topology in Production

```
┌─────────────────────────────────────────────────────────────┐
│                 Vercel Production Edge                      │
│            https://your-frontend.vercel.app                 │
│         - Hosts FrontendRegLogin static assets              │
│         - Optional /api rewrites proxy to Render            │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
               │ (1) /api/reg                  │ (2) /api/login, /api/me
               ▼                               ▼
┌──────────────────────────────┐ ┌──────────────────────────────┐
│  Render: reglogin-user-svc   │ │  Render: reglogin-auth-svc   │
│  https://user-svc.onrender   │ │  https://auth-svc.onrender   │
│  - Docker Container          │ │  - Docker Container          │
│  - Port dynamically assigned │ │  - Port dynamically assigned │
│  - BCrypt Hashing (Cost 10)  │ │  - HMAC-SHA256 JWT           │
└──────────────┬───────────────┘ └──────────────┬───────────────┘
               │                                │
               │ TLS 1.3 / SSL Mode: REQUIRED   │
               └───────────────┬────────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 Aiven for MySQL Cloud                       │
│      mysql-xxxxx-myproject.aivencloud.com:12345             │
│   - Database: reglogin (or defaultdb)                       │
│   - Tables: user, jwt_token                                 │
└─────────────────────────────────────────────────────────────┘
```

---

## Step 1: Provision Aiven for MySQL

1. **Sign Up / Log In to Aiven**:
   - Go to [https://console.aiven.io/](https://console.aiven.io/).
2. **Create a New Service**:
   - Click **Create Service**.
   - Select **MySQL**.
   - Choose Cloud Provider (AWS, GCP, or Azure) and select your nearest region.
   - Select the Free Tier or Startup plan.
   - Service Name: `reglogin-mysql`.
   - Click **Create Service**.
3. **Get Connection Parameters**:
   - Once the service status is **Running**, locate the **Connection Information** tab.
   - Copy:
     - **Host**: e.g., `mysql-12a34b-myproject.aivencloud.com`
     - **Port**: e.g., `12345`
     - **User**: `avnadmin`
     - **Password**: Click to reveal password
     - **Database**: `defaultdb` or create `reglogin`
4. **Create the `reglogin` Database & Schema**:
   - Go to the **Databases & Tables** tab in Aiven, click **Add Database**, enter `reglogin`.
   - Use Aiven's Web Query Editor or connect via MySQL CLI / DBeaver:
     ```bash
     mysql -h <AIVEN_HOST> -P <AIVEN_PORT> -u avnadmin -p --ssl-mode=REQUIRED reglogin < projects/database/aiven_setup.sql
     ```
   - Alternatively, copy and paste the contents of `projects/database/aiven_setup.sql` into Aiven's query editor.
5. **Construct Your Spring Boot JDBC URL**:
   ```
   jdbc:mysql://<AIVEN_HOST>:<AIVEN_PORT>/reglogin?sslMode=REQUIRED
   ```

---

## Step 2: Deploy Backend Microservices to Render

### Option A: Using the Render Blueprint (`render.yaml`) - Recommended
1. Push this repository to GitHub or GitLab.
2. Log in to [https://dashboard.render.com/](https://dashboard.render.com/).
3. Click **New +** -> **Blueprint**.
4. Connect your GitHub repository.
5. Render detects `projects/render.yaml` and prepares two web services:
   - `reglogin-user-service`
   - `reglogin-auth-service`
6. Fill in the requested environment variables:
   - `SPRING_DATASOURCE_URL`: `jdbc:mysql://<AIVEN_HOST>:<AIVEN_PORT>/reglogin?sslMode=REQUIRED`
   - `SPRING_DATASOURCE_USERNAME`: `avnadmin`
   - `SPRING_DATASOURCE_PASSWORD`: `<your-aiven-password>`
7. Click **Apply**. Render will automatically build both Docker images and launch them!

### Option B: Deploy Services Individually via Render Dashboard
#### 1. UserService:
- Click **New +** -> **Web Service** -> Connect your repo.
- Name: `reglogin-user-service`
- Runtime: **Docker**
- Dockerfile Path: `./projects/UserService/Dockerfile`
- Docker Context: `./projects/UserService`
- Environment Variables:
  - `PORT`: `8081`
  - `SPRING_DATASOURCE_URL`: `jdbc:mysql://<AIVEN_HOST>:<AIVEN_PORT>/reglogin?sslMode=REQUIRED`
  - `SPRING_DATASOURCE_USERNAME`: `avnadmin`
  - `SPRING_DATASOURCE_PASSWORD`: `<AIVEN_PASSWORD>`
  - `CORS_ALLOWED_ORIGINS`: `https://your-app.vercel.app,http://localhost:3036`

#### 2. AuthenticationService:
- Click **New +** -> **Web Service** -> Connect your repo.
- Name: `reglogin-auth-service`
- Runtime: **Docker**
- Dockerfile Path: `./projects/AuthenticationService/Dockerfile`
- Docker Context: `./projects/AuthenticationService`
- Environment Variables:
  - `PORT`: `8082`
  - `SPRING_DATASOURCE_URL`: `jdbc:mysql://<AIVEN_HOST>:<AIVEN_PORT>/reglogin?sslMode=REQUIRED`
  - `SPRING_DATASOURCE_USERNAME`: `avnadmin`
  - `SPRING_DATASOURCE_PASSWORD`: `<AIVEN_PASSWORD>`
  - `JWT_SECRET`: `e.g. 64-char-hex-random-string-at-least-256-bits`
  - `JWT_COOKIE_SECURE`: `true`
  - `JWT_COOKIE_SAMESITE`: `None` (or `Lax` if using Vercel proxy)
  - `CORS_ALLOWED_ORIGINS`: `https://your-app.vercel.app,http://localhost:3036`

---

## Step 3: Deploy Frontend to Vercel

### 1. Configure Backend URLs in `vercel.json`:
In `projects/FrontendRegLogin/vercel.json`, update the rewrites to point to your live Render services:
```json
{
  "rewrites": [
    {
      "source": "/api/reg",
      "destination": "https://reglogin-user-service.onrender.com/api/reg"
    },
    {
      "source": "/api/login",
      "destination": "https://reglogin-auth-service.onrender.com/api/login"
    },
    {
      "source": "/api/logout",
      "destination": "https://reglogin-auth-service.onrender.com/api/logout"
    },
    {
      "source": "/api/me",
      "destination": "https://reglogin-auth-service.onrender.com/api/me"
    }
  ]
}
```
*Why this is the best practice:* Modern browsers (Safari, Chrome) block cross-domain third-party cookies by default. By having Vercel proxy `/api/*` to Render, the HttpOnly cookie is set on the **same domain** as your frontend (`your-app.vercel.app`), guaranteeing 100% cookie reliability across all browsers!

### 2. Deploy via Vercel Dashboard or CLI:
#### Via Vercel Web Dashboard:
1. Log in to [https://vercel.com/](https://vercel.com/).
2. Click **Add New...** -> **Project**.
3. Import your GitHub repository.
4. Set **Root Directory** to `projects/FrontendRegLogin`.
5. Framework Preset: **Other**.
6. Click **Deploy**.

#### Via Vercel CLI:
```bash
cd projects/FrontendRegLogin
npm install -g vercel
vercel deploy --prod
```

---

## Step 4: Verification & Live Smoke Testing

1. Open your Vercel deployment URL (`https://your-app.vercel.app`).
2. Click **Sign Up**, enter a new username, email, phone, and password. Click **Sign Up**.
   - Verify `POST /api/reg` returns HTTP 201 Created.
3. Automatically redirects to **Login**. Enter the credentials. Click **Login**.
   - Verify `POST /api/login` returns HTTP 200 OK with `Set-Cookie: jwt_token=...; HttpOnly; Secure`.
4. The dashboard displays:
   ```
   Welcome <username>
   ```
5. Check your Aiven MySQL database:
   ```sql
   SELECT id, name, email, phone FROM user;
   SELECT tid, uid, token, expiry_time FROM jwt_token;
   ```
   Notice that the password in `user` is salted and hashed via BCrypt, and `jwt_token` contains the active session!
