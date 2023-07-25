import type { ReactElement } from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { SnackbarProvider } from 'notistack';
import { AuthProvider } from '../auth/AuthContext';
import { ColorModeProvider } from '../theme/ColorModeProvider';

export function renderWithProviders(ui: ReactElement, { route = '/' }: { route?: string } = {}) {
  return render(
    <ColorModeProvider>
      <SnackbarProvider>
        <MemoryRouter initialEntries={[route]}>
          <AuthProvider>{ui}</AuthProvider>
        </MemoryRouter>
      </SnackbarProvider>
    </ColorModeProvider>,
  );
}
