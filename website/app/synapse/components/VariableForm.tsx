import React, { useMemo } from 'react';

interface VariableFormProps {
    promptContent: string;
    variables: Record<string, string>;
    onChange: (key: string, value: string) => void;
}

const VariableForm: React.FC<VariableFormProps> = ({ promptContent, variables, onChange }) => {
    // Extract variables from content using regex {{ var }}
    const detectedVariables = useMemo(() => {
        const regex = /\{\{\s*(\w+)\s*\}\}/g;
        const vars = new Set<string>();
        let match;
        while ((match = regex.exec(promptContent)) !== null) {
            vars.add(match[1]);
        }
        return Array.from(vars);
    }, [promptContent]);

    if (detectedVariables.length === 0) {
        return (
            <div className="text-gray-500 italic text-center p-4">
                No variables detected. type {'{{ variable }}'} to add one.
            </div>
        );
    }

    return (
        <div className="space-y-4">
            <h3 className="text-sm font-semibold text-gray-400 uppercase tracking-wider mb-2">
                Prompt Variables
            </h3>
            {detectedVariables.map((varName) => (
                <div key={varName} className="group">
                    <label className="block text-xs font-bold text-slate-400 mb-2 uppercase tracking-wide group-hover:text-purple-400 transition-colors">
                        {varName}
                    </label>
                    <input
                        type="text"
                        value={variables[varName as any] || ''}
                        onChange={(e) => onChange(varName, e.target.value)}
                        className="w-full bg-black/40 border border-white/10 rounded-lg px-4 py-3 text-sm text-white focus:ring-1 focus:ring-purple-500 focus:border-purple-500 outline-none transition-all placeholder:text-slate-600 shadow-inner hover:border-white/20"
                        placeholder={`Value for ${varName}`}
                    />
                </div>
            ))}
        </div>
    );
};

export default VariableForm;
