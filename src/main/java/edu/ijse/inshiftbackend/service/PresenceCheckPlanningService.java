package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.entity.PresenceCheckPlan;

import java.time.LocalDate;
import java.util.List;

public interface PresenceCheckPlanningService {

    List<PresenceCheckPlan> generateDailyPlansForEmployee(Long employeeId, LocalDate attendanceDate);

    void generateDailyPlansForAllEligibleEmployees(LocalDate attendanceDate);
}