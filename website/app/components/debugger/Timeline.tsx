import React from 'react';
import { FaUser, FaRobot, FaTools, FaCheck, FaExclamationTriangle } from 'react-icons/fa';

interface TimelineProps {
    steps: any[];
    currentStepIndex: number;
    onStepClick: (index: number) => void;
}

const Timeline: React.FC<TimelineProps> = ({ steps, currentStepIndex, onStepClick }) => {
    return (
        <div className="flex flex-col space-y-4 p-4 h-full overflow-y-auto bg-slate-900 border-r border-slate-700">
            <h3 className="text-xl font-bold text-blue-400 mb-4">Execution Trace</h3>
            {steps.map((step, index) => (
                <div
                    key={step.stepId}
                    onClick={() => onStepClick(index)}
                    className={`cursor-pointer p-3 rounded-lg border transition-all ${index === currentStepIndex
                        ? 'border-blue-500 bg-blue-900/20 shadow-lg shadow-blue-500/10'
                        : 'border-slate-700 bg-slate-800 hover:border-slate-600'
                        }`}
                >
                    <div className="flex items-center space-x-3">
                        <div className={`p-2 rounded-full ${step.stepType === 'USER_INPUT' ? 'bg-green-500/20 text-green-400' :
                            step.stepType === 'TOOL_CALL' ? 'bg-orange-500/20 text-orange-400' :
                                step.stepType === 'MODEL_RESPONSE' ? 'bg-purple-500/20 text-purple-400' :
                                    'bg-slate-500/20 text-slate-400'
                            }`}>
                            {step.stepType === 'USER_INPUT' && <FaUser />}
                            {step.stepType === 'TOOL_CALL' && <FaTools />}
                            {step.stepType === 'MODEL_RESPONSE' && <FaRobot />}
                        </div>
                        <div className="flex-1 min-w-0">
                            <p className="text-sm font-medium text-slate-200 truncate">
                                {step.stepType.replace('_', ' ')}
                            </p>
                            <p suppressHydrationWarning className="text-xs text-slate-400 truncate">
                                {new Date(step.timestamp).toLocaleTimeString()}
                            </p>
                        </div>
                        {index === currentStepIndex && <FaCheck className="text-blue-500" />}
                    </div>
                </div>
            ))}
        </div>
    );
};

export default Timeline;
