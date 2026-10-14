/**
 * Holds the access token only for the lifetime of this JavaScript application.
 *
 * Do not add localStorage, sessionStorage, cookies, URL parameters, or logging
 * here. A page reload clears this value and requires the user to sign in again.
 *
 * This reduces accidental persistence and exposure; it does not protect the
 * token from malicious JavaScript running in the page (for example, XSS).
 */
let accessToken: string | null = null;

export function setAccessToken(token: string): void {
  accessToken = token;
}

export function getAccessToken(): string | null {
  return accessToken;
}

export function clearAccessToken(): void {
  accessToken = null;
}
