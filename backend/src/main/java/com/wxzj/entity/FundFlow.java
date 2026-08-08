package com.wxzj.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_fund_flow")
public class FundFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 40)
    private String flowNo;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private Long houseId;

    @Column(nullable = false)
    private Long communityId;

    /** DEPOSIT 缴存 / USE 使用 / REFUND 退款 */
    @Column(nullable = false, length = 20)
    private String type;

    /** IN / OUT */
    @Column(nullable = false, length = 4)
    private String direction;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balance;

    @Column(length = 40)
    private String relatedNo;

    private Long operatorId;

    private LocalDateTime bizTime;

    private String remark;

    private LocalDateTime createTime = LocalDateTime.now();
}
