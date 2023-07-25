import type { ReactNode } from 'react';
import { Avatar, Card, CardContent, Stack, Typography, alpha } from '@mui/material';

interface Props {
  label: string;
  value: ReactNode;
  hint?: ReactNode;
  icon: ReactNode;
  color: string;
}

export default function StatCard({ label, value, hint, icon, color }: Props) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Stack direction="row" spacing={2} alignItems="center">
          <Avatar variant="rounded" sx={{ bgcolor: alpha(color, 0.14), color, width: 48, height: 48 }}>
            {icon}
          </Avatar>
          <div>
            <Typography variant="body2" color="text.secondary">
              {label}
            </Typography>
            <Typography variant="h5" component="p">
              {value}
            </Typography>
          </div>
        </Stack>
        {hint && (
          <Typography variant="caption" color="text.secondary" component="p" sx={{ mt: 1.5 }}>
            {hint}
          </Typography>
        )}
      </CardContent>
    </Card>
  );
}
