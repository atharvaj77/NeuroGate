'use client';

import React, { useState, useEffect } from 'react';
import Timeline from '../components/debugger/Timeline';
import StateInspector from '../components/debugger/StateInspector';
import ReplayPlayer from '../components/debugger/ReplayPlayer';
import Link from 'next/link';
import { FaArrowLeft, FaInfoCircle, FaCodeBranch, FaBug } from 'react-icons/fa';
import { motion, AnimatePresence } from 'framer-motion';

// Mock Data (will be replaced by API call)
const MOCK_SESSION = {
    sessionId: "sess_12345",
    steps: [
        { stepId: "step_1", timestamp: "2024-01-01T15:12:00.000Z", stepType: "USER_INPUT", content: "Write a python script to scrape data", state: {} },
        { stepId: "step_2", timestamp: "2024-01-01T15:12:05.000Z", stepType: "TOOL_CALL", content: "search_web({ query: 'python scraping libraries' })", state: { memory: "empty" } },
        { stepId: "step_3", timestamp: "2024-01-01T15:12:10.000Z", stepType: "TOOL_RESULT", content: "Found: BeautifulSoup, Scrapy, Selenium...", state: { memory: "context_loaded" } },
        { stepId: "step_4", timestamp: "2024-01-01T15:12:15.000Z", stepType: "MODEL_RESPONSE", content: "Here is a script using BeautifulSoup...", state: { memory: "final_answer" } }
    ]
};

export default function DebuggerPage() {
    const [currentStepIndex, setCurrentStepIndex] = useState(0);
    const [isPlaying, setIsPlaying] = useState(false);
    const [session, setSession] = useState(MOCK_SESSION);
    const [showInfo, setShowInfo] = useState(false);

    useEffect(() => {
        let interval: NodeJS.Timeout;
        if (isPlaying) {
            interval = setInterval(() => {
                setCurrentStepIndex(prev => {
                    if (prev < session.steps.length - 1) return prev + 1;
                    setIsPlaying(false);
                    return prev;
                });
            }, 1000);
        }
        return () => clearInterval(interval);
    }, [isPlaying, session.steps.length]);

    return (
        <div className="flex h-screen bg-black text-slate-100 font-sans overflow-hidden selection:bg-primary-500/30">
            {/* Background Effects */}
            <div className="absolute inset-0 bg-[url('/grid.svg')] opacity-20 pointer-events-none" />

            {/* Left Sidebar: Timeline */}
            <div className="w-1/4 h-full border-r border-white/5 flex flex-col glass z-10">
                <div className="p-4 border-b border-white/5 bg-black/40">
                    <Link href="/" className="flex items-center gap-2 text-slate-400 hover:text-white transition-colors mb-4 text-sm">
                        <FaArrowLeft /> Back to OS
                    </Link>
                    <h1 className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-primary-400 to-accent-400 font-mono">
                        Time Travel Debugger
                    </h1>
                    {/* Simulation Mode Indicator */}
                    <div className="mt-3 flex items-center gap-2 px-3 py-1 rounded bg-yellow-500/10 border border-yellow-500/30 text-yellow-500 group relative cursor-help w-fit text-xs">
                        <div className="w-1.5 h-1.5 rounded-full bg-yellow-500 animate-pulse" />
                        SIMULATION MODE

                        {/* Tooltip */}
                        <div className="absolute top-full left-0 mt-2 w-72 p-3 bg-black border border-white/20 rounded shadow-xl opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-50">
                            <p className="text-slate-400 mb-2">Session data mocked for demo.</p>
                            <div className="p-2 bg-white/5 rounded border border-white/10 font-mono text-[10px] text-cyan-300 break-all mb-1">
                                POST /api/debug/sessions
                            </div>
                            <div className="p-2 bg-white/5 rounded border border-white/10 font-mono text-[10px] text-slate-400 break-all">
                                {`{ "requestId": "req_123" }`}
                            </div>
                            <p className="text-slate-500 mt-2 font-normal">Connects to &apos;DebuggerController&apos;.</p>
                        </div>
                    </div>

                    <div className="flex items-center gap-2 mt-3">
                        <span className="w-2 h-2 rounded-full bg-primary-500 animate-pulse-glow"></span>
                        <p className="text-xs text-primary-300 font-mono">Session: {session.sessionId}</p>
                    </div>
                </div>
                <div className="flex-1 overflow-y-auto custom-scrollbar">
                    <Timeline
                        steps={session.steps}
                        currentStepIndex={currentStepIndex}
                        onStepClick={setCurrentStepIndex}
                    />
                </div>
            </div>

            {/* Main Content: State & Player */}
            <div className="flex-1 flex flex-col h-full relative bg-gradient-to-br from-black to-primary-950/10 z-0">
                <div className="flex-1 overflow-hidden relative p-8">

                    {/* Header Controls */}
                    <div className="absolute top-4 right-4 z-20 flex gap-4">
                        <button
                            onClick={() => setShowInfo(!showInfo)}
                            className="glass-button flex items-center gap-2 text-slate-300 hover:text-white"
                        >
                            <FaInfoCircle /> {showInfo ? 'Hide Info' : 'Debugger Guide'}
                        </button>
                        <button className="glass-button border-primary-500/30 text-primary-300 hover:bg-primary-500/20 hover:text-white flex items-center gap-2">
                            <FaCodeBranch /> Forks
                        </button>
                    </div>

                    {/* Info Panel Overlay */}
                    <AnimatePresence>
                        {showInfo && (
                            <motion.div
                                initial={{ opacity: 0, scale: 0.95 }}
                                animate={{ opacity: 1, scale: 1 }}
                                exit={{ opacity: 0, scale: 0.95 }}
                                className="absolute top-16 right-4 w-[600px] glass p-8 rounded-2xl z-30 border border-white/10 shadow-2xl shadow-black/80 backdrop-blur-xl"
                            >
                                <div className="flex justify-between items-start mb-6">
                                    <div>
                                        <h3 className="text-2xl font-bold text-white mb-2 flex items-center gap-3">
                                            <FaBug className="text-primary-400" />
                                            NeuroGate Time Travel
                                        </h3>
                                        <p className="text-slate-400 text-sm">Deterministic Replay & Counterfactual Analysis Engine</p>
                                    </div>
                                    <button onClick={() => setShowInfo(false)} className="p-2 hover:bg-white/10 rounded-full transition-colors">
                                        <div className="w-6 h-6 flex items-center justify-center text-slate-500">✕</div>
                                    </button>
                                </div>

                                <div className="grid grid-cols-2 gap-8">
                                    <div className="space-y-6">
                                        <div className="group">
                                            <h4 className="flex items-center gap-2 text-primary-300 font-bold mb-2 text-sm uppercase tracking-wider">
                                                <span className="w-6 h-6 rounded bg-primary-500/10 flex items-center justify-center text-primary-400 border border-primary-500/20">1</span>
                                                Capture Phase
                                            </h4>
                                            <p className="text-xs text-slate-400 leading-relaxed">
                                                The <code className="text-orange-300">AIDebuggerService</code> acts as a hypervisor, intercepting every LLM call. It creates an immutable snapshot of:
                                                <br />• Input/Output Tensors
                                                <br />• Memory Context Window
                                                <br />• Tool Execution Results
                                            </p>
                                        </div>

                                        <div className="group">
                                            <h4 className="flex items-center gap-2 text-accent-300 font-bold mb-2 text-sm uppercase tracking-wider">
                                                <span className="w-6 h-6 rounded bg-accent-500/10 flex items-center justify-center text-accent-400 border border-accent-500/20">2</span>
                                                Replay Engine
                                            </h4>
                                            <p className="text-xs text-slate-400 leading-relaxed">
                                                The <code className="text-orange-300">DebuggerController</code> re-hydrates the agent&apos;s state from any timestamp.
                                                It allows <strong>deterministic step-through</strong>, guaranteeing the exact same random seed and environment variables as the original run.
                                            </p>
                                        </div>
                                    </div>

                                    <div className="space-y-6">
                                        <div className="group">
                                            <h4 className="flex items-center gap-2 text-green-300 font-bold mb-2 text-sm uppercase tracking-wider">
                                                <span className="w-6 h-6 rounded bg-green-500/10 flex items-center justify-center text-green-400 border border-green-500/20">3</span>
                                                Branching Timelines
                                            </h4>
                                            <p className="text-xs text-slate-400 leading-relaxed mb-3">
                                                Execute <strong>Counterfactuals</strong> by forking the session.
                                            </p>
                                            <div className="bg-black/40 rounded border border-white/5 p-3 font-mono text-[10px] text-slate-300">
                                                <div className="flex items-center gap-2 text-green-400 mb-1">
                                                    <FaCodeBranch /> forkSession(stepId)
                                                </div>
                                                <span className="text-slate-500">{`// Creates parallel universe`}</span><br />
                                                <span className="text-purple-400">const</span> diff = <span className="text-blue-300">compare</span>(original, fork);
                                            </div>
                                        </div>

                                        <div className="p-4 rounded-xl bg-gradient-to-br from-primary-900/20 to-accent-900/20 border border-white/5">
                                            <div className="text-[10px] uppercase tracking-widest text-slate-500 mb-2">Architecture</div>
                                            <div className="flex justify-between items-center text-xs font-mono">
                                                <div className="text-center">
                                                    <div className="p-2 bg-slate-800 rounded mb-1">Agent</div>
                                                </div>
                                                <div className="h-px w-8 bg-slate-600"></div>
                                                <div className="text-center">
                                                    <div className="p-2 bg-primary-900/50 border border-primary-500/30 rounded mb-1 text-primary-300">Trace</div>
                                                </div>
                                                <div className="h-px w-8 bg-slate-600"></div>
                                                <div className="text-center">
                                                    <div className="p-2 bg-slate-800 rounded mb-1">DB</div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </motion.div>
                        )}
                    </AnimatePresence>

                    {/* Visualizer Area (State Inspector) */}
                    <div className="h-full rounded-2xl border border-white/5 bg-black/40 overflow-hidden relative shadow-2xl">
                        <div className="absolute top-0 left-0 w-full h-8 bg-white/5 border-b border-white/5 flex items-center px-4 gap-2">
                            <div className="w-3 h-3 rounded-full bg-red-500/50"></div>
                            <div className="w-3 h-3 rounded-full bg-yellow-500/50"></div>
                            <div className="w-3 h-3 rounded-full bg-green-500/50"></div>
                            <span className="text-xs text-slate-500 font-mono ml-2">state_inspector_v1.tsx</span>
                        </div>
                        <div className="pt-8 h-full">
                            <StateInspector step={session.steps[currentStepIndex]} />
                        </div>
                    </div>
                </div>

                {/* Bottom Bar: Player Controls */}
                <div className="h-24 bg-black/60 backdrop-blur-md border-t border-white/10 flex items-center justify-center">
                    <ReplayPlayer
                        isPlaying={isPlaying}
                        onPlayPause={() => setIsPlaying(!isPlaying)}
                        onNext={() => setCurrentStepIndex(prev => Math.min(prev + 1, session.steps.length - 1))}
                        onPrev={() => setCurrentStepIndex(prev => Math.max(prev - 1, 0))}
                        onReset={() => { setIsPlaying(false); setCurrentStepIndex(0); }}
                        currentIndex={currentStepIndex}
                        totalSteps={session.steps.length}
                    />
                </div>
            </div>
        </div>
    );
}
