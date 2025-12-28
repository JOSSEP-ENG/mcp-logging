package com.example.mcplogging.controller;

import com.example.mcplogging.dto.ApiResponse;
import com.example.mcplogging.dto.ConnectionRequest;
import com.example.mcplogging.dto.McpConnectionDto;
import com.example.mcplogging.dto.ToolCallRequest;
import com.example.mcplogging.entity.McpConnection;
import com.example.mcplogging.mcp.service.McpConnectionService;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP 연결 관리 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/connections")
@RequiredArgsConstructor
public class McpConnectionController {

    private final McpConnectionService connectionService;

    /**
     * MCP 서버 연결
     */
    @PostMapping
    public Mono<ApiResponse<McpConnectionDto>> connect(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody ConnectionRequest request
    ) {
        log.info("MCP 연결 요청: userId={}, connectorId={}", userId, request.getConnectorId());

        return connectionService.connect(request.getConnectorId(), userId, request.getEnv())
                .map(connection -> ApiResponse.ok(McpConnectionDto.from(connection)))
                .onErrorResume(error -> {
                    log.error("MCP 연결 실패", error);
                    return Mono.just(ApiResponse.<McpConnectionDto>error("연결 실패: " + error.getMessage()));
                });
    }

    /**
     * MCP 서버 연결 해제
     */
    @DeleteMapping("/{connectionId}")
    public Mono<ApiResponse<Void>> disconnect(
            @PathVariable Long connectionId
    ) {
        log.info("MCP 연결 해제 요청: connectionId={}", connectionId);

        return connectionService.disconnect(connectionId)
                .then(Mono.just(ApiResponse.<Void>ok(null)))
                .onErrorResume(error -> {
                    log.error("MCP 연결 해제 실패", error);
                    return Mono.just(ApiResponse.<Void>error("연결 해제 실패: " + error.getMessage()));
                });
    }

    /**
     * 연결 상태 조회
     */
    @GetMapping("/{connectionId}")
    public ApiResponse<McpConnectionDto> getConnection(
            @PathVariable Long connectionId
    ) {
        try {
            McpConnection connection = connectionService.getConnectionStatus(connectionId);
            return ApiResponse.ok(McpConnectionDto.from(connection));
        } catch (Exception e) {
            log.error("연결 조회 실패", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 사용자의 모든 연결 조회
     */
    @GetMapping
    public ApiResponse<List<McpConnectionDto>> getUserConnections(
            @RequestHeader("X-User-Id") String userId
    ) {
        try {
            List<McpConnectionDto> connections = connectionService.getUserConnections(userId)
                    .stream()
                    .map(McpConnectionDto::from)
                    .collect(Collectors.toList());
            return ApiResponse.ok(connections);
        } catch (Exception e) {
            log.error("사용자 연결 목록 조회 실패", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 도구 목록 조회
     */
    @GetMapping("/{connectionId}/tools")
    public Mono<ApiResponse<McpSchema.ListToolsResult>> listTools(
            @PathVariable Long connectionId
    ) {
        log.info("도구 목록 조회: connectionId={}", connectionId);

        return connectionService.listTools(connectionId)
                .map(ApiResponse::ok)
                .onErrorResume(error -> {
                    log.error("도구 목록 조회 실패", error);
                    return Mono.just(ApiResponse.<McpSchema.ListToolsResult>error("도구 목록 조회 실패: " + error.getMessage()));
                });
    }

    /**
     * 도구 실행
     */
    @PostMapping("/{connectionId}/tools/call")
    public Mono<ApiResponse<McpSchema.CallToolResult>> callTool(
            @PathVariable Long connectionId,
            @RequestBody ToolCallRequest request
    ) {
        log.info("도구 실행: connectionId={}, tool={}", connectionId, request.getToolName());

        return connectionService.callTool(connectionId, request.getToolName(), request.getArguments())
                .map(ApiResponse::ok)
                .onErrorResume(error -> {
                    log.error("도구 실행 실패", error);
                    return Mono.just(ApiResponse.<McpSchema.CallToolResult>error("도구 실행 실패: " + error.getMessage()));
                });
    }
}
