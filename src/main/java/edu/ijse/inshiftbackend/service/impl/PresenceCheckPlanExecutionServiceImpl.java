package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.entity.PresenceCheckPlan;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckPlanStatus;
import edu.ijse.inshiftbackend.repository.PresenceCheckPlanRepository;
import edu.ijse.inshiftbackend.service.PresenceCheckPlanExecutionService;
import edu.ijse.inshiftbackend.service.PresenceCheckTriggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PresenceCheckPlanExecutionServiceImpl implements PresenceCheckPlanExecutionService {

    private final PresenceCheckPlanRepository presenceCheckPlanRepository;
    private final PresenceCheckTriggerService presenceCheckTriggerService;

    @Override
    @Transactional
    @Scheduled(fixedDelay = 60000)
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
                presenceCheckTriggerService.triggerFromPlan(duePlan);

                duePlan.setStatus(PresenceCheckPlanStatus.TRIGGERED);
                duePlan.setTriggeredAt(LocalDateTime.now());
                presenceCheckPlanRepository.save(duePlan);

            } catch (Exception e) {
                duePlan.setStatus(PresenceCheckPlanStatus.SKIPPED);
                duePlan.setTriggeredAt(LocalDateTime.now());
                presenceCheckPlanRepository.save(duePlan);

                System.err.println("Failed to execute presence-check plan id "
                        + duePlan.getId() + ": " + e.getMessage());
            }
        }
    }
}