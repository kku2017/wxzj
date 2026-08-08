package com.wxzj.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_deposit")
public class Deposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 40)
    private String orderNo;

    @Column(nullable = false)
    private Long communityId;

    @Column(nullable = false)
    private Long houseId;

    @Column(nullable = false)
    private Long accountId;

    private Long standardId;

    private Long ownerId;

    /** INITIAL / RENEWAL / SUPPLEMENT */
    @Column(length = 20)
    private String type = "INITIAL";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** PENDING 待缴 / PAID 已缴 / CANCELLED 作废 */
    @Column(length = 20)
    private String status = "PENDING";

    private Long operatorId;

    private LocalDateTime payTime;

    private String remark;

    private LocalDateTime createTime = LocalDateTime.now();
}
