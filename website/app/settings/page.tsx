'use client'

import { SignedIn, SignedOut, SignInButton, UserButton, useAuth } from '../lib/clerk'
import Link from 'next/link'
import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react'

type KeyRole = 'OWNER' | 'ADMIN' | 'DEVELOPER' | 'VIEWER'

interface ApiKeyRecord {
  keyId: string
  name: string
  maskedKey: string
  keyPrefix: string
  role: KeyRole
  lastUsedAt?: string | null
  createdAt: string
  expiresAt?: string | null
  active: boolean
}

interface CurrentUsage {
  orgId: string
  period: string
  requests: number
  requestLimit: number
  requestsRemaining: number
}

interface DailyUsage {
  date: string
  requests: number
  tokens: number
  costUsd: number
}

const API_BASE_URL = process.env.NEXT_PUBLIC_NEUROGATE_API_BASE_URL ?? 'http://localhost:8080'

function formatDate(date: Date) {
  return date.toISOString().slice(0, 10)
}

export default function SettingsPage() {
  const { isSignedIn } = useAuth()

  const [keys, setKeys] = useState<ApiKeyRecord[]>([])
  const [currentUsage, setCurrentUsage] = useState<CurrentUsage | null>(null)
  const [usageHistory, setUsageHistory] = useState<DailyUsage[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isCreating, setIsCreating] = useState(false)
  const [statusMessage, setStatusMessage] = useState<string>('')
  const [createName, setCreateName] = useState('Backend Key')
  const [createRole, setCreateRole] = useState<KeyRole>('DEVELOPER')

  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteRole, setInviteRole] = useState<KeyRole>('VIEWER')
  const [invites, setInvites] = useState<Array<{ email: string; role: KeyRole }>>([])

  const usageRange = useMemo(() => {
    const end = new Date()
    const start = new Date()
    start.setDate(end.getDate() - 6)
    return { from: formatDate(start), to: formatDate(end) }
  }, [])

  const apiFetch = useCallback(async (path: string, init?: RequestInit) => {
    const headers = new Headers(init?.headers ?? {})
    if (!headers.has('Content-Type') && init?.body) {
      headers.set('Content-Type', 'application/json')
    }

    return fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers,
      credentials: 'include',
    })
  }, [])

  const loadData = useCallback(async () => {
    setIsLoading(true)
    setStatusMessage('')
    try {
      const [keysRes, usageRes, historyRes] = await Promise.all([
        apiFetch('/api/v1/keys'),
        apiFetch('/api/v1/usage'),
        apiFetch(`/api/v1/usage/history?from=${usageRange.from}&to=${usageRange.to}`),
      ])

      if (!keysRes.ok || !usageRes.ok || !historyRes.ok) {
        throw new Error('Failed to load dashboard data. Ensure backend auth is configured and you are signed in.')
      }

      const [keysData, usageData, historyData] = await Promise.all([
        keysRes.json(),
        usageRes.json(),
        historyRes.json(),
      ])

      setKeys(keysData)
      setCurrentUsage(usageData)
      setUsageHistory(historyData)
    } catch (error: any) {
      setStatusMessage(error.message ?? 'Unable to load settings data.')
    } finally {
      setIsLoading(false)
    }
  }, [apiFetch, usageRange.from, usageRange.to])

  useEffect(() => {
    if (isSignedIn) {
      loadData()
    }
  }, [isSignedIn, loadData])

  const handleCreateKey = async (event: FormEvent) => {
    event.preventDefault()
    setIsCreating(true)
    setStatusMessage('')
    try {
      const res = await apiFetch('/api/v1/keys', {
        method: 'POST',
        body: JSON.stringify({
          name: createName,
          role: createRole,
        }),
      })

      const payload = await res.json()
      if (!res.ok) {
        throw new Error(payload?.message ?? 'Failed to create key')
      }

      setStatusMessage(`New key created: ${payload.key}. Copy it now; it will not be shown again.`)
      await loadData()
    } catch (error: any) {
      setStatusMessage(error.message ?? 'Could not create key')
    } finally {
      setIsCreating(false)
    }
  }

  const handleRevokeKey = async (id: string) => {
    setStatusMessage('')
    const res = await apiFetch(`/api/v1/keys/${id}`, { method: 'DELETE' })
    if (!res.ok) {
      const payload = await res.json().catch(() => ({}))
      setStatusMessage(payload?.message ?? 'Failed to revoke key')
      return
    }
    setStatusMessage('Key revoked.')
    await loadData()
  }

  const handleRotateKey = async (id: string) => {
    setStatusMessage('')
    const res = await apiFetch(`/api/v1/keys/${id}/rotate`, { method: 'POST' })
    const payload = await res.json().catch(() => ({}))
    if (!res.ok) {
      setStatusMessage(payload?.message ?? 'Failed to rotate key')
      return
    }
    setStatusMessage(`Key rotated: ${payload.key}. Copy it now; it will not be shown again.`)
    await loadData()
  }

  const handleInvite = (event: FormEvent) => {
    event.preventDefault()
    if (!inviteEmail.trim()) {
      return
    }
    setInvites(prev => [{ email: inviteEmail.trim(), role: inviteRole }, ...prev])
    setInviteEmail('')
  }

  const maxUsageRequest = Math.max(1, ...usageHistory.map(d => d.requests))

  return (
    <div className="min-h-screen bg-black text-slate-100">
      <div className="max-w-7xl mx-auto px-4 py-10 sm:px-6 lg:px-8">
        <header className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between mb-8">
          <div>
            <Link href="/" className="text-primary-400 hover:text-primary-300 text-sm">← Back to Home</Link>
            <h1 className="text-3xl sm:text-4xl font-bold mt-3">Settings & Dashboard</h1>
            <p className="text-slate-400 mt-2">Manage API keys, usage, team roles, and plan limits.</p>
          </div>
          <div className="flex items-center gap-3">
            <SignedIn>
              <UserButton afterSignOutUrl="/" />
            </SignedIn>
          </div>
        </header>

        <SignedOut>
          <div className="glass border border-slate-700 rounded-xl p-8 max-w-xl">
            <h2 className="text-2xl font-semibold mb-3">Sign in required</h2>
            <p className="text-slate-400 mb-6">Use Clerk authentication to access organization settings.</p>
            <SignInButton mode="redirect" forceRedirectUrl="/settings">
              <button className="px-5 py-2.5 rounded-lg bg-primary-600 hover:bg-primary-500 transition-colors font-medium">
                Sign In
              </button>
            </SignInButton>
          </div>
        </SignedOut>

        <SignedIn>
          {statusMessage && (
            <div className="mb-6 p-4 rounded-lg border border-primary-500/40 bg-primary-900/20 text-primary-200 text-sm">
              {statusMessage}
            </div>
          )}

          {isLoading ? (
            <div className="text-slate-400">Loading dashboard...</div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <section className="glass border border-slate-700 rounded-xl p-6">
                <h2 className="text-xl font-semibold mb-4">API Key Management</h2>
                <form className="grid sm:grid-cols-3 gap-3 mb-6" onSubmit={handleCreateKey}>
                  <input
                    value={createName}
                    onChange={e => setCreateName(e.target.value)}
                    className="sm:col-span-2 bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-sm"
                    placeholder="Key name"
                  />
                  <select
                    value={createRole}
                    onChange={e => setCreateRole(e.target.value as KeyRole)}
                    className="bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-sm"
                  >
                    <option value="DEVELOPER">DEVELOPER</option>
                    <option value="ADMIN">ADMIN</option>
                    <option value="VIEWER">VIEWER</option>
                    <option value="OWNER">OWNER</option>
                  </select>
                  <button
                    type="submit"
                    disabled={isCreating}
                    className="sm:col-span-3 px-4 py-2 rounded-lg bg-primary-600 hover:bg-primary-500 disabled:opacity-50 text-sm font-medium"
                  >
                    {isCreating ? 'Creating...' : 'Create API Key'}
                  </button>
                </form>

                <div className="space-y-3">
                  {keys.map(key => (
                    <div key={key.keyId} className="border border-slate-800 rounded-lg p-4 bg-slate-950/40">
                      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                        <div>
                          <p className="font-medium">{key.name}</p>
                          <p className="text-xs text-slate-400">{key.maskedKey} · {key.role}</p>
                        </div>
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => handleRotateKey(key.keyId)}
                            className="px-3 py-1.5 text-xs rounded-md border border-slate-700 hover:border-primary-500 hover:text-primary-300"
                          >
                            Rotate
                          </button>
                          <button
                            onClick={() => handleRevokeKey(key.keyId)}
                            className="px-3 py-1.5 text-xs rounded-md border border-red-700 text-red-300 hover:bg-red-900/30"
                          >
                            Revoke
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                  {keys.length === 0 && <p className="text-sm text-slate-500">No active API keys.</p>}
                </div>
              </section>

              <section className="glass border border-slate-700 rounded-xl p-6">
                <h2 className="text-xl font-semibold mb-4">Usage Overview</h2>
                {currentUsage ? (
                  <div className="space-y-4">
                    <div className="grid grid-cols-3 gap-3">
                      <div className="bg-slate-950/40 rounded-lg p-3 border border-slate-800">
                        <p className="text-xs text-slate-400">Used</p>
                        <p className="text-lg font-semibold">{currentUsage.requests.toLocaleString()}</p>
                      </div>
                      <div className="bg-slate-950/40 rounded-lg p-3 border border-slate-800">
                        <p className="text-xs text-slate-400">Limit</p>
                        <p className="text-lg font-semibold">{currentUsage.requestLimit.toLocaleString()}</p>
                      </div>
                      <div className="bg-slate-950/40 rounded-lg p-3 border border-slate-800">
                        <p className="text-xs text-slate-400">Remaining</p>
                        <p className="text-lg font-semibold">{currentUsage.requestsRemaining.toLocaleString()}</p>
                      </div>
                    </div>
                    <div className="h-44 flex items-end gap-2 border border-slate-800 rounded-lg p-3 bg-slate-950/40">
                      {usageHistory.map(day => {
                        const heightPercent = Math.max(6, (day.requests / maxUsageRequest) * 100)
                        return (
                          <div key={day.date} className="flex-1 flex flex-col items-center gap-2">
                            <div
                              className="w-full rounded-t-md bg-gradient-to-t from-primary-600 to-primary-400"
                              style={{ height: `${heightPercent}%` }}
                              title={`${day.date}: ${day.requests} requests`}
                            />
                            <span className="text-[10px] text-slate-400">{day.date.slice(5)}</span>
                          </div>
                        )
                      })}
                    </div>
                  </div>
                ) : (
                  <p className="text-sm text-slate-500">Usage data unavailable.</p>
                )}
              </section>

              <section className="glass border border-slate-700 rounded-xl p-6">
                <h2 className="text-xl font-semibold mb-4">Team Management</h2>
                <form className="grid sm:grid-cols-3 gap-3 mb-5" onSubmit={handleInvite}>
                  <input
                    value={inviteEmail}
                    onChange={e => setInviteEmail(e.target.value)}
                    className="sm:col-span-2 bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-sm"
                    placeholder="teammate@company.com"
                  />
                  <select
                    value={inviteRole}
                    onChange={e => setInviteRole(e.target.value as KeyRole)}
                    className="bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-sm"
                  >
                    <option value="VIEWER">VIEWER</option>
                    <option value="DEVELOPER">DEVELOPER</option>
                    <option value="ADMIN">ADMIN</option>
                    <option value="OWNER">OWNER</option>
                  </select>
                  <button type="submit" className="sm:col-span-3 px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-sm font-medium">
                    Invite Member
                  </button>
                </form>
                <div className="space-y-2">
                  {invites.map(member => (
                    <div key={`${member.email}-${member.role}`} className="flex items-center justify-between rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2">
                      <span className="text-sm">{member.email}</span>
                      <span className="text-xs text-slate-400">{member.role}</span>
                    </div>
                  ))}
                  {invites.length === 0 && <p className="text-sm text-slate-500">No pending invites yet.</p>}
                </div>
              </section>

              <section className="glass border border-slate-700 rounded-xl p-6">
                <h2 className="text-xl font-semibold mb-4">Plan & Billing</h2>
                <div className="space-y-3">
                  <div className="rounded-lg border border-slate-800 bg-slate-950/40 p-3">
                    <p className="text-xs text-slate-400">Current Period</p>
                    <p className="font-medium">{currentUsage?.period ?? 'N/A'}</p>
                  </div>
                  <div className="rounded-lg border border-slate-800 bg-slate-950/40 p-3">
                    <p className="text-xs text-slate-400">Request Cap</p>
                    <p className="font-medium">{currentUsage?.requestLimit?.toLocaleString() ?? 'N/A'} requests / month</p>
                  </div>
                  <p className="text-xs text-slate-500">
                    Billing tier is derived from organization plan on the backend (Free/Pro/Team) and enforced for API requests.
                  </p>
                </div>
              </section>
            </div>
          )}
        </SignedIn>
      </div>
    </div>
  )
}
