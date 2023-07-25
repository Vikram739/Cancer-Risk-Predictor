import { Box, Typography, useTheme } from '@mui/material';
import { RISK_COLORS, formatPercent } from '../utils/format';

interface Props {
  probability: number;
  lowMax?: number;
  highMin?: number;
  size?: number;
}

const START = Math.PI;

function point(cx: number, cy: number, r: number, fraction: number) {
  const angle = START - fraction * Math.PI;
  return { x: cx + r * Math.cos(angle), y: cy - r * Math.sin(angle) };
}

function arc(cx: number, cy: number, r: number, from: number, to: number) {
  const a = point(cx, cy, r, from);
  const b = point(cx, cy, r, to);
  return `M ${a.x} ${a.y} A ${r} ${r} 0 0 1 ${b.x} ${b.y}`;
}

/** Half circle gauge split into the three risk bands with a needle at the probability. */
export default function ProbabilityGauge({ probability, lowMax = 0.3, highMin = 0.7, size = 260 }: Props) {
  const theme = useTheme();
  const p = Math.min(1, Math.max(0, probability));
  const width = size;
  const height = size * 0.62;
  const cx = width / 2;
  const cy = size * 0.52;
  const r = size * 0.4;
  const stroke = size * 0.075;
  const needle = point(cx, cy, r - stroke * 0.9, p);

  return (
    <Box sx={{ textAlign: 'center' }}>
      <svg
        width={width}
        height={height}
        viewBox={`0 0 ${width} ${height}`}
        role="img"
        aria-label={`Malignancy probability ${formatPercent(p)}`}
      >
        <path d={arc(cx, cy, r, 0, lowMax)} stroke={RISK_COLORS.LOW} strokeWidth={stroke} fill="none" />
        <path d={arc(cx, cy, r, lowMax, highMin)} stroke={RISK_COLORS.MODERATE} strokeWidth={stroke} fill="none" />
        <path d={arc(cx, cy, r, highMin, 1)} stroke={RISK_COLORS.HIGH} strokeWidth={stroke} fill="none" />
        <line
          x1={cx}
          y1={cy}
          x2={needle.x}
          y2={needle.y}
          stroke={theme.palette.text.primary}
          strokeWidth={3.5}
          strokeLinecap="round"
        />
        <circle cx={cx} cy={cy} r={7} fill={theme.palette.text.primary} />
      </svg>
      <Typography variant="h4" component="p" data-testid="gauge-value" sx={{ mt: -1 }}>
        {formatPercent(p)}
      </Typography>
      <Typography variant="body2" color="text.secondary">
        estimated probability of malignancy
      </Typography>
    </Box>
  );
}
