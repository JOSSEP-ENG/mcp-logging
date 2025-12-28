package com.example.mcplogging.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolCallRequest {
    private String toolName;                    // 도구 이름
    private Map<String, Object> arguments;      // 도구 인자
}
