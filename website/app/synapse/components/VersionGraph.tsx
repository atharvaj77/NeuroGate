"use client";

import React, { useCallback } from 'react';
import ReactFlow, {
    Node,
    Edge,
    Controls,
    Background,
    MiniMap,
    useNodesState,
    useEdgesState,
    MarkerType,
} from 'reactflow';
import 'reactflow/dist/style.css';

const initialNodes: Node[] = [
    {
        id: '1',
        type: 'input',
        data: { label: 'v1.0 (Init)' },
        position: { x: 250, y: 0 },
        style: { background: '#1e293b', color: 'white', border: '1px solid #475569' },
    },
    {
        id: '2',
        data: { label: 'v1.1 (Tone fix)' },
        position: { x: 250, y: 100 },
        style: { background: '#1e293b', color: 'white', border: '1px solid #475569' },
    },
    {
        id: '3',
        data: { label: 'v2.0 (Major)' },
        position: { x: 100, y: 200 },
        style: { background: '#0f172a', color: '#60a5fa', border: '1px solid #3b82f6', fontWeight: 'bold' },
    },
    {
        id: '4',
        data: { label: 'v1.2 (Patch)' },
        position: { x: 400, y: 200 },
        style: { background: '#1e293b', color: 'white', border: '1px solid #475569' },
    },
];

const initialEdges: Edge[] = [
    { id: 'e1-2', source: '1', target: '2', markerEnd: { type: MarkerType.ArrowClosed } },
    { id: 'e2-3', source: '2', target: '3', animated: true, markerEnd: { type: MarkerType.ArrowClosed } },
    { id: 'e2-4', source: '2', target: '4', markerEnd: { type: MarkerType.ArrowClosed } },
];

const VersionGraph = () => {
    const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
    const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

    return (
        <div style={{ height: '100%', width: '100%' }}>
            <ReactFlow
                nodes={nodes}
                edges={edges}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                fitView
            >
                <Background color="#333" gap={16} />
                <Controls />
                <MiniMap style={{ background: '#1a1a1a' }} nodeColor={() => '#4a5568'} />
            </ReactFlow>
        </div>
    );
};

export default VersionGraph;
