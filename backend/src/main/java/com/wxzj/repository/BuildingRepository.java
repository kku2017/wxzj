package com.wxzj.repository;

import com.wxzj.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface BuildingRepository extends JpaRepository<Building, Long>, JpaSpecificationExecutor<Building> {

    List<Building> findByCommunityIdOrderByBuildingNo(Long communityId);
}
