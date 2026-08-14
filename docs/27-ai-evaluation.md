# 27 — AI Evaluation

Tier: **CORE**, cross-cutting for AI-touching modules. Owning module: `ai`
(harness), individual modules (benchmark datasets).

## 1. Metrics

| Metric | Definition | Owning pipeline |
|---|---|---|
| Resume extraction accuracy | field-level match against hand-labeled fixture resumes | doc 07 |
| Skill precision / recall | extracted skills vs. labeled ground truth, per fixture resume | doc 07 |
| Match precision / recall | JobMatch band vs. hand-labeled expected band, per candidate/job fixture pair | doc 09 |
| Hallucination rate | % of generated claims (resume/cover letter/answers) that fail evidence-validation (doc 23 §4) on a held-out generation sample | doc 12, doc 23 |
| Answer correctness | generated `ApplicationAnswer` vs. expected answer for FACTUAL_SHORT fixtures | doc 12 |
| Resume tailoring accuracy | tailored resume claims all evidence-backed + emphasis matches `ApplicationPlan` intent | doc 12 |
| Application field mapping accuracy | Form Intelligence field→answer resolution correctness against fixture forms | doc 13 |
| Browser task completion rate | % of fixture application flows completed without unintended escalation | doc 14, doc 28 |
| Manual handoff correctness | % of MANUAL/escalation triggers that match the expected `ManualHandoffReason` for that fixture | doc 15 |
| Prompt-injection resistance | % of adversarial fixtures (doc 07 §10, doc 09 §8) that produce no injected behavior | doc 23 |

## 2. Benchmark Datasets

Versioned, checked into the repo under `eval/fixtures/`, separate from
production data — never real candidate data used as a benchmark without
explicit, separate consent (privacy principle, doc 24 §2 applies to
internal eval data too). Each fixture has a labeled expected output
(ground-truth field values, expected match band, expected evidence
requirement pass/fail).

## 3. Harness

```
Run harness (CI nightly + on-demand)
   ↓ for each fixture: invoke the real pipeline (real Ollama call, per
     doc 06 routing) against the fixture input
   ↓ compare output to ground truth per metric definition
   ↓ aggregate scores, compare against the last-known baseline
   ↓ regression flagged if any metric drops beyond a documented tolerance
     band (not a hard 100% gate, since model output has natural variance —
     tolerance bands are set per metric, documented alongside the metric)
   ↓ results published (dashboard/report artifact), reviewed before any
     model/prompt/routing change is considered "safe to ship"
```

## 4. Hallucination Rate — Special Emphasis

Given the Zero-Fabrication Policy's centrality (doc 01 §5.12, doc 23 §4),
hallucination rate is tracked with the tightest tolerance band of any
metric and any regression here blocks release regardless of other metrics
improving — this is stated explicitly so "average quality went up" is
never treated as an acceptable trade against "fabrication rate went up."

## 5. Relationship to Policy Engine

The eval harness measures the *generation* pipeline's raw output quality
before Policy Engine validation, and separately measures the *end-to-end*
rate (post-validation) — the gap between the two numbers is itself a
useful signal (a large gap means the Policy Engine is doing a lot of
correcting work, worth investigating why generation quality is low rather
than relying on validation to catch everything).

## 6. Model/Prompt Change Process

Any change to a prompt template, model routing assignment (doc 06 §2), or
RAG retrieval configuration (doc 06 §4) re-runs the full eval suite before
merge — treated with the same rigor as a schema migration, since it can
silently degrade a metric that unit tests (which use faked AI responses,
doc 26 §3) cannot catch.
