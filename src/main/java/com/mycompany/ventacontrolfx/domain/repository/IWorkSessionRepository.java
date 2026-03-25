package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.WorkSession;
import java.util.List;
import java.util.Optional;

public interface IWorkSessionRepository {
    void save(WorkSession session);

    void update(WorkSession session);

    Optional<WorkSession> getActiveSession(Integer userId);

    List<WorkSession> getHistory(Integer userId);
}
