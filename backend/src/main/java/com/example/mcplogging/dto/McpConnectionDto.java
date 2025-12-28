package com.example.mcplogging.dto;

import com.example.mcplogging.entity.McpConnection;
import com.example.mcplogging.entity.McpStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class McpConnectionDto {
    private Long id;
    private Long connectorId;
    private String connectorName;
    private String userId;
    private McpStatus status;
    private LocalDateTime connectedAt;
    private LocalDateTime lastUsedAt;
    private String lastError;
    private LocalDateTime createdAt;

    public static McpConnectionDto from(McpConnection entity) {
        return McpConnectionDto.builder()
                .id(entity.getId())
                .connectorId(entity.getConnector().getId())
                .connectorName(entity.getConnector().getName())
                .userId(entity.getUserId())
                .status(entity.getStatus())
                .connectedAt(entity.getConnectedAt())
                .lastUsedAt(entity.getLastUsedAt())
                .lastError(entity.getLastError())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
