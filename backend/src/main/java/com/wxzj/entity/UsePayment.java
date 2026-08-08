package com.wxzj.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_use_payment")
public class UsePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long useApplyId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    private LocalDateTime payTime;

    private Long operatorId;

    private String remark;
}
