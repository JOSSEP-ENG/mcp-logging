package com.example.mcplogging.controller;

import com.example.mcplogging.entity.McpConnector;
import com.example.mcplogging.service.McpConnectorService;
import com.example.mcplogging.service.McpProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * MCP Proxy Controller - Claude Desktop이 연결할 SSE 프록시 엔드포인트
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mcp/proxy")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class McpProxyController {

    private final McpProxyService proxyService;
    private final McpConnectorService connectorService;

    /**
     * SSE Proxy 엔드포인트
     * Claude Desktop이 이 엔드포인트에 연결하여 MCP 통신
     *
     * GET /api/v1/mcp/proxy/{connectorName}/sse
     */
    @GetMapping(value = "/{connectorName}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> proxySSE(
            @PathVariable String connectorName,
            @RequestParam Map<String, String> env,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId
    ) {
        log.info("SSE Proxy 연결 요청: connector={}, userId={}", connectorName, userId);

        return proxyService.createSseProxy(connectorName, userId, env)
                .doOnSubscribe(sub -> log.info("SSE Proxy 스트림 시작: {}", connectorName))
                .doOnComplete(() -> log.info("SSE Proxy 스트림 종료: {}", connectorName))
                .doOnError(error -> log.error("SSE Proxy 에러: {}", connectorName, error));
    }

    /**
     * JSON-RPC 메시지 수신 엔드포인트 (SSE의 POST companion)
     * Claude Desktop이 JSON-RPC 요청을 이 엔드포인트로 전송
     *
     * POST /api/v1/mcp/proxy/{connectorName}/message
     */
    @PostMapping("/{connectorName}/message")
    public Mono<Map<String, Object>> handleMessage(
            @PathVariable String connectorName,
            @RequestBody Map<String, Object> message,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId
    ) {
        log.info("JSON-RPC 메시지 수신: connector={}, method={}",
                connectorName, message.get("method"));

        return proxyService.handleJsonRpcMessage(connectorName, userId, sessionId, message)
                .doOnSuccess(response -> log.info("JSON-RPC 응답 전송: {}", response.get("id")))
                .doOnError(error -> log.error("JSON-RPC 처리 에러", error));
    }

    /**
     * 프록시 상태 조회
     *
     * GET /api/v1/mcp/proxy/status
     */
    @GetMapping("/status")
    public Mono<Map<String, Object>> getProxyStatus() {
        return proxyService.getActiveProxies()
                .map(proxies -> Map.of(
                        "success", true,
                        "activeProxies", proxies.size(),
                        "proxies", proxies
                ));
    }

    /**
     * 사용 가능한 커넥터 목록 (Claude Desktop 설정용)
     *
     * GET /api/v1/mcp/proxy/connectors
     */
    @GetMapping("/connectors")
    public Mono<Map<String, Object>> getAvailableConnectors() {
        return Mono.fromCallable(() -> {
            var connectors = connectorService.getAllConnectors();
            return Map.of(
                    "success", true,
                    "connectors", connectors.stream()
                            .map(c -> Map.of(
                                    "name", c.getName(),
                                    "description", c.getDescription(),
                                    "sseUrl", String.format("http://localhost:8080/api/v1/mcp/proxy/%s/sse", c.getName()),
                                    "envTemplate", c.getEnvTemplate()
                            ))
                            .toList()
            );
        });
    }
}
