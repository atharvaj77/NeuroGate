'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { FaArrowLeft, FaSearch, FaDatabase, FaFolder, FaFileAlt, FaCube, FaPlus } from 'react-icons/fa';
import { motion, AnimatePresence } from 'framer-motion';
import { NexusClient, RAGStats } from './NexusClient';
import { FaCloudUploadAlt, FaTimes } from 'react-icons/fa';

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

const MOCK_STATS: RAGStats = {
    totalDocuments: 1842,
    averageTokenCount: 512,
    totalUsageCount: 8420,
    cacheHitRate: 0.85,
    averageCostSavings: 120.50
};

export default function NexusPage() {
    const [query, setQuery] = useState('');
    const [isSearching, setIsSearching] = useState(false);
    const [results, setResults] = useState<any[]>([]);
    const [activeCollection, setActiveCollection] = useState('eng');
    const [simulationMode, setSimulationMode] = useState(true);
    const [stats, setStats] = useState<RAGStats | null>(null);
    const [showUploadModal, setShowUploadModal] = useState(false);

    // Upload Form State
    const [uploadTitle, setUploadTitle] = useState('');
    const [uploadContent, setUploadContent] = useState('');
    const [uploadSource, setUploadSource] = useState('');
    const [isUploading, setIsUploading] = useState(false);

    // Initial Fetch for Stats
    useEffect(() => {
        if (!simulationMode) {
            NexusClient.getStats().then(setStats);
        } else {
            setStats(MOCK_STATS);
        }
    }, [simulationMode]);

    const handleUpload = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsUploading(true);
        try {
            if (simulationMode) {
                // Mock Upload
                await new Promise(r => setTimeout(r, 1000));
                alert('Document uploaded (Simulation)');
            } else {
                const success = await NexusClient.addDocument(uploadTitle, uploadContent, uploadSource);
                if (success) {
                    // Refresh stats
                    NexusClient.getStats().then(setStats);
                } else {
                    alert('Upload failed');
                }
            }
            setShowUploadModal(false);
            setUploadTitle('');
            setUploadContent('');
            setUploadSource('');
        } finally {
            setIsUploading(false);
        }
    };

    const handleSearch = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsSearching(true);
        setResults([]);

        if (simulationMode) {
            // Simulate network latency
            setTimeout(() => {
                setResults(MOCK_RESULTS);
                setIsSearching(false);
            }, 800);
        } else {
            // Real Backend Call
            try {
                // Pass activeCollection (NexusService will handle it)
                const data = await NexusClient.searchDocuments(query, activeCollection);

                // Map results to UI format
                const mapped = data.map(item => ({
                    id: item.id,
                    content: item.payload?.content || 'No content available',
                    score: item.score.toFixed(2),
                    source: item.payload?.source || 'Unknown Source'
                }));
                setResults(mapped);
            } catch (err) {
                console.error(err);
            } finally {
                setIsSearching(false);
            }
        }
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

                </div>

                {/* Add Document Button */}
                <div className="p-4 border-t border-white/5">
                    <button
                        onClick={() => setShowUploadModal(true)}
                        className="w-full flex items-center justify-center gap-2 p-3 rounded-xl bg-gradient-to-r from-nexus-600 to-blue-600 hover:from-nexus-500 hover:to-blue-500 text-white shadow-lg shadow-nexus-500/20 transition-all font-medium text-sm"
                    >
                        <FaCloudUploadAlt /> Add Content
                    </button>
                </div>

                <div className="p-4 border-t border-white/5 text-xs text-slate-500 text-center">
                    <div className="flex items-center justify-center gap-2 mb-2">
                        {stats ? (
                            <div className="text-center w-full">
                                <div className="text-2xl font-bold text-white mb-1">{stats.totalDocuments}</div>
                                <div className="text-xs text-slate-500 uppercase tracking-widest">Total Documents</div>
                                <div className="mt-2 text-[10px] text-slate-600">
                                    {(stats.averageTokenCount).toFixed(0)} avg tokens
                                </div>
                            </div>
                        ) : (
                            <>
                                <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse"></div>
                                Vector DB Connected
                            </>
                        )}
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

                        {/* Loading Skeleton */}
                        {isSearching && (
                            <div className="space-y-4 w-full">
                                {[1, 2, 3].map(i => (
                                    <div key={i} className="h-32 bg-white/5 rounded-xl border border-white/5 animate-pulse relative overflow-hidden">
                                        <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/5 to-transparent -translate-x-full animate-[shimmer_1.5s_infinite]" />
                                    </div>
                                ))}
                            </div>
                        )}

                        {results.length === 0 && !isSearching && (
                            <div className="h-full flex flex-col items-center justify-center opacity-30">
                                <FaCube className="text-6xl text-nexus-500 mb-6 animate-pulse" />
                                <p className="text-slate-400 font-mono">Awaiting Query Vectorization...</p>
                            </div>
                        )}

                        <AnimatePresence mode="popLayout">
                            {!isSearching && results.map((res, i) => (
                                <motion.div
                                    layout
                                    key={res.id}
                                    initial={{ opacity: 0, scale: 0.95, y: 20 }}
                                    animate={{ opacity: 1, scale: 1, y: 0 }}
                                    exit={{ opacity: 0, scale: 0.95 }}
                                    transition={{ delay: i * 0.05, duration: 0.3 }}
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
            {/* Upload Modal */}
            <AnimatePresence>
                {showUploadModal && (
                    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                        <motion.div
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            onClick={() => setShowUploadModal(false)}
                            className="absolute inset-0 bg-black/80 backdrop-blur-sm"
                        />
                        <motion.div
                            initial={{ scale: 0.95, opacity: 0, y: 20 }}
                            animate={{ scale: 1, opacity: 1, y: 0 }}
                            exit={{ scale: 0.95, opacity: 0, y: 20 }}
                            className="w-full max-w-lg bg-slate-900 border border-white/10 rounded-2xl p-6 relative z-10 shadow-2xl"
                        >
                            <button
                                onClick={() => setShowUploadModal(false)}
                                className="absolute top-4 right-4 text-slate-500 hover:text-white transition-colors"
                            >
                                <FaTimes />
                            </button>
                            <h2 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
                                <FaCloudUploadAlt className="text-nexus-500" />
                                Add Knowledge
                            </h2>
                            <form onSubmit={handleUpload} className="space-y-4">
                                <div>
                                    <label className="block text-xs font-mono text-slate-400 mb-1">Document Title</label>
                                    <input
                                        required
                                        type="text"
                                        value={uploadTitle}
                                        onChange={e => setUploadTitle(e.target.value)}
                                        className="w-full bg-black/40 border border-white/10 rounded-lg p-3 text-white focus:border-nexus-500 focus:outline-none"
                                        placeholder="e.g. Q4 Financial Report"
                                    />
                                </div>
                                <div>
                                    <label className="block text-xs font-mono text-slate-400 mb-1">Source Identifier</label>
                                    <input
                                        required
                                        type="text"
                                        value={uploadSource}
                                        onChange={e => setUploadSource(e.target.value)}
                                        className="w-full bg-black/40 border border-white/10 rounded-lg p-3 text-white focus:border-nexus-500 focus:outline-none"
                                        placeholder="e.g. finance_sharepoint_v2"
                                    />
                                </div>
                                <div>
                                    <label className="block text-xs font-mono text-slate-400 mb-1">Content</label>
                                    <textarea
                                        required
                                        rows={6}
                                        value={uploadContent}
                                        onChange={e => setUploadContent(e.target.value)}
                                        className="w-full bg-black/40 border border-white/10 rounded-lg p-3 text-white focus:border-nexus-500 focus:outline-none resize-none"
                                        placeholder="Paste document text here..."
                                    />
                                </div>
                                <button
                                    type="submit"
                                    disabled={isUploading}
                                    className="w-full py-3 bg-nexus-600 hover:bg-nexus-500 text-white rounded-xl font-semibold transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    {isUploading ? 'Ingesting...' : 'Ingest Document'}
                                </button>
                            </form>
                        </motion.div>
                    </div>
                )}
            </AnimatePresence>
        </div>
    );
}
