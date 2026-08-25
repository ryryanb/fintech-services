> **Architecture Evolution:** The original FinTech Core implementation contained both transaction management and Stripe/PayPal payment functionality within `transaction-service`. As part of the new architecture, payment processing is being separated into a dedicated `payment-service`, while `transaction-service` will focus exclusively on financial transaction management. This restructuring establishes clearer service boundaries and domain ownership.

# Payment Service

Payment Service is a Spring Boot microservice within the **FinTech Core** platform responsible for processing and managing payments.

The service provides a unified payment interface for the application while encapsulating integrations with external payment providers such as **Stripe** and **PayPal**.

The service is designed to keep payment-provider-specific implementation details isolated from the rest of the platform.

## 🎯 Purpose

The Payment Service is responsible for the payment domain of FinTech Core.

Its primary responsibilities include:

- Creating payment requests
- Initiating payments with external payment providers
- Tracking payment status
- Retrieving payment information
- Handling payment-provider responses
- Supporting multiple payment providers
- Providing a consistent payment API to other FinTech Core services
- Maintaining payment-related business rules

The service acts as an abstraction layer between FinTech Core and external payment providers.

```text
Customer
   │
   ▼
Frontend Application
   │
   ▼
API Gateway
   │
   ▼
Payment Service
   │
   ├── Stripe
   │
   └── PayPal
````

## 🏗 Architecture

Payment Service follows a layered Spring Boot architecture:

```text
payment-service
├── controller/
├── service/
│   └── impl/
├── provider/
├── repository/
├── entity/
├── dto/
├── mapper/
├── exception/
└── config/
```

### Controller Layer

Exposes REST endpoints for payment-related operations.

### Service Layer

Contains payment business logic and coordinates payment processing.

### Provider Layer

Contains integrations with external payment providers.

Each provider should implement a common interface so that the Payment Service does not depend directly on provider-specific implementation details.

For example:

```java
public interface PaymentProvider {

    PaymentResult createPayment(PaymentRequest request);

    PaymentResult getPayment(String providerPaymentId);

    PaymentResult cancelPayment(String providerPaymentId);
}
```

Provider implementations may include:

```text
PaymentProvider
      │
      ├── StripePaymentProvider
      │
      └── PayPalPaymentProvider
```

This allows additional payment providers to be added without significantly changing the core payment business logic.

### Repository Layer

Provides persistence operations for payment-related data using Spring Data JPA.

### Entity Layer

Contains JPA entities representing payment information.

### DTO Layer

Contains request and response objects used by the REST API.

### Mapper Layer

Converts between entities and DTOs.

### Exception Layer

Contains payment-specific exceptions and error handling.

## 🛠 Technology Stack

* Java 21
* Spring Boot
* Spring Web
* Spring Data JPA
* Jakarta Validation
* PostgreSQL
* Maven
* Docker
* Stripe API
* PayPal API

## 💳 Payment Domain

A payment represents an attempt to transfer money through a supported payment provider.

A payment may contain information such as:

* Payment ID
* Customer ID
* Transaction ID
* Amount
* Currency
* Payment provider
* Provider payment ID
* Payment status
* Payment method
* Creation timestamp
* Update timestamp

The exact payment model may evolve as additional business requirements are implemented.

## 📋 User Stories

The Payment Service will be developed incrementally through user stories.

Examples include:

| ID      | User Story                                                                                                                                       | Priority |
| ------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | -------- |
| PAY-001 | As a customer, I want to initiate a payment so that I can pay for a transaction.                                                                 | P0       |
| PAY-002 | As a customer, I want my payment information validated so that invalid payments cannot be submitted.                                             | P0       |
| PAY-003 | As a customer, I want to select a payment provider so that I can use my preferred payment method.                                                | P0       |
| PAY-004 | As a customer, I want to know the status of my payment so that I can determine whether it was successful.                                        | P0       |
| PAY-005 | As a customer, I want to view my payment history so that I can review previous payments.                                                         | P1       |
| PAY-006 | As a customer, I want payments to be processed consistently regardless of the provider so that the application has a unified payment experience. | P1       |

Additional user stories will be added as the payment domain evolves.

## 🔌 API

The Payment Service exposes REST APIs for payment processing and management.

### Create Payment

```http
POST /payments
```

Creates and initiates a payment.

Example request:

```json
{
  "customerId": "bee5da62-25aa-4aeb-8678-e922c3549c30",
  "amount": 5000.00,
  "currency": "PHP",
  "provider": "PAYPAL"
}
```

Example response:

```json
{
  "id": "payment-uuid",
  "customerId": "bee5da62-25aa-4aeb-8678-e922c3549c30",
  "amount": 5000.00,
  "currency": "PHP",
  "provider": "PAYPAL",
  "status": "PENDING"
}
```

### Get Payment

```http
GET /payments/{id}
```

Retrieves payment information using the FinTech Core payment ID.

### Get Customer Payments

```http
GET /customers/{customerId}/payments
```

Retrieves payments associated with a customer.

### Cancel Payment

```http
POST /payments/{id}/cancel
```

Attempts to cancel a payment where supported by the payment provider and business rules.

> API endpoints will evolve as the payment domain and business requirements become more clearly defined.

## 🔌 Payment Provider Integration

Payment Service provides a common abstraction over external payment providers.

```text
                 Payment Service
                       │
                       ▼
                PaymentProvider
                  /          \
                 /            \
                ▼              ▼
             Stripe          PayPal
```

The rest of FinTech Core should not need to know the provider-specific API implementation.

For example, the application should be able to request:

```http
POST /payments
```

with:

```json
{
  "amount": 5000.00,
  "currency": "PHP",
  "provider": "PAYPAL"
}
```

without directly interacting with the PayPal API.

The Payment Service handles the provider-specific communication.

## 🔄 Payment Lifecycle

A payment may move through several states during its lifecycle.

Example:

```text
PENDING
   │
   ├──► COMPLETED
   │
   ├──► FAILED
   │
   └──► CANCELLED
```

The exact states and transitions are controlled by the Payment Service business rules.

Provider-specific statuses should be translated into FinTech Core payment statuses where appropriate.

For example:

```text
Stripe / PayPal Status
          │
          ▼
Payment Service
          │
          ▼
FinTech Core Payment Status
```

This prevents the rest of the platform from becoming tightly coupled to a particular provider's status model.

## 💾 Data Ownership

Payment Service is the authoritative owner of payment data.

Other services may request payment information through the Payment Service API but should not directly modify the Payment Service database.

```text
Transaction Service
        │
        │ payment request
        ▼
Payment Service
        │
        ├── PostgreSQL
        │
        ├── Stripe
        │
        └── PayPal
```

This separation allows payment-provider integrations and payment business rules to evolve independently from transaction management.

## 🔗 Relationship with Other Services

Payment Service is part of the larger FinTech Core microservices architecture.

```text
                    ┌─────────────────┐
                    │    Frontend     │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │   API Gateway   │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
        Auth Service   Customer Service   Transaction
                                           Service
                                              │
                                              │
                                              ▼
                                       Payment Service
                                          │       │
                                          ▼       ▼
                                       Stripe   PayPal
```

### Customer Service

Customer Service owns customer information.

Payment Service may use the customer's identifier to associate payments with the appropriate customer.

### Transaction Service

Transaction Service owns financial transaction records.

A payment may be associated with a transaction through a transaction identifier.

The two services have different responsibilities:

```text
Transaction Service
    └── What financial transaction occurred?

Payment Service
    └── How was the payment processed?
```

### AI Service

AI Service may consume payment and transaction information to provide financial analysis.

AI should not directly modify payment records or communicate directly with payment providers.

## 🤖 AI Integration

Payment Service may provide payment information to the AI layer for financial analysis.

Possible future capabilities include:

* Payment spending analysis
* Recurring payment detection
* Payment anomaly detection
* Payment summaries
* Financial recommendations

However:

> **AI does not authorize or execute financial actions on its own.**

Payment processing remains controlled by deterministic backend services and the appropriate business rules.

For example, an AI assistant may answer:

> "You made 12 payments this month totaling ₱24,500."

The underlying payment information should come from the Payment Service rather than from AI-generated data.

## 🔐 Security

Payment-related operations require strong security controls.

Security considerations include:

* Authentication
* Authorization
* HTTPS
* Input validation
* Secure provider credentials
* Secure handling of provider responses
* Protection of sensitive payment information
* Idempotent payment operations
* Auditability of payment state changes

Payment provider credentials must never be committed to source control.

Sensitive configuration should be supplied through environment variables or an appropriate secrets-management mechanism.

## 🧪 Testing

The service includes unit and integration tests.

Run the test suite with:

```bash
./mvnw test
```

Tests should cover areas including:

* Payment creation
* Payment validation
* Provider selection
* Payment status handling
* Successful payments
* Failed payments
* Provider errors
* Exception handling
* Provider abstraction
* Repository interactions

Provider integrations should also be tested independently from the core payment business logic where practical.

## 🚀 Running Locally

### Prerequisites

* Java 21
* Maven
* PostgreSQL
* Docker
* Stripe test credentials (if testing Stripe)
* PayPal sandbox credentials (if testing PayPal)

### Run with Maven

```bash
./mvnw spring-boot:run
```

### Run with Docker

From the FinTech Core root directory:

```bash
docker compose up payment-service
```

## 🔧 Configuration

Configuration is provided through Spring Boot configuration files and environment variables.

Typical configuration includes:

```text
Database URL
Database username
Database password
Server port

Stripe API key
Stripe webhook secret

PayPal client ID
PayPal client secret
PayPal environment
```

Example:

```env
STRIPE_API_KEY=your_stripe_test_key
STRIPE_WEBHOOK_SECRET=your_webhook_secret

PAYPAL_CLIENT_ID=your_paypal_client_id
PAYPAL_CLIENT_SECRET=your_paypal_client_secret
PAYPAL_ENVIRONMENT=sandbox
```

Never commit real credentials to source control.

## 🗄 Database

Payment Service uses PostgreSQL for persistent payment data.

The service owns its payment-related database tables.

Other microservices should access payment information through the Payment Service API rather than directly accessing its database.

## 🔁 Idempotency

Payment operations should be designed with idempotency in mind.

A payment request may be retried because of:

* Network failures
* Client retries
* Service timeouts
* Provider communication failures

The Payment Service should prevent the same logical payment request from accidentally creating multiple payments.

For example:

```text
Client
  │
  │ Payment Request + Idempotency Key
  ▼
Payment Service
  │
  ├── First request ──► Create payment
  │
  └── Retry ─────────► Return existing result
```

This becomes particularly important when integrating with real payment providers.

## 📌 Design Principles

### Provider Independence

The core payment domain should not be tightly coupled to Stripe, PayPal, or any single provider.

### Service Ownership

Payment Service owns payment-related data and business rules.

### API-Based Communication

Other services communicate with Payment Service through APIs rather than directly accessing its database.

### Deterministic Financial Operations

Payment authorization and execution are controlled by deterministic backend services.

### Secure Provider Integration

External provider credentials and sensitive payment information must be protected.

### Extensibility

Additional payment providers should be addable without requiring significant changes to the core payment domain.

## 🔮 Future Improvements

Potential future capabilities include:

* Additional payment providers
* Payment refunds
* Partial refunds
* Payment webhooks
* Payment reconciliation
* Retry handling
* Idempotency support
* Payment audit logs
* Fraud detection
* Payment notifications
* Multi-currency payments
* Recurring payments
* Payment method management

These capabilities will be introduced through additional user stories as the project evolves.

## 📚 Related Services

See the main **FinTech Core** repository for the complete platform architecture.

Related services include:

* Authentication Service
* Customer Service
* Transaction Service
* AI Service
* Frontend Application

## 📄 License

This project is licensed under the MIT License.

# Transaction Service

Transaction Service is a Spring Boot microservice within the **FinTech Core** platform responsible for managing users' financial transactions.

The service provides the backend capabilities required to record, retrieve, and manage financial activity while keeping transaction data and business rules under the control of the service.

It is designed to operate independently from the other FinTech Core services and communicate with them through APIs.

## 🎯 Purpose

The Transaction Service is responsible for the financial transaction domain of FinTech Core.

Its primary responsibilities include:

- Creating financial transactions
- Retrieving transaction information
- Maintaining transaction history
- Managing transaction status
- Supporting transaction categorization
- Providing transaction data to other services
- Providing authoritative transaction data for AI-powered financial analysis

The AI layer may analyze transaction data, but it does **not** become the source of truth for financial transactions.

```text
Customer
   │
   ▼
Frontend Application
   │
   ▼
API Gateway
   │
   ▼
Transaction Service
   │
   └── PostgreSQL
````

## 🏗 Architecture

Transaction Service follows a layered Spring Boot architecture:

```text
transaction-service
├── controller/
├── service/
│   └── impl/
├── repository/
├── entity/
├── dto/
├── mapper/
├── exception/
└── config/
```

### Controller Layer

Exposes REST endpoints for transaction-related operations.

### Service Layer

Contains transaction business logic and coordinates interactions between controllers and repositories.

### Repository Layer

Provides persistence operations for transaction data using Spring Data JPA.

### Entity Layer

Contains JPA entities representing transaction data stored in the database.

### DTO Layer

Contains request and response objects used by the REST API.

### Mapper Layer

Converts between entities and DTOs.

### Exception Layer

Contains domain-specific exceptions and error handling.

## 🛠 Technology Stack

* Java 21
* Spring Boot
* Spring Web
* Spring Data JPA
* Jakarta Validation
* PostgreSQL
* Maven
* Docker

## 💾 Transaction Data

A transaction represents a financial activity associated with a customer.

A transaction may contain information such as:

* Transaction ID
* Customer ID
* Amount
* Currency
* Transaction type
* Category
* Description
* Transaction status
* Transaction date
* Creation timestamp
* Update timestamp

The exact transaction model may evolve as additional business requirements are implemented.

## 📋 User Stories

The Transaction Service will be developed incrementally through user stories.

Examples include:

| ID      | User Story                                                                                                    | Priority |
| ------- | ------------------------------------------------------------------------------------------------------------- | -------- |
| TRX-001 | As a customer, I want to record a financial transaction so that my financial activity is tracked.             | P0       |
| TRX-002 | As a customer, I want to view my transactions so that I can review my financial activity.                     | P0       |
| TRX-003 | As a customer, I want transaction information validated so that invalid transactions cannot enter the system. | P0       |
| TRX-004 | As a customer, I want to categorize my transactions so that I can understand my spending.                     | P1       |
| TRX-005 | As a customer, I want to filter my transactions so that I can find specific financial activity.               | P1       |
| TRX-006 | As a customer, I want to view transaction details so that I can understand an individual transaction.         | P1       |

Additional user stories will be added as the financial platform evolves.

## 🔌 API

The Transaction Service exposes REST APIs for transaction management.

### Create Transaction

```http
POST /transactions
```

Creates a new transaction.

Example request:

```json
{
  "customerId": "bee5da62-25aa-4aeb-8678-e922c3549c30",
  "amount": 5000.00,
  "currency": "PHP",
  "type": "EXPENSE",
  "category": "FOOD",
  "description": "Dinner"
}
```

### Get Transaction

```http
GET /transactions/{id}
```

Retrieves a transaction using its unique identifier.

### Get Customer Transactions

```http
GET /customers/{customerId}/transactions
```

Retrieves transactions belonging to a customer.

### Update Transaction

```http
PUT /transactions/{id}
```

Updates transaction information where supported by the business rules.

### Delete Transaction

```http
DELETE /transactions/{id}
```

Deletes a transaction where deletion is permitted by the business rules.

> API endpoints will evolve as the transaction domain and business requirements become more clearly defined.

## 🔐 Data Ownership

Transaction Service is the authoritative owner of transaction data.

Other services may consume transaction information through APIs, but should not directly modify the Transaction Service database.

For example:

```text
AI Service
    │
    │ requests transaction data
    ▼
Transaction Service
    │
    ▼
Transaction Database
```

This separation is important because AI-generated analysis should not directly modify authoritative financial records.

## 🤖 AI Integration

Transaction Service is designed to support future AI-powered capabilities within FinTech Core.

Examples include:

* Spending analysis
* Spending categorization
* Recurring expense detection
* Anomaly detection
* Financial summaries
* Personalized financial insights

The architecture follows an important principle:

> **AI provides analysis and recommendations; backend services remain authoritative for financial data and business rules.**

For example, an AI assistant may answer:

> "You spent ₱18,500 on food this month."

The AI should obtain the underlying transaction information from the Transaction Service rather than maintaining its own authoritative copy of the financial records.

## 🧪 Testing

The service includes unit and integration tests.

Run the test suite with:

```bash
./mvnw test
```

Tests should cover areas including:

* Transaction creation
* Transaction retrieval
* Input validation
* Business rules
* Repository interactions
* Exception handling

## 🚀 Running Locally

### Prerequisites

* Java 21
* Maven
* PostgreSQL
* Docker (optional)

### Run with Maven

```bash
./mvnw spring-boot:run
```

The service runs on its configured port.

### Run with Docker

From the FinTech Core root directory:

```bash
docker compose up transaction-service
```

## 🔧 Configuration

Configuration is provided through Spring Boot configuration files and environment variables.

Typical configuration includes:

```text
Database URL
Database username
Database password
Server port
```

Sensitive credentials should not be committed to source control.

## 🗄 Database

Transaction Service uses PostgreSQL for persistent transaction storage.

The service owns its transaction-related database tables.

Other microservices should access transaction information through the Transaction Service API rather than directly accessing its database.

## 🔗 Relationship with Other Services

Transaction Service is part of the larger FinTech Core microservices architecture.

```text
                    ┌─────────────────┐
                    │    Frontend     │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │   API Gateway   │
                    └────────┬────────┘
                             │
             ┌───────────────┼───────────────┐
             ▼               ▼               ▼
       Auth Service    Customer Service   Transaction
                                            Service
                                               │
                                               ▼
                                          PostgreSQL
                                               ▲
                                               │
                                         AI Service
```

### Customer Service

Customer Service owns customer information.

Transaction Service uses the customer's identifier to associate financial transactions with the appropriate customer.

### Payment Service

Payment Service is responsible for payment processing and external payment-provider integrations.

Transactions resulting from payments may be recorded or referenced by Transaction Service.

### AI Service

AI Service consumes transaction information to provide financial analysis and conversational assistance.

## 📌 Design Principles

### Service Ownership

Each microservice owns its domain data.

### API-Based Communication

Services communicate through APIs rather than directly accessing another service's database.

### Deterministic Business Logic

Financial rules and transaction state are controlled by backend services.

### AI as an Assistant

AI assists with analysis and interaction but does not become the authoritative financial system.

### Extensibility

The transaction model and APIs should be designed so that additional transaction types, categories, payment sources, and financial capabilities can be added without unnecessary redesign.

## 📚 Related Services

See the main FinTech Core repository for the complete platform architecture and other microservices.

* Customer Service
* Authentication Service
* Payment Service
* AI Service
* Frontend Application

## 📄 License

This project is licensed under the MIT License.



The most important distinction in the new architecture is:

**Transaction Service ≠ Payment Service**



> **Transaction Service records and manages financial transactions, while Payment Service handles the actual payment processing and integrations with external payment providers.**

So, for example:

```text
Customer buys something for ₱5,000
             │
             ▼
       Payment Service
             │
       ┌─────┴─────┐
       ▼           ▼
    Stripe       PayPal
             │
             ▼
       Payment Result
             │
             ▼
     Transaction Service
             │
             ▼
      Financial Record
````


