package com.example.mcplogging.config;

import com.example.mcplogging.entity.McpConnector;
import com.example.mcplogging.enums.McpServerType;
import com.example.mcplogging.enums.McpTransportType;
import com.example.mcplogging.repository.McpConnectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final McpConnectorRepository connectorRepository;

    @Bean
    public ApplicationRunner initializeOfficialServers() {
        return args -> {
            initializeNotionServer();
            initializeFilesystemServer();
            initializeMemoryServer();
            initializeGitLabServer();
            initializePuppeteerServer();
        };
    }

    private void initializeNotionServer() {
        if (connectorRepository.findByName("notion").isEmpty()) {
            McpConnector notionConnector = McpConnector.builder()
                    .name("notion")
                    .description("Notion MCP Server - 자체 호스팅 Notion MCP 서버 (STDIO)")
                    .type(McpServerType.OFFICIAL)
                    .transportType(McpTransportType.STDIO)
                    .command("npx")
                    .args("[\"-y\", \"@notionhq/notion-mcp-server\"]")
                    .serverUrl(null)
                    .envTemplate("{\"NOTION_TOKEN\": \"\"}")
                    .enabled(true)
                    .build();

            connectorRepository.save(notionConnector);
            log.info("✓ Notion MCP Server (STDIO) 초기 데이터 저장 완료");
        } else {
            log.info("✓ Notion MCP Server 이미 존재함 - 초기화 스킵");
        }
    }

    private void initializeFilesystemServer() {
        if (connectorRepository.findByName("filesystem").isEmpty()) {
            McpConnector filesystemConnector = McpConnector.builder()
                    .name("filesystem")
                    .description("Filesystem MCP Server - 파일 시스템 읽기/쓰기, 디렉토리 탐색 기능 제공")
                    .type(McpServerType.OFFICIAL)
                    .transportType(McpTransportType.STDIO)
                    .command("npx")
                    .args("[\"-y\", \"@modelcontextprotocol/server-filesystem\", \"D:/Workspace\"]")
                    .envTemplate("{}")
                    .enabled(true)
                    .build();

            connectorRepository.save(filesystemConnector);
            log.info("✓ Filesystem MCP Server 초기 데이터 저장 완료");
        } else {
            log.info("✓ Filesystem MCP Server 이미 존재함 - 초기화 스킵");
        }
    }

    private void initializeMemoryServer() {
        if (connectorRepository.findByName("memory").isEmpty()) {
            McpConnector memoryConnector = McpConnector.builder()
                    .name("memory")
                    .description("Memory MCP Server - 메모리 기반 키-값 스토리지 제공")
                    .type(McpServerType.OFFICIAL)
                    .transportType(McpTransportType.STDIO)
                    .command("npx")
                    .args("[\"-y\", \"@modelcontextprotocol/server-memory\"]")
                    .envTemplate("{}")
                    .enabled(true)
                    .build();

            connectorRepository.save(memoryConnector);
            log.info("✓ Memory MCP Server 초기 데이터 저장 완료");
        } else {
            log.info("✓ Memory MCP Server 이미 존재함 - 초기화 스킵");
        }
    }

    private void initializeGitLabServer() {
        if (connectorRepository.findByName("gitlab").isEmpty()) {
            McpConnector gitlabConnector = McpConnector.builder()
                    .name("gitlab")
                    .description("GitLab MCP Server - GitLab 리포지토리, 이슈, MR 관리 및 코드 검색 기능 제공")
                    .type(McpServerType.OFFICIAL)
                    .transportType(McpTransportType.STDIO)
                    .command("npx")
                    .args("[\"-y\", \"@modelcontextprotocol/server-gitlab\"]")
                    .envTemplate("{\"GITLAB_PERSONAL_ACCESS_TOKEN\": \"\", \"GITLAB_API_URL\": \"https://gitlab.com\"}")
                    .enabled(true)
                    .build();

            connectorRepository.save(gitlabConnector);
            log.info("✓ GitLab MCP Server 초기 데이터 저장 완료");
        } else {
            log.info("✓ GitLab MCP Server 이미 존재함 - 초기화 스킵");
        }
    }

    private void initializePuppeteerServer() {
        if (connectorRepository.findByName("puppeteer").isEmpty()) {
            McpConnector puppeteerConnector = McpConnector.builder()
                    .name("puppeteer")
                    .description("Puppeteer MCP Server - 웹 브라우저 자동화, 스크린샷, 페이지 탐색 기능 제공")
                    .type(McpServerType.OFFICIAL)
                    .transportType(McpTransportType.STDIO)
                    .command("npx")
                    .args("[\"-y\", \"@modelcontextprotocol/server-puppeteer\"]")
                    .envTemplate("{}")
                    .enabled(true)
                    .build();

            connectorRepository.save(puppeteerConnector);
            log.info("✓ Puppeteer MCP Server 초기 데이터 저장 완료");
        } else {
            log.info("✓ Puppeteer MCP Server 이미 존재함 - 초기화 스킵");
        }
    }
}
