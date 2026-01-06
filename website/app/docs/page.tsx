'use client'

import { motion } from 'framer-motion'
import { FaGithub, FaArrowLeft, FaBook, FaRocket, FaCog, FaBrain, FaShieldAlt, FaCode, FaMemory, FaMagic, FaSearch, FaUserEdit, FaFire, FaChartLine } from 'react-icons/fa'
import Link from 'next/link'

export default function Docs() {
  return (
    <main className="min-h-screen bg-[url('/grid.svg')] bg-fixed selection:bg-primary-500/30">
      {/* Navigation */}
      <nav className="fixed top-0 w-full z-50 glass border-b border-white/5 bg-black/20 backdrop-blur-md">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-20">
            <Link href="/" className="flex items-center space-x-2 text-slate-400 hover:text-primary-400 transition-colors group">
              <FaArrowLeft className="group-hover:-translate-x-1 transition-transform" />
              <span>Return to OS</span>
            </Link>
            <a href="https://github.com/atharvajoshi/NeuroGate" target="_blank" rel="noopener noreferrer" className="hover:text-primary-400 transition-colors opacity-80 hover:opacity-100">
              <FaGithub className="text-xl" />
            </a>
          </div>
        </div>
      </nav>

      <div className="pt-32 pb-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* Header */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-center mb-16"
          >
            <div className="inline-block px-4 py-1.5 rounded-full bg-primary-500/10 border border-primary-500/20 text-primary-300 text-sm font-medium mb-6">
              NeuroKernel v1.0 Documentation
            </div>
            <h1 className="text-5xl md:text-6xl font-bold mb-6">
              Kernel <span className="text-transparent bg-clip-text bg-gradient-to-r from-primary-400 via-accent-400 to-primary-400">Reference</span>
            </h1>
            <p className="text-xl text-slate-400 max-w-3xl mx-auto">
              Technical specifications for the Agent-Native AI Kernel.
            </p>
          </motion.div>

          <div className="grid lg:grid-cols-[250px_1fr] gap-12">
            {/* Sidebar Navigation (Desktop) */}
            <div className="hidden lg:block sticky top-32 h-fit">
              <h3 className="font-bold text-slate-200 mb-4 px-2">Table of Contents</h3>
              <ul className="space-y-1 text-sm text-slate-400 border-l border-white/10">
                <li className="font-bold text-synapse-400 uppercase text-xs tracking-wider pl-4 mt-4 mb-2 opacity-80">Build</li>
                {['Synapse', 'Python SDK'].map((item) => (
                  <li key={item}>
                    <a href={`#${item.toLowerCase().replace(' ', '')}`} className="block py-2 px-4 hover:text-synapse-300 hover:border-l border-transparent hover:border-synapse-400 -ml-px transition-all">
                      {item}
                    </a>
                  </li>
                ))}

                <li className="font-bold text-nexus-400 uppercase text-xs tracking-wider pl-4 mt-6 mb-2 opacity-80">Run</li>
                {['Nexus', 'Hive Mind', 'NeuroGuard', 'Iron Gate'].map((item) => (
                  <li key={item}>
                    <a href={`#${item.toLowerCase().replace(' ', '')}`} className="block py-2 px-4 hover:text-nexus-300 hover:border-l border-transparent hover:border-nexus-400 -ml-px transition-all">
                      {item}
                    </a>
                  </li>
                ))}

                <li className="font-bold text-cortex-400 uppercase text-xs tracking-wider pl-4 mt-6 mb-2 opacity-80">Improve</li>
                {['Cortex', 'Reinforce', 'Forge'].map((item) => (
                  <li key={item}>
                    <a href={`#${item.toLowerCase().replace(' ', '')}`} className="block py-2 px-4 hover:text-cortex-300 hover:border-l border-transparent hover:border-cortex-400 -ml-px transition-all">
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
                <h2 className="text-3xl font-bold mb-8 flex items-center gap-3">
                  <FaMagic className="text-synapse-400" />
                  Synapse: Prompt Studio
                </h2>
                <div className="glass p-8 rounded-xl space-y-6">
                  <p className="text-slate-400">
                    A web-based IDE for managing prompts as code. Features Git-like versioning, diff views, and hot-promotion to production.
                  </p>
                  <div className="grid md:grid-cols-2 gap-6">
                    <div className="bg-synapse-900/10 border border-synapse-500/20 p-4 rounded-lg">
                      <h4 className="font-bold text-synapse-300 mb-2">Visual Editor</h4>
                      <p className="text-sm text-slate-400">Monaco-based editor with variable highlighting. Type <code className="bg-white/10 px-1 rounded">{`{{ var }}`}</code> to auto-generate form fields.</p>
                    </div>
                    <div className="bg-synapse-900/10 border border-synapse-500/20 p-4 rounded-lg">
                      <h4 className="font-bold text-synapse-300 mb-2">Registry & Routing</h4>
                      <p className="text-sm text-slate-400">Prompts are served via CDN-like edge cache. Updates in Synapse propagate to agents in &lt;100ms.</p>
                    </div>
                  </div>
                </div>
              </section>

              {/* SDK */}
              <section id="pythonsdk">
                <h2 className="text-3xl font-bold mb-8 flex items-center gap-3">
                  <FaCode className="text-blue-400" />
                  Python SDK
                </h2>
                <div className="glass p-8 rounded-xl">
                  <p className="text-slate-400 mb-4">Drop-in replacement for specific agent implementations.</p>
                  <div className="bg-[#0B0F19] p-4 rounded-lg border border-white/10 overflow-x-auto">
                    <pre className="text-sm font-mono text-slate-300">
                      <code>pip install neurogate-sdk</code>
                    </pre>
                  </div>
                </div>
              </section>

              {/* === RUN LAYER === */}

              {/* Nexus */}
              <section id="nexus">
                <h2 className="text-3xl font-bold mb-8 flex items-center gap-3">
                  <FaSearch className="text-nexus-400" />
                  Nexus: RAG Gateway
                </h2>
                <div className="glass p-8 rounded-xl space-y-6">
                  <p className="text-slate-400">
                    Centralized Retrieval-Augmented Generation. Nexus intercepts queries, fetches context from Vector DBs (Qdrant), and injects it before the LLM sees the prompt.
                  </p>
                  <ul className="space-y-2 text-slate-400 text-sm">
                    <li className="flex gap-2"><span className="text-nexus-400">✓</span> <strong>ACL Enforcement:</strong> Users only retrieve documents matching their JWT permissions.</li>
                    <li className="flex gap-2"><span className="text-nexus-400">✓</span> <strong>Hybrid Search:</strong> Combines dense vector similarity with keyword matching (BM25).</li>
                  </ul>
                </div>
              </section>

              {/* Hive Mind */}
              <section id="hivemind">
                <h2 className="text-3xl font-bold mb-8 flex items-center gap-3">
                  <FaBrain className="text-purple-400" />
                  Hive Mind: Consensus
                </h2>
                <div className="glass p-8 rounded-xl space-y-6 text-slate-400">
                  <p>For critical decisions, Hive Mind queries multiple frontier models (GPT-4, Claude 3, Gemini 1.5) and synthesizes their responses.</p>
                  <div className="p-4 bg-purple-500/10 border border-purple-500/20 rounded-lg font-mono text-sm">
                    x_neurogate_routing: &quot;consensus&quot;
                  </div>
                </div>
              </section>

              {/* NeuroGuard */}
              <section id="neuroguard">
                <h2 className="text-3xl font-bold mb-8 flex items-center gap-3">
                  <FaShieldAlt className="text-cyan-400" />
                  NeuroGuard: Security
                </h2>
                <div className="glass p-8 rounded-xl space-y-4 text-slate-400">
                  <p>Zero-trust security layer. Detects PII (SSN, Credit Cards) and replaces them with reversible tokens (e.g., <code className="text-xs">&lt;CREDIT_CARD_1&gt;</code>) before sending to the LLM.</p>
                </div>
              </section>

              {/* === IMPROVE LAYER === */}

              {/* Cortex */}
              <section id="cortex">
                <h2 className="text-3xl font-bold mb-8 flex items-center gap-3">
                  <FaChartLine className="text-cortex-400" />
                  Cortex: Evaluation Engine
                </h2>
                <div className="glass p-8 rounded-xl space-y-6">
                  <p className="text-slate-400">
                    &quot;Unit Testing for Intelligence&quot;. Cortex runs offline evaluation suites against your agents using LLM-as-a-Judge.
                  </p>
                  <div className="flex gap-4 flex-wrap">
                    <span className="px-3 py-1 rounded-full bg-cortex-500/20 text-cortex-300 text-sm border border-cortex-500/30">Faithfulness</span>
                    <span className="px-3 py-1 rounded-full bg-cortex-500/20 text-cortex-300 text-sm border border-cortex-500/30">Relevance</span>
                    <span className="px-3 py-1 rounded-full bg-cortex-500/20 text-cortex-300 text-sm border border-cortex-500/30">Coherence</span>
                  </div>
                </div>
              </section>

              {/* Reinforce */}
              <section id="reinforce">
                <h2 className="text-3xl font-bold mb-8 flex items-center gap-3">
                  <FaUserEdit className="text-reinforce-400" />
                  Reinforce: Human Feedback
                </h2>
                <div className="glass p-8 rounded-xl space-y-6">
                  <p className="text-slate-400">
                    Human-in-the-Loop (HitL) workflow. Domain experts review sampled logs, correct bad answers, and create &quot;Golden Traces&quot;.
                  </p>
                </div>
              </section>

              {/* Forge */}
              <section id="forge">
                <h2 className="text-3xl font-bold mb-8 flex items-center gap-3">
                  <FaFire className="text-forge-400" />
                  Forge: Distillation
                </h2>
                <div className="glass p-8 rounded-xl space-y-6">
                  <p className="text-slate-400">
                    Automated Fine-Tuning pipeline. Forge takes &quot;Golden Traces&quot; from Reinforce/Cortex and fine-tunes smaller, cheaper models (e.g., Llama 3 8B) to match GPT-4 performance.
                  </p>
                </div>
              </section>

            </div>
          </div>
        </div>
      </div>
    </main>
  )
}
