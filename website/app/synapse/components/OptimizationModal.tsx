
import React from 'react';
import { FaMagic, FaTimes, FaSpinner, FaCheck } from 'react-icons/fa';
import { DiffEditor } from '@monaco-editor/react';

const OptimizationModal = ({ isOpen, onClose, original, result, isOptimizing, objective, setObjective, objectives, onOptimize, onAccept }: any) => {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
            <div className="bg-slate-900 border border-white/10 rounded-2xl shadow-2xl w-full max-w-6xl h-[85vh] flex flex-col overflow-hidden animate-in fade-in zoom-in duration-200">
                {/* Header */}
                <div className="h-16 border-b border-white/10 flex items-center justify-between px-6 bg-slate-800/50">
                    <div className="flex items-center gap-3">
                        <div className="p-2 bg-purple-500/20 rounded-lg text-purple-400">
                            <FaMagic size={18} />
                        </div>
                        <div>
                            <h2 className="text-lg font-bold text-white">Neuro-Optimizer</h2>
                            <p className="text-xs text-slate-400">AI-powered prompt refinement</p>
                        </div>
                    </div>
                    <button onClick={onClose} className="text-slate-500 hover:text-white transition-colors">
                        <FaTimes size={20} />
                    </button>
                </div>

                <div className="flex-1 flex overflow-hidden">
                    {/* Sidebar Configuration */}
                    <div className="w-80 border-r border-white/10 bg-black/20 p-6 flex flex-col gap-6 overflow-y-auto">
                        <div>
                            <label className="text-xs font-bold text-slate-400 uppercase tracking-widest block mb-3">Optimization Objective</label>
                            <div className="space-y-2">
                                {objectives.map((obj: any) => (
                                    <button
                                        key={obj.id}
                                        onClick={() => setObjective(obj.id)}
                                        className={`w-full text-left px-4 py-3 rounded-xl border transition-all ${objective === obj.id
                                            ? 'bg-purple-600/20 border-purple-500 text-white shadow-[0_0_15px_rgba(168,85,247,0.2)]'
                                            : 'bg-white/5 border-white/5 text-slate-400 hover:bg-white/10 hover:text-white'
                                            }`}
                                    >
                                        <div className="text-sm font-semibold mb-1">{obj.label}</div>
                                        <div className="text-[10px] opacity-60">
                                            {obj.id === 'FIX_GRAMMAR' && "Corrects errors and standardizes tone."}
                                            {obj.id === 'CONCISE' && "Reduces token usage without losing meaning."}
                                            {obj.id === 'REASONING' && "Injects CoT steps for complex logic."}
                                            {obj.id === 'FEW_SHOT' && "Generates 3 examples based on context."}
                                        </div>
                                    </button>
                                ))}
                            </div>
                        </div>

                        <div className="mt-auto">
                            <button
                                onClick={onOptimize}
                                disabled={isOptimizing}
                                className="w-full py-3 bg-gradient-to-r from-purple-600 to-indigo-600 rounded-xl font-bold text-white shadow-lg hover:shadow-purple-500/25 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                            >
                                {isOptimizing ? <FaSpinner className="animate-spin" /> : <FaMagic />}
                                {isOptimizing ? 'Optimizing...' : 'Generate Optimization'}
                            </button>
                        </div>
                    </div>

                    {/* Main Content (Diff) */}
                    <div className="flex-1 flex flex-col bg-slate-950 relative">
                        {result ? (
                            <div className="flex-1 flex flex-col min-h-0">
                                <div className="p-4 border-b border-white/10 bg-purple-500/5">
                                    <h3 className="text-xs font-bold text-purple-300 uppercase tracking-widest mb-2">AI Explanation</h3>
                                    <p className="text-sm text-slate-300 leading-relaxed font-light">{result.explanation}</p>
                                </div>
                                <div className="flex-1 relative">
                                    <DiffEditor
                                        original={result.originalPrompt}
                                        modified={result.optimizedPrompt}
                                        language="markdown"
                                        theme="vs-dark"
                                        options={{
                                            originalEditable: false,
                                            readOnly: true,
                                            minimap: { enabled: false },
                                            wordWrap: 'on',
                                            scrollBeyondLastLine: false,
                                            renderSideBySide: true
                                        }}
                                    />
                                </div>
                                <div className="p-4 border-t border-white/10 bg-slate-900 flex justify-end gap-3">
                                    <button onClick={onClose} className="px-4 py-2 rounded-lg text-sm text-slate-400 hover:text-white hover:bg-white/5 transition-colors">
                                        Discard
                                    </button>
                                    <button onClick={onAccept} className="px-4 py-2 rounded-lg text-sm font-semibold bg-emerald-600 text-white hover:bg-emerald-500 shadow-lg shadow-emerald-500/20 transition-all flex items-center gap-2">
                                        <FaCheck /> Accept Changes
                                    </button>
                                </div>
                            </div>
                        ) : (
                            <div className="flex-1 flex flex-col items-center justify-center text-slate-500">
                                <div className="w-24 h-24 bg-white/5 rounded-full flex items-center justify-center mb-6">
                                    <FaMagic size={40} className="text-slate-600" />
                                </div>
                                <p className="text-lg font-medium">Ready to Optimize</p>
                                <p className="text-sm opacity-60 max-w-sm text-center mt-2">Select an objective from the sidebar and click &quot;Generate&quot; to see AI suggestions.</p>
                            </div>
                        )}

                        {isOptimizing && !result && (
                            <div className="absolute inset-0 bg-slate-900/80 backdrop-blur-sm z-10 flex flex-col items-center justify-center">
                                <FaSpinner className="animate-spin text-purple-500 mb-4" size={40} />
                                <p className="text-purple-300 font-mono animate-pulse">Consulting the Oracle...</p>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default OptimizationModal;
