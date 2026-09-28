# 第 9 章：Spring Security 的职责

掌握深度：**理解应用**。先回顾：身份、功能权限、资源条件应分别在哪里取得？

## 概念摘要

Spring Security 提供认证、授权和常见 Web 防护的基础设施。它不会根据数据库表名自动理解你的部门、项目和文章规则。

| 组件或机制 | 本课程中的职责 |
|---|---|
| SecurityFilterChain | 定义请求经过的认证、会话、防护和访问检查 |
| Authentication | 表达当前身份、认证状态和权限信息 |
| SecurityContext / SecurityContextHolder | 在当前执行上下文中提供认证结果 |
| UserDetailsService | 根据用户名读取账号信息；不是完整的登录流程 |
| PasswordEncoder | 对保存的密码散列进行编码与匹配 |
| GrantedAuthority | 表达本次认证上下文中的权限字符串 |
| 请求级授权 | 公共入口放行，其他 API 要求登录，未知路径默认拒绝 |
| 方法级授权 | 在业务服务入口检查 article:review 等操作权限 |

## 理论与代码的对应关系

```text
Cookie 中的 Session 标识
  → Spring Security 读取认证上下文
  → RefreshIdentityFilter 从数据库刷新账号状态与权限
  → 请求级规则确认登录
  → @PreAuthorize 检查功能权限
  → ArticleService 将范围与项目关系加入数据库查询
  → 业务状态与自审检查
  → 修改并提交事务
```

`@PreAuthorize("hasAuthority('article:review')")` 只能回答是否有该操作权限，不能自动得知文章归属。`hasRole('ADMIN')` 在默认配置下会使用 ROLE_ 前缀，与直接检查业务权限字符串的 `hasAuthority` 不要混用。

方法级授权依赖 Spring 代理，本对象内部直接调用另一个带注解的方法通常不会经过同样的代理拦截。本项目让 Controller 调用独立 Service 的公开方法；不要复制成内部自调用后以为注解一定生效。

## Session 中的旧权限如何处理

参考项目保留 Spring Security 自带的表单登录和 Session 持久化。在之后的请求中创建新的请求上下文，使用刚读取的权限进行判断，不原地修改多个请求可能共享的上下文对象。

这是一种用于教学的明确实现，不是 Spring Security 自动保证所有应用权限实时刷新的承诺。

## 易错点

- 只引入依赖，就认为所有业务权限已经受保护。
- UserDetailsService 返回角色名称，但注解检查的是另一套权限编码。
- 把所有数据权限堆在一个通用过滤器里，忽略业务资源本身。
- 自定义登录时只设置上下文，忘记会话持久化和会话固定攻击防护；本项目使用框架登录流程避免重复实现这些职责。

## 自测

1. GrantedAuthority 中有 article:review，是否能审核任意文章？
2. 为什么还需要 ArticleService 的范围查询？
3. 框架是否会自动发现角色表发生了变更？

<details><summary>自测答案</summary>

1. 不能，还要满足资源范围、项目关系和业务约束。
2. 请求入口没有全部资源语义，具体业务需要把规则落实到查询与操作。
3. 不会，刷新和缓存失效需要应用明确设计。

</details>

## 本章验收

将一次请求的每个检查分配到框架、业务服务或数据库查询，说明理由。

参考：[Spring Security 6.5 架构](https://docs.spring.io/spring-security/reference/6.5/servlet/architecture.html)、[方法授权](https://docs.spring.io/spring-security/reference/6.5/servlet/authorization/method-security.html)。只查职责，不要求通读源码。

下一章：[理论综合设计](10-理论综合设计.md)。
