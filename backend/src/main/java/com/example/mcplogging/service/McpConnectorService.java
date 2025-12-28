package com.example.mcplogging.service;

import com.example.mcplogging.dto.McpConnectorDto;
import com.example.mcplogging.entity.McpConnector;
import com.example.mcplogging.enums.McpServerType;
import com.example.mcplogging.repository.McpConnectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class McpConnectorService {

    private final McpConnectorRepository connectorRepository;

    /**
     * 활성화된 모든 커넥터 목록 조회 (커넥터 둘러보기)
     */
    public List<McpConnectorDto> getAvailableConnectors() {
        return connectorRepository.findByEnabledTrue()
                .stream()
                .map(McpConnectorDto::from)
                .toList();
    }

    /**
     * 타입별 커넥터 목록 조회
     */
    public List<McpConnectorDto> getConnectorsByType(McpServerType type) {
        return connectorRepository.findByType(type)
                .stream()
                .map(McpConnectorDto::from)
                .toList();
    }

    /**
     * 커넥터 상세 조회
     */
    public McpConnectorDto getConnector(Long id) {
        McpConnector connector = connectorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Connector not found: " + id));
        return McpConnectorDto.from(connector);
    }

    /**
     * 커넥터 이름으로 조회
     */
    public McpConnectorDto getConnectorByName(String name) {
        McpConnector connector = connectorRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Connector not found: " + name));
        return McpConnectorDto.from(connector);
    }

    /**
     * 모든 커넥터 조회 (Entity)
     */
    public List<McpConnector> getAllConnectors() {
        return connectorRepository.findAll();
    }
}
