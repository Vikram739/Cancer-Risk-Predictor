import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link as RouterLink, Navigate, useNavigate } from 'react-router-dom';
import { Alert, Button, Link, MenuItem, Stack, TextField, Typography } from '@mui/material';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import AuthLayout from './AuthLayout';

export const registerSchema = z
  .object({
    fullName: z.string().trim().min(2, 'Enter your full name').max(120),
    email: z.string().trim().min(1, 'Email is required').email('Enter a valid email'),
    password: z
      .string()
      .min(8, 'At least 8 characters')
      .max(72)
      .regex(/[A-Za-z]/, 'Include at least one letter')
      .regex(/\d/, 'Include at least one digit'),
    confirmPassword: z.string(),
    role: z.enum(['DOCTOR', 'ANALYST']),
  })
  .refine((v) => v.password === v.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

type FormValues = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const { register: registerUser, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    register,
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { fullName: '', email: '', password: '', confirmPassword: '', role: 'DOCTOR' },
  });

  if (isAuthenticated) return <Navigate to="/" replace />;

  const onSubmit = handleSubmit(async ({ confirmPassword: _ignored, ...body }) => {
    setServerError(null);
    try {
      await registerUser(body);
      navigate('/', { replace: true });
    } catch (err) {
      setServerError(errorMessage(err, 'Registration failed'));
    }
  });

  return (
    <AuthLayout title="Create account" subtitle="Doctors can run assessments, analysts have read only access">
      <form onSubmit={onSubmit} noValidate>
        <Stack spacing={2}>
          {serverError && <Alert severity="error">{serverError}</Alert>}
          <TextField label="Full name" {...register('fullName')} error={!!errors.fullName} helperText={errors.fullName?.message} />
          <TextField label="Email" type="email" {...register('email')} error={!!errors.email} helperText={errors.email?.message} />
          <Controller
            name="role"
            control={control}
            render={({ field }) => (
              <TextField {...field} select label="Role">
                <MenuItem value="DOCTOR">Doctor</MenuItem>
                <MenuItem value="ANALYST">Analyst</MenuItem>
              </TextField>
            )}
          />
          <TextField
            label="Password"
            type="password"
            autoComplete="new-password"
            {...register('password')}
            error={!!errors.password}
            helperText={errors.password?.message ?? 'At least 8 characters with a letter and a digit'}
          />
          <TextField
            label="Confirm password"
            type="password"
            autoComplete="new-password"
            {...register('confirmPassword')}
            error={!!errors.confirmPassword}
            helperText={errors.confirmPassword?.message}
          />
          <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
            {isSubmitting ? 'Creating account...' : 'Create account'}
          </Button>
          <Typography variant="body2" textAlign="center">
            Already registered?{' '}
            <Link component={RouterLink} to="/login">
              Sign in
            </Link>
          </Typography>
        </Stack>
      </form>
    </AuthLayout>
  );
}
