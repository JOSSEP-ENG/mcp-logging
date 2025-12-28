package com.example.mcplogging.mcp.service;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 활성 MCP 연결 풀 관리자
 * 사용자별 연결 인스턴스를 메모리에 유지
 */
@Slf4j
@Component
public class McpConnectionPoolManager {

    // Key: connectionId (McpConnection.id), Value: McpSyncClient 인스턴스
    private final Map<Long, McpSyncClient> activeConnections = new ConcurrentHashMap<>();

    /**
     * 연결 풀에 클라이언트 추가
     */
    public void addConnection(Long connectionId, McpSyncClient client) {
        activeConnections.put(connectionId, client);
        log.info("연결 풀에 추가: connectionId={}, 현재 활성 연결 수={}",
                 connectionId, activeConnections.size());
    }

    /**
     * 연결 풀에서 클라이언트 조회
     */
    public Optional<McpSyncClient> getConnection(Long connectionId) {
        return Optional.ofNullable(activeConnections.get(connectionId));
    }

    /**
     * 연결 풀에서 클라이언트 제거
     */
    public Optional<McpSyncClient> removeConnection(Long connectionId) {
        McpSyncClient removed = activeConnections.remove(connectionId);
        if (removed != null) {
            log.info("연결 풀에서 제거: connectionId={}, 현재 활성 연결 수={}",
                     connectionId, activeConnections.size());
        }
        return Optional.ofNullable(removed);
    }

    /**
     * 연결 존재 여부 확인
     */
    public boolean hasConnection(Long connectionId) {
        return activeConnections.containsKey(connectionId);
    }

    /**
     * 모든 활성 연결 개수
     */
    public int getActiveConnectionCount() {
        return activeConnections.size();
    }

    /**
     * 모든 연결 종료 (서버 종료 시)
     */
    public void disconnectAll() {
        log.info("모든 연결 종료 시작: 총 {} 개", activeConnections.size());
        activeConnections.forEach((id, client) -> {
            try {
                client.close();
                log.info("연결 종료 완료: connectionId={}", id);
            } catch (Exception e) {
                log.error("연결 종료 실패: connectionId={}", id, e);
            }
        });
        activeConnections.clear();
        log.info("모든 연결 종료 완료");
    }
}
