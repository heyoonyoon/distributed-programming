import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../../../lib/api/httpClient'
import type { PaymentMethod, ProductType } from '../../../lib/types'
import { formatCurrency } from '../../../utils/format'
import { contractsApi } from '../api'
import type {
  ContractDetail,
  ContractSummary,
  MyApplication,
  PayableContract,
  ProductDetail,
  ProductSummary,
  UnpaidContract,
} from '../types'

/** 상품 조회·가입 신청·계약/납부 화면이 공유하는 데이터와 액션. */
export function useCustomerContracts(token: string, onUnauthorized: () => void) {
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
      const list = await contractsApi.getProducts({
        type: productType,
        keyword,
        minPremium,
        maxPremium,
      })
      setProducts(list)

      if (list[0]) {
        setSelectedProduct(await contractsApi.getProduct(list[0].id))
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
      setApplications(await contractsApi.getMyApplications(token))
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
        contractsApi.getContract(token, id),
        contractsApi.getUnpaidContract(token, id).catch((err) => {
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
        contractsApi.getContracts(token),
        contractsApi.getUnpaidContracts(token),
        contractsApi.getPayableContracts(token),
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
    setSelectedProduct(await contractsApi.getProduct(id))
  }

  async function submitApplication(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedProduct) {
      return
    }

    const form = new FormData(event.currentTarget)
    const requestBody =
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
      const created = await contractsApi.createApplication(token, requestBody)
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
      await contractsApi.cancelApplication(token, id)
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
      const { blob, filename } = await contractsApi.downloadContract(token, id)
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
      const result = await contractsApi.payPremium(token, selectedContract.contractId, {
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
      await contractsApi.registerAutoDebit(token, selectedContract.contractId, {
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

  return {
    productType,
    keyword,
    setKeyword,
    minPremium,
    setMinPremium,
    maxPremium,
    setMaxPremium,
    products,
    selectedProduct,
    hasSearchedProducts,
    applications,
    contracts,
    selectedContract,
    selectedUnpaid,
    unpaidContracts,
    paymentMethod,
    setPaymentMethod,
    paymentInfo,
    setPaymentInfo,
    autoDebitAccount,
    setAutoDebitAccount,
    withdrawalDay,
    setWithdrawalDay,
    error,
    success,
    isLoading,
    isContractLoading,
    isPaymentSubmitting,
    isAutoDebitSubmitting,
    selectedPayable,
    loadProducts,
    loadContractDetail,
    changeProductType,
    selectProduct,
    submitApplication,
    cancelApplication,
    downloadContract,
    submitPayment,
    submitAutoDebit,
  }
}

export type CustomerContractsState = ReturnType<typeof useCustomerContracts>
