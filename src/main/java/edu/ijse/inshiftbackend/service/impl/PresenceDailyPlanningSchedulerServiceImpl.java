package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.service.PresenceCheckPlanningService;
import edu.ijse.inshiftbackend.service.PresenceDailyPlanningSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PresenceDailyPlanningSchedulerServiceImpl implements PresenceDailyPlanningSchedulerService {

    private final PresenceCheckPlanningService presenceCheckPlanningService;

    @Override
    @Scheduled(cron = "0 0 8 * * *")
    public void generateTodayPlans() {
        LocalDate today = LocalDate.now();

        try {
            System.out.println("[PresencePlanDaily] Starting daily plan generation for " + today);
            presenceCheckPlanningService.generateDailyPlansForAllEligibleEmployees(today);
            System.out.println("[PresencePlanDaily] Finished daily plan generation for " + today);
        } catch (Exception e) {
            System.err.println("[PresencePlanDaily] Failed daily plan generation for "
                    + today + ": " + e.getMessage());
        }
    }
}