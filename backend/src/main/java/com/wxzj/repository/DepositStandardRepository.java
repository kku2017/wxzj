package com.wxzj.repository;

import com.wxzj.entity.DepositStandard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DepositStandardRepository extends JpaRepository<DepositStandard, Long>, JpaSpecificationExecutor<DepositStandard> {

    List<DepositStandard> findByCommunityIdAndStatusOrderByEffectiveDateDesc(Long communityId, String status);
}
