import React from 'react';
import { FaTag, FaCheck, FaUndo } from 'react-icons/fa';

interface Version {
    id: string;
    tag: string;
    timestamp: string;
    author: string;
    active: boolean;
}

interface VersionsListProps {
    versions: Version[];
    onRestore: (versionId: string) => void;
}

const VersionsList: React.FC<VersionsListProps> = ({ versions, onRestore }) => {
    return (
        <div className="space-y-4">
            <h3 className="text-sm font-semibold text-gray-400 uppercase tracking-wider mb-2">
                Version History
            </h3>
            <div className="space-y-2">
                {versions.map((ver) => (
                    <div
                        key={ver.id}
                        className={`p-4 rounded-xl border transition-all duration-300 group relative overflow-hidden ${ver.active
                            ? 'bg-purple-500/10 border-purple-500/50 shadow-[0_0_20px_rgba(168,85,247,0.2)]'
                            : 'bg-black/20 border-white/5 hover:border-white/20 hover:bg-black/40'
                            }`}
                    >
                        {ver.active && <div className="absolute top-0 right-0 w-16 h-16 bg-gradient-to-bl from-purple-500/20 to-transparent rounded-bl-3xl -mr-4 -mt-4 blur-xl" />}

                        <div className="flex justify-between items-start mb-2 relative z-10">
                            <div className="flex items-center space-x-3">
                                <div className={`p-1.5 rounded-lg ${ver.active ? 'bg-purple-500/20 text-purple-300' : 'bg-white/5 text-slate-500'}`}>
                                    <FaTag size={12} />
                                </div>
                                <span className={`text-sm font-bold ${ver.active ? 'text-purple-100' : 'text-slate-300'}`}>{ver.tag}</span>
                                {ver.active && (
                                    <span className="px-2 py-0.5 rounded-full text-[10px] bg-purple-500/20 text-purple-300 border border-purple-500/30 font-bold tracking-wide shadow-[0_0_10px_rgba(168,85,247,0.2)]">
                                        LIVE
                                    </span>
                                )}
                            </div>
                            {!ver.active && (
                                <button
                                    onClick={() => onRestore(ver.id)}
                                    className="opacity-0 group-hover:opacity-100 p-2 hover:bg-white/10 rounded-lg text-slate-400 hover:text-white transition-all transform hover:scale-110"
                                    title="Restore this version"
                                >
                                    <FaUndo size={12} />
                                </button>
                            )}
                        </div>
                        <div className="flex justify-between text-[11px] font-mono relative z-10">
                            <span className={ver.active ? 'text-purple-300/70' : 'text-slate-500'}>{ver.author}</span>
                            <span className={ver.active ? 'text-purple-300/70' : 'text-slate-600'}>{ver.timestamp}</span>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default VersionsList;
