# Task Manager System

Complete full-stack mini project using Spring Boot and Keycloak for authentication and authorization.

## Features
- **User Authentication**: Secure login and registration using Keycloak.
- **Role-based Authorization**: Access control for `ADMIN` and `USER` roles.
- **CRUD Operations**: Manage tasks with Title, Description, Status, and Timestamp.
- **Filtering & Pagination**: Efficient data retrieval with Spring Data JPA.
- **Audit Logging**: Automatic tracking of creation dates.
- **API Documentation**: Interactive Swagger/OpenAPI UI.

## Technology Stack
- **Backend**: Spring Boot 3.2.5, Spring Security, Spring Data JPA
- **Database**: PostgreSQL
- **Identity Provider**: Keycloak
- **Documentation**: SpringDoc OpenAPI (Swagger)
- **Containerization**: Docker & Docker Compose
- **Build Tool**: Gradle

## Getting Started

### Prerequisites
- Docker and Docker Compose
- JDK 17 (if running locally)

### Step-by-Step Setup

1. **Clone the repository** (or ensure all files are in the same directory).

2. **Run all services using Docker Compose**:
   ```bash
   docker-compose up --build
   ```

3. **Wait for services to start**:
   - Keycloak: `http://localhost:8080` (Realm and users are automatically imported)
   - PostgreSQL: `localhost:5432`
   - Spring Boot App: `http://localhost:8081`

4. **Access Swagger UI**:
   Open `http://localhost:8081/swagger-ui.html` to explore the API.

## API Documentation

### Authentication
Since the app uses Keycloak, you need a JWT token to access protected endpoints.

**How to get a token via CLI:**
```bash
# Get Admin Token
curl -X POST 'http://localhost:8080/realms/task-manager-realm/protocol/openid-connect/token' \
--data-urlencode 'username=admin-user' \
--data-urlencode 'password=password' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'client_id=task-manager-client'

# Get Regular User Token
curl -X POST 'http://localhost:8080/realms/task-manager-realm/protocol/openid-connect/token' \
--data-urlencode 'username=regular-user' \
--data-urlencode 'password=password' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'client_id=task-manager-client'
```

### Endpoints List

| Method | Endpoint | Description | Role Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Register new user | None |
| POST | `/api/auth/login` | Login to get JWT token | None |
| POST | `/api/auth/refresh` | Refresh JWT token | None |
| GET | `/api/tasks` | Get all tasks (paginated) | USER, ADMIN |
| GET | `/api/tasks/{id}` | Get task by ID | USER, ADMIN |
| POST | `/api/tasks` | Create new task | USER, ADMIN |
| PUT | `/api/tasks/{id}` | Update task | ADMIN |
| DELETE | `/api/tasks/{id}` | Delete task | ADMIN |

### Sample Request (POST /api/tasks)
**Header:** `Authorization: Bearer <TOKEN>`
**Body:**
```json
{
  "title": "Fix bug #123",
  "description": "Resolve the issue in the login flow",
  "status": "TODO"
}
```

## Project Structure
```text
.
├── docker-compose.yml
├── Dockerfile
├── build.gradle
├── keycloak/
│   └── realm-export.json
└── src/
    ├── main/
    │   ├── java/com/example/taskmanager/
    │   │   ├── config/        # Security, Swagger configs
    │   │   ├── controller/    # REST Controllers
    │   │   ├── dto/           # Data Transfer Objects
    │   │   ├── entity/        # JPA Entities
    │   │   ├── exception/     # Global Error Handling
    │   │   ├── repository/    # JPA Repositories
    │   │   └── service/       # Business Logic
    │   └── resources/
    │       └── application.yml
    └── test/                  # JUnit Tests
```
