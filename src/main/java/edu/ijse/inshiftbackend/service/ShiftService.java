package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.entity.Shift;

public interface ShiftService {
    Shift getDefaultShift();
    //Shift saveOrUpdateDefaultShift(ShiftDTO dto);
}