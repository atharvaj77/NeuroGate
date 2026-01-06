'use client'

import { useState } from 'react'
import { motion } from 'framer-motion'
import { FaCopy, FaCheck } from 'react-icons/fa'

interface CodeBlockProps {
  code: string
  language?: string
  showLineNumbers?: boolean
}

export default function CodeBlock({ code, language = 'bash', showLineNumbers = false }: CodeBlockProps) {
  const [copied, setCopied] = useState(false)

  const copyToClipboard = async () => {
    await navigator.clipboard.writeText(code)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="relative group">
      <div className="absolute top-2 right-2 z-10">
        <motion.button
          whileHover={{ scale: 1.1 }}
          whileTap={{ scale: 0.9 }}
          onClick={copyToClipboard}
          className="glass p-2 rounded-lg hover:bg-white/10 transition-colors"
          title="Copy to clipboard"
        >
          {copied ? (
            <FaCheck className="text-green-400" />
          ) : (
            <FaCopy className="text-slate-400" />
          )}
        </motion.button>
      </div>
      <div className="code-block">
        <pre className="text-sm overflow-x-auto">
          <code className={`language-${language}`}>
            {showLineNumbers ? (
              code.split('\n').map((line, i) => (
                <div key={i} className="table-row">
                  <span className="table-cell pr-4 text-slate-600 select-none text-right">
                    {i + 1}
                  </span>
                  <span className="table-cell">{line}</span>
                </div>
              ))
            ) : (
              code
            )}
          </code>
        </pre>
      </div>
    </div>
  )
}
