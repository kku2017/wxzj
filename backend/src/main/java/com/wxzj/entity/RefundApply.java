package com.wxzj.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_refund_apply")
public class RefundApply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 40)
    private String refundNo;

    @Column(nullable = false)
    private Long communityId;

    @Column(nullable = false)
    private Long houseId;

    @Column(nullable = false)
    private Long accountId;

    private Long ownerId;

    /** TRANSFER 产权转移 / DEMOLITION 房屋灭失 / OVERPAY 多缴误缴 */
    @Column(nullable = false, length = 30)
    private String reason;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(precision = 14, scale = 2)
    private BigDecimal balanceAtApply = BigDecimal.ZERO;

    /** PENDING / APPROVED / REFUNDED / REJECTED */
    @Column(length = 20)
    private String status = "PENDING";

    private Long applyUserId;

    private LocalDateTime applyTime;

    private LocalDateTime finishTime;

    private String remark;

    private LocalDateTime createTime = LocalDateTime.now();
}
