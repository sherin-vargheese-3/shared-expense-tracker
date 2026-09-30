import { apiRequest } from './client'

export interface HealthResponse {
  status: 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN'
}

export const fetchHealth = () => apiRequest<HealthResponse>('/actuator/health')
