package com.cancerrisk.service;

import com.cancerrisk.domain.AuditLog;
import com.cancerrisk.repository.AuditLogRepository;
import com.cancerrisk.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String entityType, Long entityId, String details) {
        record(SecurityUtils.currentUsername(), action, entityType, entityId, details);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String actor, String action, String entityType, Long entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setActor(actor);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);
        repository.save(entry);
    }
}
