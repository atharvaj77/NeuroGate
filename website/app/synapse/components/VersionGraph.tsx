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

interface Version {
    id: string;
    tag: string;
    content: string;
    timestamp: string;
    author: string;
    active: boolean;
}

interface VersionGraphProps {
    versions: Version[];
}

const VersionGraph: React.FC<VersionGraphProps> = ({ versions }) => {
    // Dynamically generate nodes based on versions history
    // For this mock simplification, we'll map the versions list to a linear graph
    // In a real app, this would need a DAG structure from the backend

    // Sort versions by ID (mock heuristic) or reverse order of array
    // Assuming versions[0] is latest
    const { initialNodes, initialEdges } = React.useMemo(() => {
        const reversedVersions = [...versions].reverse();

        const nodes: Node[] = reversedVersions.map((v, index) => ({
            id: v.id,
            data: { label: `${v.tag} (${v.active ? 'Active' : 'History'})` },
            position: { x: 150 + (index * 120), y: 100 + (index % 2 === 0 ? 0 : 50) }, // Zig-zag layout
            style: {
                background: v.active ? '#581c87' : '#1e293b',
                color: v.active ? '#e9d5ff' : '#cbd5e1',
                border: v.active ? '1px solid #a855f7' : '1px solid #475569',
                borderRadius: '8px',
                fontSize: '10px',
                width: 120,
                boxShadow: v.active ? '0 0 15px rgba(168, 85, 247, 0.4)' : 'none'
            },
        }));

        const edges: Edge[] = reversedVersions.slice(0, -1).map((v, index) => ({
            id: `e-${v.id}-${reversedVersions[index + 1].id}`,
            source: v.id,
            target: reversedVersions[index + 1].id,
            markerEnd: { type: MarkerType.ArrowClosed },
            style: { stroke: v.active ? '#a855f7' : '#475569', strokeWidth: v.active ? 2 : 1 }
        }));

        return { initialNodes: nodes, initialEdges: edges };
    }, [versions]);

    const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
    const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

    // Update graph when versions prop changes
    React.useEffect(() => {
        setNodes(initialNodes);
        setEdges(initialEdges);
    }, [initialNodes, initialEdges, setNodes, setEdges]);

    return (
        <div style={{ height: '100%', width: '100%' }}>
            <ReactFlow
                nodes={nodes}
                edges={edges}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                fitView
                fitViewOptions={{ padding: 0.2 }}
                attributionPosition="bottom-right"
            >
                <Background color="#555" gap={20} size={1} />
                <Controls showInteractive={false} className="bg-black/50 border-white/10" />
            </ReactFlow>
        </div>
    );
};

export default VersionGraph;
