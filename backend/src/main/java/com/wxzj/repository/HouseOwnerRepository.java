package com.wxzj.repository;

import com.wxzj.entity.HouseOwner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HouseOwnerRepository extends JpaRepository<HouseOwner, Long> {

    List<HouseOwner> findByHouseId(Long houseId);

    List<HouseOwner> findByOwnerId(Long ownerId);

    void deleteByHouseId(Long houseId);

    boolean existsByHouseIdAndOwnerId(Long houseId, Long ownerId);
}
