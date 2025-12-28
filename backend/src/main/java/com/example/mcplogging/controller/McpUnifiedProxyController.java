package com.example.mcplogging.controller;

import com.example.mcplogging.service.McpUnifiedProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Unified MCP Proxy Controller
 * Claude Desktop이 단일 MCP 서버로 인식하는 통합 프록시
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class McpUnifiedProxyController {

    private final McpUnifiedProxyService proxyService;

    /**
     * SSE 엔드포인트 - Claude Desktop이 연결
     *
     * GET /mcp/sse
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sseEndpoint(
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId
    ) {
        log.info("통합 MCP SSE 연결: userId={}", userId);

        return proxyService.createUnifiedSseStream(userId)
                .doOnSubscribe(sub -> log.info("SSE 스트림 시작"))
                .doOnComplete(() -> log.info("SSE 스트림 종료"))
                .doOnError(error -> log.error("SSE 에러", error));
    }

    /**
     * JSON-RPC 메시지 처리 (SSE 프로토콜 - 같은 경로에 POST)
     *
     * POST /mcp/sse
     */
    @PostMapping("/sse")
    public Mono<Map<String, Object>> handleSseMessage(
            @RequestBody Map<String, Object> message,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId
    ) {
        String method = (String) message.get("method");
        log.info("JSON-RPC 메시지 (SSE): method={}, id={}", method, message.get("id"));

        return proxyService.handleJsonRpcMessage(userId, sessionId, message)
                .doOnSuccess(response -> log.info("응답 전송 완료"))
                .doOnError(error -> log.error("메시지 처리 에러", error));
    }

    /**
     * JSON-RPC 메시지 처리 (신 Streamable HTTP 프로토콜)
     *
     * POST /mcp/message
     */
    @PostMapping("/message")
    public Mono<Map<String, Object>> handleMessage(
            @RequestBody Map<String, Object> message,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId
    ) {
        String method = (String) message.get("method");
        log.info("JSON-RPC 메시지: method={}, id={}", method, message.get("id"));

        return proxyService.handleJsonRpcMessage(userId, sessionId, message)
                .doOnSuccess(response -> log.info("응답 전송 완료"))
                .doOnError(error -> log.error("메시지 처리 에러", error));
    }

    /**
     * 상태 확인
     */
    @GetMapping("/status")
    public Mono<Map<String, Object>> getStatus() {
        return proxyService.getStatus();
    }
}