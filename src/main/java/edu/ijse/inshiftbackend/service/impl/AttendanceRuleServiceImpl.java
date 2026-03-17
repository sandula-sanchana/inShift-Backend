package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.AttendanceRuleDTO;
import edu.ijse.inshiftbackend.dto.AttendanceRuleUpdateDTO;
import edu.ijse.inshiftbackend.entity.AttendanceRule;
import edu.ijse.inshiftbackend.entity.enums.AttendanceRuleKey;
import edu.ijse.inshiftbackend.entity.enums.RiskSeverity;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.AttendanceRuleRepository;
import edu.ijse.inshiftbackend.service.AttendanceRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceRuleServiceImpl implements AttendanceRuleService {

    private final AttendanceRuleRepository attendanceRuleRepository;

    @Override
    public AttendanceRule getRequiredRule(AttendanceRuleKey ruleKey) {
        return attendanceRuleRepository.findByRuleKey(ruleKey)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance rule not found: " + ruleKey));
    }

    @Override
    public List<AttendanceRuleDTO> getAllRules() {
        return attendanceRuleRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional
    public AttendanceRuleDTO updateRule(Long id, AttendanceRuleUpdateDTO dto) {
        AttendanceRule rule = attendanceRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance rule not found"));

        rule.setEnabled(dto.getEnabled());

        if (dto.getThresholdValue() != null) {
            rule.setThresholdValue(dto.getThresholdValue());
        }

        if (dto.getScoreImpact() != null) {
            rule.setScoreImpact(dto.getScoreImpact());
        }

        if (dto.getRuleName() != null && !dto.getRuleName().isBlank()) {
            rule.setRuleName(dto.getRuleName());
        }

        if (dto.getDescription() != null) {
            rule.setDescription(dto.getDescription());
        }

        if (dto.getSeverity() != null && !dto.getSeverity().isBlank()) {
            rule.setSeverity(RiskSeverity.valueOf(dto.getSeverity().trim().toUpperCase()));
        }

        rule.setUpdatedAt(LocalDateTime.now());

        return mapToDTO(attendanceRuleRepository.save(rule));
    }

    private AttendanceRuleDTO mapToDTO(AttendanceRule rule) {
        return AttendanceRuleDTO.builder()
                .id(rule.getId())
                .ruleKey(rule.getRuleKey().name())
                .ruleName(rule.getRuleName())
                .description(rule.getDescription())
                .enabled(rule.getEnabled())
                .thresholdValue(rule.getThresholdValue())
                .scoreImpact(rule.getScoreImpact())
                .severity(rule.getSeverity() != null ? rule.getSeverity().name() : null)
                .build();
    }
}