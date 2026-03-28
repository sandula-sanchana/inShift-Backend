package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.entity.PresenceCheck;
import edu.ijse.inshiftbackend.entity.PresenceCheckPlan;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckPlanStatus;
import edu.ijse.inshiftbackend.repository.PresenceCheckPlanRepository;
import edu.ijse.inshiftbackend.service.PresenceCheckPlanProcessor;
import edu.ijse.inshiftbackend.service.PresenceCheckTriggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PresenceCheckPlanProcessorImpl implements PresenceCheckPlanProcessor {

    private final PresenceCheckPlanRepository presenceCheckPlanRepository;
    private final PresenceCheckTriggerService presenceCheckTriggerService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSinglePlan(Long planId) {
        System.out.println("[PresencePlanProcessor] Processing plan id " + planId);

        PresenceCheckPlan plan = presenceCheckPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalStateException("Presence check plan not found: " + planId));

        System.out.println("[PresencePlanProcessor] Current plan status: " + plan.getStatus());

        if (plan.getStatus() != PresenceCheckPlanStatus.PLANNED) {
            System.out.println("[PresencePlanProcessor] Plan is not PLANNED, skipping");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        plan.setStatus(PresenceCheckPlanStatus.EXECUTING);
        presenceCheckPlanRepository.save(plan);
        System.out.println("[PresencePlanProcessor] Plan moved to EXECUTING");

        try {
            PresenceCheck check = presenceCheckTriggerService.triggerFromPlan(plan);

            if (check == null) {
                plan.setStatus(PresenceCheckPlanStatus.SKIPPED);
                plan.setTriggeredAt(now);
                presenceCheckPlanRepository.save(plan);
                System.out.println("[PresencePlanProcessor] Plan skipped because trigger returned null");
                return;
            }

            plan.setStatus(PresenceCheckPlanStatus.TRIGGERED);
            plan.setTriggeredAt(now);
            presenceCheckPlanRepository.save(plan);
            System.out.println("[PresencePlanProcessor] Plan triggered successfully. Check id = " + check.getId());

        } catch (Exception e) {
            plan.setStatus(PresenceCheckPlanStatus.SKIPPED);
            plan.setTriggeredAt(now);
            presenceCheckPlanRepository.save(plan);

            System.err.println("[PresencePlanProcessor] Plan id " + plan.getId()
                    + " skipped after execution failure: " + e.getMessage());
            e.printStackTrace();
        }
    }
}