# 角色管理 · 理论基础

本目录是一套可自学的**权限 / 角色管理教材**：先统一问题，再学模型，再学会立项输出。  
实现方案（框架、表结构、代码）不在本分册。

**从这里开始学习 → [CURRICULUM.md](./CURRICULUM.md)**  
**术语随时查 → [glossary.md](./glossary.md)**  
**本页 HTML → [../html/index.html](../html/index.html)**

## 阅读路径（章节）

| 顺序 | 文档 | 你获得什么 |
|------|------|------------|
| 0 | [CURRICULUM.md](./CURRICULUM.md) | 学法、日程、验收标准 |
| 0′ | [glossary.md](./glossary.md) | 全书用语 |
| 1 | [01-problem-framework.md](./01-problem-framework.md) | 四元组；功能/数据；粒度；认证≠授权 |
| 2 | [02-model-evolution.md](./02-model-evolution.md) | 演进线、缺口传递、旁支位置 |
| 3 | [03-models.md](./03-models.md) → **[models/](./models/)** | 各模型教材专章 |
| 4 | [04-considerations.md](./04-considerations.md) | 立项考量 A–G |
| 5 | [05-decision-framework.md](./05-decision-framework.md) | Q1–Q4；S/M/L；决策句式 |
| 6 | [06-outputs.md](./06-outputs.md) | O1–O9 交付物与 DoD |
| 7 | [cases/](./cases/) | S/M/L 填好的范例（先写后看） |

## 目录结构

```
roleManage/
├── theory/                 ← Markdown 教材正文
│   ├── CURRICULUM.md
│   ├── glossary.md
│   ├── 01…06
│   ├── models/
│   └── cases/
├── html/                   ← theory 首页 HTML（index.html）
└── （待建）工程实现分册
```

## 一句话结论

> **RBAC 管功能权限 + 少量属性规则管数据范围 + 少数场景用 ACL/ReBAC；**  
> UCON 作为「是否需要使用过程控制」的检查清单，而非默认实现目标。

## 教材完成度

| 项 | 状态 |
|----|------|
| 学习路线 CURRICULUM | 已完成 |
| 术语表 | 已完成 |
| 01–06 方法论升格 | 已完成 |
| 模型专章 models/ | 已完成 |
| 案例 S / M / L | 已完成 |
| figures/ 示意图 | 待建 |
| 自测参考答案独立页 | 待建 |

## 维护约定

- 只写理论与决策依据，不写框架配置与 DDL。  
- 学法以 `CURRICULUM.md` 为准；术语以 `glossary.md` 为准。  
- 模型定义以 `models/` 专章为准；`03-models.md` 为入口速查。  
- 真实项目经验回填：`04` 案例区、`06` 登记表、`cases/`（若适宜公开）。
