import Link from 'next/link'
import { FaBook, FaCode } from 'react-icons/fa'

export default function ApiDocs() {
    return (
        <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
            <div className="mb-12">
                <Link href="/" className="text-primary-400 hover:text-primary-300 mb-4 inline-block">← Back to Home</Link>
                <h1 className="text-4xl font-bold mb-4">API Reference</h1>
                <p className="text-slate-400 text-lg">NeuroGate API v1.0 Documentation</p>

                <div className="mt-6 p-4 bg-yellow-900/20 border border-yellow-600/30 rounded-lg">
                    <p className="text-yellow-200 text-sm">
                        <strong>Base URL:</strong> <code className="bg-black/30 px-2 py-0.5 rounded">http://localhost:8080</code>
                    </p>
                </div>
            </div>

            <div className="grid md:grid-cols-4 gap-8">
                {/* Sidebar Navigation */}
                <div className="hidden md:block col-span-1">
                    <div className="sticky top-24 space-y-2">
                        <h3 className="font-bold text-slate-200 mb-4 uppercase tracking-wider text-sm">Endpoints</h3>
                        <a href="#authentication" className="block text-slate-400 hover:text-primary-400 transition-colors">Authentication</a>
                        <a href="#chat-completions" className="block text-slate-400 hover:text-primary-400 transition-colors">Chat Completions</a>
                        <a href="#analytics" className="block text-slate-400 hover:text-primary-400 transition-colors">Analytics & Cost</a>
                        <a href="#neuroguard" className="block text-slate-400 hover:text-primary-400 transition-colors">NeuroGuard Security</a>
                        <a href="#debugger" className="block text-slate-400 hover:text-primary-400 transition-colors">AI Debugger</a>
                    </div>
                </div>

                {/* Main Content */}
                <div className="md:col-span-3 space-y-16">

                    {/* Section: Authentication */}
                    <section id="authentication">
                        <div className="flex items-center gap-3 mb-6">
                            <span className="text-2xl">🔐</span>
                            <h2 className="text-3xl font-bold">Authentication</h2>
                        </div>

                        <div className="space-y-8">
                            <div className="glass p-6 rounded-xl border border-slate-700">
                                <h4 className="font-bold text-slate-300 mb-3">Method 1: Clerk JWT</h4>
                                <div className="bg-slate-950 p-4 rounded-lg overflow-x-auto border border-slate-800 mb-5">
                                    <pre className="text-sm text-blue-300">
                                        {`Authorization: Bearer <clerk-jwt-token>`}
                                    </pre>
                                </div>

                                <h4 className="font-bold text-slate-300 mb-3">Method 2: API Key</h4>
                                <div className="bg-slate-950 p-4 rounded-lg overflow-x-auto border border-slate-800">
                                    <pre className="text-sm text-blue-300">
                                        {`Authorization: Bearer ng_live_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
// or
X-API-Key: ng_live_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`}
                                    </pre>
                                </div>
                            </div>
                        </div>
                    </section>

                    {/* Section: Chat Completions */}
                    <section id="chat-completions">
                        <div className="flex items-center gap-3 mb-6">
                            <FaCode className="text-2xl text-primary-400" />
                            <h2 className="text-3xl font-bold">Chat Completions API</h2>
                        </div>

                        <div className="space-y-8">
                            <div className="glass p-6 rounded-xl border border-slate-700">
                                <div className="flex items-center gap-3 mb-4">
                                    <span className="bg-green-500/20 text-green-400 px-3 py-1 rounded font-mono text-sm font-bold">POST</span>
                                    <code className="text-lg">/v1/chat/completions</code>
                                </div>
                                <p className="text-slate-400 mb-6">
                                    OpenAI-compatible endpoint for LLM inference. Routes requests to optimal providers, handles PII protection, and manages caching.
                                </p>

                                <h4 className="font-bold text-slate-300 mb-3">Auth Header</h4>
                                <div className="bg-slate-950 p-4 rounded-lg overflow-x-auto border border-slate-800 mb-6">
                                    <pre className="text-sm text-blue-300">{`Authorization: Bearer <jwt-or-api-key>`}</pre>
                                </div>

                                <h4 className="font-bold text-slate-300 mb-3">Request Body</h4>
                                <div className="bg-slate-950 p-4 rounded-lg overflow-x-auto border border-slate-800">
                                    <pre className="text-sm text-blue-300">
                                        {`{
  "model": "gpt-3.5-turbo",
  "messages": [
    {
      "role": "user",
      "content": "Hello world"
    }
  ],
  "stream": false
}`}
                                    </pre>
                                </div>
                            </div>
                        </div>
                    </section>

                    {/* Section: Analytics */}
                    <section id="analytics">
                        <div className="flex items-center gap-3 mb-6">
                            <FaBook className="text-2xl text-blue-400" />
                            <h2 className="text-3xl font-bold">Analytics & Cost</h2>
                        </div>

                        <div className="space-y-8">
                            <div className="glass p-6 rounded-xl border border-slate-700">
                                <div className="flex items-center gap-3 mb-4">
                                    <span className="bg-blue-500/20 text-blue-400 px-3 py-1 rounded font-mono text-sm font-bold">GET</span>
                                    <code className="text-lg">/api/v1/analytics/costs/user/{'{userId}'}</code>
                                </div>
                                <p className="text-slate-400 mb-6">
                                    Retrieve cost analytics for a specific user within a date range.
                                </p>
                                <p className="text-slate-500 text-sm mb-4">Requires JWT or API key with VIEWER+ role.</p>
                                <div className="bg-slate-950 p-4 rounded-lg overflow-x-auto border border-slate-800">
                                    <pre className="text-sm text-blue-300">
                                        {`// Response
{
  "userId": "user-123",
  "totalCost": 24.56,
  "cacheHitRate": 0.45,
  "providerBreakdown": {
    "openai": 15.30,
    "anthropic": 9.26
  }
}`}
                                    </pre>
                                </div>
                            </div>
                        </div>
                    </section>

                    {/* Section: NeuroGuard */}
                    <section id="neuroguard">
                        <div className="flex items-center gap-3 mb-6">
                            <span className="text-2xl">🛡️</span>
                            <h2 className="text-3xl font-bold">NeuroGuard Security</h2>
                        </div>

                        <div className="space-y-8">
                            <div className="glass p-6 rounded-xl border border-slate-700">
                                <div className="flex items-center gap-3 mb-4">
                                    <span className="bg-green-500/20 text-green-400 px-3 py-1 rounded font-mono text-sm font-bold">POST</span>
                                    <code className="text-lg">/v1/neuroguard/analyze/prompt</code>
                                </div>
                                <p className="text-slate-400 mb-6">
                                    Detect prompt injection attempts and other security threats in user input.
                                </p>
                                <p className="text-slate-500 text-sm">Requires JWT or API key with DEVELOPER+ role.</p>
                            </div>
                        </div>
                    </section>

                    {/* Section: Debugger */}
                    <section id="debugger">
                        <div className="flex items-center gap-3 mb-6">
                            <span className="text-2xl">🐞</span>
                            <h2 className="text-3xl font-bold">AI Debugger</h2>
                        </div>

                        <div className="space-y-8">
                            <div className="glass p-6 rounded-xl border border-slate-700">
                                <div className="flex items-center gap-3 mb-4">
                                    <span className="bg-blue-500/20 text-blue-400 px-3 py-1 rounded font-mono text-sm font-bold">GET</span>
                                    <code className="text-lg">/api/debug/records</code>
                                </div>
                                <p className="text-slate-400 mb-6">
                                    Search and retrieve historical LLM request records for debugging.
                                </p>
                                <p className="text-slate-500 text-sm">Requires JWT or API key with VIEWER+ role.</p>
                            </div>
                        </div>
                    </section>

                </div>
            </div>
        </div>
    )
}
