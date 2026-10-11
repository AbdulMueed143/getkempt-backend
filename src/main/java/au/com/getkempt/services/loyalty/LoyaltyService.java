package au.com.getkempt.services.loyalty;

import au.com.getkempt.common.error.exceptions.ConflictException;
import au.com.getkempt.common.error.exceptions.ForbiddenException;
import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.common.pagination.CursorPageRequest;
import au.com.getkempt.common.query.SortRequest;
import au.com.getkempt.common.security.Permission;
import au.com.getkempt.common.tenant.TenantContextHolder;
import au.com.getkempt.common.web.IfMatchSupport;
import au.com.getkempt.entities.Client;
import au.com.getkempt.entities.booking.Booking;
import au.com.getkempt.entities.loyalty.ClientLoyaltyState;
import au.com.getkempt.entities.loyalty.LoyaltyProgram;
import au.com.getkempt.entities.loyalty.LoyaltyReward;
import au.com.getkempt.enums.loyalty.ProgramStatus;
import au.com.getkempt.enums.loyalty.RewardStatus;
import au.com.getkempt.enums.loyalty.TriggerType;
import au.com.getkempt.mappers.LoyaltyMapper;
import au.com.getkempt.repositories.client.ClientRepository;
import au.com.getkempt.repositories.loyalty.ClientLoyaltyStateRepository;
import au.com.getkempt.repositories.loyalty.LoyaltyProgramRepository;
import au.com.getkempt.repositories.loyalty.LoyaltyRewardRepository;
import au.com.getkempt.request.loyalty.LoyaltyProgramWriteRequest;
import au.com.getkempt.request.loyalty.ProgramReorderRequest;
import au.com.getkempt.request.loyalty.RewardRedeemRequest;
import au.com.getkempt.request.loyalty.RewardRevokeRequest;
import au.com.getkempt.responses.client.ClientLoyaltyProgressResponse;
import au.com.getkempt.responses.loyalty.LoyaltyProgramResponse;
import au.com.getkempt.responses.loyalty.LoyaltyRewardResponse;
import au.com.getkempt.responses.loyalty.LoyaltyStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Sequential loyalty programs + per-client progress, per {@code paths/loyalty.yaml}.
 *
 * <p>The contract describes a "worker job that processes booking.completed" to
 * evaluate progress; this codebase has no scheduler (see {@code BookingService}
 * class javadoc), so {@link #advanceProgress} runs synchronously, called
 * directly from {@code BookingService#complete} the moment a booking finishes —
 * functionally equivalent with zero replication lag, at the cost of adding a
 * little work to the completion request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private static final Set<String> REWARD_SORTABLE = Set.of("earnedAt", "createdAt");

    private final LoyaltyProgramRepository programRepository;
    private final LoyaltyRewardRepository rewardRepository;
    private final ClientLoyaltyStateRepository stateRepository;
    private final ClientRepository clientRepository;
    private final LoyaltyMapper mapper;

    // ── programs ────────────────────────────────────────────────────────────

    public List<LoyaltyProgramResponse> listPrograms(ProgramStatus status) {
        return programRepository.listInSequenceOrder(status).stream().map(mapper::toResponse).toList();
    }

    public LoyaltyProgramResponse getProgram(String programId) {
        return mapper.toResponse(programRepository.requireById(programId));
    }

    public LoyaltyProgramResponse createProgram(LoyaltyProgramWriteRequest request) {
        requireManageAccess();
        LoyaltyProgram program = new LoyaltyProgram();
        program.setSequenceOrder(programRepository.maxSequenceOrder() + 1);
        applyWrite(program, request, true);
        programRepository.insert(program);
        return mapper.toResponse(program);
    }

    public LoyaltyProgramResponse updateProgram(String programId, String ifMatch, LoyaltyProgramWriteRequest request) {
        requireManageAccess();
        LoyaltyProgram program = programRepository.requireById(programId);
        IfMatchSupport.check(ifMatch, program.getVersion());
        applyWrite(program, request, false);
        programRepository.save(program);
        return mapper.toResponse(program);
    }

    public void deleteProgram(String programId) {
        requireManageAccess();
        LoyaltyProgram program = programRepository.requireById(programId);
        if (rewardRepository.hasPendingForProgram(programId)) {
            throw new ConflictException("Cannot delete — clients have pending rewards under this program");
        }
        programRepository.softDelete(program);
    }

    public LoyaltyProgramResponse activateProgram(String programId) {
        requireManageAccess();
        LoyaltyProgram program = programRepository.requireById(programId);
        program.setStatus(ProgramStatus.ACTIVE);
        programRepository.save(program);
        return mapper.toResponse(program);
    }

    public LoyaltyProgramResponse pauseProgram(String programId) {
        requireManageAccess();
        LoyaltyProgram program = programRepository.requireById(programId);
        program.setStatus(ProgramStatus.INACTIVE);
        programRepository.save(program);
        return mapper.toResponse(program);
    }

    public LoyaltyProgramResponse duplicateProgram(String programId) {
        requireManageAccess();
        LoyaltyProgram source = programRepository.requireById(programId);
        LoyaltyProgram copy = new LoyaltyProgram();
        copy.setSequenceOrder(programRepository.maxSequenceOrder() + 1);
        copy.setName(source.getName() + " (Copy)");
        copy.setDescription(source.getDescription());
        copy.setRewardType(source.getRewardType());
        copy.setRewardValue(source.getRewardValue());
        copy.setRewardLabel(source.getRewardLabel());
        copy.setRedeemLocation(source.getRedeemLocation());
        copy.setPartnerName(source.getPartnerName());
        copy.setPartnerAddress(source.getPartnerAddress());
        copy.setTriggerType(source.getTriggerType());
        copy.setTriggerValue(source.getTriggerValue());
        copy.setProgramExpiryDate(source.getProgramExpiryDate());
        copy.setRewardExpiryDays(source.getRewardExpiryDays());
        copy.setStatus(ProgramStatus.DRAFT);
        programRepository.insert(copy);
        return mapper.toResponse(copy);
    }

    public void reorderPrograms(ProgramReorderRequest request) {
        requireManageAccess();
        int order = 1;
        for (String programId : request.programIds()) {
            LoyaltyProgram program = programRepository.requireById(programId);
            program.setSequenceOrder(order++);
            programRepository.save(program);
        }
    }

    // ── client progress ─────────────────────────────────────────────────────

    public ClientLoyaltyProgressResponse getClientProgress(String clientId) {
        Client client = clientRepository.requireById(clientId);
        ClientLoyaltyState state = stateRepository.getOrCreate(clientId);
        LoyaltyProgram currentProgram = state.getCurrentProgramId() != null
                ? programRepository.findById(state.getCurrentProgramId()).orElse(null)
                : null;
        double targetValue = currentProgram != null ? currentProgram.getTriggerValue() : 0;
        double progressValue = currentProgram != null ? currentTriggerValue(client, currentProgram) - state.getBaselineValue() : 0;
        List<ClientLoyaltyProgressResponse.HistoryEntry> history = state.getHistory().stream()
                .map(h -> new ClientLoyaltyProgressResponse.HistoryEntry(h.getProgramId(), h.getEarnedAt(), h.getRedeemedAt()))
                .toList();
        return new ClientLoyaltyProgressResponse(clientId, state.getCurrentProgramId(), progressValue, targetValue, history);
    }

    // ── rewards ──────────────────────────────────────────────────────────────

    public CursorPage<LoyaltyRewardResponse> listRewards(String cursor, Integer limit, String clientId, String programId, String status) {
        List<Criteria> ands = new ArrayList<>();
        if (clientId != null) {
            ands.add(Criteria.where("clientId").is(clientId));
        }
        if (programId != null) {
            ands.add(Criteria.where("programId").is(programId));
        }
        if (status != null && !status.isBlank()) {
            ands.add(Criteria.where("status").is(RewardStatus.from(status)));
        }
        Criteria filter = ands.isEmpty() ? new Criteria() : new Criteria().andOperator(ands.toArray(new Criteria[0]));
        SortRequest sort = SortRequest.parse(null, REWARD_SORTABLE, SortRequest.desc("earnedAt"));
        CursorPageRequest page = CursorPageRequest.of(cursor, limit);
        return rewardRepository.list(filter, sort, page).map(r -> mapper.toResponse(lazilyExpire(r)));
    }

    public LoyaltyRewardResponse redeemReward(String rewardId, RewardRedeemRequest request) {
        requireManageAccess();
        LoyaltyReward reward = lazilyExpire(rewardRepository.requireById(rewardId));
        if (reward.getStatus() != RewardStatus.PENDING) {
            throw new ConflictException("Reward is " + reward.getStatus().value() + ", not pending");
        }
        reward.setStatus(RewardStatus.REDEEMED);
        reward.setRedeemedAt(Instant.now());
        reward.setRedeemedAtBookingId(request != null ? request.bookingId() : null);
        rewardRepository.save(reward);

        programRepository.findById(reward.getProgramId()).ifPresent(program -> {
            program.setTotalRedeemed(program.getTotalRedeemed() + 1);
            programRepository.save(program);
        });
        stateRepository.findById(reward.getClientId()).ifPresent(state -> {
            state.getHistory().stream()
                    .filter(h -> h.getProgramId().equals(reward.getProgramId()) && h.getRedeemedAt() == null)
                    .findFirst()
                    .ifPresent(h -> h.setRedeemedAt(reward.getRedeemedAt()));
            stateRepository.save(state);
        });
        return mapper.toResponse(reward);
    }

    public LoyaltyRewardResponse revokeReward(String rewardId, RewardRevokeRequest request) {
        requireManageAccess();
        LoyaltyReward reward = lazilyExpire(rewardRepository.requireById(rewardId));
        if (reward.getStatus() != RewardStatus.PENDING) {
            throw new ConflictException("Reward is " + reward.getStatus().value() + ", not pending");
        }
        reward.setStatus(RewardStatus.REVOKED);
        reward.setRevokeReason(request != null ? request.reason() : null);
        rewardRepository.save(reward);
        return mapper.toResponse(reward);
    }

    public LoyaltyStatsResponse getStats() {
        List<LoyaltyProgram> programs = programRepository.listInSequenceOrder(null);
        int totalEarned = programs.stream().mapToInt(LoyaltyProgram::getTotalEarned).sum();
        int totalRedeemed = programs.stream().mapToInt(LoyaltyProgram::getTotalRedeemed).sum();
        double rate = totalEarned == 0 ? 0.0 : (totalRedeemed * 100.0) / totalEarned;
        LoyaltyStatsResponse.TopProgram top = programs.stream()
                .max((a, b) -> Integer.compare(a.getTotalEarned(), b.getTotalEarned()))
                .filter(p -> p.getTotalEarned() > 0)
                .map(p -> new LoyaltyStatsResponse.TopProgram(p.getId(), p.getName(), p.getTotalEarned()))
                .orElse(null);
        return new LoyaltyStatsResponse(totalEarned, totalRedeemed, rate, top);
    }

    // ── progression engine (called from BookingService#complete) ───────────

    /**
     * Evaluates the client's progress toward their current program after a
     * completed booking, emitting a {@link LoyaltyReward} and advancing to the
     * next program in the ladder if the trigger is now met. A client works
     * toward exactly one (the lowest-{@code sequenceOrder}) active program at a
     * time — see class javadoc.
     */
    public void advanceProgress(Client client, Booking completedBooking) {
        ClientLoyaltyState state = stateRepository.getOrCreate(client.getId());
        if (state.getCurrentProgramId() == null) {
            Optional<LoyaltyProgram> first = firstUnearned(state);
            if (first.isEmpty()) {
                return; // no active programs configured — nothing to progress
            }
            startProgram(state, first.get(), client);
        }

        LoyaltyProgram program = programRepository.findById(state.getCurrentProgramId()).orElse(null);
        if (program == null || program.getStatus() != ProgramStatus.ACTIVE) {
            return; // program was paused/deleted since the client started it
        }
        double currentValue = currentTriggerValue(client, program);
        double progress = currentValue - state.getBaselineValue();
        if (progress < program.getTriggerValue()) {
            stateRepository.save(state);
            return;
        }

        Instant now = Instant.now();
        LoyaltyReward reward = new LoyaltyReward();
        reward.setClientId(client.getId());
        reward.setProgramId(program.getId());
        reward.setStatus(RewardStatus.PENDING);
        reward.setEarnedAt(now);
        if (program.getRewardExpiryDays() != null) {
            reward.setExpiresAt(now.plus(program.getRewardExpiryDays(), ChronoUnit.DAYS));
        }
        rewardRepository.insert(reward);

        program.setTotalEarned(program.getTotalEarned() + 1);
        programRepository.save(program);

        ClientLoyaltyState.HistoryEntry entry = new ClientLoyaltyState.HistoryEntry();
        entry.setProgramId(program.getId());
        entry.setEarnedAt(now);
        state.getHistory().add(entry);

        Optional<LoyaltyProgram> next = programRepository.nextActiveAfter(program.getSequenceOrder());
        if (next.isPresent()) {
            startProgram(state, next.get(), client);
        } else {
            state.setCurrentProgramId(null);
            state.setBaselineValue(0);
            stateRepository.save(state);
        }
        log.info("[loyalty] client {} earned reward under program {}", client.getId(), program.getId());
    }

    private Optional<LoyaltyProgram> firstUnearned(ClientLoyaltyState state) {
        Set<String> earned = state.getHistory().stream().map(ClientLoyaltyState.HistoryEntry::getProgramId).collect(java.util.stream.Collectors.toSet());
        return programRepository.listInSequenceOrder(ProgramStatus.ACTIVE).stream()
                .filter(p -> !earned.contains(p.getId()))
                .findFirst();
    }

    private void startProgram(ClientLoyaltyState state, LoyaltyProgram program, Client client) {
        state.setCurrentProgramId(program.getId());
        state.setBaselineValue(currentTriggerValue(client, program));
        stateRepository.save(state);
    }

    private double currentTriggerValue(Client client, LoyaltyProgram program) {
        if (program == null) {
            return 0;
        }
        return program.getTriggerType() == TriggerType.SPEND ? client.getLifetimeSpendCents() : client.getTotalBookings();
    }

    private LoyaltyReward lazilyExpire(LoyaltyReward reward) {
        if (reward.getStatus() == RewardStatus.PENDING && reward.getExpiresAt() != null && reward.getExpiresAt().isBefore(Instant.now())) {
            reward.setStatus(RewardStatus.EXPIRED);
            rewardRepository.save(reward);
        }
        return reward;
    }

    private void requireManageAccess() {
        var ctx = TenantContextHolder.require();
        if (!ctx.hasPermission(Permission.LOYALTY_MANAGE) && !ctx.isOwner()) {
            throw new ForbiddenException("You do not have permission to manage loyalty");
        }
    }

    private void applyWrite(LoyaltyProgram program, LoyaltyProgramWriteRequest request, boolean creating) {
        if (request.name() != null) {
            program.setName(request.name());
        }
        if (request.description() != null) {
            program.setDescription(request.description());
        }
        if (request.rewardType() != null) {
            program.setRewardType(request.rewardType());
        }
        if (request.rewardValue() != null) {
            program.setRewardValue(request.rewardValue());
        }
        if (request.rewardLabel() != null) {
            program.setRewardLabel(request.rewardLabel());
        }
        if (request.redeemLocation() != null) {
            program.setRedeemLocation(request.redeemLocation());
        }
        if (request.partnerName() != null) {
            program.setPartnerName(request.partnerName());
        }
        if (request.partnerAddress() != null) {
            program.setPartnerAddress(request.partnerAddress());
        }
        if (request.triggerType() != null) {
            program.setTriggerType(request.triggerType());
        }
        if (request.triggerValue() != null) {
            program.setTriggerValue(request.triggerValue());
        }
        if (request.programExpiryDate() != null) {
            program.setProgramExpiryDate(request.programExpiryDate());
        }
        if (request.rewardExpiryDays() != null) {
            program.setRewardExpiryDays(request.rewardExpiryDays());
        }
        if (request.status() != null) {
            program.setStatus(request.status());
        } else if (creating) {
            program.setStatus(ProgramStatus.DRAFT);
        }
    }
}
