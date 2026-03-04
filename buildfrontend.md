# BuildFrontend Guide (Production-Ready)

This document is a full frontend implementation walkthrough for the ColdEmail backend.

Scope:
- app structure
- routing
- auth flow
- campaign workflow
- API contracts
- polling and status UX
- production hardening and deployment

## 1. Product UX Goal
Build a web app where a user can:
1. Register / login
2. Add SMTP account
3. Create campaign (subject + body template)
4. Upload contacts CSV
5. Upload resume file (.pdf/.doc/.docx)
6. Start campaign
7. Monitor progress (sent/opened/resume-downloaded/failed)
8. Inspect failed emails and reasons

## 2. Recommended Frontend Stack
- Framework: React + TypeScript
- Build tool: Vite
- Routing: React Router
- API client: Axios
- State: React Query + lightweight auth store (Zustand/Context)
- Forms: React Hook Form + Zod
- UI: Tailwind + component primitives (shadcn/ui or similar)

## 3. Environment and Base Config
Create `.env` in frontend:
- `VITE_API_BASE_URL=http://localhost:8080`

API client defaults:
- `baseURL = VITE_API_BASE_URL`
- attach JWT in `Authorization: Bearer <token>`
- global interceptor for `401` -> clear auth and redirect `/login`

Backend CORS requirement:
- Ensure backend property includes frontend origin:
  - `app.cors.allowed-origins=http://localhost:5173,http://localhost:3000`
- If frontend runs on a different port/domain, add it there and restart backend.

## 4. Route Map
Public routes:
- `/login`
- `/register`
- `/admin/login`

Protected routes:
- `/dashboard`
- `/email-accounts`
- `/campaigns`
- `/campaigns/new`
- `/campaigns/:id`
- `/campaigns/:id/contacts`
- `/campaigns/:id/progress`
- `/admin/hr-contacts`
- `/admin/failed-emails`

Recommended nested layout:
- `AppLayout` with sidebar/topbar + auth guard
- `CampaignLayout` for campaign sub-pages

## 5. Page-by-Page Build Plan

## 5.1 Auth Pages
### Register page
Fields:
- name
- email
- password

API:
- `POST /api/auth/register`

On success:
- store token + user metadata
- redirect `/dashboard`

### Login page
Fields:
- email
- password

API:
- `POST /api/auth/login`

On success:
- store token
- redirect `/dashboard`

## 5.2 Email Accounts Page
### Create SMTP account form
Fields:
- label
- smtpHost
- smtpPort
- smtpUsername
- smtpPassword
- fromEmail
- useTls (toggle)
- isDefault (toggle)

APIs:
- `POST /api/email-accounts`
- `GET /api/email-accounts`

UX:
- table/list of configured accounts
- highlight default account

## 5.3 Campaign Creation Page
### Create campaign form
Fields:
- name
- subject
- templateBody (multiline editor)
- emailAccountId (select from accounts)
- loadBalancedDispatch (toggle)

API:
- `POST /api/campaigns`

Template helper box (important):
- supported placeholders:
  - `{{first_name}}`
  - `{{last_name}}`
  - `{{company}}`
  - `{{email}}`

Trackable links:
- include button/helper to insert resume link placeholder guidance using `{{resumeTrackingUrl}}` concept in body text.

## 5.4 Campaign Detail Page
Display:
- campaign metadata
- current status
- processing details
- quick actions:
  - upload contacts CSV
  - upload resume file
  - start campaign

APIs:
- `GET /api/campaigns`
- `GET /api/campaigns/{id}/stats`
- `GET /api/campaigns/{id}/progress`

## 5.5 CSV Upload Section
Input:
- file picker (`.csv`)

API:
- `POST /api/campaigns/{id}/contacts/upload-csv` (multipart, `file`)

Show:
- createdCampaignContacts count from response

Validation hints to user:
- required header: `email`
- optional: `first_name,last_name,company`
- respects backend safety limits (size, rows)

## 5.6 Resume Upload Section
Input:
- file picker accept: `.pdf,.doc,.docx`

API:
- `POST /api/campaigns/{id}/resume/upload` (multipart, `file`)

Show:
- uploaded file name
- upload timestamp

Error handling:
- invalid MIME/extension -> show backend message clearly

## 5.7 Start Campaign Action
Button:
- `Start Campaign`

API:
- `POST /api/campaigns/{id}/start`

Behavior:
- disable button once RUNNING
- if resume missing, backend returns error -> show inline alert

## 5.8 Contacts + Failures View
API:
- `GET /api/campaigns/{id}/contacts`

Table columns:
- email
- firstName
- lastName
- company
- status
- sentAt
- openedAt
- resumeDownloadedAt
- retryCount
- failureReason

Add filters:
- status filter (`FAILED`, `SENT`, `OPENED`, etc.)
- search by email/company

## 5.9 Progress Dashboard Card
API:
- `GET /api/campaigns/{id}/progress`

Render:
- total
- pending
- sent
- failed
- opened
- resumeDownloaded
- campaignStatus
- queueDepth

Polling strategy:
- poll every 5-10 seconds while status is `RUNNING`
- stop polling when `COMPLETED` or `FAILED`
- allow manual refresh button

## 6. API Contract Summary

Auth:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/admin/login`

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

Tracking endpoints (public, used from email content):
- `GET /api/track/open/{trackingId}`
- `GET /api/track/resume/{trackingId}`

Admin:
- `GET /api/admin/hr-contacts?q=...`
- `GET /api/admin/failed-emails`
- `GET /api/admin/users`
- `PATCH /api/admin/users/{id}/status`
- `DELETE /api/admin/users/{id}`
- `GET /api/admin/email-accounts`
- `PATCH /api/admin/email-accounts/{id}/status`
- `GET /api/admin/stats`
- `GET /api/admin/worker-health`
- `GET /api/admin/settings`
- `PATCH /api/admin/settings`

Admin login credentials (hardcoded in backend):
- email: `admin@coldemail.local`
- password: `Admin@123`

## 7. Frontend State Design
Minimal stores:
- `authStore`
  - token
  - userId
  - email
  - tenantId
- `uiStore`
  - toasts
  - modal state

Server state (React Query):
- `emailAccounts`
- `campaigns`
- `campaignContacts(campaignId)`
- `campaignProgress(campaignId)`

Cache invalidation:
- after create/upload/start mutations, invalidate related campaign queries.

## 8. Error Handling Standards
Always show backend message when present:
- 400 -> form/validation error
- 401 -> session expired -> redirect login
- 403 -> forbidden
- 404 -> not found
- 409 -> state conflict (e.g., campaign already running)
- 413 -> file/row limit exceeded
- 5xx -> generic fallback message + retry option
- CORS/network error -> show \"Cannot reach API. Check backend URL/CORS settings.\"

## 9. Security Requirements (Frontend)
- Store JWT in memory-first strategy when possible; if localStorage, guard against XSS in code quality and CSP.
- Never log SMTP passwords in browser console.
- Do not expose raw stack traces to UI.
- Use HTTPS in production (`VITE_API_BASE_URL` over HTTPS).

## 10. Production Hardening Checklist
- build with `npm run build`
- environment-specific API URL
- route-level code splitting
- error boundaries
- loading skeletons for all async screens
- retry strategy for transient failures
- observability hooks (frontend logging/Sentry)
- accessibility checks (labels, keyboard navigation)
- mobile responsiveness for key workflows

## 11. Suggested Delivery Milestones
1. Auth + protected routing
2. Email account management
3. Campaign create + list
4. CSV + resume uploads
5. Start + progress polling
6. Contacts status table + failure diagnostics
7. UX polish + production hardening

## 12. Interview Demo Script (Short)
1. Login
2. Add SMTP account
3. Create campaign template with placeholders
4. Upload CSV and resume
5. Start campaign
6. Open progress page and show live counts
7. Open contacts table and show failed reason/retry count

This demonstrates full-stack product thinking: auth, async processing, observability, and user-facing campaign analytics.
