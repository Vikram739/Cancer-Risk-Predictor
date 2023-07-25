import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import {
  Box,
  Button,
  Card,
  IconButton,
  InputAdornment,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TableSortLabel,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import SearchIcon from '@mui/icons-material/Search';
import EditIcon from '@mui/icons-material/EditOutlined';
import DeleteIcon from '@mui/icons-material/DeleteOutline';
import HistoryIcon from '@mui/icons-material/TimelineOutlined';
import AssessIcon from '@mui/icons-material/AddchartOutlined';
import DownloadIcon from '@mui/icons-material/FileDownloadOutlined';
import { errorMessage } from '../api/client';
import { assessmentApi, patientApi } from '../api/endpoints';
import type { Patient, PatientInput } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import PageHeader from '../components/PageHeader';
import PatientDialog from '../components/PatientDialog';
import { EmptyState, ErrorState, LoadingState } from '../components/StatusViews';
import { useAsync, useDebouncedValue } from '../hooks/useAsync';
import { downloadBlob } from '../utils/download';
import { formatDate } from '../utils/format';

type SortField = 'lastName' | 'mrn' | 'dateOfBirth' | 'createdAt';

const COLUMNS: { field: SortField | null; label: string }[] = [
  { field: 'lastName', label: 'Name' },
  { field: 'mrn', label: 'MRN' },
  { field: 'dateOfBirth', label: 'Date of birth' },
  { field: null, label: 'Sex' },
  { field: null, label: 'Contact' },
  { field: 'createdAt', label: 'Registered' },
];

export default function PatientsPage() {
  const { hasRole } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const canEdit = hasRole('ADMIN', 'DOCTOR');
  const isAdmin = hasRole('ADMIN');

  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [sort, setSort] = useState<{ field: SortField; dir: 'asc' | 'desc' }>({ field: 'lastName', dir: 'asc' });
  const [dialog, setDialog] = useState<{ open: boolean; patient: Patient | null }>({ open: false, patient: null });
  const debouncedSearch = useDebouncedValue(search.trim(), 300);

  const { data, loading, error, reload } = useAsync(
    () => patientApi.list({ search: debouncedSearch || undefined, page, size, sort: `${sort.field},${sort.dir}` }),
    [debouncedSearch, page, size, sort.field, sort.dir],
  );

  const toggleSort = (field: SortField) => {
    setSort((prev) => ({ field, dir: prev.field === field && prev.dir === 'asc' ? 'desc' : 'asc' }));
    setPage(0);
  };

  const save = async (values: PatientInput) => {
    try {
      if (dialog.patient) {
        await patientApi.update(dialog.patient.id, values);
        enqueueSnackbar('Patient updated', { variant: 'success' });
      } else {
        await patientApi.create(values);
        enqueueSnackbar('Patient added', { variant: 'success' });
      }
      setDialog({ open: false, patient: null });
      reload();
    } catch (err) {
      enqueueSnackbar(errorMessage(err), { variant: 'error' });
    }
  };

  const remove = async (patient: Patient) => {
    if (!window.confirm(`Delete ${patient.fullName} and all of their assessments?`)) return;
    try {
      await patientApi.remove(patient.id);
      enqueueSnackbar('Patient deleted', { variant: 'success' });
      reload();
    } catch (err) {
      enqueueSnackbar(errorMessage(err), { variant: 'error' });
    }
  };

  const exportCsv = async () => {
    try {
      downloadBlob(await assessmentApi.exportCsv(), 'assessments.csv');
    } catch (err) {
      enqueueSnackbar(errorMessage(err, 'Export failed'), { variant: 'error' });
    }
  };

  return (
    <>
      <PageHeader
        title="Patients"
        subtitle={data ? `${data.totalElements} patients on record` : 'Patient registry'}
        actions={
          <>
            <Button variant="outlined" startIcon={<DownloadIcon />} onClick={exportCsv}>
              Export CSV
            </Button>
            {canEdit && (
              <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialog({ open: true, patient: null })}>
                Add patient
              </Button>
            )}
          </>
        }
      />

      <Card>
        <Box sx={{ p: 2 }}>
          <TextField
            placeholder="Search by name or MRN"
            size="small"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            sx={{ width: { xs: '100%', sm: 340 } }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" />
                </InputAdornment>
              ),
            }}
            inputProps={{ 'aria-label': 'search patients' }}
          />
        </Box>

        {error && <ErrorState message={error} onRetry={reload} />}
        {loading && !data && <LoadingState label="Loading patients" />}
        {data && (
          <>
            <TableContainer sx={{ opacity: loading ? 0.6 : 1, transition: 'opacity 0.2s' }}>
              <Table size="medium">
                <TableHead>
                  <TableRow>
                    {COLUMNS.map((col) => (
                      <TableCell key={col.label}>
                        {col.field ? (
                          <TableSortLabel
                            active={sort.field === col.field}
                            direction={sort.field === col.field ? sort.dir : 'asc'}
                            onClick={() => toggleSort(col.field as SortField)}
                          >
                            {col.label}
                          </TableSortLabel>
                        ) : (
                          col.label
                        )}
                      </TableCell>
                    ))}
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {data.content.map((p) => (
                    <TableRow key={p.id} hover>
                      <TableCell>
                        <Typography fontWeight={500}>{p.fullName}</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {p.age} years
                        </Typography>
                      </TableCell>
                      <TableCell>{p.mrn}</TableCell>
                      <TableCell>{formatDate(p.dateOfBirth)}</TableCell>
                      <TableCell sx={{ textTransform: 'capitalize' }}>{p.sex.toLowerCase()}</TableCell>
                      <TableCell>
                        <Typography variant="body2">{p.email ?? '-'}</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {p.phone ?? ''}
                        </Typography>
                      </TableCell>
                      <TableCell>{formatDate(p.createdAt)}</TableCell>
                      <TableCell align="right">
                        <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                          <Tooltip title="Assessment history">
                            <IconButton component={RouterLink} to={`/patients/${p.id}/history`} size="small">
                              <HistoryIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          {canEdit && (
                            <>
                              <Tooltip title="New assessment">
                                <IconButton component={RouterLink} to={`/assessments/new?patientId=${p.id}`} size="small">
                                  <AssessIcon fontSize="small" />
                                </IconButton>
                              </Tooltip>
                              <Tooltip title="Edit">
                                <IconButton size="small" onClick={() => setDialog({ open: true, patient: p })}>
                                  <EditIcon fontSize="small" />
                                </IconButton>
                              </Tooltip>
                            </>
                          )}
                          {isAdmin && (
                            <Tooltip title="Delete">
                              <IconButton size="small" color="error" onClick={() => remove(p)}>
                                <DeleteIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
            {data.content.length === 0 && <EmptyState message="No patients match your search" />}
            <TablePagination
              component="div"
              count={data.totalElements}
              page={page}
              rowsPerPage={size}
              rowsPerPageOptions={[10, 20, 50]}
              onPageChange={(_, p) => setPage(p)}
              onRowsPerPageChange={(e) => {
                setSize(Number(e.target.value));
                setPage(0);
              }}
            />
          </>
        )}
      </Card>

      <PatientDialog
        open={dialog.open}
        patient={dialog.patient}
        onClose={() => setDialog({ open: false, patient: null })}
        onSubmit={save}
      />
    </>
  );
}
