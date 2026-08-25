
# Transaction Service

The Transaction Service is a Spring Boot microservice responsible for managing financial transaction data and transaction-related operations within FinTech Core.

This service is part of the FinTech Core microservices architecture, an AI-powered personal finance and payment platform designed to help customers manage financial activity, payments, and personal finances.

## Current Status

This service is part of an ongoing architectural migration of FinTech Core.

The original implementation of `transaction-service` contained both:

- Transaction management
- Payment processing and payment-provider integrations

As part of the new architecture, these responsibilities are being separated.

The new architecture introduces a dedicated `payment-service` responsible for payment-provider integrations such as Stripe and PayPal.

The long-term responsibility of this service is therefore focused on **financial transaction management**, rather than payment-provider integration.

### Previous Architecture

```text
Frontend
    │
    ▼
Transaction Service
    ├── Transaction management
    ├── Payment processing
    ├── Stripe integration
    └── PayPal integration
````

### New Architecture

```text
                    API Gateway
                         │
             ┌───────────┴───────────┐
             │                       │
             ▼                       ▼
    Transaction Service       Payment Service
             │                       │
             │                       ├── Stripe
             │                       └── PayPal
             │
             ▼
       Transaction Data
```

This separation allows transaction management and payment processing to evolve independently.

---

## Responsibilities

The Transaction Service is responsible for transaction-related functionality such as:

* Creating financial transaction records
* Maintaining transaction status
* Retrieving transaction information
* Persisting transaction data
* Validating transaction requests
* Providing REST APIs for transaction operations

Payment-provider-specific responsibilities are being moved to the dedicated Payment Service.

---

## Current Features

The current implementation provides:

* REST API for transaction operations
* Transaction persistence
* Transaction status management
* Input validation
* PostgreSQL database integration
* JPA/Hibernate persistence
* Spring Boot application framework
* Docker-based local development

The original implementation also contains payment-processing functionality and Stripe integration. This functionality is being separated into `payment-service` as part of the new architecture.

---

## Technology Stack

* Java
* Spring Boot
* Spring Data JPA
* Hibernate
* PostgreSQL
* Maven
* Docker
* Docker Compose

The service currently uses Java 17+ according to the original project documentation.

---

## Project Structure

```text
transaction-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── mvnw
├── mvnw.cmd
├── render.yaml
└── README.md
```

---

## API

The current implementation exposes transaction-related REST endpoints.

### Process Payment

```http
POST /payment/process-payment
```

> **Legacy functionality:** This endpoint belongs to the original implementation where payment processing was handled by the Transaction Service.

The endpoint accepts transaction information such as:

```json
{
  "amount": 100.00,
  "senderAccount": "sender_account_123",
  "receiverAccount": "merchant_account_456",
  "paymentMethod": "tok_visa",
  "timestamp": "2025-04-05T12:30:00",
  "currencyType": "USD",
  "paymentId": "pay_id_12348"
}
```

The original implementation can return transaction states such as:

```text
PENDING
COMPLETED
FAILED
```

This functionality is being refactored as part of the separation between transaction management and payment processing.

### Get Transaction

```http
GET /transactions/{id}
```

Retrieves transaction information for a specific transaction.

---

## Transaction Data

The transaction model in the original implementation contains information such as:

| Field           | Description                          |
| --------------- | ------------------------------------ |
| `userId`        | User associated with the transaction |
| `amount`        | Transaction amount                   |
| `status`        | Current transaction status           |
| `paymentMethod` | Payment method used                  |
| `createdAt`     | Transaction creation timestamp       |
| `updatedAt`     | Last transaction update timestamp    |

The transaction model will evolve as the new FinTech Core requirements are implemented.

---

## Relationship with Payment Service

A key architectural change in the new FinTech Core architecture is the separation of transaction management from payment processing.

### Transaction Service

Responsible for:

```text
Financial transaction
    │
    ├── Transaction ID
    ├── Customer
    ├── Amount
    ├── Currency
    ├── Transaction status
    ├── Transaction type
    └── Timestamps
```

### Payment Service

Responsible for:

```text
Payment
    │
    ├── Payment request
    ├── Payment provider
    ├── Stripe
    ├── PayPal
    ├── Provider response
    └── Payment status
```

The Transaction Service should not need to know the implementation details of a payment provider.

For example:

```text
Customer
    │
    ▼
Frontend
    │
    ▼
API Gateway
    │
    ▼
Transaction Service
    │
    │ create transaction
    ▼
Transaction
    │
    │ payment required
    ▼
Payment Service
    │
    ├── Stripe
    └── PayPal
```

This separation reduces coupling between the financial transaction domain and external payment providers.

---

## Integration with Other FinTech Core Services

The Transaction Service is one component of the larger FinTech Core platform.

```text
                       FinTech Core
                            │
                       API Gateway
                            │
        ┌───────────────────┼────────────────────┐
        │                   │                    │
        ▼                   ▼                    ▼
  Auth Service       Customer Service     Transaction Service
                                                   │
                                                   ▼
                                            Payment Service
```

Additional services, including AI-powered financial services, will interact with transaction data through well-defined APIs rather than directly accessing the Transaction Service's database.

---

## Getting Started

### Prerequisites

* JDK 17+
* Maven
* PostgreSQL
* Docker
* Docker Compose

### Clone the Repository

```bash
git clone https://github.com/ryryanb/fintech_core.git
cd fintech_core/transaction-service
```

### Build

Using the Maven wrapper:

```bash
./mvnw clean install
```

On Windows:

```bash
mvnw.cmd clean install
```

### Run Tests

```bash
./mvnw test
```

### Run the Application

```bash
./mvnw spring-boot:run
```

---

## Docker

The repository includes a Dockerfile and Docker Compose configuration.

Build and start the service:

```bash
docker-compose up --build
```

Docker Compose can be used to start the application together with its PostgreSQL dependency.

---

## Configuration

Database and application configuration is defined under:

```text
src/main/resources/
```

The original implementation also used configuration for external payment-provider integrations such as Stripe.

As payment functionality is separated into `payment-service`, provider-specific configuration should eventually be removed from this service.

---

## Testing

Run the complete test suite with:

```bash
./mvnw test
```

The test suite is intended to verify transaction-service behavior independently from the other FinTech Core services.

---

## Development Direction

The Transaction Service will evolve according to the FinTech Core product requirements.

Planned responsibilities include:

### Transaction Management

* Create transactions
* Retrieve transactions
* Retrieve transaction history
* Track transaction status
* Associate transactions with customers
* Associate transactions with financial accounts
* Categorize transactions
* Record transaction timestamps
* Support transaction metadata

### Financial Activity

The service will eventually provide the authoritative transaction data required by other parts of FinTech Core.

For example, the AI Financial Assistant may ask:

```text
How much did I spend on food this month?
```

The AI service should not calculate this from its own copy of financial data.

Instead:

```text
Customer
   │
   ▼
AI Financial Assistant
   │
   ▼
Transaction Service
   │
   ▼
Authoritative transaction data
   │
   ▼
AI analysis
```

The Transaction Service remains the source of truth for transaction information.

---

## Architectural Principle

FinTech Core follows an important principle:

> **AI assists with financial intelligence, but backend services remain authoritative for financial data and business rules.**

The Transaction Service therefore remains responsible for maintaining authoritative transaction data.

The AI layer may analyze transaction data, explain spending patterns, or answer questions, but it should not independently modify authoritative transaction records without going through the appropriate backend service.

---

## Service Boundaries

| Responsibility               | Service               |
| ---------------------------- | --------------------- |
| Authentication               | Auth Service          |
| Customer information         | Customer Service      |
| Financial transactions       | Transaction Service   |
| Payment-provider integration | Payment Service       |
| AI financial assistance      | AI Service            |
| User interface               | FinTech Core Frontend |

This separation is intended to make each service independently maintainable and extensible.

---

## Related Services

FinTech Core consists of multiple services:

* `fastapi` — Authentication Service
* `customer-service` — Customer management
* `transaction-service` — Financial transaction management
* `payment-service` — Payment processing and payment-provider integrations
* `transaction-frontend` — Customer-facing React application

Each service contains its own README with service-specific documentation.

---

## License

This project is licensed under the MIT License.



