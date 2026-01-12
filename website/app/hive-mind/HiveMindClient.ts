export interface ModelResponse {
    id: string;
    choices: Array<{
        message: {
            content: string;
            role: string;
        }
    }>;
    x_neurogate_route: string;
}

export interface ConsensusResult {
    synthesis: string;
    individualResponses: ModelResponse[];
    confidence: number;
}

const API_BASE_URL = 'http://localhost:8080/api/hive';

export const HiveMindClient = {
    async runConsensus(query: string): Promise<ConsensusResult | null> {
        try {
            const response = await fetch(`${API_BASE_URL}/consensus`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    model: 'consensus-group', // Logical model for the service
                    messages: [
                        { role: 'user', content: query }
                    ]
                }),
            });

            if (!response.ok) {
                console.error('Hive Mind API Error:', response.statusText);
                return null;
            }

            return await response.json();
        } catch (error) {
            console.error('Hive Mind Network Error:', error);
            return null;
        }
    }
};
