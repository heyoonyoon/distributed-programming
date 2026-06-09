import { Navigate, Route, Routes } from 'react-router-dom'
import { CustomerShell } from '../components/layout/CustomerShell'
import { EmployeeShell } from '../components/layout/EmployeeShell'
import { useSession } from '../features/auth/hooks/useSession'
import { LoginPage } from '../features/auth/components/LoginPage'
import { CustomerHomePage } from '../features/customer-home/components/CustomerHomePage'
import { CustomerContractsPage } from '../features/contracts/components/CustomerContractsPage'
import { CustomerClaimsPage } from '../features/claims/components/CustomerClaimsPage'
import { ProfilePage } from '../features/profile/components/ProfilePage'
import { EmployeeReviewsPage } from '../features/underwriting-review/components/EmployeeReviewsPage'
import { EmployeeBenefitReviewsPage } from '../features/benefit-review/components/EmployeeBenefitReviewsPage'
import { homePath } from '../lib/session'

function App() {
  const { session, setSession, logout, handleUnauthorized } = useSession()

  return (
    <Routes>
      <Route path="/login" element={<LoginPage session={session} onLogin={setSession} />} />
      <Route
        path="/customer/*"
        element={
          session ? (
            <CustomerShell session={session} onLogout={logout}>
              <Routes>
                <Route
                  path="/home"
                  element={<CustomerHomePage token={session.token} onUnauthorized={handleUnauthorized} />}
                />
                <Route
                  path="/products"
                  element={
                    <CustomerContractsPage
                      token={session.token}
                      onUnauthorized={handleUnauthorized}
                      view="products"
                    />
                  }
                />
                <Route path="/apply" element={<Navigate to="/customer/products" replace />} />
                <Route
                  path="/applications"
                  element={
                    <CustomerContractsPage
                      token={session.token}
                      onUnauthorized={handleUnauthorized}
                      view="applications"
                    />
                  }
                />
                <Route
                  path="/contracts"
                  element={
                    <CustomerContractsPage
                      token={session.token}
                      onUnauthorized={handleUnauthorized}
                      view="contracts"
                    />
                  }
                />
                <Route
                  path="/claims/health"
                  element={
                    <CustomerClaimsPage token={session.token} onUnauthorized={handleUnauthorized} view="health" />
                  }
                />
                <Route
                  path="/claims/car-accident"
                  element={
                    <CustomerClaimsPage token={session.token} onUnauthorized={handleUnauthorized} view="car" />
                  }
                />
                <Route
                  path="/claims/status"
                  element={
                    <CustomerClaimsPage token={session.token} onUnauthorized={handleUnauthorized} view="status" />
                  }
                />
                <Route
                  path="/claims/history"
                  element={
                    <CustomerClaimsPage token={session.token} onUnauthorized={handleUnauthorized} view="history" />
                  }
                />
                <Route
                  path="/claims/benefit-analysis"
                  element={
                    <CustomerClaimsPage token={session.token} onUnauthorized={handleUnauthorized} view="analysis" />
                  }
                />
                <Route path="/claims" element={<Navigate to="/customer/claims/health" replace />} />
                <Route
                  path="/profile"
                  element={<ProfilePage token={session.token} onUnauthorized={handleUnauthorized} />}
                />
                <Route path="*" element={<Navigate to="/customer/home" replace />} />
              </Routes>
            </CustomerShell>
          ) : (
            <Navigate to="/login" replace />
          )
        }
      />
      <Route
        path="/employee/*"
        element={
          session ? (
            <EmployeeShell session={session} onLogout={logout}>
              <Routes>
                <Route
                  path="/reviews"
                  element={<EmployeeReviewsPage token={session.token} onUnauthorized={handleUnauthorized} />}
                />
                <Route
                  path="/benefit-reviews"
                  element={<EmployeeBenefitReviewsPage token={session.token} onUnauthorized={handleUnauthorized} />}
                />
                <Route path="*" element={<Navigate to="/employee/reviews" replace />} />
              </Routes>
            </EmployeeShell>
          ) : (
            <Navigate to="/login" replace />
          )
        }
      />
      <Route
        path="*"
        element={session ? <Navigate to={homePath(session)} replace /> : <Navigate to="/login" replace />}
      />
    </Routes>
  )
}

export default App
