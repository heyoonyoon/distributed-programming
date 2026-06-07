import { useEffect, useState } from 'react'
import { ApiError } from '../../../lib/api/httpClient'
import { contractsApi } from '../../contracts/api'

export type ContractSummaryTotals = {
  total: number
  health: number
  car: number
}

export function useContractSummary(token: string, onUnauthorized: () => void) {
  const [summary, setSummary] = useState<ContractSummaryTotals | null>(null)

  useEffect(() => {
    let isMounted = true

    async function loadSummary() {
      try {
        const contracts = await contractsApi.getContracts(token)
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

  return summary
}
