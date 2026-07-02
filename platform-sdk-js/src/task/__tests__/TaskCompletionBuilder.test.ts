import { TaskCompletionBuilder } from '../TaskCompletionBuilder'
import { PlatformClient } from '../../client/PlatformClient'

const mockClient = {
  post: jest.fn(),
} as unknown as PlatformClient

describe('TaskCompletionBuilder', () => {
  beforeEach(() => jest.clearAllMocks())

  it('submit posts to correct endpoint', async () => {
    ;(mockClient.post as jest.Mock).mockResolvedValue(undefined)
    await new TaskCompletionBuilder(mockClient, 'task-42').outcome('APPROVED').submit()
    expect(mockClient.post).toHaveBeenCalledWith(
      '/v1/tasks/task-42/complete',
      expect.objectContaining({ outcome: 'APPROVED' }),
    )
  })

  it('builder methods return this', () => {
    const builder = new TaskCompletionBuilder(mockClient, 't1')
    expect(builder.outcome('x')).toBe(builder)
    expect(builder.variable('k', 'v')).toBe(builder)
    expect(builder.formData({ a: 1 })).toBe(builder)
  })
})
