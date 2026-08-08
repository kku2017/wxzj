# 维修资金管理系统（wxzj）设计方案

## 1. 项目概述

维修资金（住宅专项维修资金）是专项用于住宅共用部位、共用设施设备保修期满后的维修和更新、改造的资金。本系统面向物业管理场景，覆盖维修资金全生命周期管理：**基础数据 → 资金缴存 → 资金使用 → 资金退款 → 综合查询**。

### 1.1 角色与权限

| 角色 | 编码 | 说明 | 主要权限 |
| --- | --- | --- | --- |
| 系统管理员 | ADMIN | 系统运维，维护基础数据、标准、审批 | 全部功能，含用户管理、流程配置 |
| 物业操作员 | PROPERTY | 物业工作人员，办理缴存/使用/退款登记 | 缴存、使用申请、退款申请、基础数据查看 |
| 业委会/业主代表 | COMMITTEE | 参与使用/退款审批节点 | 审批当前节点、综合查询 |
| 业主 | OWNER | 房屋业主，自助查询、发起退款 | 本人账户余额、流水、统计查询 |

### 1.2 权限矩阵

| 功能点 | ADMIN | PROPERTY | COMMITTEE | OWNER |
| --- | :-: | :-: | :-: | :-: |
| 基础数据（小区/楼栋/房屋/业主/标准） | CRUD | 查看 | 查看 | 查看本人房屋 |
| 流程配置（流程定义/节点） | CRUD | ✗ | ✗ | ✗ |
| 缴存登记/确认到账 | ✓ | ✓ | ✗ | ✗ |
| 使用申请/分摊/拨付 | ✓ | 提交申请 | ✗ | ✗ |
| 审批节点（使用/退款） | 按节点 | ✗ | 按节点 | ✗ |
| 退款申请 | ✓ | 提交申请 | ✗ | 提交申请 |
| 综合查询（余额/流水/统计） | 全部 | 全部 | 全部 | 本人 |

## 2. 模块设计

### 2.1 基础数据

- **小区管理**：小区名称、地址、开发商、建成年份、总面积、房屋套数、状态。
- **楼栋管理**：所属小区、楼栋号、总楼层、建筑面积。
- **房屋管理**：所属楼栋、房号（如 1-101）、面积、楼层、状态（空置/已售/使用中）、产权人。
- **业主管理**：姓名、身份证号、手机号、性别、地址，与房屋建立关联。
- **缴存标准**：按小区配置初始缴存单价（元/㎡），支持初始缴存/续缴标准。
- **账户开户**：房屋登记后自动为每套房屋建立维修资金账户（account_no 自动生成）。

### 2.2 资金缴存

- 缴存登记：选择房屋 → 自动带出标准与面积 → 计算应缴金额。
- 缴存类型：初始缴存 / 补缴 / 续缴。
- 流程：**登记（待缴） → 到账确认（已缴）**，确认后自动写入资金流水并更新账户余额。
- 支持作废未确认的缴存单。

### 2.3 资金使用

- 使用申请：物业提交维修项目申请（项目名称、维修原因、总金额、涉及房屋）。
- 分摊规则：按房屋面积分摊到户（`UseItem` 明细，分摊金额 = 总金额 × 户面积 / 总涉及面积）。
- 审批流：**草稿 → 待审批 → 已批准 → 已拨付（完结） / 已拒绝**。
- 拨付时按分摊明细扣减各户账户余额并生成使用流水。

### 2.4 资金退款

- 退款申请：原因（产权转移 / 房屋灭失 / 多缴误缴），附申请余额。
- 流程：**待审批 → 已批准 → 已退款（完结） / 已拒绝**。
- 退款完成时减少账户余额并生成退款流水。

### 2.5 综合查询

- 账户查询：按小区/楼栋/业主/房屋条件查账户余额。
- 流水查询：按账户/时间/类型查收支流水。
- 统计报表：缴存统计、使用统计、退款统计、余额汇总（按小区聚合）。
- 业主端自动限定为本人房屋数据。

### 2.6 工作流（可配置审批流）

轻量自研工作流，无需引入 Flowable/Activiti，核心为「流程定义 → 流程实例 → 审批记录」三件套：

- **流程定义 (t_flow_def)**：按业务类型 `USE`(资金使用) / `REFUND`(资金退款) 各配置一条，可启停。
- **流程节点 (t_flow_node)**：挂载于流程定义，按 `node_no` 顺序执行，每节点指定审批角色 `approver_role`。
- **流程实例 (t_flow_instance)**：业务单据提交时创建，记录 `current_node_no` 与状态 `RUNNING/COMPLETED/TERMINATED`。
- **审批记录 (t_approval)**：每节点审批留痕（节点、审批人、意见、动作 PASS/REJECT）。

审批驱动规则：
1. 单据提交 → 创建流程实例，置于节点 1，业务状态 `PENDING`。
2. 当前节点审批通过 → 推进到下一节点；若已到最后节点 → 实例 `COMPLETED`，业务状态置 `APPROVED`。
3. 任一级拒绝 → 实例 `TERMINATED`，业务状态置 `REJECTED`。
4. 只有 `approver_role == 当前登录角色` 才能审批当前节点（ADMIN 可任一节点代办）。

默认流程节点（可在系统内增删改）：

| 流程 | 节点 | 审批角色 |
| --- | --- | --- |
| USE 资金使用 | 业委会审核 | COMMITTEE |
| USE 资金使用 | 主管部门审批 | ADMIN |
| REFUND 资金退款 | 物业核对 | PROPERTY |
| REFUND 资金退款 | 主管部门审批 | ADMIN |

> 业务完成后端校验：使用申请 `APPROVED` 后才可「拨付完结」；退款申请 `APPROVED` 后才可「退款完结」。

### 2.7 全流程时序设计

```
开户阶段                              缴存阶段
业主 ── 登记业主 ──► 房屋 ──开户──► 资金账户(余额0)
                                    物业 ── 缴存登记(待缴) ──► 业主缴款 ──到账确认──► 流水(IN)+余额增加

使用阶段                              退款阶段
物业 ── 提交使用申请(分摊到户)
     ──► 流程实例节点1 业委会审核(通过/驳回)
     ──► 流程实例节点2 主管部门审批(通过→APPROVED)
     ──► 拨付完结 ──► 流水(OUT)-各户分摊 + 余额减少

业主/物业 ── 提交退款申请 ──► 流程实例节点1 物业核对
     ──► 流程实例节点2 主管部门审批(通过→APPROVED)
     ──► 退款完结 ──► 流水(OUT) + 余额减少

查询阶段：任何角色按权限查看 账户余额/收支流水/统计报表
```

时序（使用申请审批）：

```
物业 PROPERTY           工作流引擎           业委会 COMMITTEE       主管部门 ADMIN
   │ 提交使用申请(分摊)      │                      │                     │
   ├──────────────────► 创建流程实例节点1        │                     │
   │  status=PENDING      │                      │                     │
   │                      ├──可审批────────────────► 审核通过           │
   │                      │                      ├────────────► 流转节点2
   │                      │                                           ├─可审批─► 通过
   │                      │                                           │（末节点）COMPLETED
   │                      ├──────────── APPROVED ◄─────────────────────┤
   │  拨付完结 ◄──────────┤                                           │
   │  流水(OUT)+扣款        │                                           │
```

## 3. 数据模型

### 3.1 核心表

| 表名 | 说明 | 关键字段 |
| --- | --- | --- |
| sys_user | 用户 | username, password(bcrypt), role, owner_id, status |
| t_community | 小区 | name, address, developer, area, house_count, status |
| t_building | 楼栋 | community_id, building_no, floors, area |
| t_house | 房屋 | building_id, community_id, house_no, area, floor, status, owner_id |
| t_owner | 业主 | name, id_card, phone, gender, address |
| t_house_owner | 房屋-业主关系 | house_id, owner_id, relation_type, is_main |
| t_fund_account | 资金账户 | house_id, owner_id, account_no, balance, total_deposit, total_used, total_refund, status |
| t_deposit_standard | 缴存标准 | community_id, name, unit_price, effective_date, type, status |
| t_deposit | 缴存单 | order_no, house_id, account_id, standard_id, amount, quantity, type, status, operator_id, pay_time |
| t_use_apply | 使用申请 | apply_no, community_id, title, reason, total_amount, share_area, status, apply_user_id |
| t_use_item | 使用分摊明细 | use_apply_id, house_id, share_area, share_amount, status |
| t_use_payment | 拨付记录 | use_apply_id, amount, pay_time, operator_id |
| t_refund_apply | 退款申请 | refund_no, account_id, house_id, owner_id, reason, amount, status, apply_user_id |
| t_fund_flow | 资金流水 | flow_no, account_id, house_id, community_id, type, direction, amount, balance, related_no, operator_id, biz_time, remark |
| t_flow_def | 流程定义 | code(USE/REFUND), name, status |
| t_flow_node | 流程节点 | flow_def_id, node_no, node_name, approver_role |
| t_flow_instance | 流程实例 | flow_def_id, biz_type, biz_id, current_node_no, status, start_time, end_time |
| t_approval | 审批记录 | flow_instance_id, biz_type, biz_id, node_no, node_name, approver_id, action, opinion, time |

### 3.2 状态机

- 缴存单：`PENDING(待缴) → PAID(已缴) / CANCELLED(作废)`
- 使用申请：`DRAFT(草稿) → PENDING(待审批) → APPROVED(已批准) → PAID(已拨付) / REJECTED(已拒绝)`
- 退款申请：`PENDING(待审批) → APPROVED(已批准) → REFUNDED(已退款) / REJECTED(已拒绝)`
- 账户：`ACTIVE(正常) / FROZEN(冻结) / CLOSED(注销)`

## 4. 技术架构

```
前端 (Vue3 + Vite + Element Plus + Pinia + Vue Router + Axios)
        │  RESTful /api/**  (JWT 认证)
后端 (Spring Boot 3.x + Spring Security + Spring Data JPA)
        │  H2(开发/演示) / MySQL(生产)
数据库
```

- 后端目录：`backend/`，端口 `8080`，上下文 `/api`。
- 前端目录：`frontend/`，开发端口 `5173`，代理 `/api` → `8080`。
- JWT：登录签发 `Bearer` Token，携带角色信息。
- 演示数据：`CommandLineRunner` 自动初始化 2 个小区、楼栋、房屋、业主、标准、账户、管理员/物业/业主账号。

### 4.1 默认账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| admin | admin123 | ADMIN |
| property | property123 | PROPERTY |
| committee | committee123 | COMMITTEE |
| owner | owner123 | OWNER |

## 5. API 一览

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | /api/auth/login | 登录 | 公开 |
| GET | /api/auth/me | 当前用户 | 登录 |
| GET/POST/PUT/DELETE | /api/basic/community | 小区 CRUD | ADMIN, PROPERTY(读) |
| GET/POST/PUT/DELETE | /api/basic/building | 楼栋 CRUD | ADMIN, PROPERTY(读) |
| GET/POST/PUT/DELETE | /api/basic/house | 房屋 CRUD | ADMIN, PROPERTY(读) |
| GET/POST/PUT/DELETE | /api/basic/owner | 业主 CRUD | ADMIN, PROPERTY(读) |
| GET/POST/PUT/DELETE | /api/basic/standard | 缴存标准 CRUD | ADMIN, PROPERTY(读) |
| GET | /api/basic/account | 账户查询 | ADMIN, PROPERTY, OWNER(本人) |
| GET/POST | /api/deposit | 缴存单列表/登记 | ADMIN, PROPERTY |
| POST | /api/deposit/{id}/confirm | 到账确认 | ADMIN, PROPERTY |
| POST | /api/deposit/{id}/cancel | 作废 | ADMIN, PROPERTY |
| GET/POST | /api/use | 使用申请列表/提交 | ADMIN, PROPERTY |
| POST | /api/use/{id}/approve | 审批通过（当前节点） | 按节点角色 |
| POST | /api/use/{id}/pay | 拨付完结 | ADMIN |
| POST | /api/use/{id}/reject | 拒绝 | 按节点角色 |
| GET/POST | /api/refund | 退款申请列表/提交 | ADMIN, PROPERTY, OWNER |
| POST | /api/refund/{id}/approve | 审批通过（当前节点） | 按节点角色 |
| POST | /api/refund/{id}/confirm | 退款完结 | ADMIN |
| POST | /api/refund/{id}/reject | 拒绝 | 按节点角色 |
| GET/POST/PUT/DELETE | /api/workflow/def | 流程定义 CRUD | ADMIN |
| GET/POST/PUT/DELETE | /api/workflow/node | 流程节点 CRUD | ADMIN |
| GET | /api/workflow/instance?bizType=&bizId= | 流程实例及审批记录 | 登录 |
| GET | /api/query/account | 账户综合查询 | 登录（业主限本人） |
| GET | /api/query/flow | 流水查询 | 登录（业主限本人） |
| GET | /api/query/statistics | 统计报表 | ADMIN, PROPERTY |

## 6. 开发与运行

```bash
# 后端（需 JDK17+ 与 Maven）
cd backend && mvn spring-boot:run

# 前端（需 Node.js 18+）
cd frontend && npm install && npm run dev
```

访问 `http://localhost:5173`，用默认账号登录。
