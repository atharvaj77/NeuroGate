'use client'

import { useEffect, useRef, useState } from 'react'

interface MermaidDiagramProps {
  chart: string
  id?: string
}

export default function MermaidDiagram({ chart, id = 'mermaid' }: MermaidDiagramProps) {
  const ref = useRef<HTMLDivElement>(null)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const renderDiagram = async () => {
      try {
        setIsLoading(true)
        setError(null)

        // Dynamically import mermaid
        const mermaid = (await import('mermaid')).default

        // Initialize mermaid
        mermaid.initialize({
          startOnLoad: false,
          theme: 'dark',
          securityLevel: 'loose',
          themeVariables: {
            primaryColor: '#3b82f6',
            primaryTextColor: '#ffffff',
            primaryBorderColor: '#1d4ed8',
            lineColor: '#64748b',
            secondaryColor: '#10b981',
            tertiaryColor: '#8b5cf6',
            background: '#0f172a',
            mainBkg: '#1e293b',
            secondBkg: '#334155',
            tertiaryBkg: '#475569',
            darkMode: true,
            fontSize: '14px',
            fontFamily: 'ui-sans-serif, system-ui, sans-serif'
          },
          flowchart: {
            htmlLabels: true,
            curve: 'basis',
            padding: 15,
            nodeSpacing: 60,
            rankSpacing: 60,
            useMaxWidth: true
          }
        })

        if (ref.current) {
          // Clear previous content
          ref.current.innerHTML = ''

          // Generate unique ID
          const uniqueId = `${id}-${Date.now()}`

          // Render the diagram
          const { svg } = await mermaid.render(uniqueId, chart)

          // Insert SVG
          ref.current.innerHTML = svg

          // Style the SVG
          const svgElement = ref.current.querySelector('svg')
          if (svgElement) {
            svgElement.style.maxWidth = '100%'
            svgElement.style.height = 'auto'
            svgElement.style.display = 'block'
            svgElement.style.margin = '0 auto'
          }
        }

        setIsLoading(false)
      } catch (err) {
        console.error('Mermaid rendering error:', err)
        setError(err instanceof Error ? err.message : 'Failed to render diagram')
        setIsLoading(false)
      }
    }

    // Small delay to ensure DOM is ready
    const timer = setTimeout(renderDiagram, 100)

    return () => clearTimeout(timer)
  }, [chart, id])

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[500px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-400 mx-auto mb-4"></div>
          <p className="text-slate-400">Rendering architecture diagram...</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-[500px]">
        <div className="text-center max-w-md">
          <div className="text-red-400 text-4xl mb-4">⚠️</div>
          <p className="text-red-400 mb-2">Error rendering diagram</p>
          <p className="text-sm text-slate-500">{error}</p>
          <details className="mt-4 text-left">
            <summary className="text-sm text-slate-400 cursor-pointer hover:text-slate-300">
              View diagram code
            </summary>
            <pre className="mt-2 text-xs text-slate-500 overflow-x-auto p-4 bg-slate-900 rounded">
              {chart}
            </pre>
          </details>
        </div>
      </div>
    )
  }

  return (
    <div
      ref={ref}
      className="mermaid-container w-full overflow-x-auto py-4"
      style={{ minHeight: '400px' }}
    />
  )
}
