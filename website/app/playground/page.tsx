"use client";

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
        // In a real app, this would call the API.
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
            // PII Scenario with Detokenization
            await streamResponse(MOCK_SCENARIOS.pii.tokenized, MOCK_SCENARIOS.pii.analysis);

            // Simulate Vault Lookup Delay
            await new Promise(r => setTimeout(r, 600));

            // "Decrypt" / Restore
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
        <main className="min-h-screen bg-slate-950 text-slate-100 p-8 pt-24 grid-background selection:bg-primary-500/30">
            <div className="max-w-6xl mx-auto space-y-8">

                {/* Header */}
                <header className="flex justify-between items-center">
                    <div>
                        <h1 className="text-4xl font-bold gradient-text mb-2">NeuroGate Playground</h1>
                        <p className="text-slate-400">Test kernel modules in a simulated environment.</p>
                    </div>
                    <div className="flex items-center gap-4">
                        <div className="glass px-4 py-2 rounded-full text-slate-300 text-sm flex items-center gap-2">
                            <span className="w-2 h-2 rounded-full bg-amber-400 animate-pulse"></span>
                            <span>Simulation Mode</span>
                        </div>
                        <button onClick={() => setShowSettings(!showSettings)} className="text-slate-400 hover:text-white transition-colors">
                            <FaCog />
                        </button>
                    </div>
                </header>

                {showSettings && (
                    <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} className="glass p-4 rounded-lg border border-yellow-500/30 bg-yellow-900/10 mb-4">
                        <h3 className="text-yellow-400 font-bold text-sm mb-1">Interactive Verification</h3>
                        <p className="text-xs text-yellow-200">
                            Simulated mode is active to prevent API costs. To run live requests against your own NeuroGate instance, you would configure the endpoint here.
                        </p>
                    </motion.div>
                )}

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">

                    {/* Left Panel: Input */}
                    <section className="space-y-6">
                        <div className="glass rounded-xl p-6 glow hover:shadow-lg transition-all border border-white/5">
                            <div className="flex justify-between items-center mb-4">
                                <h2 className="text-xl font-semibold text-slate-200">Input Prompt</h2>
                                <div className="text-slate-500 text-xs font-mono">Model: gpt-4-turbo</div>
                            </div>

                            <textarea
                                value={prompt}
                                readOnly={true}
                                placeholder="Select a scenario below to load a prompt..."
                                className="w-full h-64 bg-slate-900/50 border border-slate-700/50 rounded-lg p-4 text-slate-400 focus:outline-none resize-none font-mono text-sm transition-all cursor-default"
                            />

                            <div className="mt-4 flex gap-3 overflow-x-auto pb-4 scrollbar-thin scrollbar-thumb-slate-700">
                                <button onClick={() => loadScenario('pii')} className="glass-button hover:border-red-500/50 hover:bg-red-500/10 hover:text-red-300 flex items-center gap-2 whitespace-nowrap group">
                                    <FaShieldAlt className="group-hover:text-red-400 transition-colors" /> PII Attack
                                </button>
                                <button onClick={() => loadScenario('rag')} className="glass-button hover:border-blue-500/50 hover:bg-blue-500/10 hover:text-blue-300 flex items-center gap-2 whitespace-nowrap group">
                                    <FaSearch className="group-hover:text-blue-400 transition-colors" /> RAG Retrieval
                                </button>
                                <button onClick={() => loadScenario('hallucination')} className="glass-button hover:border-purple-500/50 hover:bg-purple-500/10 hover:text-purple-300 flex items-center gap-2 whitespace-nowrap group">
                                    <FaBug className="group-hover:text-purple-400 transition-colors" /> Auto-Correct
                                </button>
                                <button onClick={() => loadScenario('consensus')} className="glass-button hover:border-amber-500/50 hover:bg-amber-500/10 hover:text-amber-300 flex items-center gap-2 whitespace-nowrap group">
                                    <FaRobot className="group-hover:text-amber-400 transition-colors" /> Hive Mind
                                </button>
                                <button onClick={() => loadScenario('cache')} className="glass-button hover:border-emerald-500/50 hover:bg-emerald-500/10 hover:text-emerald-300 flex items-center gap-2 whitespace-nowrap group">
                                    <span className="group-hover:text-emerald-400 transition-colors">⚡️</span> Cache Hit
                                </button>
                            </div>

                            <div className="mt-6">
                                <button
                                    onClick={handleRun}
                                    disabled={loading || isSimulating || !prompt}
                                    className={`w-full py-3 rounded-lg font-semibold transition-all flex items-center justify-center space-x-2
                    ${loading || isSimulating || !prompt
                                            ? 'bg-slate-800 text-slate-500 cursor-not-allowed'
                                            : 'bg-gradient-to-r from-primary-600 to-accent-600 hover:from-primary-500 hover:to-accent-500 text-white shadow-lg shadow-primary-500/20'
                                        }`}
                                >
                                    {loading ? (
                                        <>
                                            <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin mr-2" />
                                            Processing...
                                        </>
                                    ) : (
                                        <>
                                            <FaPlay className="text-xs mr-2" />
                                            Run Request
                                        </>
                                    )}
                                </button>
                            </div>
                        </div>
                    </section>

                    {/* Right Panel: Output */}
                    <section className="space-y-6">
                        <div className={`glass rounded-xl p-6 h-full min-h-[500px] flex flex-col relative overflow-hidden transition-all border border-white/5`}>
                            <h2 className="text-xl font-semibold text-slate-200 mb-4 flex items-center gap-2">
                                <FaRobot className="text-slate-400" />
                                Response & Kernel Analysis
                            </h2>

                            <div className="flex-1 bg-slate-900/50 rounded-lg p-4 font-mono text-sm relative overflow-y-auto custom-scrollbar">
                                <AnimatePresence mode='wait'>
                                    {!displayedResponse && !loading && !analysisData && (
                                        <motion.div
                                            initial={{ opacity: 0 }}
                                            animate={{ opacity: 1 }}
                                            className="flex flex-col items-center justify-center h-full text-slate-600"
                                        >
                                            <div className="text-4xl mb-4 opacity-20">⚡️</div>
                                            <p>Ready to simulate</p>
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
                                                        <div className="text-xs text-slate-500 uppercase tracking-wider mb-1">Route</div>
                                                        <div className="text-accent-400 font-bold text-xs md:text-sm truncate">{analysisData.route}</div>
                                                    </div>
                                                    <div>
                                                        <div className="text-xs text-slate-500 uppercase tracking-wider mb-1">Latency</div>
                                                        <div className="text-slate-300 text-xs md:text-sm">{analysisData.latency}ms</div>
                                                    </div>
                                                    <div>
                                                        <div className="text-xs text-slate-500 uppercase tracking-wider mb-1">Pipeline</div>
                                                        <div className="text-green-400 text-xs md:text-sm">OPTIMIZED</div>
                                                    </div>
                                                </div>
                                            )}

                                            {/* Security Log */}
                                            {analysisData?.pii > 0 && (
                                                <motion.div initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} className="bg-red-500/10 border border-red-500/20 rounded p-3">
                                                    <div className="text-red-400 text-xs font-bold uppercase tracking-wider mb-1 flex items-center gap-2">
                                                        <FaShieldAlt /> Security Intervention
                                                    </div>
                                                    <p className="text-slate-400 text-xs font-mono">
                                                        {analysisData.log}
                                                    </p>
                                                </motion.div>
                                            )}

                                            {/* System Log */}
                                            {analysisData?.log && analysisData?.pii === 0 && (
                                                <motion.div initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} className="bg-blue-500/10 border border-blue-500/20 rounded p-3">
                                                    <div className="text-blue-400 text-xs font-bold uppercase tracking-wider mb-1 flex items-center gap-2">
                                                        <FaSearch /> Kernel Trace
                                                    </div>
                                                    <p className="text-slate-400 text-xs font-mono">
                                                        {analysisData.log}
                                                    </p>
                                                </motion.div>
                                            )}

                                            {/* Content */}
                                            <div>
                                                <div className="text-xs text-slate-500 uppercase tracking-wider mb-2">Streaming Output</div>
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
                                <div className="mt-4 pt-4 border-t border-slate-800 flex justify-between text-xs text-slate-500 font-mono">
                                    <div>Trace ID: {Math.random().toString(36).substring(7)}</div>
                                    <div>Est. Cost: ${analysisData.cost.toFixed(5)}</div>
                                </div>
                            )}
                        </div>
                    </section>
                </div>
            </div>
        </main>
    );
}
