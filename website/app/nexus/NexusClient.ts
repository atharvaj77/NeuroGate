export interface SearchResult {
    id: string;
    score: number;
    payload?: {
        content?: string;
        source?: string;
        title?: string;
        [key: string]: any;
    };
}

export interface SearchRequest {
    query: string;
    limit?: number;
}

export interface RAGStats {
    totalDocuments: number;
    averageTokenCount: number;
    totalUsageCount: number;
    cacheHitRate: number;
    averageCostSavings: number;
}

const API_BASE_URL = 'http://localhost:8080/api/rag';

export const NexusClient = {
    async searchDocuments(query: string, collection?: string, limit: number = 5): Promise<SearchResult[]> {
        try {
            const body: any = { query, limit };
            if (collection) {
                body.collection = collection;
            }

            const response = await fetch(`${API_BASE_URL}/search`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(body),
            });

            if (!response.ok) {
                console.error('Nexus API Error:', response.statusText);
                return [];
            }

            const data = await response.json();
            return data as SearchResult[];
        } catch (error) {
            console.error('Nexus Client Error:', error);
            return [];
        }
    },

    async getStats(): Promise<RAGStats | null> {
        try {
            const response = await fetch(`${API_BASE_URL}/stats`);
            if (!response.ok) return null;
            return await response.json();
        } catch (error) {
            console.error('Nexus Stats Error:', error);
            return null;
        }
    },

    async addDocument(title: string, content: string, source: string): Promise<boolean> {
        try {
            const response = await fetch(`${API_BASE_URL}/documents`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ title, content, source })
            });
            return response.ok;
        } catch (error) {
            console.error('Nexus Upload Error:', error);
            return false;
        }
    }
};
