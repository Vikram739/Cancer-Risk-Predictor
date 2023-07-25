import type { ReactNode } from 'react';
import { Box, Card, CardContent, Typography } from '@mui/material';

export default function AuthLayout({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'grid',
        placeItems: 'center',
        px: 2,
        py: 4,
        background: (t) =>
          t.palette.mode === 'light'
            ? 'radial-gradient(circle at 20% 10%, #e6e3ff 0, transparent 45%), radial-gradient(circle at 85% 90%, #d9f4f1 0, transparent 40%), #f5f6fa'
            : 'radial-gradient(circle at 20% 10%, #23204a 0, transparent 45%), radial-gradient(circle at 85% 90%, #123532 0, transparent 40%), #0f1117',
      }}
    >
      <Card sx={{ width: '100%', maxWidth: 420 }}>
        <CardContent sx={{ p: { xs: 3, sm: 4 } }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
            <Box component="img" src="/favicon.svg" alt="" sx={{ width: 40, height: 40 }} />
            <Box>
              <Typography variant="h6" lineHeight={1.2}>
                Cancer Risk Predictor
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Breast cancer risk assessment dashboard
              </Typography>
            </Box>
          </Box>
          <Typography variant="h5" component="h1">
            {title}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            {subtitle}
          </Typography>
          {children}
        </CardContent>
      </Card>
    </Box>
  );
}
