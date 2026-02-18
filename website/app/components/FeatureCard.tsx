'use client'

import { motion } from 'framer-motion'
import { ReactNode } from 'react'
import { FaCheckCircle } from 'react-icons/fa'

import Link from 'next/link'

interface FeatureCardProps {
  icon: ReactNode
  title: string
  description: string
  features: string[]
  gradient: string
  delay?: number
  href?: string
  cta?: string
}

export default function FeatureCard({
  icon,
  title,
  description,
  features,
  gradient,
  delay = 0,
  href,
  cta
}: FeatureCardProps) {
  const CardContent = (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ delay }}
      whileHover={{ scale: 1.05, rotateY: 5 }}
      className={`glass p-8 rounded-xl hover:shadow-2xl transition-all relative overflow-hidden group h-full flex flex-col glow-border ${href ? 'cursor-pointer' : ''}`}
    >
      {/* Gradient background on hover */}
      <div className={`absolute inset-0 bg-gradient-to-br ${gradient} opacity-0 group-hover:opacity-10 transition-opacity`} />

      <div className="flex-1">
        <motion.div
          className={`inline-block p-4 rounded-xl bg-gradient-to-br ${gradient} bg-opacity-10 mb-6 relative z-10`}
          whileHover={{ rotate: [0, -10, 10, -10, 0] }}
          transition={{ duration: 0.5 }}
        >
          {icon}
        </motion.div>

        <h3 className="text-2xl font-bold mb-4 relative z-10">{title}</h3>
        <p className="text-slate-400 mb-6 relative z-10">{description}</p>

        <ul className="space-y-2 relative z-10">
          {features.map((item, i) => (
            <motion.li
              key={i}
              className="flex items-start"
              initial={{ opacity: 0, x: -10 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              transition={{ delay: delay + (i * 0.1) }}
            >
              <FaCheckCircle className="text-green-400 mr-2 mt-1 flex-shrink-0" />
              <span className="text-sm text-slate-300">{item}</span>
            </motion.li>
          ))}
        </ul>
      </div>

      {href && cta && (
        <div className="mt-8 pt-4 border-t border-white/10 relative z-10 opacity-60 group-hover:opacity-100 transition-opacity flex items-center justify-between">
          <span className="text-xs font-mono text-slate-400 group-hover:text-white transition-colors uppercase tracking-wider">{cta}</span>
          <div className="p-2 rounded-full bg-white/5 group-hover:bg-white/20 transition-all transform group-hover:translate-x-1">
            <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
            </svg>
          </div>
        </div>
      )}
    </motion.div>
  )

  if (href) {
    return (
      <Link href={href} className="block h-full">
        {CardContent}
      </Link>
    )
  }

  return CardContent
}
