# 立项案例库

虚构但完整的理论立项范例，对应 [06-outputs.md](../06-outputs.md) 的 O1–O9。  
用作自学「标准答案」与评审对照。

> **纪律：先独立填写空白模板，再打开范例对照。**

## 案例索引

| 档位 | 案例 | 场景摘要 | 主模型组合 |
|------|------|----------|------------|
| **S** | [s-tier-simple-admin.md](./s-tier-simple-admin.md) | 内部配置台，三角色 | 纯 RBAC |
| **M** | [m-tier-order-ops.md](./m-tier-order-ops.md) | 制造企业订单与售后 | RBAC + 数据范围 + ticket ACL |
| **M′** | [m-content-ops-console.md](./m-content-ops-console.md) | 内容运营后台（无 ACL） | RBAC + 数据范围 |
| **L** | [l-tier-multitenant-collab.md](./l-tier-multitenant-collab.md) | 多租户文档协作 | RBAC + 租户边界 + ReBAC + 属性规则 |

建议阅读顺序：**S → M′（无分享）→ M（有工单 ACL）→ L**，体会「信号变强时模型如何加，而不是换时髦词」。

## 对照时看什么

1. O2 决策句式是否完整  
2. O6 是否消除歧义（含下级？并集？分层？）  
3. O7 不做项是否敢写死  
4. O9 是否用「信号」触发升级，而非空话  

## 相关

- 术语：[glossary.md](../glossary.md)  
- 学习路线：[CURRICULUM.md](../CURRICULUM.md)  
- 决策档位：[05-decision-framework.md](../05-decision-framework.md)
