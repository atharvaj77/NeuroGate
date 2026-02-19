
export type EvaluationStatus = 'PASS' | 'FAIL' | 'WARN';

export interface CaseResult {
    id: string;
    caseId: string;
    input: string;
    agentOutput: string;
    idealOutput: string; // From backend snapshot if available
    score: number;
    judgeReasoning: string;
    status: EvaluationStatus;
}

export interface EvaluationRun {
    id: string;
    datasetId: string;
    agentVersion: string;
    createdAt: string;
    overallScore: number;
    results: CaseResult[];
    totalCases?: number; // Helpers
    datasetName?: string; // Helpers
}

const API_BASE_URL = `${process.env.NEXT_PUBLIC_NEUROGATE_API_BASE_URL ?? 'http://localhost:8080'}/api/v1/cortex`;

export const CortexClient = {
    // For MVP, we might need to look up a dataset ID if the UI assumes a "default" one.
    // Or we create a default one.

    async createDataset(name: string): Promise<string | null> {
        try {
            const response = await fetch(`${API_BASE_URL}/datasets`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name }),
                credentials: 'include',
            });
            if (!response.ok) return null;
            const data = await response.json();
            return data.id;
        } catch (e) {
            console.error(e);
            return null;
        }
    },

    async runEvaluation(datasetId: string, agentVersion: string): Promise<EvaluationRun | null> {
        try {
            const response = await fetch(`${API_BASE_URL}/runs`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ datasetId, agentVersion }),
                credentials: 'include',
            });

            if (!response.ok) {
                console.error('Cortex Run Error:', response.statusText);
                return null;
            }

            return await response.json();
        } catch (error) {
            console.error('Cortex Network Error:', error);
            return null;
        }
    },

    async getRuns(datasetId: string): Promise<EvaluationRun[]> {
        try {
            const response = await fetch(`${API_BASE_URL}/runs/${datasetId}`, {
                credentials: 'include',
            });
            if (!response.ok) return [];
            return await response.json();
        } catch (error) {
            console.error('Cortex GetRuns Error:', error);
            return [];
        }
    }
};
