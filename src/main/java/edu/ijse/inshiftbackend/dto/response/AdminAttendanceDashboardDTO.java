package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAttendanceDashboardDTO {
    private long presentToday;
    private long absentToday;
    private long lateToday;
    private long pendingApprovals;
    private long incompleteToday;
    private long overtimeToday;
}