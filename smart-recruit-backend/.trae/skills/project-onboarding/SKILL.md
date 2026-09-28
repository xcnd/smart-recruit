---
name: project-onboarding
description: 帮助新成员快速上手 smart-recruit 微服务项目的技能，输出 9 个模块职责地图、本地启动步骤与关键代码入口。适用于新人入职、陌生模块接手或项目冷启动排查时。不用于具体业务接口的编码实现。
---

# 项目速览 · 上手引导

smart-recruit 是企业级 AI 驱动智能招聘平台（Spring Cloud Alibaba 微服务 + 多智能体 AI 引擎），仓库位于 `smart-recruit-backend`。本技能用于让新人快速建立全局认知并跑通本地环境。

## 1. 技术栈（以根 pom.xml 为准）

- Java 25（启用 preview 特性，编译参数 `--enable-preview`）
- Spring Boot 4.1.0 + Spring Cloud 2025.1.2 + Spring Cloud Alibaba 2025.1.0.0（Nacos 注册中心与配置中心）
- MyBatis-Plus 3.5.16 + MySQL（mysql-connector-j 9.2.0）
- Lombok 1.18.38 / Hutool 5.8.34 / MapStruct 1.6.3 / Knife4j 4.5.0 / JJWT 0.12.6 / Redisson 3.44.0 / Caffeine 3.2.1
- AI 能力：LangChain4j 0.29.1 + AgentScope2 2.0.0（仅 ai-engine 模块使用）

## 2. 模块地图（9 个 Maven 模块）

| 模块 | 职责 | 网关路由前缀 |
|------|------|--------------|
| smart-recruit-common | 基础库：ApiResponse/PageResult/BaseDTO、异常体系、工具类、通用枚举与配置。无独立服务端口 | - |
| smart-recruit-gateway | 统一入口（端口 8080）：路由、JWT 认证、限流、CORS、统一响应。 | 全部 |
| smart-recruit-system | 用户/认证/角色/部门/权限/通知/文件/配置/审计日志 | /api/v1/auth、/users、/roles、/departments、/permissions、/notifications、/files、/configs、/audit-logs |
| smart-recruit-recruitment | 职位/简历/候选人/招聘门户 | /api/v1/jobs、/resumes、/candidates、/careers、*-statistics |
| smart-recruit-interview | 面试/测评/在线评估/题库 | /api/v1/interviews、/assessments、/question-banks、/public/assessment |
| smart-recruit-offer | Offer/入职/合同 | /api/v1/offers、/onboarding、/contracts、/public/* |
| smart-recruit-talent | 人才库/数据分析/仪表盘/招聘活动 | /api/v1/talent-pool、/analytics、/dashboard、/campaigns |
| smart-recruit-referral | 内推 | /api/v1/referrals |
| smart-recruit-ai-engine | AI 智能体/流水线/大模型适配/简历解析 | /api/v1/agents、/api/v2/agents |

完整路由映射见 `smart-recruit-gateway/src/main/resources/application.yml`。

## 3. 外部依赖

- **Nacos**: 117.72.88.11:8848，namespace=`smart-recruit`，discovery.group=`${COMPUTER_ID}`。多人共用时必须设置**每人唯一**的 `COMPUTER_ID`，否则服务实例串群、调用到别人的本地实例。
- **Redis**: 117.72.88.11:6379（gateway 默认用 database 3）
- **MySQL**: 默认本机 3306，root/123456
- **大模型**: `QWEN_API_KEY` / `DEEPSEEK_API_KEY`（AI 类功能）

## 4. 本地启动步骤（10 分钟上手）

1. 在仓库根目录执行 `cp .env.example .env`，在 `.env` 中设置唯一 `COMPUTER_ID`（必填）。
2. 确认本机可连通 Nacos / Redis / MySQL。
3. 根目录执行 `mvn clean install -DskipTests`，先构建 common 等基础模块（依赖 flatten 插件，CI 友好）。
4. 用 IDE 或 `mvn spring-boot:run` 启动目标服务，以及必须的 gateway。
5. 验证：请求 `http://localhost:8080/actuator/health` 返回 UP；再通过网关调通一个最小接口。

## 5. 关键代码入口速查

- 网关路由与鉴权白名单：`smart-recruit-gateway/src/main/resources/application.yml`
- 统一返回体：`smart-recruit-common/.../dto/ApiResponse.java`、`PageResult.java`、`BaseDTO.java`
- 全局异常处理：`smart-recruit-common/.../exception/GlobalExceptionHandler.java`
- 跨服务远程客户端（feign）：各服务的 `feign/` 包，如 `smart-recruit-interview/.../feign/AiEngineClient.java`
- 各服务启动类：`xxxServiceApplication.java`（如 `InterviewServiceApplication.java`）

## 6. 定位代码方法论

1. 从请求 URL 拿到路由前缀，在 gateway 的 application.yml 找到归属服务。
2. 服务内沿 Controller → Service/Impl → Repository(Mapper) → `resources/mapper/*.xml`（SQL）逐层下钻。
3. 跨服务数据一律走 feign client，禁止跨模块直接 import 业务类。
4. 状态与枚举：各服务 `enums/` 包（如 `InterviewEnums`、`SystemEnums`），统一 code + label + fromCode。