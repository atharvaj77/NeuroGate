package com.neurogate.synapse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.router.provider.MultiProviderRouter;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Choice;
import com.neurogate.sentinel.model.Message;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SynapseController.class)
@org.springframework.test.context.ContextConfiguration(classes = { SynapseController.class,
                org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class })
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class SynapseControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private PromptRegistry promptRegistry;

        @MockBean
        private DiffService diffService;

        @MockBean
        private MultiProviderRouter routerService;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void testPlay_Success() throws Exception {
                SynapseController.PlayRequest request = new SynapseController.PlayRequest();
                request.setPromptContent("Hello {{ name }}");
                request.setVariables(Map.of("name", "World"));
                request.setModel("gpt-4");

                ChatResponse mockResponse = ChatResponse.builder()
                                .choices(List.of(Choice.builder()
                                                .message(Message.builder()
                                                                .role("assistant")
                                                                .content("Hello World response")
                                                                .build())
                                                .build()))
                                .build();

                when(routerService.route(any())).thenReturn(mockResponse);

                mockMvc.perform(post("/api/v1/synapse/play")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.choices[0].message.content").value("Hello World response"));
        }

        @Test
        void testDiff_Success() throws Exception {
                SynapseController.DiffRequest request = new SynapseController.DiffRequest();
                request.setOriginal("A");
                request.setRevised("B");

                DiffService.DiffResult mockResult = DiffService.DiffResult.builder()
                                .deltas(List.of(DiffService.DiffDelta.builder().type("CHANGE").build()))
                                .build();

                when(diffService.computeDiff(any(), any())).thenReturn(mockResult);

                mockMvc.perform(post("/api/v1/synapse/diff")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.deltas[0].type").value("CHANGE"));
        }

        @Test
        void testDeploy_Success() throws Exception {
                SynapseController.DeployRequest request = new SynapseController.DeployRequest();
                request.setPromptName("test-prompt");
                request.setVersionId("v1");
                request.setEnvironment("production");
                request.setUser("admin");

                mockMvc.perform(post("/api/v1/synapse/deploy")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }
}
