package com.example.mcplogging.repository;

import com.example.mcplogging.entity.McpConnector;
import com.example.mcplogging.enums.McpServerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface McpConnectorRepository extends JpaRepository<McpConnector, Long> {
    List<McpConnector> findByEnabledTrue();

    List<McpConnector> findByType(McpServerType type);

    Optional<McpConnector> findByName(String name);
}
