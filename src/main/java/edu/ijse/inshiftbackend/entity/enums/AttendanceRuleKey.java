package edu.ijse.inshiftbackend.entity.enums;

public enum AttendanceRuleKey {

    // work duration
    SHORT_WORK_DURATION_MINUTES,

    // correction behavior
    TOO_MANY_CORRECTIONS_LIMIT,
    TOO_MANY_CORRECTIONS_WINDOW_DAYS,

    // manual/web dependency behaviour
    WEB_ATTENDANCE_DEPENDENCY_LIMIT,
    WEB_ATTENDANCE_DEPENDENCY_WINDOW_DAYS,

    // OT abuse intelligence
    INVALID_OT_ELIGIBILITY_SCORE,

    // trust scoring
    REVIEW_TRUST_THRESHOLD,
    HIGH_RISK_TRUST_THRESHOLD
}