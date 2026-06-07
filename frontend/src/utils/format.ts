import type { ProductType } from '../lib/types'

export function formatCurrency(value: number) {
  return `${value.toLocaleString('ko-KR')}원`
}

export function formatDate(value: string) {
  return new Date(value).toLocaleDateString('ko-KR')
}

export function formatProductType(value: ProductType) {
  return value === 'HEALTH' ? '의료보험' : '자동차보험'
}

export function formatClaimType(value: string) {
  return value === 'HEALTH' ? '의료' : '자동차'
}

export function formatContractStatus(value: string) {
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

export function formatInputDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}
