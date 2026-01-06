import React from 'react';
import { FaPlay, FaPause, FaStepBackward, FaStepForward, FaRedo } from 'react-icons/fa';

interface ReplayPlayerProps {
    isPlaying: boolean;
    onPlayPause: () => void;
    onNext: () => void;
    onPrev: () => void;
    onReset: () => void;
    currentIndex: number;
    totalSteps: number;
}

const ReplayPlayer: React.FC<ReplayPlayerProps> = ({
    isPlaying, onPlayPause, onNext, onPrev, onReset, currentIndex, totalSteps
}) => {
    return (
        <div className="flex items-center justify-between p-4 bg-slate-800 border-t border-slate-700 rounded-lg">
            <div className="flex items-center space-x-4">
                <button onClick={onReset} className="p-2 text-slate-400 hover:text-white transition-colors" title="Reset">
                    <FaRedo />
                </button>
                <button onClick={onPrev} disabled={currentIndex <= 0} className="p-2 text-slate-400 hover:text-white disabled:opacity-50 transition-colors">
                    <FaStepBackward />
                </button>
                <button
                    onClick={onPlayPause}
                    className="p-3 bg-blue-600 hover:bg-blue-500 text-white rounded-full shadow-lg transition-transform active:scale-95"
                >
                    {isPlaying ? <FaPause /> : <FaPlay className="ml-1" />}
                </button>
                <button onClick={onNext} disabled={currentIndex >= totalSteps - 1} className="p-2 text-slate-400 hover:text-white disabled:opacity-50 transition-colors">
                    <FaStepForward />
                </button>
            </div>
            <div className="text-sm font-medium text-slate-300">
                Step <span className="text-blue-400">{currentIndex + 1}</span> of {totalSteps}
            </div>
            <div className="w-1/3 bg-slate-700 rounded-full h-2 overflow-hidden">
                <div
                    className="bg-blue-500 h-full transition-all duration-300 ease-out"
                    style={{ width: `${((currentIndex + 1) / totalSteps) * 100}%` }}
                />
            </div>
        </div>
    );
};

export default ReplayPlayer;
