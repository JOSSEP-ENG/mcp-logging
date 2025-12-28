package com.example.mcplogging.service;

import com.example.mcplogging.entity.McpConnector;
import com.example.mcplogging.mcp.service.McpConnectionService;
import com.example.mcplogging.repository.ToolUsageLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Unified MCP Proxy Service
 * 모든 MCP 커넥터를 하나의 MCP 서버처럼 통합 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpUnifiedProxyService {

    private final McpConnectionService connectionService;
    private final McpConnectorService connectorService;
    private final ToolUsageLogRepository toolUsageLogRepository;
    private final ObjectMapper objectMapper;

    // 활성 세션 (sessionId → UnifiedSession)
    private final Map<String, UnifiedSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * 통합 SSE 스트림 생성
     */
    public Flux<ServerSentEvent<String>> createUnifiedSseStream(String userId) {
        String sessionId = UUID.randomUUID().toString();

        return Mono.fromCallable(() -> {
            // 세션 생성
            Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();
            UnifiedSession session = new UnifiedSession(sessionId, userId, sink);
            activeSessions.put(sessionId, session);

            log.info("통합 세션 생성: sessionId={}", sessionId);

            // 초기화 이벤트 전송
            sink.tryEmitNext(ServerSentEvent.<String>builder()
                    .event("session")
                    .data("{\"sessionId\":\"" + sessionId + "\",\"status\":\"connected\"}")
                    .build());

            return sink.asFlux();
        }).flatMapMany(flux -> flux)
                .timeout(Duration.ofHours(1))
                .doFinally(signal -> {
                    UnifiedSession session = activeSessions.remove(sessionId);
                    if (session != null) {
                        // 모든 연결 해제
                        session.connections.values().forEach(connectionId ->
                                connectionService.disconnect(connectionId).subscribe()
                        );
                        log.info("통합 세션 종료: sessionId={}", sessionId);
                    }
                });
    }

    /**
     * JSON-RPC 메시지 처리
     */
    public Mono<Map<String, Object>> handleJsonRpcMessage(
            String userId,
            String sessionId,
            Map<String, Object> message
    ) {
        return Mono.fromCallable(() -> {
            String method = (String) message.get("method");
            Object params = message.get("params");
            Object id = message.get("id");

            log.info("메시지 처리: method={}, sessionId={}, userId={}", method, sessionId, userId);

            // sessionId가 null인 경우 userId로 세션 찾기 또는 생성
            UnifiedSession session = null;
            if (sessionId != null) {
                session = activeSessions.get(sessionId);
            } else {
                // userId별 세션 찾기 (SSE 연결에서 생성된 세션)
                session = activeSessions.values().stream()
                        .filter(s -> s.userId.equals(userId))
                        .findFirst()
                        .orElse(null);

                // 세션이 없으면 임시 세션 생성 (tools/list, tools/call 용)
                if (session == null && !method.equals("initialize")) {
                    String tempSessionId = "temp-" + userId;
                    session = activeSessions.computeIfAbsent(tempSessionId, key -> {
                        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();
                        UnifiedSession newSession = new UnifiedSession(tempSessionId, userId, sink);
                        log.info("임시 세션 생성: sessionId={}, userId={}", tempSessionId, userId);
                        return newSession;
                    });
                }
            }

            return switch (method) {
                case "initialize" -> handleInitialize(id, params);
                case "tools/list" -> handleToolsList(userId, session, id);
                case "tools/call" -> handleToolsCall(userId, session, id, params);
                case "notifications/initialized", "notifications/cancelled" -> {
                    // Notification 메시지는 응답 불필요
                    log.info("Notification 수신: {}", method);
                    yield Map.of("jsonrpc", "2.0");
                }
                default -> {
                    log.warn("알 수 없는 메서드: {}", method);
                    yield createErrorResponse(id, -32601, "Method not found: " + method);
                }
            };
        });
    }

    /**
     * initialize 처리
     */
    private Map<String, Object> handleInitialize(Object id, Object params) {
        return Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "result", Map.of(
                        "protocolVersion", "2024-11-05",
                        "capabilities", Map.of(
                                "tools", Map.of()
                        ),
                        "serverInfo", Map.of(
                                "name", "mcp-logging-unified",
                                "version", "1.0.0"
                        )
                )
        );
    }

    /**
     * tools/list 처리 - 모든 커넥터의 도구를 통합
     */
    private Map<String, Object> handleToolsList(String userId, UnifiedSession session, Object id) {
        try {
            List<McpConnector> connectors = connectorService.getAllConnectors();
            List<Map<String, Object>> allTools = new ArrayList<>();

            for (McpConnector connector : connectors) {
                // 비활성화된 커넥터 스킵
                if (connector.getEnabled() == null || !connector.getEnabled()) {
                    log.debug("비활성화된 커넥터 스킵: {}", connector.getName());
                    continue;
                }

                try {
                    // 커넥터에 연결 (없으면 새로 생성)
                    Long connectionId = session.connections.computeIfAbsent(
                            connector.getName(),
                            name -> {
                                try {
                                    log.info("커넥터 연결 시작: {}", name);

                                    // 환경 변수 준비 (커넥터별 기본값)
                                    // TODO: 사용자별 토큰을 데이터베이스나 설정에서 가져오도록 구현 필요
                                    Map<String, String> env = new HashMap<>();
                                    if ("notion".equals(name)) {
                                        // TODO: 실제 사용 시 환경 변수나 사용자 설정에서 토큰을 가져와야 함
                                        String notionToken = System.getenv("NOTION_TOKEN");
                                        if (notionToken != null && !notionToken.isEmpty()) {
                                            env.put("NOTION_TOKEN", notionToken);
                                        }
                                    }

                                    var connection = connectionService.connect(
                                            connector.getId(),
                                            userId,
                                            env
                                    ).block(Duration.ofSeconds(30));  // 30초 타임아웃
                                    log.info("커넥터 연결 완료: {} → connectionId={}", name, connection.getId());
                                    return connection.getId();
                                } catch (Exception e) {
                                    log.error("커넥터 연결 실패: {}", name, e);
                                    return null;
                                }
                            }
                    );

                    if (connectionId == null) {
                        log.warn("커넥터 연결 ID가 null: {}", connector.getName());
                        continue;
                    }

                    // 도구 목록 조회
                    log.info("도구 목록 조회 시작: {}", connector.getName());
                    McpSchema.ListToolsResult result = connectionService.listTools(connectionId)
                            .block(Duration.ofSeconds(10));  // 10초 타임아웃

                    if (result != null && result.tools() != null) {
                        log.info("도구 {} 개 발견: {}", result.tools().size(), connector.getName());
                        result.tools().forEach(tool -> {
                            // 도구 이름에 커넥터 prefix 추가
                            String prefixedName = connector.getName() + "__" + tool.name();
                            allTools.add(Map.of(
                                    "name", prefixedName,
                                    "description", String.format("[%s] %s",
                                            connector.getName(),
                                            tool.description() != null ? tool.description() : ""),
                                    "inputSchema", tool.inputSchema() != null ? tool.inputSchema() : Map.of()
                            ));
                        });
                    } else {
                        log.warn("도구 목록이 비어있음: {}", connector.getName());
                    }
                } catch (Exception e) {
                    log.error("도구 목록 조회 실패: {}", connector.getName(), e);
                    // 에러가 발생해도 다음 커넥터 계속 처리
                }
            }

            return Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "result", Map.of("tools", allTools)
            );
        } catch (Exception e) {
            log.error("tools/list 에러", e);
            return createErrorResponse(id, -32603, "Internal error: " + e.getMessage());
        }
    }

    /**
     * tools/call 처리 - prefix 파싱하여 적절한 커넥터로 라우팅
     */
    private Map<String, Object> handleToolsCall(String userId, UnifiedSession session, Object id, Object params) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> paramsMap = (Map<String, Object>) params;
            String fullToolName = (String) paramsMap.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) paramsMap.get("arguments");

            // 도구 이름 파싱 (connector__toolName)
            String[] parts = fullToolName.split("__", 2);
            if (parts.length != 2) {
                return createErrorResponse(id, -32602, "Invalid tool name format. Expected: connector__toolName");
            }

            String connectorName = parts[0];
            String toolName = parts[1];

            // 연결 ID 가져오기
            Long connectionId = session.connections.get(connectorName);
            if (connectionId == null) {
                return createErrorResponse(id, -32002, "Connector not connected: " + connectorName);
            }

            // Notion 페이지 생성 시 parent 자동 설정
            if ("notion".equals(connectorName) && "API-post-page".equals(toolName)) {
                arguments = handleNotionPageCreation(connectionId, arguments);
            }

            // 도구 실행
            McpSchema.CallToolResult result = connectionService.callTool(connectionId, toolName, arguments).block();

            // 로깅
            logToolUsage(connectionId, connectorName, toolName, arguments, result);

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
     * Notion 페이지 생성 시 parent가 없으면 자동으로 설정
     */
    private Map<String, Object> handleNotionPageCreation(Long connectionId, Map<String, Object> arguments) {
        // parent가 이미 있으면 그대로 반환
        if (arguments != null && arguments.containsKey("parent")) {
            return arguments;
        }

        try {
            log.info("Notion 페이지 생성: parent 자동 설정 시작");

            // workspace의 페이지 검색
            McpSchema.CallToolResult searchResult = connectionService.callTool(
                    connectionId,
                    "API-post-search",
                    Map.of(
                            "filter", Map.of("value", "page", "property", "object"),
                            "page_size", 1
                    )
            ).block();

            log.info("검색 결과: isError={}, content size={}",
                    searchResult != null ? searchResult.isError() : "null",
                    searchResult != null && searchResult.content() != null ? searchResult.content().size() : 0);

            if (searchResult != null && searchResult.content() != null && !searchResult.content().isEmpty()) {
                // 첫 번째 content에서 JSON 파싱
                Object firstContent = searchResult.content().get(0);
                log.info("첫 번째 content 타입: {}", firstContent.getClass().getName());

                String text = null;
                if (firstContent instanceof McpSchema.TextContent) {
                    // Spring AI MCP SDK의 TextContent 객체
                    McpSchema.TextContent textContent = (McpSchema.TextContent) firstContent;
                    text = textContent.text();
                } else if (firstContent instanceof Map) {
                    // Map 형태인 경우 (호환성)
                    @SuppressWarnings("unchecked")
                    Map<String, Object> contentMap = (Map<String, Object>) firstContent;
                    text = (String) contentMap.get("text");
                }

                log.info("검색 응답 텍스트: {}", text);

                if (text != null) {
                    // JSON 파싱
                    Map<String, Object> searchData = objectMapper.readValue(text, Map.class);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> results = (List<Map<String, Object>>) searchData.get("results");

                    log.info("검색된 페이지 개수: {}", results != null ? results.size() : 0);

                    if (results != null && !results.isEmpty()) {
                        String firstPageId = (String) results.get(0).get("id");
                        log.info("자동 선택된 부모 페이지 ID: {}", firstPageId);

                        // arguments에 parent 추가
                        Map<String, Object> newArguments = new HashMap<>(arguments != null ? arguments : Map.of());
                        newArguments.put("parent", Map.of("page_id", firstPageId));
                        return newArguments;
                    } else {
                        log.warn("검색 결과가 비어있음 (Integration이 접근 가능한 페이지가 없음)");
                    }
                }
            } else {
                log.warn("검색 결과가 null이거나 비어있음");
            }

            log.warn("워크스페이스에서 페이지를 찾을 수 없음. parent 설정 실패");
            return arguments;

        } catch (Exception e) {
            log.error("Notion parent 자동 설정 실패", e);
            return arguments;
        }
    }

    /**
     * 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse(Object id, int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);  // null 허용

        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        response.put("error", error);

        return response;
    }

    /**
     * 도구 사용 로깅
     */
    private void logToolUsage(Long connectionId, String connectorName, String toolName,
                               Map<String, Object> arguments, McpSchema.CallToolResult result) {
        try {
            var connection = connectionService.getConnectionStatus(connectionId);

            var log = com.example.mcplogging.entity.ToolUsageLog.builder()
                    .connection(connection)
                    .connectorName(connectorName)
                    .toolName(toolName)
                    .toolParameters(objectMapper.writeValueAsString(arguments))
                    .toolResponse(objectMapper.writeValueAsString(result.content()))
                    .mcpRequestRaw(objectMapper.writeValueAsString(Map.of("name", toolName, "arguments", arguments)))
                    .mcpResponseRaw(objectMapper.writeValueAsString(result))
                    .executedAt(LocalDateTime.now())
                    .build();

            toolUsageLogRepository.save(log);
        } catch (Exception e) {
            this.log.error("도구 사용 로깅 실패", e);
        }
    }

    /**
     * 상태 조회
     */
    public Mono<Map<String, Object>> getStatus() {
        return Mono.fromCallable(() -> Map.of(
                "success", true,
                "activeSessions", activeSessions.size(),
                "sessions", activeSessions.entrySet().stream()
                        .map(entry -> Map.of(
                                "sessionId", entry.getKey(),
                                "userId", entry.getValue().userId,
                                "connections", entry.getValue().connections.size()
                        ))
                        .toList()
        ));
    }

    /**
     * 통합 세션
     */
    private static class UnifiedSession {
        final String sessionId;
        final String userId;
        final Sinks.Many<ServerSentEvent<String>> sink;
        final Map<String, Long> connections = new ConcurrentHashMap<>(); // connectorName → connectionId

        UnifiedSession(String sessionId, String userId, Sinks.Many<ServerSentEvent<String>> sink) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.sink = sink;
        }
    }
}
