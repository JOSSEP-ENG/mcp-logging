package com.example.mcplogging.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionRequest {
    private Long connectorId;           // 연결할 커넥터 ID
    private Map<String, String> env;    // 환경 변수 (API 키 등)
}
