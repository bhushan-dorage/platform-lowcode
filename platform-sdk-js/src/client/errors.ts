export class PlatformSdkError extends Error {
  constructor(message: string, public readonly statusCode: number) {
    super(message)
    this.name = 'PlatformSdkError'
  }
}

export class EntityNotFoundError extends PlatformSdkError {
  constructor(message: string) { super(message, 404); this.name = 'EntityNotFoundError' }
}

export class AccessDeniedError extends PlatformSdkError {
  constructor(message: string) { super(message, 403); this.name = 'AccessDeniedError' }
}

export class ValidationError extends PlatformSdkError {
  constructor(message: string) { super(message, 400); this.name = 'ValidationError' }
}

export class RateLimitError extends PlatformSdkError {
  constructor(message: string, public readonly retryAfterSeconds: number) {
    super(message, 429); this.name = 'RateLimitError'
  }
}

export class TaskAlreadyClaimedError extends PlatformSdkError {
  constructor(message: string) { super(message, 409); this.name = 'TaskAlreadyClaimedError' }
}
