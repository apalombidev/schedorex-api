SCHEDOREX

A self-hosted booking engine for independent professionals.

Schedorex is designed to let a single professional (a consultant, a hairdresser, a physiotherapist, a tutor...) publish their available time slots and let clients book appointments online, with no accounts, no subscriptions to third-party platforms, and no complex server to manage. The goal is for the professional to be able to install Schedorex on their own infrastructure and keep control of their own data.

Project status (v12): the backend (Phase 1) is complete, tested end-to-end and internationalised (IT/EN). Frontend, rate limiting, Stripe payments and Docker packaging are planned: see the Roadmap.

⚠️ Work in progress. Schedorex is a personal project still under development: it is not a commercial product, is not officially distributed or supported, and is not intended for production use. Where this document mentions self-hosted installation, releases or licensing, it describes the project's goal, not something already available.

TABLE OF CONTENTS:
- What is Schedorex
- Features
- Tech stack
- How it works (architecture)
- Quick start
- Configuration
- API reference
- Data model
- Security
- Internationalisation (IT/EN)
- Tests
- Project structure
- Design decisions and known limitations
- Roadmap
- Privacy and GDPR
- License

WHAT IS SCHEDOREX
Picture a professional's diary:

The professional configures their service: name, appointment length (e.g. 30 minutes), price, time zone, language.
They generate availability in bulk: "from the 10th to the 30th of November, Monday to Friday, 9 to 5, lunch break 1 to 2 pm". The system automatically creates all the slots (the bookable time windows).
The client picks a free slot and books, providing only a name and an email. No registration.
The client receives a personal link/token they can use to cancel the booking themselves, up to a time limit set by the professional (e.g. until 24 hours before).
The professional stays in control from a login-protected panel: they can block slots (holidays, breaks), see what is booked, cancel any booking, and get a warning when the calendar is about to run out.

One point we are particularly careful about: two people can never book the same time, even if they click at the very same instant. The guarantee is not left to application code but enforced directly by the database (see Data model).


FEATURES
Client side (public, no login)
Viewing the offered service (name, duration, price, currency, language).
List of free slots within a date range.
Booking with name, email and optional notes.
Self-service cancellation via a personal token, with a configurable time threshold.
Clear error messages translated into the instance's language (IT or EN).
Professional side (admin, with login)
Service setup and update.
Bulk slot generation (date range, active weekdays, hours, optional lunch break), with idempotent regeneration: slots that already exist are skipped without errors.
Slot overview with the status of the associated booking.
Manual blocking and unblocking of individual slots.
Cancellation of any booking, with no time limit (override).
Dashboard summary with a "less than 7 days of future availability" warning.
Cross-cutting
Bilingual API interface (Italian / English).
Admin authentication via JWT in an httpOnly cookie, CSRF protection.
Versioned database migrations (Flyway).
Automated end-to-end test suite against a real database (Testcontainers).


TECH STACK
Component	Technology
Language	Java 21
Framework	Spring Boot 4.1.0 (Spring MVC, Spring Data JPA, Spring Security 7, Bean Validation)
Database	PostgreSQL 16
Migrations	Flyway
Data access	Hibernate / JPA (ddl-auto: validate, the schema is managed by Flyway only)
Authentication	JWT (JJWT 0.13.0) in an httpOnly cookie
JSON	Jackson 3.x (tools.jackson.* namespace)
Build	Maven (Maven Wrapper included)
Boilerplate	Lombok
Testing	JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers

Note for those who know Spring Boot 3.x: Spring Boot 4.1 has further modularised many parts of the framework. Several packages and dependencies have changed (Jackson 3, relocated test classes, TestRestTemplate in a separate module...). The project's choices are already aligned, but keep this in mind if you extend the code starting from Boot 3.x examples.


HOW SCHEDOREX WORKS (architecture)

The backend is organised by feature, not by technical layer. Each area has its own folder with entities, repository, service, controller and DTOs.

        Client (browser)                    Professional (browser)
              │                                        │
              ▼                                        ▼
      /api/public/**                            /api/admin/**
      (no login)                          (JWT login + CSRF required)
              │                                        │
              └───────────────┬────────────────────────┘
                              ▼
                  REST controllers  ── receive/validate requests (@Valid)
                              │
                              ▼
                  Service layer     ── business logic
                              │          (slot generation, booking and
                              │           cancellation rules)
                              ▼
                  Repositories (Spring Data JPA)
                              │
                              ▼
                       PostgreSQL   ── uniqueness constraints, Flyway migrations

Principles followed:

Database entities never leave the API. Every response goes through a DTO (data transfer object), so no sensitive field (e.g. the password hash) can leak by mistake.
The cancellation token is exposed only once, in the booking-creation response. No other endpoint returns it.
Errors are handled in a single place (GlobalExceptionHandler): controllers contain no try/catch, and the client never receives a stack trace.
Separate paths for public and admin (/api/public/** and /api/admin/**), so security rules apply by prefix.


QUICK START
Prerequisites
Java 21
Docker (for PostgreSQL in development and for tests)
Git

You don't need to install Maven: the project includes the Maven Wrapper (mvnw / mvnw.cmd).

1. Clone the repository
bash
git clone <YOUR-REPOSITORY-URL>
cd schedorex-api
2. Start PostgreSQL
bash
docker run --name scheduling-engine-db \
  -e POSTGRES_DB=scheduling_engine \
  -e POSTGRES_USER=scheduling_user \
  -e POSTGRES_PASSWORD=changeme_local_only \
  -p 5432:5432 \
  -d postgres:16

These credentials match the default development values and must be changed in any real environment.

3. Set the JWT secret

The secret used to sign tokens has no default value (deliberately, for security): without it the application won't start. It must be at least 32 characters long.

bash
# Linux / macOS / Git Bash
export JWT_SECRET="a-random-string-at-least-32-characters-long"

# Windows PowerShell
$env:JWT_SECRET = "a-random-string-at-least-32-characters-long"
4. Start the application
bash
./mvnw spring-boot:run        # Linux / macOS / Git Bash
mvnw.cmd spring-boot:run      # Windows (cmd / PowerShell)

On first start Flyway automatically creates all tables. The API responds on http://localhost:8080.

5. Create the administrator user

By design there is no public registration: the single admin of an instance is inserted directly into the database. First generate a bcrypt hash of the password you want to use (with any bcrypt generator, or from code with BCryptPasswordEncoder), then:

sql
INSERT INTO admin_users (id, email, password_hash, created_at)
VALUES (gen_random_uuid(), 'you@example.com', '<BCRYPT_HASH>', now());

You can run it with docker exec -it scheduling-engine-db psql -U scheduling_user -d scheduling_engine.

6. Try the API
bash
curl -i -X POST http://localhost:8080/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"your-password"}'

The response contains two cookies: jwt (session) and XSRF-TOKEN (CSRF protection). See Security for how to use them.


CONFIGURATION
Configuration is done through environment variables, with default values only for local development.

Variable	Default	Description
DB_HOST	localhost	Database host
DB_PORT	5432	Database port
DB_NAME	scheduling_engine	Database name
DB_USER	scheduling_user	Database user
DB_PASSWORD	changeme_local_only	Database password (must be changed)
JWT_SECRET	none, required	JWT signing secret, minimum 32 characters
app.jwt.expiration-minutes	60	Admin session length, in minutes
app.jwt.cookie-secure	false	If true the cookie travels over HTTPS only. Set to true in production

If startup fails with an error about app.jwt.secret, the JWT_SECRET variable is not visible to the process (in Eclipse it must also be set in the Run Configuration).


API REFERENCE
All responses are JSON. Timestamps are in ISO-8601 UTC format (e.g. 2026-11-10T09:00:00Z).

Public endpoints: /api/public/** (no authentication)
Method	Path	Description
GET	/api/public/service	Service details (name, duration, price, currency, language, Stripe enabled)
GET	/api/public/availability?from=...&to=...	Free slots in the range (excludes blocked and already booked ones)
POST	/api/public/bookings	Creates a booking. The only endpoint that returns the cancellation token
GET	/api/public/bookings/{token}	Booking details via token
POST	/api/public/bookings/{token}/cancel	Cancels the booking, if within the time threshold

Booking example:

bash
curl -X POST http://localhost:8080/api/public/bookings \
  -H "Content-Type: application/json" \
  -d '{
        "availabilitySlotId": "<SLOT-UUID>",
        "clientName": "Mario Rossi",
        "clientEmail": "mario@example.com",
        "clientNotes": "First visit"
      }'
Admin endpoints: /api/admin/** (login required)
Method	Path	Description
POST	/api/admin/auth/login	Login: sets the jwt and XSRF-TOKEN cookies (requires neither authentication nor CSRF)
POST	/api/admin/auth/logout	Logout: invalidates the cookie on the client side, responds 204
GET	/api/admin/service	Reads the service configuration
PUT	/api/admin/service	Creates or updates the service (singleton, idempotent)
POST	/api/admin/availability/generate	Generates slots in bulk; returns created and skipped
GET	/api/admin/availability?from=...&to=...	Slot list with blocked status and booking status
POST	/api/admin/availability/{slotId}/block	Blocks a slot
POST	/api/admin/availability/{slotId}/unblock	Unblocks a slot
POST	/api/admin/bookings/{bookingId}/cancel	Cancels a booking with no time limit
GET	/api/admin/dashboard/summary	"Less than 7 days of future slots" warning
Response codes
Code	When
200 / 201 / 204	Success (201 when a booking is created, 204 on logout)
400	Invalid data, cancellation past the threshold, inconsistent date range, malformed parameter
401	Not authenticated
403	Authenticated but not authorised, or missing/wrong CSRF token
404	Non-existent service, slot or cancellation token
409	Slot already booked or blocked
500	Unexpected error (generic message, details only in server logs)

Example validation error:

json
{
  "timestamp": "2026-11-10T09:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "...",
  "fieldErrors": [ ... ]
}


DATA MODEL
Four tables, managed by Flyway migrations (src/main/resources/db/migration).

services ──< availability_slots ──< bookings
   │                                   │
   └───────────────────────────────────┘
admin_users  (independent)
Table	Purpose
services	The offered service and its configuration: name, duration, price, currency (EUR/GBP), time zone, language (IT/EN), cancellation threshold, Stripe on/off. An instance manages one service only
availability_slots	The bookable time windows, pre-generated and stored in UTC. Unique per (service, start time)
bookings	Bookings: client data, status (PENDING, CONFIRMED, CANCELLED), cancellation token, Stripe fields
admin_users	The administrator user (email + bcrypt password hash)
How double bookings are prevented

On bookings there is a partial unique index: for each slot there can be at most one active booking (PENDING or CONFIRMED).

sql
CREATE UNIQUE INDEX uq_availability_slot_booking_active
    ON bookings (availability_slot_id)
    WHERE status IN ('PENDING', 'CONFIRMED');

This has two important effects:

if two requests arrive at the same instant, the database rejects one of them and the API responds 409 Conflict, with no possibility of a synchronisation error in the code;
a cancelled booking stays in the history but frees the slot, which can be booked again.
Migrations
Version	Content
V1	Initial schema (tables, indexes, pgcrypto extension)
V2	Restricts language to IT/EN and currency to EUR/GBP, with CHECK constraints
V3	Normalises bookings.status to uppercase, with a CHECK constraint
V4	Replaces the full uniqueness constraint with the partial unique index described above


SECURITY
Aspect	How it is handled
Who authenticates	Only the professional. Clients have no accounts: they identify themselves with the cancellation token
Passwords	Stored as bcrypt hashes, never in plain text
Session	Signed JWT (HS256) in an httpOnly cookie with SameSite=Strict and, in production, Secure. The token never appears in response bodies, so it is not readable by JavaScript
Roles	The token contains a roles claim; /api/admin/** requires the ADMIN role
CSRF	Active on data-modifying requests in /api/admin/**, using the double-submit cookie pattern, in addition to SameSite=Strict (defence in depth)
Stateless	No server-side session: the application keeps no state between requests
Errors	Never a stack trace to the client; 401 and 403 are structured JSON responses
Secrets	JWT_SECRET and DB credentials come from environment variables, no real secrets in the repository
How to call admin endpoints with CSRF

After login, every request that modifies data (POST, PUT, etc.) must send:

the jwt cookie (the browser handles it automatically);
the X-XSRF-TOKEN header with the same value as the XSRF-TOKEN cookie.
bash
curl -c cookies.txt -X POST http://localhost:8080/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"your-password"}'

XSRF=$(grep XSRF-TOKEN cookies.txt | awk '{print $7}')

curl -b cookies.txt -X POST http://localhost:8080/api/admin/availability/generate \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: $XSRF" \
  -d '{
        "startDate": "2026-11-10",
        "endDate": "2026-11-14",
        "activeDays": ["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY"],
        "openingTime": "09:00",
        "closingTime": "17:00",
        "lunchBreakStart": "13:00",
        "lunchBreakEnd": "14:00"
      }'

Admin GET endpoints only require the jwt cookie. Login, logout and /api/public/** are excluded from the CSRF check.

Declared limitation: logout deletes the cookie in the browser but, since JWTs are stateless, a token copied elsewhere remains valid until its natural expiry (60 minutes by default).


INTERNAZIONALISATION (IT/EN)
All messages sent to the client (domain errors, field validation errors, 401/403 responses) are translated into Italian or English.

The language does not depend on the client's browser (Accept-Language) but on the language setting of the service, chosen by the professional. An instance speaks a single language, consistent with the single-service model.
The texts live in src/main/resources/i18n/messages_it.properties and messages_en.properties.
Any new client-facing text (including future email messages) must go through the same MessageSource mechanism, with no text written directly in the code.


TESTS
bash
./mvnw test          # Linux / macOS / Git Bash
mvnw.cmd test        # Windows

Docker must be running: integration tests start a disposable PostgreSQL container via Testcontainers, so every run starts from a clean database identical to the production one.

Test	What it verifies
EndToEndFlowTest	The full lifecycle with security active: login → service setup → slot generation → public search → booking → double-booking attempt (409) → admin view → dashboard → cancellation via token → slot free again → rebooking → admin cancellation → logout
SecurityEnforcementTest	Admin access without a cookie → 401; modifying request without a CSRF token → 403
BookingServiceTest	The service rejects the second concurrent booking on the same slot (unit test with Mockito)
JwtServiceTest, SecurityIntegrationTest	Token generation/validation, roles, CSRF, logout
GlobalExceptionHandlerVerificationTest	Mapping of exceptions to HTTP responses, with no interference between handlers
@DataJpaTest repository tests	Free-slot queries and database constraints

The test environment uses the test profile, which applies a seed migration (for tests only, never in production) with a sample admin.

Docker note: with very recent Docker Engine versions, Testcontainers 1.20.x may negotiate an API version that is too old. The project solves this with src/test/resources/docker-java.properties (api.version=1.40).


PROJECT STRUCTURE
The code is organised by feature, under com.myname.schedorex:

src/main/java/com/myname/schedorex/
├── auth/           Admin login, JWT, authentication filter, admin user
├── availability/   Availability slots: generation, search, block/unblock
├── booking/        Bookings: creation, cancellation, statuses
├── service/        The professional's service and its configuration
├── dashboard/      Summary for the admin panel
├── exception/      Centralised error handling
├── i18n/           IT/EN translations and language resolution
└── config/         Security configuration (Spring Security)

src/main/resources/
├── db/migration/   Flyway migrations (V1–V4)
└── i18n/           Translation files

src/test/
├── java/           Unit and integration tests
└── resources/      Test profile, test admin seed, Docker configuration

Within each feature the convention is: entities and repository in the main folder, controller/ for REST endpoints, dto/ for request and response objects.


DESIGN DECISIONS AND KNOWN LIMITATIONS
Deliberate choices
One professional, one service, one calendar per instance. No multi-staff or multi-service: simpler to install, understand and maintain.
Pre-generated slots in the database, not computed on the fly: the anti-double-booking rule becomes a simple database constraint.
No client accounts. The client identifies themselves only with the token received at booking time.
Closed sets for language and currency (IT/EN, EUR/GBP): fewer possible misconfigurations.
Validated time zone as an IANA identifier (e.g. Europe/Rome): a wrong value is rejected immediately instead of causing errors later.
Times stored in UTC; conversion to the professional's or client's time zone is the interface's responsibility.
Known limitations
Hours that cross midnight (closing before opening) are not supported in slot generation.
Logout does not invalidate the token on the server side (see Security).
Rate limiting on public endpoints is not implemented yet: until it is, the application must not be exposed publicly.
There is no admin user seeding tool for development yet: it must be inserted by hand (see Quick start).
The language lookup happens on every request (one extra query): irrelevant with a one-row table, optimisable with a cache if ever needed.
There is no admin endpoint yet that lists bookings with client data (see Roadmap).


ROADMAP
Phase	Content	Status
1. Backend core	Entities, repositories, DTOs, services, errors, REST, JWT security, end-to-end tests	✅ Completed
i18n	IT/EN messages driven by the service's language	✅ Completed
2. Frontend	Interface in JavaScript + React + Vite: setup pages, admin panel, public booking, calendar, QR code, IT/EN i18n	⬜ Next
3. Integration	CORS, time zone conversion, CSV export, rate limiting on /api/public/**	⬜ Planned
4. Stripe	Payment at checkout, synchronous verification on return, periodic reconciliation job, enabling via configuration	⬜ Planned
5. Docker	Dockerfile, docker-compose, .env.example, v1.0 release	⬜ Planned
6. Documentation	User manual, T&C and GDPR, known-limitations document	⬜ Planned
To add to the backend before starting the frontend
An admin endpoint that lists bookings with client data (at least name and email, and whatever is needed to verify Stripe payments).
An admin "am I still authenticated?" endpoint so the frontend can tell whether the session is valid.
How Stripe payments will work (Phase 4)

Designed to work on a home or business installation without opening ports or configuring the network:

Synchronous verification: when the client returns from the payment page, the backend queries Stripe to confirm the session.
Periodic reconciliation: a job re-checks completed but not yet confirmed payments at regular intervals (useful if the client closes the browser before returning).
Real webhook (optional, advanced): for those who have already exposed the application publicly over HTTPS.

Bookings start as PENDING and become CONFIRMED once payment is verified; with Stripe disabled they start directly as CONFIRMED.


PRIVACY AND GPDR
The project is not distributed yet, so no third-party data is currently processed through Schedorex. The wording planned for when it is distributed is the following (draft, to be reviewed by a professional before any publication):

The data controller for the personal data collected through the application (name and email of the end clients who make bookings) is the person who installs and manages the self-hosted instance (the professional/administrator). The software developer/seller acts solely as a software licensor, has no access to the data processed by the instance and is not responsible for its processing.

Personal data handled by the application: the client's name, email and optional notes. They remain in the instance's database, under the control of whoever runs it.

LICENSE
At the moment no license has been chosen or published: all rights are reserved by the author. The code is visible in the repository for demonstration and study purposes, but no permission to use, copy, modify or redistribute it is granted without explicit authorisation.

If the project ever becomes a product, the hypothesis under consideration is a one-time proprietary license, with a possible white-label tier for agencies. This is only an idea: nothing is defined, and the license, terms and conditions will be drafted (ideally with legal review) before any distribution.
