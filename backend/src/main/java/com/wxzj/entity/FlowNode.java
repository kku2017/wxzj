package com.wxzj.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_flow_node", uniqueConstraints = @UniqueConstraint(columnNames = {"flowDefId", "nodeNo"}))
public class FlowNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long flowDefId;

    @Column(nullable = false)
    private Integer nodeNo;

    @Column(nullable = false, length = 100)
    private String nodeName;

    /** ADMIN / PROPERTY / COMMITTEE */
    @Column(nullable = false, length = 20)
    private String approverRole;

    private LocalDateTime createTime = LocalDateTime.now();
}
