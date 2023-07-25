# API reference

The backend publishes a full OpenAPI description at `/v3/api-docs` and Swagger UI at `/swagger-ui.html`. This page collects the requests you are most likely to need.

## Authentication

```http
POST /api/auth/login
Content-Type: application/json

{"email": "rohan.kapoor@cancerrisk.dev", "password": "Password@123"}
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {"id": 2, "fullName": "Dr. Rohan Kapoor", "email": "rohan.kapoor@cancerrisk.dev", "roles": ["DOCTOR"]}
}
```

Send the access token on every other call:

```http
Authorization: Bearer <accessToken>
```

When it expires, exchange the refresh token:

```http
POST /api/auth/refresh
Content-Type: application/json

{"refreshToken": "<refreshToken>"}
```

## Patients

```http
GET /api/patients?search=das&page=0&size=20&sort=lastName,asc
```

Sortable fields: `lastName`, `firstName`, `mrn`, `dateOfBirth`, `createdAt`. An unknown sort field returns `400`.

```http
POST /api/patients
Content-Type: application/json

{
  "mrn": "MRN-240101",
  "firstName": "Kavya",
  "lastName": "Menon",
  "dateOfBirth": "1980-02-11",
  "sex": "FEMALE",
  "phone": "+91 98450 11223",
  "email": "kavya@example.com",
  "notes": "Referred by GP"
}
```

MRNs are stored upper case and must be unique (`409` otherwise).

## Assessments

```http
POST /api/assessments
Content-Type: application/json

{
  "patientId": 19,
  "notes": "Follow up after imaging",
  "features": {
    "mean_radius": 13.37, "mean_texture": 18.84, "mean_perimeter": 86.24, "mean_area": 551.1,
    "mean_smoothness": 0.09587, "mean_compactness": 0.09263, "mean_concavity": 0.06154,
    "mean_concave_points": 0.0335, "mean_symmetry": 0.1792, "mean_fractal_dimension": 0.06154,
    "radius_error": 0.3242, "texture_error": 1.108, "perimeter_error": 2.287, "area_error": 24.53,
    "smoothness_error": 0.00638, "compactness_error": 0.02045, "concavity_error": 0.02589,
    "concave_points_error": 0.01093, "symmetry_error": 0.01873, "fractal_dimension_error": 0.003187,
    "worst_radius": 14.97, "worst_texture": 25.41, "worst_perimeter": 97.66, "worst_area": 686.5,
    "worst_smoothness": 0.1313, "worst_compactness": 0.2119, "worst_concavity": 0.2267,
    "worst_concave_points": 0.09993, "worst_symmetry": 0.2822, "worst_fractal_dimension": 0.08004
  }
}
```

All 30 features are required and must be non negative. Unknown keys are rejected.

```json
{
  "id": 112,
  "patientId": 19,
  "patientName": "Aditi Reddy",
  "patientMrn": "MRN-230019",
  "probability": 0.1973,
  "riskBand": "LOW",
  "prediction": "benign",
  "modelName": "logistic_regression",
  "modelVersion": "1.0.0",
  "topFeatures": [
    {"feature": "worst_radius", "value": 14.97, "shapValue": -0.621713, "direction": "decreases_risk"},
    {"feature": "mean_concave_points", "value": 0.0335, "shapValue": -0.268467, "direction": "decreases_risk"}
  ],
  "features": {"mean_radius": 13.37},
  "createdBy": "rohan.kapoor@cancerrisk.dev",
  "createdAt": "2023-07-26T05:41:09Z"
}
```

(arrays shortened)

```http
GET /api/assessments/export?from=2023-01-01&to=2023-06-30&patientId=19
```

Returns `text/csv` with one row per assessment: id, timestamp, patient, probability, band, model, top features and all 30 measurements.

## Statistics

| Endpoint | Response |
| --- | --- |
| `GET /api/stats/summary` | `totalPatients`, `totalAssessments`, `assessmentsByRiskBand`, `highRiskPatients` (latest assessment is HIGH), `averageProbability`, `lastAssessmentAt` |
| `GET /api/stats/monthly` | `[{"month": "2023-01", "total": 8, "low": 6, "moderate": 0, "high": 2, "averageProbability": 0.2629}]` |
| `GET /api/stats/model` | `{"available": true, "model": {...ML service /model/info...}}`, `available` is false when the ML service is down |

## ML service

```bash
curl -s localhost:5000/predict -H 'Content-Type: application/json' \
  -d '{"features": {...30 values...}, "reference": "MRN-230019"}'
```

```json
{
  "reference": "MRN-230019",
  "probability": 0.4655,
  "prediction": "benign",
  "risk_band": "MODERATE",
  "model_name": "logistic_regression",
  "model_version": "1.0.0",
  "top_features": [
    {"feature": "worst_texture", "value": 21.67491, "shap_value": -0.717315, "direction": "decreases_risk"}
  ]
}
```

Batch scoring:

```bash
curl -s localhost:5000/predict/batch -H 'Content-Type: application/json' \
  -d '{"records": [{"features": {...}, "reference": "a"}, {"features": {...}, "reference": "b"}]}'
```
