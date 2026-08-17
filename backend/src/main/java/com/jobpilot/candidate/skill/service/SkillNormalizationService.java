package com.jobpilot.candidate.skill.service;

import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * Deterministic skill-string normalization to a canonical taxonomy
 * (doc 07:27-28). Pure function — no AI, no I/O — so it is trivially testable
 * and safe to call on every extracted skill. Alias keys are pre-normalized
 * (lowercase, alnum + {@code + #}, whitespace collapsed) to match the form
 * produced by {@link #normalize(String)}.
 */
@Service
public class SkillNormalizationService {

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("reactjs", "react"),
            Map.entry("react", "react"),
            Map.entry("react js", "react"),
            Map.entry("js", "javascript"),
            Map.entry("javascript", "javascript"),
            Map.entry("ecmascript", "javascript"),
            Map.entry("ts", "typescript"),
            Map.entry("typescript", "typescript"),
            Map.entry("nodejs", "node"),
            Map.entry("node", "node"),
            Map.entry("node js", "node"),
            Map.entry("postgres", "postgresql"),
            Map.entry("postgresql", "postgresql"),
            Map.entry("k8s", "kubernetes"),
            Map.entry("kubernetes", "kubernetes"),
            Map.entry("aws", "aws"),
            Map.entry("amazon web services", "aws"),
            Map.entry("gcp", "gcp"),
            Map.entry("google cloud", "gcp"),
            Map.entry("google cloud platform", "gcp"),
            Map.entry("azure", "azure"),
            Map.entry("microsoft azure", "azure"),
            Map.entry("html", "html"),
            Map.entry("html5", "html"),
            Map.entry("css", "css"),
            Map.entry("css3", "css"),
            Map.entry("spring boot", "spring boot"),
            Map.entry("springboot", "spring boot"),
            Map.entry("java", "java"),
            Map.entry("python", "python"),
            Map.entry("py", "python"),
            Map.entry("c++", "c++"),
            Map.entry("cpp", "c++"),
            Map.entry("c#", "c#"),
            Map.entry("csharp", "c#"),
            Map.entry("dotnet", ".net"),
            Map.entry("net", ".net"),
            Map.entry("go", "go"),
            Map.entry("golang", "go"),
            Map.entry("machine learning", "machine learning"),
            Map.entry("ml", "machine learning"),
            Map.entry("deep learning", "deep learning"),
            Map.entry("sql", "sql"),
            Map.entry("mysql", "mysql"),
            Map.entry("redis", "redis"),
            Map.entry("docker", "docker"),
            Map.entry("kafka", "kafka"));

    /**
     * @return canonical normalized form, or {@code ""} for null/blank input.
     */
    public String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.toLowerCase()
                .replaceAll("[^a-z0-9+#]", " ")
                .trim()
                .replaceAll("\\s+", " ");
        if (s.isEmpty()) {
            return "";
        }
        return ALIASES.getOrDefault(s, s);
    }
}