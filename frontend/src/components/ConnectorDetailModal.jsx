import React, { useState } from 'react';
import { X, Check, AlertCircle, Lock, Copy, CheckCircle } from 'lucide-react';
import { connectionService } from '../services/connectionService';

const ConnectorDetailModal = ({ connector, isOpen, onClose, onConnect }) => {
  const [envValues, setEnvValues] = useState({});
  const [isConnecting, setIsConnecting] = useState(false);
  const [error, setError] = useState('');
  const [copiedKey, setCopiedKey] = useState('');

  if (!isOpen || !connector) return null;

  const handleConnect = async () => {
    setError('');
    
    // 필수 환경변수 검증
    const requiredKeys = connector.envTemplateKeys || [];
    const missingKeys = requiredKeys.filter(key => !envValues[key]?.trim());
    
    if (missingKeys.length > 0) {
      setError(`다음 필수 설정을 입력해주세요: ${missingKeys.join(', ')}`);
      return;
    }

    try {
      setIsConnecting(true);

      // MCP 연결 생성 API 호출
      const connection = await connectionService.createConnection(connector.id, envValues);
      console.log('MCP 연결 성공:', connection);

      onConnect(connection);
      onClose();
    } catch (err) {
      console.error('MCP 연결 실패:', err);
      setError(err.message || '연결 중 오류가 발생했습니다');
    } finally {
      setIsConnecting(false);
    }
  };

  const handleCopyKey = (key) => {
    navigator.clipboard.writeText(key);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(''), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-hidden flex flex-col">
        {/* 헤더 */}
        <div className="px-6 py-5 border-b border-gray-200 flex items-start justify-between">
          <div className="flex items-start gap-4">
            <div className="w-14 h-14 bg-gradient-to-br from-orange-100 to-amber-100 
                          rounded-xl flex items-center justify-center text-3xl">
              {connector.icon || '🔌'}
            </div>
            <div>
              <h2 className="text-2xl font-semibold text-gray-900 mb-1">
                {connector.name}
              </h2>
              <p className="text-sm text-gray-600">
                {connector.category}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5 text-gray-500" />
          </button>
        </div>

        {/* 본문 */}
        <div className="flex-1 overflow-y-auto px-6 py-6">
          {/* 설명 */}
          <div className="mb-6">
            <h3 className="text-sm font-semibold text-gray-900 mb-2">설명</h3>
            <p className="text-sm text-gray-700 leading-relaxed">
              {connector.description}
            </p>
          </div>

          {/* 기능 */}
          {connector.features && connector.features.length > 0 && (
            <div className="mb-6">
              <h3 className="text-sm font-semibold text-gray-900 mb-3">주요 기능</h3>
              <ul className="space-y-2">
                {connector.features.map((feature, index) => (
                  <li key={index} className="flex items-start gap-2 text-sm text-gray-700">
                    <Check className="w-4 h-4 text-green-600 mt-0.5 flex-shrink-0" />
                    <span>{feature}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* 환경변수 설정 */}
          {connector.envTemplateKeys && connector.envTemplateKeys.length > 0 && (
            <div className="mb-6">
              <h3 className="text-sm font-semibold text-gray-900 mb-3">
                필수 설정 정보
              </h3>
              <div className="space-y-4">
                {connector.envTemplateKeys.map((key) => (
                  <div key={key}>
                    <div className="flex items-center justify-between mb-2">
                      <label className="text-sm font-medium text-gray-700 flex items-center gap-2">
                        {key}
                        {connector.envTemplate?.[key]?.required !== false && (
                          <span className="text-red-500">*</span>
                        )}
                      </label>
                      <button
                        onClick={() => handleCopyKey(key)}
                        className="text-xs text-gray-500 hover:text-gray-700 flex items-center gap-1"
                      >
                        {copiedKey === key ? (
                          <>
                            <CheckCircle className="w-3 h-3" />
                            <span>복사됨</span>
                          </>
                        ) : (
                          <>
                            <Copy className="w-3 h-3" />
                            <span>키 복사</span>
                          </>
                        )}
                      </button>
                    </div>
                    
                    <div className="relative">
                      <input
                        type={key.toLowerCase().includes('password') || 
                              key.toLowerCase().includes('secret') || 
                              key.toLowerCase().includes('token') ? 'password' : 'text'}
                        value={envValues[key] || ''}
                        onChange={(e) => setEnvValues({
                          ...envValues,
                          [key]: e.target.value
                        })}
                        placeholder={connector.envTemplate?.[key]?.placeholder || `${key} 입력...`}
                        className="w-full px-4 py-2.5 rounded-lg border border-gray-300 
                                 focus:outline-none focus:ring-2 focus:ring-orange-200 
                                 focus:border-orange-400 text-sm"
                      />
                      {(key.toLowerCase().includes('password') || 
                        key.toLowerCase().includes('secret') || 
                        key.toLowerCase().includes('token')) && (
                        <Lock className="absolute right-3 top-1/2 transform -translate-y-1/2 
                                       w-4 h-4 text-gray-400" />
                      )}
                    </div>
                    
                    {connector.envTemplate?.[key]?.description && (
                      <p className="mt-1 text-xs text-gray-500">
                        {connector.envTemplate[key].description}
                      </p>
                    )}
                  </div>
                ))}
              </div>

              {/* 안내 메시지 */}
              <div className="mt-4 p-4 bg-blue-50 rounded-lg">
                <div className="flex items-start gap-3">
                  <AlertCircle className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
                  <div className="text-sm text-blue-900">
                    <p className="font-medium mb-1">개인정보 보호 안내</p>
                    <p className="text-blue-700">
                      입력하신 정보는 안전하게 암호화되어 저장되며, 
                      커넥터 연결 목적으로만 사용됩니다.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* 에러 메시지 */}
          {error && (
            <div className="p-4 bg-red-50 rounded-lg border border-red-200">
              <div className="flex items-start gap-2">
                <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                <p className="text-sm text-red-800">{error}</p>
              </div>
            </div>
          )}
        </div>

        {/* 푸터 */}
        <div className="px-6 py-4 border-t border-gray-200 flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-100 
                     rounded-lg transition-colors"
          >
            취소
          </button>
          <button
            onClick={handleConnect}
            disabled={isConnecting}
            className="px-6 py-2.5 text-sm font-medium text-white bg-orange-500 
                     hover:bg-orange-600 rounded-lg transition-colors
                     disabled:opacity-50 disabled:cursor-not-allowed
                     flex items-center gap-2"
          >
            {isConnecting ? (
              <>
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                <span>연결 중...</span>
              </>
            ) : (
              <>
                <Check className="w-4 h-4" />
                <span>연결하기</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConnectorDetailModal;
