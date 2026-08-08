package com.wxzj.repository;

import com.wxzj.entity.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DepositRepository extends JpaRepository<Deposit, Long>, JpaSpecificationExecutor<Deposit> {

    List<Deposit> findByAccountIdOrderByCreateTimeDesc(Long accountId);

    List<Deposit> findByHouseIdOrderByCreateTimeDesc(Long houseId);
}
