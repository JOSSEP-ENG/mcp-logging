package com.example.mcplogging.dto;

import com.example.mcplogging.enums.McpServerType;
import com.example.mcplogging.enums.McpTransportType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 커넥터 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class McpConnectorCreateRequest {

    @NotBlank(message = "커넥터 이름은 필수입니다") //TODO: 다국어 처리
    private String name;

    @NotBlank(message = "설명은 필수입니다")
    private String description;

    @NotBlank(message = "타입은 필수입니다 (official, custom)")
    private McpServerType type;

    @NotBlank(message = "전송 타입은 필수입니다 (stdio, sse)")
    private McpTransportType transportType;

    // SSE 방식인 경우
    private String serverUrl;

    // STDIO 방식인 경우
    private String command;
    private String args;  // JSON 배열 문자열 예: ["arg1", "arg2"]

    // 환경 변수 템플릿 (JSON 문자열)
    // 예: {"API_KEY": "", "DB_HOST": "localhost"}
    private String envTemplate;

    // 기본값 true
    @Builder.Default
    private Boolean enabled = true;
}