package com.jobpilot.candidate.skill.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Normalization is a pure function (doc 07:27-28) — no Spring context.
 */
class SkillNormalizationServiceTest {

    private final SkillNormalizationService service = new SkillNormalizationService();

    @Test
    void normalizesReactVariantsToReact() {
        assertEquals("react", service.normalize("ReactJS"));
        assertEquals("react", service.normalize("React.js"));
        assertEquals("react", service.normalize("React"));
        assertEquals("react", service.normalize("react js"));
    }

    @Test
    void normalizesJsAndNode() {
        assertEquals("javascript", service.normalize("JavaScript"));
        assertEquals("javascript", service.normalize("JS"));
        assertEquals("node", service.normalize("Node.js"));
        assertEquals("node", service.normalize("node js"));
    }

    @Test
    void normalizesPostgresAndKubernetes() {
        assertEquals("postgresql", service.normalize("PostgreSQL"));
        assertEquals("postgresql", service.normalize("postgres"));
        assertEquals("kubernetes", service.normalize("k8s"));
    }

    @Test
    void preservesSpecialCharsAndCollapsesWhitespace() {
        assertEquals("c++", service.normalize("C++"));
        assertEquals("c#", service.normalize("C#"));
        assertEquals("machine learning", service.normalize("  Machine   Learning "));
        assertEquals(".net", service.normalize(".NET"));
    }

    @Test
    void unknownSkillReturnsNormalizedForm() {
        assertEquals("rust", service.normalize("Rust"));
        assertEquals("graphql", service.normalize("GraphQL"));
    }

    @Test
    void blankInputReturnsBlank() {
        assertEquals("", service.normalize(null));
        assertEquals("", service.normalize("   "));
    }
}