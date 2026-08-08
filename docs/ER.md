# 维修资金系统 ER 图（Mermaid）

> 在 VS Code、Typora、GitHub、或 https://mermaid.live 中粘贴渲染。

```mermaid
erDiagram
    sys_user ||--o{ t_owner : "业主账号(owner_id)"
    t_community ||--o{ t_building : "楼栋所属小区"
    t_community ||--o{ t_house : "房屋所属小区"
    t_building ||--o{ t_house : "房屋所属楼栋"
    t_owner ||--o{ t_house : "房屋产权人"
    t_house ||--o{ t_house_owner : "房屋业主关系"
    t_owner ||--o{ t_house_owner : "房屋业主关系"
    t_house ||--o{ t_fund_account : "账户对应房屋(1:1)"
    t_owner ||--o{ t_fund_account : "账户业主"
    t_community ||--o{ t_fund_account : "账户所属小区"
    t_community ||--o{ t_deposit_standard : "标准所属小区"
    t_community ||--o{ t_deposit : "缴存单小区"
    t_house ||--o{ t_deposit : "缴存房屋"
    t_fund_account ||--o{ t_deposit : "缴存账户"
    t_deposit_standard ||--o{ t_deposit : "缴存标准"
    t_owner ||--o{ t_deposit : "缴存业主"
    sys_user ||--o{ t_deposit : "经办人"
    t_community ||--o{ t_use_apply : "使用申请小区"
    sys_user ||--o{ t_use_apply : "申请人"
    t_use_apply ||--o{ t_use_item : "分摊明细-申请"
    t_house ||--o{ t_use_item : "分摊房屋"
    t_fund_account ||--o{ t_use_item : "分摊账户"
    t_use_apply ||--o{ t_use_payment : "拨付-申请"
    sys_user ||--o{ t_use_payment : "经办人"
    t_community ||--o{ t_refund_apply : "退款申请小区"
    t_house ||--o{ t_refund_apply : "退款房屋"
    t_fund_account ||--o{ t_refund_apply : "退款账户"
    t_owner ||--o{ t_refund_apply : "退款业主"
    sys_user ||--o{ t_refund_apply : "申请人"
    t_fund_account ||--o{ t_fund_flow : "流水账户"
    t_house ||--o{ t_fund_flow : "流水房屋"
    t_community ||--o{ t_fund_flow : "流水小区"
    sys_user ||--o{ t_fund_flow : "经办人"
    t_flow_def ||--o{ t_flow_node : "节点所属流程"
    t_flow_def ||--o{ t_flow_instance : "实例所属流程"
    t_flow_instance ||--o{ t_approval : "审批-实例"
    sys_user ||--o{ t_approval : "审批人"

    sys_user {
        系统登录账号
        bigint id  PK "主键"
        varchar username  "登录名，唯一"
        varchar password  "BCrypt 密文"
        varchar real_name  "姓名"
        varchar phone  "电话"
        varchar role  "ADMIN/PROPERTY/COMMITTEE/OWNER"
        bigint owner_id  FK "关联业主（业主角色）"
        varchar status  "ACTIVE/停用"
        datetime create_time  "创建时间"
    }
    t_owner {
        房屋产权人信息
        bigint id  PK "主键"
        varchar name  "姓名"
        varchar id_card  "身份证号，唯一"
        varchar phone  "电话"
        varchar gender  "性别"
        varchar address  "地址"
        datetime create_time  "创建时间"
    }
    t_house_owner {
        一房多主、主次关系
        bigint id  PK "主键"
        bigint house_id  FK "房屋"
        bigint owner_id  FK "业主"
        varchar relation_type  "产权人/共有人"
        tinyint is_main  "1主0次"
    }
    t_community {
        物业小区基本信息
        bigint id  PK "主键"
        varchar name  "小区名称"
        varchar address  "地址"
        varchar developer  "开发商"
        int build_year  "建成年份"
        decimal area  "总建筑面积(㎡)"
        int house_count  "房屋套数"
        varchar status  "启用/停用"
        datetime create_time  "创建时间"
    }
    t_building {
        小区内楼栋
        bigint id  PK "主键"
        bigint community_id  FK "所属小区"
        varchar building_no  "楼栋号"
        varchar name  "楼栋名称"
        int floors  "楼层数"
        decimal area  "建筑面积(㎡)"
        datetime create_time  "创建时间"
    }
    t_house {
        每套住宅，开户对象
        bigint id  PK "主键"
        bigint community_id  FK "所属小区"
        bigint building_id  FK "所属楼栋"
        varchar house_no  "房号，如 1-101"
        int floor  "楼层"
        decimal area  "建筑面积(㎡)"
        varchar status  "已售/空置"
        bigint owner_id  FK "产权人"
        datetime create_time  "创建时间"
    }
    t_fund_account {
        每套房屋一个维修资金账户
        bigint id  PK "主键"
        varchar account_no  "账户号，唯一"
        bigint house_id  FK "关联房屋"
        bigint owner_id  FK "业主"
        bigint community_id  FK "所属小区"
        decimal balance  "当前余额"
        decimal total_deposit  "累计缴存"
        decimal total_used  "累计使用"
        decimal total_refund  "累计退款"
        varchar status  "正常/冻结/注销"
        datetime open_time  "开户时间"
        datetime create_time  "创建时间"
    }
    t_deposit_standard {
        按小区配置缴存单价
        bigint id  PK "主键"
        bigint community_id  FK "所属小区"
        varchar name  "标准名称"
        decimal unit_price  "单价(元/㎡)"
        varchar type  "初始缴存/续缴"
        date effective_date  "生效日期"
        varchar status  "启用/停用"
        datetime create_time  "创建时间"
    }
    t_deposit {
        资金缴存业务单
        bigint id  PK "主键"
        varchar order_no  "缴存单号，唯一"
        bigint community_id  FK "小区"
        bigint house_id  FK "房屋"
        bigint account_id  FK "资金账户"
        bigint standard_id  FK "缴存标准"
        bigint owner_id  FK "业主"
        varchar type  "初始/续缴/补缴"
        decimal quantity  "面积(㎡)"
        decimal unit_price  "单价"
        decimal amount  "应缴金额"
        varchar status  "待缴/已缴/作废"
        bigint operator_id  FK "经办人"
        datetime pay_time  "到账时间"
        varchar remark  "备注"
        datetime create_time  "创建时间"
    }
    t_use_apply {
        维修资金使用申请单
        bigint id  PK "主键"
        varchar apply_no  "申请单号，唯一"
        bigint community_id  FK "小区"
        varchar title  "维修项目名称"
        varchar reason  "维修原因"
        varchar item_desc  "项目内容"
        decimal total_amount  "总金额"
        decimal share_area  "分摊面积合计"
        varchar status  "草稿/审批中/已批准/已拨付/已驳回"
        bigint apply_user_id  FK "申请人"
        datetime apply_time  "申请时间"
        datetime finish_time  "完成时间"
        varchar remark  "备注"
        datetime create_time  "创建时间"
    }
    t_use_item {
        按面积分摊到户
        bigint id  PK "主键"
        bigint use_apply_id  FK "使用申请"
        bigint house_id  FK "房屋"
        bigint account_id  FK "资金账户"
        decimal share_area  "分摊面积"
        decimal share_amount  "分摊金额"
        varchar status  "待拨付/已拨付"
    }
    t_use_payment {
        使用拨付记录
        bigint id  PK "主键"
        bigint use_apply_id  FK "使用申请"
        decimal amount  "拨付金额"
        datetime pay_time  "拨付时间"
        bigint operator_id  FK "经办人"
        varchar remark  "备注"
    }
    t_refund_apply {
        维修资金退款申请单
        bigint id  PK "主键"
        varchar refund_no  "退款单号，唯一"
        bigint community_id  FK "小区"
        bigint house_id  FK "房屋"
        bigint account_id  FK "资金账户"
        bigint owner_id  FK "业主"
        varchar reason  "产权转移/灭失/多缴"
        decimal amount  "退款金额"
        decimal balance_at_apply  "申请时余额"
        varchar status  "审批中/已批准/已退款/已驳回"
        bigint apply_user_id  FK "申请人"
        datetime apply_time  "申请时间"
        datetime finish_time  "完成时间"
        varchar remark  "备注"
        datetime create_time  "创建时间"
    }
    t_fund_flow {
        账户收支总账
        bigint id  PK "主键"
        varchar flow_no  "流水号，唯一"
        bigint account_id  FK "资金账户"
        bigint house_id  FK "房屋"
        bigint community_id  FK "小区"
        varchar type  "缴存/使用/退款"
        varchar direction  "IN 收入/OUT 支出"
        decimal amount  "金额"
        decimal balance  "变动后余额"
        varchar related_no  "关联单据号"
        bigint operator_id  FK "经办人"
        datetime biz_time  "业务时间"
        varchar remark  "备注"
        datetime create_time  "创建时间"
    }
    t_flow_def {
        业务审批流程
        bigint id  PK "主键"
        varchar code  "USE/REFUND，唯一"
        varchar name  "流程名称"
        varchar status  "启用/停用"
        datetime create_time  "创建时间"
    }
    t_flow_node {
        审批节点配置
        bigint id  PK "主键"
        bigint flow_def_id  FK "所属流程"
        int node_no  "节点顺序"
        varchar node_name  "节点名称"
        varchar approver_role  "审批角色"
        datetime create_time  "创建时间"
    }
    t_flow_instance {
        单据审批运行实例
        bigint id  PK "主键"
        bigint flow_def_id  FK "流程定义"
        varchar biz_type  "USE/REFUND"
        bigint biz_id  "业务单据ID"
        int current_node_no  "当前节点"
        varchar status  "运行中/完成/终止"
        datetime start_time  "开始时间"
        datetime end_time  "结束时间"
    }
    t_approval {
        审批留痕
        bigint id  PK "主键"
        bigint flow_instance_id  FK "流程实例"
        varchar biz_type  "USE/REFUND"
        bigint biz_id  "业务单据ID"
        int node_no  "节点"
        varchar node_name  "节点名称"
        bigint approver_id  FK "审批人"
        varchar approver_name  "审批人姓名"
        varchar action  "通过/驳回"
        varchar opinion  "意见"
        datetime time  "审批时间"
    }
```