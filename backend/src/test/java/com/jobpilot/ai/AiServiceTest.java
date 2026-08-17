package com.jobpilot.ai;

import com.jobpilot.ai.provider.fake.FakeAiService;
import com.jobpilot.ai.provider.fake.FakeEmbeddingService;
import com.jobpilot.ai.provider.fake.FakeVisionService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class AiServiceTest {

    @Test
    void fakeAiReturnsSchemaShapedOutput() {
        FakeAiService svc = new FakeAiService();
        AiRequest req = new AiRequest(AiTaskType.SKILL_CLASSIFICATION,
                new UntrustedContent("resume text"), "extract skills");
        StructuredResponse<?> resp = svc.complete(req);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.data();
        assertNotNull(data.get("skills"));
        assertEquals("fake", resp.modelUsed());
    }

    @Test
    void fakeEmbeddingIsDeterministicAndSized() {
        FakeEmbeddingService svc = new FakeEmbeddingService();
        float[] a = svc.embed("hello", EmbeddingKind.RESUME);
        float[] b = svc.embed("hello", EmbeddingKind.RESUME);
        assertEquals(768, a.length);
        assertArrayEquals(a, b);
    }

    @Test
    void fakeVisionReturnsCanned() {
        FakeVisionService svc = new FakeVisionService();
        StructuredResponse<?> resp = svc.interpret(new byte[]{1, 2, 3},
                new AiRequest(AiTaskType.PAGE_UNDERSTANDING, new UntrustedContent(""), "read"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.data();
        assertEquals(0.99, (double) data.get("confidence"), 0.001);
    }
}
