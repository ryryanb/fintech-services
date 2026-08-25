
# FinTech Core — Authentication Service

The FinTech Core Authentication Service is a FastAPI-based microservice responsible for user registration, authentication, authorization token generation, and Google OAuth authentication.

It provides the identity and authentication capabilities used by the FinTech Core platform while allowing other microservices to remain focused on their own business domains.

## Responsibilities

The Authentication Service is responsible for:

- User registration
- User login
- Password hashing and verification
- JWT access-token generation
- JWT token validation
- Protected-resource authentication
- Google OAuth authentication
- User identity persistence
- Tenant-aware user registration and login
- Authentication-related database access

The service is intentionally separated from business-domain services such as Customer, Transaction, and Payment services.

---

## Architecture

The Authentication Service is implemented using:

- **Python**
- **FastAPI**
- **SQLAlchemy**
- **PostgreSQL-compatible asynchronous database access**
- **Pydantic**
- **JWT**
- **bcrypt**
- **Google OAuth 2.0**
- **Uvicorn**
- **Docker**

The current application structure is:

```text
fastapi/
├── app/
│   ├── auth.py
│   ├── auth_router.py
│   ├── backup.py
│   ├── config.py
│   ├── crud.py
│   ├── database.py
│   ├── deps.py
│   ├── main.py
│   ├── models.py
│   ├── schemas.py
│   └── utils.py
│
├── Dockerfile
├── requirements.txt
├── render.yaml
├── runtime.txt
├── vercel.json
└── .python-version
````

The FastAPI application registers the standard authentication routes and Google OAuth routes and exposes a health-check endpoint. ([GitHub][1])

---

## Authentication Flow

### Username / Password Authentication

The basic authentication flow is:

```text
Client
   │
   │ POST /register
   ▼
Authentication Service
   │
   ├── Validate user information
   ├── Check existing user
   ├── Hash password
   └── Store user
          │
          ▼
       Database


Client
   │
   │ POST /login
   ▼
Authentication Service
   │
   ├── Find user
   ├── Verify password
   └── Generate JWT
          │
          ▼
      Access Token
```

During registration, the service checks whether a user with the same email already exists for the specified tenant. Passwords are hashed before being persisted. ([GitHub][2])

During login, the service looks up the user by email and tenant ID, verifies the supplied password, and returns a bearer access token. ([GitHub][2])

---

## Google OAuth Flow

The service also supports Google OAuth authentication.

```text
Client
   │
   │ GET /auth/google/login
   ▼
Authentication Service
   │
   ▼
Google OAuth
   │
   │ Authorization Code
   ▼
/auth/google/callback
   │
   ├── Exchange code for access token
   ├── Retrieve Google user information
   ├── Find or create local user
   └── Generate JWT
          │
          ▼
       Frontend
```

The Google OAuth implementation uses Google's authorization endpoint and retrieves user information through Google's OpenID Connect user-info endpoint. ([GitHub][3])

If a Google user does not already exist, the service creates a local user record and marks it as a Google account. ([GitHub][4])

---

## API Endpoints

### Health Check

#### `GET /health`

Returns the health status of the service.

Example:

```json
{
  "status": "healthy"
}
```

---

### Register User

#### `POST /register`

Registers a new user.

Example request:

```json
{
  "email": "user@example.com",
  "password": "password",
  "name": "Ryan Bondoc",
  "address": "Manila, Philippines",
  "tenant_id": 1
}
```

Successful response:

```json
{
  "msg": "User registered successfully"
}
```

The service checks the email and tenant combination before creating the user. If the user is already registered for that tenant, the service returns an HTTP 400 response. ([GitHub][2])

---

### Login

#### `POST /login`

Authenticates a user and returns a JWT access token.

Example request:

```json
{
  "email": "user@example.com",
  "password": "password",
  "tenant_id": 1
}
```

Successful response:

```json
{
  "access_token": "<JWT>",
  "token_type": "bearer"
}
```

The JWT contains authentication-related information including the user's email, tenant ID, and role. The current implementation assigns the `user` role during login. ([GitHub][2])

---

### Google Login

#### `GET /auth/google/login`

Returns the Google OAuth authorization URL.

The response contains an `auth_url` that the client can use to begin the Google authentication process. ([GitHub][3])

---

### Google Callback

#### `GET /auth/google/callback`

Handles the authorization code returned by Google.

The service:

1. Exchanges the authorization code for a Google access token.
2. Retrieves the user's Google profile information.
3. Finds or creates the corresponding local user.
4. Generates a JWT.
5. Redirects the user back to the frontend with the authentication result. ([GitHub][3])

---

### Protected Endpoint

#### `GET /protected`

A protected endpoint demonstrating JWT-based authentication.

The endpoint requires a valid bearer token and uses the authenticated user as a dependency. ([GitHub][2])

Example:

```http
Authorization: Bearer <JWT>
```

---

## Data Model

The service currently maintains a `users` table.

The `UserDB` model contains:

| Field               | Description                                              |
| ------------------- | -------------------------------------------------------- |
| `id`                | Auto-generated integer primary key                       |
| `email`             | User email address                                       |
| `password`          | Hashed password; nullable for Google accounts            |
| `name`              | User name                                                |
| `address`           | User address                                             |
| `tenant_id`         | Tenant identifier                                        |
| `profile_picture`   | Google profile picture URL when applicable               |
| `is_active`         | Indicates whether the user is active                     |
| `is_google_account` | Indicates whether the account was created through Google |

The email field is unique and indexed, while the database ID is auto-generated. ([GitHub][5])

---

## Multi-Tenancy

The current implementation includes a `tenant_id` associated with users.

For standard registration and login, the service uses both:

```text
email
tenant_id
```

when identifying the user.

This allows the authentication model to support users belonging to different tenants while maintaining tenant-specific registration and login behavior. ([GitHub][2])

---

## Security

The service currently uses several security mechanisms.

### Password Hashing

Passwords are hashed before being stored in the database rather than storing plaintext passwords. ([GitHub][2])

### JWT Authentication

Successful login generates a JWT access token.

Protected endpoints use the bearer token to authenticate requests. The service validates the token signature and retrieves the associated user. ([GitHub][4])

### Google OAuth

Google authentication is implemented using OAuth 2.0 authorization-code flow.

The service exchanges the authorization code with Google and retrieves user information before creating the local authentication session. ([GitHub][3])

### Environment-Based Secrets

Sensitive configuration such as the Google client credentials and JWT secret is supplied through environment configuration rather than being hard-coded into the application. ([GitHub][6])

---

## Configuration

The service uses environment variables for configuration.

Current configuration includes:

```env
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GOOGLE_REDIRECT_URI=
JWT_SECRET=
DEBUG=false
FRONTEND_URL=
DATABASE_URL=
```

The application loads configuration from environment variables and supports loading values from a `.env` file during development. ([GitHub][6])

> **Never commit production secrets to the repository.**

---

## Database

The service uses SQLAlchemy's asynchronous database support.

The current database configuration uses:

* SQLAlchemy
* `create_async_engine`
* `AsyncSession`
* `async_sessionmaker`
* asynchronous database connections

The database URL is provided through the `DATABASE_URL` environment variable. ([GitHub][7])

The application currently creates database tables during application startup when it is not running in the Vercel environment. ([GitHub][8])

---

## Technology Stack

| Technology                       | Purpose                        |
| -------------------------------- | ------------------------------ |
| Python                           | Programming language           |
| FastAPI                          | REST API framework             |
| Pydantic                         | Request/data validation        |
| SQLAlchemy                       | ORM and database access        |
| PostgreSQL / compatible database | Persistent storage             |
| JWT                              | Authentication tokens          |
| bcrypt / Passlib                 | Password hashing               |
| Google OAuth 2.0                 | Social authentication          |
| HTTPX                            | HTTP communication with Google |
| Uvicorn                          | ASGI application server        |
| Docker                           | Containerization               |

The current dependencies are defined in `requirements.txt`. ([GitHub][9])

---

## Running Locally

### Prerequisites

* Python 3.9+
* PostgreSQL or another database supported by the configured SQLAlchemy connection
* Google OAuth credentials if Google login is required

The Dockerfile currently uses Python 3.9 as its base image. ([GitHub][10])

### Install Dependencies

Create and activate a virtual environment:

```bash
python -m venv venv
source venv/bin/activate
```

Install dependencies:

```bash
pip install -r requirements.txt
```

### Environment Variables

Create a `.env` file:

```env
DATABASE_URL=your_database_url

GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=your_google_redirect_uri

JWT_SECRET=your_jwt_secret

DEBUG=false
FRONTEND_URL=http://localhost:3000
```

### Start the Service

```bash
uvicorn app.main:app --reload
```

The API will be available at:

```text
http://localhost:8000
```

FastAPI also provides interactive API documentation at:

```text
http://localhost:8000/docs
```

and ReDoc at:

```text
http://localhost:8000/redoc
```

---

## Docker

The service includes a Dockerfile based on the official Python 3.9 slim image.

The container installs the dependencies from `requirements.txt`, copies the application into `/app`, and starts the application using Uvicorn. ([GitHub][10])

Build the image:

```bash
docker build -t fintech-core-auth .
```

Run the container:

```bash
docker run -p 5000:5000 fintech-core-auth
```

The current Docker configuration exposes port `5000`. ([GitHub][10])

---

## Project Structure

```text
fastapi/
│
├── app/
│   ├── auth.py          # Registration and login endpoints
│   ├── auth_router.py   # Google OAuth endpoints
│   ├── backup.py        # Supporting/legacy functionality
│   ├── config.py        # Application configuration
│   ├── crud.py          # Database/auth helper functions
│   ├── database.py      # SQLAlchemy database configuration
│   ├── deps.py          # FastAPI dependencies and JWT authentication
│   ├── main.py          # FastAPI application entry point
│   ├── models.py        # SQLAlchemy database models
│   ├── schemas.py       # Pydantic request models
│   └── utils.py         # Authentication utilities
│
├── Dockerfile
├── requirements.txt
├── render.yaml
├── runtime.txt
├── vercel.json
└── .python-version
```

---

## Relationship to Other FinTech Core Services

The Authentication Service is responsible for **identity and authentication**, not customer financial data.

The planned architecture separates responsibilities across services:

```text
                    Client
                       │
                       ▼
                 API Gateway
                       │
        ┌──────────────┼──────────────┐
        │              │              │
        ▼              ▼              ▼
 Auth Service    Customer Service   Transaction
        │                             Service
        │                                 │
        │                                 ▼
        │                           Payment Service
        │
        └──────────────┐
                       │
                       ▼
                   AI Service
```

### Authentication Service

Responsible for:

* User identity
* Registration
* Login
* Authentication tokens
* Google authentication

### Customer Service

Responsible for:

* Customer profile
* Customer information
* KYC-related information
* Customer lifecycle

### Transaction Service

Responsible for:

* Financial transactions
* Transaction history
* Transaction-related business rules

### Payment Service

Responsible for:

* Payment initiation
* Payment processing
* Payment-provider integrations

### AI Service

Responsible for:

* Financial assistant
* Spending analysis
* Anomaly detection
* AI-powered financial insights

The AI service should consume authoritative information from the appropriate domain services rather than becoming the system of record.

---

## Current Status

This service represents the authentication component of the original FinTech Core implementation.

The broader FinTech Core architecture is currently being reorganized into more clearly separated domain microservices.

In the new architecture, this service remains responsible for authentication while customer, transaction, payment, and AI responsibilities are progressively separated into their own services.

### Planned Improvements

Potential future improvements include:

* More comprehensive role-based authorization
* Token expiration and refresh-token management
* Improved tenant isolation
* More comprehensive request validation
* Authentication audit logging
* Rate limiting
* Improved error handling
* Automated integration testing
* Centralized API gateway integration
* Service-to-service authentication

These are planned capabilities and are **not necessarily implemented in the current version**.

---

## Testing

Tests should be added and maintained for:

* User registration
* Duplicate registration
* Login with valid credentials
* Login with invalid credentials
* Password verification
* JWT generation
* JWT validation
* Protected endpoints
* Google OAuth flows
* Database interactions

---

## API Documentation

When running locally, FastAPI automatically provides interactive API documentation:

```text
http://localhost:8000/docs
```

The deployed service also exposes FastAPI's interactive documentation when enabled by the deployment configuration.

---

## Deployment

The service contains deployment configuration for cloud hosting, including:

* `render.yaml`
* `vercel.json`
* Docker configuration

The Docker image is configured to run Uvicorn and listen on the configured service port. ([GitHub][10])

---

## Architectural Note

This service is intentionally kept separate from the financial domain services.

Authentication answers:

> **"Who is this user, and are they authenticated?"**

It should not answer questions such as:

> "What is this customer's financial profile?"

> "What transactions does this customer have?"

> "Can this customer make this payment?"

Those responsibilities belong to the appropriate domain services.

This separation allows the FinTech Core platform to evolve without coupling authentication logic to financial business logic.

