# Event-Driven User Activity Tracking Service

A robust backend system that tracks user activities (login, logout, page view, etc.) using an event-driven architecture with **RabbitMQ** as the message broker, **MySQL** for persistence, and **Java/Spring Boot** for both services.

## Architecture Overview

```
┌─────────────┐     ┌───────────┐     ┌───────────────────┐     ┌──────────┐     ┌─────────┐
│  HTTP Client│────▶│  Producer │────▶│     RabbitMQ      │────▶│ Consumer │────▶│  MySQL  │
│  (POST)     │ 202 │  Service  │     │  (message queue)  │     │ Service  │     │  (DB)   │
└─────────────┘     └───────────┘     └───────────────────┘     └──────────┘     └─────────┘
```

**Key Design Decisions:**

- **Decoupled services**: The Producer publishes events to RabbitMQ and immediately returns `202 Accepted`. It never touches the database.
- **Asynchronous processing**: The Consumer independently reads from the queue and persists events to MySQL at its own pace.
- **Dead Letter Queue (DLQ)**: Malformed or unprocessable messages are routed to a DLQ (`user_activity_events_dlq`) instead of being lost or blocking the main queue.
- **Graceful shutdown**: Both services use Spring Boot's graceful shutdown (`server.shutdown=graceful`) to drain in-flight requests/messages before stopping.
- **Health endpoints**: Both services expose `/health` that verify connectivity to their dependencies (RabbitMQ, MySQL).

## Tech Stack

| Component        | Technology                     |
|------------------|--------------------------------|
| Producer API     | Java 17, Spring Boot 3.2       |
| Consumer Service | Java 17, Spring Boot 3.2       |
| Message Broker   | RabbitMQ 3 (management alpine) |
| Database         | MySQL 8.0                      |
| Containerization | Docker, Docker Compose          |

## Project Structure

```
.
├── producer-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/producer/
│       │   ├── ProducerApplication.java
│       │   ├── config/RabbitMQConfig.java
│       │   ├── controller/EventController.java
│       │   ├── controller/HealthController.java
│       │   ├── exception/GlobalExceptionHandler.java
│       │   ├── model/UserActivityEvent.java
│       │   ├── model/ErrorResponse.java
│       │   └── service/EventPublisherService.java
│       └── test/java/com/example/producer/
│           ├── controller/EventControllerTest.java
│           └── service/EventPublisherServiceTest.java
├── consumer-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/consumer/
│       │   ├── ConsumerApplication.java
│       │   ├── config/RabbitMQConfig.java
│       │   ├── controller/HealthController.java
│       │   ├── listener/ActivityEventListener.java
│       │   ├── model/UserActivity.java
│       │   ├── model/UserActivityEvent.java
│       │   ├── repository/UserActivityRepository.java
│       │   └── service/ActivityStorageService.java
│       └── test/java/com/example/consumer/
│           ├── listener/ActivityEventListenerTest.java
│           └── service/ActivityStorageServiceTest.java
├── db/
│   └── init.sql
├── docker-compose.yml
├── .env.example
└── README.md
```

## Setup Instructions

### Prerequisites

- Docker and Docker Compose installed
- (Optional) Java 17 and Maven for local development

### Quick Start

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd Event-Driven-User-Activity-Tracking-Service-with-RabbitMQ
   ```

2. **Copy and configure environment variables (optional):**
   ```bash
   cp .env.example .env
   # Edit .env if you want to change defaults
   ```

3. **Start all services:**
   ```bash
   docker-compose up --build
   ```

   This will build and start:
   - **RabbitMQ** on ports 5672 (AMQP) and 15672 (Management UI)
   - **MySQL** on port 3306
   - **Producer Service** on port 8000
   - **Consumer Service** on port 8001

4. **Verify services are healthy:**
   ```bash
   # Producer health
   curl http://localhost:8000/health

   # Consumer health
   curl http://localhost:8001/health
   ```

5. **Stop services:**
   ```bash
   docker-compose down
   ```

## API Endpoints

### Producer Service (port 8000)

#### `POST /api/v1/events/track`

Accepts a user activity event and publishes it to RabbitMQ.

**Request Body:**
```json
{
  "user_id": 123,
  "event_type": "page_view",
  "timestamp": "2023-10-27T10:00:00Z",
  "metadata": {
    "page_url": "/products/item-xyz",
    "session_id": "abc123"
  }
}
```

| Field        | Type    | Required | Description                                    |
|--------------|---------|----------|------------------------------------------------|
| `user_id`    | integer | Yes      | Unique identifier for the user                 |
| `event_type` | string  | Yes      | Type of activity (max 50 chars)                |
| `timestamp`  | string  | Yes      | ISO 8601 datetime (e.g., `2023-10-27T10:00:00Z`) |
| `metadata`   | object  | No       | Arbitrary key-value metadata                   |

**Success Response (202 Accepted):**
```json
{
  "status": "accepted",
  "message": "Event published successfully",
  "user_id": 123,
  "event_type": "page_view"
}
```

**Validation Error (400 Bad Request):**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": [
    "userId: user_id is required",
    "eventType: event_type is required"
  ],
  "timestamp": "2023-10-27T10:00:00Z"
}
```

#### `GET /health`

Returns service health status including RabbitMQ connectivity.

**Response (200 OK):**
```json
{
  "status": "UP",
  "rabbitmq": "connected"
}
```

### Consumer Service (port 8001)

#### `GET /health`

Returns service health status including RabbitMQ and MySQL connectivity.

**Response (200 OK):**
```json
{
  "status": "UP",
  "rabbitmq": "connected",
  "mysql": "connected"
}
```

## Example Usage

```bash
# Track a login event
curl -X POST http://localhost:8000/api/v1/events/track \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": 1,
    "event_type": "login",
    "timestamp": "2023-10-27T10:00:00Z",
    "metadata": {"ip_address": "192.168.1.1"}
  }'

# Track a page view
curl -X POST http://localhost:8000/api/v1/events/track \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": 1,
    "event_type": "page_view",
    "timestamp": "2023-10-27T10:01:00Z",
    "metadata": {"page_url": "/dashboard", "session_id": "sess-001"}
  }'

# Track a logout
curl -X POST http://localhost:8000/api/v1/events/track \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": 1,
    "event_type": "logout",
    "timestamp": "2023-10-27T10:30:00Z"
  }'

# Verify data was stored in MySQL
docker-compose exec mysql mysql -u root -proot_password user_activity_db \
  -e "SELECT * FROM user_activities;"
```

## Running Tests

### Via Docker Compose (recommended)

```bash
# Producer tests
docker-compose exec producer-service mvn test -f /app/pom.xml

# Consumer tests
docker-compose exec consumer-service mvn test -f /app/pom.xml
```

### Locally (requires Java 17 + Maven)

```bash
# Producer tests
cd producer-service
mvn test

# Consumer tests
cd consumer-service
mvn test
```

## Database Schema

```sql
CREATE TABLE user_activities (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL,
    event_type  VARCHAR(50)  NOT NULL,
    timestamp   DATETIME     NOT NULL,
    metadata    JSON,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_event_type (event_type),
    INDEX idx_timestamp (timestamp)
);
```

## RabbitMQ Configuration

| Resource            | Name                         |
|---------------------|------------------------------|
| Exchange            | `user_activity_exchange`     |
| Queue               | `user_activity_events`       |
| Routing Key         | `user.activity`              |
| Dead Letter Exchange| `user_activity_dlx`          |
| Dead Letter Queue   | `user_activity_events_dlq`   |

The RabbitMQ Management UI is accessible at `http://localhost:15672` (guest/guest).

## Environment Variables

| Variable            | Default          | Description                    |
|---------------------|------------------|--------------------------------|
| `RABBITMQ_HOST`     | `rabbitmq`       | RabbitMQ hostname              |
| `RABBITMQ_PORT`     | `5672`           | RabbitMQ AMQP port             |
| `RABBITMQ_USERNAME` | `guest`          | RabbitMQ username              |
| `RABBITMQ_PASSWORD` | `guest`          | RabbitMQ password              |
| `MYSQL_HOST`        | `mysql`          | MySQL hostname                 |
| `MYSQL_PORT`        | `3306`           | MySQL port                     |
| `MYSQL_USER`        | `root`           | MySQL username                 |
| `MYSQL_PASSWORD`    | `root_password`  | MySQL root password            |
| `MYSQL_DB`          | `user_activity_db` | MySQL database name          |
| `SERVER_PORT`       | `8000` / `8001`  | Service port (per service)     |

## Challenges and Solutions

1. **Message serialization/deserialization**: Used Jackson's `Jackson2JsonMessageConverter` on both producer and consumer to ensure consistent JSON handling across RabbitMQ messages with proper `@JsonProperty` annotations for snake_case mapping.

2. **Timestamp handling**: ISO 8601 timestamps with timezone offsets (e.g., `+05:30` or `Z`) are parsed using `OffsetDateTime` and then converted to `LocalDateTime` for MySQL storage.

3. **Consumer resilience**: The consumer validates each message before processing. Invalid messages and processing failures are rejected (not requeued) and automatically routed to the Dead Letter Queue, preventing a single bad message from blocking the entire queue.

4. **Service startup ordering**: Docker Compose health checks with `depends_on: condition: service_healthy` ensure the producer waits for RabbitMQ and the consumer waits for both RabbitMQ and MySQL before starting.

5. **Graceful shutdown**: Spring Boot's built-in graceful shutdown (`server.shutdown=graceful`) ensures in-flight HTTP requests complete and RabbitMQ listeners finish processing current messages before the JVM exits.
