'use client'

import { motion } from 'framer-motion'
import { FaGithub, FaShieldAlt, FaRocket, FaCheckCircle, FaPlay, FaBrain, FaNetworkWired, FaLock, FaBook, FaBolt, FaCode, FaSearch, FaClipboardCheck, FaUserEdit, FaFire, FaHammer, FaChartLine, FaMagic } from 'react-icons/fa'
import { SiSpringboot, SiKubernetes, SiDocker, SiPrometheus, SiGrafana, SiRedis, SiApachekafka, SiApachespark } from 'react-icons/si'
import { BiSolidServer, BiTestTube } from 'react-icons/bi'
import Link from 'next/link'
import AnimatedCounter from './components/AnimatedCounter'
import ScrollToTop from './components/ScrollToTop'
import CodeBlock from './components/CodeBlock'
import FeatureCard from './components/FeatureCard'
import ArchitectureDiagram from './components/ArchitectureDiagram'

export default function Home() {
  return (
    <main className="min-h-screen selection:bg-primary-500/30">
      {/* Navigation */}
      <nav className="fixed top-0 w-full z-50 glass border-b border-white/5 bg-black/20 backdrop-blur-md">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-20">
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              className="flex items-center space-x-2 cursor-pointer group"
              onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
            >
              <div className="relative">
                <div className="absolute inset-0 bg-primary-500 blur-lg opacity-50 group-hover:opacity-100 transition-opacity" />
                <BiSolidServer className="text-3xl text-primary-400 relative z-10" />
              </div>
              <span className="text-xl font-bold tracking-tight">Neuro<span className="text-primary-400">Gate</span></span>
            </motion.div>
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              className="hidden md:flex items-center space-x-8"
            >
              <Link href="#features" className="text-sm font-medium hover:text-primary-400 transition-colors">Platform</Link>
              <Link href="/debugger" className="text-sm font-medium hover:text-primary-400 transition-colors">Debugger</Link>
              <Link href="/playground" className="text-sm font-medium hover:text-primary-400 transition-colors">Terminal</Link>
              <Link href="/docs" className="text-sm font-medium hover:text-primary-400 transition-colors">Docs</Link>
              <a href="https://github.com/atharvajoshi/NeuroGate" target="_blank" rel="noopener noreferrer" className="hover:text-primary-400 transition-colors opacity-80 hover:opacity-100">
                <FaGithub className="text-xl" />
              </a>
            </motion.div>
          </div>
        </div>
      </nav>

      <ScrollToTop />

      {/* Hero Section */}
      <section className="relative min-h-screen flex items-center justify-center overflow-hidden pt-20">
        {/* Deep Space Background Effects */}
        <div className="absolute inset-0 bg-[url('/grid.svg')] bg-center [mask-image:linear-gradient(180deg,white,rgba(255,255,255,0))]" />

        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 0.4, scale: 1 }}
          transition={{ duration: 3, repeat: Infinity, repeatType: "reverse" }}
          className="absolute top-0 left-1/2 -translate-x-1/2 w-[600px] h-[600px] bg-primary-500/20 rounded-full blur-[100px] pointer-events-none"
        />
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 0.3 }}
          transition={{ duration: 5, delay: 1, repeat: Infinity, repeatType: "reverse" }}
          className="absolute bottom-0 right-0 w-[500px] h-[500px] bg-accent-500/10 rounded-full blur-[100px] pointer-events-none"
        />

        <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
            className="mb-8 flex justify-center"
          >
            <span className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-primary-500/10 border border-primary-500/20 text-primary-300 text-sm font-medium backdrop-blur-sm">
              <span className="w-2 h-2 rounded-full bg-green-400 animate-pulse" />
              The AI Kernel is Live
            </span>
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.2 }}
            className="text-5xl sm:text-7xl md:text-8xl font-bold mb-8 leading-tight tracking-tight"
          >
            Build. Run. <span className="text-transparent bg-clip-text bg-gradient-to-r from-primary-400 via-accent-400 to-primary-400 animate-gradient-x">Improve.</span><br />
            <span className="text-3xl sm:text-5xl md:text-6xl text-slate-300 font-semibold mt-4 block">The Open Source Agent Platform.</span>
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.4 }}
            className="text-lg sm:text-xl md:text-2xl text-slate-400 mb-12 max-w-3xl mx-auto leading-relaxed"
          >
            Stop building spaghetti code. NeuroGate provides the <span className="text-slate-200">RAG Gateway</span>, <span className="text-slate-200">Evaluation Engine</span>, and <span className="text-slate-200">Fine-Tuning Loop</span> you need to scale autonomous agents.
          </motion.p>

          <div className="flex flex-col sm:flex-row gap-6 justify-center items-center mb-20">
            <Link href="/playground">
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className="px-8 py-4 bg-primary-600 hover:bg-primary-500 rounded-xl font-semibold text-lg shadow-[0_0_40px_-10px_rgba(139,92,246,0.5)] transition-all flex items-center gap-3 group"
              >
                <FaPlay className="text-sm group-hover:translate-x-1 transition-transform" />
                Launch Terminal
              </motion.button>
            </Link>
            <a href="https://github.com/atharvaj77/NeuroGate" target="_blank" rel="noopener noreferrer">
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className="px-8 py-4 glass rounded-xl font-semibold text-lg hover:bg-white/5 transition-all flex items-center gap-3 border border-white/10"
              >
                <FaGithub className="text-white" />
                View Source
              </motion.button>
            </a>
          </div>

          <motion.div
            initial={{ opacity: 0, y: 40 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.8 }}
            className="grid grid-cols-2 md:grid-cols-4 gap-8 max-w-5xl mx-auto border-t border-white/5 pt-12"
          >
            {[
              { value: 5, suffix: '', label: 'Kernel Modules' },
              { value: 99, suffix: '%', label: 'Cache Hit Ratio' },
              { value: 1, suffix: 'ms', label: 'L1 Latency' },
              { value: 100, suffix: '%', label: 'Open Source' }
            ].map((stat, index) => (
              <div key={index} className="text-center group hover:bg-white/5 p-4 rounded-lg transition-colors">
                <div className="text-3xl md:text-5xl font-bold text-white mb-2 group-hover:text-primary-400 transition-colors font-mono">
                  <AnimatedCounter end={stat.value} suffix={stat.suffix} />
                </div>
                <div className="text-sm text-slate-500 font-medium tracking-wider uppercase">{stat.label}</div>
              </div>
            ))}
          </motion.div>
        </div>
      </section>

      {/* 2. Platform Features Section (Categorized) */}
      <section id="features" className="py-32 relative">
        <div className="absolute inset-0 bg-gradient-to-b from-transparent via-primary-950/5 to-transparent" />
        <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-20"
          >
            <h2 className="text-4xl md:text-5xl font-bold mb-6">
              The <span className="text-primary-400">Agent Kernel</span>
            </h2>
            <p className="text-xl text-slate-400 max-w-2xl mx-auto">
              Everything you need to go from &quot;Prototype&quot; to &quot;Production&quot;.
            </p>
          </motion.div>

          {/* LAYER 1: BUILD */}
          <div className="mb-24">
            <h3 className="text-2xl font-bold mb-8 flex items-center gap-3 text-slate-200">
              <FaCode className="text-synapse-400" />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-synapse-400 to-primary-400">
                1. Build & Design
              </span>
            </h3>
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
              <FeatureCard
                icon={<FaMagic className="text-5xl" />}
                title="Synapse"
                description="Visual Prompt Engineering Studio."
                features={[
                  'Version Control (Git-like)',
                  'Interactive Playground',
                  'Compare V1 vs V2',
                  'Promote to Production'
                ]}
                gradient="from-synapse-500 to-purple-500"
                delay={0}
              />
              <FeatureCard
                icon={<FaBook className="text-5xl" />}
                title="Python SDK"
                description="Native integration for your Agent code."
                features={[
                  'Type-safe Agent API',
                  'Async/Await Support',
                  'One-line integration',
                  'Jupyter Compatible'
                ]}
                gradient="from-blue-400 to-cyan-400"
                delay={0.1}
              />
            </div>
          </div>

          {/* LAYER 2: RUN */}
          <div className="mb-24">
            <h3 className="text-2xl font-bold mb-8 flex items-center gap-3 text-slate-200">
              <BiSolidServer className="text-nexus-400" />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-nexus-400 to-blue-400">
                2. Run & Secure
              </span>
            </h3>
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
              <FeatureCard
                icon={<FaSearch className="text-5xl" />}
                title="Nexus"
                description="Centralized RAG Gateway as a Service."
                features={[
                  'Context Injection',
                  'Qdrant / Vector DB Integration',
                  'ACL (Permission Awareness)',
                  'Hybrid Search'
                ]}
                gradient="from-nexus-500 to-blue-500"
                delay={0}
              />
              <FeatureCard
                icon={<FaBrain className="text-5xl" />}
                title="Hive Mind"
                description="Collective Intelligence Consensus."
                features={[
                  '3-Model Voting (GPT+Claude)',
                  'Neural Routing',
                  'Self-Correction',
                  'Confidence Scoring'
                ]}
                gradient="from-violet-500 to-fuchsia-500"
                delay={0.1}
              />
              <FeatureCard
                icon={<FaShieldAlt className="text-5xl" />}
                title="NeuroGuard"
                description="Active Defense & PII Protection layer."
                features={[
                  'Holographic PII Masking',
                  'Zero-Copy Tokenization',
                  'Jailbreak Defense',
                  'Audit Logging'
                ]}
                gradient="from-cyan-500 to-blue-500"
                delay={0.2}
              />
              <FeatureCard
                icon={<FaNetworkWired className="text-5xl" />}
                title="Iron Gate"
                description="Resilience patterns for unshakeable uptime."
                features={[
                  'Circuit Breakers',
                  'Concurrent Hedging',
                  'L4 Semantic Caching',
                  'Rate Limiting'
                ]}
                gradient="from-emerald-500 to-teal-500"
                delay={0.3}
              />
            </div>
          </div>

          {/* LAYER 3: IMPROVE */}
          <div>
            <h3 className="text-2xl font-bold mb-8 flex items-center gap-3 text-slate-200">
              <FaChartLine className="text-cortex-400" />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-cortex-400 to-forge-400">
                3. Measure & Improve
              </span>
            </h3>
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
              <FeatureCard
                icon={<BiTestTube className="text-5xl" />}
                title="Cortex"
                description="Automated Evaluation & Testing Engine."
                features={[
                  'LLM-as-a-Judge',
                  'Faithfulness Scoring',
                  'Regression Testing',
                  'CI/CD for Agents'
                ]}
                gradient="from-cortex-500 to-orange-500"
                delay={0}
              />
              <FeatureCard
                icon={<FaUserEdit className="text-5xl" />}
                title="Reinforce"
                description="Human-in-the-Loop Feedback workflow."
                features={[
                  'Golden Trace Curation',
                  'Expert Review Queue',
                  'Rewrite & Fix',
                  'Feedback Dataset Export'
                ]}
                gradient="from-reinforce-500 to-green-500"
                delay={0.1}
              />
              <FeatureCard
                icon={<FaFire className="text-5xl" />}
                title="Forge"
                description="Auto-Distillation & Fine-Tuning."
                features={[
                  'Teacher (GPT-4) -> Student (Llama)',
                  'Automated Training Jobs',
                  'Cost Reduction',
                  'Model Registry'
                ]}
                gradient="from-forge-500 to-red-600"
                delay={0.2}
              />
            </div>
          </div>

        </div>
      </section>

      {/* Architecture Section */}
      <section id="architecture" className="py-24 relative overflow-hidden">
        <div className="absolute inset-0 bg-primary-950/10" />
        <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <h2 className="text-4xl md:text-5xl font-bold mb-6">
              How the <span className="text-transparent bg-clip-text bg-gradient-to-r from-primary-400 to-accent-400">Kernel Works</span>
            </h2>
            <p className="text-xl text-slate-400 max-w-3xl mx-auto">
              Follow the journey of a user request through the NeuroGate protection and routing layers.
            </p>
          </motion.div>

          {/* Diagram Container */}
          <div className="flex justify-center">
            <ArchitectureDiagram />
          </div>
        </div>
      </section>

      {/* Integration Code Comparison */}
      <section className="py-24 bg-black/40 border-y border-white/5">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid lg:grid-cols-2 gap-16 items-center">
            <motion.div
              initial={{ opacity: 0, x: -50 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
            >
              <h2 className="text-4xl md:text-5xl font-bold mb-6">
                Drop-in <span className="text-primary-400">Compatibility</span>
              </h2>
              <p className="text-xl text-slate-400 mb-8 leading-relaxed">
                NeuroGate intercepts standard OpenAI API calls. No SDK rewrite required. Just change the <code className="text-primary-300 bg-primary-900/20 px-2 py-1 rounded text-sm">baseURL</code>.
              </p>
              <ul className="space-y-4 text-lg text-slate-300">
                <li className="flex items-center gap-3">
                  <FaCheckCircle className="text-green-400" />
                  <span>Works with LangChain, LlamaIndex, & AutoGen</span>
                </li>
                <li className="flex items-center gap-3">
                  <FaCheckCircle className="text-green-400" />
                  <span>Transparent Caching & PII Protection</span>
                </li>
                <li className="flex items-center gap-3">
                  <FaCheckCircle className="text-green-400" />
                  <span>&lt; 10ms processing overhead</span>
                </li>
              </ul>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, x: 50 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              className="relative"
            >
              <div className="absolute inset-0 bg-primary-500/10 blur-3xl rounded-full" />
              <div className="relative glass-card p-0 overflow-hidden border-primary-500/30">
                <div className="flex border-b border-white/10 bg-black/40">
                  <div className="px-6 py-3 text-sm font-medium border-b-2 border-transparent text-slate-500 hover:text-slate-300 cursor-not-allowed">Standard</div>
                  <div className="px-6 py-3 text-sm font-medium border-b-2 border-primary-500 text-primary-400 bg-primary-500/5">NeuroGate</div>
                </div>
                <div className="p-6 overflow-x-auto">
                  <code className="text-sm font-mono leading-relaxed">
                    <span className="text-purple-400">const</span> client = <span className="text-purple-400">new</span> <span className="text-yellow-300">OpenAI</span>({'{'}<br />
                    &nbsp;&nbsp;<span className="text-slate-400">{`// Point to your NeuroGate instance`}</span><br />
                    &nbsp;&nbsp;baseURL: <span className="text-green-400">&apos;https://gateway.internal/v1&apos;</span>,<br />
                    &nbsp;&nbsp;apiKey: <span className="text-green-400">&apos;sk-neurogate-key&apos;</span><br />
                    {'}'});<br /><br />
                    <span className="text-slate-400">{`// ⚡️ Now protected by NeuroGuard & Hive Mind`}</span><br />
                    <span className="text-purple-400">await</span> client.chat.completions.<span className="text-blue-400">create</span>({'{'}...{'}'});
                  </code>
                </div>
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* Open Source Philosophy */}
      <section className="py-32 relative">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-20"
          >
            <h2 className="text-4xl md:text-5xl font-bold mb-6">
              Open Source <span className="text-accent-400">Research</span>
            </h2>
            <p className="text-xl text-slate-400 max-w-3xl mx-auto">
              NeuroGate is an experimental project exploring the future of agentic infrastructure.
              We believe in transparent, improved, and safe AI for everyone.
            </p>
          </motion.div>

          <div className="grid md:grid-cols-2 gap-8">
            {/* Community */}
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              className="glass-card p-10 relative overflow-hidden group"
            >
              <div className="absolute top-0 right-0 p-8 opacity-10 group-hover:opacity-20 transition-opacity">
                <FaGithub className="text-9xl text-accent-400" />
              </div>
              <h3 className="text-3xl font-bold mb-4 text-accent-300">Community Driven</h3>
              <p className="text-lg text-slate-400 mb-8 h-20">
                Built by developers, for developers. MIT Licensed and free forever.
              </p>
              <ul className="space-y-4">
                <li className="flex items-start gap-3">
                  <FaCheckCircle className="text-accent-500 mt-1" />
                  <div>
                    <strong className="text-slate-200 block">Transparent Security</strong>
                    <span className="text-slate-500 text-sm">Audit the code yourself. No black boxes.</span>
                  </div>
                </li>
                <li className="flex items-start gap-3">
                  <FaCheckCircle className="text-accent-500 mt-1" />
                  <div>
                    <strong className="text-slate-200 block">Extensible Architecture</strong>
                    <span className="text-slate-500 text-sm">Write your own plugins and routing strategies.</span>
                  </div>
                </li>
              </ul>
            </motion.div>

            {/* Research */}
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              className="glass-card p-10 relative overflow-hidden group"
            >
              <div className="absolute top-0 right-0 p-8 opacity-10 group-hover:opacity-20 transition-opacity">
                <FaBrain className="text-9xl text-primary-400" />
              </div>
              <h3 className="text-3xl font-bold mb-4 text-primary-300">Research Focus</h3>
              <p className="text-lg text-slate-400 mb-8 h-20">
                Pushing the boundaries of Multi-Agent Systems and Consensus protocols.
              </p>
              <ul className="space-y-4">
                <li className="flex items-start gap-3">
                  <FaCheckCircle className="text-primary-500 mt-1" />
                  <div>
                    <strong className="text-slate-200 block">Experimental Kernels</strong>
                    <span className="text-slate-500 text-sm">Testing new theories in agent orchestration.</span>
                  </div>
                </li>
                <li className="flex items-start gap-3">
                  <FaCheckCircle className="text-primary-500 mt-1" />
                  <div>
                    <strong className="text-slate-200 block">Academic Integrity</strong>
                    <span className="text-slate-500 text-sm"> reproducible benchmarks and datasets.</span>
                  </div>
                </li>
              </ul>
            </motion.div>
          </div>
        </div>
      </section>

      {/* FAQ */}
      <section className="py-24 bg-black/20">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-3xl font-bold mb-12 text-center">Frequently Asked Questions</h2>
          <div className="space-y-6">
            {[
              { q: 'Does NeuroGate store my data?', a: 'No. NeuroGate is a pass-through kernel. Data is processed in-memory for PII detection and routing, then immediately discarded. Logs can be configured to be redacted or disabled suitable for HIPAA compliance.' },
              { q: 'What is the latency overhead?', a: 'Minimal. The core routing and PII detection layer adds approximately 5-15ms to the request. However, semantic caching often reduces overall total latency by 90% for repeated queries.' },
              { q: 'Can I self-host?', a: 'Yes. NeuroGate is distributed as a Docker container and Helm chart. It allows you to own your infrastructure completely.' }
            ].map((faq, i) => (
              <div key={i} className="glass p-6 rounded-xl border border-white/5">
                <h4 className="font-bold text-lg mb-2 text-slate-200">{faq.q}</h4>
                <p className="text-slate-400">{faq.a}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Interactive Demo Section */}
      <section id="demo" className="py-32 relative bg-black/40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid lg:grid-cols-2 gap-16 items-center">
            <motion.div
              initial={{ opacity: 0, x: -50 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
            >
              <h2 className="text-4xl md:text-5xl font-bold mb-8">
                Visualizing <span className="text-transparent bg-clip-text bg-gradient-to-r from-red-400 to-orange-400">PII Defense</span>
              </h2>
              <p className="text-xl text-slate-400 mb-8 leading-relaxed">
                See how NeuroGuard intercepts sensitive data before it leaves your network.
                Using context-aware regex and reversible tokenization, we protect user privacy without breaking agent context.
              </p>

              <div className="space-y-6">
                <div className="flex items-center gap-4 p-4 rounded-xl bg-white/5 border border-white/10">
                  <div className="w-12 h-12 rounded-full bg-red-500/20 flex items-center justify-center text-red-400">
                    <FaLock />
                  </div>
                  <div>
                    <h4 className="font-bold text-lg">Zero Trust Architecture</h4>
                    <p className="text-sm text-slate-400">Data is sanitized at the edge.</p>
                  </div>
                </div>
                <div className="flex items-center gap-4 p-4 rounded-xl bg-white/5 border border-white/10">
                  <div className="w-12 h-12 rounded-full bg-green-500/20 flex items-center justify-center text-green-400">
                    <FaCheckCircle />
                  </div>
                  <div>
                    <h4 className="font-bold text-lg">Reversible Tokenization</h4>
                    <p className="text-sm text-slate-400">LLMs see tokens, users see data.</p>
                  </div>
                </div>
              </div>
            </motion.div>

            <div className="relative">
              <div className="absolute inset-0 bg-gradient-to-r from-primary-500 to-accent-500 blur-[100px] opacity-20" />
              <div className="relative glass-card p-6 md:p-8 border-primary-500/30">
                <div className="flex items-center justify-between mb-6">
                  <div className="flex gap-2">
                    <div className="w-3 h-3 rounded-full bg-red-500" />
                    <div className="w-3 h-3 rounded-full bg-yellow-500" />
                    <div className="w-3 h-3 rounded-full bg-green-500" />
                  </div>
                  <div className="text-xs text-slate-500 font-mono">neuroguard_live_stream</div>
                </div>

                <div className="space-y-4 font-mono text-sm">
                  <div className="p-4 rounded bg-red-900/20 border border-red-500/30">
                    <div className="text-red-400 mb-2 text-xs uppercase tracking-wider">Incoming Request</div>
                    <p className="text-slate-300">
                      &quot;Book a flight for <span className="text-red-400 bg-red-900/40 px-1 rounded">sarah@acme.com</span> using card <span className="text-red-400 bg-red-900/40 px-1 rounded">4421-5555-1234-9000</span>.&quot;
                    </p>
                  </div>

                  <div className="flex justify-center text-slate-500">
                    <FaBolt className="animate-pulse" />
                  </div>

                  <div className="p-4 rounded bg-green-900/20 border border-green-500/30">
                    <div className="text-green-400 mb-2 text-xs uppercase tracking-wider">LLM Payload (Sanitized)</div>
                    <p className="text-slate-300">
                      &quot;Book a flight for <span className="text-green-400 bg-green-900/40 px-1 rounded">&lt;EMAIL_1&gt;</span> using card <span className="text-green-400 bg-green-900/40 px-1 rounded">&lt;CREDIT_CARD_1&gt;</span>.&quot;
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Tech Stack */}
      <section className="py-24 border-y border-white/5 bg-black/40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <p className="text-center text-sm text-slate-500 uppercase tracking-widest mb-12">Powered by Modern Infrastructure</p>
          <div className="flex flex-wrap justify-center gap-12 md:gap-20 opacity-70 grayscale hover:grayscale-0 transition-all duration-500">
            {[SiSpringboot, SiKubernetes, SiDocker, SiRedis, SiApachekafka, SiApachespark, SiPrometheus].map((Icon, i) => (
              <Icon key={i} className="text-5xl hover:scale-110 transition-transform" />
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-32 relative text-center">
        <div className="absolute inset-0 bg-gradient-radial from-primary-900/20 to-slate-950 -z-10" />
        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true }}
          className="max-w-4xl mx-auto px-4"
        >
          <h2 className="text-4xl md:text-6xl font-bold mb-8">Contributing</h2>
          <p className="text-xl text-slate-400 mb-12">
            NeuroGate is a community effort. We welcome PRs, issues, and discussions.
          </p>
          <div className="flex justify-center gap-6">
            <a href="https://github.com/atharvaj77/NeuroGate" target="_blank" rel="noopener noreferrer">
              <button className="px-8 py-4 bg-white text-black rounded-xl font-bold text-lg hover:bg-slate-200 transition-colors flex items-center gap-3">
                <FaGithub className="text-xl" />
                Start Contributing
              </button>
            </a>
            <Link href="/docs">
              <button className="px-8 py-4 glass text-white rounded-xl font-bold text-lg hover:bg-white/10 transition-colors flex items-center gap-3 border border-white/20">
                <FaBook className="text-xl" />
                Read the Docs
              </button>
            </Link>
          </div>
        </motion.div>
      </section>

      {/* Footer */}
      <footer className="py-8 border-t border-white/10 text-center text-slate-600 text-sm">
        <p>&copy; {new Date().getFullYear()} NeuroGate Research. Open Source MIT License.</p>
      </footer>
    </main>
  )
}
