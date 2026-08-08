package com.wxzj.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_building")
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 20)
    private String buildingNo;

    private String name;

    private Integer floors = 0;

    @Column(precision = 14, scale = 2)
    private BigDecimal area = BigDecimal.ZERO;

    private LocalDateTime createTime = LocalDateTime.now();
}
