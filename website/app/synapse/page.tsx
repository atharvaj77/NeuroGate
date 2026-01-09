"use client";

import React, { useState, useEffect, useCallback } from 'react';
import PromptEditor from './components/PromptEditor';
import VariableForm from './components/VariableForm';
import VersionGraph from './components/VersionGraph';
import HistoryList from './components/HistoryList';
import VersionsList from './components/VersionsList';
import { FaPlay, FaSave, FaRocket, FaCodeBranch, FaHistory, FaColumns, FaTerminal, FaCog } from 'react-icons/fa';

interface Version {
    id: string;
    tag: string;
    content: string;
    timestamp: string;
    author: string;
    active: boolean;
}

interface HistoryItem {
    id: string;
    timestamp: string;
    action: string;
    user: string;
    details: string;
}

export default function SynapsePage() {
    // -- State --
    const [promptContent, setPromptContent] = useState<string>(
        "You are a helpful AI assistant named {{ bot_name }}.\nToday is {{ date }}.\n\nUser: {{ user_query }}\nAssistant:"
    );
    const [variables, setVariables] = useState<Record<string, string>>({
        bot_name: "NeuroBot",
        date: new Date().toLocaleDateString(),
        user_query: "How do I optimize my LLM costs?"
    });

    const [activeRightTab, setActiveRightTab] = useState<'variables' | 'versions'>('variables');
    const [showHistory, setShowHistory] = useState(false);
    const [consoleOutput, setConsoleOutput] = useState<string>("// Ready to run...");
    const [isRunning, setIsRunning] = useState(false);

    // Backend Configuration
    const [simulationMode, setSimulationMode] = useState(true);
    const [showSettings, setShowSettings] = useState(false);
    const [backendConfig, setBackendConfig] = useState({
        url: 'http://localhost',
        port: '8080'
    });

    // Mock Data State
    const [versions, setVersions] = useState<Version[]>([
        { id: 'v1.0', tag: 'v1.0', content: "You are a specialized assistant...", timestamp: '2 days ago', author: 'System', active: false },
        { id: 'v1.1', tag: 'v1.1', content: "You are a helpful AI assistant...", timestamp: 'Yesterday', author: 'User', active: false },
        { id: 'v1.2', tag: 'v1.2-draft', content: "You are a helpful AI assistant named {{ bot_name }}...", timestamp: 'Today', author: 'You', active: true },
    ]);

    const [history, setHistory] = useState<HistoryItem[]>([
        { id: 'h1', timestamp: '10:00 AM', action: 'Deployed v1.1', user: 'Admin', details: 'Promoted to Production' },
        { id: 'h2', timestamp: '10:05 AM', action: 'Created v1.2-draft', user: 'You', details: 'Started editing' },
    ]);

    // -- Handlers --

    const handleRun = async () => {
        setIsRunning(true);

        if (simulationMode) {
            setConsoleOutput("// Running simulation against 'gpt-4-turbo'...\n");

            // Simulate API call delay
            setTimeout(() => {
                let result = promptContent;

                // Basic variable substitution for preview
                Object.entries(variables).forEach(([key, val]) => {
                    result = result.replace(new RegExp(`\\{\\{\\s*${key}\\s*\\}\\}`, 'g'), val);
                });

                const mockResponse = "To optimize your LLM costs, consider:\n1. Using smaller models for simple tasks.\n2. Implementing caching (NeuroGate Cache).\n3. Compressing prompts using our distillation engine.";

                setConsoleOutput(
                    `// ----------------------------------------\n` +
                    `// INPUT (Compiled):\n${result}\n\n` +
                    `// ----------------------------------------\n` +
                    `// OUTPUT (Simulated):\n${mockResponse}\n\n` +
                    `// METRICS:\n` +
                    `// Latency: 432ms | Tokens: 145 | Cost: $0.002`
                );

                addToHistory('Run Simulation', 'Success (432ms)');
                setIsRunning(false);
            }, 1200);
        } else {
            // Real Backend Call
            const baseUrl = `${backendConfig.url}:${backendConfig.port}`;
            setConsoleOutput(`// Connecting to NeuroGate Core at ${baseUrl}...\n`);

            try {
                const response = await fetch(`${baseUrl}/api/v1/synapse/play`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        promptContent,
                        variables,
                        model: 'gpt-4'
                    })
                });

                if (!response.ok) throw new Error(`HTTP Error: ${response.status}`);

                const data = await response.json();
                const content = data?.content || JSON.stringify(data, null, 2);

                setConsoleOutput(
                    `// ----------------------------------------\n` +
                    `// OUTPUT (Real Backend):\n${content}\n\n` +
                    `// METRICS:\n` +
                    `// Latency: ${data.latency || 'N/A'}ms | Tokens: ${data.usage?.total_tokens || 'N/A'}`
                );
                addToHistory('Run Backend', 'Success');
            } catch (err: any) {
                setConsoleOutput(`// ERROR: Failed to connect to backend.\n// Details: ${err.message}\n// Hint: Check if NeuroGate Core is running.`);
                addToHistory('Run Failed', 'Connection Error');
            } finally {
                setIsRunning(false);
            }
        }
    };

    // -- Smart Versioning Helper (Levenshtein Distance) --
    const calculateSimilarity = (s1: string, s2: string): number => {
        const longer = s1.length > s2.length ? s1 : s2;
        const shorter = s1.length > s2.length ? s2 : s1;
        if (longer.length === 0) return 1.0;

        let costs = new Array();
        for (let i = 0; i <= longer.length; i++) {
            let lastValue = i;
            for (let j = 0; j <= shorter.length; j++) {
                if (i == 0) {
                    costs[j] = j;
                } else {
                    if (j > 0) {
                        let newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                        }
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0)
                costs[shorter.length] = lastValue;
        }
        return (longer.length - costs[shorter.length]) / longer.length;
    }

    const handleSave = () => {
        // Get latest version content to compare
        const latestVersion = versions[0];
        const similarity = calculateSimilarity(latestVersion.content, promptContent);

        let bumpType = "PATCH";
        let nextVer = "v1.0.0";

        // Parse latest tag (assuming vX.Y.Z format, fallback to 1.2.0 if draft)
        const parts = latestVersion.tag.replace(/[^0-9.]/g, '').split('.').map(Number);
        if (parts.length < 3) while (parts.length < 3) parts.push(0);

        if (similarity < 0.60) {
            bumpType = "MAJOR (Breaking Change)";
            parts[0]++; parts[1] = 0; parts[2] = 0;
        } else if (similarity < 0.95) {
            bumpType = "MINOR (Feature Update)";
            parts[1]++; parts[2] = 0;
        } else {
            bumpType = "PATCH (Tiny Fix)";
            parts[2]++;
        }

        nextVer = `v${parts.join('.')}`;

        setConsoleOutput(prev => prev +
            `\n\n// Calculating Semantic Diff...\n` +
            `// Similarity Score: ${(similarity * 100).toFixed(1)}%\n` +
            `// Detected Change: ${bumpType}\n` +
            `// Saving snapshot ${nextVer}... Done.`
        );

        const newVer: Version = {
            id: nextVer,
            tag: nextVer,
            content: promptContent,
            timestamp: 'Just now',
            author: 'You',
            active: false
        };

        setVersions(prev => [newVer, ...prev]);
        setActiveRightTab('versions');
        addToHistory('Saved Version', nextVer);
    };

    const handleDeploy = () => {
        setConsoleOutput(prev => prev + `\n\n// Deploying to Production... Success!\n// Traffic shifting: 0% -> 100%`);

        // Mark current top version as active
        setVersions(prev => prev.map((v, i) => ({
            ...v,
            active: i === 0 // Make the most recent one active for simulation
        })));

        addToHistory('Deployed to Prod', 'Traffic: 100%');
    };

    const handleRestore = (verId: string) => {
        const target = versions.find(v => v.id === verId);
        if (target) {
            setPromptContent(target.content);
            setConsoleOutput(`// Restored content from ${target.tag}`);
            addToHistory('Restored Version', target.tag);
        }
    };

    // Resizing Logic
    const [sidebarWidth, setSidebarWidth] = useState(320);
    const [isResizing, setIsResizing] = useState(false);

    const startResizing = useCallback((mouseDownEvent: React.MouseEvent) => {
        setIsResizing(true);
    }, []);

    const stopResizing = useCallback(() => {
        setIsResizing(false);
    }, []);

    const resize = useCallback(
        (mouseMoveEvent: MouseEvent) => {
            if (isResizing) {
                const newWidth = window.innerWidth - mouseMoveEvent.clientX;
                if (newWidth > 250 && newWidth < 800) {
                    setSidebarWidth(newWidth);
                }
            }
        },
        [isResizing]
    );

    useEffect(() => {
        window.addEventListener("mousemove", resize);
        window.addEventListener("mouseup", stopResizing);
        return () => {
            window.removeEventListener("mousemove", resize);
            window.removeEventListener("mouseup", stopResizing);
        };
    }, [resize, stopResizing]);

    const addToHistory = (action: string, details: string) => {
        const newItem: HistoryItem = {
            id: Date.now().toString(),
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            action,
            user: 'You',
            details
        };
        setHistory(prev => [newItem, ...prev]);
    };

    return (
        <div className="flex h-screen bg-slate-950 text-white overflow-hidden font-sans selection:bg-purple-500/30">
            {/* Background Effects */}
            <div className="fixed inset-0 bg-[url('/grid.svg')] bg-center opacity-20 pointer-events-none" />
            <div className="fixed top-0 left-1/4 w-96 h-96 bg-purple-500/10 rounded-full blur-[100px] pointer-events-none" />
            <div className="fixed bottom-0 right-1/4 w-96 h-96 bg-blue-500/10 rounded-full blur-[100px] pointer-events-none" />

            {/* Sidebar (Navigation) */}
            <div className="w-16 flex flex-col items-center py-6 border-r border-white/5 bg-slate-900/50 backdrop-blur-md z-20">
                <div className="mb-8 text-purple-400 text-2xl font-bold font-mono animate-pulse">N</div>
                <div className="space-y-6 w-full px-3">
                    <button
                        onClick={() => setShowHistory(false)}
                        className={`w-full aspect-square rounded-xl flex items-center justify-center transition-all duration-300 ${!showHistory
                            ? 'bg-purple-500/20 text-purple-300 shadow-[0_0_15px_rgba(168,85,247,0.3)]'
                            : 'text-slate-500 hover:text-slate-300 hover:bg-white/5'}`}
                        title="Editor"
                    >
                        <FaCodeBranch size={20} />
                    </button>
                    <button
                        onClick={() => setShowHistory(!showHistory)}
                        className={`w-full aspect-square rounded-xl flex items-center justify-center transition-all duration-300 ${showHistory
                            ? 'bg-purple-500/20 text-purple-300 shadow-[0_0_15px_rgba(168,85,247,0.3)]'
                            : 'text-slate-500 hover:text-slate-300 hover:bg-white/5'}`}
                        title="History"
                    >
                        <FaHistory size={20} />
                    </button>
                </div>
            </div>

            {/* Main Workspace */}
            <div className="flex-1 flex flex-col min-w-0 z-10 relative">

                {/* Top Bar */}
                <div className="h-16 border-b border-white/5 flex items-center justify-between px-6 bg-slate-900/30 backdrop-blur-md">
                    <div className="flex items-center space-x-4">
                        <div className="flex flex-col">
                            <h1 className="text-lg font-bold tracking-tight text-white flex items-center gap-2">
                                Synapse Studio
                                <span className="px-2 py-0.5 rounded-full bg-purple-500/10 border border-purple-500/20 text-[10px] text-purple-300 font-medium uppercase tracking-wider">
                                    v2.4.0
                                </span>
                            </h1>
                            <span className="flex items-center space-x-2 text-xs text-slate-400 font-mono">
                                <FaTerminal size={10} className="text-slate-500" />
                                <span>prompt-engineering / support-bot</span>
                            </span>
                        </div>
                    </div>

                    <div className="flex items-center space-x-3">
                        <div className="flex items-center bg-black/40 rounded-lg px-3 py-1.5 border border-white/5 mr-4 shadow-inner">
                            <span className="text-xs text-emerald-400 font-mono flex items-center gap-2">
                                <span className="relative flex h-2 w-2">
                                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                                    <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
                                </span>
                                Redis Connected
                            </span>
                        </div>

                        {/* Simulation Toggle */}
                        <div className="flex items-center bg-white/5 rounded-lg p-1 mr-2 border border-white/10">
                            <button
                                onClick={() => setSimulationMode(true)}
                                className={`px-3 py-1 text-xs font-medium rounded transition-all ${simulationMode ? 'bg-purple-500 text-white shadow-lg' : 'text-slate-400 hover:text-white'}`}
                            >
                                Simulation
                            </button>
                            <button
                                onClick={() => setSimulationMode(false)}
                                className={`px-3 py-1 text-xs font-medium rounded transition-all ${!simulationMode ? 'bg-emerald-500 text-white shadow-lg' : 'text-slate-400 hover:text-white'}`}
                            >
                                Real Backend
                            </button>
                        </div>

                        {/* Settings Button */}
                        <div className="relative">
                            <button
                                onClick={() => setShowSettings(!showSettings)}
                                className={`p-2 rounded-lg transition-all ${showSettings ? 'bg-white/10 text-white' : 'text-slate-400 hover:text-white hover:bg-white/5'}`}
                            >
                                <FaCog size={14} />
                            </button>

                            {/* Settings Popup */}
                            {showSettings && (
                                <div className="absolute top-full right-0 mt-2 w-64 bg-slate-900 border border-white/10 rounded-xl shadow-2xl p-4 z-50 backdrop-blur-xl">
                                    <h3 className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-3">Backend Configuration</h3>
                                    <div className="space-y-3">
                                        <div>
                                            <label className="text-[10px] text-slate-500 block mb-1">Base URL</label>
                                            <input
                                                type="text"
                                                value={backendConfig.url}
                                                onChange={(e) => setBackendConfig(prev => ({ ...prev, url: e.target.value }))}
                                                className="w-full bg-black/40 border border-white/10 rounded px-2 py-1 text-xs text-white focus:border-purple-500 outline-none"
                                            />
                                        </div>
                                        <div>
                                            <label className="text-[10px] text-slate-500 block mb-1">Port</label>
                                            <input
                                                type="text"
                                                value={backendConfig.port}
                                                onChange={(e) => setBackendConfig(prev => ({ ...prev, port: e.target.value }))}
                                                className="w-full bg-black/40 border border-white/10 rounded px-2 py-1 text-xs text-white focus:border-purple-500 outline-none"
                                            />
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>

                        <button
                            onClick={handleRun}
                            disabled={isRunning}
                            className={`flex items-center space-x-2 px-5 py-2 rounded-lg text-sm font-semibold tracking-wide transition-all duration-300 ${isRunning
                                ? 'bg-slate-700 cursor-not-allowed opacity-50'
                                : 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-[0_0_20px_rgba(16,185,129,0.4)] hover:scale-105'
                                }`}
                        >
                            <FaPlay size={10} className={isRunning ? 'animate-spin' : ''} />
                            <span>{isRunning ? 'Running...' : 'Run'}</span>
                        </button>

                        <div className="h-6 w-px bg-white/10 mx-2" />

                        <button
                            onClick={handleSave}
                            className="flex items-center space-x-2 px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/10 text-white rounded-lg text-xs font-semibold transition-all hover:border-white/20"
                        >
                            <FaSave size={12} />
                            <span>Save Snapshot</span>
                        </button>

                        <button
                            onClick={handleDeploy}
                            className="flex items-center space-x-2 px-5 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-lg text-sm font-semibold transition-all shadow-[0_0_20px_rgba(147,51,234,0.4)] hover:scale-105 group"
                        >
                            <FaRocket size={12} className="group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
                            <span>Deploy</span>
                        </button>
                    </div>
                </div>

                {/* Split Pane Layout */}
                <div className="flex-1 flex overflow-hidden">

                    {/* Left Pane: History (Conditional) */}
                    {showHistory ? (
                        <div className="w-80 border-r border-white/5 bg-black/20 backdrop-blur-sm p-4 overflow-auto">
                            <h3 className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-4 px-2">Timeline</h3>
                            <HistoryList history={history} />
                        </div>
                    ) : null}

                    {/* Center Pane: Editor & Console */}
                    <div className="flex-1 flex flex-col min-w-0 bg-black/40">
                        {/* Editor */}
                        <div className="flex-1 relative p-6">
                            <div className="absolute inset-0 p-6 flex flex-col">
                                <div className="flex-1 rounded-xl overflow-hidden border border-white/10 shadow-2xl relative group">
                                    <div className="absolute inset-x-0 top-0 h-[1px] bg-gradient-to-r from-transparent via-purple-500/50 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500 z-10" />
                                    <PromptEditor
                                        value={promptContent}
                                        onChange={(val) => setPromptContent(val || '')}
                                        theme="prompt-theme"
                                    />
                                </div>
                            </div>
                        </div>

                        {/* Console */}
                        <div className="h-72 border-t border-white/5 bg-black/60 backdrop-blur-md flex flex-col shadow-[0_-5px_20px_rgba(0,0,0,0.3)]">
                            <div className="h-9 border-b border-white/5 bg-white/5 px-4 flex items-center justify-between">
                                <span className="text-[10px] font-bold text-purple-400 uppercase tracking-widest flex items-center gap-2">
                                    <FaTerminal /> Interactive Console
                                </span>
                                <button
                                    onClick={() => setConsoleOutput("// Ready to run...")}
                                    className="text-[10px] text-slate-500 hover:text-white transition-colors"
                                >
                                    Clear Output
                                </button>
                            </div>
                            <div className="flex-1 p-4 font-mono text-sm overflow-auto custom-scrollbar selection:bg-purple-900/50">
                                <pre className="whitespace-pre-wrap text-slate-300 leading-relaxed font-light">
                                    {consoleOutput}
                                </pre>
                            </div>
                        </div>
                    </div>

                    {/* Drag Handle */}
                    <div
                        className={`w-1 cursor-col-resize bg-transparent hover:bg-purple-500/50 transition-colors z-50 flex items-center justify-center group ${isResizing ? 'bg-purple-500/50' : ''}`}
                        onMouseDown={startResizing}
                    >
                        <div className="h-8 w-1 rounded-full bg-white/20 group-hover:bg-purple-400 transition-colors" />
                    </div>

                    {/* Right Pane: Context (Variables / Versions) */}
                    <div
                        style={{ width: sidebarWidth }}
                        className="border-l border-white/5 bg-slate-900/40 backdrop-blur-md flex flex-col shrink-0"
                    >
                        <div className="flex border-b border-white/5">
                            <button
                                onClick={() => setActiveRightTab('variables')}
                                className={`flex-1 py-4 text-[10px] font-bold uppercase tracking-widest transition-all ${activeRightTab === 'variables'
                                    ? 'text-purple-400 border-b-2 border-purple-500 bg-purple-500/5'
                                    : 'text-slate-500 hover:text-slate-300 hover:bg-white/5'}`}
                            >
                                Variables
                            </button>
                            <button
                                onClick={() => setActiveRightTab('versions')}
                                className={`flex-1 py-4 text-[10px] font-bold uppercase tracking-widest transition-all ${activeRightTab === 'versions'
                                    ? 'text-purple-400 border-b-2 border-purple-500 bg-purple-500/5'
                                    : 'text-slate-500 hover:text-slate-300 hover:bg-white/5'}`}
                            >
                                Versions
                            </button>
                        </div>

                        <div className="flex-1 overflow-auto p-0 custom-scrollbar">
                            {activeRightTab === 'variables' ? (
                                <div className="p-4 space-y-4">
                                    <div className="p-3 bg-purple-500/10 rounded-lg border border-purple-500/20 mb-4">
                                        <p className="text-xs text-purple-200 leading-relaxed">
                                            Define variables for your prompt template. These mock values will be injected during simulation runs.
                                        </p>
                                    </div>
                                    <VariableForm
                                        promptContent={promptContent}
                                        variables={variables}
                                        onChange={(key, val) => setVariables(prev => ({ ...prev, [key]: val }))}
                                    />
                                </div>
                            ) : (
                                <div className="p-0">
                                    <VersionsList
                                        versions={versions}
                                        onRestore={handleRestore}
                                    />
                                </div>
                            )}
                        </div>

                        {/* Mini Graph Preview */}
                        {activeRightTab === 'versions' && (
                            <div className="h-56 border-t border-white/5 bg-black/40 p-0 relative">
                                <div className="absolute top-2 left-3 z-10 text-[10px] text-slate-500 uppercase tracking-wider font-bold bg-black/50 px-2 py-0.5 rounded backdrop-blur-sm border border-white/5">
                                    Commit Graph
                                </div>
                                <VersionGraph versions={versions} />
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
