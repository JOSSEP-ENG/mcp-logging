package com.example.mcplogging.dto;

import com.example.mcplogging.entity.McpConnector;
import com.example.mcplogging.enums.McpServerType;
import com.example.mcplogging.enums.McpTransportType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class McpConnectorDto {
    private Long id;
    private String name;
    private String description;
    private McpServerType type;
    private McpTransportType transportType;
    private Boolean enabled;
    private String envTemplate; // 환경변수 템플릿 (JSON 형식)

    public static McpConnectorDto from(McpConnector entity) {
        return McpConnectorDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .type(entity.getType())
                .transportType(entity.getTransportType())
                .enabled(entity.getEnabled())
                .envTemplate(entity.getEnvTemplate())
                .build();
    }
}