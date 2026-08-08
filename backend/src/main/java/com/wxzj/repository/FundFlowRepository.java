package com.wxzj.repository;

import com.wxzj.entity.FundFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface FundFlowRepository extends JpaRepository<FundFlow, Long>, JpaSpecificationExecutor<FundFlow> {

    List<FundFlow> findByAccountIdOrderByBizTimeDesc(Long accountId);
}
