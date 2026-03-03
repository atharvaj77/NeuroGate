'use client'

import { useState, useEffect, useRef, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { FaServer, FaMicrochip, FaNetworkWired, FaShieldAlt, FaBolt, FaGlobe, FaClock, FaWifi, FaExclamationTriangle } from 'react-icons/fa'
import { SiOpenai, SiAnthropic, SiGooglecloud } from 'react-icons/si'
import Link from 'next/link'

// ─────────────────────────────────────────────────────────────────────────────
// Types
// ─────────────────────────────────────────────────────────────────────────────

interface MetricsSnapshot {
    type?: string
    timestamp: string
    payload?: {
        rps: number
        avg_latency_ms: number
        p95_latency_ms: number
        error_rate: number
        token_count: number
        cache_hit_rate: number
        pii_blocked: number
        active_provider: string
        cost_usd_total: number
    }
    // Direct fields when type !== METRIC_UPDATE
    rps?: number
    avg_latency_ms?: number
    p95_latency_ms?: number
    error_rate?: number
    token_count?: number
    cache_hit_rate?: number
    pii_blocked?: number
    active_provider?: string
    cost_usd_total?: number
}

type Provider = 'openai' | 'anthropic' | 'gemini'
type ConnectionStatus = 'CONNECTING' | 'CONNECTED' | 'RECONNECTING' | 'DISCONNECTED' | 'SIMULATION'

// ─────────────────────────────────────────────────────────────────────────────
// Simulation helpers (used as fallback when backend unreachable)
// ─────────────────────────────────────────────────────────────────────────────

const PROVIDERS: Provider[] = ['openai', 'anthropic', 'gemini']
const generateLatency = (base: number, variance: number) =>
    Math.max(10, Math.floor(base + (Math.random() * variance * (Math.random() > 0.5 ? 1 : -1))))

// ─────────────────────────────────────────────────────────────────────────────
// WebSocket hook with exponential-backoff reconnect
// ─────────────────────────────────────────────────────────────────────────────

const WS_URL = process.env.NEXT_PUBLIC_WS_URL ?? 'ws://localhost:8080/ws/pulse'
const MAX_BACKOFF_MS = 30_000

function usePulseWebSocket(
    onSnapshot: (snap: MetricsSnapshot) => void,
    onStatusChange: (status: ConnectionStatus) => void
) {
    const wsRef = useRef<WebSocket | null>(null)
    const backoffRef = useRef(1_000)
    const retryRef = useRef<ReturnType<typeof setTimeout> | null>(null)
    const unmountedRef = useRef(false)

    const connect = useCallback(() => {
        if (unmountedRef.current) return
        onStatusChange('CONNECTING')

        const ws = new WebSocket(WS_URL)
        wsRef.current = ws

        ws.onopen = () => {
            backoffRef.current = 1_000 // reset backoff on success
            onStatusChange('CONNECTED')
        }

        ws.onmessage = (evt) => {
            try {
                const msg: MetricsSnapshot = JSON.parse(evt.data)
                // Accept both wrapper format (payload field) and flat format
                if (msg.type === 'METRIC_UPDATE' && msg.payload) {
                    onSnapshot({ timestamp: msg.timestamp, ...msg.payload })
                } else if (msg.rps !== undefined || (msg.payload?.rps !== undefined)) {
                    onSnapshot(msg)
                }
            } catch {
                // Non-JSON control messages (e.g. pulse_connected) — ignore
            }
        }

        ws.onerror = () => {
            // error is always followed by close, handle in onclose
        }

        ws.onclose = () => {
            if (unmountedRef.current) return
            onStatusChange('RECONNECTING')
            retryRef.current = setTimeout(() => {
                if (!unmountedRef.current) connect()
            }, backoffRef.current)
            backoffRef.current = Math.min(backoffRef.current * 2, MAX_BACKOFF_MS)
        }
    }, [onSnapshot, onStatusChange])

    useEffect(() => {
        connect()
        return () => {
            unmountedRef.current = true
            if (retryRef.current) clearTimeout(retryRef.current)
            wsRef.current?.close()
        }
    }, [connect])
}

// ─────────────────────────────────────────────────────────────────────────────
// Page component
// ─────────────────────────────────────────────────────────────────────────────

export default function PulsePage() {
    // ── Display state ────────────────────────────────────────────────────────
    const [latencyHistory, setLatencyHistory] = useState<number[]>(new Array(40).fill(120))
    const [requestsPerSecond, setRequestsPerSecond] = useState(0)
    const [totalTokens, setTotalTokens] = useState(0)
    const [activeRoute, setActiveRoute] = useState<Provider>('openai')
    const [piiBlocked, setPiiBlocked] = useState(0)
    const [cacheHits, setCacheHits] = useState(0)
    const [errorRate, setErrorRate] = useState(0)
    const [costTotal, setCostTotal] = useState(0)

    // ── Connection state ─────────────────────────────────────────────────────
    const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('CONNECTING')
    const simulationRef = useRef<ReturnType<typeof setInterval> | null>(null)

    // ── WebSocket snapshot handler ────────────────────────────────────────────
    const handleSnapshot = useCallback((snap: MetricsSnapshot) => {
        const data = snap.payload ?? snap as any
        const latencyMs = Math.round(data.avg_latency_ms ?? 120)
        setLatencyHistory(prev => [...prev.slice(1), latencyMs])
        setRequestsPerSecond(Math.round(data.rps ?? 0))
        setTotalTokens(data.token_count ?? 0)
        setPiiBlocked(data.pii_blocked ?? 0)
        setErrorRate(data.error_rate ?? 0)
        setCostTotal(data.cost_usd_total ?? 0)

        const provider = (data.active_provider ?? 'openai').toLowerCase()
        if (PROVIDERS.includes(provider as Provider)) {
            setActiveRoute(provider as Provider)
        }

        const cacheRate = data.cache_hit_rate ?? 0
        // Derive a synthetic hit count for display (rate * approx total)
        setCacheHits(prev => Math.max(prev, Math.round(cacheRate * (data.token_count ?? 1) * 0.01)))
    }, [])

    // ── Status change handler ─────────────────────────────────────────────────
    const handleStatusChange = useCallback((status: ConnectionStatus) => {
        setConnectionStatus(status)

        if (status === 'RECONNECTING' || status === 'DISCONNECTED') {
            // Falls back to simulation when backend is unreachable
            if (!simulationRef.current) {
                setConnectionStatus('SIMULATION')
                simulationRef.current = setInterval(() => {
                    const newLatency = generateLatency(120, 40)
                    setLatencyHistory(prev => [...prev.slice(1), newLatency])
                    setRequestsPerSecond(prev => Math.max(20, Math.min(150, prev + Math.floor(Math.random() * 10 - 4))))
                    setTotalTokens(prev => prev + Math.floor(Math.random() * 500))
                    if (Math.random() > 0.95) setPiiBlocked(prev => prev + 1)
                    if (Math.random() > 0.8) setCacheHits(prev => prev + 1)
                    if (Math.random() > 0.98) {
                        setActiveRoute(PROVIDERS[Math.floor(Math.random() * PROVIDERS.length)])
                    }
                }, 800)
            }
        } else if (status === 'CONNECTED') {
            // Stop simulation when real data arrives
            if (simulationRef.current) {
                clearInterval(simulationRef.current)
                simulationRef.current = null
            }
        }
    }, [])

    usePulseWebSocket(handleSnapshot, handleStatusChange)

    useEffect(() => {
        return () => {
            if (simulationRef.current) clearInterval(simulationRef.current)
        }
    }, [])

    // ── Chart computation ─────────────────────────────────────────────────────
    const maxLatency = 200
    const points = latencyHistory.map((val, i) => {
        const x = (i / (latencyHistory.length - 1)) * 100
        const y = 100 - (val / maxLatency) * 100
        return `${x},${y}`
    }).join(' ')

    const currentLatency = latencyHistory[latencyHistory.length - 1]

    return (
        <main className="min-h-screen bg-black text-cyan-50 font-mono selection:bg-cyan-900/50 overflow-hidden relative">
            <div className="fixed inset-0 bg-[url('/grid.svg')] opacity-20 pointer-events-none" />
            <div className="fixed inset-0 bg-gradient-to-b from-black via-transparent to-black pointer-events-none" />

            {/* Navigation Bar */}
            <nav className="fixed top-0 w-full z-50 border-b border-cyan-900/30 bg-black/80 backdrop-blur-md">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between items-center h-16">
                        <Link href="/">
                            <div className="flex items-center space-x-3 cursor-pointer group">
                                <FaNetworkWired className="text-cyan-400 group-hover:animate-pulse" />
                                <span className="text-lg font-bold tracking-widest text-cyan-400 uppercase">NeuroGate<span className="text-white">OS</span></span>
                                <span className="px-2 py-0.5 rounded-sm bg-cyan-900/30 text-[10px] text-cyan-300 border border-cyan-500/30">KERNEL_MODE</span>
                            </div>
                        </Link>
                        <div className="flex items-center space-x-4 text-xs">
                            {/* Connection status badge */}
                            <ConnectionBadge status={connectionStatus} />
                            <div className="hidden md:block text-slate-500">
                                COST: <span className="text-yellow-400">${costTotal.toFixed(4)}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </nav>

            <div className="pt-24 pb-20 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">

                {/* Top KPIs */}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
                    <MetricCard
                        label="LIVE LATENCY"
                        value={`${currentLatency}ms`}
                        subtext={`P95: ${Math.round(currentLatency * 1.4)}ms`}
                        icon={<FaClock className="text-cyan-400" />}
                        glowColor="cyan"
                    />
                    <MetricCard
                        label="TOKEN FLUX"
                        value={totalTokens.toLocaleString()}
                        subtext={`${requestsPerSecond} RPS`}
                        icon={<FaBolt className="text-yellow-400" />}
                        glowColor="yellow"
                    />
                    <MetricCard
                        label="THREATS BLOCKED"
                        value={piiBlocked.toLocaleString()}
                        subtext={`Error rate: ${(errorRate * 100).toFixed(1)}%`}
                        icon={<FaShieldAlt className="text-red-400" />}
                        glowColor="red"
                    />
                    <MetricCard
                        label="CACHE SAVINGS"
                        value={`$${(cacheHits * 0.002).toFixed(2)}`}
                        subtext={`${cacheHits.toLocaleString()} Hits`}
                        icon={<FaMicrochip className="text-green-400" />}
                        glowColor="green"
                    />
                </div>

                {/* Main Dashboard Grid */}
                <div className="grid lg:grid-cols-3 gap-6 mb-8">

                    {/* 1. Real-time Latency Graph */}
                    <div className="lg:col-span-2 p-6 rounded-lg border border-cyan-900/50 bg-black/40 backdrop-blur-sm relative overflow-hidden group hover:border-cyan-500/30 transition-colors">
                        <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-cyan-500 to-transparent opacity-20" />
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="text-sm font-bold text-cyan-400 flex items-center gap-2">
                                <FaGlobe />
                                NETWORK_LATENCY_STREAM
                            </h3>
                            <div className="flex gap-2 items-center text-[10px] text-cyan-700">
                                <span className="w-2 h-2 bg-cyan-500 rounded-sm"></span>
                                avg: {currentLatency}ms
                            </div>
                        </div>

                        <div className="h-64 w-full relative">
                            {/* Grid Lines */}
                            <div className="absolute inset-0 grid grid-rows-4 grid-cols-4 border-l border-b border-cyan-900/20">
                                {Array.from({ length: 4 }).map((_, i) => (
                                    <div key={i} className="border-t border-cyan-900/10 w-full h-full" />
                                ))}
                            </div>

                            {/* SVG Line Chart */}
                            <svg className="w-full h-full overflow-visible" preserveAspectRatio="none">
                                <defs>
                                    <linearGradient id="gradient" x1="0%" y1="0%" x2="0%" y2="100%">
                                        <stop offset="0%" stopColor="#22d3ee" stopOpacity="0.2" />
                                        <stop offset="100%" stopColor="#22d3ee" stopOpacity="0" />
                                    </linearGradient>
                                </defs>
                                <motion.path
                                    d={`M0,100 L${points} L100,100 Z`}
                                    fill="url(#gradient)"
                                    vectorEffect="non-scaling-stroke"
                                />
                                <motion.polyline
                                    points={points}
                                    fill="none"
                                    stroke="#22d3ee"
                                    strokeWidth="2"
                                    vectorEffect="non-scaling-stroke"
                                    initial={{ pathLength: 0 }}
                                    animate={{ pathLength: 1 }}
                                />
                                {/* Live Dot */}
                                <motion.circle
                                    cx="100%"
                                    cy={`${100 - (currentLatency / maxLatency) * 100}%`}
                                    r="4"
                                    fill="#fff"
                                    className="drop-shadow-[0_0_8px_rgba(34,211,238,1)]"
                                />
                                {/* Error rate threshold line at 5% */}
                                {errorRate > 0.05 && (
                                    <line x1="0" y1="40" x2="100" y2="40" stroke="#ef4444" strokeWidth="1"
                                        strokeDasharray="4,4" opacity="0.5" />
                                )}
                            </svg>
                        </div>
                        <div className="mt-2 flex justify-between text-[10px] text-cyan-700 font-mono">
                            <span>-40s</span>
                            <span>NOW</span>
                        </div>
                    </div>

                    {/* 2. Active Routing Neural Net */}
                    <div className="p-6 rounded-lg border border-cyan-900/50 bg-black/40 backdrop-blur-sm flex flex-col items-center justify-center relative">
                        <h3 className="absolute top-6 left-6 text-sm font-bold text-cyan-400 flex items-center gap-2">
                            <FaNetworkWired />
                            NEURAL_ROUTER
                        </h3>

                        <div className="relative w-full h-64 flex flex-col justify-between py-8">
                            {/* Client Node */}
                            <div className="self-center z-10 bg-slate-900 border border-slate-700 p-2 rounded text-xs text-slate-300 w-24 text-center">
                                CLIENT
                            </div>

                            {/* Router Node (NeuroGate) */}
                            <div className="self-center z-10 bg-cyan-900/40 border-2 border-cyan-400 p-3 rounded-lg text-sm text-cyan-100 font-bold w-32 text-center shadow-[0_0_20px_rgba(34,211,238,0.3)]">
                                NEUROGATE
                            </div>

                            {/* Providers */}
                            <div className="flex justify-between w-full px-4 mt-8">
                                <ProviderNode name="OpenAI" active={activeRoute === 'openai'} color="text-green-400" borderColor="border-green-500" Icon={SiOpenai} />
                                <ProviderNode name="Anthropic" active={activeRoute === 'anthropic'} color="text-orange-400" borderColor="border-orange-500" Icon={SiAnthropic} />
                                <ProviderNode name="Gemini" active={activeRoute === 'gemini'} color="text-blue-400" borderColor="border-blue-500" Icon={SiGooglecloud} />
                            </div>

                            {/* Animated Beams */}
                            <svg className="absolute inset-0 w-full h-full pointer-events-none z-0">
                                <motion.path
                                    d="M170,40 L170,90"
                                    stroke="#94a3b8"
                                    strokeWidth="1"
                                    fill="none"
                                    strokeDasharray="4,4"
                                />
                                <motion.path
                                    d={
                                        activeRoute === 'openai' ? "M170,140 L60,200" :
                                            activeRoute === 'anthropic' ? "M170,140 L170,200" :
                                                "M170,140 L280,200"
                                    }
                                    stroke={
                                        activeRoute === 'openai' ? "#4ade80" :
                                            activeRoute === 'anthropic' ? "#fb923c" :
                                                "#60a5fa"
                                    }
                                    strokeWidth="2"
                                    fill="none"
                                    initial={{ pathLength: 0, opacity: 0.2 }}
                                    animate={{ pathLength: 1, opacity: 1 }}
                                    transition={{ duration: 0.5 }}
                                />
                            </svg>
                        </div>
                    </div>
                </div>

                {/* Stats Row */}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
                    <StatBox label="ERROR RATE" value={`${(errorRate * 100).toFixed(2)}%`} accent={errorRate > 0.05 ? 'red' : 'green'} />
                    <StatBox label="RPS" value={requestsPerSecond.toString()} accent="cyan" />
                    <StatBox label="TOTAL COST" value={`$${costTotal.toFixed(4)}`} accent="yellow" />
                    <StatBox label="PII BLOCKS" value={piiBlocked.toLocaleString()} accent="red" />
                </div>

                {/* Logs Console */}
                <div className="p-4 rounded-lg border border-cyan-900/30 bg-black/60 font-mono text-xs h-40 overflow-hidden relative">
                    <div className="absolute top-0 left-0 right-0 p-2 bg-black/80 border-b border-cyan-900/30 text-cyan-600 flex justify-between items-center">
                        <span>SYSTEM_LOGS</span>
                        <span className={connectionStatus === 'CONNECTED' ? 'text-green-400 animate-pulse' : 'text-yellow-500 animate-pulse'}>
                            ● {connectionStatus}
                        </span>
                    </div>
                    <div className="mt-8 space-y-1 text-slate-400">
                        <LogEntry level="INFO" msg={`WebSocket status: ${connectionStatus}`} />
                        <LogEntry level="INFO" msg={`Active provider: ${activeRoute.toUpperCase()} | RPS: ${requestsPerSecond}`} />
                        <LogEntry level="DEBUG" msg={`Latency avg=${currentLatency}ms | ErrorRate=${(errorRate * 100).toFixed(1)}%`} />
                        {errorRate > 0.05 && (
                            <LogEntry level="WARN" msg={`High error rate ${(errorRate * 100).toFixed(1)}% — investigate ${activeRoute}`} color="text-yellow-500" />
                        )}
                        <div className="text-cyan-500 animate-pulse">_</div>
                    </div>
                </div>

            </div>
        </main>
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-components
// ─────────────────────────────────────────────────────────────────────────────

function ConnectionBadge({ status }: { status: ConnectionStatus }) {
    const config: Record<ConnectionStatus, { color: string; dot: string; label: string }> = {
        CONNECTED: { color: 'text-green-400 border-green-500/30 bg-green-500/10', dot: 'bg-green-500', label: 'LIVE' },
        CONNECTING: { color: 'text-cyan-400  border-cyan-500/30  bg-cyan-500/10', dot: 'bg-cyan-500', label: 'CONNECTING' },
        RECONNECTING: { color: 'text-yellow-400 border-yellow-500/30 bg-yellow-500/10', dot: 'bg-yellow-500', label: 'RECONNECTING' },
        DISCONNECTED: { color: 'text-red-400  border-red-500/30  bg-red-500/10', dot: 'bg-red-500', label: 'DISCONNECTED' },
        SIMULATION: { color: 'text-yellow-500 border-yellow-500/30 bg-yellow-500/10', dot: 'bg-yellow-500', label: 'DEMO MODE' },
    }

    const { color, dot, label } = config[status]

    return (
        <div className={`hidden md:flex items-center gap-2 px-3 py-1 rounded border text-[10px] font-mono ${color}`}>
            <div className={`w-1.5 h-1.5 rounded-full animate-pulse ${dot}`} />
            {label}
        </div>
    )
}

function MetricCard({ label, value, subtext, icon, glowColor }: any) {
    const colorClass = {
        cyan: 'text-cyan-400   from-cyan-500/10   via-cyan-500/5',
        yellow: 'text-yellow-400 from-yellow-500/10 via-yellow-500/5',
        red: 'text-red-400    from-red-500/10    via-red-500/5',
        green: 'text-green-400  from-green-500/10  via-green-500/5',
    }[glowColor as string]

    return (
        <div className={`p-5 rounded border border-white/5 bg-gradient-to-br to-transparent ${colorClass} hover:border-white/10 transition-all group`}>
            <div className="flex justify-between items-start mb-2">
                <span className="text-[10px] font-bold tracking-widest opacity-70 uppercase">{label}</span>
                <div className="p-2 rounded bg-white/5 group-hover:bg-white/10 transition-colors">{icon}</div>
            </div>
            <div className="text-2xl font-bold font-mono tracking-tighter text-white group-hover:scale-105 transition-transform origin-left">
                {value}
            </div>
            <div className="text-[10px] opacity-60 mt-1 font-mono">{subtext}</div>
        </div>
    )
}

function StatBox({ label, value, accent }: { label: string; value: string; accent: string }) {
    const accentClass: Record<string, string> = {
        cyan: 'text-cyan-400',
        green: 'text-green-400',
        red: 'text-red-400',
        yellow: 'text-yellow-400',
    }
    return (
        <div className="p-4 rounded border border-white/5 bg-white/2 flex flex-col gap-1">
            <span className="text-[9px] font-bold tracking-widest text-slate-500 uppercase">{label}</span>
            <span className={`text-xl font-mono font-bold ${accentClass[accent] ?? 'text-white'}`}>{value}</span>
        </div>
    )
}

function ProviderNode({ name, active, color, borderColor, Icon }: any) {
    return (
        <div className={`flex flex-col items-center gap-2 transition-all duration-300 ${active ? 'scale-110 opacity-100' : 'scale-90 opacity-40 grayscale'}`}>
            <div className={`w-12 h-12 rounded-lg border-2 bg-black flex items-center justify-center text-xl shadow-[0_0_15px_rgba(0,0,0,0.5)] ${active ? borderColor : 'border-slate-800'} ${color}`}>
                <Icon />
            </div>
            <span className="text-[10px] font-bold tracking-widest">{name}</span>
        </div>
    )
}

function LogEntry({ level, msg, color }: { level: string; msg: string; color?: string }) {
    const now = new Date()
    const time = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
    return (
        <div className="flex gap-4">
            <span className="text-slate-600">[{time}]</span>
            <span className={color ?? (level === 'INFO' ? 'text-blue-400' : level === 'DEBUG' ? 'text-slate-500' : 'text-green-400')}>{level}</span>
            <span>{msg}</span>
        </div>
    )
}
