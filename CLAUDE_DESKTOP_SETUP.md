# Claude Desktop MCP Proxy 설정 가이드

이 가이드는 Claude Desktop에서 MCP 서버 요청을 우리 백엔드를 통해 프록시하여 모든 통신을 로깅하는 방법을 설명합니다.

## 🎯 구조

```
Claude Desktop → MCP Logging Proxy (localhost:8080) → 실제 MCP 서버
                            ↓
                    모든 요청/응답 로깅
                    ToolUsageLog 테이블에 저장
```

## 📋 준비사항

1. **백엔드 서버 실행 중**
   ```bash
   cd backend
   ./gradlew bootRun
   ```
   - 서버: http://localhost:8080

2. **프론트엔드 실행 (선택사항)**
   ```bash
   cd frontend
   npm run dev
   ```
   - 대시보드: http://localhost:5174

## 🔧 Claude Desktop 설정

### Windows

1. **설정 파일 위치**
   ```
   %APPDATA%\Claude\claude_desktop_config.json
   ```
   전체 경로 예시:
   ```
   C:\Users\[사용자명]\AppData\Roaming\Claude\claude_desktop_config.json
   ```

2. **설정 파일 편집**
   - 파일이 없다면 새로 생성
   - 다음 내용을 추가:

   ```json
   {
     "mcpServers": {
       "notion-proxy": {
         "url": "http://localhost:8080/api/v1/mcp/proxy/notion/sse?NOTION_TOKEN=secret_your_token_here"
       },
       "filesystem-proxy": {
         "url": "http://localhost:8080/api/v1/mcp/proxy/filesystem/sse"
       }
     }
   }
   ```

3. **Claude Desktop 재시작**
   - Claude Desktop 완전히 종료
   - 다시 실행

### macOS

1. **설정 파일 위치**
   ```
   ~/Library/Application Support/Claude/claude_desktop_config.json
   ```

2. **설정 파일 편집**
   ```bash
   # 파일 열기
   open ~/Library/Application\ Support/Claude/claude_desktop_config.json

   # 또는 vim으로 편집
   vim ~/Library/Application\ Support/Claude/claude_desktop_config.json
   ```

   ```json
   {
     "mcpServers": {
       "notion-proxy": {
         "url": "http://localhost:8080/api/v1/mcp/proxy/notion/sse?NOTION_TOKEN=secret_your_token_here"
       },
       "filesystem-proxy": {
         "url": "http://localhost:8080/api/v1/mcp/proxy/filesystem/sse"
       }
     }
   }
   ```

3. **Claude Desktop 재시작**

## 📝 설정 예시

### ✅ **권장 방식: 통합 MCP 서버**

Claude Desktop이 우리 서버를 **하나의 통합 MCP 서버**로 인식하도록 설정합니다.
모든 커넥터(Notion, Filesystem, Memory 등)의 도구를 하나의 서버에서 제공합니다.

```json
{
  "mcpServers": {
    "mcp-logging": {
      "url": "http://localhost:8080/mcp/sse"
    }
  }
}
```

**중요 주의사항:**
- ⚠️ **반드시 `url` 필드를 사용**해야 합니다 (SSE 직접 연결)
- ❌ `command` + `args`를 사용하지 마세요 (STDIO 방식이며, `mcp-remote` 같은 중간 도구는 불필요합니다)

**장점:**
- ✅ 설정이 간단함 - 단 하나의 서버만 등록
- ✅ 모든 커넥터의 도구를 한 번에 사용
- ✅ 자동으로 모든 요청/응답 로깅
- ✅ 도구 이름에 커넥터 prefix가 자동으로 추가됨 (예: `notion__API-get-user`, `filesystem__read_file`)

### 대체 방식: 개별 커넥터 프록시 (고급)

각 MCP 서버를 개별적으로 프록시하고 싶은 경우:

```json
{
  "mcpServers": {
    "notion-proxy": {
      "url": "http://localhost:8080/api/v1/mcp/proxy/notion/sse?NOTION_TOKEN=secret_xxxxx"
    },
    "filesystem-proxy": {
      "url": "http://localhost:8080/api/v1/mcp/proxy/filesystem/sse"
    }
  }
}
```

## 🔍 사용 가능한 커넥터 확인

백엔드가 실행 중일 때, 사용 가능한 모든 커넥터와 SSE URL을 확인할 수 있습니다:

```bash
curl http://localhost:8080/api/v1/mcp/proxy/connectors
```

**응답 예시:**
```json
{
  "success": true,
  "connectors": [
    {
      "name": "notion",
      "description": "Notion MCP Server - 자체 호스팅 Notion MCP 서버 (STDIO)",
      "sseUrl": "http://localhost:8080/api/v1/mcp/proxy/notion/sse",
      "envTemplate": "{\"NOTION_TOKEN\": \"\"}"
    },
    {
      "name": "filesystem",
      "description": "Filesystem MCP Server - 파일 시스템 읽기/쓰기, 디렉토리 탐색 기능 제공",
      "sseUrl": "http://localhost:8080/api/v1/mcp/proxy/filesystem/sse",
      "envTemplate": "{}"
    }
  ]
}
```

## 📊 로깅 확인

### 1. 데이터베이스에서 확인

```sql
-- 모든 도구 사용 로그 조회
SELECT * FROM tool_usage_logs ORDER BY executed_at DESC;

-- 특정 도구의 로그만 조회
SELECT * FROM tool_usage_logs WHERE tool_name = 'tools/call' ORDER BY executed_at DESC;

-- JSON 요청/응답 확인
SELECT
    tool_name,
    mcp_request_raw,
    mcp_response_raw,
    executed_at
FROM tool_usage_logs
ORDER BY executed_at DESC
LIMIT 10;
```

### 2. H2 Console에서 확인

1. 브라우저에서 http://localhost:8080/h2-console 접속
2. JDBC URL: `jdbc:h2:file:./data/mcplog`
3. User Name: `sa`
4. Password: (비어있음)
5. Connect 클릭
6. SQL 쿼리 실행

### 3. 백엔드 로그 확인

```bash
# 실시간 로그 확인
tail -f backend/logs/application.log

# 또는 콘솔 출력 확인
# - SSE Proxy 연결 요청
# - JSON-RPC 메시지 수신
# - 도구 실행 로그
```

## 🔧 트러블슈팅

### 1. Claude Desktop에서 MCP 서버가 보이지 않음

**원인:**
- 백엔드 서버가 실행되지 않음
- 설정 파일 경로가 잘못됨
- JSON 형식 오류

**해결:**
```bash
# 백엔드 서버 상태 확인
curl http://localhost:8080/api/v1/mcp/proxy/status

# 설정 파일 JSON 유효성 검사
cat claude_desktop_config.json | python -m json.tool
```

### 2. 연결은 되는데 도구를 사용할 수 없음

**원인:**
- 환경 변수(API 토큰) 누락
- 잘못된 토큰

**해결:**
- URL에 올바른 환경 변수가 포함되어 있는지 확인
- 토큰이 유효한지 확인

### 3. 로그가 기록되지 않음

**원인:**
- 데이터베이스 연결 오류
- 트랜잭션 문제

**해결:**
```bash
# H2 Console에서 테이블 확인
SELECT COUNT(*) FROM tool_usage_logs;

# 백엔드 로그에서 에러 확인
grep ERROR backend/logs/application.log
```

## 📖 동작 원리

### 1. 연결 흐름

```
1. Claude Desktop이 SSE 엔드포인트로 연결
   GET /api/v1/mcp/proxy/{connectorName}/sse

2. 백엔드가 실제 MCP 서버에 연결
   - STDIO: npx 프로세스 실행
   - SSE: HTTP SSE 연결
   - Streamable HTTP: HTTP 스트림 연결

3. 세션 생성 및 관리
   - sessionId 생성
   - ProxySession에 저장
```

### 2. 메시지 처리 흐름

```
1. Claude Desktop이 JSON-RPC 메시지 전송
   POST /api/v1/mcp/proxy/{connectorName}/message

2. 백엔드가 메시지 로깅
   - MCP 요청 원본 저장
   - 메서드, 파라미터 파싱

3. 실제 MCP 서버로 메시지 전달
   - tools/list → McpSyncClient.listTools()
   - tools/call → McpSyncClient.callTool()

4. 응답 수신 및 로깅
   - MCP 응답 원본 저장
   - ToolUsageLog 테이블에 기록

5. Claude Desktop으로 응답 전송
   - JSON-RPC 형식으로 응답
```

## 🎯 다음 단계

1. **대시보드에서 로그 확인**
   - http://localhost:5174 (프론트엔드 실행 시)
   - 실시간으로 도구 사용 현황 모니터링

2. **분석 및 인사이트**
   - 어떤 도구가 가장 많이 사용되는지
   - 에러 발생 패턴 분석
   - API 사용량 통계

3. **추가 MCP 서버 연결**
   - Puppeteer, Memory 등 다른 서버도 동일한 방법으로 추가 가능
   - `/api/v1/mcp/proxy/connectors`에서 사용 가능한 서버 확인

## 📚 참고 자료

- [Claude Desktop 공식 문서](https://claude.ai/docs)
- [Model Context Protocol 문서](https://modelcontextprotocol.io)
- [MCP Servers 목록](https://github.com/modelcontextprotocol/servers)
