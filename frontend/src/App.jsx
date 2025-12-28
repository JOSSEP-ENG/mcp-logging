import React from 'react';
import ConnectorBrowserComplete from './components/ConnectorBrowserComplete';
import './index.css';

/**
 * 메인 애플리케이션 컴포넌트
 * 
 * 사용 방법:
 * 1. ConnectorBrowserComplete를 직접 렌더링 (전체 페이지)
 * 2. 라우터와 함께 사용 (특정 경로에 마운트)
 */
function App() {
  return (
    <div className="App">
      {/* 옵션 1: 전체 페이지로 사용 */}
      <ConnectorBrowserComplete />
      
      {/* 옵션 2: 레이아웃과 함께 사용 */}
      {/* 
      <Layout>
        <Navbar />
        <ConnectorBrowserComplete />
        <Footer />
      </Layout>
      */}
    </div>
  );
}

export default App;

/**
 * React Router와 함께 사용하는 예시
 */
/*
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import ConnectorBrowserComplete from './components/ConnectorBrowserComplete';
import Home from './pages/Home';
import Dashboard from './pages/Dashboard';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/connectors" element={<ConnectorBrowserComplete />} />
      </Routes>
    </Router>
  );
}

export default App;
*/

/**
 * 네비게이션 바와 함께 사용하는 예시
 */
/*
import { useState } from 'react';
import ConnectorBrowserComplete from './components/ConnectorBrowserComplete';

const Navbar = ({ onNavigate }) => (
  <nav className="bg-white border-b border-gray-200 px-6 py-4">
    <div className="max-w-7xl mx-auto flex items-center justify-between">
      <div className="flex items-center gap-6">
        <h1 className="text-xl font-bold text-gray-900">MCP Logging</h1>
        <button 
          onClick={() => onNavigate('home')}
          className="text-gray-600 hover:text-gray-900"
        >
          홈
        </button>
        <button 
          onClick={() => onNavigate('connectors')}
          className="text-gray-600 hover:text-gray-900"
        >
          커넥터
        </button>
        <button 
          onClick={() => onNavigate('logs')}
          className="text-gray-600 hover:text-gray-900"
        >
          로그
        </button>
      </div>
    </div>
  </nav>
);

function App() {
  const [currentPage, setCurrentPage] = useState('connectors');

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar onNavigate={setCurrentPage} />
      
      <main>
        {currentPage === 'connectors' && <ConnectorBrowserComplete />}
        {currentPage === 'home' && <div>홈 페이지</div>}
        {currentPage === 'logs' && <div>로그 페이지</div>}
      </main>
    </div>
  );
}

export default App;
*/
