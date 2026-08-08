-- =============================================================
-- 维修资金管理系统（wxzj） MySQL 建表脚本
-- 说明：开发/演示环境由 Spring Boot + H2 自动建表并初始化数据，
--       生产环境使用本脚本在 MySQL 中建表，将 application.yml 切换为 mysql 配置。
-- =============================================================
CREATE DATABASE IF NOT EXISTS wxzj DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE wxzj;

-- 用户
CREATE TABLE IF NOT EXISTS sys_user (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  username     VARCHAR(64)  NOT NULL UNIQUE,
  password     VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
  real_name    VARCHAR(64),
  phone        VARCHAR(20),
  role         VARCHAR(20)  NOT NULL COMMENT 'ADMIN/PROPERTY/COMMITTEE/OWNER',
  owner_id     BIGINT,
  status       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT '系统用户';

-- 小区
CREATE TABLE IF NOT EXISTS t_community (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  address       VARCHAR(200),
  developer     VARCHAR(100),
  build_year    INT,
  area          DECIMAL(14,2) DEFAULT 0 COMMENT '总建筑面积',
  house_count   INT DEFAULT 0,
  status        VARCHAR(16) DEFAULT 'ACTIVE',
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT '小区';

-- 楼栋
CREATE TABLE IF NOT EXISTS t_building (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  community_id BIGINT NOT NULL,
  building_no  VARCHAR(20) NOT NULL,
  name         VARCHAR(100),
  floors       INT DEFAULT 0,
  area         DECIMAL(14,2) DEFAULT 0,
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_community (community_id)
) ENGINE=InnoDB COMMENT '楼栋';

-- 房屋
CREATE TABLE IF NOT EXISTS t_house (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  community_id BIGINT NOT NULL,
  building_id  BIGINT NOT NULL,
  house_no     VARCHAR(40) NOT NULL COMMENT '房号 如 1-101',
  floor        INT DEFAULT 1,
  area         DECIMAL(12,2) NOT NULL COMMENT '建筑面积(㎡)',
  status       VARCHAR(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE 已售/ EMPTY 空置',
  owner_id     BIGINT,
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_community (community_id),
  KEY idx_building (building_id)
) ENGINE=InnoDB COMMENT '房屋';

-- 业主
CREATE TABLE IF NOT EXISTS t_owner (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(64) NOT NULL,
  id_card     VARCHAR(30) NOT NULL UNIQUE,
  phone       VARCHAR(20),
  gender      VARCHAR(4),
  address     VARCHAR(200),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT '业主';

-- 房屋-业主关系（一房多主、主次关系）
CREATE TABLE IF NOT EXISTS t_house_owner (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  house_id      BIGINT NOT NULL,
  owner_id      BIGINT NOT NULL,
  relation_type VARCHAR(20) DEFAULT 'OWNER' COMMENT 'OWNER 产权人/CO_OWNER 共有人',
  is_main       TINYINT DEFAULT 1 COMMENT '1 主 0 次',
  UNIQUE KEY uk_house_owner (house_id, owner_id)
) ENGINE=InnoDB COMMENT '房屋业主关系';

-- 资金账户（每套房屋一个）
CREATE TABLE IF NOT EXISTS t_fund_account (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_no    VARCHAR(40) NOT NULL UNIQUE,
  house_id      BIGINT NOT NULL,
  owner_id      BIGINT,
  community_id  BIGINT NOT NULL,
  balance       DECIMAL(14,2) DEFAULT 0 COMMENT '当前余额',
  total_deposit DECIMAL(14,2) DEFAULT 0 COMMENT '累计缴存',
  total_used    DECIMAL(14,2) DEFAULT 0 COMMENT '累计使用',
  total_refund  DECIMAL(14,2) DEFAULT 0 COMMENT '累计退款',
  status        VARCHAR(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/FROZEN/CLOSED',
  open_time     DATETIME,
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_house (house_id),
  KEY idx_community (community_id)
) ENGINE=InnoDB COMMENT '维修资金账户';

-- 缴存标准
CREATE TABLE IF NOT EXISTS t_deposit_standard (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  community_id   BIGINT NOT NULL,
  name           VARCHAR(100) NOT NULL,
  unit_price     DECIMAL(10,2) NOT NULL COMMENT '元/㎡',
  type           VARCHAR(20) DEFAULT 'INITIAL' COMMENT 'INITIAL 初始缴存/RENEWAL 续缴',
  effective_date DATE,
  status         VARCHAR(16) DEFAULT 'ACTIVE',
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_community (community_id)
) ENGINE=InnoDB COMMENT '缴存标准';

-- 缴存单
CREATE TABLE IF NOT EXISTS t_deposit (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no      VARCHAR(40) NOT NULL UNIQUE,
  community_id  BIGINT NOT NULL,
  house_id      BIGINT NOT NULL,
  account_id    BIGINT NOT NULL,
  standard_id   BIGINT,
  owner_id      BIGINT,
  type          VARCHAR(20) DEFAULT 'INITIAL' COMMENT 'INITIAL/RENEWAL/SUPPLEMENT',
  quantity      DECIMAL(12,2) NOT NULL COMMENT '面积(㎡)',
  unit_price    DECIMAL(10,2) NOT NULL,
  amount        DECIMAL(14,2) NOT NULL COMMENT '应缴金额',
  status        VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING 待缴/PAID 已缴/CANCELLED 作废',
  operator_id   BIGINT,
  pay_time      DATETIME,
  remark        VARCHAR(500),
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_house (house_id),
  KEY idx_account (account_id)
) ENGINE=InnoDB COMMENT '缴存单';

-- 使用申请
CREATE TABLE IF NOT EXISTS t_use_apply (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  apply_no      VARCHAR(40) NOT NULL UNIQUE,
  community_id  BIGINT NOT NULL,
  title         VARCHAR(200) NOT NULL COMMENT '维修项目名称',
  reason        VARCHAR(500) COMMENT '维修原因',
  item_desc     VARCHAR(1000) COMMENT '项目内容',
  total_amount  DECIMAL(14,2) NOT NULL,
  share_area    DECIMAL(14,2) DEFAULT 0 COMMENT '分摊面积合计',
  status        VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/APPROVED/PAID/REJECTED',
  apply_user_id BIGINT,
  apply_time    DATETIME,
  finish_time   DATETIME,
  remark        VARCHAR(500),
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_community (community_id)
) ENGINE=InnoDB COMMENT '使用申请';

-- 使用分摊明细
CREATE TABLE IF NOT EXISTS t_use_item (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  use_apply_id BIGINT NOT NULL,
  house_id     BIGINT NOT NULL,
  account_id   BIGINT NOT NULL,
  share_area   DECIMAL(12,2) NOT NULL,
  share_amount DECIMAL(14,2) NOT NULL,
  status       VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/PAID',
  KEY idx_apply (use_apply_id)
) ENGINE=InnoDB COMMENT '使用分摊明细';

-- 拨付记录
CREATE TABLE IF NOT EXISTS t_use_payment (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  use_apply_id BIGINT NOT NULL,
  amount       DECIMAL(14,2) NOT NULL,
  pay_time     DATETIME,
  operator_id  BIGINT,
  remark       VARCHAR(500)
) ENGINE=InnoDB COMMENT '拨付记录';

-- 退款申请
CREATE TABLE IF NOT EXISTS t_refund_apply (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  refund_no     VARCHAR(40) NOT NULL UNIQUE,
  community_id  BIGINT NOT NULL,
  house_id      BIGINT NOT NULL,
  account_id    BIGINT NOT NULL,
  owner_id      BIGINT,
  reason        VARCHAR(30) NOT NULL COMMENT 'TRANSFER 产权转移/DEMOLITION 房屋灭失/OVERPAY 多缴误缴',
  amount        DECIMAL(14,2) NOT NULL,
  balance_at_apply DECIMAL(14,2) DEFAULT 0,
  status        VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REFUNDED/REJECTED',
  apply_user_id BIGINT,
  apply_time    DATETIME,
  finish_time   DATETIME,
  remark        VARCHAR(500),
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_account (account_id)
) ENGINE=InnoDB COMMENT '退款申请';

-- 资金流水（总账）
CREATE TABLE IF NOT EXISTS t_fund_flow (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  flow_no      VARCHAR(40) NOT NULL UNIQUE,
  account_id   BIGINT NOT NULL,
  house_id     BIGINT NOT NULL,
  community_id BIGINT NOT NULL,
  type         VARCHAR(20) NOT NULL COMMENT 'DEPOSIT 缴存/USE 使用/REFUND 退款',
  direction    VARCHAR(4) NOT NULL COMMENT 'IN 收入/OUT 支出',
  amount       DECIMAL(14,2) NOT NULL,
  balance      DECIMAL(14,2) NOT NULL COMMENT '发生后的账户余额',
  related_no   VARCHAR(40) COMMENT '关联单据号',
  operator_id  BIGINT,
  biz_time     DATETIME,
  remark       VARCHAR(500),
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_account (account_id),
  KEY idx_house (house_id),
  KEY idx_community (community_id)
) ENGINE=InnoDB COMMENT '资金流水';

-- 流程定义
CREATE TABLE IF NOT EXISTS t_flow_def (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  code        VARCHAR(30) NOT NULL UNIQUE COMMENT 'USE/REFUND',
  name        VARCHAR(100) NOT NULL,
  status      VARCHAR(16) DEFAULT 'ACTIVE',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT '流程定义';

-- 流程节点
CREATE TABLE IF NOT EXISTS t_flow_node (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  flow_def_id   BIGINT NOT NULL,
  node_no       INT NOT NULL COMMENT '顺序',
  node_name     VARCHAR(100) NOT NULL,
  approver_role VARCHAR(20) NOT NULL COMMENT '审批角色 ADMIN/PROPERTY/COMMITTEE',
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_def_node (flow_def_id, node_no)
) ENGINE=InnoDB COMMENT '流程节点';

-- 流程实例
CREATE TABLE IF NOT EXISTS t_flow_instance (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  flow_def_id    BIGINT NOT NULL,
  biz_type       VARCHAR(30) NOT NULL COMMENT 'USE/REFUND',
  biz_id         BIGINT NOT NULL,
  current_node_no INT,
  status         VARCHAR(20) DEFAULT 'RUNNING' COMMENT 'RUNNING/COMPLETED/TERMINATED',
  start_time     DATETIME,
  end_time       DATETIME,
  UNIQUE KEY uk_biz (biz_type, biz_id)
) ENGINE=InnoDB COMMENT '流程实例';

-- 审批记录
CREATE TABLE IF NOT EXISTS t_approval (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  flow_instance_id BIGINT,
  biz_type         VARCHAR(30) NOT NULL,
  biz_id           BIGINT NOT NULL,
  node_no          INT NOT NULL,
  node_name        VARCHAR(100),
  approver_id      BIGINT,
  approver_name    VARCHAR(64),
  action           VARCHAR(16) NOT NULL COMMENT 'PASS 通过/REJECT 拒绝',
  opinion          VARCHAR(500),
  time             DATETIME
) ENGINE=InnoDB COMMENT '审批记录';

-- =============================================================
-- 初始化说明
-- 演示数据（用户/小区/楼栋/房屋/业主/标准/账户/流程定义/节点）由后端
-- CommandLineRunner DataSeeder 在启动时自动初始化（密码为明文配置，BCrypt 加密入库）。
-- 如需生产部署，可在 DataSeeder 中关闭或替换。
-- =============================================================
