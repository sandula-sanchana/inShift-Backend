package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.entity.PresenceCheck;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckRiskLevel;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckStatus;
import edu.ijse.inshiftbackend.repository.PresenceCheckRepository;
import edu.ijse.inshiftbackend.service.PresenceCheckSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PresenceCheckSchedulerServiceImpl implements PresenceCheckSchedulerService {

    private final PresenceCheckRepository presenceCheckRepository;

    @Override
    @Transactional
    @Scheduled(fixedDelay = 10000)
    public void processMissedPresenceChecks() {
        LocalDateTime now = LocalDateTime.now();

        List<PresenceCheck> overdueChecks =
                presenceCheckRepository.findByStatusAndDueAtIsNotNullAndDueAtBefore(
                        PresenceCheckStatus.PENDING,
                        now
                );

        if (overdueChecks.isEmpty()) {
            return;
        }

        List<PresenceCheck> toUpdate = new ArrayList<>();

        for (PresenceCheck check : overdueChecks) {
            if (check.getStatus() != PresenceCheckStatus.PENDING) {
                continue;
            }

            check.setStatus(PresenceCheckStatus.MISSED);
            check.setMissedResponse(true);
            check.setLateResponse(true);

            boolean shouldEscalate =
                    check.getRiskLevel() == PresenceCheckRiskLevel.HIGH
                            || Boolean.TRUE.equals(check.getEscalated());

            check.setEscalated(shouldEscalate);
            check.setEscalatedAt(shouldEscalate ? now : null);
            check.setEscalationLevel(shouldEscalate ? 1 : 0);

            toUpdate.add(check);
        }

        if (!toUpdate.isEmpty()) {
            presenceCheckRepository.saveAll(toUpdate);
        }
    }
}