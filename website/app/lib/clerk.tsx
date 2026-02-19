'use client'

import React, { useCallback, useEffect, useMemo, useState } from 'react'

type SignInProps = {
  routing?: string
  path?: string
}

function hasSessionFlag(): boolean {
  if (typeof document === 'undefined') {
    return false
  }
  return document.cookie.split(';').some(cookie => cookie.trim().startsWith('ng_session=1'))
}

function setSessionFlag(value: boolean) {
  if (typeof document === 'undefined') {
    return
  }
  if (value) {
    document.cookie = 'ng_session=1; path=/; max-age=3600'
  } else {
    document.cookie = 'ng_session=; path=/; max-age=0'
  }
  window.dispatchEvent(new Event('ng-auth-changed'))
}

function useSessionFlag() {
  const [isSignedIn, setIsSignedIn] = useState<boolean>(false)

  useEffect(() => {
    const sync = () => setIsSignedIn(hasSessionFlag())
    sync()
    window.addEventListener('ng-auth-changed', sync)
    return () => window.removeEventListener('ng-auth-changed', sync)
  }, [])

  return isSignedIn
}

export function ClerkProvider({ children }: { children: React.ReactNode; publishableKey?: string }) {
  return <>{children}</>
}

export function SignedIn({ children }: { children: React.ReactNode }) {
  const isSignedIn = useSessionFlag()
  if (!isSignedIn) {
    return null
  }
  return <>{children}</>
}

export function SignedOut({ children }: { children: React.ReactNode }) {
  const isSignedIn = useSessionFlag()
  if (isSignedIn) {
    return null
  }
  return <>{children}</>
}

export function SignInButton({
  children,
  forceRedirectUrl,
}: {
  children: React.ReactNode
  mode?: string
  forceRedirectUrl?: string
}) {
  const href = forceRedirectUrl ?? '/sign-in'
  return <a href={href}>{children}</a>
}

export function UserButton({ afterSignOutUrl }: { afterSignOutUrl?: string }) {
  const handleSignOut = useCallback(async () => {
    await fetch('/api/auth/session-token', {
      method: 'DELETE',
      credentials: 'include',
    })
    setSessionFlag(false)
    window.location.href = afterSignOutUrl ?? '/'
  }, [afterSignOutUrl])

  return (
    <button
      onClick={handleSignOut}
      className="text-xs px-3 py-1.5 rounded-md border border-slate-700 hover:border-primary-500 hover:text-primary-300"
    >
      Sign Out
    </button>
  )
}

export function useAuth() {
  const isSignedIn = useSessionFlag()

  const getToken = useCallback(async () => {
    return null
  }, [])

  return useMemo(
    () => ({
      isSignedIn,
      getToken,
    }),
    [getToken, isSignedIn]
  )
}

export function SignIn({ path }: SignInProps) {
  const [token, setToken] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = useCallback(
    async (event: React.FormEvent) => {
      event.preventDefault()
      setError('')
      setIsSubmitting(true)
      try {
        const res = await fetch('/api/auth/session-token', {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ token: token.trim() }),
        })
        if (!res.ok) {
          const payload = await res.json().catch(() => ({}))
          throw new Error(payload?.error ?? 'Failed to create auth session')
        }
        setSessionFlag(true)
        window.location.href = path ?? '/settings'
      } catch (err: any) {
        setError(err.message ?? 'Sign-in failed')
      } finally {
        setIsSubmitting(false)
      }
    },
    [path, token]
  )

  return (
    <div className="glass border border-slate-700 rounded-xl p-6">
      <h1 className="text-2xl font-bold mb-3">Sign In</h1>
      <p className="text-sm text-slate-400 mb-4">
        Paste your Clerk JWT to start an authenticated session.
      </p>
      <form onSubmit={handleSubmit} className="space-y-4">
        <textarea
          value={token}
          onChange={event => setToken(event.target.value)}
          className="w-full h-28 rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs font-mono"
          placeholder="eyJhbGciOi..."
        />
        <button
          type="submit"
          disabled={isSubmitting || !token.trim()}
          className="w-full px-4 py-2 rounded-lg bg-primary-600 hover:bg-primary-500 disabled:opacity-50 text-sm font-medium"
        >
          {isSubmitting ? 'Signing in...' : 'Start Session'}
        </button>
      </form>
      {error && <p className="text-xs text-red-300 mt-3">{error}</p>}
    </div>
  )
}
