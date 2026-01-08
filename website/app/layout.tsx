import type { Metadata } from 'next'
import { Outfit, JetBrains_Mono } from 'next/font/google'
import './globals.css'

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
  description: 'The Open Source Neural Gateway for AI Agents. Orchestrate, Secure, and Optimize your LLM traffic with a production-ready kernel.',
  keywords: ['AI Gateway', 'AgentOps', 'Open Source', 'LLM Security', 'RAG'],
  authors: [{ name: 'Atharva Joshi' }],
  openGraph: {
    title: 'NeuroGate | Open Source Agent Kernel',
    description: 'The Open Source Neural Gateway for AI Agents.',
    type: 'website',
  },
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en" className="scroll-smooth">
      <body className={`${outfit.variable} ${jetbrainsMono.variable} font-sans bg-black text-slate-100 anti-aliased selection:bg-primary-500/30`}>{children}</body>
    </html>
  )
}
