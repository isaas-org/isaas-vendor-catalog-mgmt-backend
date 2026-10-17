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

| Variable | Default | Notes |
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
