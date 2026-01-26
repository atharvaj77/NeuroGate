package com.neurogate.agent.memory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/agent/memory")
@RequiredArgsConstructor
@Tag(name = "Memory", description = "Agent memory management")
public class MemoryController {

    private final AgentMemoryService memoryService;

    @Operation(summary = "Store memory", description = "Store a memory entry for an agent")
    @ApiResponse(responseCode = "200", description = "Memory stored")
    @PostMapping
    public ResponseEntity<Map<String, String>> storeMemory(@RequestBody StoreMemoryRequest request) {
        String memoryId = memoryService.save(request);
        return ResponseEntity.ok(Map.of("status", "stored", "memory_id", memoryId));
    }

    @Operation(summary = "Retrieve memory", description = "Search and retrieve relevant memories")
    @ApiResponse(responseCode = "200", description = "Memories retrieved")
    @GetMapping
    public ResponseEntity<List<String>> retrieveMemory(
            @Parameter(description = "Search query") @RequestParam String query,
            @Parameter(description = "Maximum memories to return") @RequestParam(defaultValue = "5") int limit) {
        List<String> memories = memoryService.search(query, limit);
        return ResponseEntity.ok(memories);
    }
}
