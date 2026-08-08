package com.wxzj.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_community")
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String address;

    private String developer;

    private Integer buildYear;

    @Column(precision = 14, scale = 2)
    private BigDecimal area = BigDecimal.ZERO;

    private Integer houseCount = 0;

    @Column(length = 16)
    private String status = "ACTIVE";

    private LocalDateTime createTime = LocalDateTime.now();
}
