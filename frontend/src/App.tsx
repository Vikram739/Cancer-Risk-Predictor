import { Suspense, lazy } from 'react';
import { Route, Routes } from 'react-router-dom';
import ProtectedRoute from './auth/ProtectedRoute';
import Layout from './components/Layout';
import { LoadingState } from './components/StatusViews';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import { ForbiddenPage, NotFoundPage } from './pages/StatusPages';

const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const PatientsPage = lazy(() => import('./pages/PatientsPage'));
const NewAssessmentPage = lazy(() => import('./pages/NewAssessmentPage'));
const AssessmentResultPage = lazy(() => import('./pages/AssessmentResultPage'));
const PatientHistoryPage = lazy(() => import('./pages/PatientHistoryPage'));
const ModelInfoPage = lazy(() => import('./pages/ModelInfoPage'));

export default function App() {
  return (
    <Suspense fallback={<LoadingState />}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route index element={<DashboardPage />} />
          <Route path="patients" element={<PatientsPage />} />
          <Route path="patients/:id/history" element={<PatientHistoryPage />} />
          <Route
            path="assessments/new"
            element={
              <ProtectedRoute roles={['ADMIN', 'DOCTOR']}>
                <NewAssessmentPage />
              </ProtectedRoute>
            }
          />
          <Route path="assessments/:id" element={<AssessmentResultPage />} />
          <Route path="model" element={<ModelInfoPage />} />
          <Route path="forbidden" element={<ForbiddenPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
