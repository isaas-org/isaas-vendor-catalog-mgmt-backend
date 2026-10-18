# isaas-vendor-catalog-mgmt-backend

Spring Boot API that lets vendors manage their listings in the iSaaS catalog (items, pricing plans, features, media). It reads the shared Postgres database whose schema lives in [`isaas-database`](https://github.com/isaas-org/isaas-database); this service never changes the schema.

## Stack

- Java 21, Spring Boot 4.1, Maven (wrapper included)
- Spring Web MVC, Data JPA, Validation, Actuator
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

`spring.jpa.hibernate.ddl-auto` is `none`: migrations belong to `isaas-database`.

## Tests

```bash
./mvnw verify
```

The context-load test needs a reachable Postgres at `DATABASE_URL`. CI (`.github/workflows/test.yml`) provides one as a `postgres:18` service container on every PR and every push to `feature/**`.

CI reads that database's connection details from three GitHub Actions secrets. They are set as organization secrets on `isaas-org` (Organization settings -> Secrets and variables -> Actions); a repository secret with the same name would override them. A `check-config` job fails with a clear message if any are missing.

| Secret | Value |
|--------|-------|
| `CI_DATABASE_URL` | `jdbc:postgresql://localhost:5432/isaas_test` (the database name must be `isaas_test`) |
| `CI_DATABASE_USERNAME` | any username, used to create the throwaway database |
| `CI_DATABASE_PASSWORD` | any password, used to create the throwaway database |

Secrets aren't passed to workflows run from forks, so PRs from forks fail `check-config`. To test a fork contribution, a maintainer pushes it to a branch in this repo.
