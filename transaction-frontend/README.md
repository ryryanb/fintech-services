
# FinTech Core — Transaction Frontend

The Transaction Frontend is the web application for FinTech Core.

It provides a user-facing interface for interacting with the FinTech Core backend services. The frontend is implemented as a React application and is designed to evolve alongside the platform's microservices architecture.

The current application originated from the earlier FinTech Core implementation, where transaction and payment functionality were exposed through the transaction service.

As FinTech Core is migrated to the new architecture, frontend functionality will gradually be updated to interact with the dedicated backend services.

## Architecture

The frontend sits at the presentation layer of FinTech Core:

```text
                    Customer
                       │
                       ▼
              Transaction Frontend
                    (React)
                       │
                       ▼
                  API Gateway
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
     Auth Service  Customer Service  Transaction Service
                                      │
                                      ▼
                                Payment Service
````

Additional services, including AI-powered financial services, will be integrated as the platform evolves.

The frontend is responsible for presentation and user interaction.

Backend services remain responsible for business rules, financial data, validation, and transaction processing.

## Current Role

The frontend currently serves as the user interface for the existing FinTech Core functionality.

The application originated around payment and transaction workflows and is being retained as the frontend foundation while the backend architecture is being reorganized.

The new architecture separates responsibilities that were previously combined in the transaction service.

### Previous Architecture

```text
Frontend
   │
   ▼
Transaction Service
   ├── Transaction functionality
   ├── Payment functionality
   ├── Stripe integration
   └── PayPal integration
```

### New Architecture

```text
Frontend
   │
   ▼
API Gateway
   │
   ├── Auth Service
   ├── Customer Service
   ├── Transaction Service
   └── Payment Service
```

In the new architecture, Stripe and PayPal functionality belongs to the dedicated **Payment Service** rather than the Transaction Service.

This separation allows transaction management and payment processing to evolve independently.

## Technology Stack

* React
* JavaScript
* HTML/CSS
* Create React App
* npm
* Docker
* Nginx
* Vercel

## Project Structure

```text
transaction-frontend/
├── public/             # Static public assets
├── src/                # React application source code
├── Dockerfile          # Container configuration
├── nginx.conf          # Nginx configuration
├── vercel.json         # Vercel deployment configuration
├── package.json        # Node.js project configuration
├── package-lock.json   # Dependency lock file
└── README.md
```

## Getting Started

### Prerequisites

* Node.js
* npm

### Install Dependencies

```bash
npm install
```

### Start the Development Server

```bash
npm start
```

The application will be available at:

```text
http://localhost:3000
```

The development server automatically reloads when source files are modified.

## Testing

Run the frontend test suite with:

```bash
npm test
```

The project uses the testing infrastructure provided by Create React App.

## Production Build

Create an optimized production build with:

```bash
npm run build
```

The production files are generated in:

```text
build/
```

## Docker

The frontend can also be built and run as a Docker container.

Build the image:

```bash
docker build -t fintech-core-frontend .
```

Run the container:

```bash
docker run -p 3000:80 fintech-core-frontend
```

The application can then be accessed at:

```text
http://localhost:3000
```

## Deployment

The project includes Vercel configuration and can be deployed as a frontend application.

Production deployment:

```text
https://fintech-core-frontend.vercel.app
```

## Frontend Responsibilities

The frontend is responsible for:

* Presenting financial information to customers
* Collecting user input
* Calling backend APIs
* Displaying transaction information
* Displaying payment workflows
* Handling user interaction
* Presenting validation and error messages
* Providing a foundation for future personal-finance functionality

The frontend should **not** be responsible for implementing authoritative financial business rules.

For example, the frontend should not determine whether a payment is actually successful. Instead:

```text
Customer
   │
   ▼
Frontend
   │
   │ payment request
   ▼
Payment Service
   │
   ├── Validate request
   ├── Process payment
   ├── Communicate with payment provider
   └── Record payment result
   │
   ▼
Frontend
   │
   ▼
Display result
```

This keeps the backend services authoritative.

## Planned Frontend Evolution

As the new FinTech Core architecture is implemented, the frontend will gradually support additional customer-facing capabilities.

Planned areas include:

### Customer Management

* Customer registration
* Customer profile
* Profile updates
* Customer information display

### Financial Accounts

* Connected financial accounts
* Account balances
* Account information

### Transactions

* Transaction history
* Transaction details
* Spending categories
* Spending summaries

### Payments

* Payment initiation
* Payment status
* Payment history
* Payment provider interactions

### Financial Goals

* Create financial goals
* Track progress
* View goal history

### Alerts

* Financial alerts
* Transaction notifications
* Spending alerts

### AI Financial Assistant

The frontend will eventually provide an interface for interacting with the AI financial assistant.

Example interactions:

```text
"How much did I spend on food this month?"

"Why did my spending increase this month?"

"Can I afford to spend ₱5,000 this weekend?"

"Show me my largest recurring expenses."
```

The AI assistant will obtain financial information through backend services rather than directly accessing the database.

## Relationship with Backend Services

The frontend is intended to communicate with backend services through APIs.

```text
                    Frontend
                       │
                       ▼
                  API Gateway
                       │
        ┌──────────────┼──────────────┐
        │              │              │
        ▼              ▼              ▼
   Auth Service  Customer Service  Transaction Service
                                      │
                                      ▼
                                Payment Service
```

Each service owns its own business responsibilities.

For example:

| Responsibility          | Service              |
| ----------------------- | -------------------- |
| Authentication          | Auth Service         |
| Customer information    | Customer Service     |
| Financial transactions  | Transaction Service  |
| Payment processing      | Payment Service      |
| AI financial assistance | AI Service           |
| User interface          | Transaction Frontend |

## Important Architectural Note

This repository is named `transaction-frontend` because it originated from the earlier transaction/payment implementation of FinTech Core.

The name does **not** mean that the frontend will only support transactions.

As the platform evolves into an AI-powered personal finance and payment platform, the frontend will become the primary customer-facing application for capabilities provided by the various FinTech Core services.

The backend architecture is being separated so that each service has a clear responsibility.

In particular, payment-provider functionality that previously existed inside `transaction-service` is being moved into the dedicated `payment-service`.

## Development Approach

Frontend user stories will be implemented as the corresponding backend capabilities become available.

For example:

```text
CUS-001
Register Customer
       │
       ▼
Customer Service API
       │
       ▼
Frontend Registration Screen
```

This allows backend domain functionality and APIs to be established before building the corresponding user interface.

The frontend therefore evolves incrementally with the platform rather than attempting to implement the entire application at once.

## Related Services

FinTech Core consists of multiple services:

* `fastapi` — Authentication Service
* `customer-service` — Customer management
* `transaction-service` — Financial transaction management
* `payment-service` — Payment processing and payment-provider integrations
* `transaction-frontend` — Customer-facing React application

See the individual service README files for service-specific architecture, APIs, setup instructions, and testing information.

## License

This project is licensed under the MIT License.

