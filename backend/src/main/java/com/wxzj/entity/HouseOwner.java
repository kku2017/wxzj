package com.wxzj.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "t_house_owner", uniqueConstraints = @UniqueConstraint(columnNames = {"houseId", "ownerId"}))
public class HouseOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long houseId;

    @Column(nullable = false)
    private Long ownerId;

    /** OWNER 产权人 / CO_OWNER 共有人 */
    @Column(length = 20)
    private String relationType = "OWNER";

    private Boolean isMain = true;
}
