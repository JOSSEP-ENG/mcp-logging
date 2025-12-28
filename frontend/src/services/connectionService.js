/**
 * MCP Connection API Service
 * 실제 MCP 서버와의 연결을 관리하는 서비스
 */

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/**
 * API 에러 처리
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
    'X-User-Id': 'test-user', // TODO: 실제 사용자 ID로 변경 필요
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

    throw new ApiError(
      '서버에 연결할 수 없습니다. 네트워크 연결을 확인해주세요.',
      0,
      null
    );
  }
};

/**
 * MCP Connection Service
 */
export const connectionService = {
  /**
   * MCP 서버 연결 생성
   * @param {number} connectorId - 커넥터 ID
   * @param {Object} env - 환경변수 설정 (예: { NOTION_API_KEY: 'xxx' })
   * @returns {Promise<Object>} 연결 정보
   */
  createConnection: async (connectorId, env = {}) => {
    const response = await fetchWrapper('/connections', {
      method: 'POST',
      body: JSON.stringify({
        connectorId,
        env,
      }),
    });

    return response.data; // ApiResponse에서 data 추출
  },

  /**
   * MCP 서버 연결 해제
   * @param {number} connectionId - 연결 ID
   * @returns {Promise<void>}
   */
  disconnectConnection: async (connectionId) => {
    const response = await fetchWrapper(`/connections/${connectionId}`, {
      method: 'DELETE',
    });

    return response.data;
  },

  /**
   * 연결 상태 조회
   * @param {number} connectionId - 연결 ID
   * @returns {Promise<Object>} 연결 정보
   */
  getConnection: async (connectionId) => {
    const response = await fetchWrapper(`/connections/${connectionId}`, {
      method: 'GET',
    });

    return response.data;
  },

  /**
   * 사용자의 모든 연결 조회
   * @returns {Promise<Array>} 연결 목록
   */
  getUserConnections: async () => {
    const response = await fetchWrapper('/connections', {
      method: 'GET',
    });

    return response.data || [];
  },

  /**
   * 도구 목록 조회
   * @param {number} connectionId - 연결 ID
   * @returns {Promise<Object>} 도구 목록
   */
  listTools: async (connectionId) => {
    const response = await fetchWrapper(`/connections/${connectionId}/tools`, {
      method: 'GET',
    });

    return response.data;
  },

  /**
   * 도구 실행
   * @param {number} connectionId - 연결 ID
   * @param {string} toolName - 도구 이름
   * @param {Object} toolArgs - 도구 인자
   * @returns {Promise<Object>} 실행 결과
   */
  callTool: async (connectionId, toolName, toolArgs = {}) => {
    const response = await fetchWrapper(`/connections/${connectionId}/tools/call`, {
      method: 'POST',
      body: JSON.stringify({
        toolName,
        arguments: toolArgs,
      }),
    });

    return response.data;
  },
};

export default connectionService;
