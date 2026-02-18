'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { FaArrowLeft, FaSearch, FaDatabase, FaFolder, FaShieldAlt, FaStream, FaCheck } from 'react-icons/fa';
import { motion, AnimatePresence } from 'framer-motion';
import { BiSolidServer } from 'react-icons/bi';

// Mock Collections
const MOCK_COLLECTIONS = [
    { id: 'products', name: 'Products', dimensions: 1536, metric: 'cosine', count: 245_000 },
    { id: 'documents', name: 'Documents', dimensions: 768, metric: 'euclidean', count: 89_420 },
    { id: 'users', name: 'User Profiles', dimensions: 512, metric: 'dot', count: 1_200_000 },
    { id: 'images', name: 'Image Embeddings', dimensions: 2048, metric: 'cosine', count: 450_000 },
];

// Mock Search Results
const MOCK_SEARCH_RESULTS = [
    { id: 'vec_001', content: 'Enterprise SLA guarantees 99.99% uptime with automated failover...', score: 0.96, collection: 'documents', metadata: { source: 'sla_v3.pdf', updated: '2025-12-01' } },
    { id: 'vec_002', content: 'Premium support plan includes dedicated account manager and 24/7 incident response...', score: 0.91, collection: 'documents', metadata: { source: 'support_tiers.md', updated: '2025-11-15' } },
    { id: 'vec_003', content: 'Compliance documentation available for SOC 2 Type II and HIPAA BAA...', score: 0.87, collection: 'documents', metadata: { source: 'compliance_faq.pdf', updated: '2025-10-20' } },
];

const COMPLIANCE_BADGES = [
    { name: 'SOC 2', desc: 'Type II Certified', color: 'text-blue-400 border-blue-500/30 bg-blue-500/10' },
    { name: 'HIPAA', desc: 'BAA Available', color: 'text-green-400 border-green-500/30 bg-green-500/10' },
    { name: 'FedRAMP', desc: 'In Progress', color: 'text-amber-400 border-amber-500/30 bg-amber-500/10' },
    { name: 'GDPR', desc: 'Compliant', color: 'text-purple-400 border-purple-500/30 bg-purple-500/10' },
];

export default function EngramPage() {
    const [query, setQuery] = useState('');
    const [isSearching, setIsSearching] = useState(false);
    const [results, setResults] = useState<typeof MOCK_SEARCH_RESULTS>([]);
    const [activeCollection, setActiveCollection] = useState('documents');
    const [activeTab, setActiveTab] = useState<'collections' | 'search' | 'ingest' | 'compliance'>('collections');

    const handleSearch = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!query.trim()) return;
        setIsSearching(true);
        setResults([]);

        // Simulate search
        setTimeout(() => {
            setResults(MOCK_SEARCH_RESULTS);
            setIsSearching(false);
        }, 600);
    };

    return (
        <div className="min-h-screen bg-black text-slate-100 font-sans selection:bg-indigo-500/30">
            {/* Background */}
            <div className="absolute inset-0 bg-[url('/grid.svg')] opacity-20 pointer-events-none" />
            <div className="absolute inset-0 mesh-bg pointer-events-none" />

            {/* Nav */}
            <nav className="fixed top-0 w-full z-50 glass border-b border-white/5 bg-black/40 backdrop-blur-md">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between items-center h-20">
                        <Link href="/" className="flex items-center space-x-2 cursor-pointer group">
                            <BiSolidServer className="text-3xl text-primary-400" />
                            <span className="text-xl font-bold tracking-tight text-white">NeuroGate</span>
                        </Link>
                        <Link href="/" className="text-sm font-medium text-slate-400 hover:text-white transition-colors flex items-center gap-2">
                            <FaArrowLeft className="text-xs" /> Back to Home
                        </Link>
                    </div>
                </div>
            </nav>

            {/* Hero */}
            <section className="pt-32 pb-16 relative z-10">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-sm font-medium mb-8"
                    >
                        <span className="w-2 h-2 rounded-full bg-indigo-400 animate-pulse" />
                        Simulation Mode
                    </motion.div>
                    <motion.h1
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.1 }}
                        className="text-5xl md:text-7xl font-bold mb-6"
                    >
                        <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 to-blue-400">Engram</span>
                    </motion.h1>
                    <motion.p
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.2 }}
                        className="text-xl text-slate-400 max-w-2xl mx-auto mb-12"
                    >
                        Enterprise Vector Data Store. Configurable collections, top-K similarity search, and streaming ingestion with compliance-ready architecture.
                    </motion.p>

                    {/* Stats */}
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.3 }}
                        className="grid grid-cols-2 md:grid-cols-4 gap-6 max-w-3xl mx-auto mb-16"
                    >
                        {[
                            { value: '1.98M', label: 'Vectors Stored' },
                            { value: '<45ms', label: 'Search Latency' },
                            { value: '4', label: 'Collections' },
                            { value: '99.99%', label: 'Availability' },
                        ].map((stat, i) => (
                            <div key={i} className="glass p-4 rounded-xl border border-white/5">
                                <div className="text-2xl font-bold text-white font-mono">{stat.value}</div>
                                <div className="text-xs text-slate-500 uppercase tracking-wider mt-1">{stat.label}</div>
                            </div>
                        ))}
                    </motion.div>
                </div>
            </section>

            {/* Tab Navigation */}
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
                <div className="flex gap-2 mb-8 overflow-x-auto pb-2">
                    {[
                        { key: 'collections' as const, label: 'Collections', icon: <FaFolder /> },
                        { key: 'search' as const, label: 'Vector Search', icon: <FaSearch /> },
                        { key: 'ingest' as const, label: 'Ingestion Pipeline', icon: <FaStream /> },
                        { key: 'compliance' as const, label: 'Compliance', icon: <FaShieldAlt /> },
                    ].map((tab) => (
                        <button
                            key={tab.key}
                            onClick={() => setActiveTab(tab.key)}
                            className={`flex items-center gap-2 px-5 py-3 rounded-xl text-sm font-medium transition-all whitespace-nowrap ${
                                activeTab === tab.key
                                    ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30'
                                    : 'text-slate-400 hover:bg-white/5 border border-transparent'
                            }`}
                        >
                            {tab.icon} {tab.label}
                        </button>
                    ))}
                </div>
            </div>

            {/* Tab Content */}
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-32 relative z-10">
                <AnimatePresence mode="wait">
                    {/* Collections Tab */}
                    {activeTab === 'collections' && (
                        <motion.div
                            key="collections"
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: -10 }}
                            className="space-y-4"
                        >
                            <div className="grid md:grid-cols-2 gap-4">
                                {MOCK_COLLECTIONS.map((col) => (
                                    <div
                                        key={col.id}
                                        onClick={() => setActiveCollection(col.id)}
                                        className={`glass p-6 rounded-xl border cursor-pointer transition-all group ${
                                            activeCollection === col.id
                                                ? 'border-indigo-500/40 bg-indigo-500/5'
                                                : 'border-white/5 hover:border-white/10'
                                        }`}
                                    >
                                        <div className="flex items-start justify-between mb-4">
                                            <div className="flex items-center gap-3">
                                                <div className={`p-2 rounded-lg ${activeCollection === col.id ? 'bg-indigo-500/20 text-indigo-400' : 'bg-white/5 text-slate-500'}`}>
                                                    <FaDatabase />
                                                </div>
                                                <div>
                                                    <h3 className="font-bold text-white">{col.name}</h3>
                                                    <p className="text-xs text-slate-500 font-mono">{col.id}</p>
                                                </div>
                                            </div>
                                            <span className={`text-xs px-2 py-1 rounded-full font-mono ${activeCollection === col.id ? 'bg-indigo-500/20 text-indigo-300' : 'bg-white/5 text-slate-500'}`}>
                                                {col.metric}
                                            </span>
                                        </div>
                                        <div className="grid grid-cols-2 gap-3">
                                            <div className="bg-black/30 p-3 rounded-lg">
                                                <div className="text-xs text-slate-500 mb-1">Dimensions</div>
                                                <div className="text-sm font-mono text-slate-200">{col.dimensions}</div>
                                            </div>
                                            <div className="bg-black/30 p-3 rounded-lg">
                                                <div className="text-xs text-slate-500 mb-1">Vectors</div>
                                                <div className="text-sm font-mono text-slate-200">{col.count.toLocaleString()}</div>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </motion.div>
                    )}

                    {/* Search Tab */}
                    {activeTab === 'search' && (
                        <motion.div
                            key="search"
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: -10 }}
                        >
                            <form onSubmit={handleSearch} className="relative mb-8 group">
                                <div className="absolute inset-0 bg-indigo-500/20 blur-xl opacity-0 group-focus-within:opacity-100 transition-opacity rounded-full" />
                                <input
                                    type="text"
                                    value={query}
                                    onChange={(e) => setQuery(e.target.value)}
                                    placeholder="Enter a natural language query to find similar vectors..."
                                    className="w-full bg-black/60 border border-white/10 rounded-2xl py-5 px-8 pl-14 text-lg text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500/50 focus:ring-1 focus:ring-indigo-500/30 transition-all relative z-10 glass"
                                />
                                <FaSearch className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-500 z-20 text-xl" />
                                <button
                                    type="submit"
                                    className="absolute right-3 top-1/2 -translate-y-1/2 px-6 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-sm font-semibold transition-colors z-20"
                                >
                                    {isSearching ? 'Searching...' : 'Search'}
                                </button>
                            </form>

                            {/* Results */}
                            <div className="space-y-4">
                                {isSearching && (
                                    <div className="space-y-4">
                                        {[1, 2, 3].map(i => (
                                            <div key={i} className="h-28 bg-white/5 rounded-xl border border-white/5 animate-pulse" />
                                        ))}
                                    </div>
                                )}

                                {results.length === 0 && !isSearching && (
                                    <div className="text-center py-20 opacity-40">
                                        <FaSearch className="text-5xl text-indigo-500 mx-auto mb-4" />
                                        <p className="text-slate-400 font-mono text-sm">Enter a query to perform similarity search</p>
                                    </div>
                                )}

                                <AnimatePresence>
                                    {!isSearching && results.map((res, i) => (
                                        <motion.div
                                            key={res.id}
                                            initial={{ opacity: 0, y: 15 }}
                                            animate={{ opacity: 1, y: 0 }}
                                            transition={{ delay: i * 0.08 }}
                                            className="glass p-6 rounded-xl border border-white/5 hover:border-indigo-500/30 transition-all"
                                        >
                                            <div className="flex justify-between items-start mb-3">
                                                <div className="flex items-center gap-2">
                                                    <span className="text-xs font-mono text-slate-500">{res.id}</span>
                                                    <span className="text-xs text-slate-600">|</span>
                                                    <span className="text-xs text-slate-500">{res.metadata.source}</span>
                                                </div>
                                                <div className="px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-xs font-mono">
                                                    {res.score.toFixed(2)}
                                                </div>
                                            </div>
                                            <p className="text-slate-300 leading-relaxed">{res.content}</p>
                                        </motion.div>
                                    ))}
                                </AnimatePresence>
                            </div>
                        </motion.div>
                    )}

                    {/* Ingest Tab */}
                    {activeTab === 'ingest' && (
                        <motion.div
                            key="ingest"
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: -10 }}
                        >
                            <div className="glass p-8 rounded-2xl border border-white/5 mb-8">
                                <h3 className="text-xl font-bold text-white mb-6">Ingestion Pipeline</h3>
                                <div className="grid md:grid-cols-3 gap-6 mb-8">
                                    {[
                                        { name: 'REST API', desc: 'POST /v1/engram/vectors', status: 'active', color: 'text-green-400' },
                                        { name: 'Kafka Consumer', desc: 'topic: engram.vectors.ingest', status: 'active', color: 'text-green-400' },
                                        { name: 'Batch Upload', desc: 'CSV / Parquet / JSONL', status: 'active', color: 'text-green-400' },
                                    ].map((source, i) => (
                                        <div key={i} className="bg-black/40 border border-white/5 rounded-xl p-5">
                                            <div className="flex items-center justify-between mb-3">
                                                <span className="font-bold text-white">{source.name}</span>
                                                <span className={`text-xs ${source.color} flex items-center gap-1`}>
                                                    <span className="w-1.5 h-1.5 rounded-full bg-current" /> {source.status}
                                                </span>
                                            </div>
                                            <p className="text-xs font-mono text-slate-500">{source.desc}</p>
                                        </div>
                                    ))}
                                </div>

                                {/* Visual Pipeline */}
                                <div className="flex items-center justify-center gap-4 text-slate-500 text-sm py-6">
                                    <div className="flex flex-col items-center gap-2">
                                        <div className="p-3 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
                                            <FaStream />
                                        </div>
                                        <span className="text-xs">Sources</span>
                                    </div>
                                    <div className="flex-1 h-px bg-gradient-to-r from-indigo-500/30 to-blue-500/30" />
                                    <div className="flex flex-col items-center gap-2">
                                        <div className="p-3 rounded-xl bg-blue-500/10 border border-blue-500/20 text-blue-400">
                                            <FaDatabase />
                                        </div>
                                        <span className="text-xs">Embed</span>
                                    </div>
                                    <div className="flex-1 h-px bg-gradient-to-r from-blue-500/30 to-indigo-500/30" />
                                    <div className="flex flex-col items-center gap-2">
                                        <div className="p-3 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
                                            <FaCheck />
                                        </div>
                                        <span className="text-xs">Store</span>
                                    </div>
                                </div>
                            </div>

                            {/* Throughput Stats */}
                            <div className="grid md:grid-cols-3 gap-4">
                                {[
                                    { label: 'Ingest Rate', value: '12.4K vec/s' },
                                    { label: 'Queue Depth', value: '0' },
                                    { label: 'Avg Embed Time', value: '8.2ms' },
                                ].map((stat, i) => (
                                    <div key={i} className="glass p-5 rounded-xl border border-white/5 text-center">
                                        <div className="text-2xl font-bold text-white font-mono">{stat.value}</div>
                                        <div className="text-xs text-slate-500 uppercase tracking-wider mt-1">{stat.label}</div>
                                    </div>
                                ))}
                            </div>
                        </motion.div>
                    )}

                    {/* Compliance Tab */}
                    {activeTab === 'compliance' && (
                        <motion.div
                            key="compliance"
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: -10 }}
                        >
                            <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
                                {COMPLIANCE_BADGES.map((badge) => (
                                    <div key={badge.name} className={`p-6 rounded-xl border ${badge.color} text-center`}>
                                        <FaShieldAlt className="text-3xl mx-auto mb-3" />
                                        <h3 className="text-lg font-bold">{badge.name}</h3>
                                        <p className="text-xs mt-1 opacity-70">{badge.desc}</p>
                                    </div>
                                ))}
                            </div>

                            <div className="glass p-8 rounded-2xl border border-white/5">
                                <h3 className="text-xl font-bold text-white mb-6">Security Features</h3>
                                <div className="grid md:grid-cols-2 gap-6">
                                    {[
                                        { title: 'Encryption at Rest', desc: 'AES-256 encryption for all stored vectors and metadata' },
                                        { title: 'Encryption in Transit', desc: 'TLS 1.3 for all API communications' },
                                        { title: 'Tenant Isolation', desc: 'Namespace-level isolation with separate encryption keys per tenant' },
                                        { title: 'Audit Logging', desc: 'Immutable audit trail for all CRUD operations and search queries' },
                                        { title: 'Data Residency', desc: 'Configure storage regions to meet data sovereignty requirements' },
                                        { title: 'Access Control', desc: 'RBAC with JWT-based authentication and collection-level permissions' },
                                    ].map((feature, i) => (
                                        <div key={i} className="flex items-start gap-3">
                                            <FaCheck className="text-indigo-400 mt-1 flex-shrink-0" />
                                            <div>
                                                <h4 className="font-bold text-slate-200">{feature.title}</h4>
                                                <p className="text-sm text-slate-500">{feature.desc}</p>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </motion.div>
                    )}
                </AnimatePresence>
            </div>
        </div>
    );
}
