package com.example.mcplogging.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tool_usage_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_log_id")
    private ChatLog chatLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id")
    private McpConnection connection;

    @Column(length = 100)
    private String connectorName;

    @Column(nullable = false, length = 200)
    private String toolName;

    @Column(columnDefinition = "TEXT")
    private String toolParameters; // JSON string

    @Column(columnDefinition = "TEXT")
    private String toolResponse; // JSON string

    @Column(columnDefinition = "TEXT")
    private String mcpRequestRaw; // MCP 프로토콜 원본 요청

    @Column(columnDefinition = "TEXT")
    private String mcpResponseRaw; // MCP 프로토콜 원본 응답

    @Builder.Default
    private LocalDateTime executedAt = LocalDateTime.now();
}