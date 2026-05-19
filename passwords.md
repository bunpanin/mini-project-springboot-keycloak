# Project Credentials

This file contains the default credentials for the Task Manager System.

## Keycloak Users

### 1. Admin User
- **Username:** `admin-user`
- **Password:** `password`
- **Roles:** `ADMIN`, `USER`
- **Permissions:** Full access (Create, Read, Update, Delete tasks).

### 2. Regular User
- **Username:** `regular-user`
- **Password:** `password`
- **Roles:** `USER`
- **Permissions:** Read and Create access (Get all tasks, Get task by ID, Create tasks).

## User Registration
You can register a new user via the API:
- **Endpoint:** `POST http://localhost:8081/api/auth/register`
- **Request Body:**
```json
{
  "username": "new-user",
  "password": "password123",
  "email": "newuser@example.com",
  "firstName": "New",
  "lastName": "User"
}
```
- **Default Role:** New users are automatically assigned the `USER` role.

---

## Keycloak Administration Console
Used to manage realms, clients, users, and roles.

- **URL:** `http://localhost:8080`
- **Username:** `admin`
- **Password:** `admin`

---

## Database (PostgreSQL)
- **Host:** `localhost`
- **Port:** `5432`
- **Database:** `taskdb`
- **Username:** `user`
- **Password:** `password`

> **Note:** These are default development credentials. Ensure you change them before deploying to a production environment.
