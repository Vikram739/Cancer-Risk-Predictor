import { Chip, alpha } from '@mui/material';
import type { RiskBand } from '../api/types';
import { RISK_COLORS, RISK_LABELS } from '../utils/format';

interface Props {
  band: RiskBand;
  size?: 'small' | 'medium';
}

export default function RiskBadge({ band, size = 'small' }: Props) {
  const color = RISK_COLORS[band];
  return (
    <Chip
      label={RISK_LABELS[band]}
      size={size}
      data-band={band}
      sx={{
        fontWeight: 600,
        color,
        bgcolor: alpha(color, 0.14),
        border: `1px solid ${alpha(color, 0.4)}`,
      }}
    />
  );
}
