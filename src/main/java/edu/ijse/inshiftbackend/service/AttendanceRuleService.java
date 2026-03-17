package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.AttendanceRuleDTO;
import edu.ijse.inshiftbackend.dto.AttendanceRuleUpdateDTO;
import edu.ijse.inshiftbackend.entity.AttendanceRule;
import edu.ijse.inshiftbackend.entity.enums.AttendanceRuleKey;

import java.util.List;

public interface AttendanceRuleService {

    AttendanceRule getRequiredRule(AttendanceRuleKey ruleKey);

    List<AttendanceRuleDTO> getAllRules();

    AttendanceRuleDTO updateRule(Long id, AttendanceRuleUpdateDTO dto);
}