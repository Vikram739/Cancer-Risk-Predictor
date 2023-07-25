import { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import {
  Alert,
  Autocomplete,
  Box,
  Button,
  Card,
  CardContent,
  CardHeader,
  CircularProgress,
  Grid,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import RestartIcon from '@mui/icons-material/RestartAlt';
import { errorMessage } from '../api/client';
import { assessmentApi, patientApi } from '../api/endpoints';
import type { Patient } from '../api/types';
import PageHeader from '../components/PageHeader';
import { useDebouncedValue } from '../hooks/useAsync';
import { assessmentSchema, type AssessmentFormValues } from '../utils/assessmentSchema';
import { FEATURE_GROUPS, FEATURE_RANGES, defaultFeatureValues, shortFeatureLabel, stepFor } from '../utils/features';
import { formatNumber } from '../utils/format';

export default function NewAssessmentPage() {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [params] = useSearchParams();
  const [tab, setTab] = useState(0);
  const [patientQuery, setPatientQuery] = useState('');
  const [patientOptions, setPatientOptions] = useState<Patient[]>([]);
  const [selectedPatient, setSelectedPatient] = useState<Patient | null>(null);
  const [loadingPatients, setLoadingPatients] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const debouncedQuery = useDebouncedValue(patientQuery, 250);

  const {
    control,
    handleSubmit,
    reset,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<AssessmentFormValues>({
    resolver: zodResolver(assessmentSchema),
    defaultValues: { patientId: undefined as unknown as number, notes: '', features: defaultFeatureValues() },
  });

  useEffect(() => {
    const id = Number(params.get('patientId'));
    if (id) {
      patientApi
        .get(id)
        .then((p) => {
          setSelectedPatient(p);
          setValue('patientId', p.id, { shouldValidate: true });
        })
        .catch(() => undefined);
    }
  }, [params, setValue]);

  useEffect(() => {
    let active = true;
    setLoadingPatients(true);
    patientApi
      .list({ search: debouncedQuery || undefined, size: 20, sort: 'lastName,asc' })
      .then((page) => active && setPatientOptions(page.content))
      .catch(() => active && setPatientOptions([]))
      .finally(() => active && setLoadingPatients(false));
    return () => {
      active = false;
    };
  }, [debouncedQuery]);

  const groupHasError = (index: number) =>
    FEATURE_GROUPS[index].features.some((f) => errors.features?.[f as keyof typeof errors.features]);

  const onSubmit = handleSubmit(
    async (values) => {
      setServerError(null);
      try {
        const result = await assessmentApi.create({
          patientId: values.patientId,
          notes: values.notes || undefined,
          features: values.features,
        });
        enqueueSnackbar('Assessment completed', { variant: 'success' });
        navigate(`/assessments/${result.id}`);
      } catch (err) {
        setServerError(errorMessage(err, 'Assessment failed'));
      }
    },
    (formErrors) => {
      const firstBad = FEATURE_GROUPS.findIndex((g) =>
        g.features.some((f) => formErrors.features?.[f as keyof typeof formErrors.features]),
      );
      if (firstBad >= 0) setTab(firstBad);
    },
  );

  const group = FEATURE_GROUPS[tab];

  return (
    <>
      <PageHeader
        title="New assessment"
        subtitle="Enter the 30 measurements from the fine needle aspirate report. Defaults are the dataset medians."
      />
      <form onSubmit={onSubmit} noValidate>
        <Grid container spacing={2.5}>
          <Grid item xs={12} lg={4}>
            <Card sx={{ position: { lg: 'sticky' }, top: { lg: 88 } }}>
              <CardHeader title="Patient" />
              <CardContent>
                <Stack spacing={2}>
                  <Controller
                    name="patientId"
                    control={control}
                    render={({ field }) => (
                      <Autocomplete
                        options={patientOptions}
                        value={selectedPatient}
                        loading={loadingPatients}
                        getOptionLabel={(p) => `${p.fullName} (${p.mrn})`}
                        isOptionEqualToValue={(a, b) => a.id === b.id}
                        filterOptions={(x) => x}
                        onInputChange={(_, value) => setPatientQuery(value)}
                        onChange={(_, p) => {
                          setSelectedPatient(p);
                          field.onChange(p?.id);
                        }}
                        renderInput={(input) => (
                          <TextField
                            {...input}
                            label="Search patient"
                            error={!!errors.patientId}
                            helperText={errors.patientId ? 'Select a patient' : undefined}
                            InputProps={{
                              ...input.InputProps,
                              endAdornment: (
                                <>
                                  {loadingPatients && <CircularProgress size={18} />}
                                  {input.InputProps.endAdornment}
                                </>
                              ),
                            }}
                          />
                        )}
                      />
                    )}
                  />
                  <Controller
                    name="notes"
                    control={control}
                    render={({ field }) => (
                      <TextField {...field} label="Clinical notes (optional)" multiline minRows={3} />
                    )}
                  />
                  {serverError && <Alert severity="error">{serverError}</Alert>}
                  <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
                    {isSubmitting ? 'Running model...' : 'Run assessment'}
                  </Button>
                  <Button
                    startIcon={<RestartIcon />}
                    onClick={() =>
                      reset({ patientId: selectedPatient?.id as number, notes: '', features: defaultFeatureValues() })
                    }
                  >
                    Reset measurements
                  </Button>
                  <Alert severity="info" variant="outlined">
                    Results are produced by a machine learning model trained on a public research dataset and are not
                    a diagnosis.
                  </Alert>
                </Stack>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} lg={8}>
            <Card>
              <Tabs
                value={tab}
                onChange={(_, v) => setTab(v)}
                variant="scrollable"
                sx={{ borderBottom: 1, borderColor: 'divider', px: 1 }}
              >
                {FEATURE_GROUPS.map((g, i) => (
                  <Tab
                    key={g.key}
                    label={
                      <Box component="span" sx={{ color: groupHasError(i) ? 'error.main' : undefined }}>
                        {g.title}
                      </Box>
                    }
                  />
                ))}
              </Tabs>
              <CardContent>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2.5 }}>
                  {group.description}
                </Typography>
                <Grid container spacing={2}>
                  {group.features.map((name) => {
                    const range = FEATURE_RANGES[name];
                    const fieldError = errors.features?.[name as keyof typeof errors.features];
                    return (
                      <Grid item xs={12} sm={6} key={name}>
                        <Controller
                          name={`features.${name}` as `features.${string}`}
                          control={control}
                          render={({ field }) => (
                            <TextField
                              {...field}
                              value={field.value ?? ''}
                              type="number"
                              label={shortFeatureLabel(name)}
                              fullWidth
                              inputProps={{ step: stepFor(name), min: 0, 'aria-label': name }}
                              error={!!fieldError}
                              helperText={
                                fieldError?.message ??
                                `Typical ${formatNumber(range.min)} to ${formatNumber(range.max)}`
                              }
                            />
                          )}
                        />
                      </Grid>
                    );
                  })}
                </Grid>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 3 }}>
                  <Button disabled={tab === 0} onClick={() => setTab((t) => t - 1)}>
                    Previous group
                  </Button>
                  <Button disabled={tab === FEATURE_GROUPS.length - 1} onClick={() => setTab((t) => t + 1)}>
                    Next group
                  </Button>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </form>
    </>
  );
}
