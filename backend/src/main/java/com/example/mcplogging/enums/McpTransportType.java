package com.example.mcplogging.enums;

/**
 * MCP Transport 타입
 * - STDIO: 로컬 프로세스와의 표준 입출력 통신
 * - SSE: Server-Sent Events (deprecated since MCP 2025-03-26)
 * - STREAMABLE_HTTP: HTTP Stream Transport (권장)
 */
public enum McpTransportType {
    STDIO,
    SSE,
    STREAMABLE_HTTP
}
