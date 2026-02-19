'use client'

import { useState, useEffect, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { FaServer, FaMicrochip, FaNetworkWired, FaShieldAlt, FaBolt, FaGlobe, FaClock } from 'react-icons/fa'
import { SiOpenai, SiAnthropic, SiGooglecloud } from 'react-icons/si'
import Link from 'next/link'

// --- Mock Data Generators ---
const generateLatency = (base: number, variance: number) => {
    return Math.max(10, Math.floor(base + (Math.random() * variance * (Math.random() > 0.5 ? 1 : -1))))
}

export default function PulsePage() {
    const [latencyHistory, setLatencyHistory] = useState<number[]>(new Array(40).fill(120))
    const [requestsPerSecond, setRequestsPerSecond] = useState(42)
    const [totalTokens, setTotalTokens] = useState(1452030)
    const [activeRoute, setActiveRoute] = useState<'openai' | 'anthropic' | 'gemini'>('openai')
    const [piiBlocked, setPiiBlocked] = useState(843)
    const [cacheHits, setCacheHits] = useState(12045)

    // Real-time Simulation Loop
    useEffect(() => {
        const interval = setInterval(() => {
            // 1. Update Latency Chart
            setLatencyHistory(prev => {
                const newVal = generateLatency(120, 40)
                return [...prev.slice(1), newVal]
            })

            // 2. Fluctuate RPS
            setRequestsPerSecond(prev => Math.max(20, Math.min(150, prev + Math.floor(Math.random() * 10 - 4))))

            // 3. Increment Tokens
            setTotalTokens(prev => prev + Math.floor(Math.random() * 500))

            // 4. Random Events
            if (Math.random() > 0.95) {
                setPiiBlocked(prev => prev + 1)
            }
            if (Math.random() > 0.8) {
                setCacheHits(prev => prev + 1)
            }

            // 5. Route Switching (Rarely)
            if (Math.random() > 0.98) {
                const routes: ('openai' | 'anthropic' | 'gemini')[] = ['openai', 'anthropic', 'gemini']
                setActiveRoute(routes[Math.floor(Math.random() * routes.length)])
            }

        }, 800) // Update every 800ms

        return () => clearInterval(interval)
    }, [])

    // Calculate Chart Path
    const maxLatency = 200
    const chartHeight = 100
    const chartWidth = 100 // percent
    const points = latencyHistory.map((val, i) => {
        const x = (i / (latencyHistory.length - 1)) * 100
        const y = 100 - (val / maxLatency) * 100
        return `${x},${y}`
    }).join(' ')

    return (
        <main className="min-h-screen bg-black text-cyan-50 font-mono selection:bg-cyan-900/50 overflow-hidden relative">
            <div className="fixed inset-0 bg-[url('/grid.svg')] opacity-20 pointer-events-none" />
            <div className="fixed inset-0 bg-gradient-to-b from-black via-transparent to-black pointer-events-none" />

            {/* Navigation Bar (Simplified) */}
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
                        <div className="flex items-center space-x-6 text-xs">
                            <div className="flex items-center gap-2 text-green-400">
                                <div className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse" />
                            </div>
                            {/* Demo Mode Indicator */}
                            <div className="hidden md:flex items-center gap-2 px-3 py-1 rounded bg-yellow-500/10 border border-yellow-500/30 text-yellow-500 group relative cursor-help">
                                <div className="w-1.5 h-1.5 rounded-full bg-yellow-500 animate-pulse" />
                                DEMO MODE

                                {/* Tooltip */}
                                <div className="absolute top-full right-0 mt-2 w-64 p-3 bg-black border border-white/20 rounded shadow-xl opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-50">
                                    <p className="text-slate-400 mb-2">Data simulated for demo purposes.</p>
                                    <div className="p-2 bg-white/5 rounded border border-white/10 font-mono text-[10px] text-cyan-300 break-all">
                                        new WebSocket(&apos;ws://localhost:8080/pulse&apos;)
                                    </div>
                                    <p className="text-slate-500 mt-2">Connect to Core in production.</p>
                                </div>
                            </div>

                            <div className="hidden md:block text-slate-500">UPTIME: 34d 12h 04m</div>
                        </div>
                    </div>
                </div>
            </nav>

            <div className="pt-24 pb-20 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">

                {/* Top KPIs */}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
                    <MetricCard
                        label="LIVE LATENCY"
                        value={`${latencyHistory[latencyHistory.length - 1]}ms`}
                        subtext="Global Average"
                        icon={<FaClock className="text-cyan-400" />}
                        glowColor="cyan"
                    />
                    <MetricCard
                        label="TOKEN FLUX"
                        value={totalTokens.toLocaleString()}
                        subtext="+420/sec"
                        icon={<FaBolt className="text-yellow-400" />}
                        glowColor="yellow"
                    />
                    <MetricCard
                        label="THREATS BLOCKED"
                        value={piiBlocked.toLocaleString()}
                        subtext="PII & Injections"
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
                            <div className="flex gap-2">
                                <span className="w-2 h-2 bg-cyan-500 rounded-sm"></span>
                                <span className="w-2 h-2 bg-cyan-900 rounded-sm"></span>
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
                                    cy={`${100 - (latencyHistory[latencyHistory.length - 1] / maxLatency) * 100}%`}
                                    r="4"
                                    fill="#fff"
                                    className="drop-shadow-[0_0_8px_rgba(34,211,238,1)]"
                                />
                            </svg>
                        </div>
                        <div className="mt-2 flex justify-between text-[10px] text-cyan-700 font-mono">
                            <span>-30s</span>
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
                                <ProviderNode
                                    name="OpenAI"
                                    active={activeRoute === 'openai'}
                                    color="text-green-400"
                                    borderColor="border-green-500"
                                    Icon={SiOpenai}
                                />
                                <ProviderNode
                                    name="Anthropic"
                                    active={activeRoute === 'anthropic'}
                                    color="text-orange-400"
                                    borderColor="border-orange-500"
                                    Icon={SiAnthropic}
                                />
                                <ProviderNode
                                    name="Gemini"
                                    active={activeRoute === 'gemini'}
                                    color="text-blue-400"
                                    borderColor="border-blue-500"
                                    Icon={SiGooglecloud}
                                />
                            </div>

                            {/* Animated Beams */}
                            <svg className="absolute inset-0 w-full h-full pointer-events-none z-0">
                                {/* Client -> Router */}
                                <motion.path
                                    d="M170,40 L170,90"
                                    stroke="#94a3b8"
                                    strokeWidth="1"
                                    fill="none"
                                    strokeDasharray="4,4"
                                />

                                {/* Router -> Active Provider */}
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

                {/* Logs Console */}
                <div className="p-4 rounded-lg border border-cyan-900/30 bg-black/60 font-mono text-xs h-40 overflow-hidden relative">
                    <div className="absolute top-0 left-0 right-0 p-2 bg-black/80 border-b border-cyan-900/30 text-cyan-600 flex justify-between items-center">
                        <span>SYSTEM_LOGS</span>
                        <span className="animate-pulse">● REC</span>
                    </div>
                    <div className="mt-8 space-y-1 text-slate-400">
                        <LogEntry time="11:42:01" level="INFO" msg="Incoming request method=POST /v1/chat/completions" />
                        <LogEntry time="11:42:01" level="DEBUG" msg="PII Scanner: No sensitive data found." />
                        <LogEntry time="11:42:02" level="INFO" msg={`Routing to ${activeRoute.toUpperCase()} [Latency Score: 98/100]`} />
                        <LogEntry time="11:42:02" level="WARN" msg="Rate limit capacity at 45%" color="text-yellow-500" />
                        <div className="text-cyan-500 animate-pulse">_</div>
                    </div>
                </div>

            </div>
        </main>
    )
}

function MetricCard({ label, value, subtext, icon, glowColor }: any) {
    const colorClass = {
        cyan: 'text-cyan-400 from-cyan-500/10 via-cyan-500/5',
        yellow: 'text-yellow-400 from-yellow-500/10 via-yellow-500/5',
        red: 'text-red-400 from-red-500/10 via-red-500/5',
        green: 'text-green-400 from-green-500/10 via-green-500/5'
    }[glowColor as string]

    return (
        <div className={`p-5 rounded border border-white/5 bg-gradient-to-br to-transparent ${colorClass} hover:border-white/10 transition-all group`}>
            <div className="flex justify-between items-start mb-2">
                <span className="text-[10px] font-bold tracking-widest opacity-70 uppercase">{label}</span>
                <div className="p-2 rounded bg-white/5 group-hover:bg-white/10 transition-colors">
                    {icon}
                </div>
            </div>
            <div className="text-2xl font-bold font-mono tracking-tighter text-white group-hover:scale-105 transition-transform origin-left">
                {value}
            </div>
            <div className="text-[10px] opacity-60 mt-1 font-mono">
                {subtext}
            </div>
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

function LogEntry({ time, level, msg, color }: any) {
    return (
        <div className="flex gap-4">
            <span className="text-slate-600">[{time}]</span>
            <span className={color || (level === 'INFO' ? 'text-blue-400' : level === 'DEBUG' ? 'text-slate-500' : 'text-green-400')}>{level}</span>
            <span>{msg}</span>
        </div>
    )
}
