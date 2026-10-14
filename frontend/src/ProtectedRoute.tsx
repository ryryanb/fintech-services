import { useEffect, type ReactNode } from 'react';

type ProtectedRouteProps = {
  isAuthenticated: boolean;
  children: ReactNode;
  redirectTo?: string;
};

/**
 * Prevents protected content from rendering without an authenticated session.
 * The access token is intentionally kept in memory, so a page refresh requires
 * the user to sign in again.
 */
export default function ProtectedRoute({
  isAuthenticated,
  children,
  redirectTo = '/login',
}: ProtectedRouteProps) {
  useEffect(() => {
    if (!isAuthenticated && window.location.pathname !== redirectTo) {
      window.history.replaceState(null, '', redirectTo);
      window.dispatchEvent(new PopStateEvent('popstate'));
    }
  }, [isAuthenticated, redirectTo]);

  if (!isAuthenticated) return null;

  return <>{children}</>;
}
