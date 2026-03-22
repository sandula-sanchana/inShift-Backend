package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.PresenceCheck;
import edu.ijse.inshiftbackend.entity.PresenceCheckPlan;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckTriggerReason;

public interface PresenceCheckTriggerService {

    PresenceCheck triggerPresenceCheck(
            Employee employee,
            PresenceCheckTriggerReason reason,
            String description
    );

    PresenceCheck triggerFromPlan(PresenceCheckPlan plan);
}