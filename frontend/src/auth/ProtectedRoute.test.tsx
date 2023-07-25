import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { renderWithProviders } from '../test/renderWithProviders';
import ProtectedRoute from './ProtectedRoute';
import { tokenStorage } from './tokenStorage';
import type { RoleName } from '../api/types';

function renderAt(route: string) {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<p>Login screen</p>} />
      <Route path="/forbidden" element={<p>Forbidden screen</p>} />
      <Route
        path="/secret"
        element={
          <ProtectedRoute roles={['ADMIN', 'DOCTOR']}>
            <p>Secret content</p>
          </ProtectedRoute>
        }
      />
    </Routes>,
    { route },
  );
}

function signIn(role: RoleName) {
  tokenStorage.save('token', 'refresh', { id: 1, fullName: 'Test User', email: 't@example.com', roles: [role] });
}

describe('ProtectedRoute', () => {
  it('redirects anonymous users to login', () => {
    renderAt('/secret');
    expect(screen.getByText('Login screen')).toBeInTheDocument();
  });

  it('blocks users without the required role', () => {
    signIn('ANALYST');
    renderAt('/secret');
    expect(screen.getByText('Forbidden screen')).toBeInTheDocument();
  });

  it('renders children for allowed roles', () => {
    signIn('DOCTOR');
    renderAt('/secret');
    expect(screen.getByText('Secret content')).toBeInTheDocument();
  });
});
