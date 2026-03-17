package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.entity.AttendanceFlag;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceIntelligenceService {

    void evaluateDay(Long employeeId, LocalDate attendanceDate);

    List<AttendanceFlag> getFlagsForEmployeeDay(Long employeeId, LocalDate attendanceDate);
}