import { Bar, BarChart, CartesianGrid, Cell, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Box, Typography, useTheme } from '@mui/material';
import type { FeatureContribution } from '../api/types';
import { featureLabel } from '../utils/features';
import { RISK_COLORS, formatNumber } from '../utils/format';

interface Props {
  contributions: FeatureContribution[];
  height?: number;
}

/** Horizontal bars: right of zero pushes towards malignant, left towards benign. */
export default function ShapChart({ contributions, height }: Props) {
  const theme = useTheme();
  const data = [...contributions]
    .sort((a, b) => Math.abs(b.shapValue) - Math.abs(a.shapValue))
    .map((c) => ({ ...c, label: featureLabel(c.feature) }));

  return (
    <Box>
      <Box sx={{ width: '100%', height: height ?? 60 + data.length * 44 }}>
        <ResponsiveContainer>
          <BarChart data={data} layout="vertical" margin={{ top: 8, right: 24, left: 8, bottom: 8 }}>
            <CartesianGrid horizontal={false} stroke={theme.palette.divider} />
            <XAxis type="number" stroke={theme.palette.text.secondary} fontSize={12} />
            <YAxis
              type="category"
              dataKey="label"
              width={170}
              stroke={theme.palette.text.secondary}
              fontSize={12}
              tickLine={false}
            />
            <ReferenceLine x={0} stroke={theme.palette.text.secondary} />
            <Tooltip
              cursor={{ fill: theme.palette.action.hover }}
              contentStyle={{
                background: theme.palette.background.paper,
                border: `1px solid ${theme.palette.divider}`,
                borderRadius: 8,
              }}
              formatter={(value: number | string, _name: string | number, item) => {
                const shap = Number(value);
                return [`${shap > 0 ? '+' : ''}${shap.toFixed(3)} (measured ${formatNumber(item.payload.value)})`, 'SHAP value'];
              }}
            />
            <Bar dataKey="shapValue" radius={4} isAnimationActive={false}>
              {data.map((d) => (
                <Cell key={d.feature} fill={d.shapValue >= 0 ? RISK_COLORS.HIGH : RISK_COLORS.LOW} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </Box>
      <Typography variant="caption" color="text.secondary">
        Red bars push the prediction towards malignant, green bars towards benign. Values are in log odds.
      </Typography>
    </Box>
  );
}
