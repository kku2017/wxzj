package com.wxzj.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_use_apply")
public class UseApply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 40)
    private String applyNo;

    @Column(nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String reason;

    @Column(length = 1000)
    private String itemDesc;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 14, scale = 2)
    private BigDecimal shareArea = BigDecimal.ZERO;

    /** DRAFT / PENDING / APPROVED / PAID / REJECTED */
    @Column(length = 20)
    private String status = "DRAFT";

    private Long applyUserId;

    private LocalDateTime applyTime;

    private LocalDateTime finishTime;

    private String remark;

    private LocalDateTime createTime = LocalDateTime.now();
}
