'use client';

import { motion } from 'framer-motion';
import {
    FaBug, FaChartPie, FaBalanceScale, FaHandshake,
    FaSearch, FaNetworkWired, FaBolt, FaShieldAlt,
    FaBrain, FaUserMd, FaCoins, FaRedo,
    FaLock, FaEye, FaSkull,
    FaCheckDouble, FaFlask, FaHistory,
    FaGraduationCap, FaThumbsUp, FaFire,
    FaSitemap, FaMoneyBillWave,
    FaDatabase, FaServer, FaCertificate
} from 'react-icons/fa';
import Link from 'next/link';
import { BiSolidServer } from 'react-icons/bi';
import ScrollToTop from '../components/ScrollToTop';

// -- Data --
const modules = [
    {
        id: 'synapse',
        name: 'Synapse',
        tagline: 'Visual Prompt Engineering',
        color: 'from-synapse-400 to-purple-400',
        bg: 'bg-synapse-950/20',
        scenarios: [
            {
                icon: <FaFire className="text-3xl text-red-400" />,
                title: "Crisis Management",
                desc: "Your support bot starts hallucinating refunds. With Synapse, you don't redeploy code. You click 'Rollback' to v1.2.0, and the fix propagates globally in < 5 seconds.",
                tags: ['Hot-Swap', 'Rollback']
            },
            {
                icon: <FaChartPie className="text-3xl text-blue-400" />,
                title: "A/B Testing",
                desc: "Does a 'Friendly' persona sell more than a 'Formal' one? Deploy both prompt versions to 50% of traffic and let Cortex track the conversion rates.",
                tags: ['Growth', 'Analytics']
            },
            {
                icon: <FaBalanceScale className="text-3xl text-emerald-400" />,
                title: "Compliance Audit",
                desc: "In regulated industries, you need to prove exactly what instructions the Model had on Jan 8th. Synapse provides an immutable SHA-256 log of every prompt state.",
                tags: ['FinTech', 'Legal']
            }
        ]
    },
    {
        id: 'neuroguard',
        name: 'NeuroGuard',
        tagline: 'Active Defense',
        color: 'from-cyan-400 to-blue-400',
        bg: 'bg-cyan-950/20',
        scenarios: [
            {
                icon: <FaLock className="text-3xl text-red-400" />,
                title: "GDPR Masking",
                desc: "Holographic PII Masking detects emails/CCs and replaces them with tokens BEFORE they leave your region. The LLM never sees user data.",
                tags: ['Privacy', 'Compliance']
            },
            {
                icon: <FaSkull className="text-3xl text-orange-400" />,
                title: "Jailbreak Defense",
                desc: "Heuristic scanners block 'DAN' (Do Anything Now) attacks and prompt injection attempts in real-time, protecting your brand reputation.",
                tags: ['Security', 'Firewall']
            },
            {
                icon: <FaEye className="text-3xl text-slate-400" />,
                title: "Shadow AI Visibility",
                desc: "Discover which employees are using which models. NeuroGuard logs every interaction, giving IT full visibility into AI usage across the org.",
                tags: ['Audit', 'Governance']
            }
        ]
    },
    {
        id: 'hivemind',
        name: 'Hive Mind',
        tagline: 'Consensus Engine',
        color: 'from-violet-400 to-fuchsia-400',
        bg: 'bg-violet-950/20',
        scenarios: [
            {
                icon: <FaUserMd className="text-3xl text-teal-400" />,
                title: "Critical Decisions",
                desc: "For medical diagnosis, one model isn't enough. Hive Mind queries GPT-4, Claude 3, and Med-PaLM simultaneously, only accepting the answer if 2/3 agree.",
                tags: ['Voting', 'Safety']
            },
            {
                icon: <FaCoins className="text-3xl text-amber-400" />,
                title: "Cost Optimization",
                desc: "Neural Routing analyzes complexity. Simple 'Hello' goes to cheap Llama 3. Complex 'Analyze this P&L' goes to expensive GPT-4. Save 40% on bills.",
                tags: ['Routing', 'FinOps']
            },
            {
                icon: <FaRedo className="text-3xl text-rose-400" />,
                title: "Self-Correction",
                desc: "If the Primary Model generates invalid JSON, Hive Mind intercepts the error, adds the stack trace to the context, and retries with a 'Fixer' model.",
                tags: ['Resilience', 'Auto-Heal']
            }
        ]
    },
    {
        id: 'nexus',
        name: 'Nexus',
        tagline: 'RAG Gateway',
        color: 'from-nexus-400 to-blue-400',
        bg: 'bg-nexus-950/20',
        scenarios: [
            {
                icon: <FaSearch className="text-3xl text-cyan-400" />,
                title: "Legal Discovery",
                desc: "Pure vector search misses specific case numbers. Nexus Hybrid Search combines semantic understanding with keyword precision to find case law instanty.",
                tags: ['Hybrid Search', 'Qdrant']
            },
            {
                icon: <FaNetworkWired className="text-3xl text-indigo-400" />,
                title: "Multi-Tenant SaaS",
                desc: "Context Injection ensures User A never sees User B's documents. The Gateway automatically tags every vector query with the tenant ID from the JWT.",
                tags: ['Security', 'ACL']
            },
            {
                icon: <FaBolt className="text-3xl text-yellow-400" />,
                title: "High-Speed Recall",
                desc: "80% of user questions are repeated. Semantic Caching (Redis) serves instant answers for 'How do I reset my password?' without hitting the LLM.",
                tags: ['Caching', 'Latency']
            }
        ]
    },
    {
        id: 'agentops',
        name: 'AgentOps',
        tagline: 'Observability Platform',
        color: 'from-indigo-400 to-violet-400',
        bg: 'bg-indigo-950/20',
        scenarios: [
            {
                icon: <FaSitemap className="text-3xl text-indigo-400" />,
                title: "Distributed Tracing",
                desc: "Visualize multi-step agent flows. See exactly where your chain failed, how long each step took, and the inputs/outputs at every node.",
                tags: ['Observability', 'Debugging']
            },
            {
                icon: <FaMoneyBillWave className="text-3xl text-emerald-400" />,
                title: "Cost Attribution",
                desc: "Track spend down to the user or session level. Know exactly which department is burning your GPT-4 budget.",
                tags: ['FinOps', 'Budgeting']
            },
            {
                icon: <FaBug className="text-3xl text-rose-400" />,
                title: "Loop Detection",
                desc: "Agents can get stuck in infinite loops. AgentOps automatically detects repetitive patterns and kills the process before it drains your credits.",
                tags: ['Safety', 'Reliability']
            }
        ]
    },
    {
        id: 'cortex',
        name: 'Cortex',
        tagline: 'Evaluation Engine',
        color: 'from-cortex-400 to-orange-400',
        bg: 'bg-cortex-950/20',
        scenarios: [
            {
                icon: <FaCheckDouble className="text-3xl text-green-400" />,
                title: "CI/CD Gatekeeper",
                desc: "Just like unit tests. Cortex runs a regression suite on every Prompt PR. If 'Faithfulness' drops below 95%, the deployment is blocked.",
                tags: ['DevOps', 'Quality']
            },
            {
                icon: <FaFlask className="text-3xl text-purple-400" />,
                title: "Regression Testing",
                desc: "Did your new system prompt break legacy use cases? Cortex re-runs your 'Golden Set' of 1,000 queries to ensure no regressions.",
                tags: ['Testing', 'Reliability']
            },
            {
                icon: <FaHistory className="text-3xl text-blue-400" />,
                title: "Hallucination Check",
                desc: "LLM-as-a-Judge cross-references the bot's answer against the retrieved RAG documents to calculate a 'Groundedness' score.",
                tags: ['Accuracy', 'RAG']
            }
        ]
    },
    {
        id: 'forge',
        name: 'Forge',
        tagline: 'Distillation',
        color: 'from-forge-400 to-red-400',
        bg: 'bg-forge-950/20',
        scenarios: [
            {
                icon: <FaGraduationCap className="text-3xl text-pink-400" />,
                title: "Model Distillation",
                desc: "Use expensive GPT-4 traces to train a smaller, faster Llama 3 model that mimics the teacher's performance at 1/10th the cost.",
                tags: ['Fine-Tuning', 'Optimization']
            },
            {
                icon: <FaThumbsUp className="text-3xl text-lime-400" />,
                title: "RLHF Loop",
                desc: "Turn user 'Thumbs Up/Down' feedback into a proprietary dataset. Automatically fine-tune your models to align with your specific users.",
                tags: ['Feedback', 'Alignment']
            }
        ]
    },
    {
        id: 'engram',
        name: 'Engram',
        tagline: 'Vector Data Store',
        color: 'from-indigo-400 to-blue-400',
        bg: 'bg-indigo-950/20',
        scenarios: [
            {
                icon: <FaDatabase className="text-3xl text-indigo-400" />,
                title: "Enterprise Embedding Platform",
                desc: "Centralize 100M+ embeddings across teams with configurable collections, tenant isolation, and sub-100ms similarity search at scale.",
                tags: ['Embeddings', 'Scale']
            },
            {
                icon: <FaCertificate className="text-3xl text-emerald-400" />,
                title: "Compliance-Ready Vector Search",
                desc: "SOC 2, HIPAA, and GDPR-ready architecture with encryption at rest, audit logging, and data residency controls for regulated industries.",
                tags: ['Compliance', 'Security']
            },
            {
                icon: <FaServer className="text-3xl text-cyan-400" />,
                title: "Real-Time Recommendation Engine",
                desc: "Sub-100ms similarity search powers product recommendations, content discovery, and personalization engines with streaming ingestion.",
                tags: ['Recommendations', 'Real-Time']
            }
        ]
    }
];

export default function UseCasesPage() {
    return (
        <main className="min-h-screen selection:bg-primary-500/30">
            <ScrollToTop />

            {/* Nav (Simplified) */}
            <nav className="fixed top-0 w-full z-50 glass border-b border-white/5 bg-black/20 backdrop-blur-md">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between items-center h-20">
                        <Link href="/" className="flex items-center space-x-2 cursor-pointer group">
                            <BiSolidServer className="text-3xl text-primary-400" />
                            <span className="text-xl font-bold tracking-tight text-white">NeuroGate</span>
                        </Link>
                        <Link href="/" className="text-sm font-medium text-slate-400 hover:text-white transition-colors">
                            ← Back to Home
                        </Link>
                    </div>
                </div>
            </nav>

            {/* Header */}
            <section className="pt-40 pb-20 relative overflow-hidden">
                <div className="absolute inset-0 bg-[url('/grid.svg')] bg-center [mask-image:linear-gradient(180deg,white,rgba(255,255,255,0))]" />
                <div className="max-w-7xl mx-auto px-4 text-center relative z-10">
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-white/5 border border-white/10 text-slate-300 text-sm font-medium mb-8"
                    >
                        <span className="w-2 h-2 rounded-full bg-green-400 animate-pulse" />
                        Production Scenarios
                    </motion.div>
                    <motion.h1
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.1 }}
                        className="text-5xl md:text-7xl font-bold mb-8"
                    >
                        Built for the <br />
                        <span className="text-transparent bg-clip-text bg-gradient-to-r from-primary-400 to-accent-400">Real World.</span>
                    </motion.h1>
                    <p className="text-xl text-slate-400 max-w-2xl mx-auto">
                        See how NeuroGate modules solve critical challenges in Security, Reliability, and Cost.
                    </p>
                </div>
            </section>

            {/* Modules Loop */}
            <div className="space-y-32 pb-32">
                {modules.map((mod, i) => (
                    <section key={mod.id} id={mod.id} className="relative">
                        {/* Background Glow */}
                        <div className={`absolute left-0 top-0 w-1/2 h-full bg-gradient-to-r ${mod.color} opacity-5 blur-[120px] pointer-events-none`} />

                        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
                            <div className="flex flex-col md:flex-row items-baseline gap-4 mb-12 border-b border-white/5 pb-6">
                                <h2 className="text-4xl font-bold text-white">{mod.name}</h2>
                                <span className={`text-xl font-medium text-transparent bg-clip-text bg-gradient-to-r ${mod.color}`}>
                                    {mod.tagline}
                                </span>
                            </div>

                            <div className="grid md:grid-cols-3 gap-8">
                                {mod.scenarios.map((scene, j) => (
                                    <motion.div
                                        key={j}
                                        initial={{ opacity: 0, y: 20 }}
                                        whileInView={{ opacity: 1, y: 0 }}
                                        viewport={{ once: true }}
                                        transition={{ delay: j * 0.1 }}
                                        className={`p-8 rounded-2xl border border-white/5 bg-black/40 hover:bg-white/5 transition-colors group relative overflow-hidden`}
                                    >
                                        <div className={`absolute inset-0 bg-gradient-to-br ${mod.color} opacity-0 group-hover:opacity-5 transition-opacity duration-500`} />

                                        <div className="mb-6 p-4 rounded-xl bg-white/5 w-fit border border-white/10 group-hover:scale-110 transition-transform duration-300">
                                            {scene.icon}
                                        </div>

                                        <h3 className="text-2xl font-bold mb-4 text-slate-100">{scene.title}</h3>
                                        <p className="text-slate-400 leading-relaxed mb-6">
                                            {scene.desc}
                                        </p>

                                        <div className="flex flex-wrap gap-2">
                                            {scene.tags?.map(tag => (
                                                <span key={tag} className="text-xs font-mono px-2 py-1 rounded bg-white/5 text-slate-500 border border-white/5">
                                                    #{tag}
                                                </span>
                                            ))}
                                        </div>
                                    </motion.div>
                                ))}
                            </div>
                        </div>
                    </section>
                ))}
            </div>

            {/* CTA */}
            <section className="py-24 border-t border-white/10 bg-black/20 text-center">
                <h2 className="text-3xl font-bold mb-8">Ready to secure your agents?</h2>
                <div className="flex justify-center gap-4">
                    <Link href="/playground">
                        <button className="px-8 py-3 bg-primary-600 rounded-xl font-bold shadow-lg hover:bg-primary-500 transition-all">
                            Live Simulation
                        </button>
                    </Link>
                    <a href="https://github.com/atharvaj77/NeuroGate" target="_blank" className="px-8 py-3 glass rounded-xl font-bold hover:bg-white/10 transition-all">
                        View Source
                    </a>
                </div>
            </section>
        </main>
    )
}
