import type { Metadata } from 'next'
import { Outfit } from 'next/font/google'
import './globals.css'

const outfit = Outfit({ subsets: ['latin'] })

export const metadata: Metadata = {
  title: 'NeuroKernel | The Agent-Native AI OS',
  description: 'The Agent-Native Operating System. Intelligent routing, active defense, and time-travel debugging for autonomous agents.',
  keywords: ['AI Kernel', 'AgentOps', 'Time Travel Debugger', 'Security', 'Hive Mind'],
  authors: [{ name: 'Atharva Joshi' }],
  openGraph: {
    title: 'NeuroKernel | Agent-Native AI OS',
    description: 'The complete kernel for autonomous AI agents',
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
      <body className={outfit.className}>{children}</body>
    </html>
  )
}
