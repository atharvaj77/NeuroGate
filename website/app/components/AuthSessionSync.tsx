'use client'

import { useAuth } from '../lib/clerk'
import { useEffect } from 'react'

export default function AuthSessionSync() {
  const { isSignedIn, getToken } = useAuth()

  useEffect(() => {
    if (!isSignedIn) {
      fetch('/api/auth/session-token', {
        method: 'DELETE',
        credentials: 'include',
      }).catch(() => {
        // Non-fatal in local/demo mode.
      })
      return
    }

    let cancelled = false

    ;(async () => {
      try {
        const token = await getToken()
        if (!token || cancelled) {
          return
        }

        await fetch('/api/auth/session-token', {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ token }),
        })
      } catch {
        // Best-effort token sync for backend cookie auth.
      }
    })()

    return () => {
      cancelled = true
    }
  }, [getToken, isSignedIn])

  return null
}
