package com.example.mcplogging.repository;

import com.example.mcplogging.entity.ToolUsageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ToolUsageLogRepository extends JpaRepository<ToolUsageLog, Long> {
    Page<ToolUsageLog> findByToolName(String toolName, Pageable pageable);

    Page<ToolUsageLog> findByExecutedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<ToolUsageLog> findByConnectorName(String connectorName, Pageable pageable);

    Page<ToolUsageLog> findByConnectionId(Long connectionId, Pageable pageable);

    Page<ToolUsageLog> findByChatLogUserId(String userId, Pageable pageable);
}
