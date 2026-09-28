# 权限管理 Java 参考项目

这是第 11～12 章的最终参考实现。请先完成 [第 10 章理论设计](../章节/10-理论综合设计.md)，再逐步运行和阅读。项目包含 RBAC、简单属性判断和项目成员关系，不是通用权限引擎。

## 环境与运行

- Java **21**；项目固定 Spring Boot **3.5.14**，由它管理 Spring Security 6.5 系列依赖。
- Maven Wrapper 已包含，无需预装 Maven。首次运行需要网络下载 Maven 和依赖。
- H2 文件数据库自动创建在 `data/`，无需安装数据库。
- 本地地址为 `http://127.0.0.1:8080`，无管理页面，使用接口工具。

在本目录执行：

```sh
java -version
./mvnw test
./mvnw spring-boot:run
```

Windows 使用 `mvnw.cmd`。如果默认 Java 不是 21，请在 IDE 将项目 SDK 与 Maven Runner JRE 都选为 Java 21，或为运行命令设置 `JAVA_HOME`。项目创建时本机默认 Java 为 27 预览版，不能把它视为本项目已验证的运行环境。

看到日志“学习项目已就绪：数据初始化完成，可以开始登录练习。”后再登录。框架的 `Started` 日志可能先于演示数据初始化完成。

H2 使用文件保存进度。需要重做初始练习时，**先停止服务**，把本目录的 `data/` 改名备份，再启动；新数据库会重新初始化。不要在服务运行时移动数据库文件。

## 初始数据

仅当账号表为空时初始化。所有演示账号初始密码为 `Learn-RBAC-2026!`，可在第一次启动前用 `LAB_DEMO_PASSWORD` 指定其他密码。之后改变环境变量不会修改已有用户密码。

| 账号 | 部门 | 角色与范围 | 项目 |
|---|---|---|---|
| admin | 10 | ADMIN / ALL | 共同项目、独立项目 |
| alice | 10 | AUTHOR / OWN | 共同项目 |
| bob | 10 | EDITOR / DEPARTMENT | 共同项目 |
| carol | 20 | AUTHOR / OWN | 共同项目、独立项目 |

| 角色 | 权限 |
|---|---|
| AUTHOR | article:read、article:create、article:update、article:delete |
| EDITOR | article:read、article:create、article:update、article:review |
| ADMIN | 所有已定义权限，包含 user:manage、role:manage、permission:read、project:manage、audit:read |

初始有四篇待审核文章：Alice 待审核文章、Bob 自己的文章、Carol 跨部门文章（共同项目）、Carol 独立项目文章。请求示例通过查询结果取得 ID，不假设数据库 ID 永远不变。

## 登录和 CSRF：先做这四步

1. `GET /api/auth/csrf`，保存响应 Cookie、`token` 和 `headerName`。
2. `POST /api/auth/login`，以表单格式发送 username、password，并带上同一会话 Cookie 和 CSRF 请求头。
3. 登录成功后再次 `GET /api/auth/csrf`，更新 token；保存登录后的新 Session Cookie。
4. 后续修改请求发送 Cookie 和更新后的 CSRF 头；退出使用 `POST /api/auth/logout`。

[requests.http](requests.http) 可用 IntelliJ HTTP Client 执行，会自动保存 Cookie，并通过响应脚本存储 ID 和 CSRF token。Postman、Apifox 等工具可按相同流程手动调用。更换账号会替换当前工具的登录身份；验证“保持用户原会话、由另一管理员撤权”时，请使用两个独立 Cookie 容器或阅读自动化测试。

不要为了请求方便关闭 CSRF。未携带 CSRF 的修改请求可能在认证判断前收到 403，这是预期防护行为。

## 接口与请求格式

除公开 CSRF 入口和登录处理入口外，API 均需登录；管理和文章方法还要求相应权限。带请求体的管理与文章接口使用 JSON；登录使用表单。

| 方法与路径 | 权限 | 请求体或作用 |
|---|---|---|
| GET /api/auth/csrf | 公开 | 获取 CSRF token |
| POST /api/auth/login | 公开但要求 CSRF | username、password 表单 |
| POST /api/auth/logout | CSRF | 退出并失效会话 |
| GET /api/auth/me | 登录 | 当前身份、角色 ID 和有效权限 |
| GET /api/users | user:manage | 用户列表，不返回密码散列 |
| POST /api/users | user:manage | username、password、departmentId |
| PUT /api/users/{id} | user:manage | departmentId、enabled |
| PUT /api/users/{id}/roles | user:manage | roleIds 数组；替换全部角色，空数组表示解除 |
| GET /api/roles | role:manage | 查询角色和权限配置 |
| POST /api/roles | role:manage | code、name、scope |
| PUT /api/roles/{id} | role:manage | name、enabled、scope；code 不变 |
| PUT /api/roles/{id}/permissions | role:manage | permissionCodes 数组；替换全部权限 |
| DELETE /api/roles/{id} | role:manage | 有用户引用时拒绝删除 |
| GET /api/permissions | permission:read | 后端预定义的权限编码字典 |
| GET /api/projects | project:manage | 项目列表 |
| POST /api/projects | project:manage | name |
| PUT /api/projects/{projectId}/members/{userId} | project:manage | 添加成员，无请求体，重复添加不重复记录 |
| DELETE /api/projects/{projectId}/members/{userId} | project:manage | 移除成员，无请求体 |
| GET /api/articles?page=0&size=20 | article:read | 范围内的分页结果 |
| GET /api/articles/{id} | article:read | 同范围内的文章详情 |
| POST /api/articles | article:create | title、content、projectId |
| PUT /api/articles/{id} | article:update | title、content |
| DELETE /api/articles/{id} | article:delete | 删除待审核文章 |
| POST /api/articles/{id}/review | article:review | 审核为 APPROVED，无请求体 |
| GET /api/audits?page=0&size=20 | audit:read | 成功的授权变更和文章操作记录 |

`scope` 为 OWN、DEPARTMENT、ALL。用户名只接受字母、数字、下划线和连字符，角色 code 为大写字母、数字和下划线。创建用户的密码至少 12 个字符，最多 64 个字符且不超过 72 个 UTF-8 字节。

分页响应形状：`{"items": [...], "total": 2, "page": 0, "size": 20}`，页码从 0 开始，size 为 1～100。普通错误体包含 `message`。

- 200/201/204：成功、创建成功、无响应体的成功。
- 400：请求格式、未知字段、参数或引用 ID 不合法。
- 401：未认证、登录失败或账号已停用。
- 403：缺少操作权限、非项目成员创建、自审或 CSRF 校验失败。
- 404：文章不存在或不在当前操作可访问范围；两者不作区分。
- 409：重复编码、角色仍有引用、文章状态不允许或并发写冲突。

## 具体授权规则

1. Session 维护身份，每次请求刷新账号状态和有效角色权限；账号停用后原会话不能继续使用。
2. 有效角色的允许权限取并集。某操作的数据范围只来自授予该操作的角色。
3. OWN 表示本人文章；DEPARTMENT 表示文章记录的部门与当前用户部门相同；ALL 不限制此维度。
4. 文章列表、详情、修改、删除和审核还必须满足项目成员关系。ALL 也不绕过项目关系。
5. 作者和文章部门由创建时的服务端账号信息确定；客户端不能伪造。用户之后换部门，不会自动改变既有文章的部门。
6. 新文章直接进入 PENDING；PENDING 才能修改、删除和审核。审核禁止本人，成功后为 APPROVED。
7. 使用事务和版本号检测文章并发写冲突。授权撤销保证下一次请求读取新值，不承诺中断已经通过校验的在途操作。
8. 角色停用保留配置与分配，但不贡献权限；角色被用户引用时不能删除。

## 代码阅读顺序

1. `UserAccount`、`AppRole`、`AppPermission`：对照五张表。
2. `ApiTypes`、`ApiController`、`AdminService`：观察输入、输出与角色管理流程。
3. `SecurityConfig`、`IdentityService`：登录、权限刷新和身份数据。
4. `ArticleService`：操作对应范围、项目关系和业务约束。
5. `AuthorizationIntegrationTest`：真实登录、CSRF 和拒绝场景。

为了便于学习，小项目使用一个包和包内可见的实体字段，通过 DTO 隔离 HTTP 接口；未引入 Lombok 或复杂分层框架。Repository 接口集中在 `Repositories.java` 中。

## 测试与学习边界

`./mvnw test` 使用独立内存数据库，不改变本地 `data/`。测试覆盖登录/退出、CSRF、自行提权、本人和部门范围、成员撤销、角色停用、权限撤销、多角色范围绑定、角色引用删除、参数验证、部门变更及审核状态。

测试使用真实认证和数据库，不依赖模拟用户。已排除未使用的 Mockito 依赖，避免无关的动态 Java agent 附加要求。

这是本地教学项目：自动建表、演示账号和 H2 文件库服务于练习。当前没有实现密码重置、生产部署、数据库迁移、拒绝事件监控、租户隔离或分布式授权。管理员能调整自己的角色，因此练习时不要给唯一管理员撤销全部管理权限；若已误操作，可停止服务并备份数据库后重新初始化。

项目实现简单 RBAC、ABAC 条件和直接项目关系；不包含通用策略引擎、MAC 强制标签系统、完整 DAC 分享或多级团队关系推导，这些内容按课程约定停留在理论层。
