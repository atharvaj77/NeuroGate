package com.neurogate.agentops.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class RedisMemoryStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ListOperations<String, String> listOperations;
    @Mock
    private MemoryCompressor memoryCompressor;

    private RedisMemoryStore memoryStore;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        memoryStore = new RedisMemoryStore(redisTemplate, memoryCompressor);
    }

    @Test
    void testStoreShortTerm_CompressesWhenFull() {
        when(listOperations.rightPush(anyString(), anyString())).thenReturn(1L);
        when(listOperations.size(anyString())).thenReturn(25L); // > 20, triggers compression

        // Mock compression logic
        when(listOperations.range(anyString(), anyLong(), anyLong())).thenReturn(createMockMessages(25));
        when(memoryCompressor.compress(anyList())).thenReturn(List.of("Compressed"));

        memoryStore.storeShortTerm("session1", "user", "hello");

        verify(listOperations).rightPush(eq("agent:stm:session1"), eq("user: hello"));
        verify(memoryCompressor).compress(anyList());
        // Verify delete/pushAll called during compression
        verify(redisTemplate).delete(eq("agent:stm:session1"));
    }

    @Test
    void testGetContextWindow_CallsCompressor() {
        List<String> raw = List.of("msg1", "msg2");
        when(listOperations.range(anyString(), anyLong(), anyLong())).thenReturn(raw);
        when(memoryCompressor.compress(raw)).thenReturn(raw);

        List<String> result = memoryStore.getContextWindow("session1");

        assertEquals(2, result.size());
        verify(memoryCompressor).compress(raw);
    }

    private List<String> createMockMessages(int count) {
        List<String> msgs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            msgs.add("msg" + i);
        }
        return msgs;
    }
}
