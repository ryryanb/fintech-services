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
