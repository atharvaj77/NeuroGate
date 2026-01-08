'use client';

import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FaPlay, FaRobot, FaShieldAlt, FaSearch, FaBug, FaCog } from 'react-icons/fa';

// Mock Scenarios for Simulation
const MOCK_SCENARIOS = {
    pii: {
        prompt: "Book a flight for Sarah (sarah.doe@example.com) using card 4444-5555-6666-7777 expiration 12/26.",
        response: "I have booked the flight for sarah.doe@example.com using card 4444-5555-6666-7777. The confirmation has been sent to your secure inbox.",
        tokenized: "I have booked the flight for <EMAIL_1> using card <CREDIT_CARD_1>. The confirmation has been sent to your secure inbox.",
        analysis: {
            route: "neuroguard-secure",
            latency: 45,
            pii: 2,
            cost: 0.00012,
            log: "Detected EMAIL at index 23. Detected CREDIT_CARD at index 65. Replaced with reversible tokens."
        }
    },
    rag: {
        prompt: "What is the policy on remote work according to the employee handbook?",
        response: "According to the '2025 Employee Handbook' (Doc ID: 882), remote work is permitted for all Engineering and Product roles up to 4 days a week. Please ensure you are connected to the VPN.",
        analysis: {
            route: "nexus-rag-gateway",
            latency: 120,
            pii: 0,
            cost: 0.00045,
            log: "Retrieved 3 chunks from Qdrant: 'remote_policy_v2', 'engineering_guidelines', 'vpn_access'. Semantic Score: 0.92."
        }
    },
    hallucination: {
        prompt: "Use the 'weather_check' tool to find the temperature in Mars, PA.",
        response: "Calling tool: get_current_weather(location='Mars, PA', unit='fahrenheit').\n\nThe current temperature in Mars, Pennsylvania is 42°F with partly cloudy skies.",
        analysis: {
            route: "cortex-eval",
            latency: 350,
            pii: 0,
            cost: 0.00030,
            log: "Self-Correction triggered. Model initially tried 'check_weather' (invalid). Cortex auto-corrected to 'get_current_weather' (valid) based on OpenAPI spec."
        }
    },
    consensus: {
        prompt: "Explain the medical implications of CRISPR-Cas9 for treating sickle cell anemia.",
        response: "CRISPR-Cas9 offers a potential cure for sickle cell disease by editing the BCL11A gene to reactivate fetal hemoglobin production.\n\n[Consensus verified by GPT-4 and Claude 3 Opus]",
        analysis: {
            route: "hive-mind-consensus",
            latency: 2100, // Slower due to multiple checks
            pii: 0,
            cost: 0.01200,
            log: "Consensus Protocol: triggered for high-stakes medical query. GPT-4 (0.98), Claude (0.97), Gemini (0.95). Result synthesized."
        }
    },
    cache: {
        prompt: "Write a Python script to calculate Fibonacci numbers recursively.",
        response: "Here is a recursive Python function for Fibonacci sequence:\n\ndef fib(n):\n    if n <= 1:\n        return n\n    return fib(n-1) + fib(n-2)",
        analysis: {
            route: "iron-gate-cache-L3",
            latency: 4, // Ultra fast
            pii: 0,
            cost: 0.00000,
            log: "⚡️ Cache Hit! Semantic match found (Score: 0.99). Served instantly from Redis/Qdrant."
        }
    }
};

export default function Playground() {
    const [prompt, setPrompt] = useState('');
    const [displayedResponse, setDisplayedResponse] = useState('');
    const [analysisData, setAnalysisData] = useState<any>(null);
    const [loading, setLoading] = useState(false);
    const [isSimulating, setIsSimulating] = useState(false);
    const [showSettings, setShowSettings] = useState(false);

    // Simulate Streaming Effect
    const streamResponse = async (fullText: string, data: any) => {
        setLoading(false);
        setIsSimulating(true);
        setAnalysisData(data); // Show metadata immediately
        setDisplayedResponse("");

        const chars = fullText.split("");
        for (let i = 0; i < chars.length; i++) {
            setDisplayedResponse(prev => prev + chars[i]);
            // Random delay between 10ms and 30ms to simulate token generation
            await new Promise(r => setTimeout(r, Math.random() * 20 + 10));
        }
        setIsSimulating(false);
    };

    const handleRun = async () => {
        setLoading(true);
        setDisplayedResponse("");
        setAnalysisData(null);

        // Artificial network delay
        await new Promise(r => setTimeout(r, 800));

        // Default "Generic" simulation if no specific scenario matches
        const genericResponse = "This is a simulated response running in the browser. In a production deployment, this would be streamed from the NeuroGate Kernel.";
        const genericData = {
            route: "simulator-v1",
            latency: 22,
            pii: 0,
            cost: 0.00000,
            log: "Running in client-side simulation mode."
        };

        // Check for scenario matches (simple heuristic)
        if (prompt.includes("sarah.doe")) {
            await streamResponse(MOCK_SCENARIOS.pii.tokenized, MOCK_SCENARIOS.pii.analysis);
            await new Promise(r => setTimeout(r, 600));
            setDisplayedResponse(MOCK_SCENARIOS.pii.response);
            setAnalysisData((prev: any) => ({
                ...prev,
                log: prev.log + "\n✅ Vault: Reversible tokens decrypted and restored."
            }));

        } else if (prompt.includes("remote work")) {
            await streamResponse(MOCK_SCENARIOS.rag.response, MOCK_SCENARIOS.rag.analysis);
        } else if (prompt.includes("weather")) {
            await streamResponse(MOCK_SCENARIOS.hallucination.response, MOCK_SCENARIOS.hallucination.analysis);
        } else if (prompt.includes("CRISPR")) {
            await streamResponse(MOCK_SCENARIOS.consensus.response, MOCK_SCENARIOS.consensus.analysis);
        } else if (prompt.includes("Fibonacci")) {
            await streamResponse(MOCK_SCENARIOS.cache.response, MOCK_SCENARIOS.cache.analysis);
        } else {
            await streamResponse(genericResponse, genericData);
        }
    };

    const loadScenario = (key: keyof typeof MOCK_SCENARIOS) => {
        setPrompt(MOCK_SCENARIOS[key].prompt);
        setDisplayedResponse("");
        setAnalysisData(null);
    };

    return (
        <main className="min-h-screen bg-black text-slate-100 p-8 pt-24 selection:bg-primary-500/30 overflow-hidden relative">
            {/* Background */}
            <div className="absolute inset-0 bg-[url('/grid.svg')] opacity-20 pointer-events-none" />

            <div className="max-w-7xl mx-auto space-y-8 relative z-10">

                {/* Header */}
                <header className="flex justify-between items-center">
                    <div>
                        <h1 className="text-4xl font-bold font-mono text-transparent bg-clip-text bg-gradient-to-r from-primary-400 to-accent-400 mb-2">
                            NeuroGate Terminal
                        </h1>
                        <p className="text-slate-400 font-mono text-sm">{'>'} Initialize simulation sequence...</p>
                    </div>
                    <div className="flex items-center gap-4">
                        <div className="glass px-4 py-2 rounded-full text-primary-300 text-sm flex items-center gap-2 border border-primary-500/30">
                            <span className="w-2 h-2 rounded-full bg-primary-400 animate-pulse-glow"></span>
                            <span className="font-mono">SIMULATION_MODE</span>
                        </div>
                        <button onClick={() => setShowSettings(!showSettings)} className="text-slate-400 hover:text-white transition-colors">
                            <FaCog />
                        </button>
                    </div>
                </header>

                {showSettings && (
                    <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} className="glass p-4 rounded-lg border border-yellow-500/30 bg-yellow-900/10 mb-4">
                        <h3 className="text-yellow-400 font-bold text-sm mb-1 font-mono">⚠️ SANDBOX ENVIRONMENT</h3>
                        <p className="text-xs text-yellow-200 font-mono">
                            Requests are simulated. No live LLM costs incurred.
                        </p>
                    </motion.div>
                )}

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 h-[600px]">

                    {/* Left Panel: Input */}
                    <section className="flex flex-col gap-4 h-full">
                        <div className="glass rounded-xl p-6 glow hover:shadow-lg transition-all border border-white/5 flex-1 flex flex-col">
                            <div className="flex justify-between items-center mb-4">
                                <h2 className="text-lg font-bold text-slate-200 font-mono flex items-center gap-2">
                                    <span className="text-primary-500">{'>'}</span> INPUT_BUFFER
                                </h2>
                                <div className="text-primary-400 text-xs font-mono border border-primary-500/30 px-2 py-1 rounded">MODEL: GPT-4-TURBO</div>
                            </div>

                            <textarea
                                value={prompt}
                                readOnly={true}
                                placeholder="Load a scenario to initialize buffer..."
                                className="w-full flex-1 bg-black/50 border border-white/10 rounded-lg p-4 text-slate-300 focus:outline-none resize-none font-mono text-sm transition-all cursor-default mb-4 focus:border-primary-500/50"
                            />

                            <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
                                <button onClick={() => loadScenario('pii')} className="glass-button hover:border-red-500/50 hover:bg-red-500/10 hover:text-red-300 flex items-center justify-center gap-2 group text-xs py-3">
                                    <FaShieldAlt className="group-hover:text-red-400" /> PII ATTACK
                                </button>
                                <button onClick={() => loadScenario('rag')} className="glass-button hover:border-blue-500/50 hover:bg-blue-500/10 hover:text-blue-300 flex items-center justify-center gap-2 group text-xs py-3">
                                    <FaSearch className="group-hover:text-blue-400" /> RAG RETRIEVAL
                                </button>
                                <button onClick={() => loadScenario('hallucination')} className="glass-button hover:border-purple-500/50 hover:bg-purple-500/10 hover:text-purple-300 flex items-center justify-center gap-2 group text-xs py-3">
                                    <FaBug className="group-hover:text-purple-400" /> SELF_CORRECT
                                </button>
                                <button onClick={() => loadScenario('consensus')} className="glass-button hover:border-amber-500/50 hover:bg-amber-500/10 hover:text-amber-300 flex items-center justify-center gap-2 group text-xs py-3">
                                    <FaRobot className="group-hover:text-amber-400" /> HIVE_MIND
                                </button>
                                <button onClick={() => loadScenario('cache')} className="glass-button hover:border-emerald-500/50 hover:bg-emerald-500/10 hover:text-emerald-300 flex items-center justify-center gap-2 group text-xs py-3 col-span-2 md:col-span-1">
                                    <span className="group-hover:text-emerald-400">⚡️</span> CACHE_HIT
                                </button>
                            </div>
                        </div>

                        <button
                            onClick={handleRun}
                            disabled={loading || isSimulating || !prompt}
                            className={`w-full py-4 rounded-xl font-bold font-mono tracking-wider transition-all flex items-center justify-center space-x-2 border border-transparent
                                ${loading || isSimulating || !prompt
                                    ? 'bg-slate-900 text-slate-600 cursor-not-allowed border-slate-800'
                                    : 'bg-primary-600 hover:bg-primary-500 text-white shadow-[0_0_20px_rgba(20,184,166,0.3)] hover:shadow-[0_0_30px_rgba(20,184,166,0.5)] border-primary-400/20'
                                }`}
                        >
                            {loading ? (
                                <>
                                    <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin mr-2" />
                                    EXECUTING...
                                </>
                            ) : (
                                <>
                                    <FaPlay className="text-xs mr-2" />
                                    EXECUTE SEQUENCE
                                </>
                            )}
                        </button>
                    </section>

                    {/* Right Panel: Output */}
                    <section className="h-full">
                        <div className={`glass rounded-xl p-6 h-full flex flex-col relative overflow-hidden transition-all border border-white/5`}>
                            <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-primary-500 to-accent-500 opacity-50"></div>
                            <h2 className="text-lg font-bold text-slate-200 font-mono flex items-center gap-2">
                                <span className="text-accent-500">{'>'}</span> KERNEL_OUTPUT_STREAM
                            </h2>

                            <div className="flex-1 bg-black/60 rounded-lg p-4 font-mono text-sm relative overflow-y-auto custom-scrollbar border border-white/5 shadow-inner">
                                <AnimatePresence mode='wait'>
                                    {!displayedResponse && !loading && !analysisData && (
                                        <motion.div
                                            initial={{ opacity: 0 }}
                                            animate={{ opacity: 1 }}
                                            className="flex flex-col items-center justify-center h-full text-slate-700"
                                        >
                                            <div className="text-4xl mb-4 opacity-20 animate-pulse">_</div>
                                            <p className="text-xs tracking-widest uppercase">Awaiting Input Signal</p>
                                        </motion.div>
                                    )}

                                    {(displayedResponse || loading || analysisData) && (
                                        <motion.div
                                            initial={{ opacity: 0, y: 10 }}
                                            animate={{ opacity: 1, y: 0 }}
                                            className="space-y-6"
                                        >
                                            {/* Kernel Metadata Header */}
                                            {analysisData && (
                                                <div className="grid grid-cols-3 gap-4 border-b border-slate-800 pb-4">
                                                    <div>
                                                        <div className="text-[10px] text-slate-500 uppercase tracking-wider mb-1">Route</div>
                                                        <div className="text-accent-400 font-bold text-xs truncate">{analysisData.route}</div>
                                                    </div>
                                                    <div>
                                                        <div className="text-[10px] text-slate-500 uppercase tracking-wider mb-1">Latency</div>
                                                        <div className="text-primary-400 text-xs">{analysisData.latency}ms</div>
                                                    </div>
                                                    <div>
                                                        <div className="text-[10px] text-slate-500 uppercase tracking-wider mb-1">Pipeline</div>
                                                        <div className="text-green-500 text-xs">OPTIMIZED</div>
                                                    </div>
                                                </div>
                                            )}

                                            {/* Security Log */}
                                            {analysisData?.pii > 0 && (
                                                <motion.div initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} className="bg-red-500/10 border-l-2 border-red-500 p-3">
                                                    <div className="text-red-400 text-[10px] font-bold uppercase tracking-wider mb-1 flex items-center gap-2">
                                                        <FaShieldAlt /> Security Intervention
                                                    </div>
                                                    <p className="text-slate-400 text-xs">
                                                        {analysisData.log}
                                                    </p>
                                                </motion.div>
                                            )}

                                            {/* System Log */}
                                            {analysisData?.log && analysisData?.pii === 0 && (
                                                <motion.div initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} className="bg-blue-500/10 border-l-2 border-blue-500 p-3">
                                                    <div className="text-blue-400 text-[10px] font-bold uppercase tracking-wider mb-1 flex items-center gap-2">
                                                        <FaCog /> Kernel Trace
                                                    </div>
                                                    <p className="text-slate-400 text-xs">
                                                        {analysisData.log}
                                                    </p>
                                                </motion.div>
                                            )}

                                            {/* Content */}
                                            <div>
                                                <div className="text-[10px] text-slate-500 uppercase tracking-wider mb-2">Payload</div>
                                                <div className="text-slate-300 leading-relaxed whitespace-pre-wrap">
                                                    {displayedResponse}
                                                    {isSimulating && <span className="inline-block w-2 h-4 bg-primary-400 ml-1 animate-pulse" />}
                                                </div>
                                            </div>

                                        </motion.div>
                                    )}
                                </AnimatePresence>
                            </div>

                            {/* Debug Footer */}
                            {analysisData && (
                                <div className="mt-4 pt-4 border-t border-white/5 flex justify-between text-[10px] text-slate-500 font-mono uppercase tracking-wider">
                                    <div>ID: {Math.random().toString(36).substring(7).toUpperCase()}</div>
                                    <div>Cost: ${analysisData.cost.toFixed(5)}</div>
                                </div>
                            )}
                        </div>
                    </section>
                </div>
            </div>
        </main>
    );
}
