---
name: backend-coding-conventions
description: smart-recruit 后端统一编码规范，覆盖分层结构、统一返回体、异常体系、参数校验、权限、日志与命名约定。适用于编写或审查本项目 Controller、Service、DTO、Mapper 等后端代码时。不用于非本项目代码或通用编程问答。
---

# 后端编码规范（smart-recruit）

以 `smart-recruit-common` 为基准约定的平台级规范，所有 9 个模块必须一致遵守。

## 1. 分层与包结构

- 基础包：`com.smartrecruit.<module>`（common 例外，为 `com.smartrecruit.common`）。
- 模块内目录：`config` / `controller` / `service`(接口) + `service/impl`(实现) / `repository` / `entity` / `dto/request` / `dto/response` / `dto/remote`(跨服务 DTO) / `enums` / `feign` / `task` / `util`。
- 依赖方向：Controller → Service → Repository → Entity。禁止跨微服务中心直接 import 业务类，远程调用走 `feign/` 下的客户端。

## 2. 统一返回体（强制）

- 所有 Controller 方法返回 `ApiResponse<T>`，HTTP 状态码固定 200，业务成败由 `success` 与 `code` 表达。
- 成功：`ApiResponse.success(data)` / `ApiResponse.success(message, data)` / `ApiResponse.success()`。
- 失败：抛业务异常，由 `GlobalExceptionHandler` 统一转成 `ApiResponse`（HTTP 200 + 业务 code），不要在 Controller 里直接返回 `ApiResponse.error`。
- 分页列表：返回 `ApiResponse<PageResult<T>>`，用 `PageResult.from(IPage<T>)` 组装；入参 `page`(默认1) + `size`(默认20)。
- 空结果返回 `success(null)` 或空 `PageResult`，禁止返回 null 响应体。

## 3. 异常体系（强制）

- 只抛 `com.smartrecruit.common.exception` 下的异常。
- 基类 `BusinessException`（sealed），子类：`ResourceNotFoundException`、`ValidationException`、`AuthenticationException`、`ForbiddenException`、`DuplicateResourceException`。
- 抛异常时传**错误码字符串** + 面向用户的可读 message，例如 `throw new ResourceNotFoundException("RESOURCE_NOT_FOUND", "面试不存在")`。
- 业务错误码映射（`ApiResponse.BusinessErrorCodeMapper`）：
  - 40001~40099 认证：40001 INVALID_CREDENTIALS、40002 AUTHENTICATION_FAILED、40003 TOKEN_EXPIRED、40004 TOKEN_MISSING、40029 RATE_LIMITED
  - 40101~40199 权限：40101 FORBIDDEN、40102 ACCESS_DENIED
  - 40201~40299 资源：40201 RESOURCE_NOT_FOUND、40202 USER_NOT_FOUND
  - 40301~40399 校验：40301 VALIDATION_FAILED、40302 CONSTRAINT_VIOLATION、40303 METHOD_VALIDATION_FAILED
  - 40901 DUPLICATE_RESOURCE；50001 未知内部错误、50002 GATEWAY_TIMEOUT、50003 BAD_GATEWAY
- 禁止 catch 后吞异常；兜底提示为「发生了未预期的错误，请稍后重试。」

## 4. 参数校验

- 入参 DTO 上使用 `jakarta.validation` 注解，Controller 参数标注 `@Valid`。
- 复杂业务校验放在 Service 层，失败抛业务异常，不要只在 Controller 校验。
- 校验失败由全局处理器统一映射为 40301/40302/40303。

## 5. 权限控制

- 方法级：`@PreAuthorize("hasAuthority('模块:动作')")`，如 `hasAuthority('interview:view')`。
- 或使用 `com.smartrecruit.common.annotation.RequirePermission` 注解。
- 允许匿名访问的接口在 gateway 的 `auth-skip` 配置中声明。

## 6. DTO / VO / Entity 约定

- DTO 优先使用 Java `record`；带审计字段的可继承 `BaseDTO.AuditableDTO`（id/createTime/updateTime/createBy/updateBy/deleted）。
- 分类明确：请求入参放 `dto/request`，出参 VO放 `dto/response`，跨服务 DTO 放 `dto/remote`。
- 时间字段统一 `LocalDateTime`，JSON 输出格式 `yyyy-MM-dd HH:mm:ss`。

## 7. 枚举约定

- 各服务 `enums/` 包（如 `InterviewEnums`、`SystemEnums`、`AiEnums`）。
- 每个枚举三件套：`code`(int) + `label`(中文名称) + `fromCode(Integer)` 静态查找方法。
- 数据库仅存 code，禁止硬编码魔法数字；新增状态先看是否已有枚举。

## 8. 日志

- 类上加 `@Slf4j`。
- 关键节点记录：方法入口关键参数、关键业务决策、耗时；异常必须打印堆栈（`log.error(..., ex)`）。
- 禁止打印明文密码、密钥、完整 token。

## 9. 数据库访问

- Mapper 继承 `BaseMapper<Entity>`，复杂 SQL 写在 `resources/mapper/*.xml`。
- 字段 snake_case 映射数据库；逻辑删除字段 `deleted`（0 未删 / 1 已删）由 MyBatis-Plus 处理。

## 10. 其他约定

- 允许使用 Java 25 新特性：record、sealed class、switch 表达式。
- import 置于文件头，禁止通配符 import（Controller 批量导入 `org.springframework.web.bind.annotation.*` 除外）。
- 硬编码状态串/文案落入枚举或常量类；统一用工厂方法构造响应体，不 `new` 响应对象。