# 06 — AI Architecture

## 1. Provider Abstraction

All AI access goes through three interfaces in the `ai` module — nothing
outside `ai` talks to a model provider directly:

```java
public interface AiService {
    StructuredResponse<?> complete(AiRequest request);
}
public interface EmbeddingService {
    float[] embed(String text, EmbeddingKind kind);
}
public interface VisionService {
    StructuredResponse<?> interpret(byte[] image, AiRequest context);
}
```

`AiRequest` carries: task type (enum, drives routing), the untrusted-content
payload wrapped in a `UntrustedContent` marker type (doc 23), the trusted
system instructions, an output schema reference, and a budget (doc 43).
`StructuredResponse<T>` carries the parsed+validated output plus raw
metadata (model used, tokens, latency) for observability (doc 29).

Initial provider: **Ollama**, wrapped by an `OllamaAiService` /
`OllamaEmbeddingService` / `OllamaVisionService`. Adding a second provider
(e.g., a hosted API) means adding a new implementation of these three
interfaces and a routing entry — no business-logic module changes. This is
ADR-007.

## 2. Model Routing

A `ModelRouter` maps `AiTaskType` → model profile, not a single hard-coded
model:

| Task type | Profile | Rationale |
|---|---|---|
| `SKILL_CLASSIFICATION`, `FIELD_MAPPING`, `SIMPLE_EXTRACTION` | fast/small | high volume, low reasoning depth |
| `RESUME_REASONING`, `JOB_ANALYSIS`, `APPLICATION_STRATEGY`, `ANSWER_GENERATION` | strong/large | multi-step reasoning, correctness-critical |
| `SEMANTIC_EMBEDDING` | embedding model | fixed-purpose |
| `PAGE_UNDERSTANDING` (browser/form) | vision-capable | screenshot + DOM interpretation |

`ModelRouter` is configuration-driven (profile → concrete model name per
environment), so swapping the "strong" model doesn't require code changes.
Every `StructuredResponse` records which profile and concrete model served
it, for reproducibility and evaluation (doc 27).

## 3. Cost & Resource Management

Even on self-hosted Ollama, compute is finite and shared, so:

- **Context budgets**: each `AiTaskType` has a max input token budget
  enforced before the call is made (truncate/summarize upstream via RAG
  retrieval limits, not by silently cutting mid-document).
- **Caching**: request-hash → response cache (Redis) for deterministic,
  cacheable task types (e.g., re-scoring the same job against an unchanged
  candidate profile); cache key includes candidate/job `updated_at` so
  staleness is impossible, not just unlikely.
- **Batching**: bulk operations (re-embedding after resume update, batch
  match scoring) are queued and processed in bounded-size batches, not as N
  synchronous calls.
- **Concurrency limits**: a semaphore per model profile bounds in-flight
  requests to what the Ollama instance can actually serve without thrashing.
- **Timeouts & fallbacks**: every call has a timeout; on timeout or model
  unavailability the caller gets a typed failure (`AiUnavailableException`)
  and the calling service applies its own fallback (doc 30) — never a silent
  empty/garbage result.
- **Request deduplication**: identical in-flight requests (same hash)
  collapse to one call with fan-out of the result, guarding against
  accidental N+1 AI calls from list-rendering code paths.

## 4. RAG Architecture

Used wherever a generation task needs grounding in candidate evidence, job
detail, or company detail (resume tailoring, cover letters, answers, match
explanations).

```
Source documents (resume text, job description, company page)
   ↓ chunking (semantic, ~400-token target, section-aware for resumes)
   ↓ embedding (EmbeddingService)
   ↓ storage (candidate_embeddings / job_embeddings / company_embeddings,
              plus a chunk-level embeddings table for sub-document retrieval
              where a whole-document vector isn't precise enough)
   ↓ retrieval (top-k cosine similarity, k configurable per task type)
   ↓ reranking (lightweight cross-encoder pass or heuristic rerank by
              recency/source-confidence where a full reranker model isn't
              justified yet — FUTURE: dedicated reranker)
   ↓ context construction (retrieved chunks + explicit EvidenceRef ids)
   ↓ generation (AiService.complete with UntrustedContent-wrapped chunks)
   ↓ citation/evidence mapping (output schema requires each claim to
              reference an EvidenceRef id; unmapped claims are rejected by
              the Policy Engine's evidence-validation stage, doc 23)
```

Chunk-level retrieval is used for resumes/projects (multiple discrete
evidence units); job/company retrieval more often uses the whole-document
embedding since those documents are already scoped to one entity.

## 5. Agent vs. Deterministic Classification (summary — full detail doc 16)

`ai` module exposes capabilities, not agents. Whether a capability is
invoked by a deterministic service, a rule engine, a workflow step, or an
agent's tool call is decided per-component in doc 16; `ai` itself has no
opinion and imposes no agentic loop.

## 6. Output Validation

Every `AiService.complete` call declares an output schema (JSON Schema);
the response is parsed and schema-validated before it reaches calling code.
Schema-invalid output is retried once with an error-correction prompt, then
surfaced as a typed failure — never passed through partially parsed.

## 7. Failure Modes (cross-ref doc 30)

`AiUnavailableException` (Ollama down/timeout), `AiOutputInvalidException`
(schema validation failed after retry), `AiBudgetExceededException`
(context or rate budget hit). All three are non-retryable-by-the-caller
without backoff and are surfaced distinctly so `application` module can
decide, e.g., to fall back to MANUAL mode rather than silently stall.
