'use client'

import { motion } from 'framer-motion'
import { FaArrowDown, FaArrowRight, FaCheckCircle } from 'react-icons/fa'

export default function ArchitectureDiagram() {
  return (
    <div className="w-full">
      {/* User Request */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        className="flex flex-col items-center mb-8"
      >
        <div className="bg-gradient-to-r from-blue-500 to-blue-600 px-8 py-6 rounded-xl shadow-lg text-center min-w-[300px]">
          <div className="text-3xl mb-2">👤</div>
          <div className="font-bold text-lg mb-1">User Request</div>
          <div className="text-sm text-blue-100 italic">&quot;Email john@example.com about Q4...&quot;</div>
        </div>
        <motion.div
          animate={{ y: [0, 10, 0] }}
          transition={{ duration: 1.5, repeat: Infinity }}
          className="text-blue-400 text-3xl my-4"
        >
          ↓
        </motion.div>
      </motion.div>

      {/* Main Gateway Container */}
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        whileInView={{ opacity: 1, scale: 1 }}
        viewport={{ once: true }}
        className="glass p-8 rounded-2xl border-4 border-slate-700 mb-8 w-full max-w-4xl mx-auto"
      >
        <div className="text-center mb-10">
          <h3 className="text-2xl font-bold mb-2">
            🛡️ NeuroKernel AI Gateway
          </h3>
          <p className="text-sm text-slate-400">Java 21 Virtual Threads • Spring Boot</p>
        </div>

        <div className="grid md:grid-cols-2 gap-8 relative">

          {/* Left Column: Flow */}
          <div className="space-y-8">
            {/* Layer 1: NeuroGuard */}
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              transition={{ delay: 0.1 }}
              className="bg-gradient-to-br from-green-900/40 to-green-800/40 border border-green-500/50 rounded-xl p-6 relative overflow-hidden"
            >
              <div className="absolute top-0 right-0 p-2 opacity-20 text-6xl">🔒</div>
              <h4 className="text-xl font-bold text-green-400 mb-4 flex items-center gap-2">
                <span className="flex items-center justify-center w-6 h-6 rounded-full bg-green-500 text-black text-xs">1</span>
                NeuroGuard
              </h4>
              <div className="space-y-3">
                <div className="flex items-center gap-3 text-sm text-slate-300 bg-black/20 p-2 rounded">
                  <span>👁️</span> Holographic PII Detection
                </div>
                <div className="flex items-center gap-3 text-sm text-slate-300 bg-black/20 p-2 rounded">
                  <span>🎟️</span> Zero-Copy Tokenization
                </div>
              </div>
            </motion.div>

            {/* Arrow */}
            <div className="flex justify-center text-slate-600 text-xl">↓</div>

            {/* Layer 2: Semantic Cache */}
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              transition={{ delay: 0.2 }}
              className="bg-gradient-to-br from-purple-900/40 to-purple-800/40 border border-purple-500/50 rounded-xl p-6 relative overflow-hidden"
            >
              <div className="absolute top-0 right-0 p-2 opacity-20 text-6xl">💾</div>
              <h4 className="text-xl font-bold text-purple-400 mb-4 flex items-center gap-2">
                <span className="flex items-center justify-center w-6 h-6 rounded-full bg-purple-500 text-white text-xs">2</span>
                Semantic Cache
              </h4>
              <div className="flex justify-between items-center bg-black/20 p-3 rounded mb-2">
                <div className="text-sm text-slate-300">Qdrant Vector DB</div>
                <div className="text-xs bg-purple-500/20 text-purple-300 px-2 py-1 rounded">Hit?</div>
              </div>
            </motion.div>

            {/* Arrow */}
            <div className="flex justify-center text-slate-600 text-xl">↓</div>

            {/* Layer 3: Hive Mind */}
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              transition={{ delay: 0.3 }}
              className="bg-gradient-to-br from-indigo-900/40 to-indigo-800/40 border border-indigo-500/50 rounded-xl p-6 relative overflow-hidden"
            >
              <div className="absolute top-0 right-0 p-2 opacity-20 text-6xl">🧠</div>
              <h4 className="text-xl font-bold text-indigo-400 mb-4 flex items-center gap-2">
                <span className="flex items-center justify-center w-6 h-6 rounded-full bg-indigo-500 text-white text-xs">3</span>
                Hive Mind
              </h4>
              <div className="bg-black/20 p-3 rounded">
                <div className="text-sm font-semibold text-slate-200 mb-2">Consensus Protocol</div>
                <div className="flex gap-2 justify-center">
                  <span className="px-2 py-1 bg-blue-500/20 text-blue-300 rounded text-xs border border-blue-500/30">GPT-4</span>
                  <span className="px-2 py-1 bg-orange-500/20 text-orange-300 rounded text-xs border border-orange-500/30">Claude</span>
                  <span className="px-2 py-1 bg-cyan-500/20 text-cyan-300 rounded text-xs border border-cyan-500/30">Gemini</span>
                </div>
              </div>
            </motion.div>
          </div>

          {/* Right Column: Flywheel & Feedback */}
          <div className="flex flex-col justify-center space-y-4">
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              transition={{ delay: 0.4 }}
              className="border-l-2 border-dashed border-slate-700 pl-8 ml-4 h-full flex flex-col justify-center relative"
            >
              {/* Flywheel Node */}
              <div className="bg-gradient-to-br from-amber-900/20 to-amber-800/20 border border-amber-500/30 rounded-xl p-6 mb-8">
                <h4 className="text-lg font-bold text-amber-400 mb-2">Data Flywheel 🌪️</h4>
                <p className="text-xs text-slate-400 mb-3">
                  Analyzing every interaction for quality and correctness.
                </p>
                <div className="bg-amber-950/40 border border-amber-500/20 p-3 rounded text-xs text-amber-200 font-mono">
                  &gt; Extract &quot;Golden Traces&quot;<br />
                  &gt; Fine-tune SLM<br />
                  &gt; Update Neural Weights
                </div>
              </div>

              {/* Pulse Node */}
              <div className="bg-gradient-to-br from-red-900/20 to-red-800/20 border border-red-500/30 rounded-xl p-6 mb-8">
                <h4 className="text-lg font-bold text-red-400 mb-2">Pulse Monitor 💓</h4>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="bg-black/30 p-2 rounded text-slate-300">Latency: 45ms</div>
                  <div className="bg-black/30 p-2 rounded text-slate-300">Cost: $0.002</div>
                  <div className="bg-black/30 p-2 rounded text-slate-300">Errors: 0%</div>
                  <div className="bg-black/30 p-2 rounded text-slate-300">Tokens: 450</div>
                </div>
              </div>

              {/* Engram Vector Store Node */}
              <div className="bg-gradient-to-br from-indigo-900/20 to-indigo-800/20 border border-indigo-500/30 rounded-xl p-6">
                <h4 className="text-lg font-bold text-indigo-400 mb-2">Engram Vector Store 🧬</h4>
                <div className="space-y-2 text-xs">
                  <div className="bg-black/30 p-2 rounded text-slate-300 flex items-center gap-2">
                    <span className="text-indigo-400">▸</span> Collection Manager
                  </div>
                  <div className="bg-black/30 p-2 rounded text-slate-300 flex items-center gap-2">
                    <span className="text-indigo-400">▸</span> Top-K Search
                  </div>
                </div>
              </div>
            </motion.div>
          </div>
        </div>
      </motion.div>

      {/* Response Arrow */}
      <motion.div
        initial={{ opacity: 0 }}
        whileInView={{ opacity: 1 }}
        viewport={{ once: true }}
        className="flex flex-col items-center"
      >
        <div className="h-12 w-0.5 bg-gradient-to-b from-slate-700 to-green-500/50"></div>
        <div className="bg-green-500/10 text-green-400 px-6 py-2 rounded-full border border-green-500/30 text-sm font-bold flex items-center gap-2 mt-4">
          <FaCheckCircle />
          Synthesized Response Delivered
        </div>
      </motion.div>
    </div>
  )
}
