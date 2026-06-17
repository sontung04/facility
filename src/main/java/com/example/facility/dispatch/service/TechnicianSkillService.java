package com.example.facility.dispatch.service;

import com.example.facility.dispatch.dto.request.TechnicianSkillRequest;
import com.example.facility.dispatch.dto.response.EligibleTechnicianResponse;
import com.example.facility.dispatch.dto.response.TechnicianSkillResponse;
import com.example.facility.dispatch.model.TechnicianSkill;
import com.example.facility.dispatch.repository.TechnicianSkillRepository;
import com.example.facility.dispatch.service.DispatchScoringService.ScoreBreakdown;
import com.example.facility.facility.CategoryInfo;
import com.example.facility.facility.FacilityApi;
import com.example.facility.facility.model.Category;
import com.example.facility.identity.IdentityApi;
import com.example.facility.identity.UserInfo;
import com.example.facility.shared.exception.ErrorCode;
import com.example.facility.shared.exception.WebException;
import com.example.facility.ticket.TicketApi;
import com.example.facility.ticket.TicketSummary;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TechnicianSkillService {

    private final TechnicianSkillRepository   skillRepository;
    private final IdentityApi                 identityApi;
    private final FacilityApi                 facilityApi;
    private final TicketApi                   ticketApi;
    private final EntityManager               entityManager;
    private final DispatchScoringService      scoringService;
    private final TechnicianPerformanceService performanceService;

    // ── CRUD ────────────────────────────────────────────────────────────────────

    @Transactional
    public TechnicianSkillResponse addSkill(TechnicianSkillRequest request) {
        UserInfo technician = identityApi.findById(request.getUserId());
        CategoryInfo categoryInfo = facilityApi.getCategory(request.getCategoryId());

        if (skillRepository.findByUserIdAndCategoryId(
                request.getUserId(), request.getCategoryId()).isPresent()) {
            throw new WebException(ErrorCode.SKILL_ALREADY_EXISTS);
        }

        Category categoryRef = entityManager.getReference(Category.class, categoryInfo.id());

        TechnicianSkill skill = new TechnicianSkill();
        skill.setUserId(request.getUserId());
        skill.setCategory(categoryRef);
        skill.setSkillLevel(request.getSkillLevel());
        skill.setMaxConcurrentJobs(request.getMaxConcurrentJobs());
        skill.setAvailable(request.isAvailable());
        skill = skillRepository.save(skill);

        log.info("Added skill {} / category '{}' for technician {}",
                request.getSkillLevel(), categoryInfo.name(), technician.username());
        return toResponse(skill, technician);
    }

    @Transactional(readOnly = true)
    public List<TechnicianSkillResponse> getSkillsByTechnician(Long userId) {
        UserInfo technician = identityApi.findById(userId);
        return skillRepository.findByUserId(userId).stream()
                .map(s -> toResponse(s, technician))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TechnicianSkillResponse> getSkillsByCategory(Long categoryId) {
        facilityApi.getCategory(categoryId);
        List<TechnicianSkill> skills = skillRepository.findByCategoryId(categoryId);
        List<Long> userIds = skills.stream().map(TechnicianSkill::getUserId).distinct().toList();
        Map<Long, UserInfo> userMap = identityApi.findAllById(userIds)
                .stream().collect(Collectors.toMap(UserInfo::id, ui -> ui));
        return skills.stream()
                .map(s -> toResponse(s, userMap.get(s.getUserId())))
                .toList();
    }

    @Transactional
    public TechnicianSkillResponse updateSkill(Long skillId, TechnicianSkillRequest request) {
        TechnicianSkill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
        if (request.getSkillLevel() != null)      skill.setSkillLevel(request.getSkillLevel());
        if (request.getMaxConcurrentJobs() != null) skill.setMaxConcurrentJobs(request.getMaxConcurrentJobs());
        skill.setAvailable(request.isAvailable());
        skill = skillRepository.save(skill);
        return toResponse(skill, identityApi.findById(skill.getUserId()));
    }

    @Transactional
    public void removeSkill(Long skillId) {
        if (!skillRepository.existsById(skillId)) {
            throw new WebException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        skillRepository.deleteById(skillId);
        log.info("Removed technician skill record id={}", skillId);
    }

    // ── Eligible-technicians query ───────────────────────────────────────────────

    /**
     * Returns all technicians qualified for the ticket's category, enriched with:
     * <ul>
     *   <li>live workload (activeTickets / maxConcurrentJobs),</li>
     *   <li>the full dispatch score breakdown so the dispatcher can see exactly why
     *       each technician was ranked where they were,</li>
     *   <li>a rank field (1 = the candidate the auto-assign algorithm would choose).</li>
     * </ul>
     *
     * <p>Both available and unavailable technicians are included so the dispatcher has
     * full visibility.  The rank is computed only over available, under-capacity candidates.
     */
    @Transactional(readOnly = true)
    public List<EligibleTechnicianResponse> getEligibleTechnicians(Long ticketId) {
        TicketSummary ticket = ticketApi.getTicketById(ticketId);
        Long categoryId = ticket.categoryId();

        List<TechnicianSkill> skills = skillRepository.findByCategoryId(categoryId);
        if (skills.isEmpty()) return List.of();

        // Batch data gathering — 4 queries total, no N+1
        List<Long> userIds = skills.stream().map(TechnicianSkill::getUserId).distinct().toList();
        Map<Long, Long>          workload      = ticketApi.countActiveTicketsByTechnicianIds(userIds);
        Map<Long, UserInfo>      userMap       = identityApi.findAllById(userIds).stream()
                                                    .collect(Collectors.toMap(UserInfo::id, ui -> ui));
        Map<Long, Double>        qualityScores = performanceService.computeQualityScores(userIds);
        Map<Long, LocalDateTime> lastAssigned  = scoringService.loadLastAssignmentTimes(userIds);

        // Build response DTOs with score breakdown for each candidate
        List<EligibleTechnicianResponse> responses = new ArrayList<>();
        for (TechnicianSkill s : skills) {
            long    active     = workload.getOrDefault(s.getUserId(), 0L);
            boolean atCapacity = active >= s.getMaxConcurrentJobs();
            UserInfo userInfo  = userMap.get(s.getUserId());

            // Score is always computed so the dispatcher can see the breakdown,
            // but rank is only meaningful for available, under-capacity candidates.
            ScoreBreakdown bd = scoringService.score(
                    s,
                    active,
                    qualityScores.getOrDefault(s.getUserId(), 0.5),
                    lastAssigned.get(s.getUserId()),
                    ticket);

            responses.add(EligibleTechnicianResponse.builder()
                    .technicianId(s.getUserId())
                    .username(userInfo != null ? userInfo.username() : "unknown")
                    .categoryId(s.getCategory().getId())
                    .categoryName(s.getCategory().getName())
                    .skillLevel(s.getSkillLevel())
                    .maxConcurrentJobs(s.getMaxConcurrentJobs())
                    .activeTickets(active)
                    .available(s.isAvailable())
                    .atCapacity(atCapacity)
                    // score breakdown
                    .compositeScore(round3(bd.total()))
                    .workloadScore(round3(bd.workload()))
                    .skillScore(round3(bd.skillMismatch()))
                    .qualityScore(round3(bd.qualityPenalty()))
                    .fairnessScore(round3(bd.fairnessPenalty()))
                    .urgency(round3(bd.urgency()))
                    .build());
        }

        // Assign ranks: sort available+under-capacity by composite score;
        // unavailable or over-capacity technicians get rank null (not in auto-assign pool).
        List<EligibleTechnicianResponse> rankable = responses.stream()
                .filter(r -> r.isAvailable() && !r.isAtCapacity())
                .sorted(Comparator.comparingDouble(r -> r.getCompositeScore()))
                .toList();
        for (int i = 0; i < rankable.size(); i++) {
            rankable.get(i).setRank(i + 1);
        }

        // Return all candidates sorted: ranked ones first (by rank), then the rest
        responses.sort(Comparator.comparingInt(r -> r.getRank() != null ? r.getRank() : Integer.MAX_VALUE));
        return responses;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    /** Round a double to 3 decimal places for cleaner API output. */
    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private TechnicianSkillResponse toResponse(TechnicianSkill skill, UserInfo userInfo) {
        return TechnicianSkillResponse.builder()
                .id(skill.getId())
                .userId(skill.getUserId())
                .username(userInfo != null ? userInfo.username() : "unknown")
                .categoryId(skill.getCategory().getId())
                .categoryName(skill.getCategory().getName())
                .skillLevel(skill.getSkillLevel())
                .maxConcurrentJobs(skill.getMaxConcurrentJobs())
                .available(skill.isAvailable())
                .createdAt(skill.getCreatedAt())
                .build();
    }
}
