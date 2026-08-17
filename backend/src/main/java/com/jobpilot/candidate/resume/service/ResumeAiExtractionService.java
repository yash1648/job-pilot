package com.jobpilot.candidate.resume.service;

import com.jobpilot.ai.AiOutputInvalidException;
import com.jobpilot.ai.AiRequest;
import com.jobpilot.ai.AiService;
import com.jobpilot.ai.AiTaskType;
import com.jobpilot.ai.AiUnavailableException;
import com.jobpilot.ai.ResponseSchema;
import com.jobpilot.ai.StructuredResponse;
import com.jobpilot.ai.UntrustedContent;
import com.jobpilot.candidate.certification.domain.Certification;
import com.jobpilot.candidate.certification.repository.CertificationRepository;
import com.jobpilot.candidate.domain.Achievement;
import com.jobpilot.candidate.education.domain.Education;
import com.jobpilot.candidate.education.repository.EducationRepository;
import com.jobpilot.candidate.experience.domain.Experience;
import com.jobpilot.candidate.experience.repository.ExperienceRepository;
import com.jobpilot.candidate.project.domain.Project;
import com.jobpilot.candidate.project.repository.ProjectRepository;
import com.jobpilot.candidate.repository.AchievementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Evidence extraction pipeline (doc 07 §2, TASK-JP-0011). Takes the text
 * produced by {@link ResumeParsingService}, runs it through
 * {@link AiTaskType#SIMPLE_EXTRACTION} (fast structural pass) and then
 * {@link AiTaskType#RESUME_REASONING} (strong pass), and persists the
 * resulting Experience / Education / Project / Certification / Achievement
 * rows — every one carrying a non-empty {@code rawSourceExcerpt} so no
 * fabricated entity can enter the profile (Zero-Fabrication, doc 23 §4).
 *
 * <p>The resume text is always wrapped in {@link UntrustedContent} and passed
 * as a distinct field from the trusted system instruction (doc 23 §1/§2).
 * Schema-invalid or injection-shaped output surfaces as
 * {@link ResumeExtractionException}; the caller marks the resume FAILED and
 * emits a {@code ResumeParsingFailed} audit event (doc 07:102).
 */
@Service
public class ResumeAiExtractionService {

    private static final String SIMPLE_INSTRUCTION = """
            You are a resume parser. Extract the structured career evidence from
            the resume text below and return ONLY a JSON object with the keys:
            experiences, educations, projects, certifications, achievements.
            Each array item must have: title, organization, startDate, endDate
            (null if current), description, extractedSkills (array of strings).
            Do not invent anything not present in the resume.
            """;

    private static final String REASONING_INSTRUCTION = """
            You are a senior resume analyst. Re-derive the structured career
            evidence from the resume text below, and for EVERY item also include
            a non-empty "rawSourceExcerpt": the verbatim contiguous quote from
            the resume the item was derived from. Return ONLY a JSON object with
            the keys: experiences, educations, projects, certifications,
            achievements. Each item must have: title, organization, startDate,
            endDate (null if current), description, extractedSkills (array of
            strings), rawSourceExcerpt (verbatim quote). Never invent facts or
            quotes absent from the resume.
            """;

    private static final ResponseSchema SCHEMA = new ResponseSchema(new String[]{
            "experiences", "educations", "projects", "certifications", "achievements"});

    private final AiService aiService;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final ProjectRepository projectRepository;
    private final CertificationRepository certificationRepository;
    private final AchievementRepository achievementRepository;

    public ResumeAiExtractionService(AiService aiService,
                                     ExperienceRepository experienceRepository,
                                     EducationRepository educationRepository,
                                     ProjectRepository projectRepository,
                                     CertificationRepository certificationRepository,
                                     AchievementRepository achievementRepository) {
        this.aiService = aiService;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.projectRepository = projectRepository;
        this.certificationRepository = certificationRepository;
        this.achievementRepository = achievementRepository;
    }

    /** What the extraction pass produced for one resume. */
    public record ExtractionResult(
            List<Experience> experiences,
            List<Education> educations,
            List<Project> projects,
            List<Certification> certifications,
            List<Achievement> achievements) {
    }

    /**
     * Runs the two-pass extraction and persists the evidence rows for
     * {@code candidateProfileId}. Any entity missing a {@code rawSourceExcerpt}
     * is a schema violation → {@link ResumeExtractionException} with no partial
     * persistence.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExtractionResult extract(UUID candidateProfileId, String resumeText) {
        UntrustedContent content = new UntrustedContent(resumeText);
        try {
            // Pass 1: fast structural pass (doc 07:20)
            aiService.complete(new AiRequest(
                    AiTaskType.SIMPLE_EXTRACTION, content, SIMPLE_INSTRUCTION, SCHEMA));
            // Pass 2: strong pass produces the evidence-tagged output we persist
            StructuredResponse<?> reasoned = aiService.complete(new AiRequest(
                    AiTaskType.RESUME_REASONING, content, REASONING_INSTRUCTION, SCHEMA));
            return persist(candidateProfileId, asMap(reasoned.data()));
        } catch (AiOutputInvalidException | AiUnavailableException e) {
            throw new ResumeExtractionException(
                    "AI extraction failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private ExtractionResult persist(UUID candidateProfileId, Map<String, Object> data) {
        List<Experience> experiences = experienceRepository.saveAll(
                mapList(candidateProfileId, data.get("experiences"),
                        Experience::new, "experiences", true));
        List<Education> educations = educationRepository.saveAll(
                mapList(candidateProfileId, data.get("educations"),
                        Education::new, "educations", true));
        List<Project> projects = projectRepository.saveAll(
                mapList(candidateProfileId, data.get("projects"),
                        Project::new, "projects", false));
        List<Certification> certifications = certificationRepository.saveAll(
                mapList(candidateProfileId, data.get("certifications"),
                        Certification::new, "certifications", false));
        List<Achievement> achievements = achievementRepository.saveAll(
                mapList(candidateProfileId, data.get("achievements"),
                        Achievement::new, "achievements", false));
        return new ExtractionResult(experiences, educations, projects, certifications, achievements);
    }

    private interface Factory {
        Object create(UUID candidateProfileId, String title, String organization,
                      LocalDate start, LocalDate end, String description,
                      String[] skills, String excerpt);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> mapList(UUID candidateProfileId, Object raw, Factory factory,
                               String key, boolean organizationRequired) {
        if (!(raw instanceof List<?> items)) {
            return Collections.emptyList();
        }
        List<T> out = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> map)) {
                throw new ResumeExtractionException(
                        "AI extraction produced a malformed item in \"" + key + "\"");
            }
            Map<String, Object> m = (Map<String, Object>) map;
            String title = str(m.get("title"));
            String organization = str(m.get("organization"));
            if (title.isBlank() || (organizationRequired && organization.isBlank())) {
                throw new ResumeExtractionException(
                        "AI extraction produced an item missing title/organization in \"" + key + "\"");
            }
            // organization is optional for projects/certifications/achievements
            // (doc 07 §2); store null when absent so the row still persists.
            String orgToStore = organizationRequired ? organization
                    : (organization.isBlank() ? null : organization);
            String excerpt = str(m.get("rawSourceExcerpt"));
            if (excerpt.isBlank()) {
                // Zero-Fabrication: no evidence trail → schema violation, no partial profile
                throw new ResumeExtractionException(
                        "AI extraction produced an item without rawSourceExcerpt in \"" + key + "\"");
            }
            out.add((T) factory.create(
                    candidateProfileId, title, orgToStore,
                    date(m.get("startDate")), date(m.get("endDate")),
                    str(m.get("description")),
                    skills(m.get("extractedSkills")),
                    excerpt));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object data) {
        if (data instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new ResumeExtractionException("AI extraction returned an unexpected shape");
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String[] skills(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return new String[0];
        }
        return list.stream().map(String::valueOf).toArray(String[]::new);
    }

    /** Lenient date parse — accepts ISO dates and "Jan 2019"/"2019" forms; null if absent/illegal. */
    private static LocalDate date(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        try {
            return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {
            // fall through to looser formats
        }
        for (DateTimeFormatter f : new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("yyyy-MM"),
                DateTimeFormatter.ofPattern("yyyy")}) {
            try {
                LocalDate d = LocalDate.parse(s, f);
                return d.getDayOfMonth() == 1 ? d : d.withDayOfMonth(1);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }
}