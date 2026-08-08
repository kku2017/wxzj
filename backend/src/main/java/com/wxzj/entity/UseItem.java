package com.wxzj.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "t_use_item")
public class UseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long useApplyId;

    @Column(nullable = false)
    private Long houseId;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal shareArea;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal shareAmount;

    /** PENDING / PAID */
    @Column(length = 20)
    private String status = "PENDING";
}
