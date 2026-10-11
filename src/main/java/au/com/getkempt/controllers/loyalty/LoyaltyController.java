package au.com.getkempt.controllers.loyalty;

import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.enums.loyalty.ProgramStatus;
import au.com.getkempt.request.loyalty.LoyaltyProgramWriteRequest;
import au.com.getkempt.request.loyalty.ProgramReorderRequest;
import au.com.getkempt.request.loyalty.RewardRedeemRequest;
import au.com.getkempt.request.loyalty.RewardRevokeRequest;
import au.com.getkempt.responses.client.ClientLoyaltyProgressResponse;
import au.com.getkempt.responses.loyalty.LoyaltyProgramResponse;
import au.com.getkempt.responses.loyalty.LoyaltyRewardResponse;
import au.com.getkempt.responses.loyalty.LoyaltyStatsResponse;
import au.com.getkempt.services.loyalty.LoyaltyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** {@code /api/v1/loyalty} — see {@code paths/loyalty.yaml}. */
@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/programs")
    public List<LoyaltyProgramResponse> listLoyaltyPrograms(@RequestParam(required = false) ProgramStatus status) {
        return loyaltyService.listPrograms(status);
    }

    @PostMapping("/programs")
    @ResponseStatus(HttpStatus.CREATED)
    public LoyaltyProgramResponse createLoyaltyProgram(@Valid @RequestBody LoyaltyProgramWriteRequest request) {
        return loyaltyService.createProgram(request);
    }

    @PostMapping("/programs/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderLoyaltyPrograms(@Valid @RequestBody ProgramReorderRequest request) {
        loyaltyService.reorderPrograms(request);
    }

    @GetMapping("/programs/{programId}")
    public LoyaltyProgramResponse getLoyaltyProgram(@PathVariable String programId) {
        return loyaltyService.getProgram(programId);
    }

    @PatchMapping("/programs/{programId}")
    public LoyaltyProgramResponse updateLoyaltyProgram(
            @PathVariable String programId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody LoyaltyProgramWriteRequest request) {
        return loyaltyService.updateProgram(programId, ifMatch, request);
    }

    @DeleteMapping("/programs/{programId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLoyaltyProgram(@PathVariable String programId) {
        loyaltyService.deleteProgram(programId);
    }

    @PostMapping("/programs/{programId}/activate")
    public LoyaltyProgramResponse activateLoyaltyProgram(@PathVariable String programId) {
        return loyaltyService.activateProgram(programId);
    }

    @PostMapping("/programs/{programId}/pause")
    public LoyaltyProgramResponse pauseLoyaltyProgram(@PathVariable String programId) {
        return loyaltyService.pauseProgram(programId);
    }

    @PostMapping("/programs/{programId}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public LoyaltyProgramResponse duplicateLoyaltyProgram(@PathVariable String programId) {
        return loyaltyService.duplicateProgram(programId);
    }

    @GetMapping("/clients/{clientId}/progress")
    public ClientLoyaltyProgressResponse getLoyaltyClientProgress(@PathVariable String clientId) {
        return loyaltyService.getClientProgress(clientId);
    }

    @GetMapping("/rewards")
    public CursorPage<LoyaltyRewardResponse> listLoyaltyRewards(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String programId,
            @RequestParam(required = false) String status) {
        return loyaltyService.listRewards(cursor, limit, clientId, programId, status);
    }

    @PostMapping("/rewards/{rewardId}/redeem")
    public LoyaltyRewardResponse redeemLoyaltyReward(
            @PathVariable String rewardId,
            @RequestBody(required = false) RewardRedeemRequest request) {
        return loyaltyService.redeemReward(rewardId, request);
    }

    @PostMapping("/rewards/{rewardId}/revoke")
    public LoyaltyRewardResponse revokeLoyaltyReward(
            @PathVariable String rewardId,
            @RequestBody(required = false) RewardRevokeRequest request) {
        return loyaltyService.revokeReward(rewardId, request);
    }

    @GetMapping("/stats")
    public LoyaltyStatsResponse getLoyaltyStats() {
        return loyaltyService.getStats();
    }
}
