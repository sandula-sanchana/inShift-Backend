package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.entity.PresenceCheck;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckStatus;
import edu.ijse.inshiftbackend.repository.PresenceCheckRepository;
import edu.ijse.inshiftbackend.service.PresenceCheckSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PresenceCheckSchedulerServiceImpl implements PresenceCheckSchedulerService {

    private final PresenceCheckRepository presenceCheckRepository;

    @Override
    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void processMissedPresenceChecks() {
        List<PresenceCheck> overdueChecks = presenceCheckRepository.findByStatusAndDueAtBefore(
                PresenceCheckStatus.PENDING,
                LocalDateTime.now()
        );

        if (overdueChecks.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        for (PresenceCheck check : overdueChecks) {
            check.setStatus(PresenceCheckStatus.MISSED);
            check.setMissedResponse(true);
            check.setLateResponse(true);
            check.setEscalated(true);
            check.setEscalatedAt(now);
            check.setEscalationLevel(1);
        }

        presenceCheckRepository.saveAll(overdueChecks);
    }
}