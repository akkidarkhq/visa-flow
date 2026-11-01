# VisaFlow ✈️🌐

VisaFlow is a full-stack visa application and dossier management platform designed for clients, travellers, and visa operations teams. It streamlines individual and group travel visa processing, document uploads, and tracking.

---

## 📸 Application Screenshots

### 1. User Authentication & Login
Secure sign-in for clients to access their applications and dashboard with JWT bearer tokens.

![VisaFlow Login](screenshots/01-login-screen.png)

---

### 2. Client Dashboard & Profile Overview
Overview of active client account, registration metadata, application counts, and quick actions.

![VisaFlow Dashboard](screenshots/02-dashboard-profile.png)

---

### 3. Application Portfolio (Group & Individual)
Real-time tracking of group travel batches and individual visa cases with document statuses and fees.

![Application Portfolio](screenshots/03-dashboard-applications.png)

---

### 4. New Application Wizard: Travellers Roster (Step 3)
Dynamic multi-passenger roster supporting passport details, expiry validation, and saved traveller profiles.

![Travellers Roster](screenshots/04-application-travellers-roster.png)

---

### 5. Application Summary & Fee Schedule (Step 4)
Transparent breakdown of government/embassy fees and processing fees prior to submission.

![Application Review Summary](screenshots/05-application-review-summary.png)

---

### 6. Application Created Confirmation
Instant group tracking reference code and individual application reference generation.

![Application Created Confirmation](screenshots/06-application-success-created.png)

---

## 🛠 Tech Stack

- **Backend**: Spring Boot 3.4 / Java 17, Spring Security (JWT), Spring Data JPA, Hibernate, PostgreSQL, Bean Validation
- **API Documentation**: SpringDoc OpenAPI / Swagger UI (`/swagger-ui.html`)
- **Frontend**: Next.js 15, React, TypeScript, Tailwind CSS
- **Testing**: JUnit 5, Mockito, Spring Boot Starter Test (MockMvc)
