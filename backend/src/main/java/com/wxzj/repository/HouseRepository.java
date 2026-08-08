package com.wxzj.repository;

import com.wxzj.entity.House;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface HouseRepository extends JpaRepository<House, Long>, JpaSpecificationExecutor<House> {

    List<House> findByBuildingIdOrderByHouseNo(Long buildingId);

    List<House> findByCommunityIdOrderByHouseNo(Long communityId);

    long countByCommunityId(Long communityId);
}
