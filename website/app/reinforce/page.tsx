'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { FaArrowLeft, FaUserEdit, FaCheck, FaTimes, FaPen, FaRobot, FaUser, FaSave } from 'react-icons/fa';
import { motion, AnimatePresence } from 'framer-motion';

// Mock Data for "Simulation"
const MOCK_QUEUE = [
    {
        id: 'trace_101',
        user: 'How do I optimize a SQL query?',
        agent: 'You can use `EXPLAIN ANALYZE` to check the query plan. Indexing columns used in WHERE clauses is also critical.',
        confidence: 0.88
    },
    {
        id: 'trace_102',
        user: 'Is the moon made of cheese?',
        agent: 'Yes, the moon is primarily composed of Wensleydale cheese, as discovered by Wallace in 1989.',
        confidence: 0.12,
        flagged: true
    },
    {
        id: 'trace_103',
        user: 'Write a python binary search.',
        agent: 'def binary_search(arr, x): ... (code snippet)',
        confidence: 0.95
    }
];

export default function ReinforcePage() {
    const [queue, setQueue] = useState(MOCK_QUEUE);
    const [history, setHistory] = useState<any[]>([]);
    const [isEditing, setIsEditing] = useState(false);
    const [editValue, setEditValue] = useState('');

    const currentCard = queue[0];

    // Card Animation Variants
    const [exitX, setExitX] = useState(0);

    const handleVote = (approved: boolean) => {
        if (!currentCard) return;

        setExitX(approved ? 200 : -200);

        setTimeout(() => {
            const processed = { ...currentCard, approved, edited: false };
            setHistory([processed, ...history]);
            setQueue(queue.slice(1));
            setExitX(0);
            setIsEditing(false);
        }, 300);
    };

    const handleSaveEdit = () => {
        if (!currentCard) return;

        // Treat save as an approval of the *new* version
        const processed = { ...currentCard, agent: editValue, approved: true, edited: true };
        setHistory([processed, ...history]);
        setQueue(queue.slice(1));
        setIsEditing(false);
    };

    const startEdit = () => {
        setEditValue(currentCard.agent);
        setIsEditing(true);
    };

    return (
        <div className="flex h-screen bg-black text-slate-100 font-sans overflow-hidden selection:bg-green-500/30">
            {/* Background Effects */}
            <div className="absolute inset-0 bg-[url('/grid.svg')] opacity-20 pointer-events-none" />
            <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-reinforce-500/10 rounded-full blur-[100px] pointer-events-none" />

            {/* Sidebar */}
            <div className="w-80 h-full border-r border-white/5 flex flex-col glass z-10 bg-black/40">
                <div className="p-6 border-b border-white/5">
                    <Link href="/" className="flex items-center gap-2 text-slate-400 hover:text-white transition-colors mb-4 text-xs">
                        <FaArrowLeft /> Back to OS
                    </Link>
                    <h1 className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-reinforce-400 to-emerald-400 font-mono flex items-center gap-2">
                        <FaUserEdit className="text-reinforce-400" />
                        REINFORCE
                    </h1>
                    <p className="text-xs text-slate-500 mt-1 uppercase tracking-widest">RLHF Studio</p>
                </div>

                <div className="p-6">
                    <div className="bg-white/5 rounded-xl p-4 border border-white/10 mb-6">
                        <div className="text-xs text-slate-500 uppercase tracking-wider mb-1">Queue Size</div>
                        <div className="text-3xl font-mono font-bold text-white">{queue.length}</div>
                    </div>

                    <div className="bg-white/5 rounded-xl p-4 border border-white/10">
                        <div className="text-xs text-slate-500 uppercase tracking-wider mb-1">Session stats</div>
                        <div className="space-y-2 mt-3">
                            <div className="flex justify-between text-sm">
                                <span className="text-slate-400">Approved</span>
                                <span className="text-green-400 font-mono">{history.filter(h => h.approved && !h.edited).length}</span>
                            </div>
                            <div className="flex justify-between text-sm">
                                <span className="text-slate-400">Rewritten</span>
                                <span className="text-yellow-400 font-mono">{history.filter(h => h.edited).length}</span>
                            </div>
                            <div className="flex justify-between text-sm">
                                <span className="text-slate-400">Rejected</span>
                                <span className="text-red-400 font-mono">{history.filter(h => !h.approved).length}</span>
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
                            Batch: <span className="text-reinforce-300">Default_Queue</span>
                        </div>
                    </div>

                    {/* Simulation Mode Badge */}
                    <div className="flex items-center gap-2 px-3 py-1 rounded bg-yellow-500/10 border border-yellow-500/30 text-yellow-500 group relative cursor-help text-xs">
                        <div className="w-1.5 h-1.5 rounded-full bg-yellow-500 animate-pulse" />
                        SIMULATION MODE

                        {/* Tooltip */}
                        <div className="absolute top-full right-0 mt-2 w-72 p-3 bg-black border border-white/20 rounded shadow-xl opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-50">
                            <p className="text-slate-400 mb-2">Feedback queue mocked.</p>
                            <div className="p-2 bg-white/5 rounded border border-white/10 font-mono text-[10px] text-cyan-300 break-all mb-1">
                                POST /api/v1/reinforce/feedback
                            </div>
                            <div className="p-2 bg-white/5 rounded border border-white/10 font-mono text-[10px] text-slate-400 break-all">
                                {`{ "traceId": "...", "score": 1.0 }`}
                            </div>
                            <p className="text-slate-500 mt-2 font-normal">Connects to &apos;FeedbackService&apos;.</p>
                        </div>
                    </div>
                </header>

                <div className="flex-1 flex flex-col items-center justify-center p-8 relative">
                    <AnimatePresence>
                        {currentCard ? (
                            <motion.div
                                key={currentCard.id}
                                initial={{ opacity: 0, scale: 0.9, y: 20 }}
                                animate={{
                                    opacity: 1,
                                    scale: 1,
                                    y: 0,
                                    x: exitX,
                                    rotate: exitX * 0.05
                                }}
                                exit={{ opacity: 0, scale: 0.9, transition: { duration: 0.2 } }}
                                transition={{ type: "spring", stiffness: 300, damping: 20 }}
                                className="w-full max-w-2xl bg-black/80 glass border border-white/10 rounded-2xl overflow-hidden shadow-2xl relative"
                            >
                                {/* Header Bar */}
                                <div className="h-2 bg-gradient-to-r from-reinforce-500 to-emerald-500" />

                                <div className="p-8">
                                    {/* User Query */}
                                    <div className="mb-8">
                                        <div className="flex items-center gap-2 mb-3 text-slate-500 text-xs font-bold uppercase tracking-widest">
                                            <FaUser /> User Query
                                        </div>
                                        <div className="text-lg text-white font-medium bg-white/5 p-4 rounded-xl border border-white/5">
                                            {currentCard.user}
                                        </div>
                                    </div>

                                    {/* Agent Response */}
                                    <div>
                                        <div className="flex items-center justify-between mb-3">
                                            <div className="flex items-center gap-2 text-reinforce-400 text-xs font-bold uppercase tracking-widest">
                                                <FaRobot /> Agent Response
                                                <span className="bg-reinforce-500/10 px-2 py-0.5 rounded text-[10px] border border-reinforce-500/20">
                                                    Confidence: {currentCard.confidence}
                                                </span>
                                            </div>
                                            {!isEditing && (
                                                <button
                                                    onClick={startEdit}
                                                    className="text-xs flex items-center gap-1 text-slate-400 hover:text-white transition-colors"
                                                >
                                                    <FaPen /> Edit
                                                </button>
                                            )}
                                        </div>

                                        {isEditing ? (
                                            <textarea
                                                value={editValue}
                                                onChange={(e) => setEditValue(e.target.value)}
                                                className="w-full h-40 bg-black/60 border border-nexus-500/50 rounded-xl p-4 text-slate-200 focus:outline-none focus:ring-1 focus:ring-nexus-500 font-mono text-sm resize-none"
                                            />
                                        ) : (
                                            <div className="text-slate-300 leading-relaxed bg-black/40 p-4 rounded-xl border border-white/5 min-h-[10rem]">
                                                {currentCard.agent}
                                            </div>
                                        )}
                                    </div>
                                </div>

                                {/* Action Buttons */}
                                <div className="p-6 bg-white/5 border-t border-white/5 flex items-center justify-between gap-4">
                                    {isEditing ? (
                                        <>
                                            <button
                                                onClick={() => setIsEditing(false)}
                                                className="flex-1 py-4 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold transition-all"
                                            >
                                                Cancel
                                            </button>
                                            <button
                                                onClick={handleSaveEdit}
                                                className="flex-1 py-4 rounded-xl bg-nexus-600 hover:bg-nexus-500 text-white font-bold transition-all flex items-center justify-center gap-2"
                                            >
                                                <FaSave /> Save Repair
                                            </button>
                                        </>
                                    ) : (
                                        <>
                                            <button
                                                onClick={() => handleVote(false)}
                                                className="flex-1 py-4 rounded-xl bg-red-500/10 hover:bg-red-500 text-red-500 hover:text-white border border-red-500/30 hover:border-red-500 font-bold transition-all flex items-center justify-center gap-2 group"
                                            >
                                                <FaTimes className="group-hover:scale-110 transition-transform" />
                                                Reject
                                            </button>
                                            <button
                                                onClick={() => handleVote(true)}
                                                className="flex-1 py-4 rounded-xl bg-reinforce-500/10 hover:bg-reinforce-500 text-reinforce-400 hover:text-white border border-reinforce-500/30 hover:border-reinforce-500 font-bold transition-all flex items-center justify-center gap-2 group"
                                            >
                                                <FaCheck className="group-hover:scale-110 transition-transform" />
                                                Reward
                                            </button>
                                        </>
                                    )}
                                </div>
                            </motion.div>
                        ) : (
                            <motion.div
                                initial={{ opacity: 0 }}
                                animate={{ opacity: 1 }}
                                className="text-center"
                            >
                                <div className="text-6xl mb-6">🎉</div>
                                <h2 className="text-2xl font-bold text-white mb-2">Queue Empty!</h2>
                                <p className="text-slate-400">Great job improving the dataset.</p>
                                <button
                                    onClick={() => setQueue(MOCK_QUEUE)}
                                    className="mt-8 px-6 py-2 bg-white/5 hover:bg-white/10 rounded-full text-sm text-slate-300 transition-colors"
                                >
                                    Reset Simulation
                                </button>
                            </motion.div>
                        )}
                    </AnimatePresence>

                    {/* Keyboard Shortcuts Hint */}
                    <div className="absolute bottom-8 text-xs text-slate-600 font-mono">
                        Press <span className="px-1.5 py-0.5 rounded bg-white/10 text-slate-400">←</span> to Reject, <span className="px-1.5 py-0.5 rounded bg-white/10 text-slate-400">→</span> to Reward
                    </div>
                </div>
            </div>
        </div>
    );
}
