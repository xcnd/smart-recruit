import request from './request'

/** GET /api/v1/referrals/public/programs — list all active programs + positions */
export function getPublicPrograms(): Promise<unknown> {
  return request.get('/referrals/public/programs')
}

/** GET /api/v1/referrals/public/programs/:token — get program by share token */
export function getProgramByToken(token: string): Promise<unknown> {
  return request.get(`/referrals/public/programs/${token}`)
}

/** GET /api/v1/referrals/public/positions/:programJobId/detail — job detail */
export function getJobDetail(programJobId: number): Promise<unknown> {
  return request.get(`/referrals/public/positions/${programJobId}/detail`)
}

/** POST /api/v1/referrals/public/upload-resume — upload resume file */
export function uploadResume(file: File): Promise<unknown> {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/referrals/public/upload-resume', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** POST /api/v1/referrals/public/submit — submit referral application */
export function submitReferral(data: Record<string, unknown>): Promise<unknown> {
  return request.post('/referrals/public/submit', data)
}

/** GET /api/v1/referrals/public/check-applied — check if user already applied */
export function checkApplied(programJobId: number): Promise<unknown> {
  return request.get('/referrals/public/check-applied', { params: { programJobId } })
}

/** GET /api/v1/referrals/my-applications — get logged-in user's applications */
export function getMyApplications(): Promise<unknown> {
  return request.get('/referrals/my-applications')
}
