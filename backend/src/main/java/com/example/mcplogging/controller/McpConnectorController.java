package com.example.mcplogging.controller;

import com.example.mcplogging.dto.ApiResponse;
import com.example.mcplogging.dto.McpConnectorDto;
import com.example.mcplogging.enums.McpServerType;
import com.example.mcplogging.service.McpConnectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/connectors")
@RequiredArgsConstructor
public class McpConnectorController {

    private final McpConnectorService connectorService;

    @GetMapping
    public ApiResponse<List<McpConnectorDto>> getAvailableConnectors() {
        List<McpConnectorDto> connectors = connectorService.getAvailableConnectors();
        return ApiResponse.ok(connectors);
    }

    /**
     * 타입별 커넥터 조회
     *
     * @param type 커넥터 타입 (official, custom)
     * @return 해당 타입의 커넥터 목록
     */
    @GetMapping("/type/{type}")
    public ApiResponse<List<McpConnectorDto>> getConnectorsByType(@PathVariable McpServerType type) {
        List<McpConnectorDto> connectors = connectorService.getConnectorsByType(type);
        return ApiResponse.ok(connectors);
    }

    /**
     * 커넥터 상세 조회
     *
     * @param id 커넥터 ID
     * @return 커넥터 상세 정보
     */
    @GetMapping("/{id}")
    public ApiResponse<McpConnectorDto> getConnector(@PathVariable Long id) {
        McpConnectorDto connector = connectorService.getConnector(id);
        return ApiResponse.ok(connector);
    }
}
