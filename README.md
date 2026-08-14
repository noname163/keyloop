# Keyloop Unified Document Viewer

Spring Boot service backed by PostgreSQL.

The Docker Compose setup:

- Builds the Spring Boot application
- Starts the PostgreSQL database
- Runs Flyway migrations at application startup
- Persists PostgreSQL data in a named Docker volume

---

## Run with Docker Compose

### 1. Create the deployment environment file

```bash
cp .env.example .env
```

### 2. Configure environment variables

Update `POSTGRES_PASSWORD` in the `.env` file.

If the Sales System API and Service System API do not run on ports `8081` and `8082` of the Docker host, update their URLs accordingly.

### 3. Build and start the application

```bash
docker compose up --build -d
```

### 4. Open Swagger UI

Swagger UI is available at:

`http://localhost:8080/swagger-ui/index.html`

If `APP_PORT` has been changed, use the configured port instead.

### 5. View application logs

```bash
docker compose logs -f application
```

### Stop the application

```bash
docker compose down
```

Database contents remain persisted in the `postgres-data` Docker volume.

To remove the containers **and all database data**, run:

```bash
docker compose down --volumes
```

All deployment settings are documented in `.env.example`.

Docker Compose supplies the application's:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

from the PostgreSQL configuration, so users do not need to manually construct a JDBC URL.

---

## Build Only the Application Image

Build the Docker image:

```bash
docker build -t keyloop-unified-document-viewer .
```

When running the image without Docker Compose, provide every environment variable referenced by:

```text
src/main/resources/application.properties
```

Example:

```bash
docker run --rm -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://database-host:5432/keyloop \
  -e DB_USERNAME=keyloop \
  -e DB_PASSWORD=secret \
  -e DB_MAX_POOL_SIZE=10 \
  -e DB_MIN_IDLE=2 \
  -e DB_CONNECTION_TIMEOUT_MS=30000 \
  -e SALES_SYSTEM_BASE_URL=http://sales-host:8081 \
  -e SERVICE_SYSTEM_BASE_URL=http://service-host:8082 \
  keyloop-unified-document-viewer
```

---

## Local Audit Lookup API

The application writes structured JSON log records to daily rolling log files using the existing Logback configuration.

```text
logs/unifieddocumentviewer.log
logs/unifieddocumentviewer-YYYY-MM-DD.log
```

### Query Audit Logs

For local assessment and diagnostic purposes, audit records can be queried using a `requestId`.

```bash
curl \
  -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/v1/audits/22c9792e-7396-4d46-a843-922e13a38117
```

### Example Response

```json
{
  "requestId": "22c9792e-7396-4d46-a843-922e13a38117",
  "records": [
    {
      "timestamp": "2026-08-12T13:30:19.100Z",
      "applicationName": "unifieddocumentviewer",
      "requestId": "22c9792e-7396-4d46-a843-922e13a38117",
      "userId": "user-001",
      "tenantId": "tenant-001",
      "apiName": "GET /api/v1/vehicles/1HGCM82633A004352/documents",
      "layer": "CONTROLLER",
      "className": "DocumentController",
      "methodName": "searchDocuments",
      "status": "SUCCESS",
      "startTime": "2026-08-12T13:30:19.100Z",
      "endTime": "2026-08-12T13:30:19.950Z",
      "durationMs": 850,
      "message": "controller operation success"
    }
  ]
}
```

---

## Request ID Validation

The `requestId` must match the same safe format accepted by `X-Request-ID`:

```regex
[A-Za-z0-9._:-]{1,128}
```

The audit lookup does **not** use the request ID to construct a file path.

Instead, it:

1. Scans the active application log
2. Scans recent daily rollover log files under `LOG_PATH`
3. Parses each JSON line using Jackson
4. Matches the structured `requestId` field exactly

Malformed or partially written log lines are skipped and logged internally.

---

## Audit Lookup Configuration

The lookup window defaults to **7 days**.

It can be configured using an environment variable:

```properties
AUDIT_LOOKUP_MAX_DAYS=7
```

or through the application property:

```properties
audit.lookup.max-days=7
```

---

## Error Responses

If no audit records are found:

```text
HTTP 404
AUDIT_LOG_NOT_FOUND
```

If the supplied request ID is invalid:

```text
HTTP 400
INVALID_REQUEST_ID
```

---

## Security Considerations

Security behavior follows the current assessment configuration.

Bearer tokens are parsed to extract:

- `userId`
- `tenantId`

These values are used as part of the audit context.

Application endpoints are otherwise permitted because authorization roles are not currently defined.

> **Important:** `/api/v1/audits/{requestId}` is intended as a local/demo operational endpoint.
>
> Do not expose this endpoint publicly without adding an appropriate administrative or audit-viewer permission.

---

## Audit Lookup Limitations

The file-based audit lookup is intentionally lightweight.

Its lookup complexity is:

```text
O(n)
```

where `n` is the number of log lines within the configured lookup window.

This approach is suitable for:

- Local development
- Demo environments
- Assessment traceability
- Low-volume diagnostics

It works well for a **single application instance**.

In a multi-instance deployment, however, a request may have been handled by another application node. Therefore, the corresponding audit records may not exist in the local instance's log files.

---

## Production Audit Architecture

For production environments, audit and application logs should be moved to centralized log storage.

A possible architecture is:

```text
Application Instances
        |
        v
Fluent Bit / Filebeat
        |
        v
Elasticsearch
        |
        v
Kibana / Centralized Audit Search
```

This allows audit records from multiple application instances to be searched from a single location.
