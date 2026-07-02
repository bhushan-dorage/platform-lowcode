import { useEffect, useRef, useCallback } from 'react'
import keycloak from '../keycloak'

type WsMessage = { type: string; payload: unknown }

export function useWebSocket(
  url: string,
  onMessage: (msg: WsMessage) => void,
) {
  const wsRef = useRef<WebSocket | null>(null)
  const onMessageRef = useRef(onMessage)
  onMessageRef.current = onMessage

  const connect = useCallback(() => {
    const token = keycloak.token ?? ''
    const ws = new WebSocket(`${url}?token=${encodeURIComponent(token)}`)
    wsRef.current = ws

    ws.onmessage = (event) => {
      try {
        const msg: WsMessage = JSON.parse(event.data)
        onMessageRef.current(msg)
      } catch {
        // ignore non-JSON frames
      }
    }

    ws.onclose = () => {
      // reconnect after 5 seconds
      setTimeout(connect, 5000)
    }
  }, [url])

  useEffect(() => {
    connect()
    return () => wsRef.current?.close()
  }, [connect])
}
