# 커넥터 브라우저 - Claude 스타일 UI

Claude와 동일한 디자인으로 구현된 커넥터 목록 및 관리 화면입니다.

## 📸 주요 기능

- ✨ Claude 스타일의 깔끔하고 모던한 UI
- 🔍 실시간 검색 및 카테고리 필터링
- 📊 커넥터 통계 대시보드
- 🔌 커넥터 상세 정보 모달
- ⚙️ 환경변수 설정 인터페이스
- 🔐 보안 정보 입력 (비밀번호 타입 자동 감지)
- ✅ 연결 상태 관리
- 📱 반응형 디자인 (모바일, 태블릿, 데스크톱)

## 🚀 설치 방법

### 1. 의존성 설치

```bash
npm install lucide-react
# 또는
yarn add lucide-react
```

### 2. Tailwind CSS 설정

#### package.json에 추가
```json
{
  "devDependencies": {
    "tailwindcss": "^3.4.0",
    "@tailwindcss/line-clamp": "^0.4.4",
    "autoprefixer": "^10.4.16",
    "postcss": "^8.4.32"
  }
}
```

#### tailwind.config.js 설정
제공된 `tailwind.config.js` 파일을 프로젝트 루트에 복사합니다.

#### postcss.config.js 생성
```javascript
module.exports = {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
}
```

#### CSS 파일에 Tailwind 추가 (src/index.css)
```css
@tailwind base;
@tailwind components;
@tailwind utilities;

/* 스크롤바 숨김 유틸리티 */
.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.scrollbar-hide::-webkit-scrollbar {
  display: none;
}

/* 부드러운 트랜지션 */
* {
  @apply transition-colors duration-200;
}
```

## 📦 컴포넌트 구조

```
src/
├── components/
│   ├── ConnectorBrowserComplete.jsx  # 메인 컴포넌트
│   └── ConnectorDetailModal.jsx      # 상세 정보 모달
├── index.css                          # Tailwind CSS
└── App.jsx                            # 앱 엔트리포인트
```

## 💻 사용 방법

### App.jsx에 통합

```jsx
import React from 'react';
import ConnectorBrowserComplete from './components/ConnectorBrowserComplete';

function App() {
  return (
    <div className="App">
      <ConnectorBrowserComplete />
    </div>
  );
}

export default App;
```

### API 연동

컴포넌트는 다음 API 엔드포인트를 사용합니다:

#### 1. 커넥터 목록 조회
```
GET /api/connectors
```

**응답 예시:**
```json
[
  {
    "id": "1",
    "name": "Google Drive",
    "description": "Google Drive의 문서, 스프레드시트에 접근",
    "icon": "📁",
    "category": "storage",
    "isConnected": true,
    "tags": ["클라우드", "문서관리"],
    "envTemplateKeys": ["GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET"],
    "envTemplate": {
      "GOOGLE_CLIENT_ID": {
        "required": true,
        "placeholder": "클라이언트 ID 입력",
        "description": "Google Cloud Console에서 발급받은 클라이언트 ID"
      },
      "GOOGLE_CLIENT_SECRET": {
        "required": true,
        "placeholder": "클라이언트 시크릿 입력",
        "description": "Google Cloud Console에서 발급받은 클라이언트 시크릿"
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

#### 2. 커넥터 연결
```
POST /api/connectors/{connectorId}/connect
```

**요청 바디:**
```json
{
  "envConfig": {
    "GOOGLE_CLIENT_ID": "your-client-id",
    "GOOGLE_CLIENT_SECRET": "your-client-secret"
  }
}
```

**응답 예시:**
```json
{
  "success": true,
  "message": "커넥터가 성공적으로 연결되었습니다",
  "connector": {
    "id": "1",
    "isConnected": true
  }
}
```

## 🎨 디자인 특징

### Claude 스타일 적용 요소

1. **색상 팔레트**
   - Primary: Orange (#f97316)
   - Secondary: Amber (#f59e0b)
   - Background: Gradient from amber-50 to orange-50
   - Text: Gray scale

2. **타이포그래피**
   - 헤더: 4xl, semibold
   - 서브헤더: lg, regular
   - 본문: sm-base, regular

3. **컴포넌트**
   - 둥근 모서리 (rounded-xl, rounded-2xl)
   - 부드러운 그림자
   - 호버 효과 (scale, translate, shadow)
   - 애니메이션 트랜지션

4. **인터랙션**
   - 부드러운 호버 애니메이션
   - 명확한 피드백 (버튼, 입력 필드)
   - 모달 백드롭 블러

## 🔧 커스터마이징

### 카테고리 추가

```javascript
const categories = [
  { id: 'all', label: '전체' },
  { id: 'custom', label: '커스텀 카테고리' }, // 추가
  // ...
];
```

### 목업 데이터 변경

개발 환경에서 백엔드 없이 테스트하려면 `getMockConnectors()` 함수를 수정하세요:

```javascript
const getMockConnectors = () => [
  {
    id: '1',
    name: '나만의 커넥터',
    description: '커스텀 설명',
    icon: '🎯',
    category: 'custom',
    isConnected: false,
    tags: ['태그1', '태그2'],
    envTemplateKeys: ['API_KEY'],
    features: ['기능1', '기능2']
  }
];
```

## 🎯 주요 컴포넌트 Props

### ConnectorBrowserComplete
메인 컴포넌트로 props 없이 독립적으로 동작합니다.

### ConnectorDetailModal
```typescript
{
  connector: Connector | null,      // 선택된 커넥터 정보
  isOpen: boolean,                  // 모달 표시 여부
  onClose: () => void,              // 모달 닫기 핸들러
  onConnect: (result) => void       // 연결 성공 핸들러
}
```

## 📱 반응형 브레이크포인트

- **Mobile**: < 768px (1 column)
- **Tablet**: 768px - 1024px (2 columns)
- **Desktop**: > 1024px (3 columns)

## 🔒 보안 고려사항

1. **환경변수 암호화**: 비밀번호, 토큰 등은 자동으로 password 타입으로 처리
2. **키 복사 기능**: 클립보드 API 사용
3. **입력 검증**: 필수 필드 검증
4. **에러 처리**: 사용자 친화적인 에러 메시지

## 🐛 문제 해결

### Tailwind 스타일이 적용되지 않는 경우
1. `tailwind.config.js`의 content 경로 확인
2. CSS 파일에 @tailwind 디렉티브 포함 확인
3. 개발 서버 재시작

### Lucide 아이콘이 표시되지 않는 경우
```bash
npm install lucide-react --save
# 또는
yarn add lucide-react
```

### API 연결 오류
1. CORS 설정 확인
2. API 엔드포인트 URL 확인
3. 네트워크 탭에서 요청/응답 확인

## 📝 라이선스

MIT License

## 🤝 기여

이슈와 PR은 언제나 환영합니다!
