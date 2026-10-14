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