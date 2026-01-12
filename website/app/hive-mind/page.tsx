'use client';

import React, { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { FaArrowLeft, FaBrain, FaRobot, FaCheckCircle, FaNetworkWired, FaBolt } from 'react-icons/fa';
import { motion, AnimatePresence } from 'framer-motion';
import { HiveMindClient } from './HiveMindClient';

// Mock Responses for Simulation
// Mock Responses for Simulation
const PROMPT_SCENARIOS = {
    default: {
        query: "Explain the CAP theorem in one sentence.",
        models: [
            { name: "GPT-4", color: "text-purple-400", bg: "from-purple-500/20", border: "border-purple-500/50", content: "The CAP theorem states that a distributed data store can only simultaneously provide two out of three guarantees: Consistency, Availability, and Partition tolerance." },
            { name: "Claude 3 Opus", color: "text-orange-400", bg: "from-orange-500/20", border: "border-orange-500/50", content: "In theoretical computer science, the CAP theorem asserts that any distributed system can deliver at most two of these three properties: consistency, availability, and partition tolerance." },
            { name: "Llama 3 70B", color: "text-blue-400", bg: "from-blue-500/20", border: "border-blue-500/50", content: "CAP theorem posits that distributed systems must choose between consistency, availability, and partition tolerance, as it is impossible to achieve all three simultaneously." }
        ],
        consensus: "The CAP theorem states that a distributed system can only guarantee two of the following three properties significantly: Consistency (all nodes see the same data), Availability (every request receives a response), and Partition Tolerance (system continues despite network failures)."
    }
};

const MODEL_STYLES = [
    { color: "text-purple-400", bg: "from-purple-500/20", border: "border-purple-500/50" },
    { color: "text-orange-400", bg: "from-orange-500/20", border: "border-orange-500/50" },
    { color: "text-blue-400", bg: "from-blue-500/20", border: "border-blue-500/50" }
];

export default function HiveMindPage() {
    const [query, setQuery] = useState(PROMPT_SCENARIOS.default.query);
    const [isSimulating, setIsSimulating] = useState(false);
    const [simulationMode, setSimulationMode] = useState(true);

    // Dynamic State
    const [activeModels, setActiveModels] = useState(PROMPT_SCENARIOS.default.models);
    const [streamedContent, setStreamedContent] = useState(['', '', '']);
    const [consensusContent, setConsensusContent] = useState('');
    const [showConsensus, setShowConsensus] = useState(false);
    const [finalConsensusText, setFinalConsensusText] = useState('');

    const runConsensus = async () => {
        setIsSimulating(true);
        setStreamedContent(['', '', '']);
        setConsensusContent('');
        setShowConsensus(false);

        if (simulationMode) {
            // --- SIMULATION MODE ---
            setActiveModels(PROMPT_SCENARIOS.default.models);
            setFinalConsensusText(PROMPT_SCENARIOS.default.consensus);

            // Simulate Streaming
            const modelIntervals: NodeJS.Timeout[] = [];
            PROMPT_SCENARIOS.default.models.forEach((model, index) => {
                let charIndex = 0;
                const interval = setInterval(() => {
                    setStreamedContent(prev => {
                        const next = [...prev];
                        next[index] = model.content.substring(0, charIndex + 1);
                        return next;
                    });
                    charIndex++;
                    if (charIndex >= model.content.length) clearInterval(interval);
                }, 30 + Math.random() * 20);
                modelIntervals.push(interval);
            });

            // Start Consensus after models finish
            setTimeout(() => {
                setShowConsensus(true);
                let charIndex = 0;
                const consensusInterval = setInterval(() => {
                    setConsensusContent(PROMPT_SCENARIOS.default.consensus.substring(0, charIndex + 1));
                    charIndex++;
                    if (charIndex >= PROMPT_SCENARIOS.default.consensus.length) {
                        clearInterval(consensusInterval);
                        setIsSimulating(false);
                    }
                }, 20);
            }, 3500);

        } else {
            // --- REAL BACKEND MODE ---
            try {
                // Call Backend
                const result = await HiveMindClient.runConsensus(query);

                if (result) {
                    // Map Real Responses to UI Models
                    const mappedModels = result.individualResponses.map((resp, i) => {
                        const style = MODEL_STYLES[i % MODEL_STYLES.length];
                        return {
                            name: resp.x_neurogate_route.toUpperCase(), // e.g. "OPENAI"
                            color: style.color,
                            bg: style.bg,
                            border: style.border,
                            content: resp.choices[0]?.message?.content || "No response"
                        };
                    });

                    setActiveModels(mappedModels);
                    setFinalConsensusText(result.synthesis);

                    // Fast "Replay" Animation for Real Data
                    const modelIntervals: NodeJS.Timeout[] = [];
                    mappedModels.forEach((model, index) => {
                        let charIndex = 0;
                        const interval = setInterval(() => {
                            setStreamedContent(prev => {
                                const next = [...prev];
                                // Ensure array size matches
                                while (next.length <= index) next.push('');
                                next[index] = model.content.substring(0, charIndex + 1);
                                return next;
                            });
                            charIndex += 5; // Faster typing for real data
                            if (charIndex >= model.content.length) {
                                setStreamedContent(prev => {
                                    const next = [...prev];
                                    next[index] = model.content;
                                    return next;
                                });
                                clearInterval(interval);
                            }
                        }, 20);
                        modelIntervals.push(interval);
                    });

                    // Show Consensus after delay
                    setTimeout(() => {
                        setShowConsensus(true);
                        setConsensusContent(result.synthesis);
                        setIsSimulating(false);
                    }, 2000); // 2s delay for "thinking"
                }
            } catch (err) {
                console.error(err);
                setIsSimulating(false);
            }
        }
    };

    return (
        <div className="flex h-screen bg-black text-slate-100 font-sans overflow-hidden selection:bg-purple-500/30">
            {/* Background Effects */}
            <div className="absolute inset-0 bg-[url('/grid.svg')] opacity-20 pointer-events-none" />

            {/* Sidebar */}
            <div className="w-80 h-full border-r border-white/5 flex flex-col glass z-10 bg-black/40">
                <div className="p-6 border-b border-white/5">
                    <Link href="/" className="flex items-center gap-2 text-slate-400 hover:text-white transition-colors mb-4 text-xs">
                        <FaArrowLeft /> Back to OS
                    </Link>
                    <h1 className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-violet-400 to-fuchsia-400 font-mono flex items-center gap-2">
                        <FaBrain className="text-violet-400" />
                        HIVE MIND
                    </h1>
                    <p className="text-xs text-slate-500 mt-1 uppercase tracking-widest">Consensus Engine</p>
                </div>

                <div className="p-6 space-y-6">
                    <div className="bg-white/5 rounded-xl p-4 border border-white/10">
                        <div className="text-xs text-slate-500 uppercase tracking-wider mb-2">Active Colony</div>
                        <div className="flex items-center gap-2 mb-2">
                            <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse"></div>
                            <span className="text-sm font-mono text-green-400">3 Models Online</span>
                        </div>
                        <div className="space-y-1 ml-4 border-l border-white/10 pl-3">
                            <div className="text-xs text-slate-400">GPT-4 Turbo</div>
                            <div className="text-xs text-slate-400">Claude 3 Opus</div>
                            <div className="text-xs text-slate-400">Llama 3 70B</div>
                        </div>
                    </div>

                    <div className="bg-white/5 rounded-xl p-4 border border-white/10">
                        <div className="text-xs text-slate-500 uppercase tracking-wider mb-2">Configuration</div>
                        <div className="flex justify-between items-center text-sm mb-1">
                            <span className="text-slate-400">Threshold</span>
                            <span className="text-violet-400 font-mono">0.85</span>
                        </div>
                        <div className="w-full bg-white/10 rounded-full h-1">
                            <div className="bg-violet-500 w-[85%] h-full rounded-full"></div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Main Content */}
            <div className="flex-1 flex flex-col h-full relative z-0">
                {/* Header */}
                <header className="h-16 border-b border-white/5 flex items-center justify-between px-8 glass bg-black/20">
                    <div className="flex items-center gap-4">
                        <div className="text-sm text-slate-400">
                            Strategy: <span className="text-violet-300">Weighted_Majority_Vote</span>
                        </div>
                    </div>

                    {/* Simulation Mode Toggle */}
                    <button
                        onClick={() => setSimulationMode(!simulationMode)}
                        className={`flex items-center gap-2 px-3 py-1.5 rounded-lg border text-xs font-mono transition-all ${simulationMode
                            ? 'bg-yellow-500/10 border-yellow-500/30 text-yellow-500 hover:bg-yellow-500/20'
                            : 'bg-green-500/10 border-green-500/30 text-green-500 hover:bg-green-500/20'
                            }`}
                    >
                        <div className={`w-2 h-2 rounded-full ${simulationMode ? 'bg-yellow-500 animate-pulse' : 'bg-green-500'}`} />
                        {simulationMode ? 'SIMULATION MODE' : 'REAL BACKEND'}
                    </button>
                </header>

                <div className="flex-1 p-8 flex flex-col overflow-hidden relative">

                    {/* Input Area */}
                    <div className="w-full max-w-4xl mx-auto mb-8 relative z-20">
                        <div className="relative group">
                            <div className="absolute inset-0 bg-violet-500/20 blur-xl opacity-0 group-focus-within:opacity-100 transition-opacity rounded-full"></div>
                            <input
                                type="text"
                                value={query}
                                onChange={(e) => setQuery(e.target.value)}
                                className="w-full bg-black/60 border border-white/10 rounded-2xl py-4 px-6 text-lg text-white placeholder-slate-500 focus:outline-none focus:border-violet-500/50 transition-all relative z-10 glass"
                            />
                            <button
                                onClick={!isSimulating ? runConsensus : undefined}
                                className={`absolute right-2 top-1/2 -translate-y-1/2 px-6 py-2 rounded-xl text-sm font-semibold transition-all z-20 flex items-center gap-2 ${isSimulating ? 'bg-slate-700 text-slate-400 cursor-not-allowed' : 'bg-violet-600 hover:bg-violet-500 text-white'}`}
                            >
                                <FaBolt /> {isSimulating ? 'Processing...' : 'Run Consensus'}
                            </button>
                        </div>
                    </div>

                    {/* Three Columns - Models */}
                    <div className="flex-1 grid grid-cols-3 gap-6 max-w-6xl mx-auto w-full mb-8 min-h-0">
                        {activeModels.map((model, i) => (
                            <div key={i} className="flex flex-col h-full">
                                <motion.div
                                    initial={{ opacity: 0.5, y: 10 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    transition={{ delay: i * 0.1 }}
                                    className={`flex items-center gap-3 p-3 rounded-t-xl bg-gradient-to-r ${model.bg} border-t border-l border-r ${model.border}`}
                                >
                                    <FaRobot className={model.color} />
                                    <span className={`font-bold font-mono ${model.color}`}>{model.name}</span>
                                </motion.div>
                                <div className={`flex-1 glass border-b border-l border-r border-white/10 rounded-b-xl p-4 font-mono text-sm text-slate-300 leading-relaxed overflow-y-auto relative ${isSimulating && streamedContent[i] && streamedContent[i].length < model.content.length ? 'animate-pulse-subtle' : ''}`}>
                                    {streamedContent[i]}
                                    {isSimulating && streamedContent[i] && streamedContent[i].length < model.content.length && (
                                        <span className="inline-block w-2 h-4 bg-violet-400 animate-pulse ml-1 align-middle" />
                                    )}
                                </div>
                                {/* Connecting Line Visual */}
                                <div className="h-8 w-px bg-gradient-to-b from-white/10 to-violet-500/50 mx-auto mt-2" />
                            </div>
                        ))}
                    </div>

                    {/* Consensus Core (Bottom) */}
                    <div className="max-w-4xl mx-auto w-full relative z-10">
                        <AnimatePresence>
                            <motion.div
                                initial={{ opacity: 0, scale: 0.9 }}
                                animate={{ opacity: 1, scale: 1 }}
                                className={`rounded-2xl border bg-black/80 p-1 relative overflow-hidden transition-colors duration-500 ${showConsensus ? 'border-violet-500 shadow-[0_0_50px_-10px_rgba(139,92,246,0.3)]' : 'border-white/10'}`}
                            >
                                {showConsensus && (
                                    <div className="absolute inset-0 bg-gradient-to-r from-violet-500/10 via-fuchsia-500/10 to-violet-500/10 animate-shimmer" />
                                )}

                                <div className="bg-black/90 rounded-xl p-6 relative">
                                    <div className="flex items-center gap-3 mb-4">
                                        <div className={`p-2 rounded-lg ${showConsensus ? 'bg-violet-500 text-white' : 'bg-slate-800 text-slate-500'} transition-colors`}>
                                            <FaNetworkWired />
                                        </div>
                                        <div>
                                            <div className="text-sm font-bold text-white uppercase tracking-wider">Final Truth Consensus</div>
                                            {showConsensus && <div className="text-xs text-violet-400">Confidence: 99.8%</div>}
                                        </div>
                                    </div>

                                    <div className="min-h-[4rem] text-slate-200 font-medium leading-relaxed">
                                        {consensusContent}
                                        {showConsensus && consensusContent.length < finalConsensusText.length && (
                                            <span className="inline-block w-2 h-4 bg-violet-400 animate-pulse ml-1 align-middle" />
                                        )}
                                        {!showConsensus && !isSimulating && (
                                            <span className="text-slate-600 italic">Waiting for input...</span>
                                        )}
                                        {!showConsensus && isSimulating && (
                                            <span className="text-violet-400 animate-pulse">Synthesizing perspectives...</span>
                                        )}
                                    </div>
                                </div>
                            </motion.div>
                        </AnimatePresence>
                    </div>

                </div>
            </div>
        </div>
    );
}
