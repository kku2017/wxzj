package com.wxzj.repository;

import com.wxzj.entity.FundAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface FundAccountRepository extends JpaRepository<FundAccount, Long>, JpaSpecificationExecutor<FundAccount> {

    Optional<FundAccount> findByHouseId(Long houseId);

    List<FundAccount> findByOwnerId(Long ownerId);
}
