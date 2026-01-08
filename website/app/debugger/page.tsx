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
                    <div className="flex items-center gap-2 mt-2">
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
                                initial={{ opacity: 0, x: 20 }}
                                animate={{ opacity: 1, x: 0 }}
                                exit={{ opacity: 0, x: 20 }}
                                className="absolute top-16 right-4 w-80 glass p-6 rounded-xl z-30 border-l-2 border-l-accent-500 shadow-2xl shadow-black/50"
                            >
                                <h3 className="font-bold text-accent-400 mb-4 flex items-center gap-2">
                                    <FaBug /> Debugging Capabilities
                                </h3>
                                <ul className="space-y-3 text-sm text-slate-400">
                                    <li className="flex gap-2">
                                        <span className="text-primary-400">01.</span>
                                        <span><strong>Time Travel:</strong> Scrub through the agent&apos;s execution history step-by-step.</span>
                                    </li>
                                    <li className="flex gap-2">
                                        <span className="text-primary-400">02.</span>
                                        <span><strong>State Inspection:</strong> View the exact memory, context window, and tool outputs at any point in time.</span>
                                    </li>
                                    <li className="flex gap-2">
                                        <span className="text-primary-400">03.</span>
                                        <span><strong>Counterfactuals:</strong> Fork the session at any step to test &quot;what if&quot; scenarios.</span>
                                    </li>
                                </ul>
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
