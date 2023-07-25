import { Alert, AlertTitle, Box, Button, CircularProgress, Typography } from '@mui/material';

export function LoadingState({ label = 'Loading' }: { label?: string }) {
  return (
    <Box
      role="status"
      sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 2, py: 8, color: 'text.secondary' }}
    >
      <CircularProgress size={28} />
      <Typography>{label}...</Typography>
    </Box>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <Alert
      severity="error"
      sx={{ my: 2 }}
      action={
        onRetry && (
          <Button color="inherit" size="small" onClick={onRetry}>
            Retry
          </Button>
        )
      }
    >
      <AlertTitle>Could not load data</AlertTitle>
      {message}
    </Alert>
  );
}

export function EmptyState({ message }: { message: string }) {
  return (
    <Typography color="text.secondary" sx={{ py: 6, textAlign: 'center' }}>
      {message}
    </Typography>
  );
}
