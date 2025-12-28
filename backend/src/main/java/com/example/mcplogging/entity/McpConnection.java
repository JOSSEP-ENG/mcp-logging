package com.example.mcplogging.entity;

import com.example.mcplogging.converter.McpStatusConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mcp_connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class McpConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connector_id", nullable = false)
    private McpConnector connector;

    private String userId;

    @Column(columnDefinition = "TEXT")
    private String envConfig;

    @Column(length = 50)
    @Convert(converter = McpStatusConverter.class)
    @Builder.Default
    private McpStatus status = McpStatus.DISCONNECTED;

    private LocalDateTime connectedAt;

    private LocalDateTime lastUsedAt;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
