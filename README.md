# FinTech Services

FinTech Services is a production-oriented financial services platform under active development. The project is being built as a collection of Spring Boot microservices using Java 21, PostgreSQL, Docker, and REST APIs.

The project focuses on building a realistic financial-services backend with clear service boundaries, domain ownership, security, persistence, validation, testing, and incremental architectural evolution.

> **Project status:** Active development

---

## 🎯 Project Goals

The project is designed to demonstrate practical backend engineering and distributed-system design through a realistic financial-services domain.

The primary goals are to demonstrate:

* Java 21 backend development
* Spring Boot microservices
* REST API design
* PostgreSQL persistence
* Database migrations with Flyway
* Authentication and authorization
* Domain-driven service boundaries
* Input validation and business rules
* Automated testing
* Docker-based development
* Incremental architecture evolution

Future development will extend the platform with transaction processing, payment services, and AI-assisted financial capabilities.

---

# 🏗 Current Architecture

The current repository contains three Spring Boot services:

```text
                         FinTech Services
                                │
             ┌──────────────────┼──────────────────┐
             │                  │                  │
             ▼                  ▼                  ▼
      Auth Service       Customer Service     Account Service
      Spring Boot        Spring Boot          Spring Boot
             │                  │                  │
             ▼                  ▼                  ▼
        Auth Data        Customer Data        Account Data
             │                  │                  │
             └──────────────────┼──────────────────┘
                                │
                           PostgreSQL
```

Each service owns its own application logic and persistence model.

The architecture is intentionally being developed incrementally rather than introducing infrastructure before it is required.

---

# 📦 Implemented Services

## Auth Service

`auth-service`

Spring Boot service responsible for authentication and access control.

Current implementation includes:

* User registration
* User authentication
* Login
* JWT-based access tokens
* RSA-based JWT signing
* Spring Security integration
* Authentication-related persistence
* Request/response DTOs
* Validation and exception handling
* Security configuration
* Service-layer separation
* Automated tests

The service is organized into dedicated packages for controllers, DTOs, entities, repositories, services, security, configuration, and exceptions.

---

## Account Service

`account-service`

Spring Boot service responsible for financial account management.

Current implementation includes:

* Financial account creation
* Account persistence
* System-generated UUID identifiers
* Account-related DTOs
* Account domain entities
* Repository layer
* Service layer
* REST controller
* PostgreSQL persistence
* Docker-based development

The Account Service establishes the foundation for future financial-account functionality such as balances, account types, and account lifecycle management.

---

## Customer Service

`customer-service`

Spring Boot service responsible for customer information and customer-related business rules.

Current implementation includes:

* Customer registration
* Customer retrieval
* Customer information validation
* UUID-based customer identification
* Optional business customer numbers
* Customer status
* KYC status
* PostgreSQL persistence
* Jakarta Bean Validation
* JPA/Hibernate
* Unit testing with JUnit and Mockito
* Docker-based development

The service maintains ownership of customer information and does not own authentication or payment processing.

---

# 🧪 Testing

Testing is implemented at the individual service level.

Current services contain automated tests covering areas such as:

* Service-layer behavior
* Request validation
* Business rules
* Customer registration
* Error conditions

Tests can be executed using the Maven wrapper within each service:

```bash
cd customer-service
./mvnw test
```

The test strategy will expand as the platform evolves to include broader integration and end-to-end testing.

---

# 🗄 Technology Stack

## Backend

* Java 21
* Spring Boot
* Spring Web
* Spring Data JPA
* Hibernate
* Jakarta Bean Validation
* Spring Security
* Maven

## Database

* PostgreSQL
* Flyway

## Testing

* JUnit
* Mockito
* Spring testing support

## Infrastructure

* Docker
* Docker Compose

## API

* REST
* JSON
* JWT

---

# 🔐 Security

Security is a core architectural concern of the platform.

Current security work includes:

* JWT-based authentication
* RSA-based token signing
* Spring Security
* Authentication and authorization boundaries
* Request validation
* Separation of authentication responsibilities from business services

Security will continue to evolve as additional services are introduced.

Planned security improvements include:

* Service-to-service authorization
* More granular authorization policies
* Improved secret management
* Security-focused integration testing
* Additional production-hardening measures

---

# 🚧 Planned Services

The following capabilities are **planned and are not currently represented as completed services in the repository**.

## Transaction Service

A dedicated transaction service will own financial transaction processing.

Planned responsibilities include:

* Transaction creation
* Transaction retrieval
* Transaction lifecycle management
* Transaction validation
* Transaction history
* Transaction categorization
* Transaction-related business rules

The transaction domain will remain separate from payment-provider integration.

---

## Payment Service

A dedicated payment service is planned to isolate payment processing from transaction management.

Planned responsibilities include:

* Payment initiation
* Payment status management
* Payment-provider integration
* Payment lifecycle handling
* Provider-specific adapters
* Payment failure handling
* Idempotent payment operations

Potential payment-provider integrations include Stripe and PayPal.

---

# 🤖 Planned AI Capabilities

AI is a long-term extension of the platform rather than a replacement for the deterministic financial services.

The planned AI architecture follows an important principle:

> **AI assists with financial analysis and interaction; authoritative backend services remain the source of truth for financial state.**

Planned capabilities include:

### Financial Assistant

Natural-language interaction with financial information.

Examples:

```text
How much did I spend on food this month?

Why did my spending increase this month?

Show me my largest recurring expenses.

Can I afford to spend ₱5,000 this weekend?
```

### Spending Analysis

Potential capabilities include:

* Spending-pattern analysis
* Category analysis
* Recurring-expense identification
* Financial summaries
* Trend analysis

### Anomaly Detection

AI-assisted analysis may eventually identify potentially unusual financial activity and surface it to users.

### RAG

Retrieval-augmented generation may be introduced where it provides meaningful value, particularly for combining financial data with approved financial knowledge and contextual information.

### Agent Workflows

Agent-based workflows may eventually be introduced for selected financial tasks.

These capabilities remain **planned** and are not currently part of the implemented platform.

---

# 🗺 Development Roadmap

## Phase 1 — Core Services

**Current**

* [x] Authentication Service
* [x] Customer Service
* [x] Account Service
* [x] PostgreSQL persistence
* [x] REST APIs
* [x] JWT authentication
* [x] Service-level testing
* [x] Docker-based development

## Phase 2 — Financial Operations

**Planned**

* [ ] Transaction Service
* [ ] Transaction lifecycle
* [ ] Transaction history
* [ ] Transaction categorization
* [ ] Payment Service
* [ ] Payment-provider integration
* [ ] Payment idempotency
* [ ] Failure handling

## Phase 3 — Platform Hardening

**Planned**

* [ ] Integration testing
* [ ] End-to-end testing
* [ ] Service-to-service authorization
* [ ] Centralized configuration
* [ ] Structured logging
* [ ] Observability
* [ ] CI/CD
* [ ] Improved security hardening
* [ ] Production-oriented deployment

## Phase 4 — AI-Assisted Financial Services

**Planned**

* [ ] AI financial assistant
* [ ] Financial data analysis
* [ ] Spending analysis
* [ ] Financial summaries
* [ ] Anomaly detection
* [ ] RAG
* [ ] Selected agent workflows

## Phase 5 — Distributed Platform Evolution

**Planned**

* [ ] API Gateway
* [ ] Event-driven communication where justified
* [ ] Asynchronous processing
* [ ] Improved resilience
* [ ] Expanded observability
* [ ] Production-oriented infrastructure

---

# 📁 Repository Structure

```text
fintech-services/
│
├── account-service/
│   ├── src/
│   ├── Dockerfile
│   ├── compose.yaml
│   └── pom.xml
│
├── auth-service/
│   ├── src/
│   ├── Dockerfile
│   ├── compose.yml
│   └── pom.xml
│
├── customer-service/
│   ├── src/
│   ├── README.md
│   ├── Dockerfile
│   ├── compose.yaml
│   └── pom.xml
│
├── docs/
│   └── images/
│
├── docker-compose.yml
├── .gitignore
└── README.md
```

Each service is developed as an independently deployable Spring Boot application with its own source code, configuration, build configuration, and persistence responsibilities.

---

# 🔄 Architecture Evolution

FinTech Services is intentionally being developed through incremental architectural evolution.

The previous FinTech Core implementation contained legacy services and combined multiple responsibilities, including authentication and transaction/payment functionality.

The current repository represents a cleaner architecture centered on Spring Boot services with explicit domain boundaries.

The legacy FastAPI authentication service and previous transaction service are no longer part of the current repository structure.

Their historical implementation remains available in the project's Git history, preserving the evolution of the architecture without making legacy services part of the current system.

The next architectural step is to introduce dedicated transaction and payment services rather than recreating the previous combined transaction/payment boundary.

---

# 🧭 Engineering Principles

The project follows several architectural principles:

### 1. Clear Service Ownership

Each service should have a well-defined domain responsibility.

### 2. Authoritative Financial State

Financial state must be maintained by deterministic backend services rather than AI systems.

### 3. Incremental Complexity

Infrastructure and distributed-system patterns should be introduced when they solve a real architectural problem.

### 4. Testable Business Logic

Business rules should be isolated from infrastructure concerns and covered by automated tests.

### 5. Secure by Design

Authentication, authorization, secret management, and service boundaries are treated as architectural concerns rather than features added at the end.

### 6. Evolution Over Perfection

The platform is developed incrementally, with architectural decisions evolving as the domain and requirements become clearer.

---

# 🎓 Portfolio Purpose

FinTech Services is a portfolio project demonstrating practical software-engineering skills in a realistic financial-services domain.

The project emphasizes:

* Java backend engineering
* Spring Boot
* REST API design
* Microservice architecture
* PostgreSQL
* Security and authentication
* Domain modeling
* Automated testing
* Docker
* Distributed-system concepts
* Architecture evolution
* AI-assisted application design

The goal is not to simulate a complete banking system. The goal is to demonstrate how a backend engineer designs, implements, tests, secures, and evolves a multi-service application around realistic business requirements.

---

## 📄 License

This project is licensed under the MIT License.

See the `LICENSE` file for details.
