"use client";

import React, { useState } from 'react';
import PromptEditor from './components/PromptEditor';
import VariableForm from './components/VariableForm';
import VersionGraph from './components/VersionGraph';
import { FaPlay, FaSave, FaRocket, FaCodeBranch, FaHistory } from 'react-icons/fa';

export default function SynapsePage() {
    const [promptContent, setPromptContent] = useState<string>(
        "You are a helpful AI assistant named {{ bot_name }}.\nToday is {{ date }}.\n\nUser: {{ user_query }}\nAssistant:"
    );
    const [variables, setVariables] = useState<Record<string, string>>({});
    const [activeTab, setActiveTab] = useState<'variables' | 'versions'>('variables');
    const [consoleOutput, setConsoleOutput] = useState<string>("// Ready to run...");

    const handleRun = async () => {
        setConsoleOutput("// Running...\n");
        // Simulate API call delay
        setTimeout(() => {
            let result = promptContent;
            Object.entries(variables).forEach(([key, val]) => {
                result = result.replace(new RegExp(`\\{\\{\\s*${key}\\s*\\}\\}`, 'g'), val);
            });
            setConsoleOutput(`// Simulated Output:\n${result}\n\n// AI Response:\nHello! How can I help you today?`);
        }, 800);
    };

    return (
        <div className="flex h-screen bg-[#0d1117] text-white overflow-hidden">
            {/* Sidebar */}
            <div className="w-16 flex flex-col items-center py-4 border-r border-gray-800 bg-[#161b22]">
                <div className="mb-8 text-blue-500 text-2xl font-bold">N</div>
                <div className="space-y-6">
                    <button className="p-3 bg-blue-600 rounded-lg text-white shadow-lg shadow-blue-500/20 hover:bg-blue-500 transition-all">
                        <FaCodeBranch />
                    </button>
                    <button className="p-3 text-gray-500 hover:text-gray-300 transition-all">
                        <FaHistory />
                    </button>
                </div>
            </div>

            {/* Main Content */}
            <div className="flex-1 flex flex-col min-w-0">
                {/* Header */}
                <div className="h-16 border-b border-gray-800 flex items-center justify-between px-6 bg-[#0d1117]">
                    <div className="flex items-center space-x-4">
                        <h1 className="text-lg font-semibold tracking-wide">Synapse Studio</h1>
                        <span className="px-2 py-0.5 rounded text-xs bg-gray-800 text-gray-400 border border-gray-700">v1.2-draft</span>
                    </div>
                    <div className="flex items-center space-x-3">
                        <button
                            onClick={handleRun}
                            className="flex items-center space-x-2 px-4 py-2 bg-green-600 hover:bg-green-500 text-white rounded text-sm font-medium transition-all"
                        >
                            <FaPlay size={12} />
                            <span>Run</span>
                        </button>
                        <button className="flex items-center space-x-2 px-4 py-2 bg-gray-800 hover:bg-gray-700 border border-gray-700 text-white rounded text-sm font-medium transition-all">
                            <FaSave size={12} />
                            <span>Save</span>
                        </button>
                        <button className="flex items-center space-x-2 px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded text-sm font-medium transition-all">
                            <FaRocket size={12} />
                            <span>Deploy</span>
                        </button>
                    </div>
                </div>

                {/* Workspace Split */}
                <div className="flex-1 flex overflow-hidden">
                    {/* Editor Area (Left or Center) */}
                    <div className="flex-1 flex flex-col border-r border-gray-800">
                        <div className="flex-1 p-4 bg-[#0d1117]">
                            <PromptEditor
                                value={promptContent}
                                onChange={(val) => setPromptContent(val || '')}
                            />
                        </div>
                        {/* Console / Output */}
                        <div className="h-1/3 border-t border-gray-800 bg-[#010409] p-4 font-mono text-sm text-gray-300 overflow-auto">
                            <div className="text-gray-500 mb-2 uppercase text-xs font-bold tracking-wider">Console Output</div>
                            <pre className="whitespace-pre-wrap">{consoleOutput}</pre>
                        </div>
                    </div>

                    {/* Right Panel (Variables / Versions) */}
                    <div className="w-80 bg-[#161b22] flex flex-col border-l border-gray-800">
                        <div className="flex border-b border-gray-800">
                            <button
                                onClick={() => setActiveTab('variables')}
                                className={`flex-1 py-3 text-sm font-medium ${activeTab === 'variables' ? 'text-blue-400 border-b-2 border-blue-400 bg-[#1f2937]' : 'text-gray-400 hover:text-white'}`}
                            >
                                Variables
                            </button>
                            <button
                                onClick={() => setActiveTab('versions')}
                                className={`flex-1 py-3 text-sm font-medium ${activeTab === 'versions' ? 'text-blue-400 border-b-2 border-blue-400 bg-[#1f2937]' : 'text-gray-400 hover:text-white'}`}
                            >
                                Versions
                            </button>
                        </div>

                        <div className="flex-1 overflow-auto p-4">
                            {activeTab === 'variables' ? (
                                <VariableForm
                                    promptContent={promptContent}
                                    variables={variables}
                                    onChange={(key, val) => setVariables(prev => ({ ...prev, [key]: val }))}
                                />
                            ) : (
                                <div className="h-full">
                                    <VersionGraph />
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
