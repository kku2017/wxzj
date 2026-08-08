package com.wxzj.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_deposit_standard")
public class DepositStandard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /** INITIAL 初始缴存 / RENEWAL 续缴 */
    @Column(length = 20)
    private String type = "INITIAL";

    private LocalDate effectiveDate;

    @Column(length = 16)
    private String status = "ACTIVE";

    private LocalDateTime createTime = LocalDateTime.now();
}
