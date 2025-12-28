# 프로젝트 구조

```
project-root/
│
├── public/
│   └── index.html
│
├── src/
│   ├── components/
│   │   ├── ConnectorBrowserComplete.jsx    # 메인 커넥터 브라우저 컴포넌트
│   │   └── ConnectorDetailModal.jsx        # 커넥터 상세 정보 모달
│   │
│   ├── services/
│   │   └── connectorService.js             # API 서비스 레이어
│   │
│   ├── App.jsx                              # 앱 진입점
│   ├── index.js                             # React DOM 렌더링
│   └── index.css                            # Tailwind CSS 및 전역 스타일
│
├── .env.example                             # 환경변수 예시
├── tailwind.config.js                       # Tailwind 설정
├── postcss.config.js                        # PostCSS 설정
├── package.json
└── README.md

```

## 📁 파일 설명

### 핵심 컴포넌트

#### 1. ConnectorBrowserComplete.jsx
- **역할**: 커넥터 목록을 보여주는 메인 화면
- **주요 기능**:
  - 커넥터 목록 표시
  - 검색 및 필터링
  - 카테고리별 분류
  - 통계 대시보드
  - 연결 상태 관리
- **의존성**: ConnectorDetailModal, lucide-react

#### 2. ConnectorDetailModal.jsx
- **역할**: 커넥터 상세 정보 및 연결 설정
- **주요 기능**:
  - 커넥터 상세 정보 표시
  - 환경변수 입력 폼
  - 연결/연결 해제
  - 입력 검증
  - 에러 처리
- **의존성**: lucide-react

### 서비스 레이어

#### 3. connectorService.js
- **역할**: 백엔드 API 통신 관리
- **주요 기능**:
  - API 요청 래핑
  - 에러 처리
  - 환경변수 유틸리티
  - 로컬 스토리지 관리
- **엔드포인트**:
  ```
  GET    /api/connectors              # 전체 목록
  GET    /api/connectors/:id          # 상세 정보
  POST   /api/connectors/:id/connect  # 연결
  POST   /api/connectors/:id/disconnect # 연결 해제
  PUT    /api/connectors/:id/config   # 설정 업데이트
  POST   /api/connectors/:id/test     # 연결 테스트
  ```

### 스타일 파일

#### 4. index.css
- **역할**: Tailwind CSS 및 전역 스타일
- **포함 내용**:
  - Tailwind 디렉티브
  - 커스텀 유틸리티 클래스
  - 커스텀 컴포넌트 스타일
  - 애니메이션 정의
  - 스크롤바 커스터마이징
  - 반응형 폰트 설정

#### 5. tailwind.config.js
- **역할**: Tailwind CSS 설정
- **커스터마이징**:
  - Orange/Amber 색상 팔레트
  - 애니메이션 정의
  - 그림자 스타일
  - 컨텐츠 경로 설정

### 앱 구조

#### 6. App.jsx
- **역할**: 애플리케이션 진입점
- **사용 예시**:
  - 단독 사용
  - React Router와 함께 사용
  - 네비게이션 바와 함께 사용

## 🔌 백엔드 API 요구사항

### 응답 데이터 구조

#### Connector 객체
```typescript
interface Connector {
  id: string;
  name: string;
  description: string;
  icon?: string;
  category: 'productivity' | 'communication' | 'storage' | 'database' | 'development';
  isConnected: boolean;
  tags?: string[];
  envTemplateKeys?: string[];
  envTemplate?: {
    [key: string]: {
      required?: boolean;
      placeholder?: string;
      description?: string;
    }
  };
  features?: string[];
}
```

#### API 응답 예시

**GET /api/connectors**
```json
[
  {
    "id": "google-drive",
    "name": "Google Drive",
    "description": "Google Drive의 파일과 폴더에 접근합니다",
    "icon": "📁",
    "category": "storage",
    "isConnected": true,
    "tags": ["클라우드", "문서"],
    "envTemplateKeys": ["GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET"],
    "envTemplate": {
      "GOOGLE_CLIENT_ID": {
        "required": true,
        "placeholder": "클라이언트 ID 입력",
        "description": "Google Cloud Console에서 발급"
      },
      "GOOGLE_CLIENT_SECRET": {
        "required": true,
        "placeholder": "시크릿 키 입력",
        "description": "Google Cloud Console에서 발급"
      }
    },
    "features": [
      "파일 검색 및 조회",
      "문서 생성 및 수정",
      "폴더 관리"
    ]
  }
]
```

**POST /api/connectors/:id/connect**

요청:
```json
{
  "envConfig": {
    "GOOGLE_CLIENT_ID": "your-client-id",
    "GOOGLE_CLIENT_SECRET": "your-client-secret"
  }
}
```

응답:
```json
{
  "success": true,
  "message": "커넥터가 성공적으로 연결되었습니다",
  "connector": {
    "id": "google-drive",
    "isConnected": true
  }
}
```

## 🚀 빠른 시작

### 1. 프로젝트 생성
```bash
npx create-react-app my-connector-app
cd my-connector-app
```

### 2. 파일 복사
생성된 파일들을 적절한 위치에 복사:
- `ConnectorBrowserComplete.jsx` → `src/components/`
- `ConnectorDetailModal.jsx` → `src/components/`
- `connectorService.js` → `src/services/`
- `App.jsx` → `src/`
- `index.css` → `src/`
- `tailwind.config.js` → 프로젝트 루트

### 3. 의존성 설치
```bash
npm install lucide-react
npm install -D tailwindcss postcss autoprefixer @tailwindcss/line-clamp
npx tailwindcss init -p
```

### 4. 환경변수 설정
`.env` 파일 생성:
```
REACT_APP_API_URL=http://localhost:8080/api
```

### 5. 개발 서버 실행
```bash
npm start
```

## 🎨 커스터마이징 가이드

### 색상 변경
`tailwind.config.js`에서 색상 팔레트 수정:
```javascript
theme: {
  extend: {
    colors: {
      primary: {
        // 원하는 색상으로 변경
      }
    }
  }
}
```

### 카테고리 추가
`ConnectorBrowserComplete.jsx`에서 categories 배열 수정:
```javascript
const categories = [
  { id: 'all', label: '전체' },
  { id: 'new-category', label: '새 카테고리' }, // 추가
];
```

### API 엔드포인트 변경
`connectorService.js`에서 API_BASE_URL 수정

## 📊 데이터 흐름

```
사용자 액션
    ↓
ConnectorBrowserComplete (상태 관리)
    ↓
connectorService (API 호출)
    ↓
백엔드 API
    ↓
응답 데이터
    ↓
UI 업데이트
```

## 🔧 개발 팁

1. **목업 데이터 사용**
   - 백엔드가 준비되지 않았다면 `getMockConnectors()` 함수 활용

2. **React Query 통합**
   - 캐싱과 상태 관리를 위해 React Query 사용 권장
   - `connectorService.js`에 커스텀 훅 예시 포함

3. **에러 처리**
   - ApiError 클래스를 활용한 일관된 에러 처리
   - 사용자 친화적인 에러 메시지 표시

4. **보안**
   - 민감한 정보는 환경변수로 관리
   - API 토큰은 절대 클라이언트에 노출하지 않기

5. **성능 최적화**
   - React.memo로 불필요한 리렌더링 방지
   - 가상 스크롤링 구현 고려 (많은 커넥터가 있을 경우)

## 🐛 트러블슈팅

### 일반적인 문제

1. **Tailwind 스타일이 적용되지 않음**
   - `tailwind.config.js`의 content 경로 확인
   - PostCSS 설정 확인
   - 캐시 삭제 후 재시작

2. **API 연결 오류**
   - CORS 설정 확인
   - 환경변수 확인
   - 네트워크 탭에서 요청 확인

3. **아이콘이 표시되지 않음**
   - lucide-react 설치 확인
   - import 경로 확인

## 📝 체크리스트

### 배포 전 확인사항
- [ ] 환경변수 설정 완료
- [ ] API 엔드포인트 확인
- [ ] 에러 처리 구현
- [ ] 로딩 상태 처리
- [ ] 반응형 테스트
- [ ] 브라우저 호환성 테스트
- [ ] 접근성 검사
- [ ] 성능 최적화

## 🔗 유용한 링크

- [Tailwind CSS 문서](https://tailwindcss.com/docs)
- [Lucide Icons](https://lucide.dev/)
- [React 문서](https://react.dev/)
- [Fetch API](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API)
