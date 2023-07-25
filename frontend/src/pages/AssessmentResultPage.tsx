import { Link as RouterLink, useParams } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CardHeader,
  Divider,
  Grid,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableRow,
  Typography,
} from '@mui/material';
import HistoryIcon from '@mui/icons-material/TimelineOutlined';
import AddIcon from '@mui/icons-material/AddchartOutlined';
import { assessmentApi, statsApi } from '../api/endpoints';
import type { ModelInfo } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import PageHeader from '../components/PageHeader';
import ProbabilityGauge from '../components/ProbabilityGauge';
import RiskBadge from '../components/RiskBadge';
import ShapChart from '../components/ShapChart';
import { ErrorState, LoadingState } from '../components/StatusViews';
import { useAsync } from '../hooks/useAsync';
import { FEATURE_GROUPS, shortFeatureLabel } from '../utils/features';
import { formatDateTime, formatNumber, modelDisplayName } from '../utils/format';

const GUIDANCE = {
  LOW: 'The measurements resemble benign samples in the training data. Continue routine follow up as clinically indicated.',
  MODERATE: 'The model is uncertain. Consider correlating with imaging and clinical findings before deciding next steps.',
  HIGH: 'The measurements closely resemble malignant samples. Prompt specialist review is recommended.',
};

export default function AssessmentResultPage() {
  const id = Number(useParams().id);
  const { hasRole } = useAuth();
  const { data, loading, error, reload } = useAsync(
    () => Promise.all([assessmentApi.get(id), statsApi.model().catch(() => null)]),
    [id],
  );

  if (loading && !data) return <LoadingState label="Loading assessment" />;
  if (error) return <ErrorState message={error} onRetry={reload} />;
  if (!data) return null;

  const [assessment, model] = data;
  const thresholds = (model?.model as ModelInfo | undefined)?.risk_thresholds;

  return (
    <>
      <PageHeader
        title={`Assessment #${assessment.id}`}
        subtitle={`${assessment.patientName} (${assessment.patientMrn}), ${formatDateTime(assessment.createdAt)}`}
        actions={
          <>
            <Button component={RouterLink} to={`/patients/${assessment.patientId}/history`} startIcon={<HistoryIcon />}>
              Patient history
            </Button>
            {hasRole('ADMIN', 'DOCTOR') && (
              <Button
                component={RouterLink}
                to={`/assessments/new?patientId=${assessment.patientId}`}
                variant="contained"
                startIcon={<AddIcon />}
              >
                New assessment
              </Button>
            )}
          </>
        }
      />

      <Grid container spacing={2.5}>
        <Grid item xs={12} md={5}>
          <Card sx={{ height: '100%' }}>
            <CardHeader title="Result" action={<RiskBadge band={assessment.riskBand} size="medium" />} />
            <CardContent>
              <ProbabilityGauge
                probability={assessment.probability}
                lowMax={thresholds?.low_max}
                highMin={thresholds?.high_min}
              />
              <Alert severity={assessment.riskBand === 'HIGH' ? 'error' : assessment.riskBand === 'MODERATE' ? 'warning' : 'success'} sx={{ mt: 3 }}>
                {GUIDANCE[assessment.riskBand]}
              </Alert>
              <Divider sx={{ my: 2 }} />
              <Stack spacing={0.5}>
                <Typography variant="body2">
                  <strong>Model:</strong> {modelDisplayName(assessment.modelName)} v{assessment.modelVersion}
                </Typography>
                <Typography variant="body2">
                  <strong>Predicted class:</strong> {assessment.prediction}
                </Typography>
                {assessment.createdBy && (
                  <Typography variant="body2">
                    <strong>Assessed by:</strong> {assessment.createdBy}
                  </Typography>
                )}
                {assessment.notes && (
                  <Typography variant="body2">
                    <strong>Notes:</strong> {assessment.notes}
                  </Typography>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={7}>
          <Card sx={{ height: '100%' }}>
            <CardHeader title="Why this result" subheader="Top contributing features from SHAP" />
            <CardContent>
              <ShapChart contributions={assessment.topFeatures} />
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12}>
          <Card>
            <CardHeader title="Submitted measurements" />
            <CardContent sx={{ pt: 0 }}>
              <Grid container spacing={2}>
                {FEATURE_GROUPS.map((group) => (
                  <Grid item xs={12} md={4} key={group.key}>
                    <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                      {group.title}
                    </Typography>
                    <Box sx={{ overflowX: 'auto' }}>
                      <Table size="small">
                        <TableBody>
                          {group.features.map((f) => (
                            <TableRow key={f}>
                              <TableCell sx={{ pl: 0 }}>{shortFeatureLabel(f)}</TableCell>
                              <TableCell align="right" sx={{ pr: 0, fontVariantNumeric: 'tabular-nums' }}>
                                {formatNumber(assessment.features[f] ?? 0, 5)}
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </Box>
                  </Grid>
                ))}
              </Grid>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </>
  );
}
