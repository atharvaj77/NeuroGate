
import React, { useState } from 'react';
import { FaPlay, FaPlus, FaTrash, FaCheckCircle, FaTimesCircle, FaSpinner, FaFlask } from 'react-icons/fa';
import { motion, AnimatePresence } from 'framer-motion';

interface TestCase {
    id: string;
    input: string;
    expected: string;
    actual?: string;
    status?: 'pending' | 'pass' | 'fail';
    reason?: string;
    score?: number;
}

interface TestsPanelProps {
    promptContent: string;
    testCases: TestCase[];
    setTestCases: (cases: TestCase[]) => void;
    onRunTests: (cases: TestCase[]) => Promise<void>;
    isRunning: boolean;
}

const TestsPanel = ({ promptContent, testCases, setTestCases, onRunTests, isRunning }: TestsPanelProps) => {

    const addTestCase = () => {
        const newCase: TestCase = {
            id: Date.now().toString(),
            input: '',
            expected: '',
            status: 'pending'
        };
        setTestCases([...testCases, newCase]);
    };

    const removeTestCase = (id: string) => {
        setTestCases(testCases.filter(c => c.id !== id));
    };

    const updateTestCase = (id: string, field: 'input' | 'expected', value: string) => {
        setTestCases(testCases.map(c => c.id === id ? { ...c, [field]: value } : c));
    };

    return (
        <div className="flex flex-col h-full bg-slate-900/50">
            {/* Header */}
            <div className="p-4 border-b border-white/5 flex items-center justify-between bg-white/5">
                <div>
                    <h3 className="text-sm font-bold text-white flex items-center gap-2">
                        <FaFlask className="text-purple-400" />
                        Test Suite
                    </h3>
                    <p className="text-[10px] text-slate-400 mt-1">Define inputs and expected outputs to verify prompt behavior.</p>
                </div>
                <div className="flex gap-2">
                    <button
                        onClick={addTestCase}
                        className="px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-xs text-white border border-white/10 transition-colors flex items-center gap-2"
                    >
                        <FaPlus size={10} /> Add Case
                    </button>
                    <button
                        onClick={() => onRunTests(testCases)}
                        disabled={isRunning || testCases.length === 0}
                        className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 disabled:cursor-not-allowed text-xs text-white font-bold shadow-lg shadow-emerald-500/20 transition-all flex items-center gap-2"
                    >
                        {isRunning ? <FaSpinner className="animate-spin" /> : <FaPlay size={10} />}
                        Run Tests
                    </button>
                </div>
            </div>

            {/* List */}
            <div className="flex-1 overflow-y-auto p-4 space-y-3 custom-scrollbar">
                {testCases.length === 0 ? (
                    <div className="flex flex-col items-center justify-center h-48 text-slate-500 border-2 border-dashed border-white/5 rounded-xl">
                        <FaFlask size={24} className="mb-2 opacity-50" />
                        <p className="text-xs">No test cases defined.</p>
                        <button onClick={addTestCase} className="text-purple-400 text-xs mt-1 hover:underline">Add your first test case</button>
                    </div>
                ) : (
                    <AnimatePresence>
                        {testCases.map((tc, index) => (
                            <motion.div
                                key={tc.id}
                                initial={{ opacity: 0, y: 10 }}
                                animate={{ opacity: 1, y: 0 }}
                                exit={{ opacity: 0, height: 0 }}
                                className={`rounded-xl border p-3 transition-all group ${tc.status === 'pass' ? 'bg-emerald-500/5 border-emerald-500/20' :
                                        tc.status === 'fail' ? 'bg-red-500/5 border-red-500/20' :
                                            'bg-white/5 border-white/5 hover:border-white/10'
                                    }`}
                            >
                                <div className="flex items-start gap-3">
                                    <div className="flex-none mt-1 text-[10px] font-mono text-slate-500 w-4">{index + 1}</div>
                                    <div className="flex-1 space-y-2">
                                        <div>
                                            <label className="text-[10px] text-slate-400 uppercase tracking-wider block mb-1">Input (User Query)</label>
                                            <textarea
                                                value={tc.input}
                                                onChange={(e) => updateTestCase(tc.id, 'input', e.target.value)}
                                                placeholder="e.g. How do I reset my password?"
                                                className="w-full bg-black/40 border border-white/10 rounded px-2 py-1.5 text-xs text-white outline-none focus:border-purple-500 resize-none h-14 font-mono"
                                            />
                                        </div>
                                        <div>
                                            <label className="text-[10px] text-slate-400 uppercase tracking-wider block mb-1">Expected Output (Ideal)</label>
                                            <textarea
                                                value={tc.expected}
                                                onChange={(e) => updateTestCase(tc.id, 'expected', e.target.value)}
                                                placeholder="e.g. Instructions to go to settings page."
                                                className="w-full bg-black/40 border border-white/10 rounded px-2 py-1.5 text-xs text-white outline-none focus:border-emerald-500 resize-none h-14 font-mono"
                                            />
                                        </div>

                                        {/* Result Section */}
                                        {tc.status && tc.status !== 'pending' && (
                                            <div className={`mt-2 p-2 rounded bg-black/20 text-xs border ${tc.status === 'pass' ? 'border-emerald-500/20' : 'border-red-500/20'}`}>
                                                <div className="flex items-center gap-2 mb-1">
                                                    {tc.status === 'pass' ? <FaCheckCircle className="text-emerald-400" /> : <FaTimesCircle className="text-red-400" />}
                                                    <span className={`font-bold ${tc.status === 'pass' ? 'text-emerald-400' : 'text-red-400'}`}>
                                                        {tc.status === 'pass' ? 'Passed' : 'Failed'} ({tc.score}%)
                                                    </span>
                                                </div>
                                                {tc.reason && <p className="text-slate-400 text-[10px] leading-relaxed italic">{tc.reason}</p>}
                                                {tc.actual && (
                                                    <div className="mt-2 pt-2 border-t border-white/5">
                                                        <span className="text-[9px] text-slate-500 uppercase">Actual Output:</span>
                                                        <p className="text-slate-300 font-mono text-[10px] mt-0.5 line-clamp-3 hover:line-clamp-none cursor-help transition-all">{tc.actual}</p>
                                                    </div>
                                                )}
                                            </div>
                                        )}
                                    </div>
                                    <button
                                        onClick={() => removeTestCase(tc.id)}
                                        className="p-1.5 text-slate-600 hover:text-red-400 transition-colors opacity-0 group-hover:opacity-100"
                                        title="Remove Case"
                                    >
                                        <FaTrash size={12} />
                                    </button>
                                </div>
                            </motion.div>
                        ))}
                    </AnimatePresence>
                )}
            </div>
        </div>
    );
};

export default TestsPanel;
