import {
  AlertTriangle,
  ArrowRight,
  BadgeCheck,
  Bell,
  BriefcaseBusiness,
  CalendarDays,
  Car,
  CheckCircle2,
  ClipboardCheck,
  CreditCard,
  Download,
  FileText,
  HeartPulse,
  LogOut,
  Receipt,
  Search,
  Send,
  ShieldCheck,
  Users,
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import {
  Link,
  Navigate,
  NavLink,
  Route,
  Routes,
  useNavigate,
} from 'react-router-dom'
import { ApiError, apiClient, decodeUserType, employeeIdFromToken } from './api/apiClient'
import './App.css'
import type {
  AuthSession,
  BenefitAnalysis,
  CarAccidentReportResponse,
  ClaimListItem,
  BenefitReviewDetail,
  BenefitReviewResult,
  BenefitReviewSummary,
  UnassignedBenefitReview,
  ConfirmBenefitReviewResponse,
  ConfirmReviewResponse,
  ContractDetail,
  ContractSummary,
  HealthClaimResponse,
  MyApplication,
  PayableContract,
  PaymentMethod,
  PendingReview,
  PolicyholderProfile,
  ProductDetail,
  ProductSummary,
  ProductType,
  ReviewApplicationDetail,
  ReviewResult,
  UnpaidContract,
} from './types'

const tokenKey = 'insurance.authToken'

const maxClaimAttachmentSize = 10 * 1024 * 1024
const acceptedClaimFileTypes = new Set([
  'application/pdf',
  'image/jpeg',
  'image/png',
])

function readSession(): AuthSession | null {
  const token = window.localStorage.getItem(tokenKey)
  return token ? { token, userType: decodeUserType(token) } : null
}

function homePath(session: AuthSession) {
  return session.userType === 'EMPLOYEE' ? '/employee/reviews' : '/customer/home'
}

function formatCurrency(value: number) {
  return `${value.toLocaleString('ko-KR')}원`
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString('ko-KR')
}

function formatProductType(value: ProductType) {
  return value === 'HEALTH' ? '의료보험' : '자동차보험'
}

function formatClaimType(value: string) {
  return value === 'HEALTH' ? '의료' : '자동차'
}

function formatContractStatus(value: string) {
  const labels: Record<string, string> = {
    ACTIVE: '정상',
    SUSPENDED: '정지',
    TERMINATED: '해지',
    PENDING: '대기',
    APPROVED: '승인',
    REJECTED: '거절',
    CANCELLED: '취소',
    COMPLETED: '완료',
    FAILED: '실패',
    IN_REVIEW: '심사중',
    SIMPLE: '간편',
    COMPLEX: '심사필요',
    CARD: '카드',
    TRANSFER: '계좌이체',
    AUTO_DEBIT: '자동이체',
  }

  return labels[value] ?? value
}

function formatInputDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function describeBenefitReviewResult(result: ConfirmBenefitReviewResponse) {
  if (result.claimStatus === 'COMPLETED') {
    return '지급 완료'
  }

  if (result.claimStatus === 'FAILED') {
    return '지급 실패, 계좌 확인 후 재시도'
  }

  if (result.claimStatus === 'REJECTED') {
    return '반려 완료'
  }

  return `청구 상태 ${result.claimStatus}`
}

function isAcceptedClaimFile(file: File) {
  if (acceptedClaimFileTypes.has(file.type)) {
    return true
  }

  return /\.(pdf|jpe?g|png)$/i.test(file.name)
}

function describeHealthClaimResult(result: HealthClaimResponse) {
  if (result.complexity === 'COMPLEX') {
    return '복잡한 청구로 접수되었습니다. 담당자 배정 후 심사를 진행합니다.'
  }

  if (result.status === 'COMPLETED') {
    return '보험금이 지급되었습니다.'
  }

  if (result.status === 'FAILED') {
    return '지급에 실패했습니다. 계좌 정보를 확인해 주세요.'
  }

  return '청구가 접수되었습니다.'
}

function LoginPage({
  session,
  onLogin,
}: {
  session: AuthSession | null
  onLogin: (session: AuthSession) => void
}) {
  const navigate = useNavigate()
  const [email, setEmail] = useState('hong@test.com')
  const [password, setPassword] = useState('1234')
  const [error, setError] = useState('')
  const [isSubmitting, setSubmitting] = useState(false)
  const selectedLoginType =
    email === 'staff@test.com' ? 'employee' : email === 'hong@test.com' ? 'policyholder' : null

  if (session) {
    return <Navigate to={homePath(session)} replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSubmitting(true)

    try {
      const { token } = await apiClient.login({ email, password })
      const nextSession = { token, userType: decodeUserType(token) }
      window.localStorage.setItem(tokenKey, token)
      onLogin(nextSession)
      navigate(homePath(nextSession))
    } catch (err) {
      setError(err instanceof Error ? err.message : '로그인에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="login-screen">
      <section className="login-panel">
        <div className="login-copy">
          <div className="mark">
            <ShieldCheck size={28} />
          </div>
          <h1>보험 업무 시스템</h1>
          <p>고객 포털과 보험사 직원 업무 화면을 분리한 데모 환경입니다.</p>
        </div>
        <form className="login-form" onSubmit={handleSubmit}>
          <div className="account-switch">
            <button
              aria-pressed={selectedLoginType === 'policyholder'}
              className={selectedLoginType === 'policyholder' ? 'is-selected' : ''}
              type="button"
              onClick={() => setEmail('hong@test.com')}
            >
              가입자
            </button>
            <button
              aria-pressed={selectedLoginType === 'employee'}
              className={selectedLoginType === 'employee' ? 'is-selected' : ''}
              type="button"
              onClick={() => setEmail('staff@test.com')}
            >
              직원
            </button>
          </div>
          <label>
            이메일
            <input
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              type="email"
            />
          </label>
          <label>
            비밀번호
            <input
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              type="password"
            />
          </label>
          {error ? <p className="form-error">{error}</p> : null}
          <button className="primary-button" disabled={isSubmitting} type="submit">
            {isSubmitting ? '로그인 중' : '로그인'}
            <ArrowRight size={18} />
          </button>
        </form>
      </section>
    </main>
  )
}

function CustomerShell({
  session,
  onLogout,
  children,
}: {
  session: AuthSession
  onLogout: () => void
  children: ReactNode
}) {
  if (session.userType !== 'POLICYHOLDER') {
    return <Navigate to={homePath(session)} replace />
  }

  return (
    <div className="customer-shell">
      <header className="customer-nav">
        <Link className="brand" to="/customer/home" aria-label="고객 홈">
          <ShieldCheck size={22} />
          <span>보험</span>
        </Link>
        <nav aria-label="고객 메뉴">
          <NavLink to="/customer/home">홈</NavLink>
          <NavLink to="/customer/products">상품/가입</NavLink>
          <NavLink to="/customer/applications">신청 내역</NavLink>
          <NavLink to="/customer/contracts">계약/납부</NavLink>
          <NavLink to="/customer/claims/health">의료청구</NavLink>
          <NavLink to="/customer/claims/car-accident">사고접수</NavLink>
          <NavLink to="/customer/claims/status">보상현황</NavLink>
          <NavLink to="/customer/claims/history">보상이력</NavLink>
          {/* 데모 중 임시 숨김 — 라우트/페이지는 그대로, 탭만 비표시 */}
          {/* <NavLink to="/customer/claims/benefit-analysis">실익분석</NavLink> */}
          <NavLink to="/customer/profile">내 정보</NavLink>
        </nav>
        <button className="text-button" type="button" onClick={onLogout}>
          <LogOut size={17} />
          로그아웃
        </button>
      </header>
      <main className="customer-content">{children}</main>
    </div>
  )
}

function EmployeeShell({
  session,
  onLogout,
  children,
}: {
  session: AuthSession
  onLogout: () => void
  children: ReactNode
}) {
  if (session.userType !== 'EMPLOYEE') {
    return <Navigate to={homePath(session)} replace />
  }

  return (
    <div className="employee-shell">
      <aside className="sidebar">
        <Link className="brand" to="/employee/reviews" aria-label="심사 대기 목록">
          <BriefcaseBusiness size={22} />
          <span>직원</span>
        </Link>
        <nav className="nav-list" aria-label="직원 메뉴">
          <NavLink to="/employee/reviews">
            <ClipboardCheck size={18} />
            가입 심사
          </NavLink>
          <NavLink to="/employee/benefit-reviews">
            <Receipt size={18} />
            보험금 심사
          </NavLink>
        </nav>
        <div className="sidebar-footer">
          <div>
            <strong>보험사 직원</strong>
            <span>직원 포털</span>
          </div>
          <button className="icon-button" type="button" onClick={onLogout}>
            <LogOut size={18} />
            <span className="sr-only">로그아웃</span>
          </button>
        </div>
      </aside>
      <main className="content">{children}</main>
    </div>
  )
}

function CustomerHomePage({
  token,
  onUnauthorized,
}: {
  token: string
  onUnauthorized: () => void
}) {
  const [summary, setSummary] = useState<{
    total: number
    health: number
    car: number
  } | null>(null)
  const actions = [
    { label: '상품 조회 및 가입 신청', icon: Search, path: '/customer/products' },
    { label: '신청 내역', icon: Receipt, path: '/customer/applications' },
    { label: '의료보험 청구', icon: HeartPulse, path: '/customer/claims/health' },
    { label: '자동차사고 접수', icon: Car, path: '/customer/claims/car-accident' },
    { label: '보상 현황', icon: ClipboardCheck, path: '/customer/claims/status' },
    { label: '보상 이력', icon: CalendarDays, path: '/customer/claims/history' },
    { label: '보험료 납부', icon: CreditCard, path: '/customer/contracts' },
  ]

  useEffect(() => {
    let isMounted = true

    async function loadSummary() {
      try {
        const contracts = await apiClient.getContracts(token)
        const activeContracts = contracts.filter((contract) => contract.status === 'ACTIVE')

        if (!isMounted) {
          return
        }

        setSummary({
          total: activeContracts.length,
          health: activeContracts.filter((contract) => contract.productType === 'HEALTH').length,
          car: activeContracts.filter((contract) => contract.productType === 'CAR').length,
        })
      } catch (err) {
        if (err instanceof ApiError && err.status === 401) {
          onUnauthorized()
          return
        }

        if (isMounted) {
          setSummary({ total: 0, health: 0, car: 0 })
        }
      }
    }

    loadSummary()

    return () => {
      isMounted = false
    }
  }, [onUnauthorized, token])

  return (
    <section className="customer-home">
      <div className="customer-hero">
        <span className="eyebrow">가입자 포털</span>
        <h1>고객님, 필요한 보험 업무를 바로 처리하세요.</h1>
        <p>계약 확인, 청구 접수, 보험료 납부를 고객용 화면에서 진행합니다.</p>
      </div>

      <div className="action-grid">
        {actions.map(({ label, icon: Icon, path }) => (
          <Link className="action-tile" to={path} key={label}>
            <Icon size={22} />
            <span>{label}</span>
            <ArrowRight size={18} />
          </Link>
        ))}
      </div>

      <section className="status-panel">
        <div>
          <BadgeCheck size={22} />
          <h2>
            {summary ? `현재 유효한 계약 ${summary.total}건` : '현재 유효한 계약을 불러오는 중입니다.'}
          </h2>
          <p>
            {summary
              ? summary.total > 0
                ? `의료보험 ${summary.health}건, 자동차보험 ${summary.car}건이 정상 유지 중입니다.`
                : '현재 유효한 계약이 없습니다.'
              : '가입자 계약 현황을 계산하고 있습니다.'}
          </p>
        </div>
        <Link className="secondary-button" to="/customer/contracts">
          계약 보기
        </Link>
      </section>
    </section>
  )
}

type CustomerContractView = 'products' | 'applications' | 'contracts'

function CustomerContractsPage({
  token,
  onUnauthorized,
  view,
}: {
  token: string
  onUnauthorized: () => void
  view: CustomerContractView
}) {
  const [productType, setProductType] = useState<ProductType>('HEALTH')
  const [keyword, setKeyword] = useState('')
  const [minPremium, setMinPremium] = useState('')
  const [maxPremium, setMaxPremium] = useState('')
  const [products, setProducts] = useState<ProductSummary[]>([])
  const [selectedProduct, setSelectedProduct] = useState<ProductDetail | null>(null)
  const [hasSearchedProducts, setSearchedProducts] = useState(false)
  const [applications, setApplications] = useState<MyApplication[]>([])
  const [contracts, setContracts] = useState<ContractSummary[]>([])
  const [selectedContract, setSelectedContract] = useState<ContractDetail | null>(null)
  const [unpaidContracts, setUnpaidContracts] = useState<UnpaidContract[]>([])
  const [selectedUnpaid, setSelectedUnpaid] = useState<UnpaidContract | null>(null)
  const [payableContracts, setPayableContracts] = useState<PayableContract[]>([])
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CARD')
  const [paymentInfo, setPaymentInfo] = useState('1234-5678-9012-3456')
  const [autoDebitAccount, setAutoDebitAccount] = useState('110-222-333333')
  const [withdrawalDay, setWithdrawalDay] = useState('25')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [isLoading, setLoading] = useState(false)
  const [isContractLoading, setContractLoading] = useState(false)
  const [isPaymentSubmitting, setPaymentSubmitting] = useState(false)
  const [isAutoDebitSubmitting, setAutoDebitSubmitting] = useState(false)
  const selectedPayable = selectedContract
    ? payableContracts.find((contract) => contract.contractId === selectedContract.contractId)
    : null

  async function loadProducts() {
    setError('')
    setLoading(true)
    setSearchedProducts(true)

    try {
      const list = await apiClient.getProducts({
        type: productType,
        keyword,
        minPremium,
        maxPremium,
      })
      setProducts(list)

      if (list[0]) {
        setSelectedProduct(await apiClient.getProduct(list[0].id))
      } else {
        setSelectedProduct(null)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '상품 조회에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  async function loadApplications() {
    try {
      setApplications(await apiClient.getMyApplications(token))
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '신청 목록 조회에 실패했습니다.')
    }
  }

  async function loadContractDetail(id: number) {
    setError('')
    setContractLoading(true)

    try {
      const [detail, unpaid] = await Promise.all([
        apiClient.getContract(token, id),
        apiClient.getUnpaidContract(token, id).catch((err) => {
          if (err instanceof ApiError && err.status === 404) {
            return null
          }
          throw err
        }),
      ])

      setSelectedContract(detail)
      setSelectedUnpaid(unpaid)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '계약 상세 조회에 실패했습니다.')
    } finally {
      setContractLoading(false)
    }
  }

  async function loadContractData() {
    setContractLoading(true)

    try {
      const [contractList, unpaidList, payableList] = await Promise.all([
        apiClient.getContracts(token),
        apiClient.getUnpaidContracts(token),
        apiClient.getPayableContracts(token),
      ])

      setContracts(contractList)
      setUnpaidContracts(unpaidList)
      setPayableContracts(payableList)
      setSelectedContract(null)
      setSelectedUnpaid(null)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '계약 정보를 조회하지 못했습니다.')
    } finally {
      setContractLoading(false)
    }
  }

  useEffect(() => {
    void Promise.resolve().then(() => {
      loadApplications()
      loadContractData()
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function changeProductType(type: ProductType) {
    setProductType(type)
    setProducts([])
    setSelectedProduct(null)
    setSearchedProducts(false)
    setError('')
  }

  async function selectProduct(id: number) {
    setError('')
    setSelectedProduct(await apiClient.getProduct(id))
  }

  async function submitApplication(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedProduct) {
      return
    }

    const form = new FormData(event.currentTarget)
    const request =
      selectedProduct.productType === 'HEALTH'
        ? {
            productId: selectedProduct.id,
            medicalHistory: {
              currentConditions: String(form.get('currentConditions')),
              pastHospitalization: String(form.get('pastHospitalization')),
              medications: String(form.get('medications')),
            },
          }
        : {
            productId: selectedProduct.id,
            vehicleInfo: {
              plateNumber: String(form.get('plateNumber')),
              vehicleType: String(form.get('vehicleType')),
              modelYear: Number(form.get('modelYear')),
              drivingExperienceYears: Number(form.get('drivingExperienceYears')),
            },
          }

    try {
      const created = await apiClient.createApplication(token, request)
      setSuccess(`가입 신청 ${created.applicationId}번이 ${created.status} 상태로 접수되었습니다.`)
      await loadApplications()
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '가입 신청에 실패했습니다.')
    }
  }

  async function cancelApplication(id: number) {
    setError('')
    setSuccess('')

    try {
      await apiClient.cancelApplication(token, id)
      setSuccess(`가입 신청 ${id}번을 취소했습니다.`)
      await loadApplications()
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '신청 취소에 실패했습니다.')
    }
  }

  async function downloadContract(id: number) {
    setError('')
    setSuccess('')

    try {
      const { blob, filename } = await apiClient.downloadContract(token, id)
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      link.click()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '계약서 다운로드에 실패했습니다.')
    }
  }

  async function submitPayment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedContract) {
      return
    }

    setError('')
    setSuccess('')
    setPaymentSubmitting(true)

    try {
      const result = await apiClient.payPremium(token, selectedContract.contractId, {
        method: paymentMethod,
        paymentInfo,
      })

      if (result.status === 'FAILED') {
        setError(`결제 실패: ${result.reason ?? '승인되지 않았습니다.'}`)
      } else {
        setSuccess(`납부 ${result.paymentId}번이 완료되었습니다. (${formatCurrency(result.amount)})`)
      }

      await loadContractData()
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '보험료 납부에 실패했습니다.')
    } finally {
      setPaymentSubmitting(false)
    }
  }

  async function submitAutoDebit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedContract) {
      return
    }

    setError('')
    setSuccess('')
    setAutoDebitSubmitting(true)

    try {
      await apiClient.registerAutoDebit(token, selectedContract.contractId, {
        account: autoDebitAccount,
        withdrawalDay: Number(withdrawalDay),
      })
      setSuccess('자동이체가 등록되었습니다.')
      await loadContractDetail(selectedContract.contractId)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '자동이체 등록에 실패했습니다.')
    } finally {
      setAutoDebitSubmitting(false)
    }
  }

  const pageMeta: Record<CustomerContractView, { eyebrow: string; title: string }> = {
    products: { eyebrow: 'UC01 / UC02', title: '보험 상품 조회 및 가입 신청' },
    applications: { eyebrow: 'UC02', title: '가입 신청 내역' },
    contracts: { eyebrow: 'A-2 / A-3', title: '내 계약 및 보험료 납부' },
  }

  return (
    <section className="page">
      <div className="page-header">
        <div>
          <span className="eyebrow">{pageMeta[view].eyebrow}</span>
          <h1>{pageMeta[view].title}</h1>
        </div>
      </div>

      {view === 'products' ? (
      <div className="split-layout">
        <section className="panel">
          <div className="section-title">
            <Search size={18} />
            <h2>보험 상품 조회</h2>
          </div>
          <form className="filter-form" onSubmit={(event) => {
            event.preventDefault()
            loadProducts()
          }}>
            <div className="account-switch">
              <button
                aria-pressed={productType === 'HEALTH'}
                className={productType === 'HEALTH' ? 'is-selected' : ''}
                type="button"
                onClick={() => changeProductType('HEALTH')}
              >
                의료보험
              </button>
              <button
                aria-pressed={productType === 'CAR'}
                className={productType === 'CAR' ? 'is-selected' : ''}
                type="button"
                onClick={() => changeProductType('CAR')}
              >
                자동차보험
              </button>
            </div>
            <label>
              검색어
              <input value={keyword} onChange={(event) => setKeyword(event.target.value)} />
            </label>
            <div className="inline-fields">
              <label>
                최소 보험료
                <input value={minPremium} onChange={(event) => setMinPremium(event.target.value)} />
              </label>
              <label>
                최대 보험료
                <input value={maxPremium} onChange={(event) => setMaxPremium(event.target.value)} />
              </label>
            </div>
            <button className="secondary-button" type="submit">
              필터 적용
            </button>
          </form>
          <div className="product-tabs">
            {isLoading ? <p>상품을 불러오는 중입니다.</p> : null}
            {!isLoading && !hasSearchedProducts ? <p>조건을 선택한 뒤 필터 적용을 눌러 조회하세요.</p> : null}
            {products.map((product) => (
              <button
                aria-pressed={product.id === selectedProduct?.id}
                className={product.id === selectedProduct?.id ? 'is-selected' : ''}
                key={product.id}
                type="button"
                onClick={() => selectProduct(product.id)}
              >
                {product.productName}
                <span>{product.coverageSummary} · {product.monthlyPremium.toLocaleString()}원</span>
              </button>
            ))}
            {hasSearchedProducts && products.length === 0 && !isLoading ? <p>조회된 상품이 없습니다.</p> : null}
          </div>
        </section>

        <form className="panel form-panel" onSubmit={submitApplication}>
          <div className="section-title">
            <FileText size={18} />
            <h2>상품 상세 및 가입 신청</h2>
          </div>
          {selectedProduct ? (
            <>
              <article className="detail-card">
                <span className="badge">{formatProductType(selectedProduct.productType)}</span>
                <h3>{selectedProduct.productName}</h3>
                <p>{selectedProduct.description}</p>
                <strong>{selectedProduct.monthlyPremium.toLocaleString()}원 / 월</strong>
                <ul>
                  {selectedProduct.coverageItems.map((item) => (
                    <li key={item.itemName}>
                      <CheckCircle2 size={16} />
                      {item.itemName} · 한도 {item.coverageLimit.toLocaleString()}원 · 자기부담 {item.deductible.toLocaleString()}원
                    </li>
                  ))}
                </ul>
              </article>
              {selectedProduct.productType === 'HEALTH' ? (
                <>
                  <label>
                    현재 병력
                    <input name="currentConditions" defaultValue="없음" />
                  </label>
                  <label>
                    과거 입원 이력
                    <input name="pastHospitalization" defaultValue="없음" />
                  </label>
                  <label>
                    복용 중인 약물
                    <input name="medications" defaultValue="없음" />
                  </label>
                </>
              ) : (
                <>
                  <label>
                    차량번호
                    <input name="plateNumber" defaultValue="12가3456" />
                  </label>
                  <label>
                    차종
                    <input name="vehicleType" defaultValue="승용차" />
                  </label>
                  <div className="inline-fields">
                    <label>
                      연식
                      <input name="modelYear" defaultValue="2020" />
                    </label>
                    <label>
                      운전경력
                      <input name="drivingExperienceYears" defaultValue="5" />
                    </label>
                  </div>
                </>
              )}
              <button className="primary-button" type="submit">
                가입 신청
                <ArrowRight size={18} />
              </button>
            </>
          ) : (
            <p>상품을 먼저 조회하고 선택하세요.</p>
          )}
          {error ? <p className="form-error">{error}</p> : null}
          {success ? <p className="form-success">{success}</p> : null}
        </form>
      </div>
      ) : null}

      {view === 'applications' ? (
      <section className="panel">
        <div className="section-title">
          <Receipt size={18} />
          <h2>내 가입 신청</h2>
        </div>
        <div className="customer-list compact">
          {applications.map((application) => (
            <article key={application.applicationId}>
              <FileText size={20} />
              <div>
                <strong>{application.productName}</strong>
                <span>신청번호 {application.applicationId} · {formatContractStatus(application.status)}</span>
                <small>{new Date(application.appliedAt).toLocaleString()}</small>
              </div>
              {application.status === 'PENDING' ? (
                <button
                  className="secondary-button"
                  type="button"
                  onClick={() => cancelApplication(application.applicationId)}
                >
                  취소
                </button>
              ) : null}
            </article>
          ))}
          {applications.length === 0 ? <p>가입 신청 내역이 없습니다.</p> : null}
        </div>
      </section>
      ) : null}

      {view === 'contracts' ? (
      <section className="panel">
        <div className="section-title">
          <Download size={18} />
          <h2>내 계약 및 보험료 납부</h2>
        </div>
        {isContractLoading ? <p>계약 정보를 불러오는 중입니다.</p> : null}
        <div className="contract-workspace">
          <div className="customer-list compact">
            {contracts.map((contract) => (
              <article
                className={
                  contract.contractId === selectedContract?.contractId ? 'is-selected' : ''
                }
                key={contract.contractId}
              >
                <Receipt size={20} />
                <div>
                  <strong>{contract.productName}</strong>
                  <span>
                    {formatDate(contract.startDate)} - {formatDate(contract.endDate)} · 월{' '}
                    {formatCurrency(contract.monthlyPremium)}
                  </span>
                  <small>
                    {formatProductType(contract.productType)} · {formatContractStatus(contract.status)}
                  </small>
                </div>
                <button
                  aria-pressed={contract.contractId === selectedContract?.contractId}
                  className="secondary-button"
                  type="button"
                  onClick={() => loadContractDetail(contract.contractId)}
                >
                  상세보기
                </button>
              </article>
            ))}
            {contracts.length === 0 ? <p>유효한 계약이 없습니다.</p> : null}
          </div>

          {selectedContract ? (
            <>
              <div className="contract-detail-grid">
                <section className="payment-box">
                  <div>
                    <span className="badge">계약 상세</span>
                    <h3>{selectedContract.productName}</h3>
                  <p>
                    계약번호 {selectedContract.contractId} · 납부 방법{' '}
                    {formatContractStatus(selectedContract.paymentMethod)}
                  </p>
                  </div>
                  <ul className="coverage-list">
                    {selectedContract.coverageItems.map((item) => (
                      <li key={item.itemName}>
                        <CheckCircle2 size={16} />
                        {item.itemName} · 한도 {formatCurrency(item.coverageLimit)} · 자기부담{' '}
                        {formatCurrency(item.deductible)}
                      </li>
                    ))}
                  </ul>
                  <button
                    className="secondary-button"
                    type="button"
                    onClick={() => downloadContract(selectedContract.contractId)}
                  >
                    계약서 다운로드
                    <Download size={16} />
                  </button>
                </section>

                <section className="payment-box">
                  <div>
                    <span className="badge">미납/연체</span>
                    <h3>
                      {selectedUnpaid
                        ? formatCurrency(selectedUnpaid.unpaidPrincipal)
                        : '연체 없음'}
                    </h3>
                    <p>
                      {selectedUnpaid
                        ? `기한 ${formatDate(selectedUnpaid.dueDate)} · ${selectedUnpaid.overdueDays}일 연체 · 이자 ${formatCurrency(selectedUnpaid.overdueInterest)}`
                        : '선택한 계약에 표시할 연체 내역이 없습니다.'}
                    </p>
                  </div>
                  <div className="unpaid-strip">
                    {unpaidContracts.map((contract) => (
                      <button
                        type="button"
                        key={contract.contractId}
                        onClick={() => loadContractDetail(contract.contractId)}
                      >
                        <AlertTriangle size={16} />
                        <span>{contract.productName}</span>
                        <strong>{formatCurrency(contract.unpaidPrincipal)}</strong>
                      </button>
                    ))}
                    {unpaidContracts.length === 0 ? <span>전체 연체 내역 없음</span> : null}
                  </div>
                </section>
              </div>
              <div className="contract-detail-grid">
              <form className="payment-box" onSubmit={submitPayment}>
                <div>
                  <span className="badge">보험료 납부</span>
                  <h3>한 회차 납부</h3>
                  <p>
                    {selectedPayable
                      ? `납부 예정 ${formatCurrency(selectedPayable.amount)}`
                      : '현재 납부할 미납 회차가 없습니다.'}
                  </p>
                </div>
                <div className="inline-fields">
                  <label>
                    납부 방법
                    <select
                      value={paymentMethod}
                      onChange={(event) => setPaymentMethod(event.target.value as PaymentMethod)}
                    >
                      <option value="CARD">카드</option>
                      <option value="TRANSFER">계좌이체</option>
                      <option value="AUTO_DEBIT">자동이체</option>
                    </select>
                  </label>
                  <label>
                    결제 정보
                    <input
                      value={paymentInfo}
                      onChange={(event) => setPaymentInfo(event.target.value)}
                    />
                  </label>
                </div>
                <button
                  className="primary-button"
                  disabled={isPaymentSubmitting}
                  type="submit"
                >
                  {isPaymentSubmitting ? '처리 중' : '납부하기'}
                  <CreditCard size={18} />
                </button>
              </form>

              <form className="payment-box" onSubmit={submitAutoDebit}>
                <div>
                  <span className="badge">자동이체</span>
                  <h3>출금 계좌 등록</h3>
                  <p>등록 후 계약 상세의 납부 방법이 자동이체로 표시됩니다.</p>
                </div>
                <div className="inline-fields">
                  <label>
                    계좌번호
                    <input
                      value={autoDebitAccount}
                      onChange={(event) => setAutoDebitAccount(event.target.value)}
                    />
                  </label>
                  <label>
                    출금일
                    <input
                      min="1"
                      max="31"
                      type="number"
                      value={withdrawalDay}
                      onChange={(event) => setWithdrawalDay(event.target.value)}
                    />
                  </label>
                </div>
                <button
                  className="secondary-button"
                  disabled={isAutoDebitSubmitting}
                  type="submit"
                >
                  {isAutoDebitSubmitting ? '등록 중' : '자동이체 등록'}
                </button>
              </form>
            </div>
            </>
          ) : (
            <section className="payment-box">
              <div>
                <span className="badge">계약 선택</span>
                <h3>계약을 선택하세요</h3>
                <p>계약 목록에서 상세보기를 누르면 계약 상세, 미납/연체, 납부 기능이 표시됩니다.</p>
              </div>
            </section>
          )}
        </div>
      </section>
      ) : null}
    </section>
  )
}

type CustomerClaimView = 'health' | 'car' | 'status' | 'history' | 'analysis'

function CustomerClaimsPage({
  token,
  onUnauthorized,
  view,
}: {
  token: string
  onUnauthorized: () => void
  view: CustomerClaimView
}) {
  const [contracts, setContracts] = useState<ContractSummary[]>([])
  const [selectedHealthContractId, setSelectedHealthContractId] = useState('')
  const [hospitalName, setHospitalName] = useState('서울중앙병원')
  const [diagnosisCode, setDiagnosisCode] = useState('J00')
  const [treatmentDate, setTreatmentDate] = useState('2026-06-01')
  const [requestAmount, setRequestAmount] = useState('184000')
  const [receiptAmount, setReceiptAmount] = useState('184000')
  const [healthAttachments, setHealthAttachments] = useState<File[]>([])
  const [claimResult, setClaimResult] = useState<HealthClaimResponse | null>(null)
  const [claimError, setClaimError] = useState('')
  const [isClaimLoading, setClaimLoading] = useState(true)
  const [isClaimSubmitting, setClaimSubmitting] = useState(false)
  const [selectedCarContractId, setSelectedCarContractId] = useState('')
  const [accidentDate, setAccidentDate] = useState('2026-06-01')
  const [accidentLocation, setAccidentLocation] = useState('서울 강남구 역삼동')
  const [accidentType, setAccidentType] = useState('쌍방')
  const [vehicleNumber, setVehicleNumber] = useState('12가3456')
  const [hasInjury, setHasInjury] = useState(false)
  const [injuredCount, setInjuredCount] = useState('0')
  const [accidentAttachments, setAccidentAttachments] = useState<File[]>([])
  const [accidentResult, setAccidentResult] = useState<CarAccidentReportResponse | null>(null)
  const [accidentError, setAccidentError] = useState('')
  const [isAccidentSubmitting, setAccidentSubmitting] = useState(false)
  const today = useMemo(() => new Date(), [])
  const oneYearAgo = useMemo(() => {
    const date = new Date(today)
    date.setFullYear(date.getFullYear() - 1)
    return date
  }, [today])
  const [statusClaims, setStatusClaims] = useState<ClaimListItem[]>([])
  const [historyClaims, setHistoryClaims] = useState<ClaimListItem[]>([])
  const [historyFrom, setHistoryFrom] = useState(formatInputDate(oneYearAgo))
  const [historyTo, setHistoryTo] = useState(formatInputDate(today))
  const [hasHistorySearched, setHasHistorySearched] = useState(false)
  const [selectedAnalysisContractId, setSelectedAnalysisContractId] = useState('')
  const [benefitAnalysis, setBenefitAnalysis] = useState<BenefitAnalysis | null>(null)
  const [queryError, setQueryError] = useState('')
  const [isQueryLoading, setQueryLoading] = useState(true)
  const [isHistoryLoading, setHistoryLoading] = useState(false)
  const [isAnalysisLoading, setAnalysisLoading] = useState(false)
  const healthContracts = contracts.filter(
    (contract) => contract.productType === 'HEALTH' && contract.status === 'ACTIVE',
  )
  const carContracts = contracts.filter(
    (contract) => contract.productType === 'CAR' && contract.status === 'ACTIVE',
  )

  useEffect(() => {
    let isMounted = true

    async function loadContracts() {
      setClaimError('')
      setAccidentError('')
      setClaimLoading(true)

      try {
        const list = await apiClient.getContracts(token)
        const activeHealthContracts = list.filter(
          (contract) => contract.productType === 'HEALTH' && contract.status === 'ACTIVE',
        )
        const activeCarContracts = list.filter(
          (contract) => contract.productType === 'CAR' && contract.status === 'ACTIVE',
        )

        if (isMounted) {
          setContracts(list)
          setSelectedHealthContractId((current) => current || String(activeHealthContracts[0]?.contractId ?? ''))
          setSelectedCarContractId((current) => current || String(activeCarContracts[0]?.contractId ?? ''))
          setSelectedAnalysisContractId((current) => current || String(list[0]?.contractId ?? ''))
        }
      } catch (err) {
        if (err instanceof ApiError && err.status === 401) {
          onUnauthorized()
          return
        }

        if (isMounted) {
          setClaimError(err instanceof Error ? err.message : '계약 목록 조회에 실패했습니다.')
          setAccidentError(err instanceof Error ? err.message : '계약 목록 조회에 실패했습니다.')
        }
      } finally {
        if (isMounted) {
          setClaimLoading(false)
        }
      }
    }

    loadContracts()

    return () => {
      isMounted = false
    }
  }, [onUnauthorized, token])

  async function loadStatusClaims() {
    setQueryError('')
    setQueryLoading(true)

    try {
      setStatusClaims(await apiClient.getClaimStatus(token))
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }

      setQueryError(err instanceof Error ? err.message : '보상 조회에 실패했습니다.')
    } finally {
      setQueryLoading(false)
    }
  }

  // 같은 CustomerClaimsPage 인스턴스가 view prop만 바뀌며 재사용되므로(리마운트 X),
  // 보상현황 탭으로 진입할 때마다 최신 데이터를 다시 불러온다.
  useEffect(() => {
    if (view !== 'status') {
      return
    }
    void Promise.resolve().then(() => {
      loadStatusClaims()
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [view])

  function changeHealthAttachments(files: FileList | null) {
    const nextFiles = Array.from(files ?? [])
    const invalidType = nextFiles.find((file) => !isAcceptedClaimFile(file))
    const oversized = nextFiles.find((file) => file.size > maxClaimAttachmentSize)

    setClaimResult(null)

    if (invalidType) {
      setHealthAttachments([])
      setClaimError('지원하지 않는 파일 형식입니다. (허용: PDF, JPG, PNG)')
      return
    }

    if (oversized) {
      setHealthAttachments([])
      setClaimError('파일 크기는 개당 10MB 이하여야 합니다.')
      return
    }

    setClaimError('')
    setHealthAttachments(nextFiles)
  }

  function changeAccidentAttachments(files: FileList | null) {
    const nextFiles = Array.from(files ?? [])
    const invalidType = nextFiles.find((file) => !isAcceptedClaimFile(file))
    const oversized = nextFiles.find((file) => file.size > maxClaimAttachmentSize)

    setAccidentResult(null)

    if (invalidType) {
      setAccidentAttachments([])
      setAccidentError('지원하지 않는 파일 형식입니다. (허용: PDF, JPG, PNG)')
      return
    }

    if (oversized) {
      setAccidentAttachments([])
      setAccidentError('파일 크기는 개당 10MB 이하여야 합니다.')
      return
    }

    setAccidentError('')
    setAccidentAttachments(nextFiles)
  }

  async function submitClaim(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setClaimError('')
    setClaimResult(null)

    const contractId = Number(selectedHealthContractId)
    const parsedRequestAmount = Number(requestAmount)
    const parsedReceiptAmount = Number(receiptAmount)

    if (!contractId) {
      setClaimError('청구할 의료보험 계약을 선택해 주세요.')
      return
    }

    if (parsedRequestAmount <= 0 || parsedReceiptAmount <= 0) {
      setClaimError('청구 금액과 영수증 금액은 0보다 커야 합니다.')
      return
    }

    setClaimSubmitting(true)

    try {
      const response = await apiClient.submitHealthClaim(token, {
        contractId,
        hospitalName,
        diagnosisCode,
        treatmentDate,
        requestAmount: parsedRequestAmount,
        receiptAmount: parsedReceiptAmount,
        attachments: healthAttachments,
      })
      setClaimResult(response)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }

      setClaimError(err instanceof Error ? err.message : '의료보험 청구에 실패했습니다.')
    } finally {
      setClaimSubmitting(false)
    }
  }

  async function submitAccident(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setAccidentError('')
    setAccidentResult(null)

    const contractId = Number(selectedCarContractId)
    const parsedInjuredCount = Number(injuredCount)
    const today = formatInputDate(new Date())

    if (!contractId) {
      setAccidentError('접수할 자동차보험 계약을 선택해 주세요.')
      return
    }

    if (accidentDate > today) {
      setAccidentError('사고 일자는 미래일 수 없습니다.')
      return
    }

    if (parsedInjuredCount < 0) {
      setAccidentError('부상자 수는 0 이상이어야 합니다.')
      return
    }

    if (hasInjury && parsedInjuredCount < 1) {
      setAccidentError('대인사고는 부상자 수를 1명 이상 입력해야 합니다.')
      return
    }

    if (!hasInjury && parsedInjuredCount !== 0) {
      setAccidentError('대인사고가 아니면 부상자 수는 0이어야 합니다.')
      return
    }

    setAccidentSubmitting(true)

    try {
      const response = await apiClient.submitCarAccidentReport(token, {
        contractId,
        accidentDate,
        accidentLocation,
        accidentType,
        vehicleNumber,
        hasInjury,
        injuredCount: parsedInjuredCount,
        attachments: accidentAttachments,
      })
      setAccidentResult(response)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }

      setAccidentError(err instanceof Error ? err.message : '자동차사고 접수에 실패했습니다.')
    } finally {
      setAccidentSubmitting(false)
    }
  }

  async function loadHistory(event?: FormEvent<HTMLFormElement>) {
    event?.preventDefault()
    setQueryError('')
    setHistoryLoading(true)

    try {
      setHistoryClaims(await apiClient.getClaimHistory(token, {
        from: historyFrom,
        to: historyTo,
      }))
      setHasHistorySearched(true)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }

      setQueryError(err instanceof Error ? err.message : '보상 이력 조회에 실패했습니다.')
    } finally {
      setHistoryLoading(false)
    }
  }

  async function loadBenefitAnalysis(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setQueryError('')
    setBenefitAnalysis(null)

    const contractId = Number(selectedAnalysisContractId)
    if (!contractId) {
      setQueryError('분석할 계약을 선택해 주세요.')
      return
    }

    setAnalysisLoading(true)

    try {
      setBenefitAnalysis(await apiClient.getBenefitAnalysis(token, contractId))
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }

      setQueryError(err instanceof Error ? err.message : '실익 분석 조회에 실패했습니다.')
    } finally {
      setAnalysisLoading(false)
    }
  }

  const pageMeta: Record<CustomerClaimView, { eyebrow: string; title: string }> = {
    health: { eyebrow: 'UC05 / UC17', title: '의료보험 청구' },
    car: { eyebrow: 'UC09', title: '자동차사고 접수' },
    status: { eyebrow: 'UC03', title: '보상 처리 현황' },
    history: { eyebrow: 'UC04', title: '보상 이력' },
    analysis: { eyebrow: 'UC11', title: '실익 분석' },
  }

  return (
    <section className="page">
      <div className="page-header">
        <div>
          <span className="eyebrow">{pageMeta[view].eyebrow}</span>
          <h1>{pageMeta[view].title}</h1>
        </div>
      </div>

      {view === 'health' ? (
        <>
        <form className="panel form-panel" onSubmit={submitClaim}>
          <div className="section-title">
            <HeartPulse size={18} />
            <h2>청구 신청</h2>
          </div>
          <label>
            계약
            <select
              disabled={isClaimLoading || healthContracts.length === 0}
              value={selectedHealthContractId}
              onChange={(event) => setSelectedHealthContractId(event.target.value)}
            >
              {healthContracts.map((contract) => (
                <option key={contract.contractId} value={contract.contractId}>
                  {contract.productName} · {formatDate(contract.startDate)} · 월{' '}
                  {formatCurrency(contract.monthlyPremium)}
                </option>
              ))}
              {healthContracts.length === 0 ? <option value="">선택 가능한 계약 없음</option> : null}
            </select>
          </label>
          <label>
            병원명
            <input
              required
              value={hospitalName}
              onChange={(event) => setHospitalName(event.target.value)}
            />
          </label>
          <label>
            진단코드
            <input
              required
              value={diagnosisCode}
              onChange={(event) => setDiagnosisCode(event.target.value)}
            />
          </label>
          <label>
            진료일
            <input
              required
              type="date"
              value={treatmentDate}
              onChange={(event) => setTreatmentDate(event.target.value)}
            />
          </label>
          <div className="inline-fields">
            <label>
              청구 금액
              <input
                min="1"
                required
                type="number"
                value={requestAmount}
                onChange={(event) => setRequestAmount(event.target.value)}
              />
            </label>
            <label>
              영수증 금액
              <input
                min="1"
                required
                type="number"
                value={receiptAmount}
                onChange={(event) => setReceiptAmount(event.target.value)}
              />
            </label>
          </div>
          <label>
            증빙 첨부
            <input
              accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
              multiple
              type="file"
              onChange={(event) => changeHealthAttachments(event.target.files)}
            />
          </label>
          {healthAttachments.length > 0 ? (
            <div className="attachment-list">
              {healthAttachments.map((file) => (
                <span key={`${file.name}-${file.size}`}>
                  {file.name} · {(file.size / 1024 / 1024).toFixed(2)}MB
                </span>
              ))}
            </div>
          ) : null}
          {claimError ? <p className="form-error">{claimError}</p> : null}
          <button
            className="primary-button"
            disabled={isClaimSubmitting || healthContracts.length === 0}
            type="submit"
          >
            {isClaimSubmitting ? '접수 중' : '청구 신청'}
            <Send size={18} />
          </button>
        </form>
        <section className="panel result-panel">
        <div className="section-title">
          <Receipt size={18} />
          <h2>청구 결과</h2>
        </div>
        {claimResult ? (
          <article className={`claim-result ${claimResult.status.toLowerCase()}`}>
            <span className="badge">{formatContractStatus(claimResult.complexity)}</span>
            <div>
              <h3>청구번호 {claimResult.claimId}</h3>
              <p>{describeHealthClaimResult(claimResult)}</p>
            </div>
            <strong>{formatContractStatus(claimResult.status)}</strong>
          </article>
        ) : (
          <div className="empty-result">
            <FileText size={28} />
            <p>청구 신청 후 결과가 표시됩니다.</p>
          </div>
        )}
        <div className="claim-rule-grid">
          <article>
            <strong>간편 청구</strong>
            <span>1,000,000원 미만 · 즉시지급</span>
          </article>
          <article>
            <strong>심사 청구</strong>
            <span>1,000,000원 이상 · 심사대기</span>
          </article>
        </div>
      </section>
      </>
      ) : null}

      {view === 'car' ? (
        <form className="panel form-panel" onSubmit={submitAccident}>
          <div className="section-title">
            <Car size={18} />
            <h2>자동차사고 접수</h2>
          </div>
          <label>
            계약
            <select
              disabled={isClaimLoading || carContracts.length === 0}
              value={selectedCarContractId}
              onChange={(event) => setSelectedCarContractId(event.target.value)}
            >
              {carContracts.map((contract) => (
                <option key={contract.contractId} value={contract.contractId}>
                  {contract.productName} · {formatDate(contract.startDate)}
                </option>
              ))}
              {carContracts.length === 0 ? <option value="">선택 가능한 계약 없음</option> : null}
            </select>
          </label>
          <label>
            사고 일자
            <input
              required
              type="date"
              value={accidentDate}
              onChange={(event) => setAccidentDate(event.target.value)}
            />
          </label>
          <label>
            사고 장소
            <input
              required
              value={accidentLocation}
              onChange={(event) => setAccidentLocation(event.target.value)}
            />
          </label>
          <label>
            사고 유형
            <input
              required
              value={accidentType}
              onChange={(event) => setAccidentType(event.target.value)}
            />
          </label>
          <label>
            차량 번호
            <input
              required
              value={vehicleNumber}
              onChange={(event) => setVehicleNumber(event.target.value)}
            />
          </label>
          <label className="toggle-row">
            <input
              checked={hasInjury}
              type="checkbox"
              onChange={(event) => {
                setHasInjury(event.target.checked)
                setInjuredCount(event.target.checked ? '1' : '0')
              }}
            />
            대인사고
          </label>
          {hasInjury ? (
            <label>
              부상자 수
              <input
                min="1"
                required
                type="number"
                value={injuredCount}
                onChange={(event) => setInjuredCount(event.target.value)}
              />
            </label>
          ) : null}
          <label>
            현장 사진/증빙
            <input
              accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
              multiple
              type="file"
              onChange={(event) => changeAccidentAttachments(event.target.files)}
            />
          </label>
          {accidentAttachments.length > 0 ? (
            <div className="attachment-list">
              {accidentAttachments.map((file) => (
                <span key={`${file.name}-${file.size}`}>
                  {file.name} · {(file.size / 1024 / 1024).toFixed(2)}MB
                </span>
              ))}
            </div>
          ) : null}
          {accidentResult ? (
            <p className="form-success">
              자동차사고 접수번호 {accidentResult.reportId}번이 {formatContractStatus(accidentResult.status)} 상태로 접수되었습니다.
            </p>
          ) : null}
          {accidentError ? <p className="form-error">{accidentError}</p> : null}
          <button
            className="primary-button"
            disabled={isAccidentSubmitting || carContracts.length === 0}
            type="submit"
          >
            {isAccidentSubmitting ? '접수 중' : '접수하기'}
            <Send size={18} />
          </button>
        </form>
      ) : null}

      {view === 'status' ? (
      <section className="panel">
        <div className="section-title">
          <ClipboardCheck size={18} />
          <h2>보상 처리 현황</h2>
        </div>
        {isQueryLoading ? <p>보상 현황을 불러오는 중입니다.</p> : null}
        <div className="claim-table">
          {statusClaims.length > 0 ? (
            <article className="claim-table__head">
              <span>청구</span>
              <span>접수일</span>
              <span>청구금액</span>
              <span>지급금액</span>
              <span>상태</span>
            </article>
          ) : null}
          {statusClaims.map((claim) => (
            <article key={`${claim.claimType}-${claim.claimId}`}>
              <strong>{formatClaimType(claim.claimType)}-{claim.claimId}</strong>
              <span>{formatDate(claim.claimDate)}</span>
              <span>{formatCurrency(claim.requestAmount)}</span>
              <span>{formatCurrency(claim.paidAmount)}</span>
              <span className={`status-badge ${claim.status.toLowerCase()}`}>
                {formatContractStatus(claim.status)}
              </span>
            </article>
          ))}
          {statusClaims.length === 0 && !isQueryLoading ? <p>진행 중인 보상 건이 없습니다.</p> : null}
        </div>
      </section>
      ) : null}

      {view === 'history' ? (
      <section className="panel">
        <div className="section-title">
          <CalendarDays size={18} />
          <h2>보상 이력</h2>
        </div>
        <p className="section-note">기간을 정한 뒤 조회 버튼을 눌러 보상이력을 불러오세요.</p>
        <form className="inline-fields" onSubmit={loadHistory}>
          <label>
            시작일
            <input
              type="date"
              value={historyFrom}
              onChange={(event) => setHistoryFrom(event.target.value)}
            />
          </label>
          <label>
            종료일
            <input
              type="date"
              value={historyTo}
              onChange={(event) => setHistoryTo(event.target.value)}
            />
          </label>
          <button className="secondary-button" disabled={isHistoryLoading} type="submit">
            {isHistoryLoading ? '조회 중' : '조회'}
          </button>
        </form>
        <div className="claim-table">
          {historyClaims.length > 0 ? (
            <article className="claim-table__head">
              <span>청구</span>
              <span>접수일</span>
              <span>청구금액</span>
              <span>지급금액</span>
              <span>상태</span>
            </article>
          ) : null}
          {historyClaims.map((claim) => (
            <article key={`${claim.claimType}-${claim.claimId}`}>
              <strong>{formatClaimType(claim.claimType)}-{claim.claimId}</strong>
              <span>{formatDate(claim.claimDate)}</span>
              <span>{formatCurrency(claim.requestAmount)}</span>
              <span>{formatCurrency(claim.paidAmount)}</span>
              <span className={`status-badge ${claim.status.toLowerCase()}`}>
                {formatContractStatus(claim.status)}
              </span>
            </article>
          ))}
          {historyClaims.length === 0 && !isQueryLoading && !isHistoryLoading && hasHistorySearched ? (
            <p>조회 기간의 보상 이력이 없습니다.</p>
          ) : null}
          {!hasHistorySearched && !isHistoryLoading ? <p>조회 버튼을 누르면 보상이력이 표시됩니다.</p> : null}
        </div>
      </section>
      ) : null}

      {view === 'analysis' ? (
      <section className="panel">
        <div className="section-title">
          <BadgeCheck size={18} />
          <h2>실익 분석</h2>
        </div>
        <p className="section-note">계약 유지 6개월 이후에만 조회할 수 있습니다.</p>
        <form className="analysis-form" onSubmit={loadBenefitAnalysis}>
          <label>
            계약
            <select
              value={selectedAnalysisContractId}
              onChange={(event) => setSelectedAnalysisContractId(event.target.value)}
            >
              {contracts.map((contract) => (
                <option key={contract.contractId} value={contract.contractId}>
                  {contract.productName} · {formatProductType(contract.productType)}
                </option>
              ))}
              {contracts.length === 0 ? <option value="">선택 가능한 계약 없음</option> : null}
            </select>
          </label>
          <button className="secondary-button" disabled={isAnalysisLoading || contracts.length === 0} type="submit">
            {isAnalysisLoading ? '분석 중' : '분석'}
          </button>
        </form>
        {benefitAnalysis ? (
          <div className="analysis-grid">
            <article>
              <span>총납입</span>
              <strong>{formatCurrency(benefitAnalysis.totalPaidPremium)}</strong>
            </article>
            <article>
              <span>총수령</span>
              <strong>{formatCurrency(benefitAnalysis.totalReceivedBenefit)}</strong>
            </article>
            <article>
              <span>실익</span>
              <strong>{formatCurrency(benefitAnalysis.profit)}</strong>
            </article>
            <article>
              <span>실익률</span>
              <strong>{benefitAnalysis.profitRate.toFixed(2)}</strong>
            </article>
          </div>
        ) : null}
        {queryError ? <p className="form-error">{queryError}</p> : null}
      </section>
      ) : null}
    </section>
  )
}

function ProfilePage({
  token,
  onUnauthorized,
}: {
  token: string
  onUnauthorized: () => void
}) {
  const [profile, setProfile] = useState<PolicyholderProfile | null>(null)
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(true)
  const [isSaving, setSaving] = useState(false)

  useEffect(() => {
    let isMounted = true

    async function loadProfile() {
      setError('')
      setLoading(true)

      try {
        const nextProfile = await apiClient.getMe(token)
        if (isMounted) {
          setProfile(nextProfile)
        }
      } catch (err) {
        if (err instanceof ApiError && err.status === 401) {
          onUnauthorized()
          return
        }

        if (isMounted) {
          setError(err instanceof Error ? err.message : '내 정보 조회에 실패했습니다.')
        }
      } finally {
        if (isMounted) {
          setLoading(false)
        }
      }
    }

    loadProfile()

    return () => {
      isMounted = false
    }
  }, [onUnauthorized, token])

  if (isLoading) {
    return <section className="page">내 정보를 불러오는 중입니다.</section>
  }

  if (!profile) {
    return (
      <section className="page">
        <div className="page-header">
          <div>
            <span className="eyebrow">내 정보 조회</span>
            <h1>마이페이지</h1>
          </div>
        </div>
        <p className="form-error">{error || '내 정보를 표시할 수 없습니다.'}</p>
      </section>
    )
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setStatus('')
    setError('')
    setSaving(true)

    const form = new FormData(event.currentTarget)

    try {
      const nextProfile = await apiClient.updateProfile(token, {
        email: String(form.get('email')),
        phone: String(form.get('phone')),
        address: String(form.get('address')),
        bankAccount: String(form.get('bankAccount')),
      })

      setProfile(nextProfile)
      setStatus('개인정보가 성공적으로 변경되었습니다.')
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }

      setError(err instanceof Error ? err.message : '개인정보 수정에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="page">
      <div className="page-header">
        <div>
          <span className="eyebrow">UC06 내 정보 수정</span>
          <h1>마이페이지</h1>
        </div>
      </div>

      <section className="status-panel">
        <div>
          <BadgeCheck size={22} />
          <h2>{profile.name}님의 내 정보</h2>
          <p>{profile.email} · {profile.phone}</p>
        </div>
      </section>

      <form className="profile-form" key={profile.email} onSubmit={handleSubmit}>
        <label>
          이름
          <input name="name" value={profile.name} disabled />
        </label>
        <label>
          이메일
          <input name="email" defaultValue={profile.email} type="email" />
        </label>
        <label>
          전화번호
          <input name="phone" defaultValue={profile.phone} />
        </label>
        <label>
          주소
          <input name="address" defaultValue={profile.address} />
        </label>
        <label>
          계좌번호
          <input name="bankAccount" defaultValue={profile.bankAccount} />
        </label>
        {error ? <p className="form-error">{error}</p> : null}
        {status ? <p className="form-success">{status}</p> : null}
        <button className="primary-button" disabled={isSaving} type="submit">
          {isSaving ? '저장 중' : '변경사항 저장'}
          <BadgeCheck size={18} />
        </button>
      </form>
    </section>
  )
}

function EmployeeReviewsPage({
  token,
  onUnauthorized,
}: {
  token: string
  onUnauthorized: () => void
}) {
  const [pendingReviews, setPendingReviews] = useState<PendingReview[]>([])
  const [selectedReview, setSelectedReview] = useState<ReviewApplicationDetail | null>(null)
  const [reviewResult, setReviewResult] = useState<ReviewResult>('APPROVED')
  const [comment, setComment] = useState('이상 없음')
  const [surchargeRate, setSurchargeRate] = useState('0.2')
  const [decision, setDecision] = useState<ConfirmReviewResponse | null>(null)
  const [error, setError] = useState('')
  const metrics = useMemo(
    () => [
      { label: '대기 신청', value: String(pendingReviews.length), icon: ClipboardCheck },
      {
        label: '자동차 심사',
        value: String(pendingReviews.filter((review) => review.productName.includes('드라이브') || review.productName.includes('자동차')).length),
        icon: Bell,
      },
      { label: '선택 건', value: selectedReview ? '1' : '0', icon: Users },
    ],
    [pendingReviews, selectedReview],
  )

  async function loadPendingReviews() {
    try {
      const list = await apiClient.getPendingReviews(token)
      setPendingReviews(list)
      if (list[0]) {
        setSelectedReview(await apiClient.getReviewApplication(token, list[0].applicationId))
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '심사 대기 목록 조회에 실패했습니다.')
    }
  }

  useEffect(() => {
    void Promise.resolve().then(() => {
      loadPendingReviews()
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function selectReview(id: number) {
    setError('')
    setDecision(null)
    try {
      setSelectedReview(await apiClient.getReviewApplication(token, id))
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '심사 상세 조회에 실패했습니다.')
    }
  }

  async function confirmReview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedReview) {
      return
    }

    try {
      const response = await apiClient.confirmReview(token, selectedReview.applicationId, {
        result: reviewResult,
        comment,
        ...(reviewResult === 'CONDITIONAL' ? { surchargeRate: Number(surchargeRate) } : {}),
      })
      setDecision(response)
      await loadPendingReviews()
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '심사 확정에 실패했습니다.')
    }
  }

  return (
    <section className="page">
      <div className="page-header">
        <div>
          <span className="eyebrow">UC12 / UC13 / UC15</span>
          <h1>심사 대기 목록</h1>
        </div>
      </div>
      <div className="metric-grid">
        {metrics.map(({ label, value, icon: Icon }) => (
          <article className="metric-card" key={label}>
            <Icon size={20} />
            <strong>{value}</strong>
            <span>{label}</span>
          </article>
        ))}
      </div>

      <div className="split-layout">
        <section className="panel">
          <div className="section-title">
            <ClipboardCheck size={18} />
            <h2>대기 건</h2>
          </div>
          <div className="review-list">
            {pendingReviews.map((review) => (
              <button
                aria-pressed={review.applicationId === selectedReview?.applicationId}
                className={review.applicationId === selectedReview?.applicationId ? 'is-selected' : ''}
                key={review.applicationId}
                type="button"
                onClick={() => selectReview(review.applicationId)}
              >
                <strong>신청-{review.applicationId}</strong>
                <span>{review.productName} · {review.applicantName}</span>
                <small>{new Date(review.appliedAt).toLocaleString()} · {review.basePremium.toLocaleString()}원</small>
              </button>
            ))}
            {pendingReviews.length === 0 ? <p>심사 대기 건이 없습니다.</p> : null}
          </div>
        </section>

        <form className="panel" onSubmit={confirmReview}>
          <div className="section-title">
            <FileText size={18} />
            <h2>심사 상세</h2>
          </div>
          {selectedReview ? (
            <article className="detail-card">
              <span className="badge">신청 {selectedReview.applicationId}</span>
              <h3>{selectedReview.applicantName}</h3>
              <p>{selectedReview.productName} · 기본 보험료 {selectedReview.basePremium.toLocaleString()}원</p>
              <p>생년월일 {selectedReview.birthDate} · 주민등록번호 {selectedReview.ssn}</p>
              {selectedReview.medicalHistory ? (
                <div className="risk-box">
                  <HeartPulse size={18} />
                  <span>
                    현재 병력 {selectedReview.medicalHistory.currentConditions} · 입원 이력 {selectedReview.medicalHistory.pastHospitalization}
                  </span>
                </div>
              ) : null}
              {selectedReview.vehicleInfo ? (
                <div className="risk-box">
                  <Car size={18} />
                  <span>
                    {selectedReview.vehicleInfo.plateNumber} · {selectedReview.vehicleInfo.vehicleType} · {selectedReview.vehicleInfo.modelYear}년식
                  </span>
                </div>
              ) : null}
              {selectedReview.accidentHistory ? (
                <div className="risk-box">
                  <AlertTriangle size={18} />
                  <span>
                    사고 {selectedReview.accidentHistory.accidentCount}건 · 지급 {selectedReview.accidentHistory.totalPaidAmount.toLocaleString()}원 · 면허 {selectedReview.accidentHistory.licenseStatus}
                  </span>
                </div>
              ) : null}
              <label>
                심사 결과
                <select value={reviewResult} onChange={(event) => setReviewResult(event.target.value as ReviewResult)}>
                  <option value="APPROVED">승인</option>
                  <option value="CONDITIONAL">조건부 승인</option>
                  <option value="REJECTED">거절</option>
                </select>
              </label>
              {reviewResult === 'CONDITIONAL' ? (
                <label>
                  할증률
                  <input value={surchargeRate} onChange={(event) => setSurchargeRate(event.target.value)} />
                </label>
              ) : null}
              <label>
                심사 의견
                <input value={comment} onChange={(event) => setComment(event.target.value)} />
              </label>
              {decision ? (
                <p className="form-success">
                  심사 {decision.reviewId}번 {formatContractStatus(decision.result)} · 최종 월 보험료 {decision.adjustedPremium.toLocaleString()}원
                </p>
              ) : null}
              {error ? <p className="form-error">{error}</p> : null}
              <button className="primary-button" type="submit">
                심사 확정
                <CheckCircle2 size={18} />
              </button>
            </article>
          ) : (
            <p>심사 건을 선택하세요.</p>
          )}
        </form>
      </div>
    </section>
  )
}

function EmployeeBenefitReviewsPage({
  token,
  onUnauthorized,
}: {
  token: string
  onUnauthorized: () => void
}) {
  const [reviews, setReviews] = useState<BenefitReviewSummary[]>([])
  const [unassigned, setUnassigned] = useState<UnassignedBenefitReview[]>([])
  const [assigningClaimId, setAssigningClaimId] = useState<number | null>(null)
  const [selectedReview, setSelectedReview] = useState<BenefitReviewDetail | null>(null)
  const [reviewResult, setReviewResult] = useState<BenefitReviewResult>('APPROVED')
  const [comment, setComment] = useState('정상 청구')
  const [payoutAmount, setPayoutAmount] = useState('')
  const [decision, setDecision] = useState<ConfirmBenefitReviewResponse | null>(null)
  const [statusMessage, setStatusMessage] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(true)
  const [isSubmitting, setSubmitting] = useState(false)
  const [isRetrying, setRetrying] = useState(false)
  const canRetry =
    selectedReview?.claimStatus === 'FAILED' || decision?.claimStatus === 'FAILED'
  const metrics = useMemo(
    () => [
      { label: '배정 심사', value: String(reviews.length), icon: ClipboardCheck },
      {
        label: '지급 실패',
        value: String(reviews.filter((review) => review.claimStatus === 'FAILED').length),
        icon: AlertTriangle,
      },
      { label: '선택 청구', value: selectedReview ? `#${selectedReview.claimId}` : '-', icon: Receipt },
    ],
    [reviews, selectedReview],
  )

  async function refreshBenefitReviews() {
    setReviews(await apiClient.getBenefitReviews(token))
    await loadUnassigned()
  }

  async function loadUnassigned() {
    try {
      setUnassigned(await apiClient.getUnassignedBenefitReviews(token))
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
      }
      // 미배정 목록은 보조 정보라 실패해도 메인 흐름은 막지 않는다.
    }
  }

  // 미배정 자동차사고를 본인(로그인 직원)에게 배정 → 내 심사 큐로 편입.
  async function assignToMe(claimId: number) {
    const employeeId = employeeIdFromToken(token)
    if (!employeeId) {
      setError('토큰에서 직원 정보를 확인할 수 없습니다. 다시 로그인해 주세요.')
      return
    }

    setError('')
    setStatusMessage('')
    setAssigningClaimId(claimId)

    try {
      await apiClient.assignClaim(token, claimId, { employeeId })
      setStatusMessage(`청구 ${claimId}번을 내 심사 큐로 배정했습니다.`)
      await refreshBenefitReviews()
      await selectBenefitReview(claimId)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      if (err instanceof ApiError && err.status === 409) {
        setError('이미 다른 담당자에게 배정된 건입니다.')
        await refreshBenefitReviews()
        return
      }
      setError(err instanceof Error ? err.message : '배정에 실패했습니다.')
    } finally {
      setAssigningClaimId(null)
    }
  }

  async function loadBenefitReviews() {
    setError('')
    setLoading(true)

    try {
      const list = await apiClient.getBenefitReviews(token)
      setReviews(list)
      await loadUnassigned()
      if (list[0]) {
        await selectBenefitReview(list[0].claimId)
      } else {
        setSelectedReview(null)
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '보험금 심사 목록 조회에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void Promise.resolve().then(() => {
      loadBenefitReviews()
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function selectBenefitReview(claimId: number) {
    setError('')
    setDecision(null)
    setStatusMessage('')

    try {
      const detail = await apiClient.getBenefitReview(token, claimId)
      setSelectedReview(detail)
      setPayoutAmount(String(detail.requestAmount))
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      if (err instanceof ApiError && err.status === 409) {
        setError('다른 담당자 처리 중입니다.')
        return
      }
      setError(err instanceof Error ? err.message : '보험금 심사 상세 조회에 실패했습니다.')
    }
  }

  async function confirmBenefitReview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedReview) {
      return
    }

    const parsedPayout = Number(payoutAmount)
    if (reviewResult === 'APPROVED' && (!payoutAmount || Number.isNaN(parsedPayout) || parsedPayout <= 0)) {
      setError('승인 시 지급금액을 0보다 큰 값으로 입력해 주세요.')
      return
    }

    setError('')
    setStatusMessage('')
    setSubmitting(true)

    try {
      const response = await apiClient.confirmBenefitReview(token, selectedReview.claimId, {
        result: reviewResult,
        comment,
        ...(reviewResult === 'APPROVED' ? { payoutAmount: parsedPayout } : {}),
      })
      setDecision(response)
      setStatusMessage(describeBenefitReviewResult(response))
      await refreshBenefitReviews()
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '보험금 심사 확정에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  async function retryPayout() {
    if (!selectedReview) {
      return
    }

    setError('')
    setStatusMessage('')
    setRetrying(true)

    try {
      const response = await apiClient.retryBenefitPayout(token, selectedReview.claimId)
      const claimStatus = response?.claimStatus ?? 'COMPLETED'
      setStatusMessage(
        claimStatus === 'COMPLETED'
          ? '지급 재시도 완료'
          : `지급 재시도 결과 ${formatContractStatus(claimStatus)}`,
      )
      await refreshBenefitReviews()
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '지급 재시도에 실패했습니다.')
    } finally {
      setRetrying(false)
    }
  }

  return (
    <section className="page">
      <div className="page-header">
        <div>
          <span className="eyebrow">UC12 / UC14 / UC17</span>
          <h1>보험금 지급 심사</h1>
        </div>
      </div>

      <div className="metric-grid">
        {metrics.map(({ label, value, icon: Icon }) => (
          <article className="metric-card" key={label}>
            <Icon size={20} />
            <strong>{value}</strong>
            <span>{label}</span>
          </article>
        ))}
      </div>

      <div className="split-layout">
        <section className="panel">
          <div className="section-title">
            <AlertTriangle size={18} />
            <h2>미배정 자동차사고</h2>
          </div>
          <p className="section-note">
            미배정 사고접수 건입니다. [나에게 배정]을 누르면 내 심사 큐로 들어옵니다.
          </p>
          <div className="review-list">
            {unassigned.map((item) => (
              <article key={item.claimId} className="review-row">
                <div>
                  <strong>{formatClaimType(item.claimType)}-{item.claimId}</strong>
                  <span>
                    {item.accidentType ? `${item.accidentType} · ` : ''}
                    {formatCurrency(item.requestAmount)}
                  </span>
                  <small>{formatContractStatus(item.claimStatus)}</small>
                </div>
                <button
                  className="secondary-button"
                  type="button"
                  disabled={assigningClaimId === item.claimId}
                  onClick={() => assignToMe(item.claimId)}
                >
                  {assigningClaimId === item.claimId ? '배정 중' : '나에게 배정'}
                </button>
              </article>
            ))}
            {unassigned.length === 0 ? <p>미배정 건이 없습니다.</p> : null}
          </div>
        </section>

        <section className="panel">
          <div className="section-title">
            <ClipboardCheck size={18} />
            <h2>배정된 심사</h2>
          </div>
          {isLoading ? <p>보험금 심사 목록을 불러오는 중입니다.</p> : null}
          <div className="review-list">
            {reviews.map((review) => (
              <button
                aria-pressed={review.claimId === selectedReview?.claimId}
                className={review.claimId === selectedReview?.claimId ? 'is-selected' : ''}
                key={review.claimId}
                type="button"
                onClick={() => selectBenefitReview(review.claimId)}
              >
                <strong>청구-{review.claimId}</strong>
                <span>{review.hospitalName} · {formatCurrency(review.requestAmount)}</span>
                <small>{formatContractStatus(review.claimStatus)}</small>
              </button>
            ))}
            {reviews.length === 0 && !isLoading ? <p>배정된 심사 건이 없습니다.</p> : null}
          </div>
        </section>

        <form className="panel" onSubmit={confirmBenefitReview}>
          <div className="section-title">
            <FileText size={18} />
            <h2>심사 상세</h2>
          </div>
          {selectedReview ? (
            <article className="detail-card">
              <span className="badge">{formatContractStatus(selectedReview.claimStatus)}</span>
              <h3>청구번호 {selectedReview.claimId}</h3>
              <p>
                {selectedReview.hospitalName} · 진단코드 {selectedReview.diagnosisCode}
              </p>
              <p>
                청구금액 {formatCurrency(selectedReview.requestAmount)} · 담당자{' '}
                {selectedReview.assignedStaffId}
              </p>
              <label>
                심사 결과
                <select
                  value={reviewResult}
                  onChange={(event) => setReviewResult(event.target.value as BenefitReviewResult)}
                >
                  <option value="APPROVED">승인</option>
                  <option value="REJECTED">거절</option>
                </select>
              </label>
              {reviewResult === 'APPROVED' ? (
                <label>
                  지급금액
                  <input
                    type="number"
                    min="1"
                    value={payoutAmount}
                    onChange={(event) => setPayoutAmount(event.target.value)}
                  />
                </label>
              ) : null}
              <label>
                심사 의견
                <input value={comment} onChange={(event) => setComment(event.target.value)} />
              </label>
              <div className="button-row">
                <button className="primary-button" disabled={isSubmitting} type="submit">
                  {isSubmitting ? '확정 중' : '심사 확정'}
                  <CheckCircle2 size={18} />
                </button>
                <button
                  className="secondary-button"
                  disabled={isRetrying || !canRetry}
                  type="button"
                  onClick={retryPayout}
                >
                  {isRetrying ? '재시도 중' : '지급 재시도'}
                </button>
              </div>
            </article>
          ) : (
            <p>심사 건을 선택하세요.</p>
          )}
          {decision ? (
            <p className={decision.claimStatus === 'FAILED' ? 'form-error' : 'form-success'}>
              청구 {decision.claimId}번 {formatContractStatus(decision.result)} · {statusMessage}
            </p>
          ) : null}
          {statusMessage && !decision ? <p className="form-success">{statusMessage}</p> : null}
          {error ? <p className="form-error">{error}</p> : null}
        </form>
      </div>
    </section>
  )
}

function App() {
  const [session, setSession] = useState<AuthSession | null>(() => readSession())

  function handleLogout() {
    void apiClient.logout().catch(() => undefined)
    window.localStorage.removeItem(tokenKey)
    setSession(null)
  }

  function handleUnauthorized() {
    window.localStorage.removeItem(tokenKey)
    setSession(null)
  }

  return (
    <Routes>
      <Route
        path="/login"
        element={<LoginPage session={session} onLogin={setSession} />}
      />
      <Route
        path="/customer/*"
        element={
          session ? (
            <CustomerShell session={session} onLogout={handleLogout}>
              <Routes>
                <Route
                  path="/home"
                  element={
                    <CustomerHomePage
                      token={session.token}
                      onUnauthorized={handleUnauthorized}
                    />
                  }
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
                <Route
                  path="/apply"
                  element={<Navigate to="/customer/products" replace />}
                />
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
                    <CustomerClaimsPage
                      token={session.token}
                      onUnauthorized={handleUnauthorized}
                      view="health"
                    />
                  }
                />
                <Route
                  path="/claims/car-accident"
                  element={
                    <CustomerClaimsPage
                      token={session.token}
                      onUnauthorized={handleUnauthorized}
                      view="car"
                    />
                  }
                />
                <Route
                  path="/claims/status"
                  element={
                    <CustomerClaimsPage
                      token={session.token}
                      onUnauthorized={handleUnauthorized}
                      view="status"
                    />
                  }
                />
                <Route
                  path="/claims/history"
                  element={
                    <CustomerClaimsPage
                      token={session.token}
                      onUnauthorized={handleUnauthorized}
                      view="history"
                    />
                  }
                />
                <Route
                  path="/claims/benefit-analysis"
                  element={
                    <CustomerClaimsPage
                      token={session.token}
                      onUnauthorized={handleUnauthorized}
                      view="analysis"
                    />
                  }
                />
                <Route path="/claims" element={<Navigate to="/customer/claims/health" replace />} />
                <Route
                  path="/profile"
                  element={
                    <ProfilePage
                      token={session.token}
                      onUnauthorized={handleUnauthorized}
                    />
                  }
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
            <EmployeeShell session={session} onLogout={handleLogout}>
              <Routes>
                <Route
                  path="/reviews"
                  element={
                    <EmployeeReviewsPage
                      token={session.token}
                      onUnauthorized={handleUnauthorized}
                    />
                  }
                />
                <Route
                  path="/benefit-reviews"
                  element={
                    <EmployeeBenefitReviewsPage
                      token={session.token}
                      onUnauthorized={handleUnauthorized}
                    />
                  }
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
        element={
          session ? (
            <Navigate to={homePath(session)} replace />
          ) : (
            <Navigate to="/login" replace />
          )
        }
      />
    </Routes>
  )
}

export default App
