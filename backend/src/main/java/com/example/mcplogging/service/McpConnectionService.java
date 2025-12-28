package com.example.mcplogging.mcp.service;

import com.example.mcplogging.entity.McpConnection;
import com.example.mcplogging.entity.McpConnector;
import com.example.mcplogging.entity.McpStatus;
import com.example.mcplogging.enums.McpTransportType;
import com.example.mcplogging.repository.McpConnectionRepository;
import com.example.mcplogging.repository.McpConnectorRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * MCP 연결 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class McpConnectionService {

    private final McpConnectionRepository connectionRepository;
    private final McpConnectorRepository connectorRepository;
    private final McpConnectionPoolManager connectionPool;
    private final ObjectMapper objectMapper;

    /**
     * Connector 이름으로 조회
     */
    public McpConnector getConnectorByName(String name) {
        return connectorRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Connector not found: " + name));
    }

    /**
     * MCP 서버 연결
     */
    @Transactional
    public Mono<McpConnection> connect(Long connectorId, String userId, Map<String, String> environment) {
        // 1. Connector 조회
        McpConnector connector = connectorRepository.findById(connectorId)
                .orElseThrow(() -> new IllegalArgumentException("Connector not found: " + connectorId));

        // 2. McpConnection 엔티티 생성 (DB에 저장)
        McpConnection connection = McpConnection.builder()
                .connector(connector)
                .userId(userId)
                .status(McpStatus.DISCONNECTED)
                .envConfig(serializeEnvironment(environment))
                .connectedAt(null)
                .lastError(null)
                .build();

        McpConnection savedConnection = connectionRepository.save(connection);
        final Long connectionId = savedConnection.getId();

        // 3-4. McpSyncClient 생성 및 초기화
        return Mono.fromCallable(() -> {
            try {
                McpSyncClient client = createMcpClient(connector, environment);

                // 연결 성공 처리
                McpConnection conn = connectionRepository.findById(connectionId).orElseThrow();
                conn.setStatus(McpStatus.CONNECTED);
                conn.setConnectedAt(LocalDateTime.now());
                conn.setLastError(null);
                connectionRepository.save(conn);

                // 연결 풀에 추가
                connectionPool.addConnection(connectionId, client);

                log.info("MCP 연결 성공: connectionId={}", connectionId);
                return savedConnection;
            } catch (Exception e) {
                // 연결 실패 처리
                McpConnection conn = connectionRepository.findById(connectionId).orElse(null);
                if (conn != null) {
                    conn.setStatus(McpStatus.DISCONNECTED);
                    conn.setLastError(e.getMessage());
                    connectionRepository.save(conn);
                }
                log.error("MCP 연결 실패: connectionId={}", connectionId, e);
                throw new RuntimeException("MCP 연결 실패: " + e.getMessage(), e);
            }
        });
    }

    /**
     * MCP 서버 연결 해제
     */
    @Transactional
    public Mono<Void> disconnect(Long connectionId) {
        return Mono.fromRunnable(() -> {
            // 1. DB에서 연결 조회
            McpConnection connection = connectionRepository.findById(connectionId)
                    .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

            // 2. 연결 풀에서 클라이언트 제거
            connectionPool.removeConnection(connectionId).ifPresent(client -> {
                // 3. 클라이언트 연결 해제
                client.close();
            });

            // 4. DB 상태 업데이트
            connection.setStatus(McpStatus.DISCONNECTED);
            connection.setConnectedAt(null);
            connectionRepository.save(connection);

            log.info("MCP 연결 해제: connectionId={}", connectionId);
        });
    }

    /**
     * 연결 상태 조회
     */
    public McpConnection getConnectionStatus(Long connectionId) {
        return connectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));
    }

    /**
     * 사용자의 모든 연결 조회
     */
    public List<McpConnection> getUserConnections(String userId) {
        return connectionRepository.findByUserId(userId);
    }

    /**
     * 도구 목록 조회
     */
    public Mono<McpSchema.ListToolsResult> listTools(Long connectionId) {
        return Mono.fromCallable(() -> {
            McpSyncClient client = connectionPool.getConnection(connectionId)
                    .orElseThrow(() -> new IllegalStateException("Connection not active: " + connectionId));
            return client.listTools(null);  // cursor parameter
        });
    }

    /**
     * 도구 실행
     */
    public Mono<McpSchema.CallToolResult> callTool(Long connectionId, String toolName, Map<String, Object> arguments) {
        return Mono.fromCallable(() -> {
            McpSyncClient client = connectionPool.getConnection(connectionId)
                    .orElseThrow(() -> new IllegalStateException("Connection not active: " + connectionId));

            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, arguments);
            return client.callTool(request);
        });
    }

    // ===== Private Helper Methods =====

    /**
     * MCP 클라이언트 생성
     */
    private McpSyncClient createMcpClient(McpConnector connector, Map<String, String> environment) {
        if (connector.getTransportType() == McpTransportType.SSE) {
            // SSE Client
            String serverUrl = connector.getServerUrl();
            if (serverUrl == null || serverUrl.isBlank()) {
                throw new IllegalArgumentException("SSE transport는 serverUrl이 필요합니다.");
            }

            log.info("SSE Transport로 MCP 서버 연결: {}", serverUrl);
            HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(serverUrl)
                .build();

            McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .build();

            client.initialize();
            return client;
        }

        if (connector.getTransportType() == McpTransportType.STREAMABLE_HTTP) {
            // Streamable HTTP Client (권장 방식)
            String serverUrl = connector.getServerUrl();
            if (serverUrl == null || serverUrl.isBlank()) {
                throw new IllegalArgumentException("Streamable HTTP transport는 serverUrl이 필요합니다.");
            }

            log.info("Streamable HTTP Transport로 MCP 서버 연결: {}", serverUrl);
            HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(serverUrl)
                .build();

            McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .build();

            client.initialize();
            return client;
        }

        // STDIO Client
        List<String> args = parseArgs(connector.getArgs());
        String command = connector.getCommand();

        // Windows 환경 처리
        ServerParameters.Builder paramsBuilder;
        if (isWindows()) {
            // Windows에서는 cmd.exe /c 로 실행하고, command와 args를 모두 합쳐서 전달
            paramsBuilder = ServerParameters.builder("cmd.exe");
            List<String> allArgs = new java.util.ArrayList<>();
            allArgs.add("/c");
            allArgs.add(command);
            if (args != null && !args.isEmpty()) {
                allArgs.addAll(args);
            }
            paramsBuilder.args(allArgs.toArray(new String[0]));
        } else {
            // Unix/Linux/Mac에서는 command를 직접 실행
            paramsBuilder = ServerParameters.builder(command);
            if (args != null && !args.isEmpty()) {
                paramsBuilder.args(args.toArray(new String[0]));
            }
        }

        if (environment != null && !environment.isEmpty()) {
            paramsBuilder.env(environment);
        }

        ServerParameters stdioParams = paramsBuilder.build();
        StdioClientTransport transport = new StdioClientTransport(stdioParams, McpJsonMapper.createDefault());

        McpSyncClient client = McpClient.sync(transport)
            .requestTimeout(Duration.ofSeconds(30))
            .build();

        // 연결 초기화
        client.initialize();
        return client;
    }

    /**
     * Windows 환경 확인
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * args 문자열을 리스트로 파싱
     */
    private List<String> parseArgs(String argsString) {
        if (argsString == null || argsString.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(argsString, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // JSON 파싱 실패 시 공백으로 split
            return Arrays.asList(argsString.split("\\s+"));
        }
    }

    /**
     * 환경 변수를 JSON 문자열로 직렬화
     */
    private String serializeEnvironment(Map<String, String> environment) {
        if (environment == null || environment.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(environment);
        } catch (Exception e) {
            log.error("환경 변수 직렬화 실패", e);
            return null;
        }
    }
}
