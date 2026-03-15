package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.ShiftDTO;
import edu.ijse.inshiftbackend.entity.Shift;

public interface ShiftService {
    ShiftDTO getDefaultShift();
    ShiftDTO saveOrUpdateDefaultShift(ShiftDTO dto);
}