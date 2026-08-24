# 03 · 权限模型（入口）

> **教材正文在 [`models/`](./models/)。**  
> 本页只做导航与一句话速查。  
> 建议：先读完 [02-model-evolution.md](./02-model-evolution.md)，再按 [models/README.md](./models/README.md) 精读专章。

## 学法

1. [02-model-evolution.md](./02-model-evolution.md) 建立地图  
2. [models/00-overview.md](./models/00-overview.md) → 主线 01…05 → 旁支 06…07 → [08-comparison.md](./models/08-comparison.md)  
3. 每章做自测；全部读完默写对照表  
4. 返回 [04-considerations.md](./04-considerations.md) 进入立项方法  

总路线：[CURRICULUM.md](./CURRICULUM.md)

## 一句话速查

| 模型 | 判定看什么 | 专章 |
|------|------------|------|
| DAC | 所有者授权 | [models/01-dac.md](./models/01-dac.md) |
| MAC | 强制标签/策略 | [models/02-mac.md](./models/02-mac.md) |
| RBAC | 角色→权限 | [models/03-rbac.md](./models/03-rbac.md) |
| ABAC | 属性+策略 | [models/04-abac.md](./models/04-abac.md) |
| UCON | 授权+义务+条件（可持续） | [models/05-ucon.md](./models/05-ucon.md) |
| ACL | 资源名单 | [models/06-acl.md](./models/06-acl.md) |
| ReBAC | 关系是否成立 | [models/07-rebac.md](./models/07-rebac.md) |
| 对照与组合 | — | [models/08-comparison.md](./models/08-comparison.md) |

## 业务稳态（预告）

1. 功能权限 → RBAC  
2. 数据权限 → RBAC + 轻量属性规则  
3. 对象分享 → ACL 或 ReBAC  
4. 使用过程 → 按需借用 UCON 思想  

口诀与误见：[models/08-comparison.md](./models/08-comparison.md)  
选型：[05-decision-framework.md](./05-decision-framework.md)
