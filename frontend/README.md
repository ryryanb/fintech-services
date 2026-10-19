# Fintech Services Frontend

React, TypeScript, and Vite frontend for the Fintech Services application.

## Requirements

- Node.js 20.19+ or 22.12+
- npm

## Run locally

From the repository root:

```bash
cd frontend
npm install
npm run dev
```

Vite serves the app at http://localhost:5173 by default.

## Available scripts

- `npm run dev` — start the local development server
- `npm run typecheck` — run the TypeScript project checks
- `npm run build` — type-check and create a production build in `dist/`
- `npm run preview` — preview the production build locally

## FE-001: Display Registration Form

The initial screen includes required first name, last name, email address, and password fields. Field constraints mirror the backend `RegisterRequest` DTO: valid email, first and last names up to 100 characters, and a password between 8 and 100 characters. The password can be shown or hidden.

This story is limited to displaying the form. The submit interaction gives a clear informational message and does **not** call the registration API or create an account. API integration, loading/error handling, and the sign-in screen should be added in their own stories.

## Backend integration

The Auth Service registration endpoint is `POST /auth/register`. Configure a development proxy or an environment-based API URL in a follow-up integration story; this initial screen intentionally has no backend dependency.


## FE-003: Register Account

The registration form submits the validated fields to the Auth Service using
`POST /auth/register`. During local development, Vite proxies requests from
`/api/*` to `http://localhost:8086/*`, so the frontend calls
`/api/auth/register` without requiring browser CORS configuration.

Run the frontend with `npm run dev` and ensure the Auth Service and Customer
Service, including their PostgreSQL databases, are running. Registration creates
both the authentication user and the associated customer record. A duplicate
email is shown as a user-friendly error; network and server errors can be retried.

For deployment, configure the hosting reverse proxy to route `/api/*` to the
Auth Service and strip the `/api` prefix, or adjust the frontend API URL and
deployment configuration accordingly.


## FE-005: Display Login Form

Selecting **Sign in** on the registration screen opens a dedicated login form.
The form includes required email and password inputs, appropriate autocomplete
hints, a show/hide password control, and a password recovery link placeholder.
**Create account** returns to the registration form.

This story implements the login form UI only. Submitting the form displays an
informational message; it does not authenticate credentials or create a session.
Authentication and password recovery are separate follow-up stories.

### Verify FE-005

1. Run `npm install` and `npm run dev` from `frontend/`.
2. Confirm the registration screen appears at `http://localhost:5173`.
3. Select **Sign in** and verify the login form is displayed.
4. Verify email and password are required, the password visibility control works,
   and the **Create account** link returns to registration.
5. Submit populated fields and confirm the UI explains that authentication is not
   implemented yet.
6. Run `npm run typecheck` and `npm run build`.


## FE-006: Authenticate User

The login form sends credentials to `POST /api/auth/login`, which the local
Vite proxy forwards to the Auth Service's `POST /auth/login` endpoint. On
success, the UI validates the response shape, clears the password field, and
shows a signed-in state with token type and expiry. Invalid credentials and
service errors are presented without exposing backend internals.

The access token is held by the dedicated `authSession` module in JavaScript
memory only; it is not written to localStorage, sessionStorage, cookies, URLs,
or logs, and is not passed through React component props/state. Signing out
clears the token. Refreshing or closing the page clears this client-side session,
so the user must sign in again. This limits accidental persistence and exposure,
but an in-memory token remains accessible to JavaScript executing in the page;
preventing XSS remains essential. FE-008 adds a protected
`/profile` route: direct unauthenticated visits redirect to `/login`, and the
profile component is rendered only when the in-memory session exists. This is
client-side navigation protection, not a substitute for backend authorization.

### Verify FE-006

1. Start the Auth Service and its PostgreSQL database, then run the frontend
   with `npm run dev` from `frontend/`.
2. Open `http://localhost:5173` and select **Sign in**.
3. Enter the email and password for a registered, active account. Verify a
   successful login shows the signed-in state and token expiry.
4. Select **Sign out** and verify the login form returns.
5. Try an incorrect password and an unknown email; both should show the same
   invalid-credentials message.
6. Stop the Auth Service and retry to verify the connection-error message.
7. Run `npm run typecheck` and `npm run build`.


## FE-013: Display User Profile

After a successful login, the frontend loads the signed-in user's customer
profile from `GET /customers/by-user/{userId}`. It reads the JWT subject
(`sub`) to identify the user and sends the token in the Authorization header.
The Vite development proxy routes `/api/customers/*` to Customer Service on
port 8082; authentication requests continue to route to Auth Service on port
8086.

The profile displays the user's name, email, status, and customer number when
available. It includes loading, unavailable/error, and retry states, and a
sign-out action. The access token is retrieved from the in-memory `authSession` module for the
request and is not persisted by the frontend.

**Security note:** The frontend sends the bearer token, but the existing
Customer Service `GET /customers/by-user/{userId}` endpoint must enforce JWT
authentication and ensure the requested user ID matches the authenticated
token subject (or use the subject directly). A frontend-supplied user ID is
not an authorization boundary. Do not deploy profile data access until this
server-side authorization is enforced.

### Verify FE-013

1. Start the Auth Service and Customer Service with their PostgreSQL databases.
2. From `frontend/`, run `npm ci`, `npm run typecheck`, `npm run build`,
   then `npm run dev`.
3. Sign in with a registered account and verify the profile shows that user's
   name, email, status, and customer number when returned by the API.
4. Confirm the browser Network panel shows a bearer Authorization header on
   `GET /api/customers/by-user/{userId}`; do not copy or share the token.
5. Stop Customer Service and verify the page shows a friendly error and retry
   option. Restart it and verify the retry works.
6. Verify sign out returns to the login screen.
7. Before deployment, confirm Customer Service rejects missing/invalid tokens
   and prevents one user's token from reading another user's profile.


## FE-007: Store Access Token Securely

The frontend stores the access token in a dedicated in-memory module rather than
React component state or props. The token is never written to localStorage,
sessionStorage, cookies, URLs, or logs. The profile request obtains the token
only when it needs to construct the Authorization header. Signing out clears
the in-memory token. A page reload or closing the tab clears the session and
requires another sign-in; the UI does not offer a misleading "Remember me"
option.

This is a client-side exposure reduction, not an XSS defense. Any malicious
JavaScript executing in the page could still access a JavaScript-memory token.
Use a strict Content Security Policy, avoid unsafe script injection, and keep
dependencies updated. An HttpOnly, Secure, SameSite cookie-based session would
require a coordinated backend change and CSRF protections; it is not implied
by this frontend-only story.

### Verify FE-007

1. Sign in and verify profile requests include the bearer token in the
   Authorization header; do not copy or share the token.
2. Inspect application storage and confirm the token is absent from localStorage,
   sessionStorage, and cookies.
3. Sign out and verify the in-memory token is cleared and the profile is no
   longer rendered.
4. Refresh `/profile` and verify the app requires sign-in again.
5. Run `npm run typecheck` and `npm run build` from `frontend/`.

## FE-008: Protect Authenticated Routes

- `/` displays the public registration page.
- `/login` displays the public sign-in page.
- `/profile` is protected and renders the profile only when a successful login has established an in-memory session.
- Visiting `/profile` without a session redirects to `/login`; signing out clears the session and returns to `/login`.
- Refreshing clears the in-memory session, so `/profile` returns to the sign-in screen.

### Verify FE-008

1. Open `http://localhost:5173/profile` in a signed-out browser. Confirm the sign-in screen appears and the URL changes to `/login`.
2. Attempt to open `/profile` again while signed out. Confirm the profile content never appears.
3. Sign in with a valid account. Confirm the URL becomes `/profile` and the profile page loads.
4. Refresh `/profile`. Confirm the session is gone and the app returns to sign-in.
5. Sign in again, select **Sign out**, and confirm the URL becomes `/login` and profile data is no longer rendered.
6. Run `npm run typecheck` and `npm run build` from `frontend/`.

**Security boundary:** A React route guard only controls what the browser renders. APIs that return customer or financial data must independently validate the bearer token and authorize access to the requested user's data.

## FE-022: View Account Details

After signing in, select **Financial accounts** from the profile page to view accounts associated with the customer ID in the access token. The frontend requests `GET /api/accounts` through the Vite proxy to Account Service on port `8083`. The service derives customer identity from the validated bearer token; a customer ID supplied by the browser is not used to choose the result scope. Selecting **View details** requests `GET /api/accounts/{accountId}` and displays the account name, type, institution, currency, current balance, status, and account ID.

The screen includes loading, empty, retry, and friendly error states. The bearer token is read from the in-memory `authSession` module and is not persisted or logged. Account Service must independently validate the JWT and enforce customer ownership for both the list and detail endpoints; the customer ID in a decoded browser token is only used to form the request, not treated as an authorization boundary.

### Verify FE-022

1. Start Auth Service, Account Service, and their PostgreSQL databases, then run the frontend from `frontend/` with `npm run dev`.
2. Sign in with a user whose JWT contains a valid `customerId` claim.
3. Open **Financial accounts** from the profile and confirm the list displays the account name, institution/type, balance, currency, and status.
4. Select **View details** and confirm the account details are fetched from `GET /accounts/{accountId}`.
5. Confirm empty-account, service-unavailable, unauthorized, and not-found states show a useful message and retry where appropriate.
6. Verify an account owned by another customer cannot be retrieved using the signed-in user's token; this must be enforced by Account Service, not the frontend.
7. Run `npm run typecheck` and `npm run build`.


## FE-028: Display Transactions

After signing in, select **Transactions** from the profile page. The frontend loads the authenticated customer's accounts through `GET /api/accounts`, then requests `GET /api/transactions?accountId={accountId}` for each returned account. It combines the results into one newest-first activity list with merchant/description, category, account, transaction date, status, and signed amount.

The view supports account filtering, text search, refresh, loading and retry states, and a helpful empty state. Credits are displayed with a plus sign and debits with a minus sign. The Vite proxy forwards `/api/transactions` to Transaction Service on port `8087`.

**Security boundary:** The browser only requests transactions for accounts returned by Account Service, but that is not an authorization guarantee. Transaction Service must validate the bearer token and enforce account ownership on `GET /transactions?accountId=...`. Do not rely on a customer ID or account ID supplied by the browser to authorize access.

### Verify FE-028

1. Start Auth, Customer, Account, and Transaction Services with their databases.
2. Run `npm ci`, `npm run typecheck`, `npm run build`, then `npm run dev` from `frontend/`.
3. Sign in and select **Transactions** from the profile page.
4. Confirm transactions from all of the signed-in customer's accounts appear newest first with the correct amount, currency, direction, status, and date.
5. Filter by account and search by merchant, category, or description.
6. Verify the empty-history state, refresh action, and friendly error/retry states.
7. Test that requesting another customer's account ID directly from the Transaction Service is rejected by backend authorization.


## FE-030: Record Transaction

From the signed-in profile, open **Transactions**, then choose **Record transaction**. The form loads active accounts from the authenticated Account Service and uses the selected account's currency. It submits the transaction to `POST /api/transactions`, proxied to Transaction Service on port `8087`.

The form captures account, type, direction, positive amount, category, optional merchant/payee and description, and transaction date/time. It validates the amount and text limits before submitting and provides loading, validation, service-error, and success states. The Transaction Service assigns the saved transaction ID and status. A successful record can then be checked in transaction history.

**Important:** This story records a financial activity entry in the application. It does not execute a real bank transfer, debit a bank account, or update the account's balance. The backend must continue to validate the bearer token and enforce ownership of the selected account; client-side account filtering is not an authorization control.

### Verify FE-030

1. Start Auth, Customer, Account, and Transaction Services with their databases.
2. Run `npm ci`, `npm run typecheck`, and `npm run build` from `frontend/`, then start `npm run dev`.
3. Sign in, navigate to **Transactions**, and choose **Record transaction**.
4. Record a valid entry for an active account; verify the success message and that it appears in transaction history.
5. Verify missing/invalid amounts, optional fields, category and direction selections, and date/time.
6. Verify no-active-account, unauthenticated, forbidden-account, validation-error, and service-unavailable cases.
7. Confirm that the account balance is unchanged because this feature records history only.

## ACC-002A: View Own Financial Accounts

The Account Service derives the customer ID for `GET /accounts` from the validated bearer token. The endpoint no longer accepts a customer ID as a query parameter, so clients cannot select another customer's account list by changing a request parameter. The frontend sends the access token and requests `/api/accounts` without decoding or supplying customer identity. Account detail and balance endpoints also scope database lookups by the authenticated customer ID.

### Verify ACC-002A

1. Sign in as customer A and request `GET /accounts` with customer A's bearer token. Confirm that only customer A's accounts are returned.
2. Add a `customerId` query parameter containing customer B's ID to the same request. Confirm the response still contains only customer A's accounts.
3. Request an account detail or balance using an account ID belonging to customer B. Confirm the service does not return that account's data.
4. Call `GET /accounts` without a valid bearer token. Confirm access is rejected by Spring Security.


## SEC-001: Isolate Customer Financial Data

Customer identity and ownership are enforced by backend services; frontend routing and decoded client-side claims are not authorization boundaries.

- **Account Service:** requires an authenticated JWT, derives the customer ID from its `customerId` claim for account listing, and scopes individual account and balance lookups by both account ID and authenticated customer ID. Account creation rejects a request whose customer ID does not match the authenticated customer.
- **Transaction Service:** validates account ownership with Account Service before listing transactions or creating a transaction. For transaction-by-ID reads, it verifies ownership of the transaction's associated account before returning the transaction.
- **Customer Service:** checks authenticated user/customer identity for customer profile and address operations; customer listing and search require the administrator role.
- **Failure behavior:** missing or invalid authentication is rejected by Spring Security. Requests for data outside the authenticated customer's scope are denied or return not found, without returning the protected financial data.

### Verify SEC-001

1. Run backend tests from the repository root with `./mvnw test` (or run the relevant service's Maven tests).
2. Confirm the Account Service controller test verifies that a client-supplied `customerId` cannot change the account-list scope.
3. Confirm Transaction Service authorization tests allow the owning customer and reject another customer's account, unverifiable ownership, and transactions linked to another customer's account.
4. Using two test users, sign in as user A and try to list user B's accounts, retrieve user B's account/balance by ID, list transactions for user B's account, and retrieve a transaction belonging to user B. None of these requests should return user B's financial data.
5. Attempt to create an account for user B while authenticated as user A, and attempt to create a transaction against user B's account. Both operations must be rejected and must not persist data.
6. Repeat with no token and with an invalid/expired token; protected endpoints must reject the requests.
7. Do not use a decoded token, hidden frontend controls, or a customer/account ID supplied by the client as proof of ownership.


## FE-020: Add Financial Account

The authenticated **Financial accounts** page includes an **Add account** action that opens the protected `/accounts/new` route. The form supports an account name, account type, three-letter currency code, opening balance, and optional financial institution name.

On submission, the frontend sends `POST /api/accounts` with the bearer token and the fields expected by Account Service's `CreateFinancialAccountRequest`. The customer ID is read from the token to populate the request, but it is not treated as an authorization boundary: Account Service must validate the token and reject a customer ID that does not match the authenticated customer. The page returns to the accounts list only after the API confirms successful creation. Validation, authorization, server, and network errors are shown without reporting a false success.

The form creates a tracking record in this application only. It does not open a real bank account, connect to a financial institution, or transfer money.

### Verify FE-020

1. Start the Auth Service, Account Service, and their required databases; run the frontend using `npm run dev` from `frontend/`.
2. Sign in and navigate to **Financial accounts**. Verify the **Add account** action is visible.
3. Select **Add account** and verify the form is displayed at `/accounts/new`. Visiting this route without a signed-in session must redirect to sign-in.
4. Submit a valid bank account with a name, account type, currency (for example `PHP`), opening balance, and optional institution. Verify `POST /api/accounts` is sent with the bearer token and the created account appears in the refreshed account list.
5. Verify invalid names, malformed currency codes, negative/non-numeric balances, and overly long institution names are rejected in the UI.
6. Verify a 401/403 response or network/server failure displays an error and does not navigate away as if creation succeeded.
7. Verify cancelling returns to the account list without making an API request.
8. Test with two users: a user must not be able to create an account for another customer's ID. Account Service must reject this request even if a client manually changes the request body.
9. Run `npm run typecheck` and `npm run build`.


## FE-029: Display Transaction Details

Select a transaction title or its **Details** action in the transaction history to open the protected `/transactions/{transactionId}` route. The page retrieves the individual transaction using `GET /api/transactions/{transactionId}` with the authenticated bearer token and displays its ID, type, direction, amount, currency, merchant/payee, category, description, status, and date/time. It also attempts to retrieve the linked account's name and institution from `GET /api/accounts/{accountId}`; the transaction details remain visible if supplementary account metadata is unavailable.

The page includes loading, retry, not-found, unauthorized, and service-error states, responsive detail fields, and navigation back to transaction history or to accounts. Direct access to the detail route is protected by the frontend route guard. This client-side guard is only a usability measure: Transaction Service must independently authenticate the bearer token and authorize access to the transaction's associated account before returning data.

### Verify FE-029

1. Start the Auth Service, Account Service, and Transaction Service, including their required databases; run the frontend with `npm run dev`.
2. Sign in and open **Transactions**. Verify each transaction provides a clickable activity title and a **Details** action.
3. Open a transaction and verify the page shows its amount, currency, direction, type, category, merchant/payee, description, status, date/time, transaction ID, and linked account metadata when available.
4. Verify the amount is formatted using the transaction currency and credits/debits are visually distinguished.
5. Verify **Back to transactions**, **Accounts**, and **Sign out** work.
6. Open `/transactions/{transactionId}` directly while signed out and confirm the user is redirected to sign-in. Sign in and open a valid transaction URL to verify it loads.
7. Test a nonexistent transaction ID and a transaction belonging to another customer. The backend must return an appropriate not-found/forbidden response without leaking transaction data; the frontend should display a safe error.
8. Stop Transaction Service and verify the UI displays a helpful error and retry action. If account lookup fails while the transaction lookup succeeds, transaction details should still display.
9. Run `npm run typecheck` and `npm run build` from `frontend/`.


## SPEND-001: Categorize Transactions

New transaction entries can be categorized automatically or assigned a category explicitly. When **Auto-categorize from merchant and description** is selected on the Record transaction form, Transaction Service applies deterministic keyword rules to the merchant, description, transaction type, and direction. Recognized examples include supermarkets/groceries, restaurants and food delivery, public transport/rides, utilities, housing, healthcare, travel, entertainment subscriptions, salary/payroll, bank fees, shopping, and transfers. If the rules do not have enough information, the transaction is saved as `OTHER`. A category selected manually by the user takes precedence over the automatic rules.

The saved category is returned in the transaction response and shown in transaction history and transaction details. Transaction history also has a category filter so users can focus on one spending/income category at a time. The categorizer is deterministic and local to Transaction Service; it does not call an external AI service or share transaction data with a third party. Keyword matches are heuristic, so users should review suggestions and select a category manually when a merchant is ambiguous.

### Verify SPEND-001

1. Start Transaction Service and its database, along with Auth Service, Account Service, and the frontend.
2. Open **Record transaction** and leave Category on **Auto-categorize from merchant and description**.
3. Record a payment with merchant `SM Supermarket` and description `Weekly groceries`; verify the saved category is **Groceries**.
4. Try merchant `Meralco` and description `Monthly electric bill`; verify **Utilities**. Try `Netflix` to verify **Entertainment**, and a transaction of type **Transfer** to verify **Transfer**.
5. Record a transaction with an unrecognized merchant/description and verify it falls back to **Other**.
6. Repeat a recognizable transaction after manually selecting a different category and verify the manual selection is preserved.
7. Open transaction history, use the **Category** filter, and verify only transactions in the selected category remain visible. Clear filters and verify the full list returns.
8. Open a transaction's details and verify the saved category is displayed there.
9. Run `npm run typecheck` and `npm run build` in `frontend/`, and run the Transaction Service tests with `./mvnw -pl transaction-service test` from the repository root (or the project's documented Maven wrapper command).


## SPEND-004: View Spending by Category

The Transactions page includes a spending overview above transaction history. It summarizes debit transactions by their saved category, with each category showing its total, share of spending, and transaction count. The period selector supports **This month**, **Last 30 days**, and **All time**; the selected account filter also applies to the summary. Transfers and salary-category entries are excluded from spending totals. When accounts use different currencies, totals are calculated and displayed separately per currency rather than adding incomparable amounts together. Categories are based on the category stored on each transaction, including the deterministic suggestions from SPEND-001.

### Verify SPEND-004

1. Sign in and open **Transactions**.
2. Record several debit transactions in different categories, ideally with different amounts and dates.
3. Confirm **Spending by category** shows a total and a category row for each category represented by eligible transactions. Check that percentages are based on the total for that currency and add up to approximately 100%.
4. Change the period between **This month**, **Last 30 days**, and **All time** and confirm the summary changes to match the selected date range.
5. Select a single account in the existing account filter and confirm the summary only includes that account.
6. Record a credit/income transaction and a transfer; verify neither increases spending totals.
7. If transactions exist in more than one currency, verify each currency has a separate total.
8. Verify the empty summary message when no eligible expenses exist in the selected period.
9. Run `npm run typecheck` and `npm run build` from `frontend/`.


## SPEND-003: Create Spending Categories

Open **Spending categories** from Transactions or Record transaction to create and manage custom category labels. Categories are scoped to the authenticated customer. Names are trimmed, whitespace is normalized, limited to 60 characters, and checked against built-in and existing categories. Custom categories are selectable when recording transactions and appear in history, transaction details, and spending summaries. Deleting a custom category prevents future use but leaves existing transaction labels intact.

### Verify SPEND-003

1. Sign in and open **Spending categories** from Transactions or Record transaction.
2. Create a category such as `Pets`; verify it appears in the list.
3. Try a blank name, a name longer than 60 characters, a built-in name such as `Groceries`, and a duplicate; verify rejection.
4. Open Record transaction and verify the custom category appears in the category selector.
5. Record a transaction with the custom category and verify the label appears in history, details, and SPEND-004 summary.
6. Delete the custom category; verify it no longer appears in the selector while existing transactions retain their saved label.
7. Sign in as another customer and verify the first customer's custom categories are not listed or accepted for new transactions.
8. Run `npm run typecheck` and `npm run build` in `frontend/`, and run Transaction Service tests.


## FE-014: Edit User Profile

From the authenticated profile page, select **Edit profile** to update first name, last name, and email address. The form validates required fields and email format, shows save errors, and refreshes the displayed profile after a successful save. Updates use the authenticated Customer Service endpoint `PUT /customers/{customerId}`; the backend enforces customer ownership, email uniqueness, and the existing rule that email cannot change after KYC verification.

### Verify FE-014

1. Sign in and open the profile page.
2. Select **Edit profile**, change the first or last name, and save. Confirm the updated value appears in the profile.
3. Try blank names and an invalid email address; confirm validation prevents submission.
4. Try an email address already used by another customer; confirm the save fails and the original profile remains visible.
5. For a KYC-verified customer, try changing the email address; confirm the backend rejects it and the page displays the error.
6. Cancel an edit and verify unsaved changes are discarded.
7. Sign in as another customer and verify the API does not allow updating the first customer's profile.
