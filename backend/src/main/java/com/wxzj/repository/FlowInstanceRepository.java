package com.wxzj.repository;

import com.wxzj.entity.FlowInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FlowInstanceRepository extends JpaRepository<FlowInstance, Long> {

    Optional<FlowInstance> findByBizTypeAndBizId(String bizType, Long bizId);
}
