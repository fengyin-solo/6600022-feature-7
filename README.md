# 棋类 AI 对弈与棋谱回放系统

五子棋（Gobang / Five-in-a-row）AI 对弈与棋谱回放系统，支持人机对弈、棋谱记录和回放。

## 技术栈

### 前端
- **Vue 3** + **TypeScript** - 响应式 UI 框架
- **Vite** - 构建工具
- **Pinia** - 状态管理
- **Tailwind CSS** - 暗色主题样式
- **Canvas API** - 棋盘渲染

### 后端
- **Java 17** + **Spring Boot 3.2.0**
- **Spring Web** - REST API
- **Spring WebSocket** - 实时通信
- **Lombok** - 简化代码

## 功能特性

- 15×15 标准五子棋棋盘
- Minimax + Alpha-Beta 剪枝 AI（可调节搜索深度 1-4）
- Canvas 渲染棋子与最后落子标记
- 棋谱自动记录与回放
- 回放支持：逐步前进/后退、自动播放、速度调节
- AI 执黑/执白可选，也可关闭 AI 进行双人对弈

## 快速启动

### 前端
```bash
cd frontend
npm install
npm run dev
```

### 后端
```bash
cd backend
mvn spring-boot:run
```

前端默认运行在 `http://localhost:5182`，后端运行在 `http://localhost:8080`。
