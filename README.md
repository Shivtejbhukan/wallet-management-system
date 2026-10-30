# Wallet Management System

A Spring Boot REST API application that provides secure wallet management with JWT authentication, idempotency handling, and concurrency control.

## Live Application

- **Application URL:** https://wallet-system-kqg0.onrender.com
- **Swagger API Documentation:** https://wallet-system-kqg0.onrender.com/swagger-ui/index.html

## Technologies Used

- Java 25
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- H2 Database
- JWT Authentication
- Maven
- Swagger / OpenAPI
- JUnit and Mockito
- Docker
- Render

## Features

### Authentication and Authorization
- User registration and login.
- Password encryption using BCrypt.
- JWT-based authentication.
- Role-based authorization for USER and ADMIN.

### Wallet Management
- Every registered user gets one wallet.
- Check wallet balance.
- Add money to wallet.
- Transfer money between users.

### Idempotency
- Uses Idempotency-Key to prevent duplicate transactions.
- Repeated requests with the same key do not create duplicate transactions.

### Concurrency Handling
- Uses database locking to maintain wallet balance consistency.
- Prevents race conditions during concurrent transfers.
- Uses database transactions for atomic money transfers.

### Transaction Management
- View personal transaction history.
- Paginated transaction records.
- Admin can view all wallets and transactions.

## API Endpoints

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | /auth/register | Register user | Public |
| POST | /auth/login | User login | Public |
| GET | /wallet | Get my wallet | USER |
| POST | /wallet/add | Add money | USER |
| POST | /wallet/transfer | Transfer money | USER |
| GET | /wallet/transactions | My transactions | USER |
| GET | /admin/wallets | View all wallets | ADMIN |
| GET | /admin/transactions | View all transactions | ADMIN |

## How to Run Locally

### 1. Clone the repository

```bash
git clone YOUR_GITHUB_REPOSITORY_URL
```

### 2. Open the project

Open the cloned project in IntelliJ IDEA.

### 3. Configure JWT Secret

Set the `JWT_SECRET` environment variable with a strong Base64-encoded secret.

Never commit your JWT secret to GitHub.

### 4. Run the application

Using Maven:

```bash
mvn spring-boot:run
```

Or run the main Spring Boot application class from IntelliJ IDEA.

### 5. Open Swagger UI

```text
http://localhost:8080/swagger-ui/index.html
```

## Database Configuration

This project uses an H2 in-memory database.

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:walletdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
```

H2 Console (local development):

```text
http://localhost:8080/h2-console
```

JDBC URL:

```text
jdbc:h2:mem:walletdb
```

Note: In-memory database data is not permanent and is lost when the application process restarts.

## Running Tests

Execute the following command:

```bash
mvn clean verify
```

## Deployment

The application is deployed on Render using Docker.

Live URL:

https://wallet-system-kqg0.onrender.com

Swagger Documentation:

https://wallet-system-kqg0.onrender.com/swagger-ui/index.html

## Important Security Notes

- Passwords are stored using BCrypt hashing.
- JWT secrets must be configured through environment variables.
- Protected APIs require a valid JWT token.
- Admin APIs require ADMIN authorization.
- Never expose admin credentials or JWT secrets publicly.

## Author

Developed as a Wallet Management System project for demonstrating Spring Boot, REST APIs, JWT security, idempotency, and concurrency management.