'use client';

import React, { useState, useEffect } from 'react';
import Timeline from '../components/debugger/Timeline';
import StateInspector from '../components/debugger/StateInspector';
import ReplayPlayer from '../components/debugger/ReplayPlayer';

// Mock Data (will be replaced by API call)
const MOCK_SESSION = {
    sessionId: "sess_12345",
    steps: [
        { stepId: "step_1", timestamp: "2024-01-01T15:12:00.000Z", stepType: "USER_INPUT", content: "Write a python script to scrape data", state: {} },
        { stepId: "step_2", timestamp: "2024-01-01T15:12:05.000Z", stepType: "TOOL_CALL", content: "search_web({ query: 'python scraping libraries' })", state: { memory: "empty" } },
        { stepId: "step_3", timestamp: "2024-01-01T15:12:10.000Z", stepType: "TOOL_RESULT", content: "Found: BeautifulSoup, Scrapy, Selenium...", state: { memory: "context_loaded" } },
        { stepId: "step_4", timestamp: "2024-01-01T15:12:15.000Z", stepType: "MODEL_RESPONSE", content: "Here is a script using BeautifulSoup...", state: { memory: "final_answer" } }
    ]
};

export default function DebuggerPage() {
    const [currentStepIndex, setCurrentStepIndex] = useState(0);
    const [isPlaying, setIsPlaying] = useState(false);
    const [session, setSession] = useState(MOCK_SESSION);

    useEffect(() => {
        let interval: NodeJS.Timeout;
        if (isPlaying) {
            interval = setInterval(() => {
                setCurrentStepIndex(prev => {
                    if (prev < session.steps.length - 1) return prev + 1;
                    setIsPlaying(false);
                    return prev;
                });
            }, 1000);
        }
        return () => clearInterval(interval);
    }, [isPlaying, session.steps.length]);

    return (
        <div className="flex h-screen bg-slate-950 text-white font-sans overflow-hidden grid-background selection:bg-accent-500/30">
            {/* Left Sidebar: Timeline */}
            <div className="w-1/4 h-full border-r border-white/5 flex flex-col glass bg-black/40">
                <div className="p-4 border-b border-white/5 bg-white/5">
                    <h1 className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-primary-400 to-accent-400">
                        NeuroKernel Debugger
                    </h1>
                    <div className="flex items-center gap-2 mt-2">
                        <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse"></span>
                        <p className="text-xs text-slate-400 font-mono">Session: {session.sessionId}</p>
                    </div>
                </div>
                <Timeline
                    steps={session.steps}
                    currentStepIndex={currentStepIndex}
                    onStepClick={setCurrentStepIndex}
                />
            </div>

            {/* Main Content: State & Player */}
            <div className="flex-1 flex flex-col h-full relative bg-gradient-to-br from-slate-950 to-primary-950/20">
                <div className="flex-1 overflow-hidden relative">
                    <div className="absolute top-4 right-4 z-10">
                        <button className="glass-button border-primary-500/30 text-primary-200 hover:bg-primary-500/20 hover:text-white flex items-center gap-2">
                            <span className="text-lg">🌿</span> Forks & Variations
                        </button>
                    </div>

                    {/* Visualizer Area (State Inspector) */}
                    <div className="h-full">
                        <StateInspector step={session.steps[currentStepIndex]} />
                    </div>
                </div>

                {/* Bottom Bar: Player Controls */}
                <div className="h-20 bg-slate-900 border-t border-slate-800">
                    <ReplayPlayer
                        isPlaying={isPlaying}
                        onPlayPause={() => setIsPlaying(!isPlaying)}
                        onNext={() => setCurrentStepIndex(prev => Math.min(prev + 1, session.steps.length - 1))}
                        onPrev={() => setCurrentStepIndex(prev => Math.max(prev - 1, 0))}
                        onReset={() => { setIsPlaying(false); setCurrentStepIndex(0); }}
                        currentIndex={currentStepIndex}
                        totalSteps={session.steps.length}
                    />
                </div>
            </div>
        </div>
    );
}
