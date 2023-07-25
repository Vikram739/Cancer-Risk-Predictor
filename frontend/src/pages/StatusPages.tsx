import { Link as RouterLink } from 'react-router-dom';
import { Box, Button, Typography } from '@mui/material';

function Message({ code, title, text }: { code: string; title: string; text: string }) {
  return (
    <Box sx={{ textAlign: 'center', py: 10 }}>
      <Typography variant="h2" color="primary" fontWeight={700}>
        {code}
      </Typography>
      <Typography variant="h5" sx={{ mt: 1 }}>
        {title}
      </Typography>
      <Typography color="text.secondary" sx={{ mt: 1, mb: 3 }}>
        {text}
      </Typography>
      <Button component={RouterLink} to="/" variant="contained">
        Back to dashboard
      </Button>
    </Box>
  );
}

export function NotFoundPage() {
  return <Message code="404" title="Page not found" text="The page you are looking for does not exist." />;
}

export function ForbiddenPage() {
  return <Message code="403" title="Access denied" text="Your role does not allow access to this page." />;
}
