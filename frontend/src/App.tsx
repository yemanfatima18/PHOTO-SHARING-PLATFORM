import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import { Layout } from './layouts/Layout';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';
import { AdminEventDetailPage } from './pages/admin/AdminEventDetailPage';
import { AdminEventPhotosPage } from './pages/admin/AdminEventPhotosPage';
import { AdminEventMembersPage } from './pages/admin/AdminEventMembersPage';
import { AdminEventGalleryPage } from './pages/admin/AdminEventGalleryPage';
import { TeamDashboardPage } from './pages/team/TeamDashboardPage';
import { TeamEventPage } from './pages/team/TeamEventPage';
import { PublicGalleryPage } from './pages/public/PublicGalleryPage';
import { PublicGalleryPhotosPage } from './pages/public/PublicGalleryPhotosPage';

function PrivateRoute({ children, allowedRoles }: { children: React.ReactNode; allowedRoles?: string[] }) {
  const { isAuthenticated, isLoading, user } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-indigo-600 border-t-transparent"></div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}

function PublicRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-indigo-600 border-t-transparent"></div>
      </div>
    );
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
      <Route path="/register" element={<PublicRoute><RegisterPage /></PublicRoute>} />

      <Route
        path="/"
        element={
          <PrivateRoute>
            <Layout />
          </PrivateRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<AdminDashboardPage />} />
        <Route path="events/:eventId" element={<AdminEventDetailPage />} />
        <Route path="events/:eventId/photos" element={<AdminEventPhotosPage />} />
        <Route path="events/:eventId/members" element={<AdminEventMembersPage />} />
        <Route path="events/:eventId/gallery" element={<AdminEventGalleryPage />} />
      </Route>

      <Route
        path="/team"
        element={
          <PrivateRoute allowedRoles={['TEAM_MEMBER']}>
            <Layout />
          </PrivateRoute>
        }
      >
        <Route path="dashboard" element={<TeamDashboardPage />} />
        <Route path="events/:eventId" element={<TeamEventPage />} />
      </Route>

      <Route path="/gallery/:token" element={<PublicGalleryPage />} />
      <Route path="/gallery/:token/photos" element={<PublicGalleryPhotosPage />} />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return <AppRoutes />;
}