# 维修资金管理系统（wxzj）

面向物业管理的住宅专项维修资金管理系统，覆盖**基础数据 → 资金缴存 → 资金使用 → 资金退款 → 综合查询**全流程，内置**可配置审批工作流**与四类角色权限。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios |
| 后端 | Spring Boot 3.3 + Spring Security(JWT) + Spring Data JPA |
| 数据库 | H2（开发/演示，自动建表+初始化数据）/ MySQL（生产） |

## 目录结构

```
wxzj/
├── docs/
│   ├── DESIGN.md          # 设计方案（模块/数据模型/工作流/时序图/API）
│   ├── TESTING.md         # 测试文档（功能用例/场景/接口/边界）
│   └── MIGRATION.md       # 数据迁移工具使用手册
├── migration/             # 历史数据迁移模板与测试数据（house_info.csv）
├── sql/schema.sql         # MySQL 建表脚本
├── backend/               # Spring Boot 后端（端口 8080）
└── frontend/              # Vue3 前端（端口 5173，/api 代理到 8080）
```

## 快速开始

### 1. 启动后端（需 JDK 17+ 与 Maven）

```bash
cd backend
mvn spring-boot:run        # 默认 H2 演示库，自动建表并初始化演示数据
```

切换 MySQL：`application.yml` 中 `spring.profiles.active: h2` 改为 `mysql`（先执行 `sql/schema.sql`）。

### 2. 启动前端（需 Node.js 18+）

```bash
cd frontend
npm install
npm run dev                # 访问 http://localhost:5173
```

### 3. 默认账号

| 账号 | 密码 | 角色 | 说明 |
| --- | --- | --- | --- |
| admin | admin123 | 系统管理员 | 全部功能 + 流程配置 |
| property | property123 | 物业操作员 | 缴存/使用/退款登记与提交 |
| committee | committee123 | 业委会 | 参与审批节点 |
| owner | owner123 | 业主 | 本人账户查询、发起退款 |

## 主要功能

- **基础数据**：小区 / 楼栋 / 房屋（自动开户）/ 业主 / 缴存标准
- **资金缴存**：登记（自动取标准单价×面积）→ 到账确认 → 流水与余额更新
- **资金使用**：按面积分摊 → 提交审批 → 审批流 → 拨付扣款
- **资金退款**：发起（产权转移/灭失/多缴）→ 审批流 → 退款完结
- **综合查询**：账户余额、收支流水、统计报表（按小区汇总+占比）
- **数据迁移**：上传 house_info.csv 自动补齐档案、建账户、生成缴存单与流水（支持一房多主，见 `docs/MIGRATION.md`）
- **可配置工作流**：流程定义 + 顺序节点（节点可增删改），审批留痕

## 文档

- 设计方案（含工作流与全流程时序）：`docs/DESIGN.md`
- 测试文档（用例/场景/边界/验证结果）：`docs/TESTING.md`
- 数据迁移工具使用手册（CSV 模板/摘要规则/验证结果）：`docs/MIGRATION.md`

## 常见问题

- **端口占用**：8080/5173 被占用时修改 `application.yml` 与 `frontend/vite.config.js`。
- **重置演示数据**：删除 `backend/data/` 目录后重启后端。
- **H2 控制台**：http://localhost:8080/h2-console （JDBC URL：`jdbc:h2:file:./data/wxzj`，用户 `sa`，密码空）。
