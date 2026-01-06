package com.neurogate.prompts;

import com.neurogate.router.cache.EmbeddingService;
import com.neurogate.router.provider.MultiProviderRouter;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for Prompt Version Control Service
 */
@ExtendWith(MockitoExtension.class)
class PromptVersionControlServiceTest {

        @Mock
        private EmbeddingService embeddingService;

        @Mock
        private MultiProviderRouter routerService;

        // Use Spy/real implementation for in-memory repositories to simplify testing
        // logic
        // or Mock them if strict isolation is preferred. Given the test logic relies on
        // state persistence
        // (createBranch checks if base version exists), using real in-memory repos is
        // easier than complex mocking.
        private PromptRepository promptRepository = new InMemoryPromptRepository();
        private TemplateRepository templateRepository = new InMemoryTemplateRepository();

        private PromptVersionControlService versionControlService;

        @BeforeEach
        void setUp() {
                versionControlService = new PromptVersionControlService(
                                embeddingService, routerService, promptRepository, templateRepository);
        }

        @Test
        void testCommitPrompt_FirstVersion() {
                // Given
                String prompt = "Summarize this document";
                float[] embedding = new float[] { 0.1f, 0.2f, 0.3f };
                when(embeddingService.generateEmbedding(prompt)).thenReturn(embedding);

                // When
                PromptVersion version = versionControlService.commit(
                                prompt, "Initial commit", "author1", "main");

                // Then
                assertThat(version).isNotNull();
                assertThat(version.getMajorVersion()).isEqualTo(1);
                assertThat(version.getMinorVersion()).isEqualTo(0);
                assertThat(version.getPatchVersion()).isEqualTo(0);
                assertThat(version.getVersionString()).isEqualTo("1.0.0");
                assertThat(version.getBranchName()).isEqualTo("main");
        }

        @Test
        void testCommitPrompt_PatchVersion() {
                // Given - Create parent version
                float[] embedding1 = new float[] { 0.9f, 0.9f, 0.9f };
                when(embeddingService.generateEmbedding("Summarize this document"))
                                .thenReturn(embedding1);

                versionControlService.commit("Summarize this document",
                                "Initial", "author1", "main");

                // Similar prompt (97% similar) should be patch
                float[] embedding2 = new float[] { 0.91f, 0.91f, 0.91f };
                when(embeddingService.generateEmbedding("Summarize this document concisely"))
                                .thenReturn(embedding2);

                // When
                PromptVersion version2 = versionControlService.commit(
                                "Summarize this document concisely", "Minor wording", "author1", "main");

                // Then
                assertThat(version2.getMajorVersion()).isEqualTo(1);
                assertThat(version2.getMinorVersion()).isEqualTo(0);
                assertThat(version2.getPatchVersion()).isEqualTo(1);
                assertThat(version2.getVersionString()).isEqualTo("1.0.1");
        }

        @Test
        void testCommitPrompt_MinorVersion() {
                // Given - Create parent
                float[] embedding1 = new float[] { 1.0f, 0.0f, 0.0f };
                when(embeddingService.generateEmbedding("Summarize document"))
                                .thenReturn(embedding1);

                versionControlService.commit("Summarize document", "Initial", "author1", "main");

                // Moderately different (~70% similar) should be minor
                float[] embedding2 = new float[] { 0.7f, 0.7f, 0.0f };
                when(embeddingService.generateEmbedding("Extract key points from document"))
                                .thenReturn(embedding2);

                // When
                PromptVersion version2 = versionControlService.commit(
                                "Extract key points from document", "Different approach", "author1", "main");

                // Then
                assertThat(version2.getMajorVersion()).isEqualTo(1);
                assertThat(version2.getMinorVersion()).isEqualTo(1);
                assertThat(version2.getPatchVersion()).isEqualTo(0);
        }

        @Test
        void testCommitPrompt_MajorVersion() {
                // Given - Create parent
                float[] embedding1 = new float[] { 1.0f, 0.0f, 0.0f };
                when(embeddingService.generateEmbedding("Summarize document"))
                                .thenReturn(embedding1);

                versionControlService.commit("Summarize document", "Initial", "author1", "main");

                // Very different (0% similar) should be major
                float[] embedding2 = new float[] { 0.0f, 1.0f, 0.0f };
                when(embeddingService.generateEmbedding("Translate to Spanish"))
                                .thenReturn(embedding2);

                // When
                PromptVersion version2 = versionControlService.commit(
                                "Translate to Spanish", "Completely different", "author1", "main");

                // Then
                assertThat(version2.getMajorVersion()).isEqualTo(2);
                assertThat(version2.getMinorVersion()).isEqualTo(0);
                assertThat(version2.getPatchVersion()).isEqualTo(0);
        }

        @Test
        void testCreateBranch() {
                // Given - Create base version
                float[] embedding = new float[] { 0.1f, 0.2f, 0.3f };
                when(embeddingService.generateEmbedding(any())).thenReturn(embedding);

                PromptVersion base = versionControlService.commit(
                                "Base prompt", "Initial", "author1", "main");

                // When
                PromptBranch branch = versionControlService.createBranch(
                                "feature-experiment", base.getVersionId(), "author1");

                // Then
                assertThat(branch).isNotNull();
                assertThat(branch.getBranchName()).isEqualTo("feature-experiment");
                assertThat(branch.getBaseVersionId()).isEqualTo(base.getVersionId());
                assertThat(branch.getStatus()).isEqualTo(PromptBranch.BranchStatus.ACTIVE);
        }

        @Test
        void testCreateBranch_AlreadyExists() {
                // Given
                float[] embedding = new float[] { 0.1f, 0.2f, 0.3f };
                when(embeddingService.generateEmbedding(any())).thenReturn(embedding);

                PromptVersion base = versionControlService.commit(
                                "Base", "Initial", "author1", "main");

                versionControlService.createBranch("test-branch", base.getVersionId(), "author1");

                // When/Then
                assertThatThrownBy(
                                () -> versionControlService.createBranch("test-branch", base.getVersionId(), "author1"))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("already exists");
        }

        @Test
        void testMergeBranches_Success() {
                // Given - Create main branch version
                float[] embedding1 = new float[] { 0.5f, 0.5f, 0.5f };
                when(embeddingService.generateEmbedding("Main prompt")).thenReturn(embedding1);

                PromptVersion mainVersion = versionControlService.commit(
                                "Main prompt", "Main", "author1", "main");

                // Create feature branch
                versionControlService.createBranch("feature", mainVersion.getVersionId(), "author1");

                float[] embedding2 = new float[] { 0.6f, 0.6f, 0.6f }; // Similar enough
                when(embeddingService.generateEmbedding("Main prompt improved")).thenReturn(embedding2);

                versionControlService.commit("Main prompt improved", "Feature", "author1", "feature");

                // When
                MergeResult result = versionControlService.mergeBranches(
                                "feature", "main", "author1", "Merge feature");

                // Then
                assertThat(result.isSuccess()).isTrue();
                assertThat(result.isConflictDetected()).isFalse();
                assertThat(result.getMergedVersionId()).isNotNull();
        }

        @Test
        void testMergeBranches_Conflict() {
                // Given - Create very different branches
                float[] embedding1 = new float[] { 1.0f, 0.0f, 0.0f };
                when(embeddingService.generateEmbedding("Prompt A")).thenReturn(embedding1);

                PromptVersion mainVersion = versionControlService.commit(
                                "Prompt A", "Main", "author1", "main");

                versionControlService.createBranch("feature", mainVersion.getVersionId(), "author1");

                float[] embedding2 = new float[] { 0.0f, 1.0f, 0.0f }; // Very different (0.0 similarity)
                when(embeddingService.generateEmbedding("Completely different")).thenReturn(embedding2);

                versionControlService.commit("Completely different", "Feature", "author1", "feature");

                // When
                MergeResult result = versionControlService.mergeBranches(
                                "feature", "main", "author1", null);

                // Then
                assertThat(result.isSuccess()).isFalse();
                assertThat(result.isConflictDetected()).isTrue();
                assertThat(result.getMessage()).contains("conflict");
        }

        @Test
        void testRollback() {
                // Given - Create version history
                float[] embedding = new float[] { 0.1f, 0.2f, 0.3f };
                when(embeddingService.generateEmbedding(any())).thenReturn(embedding);

                PromptVersion v1 = versionControlService.commit("V1", "First", "author", "main");
                versionControlService.commit("V2", "Second", "author", "main");
                versionControlService.commit("V3", "Third", "author", "main");

                // When - Rollback to V1
                PromptVersion rollback = versionControlService.rollback(
                                "main", v1.getVersionId(), "author");

                // Then
                assertThat(rollback.getPromptText()).isEqualTo("V1");
                assertThat(rollback.getCommitMessage()).contains("Rollback");
        }

        @Test
        void testGetVersionHistory() {
                // Given - Create version chain
                float[] embedding = new float[] { 0.1f, 0.2f, 0.3f };
                when(embeddingService.generateEmbedding(any())).thenReturn(embedding);

                versionControlService.commit("V1", "First", "author", "main");
                versionControlService.commit("V2", "Second", "author", "main");
                versionControlService.commit("V3", "Third", "author", "main");

                // When
                List<PromptVersion> history = versionControlService.getVersionHistory("main");

                // Then
                assertThat(history).hasSize(3);
                assertThat(history.get(0).getPromptText()).isEqualTo("V3"); // Most recent first
                assertThat(history.get(1).getPromptText()).isEqualTo("V2");
                assertThat(history.get(2).getPromptText()).isEqualTo("V1");
        }

        @Test
        void testCreateTemplate() {
                // Given
                Map<String, PromptTemplate.VariableDefinition> variables = Map.of(
                                "doc_type", PromptTemplate.VariableDefinition.builder()
                                                .name("doc_type")
                                                .description("Type of document")
                                                .type("string")
                                                .required(true)
                                                .build());

                // When
                PromptTemplate template = versionControlService.createTemplate(
                                "Document Analyzer",
                                "Analyze documents",
                                "Analyze {doc_type} for risks",
                                variables,
                                new String[] { "analysis", "documents" });

                // Then
                assertThat(template).isNotNull();
                assertThat(template.getTemplateName()).isEqualTo("Document Analyzer");
                assertThat(template.getTemplate()).contains("{doc_type}");
                assertThat(template.getVariables()).containsKey("doc_type");
        }

        @Test
        void testTemplateRender() {
                // Given
                PromptTemplate template = PromptTemplate.builder()
                                .template("Analyze {doc_type} for {analysis_type}")
                                .defaults(Map.of("analysis_type", "risks"))
                                .build();

                // When
                String rendered = template.render(Map.of("doc_type", "contract"));

                // Then
                assertThat(rendered).isEqualTo("Analyze contract for risks");
        }
}
