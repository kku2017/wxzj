package com.wxzj.repository;

import com.wxzj.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    List<Approval> findByBizTypeAndBizIdOrderByNodeNoAsc(String bizType, Long bizId);
}
