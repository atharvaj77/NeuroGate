'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { FaArrowLeft, FaSearch, FaDatabase, FaFolder, FaFileAlt, FaCube, FaPlus } from 'react-icons/fa';
import { motion, AnimatePresence } from 'framer-motion';

// Mock Data for "Simulation"
const MOCK_COLLECTIONS = [
    { id: 'eng', name: 'Engineering', count: 124 },
    { id: 'hr', name: 'Human Resources', count: 45 },
    { id: 'legal', name: 'Legal', count: 12 }
];

const MOCK_RESULTS = [
    { id: 'doc_1', content: 'To reset your VPN password, navigate to idm.corp.local...', score: 0.92, source: 'it_handbook_v2.pdf' },
    { id: 'doc_2', content: 'VPN access policies require 2FA authentication...', score: 0.85, source: 'security_policy.docx' },
    { id: 'doc_3', content: 'Legacy VPN gateways will be deprecated in Q4...', score: 0.71, source: 'infra_roadmap.md' }
];

export default function NexusPage() {
    const [query, setQuery] = useState('');
    const [isSearching, setIsSearching] = useState(false);
    const [results, setResults] = useState<typeof MOCK_RESULTS>([]);
    const [activeCollection, setActiveCollection] = useState('eng');

    const handleSearch = (e: React.FormEvent) => {
        e.preventDefault();
        setIsSearching(true);
        // Simulate network latency
        setTimeout(() => {
            setResults(MOCK_RESULTS);
            setIsSearching(false);
        }, 800);
    };

    return (
        <div className="flex h-screen bg-black text-slate-100 font-sans overflow-hidden selection:bg-blue-500/30">
            {/* Background Effects */}
            <div className="absolute inset-0 bg-[url('/grid.svg')] opacity-20 pointer-events-none" />

            {/* Left Sidebar: Knowledge Graph Nav */}
            <div className="w-64 h-full border-r border-white/5 flex flex-col glass z-10 bg-black/40">
                <div className="p-6 border-b border-white/5">
                    <Link href="/" className="flex items-center gap-2 text-slate-400 hover:text-white transition-colors mb-4 text-xs">
                        <FaArrowLeft /> Back to OS
                    </Link>
                    <h1 className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-nexus-400 to-blue-400 font-mono flex items-center gap-2">
                        <FaDatabase className="text-nexus-400" />
                        NEXUS
                    </h1>
                    <p className="text-xs text-slate-500 mt-1 uppercase tracking-widest">RAG Gateway</p>
                </div>

                <div className="flex-1 overflow-y-auto p-4 space-y-2">
                    <div className="text-xs font-bold text-slate-500 uppercase mb-2 px-2">Collections</div>
                    {MOCK_COLLECTIONS.map(col => (
                        <button
                            key={col.id}
                            onClick={() => setActiveCollection(col.id)}
                            className={`w-full flex items-center justify-between p-3 rounded-lg text-sm transition-all ${activeCollection === col.id
                                    ? 'bg-nexus-500/20 text-white border border-nexus-500/30'
                                    : 'text-slate-400 hover:bg-white/5'
                                }`}
                        >
                            <div className="flex items-center gap-3">
                                <FaFolder className={activeCollection === col.id ? 'text-nexus-400' : 'text-slate-600'} />
                                <span>{col.name}</span>
                            </div>
                            <span className="text-xs bg-black/40 px-2 py-0.5 rounded-full text-slate-500">{col.count}</span>
                        </button>
                    ))}

                    <button className="w-full flex items-center gap-3 p-3 text-sm text-slate-500 hover:text-nexus-400 transition-colors border-t border-white/5 mt-4 pt-4">
                        <FaPlus /> New Collection
                    </button>
                </div>

                <div className="p-4 border-t border-white/5 text-xs text-slate-500 text-center">
                    <div className="flex items-center justify-center gap-2 mb-2">
                        <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse"></div>
                        Vector DB Connected
                    </div>
                </div>
            </div>

            {/* Main Content */}
            <div className="flex-1 flex flex-col h-full relative z-0">
                {/* Header */}
                <header className="h-16 border-b border-white/5 flex items-center justify-between px-8 glass bg-black/20">
                    <div className="flex items-center gap-4">
                        <div className="text-sm text-slate-400">
                            Context: <span className="text-nexus-300">All Collections</span>
                        </div>
                    </div>

                    {/* Simulation Mode Badge */}
                    <div className="flex items-center gap-2 px-3 py-1 rounded bg-yellow-500/10 border border-yellow-500/30 text-yellow-500 group relative cursor-help text-xs">
                        <div className="w-1.5 h-1.5 rounded-full bg-yellow-500 animate-pulse" />
                        SIMULATION MODE

                        {/* Tooltip */}
                        <div className="absolute top-full right-0 mt-2 w-72 p-3 bg-black border border-white/20 rounded shadow-xl opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-50">
                            <p className="text-slate-400 mb-2">Vector search mocked.</p>
                            <div className="p-2 bg-white/5 rounded border border-white/10 font-mono text-[10px] text-cyan-300 break-all mb-1">
                                POST /api/v1/nexus/search
                            </div>
                            <p className="text-slate-500 mt-2 font-normal">Connects to &apos;NexusService&apos; & Qdrant.</p>
                        </div>
                    </div>
                </header>

                <div className="flex-1 overflow-hidden relative p-8 flex flex-col items-center">

                    {/* Search Bar */}
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="w-full max-w-3xl mb-12"
                    >
                        <form onSubmit={handleSearch} className="relative group">
                            <div className="absolute inset-0 bg-nexus-500/20 blur-xl opacity-0 group-focus-within:opacity-100 transition-opacity rounded-full"></div>
                            <input
                                type="text"
                                value={query}
                                onChange={(e) => setQuery(e.target.value)}
                                placeholder="Search knowledge base..."
                                className="w-full bg-black/60 border border-white/10 rounded-2xl py-5 px-8 pl-14 text-lg text-white placeholder-slate-500 focus:outline-none focus:border-nexus-500/50 focus:ring-1 focus:ring-nexus-500/30 transition-all relative z-10 glass"
                            />
                            <FaSearch className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-500 z-20 text-xl" />
                            <button
                                type="submit"
                                className="absolute right-3 top-1/2 -translate-y-1/2 px-6 py-2 bg-nexus-600 hover:bg-nexus-500 text-white rounded-xl text-sm font-semibold transition-colors z-20"
                            >
                                {isSearching ? 'Searching...' : 'Search'}
                            </button>
                        </form>
                    </motion.div>

                    {/* Results Grid (or Empty State) */}
                    <div className="w-full max-w-5xl flex-1 overflow-y-auto custom-scrollbar">
                        {results.length === 0 && !isSearching && (
                            <div className="h-full flex flex-col items-center justify-center opacity-30">
                                <FaCube className="text-6xl text-nexus-500 mb-6 animate-pulse" />
                                <p className="text-slate-400 font-mono">Awaiting Query Vectorization...</p>
                            </div>
                        )}

                        <AnimatePresence>
                            {results.map((res, i) => (
                                <motion.div
                                    key={res.id}
                                    initial={{ opacity: 0, x: -20 }}
                                    animate={{ opacity: 1, x: 0 }}
                                    transition={{ delay: i * 0.1 }}
                                    className="mb-4 group"
                                >
                                    <div className="p-6 rounded-xl glass border border-white/5 hover:border-nexus-500/30 transition-all bg-black/40 hover:bg-nexus-900/10 relative overflow-hidden">
                                        <div className="flex justify-between items-start mb-2">
                                            <div className="flex items-center gap-3">
                                                <div className="p-2 rounded bg-nexus-500/10 text-nexus-400">
                                                    <FaFileAlt />
                                                </div>
                                                <span className="text-sm font-mono text-slate-400">{res.source}</span>
                                            </div>
                                            <div className="px-3 py-1 rounded-full bg-nexus-500/10 border border-nexus-500/20 text-nexus-300 text-xs font-mono">
                                                Score: {res.score}
                                            </div>
                                        </div>
                                        <p className="text-slate-300 leading-relaxed pl-11">
                                            {res.content}
                                        </p>

                                        {/* Vector Visual (Abstract) */}
                                        <div className="absolute right-0 top-0 w-32 h-full bg-gradient-to-l from-nexus-500/5 to-transparent pointer-events-none" />
                                    </div>
                                </motion.div>
                            ))}
                        </AnimatePresence>
                    </div>
                </div>
            </div>
        </div>
    );
}
