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
hints, a show/hide password control, a remember-me checkbox, and a password
recovery link placeholder. **Create account** returns to the registration form.

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
