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
import com.jobpilot.candidate.skill.repository.SkillEvidenceRepository;
import com.jobpilot.candidate.skill.repository.SkillRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SkillExtractionService unit tests (doc 07 §4). No Spring context — repositories
 * are mocked; the fake-free deterministic normalization is used directly.
 */
@ExtendWith(MockitoExtension.class)
class SkillExtractionServiceTest {

    @Mock
    ExperienceRepository experienceRepository;

    @Mock
    EducationRepository educationRepository;

    @Mock
    ProjectRepository projectRepository;

    @Mock
    CertificationRepository certificationRepository;

    @Mock
    SkillRepository skillRepository;

    @Mock
    SkillEvidenceRepository skillEvidenceRepository;

    private SkillExtractionService service;

    @BeforeEach
    void setUp() {
        service = new SkillExtractionService(experienceRepository, educationRepository,
                projectRepository, certificationRepository, skillRepository,
                skillEvidenceRepository, new SkillNormalizationService());
    }

    @Test
    void extractsSkillsWithEvidenceFromEachEntity() {
        UUID candidateId = UUID.randomUUID();
        Experience exp = new Experience(candidateId, "Eng", "Acme", null, null, "did X",
                new String[]{"ReactJS", "Node.js"}, "exp excerpt");
        Education ed = new Education(candidateId, "BSc", "Uni", null, null, "",
                new String[]{"Algorithms"}, "ed excerpt");
        Project pr = new Project(candidateId, "P", "Acme", null, null, "",
                new String[]{"Java", "Spring Boot"}, "pr excerpt");
        Certification cert = new Certification(candidateId, "CKA", "CNCF", null, null, "",
                new String[]{}, "cert excerpt");

        when(experienceRepository.findByCandidateProfileId(candidateId)).thenReturn(List.of(exp));
        when(educationRepository.findByCandidateProfileId(candidateId)).thenReturn(List.of(ed));
        when(projectRepository.findByCandidateProfileId(candidateId)).thenReturn(List.of(pr));
        when(certificationRepository.findByCandidateProfileId(candidateId)).thenReturn(List.of(cert));
        when(skillRepository.findByCandidateProfileId(candidateId)).thenReturn(List.of());
        when(skillRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(skillEvidenceRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Skill> skills = service.extractSkills(candidateId);

        // react, node, algorithms, java, spring boot = 5 distinct normalized skills
        assertEquals(5, skills.size());
        Set<String> norms = skills.stream().map(Skill::getNormalizedName).collect(Collectors.toSet());
        assertTrue(norms.containsAll(Set.of("react", "node", "algorithms", "java", "spring boot")));

        ArgumentCaptor<List<SkillEvidence>> captor = ArgumentCaptor.forClass(List.class);
        verify(skillEvidenceRepository).saveAll(captor.capture());
        List<SkillEvidence> evidence = captor.getValue();
        assertEquals(5, evidence.size());
        assertTrue(evidence.stream().allMatch(e -> e.getConfidence() == 0.9));
        assertTrue(evidence.stream().allMatch(e -> e.getSourceType() != null));
    }

    @Test
    void everySkillHasAtLeastOneEvidenceRow() {
        UUID candidateId = UUID.randomUUID();
        Experience exp = new Experience(candidateId, "Eng", "Acme", null, null, "",
                new String[]{"ReactJS"}, "x");

        when(experienceRepository.findByCandidateProfileId(candidateId)).thenReturn(List.of(exp));
        when(educationRepository.findByCandidateProfileId(candidateId)).thenReturn(List.of());
        when(projectRepository.findByCandidateProfileId(candidateId)).thenReturn(List.of());
        when(certificationRepository.findByCandidateProfileId(candidateId)).thenReturn(List.of());
        when(skillRepository.findByCandidateProfileId(candidateId)).thenReturn(List.of());
        when(skillRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(skillEvidenceRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Skill> skills = service.extractSkills(candidateId);

        ArgumentCaptor<List<SkillEvidence>> evCap = ArgumentCaptor.forClass(List.class);
        verify(skillEvidenceRepository).saveAll(evCap.capture());
        // invariant: #skills == #evidence (one evidence per skill occurrence)
        assertEquals(skills.size(), evCap.getValue().size());
        Set<UUID> skillIds = skills.stream().map(Skill::getId).collect(Collectors.toSet());
        Set<UUID> evidenceSkillIds = evCap.getValue().stream()
                .map(SkillEvidence::getSkillId).collect(Collectors.toSet());
        assertTrue(evidenceSkillIds.containsAll(skillIds));
    }

    @Test
    void reusesExistingSkillOnReparseAndAddsNewEvidence() {
        UUID candidateId = UUID.randomUUID();
        Skill existing = new Skill(candidateId, "React", "react");
        Experience exp = new Experience(candidateId, "Eng", "Acme", null, null, "",
                new String[]{"ReactJS"}, "x");

        when(experienceRepository.findByCandidateProfileId(candidateId)).thenReturn(List.of(exp));
        when(educationRepository.findByCandidateProfileId(candidateId)).thenReturn(List.of());
        when(projectRepository.findByCandidateProfileId(candidateId)).thenReturn(List.of());
        when(certificationRepository.findByCandidateProfileId(candidateId)).thenReturn(List.of());
        when(skillRepository.findByCandidateProfileId(candidateId)).thenReturn(List.of(existing));
        when(skillEvidenceRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Skill> skills = service.extractSkills(candidateId);

        assertEquals(1, skills.size());
        assertEquals(existing.getId(), skills.get(0).getId()); // reused, not duplicated
        verify(skillRepository, never()).saveAll(anyList());
        ArgumentCaptor<List<SkillEvidence>> evCap = ArgumentCaptor.forClass(List.class);
        verify(skillEvidenceRepository).saveAll(evCap.capture());
        assertEquals(1, evCap.getValue().size());
        assertEquals(existing.getId(), evCap.getValue().get(0).getSkillId());
    }
}