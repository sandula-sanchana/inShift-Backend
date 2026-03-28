package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.entity.PresenceCheck;
import edu.ijse.inshiftbackend.entity.PresenceCheckPlan;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckPlanStatus;
import edu.ijse.inshiftbackend.repository.PresenceCheckPlanRepository;
import edu.ijse.inshiftbackend.service.PresenceCheckPlanExecutionService;
import edu.ijse.inshiftbackend.service.PresenceCheckTriggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PresenceCheckPlanExecutionServiceImpl implements PresenceCheckPlanExecutionService {

    private final PresenceCheckPlanRepository presenceCheckPlanRepository;
    private final PresenceCheckTriggerService presenceCheckTriggerService;

    @Override
    @Scheduled(fixedDelay = 15000)
    public void executeDuePlans() {
        List<PresenceCheckPlan> duePlans =
                presenceCheckPlanRepository.findByStatusAndPlannedAtBeforeOrderByPlannedAtAsc(
                        PresenceCheckPlanStatus.PLANNED,
                        LocalDateTime.now()
                );

        if (duePlans.isEmpty()) {
            return;
        }

        for (PresenceCheckPlan duePlan : duePlans) {
            try {
                processSinglePlan(duePlan.getId());
            } catch (Exception e) {
                System.err.println("[PresencePlanExec] Failed to process plan id "
                        + duePlan.getId() + ": " + e.getMessage());
            }
        }
    }
    //Each plan is processed in its own transaction //one failed plan does not roll back the whole scheduler batch
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSinglePlan(Long planId) {
        PresenceCheckPlan plan = presenceCheckPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalStateException("Presence check plan not found: " + planId));

        if (plan.getStatus() != PresenceCheckPlanStatus.PLANNED) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        plan.setStatus(PresenceCheckPlanStatus.EXECUTING);
        presenceCheckPlanRepository.save(plan);

        try {
            PresenceCheck check = presenceCheckTriggerService.triggerFromPlan(plan);

            if (check == null) {
                plan.setStatus(PresenceCheckPlanStatus.SKIPPED);
                plan.setTriggeredAt(now);
                presenceCheckPlanRepository.save(plan);
                return;
            }

            plan.setStatus(PresenceCheckPlanStatus.TRIGGERED);
            plan.setTriggeredAt(now);
            presenceCheckPlanRepository.save(plan);

        } catch (Exception e) {
            plan.setStatus(PresenceCheckPlanStatus.SKIPPED);
            plan.setTriggeredAt(now);
            presenceCheckPlanRepository.save(plan);

            System.err.println("[PresencePlanExec] Plan id " + plan.getId()
                    + " skipped after execution failure: " + e.getMessage());
        }
    }
}