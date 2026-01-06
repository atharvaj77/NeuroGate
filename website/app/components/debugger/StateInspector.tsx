import React from 'react';
// import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
// import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';

interface StateInspectorProps {
    step: any;
}

const StateInspector: React.FC<StateInspectorProps> = ({ step }) => {
    if (!step) return <div className="p-8 text-center text-slate-500">Select a step to view details</div>;

    return (
        <div className="h-full flex flex-col space-y-6 p-6 overflow-y-auto bg-slate-950">
            <div className="flex items-center justify-between mb-4">
                <h2 className="text-2xl font-bold text-white">{step.stepType}</h2>
                <span className="text-sm font-mono text-slate-500">{step.stepId}</span>
            </div>

            <div className="space-y-2">
                <h4 className="text-sm font-semibold uppercase tracking-wider text-slate-400">Content</h4>
                <div className="bg-slate-900 p-4 rounded-lg border border-slate-800 text-slate-300 font-mono text-sm whitespace-pre-wrap">
                    {step.content || "No content"}
                </div>
            </div>

            {step.state && Object.keys(step.state).length > 0 && (
                <div className="space-y-2">
                    <h4 className="text-sm font-semibold uppercase tracking-wider text-slate-400">Context / Memory</h4>
                    <pre className="bg-slate-900 p-4 rounded-lg border border-slate-800 text-green-400 font-mono text-sm overflow-x-auto">
                        {JSON.stringify(step.state, null, 2)}
                    </pre>
                </div>
            )}
        </div>
    );
};

export default StateInspector;
