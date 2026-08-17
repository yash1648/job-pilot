package com.jobpilot.candidate.skill.service;

import com.jobpilot.candidate.certification.domain.Certification;
import com.jobpilot.candidate.certification.repository.CertificationRepository;
import com.jobpilot.candidate.education.domain.Education;
import com.jobpilot.candidate.education.repository.EducationRepository;
import com.jobpilot.candidate.experience.domain.Experience;
import com.jobpilot.candidate.experience.repository.ExperienceRepository;
import com.jobpilot.candidate.project.domain.Project;
import com.jobpilot.candidate.project.repository.ProjectRepository;
import com.jobpilot.candidate.skill.domain.Skill;
import com.jobpilot.candidate.skill.domain.SkillEvidence;
import com.jobpilot.candidate.skill.domain.SkillEvidenceSourceType;
import com.jobpilot.candidate.skill.repository.SkillEvidenceRepository;
import com.jobpilot.candidate.skill.repository.SkillRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Derives {@link Skill} rows from extracted candidate evidence (doc 07 §4). For
 * every skill drawn from an experience/project/certification/education entity a
 * {@link SkillEvidence} row is persisted pointing at that specific entity and
 * its verbatim excerpt — the Zero-Fabrication grounding requirement. A Skill is
 * never persisted without evidence, and raw strings are normalized (doc 07:27-28)
 * before persistence. Re-running (re-parse) reuses existing skills and only
 * appends new evidence, so no duplicate skill rows accumulate.
 */
@Service
public class SkillExtractionService {

    // Entity-derived skills are demonstrated (not merely "listed"), so they clear
    // the high-confidence threshold for matching/generation (doc 23 §4, doc 35).
    static final double DEMONSTRATED_CONFIDENCE = 0.9;

    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final ProjectRepository projectRepository;
    private final CertificationRepository certificationRepository;
    private final SkillRepository skillRepository;
    private final SkillEvidenceRepository skillEvidenceRepository;
    private final SkillNormalizationService normalizationService;

    public SkillExtractionService(ExperienceRepository experienceRepository,
                                  EducationRepository educationRepository,
                                  ProjectRepository projectRepository,
                                  CertificationRepository certificationRepository,
                                  SkillRepository skillRepository,
                                  SkillEvidenceRepository skillEvidenceRepository,
                                  SkillNormalizationService normalizationService) {
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.projectRepository = projectRepository;
        this.certificationRepository = certificationRepository;
        this.skillRepository = skillRepository;
        this.skillEvidenceRepository = skillEvidenceRepository;
        this.normalizationService = normalizationService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Skill> extractSkills(UUID candidateProfileId) {
        List<SkillSource> sources = new ArrayList<>();
        experienceRepository.findByCandidateProfileId(candidateProfileId)
                .forEach(e -> sources.add(new SkillSource(e.getExtractedSkills(),
                        SkillEvidenceSourceType.EXPERIENCE, e.getId(), e.getRawSourceExcerpt())));
        educationRepository.findByCandidateProfileId(candidateProfileId)
                .forEach(ed -> sources.add(new SkillSource(ed.getExtractedSkills(),
                        SkillEvidenceSourceType.EDUCATION, ed.getId(), ed.getRawSourceExcerpt())));
        projectRepository.findByCandidateProfileId(candidateProfileId)
                .forEach(p -> sources.add(new SkillSource(p.getExtractedSkills(),
                        SkillEvidenceSourceType.PROJECT, p.getId(), p.getRawSourceExcerpt())));
        certificationRepository.findByCandidateProfileId(candidateProfileId)
                .forEach(c -> sources.add(new SkillSource(c.getExtractedSkills(),
                        SkillEvidenceSourceType.CERTIFICATION, c.getId(), c.getRawSourceExcerpt())));

        // reuse existing skills (re-parse safe) keyed by normalized name
        Map<String, Skill> existing = new HashMap<>();
        skillRepository.findByCandidateProfileId(candidateProfileId)
                .forEach(s -> existing.put(s.getNormalizedName(), s));

        Map<String, Skill> normalizedToSkill = new HashMap<>();
        List<Skill> newSkills = new ArrayList<>();
        List<SkillEvidence> evidence = new ArrayList<>();

        for (SkillSource src : sources) {
            if (src.skills() == null) {
                continue;
            }
            for (String raw : src.skills()) {
                String normalized = normalizationService.normalize(raw);
                if (normalized.isBlank()) {
                    continue;
                }
                Skill skill = normalizedToSkill.get(normalized);
                if (skill == null) {
                    skill = existing.get(normalized);
                    if (skill == null) {
                        skill = new Skill(candidateProfileId, raw.trim(), normalized);
                        newSkills.add(skill);
                    }
                    normalizedToSkill.put(normalized, skill);
                }
                // invariant: every skill gets >=1 evidence row
                evidence.add(new SkillEvidence(skill.getId(), src.sourceType(), src.sourceId(),
                        src.excerpt() == null ? "" : src.excerpt(), DEMONSTRATED_CONFIDENCE));
            }
        }

        if (!newSkills.isEmpty()) {
            skillRepository.saveAll(newSkills);
        }
        skillEvidenceRepository.saveAll(evidence);

        List<Skill> result = new ArrayList<>(normalizedToSkill.values());
        result.sort(Comparator.comparing(Skill::getNormalizedName));
        return result;
    }

    private record SkillSource(String[] skills, SkillEvidenceSourceType sourceType, UUID sourceId, String excerpt) {
    }
}