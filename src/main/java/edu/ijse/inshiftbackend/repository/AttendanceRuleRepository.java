package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.AttendanceRule;
import edu.ijse.inshiftbackend.entity.enums.AttendanceRuleKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceRuleRepository extends JpaRepository<AttendanceRule, Long> {
    Optional<AttendanceRule> findByRuleKey(AttendanceRuleKey ruleKey);
}