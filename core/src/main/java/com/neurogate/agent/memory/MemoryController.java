package com.neurogate.agent.memory;

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
public class MemoryController {

    private final AgentMemoryService memoryService;

    @PostMapping
    public ResponseEntity<Map<String, String>> storeMemory(@RequestBody StoreMemoryRequest request) {
        String memoryId = memoryService.save(request);
        return ResponseEntity.ok(Map.of("status", "stored", "memory_id", memoryId));
    }

    @GetMapping
    public ResponseEntity<List<String>> retrieveMemory(@RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        List<String> memories = memoryService.search(query, limit);
        return ResponseEntity.ok(memories);
    }
}
