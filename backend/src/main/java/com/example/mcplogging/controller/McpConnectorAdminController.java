package com.example.mcplogging.controller;

import com.example.mcplogging.dto.ApiResponse;
import com.example.mcplogging.dto.McpConnectorCreateRequest;
import com.example.mcplogging.dto.McpConnectorDto;
import com.example.mcplogging.service.McpConnectorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/connectors")
@RequiredArgsConstructor
public class McpConnectorAdminController { //TODO: 관리자 권한

    private final McpConnectorService connectorService;

//    /**
//     * 새 커넥터 생성
//     *
//     * @param request 커넥터 생성 정보
//     * @return 생성된 커넥터 정보
//     */
//    @PostMapping
//    public ApiResponse<McpConnectorDto> createConnector(@Valid @RequestBody McpConnectorCreateRequest request) {
//        McpConnectorDto connector = connectorService.createConnector(request);
//        return ApiResponse.ok("created connector.", connector);
//    }
}
