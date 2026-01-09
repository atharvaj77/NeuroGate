import React from 'react';

interface HistoryItem {
    id: string;
    timestamp: string;
    action: string;
    user: string;
    details: string;
}

interface HistoryListProps {
    history: HistoryItem[];
}

const HistoryList: React.FC<HistoryListProps> = ({ history }) => {
    return (
        <div className="space-y-4">
            <h3 className="text-sm font-semibold text-gray-400 uppercase tracking-wider mb-2">
                Activity History
            </h3>
            {history.length === 0 ? (
                <div className="text-gray-500 italic text-sm">No activity yet.</div>
            ) : (
                <div className="relative border-l border-white/10 ml-2 space-y-8">
                    {history.map((item) => (
                        <div key={item.id} className="relative ml-6">
                            <div className="absolute w-3 h-3 bg-purple-500 rounded-full -left-[31px] mt-1.5 border-2 border-[#0f172a] shadow-[0_0_10px_rgba(168,85,247,0.5)]"></div>
                            <div className="flex flex-col group">
                                <span className="text-[10px] text-slate-500 font-mono mb-1 uppercase tracking-widest">{item.timestamp}</span>
                                <span className="text-sm text-slate-200 font-bold group-hover:text-purple-400 transition-colors">{item.action}</span>
                                <span className="text-xs text-slate-500">by <span className="text-slate-400">{item.user}</span></span>
                                {item.details && (
                                    <div className="mt-2 p-3 bg-white/5 rounded-lg text-xs font-mono text-slate-400 border border-white/5 group-hover:border-purple-500/30 transition-colors">
                                        {item.details}
                                    </div>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default HistoryList;
