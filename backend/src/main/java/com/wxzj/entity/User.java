package com.wxzj.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    private String realName;

    private String phone;

    /** ADMIN / PROPERTY / COMMITTEE / OWNER */
    @Column(nullable = false, length = 20)
    private String role;

    private Long ownerId;

    @Column(length = 16)
    private String status = "ACTIVE";

    private LocalDateTime createTime = LocalDateTime.now();
}
