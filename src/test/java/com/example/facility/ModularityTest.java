package com.example.facility;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Verifies Spring Modulith module boundaries.
 *
 * <p>Run with: {@code mvn test -Dtest=ModularityTest}
 *
 * <p>A passing test confirms that no module imports another module's internal
 * packages (service, repository, model sub-packages) without going through
 * the published public API.
 *
 * <p>Known structural couplings that require JPA entity references
 * (e.g. SLABreach → Ticket, SLAPolicy → Category) are documented in the
 * respective module's {@code package-info.java} and may appear as warnings.
 * They are accepted because eliminating them would require schema changes
 * (replacing {@code @ManyToOne} with plain ID columns).
 */
class ModularityTest {

    static final ApplicationModules modules =
            ApplicationModules.of(FacilityApplication.class);

    @Test
    void verifyModuleBoundaries() {
        modules.verify();
    }

    @Test
    void generateModuleDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
