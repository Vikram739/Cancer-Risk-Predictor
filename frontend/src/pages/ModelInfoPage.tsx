import {
  Alert,
  Box,
  Card,
  CardContent,
  CardHeader,
  Chip,
  Grid,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
  useTheme,
} from '@mui/material';
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { statsApi } from '../api/endpoints';
import type { ModelInfo } from '../api/types';
import PageHeader from '../components/PageHeader';
import { ErrorState, LoadingState } from '../components/StatusViews';
import { useAsync } from '../hooks/useAsync';
import { featureLabel } from '../utils/features';
import { RISK_COLORS, formatPercent, modelDisplayName } from '../utils/format';

const METRICS = [
  { key: 'recall', label: 'Recall' },
  { key: 'precision', label: 'Precision' },
  { key: 'f1', label: 'F1' },
  { key: 'accuracy', label: 'Accuracy' },
  { key: 'roc_auc', label: 'ROC-AUC' },
] as const;

const SERIES_COLORS = ['#5b4fc7', '#1f9e94', '#e0a020', '#d64545'];

export default function ModelInfoPage() {
  const theme = useTheme();
  const { data, loading, error, reload } = useAsync(() => statsApi.model(), []);

  if (loading && !data) return <LoadingState label="Loading model details" />;
  if (error) return <ErrorState message={error} onRetry={reload} />;
  if (!data) return null;
  if (!data.available) {
    return <Alert severity="warning">The model service is not reachable. Try again in a moment.</Alert>;
  }

  const info = data.model as ModelInfo;
  const metrics = info.metrics;
  const modelNames = metrics ? Object.keys(metrics.models) : [];
  const chartData = METRICS.map((m) => ({
    metric: m.label,
    ...Object.fromEntries(modelNames.map((name) => [name, Number((metrics!.models[name].test[m.key] * 100).toFixed(2))])),
  }));
  const best = metrics?.models[info.model_name];
  const cm = best?.test.confusion_matrix;

  return (
    <>
      <PageHeader
        title="Model information"
        subtitle={`${modelDisplayName(info.model_name)} version ${info.model_version}, selected by cross validated ${metrics?.selection_metric ?? 'recall'}`}
      />
      <Grid container spacing={2.5}>
        <Grid item xs={12} md={4}>
          <Card sx={{ height: '100%' }}>
            <CardHeader title="Training data" />
            <CardContent sx={{ pt: 0 }}>
              {metrics && (
                <Stack spacing={1}>
                  <Typography variant="body2">{metrics.dataset.name}</Typography>
                  <Typography variant="body2">
                    {metrics.dataset.samples} samples, {metrics.dataset.features} features
                  </Typography>
                  <Typography variant="body2">
                    {metrics.dataset.malignant} malignant, {metrics.dataset.benign} benign
                  </Typography>
                  <Typography variant="body2">
                    Stratified split: {metrics.dataset.train_size} train, {metrics.dataset.test_size} test
                  </Typography>
                  <Typography variant="body2">SMOTE oversampling inside every training fold</Typography>
                </Stack>
              )}
              <Typography variant="subtitle2" sx={{ mt: 2.5, mb: 1 }}>
                Risk bands
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                <Chip size="small" label={`Low < ${formatPercent(info.risk_thresholds.low_max, 0)}`} sx={{ color: RISK_COLORS.LOW }} variant="outlined" />
                <Chip size="small" label="Moderate" sx={{ color: RISK_COLORS.MODERATE }} variant="outlined" />
                <Chip size="small" label={`High >= ${formatPercent(info.risk_thresholds.high_min, 0)}`} sx={{ color: RISK_COLORS.HIGH }} variant="outlined" />
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={8}>
          <Card sx={{ height: '100%' }}>
            <CardHeader title="Model comparison" subheader="Scores on the held out test set" />
            <CardContent sx={{ height: 320 }}>
              <ResponsiveContainer>
                <BarChart data={chartData} margin={{ top: 5, right: 10, left: -10, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} />
                  <XAxis dataKey="metric" stroke={theme.palette.text.secondary} fontSize={12} />
                  <YAxis domain={[85, 100]} unit="%" stroke={theme.palette.text.secondary} fontSize={12} />
                  <Tooltip
                    contentStyle={{ background: theme.palette.background.paper, border: `1px solid ${theme.palette.divider}`, borderRadius: 8 }}
                    formatter={(v: number | string, name: string) => [`${v}%`, modelDisplayName(name)]}
                  />
                  <Legend formatter={(name: string) => modelDisplayName(name)} />
                  {modelNames.map((name, i) => (
                    <Bar key={name} dataKey={name} fill={SERIES_COLORS[i % SERIES_COLORS.length]} radius={[3, 3, 0, 0]} isAnimationActive={false} />
                  ))}
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} lg={8}>
          <Card>
            <CardHeader title="Cross validation and test scores" subheader="5 fold stratified cross validation on the training set" />
            <Box sx={{ overflowX: 'auto' }}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Model</TableCell>
                    <TableCell align="right">CV recall</TableCell>
                    <TableCell align="right">CV F1</TableCell>
                    <TableCell align="right">Test recall</TableCell>
                    <TableCell align="right">Test precision</TableCell>
                    <TableCell align="right">Test ROC-AUC</TableCell>
                    <TableCell>Best parameters</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {modelNames.map((name) => {
                    const m = metrics!.models[name];
                    const selected = name === info.model_name;
                    return (
                      <TableRow key={name} selected={selected}>
                        <TableCell>
                          {m.display_name} {selected && <Chip size="small" color="primary" label="selected" sx={{ ml: 1 }} />}
                        </TableCell>
                        <TableCell align="right">{formatPercent(m.cv.recall, 2)}</TableCell>
                        <TableCell align="right">{formatPercent(m.cv.f1, 2)}</TableCell>
                        <TableCell align="right">{formatPercent(m.test.recall, 2)}</TableCell>
                        <TableCell align="right">{formatPercent(m.test.precision, 2)}</TableCell>
                        <TableCell align="right">{m.test.roc_auc.toFixed(4)}</TableCell>
                        <TableCell>
                          <Typography variant="caption" sx={{ fontFamily: 'monospace' }}>
                            {Object.entries(m.best_params)
                              .map(([k, v]) => `${k}=${v}`)
                              .join(', ')}
                          </Typography>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </Box>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} lg={4}>
          <Card sx={{ height: '100%' }}>
            <CardHeader title="Confusion matrix" subheader={`${modelDisplayName(info.model_name)} on the test set`} />
            <CardContent>
              {cm && (
                <Box
                  sx={{
                    display: 'grid',
                    gridTemplateColumns: 'auto 1fr 1fr',
                    gap: 1,
                    textAlign: 'center',
                    alignItems: 'center',
                  }}
                >
                  <span />
                  <Typography variant="caption">Pred. benign</Typography>
                  <Typography variant="caption">Pred. malignant</Typography>
                  <Typography variant="caption">Benign</Typography>
                  <Cell value={cm.tn} good />
                  <Cell value={cm.fp} />
                  <Typography variant="caption">Malignant</Typography>
                  <Cell value={cm.fn} />
                  <Cell value={cm.tp} good />
                </Box>
              )}
              <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                False negatives (malignant cases predicted benign) are the costliest error, which is why recall is the
                selection metric.
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12}>
          <Card>
            <CardHeader
              title="Selected features"
              subheader={`${info.selected_features.length} of ${info.features.length} features kept by univariate selection`}
            />
            <CardContent sx={{ pt: 0 }}>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {info.selected_features.map((f) => (
                  <Chip key={f} label={featureLabel(f)} size="small" variant="outlined" />
                ))}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </>
  );
}

function Cell({ value, good = false }: { value: number; good?: boolean }) {
  const color = good ? RISK_COLORS.LOW : RISK_COLORS.HIGH;
  return (
    <Box
      sx={{
        py: 2,
        borderRadius: 2,
        fontSize: 22,
        fontWeight: 700,
        color,
        bgcolor: `${color}1f`,
      }}
    >
      {value}
    </Box>
  );
}
