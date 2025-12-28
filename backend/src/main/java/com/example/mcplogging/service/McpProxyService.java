package com.example.mcplogging.service;

import com.example.mcplogging.entity.McpConnection;
import com.example.mcplogging.entity.ToolUsageLog;
import com.example.mcplogging.mcp.service.McpConnectionService;
import com.example.mcplogging.repository.ToolUsageLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Proxy Service - Claude Desktop과 실제 MCP 서버 사이의 프록시
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpProxyService {

    private final McpConnectionService connectionService;
    private final ToolUsageLogRepository toolUsageLogRepository;
    private final ObjectMapper objectMapper;

    // 활성 프록시 세션 관리 (sessionId → connection)
    private final Map<String, ProxySession> activeSessions = new ConcurrentHashMap<>();

    /**
     * SSE Proxy 스트림 생성
     */
    public Flux<ServerSentEvent<String>> createSseProxy(
            String connectorName,
            String userId,
            Map<String, String> env
    ) {
        String sessionId = UUID.randomUUID().toString();

        return Mono.fromCallable(() -> {
            // 1. MCP 서버에 연결
            var connector = connectionService.getConnectorByName(connectorName);
            var connection = connectionService.connect(connector.getId(), userId, env).block();

            // 2. 세션 생성
            Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();
            ProxySession session = new ProxySession(sessionId, connection.getId(), userId, sink);
            activeSessions.put(sessionId, session);

            log.info("프록시 세션 생성: sessionId={}, connectionId={}", sessionId, connection.getId());

            // 3. 초기화 이벤트 전송
            sink.tryEmitNext(ServerSentEvent.<String>builder()
                    .event("session")
                    .data("{\"sessionId\":\"" + sessionId + "\",\"status\":\"connected\"}")
                    .build());

            return sink.asFlux();
        }).flatMapMany(flux -> flux)
                .timeout(Duration.ofHours(1))
                .doFinally(signal -> {
                    // 세션 정리
                    ProxySession session = activeSessions.remove(sessionId);
                    if (session != null) {
                        connectionService.disconnect(session.connectionId).subscribe();
                        log.info("프록시 세션 종료: sessionId={}", sessionId);
                    }
                });
    }

    /**
     * JSON-RPC 메시지 처리
     */
    @Transactional
    public Mono<Map<String, Object>> handleJsonRpcMessage(
            String connectorName,
            String userId,
            String sessionId,
            Map<String, Object> message
    ) {
        return Mono.fromCallable(() -> {
            // 1. 요청 로깅
            String method = (String) message.get("method");
            Object params = message.get("params");
            Object id = message.get("id");

            log.info("JSON-RPC 요청: method={}, id={}", method, id);

            // 2. 세션에서 연결 가져오기
            ProxySession session = activeSessions.get(sessionId);
            if (session == null) {
                throw new IllegalStateException("Session not found: " + sessionId);
            }

            // 3. 메서드별 처리
            Map<String, Object> response = switch (method) {
                case "tools/list" -> handleToolsList(session.connectionId, id);
                case "tools/call" -> handleToolsCall(session.connectionId, id, params);
                case "initialize" -> handleInitialize(session.connectionId, id, params);
                default -> {
                    log.warn("Unknown method: {}", method);
                    yield createErrorResponse(id, -32601, "Method not found: " + method);
                }
            };

            // 4. 응답 로깅
            logToolUsage(session.connectionId, method, params, response);

            return response;
        });
    }

    /**
     * tools/list 처리
     */
    private Map<String, Object> handleToolsList(Long connectionId, Object id) {
        try {
            McpSchema.ListToolsResult result = connectionService.listTools(connectionId).block();

            return Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "result", Map.of(
                            "tools", result.tools().stream()
                                    .map(tool -> Map.of(
                                            "name", tool.name(),
                                            "description", tool.description() != null ? tool.description() : "",
                                            "inputSchema", tool.inputSchema() != null ? tool.inputSchema() : Map.of()
                                    ))
                                    .toList()
                    )
            );
        } catch (Exception e) {
            log.error("tools/list 에러", e);
            return createErrorResponse(id, -32603, "Internal error: " + e.getMessage());
        }
    }

    /**
     * tools/call 처리
     */
    private Map<String, Object> handleToolsCall(Long connectionId, Object id, Object params) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> paramsMap = (Map<String, Object>) params;
            String toolName = (String) paramsMap.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) paramsMap.get("arguments");

            McpSchema.CallToolResult result = connectionService.callTool(connectionId, toolName, arguments).block();

            return Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "result", Map.of(
                            "content", result.content(),
                            "isError", result.isError() != null ? result.isError() : false
                    )
            );
        } catch (Exception e) {
            log.error("tools/call 에러", e);
            return createErrorResponse(id, -32603, "Internal error: " + e.getMessage());
        }
    }

    /**
     * initialize 처리
     */
    private Map<String, Object> handleInitialize(Long connectionId, Object id, Object params) {
        return Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "result", Map.of(
                        "protocolVersion", "2024-11-05",
                        "capabilities", Map.of(
                                "tools", Map.of()
                        ),
                        "serverInfo", Map.of(
                                "name", "mcp-logging-proxy",
                                "version", "1.0.0"
                        )
                )
        );
    }

    /**
     * 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse(Object id, int code, String message) {
        return Map.of(
                "jsonrpc", "2.0",
                "id", id != null ? id : null,
                "error", Map.of(
                        "code", code,
                        "message", message
                )
        );
    }

    /**
     * 도구 사용 로깅
     */
    private void logToolUsage(Long connectionId, String method, Object params, Map<String, Object> response) {
        try {
            McpConnection connection = connectionService.getConnectionStatus(connectionId);

            ToolUsageLog log = ToolUsageLog.builder()
                    .connection(connection)
                    .toolName(method)
                    .toolParameters(objectMapper.writeValueAsString(params))
                    .mcpRequestRaw(objectMapper.writeValueAsString(Map.of("method", method, "params", params)))
                    .mcpResponseRaw(objectMapper.writeValueAsString(response))
                    .toolResponse(objectMapper.writeValueAsString(response.get("result")))
                    .executedAt(LocalDateTime.now())
                    .build();

            toolUsageLogRepository.save(log);
        } catch (Exception e) {
            this.log.error("도구 사용 로깅 실패", e);
        }
    }

    /**
     * 활성 프록시 목록 조회
     */
    public Mono<List<Map<String, Object>>> getActiveProxies() {
        return Mono.fromCallable(() ->
                activeSessions.entrySet().stream()
                        .map(entry -> Map.<String, Object>of(
                                "sessionId", entry.getKey(),
                                "userId", entry.getValue().userId,
                                "connectionId", entry.getValue().connectionId
                        ))
                        .toList()
        );
    }

    /**
     * 프록시 세션
     */
    private static class ProxySession {
        final String sessionId;
        final Long connectionId;
        final String userId;
        final Sinks.Many<ServerSentEvent<String>> sink;

        ProxySession(String sessionId, Long connectionId, String userId, Sinks.Many<ServerSentEvent<String>> sink) {
            this.sessionId = sessionId;
            this.connectionId = connectionId;
            this.userId = userId;
            this.sink = sink;
        }
    }
}
