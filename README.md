# Keyloop Unified Document Viewer

Spring Boot service backed by PostgreSQL that provides a unified view of vehicle-related documents from multiple external dealership systems.

The application currently aggregates documents from:

- Sales System API
- Service System API

It also provides:

- Vehicle lookup support
- Development bearer-token generation
- Request/audit lookup by `requestId`
- Swagger/OpenAPI documentation
- Docker Compose based local deployment

---

## Quick Start

### Prerequisites

Make sure the following tools are installed:

- Docker
- Docker Compose

No manual environment-variable setup is required for the default local setup. `compose.yaml` already provides default values for PostgreSQL, datasource settings, connection-pool settings, the Sales System API, the Service System API, and the application port.

### 1. Clone the repository

```bash
git clone https://github.com/noname163/keyloop.git
cd keyloop
```

### 2. Build and start the stack

```bash
docker compose up --build -d
```

This starts:

- PostgreSQL
- Keyloop Unified Document Viewer application

The application waits for PostgreSQL to become healthy before starting.

Flyway migrations run automatically during application startup.

### 3. Verify the containers

```bash
docker compose ps
```

### 4. Open Swagger UI

```text
http://localhost:8080/swagger-ui/index.html
```

The default application port is `8080`.

### 5. View application logs

```bash
docker compose logs -f application
```

### Stop the application

```bash
docker compose down
```

PostgreSQL data is persisted in the `postgres-data` Docker volume.

To stop the application and remove all persisted database data:

```bash
docker compose down --volumes
```

---

## Docker Compose Configuration

The default local values are already defined in `compose.yaml`, so creating a `.env` file is optional.

Default values include:

| Setting | Default |
| --- | --- |
| PostgreSQL database | `keyloop` |
| PostgreSQL user | `keyloop` |
| PostgreSQL password | `change-me` |
| Application port | `8080` |
| DB max pool size | `10` |
| DB min idle connections | `2` |
| DB connection timeout | `30000 ms` |
| Sales System API | Mock API configured in `compose.yaml` |
| Service System API | Mock API configured in `compose.yaml` |

You only need to override these values when you want to use a different local configuration.

For example:

```bash
APP_PORT=8090 POSTGRES_PASSWORD=my-password docker compose up --build -d
```

---

## API Usage

The easiest way to explore the API is through Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

The following examples use `curl` so the complete API flow can also be tested from the command line.

---

## Generate a Development Bearer Token

For local assessment and testing, the application provides a development token-generation endpoint:

```text
POST /api/develop/tokens
```

Request body:

```json
{
  "userId": "user-001"
}
```

Example request:

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-001"}' \
  http://localhost:8080/api/develop/tokens
```

Example response:

```json
{
  "token": "<generated-token>"
}
```

The generated development token contains:

- `userId`: taken from the request
- `tenantId`: currently defaults to `TENANT-001`
- `iat`: token creation timestamp

> **Important:** The development token uses an unsigned JWT-style token (`alg: none`). It exists only to simplify local/demo testing and must not be treated as a production authentication mechanism.

Store the returned token and send it as a Bearer token when testing APIs:

```bash
TOKEN="<generated-token>"
```

Then call an API with:

```bash
curl \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/vehicles
```

In Swagger UI, click **Authorize** and enter:

```text
Bearer <generated-token>
```

---

## Vehicle API

### List Vehicles

```text
GET /vehicles
```

Example:

```bash
curl \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/vehicles?page=0&size=20"
```

The endpoint returns a paginated list of vehicles stored in PostgreSQL.

---

## Unified Document Search API

The main feature of this service is the unified vehicle document lookup.

```text
GET /api/v1/vehicles/{vin}/documents
```

The VIN must contain exactly 17 valid VIN characters.

Example:

```bash
curl \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/vehicles/1HGCM82633A004352/documents
```

The backend queries the configured document sources and returns one consolidated response.

The current implementation integrates:

- Sales System
- Service System

The response includes documents from the available sources together with source information so the client can distinguish where each document came from.

---

## Request ID and Traceability

Requests can include an optional request ID through the `X-Request-ID` header.

Example:

```bash
curl \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Request-ID: demo-request-001" \
  http://localhost:8080/api/v1/vehicles/1HGCM82633A004352/documents
```

A request ID must match:

```regex
[A-Za-z0-9._:-]{1,128}
```

The request ID is included in structured application logs and can later be used for audit lookup.

---

## Local Audit Lookup API

The application writes structured JSON log records to daily rolling log files:

```text
logs/unifieddocumentviewer.log
logs/unifieddocumentviewer-YYYY-MM-DD.log
```

Audit records can be queried by request ID:

```text
GET /api/v1/audits/{requestId}
```

Example:

```bash
curl \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/audits/demo-request-001
```

Example response:

```json
{
  "requestId": "demo-request-001",
  "records": [
    {
      "timestamp": "2026-08-12T13:30:19.100Z",
      "applicationName": "unifieddocumentviewer",
      "requestId": "demo-request-001",
      "userId": "user-001",
      "tenantId": "TENANT-001",
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

The audit lookup does not construct file paths from the supplied request ID. It scans the active and recent rollover log files, parses each JSON record, and performs an exact structured `requestId` match.

Malformed or partially written log lines are skipped and logged internally.

### Audit Lookup Errors

If no records are found:

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

## Security Notes

The current project is configured for assessment/demo usage.

Bearer tokens are parsed to populate audit context such as:

- `userId`
- `tenantId`

The current Spring Security configuration permits application endpoints because application roles and authorization policies are not part of the current assessment scope.

The token-generation endpoint and local audit endpoint should therefore be considered development/demo utilities.

Do not expose the following endpoints publicly without production-grade authentication and authorization:

```text
POST /api/develop/tokens
GET /api/v1/audits/{requestId}
```

---

## Audit Lookup Limitations

The current file-based lookup is intentionally lightweight and suitable for:

- Local development
- Demonstrations
- Assessment traceability
- Low-volume diagnostics

The lookup is approximately `O(n)` over the log lines in the configured lookup window.

It works best for a single application instance. In a multi-instance deployment, a request may have been handled by another instance and therefore may not exist in the local node's files.

For production, move logs to centralized storage such as:

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

---

## Build Only the Application Image

To build only the Spring Boot Docker image:

```bash
docker build -t keyloop-unified-document-viewer .
```

Running this image directly instead of using Docker Compose requires supplying a reachable PostgreSQL datasource and any external-service configuration required by `application.properties`.

For the recommended local setup, use Docker Compose instead:

```bash
docker compose up --build -d
```
