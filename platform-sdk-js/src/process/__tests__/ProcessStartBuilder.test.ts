import { ProcessStartBuilder } from '../ProcessStartBuilder'
import { PlatformClient } from '../../client/PlatformClient'
import { ProcessTracker } from '../types'

const mockClient = {
  post: jest.fn(),
} as unknown as PlatformClient

describe('ProcessStartBuilder', () => {
  beforeEach(() => jest.clearAllMocks())

  it('submit sends processKey in body', async () => {
    const tracker: ProcessTracker = { trackingId: 't-1', statusUrl: '/status/t-1' }
    ;(mockClient.post as jest.Mock).mockResolvedValue(tracker)

    const result = await new ProcessStartBuilder(mockClient, 'order-process').submit()

    expect(result.trackingId).toBe('t-1')
    expect(mockClient.post).toHaveBeenCalledWith(
      '/v1/processes',
      expect.objectContaining({ processKey: 'order-process' }),
    )
  })

  it('builder methods return this for chaining', () => {
    const builder = new ProcessStartBuilder(mockClient, 'key')
    expect(builder.businessKey('bk')).toBe(builder)
    expect(builder.idempotencyKey('ik')).toBe(builder)
    expect(builder.variable('k', 'v')).toBe(builder)
    expect(builder.variables({ a: 1 })).toBe(builder)
    expect(builder.callbackUrl('http://cb')).toBe(builder)
  })

  it('businessKey is included in body when set', async () => {
    ;(mockClient.post as jest.Mock).mockResolvedValue({ trackingId: 't', statusUrl: '/s' })
    await new ProcessStartBuilder(mockClient, 'proc')
      .businessKey('biz-123')
      .submit()
    expect(mockClient.post).toHaveBeenCalledWith('/v1/processes',
      expect.objectContaining({ businessKey: 'biz-123' }))
  })
})
