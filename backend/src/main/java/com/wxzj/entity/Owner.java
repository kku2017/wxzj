package com.wxzj.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_owner")
public class Owner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(unique = true, nullable = false, length = 30)
    private String idCard;

    private String phone;

    @Column(length = 4)
    private String gender;

    private String address;

    private LocalDateTime createTime = LocalDateTime.now();
}
