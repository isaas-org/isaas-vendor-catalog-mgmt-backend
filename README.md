# isaas-vendor-catalog-mgmt-backend

Spring Boot API that lets vendors manage their listings in the iSaaS catalog (items, pricing plans, features, media). It reads the shared Postgres database whose schema lives in [`isaas-database`](https://github.com/isaas-org/isaas-database); this service never changes the schema.

## Stack

- Java 21, Spring Boot 4.1, Maven (wrapper included)
- Spring Web MVC, Data JPA, Validation, Actuator
- Spring Security as an OAuth2 resource server (Auth0)
- PostgreSQL 18 (Neon in production)

## Running locally

Start the database from `isaas-database` and seed it:

```bash
cd ../isaas-database
docker compose up -d
dbmate up
DATABASE_URL=postgres://postgres:postgres@localhost:55432/isaas_dev?sslmode=disable ./scripts/seed.sh
```

Then run this service:

```bash
./mvnw spring-boot:run
curl localhost:8082/actuator/health
```

## Configuration

The defaults below are the **local development configuration**: they point at the docker Postgres started from `isaas-database` (`postgres`/`postgres` on port 55432). They are not production settings. In any deployed environment (Neon), set `DATABASE_URL`, `DATABASE_USERNAME` and `DATABASE_PASSWORD` explicitly from that environment's secrets.

| Variable | Local default | Notes |
|----------|---------|-------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:55432/isaas_dev` | JDBC form. For Neon use the pooled host with `?sslmode=require` |
| `DATABASE_USERNAME` | `postgres` | |
| `DATABASE_PASSWORD` | `postgres` | |
| `PORT` | `8082` | The buyer backend uses 8081 so both can run side by side |
| `AUTH0_ISSUER_URI` | `https://isaas-dev.us.auth0.com/` | Auth0 tenant; the trailing slash must match the token's `iss` |
| `AUTH0_AUDIENCE` | `https://api.isaas/vendor` | The vendor API's identifier in Auth0 |

`spring.jpa.hibernate.ddl-auto` is `none`: migrations belong to `isaas-database`.

## Authentication and authorization

Every endpoint except `/actuator/health` and `/actuator/info` needs `Authorization: Bearer <token>`, where the token is either:

- an **Auth0 access token** for the vendor API, from the vendor frontend's login (tenant config in [`isaas-auth0-config`](https://github.com/isaas-org/isaas-auth0-config)), or
- a **personal access token** (`isaas_vpat_...`) that an agent app uses to act as one vendor user.

Auth0 only says who the caller is. Which vendor they belong to and their role come from their `vendors.vendor_users` row, looked up on every request. On first login the token's subject is bound to an invited or seeded row with the same email, only if the token marks the email verified. The vendor a request acts on is always the caller's own; `/vendor/...` never takes a vendor ID.

| Endpoint | Who |
|----------|-----|
| `GET /me` | Anyone signed in, or a token. Membership and staff role, or nulls for an unaffiliated user |
| `POST /vendors`, `GET /vendors?q=`, `POST /vendors/{id}/join-requests`, `GET /me/join-requests` | A signed-in user with a verified email who isn't a member yet: create a vendor (as its admin), or ask to join an active one |
| `GET /vendor`, `GET /vendor/members`, `GET /vendor/verification-documents` | Any active member |
| `POST /vendor/invitations`, `PATCH /vendor/members/{id}`, `GET /vendor/join-requests`, `POST /vendor/join-requests/{id}/approve` and `/reject`, `POST /vendor/verification-documents` | Vendor admins. A vendor always keeps one active admin |
| `POST`, `GET /me/access-tokens`, `DELETE /me/access-tokens/{id}` | A signed-in member (not a token). The token value is returned once; at most 90 days |
| `/staff/vendors...`, `/staff/verification-documents/{id}/review` | Active staff (`platform.staff_users`), signed in. Verify, then activate |

Errors are RFC 9457 problem details with a `code`, for example `NOT_A_MEMBER`, `ADMIN_REQUIRED`, `LAST_ADMIN`. Rules the database enforces (vendor activation, the join request gate, unique emails, slug format) come back as `409` with the database's message and the `constraint` name; they aren't re-checked in Java.

## Tests

```bash
./mvnw verify
```

The tests are integration tests against the real schema: they need a Postgres at `DATABASE_URL` with `isaas-database`'s migrations applied (`dbmate up`), and they never call Auth0 (logins are simulated with Spring Security's `jwt()` test support). Each test writes its own uniquely named rows, so no cleanup is needed. CI (`.github/workflows/test.yml`) runs a `postgres:18` service container, applies the migrations from `isaas-database`'s `main`, then runs `./mvnw verify` on every PR and every push to `feature/**`.

CI reads that database's connection details from three GitHub Actions secrets. They are set as organization secrets on `isaas-org` (Organization settings -> Secrets and variables -> Actions); a repository secret with the same name would override them. A `check-config` job fails with a clear message if any are missing.

| Secret | Value |
|--------|-------|
| `CI_DATABASE_URL` | `jdbc:postgresql://localhost:5432/isaas_test` (the database name must be `isaas_test`) |
| `CI_DATABASE_USERNAME` | any username, used to create the throwaway database |
| `CI_DATABASE_PASSWORD` | any password, used to create the throwaway database |

Secrets aren't passed to workflows run from forks, so PRs from forks fail `check-config`. To test a fork contribution, a maintainer pushes it to a branch in this repo.
