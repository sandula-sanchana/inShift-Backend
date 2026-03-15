package edu.ijse.inshiftbackend.config;

import edu.ijse.inshiftbackend.entity.Shift;
import edu.ijse.inshiftbackend.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class BootstrapDefaultShiftInitializer implements CommandLineRunner {

    private final ShiftRepository shiftRepository;

    @Value("${inshift.bootstrap.shift.enabled:false}")
    private boolean enabled;

    @Value("${inshift.bootstrap.shift.name:Default Office Shift}")
    private String shiftName;

    @Value("${inshift.bootstrap.shift.start:08:00}")
    private String start;

    @Value("${inshift.bootstrap.shift.end:17:00}")
    private String end;

    @Value("${inshift.bootstrap.shift.graceMinutes:15}")
    private Integer graceMinutes;

    @Value("${inshift.bootstrap.shift.earlyCheckInMinutes:30}")
    private Integer earlyCheckInMinutes;

    @Value("${inshift.bootstrap.shift.earlyLeaveGraceMinutes:10}")
    private Integer earlyLeaveGraceMinutes;

    @Value("${inshift.bootstrap.shift.overtimeAfterMinutes:30}")
    private Integer overtimeAfterMinutes;

    @Value("${inshift.bootstrap.shift.breakMinutes:60}")
    private Integer breakMinutes;

    @Override
    public void run(String... args) {

        if (!enabled) return;

        boolean exists = shiftRepository
                .findByIsDefaultTrueAndActiveTrue()
                .isPresent();

        if (exists) return;

        Shift defaultShift = Shift.builder()
                .shiftName(shiftName)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .graceMinutes(graceMinutes)
                .earlyCheckInMinutes(earlyCheckInMinutes)
                .earlyLeaveGraceMinutes(earlyLeaveGraceMinutes)
                .overtimeAfterMinutes(overtimeAfterMinutes)
                .breakMinutes(breakMinutes)
                .active(true)
                .isDefault(true)
                .build();

        shiftRepository.save(defaultShift);

        System.out.println("✅ Bootstrap DEFAULT SHIFT created: " + shiftName +
                " (" + start + " - " + end + ")");
    }
}