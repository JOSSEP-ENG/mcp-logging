package com.example.mcplogging.entity;

import com.example.mcplogging.converter.McpServerTypeConverter;
import com.example.mcplogging.converter.McpTransportTypeConverter;
import com.example.mcplogging.enums.McpServerType;
import com.example.mcplogging.enums.McpTransportType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mcp_connectors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class McpConnector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    @Convert(converter = McpServerTypeConverter.class)
    private McpServerType type;

    @Column(length = 50)
    @Convert(converter = McpTransportTypeConverter.class)
    private McpTransportType transportType;

    @Column(length = 500)
    private String command;

    @Column(columnDefinition = "TEXT")
    private String args;

    @Column(columnDefinition = "TEXT")
    private String envTemplate;

    @Column(length = 500)
    private String serverUrl;

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
