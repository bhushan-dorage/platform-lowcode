import {
  PlatformSdkError,
  EntityNotFoundError,
  AccessDeniedError,
  ValidationError,
  RateLimitError,
  TaskAlreadyClaimedError,
} from '../errors'

describe('SDK Errors', () => {
  it('PlatformSdkError has statusCode', () => {
    const err = new PlatformSdkError('test', 500)
    expect(err.statusCode).toBe(500)
    expect(err.message).toBe('test')
  })

  it('EntityNotFoundError has 404 status', () => {
    expect(new EntityNotFoundError('not found').statusCode).toBe(404)
  })

  it('AccessDeniedError has 403 status', () => {
    expect(new AccessDeniedError('forbidden').statusCode).toBe(403)
  })

  it('ValidationError has 400 status', () => {
    expect(new ValidationError('invalid').statusCode).toBe(400)
  })

  it('RateLimitError has 429 status and retryAfterSeconds', () => {
    const err = new RateLimitError('rate limited', 60)
    expect(err.statusCode).toBe(429)
    expect(err.retryAfterSeconds).toBe(60)
  })

  it('TaskAlreadyClaimedError has 409 status', () => {
    expect(new TaskAlreadyClaimedError('conflict').statusCode).toBe(409)
  })
})
