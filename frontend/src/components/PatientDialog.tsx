import { useEffect } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  MenuItem,
  TextField,
} from '@mui/material';
import type { Patient, PatientInput } from '../api/types';

export const patientSchema = z.object({
  mrn: z
    .string()
    .trim()
    .min(1, 'MRN is required')
    .max(32)
    .regex(/^[A-Za-z0-9-]+$/, 'Letters, digits and hyphens only'),
  firstName: z.string().trim().min(1, 'First name is required').max(80),
  lastName: z.string().trim().min(1, 'Last name is required').max(80),
  dateOfBirth: z
    .string()
    .min(1, 'Date of birth is required')
    .refine((v) => !Number.isNaN(Date.parse(v)) && new Date(v) < new Date(), 'Must be a past date'),
  sex: z.enum(['FEMALE', 'MALE', 'OTHER']),
  phone: z
    .string()
    .max(32)
    .regex(/^[0-9+()\-\s]*$/, 'Invalid phone number')
    .optional()
    .or(z.literal('')),
  email: z.string().email('Invalid email').max(160).optional().or(z.literal('')),
  notes: z.string().max(1000).optional().or(z.literal('')),
});

type FormValues = z.infer<typeof patientSchema>;

const EMPTY: FormValues = {
  mrn: '',
  firstName: '',
  lastName: '',
  dateOfBirth: '',
  sex: 'FEMALE',
  phone: '',
  email: '',
  notes: '',
};

interface Props {
  open: boolean;
  patient?: Patient | null;
  onClose: () => void;
  onSubmit: (values: PatientInput) => Promise<void>;
}

export default function PatientDialog({ open, patient, onClose, onSubmit }: Props) {
  const {
    control,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(patientSchema), defaultValues: EMPTY });

  useEffect(() => {
    if (open) {
      reset(
        patient
          ? {
              mrn: patient.mrn,
              firstName: patient.firstName,
              lastName: patient.lastName,
              dateOfBirth: patient.dateOfBirth,
              sex: patient.sex,
              phone: patient.phone ?? '',
              email: patient.email ?? '',
              notes: patient.notes ?? '',
            }
          : EMPTY,
      );
    }
  }, [open, patient, reset]);

  const submit = handleSubmit(async (values) => {
    await onSubmit({
      ...values,
      phone: values.phone || undefined,
      email: values.email || undefined,
      notes: values.notes || undefined,
    });
  });

  const field = (name: keyof FormValues, label: string, props: Record<string, unknown> = {}) => (
    <Controller
      name={name}
      control={control}
      render={({ field: f }) => (
        <TextField
          {...f}
          {...props}
          label={label}
          fullWidth
          error={!!errors[name]}
          helperText={errors[name]?.message}
        />
      )}
    />
  );

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <form onSubmit={submit} noValidate>
        <DialogTitle>{patient ? 'Edit patient' : 'Add patient'}</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              {field('firstName', 'First name', { autoFocus: true })}
            </Grid>
            <Grid item xs={12} sm={6}>
              {field('lastName', 'Last name')}
            </Grid>
            <Grid item xs={12} sm={6}>
              {field('mrn', 'Medical record number')}
            </Grid>
            <Grid item xs={12} sm={6}>
              {field('dateOfBirth', 'Date of birth', { type: 'date', InputLabelProps: { shrink: true } })}
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="sex"
                control={control}
                render={({ field: f }) => (
                  <TextField {...f} select label="Sex" fullWidth>
                    <MenuItem value="FEMALE">Female</MenuItem>
                    <MenuItem value="MALE">Male</MenuItem>
                    <MenuItem value="OTHER">Other</MenuItem>
                  </TextField>
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              {field('phone', 'Phone')}
            </Grid>
            <Grid item xs={12}>
              {field('email', 'Email')}
            </Grid>
            <Grid item xs={12}>
              {field('notes', 'Notes', { multiline: true, minRows: 2 })}
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={isSubmitting}>
            {isSubmitting ? 'Saving...' : 'Save'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
