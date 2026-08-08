package com.wxzj.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_house")
public class House {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long communityId;

    @Column(nullable = false)
    private Long buildingId;

    /** 房号 如 1-101 */
    @Column(nullable = false, length = 40)
    private String houseNo;

    private Integer floor = 1;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal area;

    /** ACTIVE 已售 / EMPTY 空置 */
    @Column(length = 16)
    private String status = "ACTIVE";

    private Long ownerId;

    private LocalDateTime createTime = LocalDateTime.now();
}
