import type { UserType } from '../types'

export const baseUrl = 'http://localhost:8080'

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

type TokenPayload = {
  userType?: UserType
  // 직원 토큰의 sub 클레임이 곧 employeeId (문자열 숫자, 예 "3").
  sub?: string
}

function decodeBase64Url(value: string) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(
    normalized.length + ((4 - (normalized.length % 4)) % 4),
    '=',
  )

  return window.atob(padded)
}

export function decodeUserType(token: string): UserType | null {
  try {
    const payload = JSON.parse(decodeBase64Url(token.split('.')[1])) as TokenPayload
    return payload.userType === 'POLICYHOLDER' || payload.userType === 'EMPLOYEE'
      ? payload.userType
      : null
  } catch {
    return null
  }
}

// 직원 JWT의 sub 클레임에서 employeeId를 추출한다. 없거나 파싱 불가면 null.
export function employeeIdFromToken(token: string): number | null {
  try {
    const payload = JSON.parse(decodeBase64Url(token.split('.')[1])) as TokenPayload
    const id = Number(payload.sub)
    return Number.isFinite(id) && id > 0 ? id : null
  } catch {
    return null
  }
}

async function readError(response: Response) {
  const contentType = response.headers.get('content-type') ?? ''

  if (contentType.includes('application/json')) {
    const body = await response.json()
    if (typeof body === 'string') {
      return body
    }
    if (body && typeof body.message === 'string') {
      return body.message
    }
    if (body && typeof body.error === 'string') {
      return body.error
    }
  }

  const text = await response.text()
  return text || `요청 처리에 실패했습니다. (${response.status})`
}

export async function request<T>(
  path: string,
  options: RequestInit = {},
  token?: string,
): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  })

  if (!response.ok) {
    throw new ApiError(response.status, await readError(response))
  }

  if (response.status === 204) {
    return undefined as T
  }

  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) {
    return response.json() as Promise<T>
  }

  return undefined as T
}

function readFilename(response: Response, fallback: string) {
  const disposition = response.headers.get('content-disposition') ?? ''
  const match = /filename="?([^"]+)"?/i.exec(disposition)
  return match?.[1] ?? fallback
}

export async function requestBlob(
  path: string,
  token: string,
): Promise<{ blob: Blob; filename: string }> {
  const response = await fetch(`${baseUrl}${path}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  if (!response.ok) {
    throw new ApiError(response.status, await readError(response))
  }

  return {
    blob: await response.blob(),
    filename: readFilename(response, 'contract.txt'),
  }
}

export async function requestForm<T>(
  path: string,
  body: FormData,
  token: string,
): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
    },
    body,
  })

  if (!response.ok) {
    throw new ApiError(response.status, await readError(response))
  }

  return response.json() as Promise<T>
}
