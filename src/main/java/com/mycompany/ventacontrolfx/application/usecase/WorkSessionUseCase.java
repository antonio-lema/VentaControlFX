package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.WorkSession;
import com.mycompany.ventacontrolfx.domain.repository.IWorkSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class WorkSessionUseCase {
    private final IWorkSessionRepository repository;
    private com.mycompany.ventacontrolfx.infrastructure.persistence.BackupService backupService;

    public WorkSessionUseCase(IWorkSessionRepository repository) {
        this.repository = repository;
    }

    public void setBackupService(com.mycompany.ventacontrolfx.infrastructure.persistence.BackupService backupService) {
        this.backupService = backupService;
    }

    public void startShift(Integer userId) {
        if (repository.getActiveSession(userId).isPresent()) {
            throw new IllegalStateException("User already has an active session");
        }
        WorkSession session = new WorkSession(userId, WorkSession.SessionType.SHIFT);
        repository.save(session);
    }

    public void endShift(Integer userId) {
        Optional<WorkSession> activeSession = repository.getActiveSession(userId);
        if (activeSession.isPresent() && activeSession.get().getType() == WorkSession.SessionType.SHIFT) {
            WorkSession session = activeSession.get();
            session.setEndTime(LocalDateTime.now());
            session.setStatus(WorkSession.SessionStatus.COMPLETED);
            repository.update(session);

            // AUTO BACKUP ON END SHIFT
            if (backupService != null) {
                new Thread(() -> {
                    backupService.createDefaultBackup();
                }).start();
            }
        } else {
            throw new IllegalStateException("No active shift found to end");
        }
    }

    public void startBreak(Integer userId) {
        Optional<WorkSession> activeSession = repository.getActiveSession(userId);
        if (activeSession.isPresent()) {
            if (activeSession.get().getType() == WorkSession.SessionType.BREAK) {
                throw new IllegalStateException("User is already on a break");
            }
            // End the current shift before starting a break?
            // Usually, breaks happen WITHIN a shift.
            // Let's allow starting a break if there's an active shift or even if there
            // isn't?
            // Usually, we track them separately.
            // For simplicity, let's treat them as exclusive sessions for now or nested.
            // If we want nested, the DB schema needs to support it.
            // If they are exclusive, the user "ends" the shift and "starts" a break.
            // But usually you want both.
            // For now, let's follow the simple requirement: track shifts OR breaks.

            // Actually, if we want "good management", we should allow multiple active
            // sessions if they are different types?
            // No, usually one person is either working or resting.

            // Let's end the previous session automatically?
            endSession(userId);
        }
        WorkSession session = new WorkSession(userId, WorkSession.SessionType.BREAK);
        repository.save(session);
    }

    public void endBreak(Integer userId) {
        Optional<WorkSession> activeSession = repository.getActiveSession(userId);
        if (activeSession.isPresent() && activeSession.get().getType() == WorkSession.SessionType.BREAK) {
            endSession(userId);
        } else {
            throw new IllegalStateException("No active break found to end");
        }
    }

    public void endSession(Integer userId) {
        repository.getActiveSession(userId).ifPresent(session -> {
            session.setEndTime(LocalDateTime.now());
            session.setStatus(WorkSession.SessionStatus.COMPLETED);
            repository.update(session);
        });
    }

    public Optional<WorkSession> getActiveSession(Integer userId) {
        return repository.getActiveSession(userId);
    }

    public List<WorkSession> getHistory(Integer userId) {
        return repository.getHistory(userId);
    }

    public List<WorkSession> getAllActiveSessions() {
        return repository.getAllActiveSessions();
    }

    public List<WorkSession> getHistoryByDate(java.time.LocalDate date) {
        return repository.getHistoryByDate(date);
    }
}

