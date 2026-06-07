import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../../../lib/api/httpClient'
import { formatInputDate } from '../../../utils/format'
import { contractsApi } from '../../contracts/api'
import type { ContractSummary } from '../../contracts/types'
import { claimsApi } from '../api'
import type { BenefitAnalysis, CarAccidentReportResponse, ClaimListItem, HealthClaimResponse } from '../types'
import { isAcceptedClaimFile, maxClaimAttachmentSize } from '../utils'

/** 의료청구·자동차사고 접수·보상현황/이력/실익분석 화면이 공유하는 데이터와 액션. */
export function useCustomerClaims(token: string, onUnauthorized: () => void) {
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
        const list = await contractsApi.getContracts(token)
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
      setStatusClaims(await claimsApi.getClaimStatus(token))
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

  useEffect(() => {
    void Promise.resolve().then(() => {
      loadStatusClaims()
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

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
      const response = await claimsApi.submitHealthClaim(token, {
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
    const todayValue = formatInputDate(new Date())

    if (!contractId) {
      setAccidentError('접수할 자동차보험 계약을 선택해 주세요.')
      return
    }

    if (accidentDate > todayValue) {
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
      const response = await claimsApi.submitCarAccidentReport(token, {
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
      setHistoryClaims(
        await claimsApi.getClaimHistory(token, {
          from: historyFrom,
          to: historyTo,
        }),
      )
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
      setBenefitAnalysis(await claimsApi.getBenefitAnalysis(token, contractId))
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

  return {
    healthContracts,
    carContracts,
    contracts,
    selectedHealthContractId,
    setSelectedHealthContractId,
    hospitalName,
    setHospitalName,
    diagnosisCode,
    setDiagnosisCode,
    treatmentDate,
    setTreatmentDate,
    requestAmount,
    setRequestAmount,
    receiptAmount,
    setReceiptAmount,
    healthAttachments,
    claimResult,
    claimError,
    isClaimLoading,
    isClaimSubmitting,
    selectedCarContractId,
    setSelectedCarContractId,
    accidentDate,
    setAccidentDate,
    accidentLocation,
    setAccidentLocation,
    accidentType,
    setAccidentType,
    vehicleNumber,
    setVehicleNumber,
    hasInjury,
    setHasInjury,
    injuredCount,
    setInjuredCount,
    accidentAttachments,
    accidentResult,
    accidentError,
    isAccidentSubmitting,
    statusClaims,
    historyClaims,
    historyFrom,
    setHistoryFrom,
    historyTo,
    setHistoryTo,
    hasHistorySearched,
    selectedAnalysisContractId,
    setSelectedAnalysisContractId,
    benefitAnalysis,
    queryError,
    isQueryLoading,
    isHistoryLoading,
    isAnalysisLoading,
    changeHealthAttachments,
    changeAccidentAttachments,
    submitClaim,
    submitAccident,
    loadHistory,
    loadBenefitAnalysis,
  }
}

export type CustomerClaimsState = ReturnType<typeof useCustomerClaims>
