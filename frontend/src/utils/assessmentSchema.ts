import { z } from 'zod';
import { FEATURE_NAMES, featureLabel, maxAllowed } from './features';

const featureShape = Object.fromEntries(
  FEATURE_NAMES.map((name) => [
    name,
    z.coerce
      .number({ invalid_type_error: `${featureLabel(name)} must be a number` })
      .min(0, 'Must be zero or more')
      .max(maxAllowed(name), `Unusually large, check the value (max ${maxAllowed(name)})`),
  ]),
) as Record<string, z.ZodNumber>;

export const assessmentSchema = z.object({
  patientId: z.coerce.number({ invalid_type_error: 'Select a patient' }).int().positive('Select a patient'),
  notes: z.string().max(1000).optional(),
  features: z.object(featureShape),
});

export type AssessmentFormValues = z.infer<typeof assessmentSchema>;
