'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { FaArrowLeft, FaFire, FaRobot, FaBrain, FaChartLine, FaDownload, FaPlay, FaStop, FaMicrochip } from 'react-icons/fa';
import { motion } from 'framer-motion';

// Mock Training Data
const MAX_STEPS = 500;
const INITIAL_LOSS = 2.4;

export default function ForgePage() {
    const [isTraining, setIsTraining] = useState(false);
    const [step, setStep] = useState(0);
    const [loss, setLoss] = useState(INITIAL_LOSS);
    const [isComplete, setIsComplete] = useState(false);

    useEffect(() => {
        let interval: NodeJS.Timeout;
        if (isTraining && step < MAX_STEPS) {
            interval = setInterval(() => {
                setStep(prev => {
                    const next = prev + 5;
                    // Simulate loss curve (1/x decay)
                    const progress = next / MAX_STEPS;
                    const newLoss = Math.max(0.1, INITIAL_LOSS - (INITIAL_LOSS * 0.9 * Math.sqrt(progress)) + (Math.random() * 0.05));
                    setLoss(newLoss);

                    if (next >= MAX_STEPS) {
                        setIsTraining(false);
                        setIsComplete(true);
                        return MAX_STEPS;
                    }
                    return next;
                });
            }, 100);
        }
        return () => clearInterval(interval);
    }, [isTraining, step]);

    const startTraining = () => {
        setStep(0);
        setLoss(INITIAL_LOSS);
        setIsComplete(false);
        setIsTraining(true);
    };

    const progressPercent = (step / MAX_STEPS) * 100;

    return (
        <div className="flex h-screen bg-black text-slate-100 font-sans overflow-hidden selection:bg-red-500/30">
            {/* Background Effects */}
            <div className="absolute inset-0 bg-[url('/grid.svg')] opacity-20 pointer-events-none" />
            <div className="absolute top-0 right-0 w-[600px] h-[600px] bg-forge-600/10 rounded-full blur-[120px] pointer-events-none" />

            {/* Sidebar */}
            <div className="w-80 h-full border-r border-white/5 flex flex-col glass z-10 bg-black/40">
                <div className="p-6 border-b border-white/5">
                    <Link href="/" className="flex items-center gap-2 text-slate-400 hover:text-white transition-colors mb-4 text-xs">
                        <FaArrowLeft /> Back to OS
                    </Link>
                    <h1 className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-forge-400 to-red-500 font-mono flex items-center gap-2">
                        <FaFire className="text-forge-500" />
                        FORGE
                    </h1>
                    <p className="text-xs text-slate-500 mt-1 uppercase tracking-widest">Model Distillation</p>
                </div>

                <div className="p-6 space-y-6">
                    <div className="group">
                        <div className="text-xs font-bold text-slate-500 uppercase mb-3">Active Job</div>
                        <div className="p-4 rounded-xl bg-white/5 border border-white/10 relative overflow-hidden">
                            <div className="flex items-center justify-between mb-2">
                                <span className="text-white font-bold">Llama-3-8B-Optimized</span>
                                <span className={`w-2 h-2 rounded-full ${isTraining ? 'bg-green-500 animate-pulse' : 'bg-slate-500'}`} />
                            </div>
                            <div className="text-xs text-slate-400 mb-4">Fine-tuning on &quot;Golden Traces v2&quot;</div>

                            {/* Mini Progress */}
                            <div className="w-full h-1 bg-white/10 rounded-full overflow-hidden">
                                <motion.div
                                    className="h-full bg-forge-500"
                                    style={{ width: `${progressPercent}%` }}
                                />
                            </div>
                        </div>
                    </div>

                    <div>
                        <div className="text-xs font-bold text-slate-500 uppercase mb-3">Hardware</div>
                        <div className="grid grid-cols-2 gap-2">
                            <div className="p-3 rounded bg-white/5 border border-white/10 text-center">
                                <FaMicrochip className="mx-auto mb-1 text-slate-400" />
                                <div className="text-xs text-slate-300">A100 x 4</div>
                            </div>
                            <div className="p-3 rounded bg-white/5 border border-white/10 text-center">
                                <div className="text-xs font-mono text-green-400 mb-1">98%</div>
                                <div className="text-xs text-slate-300">GPU Util</div>
                            </div>
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
                            Dataset: <span className="text-forge-300">Golden_Pro_V1.jsonl (150MB)</span>
                        </div>
                    </div>

                    {/* Simulation Mode Badge */}
                    <div className="flex items-center gap-2 px-3 py-1 rounded bg-yellow-500/10 border border-yellow-500/30 text-yellow-500 group relative cursor-help text-xs">
                        <div className="w-1.5 h-1.5 rounded-full bg-yellow-500 animate-pulse" />
                        SIMULATION MODE

                        {/* Tooltip */}
                        <div className="absolute top-full right-0 mt-2 w-72 p-3 bg-black border border-white/20 rounded shadow-xl opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-50">
                            <p className="text-slate-400 mb-2">Training job mocked.</p>
                            <div className="p-2 bg-white/5 rounded border border-white/10 font-mono text-[10px] text-cyan-300 break-all mb-1">
                                POST /api/v1/forge/jobs
                            </div>
                            <div className="p-2 bg-white/5 rounded border border-white/10 font-mono text-[10px] text-slate-400 break-all">
                                {`{ "teacher": "gpt-4", "student": "llama-3" }`}
                            </div>
                            <p className="text-slate-500 mt-2 font-normal">Connects to &apos;ForgeService&apos; and Torch.</p>
                        </div>
                    </div>
                </header>

                <div className="flex-1 p-8 flex flex-col justify-center items-center relative overflow-hidden">

                    {/* Pipeline Visual */}
                    <div className="w-full max-w-4xl flex items-center justify-between mb-16 relative">
                        {/* Connecting Pipe */}
                        <div className="absolute top-1/2 left-0 w-full h-2 bg-white/5 -z-10 rounded-full overflow-hidden">
                            {isTraining && (
                                <motion.div
                                    className="h-full bg-gradient-to-r from-transparent via-forge-500 to-transparent w-1/3 opacity-50"
                                    animate={{ x: ['0%', '300%'] }}
                                    transition={{ duration: 1.5, repeat: Infinity, ease: "linear" }}
                                />
                            )}
                        </div>

                        {/* Teacher */}
                        <div className="flex flex-col items-center gap-4">
                            <div className="w-24 h-24 rounded-2xl bg-gradient-to-br from-indigo-600 to-purple-600 flex items-center justify-center shadow-[0_0_30px_rgba(79,70,229,0.3)] relative">
                                <FaBrain className="text-4xl text-white" />
                                <div className="absolute -bottom-2 px-2 py-0.5 bg-black border border-purple-500 text-[10px] rounded text-purple-300">GPT-4</div>
                            </div>
                            <div className="text-center">
                                <div className="text-lg font-bold">Teacher</div>
                                <div className="text-xs text-slate-500">1.76T Params</div>
                            </div>
                        </div>

                        {/* Distillation Process */}
                        <div className="flex flex-col items-center">
                            <div className="mb-2 text-forge-400 font-mono text-sm tracking-widest">KNOWLEDGE TRANSFER</div>
                            {isTraining ? (
                                <div className="px-4 py-1 rounded-full bg-forge-500/10 border border-forge-500/20 text-forge-400 text-xs animate-pulse">
                                    Step {step} / {MAX_STEPS}
                                </div>
                            ) : isComplete ? (
                                <div className="px-4 py-1 rounded-full bg-green-500/10 border border-green-500/20 text-green-400 text-xs">
                                    Completed
                                </div>
                            ) : (
                                <div className="px-4 py-1 rounded-full bg-slate-500/10 border border-slate-500/20 text-slate-400 text-xs">
                                    Ready
                                </div>
                            )}
                        </div>

                        {/* Student */}
                        <div className="flex flex-col items-center gap-4">
                            <div className="w-24 h-24 rounded-2xl bg-gradient-to-br from-slate-800 to-slate-900 border border-white/10 flex items-center justify-center relative">
                                <FaRobot className={`text-4xl transition-colors ${isComplete ? 'text-forge-400' : 'text-slate-500'}`} />
                                <div className="absolute -bottom-2 px-2 py-0.5 bg-black border border-slate-500 text-[10px] rounded text-slate-300">Llama 3</div>
                            </div>
                            <div className="text-center">
                                <div className="text-lg font-bold">Student</div>
                                <div className="text-xs text-slate-500">8B Params</div>
                            </div>
                        </div>
                    </div>

                    {/* Stats Dashboard */}
                    <div className="grid grid-cols-3 gap-6 w-full max-w-4xl">
                        {/* Loss Chart */}
                        <div className="col-span-2 bg-black/40 glass border border-white/5 rounded-2xl p-6 relative overflow-hidden">
                            <div className="flex items-center justify-between mb-4">
                                <h3 className="flex items-center gap-2 text-slate-300 font-bold">
                                    <FaChartLine className="text-forge-500" /> Training Loss
                                </h3>
                                <span className="font-mono text-forge-400">{loss.toFixed(4)}</span>
                            </div>
                            <div className="h-32 flex items-end gap-1">
                                {Array.from({ length: 40 }).map((_, i) => (
                                    <div
                                        key={i}
                                        className="flex-1 bg-forge-500/20 rounded-t-sm transition-all duration-300"
                                        style={{
                                            height: `${Math.max(5, (isTraining || isComplete) ? (100 / (i + 1)) * (loss * 10) : 5)}%`,
                                            opacity: (i / 40) < (step / MAX_STEPS) ? 1 : 0.3
                                        }}
                                    />
                                ))}
                            </div>
                        </div>

                        {/* Actions & Metrics */}
                        <div className="space-y-6">
                            <div className="bg-black/40 glass border border-white/5 rounded-2xl p-6">
                                <div className="text-xs text-slate-500 uppercase mb-1">Projected Savings</div>
                                <div className="text-4xl font-bold text-white mb-1">$450<span className="text-lg text-slate-500 font-normal">/mo</span></div>
                                <div className="text-xs text-green-400">92% cheaper than GPT-4</div>
                            </div>

                            {!isTraining && !isComplete && (
                                <button
                                    onClick={startTraining}
                                    className="w-full py-4 rounded-xl bg-forge-600 hover:bg-forge-500 text-white font-bold flex items-center justify-center gap-2 shadow-lg shadow-forge-600/20 transition-all hover:scale-105"
                                >
                                    <FaPlay /> Start Distillation
                                </button>
                            )}

                            {isTraining && (
                                <button
                                    onClick={() => setIsTraining(false)}
                                    className="w-full py-4 rounded-xl bg-red-500/10 border border-red-500/50 text-red-400 font-bold flex items-center justify-center gap-2"
                                >
                                    <FaStop /> Stop Job
                                </button>
                            )}

                            {isComplete && (
                                <button
                                    className="w-full py-4 rounded-xl bg-green-600 hover:bg-green-500 text-white font-bold flex items-center justify-center gap-2 animate-bounce-short"
                                >
                                    <FaDownload /> Deploy Model
                                </button>
                            )}
                        </div>
                    </div>

                </div>
            </div>
        </div>
    );
}
