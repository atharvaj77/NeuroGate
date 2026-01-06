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
                    <label className="block text-xs font-medium text-gray-300 mb-1">
                        {varName}
                    </label>
                    <input
                        type="text"
                        value={variables[varName as any] || ''}
                        onChange={(e) => onChange(varName, e.target.value)}
                        className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-2 text-sm text-gray-100 focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all"
                        placeholder={`Value for ${varName}`}
                    />
                </div>
            ))}
        </div>
    );
};

export default VariableForm;
