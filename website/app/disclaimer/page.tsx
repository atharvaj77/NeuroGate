import Link from 'next/link'

export default function Disclaimer() {
  return (
    <div className="max-w-4xl mx-auto px-4 py-20">
      <h1 className="text-4xl font-bold mb-8">Disclaimer & Terms</h1>

      <section className="mb-8">
        <h2 className="text-2xl font-bold mb-4">Project Status</h2>
        <p>NeuroGate is a research project and personal portfolio piece. It is:</p>
        <ul className="list-disc ml-6 space-y-2 mt-4 text-slate-300">
          <li>❌ NOT production-ready</li>
          <li>❌ NOT enterprise-grade (despite architectural patterns)</li>
          <li>❌ NOT security-audited</li>
          <li>❌ NOT compliant with SOC 2, HIPAA, or other standards</li>
          <li>❌ NOT recommended for use with real customer data</li>
        </ul>
      </section>

      <section className="mb-8">
        <h2 className="text-2xl font-bold mb-4">What NeuroGate IS:</h2>
        <ul className="list-disc ml-6 space-y-2 text-slate-300">
          <li>✅ Educational open-source project</li>
          <li>✅ Demonstration of modern Java architecture</li>
          <li>✅ Reference implementation for AI gateway patterns</li>
          <li>✅ Suitable for development/testing environments</li>
          <li>✅ MIT Licensed (use at your own risk)</li>
        </ul>
      </section>

      <section className="mb-8">
        <h2 className="text-2xl font-bold mb-4">Limitations</h2>
        <ul className="list-disc ml-6 space-y-2 text-slate-300">
          <li>Solo developer project (not a company/team)</li>
          <li>Maintained part-time</li>
          <li>No warranty or guarantee</li>
          <li>No SLA or support contracts</li>
          <li>Some features are incomplete or experimental</li>
        </ul>
      </section>

      <section className="mb-8 p-6 bg-red-900/20 border border-red-500/50 rounded-xl">
        <h2 className="text-2xl font-bold mb-4">⚠️ Warranty Disclaimer</h2>
        <p className="font-mono text-sm text-slate-300">
          THIS SOFTWARE IS PROVIDED &quot;AS IS&quot;, WITHOUT WARRANTY OF ANY KIND,
          EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
          OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
          NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR
          ANY CLAIM, DAMAGES OR OTHER LIABILITY.
        </p>
      </section>

      <section>
        <h2 className="text-2xl font-bold mb-4">About the Author</h2>
        <p className="text-slate-300">
          NeuroGate is a personal project by Atharva Joshi, built as a learning
          exercise and portfolio piece. I am employed full-time elsewhere and
          maintain this project in my spare time.
        </p>
        <p className="mt-4 text-slate-300">
          <strong>Contact:</strong> (for questions only, not support) - check GitHub profile
        </p>
      </section>

      <div className="mt-12">
        <Link href="/" className="text-primary-400 hover:underline">← Back to Home</Link>
      </div>
    </div>
  )
}
