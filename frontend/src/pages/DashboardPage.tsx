import { Link as RouterLink } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  CardHeader,
  Grid,
  LinearProgress,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
  useTheme,
} from '@mui/material';
import PeopleIcon from '@mui/icons-material/PeopleAltOutlined';
import AssignmentIcon from '@mui/icons-material/AssignmentTurnedInOutlined';
import WarningIcon from '@mui/icons-material/ReportProblemOutlined';
import PercentIcon from '@mui/icons-material/PercentOutlined';
import {
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { assessmentApi, statsApi } from '../api/endpoints';
import type { ModelInfo, RiskBand } from '../api/types';
import PageHeader from '../components/PageHeader';
import RiskBadge from '../components/RiskBadge';
import StatCard from '../components/StatCard';
import { ErrorState, LoadingState } from '../components/StatusViews';
import { useAsync } from '../hooks/useAsync';
import { RISK_COLORS, RISK_LABELS, formatDate, formatDateTime, formatMonth, formatPercent, modelDisplayName } from '../utils/format';

const BANDS: RiskBand[] = ['LOW', 'MODERATE', 'HIGH'];

function ModelCard({ info, available }: { info?: ModelInfo; available: boolean }) {
  if (!available || !info?.metrics) {
    return (
      <Card sx={{ height: '100%' }}>
        <CardHeader title="Model performance" />
        <CardContent>
          <Typography color="text.secondary">Model service is unavailable right now.</Typography>
        </CardContent>
      </Card>
    );
  }
  const best = info.metrics.models[info.model_name]?.test;
  const rows: [string, number][] = [
    ['Recall', best.recall],
    ['Precision', best.precision],
    ['F1 score', best.f1],
    ['Accuracy', best.accuracy],
    ['ROC-AUC', best.roc_auc],
  ];
  return (
    <Card sx={{ height: '100%' }}>
      <CardHeader
        title="Model performance"
        subheader={`${modelDisplayName(info.model_name)} v${info.model_version}, held out test set`}
        action={
          <Button component={RouterLink} to="/model" size="small">
            Details
          </Button>
        }
      />
      <CardContent sx={{ pt: 0 }}>
        <Stack spacing={1.6}>
          {rows.map(([label, value]) => (
            <Box key={label}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                <Typography variant="body2">{label}</Typography>
                <Typography variant="body2" fontWeight={600}>
                  {formatPercent(value, 1)}
                </Typography>
              </Box>
              <LinearProgress variant="determinate" value={value * 100} sx={{ height: 7, borderRadius: 4 }} />
            </Box>
          ))}
        </Stack>
      </CardContent>
    </Card>
  );
}

export default function DashboardPage() {
  const theme = useTheme();
  const { data, loading, error, reload } = useAsync(
    () =>
      Promise.all([
        statsApi.summary(),
        statsApi.monthly(),
        statsApi.model(),
        assessmentApi.list({ size: 6, sort: 'createdAt,desc' }),
      ]),
    [],
  );

  if (loading && !data) return <LoadingState label="Loading dashboard" />;
  if (error) return <ErrorState message={error} onRetry={reload} />;
  if (!data) return null;

  const [summary, monthly, model, recent] = data;
  const pieData = BANDS.map((band) => ({
    band,
    name: RISK_LABELS[band],
    value: summary.assessmentsByRiskBand[band] ?? 0,
  }));
  const trend = monthly.map((m) => ({ ...m, label: formatMonth(m.month) }));
  const tooltipStyle = {
    background: theme.palette.background.paper,
    border: `1px solid ${theme.palette.divider}`,
    borderRadius: 8,
  };

  return (
    <>
      <PageHeader
        title="Dashboard"
        subtitle={`Overview of patients and risk assessments. Last assessment ${formatDateTime(summary.lastAssessmentAt)}`}
      />
      <Grid container spacing={2.5}>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard label="Patients" value={summary.totalPatients} icon={<PeopleIcon />} color={theme.palette.primary.main} />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard
            label="Assessments"
            value={summary.totalAssessments}
            icon={<AssignmentIcon />}
            color={theme.palette.secondary.main}
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard
            label="High risk patients"
            value={summary.highRiskPatients}
            hint="based on each patient's latest assessment"
            icon={<WarningIcon />}
            color={RISK_COLORS.HIGH}
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard
            label="Mean probability"
            value={formatPercent(summary.averageProbability)}
            hint="average malignancy probability"
            icon={<PercentIcon />}
            color={RISK_COLORS.MODERATE}
          />
        </Grid>

        <Grid item xs={12} md={5} lg={4}>
          <Card sx={{ height: '100%' }}>
            <CardHeader title="Risk distribution" subheader="All assessments by risk band" />
            <CardContent sx={{ height: 300 }}>
              <ResponsiveContainer>
                <PieChart margin={{ top: 16, right: 16, bottom: 0, left: 16 }}>
                  <Pie
                    data={pieData}
                    dataKey="value"
                    nameKey="name"
                    innerRadius="52%"
                    outerRadius="78%"
                    paddingAngle={2}
                    isAnimationActive={false}
                    label={({ value }) => value}
                  >
                    {pieData.map((d) => (
                      <Cell key={d.band} fill={RISK_COLORS[d.band]} stroke={theme.palette.background.paper} />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={tooltipStyle} />
                  <Legend verticalAlign="bottom" />
                </PieChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={7} lg={8}>
          <Card sx={{ height: '100%' }}>
            <CardHeader title="Monthly assessments" subheader="Volume per month split by risk band" />
            <CardContent sx={{ height: 300 }}>
              <ResponsiveContainer>
                <LineChart data={trend} margin={{ top: 5, right: 20, left: -10, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} />
                  <XAxis dataKey="label" stroke={theme.palette.text.secondary} fontSize={12} />
                  <YAxis allowDecimals={false} stroke={theme.palette.text.secondary} fontSize={12} />
                  <Tooltip contentStyle={tooltipStyle} />
                  <Legend />
                  <Line type="monotone" dataKey="total" name="Total" stroke={theme.palette.primary.main} strokeWidth={3} isAnimationActive={false} />
                  <Line type="monotone" dataKey="high" name="High" stroke={RISK_COLORS.HIGH} strokeWidth={2} isAnimationActive={false} />
                  <Line type="monotone" dataKey="moderate" name="Moderate" stroke={RISK_COLORS.MODERATE} strokeWidth={2} isAnimationActive={false} />
                  <Line type="monotone" dataKey="low" name="Low" stroke={RISK_COLORS.LOW} strokeWidth={2} isAnimationActive={false} />
                </LineChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} lg={8}>
          <Card sx={{ height: '100%' }}>
            <CardHeader title="Recent assessments" />
            <Box sx={{ overflowX: 'auto' }}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Patient</TableCell>
                    <TableCell>Date</TableCell>
                    <TableCell align="right">Probability</TableCell>
                    <TableCell>Risk</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {recent.content.map((a) => (
                    <TableRow
                      key={a.id}
                      hover
                      component={RouterLink}
                      to={`/assessments/${a.id}`}
                      sx={{ textDecoration: 'none' }}
                    >
                      <TableCell>
                        <Typography variant="body2" fontWeight={500}>
                          {a.patientName}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {a.patientMrn}
                        </Typography>
                      </TableCell>
                      <TableCell>{formatDate(a.createdAt)}</TableCell>
                      <TableCell align="right">{formatPercent(a.probability)}</TableCell>
                      <TableCell>
                        <RiskBadge band={a.riskBand} />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Box>
          </Card>
        </Grid>

        <Grid item xs={12} lg={4}>
          <ModelCard info={model.model as ModelInfo} available={model.available} />
        </Grid>
      </Grid>
    </>
  );
}
