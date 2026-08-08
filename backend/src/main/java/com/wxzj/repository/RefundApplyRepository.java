package com.wxzj.repository;

import com.wxzj.entity.RefundApply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RefundApplyRepository extends JpaRepository<RefundApply, Long>, JpaSpecificationExecutor<RefundApply> {

    List<RefundApply> findByAccountIdOrderByCreateTimeDesc(Long accountId);
}
