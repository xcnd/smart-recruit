---
name: api-development-workflow
description: smart-recruit 新增与修改 RESTful 接口的标准工作流，从需求分析、建表、分层代码到自检与接口文档的落地步骤。适用于在本项目各微服务中开发后端接口时。不用于纯前端任务、测试用例编写或项目整体架构调整。
---

# 接口开发工作流（smart-recruit）

适用于本仓库新增/修改一个后端接口的完整流程。具体编码细节遵循 `backend-coding-conventions` 技能。

## 第 1 步 需求与归属分析

- 明确业务归属模块：用 gateway 的 application.yml 路由前缀判断（如 `/api/v1/jobs` → recruitment、`/api/v1/interviews` → interview）。归属不明先确认，不要擅自跨模块落地。
- 若依赖他服务数据，先查阅目标服务的 `feign/` 客户端已有契约，禁止凭想象新建远程接口。

## 第 2 步 证据优先（必须）

- 参照同模块已存在的同类接口（Controller + ServiceImpl）保持写法与命名一致。
- 枚举值、错误码、字段名先查既有定义（`SystemEnums`、`InterviewEnums`、`dto/`、`entity/`），**禁止编造**不存在的枚举值与依赖。
- 涉及状态流转的，先确认已有枚举能否覆盖。

## 第 3 步 数据层

- 需要建表时先输出 DDL（含 `id` 主键、`create_time/update_time`、逻辑删除 `deleted`、业务索引）。**DDL 落地前必须征询用户同意**，未获批只输出执行计划。
- 新增 `Entity` 类与 Mapper（继承 `BaseMapper`）；复杂 SQL 写入 `resources/mapper/*.xml`。

## 第 4 步 服务层

- 定义 `XxxService` 接口 + `XxxServiceImpl` 实现：业务校验、装配、状态流转、feign 远程调用、抛业务异常均在此层。
- 出参结构使用 `dto/response` 下的 VO；跨服务入参用 `dto/remote`。

## 第 5 步 控制层

- 按项目风格编写：`@RestController` + `@RequestMapping("/api/v1/<资源>")` + `@RequiredArgsConstructor` + `@Slf4j`。
- 返回 `ApiResponse<T>`；分页用 `PageResult` + `Page<?>`；入参校验 `@Valid`；权限 `@PreAuthorize("hasAuthority('模块:动作')")`。

## 第 6 步 自检清单（提交前逐项核对）

- [ ] 返回体一律 `ApiResponse`，业务失败抛异常而非返回 null / 500。
- [ ] 入参校验完整，Service 层二次业务校验。
- [ ] 权限注解齐全，匿名接口已在 gateway 白名单配置。
- [ ] 关键节点日志 + 异常堆栈完整，无敏感信息泄露。
- [ ] 无跨服务中心直接 import，远程调用走 feign 客户端。
- [ ] 无硬编码魔数/状态串，已用枚举或常量。
- [ ] import 无通配符（Controller 的 web 注解批量导入除外）。

## 第 7 步 交付

- 输出最小可用 `curl` 示例（经网关 8080 的完整 URL）。
- 说明涉及的表变更与执行状态（DDL 未执行前仅在文档中说明，不落库）。
- 若项目有接口文档约定目录（如 `interface.md` / Knife4j），同步更新。