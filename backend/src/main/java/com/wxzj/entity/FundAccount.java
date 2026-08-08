package com.wxzj.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_fund_account")
public class FundAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 40)
    private String accountNo;

    @Column(nullable = false)
    private Long houseId;

    private Long ownerId;

    @Column(nullable = false)
    private Long communityId;

    @Column(precision = 14, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalDeposit = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalUsed = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalRefund = BigDecimal.ZERO;

    /** ACTIVE / FROZEN / CLOSED */
    @Column(length = 16)
    private String status = "ACTIVE";

    private LocalDateTime openTime;

    private LocalDateTime createTime = LocalDateTime.now();
}
