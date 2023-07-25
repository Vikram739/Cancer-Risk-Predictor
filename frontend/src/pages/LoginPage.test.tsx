import { describe, expect, it, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { renderWithProviders } from '../test/renderWithProviders';
import LoginPage from './LoginPage';
import { authApi } from '../api/endpoints';

vi.mock('../api/endpoints', () => ({
  authApi: { login: vi.fn(), register: vi.fn(), me: vi.fn() },
}));

const login = vi.mocked(authApi.login);

function renderLogin() {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<p>Dashboard home</p>} />
    </Routes>,
    { route: '/login' },
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    login.mockReset();
  });

  it('validates fields before calling the API', async () => {
    renderLogin();
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(await screen.findByText('Email is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
    expect(login).not.toHaveBeenCalled();
  });

  it('logs in and redirects to the dashboard', async () => {
    login.mockResolvedValue({
      accessToken: 'a',
      refreshToken: 'r',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: { id: 1, fullName: 'Dr. Rohan Kapoor', email: 'rohan@example.com', roles: ['DOCTOR'] },
    });
    renderLogin();
    await userEvent.click(screen.getByLabelText(/Email/));
    await userEvent.paste('rohan@example.com');
    await userEvent.click(screen.getByLabelText(/Password/));
    await userEvent.paste('Password@123');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByText('Dashboard home')).toBeInTheDocument();
    expect(login).toHaveBeenCalledWith('rohan@example.com', 'Password@123');
    expect(localStorage.getItem('crp.accessToken')).toBe('a');
  });

  it('shows the server error message', async () => {
    login.mockRejectedValue(new Error('Invalid email or password'));
    renderLogin();
    await userEvent.click(screen.getByLabelText(/Email/));
    await userEvent.paste('rohan@example.com');
    await userEvent.click(screen.getByLabelText(/Password/));
    await userEvent.paste('wrong');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Invalid email or password'));
  });
});
