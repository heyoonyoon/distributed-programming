import type { HealthClaimResponse } from './types'

export const maxClaimAttachmentSize = 10 * 1024 * 1024

const acceptedClaimFileTypes = new Set([
  'application/pdf',
  'image/jpeg',
  'image/png',
])

export function isAcceptedClaimFile(file: File) {
  if (acceptedClaimFileTypes.has(file.type)) {
    return true
  }

  return /\.(pdf|jpe?g|png)$/i.test(file.name)
}

export function describeHealthClaimResult(result: HealthClaimResponse) {
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
