"use client";

import React, { useRef, useEffect } from 'react';
import Editor, { Monaco } from '@monaco-editor/react';

interface PromptEditorProps {
    value: string;
    onChange: (value: string | undefined) => void;
    theme?: string;
}

const PromptEditor: React.FC<PromptEditorProps> = ({ value, onChange, theme = 'vs-dark' }) => {
    const editorRef = useRef<any>(null);

    const handleEditorDidMount = (editor: any, monaco: Monaco) => {
        editorRef.current = editor;

        // Register custom language for prompt engineering
        monaco.languages.register({ id: 'prompt-lang' });

        // Define syntax highlighting for variables like {{ variable }}
        monaco.languages.setMonarchTokensProvider('prompt-lang', {
            tokenizer: {
                root: [
                    [/\{\{.*?\}\}/, 'variable'],
                    [/@\w+/, 'mention'],
                    [/[^\{\}@]+/, 'text'],
                ],
            },
        });

        // Define "Deep Space" theme colors
        monaco.editor.defineTheme('prompt-theme', {
            base: 'vs-dark',
            inherit: true,
            rules: [
                { token: 'variable', foreground: 'A855F7', fontStyle: 'bold' }, // Purple-500 for variables
                { token: 'mention', foreground: '34D399' }, // Emerald-400 for mentions
                { token: 'text', foreground: 'E2E8F0' }, // Slate-200
            ],
            colors: {
                'editor.background': '#020617', // Slate-950
                'editor.lineHighlightBackground': '#1e293b',
                'editor.selectionBackground': '#3b82f640',
            },
        });
    };

    return (
        <div className="h-full w-full rounded-lg overflow-hidden bg-transparent">
            <Editor
                height="100%"
                defaultLanguage="prompt-lang"
                defaultValue={value}
                theme="prompt-theme"
                onChange={onChange}
                onMount={handleEditorDidMount}
                options={{
                    minimap: { enabled: false },
                    fontSize: 14,
                    lineNumbers: 'on',
                    scrollBeyondLastLine: false,
                    automaticLayout: true,
                    padding: { top: 16, bottom: 16 },
                }}
            />
        </div>
    );
};

export default PromptEditor;
