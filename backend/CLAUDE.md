# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MCP Logging is a Spring Boot application for managing and logging MCP (Model Context Protocol) server connections and tool usage. The application tracks chat interactions, MCP connector configurations, and tool execution logs.

## Build & Run Commands

### Build
```bash
./gradlew build
```

### Run Application
```bash
./gradlew bootRun
```

### Run Tests
```bash
./gradlew test
```

### Run Single Test
```bash
./gradlew test --tests "ClassName.methodName"
```

### Clean Build
```bash
./gradlew clean build
```

## Architecture

### Core Domain Model

The application manages four primary entities with the following relationships:

1. **McpConnector** - MCP 서버 연결 설정 템플릿
   - Contains configuration templates for MCP server connections
   - Supports two server types (OFFICIAL, CUSTOM) and two transport types (STDIO, SSE)
   - Stores command, args, environment templates, and server URLs

2. **McpConnection** - 사용자별 실제 MCP 연결 인스턴스
   - User-specific connection instances based on McpConnector templates
   - Tracks connection status (CONNECTED, DISCONNECTED), timestamps, and errors
   - Lazy-loaded Many-to-One relationship with McpConnector

3. **ChatLog** - 사용자 질문/응답 기록
   - Records user questions and AI responses
   - One-to-Many relationship with ToolUsageLog (cascade ALL, orphan removal)

4. **ToolUsageLog** - MCP 도구 실행 로그
   - Logs individual tool executions during chat interactions
   - References both ChatLog (parent conversation) and McpConnection (which connector was used)
   - Stores tool parameters, responses, and raw MCP protocol request/response

### Enum Converters

All enum types use JPA AttributeConverters to store lowercase values in the database:

- **McpServerTypeConverter** - Converts McpServerType enum (OFFICIAL/CUSTOM)
- **McpTransportTypeConverter** - Converts McpTransportType enum (STDIO/SSE)
- **McpStatusConverter** - Converts McpStatus enum (CONNECTED/DISCONNECTED)

These converters are applied using `@Convert` annotation on entity fields.

### Service Layer

Services are transactional with `@Transactional(readOnly = true)` by default:

- **McpConnectorService** - Manages connector templates (browse, filter by type, retrieve details)

### API Structure

RESTful endpoints follow the pattern `/api/v1/{resource}`:

- `/api/v1/connectors` - Browse available MCP connectors
- `/api/v1/connectors/type/{type}` - Filter by server type
- `/api/v1/connectors/{id}` - Get connector details

All responses use the `ApiResponse<T>` wrapper with standard success/error format.

## Database Configuration

- **Database**: H2 file-based database at `./data/mcplog`
- **H2 Console**: Available at `/h2-console` (enabled in development)
- **JPA**: DDL auto-update mode with SQL logging enabled
- **Connection**: `jdbc:h2:file:./data/mcplog` (username: `sa`, no password)

## Technology Stack

- **Java**: 25
- **Spring Boot**: 4.0.1
- **Build Tool**: Gradle
- **Database**: H2 (file-based)
- **ORM**: Spring Data JPA
- **Utilities**: Lombok for boilerplate reduction

## Package Structure

```
com.example.mcplogging/
├── controller/      # REST API endpoints
├── converter/       # JPA enum converters
├── dto/            # Data transfer objects
├── entity/         # JPA entities
├── enums/          # Enum types
├── repository/     # Spring Data JPA repositories
└── service/        # Business logic layer
```
