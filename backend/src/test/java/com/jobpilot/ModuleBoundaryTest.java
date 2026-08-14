package com.jobpilot;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Module boundary enforcement (doc 34 §3, doc 02 §3).
 * Cross-module access must go through published service interfaces — never
 * through another module's repository or domain packages.
 */
class ModuleBoundaryTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.jobpilot");

    @Test
    void noModuleImportsAnotherModulesRepositoryOrDomain() {
        String[] modules = {
                "auth", "user", "candidate", "company", "jobs", "matching",
                "career", "ai", "application", "browser", "workflow", "tracking",
                "analytics", "notification", "security", "audit", "storage", "common"
        };
        for (String m : modules) {
            // e.g. auth must not import candidate.repository / candidate.domain, etc.
            for (String other : modules) {
                if (m.equals(other)) continue;
                ArchRule rule = noClasses()
                        .that().resideInAPackage("com.jobpilot." + m + "..")
                        .should().accessClassesThat()
                        .resideInAnyPackage("com.jobpilot." + other + ".repository..",
                                             "com.jobpilot." + other + ".domain..");
                rule.check(CLASSES);
            }
        }
    }

    @Test
    void nothingOutsideAiCallsAProviderDirectly() {
        // doc 02 §5: all AI access goes through the ai module interfaces
        noClasses()
                .that().resideOutsideOfPackage("com.jobpilot.ai..")
                .should().dependOnClassesThat().resideInAnyPackage("com.jobpilot.ai.provider..")
                .because("all AI access must go through the ai module service interfaces (doc 02 §5)")
                .check(CLASSES);
    }
}
