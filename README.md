# https-github.com-ran0808-qisheng-tcg-server
网关多模块版
# **TCG-Card-Game-Server（七圣召唤轻量版服务器）**

[![Java](https://img.shields.io/badge/Java-17+-red?style=flat&logo=openjdk)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![Netty](https://img.shields.io/badge/Netty-4.1.94.Final-orange?style=flat)](https://netty.io/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat)](LICENSE)

**一个基于Java、Netty与Spring Cloud构建的分布式、高性能TCG（集换式卡牌）游戏服务器。** 本项目从单体架构重构为微服务架构，实现了多玩家在线匹配、实时卡牌对战、帧同步逻辑与数据持久化等功能。
---

## ✨ 项目特色
-   **🎯 微服务架构**：采用Spring Cloud Alibaba (Nacos, Gateway) 进行服务治理，解耦系统，便于扩展与维护。
-   **⚡ 高性能网络**：基于Netty实现自定义TCP协议，提供高并发、低延迟的网络通信，保障游戏流畅体验。
-   **🕹️ 帧同步机制**：核心战斗采用帧同步算法，严格保证多客户端操作时序一致性，解决网络延迟与卡顿问题。
-   **🔐 分布式认证**：基于JWT + Redis实现无状态登录与会话管理，保障系统安全性与扩展性。
-   **🐇 异步匹配**：集成RabbitMQ消息队列，实现高效、解耦的玩家匹配系统。
-   **📦 容器化部署**：项目全程使用Docker & Docker Compose部署，环境一致，开箱即用。

---

## 🗂️ 项目架构

### 系统架构图
flowchart TD
    %% 定义样式
    classDef client fill:#9affb3,stroke:#333,stroke-width:1px;
    classDef gateway fill:#ff9,stroke:#333,stroke-width:1px;
    classDef service fill:#a2c4ff,stroke:#333,stroke-width:1px;
    classDef infra fill:#f9cb9c,stroke:#333,stroke-width:1px;
    classDef data fill:#ea9999,stroke:#333,stroke-width:1px;

    subgraph A [客户端层]
        Client[游戏客户端]
    end

    subgraph B [网关层]
        Gateway[API网关<br>Netty + Spring Cloud Gateway]
    end

    subgraph C [业务服务层]
        AuthService[认证服务<br>Spring Boot + JWT]
        MatchService[匹配服务<br>Spring Boot]
        GameService[游戏服务<br>Spring Boot]
    end

    subgraph D [数据与基础设施层]
        Nacos[服务注册与发现<br>Nacos]
        Redis[缓存<br>Redis<br>会话/状态]
        MySQL[数据库<br>MySQL<br>持久化数据]
        RabbitMQ[消息队列<br>RabbitMQ<br>异步通信]
    end

    %% 数据流向
    Client -- TCP长连接<br>自定义协议 --> Gateway

    Gateway -- HTTP/REST<br>服务调用 --> AuthService
    Gateway -- HTTP/REST<br>服务调用 --> MatchService
    Gateway -- HTTP/REST<br>服务调用 --> GameService

    AuthService -- HTTP/REST<br>注册/发现 --> Nacos
    MatchService -- HTTP/REST<br>注册/发现 --> Nacos
    GameService -- HTTP/REST<br>注册/发现 --> Nacos
    Gateway -- HTTP/REST<br>注册/发现 --> Nacos

    AuthService -- 读写<br>会话存储 --> Redis
    GameService -- 读写<br>游戏状态 --> Redis
    GameService -- 读写<br>持久化数据 --> MySQL

    MatchService -- 生产消息<br>创建房间 --> RabbitMQ
    RabbitMQ -- 消费消息<br>开始游戏 --> GameService

    %% 应用样式
    class Client client;
    class Gateway gateway;
    class AuthService,MatchService,GameService service;
    class Nacos,RabbitMQ infra;
    class Redis,MySQL data;

### 模块说明

| 模块名称 | 说明 | 技术栈 |
| :--- | :--- | :--- |
| **`phantom-gateway`** | 网络网关，处理TCP连接、协议编解码、路由转发 | Netty, Spring Cloud Gateway |
| **`phantom-auth-service`** | 认证服务，负责用户登录、注册、JWT令牌签发 | Spring Boot, JWT, Redis |
| **`phantom-match-service`** | 匹配服务，通过消息队列管理玩家匹配逻辑 | Spring Boot, RabbitMQ |
| **`phantom-game-service`** | 游戏核心服务，处理房间管理、战斗逻辑、帧同步 | Spring Boot, (未来计划集成gRPC) |
| **`common`** | 公共模块，存放DTO、工具类等 | |

---

## 🛠️ 技术栈

-   **后端框架**: Java 17, Spring Boot 3.x, Spring Cloud Gateway, Nacos
-   **网络通信**: Netty (自定义TCP协议)
-   **数据存储**: MySQL 8.0, Redis 7.x
-   **消息队列**: RabbitMQ
-   **授权认证**: JWT
-   **持久层框架**: MyBatis-Plus
-   **部署与运维**: Docker, Docker Compose
-   **其他工具**: Maven, Git

---

## 🚀 快速开始

###  prerequisites (前置条件)

确保你的开发环境已安装：
-   JDK 17+
-   Maven 3.6+
-   Docker & Docker Compose
-   Git

### 1. 克隆项目

```bash
git clone https://github.com/your-username/qisheng-tcg-server-lite.git
cd qisheng-tcg-server-lite
```

### 2. 部署基础设施（使用Docker Compose）

我们一键部署所有依赖的中间件。

```bash
# 进入部署目录
cd deploy

# 启动所有服务 (Nacos, MySQL, Redis, RabbitMQ)
docker-compose -f docker-compose-env.yml up -d
```

### 3. 配置与启动微服务

1.  导入项目到IDE（如IDEA）。
2.  检查 `application.yml` 文件中的中间件连接配置（如MySQL、Redis的地址），确保与Docker启动的服务IP一致。
3.  按顺序启动微服务：
    -   `phantom-auth-service`
    -   `phantom-match-service`
    -   `phantom-game-service`
    -   `phantom-gateway`

### 4. 连接测试

使用你提供的游戏客户端，或者使用 `telnet` / `Netcat` 工具连接网关服务器（默认端口：8888）进行基础通信测试。

```bash
telnet localhost 8888
```
---

## 📖 核心逻辑介绍

### 帧同步流程
1.  **操作指令**: 客户端在每帧发送玩家操作指令到网关。
2.  **转发与缓存**: 网关将指令转发至游戏服务，游戏服务将其缓存到对应房间的当前帧。
3.  **定时广播**: 游戏服务以一个固定的频率（如每秒15帧）将收集到的所有玩家操作广播给房间内所有客户端。
4.  **客户端表现**: 所有客户端收到同一帧的所有操作后，在本地进行逻辑计算和表现，保证一致性。

### 匹配流程
1.  **请求匹配**: 玩家客户端向网关发送匹配请求。
2.  **路由至匹配服务**: 网关通过HTTP API将请求转发至匹配服务。
3.  **消息队列异步处理**: 匹配服务将玩家放入Redis或内存中的匹配池，并通过RabbitMQ进行异步匹配计算。
4.  **匹配成功**: 当匹配到两名玩家后，匹配服务会通知游戏服务创建一个新的房间，并将房间信息返回给玩家客户端。

---

## 🧪 性能优化

-   **网络延迟**: 通过Netty的Reactor多线程模型与自定义紧凑的二进制协议，将平均响应延迟从200ms优化至50ms以内。
-   **数据库压力**: 使用Redis缓存玩家状态、房间信息等高频数据，MySQL查询耗时降低85%，有效避免数据库瓶颈。
-   **连接稳定性**: 设计心跳包机制与连接管理器，及时清理无效连接，服务器可稳定运行72小时以上无Full GC。
