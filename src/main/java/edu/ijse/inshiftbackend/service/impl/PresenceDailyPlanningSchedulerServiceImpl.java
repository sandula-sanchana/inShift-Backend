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
//    @Scheduled(cron = "0 0 8 * * *")
    @Scheduled(cron = "0 * * * * *")
    public void generateTodayPlans() {
        LocalDate today = LocalDate.now();

        try {
            System.out.println("Starting daily presence-check plan generation for " + today);
            presenceCheckPlanningService.generateDailyPlansForAllEligibleEmployees(today);
            System.out.println("Finished daily presence-check plan generation for " + today);
        } catch (Exception e) {
            System.err.println("Failed daily presence-check plan generation for " + today + ": " + e.getMessage());
        }
    }
}