package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.ShiftDTO;
import edu.ijse.inshiftbackend.entity.Shift;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.repository.ShiftRepository;
import edu.ijse.inshiftbackend.service.ShiftService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ShiftServiceImpl implements ShiftService {

    private final ShiftRepository shiftRepository;

    @Override
    public ShiftDTO getDefaultShift() {
        Shift shift = shiftRepository.findByIsDefaultTrueAndActiveTrue()
                .orElseThrow(() -> new BadRequestException("Default shift not configured"));

        return mapToDTO(shift);
    }

    @Override
    @Transactional
    public ShiftDTO saveOrUpdateDefaultShift(ShiftDTO dto) {

        validate(dto);

        Shift shift = shiftRepository.findByIsDefaultTrueAndActiveTrue()
                .orElse(
                        Shift.builder()
                                .isDefault(true)
                                .active(true)
                                .build()
                );

        shift.setShiftName(dto.getShiftName());
        shift.setStartTime(dto.getStartTime());
        shift.setEndTime(dto.getEndTime());
        shift.setGraceMinutes(dto.getGraceMinutes());
        shift.setEarlyCheckInMinutes(dto.getEarlyCheckInMinutes());
        shift.setEarlyLeaveGraceMinutes(dto.getEarlyLeaveGraceMinutes());
        shift.setOvertimeAfterMinutes(dto.getOvertimeAfterMinutes());
        shift.setBreakMinutes(dto.getBreakMinutes());
        shift.setActive(dto.getActive() != null ? dto.getActive() : true);
        shift.setIsDefault(true);

        Shift saved = shiftRepository.save(shift);

        return mapToDTO(saved);
    }

    private void validate(ShiftDTO dto) {
        if (dto.getShiftName() == null || dto.getShiftName().trim().isEmpty()) {
            throw new BadRequestException("Shift name is required");
        }

        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new BadRequestException("Start time and end time are required");
        }

        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        if (negative(dto.getGraceMinutes())
                || negative(dto.getEarlyCheckInMinutes())
                || negative(dto.getEarlyLeaveGraceMinutes())
                || negative(dto.getOvertimeAfterMinutes())
                || negative(dto.getBreakMinutes())) {
            throw new BadRequestException("Shift minute values cannot be negative");
        }
    }

    private boolean negative(Integer value) {
        return value == null || value < 0;
    }

    private ShiftDTO mapToDTO(Shift shift) {
        return ShiftDTO.builder()
                .shiftId(shift.getShiftId())
                .shiftName(shift.getShiftName())
                .startTime(shift.getStartTime())
                .endTime(shift.getEndTime())
                .graceMinutes(shift.getGraceMinutes())
                .earlyCheckInMinutes(shift.getEarlyCheckInMinutes())
                .earlyLeaveGraceMinutes(shift.getEarlyLeaveGraceMinutes())
                .overtimeAfterMinutes(shift.getOvertimeAfterMinutes())
                .breakMinutes(shift.getBreakMinutes())
                .active(shift.getActive())
                .isDefault(shift.getIsDefault())
                .build();
    }
}