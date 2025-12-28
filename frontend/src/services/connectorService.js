/**
 * Connector API Service
 * 백엔드 API와의 통신을 담당하는 서비스 레이어
 */

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/**
 * API 에러 처리 헬퍼 함수
 */
class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.status = status;
    this.data = data;
    this.name = 'ApiError';
  }
}

/**
 * Fetch 래퍼 함수
 */
const fetchWrapper = async (url, options = {}) => {
  const defaultHeaders = {
    'Content-Type': 'application/json',
  };

  const config = {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  };

  try {
    const response = await fetch(`${API_BASE_URL}${url}`, config);
    
    // 응답이 JSON이 아닐 수 있으므로 체크
    const contentType = response.headers.get('content-type');
    const isJson = contentType && contentType.includes('application/json');
    
    const data = isJson ? await response.json() : await response.text();

    if (!response.ok) {
      throw new ApiError(
        data.message || '요청 처리 중 오류가 발생했습니다',
        response.status,
        data
      );
    }

    return data;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    
    // 네트워크 오류 등
    throw new ApiError(
      '서버에 연결할 수 없습니다. 네트워크 연결을 확인해주세요.',
      0,
      null
    );
  }
};

/**
 * 커넥터 이름에 따른 아이콘 매핑
 */
const getConnectorIcon = (name) => {
  const iconMap = {
    'notion': '📝',
    'github': '🐙',
    'gitlab': '🦊',
    'slack': '💬',
    'google-drive': '📁',
    'google-calendar': '📅',
    'gmail': '📧',
    'trello': '📋',
    'jira': '📊',
    'confluence': '📚',
    'asana': '✅',
    'linear': '📐',
    'figma': '🎨',
    'discord': '🎮',
    'telegram': '✈️',
    'postgres': '🐘',
    'postgresql': '🐘',
    'mysql': '🐬',
    'mongodb': '🍃',
    'redis': '🔴',
    'sqlite': '💾',
    'docker': '🐳',
    'kubernetes': '☸️',
    'aws': '☁️',
    'gcp': '☁️',
    'azure': '☁️',
    'vercel': '▲',
    'netlify': '💚',
    'stripe': '💳',
    'twilio': '📞',
    'sendgrid': '📨',
  };

  const normalizedName = name.toLowerCase().trim();
  return iconMap[normalizedName] || '🔌';
};

/**
 * 백엔드 DTO를 프론트엔드 형식으로 변환
 * @param {Object} dto - 백엔드 McpConnectorDto
 * @returns {Object} 프론트엔드 형식의 커넥터 객체
 */
const mapConnectorDto = (dto) => {
  // type을 category로 매핑 (official -> productivity, custom -> development)
  const categoryMap = {
    'OFFICIAL': 'productivity',
    'CUSTOM': 'development'
  };

  // envTemplate JSON 파싱
  let envTemplate = {};
  let envTemplateKeys = [];

  if (dto.envTemplate) {
    try {
      envTemplate = typeof dto.envTemplate === 'string'
        ? JSON.parse(dto.envTemplate)
        : dto.envTemplate;
      envTemplateKeys = Object.keys(envTemplate);
    } catch (error) {
      console.warn('Failed to parse envTemplate:', error);
      envTemplate = {};
      envTemplateKeys = [];
    }
  }

  return {
    id: dto.id,
    name: dto.name,
    description: dto.description || '',
    icon: getConnectorIcon(dto.name), // 커넥터 이름에 따른 아이콘
    category: categoryMap[dto.type] || 'productivity',
    isConnected: false, // TODO: 실제 연결 상태는 McpConnection에서 조회 필요
    tags: [dto.type?.toLowerCase() || 'connector', dto.transportType?.toLowerCase() || 'stdio'],
    envTemplate: envTemplate, // 파싱된 환경변수 템플릿 객체
    envTemplateKeys: envTemplateKeys, // 환경변수 키 목록
    features: [], // 선택적 필드 (백엔드에서 제공하지 않음)
    // 원본 데이터 보존
    _original: {
      type: dto.type,
      transportType: dto.transportType,
      enabled: dto.enabled
    }
  };
};

/**
 * Connector API Service
 */
export const connectorService = {
  /**
   * 모든 커넥터 목록 조회
   * @returns {Promise<Array>} 커넥터 목록
   */
  getAllConnectors: async () => {
    const response = await fetchWrapper('/connectors', {
      method: 'GET',
    });

    // ApiResponse에서 data 필드 추출 및 매핑
    const connectors = response.data || [];
    return connectors.map(mapConnectorDto);
  },

  /**
   * 특정 커넥터 상세 정보 조회
   * @param {string} connectorId - 커넥터 ID
   * @returns {Promise<Object>} 커넥터 상세 정보
   */
  getConnectorById: async (connectorId) => {
    const response = await fetchWrapper(`/connectors/${connectorId}`, {
      method: 'GET',
    });

    // ApiResponse에서 data 필드 추출 및 매핑
    return mapConnectorDto(response.data);
  },

  /**
   * 커넥터 연결
   * @param {string} connectorId - 커넥터 ID
   * @param {Object} envConfig - 환경변수 설정
   * @returns {Promise<Object>} 연결 결과
   */
  connectConnector: async (connectorId, envConfig) => {
    return fetchWrapper(`/connectors/${connectorId}/connect`, {
      method: 'POST',
      body: JSON.stringify({ envConfig }),
    });
  },

  /**
   * 커넥터 연결 해제
   * @param {string} connectorId - 커넥터 ID
   * @returns {Promise<Object>} 연결 해제 결과
   */
  disconnectConnector: async (connectorId) => {
    return fetchWrapper(`/connectors/${connectorId}/disconnect`, {
      method: 'POST',
    });
  },

  /**
   * 커넥터 설정 업데이트
   * @param {string} connectorId - 커넥터 ID
   * @param {Object} envConfig - 새로운 환경변수 설정
   * @returns {Promise<Object>} 업데이트 결과
   */
  updateConnectorConfig: async (connectorId, envConfig) => {
    return fetchWrapper(`/connectors/${connectorId}/config`, {
      method: 'PUT',
      body: JSON.stringify({ envConfig }),
    });
  },

  /**
   * 커넥터 연결 테스트
   * @param {string} connectorId - 커넥터 ID
   * @returns {Promise<Object>} 테스트 결과
   */
  testConnection: async (connectorId) => {
    return fetchWrapper(`/connectors/${connectorId}/test`, {
      method: 'POST',
    });
  },

  /**
   * 카테고리별 커넥터 조회
   * @param {string} category - 카테고리명
   * @returns {Promise<Array>} 커넥터 목록
   */
  getConnectorsByCategory: async (category) => {
    return fetchWrapper(`/connectors?category=${category}`, {
      method: 'GET',
    });
  },

  /**
   * 검색어로 커넥터 검색
   * @param {string} query - 검색어
   * @returns {Promise<Array>} 검색 결과
   */
  searchConnectors: async (query) => {
    return fetchWrapper(`/connectors/search?q=${encodeURIComponent(query)}`, {
      method: 'GET',
    });
  },
};

/**
 * React Query를 사용하는 경우의 커스텀 훅 예시
 */
/*
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

export const useConnectors = () => {
  return useQuery({
    queryKey: ['connectors'],
    queryFn: connectorService.getAllConnectors,
  });
};

export const useConnector = (connectorId) => {
  return useQuery({
    queryKey: ['connector', connectorId],
    queryFn: () => connectorService.getConnectorById(connectorId),
    enabled: !!connectorId,
  });
};

export const useConnectConnector = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ connectorId, envConfig }) => 
      connectorService.connectConnector(connectorId, envConfig),
    onSuccess: () => {
      // 커넥터 목록 새로고침
      queryClient.invalidateQueries({ queryKey: ['connectors'] });
    },
  });
};

export const useDisconnectConnector = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (connectorId) => 
      connectorService.disconnectConnector(connectorId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['connectors'] });
    },
  });
};
*/

/**
 * 환경변수 템플릿 유틸리티 함수들
 * Java의 EnvTemplateUtil과 대응
 */
export const envTemplateUtils = {
  /**
   * JSON 문자열을 객체로 파싱
   * @param {string} envTemplateJson - JSON 문자열
   * @returns {Object} 파싱된 객체
   */
  parseEnvTemplate: (envTemplateJson) => {
    if (!envTemplateJson || envTemplateJson.trim() === '') {
      return {};
    }

    try {
      return JSON.parse(envTemplateJson);
    } catch (error) {
      console.error('Failed to parse envTemplate JSON:', error);
      return {};
    }
  },

  /**
   * 객체를 JSON 문자열로 변환
   * @param {Object} envTemplateMap - 환경변수 객체
   * @returns {string} JSON 문자열
   */
  toJsonString: (envTemplateMap) => {
    if (!envTemplateMap || Object.keys(envTemplateMap).length === 0) {
      return '{}';
    }

    try {
      return JSON.stringify(envTemplateMap);
    } catch (error) {
      console.error('Failed to convert envTemplate to JSON:', error);
      return '{}';
    }
  },

  /**
   * 필수 환경변수 키 목록 추출
   * @param {string} envTemplateJson - JSON 문자열
   * @returns {Array<string>} 키 목록
   */
  getRequiredKeys: (envTemplateJson) => {
    const map = envTemplateUtils.parseEnvTemplate(envTemplateJson);
    return Object.keys(map);
  },

  /**
   * JSON 형식 유효성 검증
   * @param {string} envTemplateJson - JSON 문자열
   * @returns {boolean} 유효하면 true
   */
  isValidJson: (envTemplateJson) => {
    if (!envTemplateJson || envTemplateJson.trim() === '') {
      return true; // null이나 빈 문자열은 허용
    }

    try {
      JSON.parse(envTemplateJson);
      return true;
    } catch (error) {
      return false;
    }
  },

  /**
   * 사용자 설정이 템플릿과 일치하는지 검증
   * @param {string} templateJson - 템플릿 JSON
   * @param {string} userConfigJson - 사용자 설정 JSON
   * @returns {boolean} 모든 필수 키가 포함되어 있으면 true
   */
  validateUserConfig: (templateJson, userConfigJson) => {
    const template = envTemplateUtils.parseEnvTemplate(templateJson);
    const userConfig = envTemplateUtils.parseEnvTemplate(userConfigJson);

    // 템플릿의 모든 키가 사용자 설정에 있는지 확인
    for (const key of Object.keys(template)) {
      if (!userConfig.hasOwnProperty(key)) {
        console.warn('Missing required environment variable:', key);
        return false;
      }
    }

    return true;
  },

  /**
   * 빈 값을 가진 키들 추출
   * @param {string} envTemplateJson - 템플릿 JSON
   * @returns {Array<string>} 빈 값을 가진 키 목록
   */
  getEmptyKeys: (envTemplateJson) => {
    const map = envTemplateUtils.parseEnvTemplate(envTemplateJson);
    return Object.entries(map)
      .filter(([_, value]) => !value || value.trim() === '')
      .map(([key, _]) => key);
  },
};

/**
 * 로컬 스토리지 유틸리티
 */
export const storageUtils = {
  /**
   * 커넥터 설정을 로컬 스토리지에 저장
   * (개발 환경에서만 사용 권장)
   */
  saveConnectorConfig: (connectorId, config) => {
    try {
      localStorage.setItem(
        `connector_config_${connectorId}`,
        JSON.stringify(config)
      );
    } catch (error) {
      console.error('Failed to save connector config:', error);
    }
  },

  /**
   * 로컬 스토리지에서 커넥터 설정 불러오기
   */
  loadConnectorConfig: (connectorId) => {
    try {
      const config = localStorage.getItem(`connector_config_${connectorId}`);
      return config ? JSON.parse(config) : null;
    } catch (error) {
      console.error('Failed to load connector config:', error);
      return null;
    }
  },

  /**
   * 로컬 스토리지에서 커넥터 설정 삭제
   */
  removeConnectorConfig: (connectorId) => {
    try {
      localStorage.removeItem(`connector_config_${connectorId}`);
    } catch (error) {
      console.error('Failed to remove connector config:', error);
    }
  },
};

export default connectorService;
