import type { Metadata } from 'next'
import { Outfit, JetBrains_Mono } from 'next/font/google'
import { ClerkProvider } from './lib/clerk'
import './globals.css'
import AuthSessionSync from './components/AuthSessionSync'

const outfit = Outfit({
  subsets: ['latin'],
  variable: '--font-outfit',
})

const jetbrainsMono = JetBrains_Mono({
  subsets: ['latin'],
  variable: '--font-jetbrains-mono',
})

export const metadata: Metadata = {
  title: 'NeuroGate | Open Source Agent Kernel',
  description: 'The Open Source Neural Gateway for AI Agents. Orchestrate, Secure, and Optimize your LLM traffic with a production-ready kernel featuring vector database, embedding store, and enterprise AI capabilities.',
  keywords: ['AI Gateway', 'AgentOps', 'Open Source', 'LLM Security', 'RAG', 'vector database', 'embedding store', 'enterprise AI', 'semantic caching', 'PII protection', 'agent orchestration', 'LLM routing'],
  authors: [{ name: 'Atharva Joshi' }],
  openGraph: {
    title: 'NeuroGate | Open Source Agent Kernel',
    description: 'The Open Source Neural Gateway for AI Agents. 9+ modules for routing, security, evaluation, and vector storage.',
    type: 'website',
    images: ['/og-image.png'],
  },
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const publishableKey = process.env.NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY || 'pk_test_placeholder'

  return (
    <html lang="en" className="scroll-smooth">
      <body className={`${outfit.variable} ${jetbrainsMono.variable} font-sans bg-black text-slate-100 anti-aliased selection:bg-primary-500/30`}>
        <ClerkProvider publishableKey={publishableKey}>
          {children}
          <AuthSessionSync />
        </ClerkProvider>
      </body>
    </html>
  )
}
