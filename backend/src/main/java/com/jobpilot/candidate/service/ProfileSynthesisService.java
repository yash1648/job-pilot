package com.jobpilot.candidate.service;

import com.jobpilot.ai.AiOutputInvalidException;
import com.jobpilot.ai.AiRequest;
import com.jobpilot.ai.AiService;
import com.jobpilot.ai.AiTaskType;
import com.jobpilot.ai.AiUnavailableException;
import com.jobpilot.ai.EmbeddingKind;
import com.jobpilot.ai.EmbeddingService;
import com.jobpilot.ai.ResponseSchema;
import com.jobpilot.ai.UntrustedContent;
import com.jobpilot.candidate.domain.CandidateEmbedding;
import com.jobpilot.candidate.domain.CandidateProfile;
import com.jobpilot.candidate.experience.domain.Experience;
import com.jobpilot.candidate.experience.repository.ExperienceRepository;
import com.jobpilot.candidate.repository.CandidateEmbeddingRepository;
import com.jobpilot.candidate.repository.CandidateProfileRepository;
import com.jobpilot.candidate.skill.domain.Skill;
import com.jobpilot.candidate.skill.repository.SkillRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Derives {@link CandidateProfile} derived fields from extracted evidence and
 * produces a {@link CandidateEmbedding} (doc 07 §2, §5). A heuristic seniority
 * pre-pass always runs; an AI {@code RESUME_REASONING} pass refines the result
 * and must cite evidence. Failures degrade gracefully (doc 07 §9, doc 09): AI
 * failure → heuristic-only + no embedding; embedding failure → profile saved,
 * no embedding. No fabrication.
 */
@Service
@Transactional
public class ProfileSynthesisService {

    private static final Logger log = LoggerFactory.getLogger(ProfileSynthesisService.class);
    private static final String EMBEDDING_MODEL_VERSION = "nomic-embed-text";
    private static final String SYSTEM_INSTRUCTION = """
            You are a career-profile synthesizer. Given a candidate's extracted \
            resume evidence and a heuristic seniority estimate, produce a JSON \
            object with exactly these keys:
            - "domainClassification": array of strings (professional domains, e.g. ["backend","fintech"])
            - "seniorityEstimate": one of INTERN, JUNIOR, MID, SENIOR, STAFF, PRINCIPAL — refine the provided heuristic; if you deviate by more than one band, justify it in careerTrajectorySummary
            - "careerTrajectorySummary": string summarizing trajectory (IC to leadership, lateral domain moves), citing which experiences/projects justify it
            - "strengths": array of strings
            - "weaknesses": array of strings
            - "skillGaps": array of strings (skills the candidate appears to lack for their target level)
            - "transferableSkills": array of strings
            Output ONLY valid JSON. Base every field strictly on the provided evidence; do not invent.""";

    private static final List<String> BANDS = List.of("INTERN", "JUNIOR", "MID", "SENIOR", "STAFF", "PRINCIPAL");

    private final AiService aiService;
    private final EmbeddingService embeddingService;
    private final CandidateProfileRepository profileRepository;
    private final SkillRepository skillRepository;
    private final ExperienceRepository experienceRepository;
    private final CandidateEmbeddingRepository embeddingRepository;

    public ProfileSynthesisService(AiService aiService, EmbeddingService embeddingService,
            CandidateProfileRepository profileRepository, SkillRepository skillRepository,
            ExperienceRepository experienceRepository, CandidateEmbeddingRepository embeddingRepository) {
        this.aiService = aiService;
        this.embeddingService = embeddingService;
        this.profileRepository = profileRepository;
        this.skillRepository = skillRepository;
        this.experienceRepository = experienceRepository;
        this.embeddingRepository = embeddingRepository;
    }

    public void synthesize(UUID candidateProfileId) {
        CandidateProfile profile = profileRepository.findById(candidateProfileId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "CandidateProfile not found: " + candidateProfileId));

        List<Experience> experiences = experienceRepository.findByCandidateProfileId(candidateProfileId);
        List<Skill> skills = skillRepository.findByCandidateProfileId(candidateProfileId);

        String heuristicSeniority = estimateSeniorityHeuristic(experiences);

        String seniorityEstimate = heuristicSeniority;
        List<String> domainClassification = List.of();
        String careerTrajectorySummary = null;
        List<String> strengths = List.of();
        List<String> weaknesses = List.of();
        List<String> skillGaps = List.of();
        List<String> transferableSkills = List.of();

        boolean aiSucceeded = false;
        try {
            String context = buildSynthesisContext(profile, experiences, skills, heuristicSeniority);
            AiRequest req = new AiRequest(
                    AiTaskType.RESUME_REASONING,
                    new UntrustedContent(context),
                    SYSTEM_INSTRUCTION,
                    new ResponseSchema(new String[]{
                            "domainClassification", "seniorityEstimate", "careerTrajectorySummary",
                            "strengths", "weaknesses", "skillGaps", "transferableSkills"}));
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) aiService.complete(req).data();

            seniorityEstimate = asString(data.get("seniorityEstimate"), heuristicSeniority);
            domainClassification = asStringList(data.get("domainClassification"));
            careerTrajectorySummary = asString(data.get("careerTrajectorySummary"), null);
            strengths = asStringList(data.get("strengths"));
            weaknesses = asStringList(data.get("weaknesses"));
            skillGaps = asStringList(data.get("skillGaps"));
            transferableSkills = asStringList(data.get("transferableSkills"));
            aiSucceeded = true;
        } catch (AiUnavailableException | AiOutputInvalidException e) {
            log.warn("AI synthesis unavailable for candidate {}; using heuristic seniority only. Reason: {}",
                    candidateProfileId, e.getMessage());
        }

        profile.setSeniorityEstimate(seniorityEstimate);
        profile.setDomainClassification(domainClassification.toArray(new String[0]));
        profile.setCareerTrajectorySummary(careerTrajectorySummary);
        profile.setStrengths(strengths.toArray(new String[0]));
        profile.setWeaknesses(weaknesses.toArray(new String[0]));
        profile.setSkillGaps(skillGaps.toArray(new String[0]));
        profile.setTransferableSkills(transferableSkills.toArray(new String[0]));
        profile.setUpdatedAt(Instant.now());
        profileRepository.save(profile);

        if (!aiSucceeded) {
            // AI failed: skip embedding (no reliable synthesis text). Profile saved with heuristic seniority.
            return;
        }

        try {
            String synthText = buildEmbeddingText(profile, experiences, skills, seniorityEstimate,
                    careerTrajectorySummary, domainClassification);
            float[] vector = embeddingService.embed(synthText, EmbeddingKind.RESUME);
            CandidateEmbedding embedding = embeddingRepository.findByCandidateProfileId(candidateProfileId)
                    .orElseGet(() -> new CandidateEmbedding(candidateProfileId));
            embedding.setVector(vector);
            embedding.setModelVersion(EMBEDDING_MODEL_VERSION);
            embedding.setUpdatedAt(Instant.now());
            embeddingRepository.save(embedding);
        } catch (Exception e) {
            log.warn("Embedding generation failed for candidate {}; profile synthesized without embedding. Reason: {}",
                    candidateProfileId, e.getMessage());
        }
    }

    private String buildSynthesisContext(CandidateProfile profile, List<Experience> experiences,
            List<Skill> skills, String heuristicSeniority) {
        StringBuilder sb = new StringBuilder();
        if (profile.getHeadline() != null) {
            sb.append("Headline: ").append(profile.getHeadline()).append("\n");
        }
        sb.append("Heuristic seniority estimate: ").append(heuristicSeniority).append("\n\n");
        sb.append("Experiences:\n");
        for (Experience e : experiences) {
            sb.append("- ").append(e.getTitle() == null ? "?" : e.getTitle())
                    .append(" at ").append(e.getOrganization() == null ? "?" : e.getOrganization());
            if (e.getStartDate() != null) {
                sb.append(" (").append(e.getStartDate());
                if (e.getEndDate() != null) {
                    sb.append(" to ").append(e.getEndDate());
                }
                sb.append(")");
            }
            sb.append("\n");
            if (e.getDescription() != null && !e.getDescription().isBlank()) {
                sb.append("  ").append(e.getDescription()).append("\n");
            }
        }
        sb.append("\nSkills:\n");
        for (Skill s : skills) {
            sb.append("- ").append(s.getName());
            if (s.getCategory() != null) {
                sb.append(" (").append(s.getCategory()).append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildEmbeddingText(CandidateProfile profile, List<Experience> experiences,
            List<Skill> skills, String seniorityEstimate, String careerTrajectorySummary,
            List<String> domainClassification) {
        StringBuilder sb = new StringBuilder();
        if (profile.getHeadline() != null) {
            sb.append(profile.getHeadline()).append(". ");
        }
        if (seniorityEstimate != null) {
            sb.append(seniorityEstimate).append(" ");
        }
        if (domainClassification != null && !domainClassification.isEmpty()) {
            sb.append(String.join(" ", domainClassification)).append(". ");
        }
        if (careerTrajectorySummary != null) {
            sb.append(careerTrajectorySummary).append(" ");
        }
        for (Skill s : skills) {
            sb.append(s.getName()).append(" ");
        }
        for (Experience e : experiences) {
            if (e.getTitle() != null) {
                sb.append(e.getTitle()).append(" ");
            }
            if (e.getOrganization() != null) {
                sb.append(e.getOrganization()).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String estimateSeniorityHeuristic(List<Experience> experiences) {
        int totalMonths = 0;
        String keywordBand = "MID";
        for (Experience e : experiences) {
            LocalDate start = e.getStartDate();
            LocalDate end = e.getEndDate() != null ? e.getEndDate() : LocalDate.now();
            if (start != null && end != null && !end.isBefore(start)) {
                totalMonths += Math.max(0, (int) ChronoUnit.MONTHS.between(start, end));
            }
            String title = e.getTitle() != null ? e.getTitle().toLowerCase() : "";
            if (title.contains("principal") || title.contains("distinguished")) {
                keywordBand = "PRINCIPAL";
            } else if (title.contains("staff") || title.contains("architect")) {
                keywordBand = "STAFF";
            } else if (title.contains("senior") || title.contains("lead") || title.contains("manager")) {
                keywordBand = "SENIOR";
            } else if (title.contains("junior") || title.contains("associate") || title.contains("entry")) {
                keywordBand = "JUNIOR";
            } else if (title.contains("intern") || title.contains("trainee")) {
                keywordBand = "INTERN";
            }
        }
        int years = totalMonths / 12;
        String yearsBand = years >= 12 ? "PRINCIPAL" : years >= 8 ? "STAFF"
                : years >= 5 ? "SENIOR" : years >= 2 ? "MID" : "JUNIOR";
        return higherBand(keywordBand, yearsBand);
    }

    private String higherBand(String a, String b) {
        int ia = BANDS.indexOf(a);
        int ib = BANDS.indexOf(b);
        if (ia < 0) {
            ia = 2;
        }
        if (ib < 0) {
            ib = 2;
        }
        return BANDS.get(Math.max(ia, ib));
    }

    private String asString(Object o, String fallback) {
        return o instanceof String s && !s.isBlank() ? s : fallback;
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object o) {
        if (o instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return List.of();
    }
}
