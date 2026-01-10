"use client";

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import PromptEditor from './components/PromptEditor';
import VariableForm from './components/VariableForm';
import VersionGraph from './components/VersionGraph';
import HistoryList from './components/HistoryList';
import VersionsList from './components/VersionsList';
import TestsPanel from './components/TestsPanel';
import OptimizationModal from './components/OptimizationModal';
import DeployModal from './components/DeployModal';
import { FaPlay, FaSave, FaRocket, FaCodeBranch, FaHistory, FaColumns, FaTerminal, FaCog, FaSpinner, FaRobot, FaMagic, FaTimes, FaCheck, FaFlask } from 'react-icons/fa';
import { motion, AnimatePresence } from 'framer-motion';
import { DiffEditor } from '@monaco-editor/react';

// --- Interfaces ---

interface PromptVersionDTO {
    versionId: string;
    promptText: string;
    majorVersion: number;
    minorVersion: number;
    patchVersion: number;
    author: string;
    timestamp: string; // ISO string
    commitMessage: string;
    branchName: string;
    // metrics
    averageLatency?: number;
    usageCount?: number;
}

interface Version {
    id: string;
    tag: string;
    content: string;
    timestamp: string;
    author: string;
    active: boolean; // active in UI viewer
    isLive?: boolean; // deployed to prod
}

interface HistoryItem {
    id: string;
    timestamp: string;
    action: string;
    user: string;
    details: string;
}

interface OptimizationResult {
    originalPrompt: string;
    optimizedPrompt: string;
    explanation: string;
    objective: string;
}

const OPTIMIZATION_OBJECTIVES = [
    { id: 'FIX_GRAMMAR', label: 'Fix Grammar & Tone' },
    { id: 'CONCISE', label: 'Make Concise' },
    { id: 'REASONING', label: 'Enhance Reasoning (CoT)' },
    { id: 'FEW_SHOT', label: 'Generate Few-Shot Examples' }
];

// --- Component ---

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

    const [activeRightTab, setActiveRightTab] = useState<'variables' | 'versions' | 'tests'>('variables');
    const [showHistory, setShowHistory] = useState(false);

    // Testing State
    const [testCases, setTestCases] = useState<any[]>([]); // Using 'any' for now to match TestsPanel interface
    const [isRunningTests, setIsRunningTests] = useState(false);
    const [consoleOutput, setConsoleOutput] = useState<string>("// Ready to run...");
    const [isRunning, setIsRunning] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [isDeploying, setIsDeploying] = useState(false);
    const [showDeployModal, setShowDeployModal] = useState(false);
    const [isLoadingVersions, setIsLoadingVersions] = useState(false);

    // Optimizer State
    const [showOptimizer, setShowOptimizer] = useState(false);
    const [optimizationObjective, setOptimizationObjective] = useState(OPTIMIZATION_OBJECTIVES[0].id);
    const [isOptimizing, setIsOptimizing] = useState(false);
    const [optimizationResult, setOptimizationResult] = useState<OptimizationResult | null>(null);

    // Backend Configuration
    const [simulationMode, setSimulationMode] = useState(true);
    const [showSettings, setShowSettings] = useState(false);
    const [backendConfig, setBackendConfig] = useState({
        url: 'http://localhost',
        port: '8080'
    });

    // Data State
    const [versions, setVersions] = useState<Version[]>([]);
    const [history, setHistory] = useState<HistoryItem[]>([
        { id: 'h1', timestamp: '10:00', action: 'System Init', user: 'System', details: 'Synapse Studio Loaded' }
    ]);

    // -- Helpers --

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

    const getBaseUrl = () => `${backendConfig.url}:${backendConfig.port}`;

    // -- Effects --

    // Fetch versions when switching to Real Backend or on mount if already real
    useEffect(() => {
        if (!simulationMode) {
            fetchVersions();
        } else {
            // Load mock versions for simulation mode
            setVersions([
                { id: 'v1.0', tag: 'v1.0.0', content: "You are a specialized assistant...", timestamp: '2 days ago', author: 'System', active: false },
                { id: 'v1.1', tag: 'v1.1.0', content: "You are a helpful AI assistant...", timestamp: 'Yesterday', author: 'User', active: false },
                { id: 'v1.2', tag: 'v1.2.0-draft', content: promptContent, timestamp: 'Today', author: 'You', active: true },
            ]);
        }
    }, [simulationMode]);

    const fetchVersions = async () => {
        setIsLoadingVersions(true);
        try {
            // Fetch versions
            const versionsRes = await fetch(`${getBaseUrl()}/api/prompts/versions?branchName=main`);
            if (!versionsRes.ok) throw new Error("Failed to fetch versions");
            const data: PromptVersionDTO[] = await versionsRes.json();

            // Fetch workflow status
            let workflow: any = null;
            try {
                const wfRes = await fetch(`${getBaseUrl()}/api/v1/synapse/prompts/default-prompt/workflow`);
                if (wfRes.ok) {
                    workflow = await wfRes.json();
                }
            } catch (e) {
                console.warn("Could not fetch workflow", e);
            }

            const mappedVersions: Version[] = data.map((v, idx) => ({
                id: v.versionId,
                tag: `v${v.majorVersion}.${v.minorVersion}.${v.patchVersion}`,
                content: v.promptText,
                timestamp: new Date(v.timestamp).toLocaleString(),
                author: v.author || 'Unknown',
                active: idx === 0,
                isLive: workflow ? workflow.activeProductionVersionId === v.versionId : v.branchName === 'main',
                isShadow: workflow ? workflow.activeShadowVersionId === v.versionId : false
            }));

            // Sort by timestamp desc
            mappedVersions.sort((a, b) => new Date(b.timestamp).valueOf() - new Date(a.timestamp).valueOf());

            setVersions(mappedVersions);
        } catch (err) {
            console.error(err);
            addToHistory('Fetch Error', 'Could not load versions');
        } finally {
            setIsLoadingVersions(false);
        }
    };

    // -- Handlers --

    const handleRun = async () => {
        setIsRunning(true);

        if (simulationMode) {
            setConsoleOutput("// Running simulation against 'gpt-4-turbo'...\n");

            // Simulate API call delay
            setTimeout(() => {
                let result = promptContent;
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
            }, 1000);
        } else {
            // Real Backend Call
            setConsoleOutput(`// Connecting to NeuroGate Core at ${getBaseUrl()}...\n`);

            try {
                const response = await fetch(`${getBaseUrl()}/api/v1/synapse/play`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        promptContent,
                        variables,
                        model: 'gpt-4'
                    })
                });

                if (!response.ok) {
                    const errText = await response.text();
                    throw new Error(`HTTP ${response.status}: ${errText}`);
                }

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

    const handleSave = async () => {
        setIsSaving(true);
        if (simulationMode) {
            // Mock Save
            setTimeout(() => {
                const parts = versions.length > 0 ? versions[0].tag.replace('v', '').split('.').map(Number) : [1, 0, 0];
                parts[2]++; // patch bump
                const nextVer = `v${parts.join('.')}`;

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
                addToHistory('Saved Snapshot', nextVer);
                setIsSaving(false);
            }, 800);
        } else {
            // Real Commit
            try {
                const res = await fetch(`${getBaseUrl()}/api/prompts/commit`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        promptText: promptContent,
                        commitMessage: "Update from Synapse Studio",
                        author: "User", // TODO: Get real user
                        branchName: "main"
                    })
                });

                if (!res.ok) throw new Error("Failed to commit");

                await fetchVersions();
                setActiveRightTab('versions');
                addToHistory('Committed', 'Saved to Main Branch');
            } catch (err: any) {
                setConsoleOutput(`// ERROR: Failed to save.\n// ${err.message}`);
                addToHistory('Save Failed', err.message);
            } finally {
                setIsSaving(false);
            }
        }
    };

    const handleDeployClick = () => {
        setShowDeployModal(true);
    };

    const handleConfirmDeploy = async (environment: string) => {
        setIsDeploying(true);
        if (simulationMode) {
            setTimeout(() => {
                setConsoleOutput(prev => prev + `\n\n// Deploying to ${environment.toUpperCase()}... Success!\n// Traffic shifting to ${environment}...`);
                addToHistory(`Deployed to ${environment}`, 'Success');
                setIsDeploying(false);
                setShowDeployModal(false);
            }, 1000);
        } else {
            try {
                if (versions.length === 0) throw new Error("No version to deploy");
                const versionId = versions[0].id; // Latest for now

                const res = await fetch(`${getBaseUrl()}/api/v1/synapse/deploy`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        promptName: "default-prompt", // TODO real name
                        versionId: versionId,
                        environment: environment,
                        user: "User"
                    })
                });

                if (!res.ok) throw new Error("Deploy failed");

                addToHistory('Deployed', `Version ${versionId} to ${environment}`);
                setConsoleOutput(prev => prev + `\n// Deployment to ${environment} Successful!`);

                // Refresh workflow/versions to show new badges
                await fetchVersions();
            } catch (err: any) {
                setConsoleOutput(`// DEPLOY ERROR: ${err.message}`);
                addToHistory('Deploy Failed', err.message);
            } finally {
                setIsDeploying(false);
                setShowDeployModal(false);
            }
        }
    };



    const handleOptimize = async () => {
        setIsOptimizing(true);
        setOptimizationResult(null);
        try {
            if (simulationMode) {
                // Mock Optimization
                setTimeout(() => {
                    setOptimizationResult({
                        originalPrompt: promptContent,
                        optimizedPrompt: "You are a highly capable AI assistant named {{ bot_name }}.\n\nContext: Current date is {{ date }}.\n\nTask:\nUser asks: {{ user_query }}\n\nStep-by-step reasoning:\n1. Analyze the user's intent.\n2. Formulate a concise and accurate response.\n3. Ensure tone is professional.\n\nAnswer:",
                        explanation: "Added 'highly capable' persona, structured the prompt with 'Context' and 'Task' sections, and included a 'Step-by-step reasoning' block (Chain-of-Thought) to improve logic.",
                        objective: optimizationObjective
                    });
                    setIsOptimizing(false);
                }, 1500);
            } else {
                // Real Backend
                const res = await fetch(`${getBaseUrl()}/api/v1/synapse/optimize`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        originalPrompt: promptContent,
                        objective: optimizationObjective,
                        modelPreference: 'gpt-4'
                    })
                });

                if (!res.ok) throw new Error("Optimization failed");
                const data = await res.json();
                setOptimizationResult(data);
                setIsOptimizing(false);
            }
        } catch (err: any) {
            setConsoleOutput(`// OPTIMIZER ERROR: ${err.message}`);
            setIsOptimizing(false);
        }
    };

    const handleAcceptOptimization = () => {
        if (optimizationResult) {
            setPromptContent(optimizationResult.optimizedPrompt);
            setShowOptimizer(false);
            setOptimizationResult(null);
            addToHistory('Optimized Prompt', optimizationResult.objective);
        }
    };

    const handleRestore = (verId: string) => {
        const target = versions.find(v => v.id === verId);
        if (target) {
            setPromptContent(target.content);
            setConsoleOutput(`// Restored content from ${target.tag}`);
            addToHistory('Restored Version', target.tag);
            // set this version as active in UI
            setVersions(prev => prev.map(v => ({ ...v, active: v.id === verId })));
        }
    };

    const handleRunTests = async (cases: any[]) => {
        setIsRunningTests(true);
        try {
            if (simulationMode) {
                // Mock Test Run
                setTimeout(() => {
                    const results = cases.map(c => ({
                        ...c,
                        status: Math.random() > 0.3 ? 'pass' : 'fail',
                        actual: c.status === 'pass' ? c.expected : "Unexpected output from model...",
                        score: Math.random() > 0.3 ? 100 : 45,
                        reason: "Eval score based on semantic similarity."
                    }));
                    setTestCases(results);
                    setIsRunningTests(false);
                    addToHistory('Run Tests', `Completed (${results.filter((r: any) => r.status === 'pass').length}/${results.length} Passed)`);
                }, 2000);
            } else {
                // Real Backend Ad-Hoc Eval
                const res = await fetch(`${getBaseUrl()}/api/v1/cortex/evaluate`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        promptTemplate: promptContent,
                        model: "gpt-4",
                        testCases: cases.map(c => ({
                            id: c.id,
                            input: c.input,
                            expectedOutput: c.expected
                        }))
                    })
                });

                if (!res.ok) throw new Error("Evaluation failed");
                const data = await res.json();

                // Merge results back
                const updatedCases = cases.map(c => {
                    const result = data.results.find((r: any) => r.caseId === c.id);
                    if (result) {
                        return {
                            ...c,
                            status: result.passed ? 'pass' : 'fail',
                            actual: result.actualOutput,
                            score: result.score,
                            reason: result.reason
                        };
                    }
                    return c;
                });

                setTestCases(updatedCases);
                setIsRunningTests(false);
                addToHistory('Run Tests', `Completed (Score: ${data.overallScore.toFixed(0)}%)`);
            }
        } catch (err: any) {
            setConsoleOutput(`// TEST ERROR: ${err.message}`);
            setIsRunningTests(false);
        }
    };

    // Resizing Logic
    const [sidebarWidth, setSidebarWidth] = useState(320);
    const [isResizing, setIsResizing] = useState(false);

    const startResizing = useCallback(() => setIsResizing(true), []);
    const stopResizing = useCallback(() => setIsResizing(false), []);
    const resize = useCallback((e: MouseEvent) => {
        if (isResizing) {
            const newWidth = window.innerWidth - e.clientX;
            if (newWidth > 250 && newWidth < 800) setSidebarWidth(newWidth);
        }
    }, [isResizing]);

    useEffect(() => {
        window.addEventListener("mousemove", resize);
        window.addEventListener("mouseup", stopResizing);
        return () => {
            window.removeEventListener("mousemove", resize);
            window.removeEventListener("mouseup", stopResizing);
        };
    }, [resize, stopResizing]);

    return (
        <div className="flex h-screen bg-slate-950 text-white overflow-hidden font-sans selection:bg-purple-500/30">
            {/* Background Effects */}
            <div className="fixed inset-0 bg-[url('/grid.svg')] bg-center opacity-20 pointer-events-none" />
            <motion.div
                animate={{ scale: [1, 1.2, 1], opacity: [0.1, 0.15, 0.1] }}
                transition={{ duration: 10, repeat: Infinity, repeatType: "reverse" }}
                className="fixed top-0 left-1/4 w-96 h-96 bg-purple-500/10 rounded-full blur-[100px] pointer-events-none"
            />
            <motion.div
                animate={{ scale: [1, 1.1, 1], opacity: [0.1, 0.15, 0.1] }}
                transition={{ duration: 8, repeat: Infinity, repeatType: "reverse", delay: 2 }}
                className="fixed bottom-0 right-1/4 w-96 h-96 bg-blue-500/10 rounded-full blur-[100px] pointer-events-none"
            />

            {/* Sidebar (Navigation) */}
            <div className="w-16 flex flex-col items-center py-6 border-r border-white/5 bg-slate-900/50 backdrop-blur-md z-20">
                <div className="mb-8 text-purple-400 text-2xl font-bold font-mono animate-pulse">N</div>
                <div className="space-y-6 w-full px-3">
                    <NavIcon icon={<FaCodeBranch size={20} />} active={!showHistory} onClick={() => setShowHistory(false)} tooltip="Editor" />
                    <NavIcon icon={<FaHistory size={20} />} active={showHistory} onClick={() => setShowHistory(true)} tooltip="History" />
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
                                    beta
                                </span>
                            </h1>
                            <span className="flex items-center space-x-2 text-xs text-slate-400 font-mono">
                                <FaTerminal size={10} className="text-slate-500" />
                                <span>main / playground-prompt</span>
                            </span>
                        </div>
                    </div>

                    <div className="flex items-center space-x-3">
                        <AnimatePresence>
                            {!simulationMode && (
                                <motion.div
                                    initial={{ opacity: 0, scale: 0.9 }}
                                    animate={{ opacity: 1, scale: 1 }}
                                    exit={{ opacity: 0, scale: 0.9 }}
                                    className="flex items-center bg-emerald-500/10 rounded-lg px-3 py-1.5 border border-emerald-500/20 mr-4 shadow-inner"
                                >
                                    <span className="text-xs text-emerald-400 font-mono flex items-center gap-2">
                                        <div className="relative flex h-2 w-2">
                                            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                                            <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
                                        </div>
                                        Core Connected
                                    </span>
                                </motion.div>
                            )}
                        </AnimatePresence>

                        {/* Optimizer Button */}
                        <button
                            onClick={() => setShowOptimizer(true)}
                            className="p-2 rounded-lg bg-purple-500/10 text-purple-400 border border-purple-500/20 hover:bg-purple-500/20 hover:text-white transition-all mr-2 group relative"
                            title="Neuro-Optimizer"
                        >
                            <FaMagic size={14} className="group-hover:animate-pulse" />
                            <span className="absolute -top-1 -right-1 flex h-2 w-2">
                                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-purple-400 opacity-75"></span>
                                <span className="relative inline-flex rounded-full h-2 w-2 bg-purple-500"></span>
                            </span>
                        </button>

                        {/* Simulation Toggle */}
                        <div className="flex items-center bg-white/5 rounded-lg p-1 mr-2 border border-white/10">
                            <button
                                onClick={() => setSimulationMode(true)}
                                className={`px-3 py-1 text-xs font-medium rounded transition-all ${simulationMode ? 'bg-purple-600 text-white shadow-lg' : 'text-slate-400 hover:text-white'}`}
                            >
                                Simulation
                            </button>
                            <button
                                onClick={() => setSimulationMode(false)}
                                className={`px-3 py-1 text-xs font-medium rounded transition-all ${!simulationMode ? 'bg-emerald-600 text-white shadow-lg' : 'text-slate-400 hover:text-white'}`}
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
                            <AnimatePresence>
                                {showSettings && (
                                    <motion.div
                                        initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }}
                                        className="absolute top-full right-0 mt-2 w-64 bg-slate-900 border border-white/10 rounded-xl shadow-2xl p-4 z-50 backdrop-blur-xl"
                                    >
                                        <h3 className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-3">Backend Configuration</h3>
                                        <div className="space-y-3">
                                            <InputGroup label="Base URL" value={backendConfig.url} onChange={v => setBackendConfig({ ...backendConfig, url: v })} />
                                            <InputGroup label="Port" value={backendConfig.port} onChange={v => setBackendConfig({ ...backendConfig, port: v })} />
                                        </div>
                                    </motion.div>
                                )}
                            </AnimatePresence>
                        </div>

                        <ActionButton
                            onClick={handleRun}
                            disabled={isRunning}
                            icon={isRunning ? <FaSpinner className="animate-spin" /> : <FaPlay />}
                            label={isRunning ? 'Running...' : 'Run'}
                            variant="primary"
                        />

                        <div className="h-6 w-px bg-white/10 mx-2" />

                        <ActionButton
                            onClick={handleSave}
                            disabled={isSaving}
                            icon={isSaving ? <FaSpinner className="animate-spin" /> : <FaSave />}
                            label="Save"
                            variant="secondary"
                        />

                        <ActionButton
                            onClick={handleDeployClick}
                            disabled={isDeploying}
                            icon={isDeploying ? <FaSpinner className="animate-spin" /> : <FaRocket />}
                            label="Deploy"
                            variant="deploy"
                        />
                    </div>
                </div>

                {/* Split Pane Layout */}
                <div className="flex-1 flex overflow-hidden">

                    {/* Left Pane: History (Conditional) */}
                    <AnimatePresence mode="popLayout">
                        {showHistory && (
                            <motion.div
                                initial={{ width: 0, opacity: 0 }}
                                animate={{ width: 320, opacity: 1 }}
                                exit={{ width: 0, opacity: 0 }}
                                className="border-r border-white/5 bg-black/20 backdrop-blur-sm p-4 overflow-auto"
                            >
                                <h3 className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-4 px-2">Timeline</h3>
                                <HistoryList history={history} />
                            </motion.div>
                        )}
                    </AnimatePresence>

                    {/* Center Pane: Editor & Console */}
                    <div className="flex-1 flex flex-col min-w-0 bg-black/40">
                        {/* Editor */}
                        <div className="flex-1 relative p-6">
                            <div className="absolute inset-0 p-6 flex flex-col">
                                <div className="flex-1 rounded-xl overflow-hidden border border-white/10 shadow-2xl relative group bg-slate-900/50">
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
                            <TabButton label="Variables" active={activeRightTab === 'variables'} onClick={() => setActiveRightTab('variables')} />
                            <TabButton label="Versions" active={activeRightTab === 'versions'} onClick={() => setActiveRightTab('versions')} />
                            <TabButton label="Tests" active={activeRightTab === 'tests'} onClick={() => setActiveRightTab('tests')} />
                        </div>

                        <div className="flex-1 overflow-auto p-0 custom-scrollbar relative">
                            {isLoadingVersions && (
                                <div className="absolute inset-0 flex items-center justify-center bg-slate-900/80 z-20">
                                    <FaSpinner className="animate-spin text-purple-500" size={24} />
                                </div>
                            )}

                            <AnimatePresence mode="wait">
                                {activeRightTab === 'variables' ? (
                                    <motion.div
                                        key="vars"
                                        initial={{ opacity: 0, x: 20 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        exit={{ opacity: 0, x: -20 }}
                                        transition={{ duration: 0.2 }}
                                        className="p-4 space-y-4"
                                    >
                                        <div className="p-3 bg-purple-500/10 rounded-lg border border-purple-500/20 mb-4">
                                            <p className="text-xs text-purple-200 leading-relaxed flex items-start gap-2">
                                                <FaRobot className="mt-1 shrink-0" />
                                                Define variables for your prompt template. Mock values are injected during simulation.
                                            </p>
                                        </div>
                                        <VariableForm
                                            promptContent={promptContent}
                                            variables={variables}
                                            onChange={(key, val) => setVariables(prev => ({ ...prev, [key]: val }))}
                                        />
                                    </motion.div>
                                ) : activeRightTab === 'versions' ? (
                                    <motion.div
                                        key="vers"
                                        initial={{ opacity: 0, x: 20 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        exit={{ opacity: 0, x: -20 }}
                                        transition={{ duration: 0.2 }}
                                        className="p-0"
                                    >
                                        <VersionsList
                                            versions={versions}
                                            onRestore={handleRestore}
                                        />
                                    </motion.div>
                                ) : (
                                    <motion.div
                                        key="tests"
                                        initial={{ opacity: 0, x: 20 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        exit={{ opacity: 0, x: -20 }}
                                        transition={{ duration: 0.2 }}
                                        className="h-full"
                                    >
                                        <TestsPanel
                                            testCases={testCases}
                                            setTestCases={setTestCases}
                                            onRunTests={handleRunTests}
                                            isRunning={isRunningTests}
                                            promptContent={promptContent}
                                        />
                                    </motion.div>
                                )}
                            </AnimatePresence>
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

            <OptimizationModal
                isOpen={showOptimizer}
                onClose={() => setShowOptimizer(false)}
                original={promptContent}
                result={optimizationResult}
                isOptimizing={isOptimizing}
                objective={optimizationObjective}
                setObjective={setOptimizationObjective}
                objectives={OPTIMIZATION_OBJECTIVES}
                onOptimize={handleOptimize}
                onAccept={handleAcceptOptimization}
            />

            <DeployModal
                isOpen={showDeployModal}
                onClose={() => setShowDeployModal(false)}
                onDeploy={handleConfirmDeploy}
                isDeploying={isDeploying}
            />
        </div>
    );
}

// --- Sub Components ---

const NavIcon = ({ icon, active, onClick, tooltip }: any) => (
    <button
        onClick={onClick}
        className={`w-full aspect-square rounded-xl flex items-center justify-center transition-all duration-300 ${active
            ? 'bg-purple-500/20 text-purple-300 shadow-[0_0_15px_rgba(168,85,247,0.3)]'
            : 'text-slate-500 hover:text-slate-300 hover:bg-white/5'}`}
        title={tooltip}
    >
        {icon}
    </button>
);

const ActionButton = ({ onClick, disabled, icon, label, variant }: any) => {
    const styles: any = {
        primary: 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-[0_0_20px_rgba(16,185,129,0.4)]',
        secondary: 'bg-white/5 hover:bg-white/10 border border-white/10 text-white hover:border-white/20',
        deploy: 'bg-purple-600 hover:bg-purple-500 text-white shadow-[0_0_20px_rgba(147,51,234,0.4)]'
    };

    return (
        <button
            onClick={onClick}
            disabled={disabled}
            className={`flex items-center space-x-2 px-4 py-2 rounded-lg text-sm font-semibold tracking-wide transition-all duration-300 hover:scale-105 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100 ${styles[variant]}`}
        >
            <span className="text-xs">{icon}</span>
            <span className="text-xs">{label}</span>
        </button>
    );
};

const TabButton = ({ label, active, onClick }: any) => (
    <button
        onClick={onClick}
        className={`flex-1 py-4 text-[10px] font-bold uppercase tracking-widest transition-all ${active
            ? 'text-purple-400 border-b-2 border-purple-500 bg-purple-500/5'
            : 'text-slate-500 hover:text-slate-300 hover:bg-white/5'}`}
    >
        {label}
    </button>
);

const InputGroup = ({ label, value, onChange }: any) => (
    <div>
        <label className="text-[10px] text-slate-500 block mb-1">{label}</label>
        <input
            type="text"
            value={value}
            onChange={(e) => onChange(e.target.value)}
            className="w-full bg-black/40 border border-white/10 rounded px-2 py-1 text-xs text-white focus:border-purple-500 outline-none transition-colors"
        />
    </div>
);
