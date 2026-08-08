package com.wxzj.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_approval")
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long flowInstanceId;

    @Column(nullable = false, length = 30)
    private String bizType;

    @Column(nullable = false)
    private Long bizId;

    @Column(nullable = false)
    private Integer nodeNo;

    private String nodeName;

    private Long approverId;

    private String approverName;

    /** PASS / REJECT */
    @Column(nullable = false, length = 16)
    private String action;

    private String opinion;

    private LocalDateTime time = LocalDateTime.now();
}
