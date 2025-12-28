package com.example.mcplogging.repository;

import com.example.mcplogging.entity.McpConnection;
import com.example.mcplogging.entity.McpStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface McpConnectionRepository extends JpaRepository<McpConnection, Long> {
    List<McpConnection> findByUserId(String userId);

    List<McpConnection> findByUserIdAndStatus(String userId, McpStatus status);

    Optional<McpConnection> findByConnectorIdAndUserId(Long connectorId, String userId);

    Optional<McpConnection> findByConnectorNameAndUserId(String connectorName, String userId);
}
