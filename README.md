

# FinTech Core

FinTech Core is an **AI-powered personal finance and payment platform** designed to help individuals manage their financial activity, understand their spending, track financial goals, and make payments.

The project is being developed as a **microservices-based financial platform**, with traditional backend services remaining authoritative for financial data and business rules.

AI capabilities are designed to assist users with financial analysis and interaction without becoming the system of record.

> **Project status:** Active development

---

## 🎯 Product Vision

FinTech Core is intended to provide an end-user financial platform where customers can:

- Create and manage their financial profile
- Connect financial accounts
- View and analyze transactions
- Make payments
- Categorize spending
- Create financial goals
- Receive financial alerts
- Ask questions about their financial activity
- Use an AI financial assistant to understand their finances

Example interactions with the future AI financial assistant:

> "How much did I spend on food this month?"

> "Why did my spending increase this month?"

> "Can I afford to spend ₱5,000 this weekend?"

> "Show me my largest recurring expenses."

The AI layer is designed to **analyze and explain financial information**, while the underlying microservices remain responsible for maintaining financial state and enforcing business rules.

---

# 🏗 Architecture

FinTech Core is organized as a collection of independent services that communicate through APIs.

```text
                         ┌─────────────────────┐
                         │      Customer       │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │   Web Application   │
                         │       (React)       │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │     API Gateway     │
                         └──────────┬──────────┘
                                    │
                 ┌──────────────────┼──────────────────┐
                 │                  │                  │
                 ▼                  ▼                  ▼
        ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
        │ Auth Service   │ │Customer Service│ │Transaction     │
        │    FastAPI     │ │  Spring Boot   │ │Service         │
        └────────────────┘ └────────────────┘ │Spring Boot     │
                                               └───────┬────────┘
                                                       │
                                                       ▼
                                               ┌────────────────┐
                                               │Payment         │
                                               │Integrations    │
                                               │Stripe / PayPal │
                                               └────────────────┘

                         ┌─────────────────────┐
                         │     AI Service      │
                         │                     │
                         │ Financial Assistant │
                         │ Spending Analysis   │
                         │ Anomaly Detection   │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         Financial Data APIs
````

The architecture will evolve as additional capabilities are implemented.

---

# 📦 Services

The repository contains multiple independently developed applications.

| Service                | Technology         | Responsibility                                        |
| ---------------------- | ------------------ | ----------------------------------------------------- |
| `fastapi`              | FastAPI / Python   | Authentication and identity                           |
| `customer-service`     | Java / Spring Boot | Customer information and profile management           |
| `transaction-service`  | Java / Spring Boot | Financial transactions and payment-related operations |
| `transaction-frontend` | React              | User-facing web application                           |

Additional services, including AI capabilities and an API gateway, are planned as the platform evolves.

---

# 📚 Service Documentation

Each service maintains its own README containing its implementation-specific documentation.

### Customer Service

The Customer Service manages customer information within FinTech Core.

Documentation:

```text
customer-service/README.md
```

The service README contains information such as:

* Service responsibilities
* Technology stack
* API endpoints
* Data model
* Validation rules
* User stories
* Testing
* Local development
* Example API requests
* Known limitations

### Transaction Service

The Transaction Service manages financial transaction-related functionality.

Documentation:

```text
transaction-service/README.md
```

### Authentication Service

The authentication service is implemented using FastAPI.

Documentation:

```text
fastapi/README.md
```

### Frontend

The frontend provides the user-facing application.

Documentation:

```text
transaction-frontend/README.md
```

---

# 🌟 Current Capabilities

The platform currently contains the following functionality.

## Authentication

The authentication service provides:

* User registration
* User authentication
* JWT-based authentication
* Google OAuth integration
* Role-based access control
* Multi-tenant support

## Customer Management

The Customer Service is being developed to manage customer information.

Current functionality includes:

* Customer registration
* Customer information validation
* Customer retrieval
* Customer identification using UUIDs
* Optional business-provided customer numbers

Customer-specific implementation details are documented in:

```text
customer-service/README.md
```

## Transactions and Payments

The Transaction Service currently provides transaction and payment-related functionality, including integrations with external payment providers.

Supported payment providers currently include:

* PayPal
* Stripe

## Frontend

The React frontend provides the user-facing application for interacting with the platform.

---

# 🤖 AI Architecture

AI is a major part of the long-term product direction.

The AI layer is intended to provide capabilities such as:

### Financial Assistant

Allows customers to ask questions about their financial activity using natural language.

Examples:

```text
How much did I spend on food this month?

Why did my spending increase this month?

Can I afford to spend ₱5,000 this weekend?

Show me my largest recurring expenses.
```

### Spending Analysis

The AI layer can analyze financial data provided by authoritative backend services to identify:

* Spending patterns
* Category changes
* Recurring expenses
* Significant changes in spending
* Potential areas for savings

### Anomaly Detection

AI-assisted analysis can identify potentially unusual financial activity and surface it to the customer.

---

## 🔐 AI Does Not Own Financial State

A fundamental architectural principle of FinTech Core is:

> **AI assists with financial decisions and understanding; deterministic backend services remain authoritative.**

The AI service should not directly become the source of truth for:

* Account balances
* Customer records
* Transactions
* Payments
* Payment status
* Financial rules

Instead, AI capabilities consume information from authoritative backend services.

```text
Customer
   │
   ▼
AI Assistant
   │
   ▼
Financial APIs
   │
   ├── Customer Service
   ├── Transaction Service
   └── Payment Service
```

This separation is intended to make AI capabilities easier to evolve without making the underlying financial system dependent on an LLM.

---

# 💳 Payment Demonstration

The current deployment includes a working payment integration demonstration.

### Live Applications

**Frontend**

[https://fintech-core-frontend.vercel.app](https://fintech-core-frontend.vercel.app)

**Authentication Service**

[https://fintech-core-auth-service.vercel.app/docs](https://fintech-core-auth-service.vercel.app/docs)

**Transaction Service**

[https://transaction-service-dd9l.onrender.com](https://transaction-service-dd9l.onrender.com)

---

## Payment Flow

The current payment demonstration follows this flow:

```text
Register
   │
   ▼
Sign In
   │
   ▼
Payment Portal
   │
   ├── Stripe
   │
   └── PayPal
         │
         ▼
      Checkout
         │
         ▼
   Payment Completed
```

### 1. Register

Users can register through the Authentication Service API documentation.

**Registration Request** <img width="1399" height="670" alt="Register Request" src="https://github.com/user-attachments/assets/9b2e9017-6308-4f91-af5d-24a65bc1832d" /> **Registration Response** <img width="1386" height="480" alt="Registration Response" src="https://github.com/user-attachments/assets/2dde902a-7534-4fdd-b4ca-d79bdebb9436" />

### 2. Sign In

Users sign in through the FinTech Core frontend.

<img width="1227" height="711" alt="Login Screen" src="https://github.com/user-attachments/assets/ff0f53a6-88c7-4376-83d0-b8b80966b3f9" />

### 3. Select Payment Provider

The payment portal allows the user to select a supported payment provider.

<img width="1308" height="737" alt="Payment Portal" src="https://github.com/user-attachments/assets/87783b37-1798-4f35-9686-997495f0d7d0" />

### 4. Complete Checkout

The user completes the payment through the selected provider.

**PayPal Checkout** <img width="1362" height="725" alt="PayPal Checkout" src="https://github.com/user-attachments/assets/97ba4115-489a-43db-bee9-b483e8baa3f2" /> **Payment Details** <img width="1421" height="758" alt="PayPal Payment Details" src="https://github.com/user-attachments/assets/30b844f7-eaed-47cd-9fae-233ada04c591" />

### 5. Payment Confirmation

The application displays the result of the payment operation.

<img width="1053" height="239" alt="Payment Success" src="https://github.com/user-attachments/assets/5bcf5259-e909-4984-8d6d-fde08d56fa28" />

---

# 🧪 Testing

Each microservice maintains its own test suite.

For example:

```bash
cd customer-service
./mvnw test
```

Service-specific testing instructions are documented in each service's README.

The project uses automated tests at the service level while additional integration and end-to-end testing will be added as the platform develops.

---

# 🚀 Running the Project

## Prerequisites

Depending on which services are being developed, you may need:

* Docker
* Docker Compose
* Java 21
* Maven
* Python
* Node.js
* PostgreSQL

## Clone the Repository

```bash
git clone https://github.com/ryryanb/fintech_core.git
cd fintech_core
```

## Docker

The project uses Docker for containerized development.

```bash
docker compose up
```

Individual services can also be developed and tested independently.

Refer to each service's README for service-specific development instructions.

---

# 📁 Repository Structure

```text
fintech_core/
│
├── README.md
│
├── customer-service/
│   ├── README.md
│   └── ...
│
├── transaction-service/
│   ├── README.md
│   └── ...
│
├── fastapi/
│   ├── README.md
│   └── ...
│
├── transaction-frontend/
│   ├── README.md
│   └── ...
│
└── docker-compose.yml
```

The root README describes the **overall product and architecture**.

Individual service READMEs describe the **implementation of each component**.

---

# 🛠 Technology Stack

## Backend

* Java 21
* Spring Boot
* Spring Data JPA
* Hibernate
* Python
* FastAPI
* REST APIs

## Databases

* PostgreSQL

## Frontend

* React
* JavaScript

## Infrastructure

* Docker
* Docker Compose

## Payments

* PayPal
* Stripe

## AI

Planned AI capabilities include:

* LLM-powered financial assistance
* Financial data analysis
* Spending analysis
* Anomaly detection
* Retrieval-augmented generation
* Agent-based workflows

The AI technology stack will evolve as these capabilities are implemented.

---

# 🔒 Security

Security is an important architectural consideration for the platform.

Current and planned security capabilities include:

* JWT-based authentication
* OAuth integration
* Role-based access control
* HTTPS
* CORS configuration
* Secure handling of payment credentials
* Service-level authorization
* Separation of financial data from AI-generated responses

Sensitive credentials and environment-specific configuration should not be committed to the repository.

---

# 📈 Development Approach

FinTech Core is being developed incrementally as a portfolio project.

Development follows a **user-story-driven approach**, with functionality implemented and tested at the service level before expanding into additional platform capabilities.

For example:

```text
Customer Management
│
├── CUS-001  Register Customer
├── CUS-002  Validate Customer Information
├── CUS-003  Retrieve Customer
├── CUS-004  Update Customer
└── ...
```

Detailed user stories, acceptance criteria, implementation notes, and verification procedures are maintained with the relevant service rather than in this root README.

---

# 🗺 Roadmap

The platform will evolve through several stages.

### Phase 1 — Core Platform

* Authentication
* Customer management
* Transaction management
* Payment integration
* Basic frontend

### Phase 2 — Financial Management

* Financial account management
* Spending categorization
* Financial goals
* Transaction analysis
* Alerts

### Phase 3 — AI Financial Assistant

* Natural-language financial queries
* Spending analysis
* Financial summaries
* Recurring expense analysis

### Phase 4 — Advanced AI

* Anomaly detection
* RAG-based financial assistance
* Agent workflows
* Personalized financial insights

### Phase 5 — Platform Evolution

* API Gateway
* Improved service-to-service communication
* Event-driven architecture
* Improved observability
* Expanded security
* Production-oriented deployment

---

# 🎓 Portfolio Goals

FinTech Core is intended to demonstrate practical experience with:

* Java backend engineering
* Spring Boot
* REST API design
* Microservice architecture
* PostgreSQL
* Docker
* Distributed systems
* Authentication and authorization
* Payment integrations
* AI/LLM integration
* RAG
* AI agent workflows
* Automated testing
* API design
* Incremental product development

The project emphasizes **backend architecture and engineering**, while also providing a real end-user product rather than functioning only as a collection of backend demonstrations.

---

> **Architecture Evolution:** The original FinTech Core implementation contained both transaction management and Stripe/PayPal payment functionality within `transaction-service`. As part of the new architecture, payment processing is being separated into a dedicated `payment-service`, while `transaction-service` will focus exclusively on financial transaction management. This restructuring establishes clearer service boundaries and domain ownership.

# 📄 License

This project is licensed under the MIT License.

See the [LICENSE](LICENSE) file for details.

````

