# MCP Logging System

Spring Boot + React 기반 MCP (Model Context Protocol) 로깅 시스템입니다. 여러 MCP 서버를 하나의 통합 프록시로 관리하고, Notion 페이지 생성 시 자동 parent 선택 기능을 제공합니다.

## 🌟 주요 기능

- **통합 MCP 프록시**: 여러 MCP 커넥터를 하나의 서버로 통합
- **자동 Parent 선택**: Notion 페이지 생성 시 parent를 지정하지 않으면 자동으로 검색하여 선택
- **도구 사용 로깅**: MCP 도구 호출 내역을 데이터베이스에 저장
- **커넥터 관리 UI**: React 기반 웹 인터페이스로 커넥터 관리
- **다중 전송 타입 지원**: STDIO 및 SSE 전송 타입 지원

## 🛠️ 기술 스택

### 백엔드
- **Spring Boot** 4.0.1
- **Spring AI MCP SDK** 1.1.0 (MCP Java SDK v0.16.0)
- **H2 Database** (file-based)
- **Gradle** 9.2.1
- **Java** 25

### 프론트엔드
- **React** 19
- **Vite** 6
- **Tailwind CSS** 4
- **Lucide React** (아이콘)

## 📦 지원 MCP 커넥터

- **Notion** - Notion API 통합
- **Filesystem** - 파일 시스템 읽기/쓰기
- **Memory** - 지식 그래프 메모리
- **GitLab** - GitLab API 통합
- **Puppeteer** - 웹 자동화

## 🚀 시작하기

### 사전 요구사항

- Java 25
- Node.js 18+
- npm 또는 yarn

### 백엔드 실행

```bash
cd backend
./gradlew bootRun
```

백엔드는 `http://localhost:8080`에서 실행됩니다.

### 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

프론트엔드는 `http://localhost:5173`에서 실행됩니다.

## 🔧 환경 변수 설정

MCP 커넥터 사용을 위해 필요한 환경 변수를 설정하세요:

```bash
# Notion
export NOTION_TOKEN=your_notion_integration_token

# GitLab
export GITLAB_PERSONAL_ACCESS_TOKEN=your_gitlab_token
```

## 📖 API 엔드포인트

### 통합 MCP 프록시

- **SSE 스트림**: `GET /mcp/sse`
- **JSON-RPC 메시지**: `POST /mcp/sse`

### REST API

- **커넥터 목록**: `GET /api/v1/connectors`
- **연결 생성**: `POST /api/v1/connections`
- **도구 목록**: `GET /api/v1/connections/{id}/tools`
- **도구 호출**: `POST /api/v1/connections/{id}/tools/call`

## 🎯 자동 Parent 선택 기능

Notion 페이지 생성 시 `parent` 파라미터를 생략하면:

1. `API-post-search`를 호출하여 접근 가능한 페이지 검색
2. 검색된 첫 번째 페이지를 parent로 자동 선택
3. 선택된 parent 하위에 새 페이지 생성

### 사용 예시

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "id": 1,
  "params": {
    "name": "notion__API-post-page",
    "arguments": {
      "properties": {
        "title": [{"text": {"content": "새 페이지"}}]
      }
      // parent 파라미터 생략 → 자동 선택!
    }
  }
}
```

## 🔍 주요 구현 내용

### 문제 해결

**문제**: 통합 프록시가 MCP 커넥터 연결 시 환경 변수를 전달하지 않아 401 에러 발생

**해결**: `McpUnifiedProxyService.java`에서 연결 생성 시 환경 변수를 포함하도록 수정

```java
// 수정 전
connectionService.connect(connectorId, userId, Map.of())

// 수정 후
Map<String, String> env = new HashMap<>();
if ("notion".equals(name)) {
    String notionToken = System.getenv("NOTION_TOKEN");
    if (notionToken != null) {
        env.put("NOTION_TOKEN", notionToken);
    }
}
connectionService.connect(connectorId, userId, env)
```

### Spring AI MCP SDK 통합

Spring AI MCP SDK의 `TextContent` 객체를 올바르게 처리:

```java
if (firstContent instanceof McpSchema.TextContent) {
    McpSchema.TextContent textContent = (McpSchema.TextContent) firstContent;
    text = textContent.text();
}
```

## 📝 데이터베이스 스키마

### 주요 테이블

- `mcp_connectors` - MCP 서버 연결 설정 템플릿
- `mcp_connections` - 사용자별 실제 연결 인스턴스
- `chat_logs` - 사용자 질문/응답 기록
- `tool_usage_logs` - MCP 도구 실행 로그

## 🧪 테스트

curl을 사용한 API 테스트:

```bash
# 연결 생성
curl -X POST http://localhost:8080/api/v1/connections \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test-user" \
  -d '{"connectorId":1,"env":{"NOTION_TOKEN":"your_token"}}'

# 도구 목록 조회
curl http://localhost:8080/api/v1/connections/1/tools \
  -H "X-User-Id: test-user"

# 도구 호출
curl -X POST http://localhost:8080/api/v1/connections/1/tools/call \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test-user" \
  -d '{"toolName":"API-post-search","arguments":{"page_size":1}}'
```

## 🤝 기여

이슈와 Pull Request는 언제나 환영합니다!

## 📄 라이선스

MIT License

## 👤 개발자

Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude Sonnet 4.5
