import { Link as RouterLink, useParams } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  CardHeader,
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
import AddIcon from '@mui/icons-material/AddchartOutlined';
import DownloadIcon from '@mui/icons-material/FileDownloadOutlined';
import { useSnackbar } from 'notistack';
import {
  CartesianGrid,
  Line,
  LineChart,
  ReferenceArea,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { errorMessage } from '../api/client';
import { assessmentApi, patientApi } from '../api/endpoints';
import { useAuth } from '../auth/AuthContext';
import PageHeader from '../components/PageHeader';
import RiskBadge from '../components/RiskBadge';
import { EmptyState, ErrorState, LoadingState } from '../components/StatusViews';
import { useAsync } from '../hooks/useAsync';
import { downloadBlob } from '../utils/download';
import { featureLabel } from '../utils/features';
import { RISK_COLORS, formatDate, formatDateTime, formatPercent } from '../utils/format';

export default function PatientHistoryPage() {
  const id = Number(useParams().id);
  const theme = useTheme();
  const { hasRole } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const { data, loading, error, reload } = useAsync(
    () => Promise.all([patientApi.get(id), patientApi.history(id)]),
    [id],
  );

  if (loading && !data) return <LoadingState label="Loading history" />;
  if (error) return <ErrorState message={error} onRetry={reload} />;
  if (!data) return null;

  const [patient, history] = data;
  const assessments = history.content;
  const chronological = [...assessments].reverse().map((a) => ({
    date: formatDate(a.createdAt),
    probability: Number((a.probability * 100).toFixed(1)),
    worstRadius: a.features.worst_radius,
    worstConcavePoints: a.features.worst_concave_points,
  }));
  const latest = assessments[0];

  const exportCsv = async () => {
    try {
      downloadBlob(await assessmentApi.exportCsv({ patientId: id }), `${patient.mrn}-assessments.csv`);
    } catch (err) {
      enqueueSnackbar(errorMessage(err, 'Export failed'), { variant: 'error' });
    }
  };

  const tooltipStyle = {
    background: theme.palette.background.paper,
    border: `1px solid ${theme.palette.divider}`,
    borderRadius: 8,
  };

  return (
    <>
      <PageHeader
        title={patient.fullName}
        subtitle={`${patient.mrn}, ${patient.age} years, ${patient.sex.toLowerCase()}, born ${formatDate(patient.dateOfBirth)}`}
        actions={
          <>
            <Button variant="outlined" startIcon={<DownloadIcon />} onClick={exportCsv} disabled={!assessments.length}>
              Export CSV
            </Button>
            {hasRole('ADMIN', 'DOCTOR') && (
              <Button component={RouterLink} to={`/assessments/new?patientId=${id}`} variant="contained" startIcon={<AddIcon />}>
                New assessment
              </Button>
            )}
          </>
        }
      />

      {assessments.length === 0 ? (
        <Card>
          <EmptyState message="No assessments recorded for this patient yet" />
        </Card>
      ) : (
        <Grid container spacing={2.5}>
          <Grid item xs={12} md={4}>
            <Card sx={{ height: '100%' }}>
              <CardHeader title="Latest result" />
              <CardContent>
                <Stack spacing={1.5} alignItems="flex-start">
                  <RiskBadge band={latest.riskBand} size="medium" />
                  <Typography variant="h3" component="p">
                    {formatPercent(latest.probability)}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {formatDateTime(latest.createdAt)}, {assessments.length} assessment
                    {assessments.length === 1 ? '' : 's'} in total
                  </Typography>
                  <Typography variant="body2">
                    Top factor: <strong>{featureLabel(latest.topFeatures[0]?.feature ?? '-')}</strong>
                  </Typography>
                  <Button component={RouterLink} to={`/assessments/${latest.id}`} size="small">
                    View explanation
                  </Button>
                </Stack>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={8}>
            <Card sx={{ height: '100%' }}>
              <CardHeader title="Probability over time" subheader="Shaded areas show the risk bands" />
              <CardContent sx={{ height: 280 }}>
                <ResponsiveContainer>
                  <LineChart data={chronological} margin={{ top: 5, right: 20, left: -10, bottom: 0 }}>
                    <ReferenceArea y1={0} y2={30} fill={RISK_COLORS.LOW} fillOpacity={0.08} />
                    <ReferenceArea y1={30} y2={70} fill={RISK_COLORS.MODERATE} fillOpacity={0.08} />
                    <ReferenceArea y1={70} y2={100} fill={RISK_COLORS.HIGH} fillOpacity={0.08} />
                    <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} />
                    <XAxis dataKey="date" stroke={theme.palette.text.secondary} fontSize={12} />
                    <YAxis domain={[0, 100]} unit="%" stroke={theme.palette.text.secondary} fontSize={12} />
                    <Tooltip contentStyle={tooltipStyle} formatter={(v: number | string, _name: string) => [`${v}%`, 'Probability']} />
                    <Line
                      type="monotone"
                      dataKey="probability"
                      stroke={theme.palette.primary.main}
                      strokeWidth={3}
                      dot={{ r: 5 }}
                      isAnimationActive={false}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={6}>
            <Card>
              <CardHeader title="Key measurements" subheader="Worst radius and worst concave points per visit" />
              <CardContent sx={{ height: 260 }}>
                <ResponsiveContainer>
                  <LineChart data={chronological} margin={{ top: 5, right: 10, left: -10, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} />
                    <XAxis dataKey="date" stroke={theme.palette.text.secondary} fontSize={12} />
                    <YAxis yAxisId="r" stroke={theme.palette.secondary.main} fontSize={12} />
                    <YAxis yAxisId="c" orientation="right" stroke={RISK_COLORS.MODERATE} fontSize={12} />
                    <Tooltip contentStyle={tooltipStyle} />
                    <Line yAxisId="r" dataKey="worstRadius" name="Worst radius" stroke={theme.palette.secondary.main} strokeWidth={2} isAnimationActive={false} />
                    <Line yAxisId="c" dataKey="worstConcavePoints" name="Worst concave points" stroke={RISK_COLORS.MODERATE} strokeWidth={2} isAnimationActive={false} />
                  </LineChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={6}>
            <Card>
              <CardHeader title="All assessments" />
              <Box sx={{ overflowX: 'auto' }}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Date</TableCell>
                      <TableCell align="right">Probability</TableCell>
                      <TableCell>Risk</TableCell>
                      <TableCell>Model</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {assessments.map((a) => (
                      <TableRow key={a.id} hover component={RouterLink} to={`/assessments/${a.id}`} sx={{ textDecoration: 'none' }}>
                        <TableCell>{formatDateTime(a.createdAt)}</TableCell>
                        <TableCell align="right">{formatPercent(a.probability)}</TableCell>
                        <TableCell>
                          <RiskBadge band={a.riskBand} />
                        </TableCell>
                        <TableCell>v{a.modelVersion}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Box>
            </Card>
          </Grid>
        </Grid>
      )}
    </>
  );
}
