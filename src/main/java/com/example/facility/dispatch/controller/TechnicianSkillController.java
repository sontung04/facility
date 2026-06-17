package com.example.facility.dispatch.controller;

import com.example.facility.shared.apiresponse.ApiResponse;
import com.example.facility.dispatch.dto.response.EligibleTechnicianResponse;
import com.example.facility.dispatch.dto.request.TechnicianSkillRequest;
import com.example.facility.dispatch.dto.response.TechnicianSkillResponse;
import com.example.facility.dispatch.service.TechnicianSkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Technician-skill CRUD + eligible-technician query.
 * Base path: /api/v1/dispatch/skills
 */
@RestController
@RequestMapping("/api/v1/dispatch/skills")
@RequiredArgsConstructor
public class TechnicianSkillController {

    private final TechnicianSkillService skillService;

    // ── CRUD ────────────────────────────────────────────────────────────────────

    /**
     * Register a new skill/category for a technician.
     * Body: { userId, categoryId, skillLevel, maxConcurrentJobs, available }
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TechnicianSkillResponse>> addSkill(
            @Valid @RequestBody TechnicianSkillRequest request) {
        TechnicianSkillResponse response = skillService.addSkill(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Skill added", response));
    }

    /**
     * List all skill records for a given technician.
     */
    @GetMapping("/technician/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<TechnicianSkillResponse>>> getByTechnician(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Skills retrieved",
                skillService.getSkillsByTechnician(userId)));
    }

    /**
     * List all technicians who have a skill record for a given category.
     */
    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TechnicianSkillResponse>>> getByCategory(
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success("Skills retrieved",
                skillService.getSkillsByCategory(categoryId)));
    }

    /**
     * Update skill level, max-concurrent-jobs, or availability for an existing skill record.
     * Only provided fields are updated.
     */
    @PutMapping("/{skillId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TechnicianSkillResponse>> updateSkill(
            @PathVariable Long skillId,
            @RequestBody TechnicianSkillRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Skill updated",
                skillService.updateSkill(skillId, request)));
    }

    /**
     * Remove a skill record from a technician.
     * This does NOT affect existing ticket assignments.
     */
    @DeleteMapping("/{skillId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeSkill(@PathVariable Long skillId) {
        skillService.removeSkill(skillId);
        return ResponseEntity.ok(ApiResponse.success("Skill removed", null));
    }

    // ── Eligible-technician query ────────────────────────────────────────────────

    /**
     * Return all technicians qualified for a ticket's category, with live workload info.
     * Useful for the dispatcher's "manual assign" UI dropdown.
     * GET /api/v1/dispatch/skills/eligible?ticketId=42
     */
    @GetMapping("/eligible")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<EligibleTechnicianResponse>>> getEligibleTechnicians(
            @RequestParam Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success("Eligible technicians retrieved",
                skillService.getEligibleTechnicians(ticketId)));
    }
}


