import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FaRocket, FaTimes, FaGhost, FaServer, FaFlask } from 'react-icons/fa';

interface DeployModalProps {
    isOpen: boolean;
    onClose: () => void;
    onDeploy: (environment: string) => void;
    isDeploying: boolean;
}

const DeployModal: React.FC<DeployModalProps> = ({ isOpen, onClose, onDeploy, isDeploying }) => {
    const [selectedEnv, setSelectedEnv] = useState<'production' | 'staging' | 'shadow'>('staging');

    if (!isOpen) return null;

    const environments = [
        {
            id: 'production',
            label: 'Production',
            desc: 'Deploy to live traffic. High risk.',
            icon: <FaRocket className="text-red-400" />,
            color: 'border-red-500/30 bg-red-500/10 hover:bg-red-500/20'
        },
        {
            id: 'staging',
            label: 'Staging',
            desc: 'Deploy to QA environment for testing.',
            icon: <FaFlask className="text-blue-400" />,
            color: 'border-blue-500/30 bg-blue-500/10 hover:bg-blue-500/20'
        },
        {
            id: 'shadow',
            label: 'Shadow (Specter Mode)',
            desc: 'Deploy silently. Receives async traffic for comparison.',
            icon: <FaGhost className="text-purple-400" />,
            color: 'border-purple-500/30 bg-purple-500/10 hover:bg-purple-500/20'
        }
    ];

    return (
        <AnimatePresence>
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="absolute inset-0 bg-black/60 backdrop-blur-sm"
                    onClick={onClose}
                />
                <motion.div
                    initial={{ scale: 0.95, opacity: 0, y: 20 }}
                    animate={{ scale: 1, opacity: 1, y: 0 }}
                    exit={{ scale: 0.95, opacity: 0, y: 20 }}
                    className="relative w-full max-w-lg bg-slate-900 border border-white/10 rounded-2xl shadow-2xl overflow-hidden"
                >
                    <div className="p-6 border-b border-white/5 flex justify-between items-center bg-white/5">
                        <h2 className="text-xl font-bold text-white flex items-center gap-2">
                            <FaServer className="text-slate-400" />
                            Deploy Prompt
                        </h2>
                        <button onClick={onClose} className="text-slate-400 hover:text-white transition-colors">
                            <FaTimes />
                        </button>
                    </div>

                    <div className="p-6 space-y-4">
                        <p className="text-sm text-slate-400">Select the target environment for this deployment.</p>

                        <div className="grid gap-3">
                            {environments.map((env) => (
                                <button
                                    key={env.id}
                                    onClick={() => setSelectedEnv(env.id as any)}
                                    className={`relative p-4 rounded-xl border flex items-start text-left transition-all ${selectedEnv === env.id
                                            ? `${env.color} ring-1 ring-inset ring-white/20`
                                            : 'border-white/5 bg-white/5 hover:bg-white/10'
                                        }`}
                                >
                                    <div className="mr-4 mt-1 text-lg">{env.icon}</div>
                                    <div>
                                        <div className="font-bold text-white mb-1">{env.label}</div>
                                        <div className="text-xs text-slate-400">{env.desc}</div>
                                    </div>
                                    {selectedEnv === env.id && (
                                        <div className="absolute top-4 right-4 h-3 w-3 rounded-full bg-white shadow-[0_0_10px_white]" />
                                    )}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className="p-6 border-t border-white/5 bg-black/20 flex justify-end gap-3">
                        <button
                            onClick={onClose}
                            className="px-4 py-2 rounded-lg text-sm font-medium text-slate-400 hover:text-white hover:bg-white/5 transition-colors"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={() => onDeploy(selectedEnv)}
                            disabled={isDeploying}
                            className="px-6 py-2 rounded-lg text-sm font-bold bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-500 hover:to-purple-500 text-white shadow-lg shadow-purple-900/20 disabled:opacity-50 flex items-center gap-2"
                        >
                            {isDeploying ? 'Deploying...' : 'Confirm Deployment'}
                        </button>
                    </div>
                </motion.div>
            </div>
        </AnimatePresence>
    );
};

export default DeployModal;
