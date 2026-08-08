package com.wxzj.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_flow_instance", uniqueConstraints = @UniqueConstraint(columnNames = {"bizType", "bizId"}))
public class FlowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long flowDefId;

    @Column(nullable = false, length = 30)
    private String bizType;

    @Column(nullable = false)
    private Long bizId;

    private Integer currentNodeNo;

    /** RUNNING / COMPLETED / TERMINATED */
    @Column(length = 20)
    private String status = "RUNNING";

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
