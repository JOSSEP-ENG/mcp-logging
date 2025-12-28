import React, { useState, useEffect } from 'react';
import { Search, Plus, ChevronRight, Check, Settings, Filter } from 'lucide-react';
import ConnectorDetailModal from './ConnectorDetailModal';
import { connectorService } from '../services/connectorService';

const ConnectorBrowserComplete = () => {
  const [connectors, setConnectors] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [loading, setLoading] = useState(true);
  const [selectedConnector, setSelectedConnector] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [showConnectedOnly, setShowConnectedOnly] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchConnectors();
  }, []);

  const fetchConnectors = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await connectorService.getAllConnectors();
      setConnectors(data);
    } catch (err) {
      console.error('Failed to fetch connectors:', err);
      setConnectors([]);
      setError(err.message || '커넥터 목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleConnectorClick = (connector) => {
    setSelectedConnector(connector);
    setIsModalOpen(true);
  };

  const handleConnect = async (result) => {
    // 연결 성공 시 커넥터 목록 새로고침
    await fetchConnectors();
  };

  const filteredConnectors = connectors.filter(connector => {
    const matchesSearch = connector.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         connector.description?.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory = selectedCategory === 'all' || connector.category === selectedCategory;
    const matchesConnected = !showConnectedOnly || connector.isConnected;
    return matchesSearch && matchesCategory && matchesConnected;
  });

  const categories = [
    { id: 'all', label: '전체', count: connectors.length },
    { id: 'productivity', label: '생산성', count: connectors.filter(c => c.category === 'productivity').length },
    { id: 'communication', label: '커뮤니케이션', count: connectors.filter(c => c.category === 'communication').length },
    { id: 'storage', label: '스토리지', count: connectors.filter(c => c.category === 'storage').length },
    { id: 'database', label: '데이터베이스', count: connectors.filter(c => c.category === 'database').length },
    { id: 'development', label: '개발', count: connectors.filter(c => c.category === 'development').length },
  ];

  const stats = {
    total: connectors.length,
    connected: connectors.filter(c => c.isConnected).length,
    available: connectors.filter(c => !c.isConnected).length,
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-amber-50 via-white to-orange-50">
      <div className="max-w-7xl mx-auto px-6 py-8">
        {/* 헤더 */}
        <div className="mb-8">
          <div className="flex items-start justify-between mb-4">
            <div>
              <h1 className="text-4xl font-semibold text-gray-900 mb-3">
                커넥터 둘러보기
              </h1>
              <p className="text-lg text-gray-600">
                다양한 서비스를 연결하여 기능을 확장하세요
              </p>
            </div>

            {/* 통계 */}
            <div className="flex gap-4">
              <div className="bg-white rounded-xl px-5 py-3 border border-gray-200 shadow-sm">
                <div className="text-sm text-gray-600 mb-1">전체</div>
                <div className="text-2xl font-bold text-gray-900">{stats.total}</div>
              </div>
              <div className="bg-green-50 rounded-xl px-5 py-3 border border-green-200">
                <div className="text-sm text-green-700 mb-1">연결됨</div>
                <div className="text-2xl font-bold text-green-900">{stats.connected}</div>
              </div>
            </div>
          </div>
        </div>

        {/* 검색 및 필터 */}
        <div className="mb-8 space-y-4">
          {/* 검색바 */}
          <div className="flex gap-3">
            <div className="relative flex-1">
              <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
              <input
                type="text"
                placeholder="커넥터 검색..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-12 pr-4 py-3.5 rounded-xl border border-gray-200 
                         focus:outline-none focus:ring-2 focus:ring-orange-200 focus:border-orange-400
                         text-gray-900 placeholder-gray-400 bg-white shadow-sm
                         transition-all duration-200"
              />
            </div>

            {/* 필터 토글 */}
            <button
              onClick={() => setShowConnectedOnly(!showConnectedOnly)}
              className={`px-5 py-3.5 rounded-xl font-medium transition-all duration-200 flex items-center gap-2
                ${showConnectedOnly
                  ? 'bg-orange-500 text-white shadow-lg shadow-orange-200'
                  : 'bg-white text-gray-700 border border-gray-200 hover:bg-gray-50'
                }`}
            >
              <Filter className="w-5 h-5" />
              <span>연결된 항목만</span>
            </button>
          </div>

          {/* 카테고리 필터 */}
          <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-hide">
            {categories.map(category => (
              <button
                key={category.id}
                onClick={() => setSelectedCategory(category.id)}
                className={`px-4 py-2.5 rounded-lg font-medium whitespace-nowrap transition-all duration-200
                  flex items-center gap-2
                  ${selectedCategory === category.id
                    ? 'bg-orange-100 text-orange-900 shadow-sm ring-2 ring-orange-200'
                    : 'bg-white text-gray-600 hover:bg-gray-50 border border-gray-200'
                  }`}
              >
                <span>{category.label}</span>
                <span className={`text-xs px-2 py-0.5 rounded-full
                  ${selectedCategory === category.id
                    ? 'bg-orange-200 text-orange-800'
                    : 'bg-gray-100 text-gray-600'
                  }`}>
                  {category.count}
                </span>
              </button>
            ))}
          </div>
        </div>

        {/* 필터 결과 표시 */}
        {(searchQuery || selectedCategory !== 'all' || showConnectedOnly) && (
          <div className="mb-6 flex items-center justify-between">
            <p className="text-sm text-gray-600">
              <span className="font-semibold text-gray-900">{filteredConnectors.length}개</span>의 커넥터 찾음
            </p>
            {(searchQuery || selectedCategory !== 'all' || showConnectedOnly) && (
              <button
                onClick={() => {
                  setSearchQuery('');
                  setSelectedCategory('all');
                  setShowConnectedOnly(false);
                }}
                className="text-sm text-orange-600 hover:text-orange-700 font-medium"
              >
                필터 초기화
              </button>
            )}
          </div>
        )}

        {/* 에러 메시지 */}
        {error && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-xl">
            <div className="flex items-start gap-3">
              <div className="flex-shrink-0">
                <svg className="w-5 h-5 text-red-600" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                </svg>
              </div>
              <div className="flex-1">
                <h3 className="text-sm font-medium text-red-800 mb-1">오류 발생</h3>
                <p className="text-sm text-red-700">{error}</p>
                <button
                  onClick={fetchConnectors}
                  className="mt-2 text-sm text-red-800 font-medium hover:text-red-900 underline"
                >
                  다시 시도
                </button>
              </div>
            </div>
          </div>
        )}

        {/* 커넥터 그리드 */}
        {loading ? (
          <LoadingSkeleton />
        ) : filteredConnectors.length === 0 ? (
          <EmptyState 
            searchQuery={searchQuery}
            onReset={() => {
              setSearchQuery('');
              setSelectedCategory('all');
              setShowConnectedOnly(false);
            }}
          />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {filteredConnectors.map(connector => (
              <ConnectorCard 
                key={connector.id} 
                connector={connector}
                onClick={() => handleConnectorClick(connector)}
              />
            ))}
          </div>
        )}
      </div>

      {/* 상세 모달 */}
      <ConnectorDetailModal
        connector={selectedConnector}
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onConnect={handleConnect}
      />
    </div>
  );
};

const ConnectorCard = ({ connector, onClick }) => {
  const [isHovered, setIsHovered] = useState(false);

  return (
    <div
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      onClick={onClick}
      className="bg-white rounded-xl p-6 border border-gray-200 hover:border-orange-300 
               hover:shadow-xl transition-all duration-300 cursor-pointer group
               transform hover:-translate-y-1"
    >
      <div className="flex items-start justify-between mb-4">
        <div className="w-12 h-12 bg-gradient-to-br from-orange-100 to-amber-100 
                      rounded-lg flex items-center justify-center text-2xl
                      group-hover:scale-110 transition-transform duration-300
                      shadow-sm">
          {connector.icon || '🔌'}
        </div>

        {connector.isConnected ? (
          <div className="flex items-center gap-1.5 px-3 py-1.5 bg-green-50 text-green-700 
                        rounded-full text-sm font-medium border border-green-200">
            <Check className="w-4 h-4" />
            <span>연결됨</span>
          </div>
        ) : (
          <div className="flex items-center gap-1.5 px-3 py-1.5 bg-orange-50 text-orange-700 
                        rounded-full text-sm font-medium border border-orange-200
                        group-hover:bg-orange-100 transition-colors">
            <Plus className="w-4 h-4" />
            <span>연결</span>
          </div>
        )}
      </div>

      <div className="mb-4">
        <h3 className="text-lg font-semibold text-gray-900 mb-2 group-hover:text-orange-600 transition-colors">
          {connector.name}
        </h3>
        <p className="text-sm text-gray-600 line-clamp-2 leading-relaxed">
          {connector.description}
        </p>
      </div>

      {connector.envTemplateKeys && connector.envTemplateKeys.length > 0 && (
        <div className="mb-4 pt-4 border-t border-gray-100">
          <div className="flex items-center gap-2 text-xs text-gray-500">
            <Settings className="w-3.5 h-3.5" />
            <span>필수 설정: {connector.envTemplateKeys.length}개</span>
          </div>
        </div>
      )}

      <div className="flex items-center justify-between pt-4 border-t border-gray-100">
        <div className="flex items-center gap-2 flex-wrap">
          {connector.tags?.slice(0, 2).map((tag, index) => (
            <span key={index} className="px-2.5 py-1 bg-gray-100 text-gray-600 rounded-md text-xs font-medium">
              {tag}
            </span>
          ))}
        </div>
        
        <ChevronRight 
          className={`w-5 h-5 text-gray-400 transition-all duration-300 
                    ${isHovered ? 'translate-x-1 text-orange-500' : ''}`}
        />
      </div>
    </div>
  );
};

const LoadingSkeleton = () => (
  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
    {[1, 2, 3, 4, 5, 6].map(i => (
      <div key={i} className="bg-white rounded-xl p-6 border border-gray-200 animate-pulse">
        <div className="flex items-start justify-between mb-4">
          <div className="w-12 h-12 bg-gray-200 rounded-lg"></div>
          <div className="w-20 h-7 bg-gray-200 rounded-full"></div>
        </div>
        <div className="h-6 bg-gray-200 rounded mb-3 w-3/4"></div>
        <div className="h-4 bg-gray-200 rounded mb-2"></div>
        <div className="h-4 bg-gray-200 rounded w-5/6"></div>
        <div className="mt-6 pt-4 border-t border-gray-100">
          <div className="h-4 bg-gray-200 rounded w-1/2"></div>
        </div>
      </div>
    ))}
  </div>
);

const EmptyState = ({ searchQuery, onReset }) => (
  <div className="text-center py-20">
    <div className="w-20 h-20 bg-gradient-to-br from-gray-100 to-gray-200 
                  rounded-full flex items-center justify-center mx-auto mb-6">
      <Search className="w-10 h-10 text-gray-400" />
    </div>
    <h3 className="text-xl font-semibold text-gray-900 mb-2">
      검색 결과가 없습니다
    </h3>
    <p className="text-gray-600 mb-6 max-w-md mx-auto">
      {searchQuery 
        ? `"${searchQuery}"에 대한 검색 결과를 찾을 수 없습니다.`
        : '선택한 필터 조건에 맞는 커넥터가 없습니다.'
      }
    </p>
    <button
      onClick={onReset}
      className="px-6 py-3 bg-orange-500 text-white rounded-lg 
               hover:bg-orange-600 transition-colors font-medium"
    >
      모든 커넥터 보기
    </button>
  </div>
);


export default ConnectorBrowserComplete;
