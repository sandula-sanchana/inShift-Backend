package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.entity.PresenceCheckPlan;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckPlanStatus;
import edu.ijse.inshiftbackend.repository.PresenceCheckPlanRepository;
import edu.ijse.inshiftbackend.service.PresenceCheckPlanExecutionService;
import edu.ijse.inshiftbackend.service.PresenceCheckPlanProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PresenceCheckPlanExecutionServiceImpl implements PresenceCheckPlanExecutionService {

    private final PresenceCheckPlanRepository presenceCheckPlanRepository;
    private final PresenceCheckPlanProcessor presenceCheckPlanProcessor;

    @Override
    @Scheduled(fixedDelay = 15000)
    public void executeDuePlans() {
        System.out.println("[PresencePlanExec] Scheduler tick at " + LocalDateTime.now());

        List<PresenceCheckPlan> duePlans =
                presenceCheckPlanRepository.findByStatusAndPlannedAtBeforeOrderByPlannedAtAsc(
                        PresenceCheckPlanStatus.PLANNED,
                        LocalDateTime.now()
                );

        System.out.println("[PresencePlanExec] Due plans found: " + duePlans.size());

        if (duePlans.isEmpty()) {
            return;
        }

        for (PresenceCheckPlan duePlan : duePlans) {
            try {
                System.out.println("[PresencePlanExec] Sending plan to processor: " + duePlan.getId());
                presenceCheckPlanProcessor.processSinglePlan(duePlan.getId());
            } catch (Exception e) {
                System.err.println("[PresencePlanExec] Failed to process plan id "
                        + duePlan.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}