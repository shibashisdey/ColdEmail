# ColdEmail Backend (V1 Async Upgrade)

Backend with JWT auth, CSV contact import, SMTP sending, Redis queue worker, and tracking endpoints.

## Product Overview
ColdEmail is a cold outreach campaign system for job seekers, freelancers, founders, and marketers who need to send personalized emails at scale and measure engagement.

Business goals:
- Send personalized outreach efficiently
- Track email opens
- Track resume/portfolio clicks
- Monitor campaign performance in real time
- Prevent duplicate sends
- Dispatch safely to reduce spam risk

Typical workflow:
1. Register and log in
2. Add SMTP account
3. Create campaign with subject + dynamic template
4. Upload recruiter/client CSV
5. Start campaign
6. Worker sends emails asynchronously via Redis queue
7. Track opens and resume clicks
8. Monitor progress from dashboard polling endpoint

Value proposition:
- Replaces one-by-one manual outreach
- Improves response rates via personalization
- Gives measurable feedback (sent, opened, clicked, failed)

## Features
- JWT register/login
- Separate hardcoded admin login with admin-only APIs
- Multi-tenant-capable schema
- Email account (SMTP) management
- Campaign creation with subject + dynamic template body
- CSV upload (`email`, optional `first_name,last_name,company`)
- Async sending with Redis job queue
- Optional campaign-level SMTP load-balanced dispatch across tenant accounts
- Status transitions and retry counters on `campaign_contacts`
- Open tracking and resume download tracking
- Campaign progress endpoint for frontend polling
- SMTP circuit breaker protection during provider outages
- Mustache-style placeholders supported in templates (e.g. `{{first_name}}`, `{{company}}`)

## Local Prerequisites
- Java 17
- PostgreSQL running locally
- Redis running locally

## Local Configuration
Edit [application.properties](/home/shibashis/Desktop/cold/ColdEmail/src/main/resources/application.properties) or export env vars:

- `DB_URL` (default `jdbc:postgresql://localhost:5432/coldmailer`)
- `DB_USERNAME` (default `postgres`)
- `DB_PASSWORD` (set to your local password, e.g. `root`)
- `REDIS_HOST` (default `localhost`)
- `REDIS_PORT` (default `6379`)
- `JWT_SECRET` (must be strong, 32+ chars)
- `APP_BASE_URL` (for tracking links)
- `APP_RESUME_URL` (resume redirect target)
- `app.cors.allowed-origins` (comma-separated frontend origins)

## Run Locally
```bash
export DB_URL='jdbc:postgresql://localhost:5432/coldmailer'
export DB_USERNAME='postgres'
export DB_PASSWORD='root'
export REDIS_HOST='localhost'
export REDIS_PORT='6379'
export JWT_SECRET='change-this-jwt-secret-to-a-long-random-string-32chars-min'
bash ./mvnw spring-boot:run
```

## Frontend Integration Quickstart
- Default allowed frontend origins are:
  - `http://localhost:5173` (Vite)
  - `http://localhost:3000` (CRA/Next dev)
- If your frontend runs on another host/port, update:
  - `app.cors.allowed-origins=http://your-frontend-origin`
- Always send JWT as:
  - `Authorization: Bearer <token>`
- Use `Content-Type: application/json` for JSON endpoints.

## API

Auth:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/admin/login`
  - hardcoded admin credentials:
    - email: `admin@coldemail.local`
    - password: `Admin@123`

Email accounts:
- `POST /api/email-accounts`
- `GET /api/email-accounts`

Campaigns:
- `POST /api/campaigns`
- `GET /api/campaigns`
- `POST /api/campaigns/{id}/contacts/upload-csv`
- `POST /api/campaigns/{id}/resume/upload`
- `GET /api/campaigns/{id}/contacts`
- `POST /api/campaigns/{id}/start`
- `GET /api/campaigns/{id}/stats`
- `GET /api/campaigns/{id}/progress`

Tracking:
- `GET /api/track/open/{trackingId}`
- `GET /api/track/resume/{trackingId}`

Admin (requires admin JWT):
- `GET /api/admin/hr-contacts?q=...` (cross-tenant HR contact listing/search)
- `GET /api/admin/failed-emails` (cross-tenant failed send audit)
- `GET /api/admin/users` (all users)
- `PATCH /api/admin/users/{id}/status` (activate/suspend user)
- `DELETE /api/admin/users/{id}` (soft-remove user: deactivate + mark removed)
- `GET /api/admin/email-accounts` (all SMTP accounts)
- `PATCH /api/admin/email-accounts/{id}/status` (activate/suspend SMTP account)
- `GET /api/admin/stats` (platform-wide analytics)
- `GET /api/admin/worker-health` (queue + worker health)
- `GET /api/admin/settings` (platform policy settings)
- `PATCH /api/admin/settings` (update policy settings)
  - allowed keys:
    - `MAX_CONTACTS_PER_CAMPAIGN`
    - `MAX_RETRIES`
    - `DISPATCH_MIN_DELAY_MS`
    - `DISPATCH_MAX_DELAY_MS`
    - `TRACK_OPEN_ENABLED`
    - `TRACK_RESUME_ENABLED`

## Worker Behavior
- Queue key: `email:jobs`
- Worker pops jobs and sends mail with random delay (default `8s-15s`)
- If campaign `loadBalancedDispatch=true`, sender account is selected round-robin from tenant email accounts
- Idempotency: skips already sent/opened/resume-downloaded contacts
- Retries: increments `retry_count` and requeues up to configured max retries
- Contact list endpoint (`GET /api/campaigns/{id}/contacts`) exposes failed recipients via `status`, `failureReason`, and `retryCount`.

## Circuit Breaker
- SMTP sending is protected with per-account circuit breakers
- Open breaker fails fast and marks jobs failed/retryable without blocking the worker
- Config keys:
  - `app.smtp.cb.failure-rate-threshold`
  - `app.smtp.cb.sliding-window-size`
  - `app.smtp.cb.minimum-number-of-calls`
  - `app.smtp.cb.wait-duration-open-seconds`

## CSV Safety Limits
- Max file size: `app.csv.max-bytes` (default `1048576` = 1 MB)
- Max rows per upload: `app.csv.max-rows` (default `10000`)

## Resume Upload Rules
- Endpoint: `POST /api/campaigns/{id}/resume/upload` (multipart field `file`)
- Allowed MIME types:
  - `application/pdf`
  - `application/msword`
  - `application/vnd.openxmlformats-officedocument.wordprocessingml.document`
- Allowed extensions: `.pdf`, `.doc`, `.docx`
- Max file size: `app.resume.max-bytes` (default `5242880` = 5 MB)
- Resume files are stored locally under `app.resume.storage-dir` (default `uploads/resumes`)
- Campaign must have a resume uploaded before `POST /api/campaigns/{id}/start`.

## Notes
- SMTP credentials are currently stored as plaintext in DB (planned hardening: encryption/KMS).
- For HTTPS tracking, set `APP_BASE_URL` to your HTTPS domain.
- Contact `firstName` and `company` are derived from email by default; generic mailbox/provider patterns map to `Generic`.
- Admin credentials are intentionally hardcoded for this version; move to secure secret storage for production.

## CORS Troubleshooting
- If browser shows blocked CORS request:
  1. Confirm frontend origin is listed in `app.cors.allowed-origins`.
  2. Restart backend after changing properties.
  3. Ensure requests go directly to backend base URL (`http://localhost:8080`).

## Validation
Run:
```bash
bash ./mvnw -q -DskipTests compile
bash ./mvnw test
```
