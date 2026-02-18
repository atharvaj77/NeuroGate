'use client'

import { motion } from 'framer-motion'
import { FaGithub, FaArrowLeft, FaBook, FaRocket, FaCog, FaBrain, FaShieldAlt, FaCode, FaMemory, FaMagic, FaSearch, FaUserEdit, FaFire, FaChartLine, FaDatabase } from 'react-icons/fa'
import Link from 'next/link'

export default function Docs() {
  return (
    <main className="min-h-screen bg-black text-slate-100 selection:bg-primary-500/30 overflow-hidden relative">
      {/* Background */}
      <div className="absolute inset-0 bg-[url('/grid.svg')] opacity-20 pointer-events-none" />

      {/* Navigation */}
      <nav className="fixed top-0 w-full z-50 glass border-b border-white/5 bg-black/40 backdrop-blur-md">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-20">
            <Link href="/" className="flex items-center space-x-2 text-slate-400 hover:text-white transition-colors group">
              <FaArrowLeft className="group-hover:-translate-x-1 transition-transform text-xs" />
              <span className="font-mono text-sm">OS_ROOT</span>
            </Link>
            <a href="https://github.com/atharvajoshi/NeuroGate" target="_blank" rel="noopener noreferrer" className="hover:text-primary-400 transition-colors opacity-80 hover:opacity-100 flex items-center gap-2">
              <FaGithub className="text-xl" />
              <span className="font-mono text-xs hidden md:inline">Star on GitHub</span>
            </a>
          </div>
        </div>
      </nav>

      <div className="pt-32 pb-20 relative z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* Header */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-center mb-16"
          >
            <div className="inline-block px-4 py-1.5 rounded-full bg-primary-500/10 border border-primary-500/20 text-primary-300 text-sm font-mono tracking-wider mb-6">
              KERNEL VERSION v1.0.0
            </div>
            <h1 className="text-5xl md:text-7xl font-bold mb-6 font-mono tracking-tight">
              KERNEL <span className="text-transparent bg-clip-text bg-gradient-to-r from-primary-400 via-accent-400 to-primary-400 animate-gradient-x">REFERENCE</span>
            </h1>
            <p className="text-xl text-slate-400 max-w-3xl mx-auto font-light leading-relaxed">
              Technical specifications for the Open Source Agent-Native AI Kernel.
            </p>
          </motion.div>

          <div className="grid lg:grid-cols-[250px_1fr] gap-12">
            {/* Sidebar Navigation (Desktop) */}
            <div className="hidden lg:block sticky top-32 h-fit">
              <h3 className="font-bold text-slate-200 mb-4 px-2 flex items-center gap-2">
                <FaBook className="text-primary-500" /> Directory
              </h3>
              <ul className="space-y-1 text-sm text-slate-400 border-l border-white/10 pl-4">
                <li className="font-bold text-slate-600 uppercase text-[10px] tracking-widest mt-4 mb-2">Build Layer</li>
                {['Synapse', 'OpenAI Compatible'].map((item) => (
                  <li key={item}>
                    <a href={`#${item.toLowerCase().replace(' ', '')}`} className="block py-2 hover:text-primary-300 transition-colors font-mono text-xs">
                      {item}
                    </a>
                  </li>
                ))}

                <li className="font-bold text-slate-600 uppercase text-[10px] tracking-widest mt-6 mb-2">Run Layer</li>
                {['Nexus', 'Hive Mind', 'NeuroGuard', 'Iron Gate'].map((item) => (
                  <li key={item}>
                    <a href={`#${item.toLowerCase().replace(' ', '')}`} className="block py-2 hover:text-accent-300 transition-colors font-mono text-xs">
                      {item}
                    </a>
                  </li>
                ))}

                <li className="font-bold text-slate-600 uppercase text-[10px] tracking-widest mt-6 mb-2">Improve Layer</li>
                {['Cortex', 'Reinforce', 'Forge'].map((item) => (
                  <li key={item}>
                    <a href={`#${item.toLowerCase().replace(' ', '')}`} className="block py-2 hover:text-green-300 transition-colors font-mono text-xs">
                      {item}
                    </a>
                  </li>
                ))}

                <li className="font-bold text-slate-600 uppercase text-[10px] tracking-widest mt-6 mb-2">Store Layer</li>
                {['Engram'].map((item) => (
                  <li key={item}>
                    <a href={`#${item.toLowerCase()}`} className="block py-2 hover:text-indigo-300 transition-colors font-mono text-xs">
                      {item}
                    </a>
                  </li>
                ))}
              </ul>
            </div>

            {/* Main Content */}
            <div className="space-y-20">

              {/* === BUILD LAYER === */}

              {/* Synapse */}
              <section id="synapse">
                <h2 className="text-3xl font-bold mb-6 flex items-center gap-3 font-mono text-slate-200 border-b border-white/5 pb-4">
                  <FaMagic className="text-synapse-400" />
                  01_Synapse.module
                </h2>
                <div className="glass p-8 rounded-xl space-y-6 border border-white/5">
                  <p className="text-slate-400 text-lg leading-relaxed">
                    A web-based IDE for managing prompts as code. Features Git-like versioning, diff views, and hot-promotion to production.
                  </p>
                  <div className="grid md:grid-cols-2 gap-6">
                    <div className="bg-black/40 border border-white/5 p-6 rounded-lg">
                      <h4 className="font-bold text-synapse-300 mb-2 font-mono text-sm">Visual Editor</h4>
                      <p className="text-sm text-slate-400">Monaco-based editor with variable highlighting. Type <code className="bg-white/10 px-1 rounded text-primary-300">{`{{ var }}`}</code> to auto-generate form fields.</p>
                    </div>
                    <div className="bg-black/40 border border-white/5 p-6 rounded-lg">
                      <h4 className="font-bold text-synapse-300 mb-2 font-mono text-sm">Registry & Routing</h4>
                      <p className="text-sm text-slate-400">Prompts are served via CDN-like edge cache. Updates in Synapse propagate to agents in &lt;100ms.</p>
                    </div>
                  </div>
                </div>
              </section>

              {/* OpenAI Compatible */}
              <section id="openaicompatible">
                <h2 className="text-3xl font-bold mb-6 flex items-center gap-3 font-mono text-slate-200 border-b border-white/5 pb-4">
                  <FaCode className="text-blue-400" />
                  02_OpenAI_Compatible
                </h2>
                <div className="glass p-8 rounded-xl border border-white/5">
                  <p className="text-slate-400 mb-6">Drop-in replacement for any OpenAI SDK. Just change the <code className="bg-white/10 px-1 rounded text-primary-300">baseURL</code> — works with LangChain, LlamaIndex, and any OpenAI-compatible client.</p>
                  <div className="bg-gray-950 p-4 rounded-lg border border-white/10 overflow-x-auto shadow-inner">
                    <pre className="text-sm font-mono text-slate-300">
                      <code><span className="text-accent-400">const</span> client = <span className="text-accent-400">new</span> <span className="text-yellow-300">OpenAI</span>({'{'}<br/>  baseURL: <span className="text-green-400">&apos;https://gateway.internal/v1&apos;</span><br/>{'}'})</code>
                    </pre>
                  </div>
                </div>
              </section>

              {/* === RUN LAYER === */}

              {/* Nexus */}
              <section id="nexus">
                <h2 className="text-3xl font-bold mb-6 flex items-center gap-3 font-mono text-slate-200 border-b border-white/5 pb-4">
                  <FaSearch className="text-nexus-400" />
                  03_Nexus_Gateway
                </h2>
                <div className="glass p-8 rounded-xl space-y-6 border border-white/5">
                  <p className="text-slate-400 text-lg">
                    Centralized Retrieval-Augmented Generation. Nexus intercepts queries, fetches context from Vector DBs (Qdrant), and injects it before the LLM sees the prompt.
                  </p>
                  <ul className="space-y-3 text-slate-400 text-sm font-mono">
                    <li className="flex gap-3 items-center"><span className="text-nexus-400">✓</span> <strong>ACL Enforcement:</strong> Users only retrieve documents matching their JWT permissions.</li>
                    <li className="flex gap-3 items-center"><span className="text-nexus-400">✓</span> <strong>Hybrid Search:</strong> Combines dense vector similarity with keyword matching (BM25).</li>
                  </ul>
                </div>
              </section>

              {/* Hive Mind */}
              <section id="hivemind">
                <h2 className="text-3xl font-bold mb-6 flex items-center gap-3 font-mono text-slate-200 border-b border-white/5 pb-4">
                  <FaBrain className="text-purple-400" />
                  04_Hive_Mind
                </h2>
                <div className="glass p-8 rounded-xl space-y-6 text-slate-400 border border-white/5">
                  <p className="text-lg">For critical decisions, Hive Mind queries multiple frontier models (GPT-4, Claude 3, Gemini 1.5) and synthesizes their responses.</p>
                  <div className="p-4 bg-purple-900/10 border border-purple-500/20 rounded-lg font-mono text-sm text-purple-300">
                    x_neurogate_routing: &quot;consensus&quot;
                  </div>
                </div>
              </section>

              {/* NeuroGuard */}
              <section id="neuroguard">
                <h2 className="text-3xl font-bold mb-6 flex items-center gap-3 font-mono text-slate-200 border-b border-white/5 pb-4">
                  <FaShieldAlt className="text-cyan-400" />
                  05_NeuroGuard
                </h2>
                <div className="glass p-8 rounded-xl space-y-4 text-slate-400 border border-white/5">
                  <p className="text-lg">Zero-trust security layer. Detects PII (SSN, Credit Cards) and replaces them with reversible tokens (e.g., <code className="text-xs bg-red-500/10 text-red-400 px-1 rounded">&lt;CREDIT_CARD_1&gt;</code>) before sending to the LLM.</p>
                </div>
              </section>

              {/* === IMPROVE LAYER === */}

              {/* Cortex */}
              <section id="cortex">
                <h2 className="text-3xl font-bold mb-6 flex items-center gap-3 font-mono text-slate-200 border-b border-white/5 pb-4">
                  <FaChartLine className="text-cortex-400" />
                  06_Cortex_Eval
                </h2>
                <div className="glass p-8 rounded-xl space-y-6 border border-white/5">
                  <p className="text-slate-400 text-lg">
                    &quot;Unit Testing for Intelligence&quot;. Cortex runs offline evaluation suites against your agents using LLM-as-a-Judge.
                  </p>
                  <div className="flex gap-4 flex-wrap">
                    <span className="px-3 py-1 rounded-full bg-cortex-500/10 text-cortex-300 text-xs font-mono border border-cortex-500/30">Faithfulness</span>
                    <span className="px-3 py-1 rounded-full bg-cortex-500/10 text-cortex-300 text-xs font-mono border border-cortex-500/30">Relevance</span>
                    <span className="px-3 py-1 rounded-full bg-cortex-500/10 text-cortex-300 text-xs font-mono border border-cortex-500/30">Coherence</span>
                  </div>
                </div>
              </section>

              {/* Reinforce */}
              <section id="reinforce">
                <h2 className="text-3xl font-bold mb-6 flex items-center gap-3 font-mono text-slate-200 border-b border-white/5 pb-4">
                  <FaUserEdit className="text-reinforce-400" />
                  07_Reinforce_RLHF
                </h2>
                <div className="glass p-8 rounded-xl space-y-6 border border-white/5">
                  <p className="text-slate-400 text-lg">
                    Human-in-the-Loop (HitL) workflow. Domain experts review sampled logs, correct bad answers, and create &quot;Golden Traces&quot;.
                  </p>
                </div>
              </section>

              {/* Forge */}
              <section id="forge">
                <h2 className="text-3xl font-bold mb-6 flex items-center gap-3 font-mono text-slate-200 border-b border-white/5 pb-4">
                  <FaFire className="text-forge-400" />
                  08_Forge_Distill
                </h2>
                <div className="glass p-8 rounded-xl space-y-6 border border-white/5">
                  <p className="text-slate-400 text-lg">
                    Automated Fine-Tuning pipeline. Forge takes &quot;Golden Traces&quot; from Reinforce/Cortex and fine-tunes smaller, cheaper models (e.g., Llama 3 8B) to match GPT-4 performance.
                  </p>
                </div>
              </section>

              {/* === STORE LAYER === */}

              {/* Engram */}
              <section id="engram">
                <h2 className="text-3xl font-bold mb-6 flex items-center gap-3 font-mono text-slate-200 border-b border-white/5 pb-4">
                  <FaDatabase className="text-indigo-400" />
                  09_Engram_Store
                </h2>
                <div className="glass p-8 rounded-xl space-y-6 border border-white/5">
                  <p className="text-slate-400 text-lg leading-relaxed">
                    Enterprise-grade vector data store integrated directly into the NeuroGate kernel. Engram provides configurable collections, streaming ingestion, and top-K similarity search with compliance-ready architecture.
                  </p>
                  <div className="grid md:grid-cols-2 gap-6">
                    <div className="bg-black/40 border border-white/5 p-6 rounded-lg">
                      <h4 className="font-bold text-indigo-300 mb-2 font-mono text-sm">Collection Management</h4>
                      <p className="text-sm text-slate-400">Create and manage named collections with configurable dimensions, distance metrics (cosine, euclidean, dot), and metadata schemas.</p>
                    </div>
                    <div className="bg-black/40 border border-white/5 p-6 rounded-lg">
                      <h4 className="font-bold text-indigo-300 mb-2 font-mono text-sm">Ingestion Pipeline</h4>
                      <p className="text-sm text-slate-400">Stream vectors via REST, Kafka, or batch upload. Automatic deduplication and versioning for production workloads.</p>
                    </div>
                    <div className="bg-black/40 border border-white/5 p-6 rounded-lg">
                      <h4 className="font-bold text-indigo-300 mb-2 font-mono text-sm">Search Engine</h4>
                      <p className="text-sm text-slate-400">Top-K similarity search with optional metadata filtering. Sub-100ms P99 latency on collections up to 100M vectors.</p>
                    </div>
                    <div className="bg-black/40 border border-white/5 p-6 rounded-lg">
                      <h4 className="font-bold text-indigo-300 mb-2 font-mono text-sm">Compliance</h4>
                      <p className="text-sm text-slate-400">Architecture supports SOC 2, HIPAA, and GDPR requirements. Data encryption at rest and in transit with tenant isolation.</p>
                    </div>
                  </div>
                </div>
              </section>

            </div>
          </div>
        </div>
      </div>
    </main>
  )
}
