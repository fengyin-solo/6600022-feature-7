# 五子棋 AI 后端

基于 Spring Boot 3.2.0 的五子棋 AI 对弈服务端。

## 技术栈
- Java 17
- Spring Boot 3.2.0
- Spring Web (REST API)
- Spring WebSocket
- Lombok

## 环境要求
- JDK 17+
- Maven 3.8+

## 启动
```bash
cd backend
mvn spring-boot:run
```

服务默认运行在 `http://localhost:8080`。

## API 接口

| 方法   | 路径                  | 说明         |
|--------|-----------------------|--------------|
| POST   | `/api/game/new`       | 创建新游戏   |
| GET    | `/api/game/{id}`      | 获取游戏状态 |
| POST   | `/api/game/{id}/move` | 玩家落子     |
| POST   | `/api/game/{id}/ai-move` | AI 落子   |
| GET    | `/api/game/records`   | 获取棋谱记录 |
| DELETE | `/api/game/{id}`      | 删除游戏     |

## AI 实现
- Minimax 算法 + Alpha-Beta 剪枝
- 默认搜索深度: 3
- 评估函数: 基于连子数(2/3/4/5连)和开放端数计算权重
